package org.openelisglobal.barcode.labeltype;

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

        // adding bar code
        setCode(freezerCode);
    }

    @Override
    public int getNumTextRowsBefore() {
        return 0;
    }

    @Override
    public int getNumTextRowsAfter() {
        return 0;
    }

    @Override
    public int getMaxNumLabels() {
        return -1;
    }
}
