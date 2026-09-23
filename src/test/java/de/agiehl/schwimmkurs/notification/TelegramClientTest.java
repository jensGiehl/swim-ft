package de.agiehl.schwimmkurs.notification;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TelegramClientTest {

    @Test
    void splitsLongMessagesAtLineBoundaries() {
        var message = "A".repeat(300) + "\n" + "B".repeat(300);

        var parts = TelegramClient.split(message, 500);

        assertThat(parts).containsExactly("A".repeat(300), "B".repeat(300));
    }
}
