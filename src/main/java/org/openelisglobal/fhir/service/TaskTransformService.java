package org.openelisglobal.fhir.service;

import java.util.Optional;
import org.hl7.fhir.r4.model.Task;
import org.openelisglobal.common.service.CrossDomainService;
import org.openelisglobal.sample.valueholder.Sample;

/**
 * OpenELIS Sample (order) to FHIR Task, including referral task linkage.
 */
@CrossDomainService(callers = "FHIR transform pipeline — one of the per-resource transformers"
        + " FhirTransformService (itself @CrossDomainService) was decomposed into. Pure resource"
        + " mapping invoked by the import/export pipeline and by sibling transformers; no controller"
        + " references it. The caller's own endpoint carries the privilege gate.")
public interface TaskTransformService {

    void updateReferringTaskWithTaskInfo(Task referringTask, Task task);

    Optional<Task> getReferringTaskForSample(Sample sample);

    Task transformToTask(String sampleId);

    Task transformToTask(Sample sample);
}
