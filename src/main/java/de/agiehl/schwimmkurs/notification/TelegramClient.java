package de.agiehl.schwimmkurs.notification;

import de.agiehl.schwimmkurs.config.SchwimmkursProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class TelegramClient {

    private static final MediaType UTF_8_FORM = new MediaType(
            MediaType.APPLICATION_FORM_URLENCODED,
            StandardCharsets.UTF_8
    );

    private final SchwimmkursProperties properties;
    private final RestClient restClient;

    public TelegramClient(SchwimmkursProperties properties) {
        this.properties = properties;
        restClient = RestClient.builder()
                .baseUrl(properties.telegram().apiBaseUrl().toString())
                .build();
    }

    public void send(String message) {
        for (var part : split(message, properties.telegram().maximumMessageLength())) {
            var form = new LinkedMultiValueMap<String, String>();
            form.add("chat_id", properties.telegram().chatId());
            form.add("text", part);
            form.add("disable_notification", Boolean.toString(properties.telegram().disableNotification()));
            restClient.post()
                    .uri("/bot{token}/sendMessage", properties.telegram().botToken())
                    .contentType(UTF_8_FORM)
                    .body(form)
                    .retrieve()
                    .toBodilessEntity();
        }
    }

    static java.util.List<String> split(String message, int maximumLength) {
        var parts = new java.util.ArrayList<String>();
        var remaining = message.strip();
        while (remaining.length() > maximumLength) {
            var splitAt = remaining.lastIndexOf('\n', maximumLength);
            if (splitAt < maximumLength / 2) {
                splitAt = maximumLength;
            }
            parts.add(remaining.substring(0, splitAt).strip());
            remaining = remaining.substring(splitAt).strip();
        }
        if (!remaining.isEmpty()) {
            parts.add(remaining);
        }
        return List.copyOf(parts);
    }
}
