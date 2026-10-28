package org.openelisglobal.program.service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Set;

/**
 * Validates case attachment content before the owning service mutates the case.
 */
public final class ClinicalAttachmentService {
    private static final int MAX_BYTES = 10 * 1024 * 1024;
    private static final Set<String> TYPES = Set.of("data:image/jpeg", "data:image/png", "data:application/pdf");

    private ClinicalAttachmentService() {
    }

    public static void validate(ClinicalAttachmentUpload attachment) {
        byte[] bytes = attachment.getImage();
        String type = attachment.getFileType();
        String encoded = attachment.getBase64Image();
        if (encoded != null) {
            int separator = encoded.indexOf(";base64,");
            if (separator < 0 || encoded.length() > 4L * ((MAX_BYTES + 2) / 3) + 40) {
                throw new InvalidClinicalAttachmentException();
            }
            type = encoded.substring(0, separator);
            if (!TYPES.contains(type)) {
                throw new InvalidClinicalAttachmentException();
            }
            try {
                bytes = Base64.getDecoder().decode(encoded.substring(separator + 8));
            } catch (IllegalArgumentException e) {
                throw new InvalidClinicalAttachmentException();
            }
        }
        // Existing slide rows may be posted without their stored image. Preserve it.
        if (bytes == null) {
            return;
        }
        if (type == null || !TYPES.contains(type) || bytes.length == 0 || bytes.length > MAX_BYTES
                || !matchesType(type, bytes)) {
            throw new InvalidClinicalAttachmentException();
        }
        attachment.setImage(bytes);
        attachment.setFileType(type);
    }

    private static boolean matchesType(String type, byte[] bytes) {
        return switch (type) {
        case "data:image/png" -> startsWith(bytes, new byte[] { (byte) 137, 80, 78, 71, 13, 10, 26, 10 });
        case "data:image/jpeg" -> startsWith(bytes, new byte[] { (byte) 255, (byte) 216, (byte) 255 });
        case "data:application/pdf" -> startsWith(bytes, "%PDF-".getBytes(StandardCharsets.US_ASCII));
        default -> false;
        };
    }

    private static boolean startsWith(byte[] bytes, byte[] signature) {
        if (bytes.length < signature.length) {
            return false;
        }
        for (int index = 0; index < signature.length; index++) {
            if (bytes[index] != signature[index]) {
                return false;
            }
        }
        return true;
    }
}
