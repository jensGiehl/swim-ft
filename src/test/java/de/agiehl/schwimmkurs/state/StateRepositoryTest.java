package de.agiehl.schwimmkurs.state;

import de.agiehl.schwimmkurs.config.SchwimmkursProperties;
import de.agiehl.schwimmkurs.domain.CourseOffer;
import de.agiehl.schwimmkurs.domain.MonitorState;
import de.agiehl.schwimmkurs.domain.OfferField;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StateRepositoryTest {

    @TempDir
    Path directory;

    @Test
    void savesAndLoadsState() {
        var repository = new StateRepository(properties());
        var state = new MonitorState(
                Instant.parse("2026-09-23T18:00:00Z"),
                List.of(new CourseOffer(List.of(new OfferField("Freie Plätze", "2")))),
                LocalDate.parse("2026-09-20")
        );

        repository.save(state);

        assertThat(repository.load()).contains(state);
    }

    private SchwimmkursProperties properties() {
        return new SchwimmkursProperties(
                new SchwimmkursProperties.Source(URI.create("https://example.org"), "#courses", "Firefox", Duration.ofSeconds(1), Duration.ofSeconds(1)),
                new SchwimmkursProperties.State(directory.resolve("state.bin").toString(), directory.resolve("state.lock").toString()),
                new SchwimmkursProperties.Telegram(URI.create("https://api.telegram.org"), "token", "chat", false, 3900),
                new SchwimmkursProperties.HealthCheck(true, DayOfWeek.SUNDAY, LocalTime.of(18, 0), ZoneId.of("Europe/Berlin"), "Läuft")
        );
    }
}
