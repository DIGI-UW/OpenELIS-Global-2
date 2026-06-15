package org.openelisglobal.labelpreset.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.labelpreset.valueholder.BarcodeType;
import org.openelisglobal.labelpreset.valueholder.LabelPreset;

public interface LabelPresetDAO extends BaseDAO<LabelPreset, Integer> {

    /** All presets with is_active = true. */
    List<LabelPreset> listActive();

    /** All presets of the given barcode symbology. */
    List<LabelPreset> listByBarcodeType(BarcodeType barcodeType);
}
