package de.agiehl.schwimmkurs.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CourseDiffTest {

    @Test
    void handlesDuplicateOffersAsAMultiset() {
        var offer = new CourseOffer(List.of(new OfferField("Freie Plätze", "1")));

        var diff = CourseDiff.between(List.of(offer, offer), List.of(offer));

        assertThat(diff.added()).isEmpty();
        assertThat(diff.removed()).containsExactly(offer);
    }
}
