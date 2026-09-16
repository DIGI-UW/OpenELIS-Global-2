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
        UUID.fromString(id);
        UUID.fromString(worker);
        Files.createDirectories(settings.directory(),
                PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
        return Files.createFile(settings.directory().resolve(id + "." + worker + ".part"),
                PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
    }

    public InputStream open(String id) throws IOException {
        return Files.newInputStream(path(id));
    }
}
