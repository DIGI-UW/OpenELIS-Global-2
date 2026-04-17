package org.openelisglobal.dataexchange.fhir.service;

import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.TaskStatus;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.OrderStatus;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskFhirServiceImpl implements TaskFhirService {

    @Autowired
    private SampleService sampleService;
    @Autowired
    private FhirTransformService fhirTransformService;
    @Autowired
    private FhirPersistanceService fhirPersistenceService;
    @Autowired
    private IStatusService statusService;

    @Override
    @Transactional(readOnly = true)
    public Task getTaskByFhirId(String taskFhirId) {
        Sample sample = sampleService.getSampleByFhirUuid(taskFhirId);
        if (sample == null) {
            throw new ResourceNotFoundException("Task/" + taskFhirId);
        }

        Task task = fhirTransformService.transformToTask(sample.getId());
        if (task == null) {
            throw new InternalErrorException("Transformation failed for Task/" + taskFhirId);
        }

        return task;
    }

    @Override
    @Transactional
    public Task updateTaskStatus(String taskFhirId, TaskStatus incomingStatus, String sysUserId) {
        if (incomingStatus == null) {
            throw new InvalidRequestException("Task.status is required");
        }

        Sample sample = getSampleOrThrow(taskFhirId);
        Task currentTask = getTaskOrThrow(sample, taskFhirId);
        TaskStatus currentStatus = currentTask.getStatus();

        if (!isValidTransition(currentStatus, incomingStatus)) {
            throw new InvalidRequestException(
                    "Invalid status transition: " + currentStatus.toCode() + " → " + incomingStatus.toCode());
        }

        String newStatusId = mapTaskStatusToOrderStatusId(incomingStatus);
        if (newStatusId == null) {
            throw new InvalidRequestException("Unsupported Task status: " + incomingStatus.toCode());
        }

        sample.setStatusId(newStatusId);
        sample.setSysUserId(sysUserId);
        sampleService.update(sample);

        Task updatedTask = getTaskOrThrow(sample, taskFhirId);
        syncToFhirStore(updatedTask, "updateTaskStatus");
        return updatedTask;
    }

    @Override
    @Transactional
    public Task cancelTask(String taskFhirId, String sysUserId) {
        Sample sample = getSampleOrThrow(taskFhirId);
        Task currentTask = getTaskOrThrow(sample, taskFhirId);

        if (isTerminal(currentTask.getStatus())) {
            throw new InvalidRequestException(
                    "Cannot cancel a task in terminal state: " + currentTask.getStatus().toCode());
        }

        String cancelStatusId = mapTaskStatusToOrderStatusId(TaskStatus.CANCELLED);
        sample.setStatusId(cancelStatusId);
        sample.setSysUserId(sysUserId);
        sampleService.update(sample);

        Task cancelledTask = getTaskOrThrow(sample, taskFhirId);
        syncToFhirStore(cancelledTask, "cancelTask");
        return cancelledTask;
    }

    private Sample getSampleOrThrow(String taskFhirId) {
        Sample sample = sampleService.getSampleByFhirUuid(taskFhirId);
        if (sample == null) {
            throw new ResourceNotFoundException("Task/" + taskFhirId);
        }
        return sample;
    }

    private Task getTaskOrThrow(Sample sample, String taskFhirId) {
        Task task = fhirTransformService.transformToTask(sample.getId());
        if (task == null) {
            throw new InternalErrorException("Transformation failed for Task/" + taskFhirId);
        }
        return task;
    }

    private void syncToFhirStore(Task task, String method) {
        try {
            fhirPersistenceService.updateFhirResourceInFhirStore(task);
        } catch (Exception syncEx) {
            LogEvent.logError(getClass().getSimpleName(), method,
                    "FHIR store sync failed (continuing anyway): " + syncEx.getMessage());
        }
    }

    private boolean isValidTransition(TaskStatus current, TaskStatus incoming) {
        if (current == null || incoming == null) {
            return false;
        }
        if (current == incoming) {
            return true;
        }
        if (isTerminal(current)) {
            return false;
        }
        if (current == TaskStatus.READY) {
            return incoming == TaskStatus.INPROGRESS || incoming == TaskStatus.REJECTED || incoming == TaskStatus.FAILED
                    || incoming == TaskStatus.CANCELLED;
        }
        if (current == TaskStatus.INPROGRESS) {
            return incoming == TaskStatus.COMPLETED || incoming == TaskStatus.REJECTED || incoming == TaskStatus.FAILED
                    || incoming == TaskStatus.CANCELLED;
        }
        return false;
    }

    private boolean isTerminal(TaskStatus status) {
        return status == TaskStatus.COMPLETED || status == TaskStatus.REJECTED || status == TaskStatus.FAILED
                || status == TaskStatus.CANCELLED;
    }

    private String mapTaskStatusToOrderStatusId(TaskStatus status) {
        switch (status) {
        case READY:
            return statusService.getStatusID(OrderStatus.Entered);
        case INPROGRESS:
            return statusService.getStatusID(OrderStatus.Started);
        case COMPLETED:
            return statusService.getStatusID(OrderStatus.Finished);
        case REJECTED:
        case CANCELLED:
        case FAILED:
            return statusService.getStatusID(OrderStatus.NonConforming_depricated);
        default:
            return null;
        }
    }
}
