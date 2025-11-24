package org.openelisglobal.document.validation;

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Component;

@Component
public class DocumentValidationServiceImpl implements DocumentValidationService {

    private static final long MAX_BYTES = 10 * 1024 * 1024; // 10MB
    private static final Set<String> ALLOWED = new HashSet<>(Arrays.asList(
            "image/jpeg",
            "image/png",
            "application/pdf"
    ));

    @Override
    public void validate(InputStream data, String contentType, long size) throws Exception {
        if (size <= 0) throw new IllegalArgumentException("Empty file");
        if (size > MAX_BYTES) throw new IllegalArgumentException("File exceeds maximum allowed size of 10MB");
        if (contentType == null || !ALLOWED.contains(contentType)) throw new IllegalArgumentException("Unsupported content type: " + contentType);
        // Basic validation only; deeper checks (image integrity, pdf parsing, malware scan) are separate concerns.
    }
}
