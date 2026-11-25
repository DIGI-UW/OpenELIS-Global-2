package org.openelisglobal.reports.dataexport.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.ReadOnlyFileSystemException;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.UUID;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ReportingFiles {
    public static final String OUTPUT_UNAVAILABLE = "reporting.job.outputUnavailable";
    @Autowired
    private ReportingSettings settings;

    public Path path(String id) {
        UUID.fromString(id);
        return settings.directory().resolve(id + ".csv");
    }

    /**
     * Creates the job's partial file. An export directory the application cannot
     * write fails the job as {@value #OUTPUT_UNAVAILABLE}, which the queue explains
     * to the user, instead of the generic generation failure that hid the cause.
     */
    public Path stage(String id, String worker) throws IOException {
        try {
            Files.createDirectories(settings.directory(),
                    PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
            return Files.createFile(stagedPath(id, worker),
                    PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
        } catch (AccessDeniedException | NotDirectoryException | ReadOnlyFileSystemException error) {
            LogEvent.logError(getClass().getSimpleName(), "stage",
                    "Export directory " + settings.directory() + " is not writable by the application: " + error);
            throw new ReportingException(503, OUTPUT_UNAVAILABLE);
        }
    }

    /**
     * Whether export files can be written, checked once at startup so an unwritable
     * directory is reported before any user runs a report.
     */
    public boolean outputWritable() {
        Path directory = settings.directory();
        try {
            Files.createDirectories(directory);
        } catch (IOException | UnsupportedOperationException error) {
            return false;
        }
        return Files.isDirectory(directory) && Files.isWritable(directory);
    }

    private Path stagedPath(String id, String worker) {
        UUID.fromString(id);
        UUID.fromString(worker);
        return settings.directory().resolve(id + "." + worker + ".part");
    }

    public long publish(String id, String worker) throws IOException {
        Files.move(stagedPath(id, worker), path(id), java.nio.file.StandardCopyOption.ATOMIC_MOVE);
        return Files.size(path(id));
    }

    public void removeOutput(String id, String worker) throws IOException {
        if (worker != null)
            Files.deleteIfExists(stagedPath(id, worker));
        Files.deleteIfExists(path(id));
    }

    public InputStream open(String id) throws IOException {
        return Files.newInputStream(path(id));
    }
}
