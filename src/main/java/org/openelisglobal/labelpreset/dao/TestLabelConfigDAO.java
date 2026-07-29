package org.openelisglobal.labelpreset.dao;

import java.util.Optional;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.labelpreset.valueholder.TestLabelConfig;

public interface TestLabelConfigDAO extends BaseDAO<TestLabelConfig, Integer> {

    /** The (at most one) config row for a test; empty if none exists yet. */
    Optional<TestLabelConfig> getByTestId(String testId);
}
