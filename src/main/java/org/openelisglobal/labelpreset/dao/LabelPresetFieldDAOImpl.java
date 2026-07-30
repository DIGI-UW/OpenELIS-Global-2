package org.openelisglobal.labelpreset.dao;

import java.util.List;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.labelpreset.valueholder.LabelPresetField;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class LabelPresetFieldDAOImpl extends BaseDAOImpl<LabelPresetField, Integer> implements LabelPresetFieldDAO {

    public LabelPresetFieldDAOImpl() {
        super(LabelPresetField.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LabelPresetField> listByPresetId(Integer presetId) {
        return entityManager
                .createQuery("FROM LabelPresetField f WHERE f.preset.id = :presetId ORDER BY f.displayOrder ASC",
                        LabelPresetField.class)
                .setParameter("presetId", presetId).getResultList();
    }
}
