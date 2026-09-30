package org.openelisglobal.testresultinterpretation.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.testresultinterpretation.valueholder.TestResultInterpretation;

public interface TestResultInterpretationService extends BaseObjectService<TestResultInterpretation, String> {

    List<TestResultInterpretation> getByComponentId(String componentId);

    List<TestResultInterpretation> getActiveByComponentId(String componentId);

    /**
     * Reconciles a component's active interpretations to the desired set: a desired
     * interpretation whose id matches an existing active row is updated; one
     * without a matching id is inserted; an existing active row absent from the
     * desired set is soft-deleted (is_active='N'). Returns the resulting active
     * list.
     */
    List<TestResultInterpretation> saveInterpretationsForComponent(String componentId,
            List<TestResultInterpretation> desired, String sysUserId);
}
