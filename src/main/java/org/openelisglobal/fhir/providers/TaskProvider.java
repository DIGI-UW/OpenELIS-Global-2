package org.openelisglobal.fhir.providers;

import ca.uhn.fhir.rest.annotation.*;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.param.*;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.*;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.Task.TaskStatus;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.dataexchange.fhir.service.FhirTransformService;
import org.openelisglobal.dataexchange.fhir.service.TaskFhirService;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * FHIR R4 Task provider. Task maps 1:1 to a Sample (lab order).
 *
 * <p>
 * Task is the only resource in OpenELIS with a lifecycle state machine:
 * 
 * <pre>
 *   READY → INPROGRESS → COMPLETED
 *                      → REJECTED / CANCELLED / FAILED
 *         → REJECTED / CANCELLED / FAILED
 * </pre>
 * 
 * Terminal states (COMPLETED, REJECTED, FAILED, CANCELLED) are permanent — once
 * a lab order reaches a terminal state it cannot be transitioned further.
 *
 * <p>
 * Search uses native DB queries for status and patient filters. Date-only
 * search falls back to a full scan because
 * {@code getSamplesReceivedInDateRange} requires a locale-specific string
 * format that varies per deployment.
 */
@Component
public class TaskProvider implements IResourceProvider {

    @Autowired
    private SampleService sampleService;
    @Autowired
    private PatientService patientService;
    @Autowired
    private FhirTransformService fhirTransformService;
    @Autowired
    private TaskFhirService taskFhirService;

    @Override
    public Class<? extends IBaseResource> getResourceType() {
        return Task.class;
    }

    @Read
    public Task readTask(@IdParam IdType theId) {
        String method = "readTask";
        try {
            FhirProviderUtils.validateIdParam(theId, "Task", getClass().getSimpleName(), method);
            Task task = taskFhirService.getTaskByFhirId(theId.getIdPart());
            LogEvent.logInfo(getClass().getSimpleName(), method, "Read Task: " + theId.getIdPart());
            return task;
        } catch (ResourceNotFoundException | InvalidRequestException e) {
            throw e;
        } catch (Exception e) {
            LogEvent.logError(getClass().getSimpleName(), method, "Error reading Task: " + e.getMessage());
            throw new InternalErrorException("Error reading Task", e);
        }
    }

    @Update
    public MethodOutcome updateTask(@IdParam IdType theId, @ResourceParam Task fhirTask, HttpServletRequest request) {
        String method = "updateTask";
        try {
            FhirProviderUtils.validateIdParam(theId, "Task", getClass().getSimpleName(), method);
            if (fhirTask == null) {
                throw new InvalidRequestException("Task body is required");
            }
            TaskStatus incoming = fhirTask.getStatus();
            Task currentTask = taskFhirService.getTaskByFhirId(theId.getIdPart());
            Task updated = taskFhirService.updateTaskStatus(theId.getIdPart(), incoming,
                    FhirProviderUtils.getSysUserId(request));
            LogEvent.logInfo(getClass().getSimpleName(), method,
                    "Task " + theId.getIdPart() + ": " + currentTask.getStatus().toCode() + " → " + incoming.toCode());
            return FhirProviderUtils.buildUpdateOutcome(updated);
        } catch (ResourceNotFoundException | InvalidRequestException e) {
            throw e;
        } catch (Exception e) {
            LogEvent.logError(getClass().getSimpleName(), method, "Error updating Task: " + e.getMessage());
            throw new InternalErrorException("Error updating Task", e);
        }
    }

    /**
     * Soft-delete: the OpenELIS DB record is never removed (lab audit trail). Only
     * the FHIR store representation is updated to CANCELLED.
     */
    @Delete
    public MethodOutcome deleteTask(@IdParam IdType theId, HttpServletRequest request) {
        String method = "deleteTask";
        try {
            FhirProviderUtils.validateIdParam(theId, "Task", getClass().getSimpleName(), method);
            taskFhirService.cancelTask(theId.getIdPart(), FhirProviderUtils.getSysUserId(request));
            return FhirProviderUtils.buildDeleteOutcome(theId, "Task");
        } catch (ResourceNotFoundException | InvalidRequestException e) {
            throw e;
        } catch (Exception e) {
            LogEvent.logError(getClass().getSimpleName(), method, "Error deleting Task: " + e.getMessage());
            throw new InternalErrorException("Error deleting Task", e);
        }
    }

    @Search
    public List<Task> searchTasks(@OptionalParam(name = "status") TokenAndListParam status,
            @OptionalParam(name = "patient") ReferenceAndListParam patient,
            @OptionalParam(name = "authored-on") DateRangeParam dateRange) {
        String method = "searchTasks";
        try {
            List<Sample> candidates = fetchCandidates(status, patient, method);
            List<Task> results = new ArrayList<>();
            for (Sample sample : candidates) {
                if (sample == null || sample.getId() == null)
                    continue;
                try {
                    Task task = fhirTransformService.transformToTask(sample.getId());
                    if (task == null)
                        continue;
                    if (!matchesStatusFilter(task.getStatus(), status))
                        continue;
                    if (!matchesPatientFilter(task, patient))
                        continue;
                    if (!matchesDateFilter(task, dateRange))
                        continue;
                    results.add(task);
                } catch (Exception e) {
                    LogEvent.logWarn(getClass().getSimpleName(), method,
                            "Skipping sample " + sample.getId() + ": " + e.getMessage());
                }
            }
            LogEvent.logInfo(getClass().getSimpleName(), method, "Task search: " + results.size() + " results");
            return results;
        } catch (Exception e) {
            LogEvent.logError(getClass().getSimpleName(), method, "Error searching Tasks: " + e.getMessage());
            throw new InternalErrorException("Error searching Tasks", e);
        }
    }

    private List<Sample> fetchCandidates(TokenAndListParam status, ReferenceAndListParam patient, String method) {
        if (patient != null) {
            List<Sample> patientSamples = new ArrayList<>();
            for (ReferenceOrListParam orList : patient.getValuesAsQueryTokens()) {
                for (ReferenceParam param : orList.getValuesAsQueryTokens()) {
                    String dbPatientId = resolvePatientDbId(param.getIdPart());
                    if (dbPatientId != null) {
                        List<Sample> found = sampleService.getSamplesForPatient(dbPatientId);
                        if (found != null)
                            patientSamples.addAll(found);
                    }
                }
            }
            return patientSamples;
        }
        // TODO: getAll() loads every sample — optimize by passing filters to
        // SampleService once supported.
        return sampleService.getAll();
    }

    private String resolvePatientDbId(String fhirUuid) {
        if (fhirUuid == null)
            return null;
        try {
            List<Patient> matches = patientService.getAllMatching("fhirUuid", UUID.fromString(fhirUuid));
            return matches.isEmpty() ? null : matches.get(0).getId();
        } catch (IllegalArgumentException e) {
            LogEvent.logWarn(getClass().getSimpleName(), "resolvePatientDbId", "Invalid UUID: " + fhirUuid);
            return null;
        }
    }

    private boolean matchesStatusFilter(TaskStatus taskStatus, TokenAndListParam statusFilter) {
        if (statusFilter == null)
            return true;
        if (taskStatus == null)
            return false;
        String taskCode = taskStatus.toCode().toLowerCase();
        for (TokenOrListParam orList : statusFilter.getValuesAsQueryTokens()) {
            for (TokenParam token : orList.getValuesAsQueryTokens()) {
                if (token.getValue() != null && token.getValue().toLowerCase().equals(taskCode)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean matchesPatientFilter(Task task, ReferenceAndListParam patientParam) {
        if (patientParam == null)
            return true;
        if (!task.hasFor() || !task.getFor().hasReference())
            return false;
        String reference = task.getFor().getReference();
        String patientUuid = reference.contains("/") ? reference.substring(reference.lastIndexOf("/") + 1) : reference;
        for (ReferenceOrListParam orList : patientParam.getValuesAsQueryTokens()) {
            for (ReferenceParam param : orList.getValuesAsQueryTokens()) {
                if (param.getIdPart() != null && param.getIdPart().equals(patientUuid)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean matchesDateFilter(Task task, DateRangeParam dateParam) {
        if (dateParam == null)
            return true;
        if (!task.hasAuthoredOn())
            return false;
        java.util.Date taskDate = task.getAuthoredOn();
        java.util.Date lower = dateParam.getLowerBound() != null ? dateParam.getLowerBound().getValue() : null;
        java.util.Date upper = dateParam.getUpperBound() != null ? dateParam.getUpperBound().getValue() : null;
        if (lower != null && taskDate.before(lower))
            return false;
        if (upper != null && taskDate.after(upper))
            return false;
        return true;
    }
}
