package com.janmanch.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/** Stores files in a folder on disk and serves them under /media/**. */
@Service
@ConditionalOnProperty(name = "janmanch.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalStorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalStorageService.class);

    private final Path baseDir;

    public LocalStorageService(@Value("${janmanch.storage.local-dir}") String localDir) throws IOException {
        this.baseDir = Paths.get(localDir).toAbsolutePath().normalize();
        Files.createDirectories(baseDir);
    }

    @Override
    public void store(String key, InputStream content, long size, String contentType) throws IOException {
        Path target = resolve(key);
        Files.createDirectories(target.getParent());
        Files.copy(content, target, StandardCopyOption.REPLACE_EXISTING);
    }

    @Override
    public Path fetchToLocal(String key) throws IOException {
        Path path = resolve(key);
        if (!Files.exists(path)) {
            throw new IOException("File not found in local storage: " + key);
        }
        return path;
    }

    @Override
    public void releaseLocal(Path path) {
        // The file is the stored original, so there is nothing to clean up.
    }

    @Override
    public String urlFor(String key) {
        return "/media/" + key;
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException ex) {
            log.warn("Could not delete {} from local storage", key, ex);
        }
    }

    /** Keeps every key inside the storage folder. */
    private Path resolve(String key) throws IOException {
        Path path = baseDir.resolve(key).normalize();
        if (!path.startsWith(baseDir)) {
            throw new IOException("Invalid storage key: " + key);
        }
        return path;
    }
}
