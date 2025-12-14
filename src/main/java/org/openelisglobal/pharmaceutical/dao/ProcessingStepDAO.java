package org.openelisglobal.pharmaceutical.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.pharmaceutical.valueholder.ProcessingStep;

public interface ProcessingStepDAO extends BaseDAO<ProcessingStep, Integer> {

    List<ProcessingStep> findBySampleId(Integer sampleId);

    List<ProcessingStep> findByStepType(ProcessingStep.StepType stepType);
}
