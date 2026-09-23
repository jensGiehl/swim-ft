package de.agiehl.schwimmkurs.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record MonitorState(Instant checkedAt, List<CourseOffer> offers, LocalDate lastHealthCheckDate) {

    public MonitorState {
        offers = List.copyOf(offers);
    }

    public MonitorState withSnapshot(Instant newCheckedAt, List<CourseOffer> newOffers) {
        return new MonitorState(newCheckedAt, newOffers, lastHealthCheckDate);
    }

    public MonitorState withLastHealthCheckDate(LocalDate date) {
        return new MonitorState(checkedAt, offers, date);
    }
}
