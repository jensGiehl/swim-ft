package de.agiehl.schwimmkurs.domain;

import java.util.ArrayList;
import java.util.List;

public record CourseDiff(List<CourseOffer> added, List<CourseOffer> removed) {

    public CourseDiff {
        added = List.copyOf(added);
        removed = List.copyOf(removed);
    }

    public static CourseDiff between(List<CourseOffer> previous, List<CourseOffer> current) {
        var unmatchedPrevious = new ArrayList<>(previous);
        var added = new ArrayList<CourseOffer>();

        for (var offer : current) {
            if (!unmatchedPrevious.remove(offer)) {
                added.add(offer);
            }
        }

        return new CourseDiff(added, unmatchedPrevious);
    }

    public boolean hasChanges() {
        return !added.isEmpty() || !removed.isEmpty();
    }
}
