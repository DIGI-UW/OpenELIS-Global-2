package org.openelisglobal.barcode.labeltype;

import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;

public class SlideLabel extends Label {

    public SlideLabel(String slideNumber) {
        // set dimensions
        try {
            width = Float
                    .parseFloat(ConfigurationProperties.getInstance().getPropertyValue(Property.SLIDE_BARCODE_WIDTH));
            height = Float
                    .parseFloat(ConfigurationProperties.getInstance().getPropertyValue(Property.SLIDE_BARCODE_HEIGHT));
        } catch (Exception e) {
            LogEvent.logError("SlideLabel", "SlideLabel SlideLabel()", e.toString());
        }

        // adding bar code
        setCode(slideNumber);
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
