package de.agiehl.schwimmkurs.state;

import de.agiehl.schwimmkurs.config.SchwimmkursProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

@Component
public class RunLock {

    private final Path lockFile;

    public RunLock(SchwimmkursProperties properties) {
        lockFile = Path.of(properties.state().lockFile()).toAbsolutePath().normalize();
    }

    public LockedRun acquire() {
        try {
            Files.createDirectories(lockFile.getParent());
            var channel = FileChannel.open(lockFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            try {
                var lock = channel.tryLock();
                if (lock == null) {
                    channel.close();
                    throw new IllegalStateException("Ein anderer Programmlauf ist noch aktiv.");
                }
                return new LockedRun(channel, lock);
            } catch (OverlappingFileLockException exception) {
                channel.close();
                throw new IllegalStateException("Ein anderer Programmlauf ist noch aktiv.", exception);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Die Sperrdatei konnte nicht verwendet werden: " + lockFile, exception);
        }
    }

    public record LockedRun(FileChannel channel, FileLock lock) implements AutoCloseable {

        @Override
        public void close() {
            try {
                lock.close();
            } catch (IOException exception) {
                throw new IllegalStateException("Die Programmsperre konnte nicht freigegeben werden.", exception);
            } finally {
                try {
                    channel.close();
                } catch (IOException exception) {
                    throw new IllegalStateException("Die Sperrdatei konnte nicht geschlossen werden.", exception);
                }
            }
        }
    }
}
