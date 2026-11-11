package org.openelisglobal.fhir.service;

import java.util.Optional;
import org.hl7.fhir.r4.model.Task;
import org.openelisglobal.sample.valueholder.Sample;

/**
 * OpenELIS Sample (order) to FHIR Task, including referral task linkage.
 */
public interface TaskTransformService {

    void updateReferringTaskWithTaskInfo(Task referringTask, Task task);

    Optional<Task> getReferringTaskForSample(Sample sample);

    Task transformToTask(String sampleId);

    Task transformToTask(Sample sample);
}
