package org.openelisglobal.storage.service;

import java.util.Set;

/**
 * Service for generating codes from location names Generates codes following
 * algorithm: uppercase, remove non-alphanumeric (keep hyphens/underscores),
 * truncate to 10 chars, append numeric suffix if conflict
 */
public interface CodeGenerationService {

    /**
     * Generate code from location name Algorithm: uppercase name, remove
     * non-alphanumeric characters (keep hyphens/underscores), truncate to 10 chars
     * 
     * @param name    Location name
     * @param context Context type: "room", "device", "shelf", or "rack"
     * @return Generated code (≤10 chars)
     */
    String generateCodeFromName(String name, String context);

    /**
     * Generate code with conflict resolution If generated code conflicts with
     * existing codes, append numeric suffix (e.g., "MAINLAB-1", "MAINLAB-2")
     * 
     * @param name          Location name
     * @param context       Context type: "room", "device", "shelf", or "rack"
     * @param existingCodes Set of existing codes to check against
     * @return Generated code with conflict resolution (≤10 chars)
     */
    String generateCodeWithConflictResolution(String name, String context, Set<String> existingCodes);
}
