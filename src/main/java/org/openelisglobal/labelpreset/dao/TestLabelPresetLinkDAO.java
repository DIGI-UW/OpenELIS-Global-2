package org.openelisglobal.labelpreset.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.labelpreset.valueholder.TestLabelPresetLink;

public interface TestLabelPresetLinkDAO extends BaseDAO<TestLabelPresetLink, Integer> {

    /** All preset links for a test. */
    List<TestLabelPresetLink> listByTestId(String testId);

    /** All test links referencing a preset. */
    List<TestLabelPresetLink> listByPresetId(Integer presetId);
}
