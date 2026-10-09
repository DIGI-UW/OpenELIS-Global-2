package org.openelisglobal.fhir.service;

import java.util.List;
import java.util.Optional;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.TaskIntent;
import org.hl7.fhir.r4.model.Task.TaskPriority;
import org.hl7.fhir.r4.model.Task.TaskStatus;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.services.StatusService.OrderStatus;
import org.openelisglobal.common.services.StatusService.SampleStatus;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.dataexchange.fhir.service.FhirPersistanceService;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrder;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrderType;
import org.openelisglobal.dataexchange.service.order.ElectronicOrderService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.OrderPriority;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TaskTransformServiceImpl implements TaskTransformService {

    @Autowired
    private FhirConfig fhirConfig;
    @Autowired
    private ElectronicOrderService electronicOrderService;
    @Autowired
    private SampleService sampleService;
    @Autowired
    private SampleHumanService sampleHumanService;
    @Autowired
    private FhirPersistanceService fhirPersistanceService;
    @Autowired
    private IStatusService statusService;
    @Autowired
    private FhirCommonTransformService common;

    @Override
    public void updateReferringTaskWithTaskInfo(Task referringTask, Task task) {
        LogEvent.logTrace(this.getClass().getSimpleName(), "updateReferringTaskWithTaskInfo",
                "updateReferringTaskWithTaskInfo called");

        if (TaskStatus.COMPLETED.equals(task.getStatus())) {
            referringTask.setStatus(TaskStatus.COMPLETED);
            task.getOutput().forEach(outPut -> {
                referringTask.addOutput(outPut);
            });
        }
    }

    @Override
    public Optional<Task> getReferringTaskForSample(Sample sample) {
        LogEvent.logTrace(this.getClass().getSimpleName(), "getReferringTaskForSample",
                "getReferringTaskForSample called");

        List<ElectronicOrder> eOrders = electronicOrderService.getElectronicOrdersByExternalId(sample.getReferringId());
        if (eOrders.size() > 0 && ElectronicOrderType.FHIR.equals(eOrders.get(0).getType())) {
            return fhirPersistanceService.getTaskBasedOnServiceRequest(sample.getReferringId());
        }
        return Optional.empty();
    }

    @Override
    public Task transformToTask(String sampleId) {
        return this.transformToTask(sampleService.get(sampleId));
    }

    @Override
    public Task transformToTask(Sample sample) {
        LogEvent.logTrace(this.getClass().getSimpleName(), "transformToTask", "transformToTask called");

        Task task = new Task();
        Patient patient = sampleHumanService.getPatientForSample(sample);
        List<Analysis> analysises = sampleService.getAnalysis(sample);
        task.setId(sample.getFhirUuidAsString());
        Optional<Task> referredTask = getReferringTaskForSample(sample);
        if (referredTask.isPresent()) {
            task.addPartOf(common.createReferenceFor(referredTask.get()));
            task.setIntent(TaskIntent.ORDER);
        } else {
            task.setIntent(TaskIntent.ORIGINALORDER);
        }
        task.setStatus(taskStatusFor(sample));
        task.setAuthoredOn(sample.getEnteredDate());
        task.setPriority(convertToTaskPriority(sample.getPriority()));
        task.addIdentifier(
                common.createIdentifier(fhirConfig.getOeFhirSystem() + "/order_uuid", sample.getFhirUuidAsString()));
        task.addIdentifier(common.createIdentifier(fhirConfig.getOeFhirSystem() + "/order_accessionNumber",
                sample.getAccessionNumber()));

        for (Analysis analysis : analysises) {
            task.addBasedOn(common.createReferenceFor(ResourceType.ServiceRequest, analysis.getFhirUuidAsString()));
            if (TaskStatus.COMPLETED.equals(task.getStatus())) {
                task.addOutput() //
                        .setType(new CodeableConcept().addCoding(new Coding()
                                .setSystem(fhirConfig.getOeFhirSystem() + "/task_output").setCode("DiagnosticReport"))) //
                        .setValue(common.createReferenceFor(ResourceType.DiagnosticReport,
                                analysis.getFhirUuidAsString()));
            }
        }
        // OGC-356: Environmental samples don't have a patient, so only set the patient
        // reference if patient exists
        if (patient != null) {
            task.setFor(common.createReferenceFor(ResourceType.Patient, patient.getFhirUuidAsString()));
        }

        return task;
    }

    /**
     * R4 requires a Task status. A status with no counterpart is sent as
     * in-progress and logged, so a new order status cannot publish a Task without
     * one.
     */
    private TaskStatus taskStatusFor(Sample sample) {
        String statusId = sample.getStatusId();
        if (statusId != null) {
            if (statusId.equals(statusService.getStatusID(OrderStatus.Entered))
                    || statusId.equals(statusService.getStatusID(SampleStatus.Entered))) {
                return TaskStatus.READY;
            }
            if (statusId.equals(statusService.getStatusID(OrderStatus.Started))
                    || statusId.equals(statusService.getStatusID(AnalysisStatus.TechnicalAcceptance))) {
                return TaskStatus.INPROGRESS;
            }
            if (statusId.equals(statusService.getStatusID(AnalysisStatus.TechnicalRejected))) {
                return TaskStatus.FAILED;
            }
            if (statusId.equals(statusService.getStatusID(OrderStatus.NonConforming_depricated))
                    || statusId.equals(statusService.getStatusID(AnalysisStatus.BiologistRejected))
                    || statusId.equals(statusService.getStatusID(SampleStatus.SampleRejected))) {
                return TaskStatus.REJECTED;
            }
            if (statusId.equals(statusService.getStatusID(SampleStatus.Canceled))) {
                return TaskStatus.CANCELLED;
            }
            if (statusId.equals(statusService.getStatusID(OrderStatus.Finished))) {
                return TaskStatus.COMPLETED;
            }
        }
        LogEvent.logWarn(this.getClass().getSimpleName(), "taskStatusFor", "order " + sample.getAccessionNumber()
                + " has status " + statusId + ", which has no Task status; sending in-progress");
        return TaskStatus.INPROGRESS;
    }

    private TaskPriority convertToTaskPriority(OrderPriority orderPriority) {
        if (orderPriority == null) {
            return TaskPriority.ROUTINE;
        }
        switch (orderPriority) {
        case ROUTINE:
            return TaskPriority.ROUTINE;
        case ASAP:
            return TaskPriority.ASAP;
        case STAT:
        case FUTURE_STAT:
            return TaskPriority.STAT;
        case TIMED:
            return TaskPriority.URGENT;
        default:
            return TaskPriority.ROUTINE;
        }
    }
}
