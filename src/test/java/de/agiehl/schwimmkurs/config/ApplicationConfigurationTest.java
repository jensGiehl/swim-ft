package de.agiehl.schwimmkurs.config;

import de.agiehl.schwimmkurs.notification.MessageFactory;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationConfigurationTest {

    @Test
    void loadsDefaultHealthCheckMessageAsUtf8() throws IOException {
        var environment = new StandardEnvironment();
        var propertySources = new YamlPropertySourceLoader()
                .load("application", new ClassPathResource("application.yaml"));
        propertySources.forEach(environment.getPropertySources()::addLast);
        var properties = Binder.get(environment)
                .bind("schwimmkurs", Bindable.of(SchwimmkursProperties.class))
                .orElseThrow(() -> new IllegalStateException("Die Standardkonfiguration konnte nicht geladen werden."));

        assertThat(properties.healthCheck().message())
                .isEqualTo("Schwimmkurs-Monitor läuft weiterhin und konnte die Kursseite erfolgreich prüfen.");
        assertThat(new MessageFactory(properties).healthCheckMessage())
                .startsWith("✅ Schwimmkurs-Monitor läuft weiterhin und konnte die Kursseite erfolgreich prüfen.");
    }
}
