package org.openelisglobal.barcode.labeltype;

import java.util.ArrayList;
import org.apache.commons.lang3.StringUtils;
import org.openelisglobal.barcode.LabelField;
import org.openelisglobal.barcode.util.BarcodeConfigUtil;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.internationalization.MessageUtil;

public class FreezerLabel extends Label {

    public FreezerLabel(String freezerCode) {
        this(freezerCode, null, null, null, null, null);
    }

    public FreezerLabel(String freezerCode, String patientId, String storageLocation, String specimenType,
            String collectionDate, String expiryDate) {
        // set dimensions (safe parsing for admin-configured DB values)
        width = BarcodeConfigUtil.parseFloatSafe(
                ConfigurationProperties.getInstance().getPropertyValue(Property.FREEZER_LABEL_BARCODE_WIDTH), 2.0f);
        height = BarcodeConfigUtil.parseFloatSafe(
                ConfigurationProperties.getInstance().getPropertyValue(Property.FREEZER_LABEL_BARCODE_HEIGHT), 2.0f);
        boolean usePatientId = "true".equals(
                ConfigurationProperties.getInstance().getPropertyValue(Property.FREEZER_LABEL_FIELD_PATIENT_ID));
        boolean useStorageLocation = "true".equals(
                ConfigurationProperties.getInstance().getPropertyValue(Property.FREEZER_LABEL_FIELD_STORAGE_LOCATION));
        boolean useSpecimenType = "true".equals(
                ConfigurationProperties.getInstance().getPropertyValue(Property.FREEZER_LABEL_FIELD_SPECIMEN_TYPE));
        boolean useCollectionDate = "true".equals(
                ConfigurationProperties.getInstance().getPropertyValue(Property.FREEZER_LABEL_FIELD_COLLECTION_DATE));
        boolean useExpiryDate = "true".equals(
                ConfigurationProperties.getInstance().getPropertyValue(Property.FREEZER_LABEL_FIELD_EXPIRY_DATE));

        // adding fields above bar code
        aboveFields = new ArrayList<>();
        // adding fields below bar code
        belowFields = new ArrayList<>();

        if (usePatientId) {
            aboveFields.add(new LabelField(MessageUtil.getMessage("barcode.label.info.patientId"),
                    StringUtils.defaultString(patientId), 4));
        }
        if (useStorageLocation) {
            aboveFields.add(new LabelField(MessageUtil.getMessage("barcode.label.info.storageLocation"),
                    StringUtils.defaultString(storageLocation), 4));
        }
        if (useSpecimenType) {
            aboveFields.add(new LabelField(MessageUtil.getMessage("barcode.label.info.specimenType"),
                    StringUtils.defaultString(specimenType), 4));
        }
        if (useCollectionDate) {
            belowFields.add(new LabelField(MessageUtil.getMessage("barcode.label.info.collectionDate"),
                    StringUtils.defaultString(collectionDate), 4));
        }
        if (useExpiryDate) {
            belowFields.add(new LabelField(MessageUtil.getMessage("barcode.label.info.expiryDate"),
                    StringUtils.defaultString(expiryDate), 4));
        }
        // adding bar code
        setCode(freezerCode);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openelisglobal.barcode.labeltype.Label#getNumTextRowsBefore()
     */
    @Override
    public int getNumTextRowsBefore() {
        Iterable<LabelField> fields = getAboveFields();
        return getNumRows(fields);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openelisglobal.barcode.labeltype.Label#getNumTextRowsAfter()
     */
    @Override
    public int getNumTextRowsAfter() {
        Iterable<LabelField> fields = getBelowFields();
        return getNumRows(fields);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openelisglobal.barcode.labeltype.Label#getMaxNumLabels()
     */
    @Override
    public int getMaxNumLabels() {
        return BarcodeConfigUtil.parseIntSafe(
                ConfigurationProperties.getInstance().getPropertyValue(Property.MAX_FREEZER_LABEL_PRINTED), 10);
    }
}
