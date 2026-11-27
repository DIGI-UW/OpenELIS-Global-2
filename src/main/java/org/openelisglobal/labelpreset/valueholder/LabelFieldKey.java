package org.openelisglobal.labelpreset.valueholder;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The content fields a label preset can carry (OGC-285 data-model §2.2,
 * OGC-1218). {@link #LAB_NUMBER} is always present, required and first on every
 * preset; the other fifteen are selectable. Each key names the message that
 * labels it on the printed label and in the editor.
 */
public enum LabelFieldKey {
    LAB_NUMBER("barcode.label.info.labNumber"), //
    PATIENT_NAME("barcode.label.info.patientName"), //
    PATIENT_ID("barcode.label.info.patientId"), //
    PATIENT_DOB("barcode.label.info.patientDobFull"), //
    PATIENT_SEX("barcode.label.info.patientSexFull"), //
    SITE_ID("barcode.label.info.siteId"), //
    COLLECTION_DATETIME("barcode.label.info.collectionDateTime"), //
    COLLECTED_BY("barcode.label.info.collectedBy"), //
    TESTS("barcode.label.info.tests"), //
    SPECIMEN_TYPE("barcode.label.info.specimenType"), //
    BLOCK_ID("barcode.label.info.blockId"), //
    SLIDE_ID("barcode.label.info.slideId"), //
    STAIN_TYPE("barcode.label.info.stainType"), //
    CASE_NUMBER("barcode.label.info.caseNumber"), //
    STORAGE_LOCATION("barcode.label.info.storageLocation"), //
    EXPIRY_DATE("barcode.label.info.expiryDate");

    private final String messageKey;

    LabelFieldKey(String messageKey) {
        this.messageKey = messageKey;
    }

    /** The message bundle key of the field's human label. */
    public String getMessageKey() {
        return messageKey;
    }

    /** Whether {@code key} names one of the catalogued fields. */
    public static boolean isKnown(String key) {
        return fromKey(key) != null;
    }

    public static LabelFieldKey fromKey(String key) {
        if (key == null) {
            return null;
        }
        String wanted = key.trim();
        for (LabelFieldKey candidate : values()) {
            if (candidate.name().equals(wanted)) {
                return candidate;
            }
        }
        return null;
    }

    /** The fifteen fields an administrator may add, in catalogue order. */
    public static List<LabelFieldKey> selectable() {
        return Collections.unmodifiableList(
                Arrays.stream(values()).filter(key -> key != LAB_NUMBER).collect(Collectors.toList()));
    }
}
