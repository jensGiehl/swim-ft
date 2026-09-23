package de.agiehl.schwimmkurs.service;

import de.agiehl.schwimmkurs.config.SchwimmkursProperties;
import de.agiehl.schwimmkurs.domain.CourseOffer;
import de.agiehl.schwimmkurs.domain.MonitorState;
import de.agiehl.schwimmkurs.domain.OfferField;
import de.agiehl.schwimmkurs.notification.MessageFactory;
import de.agiehl.schwimmkurs.notification.TelegramClient;
import de.agiehl.schwimmkurs.source.CoursePageClient;
import de.agiehl.schwimmkurs.state.StateRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.net.URI;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MonitorServiceTest {

    private static final ZoneId ZONE_ID = ZoneId.of("Europe/Berlin");

    @Test
    void firstRunSendsStartupMessageAndStoresSnapshot() {
        var fixture = fixture("2026-09-23T12:00:00Z");
        var offer = offer("2");
        when(fixture.pageClient.fetchOffers()).thenReturn(List.of(offer));
        when(fixture.stateRepository.load()).thenReturn(Optional.empty());

        fixture.service.check();

        verify(fixture.telegramClient).send(org.mockito.ArgumentMatchers.startsWith("🏊 Der Schwimmkurs-Monitor"));
        var state = ArgumentCaptor.forClass(MonitorState.class);
        verify(fixture.stateRepository).save(state.capture());
        assertThat(state.getValue().offers()).containsExactly(offer);
    }

    @Test
    void firstRunDoesNotStoreSnapshotWhenStartupMessageFails() {
        var fixture = fixture("2026-09-23T12:00:00Z");
        when(fixture.pageClient.fetchOffers()).thenReturn(List.of());
        when(fixture.stateRepository.load()).thenReturn(Optional.empty());
        doThrow(new IllegalStateException("Telegram nicht erreichbar"))
                .when(fixture.telegramClient).send(org.mockito.ArgumentMatchers.anyString());

        assertThatThrownBy(fixture.service::check)
                .isInstanceOf(IllegalStateException.class);

        verify(fixture.stateRepository, never()).save(org.mockito.ArgumentMatchers.any(MonitorState.class));
    }

    @Test
    void sendsHealthCheckOnSundayAfterConfiguredTime() {
        var fixture = fixture("2026-09-20T16:30:00Z");
        var existing = new MonitorState(Instant.parse("2026-09-20T16:25:00Z"), List.of(), null);
        when(fixture.pageClient.fetchOffers()).thenReturn(List.of());
        when(fixture.stateRepository.load()).thenReturn(Optional.of(existing));

        fixture.service.check();

        verify(fixture.telegramClient).send(org.mockito.ArgumentMatchers.startsWith("✅"));
        var states = ArgumentCaptor.forClass(MonitorState.class);
        verify(fixture.stateRepository, org.mockito.Mockito.times(2)).save(states.capture());
        assertThat(states.getAllValues().getLast().lastHealthCheckDate()).isEqualTo(LocalDate.parse("2026-09-20"));
    }

    @Test
    void doesNotSendHealthCheckTwiceOnTheSameSunday() {
        var fixture = fixture("2026-09-20T19:00:00Z");
        var existing = new MonitorState(
                Instant.parse("2026-09-20T18:55:00Z"),
                List.of(),
                LocalDate.parse("2026-09-20")
        );
        when(fixture.pageClient.fetchOffers()).thenReturn(List.of());
        when(fixture.stateRepository.load()).thenReturn(Optional.of(existing));

        fixture.service.check();

        verify(fixture.telegramClient, never()).send(org.mockito.ArgumentMatchers.anyString());
        verify(fixture.stateRepository).save(org.mockito.ArgumentMatchers.any(MonitorState.class));
    }

    private Fixture fixture(String instant) {
        var properties = properties();
        var pageClient = mock(CoursePageClient.class);
        var stateRepository = mock(StateRepository.class);
        var telegramClient = mock(TelegramClient.class);
        var messageFactory = new MessageFactory(properties);
        var clock = Clock.fixed(Instant.parse(instant), ZONE_ID);
        var service = new MonitorService(properties, pageClient, stateRepository, telegramClient, messageFactory, clock);
        return new Fixture(service, pageClient, stateRepository, telegramClient);
    }

    private CourseOffer offer(String freePlaces) {
        return new CourseOffer(List.of(new OfferField("Freie Plätze", freePlaces)));
    }

    private SchwimmkursProperties properties() {
        return new SchwimmkursProperties(
                new SchwimmkursProperties.Source(URI.create("https://example.org"), "#courses", "Firefox", Duration.ofSeconds(1), Duration.ofSeconds(1)),
                new SchwimmkursProperties.State("state.bin", "state.lock"),
                new SchwimmkursProperties.Telegram(URI.create("https://api.telegram.org"), "token", "chat", false, 3900),
                new SchwimmkursProperties.HealthCheck(true, DayOfWeek.SUNDAY, LocalTime.of(18, 0), ZONE_ID, "Läuft")
        );
    }

    private record Fixture(
            MonitorService service,
            CoursePageClient pageClient,
            StateRepository stateRepository,
            TelegramClient telegramClient
    ) {
    }
}
