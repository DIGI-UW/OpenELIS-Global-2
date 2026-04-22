package org.openelisglobal.dataexchange.fhir.service;

import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.TaskStatus;

public interface TaskFhirService {

    Task getTaskByFhirId(String taskFhirId);

    Task updateTaskStatus(String taskFhirId, TaskStatus incomingStatus, String sysUserId);

    Task cancelTask(String taskFhirId, String sysUserId);
}
