package de.agiehl.schwimmkurs.service;

import de.agiehl.schwimmkurs.config.SchwimmkursProperties;
import de.agiehl.schwimmkurs.domain.CourseDiff;
import de.agiehl.schwimmkurs.domain.MonitorState;
import de.agiehl.schwimmkurs.notification.MessageFactory;
import de.agiehl.schwimmkurs.notification.TelegramClient;
import de.agiehl.schwimmkurs.source.CoursePageClient;
import de.agiehl.schwimmkurs.state.StateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.ZonedDateTime;

@Service
public class MonitorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MonitorService.class);

    private final SchwimmkursProperties properties;
    private final CoursePageClient pageClient;
    private final StateRepository stateRepository;
    private final TelegramClient telegramClient;
    private final MessageFactory messageFactory;
    private final Clock clock;

    @Autowired
    public MonitorService(
            SchwimmkursProperties properties,
            CoursePageClient pageClient,
            StateRepository stateRepository,
            TelegramClient telegramClient,
            MessageFactory messageFactory
    ) {
        this(properties, pageClient, stateRepository, telegramClient, messageFactory, Clock.systemUTC());
    }

    MonitorService(
            SchwimmkursProperties properties,
            CoursePageClient pageClient,
            StateRepository stateRepository,
            TelegramClient telegramClient,
            MessageFactory messageFactory,
            Clock clock
    ) {
        this.properties = properties;
        this.pageClient = pageClient;
        this.stateRepository = stateRepository;
        this.telegramClient = telegramClient;
        this.messageFactory = messageFactory;
        this.clock = clock;
    }

    public void check() {
        var offers = pageClient.fetchOffers();
        var now = clock.instant();
        var previousState = stateRepository.load();
        var state = previousState
                .map(existing -> existing.withSnapshot(now, offers))
                .orElseGet(() -> new MonitorState(now, offers, null));

        if (previousState.isEmpty()) {
            telegramClient.send(messageFactory.startupMessage());
            stateRepository.save(state);
            LOGGER.info("Startmeldung gesendet und erster Snapshot mit {} Angeboten gespeichert.", offers.size());
        } else {
            var diff = CourseDiff.between(previousState.orElseThrow().offers(), offers);
            if (diff.hasChanges()) {
                telegramClient.send(messageFactory.changeMessage(diff, offers.size()));
                LOGGER.info("Änderung mit {} neuen und {} entfernten Angeboten gemeldet.", diff.added().size(), diff.removed().size());
            } else {
                LOGGER.info("Keine Änderung bei {} gefundenen Angeboten.", offers.size());
            }
            stateRepository.save(state);
        }

        if (healthCheckIsDue(state)) {
            telegramClient.send(messageFactory.healthCheckMessage());
            state = state.withLastHealthCheckDate(localNow().toLocalDate());
            stateRepository.save(state);
            LOGGER.info("Wöchentlicher Health-Check gesendet.");
        }
    }

    private boolean healthCheckIsDue(MonitorState state) {
        if (!properties.healthCheck().enabled()) {
            return false;
        }
        var localNow = localNow();
        return localNow.getDayOfWeek() == properties.healthCheck().dayOfWeek()
                && !localNow.toLocalTime().isBefore(properties.healthCheck().earliestTime())
                && !localNow.toLocalDate().equals(state.lastHealthCheckDate());
    }

    private ZonedDateTime localNow() {
        return ZonedDateTime.ofInstant(clock.instant(), properties.healthCheck().zoneId());
    }
}
