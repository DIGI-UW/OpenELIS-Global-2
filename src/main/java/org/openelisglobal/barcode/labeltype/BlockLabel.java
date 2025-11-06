package org.openelisglobal.barcode.labeltype;

import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;

public class BlockLabel extends Label {

    public BlockLabel(String blockNumber) {
        // set dimensions
        try {
            width = Float
                    .parseFloat(ConfigurationProperties.getInstance().getPropertyValue(Property.BLOCK_BARCODE_WIDTH));
            height = Float
                    .parseFloat(ConfigurationProperties.getInstance().getPropertyValue(Property.BLOCK_BARCODE_HEIGHT));
        } catch (Exception e) {
            LogEvent.logError("BlockLabel", "BlockLabel BlockLabel()", e.toString());
        }

        // adding bar code
        setCode(blockNumber);
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
