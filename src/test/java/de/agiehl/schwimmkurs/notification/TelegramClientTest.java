package de.agiehl.schwimmkurs.notification;

import com.sun.net.httpserver.HttpServer;
import de.agiehl.schwimmkurs.config.SchwimmkursProperties;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class TelegramClientTest {

    @Test
    void sendsGermanCharactersAsUtf8() throws Exception {
        var contentType = new AtomicReference<String>();
        var requestBody = new AtomicReference<String>();
        var server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/bottest-token/sendMessage", exchange -> {
            contentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.US_ASCII));
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        });
        server.start();

        try {
            var apiBaseUrl = URI.create("http://localhost:" + server.getAddress().getPort());
            var client = new TelegramClient(properties(apiBaseUrl));

            client.send("Grüße für Plätze: äöü ÄÖÜ ß €");

            assertThat(contentType.get()).isEqualTo("application/x-www-form-urlencoded;charset=UTF-8");
            assertThat(decodeForm(requestBody.get())).containsEntry("text", "Grüße für Plätze: äöü ÄÖÜ ß €");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void splitsLongMessagesAtLineBoundaries() {
        var message = "A".repeat(300) + "\n" + "B".repeat(300);

        var parts = TelegramClient.split(message, 500);

        assertThat(parts).containsExactly("A".repeat(300), "B".repeat(300));
    }

    private Map<String, String> decodeForm(String body) {
        return Arrays.stream(body.split("&"))
                .map(parameter -> parameter.split("=", 2))
                .collect(Collectors.toMap(
                        pair -> URLDecoder.decode(pair[0], StandardCharsets.UTF_8),
                        pair -> URLDecoder.decode(pair[1], StandardCharsets.UTF_8)
                ));
    }

    private SchwimmkursProperties properties(URI apiBaseUrl) {
        return new SchwimmkursProperties(
                new SchwimmkursProperties.Source(
                        URI.create("https://example.org"),
                        "#courses",
                        "Firefox",
                        Duration.ofSeconds(1),
                        Duration.ofSeconds(1)
                ),
                new SchwimmkursProperties.State("state.bin", "state.lock"),
                new SchwimmkursProperties.Telegram(apiBaseUrl, "test-token", "test-chat", false, 3900),
                new SchwimmkursProperties.HealthCheck(
                        true,
                        DayOfWeek.SUNDAY,
                        LocalTime.of(18, 0),
                        ZoneId.of("Europe/Berlin"),
                        "Läuft"
                )
        );
    }
}
