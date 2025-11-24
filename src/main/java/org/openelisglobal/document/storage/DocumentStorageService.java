package org.openelisglobal.document.storage;

import java.io.InputStream;

public interface DocumentStorageService {
    /**
     * Store the given input stream under a generated/derived path and return the storage path.
     */
    String store(InputStream data, String targetPath) throws java.io.IOException;

    /**
     * Retrieve an InputStream for the given storage path.
     */
    InputStream retrieve(String storagePath) throws java.io.IOException;

    /**
     * Delete the object at storage path (best-effort / soft-delete support optional).
     */
    void delete(String storagePath) throws java.io.IOException;
}
