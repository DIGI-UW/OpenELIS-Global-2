package org.openelisglobal.storage.service;

import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * Implementation of CodeGenerationService Generates codes from location names
 * following specified algorithm
 */
@Service
public class CodeGenerationServiceImpl implements CodeGenerationService {

    private static final int MAX_CODE_LENGTH = 10;

    @Override
    public String generateCodeFromName(String name, String context) {
        if (StringUtils.isBlank(name)) {
            // Fallback for empty/null names
            return "CODE-1";
        }

        // Step 1: Convert to uppercase
        String code = name.toUpperCase();

        // Step 2: Remove non-alphanumeric characters (keep hyphens and underscores)
        code = code.replaceAll("[^A-Z0-9_-]", "");

        // Step 3: Truncate to 10 characters
        if (code.length() > MAX_CODE_LENGTH) {
            code = code.substring(0, MAX_CODE_LENGTH);
        }

        // Step 4: Ensure code starts with letter or number (not hyphen/underscore)
        if (code.length() > 0 && (code.startsWith("-") || code.startsWith("_"))) {
            // If starts with hyphen/underscore, remove it and truncate again if needed
            code = code.substring(1);
            if (code.length() > MAX_CODE_LENGTH) {
                code = code.substring(0, MAX_CODE_LENGTH);
            }
        }

        // If code is empty after processing, use fallback
        if (StringUtils.isBlank(code)) {
            return "CODE-1";
        }

        return code;
    }

    @Override
    public String generateCodeWithConflictResolution(String name, String context, Set<String> existingCodes) {
        // Generate base code
        String baseCode = generateCodeFromName(name, context);

        // Check if base code conflicts
        if (existingCodes == null || !existingCodes.contains(baseCode)) {
            return baseCode;
        }

        // Find next available suffix
        int suffix = 1;
        String codeWithSuffix;
        do {
            // Calculate available space for suffix
            int baseLength = baseCode.length();
            int suffixLength = String.valueOf(suffix).length() + 1; // +1 for hyphen

            if (baseLength + suffixLength > MAX_CODE_LENGTH) {
                // Need to truncate base code to fit suffix
                int maxBaseLength = MAX_CODE_LENGTH - suffixLength;
                if (maxBaseLength > 0) {
                    baseCode = baseCode.substring(0, maxBaseLength);
                } else {
                    // Fallback if suffix alone exceeds limit
                    return "CODE-" + suffix;
                }
            }

            codeWithSuffix = baseCode + "-" + suffix;
            suffix++;
        } while (existingCodes.contains(codeWithSuffix) && suffix < 1000); // Prevent infinite loop

        return codeWithSuffix;
    }
}
