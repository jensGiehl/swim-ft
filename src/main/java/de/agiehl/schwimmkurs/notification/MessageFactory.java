package de.agiehl.schwimmkurs.notification;

import de.agiehl.schwimmkurs.config.SchwimmkursProperties;
import de.agiehl.schwimmkurs.domain.CourseDiff;
import org.springframework.stereotype.Component;

@Component
public class MessageFactory {

    private final SchwimmkursProperties properties;

    public MessageFactory(SchwimmkursProperties properties) {
        this.properties = properties;
    }

    public String changeMessage(CourseDiff diff, int currentOfferCount) {
        var message = new StringBuilder("🏊 Änderung bei den Schwimmkursen\n");
        if (!diff.added().isEmpty()) {
            message.append("\nNeu verfügbar:\n");
            diff.added().forEach(offer -> message.append("+ ").append(offer.readableDescription()).append('\n'));
        }
        if (!diff.removed().isEmpty()) {
            message.append("\nNicht mehr verfügbar oder geändert:\n");
            diff.removed().forEach(offer -> message.append("- ").append(offer.readableDescription()).append('\n'));
        }
        message.append("\nAktuell gefundene Angebote: ").append(currentOfferCount)
                .append("\n\nZur Kursseite:\n").append(properties.source().url());
        return message.toString();
    }

    public String healthCheckMessage() {
        return "✅ " + properties.healthCheck().message()
                + "\n\nZur Kursseite:\n" + properties.source().url();
    }
}
