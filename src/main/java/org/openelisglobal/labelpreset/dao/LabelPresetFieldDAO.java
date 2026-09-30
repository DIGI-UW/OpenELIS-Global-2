package org.openelisglobal.labelpreset.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.labelpreset.valueholder.LabelPresetField;

public interface LabelPresetFieldDAO extends BaseDAO<LabelPresetField, Integer> {

    /** Fields for a preset, ordered by display_order ascending. */
    List<LabelPresetField> listByPresetId(Integer presetId);
}
