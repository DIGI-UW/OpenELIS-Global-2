package org.openelisglobal.fhir.serviceImpl;

import java.util.List;
import java.util.Optional;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.TaskIntent;
import org.hl7.fhir.r4.model.Task.TaskStatus;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.services.StatusService.OrderStatus;
import org.openelisglobal.dataexchange.fhir.FHIRTransformUtil;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.dataexchange.fhir.service.FhirPersistanceService;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrder;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrderType;
import org.openelisglobal.dataexchange.service.order.ElectronicOrderService;
import org.openelisglobal.fhir.service.TaskTransformService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TaskTransformServiceImpl implements TaskTransformService {
    @Autowired
    private FHIRTransformUtil fhirTransformUtil;
    @Autowired
    private SampleHumanService sampleHumanService;
    @Autowired
    private SampleService sampleService;
    @Autowired
    private FhirPersistanceService fhirPersistanceService;
    private IStatusService statusService;
    @Autowired
    private FhirConfig fhirConfig;
    @Autowired
    private ElectronicOrderService electronicOrderService;

    @Override
    public Task transformToTask(Sample sample) {
        LogEvent.logTrace(this.getClass().getSimpleName(), "transformToTask", "transformToTask called");

        Task task = new Task();
        Patient patient = sampleHumanService.getPatientForSample(sample);
        List<Analysis> analysises = sampleService.getAnalysis(sample);
        task.setId(sample.getFhirUuidAsString());
        Optional<Task> referredTask = getReferringTaskForSample(sample);
        if (referredTask.isPresent()) {
            task.addPartOf(fhirTransformUtil.createReferenceFor(referredTask.get()));
            task.setIntent(TaskIntent.ORDER);
        } else {
            task.setIntent(TaskIntent.ORIGINALORDER);
        }
        if (sample.getStatusId().equals(statusService.getStatusID(OrderStatus.Entered))) {
            task.setStatus(TaskStatus.READY);
        } else if (sample.getStatusId().equals(statusService.getStatusID(OrderStatus.Started))
                || sample.getStatusId().equals(statusService.getStatusID(AnalysisStatus.TechnicalAcceptance))) {
            task.setStatus(TaskStatus.INPROGRESS);
        } else if (sample.getStatusId().equals(statusService.getStatusID(AnalysisStatus.TechnicalRejected))) {
            task.setStatus(TaskStatus.FAILED);
        } else if (sample.getStatusId().equals(statusService.getStatusID(OrderStatus.NonConforming_depricated))
                || sample.getStatusId().equals(statusService.getStatusID(AnalysisStatus.BiologistRejected))) {
            task.setStatus(TaskStatus.REJECTED);
        } else if (sample.getStatusId().equals(statusService.getStatusID(OrderStatus.Finished))) {
            task.setStatus(TaskStatus.COMPLETED);
        } else {
            task.setStatus(TaskStatus.NULL);
        }
        task.setAuthoredOn(sample.getEnteredDate());
        task.setPriority(fhirTransformUtil.convertToTaskPriority(sample.getPriority()));
        task.addIdentifier(fhirTransformUtil.createIdentifier(fhirConfig.getOeFhirSystem() + "/order_uuid",
                sample.getFhirUuidAsString()));
        task.addIdentifier(fhirTransformUtil.createIdentifier(fhirConfig.getOeFhirSystem() + "/order_accessionNumber",
                sample.getAccessionNumber()));

        for (Analysis analysis : analysises) {
            task.addBasedOn(
                    fhirTransformUtil.createReferenceFor(ResourceType.ServiceRequest, analysis.getFhirUuidAsString()));
            if (sample.getStatusId().equals(statusService.getStatusID(OrderStatus.Finished))) {
                task.addOutput() //
                        .setType(new CodeableConcept().addCoding(new Coding().setCode("reference"))) //
                        .setValue(fhirTransformUtil.createReferenceFor(ResourceType.DiagnosticReport,
                                analysis.getFhirUuidAsString()));
            }
        }
        // OGC-356: Environmental samples don't have a patient, so only set the patient
        // reference if patient exists
        if (patient != null) {
            task.setFor(fhirTransformUtil.createReferenceFor(ResourceType.Patient, patient.getFhirUuidAsString()));
        }

        return task;
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

}
