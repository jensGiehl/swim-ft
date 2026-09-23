package de.agiehl.schwimmkurs.domain;

import java.util.List;

public record CourseOffer(List<OfferField> fields) {

    public CourseOffer {
        fields = List.copyOf(fields);
    }

    public String readableDescription() {
        return fields.stream()
                .map(field -> field.name() + ": " + field.value())
                .reduce((left, right) -> left + " | " + right)
                .orElse("Unbekanntes Angebot");
    }
}
