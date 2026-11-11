package org.openelisglobal.testresultinterpretation.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.testresultinterpretation.valueholder.TestResultInterpretation;

public interface TestResultInterpretationDAO extends BaseDAO<TestResultInterpretation, String> {

    /** All interpretations for a component (active + inactive), ordered. */
    List<TestResultInterpretation> getByComponentId(String componentId);

    /** Active interpretations only, ordered by display order. */
    List<TestResultInterpretation> getActiveByComponentId(String componentId);
}
