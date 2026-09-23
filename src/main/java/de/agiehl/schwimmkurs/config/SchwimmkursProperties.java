package de.agiehl.schwimmkurs.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalTime;
import java.time.ZoneId;

@Validated
@ConfigurationProperties("schwimmkurs")
public record SchwimmkursProperties(
        @NotNull @Valid Source source,
        @NotNull @Valid State state,
        @NotNull @Valid Telegram telegram,
        @NotNull @Valid HealthCheck healthCheck
) {

    public record Source(
            @NotNull URI url,
            @NotBlank String tableSelector,
            @NotBlank String userAgent,
            @NotNull Duration connectTimeout,
            @NotNull Duration readTimeout
    ) {
    }

    public record State(
            @NotBlank String file,
            @NotBlank String lockFile
    ) {
    }

    public record Telegram(
            @NotNull URI apiBaseUrl,
            @NotBlank String botToken,
            @NotBlank String chatId,
            boolean disableNotification,
            @Min(500) @Max(4096) int maximumMessageLength
    ) {
    }

    public record HealthCheck(
            boolean enabled,
            @NotNull DayOfWeek dayOfWeek,
            @NotNull LocalTime earliestTime,
            @NotNull ZoneId zoneId,
            @NotBlank String message
    ) {
    }
}
