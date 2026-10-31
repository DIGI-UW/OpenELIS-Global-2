package org.openelisglobal.program.service;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Validates case attachment content before the owning service mutates the case.
 */
public final class ClinicalAttachmentService {
    private static final int MAX_BYTES = 10 * 1024 * 1024;
    private static final Set<String> TYPES = Set.of("data:image/jpeg", "data:image/png", "data:application/pdf");

    private ClinicalAttachmentService() {
    }

    public static boolean isUnchanged(ClinicalAttachmentUpload upload, Integer storedId, byte[] storedImage,
            String storedType) {
        return storedId != null && storedId.equals(upload.getId()) && upload.getBase64Image() == null
                && Objects.equals(storedType, upload.getFileType()) && Arrays.equals(storedImage, upload.getImage());
    }

    public static void validate(ClinicalAttachmentUpload attachment) {
        byte[] bytes = attachment.getImage();
        String type = canonicalType(attachment.getFileType());
        String encoded = attachment.getBase64Image();
        if (encoded != null) {
            int separator = encoded.indexOf(";base64,");
            if (separator < 0 || encoded.length() > 4L * ((MAX_BYTES + 2) / 3) + 256) {
                throw new InvalidClinicalAttachmentException();
            }
            type = canonicalType(encoded.substring(0, separator));
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
        validateContent(type, bytes);
        attachment.setImage(bytes);
        attachment.setFileType(type);
    }

    public static void validateContent(String type, byte[] bytes) {
        if (type == null || !TYPES.contains(type) || bytes == null || bytes.length == 0 || bytes.length > MAX_BYTES
                || !matchesType(type, bytes)) {
            throw new InvalidClinicalAttachmentException();
        }
    }

    private static String canonicalType(String type) {
        return type == null ? null : type.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
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
