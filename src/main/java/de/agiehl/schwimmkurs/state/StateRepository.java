package de.agiehl.schwimmkurs.state;

import de.agiehl.schwimmkurs.config.SchwimmkursProperties;
import de.agiehl.schwimmkurs.domain.CourseOffer;
import de.agiehl.schwimmkurs.domain.MonitorState;
import de.agiehl.schwimmkurs.domain.OfferField;
import org.springframework.stereotype.Repository;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Optional;

@Repository
public class StateRepository {

    private static final int MAGIC = 0x53434654;
    private static final int VERSION = 1;

    private final Path stateFile;

    public StateRepository(SchwimmkursProperties properties) {
        stateFile = Path.of(properties.state().file()).toAbsolutePath().normalize();
    }

    public Optional<MonitorState> load() {
        if (Files.notExists(stateFile)) {
            return Optional.empty();
        }

        try (var input = new DataInputStream(new BufferedInputStream(Files.newInputStream(stateFile)))) {
            if (input.readInt() != MAGIC || input.readInt() != VERSION) {
                throw new IllegalStateException("Unbekanntes Format der Statusdatei: " + stateFile);
            }
            var checkedAt = Instant.ofEpochMilli(input.readLong());
            var healthDate = input.readBoolean() ? LocalDate.ofEpochDay(input.readLong()) : null;
            var offers = new ArrayList<CourseOffer>();
            var offerCount = readCount(input, "Angebote");
            for (int offerIndex = 0; offerIndex < offerCount; offerIndex++) {
                var fields = new ArrayList<OfferField>();
                var fieldCount = readCount(input, "Felder");
                for (int fieldIndex = 0; fieldIndex < fieldCount; fieldIndex++) {
                    fields.add(new OfferField(input.readUTF(), input.readUTF()));
                }
                offers.add(new CourseOffer(fields));
            }
            return Optional.of(new MonitorState(checkedAt, offers, healthDate));
        } catch (EOFException exception) {
            throw new IllegalStateException("Die Statusdatei ist unvollständig: " + stateFile, exception);
        } catch (IOException exception) {
            throw new IllegalStateException("Die Statusdatei konnte nicht gelesen werden: " + stateFile, exception);
        }
    }

    public void save(MonitorState state) {
        var parent = stateFile.getParent();
        try {
            Files.createDirectories(parent);
            var temporaryFile = Files.createTempFile(parent, stateFile.getFileName().toString(), ".tmp");
            try {
                write(temporaryFile, state);
                moveAtomically(temporaryFile);
            } finally {
                Files.deleteIfExists(temporaryFile);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Die Statusdatei konnte nicht gespeichert werden: " + stateFile, exception);
        }
    }

    private void write(Path file, MonitorState state) throws IOException {
        try (var output = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(file)))) {
            output.writeInt(MAGIC);
            output.writeInt(VERSION);
            output.writeLong(state.checkedAt().toEpochMilli());
            output.writeBoolean(state.lastHealthCheckDate() != null);
            if (state.lastHealthCheckDate() != null) {
                output.writeLong(state.lastHealthCheckDate().toEpochDay());
            }
            output.writeInt(state.offers().size());
            for (var offer : state.offers()) {
                output.writeInt(offer.fields().size());
                for (var field : offer.fields()) {
                    output.writeUTF(field.name());
                    output.writeUTF(field.value());
                }
            }
        }
    }

    private void moveAtomically(Path temporaryFile) throws IOException {
        try {
            Files.move(temporaryFile, stateFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(temporaryFile, stateFile, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static int readCount(DataInputStream input, String description) throws IOException {
        var count = input.readInt();
        if (count < 0 || count > 10_000) {
            throw new IllegalStateException("Ungültige Anzahl für " + description + ": " + count);
        }
        return count;
    }
}
