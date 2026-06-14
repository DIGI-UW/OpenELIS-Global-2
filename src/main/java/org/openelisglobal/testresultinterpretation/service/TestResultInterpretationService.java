package org.openelisglobal.testresultinterpretation.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.testresultinterpretation.valueholder.TestResultInterpretation;

public interface TestResultInterpretationService extends BaseObjectService<TestResultInterpretation, String> {

    List<TestResultInterpretation> getByComponentId(String componentId);

    List<TestResultInterpretation> getActiveByComponentId(String componentId);
}
