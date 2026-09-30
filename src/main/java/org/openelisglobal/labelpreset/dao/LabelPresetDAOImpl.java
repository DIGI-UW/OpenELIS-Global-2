package org.openelisglobal.labelpreset.dao;

import java.util.List;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.labelpreset.valueholder.BarcodeType;
import org.openelisglobal.labelpreset.valueholder.LabelPreset;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class LabelPresetDAOImpl extends BaseDAOImpl<LabelPreset, Integer> implements LabelPresetDAO {

    public LabelPresetDAOImpl() {
        super(LabelPreset.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LabelPreset> listActive() {
        return getAllMatching("isActive", true);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LabelPreset> listByBarcodeType(BarcodeType barcodeType) {
        return getAllMatching("barcodeType", barcodeType);
    }
}
