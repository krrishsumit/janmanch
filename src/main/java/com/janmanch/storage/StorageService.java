package com.janmanch.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

/**
 * Where video and thumbnail files live. Two implementations:
 * local disk (development) and Amazon S3 (deployment).
 */
public interface StorageService {

    void store(String key, InputStream content, long size, String contentType) throws IOException;

    /** Returns a local file with the object's content, for FFmpeg to read. */
    Path fetchToLocal(String key) throws IOException;

    /** Cleans up a file returned by fetchToLocal once processing is finished. */
    void releaseLocal(Path path);

    /** A URL the browser can use to load the file. */
    String urlFor(String key);

    void delete(String key);
}
