package org.openelisglobal.reports.dataexport.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ReportingFiles {
    @Autowired
    private ReportingSettings settings;

    public Path path(String id) {
        UUID.fromString(id);
        return settings.directory().resolve(id + ".csv");
    }

    public Path stage(String id, String worker) throws IOException {
        Files.createDirectories(settings.directory(),
                PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
        return Files.createFile(stagedPath(id, worker),
                PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
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
