package org.openelisglobal.barcode.labeltype;

import java.util.ArrayList;

import org.openelisglobal.barcode.LabelField;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;

public class FreezerLabel extends Label {

    public FreezerLabel(String freezerCode) {
        // set dimensions
        try {
            width = Float
                    .parseFloat(ConfigurationProperties.getInstance()
                            .getPropertyValue(Property.FREEZER_LABEL_BARCODE_WIDTH));
            height = Float
                    .parseFloat(
                            ConfigurationProperties.getInstance()
                                    .getPropertyValue(Property.FREEZER_LABEL_BARCODE_HEIGHT));
        } catch (Exception e) {
            LogEvent.logError("FreezerLabel", "FreezerLabel FreezerLabel()", e.toString());
        }
        boolean usePatientId = "true".equals(ConfigurationProperties.getInstance()
                .getPropertyValue(Property.FREEZER_LABEL_FIELD_PATIENT_ID));
        boolean useStorageLocation = "true".equals(ConfigurationProperties.getInstance()
                .getPropertyValue(Property.FREEZER_LABEL_FIELD_STORAGE_LOCATION));
        boolean useSpecimenType = "true".equals(ConfigurationProperties.getInstance()
                .getPropertyValue(Property.FREEZER_LABEL_FIELD_SPECIMEN_TYPE));
        boolean useCollectionDate = "true".equals(ConfigurationProperties.getInstance()
                .getPropertyValue(Property.FREEZER_LABEL_FIELD_COLLECTION_DATE));
        boolean useExpiryDate = "true".equals(ConfigurationProperties.getInstance()
                .getPropertyValue(Property.FREEZER_LABEL_FIELD_EXPIRY_DATE));

        // adding fields above bar code
        aboveFields = new ArrayList<>();
        // adding fields below bar code
        belowFields = new ArrayList<>();
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
        int max = 0;
        max = Integer.parseInt(ConfigurationProperties.getInstance()
                .getPropertyValue(Property.MAX_FREEZER_LABEL_PRINTED));
        return max;
    }
}
