package org.openelisglobal.fhir.transormation.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.hl7.fhir.r4.model.Bundle;
import org.itech.fhir.dataexport.api.service.DataExportService;
import org.itech.fhir.dataexport.core.model.DataExportTask;
import org.itech.fhir.dataexport.core.service.DataExportTaskService;
import org.openelisglobal.common.controller.BaseController;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.dataexchange.fhir.exception.FhirLocalPersistingException;
import org.openelisglobal.dataexchange.fhir.exception.FhirPersistanceException;
import org.openelisglobal.dataexchange.fhir.service.FhirTransformService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FhirTransformationController extends BaseController {
    @Autowired
    private SampleService sampleService;
    @Autowired
    private SampleHumanService sampleHumanService;
    @Autowired
    private FhirTransformService fhirTransformService;

    @Autowired
    private DataExportService dataExportService;
    @Autowired
    private DataExportTaskService dataExportTaskService;

    // global variable for tracking state as only one process can be run at a time
    private TransformationInfo info;

    @Scheduled(initialDelay = 10 * 1000, fixedRate = Long.MAX_VALUE)
    private void transformOEObjectsOnBoot() throws FhirLocalPersistingException, IOException {
        transformPersistMissingFhirObjects(false, 100, 1, true);
    }

    @GetMapping("/OEToFhir/info")
    public TransformationInfo getTransformationInfo() throws FhirLocalPersistingException, IOException {
        return info;
    }

    @GetMapping("/PatientToFhir")
    public TransformationInfo transformPersistFhirPatients(@RequestParam(defaultValue = "false") Boolean checkAll,
            @RequestParam(defaultValue = "100") int batchSize, @RequestParam(defaultValue = "1") int threads,
            @RequestParam(defaultValue = "true") boolean waitForResults)
            throws FhirLocalPersistingException, IOException {
        if (inProcess()) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "transformPersistFhirPatients",
                    "processs already running");
            return info;
        }
        info.checkAll = checkAll;
        info.batchSize = batchSize;
        info.threads = threads;
        info.waitForResults = waitForResults;
        info.objectType = "Patient";
        info.phase = "Fetching";

        List<Patient> patients;
        if (info.checkAll) {
            patients = sampleHumanService.getAllPatientsWithSampleEntered();
        } else {
            patients = sampleHumanService.getAllPatientsWithSampleEnteredMissingFhirUuid();
        }
        LogEvent.logDebug(this.getClass().getSimpleName(), "transformPersistFhirPatients",
                "patients to convert: " + patients.size());
        List<String> patientIds = new ArrayList<>();
        List<Future<Bundle>> promises = new ArrayList<>();
        info.objectType = "Patient";
        for (int i = 0; i < patients.size(); ++i) {
            info.phase = "Batch Transforming";
            patientIds.add(patients.get(i).getId());
            if (i % info.batchSize == info.batchSize - 1 || i + 1 == patients.size()) {
                LogEvent.logDebug(this.getClass().getSimpleName(), "",
                        "persisting batch " + (i - info.batchSize + 1) + "-" + i + " of " + patients.size());
                try {
                    promises.add(fhirTransformService.transformPersistPatients(patientIds));
                    ++info.batches;
                    patientIds = new ArrayList<>();
                    if (info.waitForResults && (promises.size() >= info.threads || i + 1 == patients.size())) {
                        waitForResults(promises);
                        promises = new ArrayList<>();
                    }
                } catch (FhirPersistanceException e) {
                    ++info.batchFailure;
                    LogEvent.logError(e);
                    LogEvent.logError(this.getClass().getSimpleName(), "transformPersistFhirPatients",
                            "error persisting batch " + (i - info.batchSize + 1) + "-" + i);
                } catch (Exception e) {
                    ++info.batchFailure;
                    LogEvent.logError(e);
                    LogEvent.logError(this.getClass().getSimpleName(), "transformPersistFhirPatients",
                            "error with batch " + (i - info.batchSize + 1) + "-" + i);
                } finally {
                    if (promises.size() >= info.threads || i + 1 == patients.size()) {
                        promises = new ArrayList<>();
                    }
                }
            }
        }
        info.phase = "Finished";
        LogEvent.logDebug(this.getClass().getSimpleName(), "transformPersistFhirPatients", "finished all batches");

        return info;
    }

    @GetMapping("/OEToFhir")
    public TransformationInfo transformPersistMissingFhirObjects(@RequestParam(defaultValue = "false") Boolean checkAll,
            @RequestParam(defaultValue = "100") int batchSize, @RequestParam(defaultValue = "1") int threads,
            @RequestParam(defaultValue = "true") boolean waitForResults)
            throws FhirLocalPersistingException, IOException {

        if (inProcess()) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "transformPersistMissingFhirObjects",
                    "processs already running");
            return info;
        }
        info.checkAll = checkAll;
        info.batchSize = batchSize;
        info.threads = threads;
        info.waitForResults = waitForResults;

        transformPersistFhirObjects();

        return info;

    }

    private void transformPersistFhirObjects() {
        try {
            info.objectType = "Patient";
            info.phase = "Fetching";
            List<Patient> patients;
            if (info.checkAll) {
                patients = sampleHumanService.getAllPatientsWithSampleEntered();
            } else {
                patients = sampleHumanService.getAllPatientsWithSampleEnteredMissingFhirUuid();
            }
            LogEvent.logDebug(this.getClass().getSimpleName(), "transformPersistMissingFhirObjects",
                    "patients to convert: " + patients.size());
            List<String> patientIds = new ArrayList<>();
            List<Future<Bundle>> promises = new ArrayList<>();
            info.objectType = "Patient";
            for (int i = 0; i < patients.size(); ++i) {
                info.phase = "Batch Transforming";
                patientIds.add(patients.get(i).getId());
                if (i % info.batchSize == info.batchSize - 1 || i + 1 == patients.size()) {
                    LogEvent.logDebug(this.getClass().getSimpleName(), "",
                            "persisting batch " + (i - info.batchSize + 1) + "-" + i + " of " + patients.size());
                    try {
                        promises.add(fhirTransformService.transformPersistPatients(patientIds));
                        patientIds = new ArrayList<>();
                        if (info.waitForResults && (promises.size() >= info.threads || i + 1 == patients.size())) {
                            waitForResults(promises);
                        }
                    } catch (FhirPersistanceException e) {
                        LogEvent.logError(e);
                        LogEvent.logError(this.getClass().getSimpleName(), "transformPersistMissingFhirObjects",
                                "error persisting batch " + (i - info.batchSize + 1) + "-" + i);
                    } catch (Exception e) {
                        LogEvent.logError(e);
                        LogEvent.logError(this.getClass().getSimpleName(), "transformPersistMissingFhirObjects",
                                "error with batch " + (i - info.batchSize + 1) + "-" + i);
                    }
                }
            }

            info.objectType = "Sample";
            info.phase = "Fetching";
            List<Sample> samples;
            if (info.checkAll) {
                samples = sampleService.getAll();
            } else {
                samples = sampleService.getAllMissingFhirUuid();
            }
            LogEvent.logDebug(this.getClass().getSimpleName(), "transformPersistMissingFhirObjects",
                    "samples to convert: " + samples.size());

            List<String> sampleIds = new ArrayList<>();
            promises = new ArrayList<>();
            for (int i = 0; i < samples.size(); ++i) {
                info.phase = "Batch Transforming";
                sampleIds.add(samples.get(i).getId());
                if (i % info.batchSize == info.batchSize - 1 || i + 1 == samples.size()) {
                    LogEvent.logDebug(this.getClass().getSimpleName(), "",
                            "persisting batch " + (i - info.batchSize + 1) + "-" + i + " of " + samples.size());
                    try {
                        promises.add(fhirTransformService.transformPersistObjectsUnderSamples(sampleIds));
                        ++info.batches;
                        sampleIds = new ArrayList<>();
                        if (info.waitForResults && (promises.size() >= info.threads || i + 1 == samples.size())) {
                            waitForResults(promises);
                        }
                    } catch (FhirPersistanceException e) {
                        ++info.batchFailure;
                        LogEvent.logError(e);
                        LogEvent.logError(this.getClass().getSimpleName(), "transformPersistMissingFhirObjects",
                                "error persisting batch " + (i - info.batchSize + 1) + "-" + i);
                    } catch (Exception e) {
                        ++info.batchFailure;
                        LogEvent.logError(e);
                        LogEvent.logError(this.getClass().getSimpleName(), "transformPersistMissingFhirObjects",
                                "error with batch " + (i - info.batchSize + 1) + "-" + i);
                    }
                }
            }
            LogEvent.logDebug(this.getClass().getSimpleName(), "transformPersistMissingFhirObjects",
                    "finished all batches");
            info.phase = "Finished";
        } catch (RuntimeException e) {
            throw e;
        } finally {
            endProcess();
        }
    }

    private void waitForResults(List<Future<Bundle>> promises) throws Exception {
        LogEvent.logDebug(this.getClass().getSimpleName(), "", "waiting for results from "
                + (promises.size() == 1 ? promises.size() + " thread" : promises.size() + " threads"));

        info.phase = "Waiting on Threads";
        for (int i = promises.size() - 1; i >= 0; i--) {
            try {
                promises.remove(i).get();
            } catch (InterruptedException | ExecutionException e) {
                LogEvent.logError(e);
                LogEvent.logError(this.getClass().getSimpleName(), "waitForResults", "Error getting value from thread");
                throw new Exception();
            }
        }
        // done so if there is a lot of data being processed, we backup to the CS in
        // tandem
        runExportTasks();
    }

    private void runExportTasks() {
        for (DataExportTask dataExportTask : dataExportTaskService.getDAO().findAll()) {
            dataExportService.exportNewDataFromLocalToRemote(dataExportTask);
        }
    }

    @Override
    protected String findLocalForward(String forward) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected String getPageTitleKey() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected String getPageSubtitleKey() {
        // TODO Auto-generated method stub
        return null;
    }

    private synchronized boolean inProcess() {
        if (info == null || !info.running) {
            info = new TransformationInfo();
            info.running = true;
            return false;
        }
        return true;
    }

    private synchronized void endProcess() {
        info.running = false;
    }

    public class TransformationInfo {
        public boolean running;
        public int batches;
        public int batchFailure;
        public String objectType;
        public String phase;
        public int batchSize;
        public int threads; // ignored if waitForResults is false
        public boolean checkAll;
        public boolean waitForResults;
    }
}
