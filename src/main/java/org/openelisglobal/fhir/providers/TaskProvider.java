package org.openelisglobal.fhir.providers;

import ca.uhn.fhir.rest.annotation.Delete;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.annotation.Update;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.TaskStatus;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.OrderStatus;
import org.openelisglobal.dataexchange.fhir.service.FhirPersistanceService;
import org.openelisglobal.dataexchange.fhir.service.FhirTransformService;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * FHIR R4 Resource Provider for Task resources.
 *
 * Task maps to the Sample entity in OpenELIS and represents the lab order
 * workflow state.
 *
 * Status mapping (OpenELIS OrderStatus to FHIR TaskStatus): Entered -> READY
 * Started -> INPROGRESS Finished -> COMPLETED NonConforming -> REJECTED
 * TechnicalRejected -> FAILED
 *
 * Supported operations: READ: GET /fhir/Task/{uuid} UPDATE: PUT
 * /fhir/Task/{uuid} - status transitions only DELETE: DELETE /fhir/Task/{uuid}
 * - soft delete SEARCH: GET /fhir/Task - queries OpenELIS DB directly
 *
 * Auto-discovered by FhirRestfulServer as a Spring Component.
 */
@Component
public class TaskProvider implements IResourceProvider {

    @Autowired
    private FhirTransformService fhirTransformService;

    @Autowired
    private FhirPersistanceService fhirPersistenceService;

    @Autowired
    private SampleService sampleService;

    @Autowired
    private IStatusService statusService;

    @Override
    public Class<? extends IBaseResource> getResourceType() {
        return Task.class;
    }

    @Read
    public Task readTask(@IdParam IdType theId) {
        String method = "readTask";
        try {
            FhirProviderUtils.validateIdParam(theId, "Task", this.getClass().getSimpleName(), method);

            Sample sample = sampleService.getSampleByFhirUuid(theId.getIdPart());
            if (sample == null) {
                throw new ResourceNotFoundException("Task/" + theId.getIdPart());
            }

            Task task = fhirTransformService.transformToTask(sample.getId());
            if (task == null) {
                throw new InternalErrorException("Failed to transform Sample to Task: " + theId.getIdPart());
            }
            return task;

        } catch (ResourceNotFoundException | InvalidRequestException e) {
            throw e;
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), method,
                    "Unexpected error reading Task: " + e.getMessage());
            throw new InternalErrorException("Unexpected server error reading Task", e);
        }
    }

    @Update
    public MethodOutcome updateTask(@IdParam IdType theId, @ResourceParam Task fhirTask, HttpServletRequest request) {
        String method = "updateTask";
        try {
            FhirProviderUtils.validateIdParam(theId, "Task", this.getClass().getSimpleName(), method);

            if (fhirTask == null) {
                throw new InvalidRequestException("Task resource body is required");
            }

            Sample sample = sampleService.getSampleByFhirUuid(theId.getIdPart());
            if (sample == null) {
                throw new ResourceNotFoundException("Task/" + theId.getIdPart());
            }

            TaskStatus incomingStatus = fhirTask.getStatus();
            if (incomingStatus == null) {
                throw new InvalidRequestException("Task.status is required for update");
            }

            TaskStatus currentStatus = fhirTransformService.transformToTask(sample.getId()).getStatus();

            if (!isValidTransition(currentStatus, incomingStatus)) {
                throw new InvalidRequestException("Invalid status transition from " + currentStatus.toCode() + " to "
                        + incomingStatus.toCode() + ". Task workflow is forward-only.");
            }

            String newStatusId = mapTaskStatusToOrderStatus(incomingStatus);
            if (newStatusId == null) {
                throw new InvalidRequestException("Unsupported Task status: " + incomingStatus.toCode());
            }

            sample.setStatusId(newStatusId);
            sample.setSysUserId(FhirProviderUtils.getSysUserId(request));
            sampleService.save(sample);

            Task updatedTask = fhirTransformService.transformToTask(sample.getId());

            FhirProviderUtils.syncToFhirStore(fhirPersistenceService, updatedTask, this.getClass().getSimpleName(),
                    method);

            LogEvent.logInfo(this.getClass().getSimpleName(), method,
                    "Successfully updated Task: " + theId.getIdPart());

            return FhirProviderUtils.buildUpdateOutcome(updatedTask);

        } catch (ResourceNotFoundException | InvalidRequestException e) {
            throw e;
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), method,
                    "Unexpected error updating Task: " + e.getMessage());
            throw new InternalErrorException("Unexpected server error updating Task", e);
        }
    }

    @Delete
    public MethodOutcome deleteTask(@IdParam IdType theId, HttpServletRequest request) {
        String method = "deleteTask";
        try {
            FhirProviderUtils.validateIdParam(theId, "Task", this.getClass().getSimpleName(), method);

            Sample sample = sampleService.getSampleByFhirUuid(theId.getIdPart());
            if (sample == null) {
                throw new ResourceNotFoundException("Task/" + theId.getIdPart());
            }

            sample.setSysUserId(FhirProviderUtils.getSysUserId(request));

            Task cancelledTask = fhirTransformService.transformToTask(sample.getId());
            cancelledTask.setStatus(TaskStatus.CANCELLED);
            FhirProviderUtils.syncToFhirStore(fhirPersistenceService, cancelledTask, this.getClass().getSimpleName(),
                    method);

            LogEvent.logInfo(this.getClass().getSimpleName(), method,
                    "Successfully deleted Task: " + theId.getIdPart());

            return FhirProviderUtils.buildDeleteOutcome(theId, "Task");

        } catch (ResourceNotFoundException | InvalidRequestException e) {
            throw e;
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), method,
                    "Unexpected error deleting Task: " + e.getMessage());
            throw new InternalErrorException("Unexpected server error deleting Task", e);
        }
    }

    @Search
    public Bundle searchTasks(@OptionalParam(name = Task.SP_STATUS) TokenAndListParam status,
            @OptionalParam(name = "_lastUpdated") DateRangeParam lastUpdated, HttpServletRequest request) {
        String method = "searchTasks";
        try {

            List<Sample> samples = sampleService.getAll();

            Bundle bundle = new Bundle();
            bundle.setType(Bundle.BundleType.SEARCHSET);

            for (Sample sample : samples) {
                try {
                    Task task = fhirTransformService.transformToTask(sample.getId());
                    if (task != null) {
                        bundle.addEntry().setResource(task);
                    }
                } catch (Exception taskEx) {
                    // Skip samples that cannot be transformed
                    // (e.g. no patient linked) — log and continue
                    LogEvent.logWarn(this.getClass().getSimpleName(), "searchTasks",
                            "Skipping sample " + sample.getId() + ": " + taskEx.getMessage());
                }
            }

            bundle.setTotal(bundle.getEntry().size());
            return bundle;

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), method, "Error searching Tasks: " + e.getMessage());
            throw new InternalErrorException("Unexpected server error searching Tasks");
        }
    }

    private boolean isValidTransition(TaskStatus current, TaskStatus incoming) {
        if (current == null || incoming == null)
            return false;
        if (current == incoming)
            return true;

        if (current == TaskStatus.COMPLETED || current == TaskStatus.REJECTED || current == TaskStatus.FAILED) {
            return false;
        }

        if (current == TaskStatus.READY) {
            return incoming == TaskStatus.INPROGRESS || incoming == TaskStatus.REJECTED
                    || incoming == TaskStatus.FAILED;
        }

        if (current == TaskStatus.INPROGRESS) {
            return incoming == TaskStatus.COMPLETED || incoming == TaskStatus.REJECTED || incoming == TaskStatus.FAILED;
        }

        return false;
    }

    private String mapTaskStatusToOrderStatus(TaskStatus status) {
        if (status == null)
            return null;
        switch (status) {
        case READY:
            return statusService.getStatusID(OrderStatus.Entered);
        case INPROGRESS:
            return statusService.getStatusID(OrderStatus.Started);
        case COMPLETED:
            return statusService.getStatusID(OrderStatus.Finished);
        case REJECTED:
            return statusService.getStatusID(OrderStatus.NonConforming_depricated);
        default:
            return null;
        }
    }
}
