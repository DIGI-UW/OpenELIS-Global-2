package org.openelisglobal.document.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class LocalFileSystemStorageService implements DocumentStorageService {

    private final Path basePath;

    public LocalFileSystemStorageService(@Value("${document.storage.path:./data/documents}") String base) throws IOException {
        this.basePath = Path.of(base).toAbsolutePath();
        Files.createDirectories(this.basePath);
    }

    @Override
    public String store(InputStream data, String targetPath) throws IOException {
        Path dest = basePath.resolve(targetPath);
        Files.createDirectories(dest.getParent());
        Files.copy(data, dest, StandardCopyOption.REPLACE_EXISTING);
        return dest.toString();
    }

    @Override
    public InputStream retrieve(String storagePath) throws IOException {
        Path p = Path.of(storagePath);
        return Files.newInputStream(p);
    }

    @Override
    public void delete(String storagePath) throws IOException {
        Path p = Path.of(storagePath);
        Files.deleteIfExists(p);
    }
}
