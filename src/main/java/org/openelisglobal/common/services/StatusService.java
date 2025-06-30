/**
 * The contents of this file are subject to the Mozilla Public License Version 1.1 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.mozilla.org/MPL/
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF
 * ANY KIND, either express or implied. See the License for the specific language governing rights
 * and limitations under the License.
 *
 * <p>The Original Code is OpenELIS code.
 *
 * <p>Copyright (C) ITECH, University of Washington, Seattle WA. All Rights Reserved.
 */
package org.openelisglobal.common.services;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.observationhistory.service.ObservationHistoryService;
import org.openelisglobal.observationhistory.valueholder.ObservationHistory;
import org.openelisglobal.observationhistory.valueholder.ObservationHistory.ValueType;
import org.openelisglobal.observationhistorytype.service.ObservationHistoryTypeService;
import org.openelisglobal.observationhistorytype.valueholder.ObservationHistoryType;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.samplehuman.valueholder.SampleHuman;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.statusofsample.service.StatusOfSampleService;
import org.openelisglobal.statusofsample.valueholder.StatusOfSample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StatusService implements IStatusService {
    public enum OrderStatus {
        Entered, Started, Finished, NonConforming_depricated
    }

    public enum AnalysisStatus {
        SampleRejected, NotStarted, Canceled, TechnicalAcceptance, TechnicalRejected, BiologistRejected,
        NonConforming_depricated, Finalized
    }

    public enum RecordStatus {
        NotRegistered, InitialRegistration, ValidationRegistration
    }

    public enum SampleStatus {
        SampleRejected, Entered, Canceled
    }

    public enum StatusType {
        Analysis, Sample, Order, SampleEntry, PatientEntry
    }

    public enum ExternalOrderStatus {
        Entered, Cancelled, Realized, NonConforming
    }

    private Map<String, OrderStatus> idToOrderStatusMap = null;
    private Map<String, SampleStatus> idToSampleStatusMap = null;
    private Map<String, AnalysisStatus> idToAnalysisStatusMap = null;
    private Map<String, RecordStatus> idToRecordStatusMap = null;
    private Map<String, ExternalOrderStatus> idToExternalOrderStatusMap = null;

    private Map<OrderStatus, StatusOfSample> orderStatusToObjectMap = null;
    private Map<SampleStatus, StatusOfSample> sampleStatusToObjectMap = null;
    private Map<AnalysisStatus, StatusOfSample> analysisStatusToObjectMap = null;
    private Map<RecordStatus, Dictionary> recordStatusToObjectMap = null;
    private Map<ExternalOrderStatus, StatusOfSample> externalOrderStatusToObjectMap = null;

    private String orderRecordStatusID;
    private String patientRecordStatusID;

    @Autowired
    ObservationHistoryService observationHistoryService;
    @Autowired
    SampleService sampleService;
    @Autowired
    StatusOfSampleService statusOfSampleService;
    @Autowired
    DictionaryService dictionaryService;
    @Autowired
    ObservationHistoryTypeService observationTypeService;
    @Autowired
    AnalysisService analysisService;
    @Autowired
    SampleHumanService sampleHumanService;

    @PostConstruct
    private void buildMaps() {
        orderStatusToObjectMap = new HashMap<>();
        sampleStatusToObjectMap = new HashMap<>();
        analysisStatusToObjectMap = new HashMap<>();
        recordStatusToObjectMap = new HashMap<>();
        externalOrderStatusToObjectMap = new HashMap<>();
        idToOrderStatusMap = new HashMap<>();
        idToSampleStatusMap = new HashMap<>();
        idToAnalysisStatusMap = new HashMap<>();
        idToRecordStatusMap = new HashMap<>();
        idToExternalOrderStatusMap = new HashMap<>();

        buildStatusToIdMaps();

        // now put everything in the reverse map
        buildIdToStatusMapsFromStatusToIdMaps();

        getObservationHistoryTypeIDs();
    }

    public static IStatusService getInstance() {
        return SpringContext.getBean(IStatusService.class);
    }

    @Override
    public boolean matches(String id, SampleStatus sampleStatus) {
        return getStatusID(sampleStatus).equals(id);
    }

    @Override
    public boolean matches(String id, AnalysisStatus analysisStatus) {
        return getStatusID(analysisStatus).equals(id);
    }

    @Override
    public boolean matches(String id, OrderStatus orderStatus) {
        return getStatusID(orderStatus).equals(id);
    }

    @Override
    public boolean matches(String id, ExternalOrderStatus externalOrderStatus) {
        return getStatusID(externalOrderStatus).equals(id);
    }

    @Override
    public String getStatusID(OrderStatus statusType) {
        StatusOfSample status = orderStatusToObjectMap.get(statusType);
        return status == null ? "-1" : status.getId();
    }

    @Override
    public String getStatusID(SampleStatus statusType) {
        StatusOfSample status = sampleStatusToObjectMap.get(statusType);
        return status == null ? "-1" : status.getId();
    }

    @Override
    public String getStatusID(AnalysisStatus statusType) {
        StatusOfSample status = analysisStatusToObjectMap.get(statusType);
        return status == null ? "-1" : status.getId();
    }

    @Override
    public String getStatusID(ExternalOrderStatus statusType) {
        StatusOfSample status = externalOrderStatusToObjectMap.get(statusType);
        return status == null ? "-1" : status.getId();
    }

    @Override
    public String getStatusName(RecordStatus statusType) {
        Dictionary dictionary = recordStatusToObjectMap.get(statusType);
        return dictionary == null ? "unknown" : dictionary.getLocalizedName();
    }

    @Override
    public String getStatusName(OrderStatus statusType) {
        StatusOfSample status = orderStatusToObjectMap.get(statusType);
        return status == null ? "unknown" : status.getLocalizedName();
    }

    @Override
    public String getStatusName(SampleStatus statusType) {
        StatusOfSample status = sampleStatusToObjectMap.get(statusType);
        return status == null ? "unknown" : status.getLocalizedName();
    }

    @Override
    public String getStatusName(AnalysisStatus statusType) {
        StatusOfSample status = analysisStatusToObjectMap.get(statusType);
        return status == null ? "unknown" : status.getLocalizedName();
    }

    @Override
    public String getStatusName(ExternalOrderStatus statusType) {
        StatusOfSample status = externalOrderStatusToObjectMap.get(statusType);
        return status == null ? "unknown" : status.getLocalizedName();
    }

    @Override
    public String getDictionaryID(RecordStatus statusType) {
        Dictionary dictionary = recordStatusToObjectMap.get(statusType);
        return dictionary == null ? "-1" : dictionary.getId();
    }

    @Override
    public OrderStatus getOrderStatusForID(String id) {
        return idToOrderStatusMap.get(id);
    }

    @Override
    public SampleStatus getSampleStatusForID(String id) {
        return idToSampleStatusMap.get(id);
    }

    @Override
    public AnalysisStatus getAnalysisStatusForID(String id) {
        return idToAnalysisStatusMap.get(id);
    }

    @Override
    public ExternalOrderStatus getExternalOrderStatusForID(String id) {
        return idToExternalOrderStatusMap.get(id);
    }

    @Override
    public RecordStatus getRecordStatusForID(String id) {
        return idToRecordStatusMap.get(id);
    }

    @Override
    @Transactional(readOnly = true)
    public StatusSet getStatusSetForSampleId(String sampleId) {
        Sample sample = new Sample();
        sample.setId(sampleId);

        sampleService.getData(sample);

        return buildStatusSet(sample);
    }

    @Override
    @Transactional(readOnly = true)
    public StatusSet getStatusSetForAccessionNumber(String accessionNumber) {
        if (GenericValidator.isBlankOrNull(accessionNumber)) {
            return new StatusSet();
        }

        Sample sample = sampleService.getSampleByAccessionNumber(accessionNumber);

        return buildStatusSet(sample);
    }

    /*
     * Preconditions: It is called within a transaction Both the patient and sample
     * ids are valid
     *
     * For now it will fail silently Either sampleStatus or patient status may be
     * null
     */
    @Override
    @Transactional
    public void persistRecordStatusForSample(Sample sample, RecordStatus recordStatus, Patient patient,
            RecordStatus patientStatus, String sysUserId) {

        if (sample == null || patient == null) {
            return;
        }

        List<ObservationHistory> observationList = observationHistoryService.getAll(patient, sample);

        ObservationHistory sampleRecord = null;
        ObservationHistory patientRecord = null;

        for (ObservationHistory currentHistory : observationList) {
            if (currentHistory.getObservationHistoryTypeId().equals(orderRecordStatusID)) {
                sampleRecord = currentHistory;
            } else if (currentHistory.getObservationHistoryTypeId().equals(patientRecordStatusID)) {
                patientRecord = currentHistory;
            }
        }

        if (recordStatus != null) {
            insertOrUpdateStatus(sample, patient, recordStatus, sysUserId, sampleRecord, orderRecordStatusID);
        }

        if (patientStatus != null) {
            insertOrUpdateStatus(sample, patient, patientStatus, sysUserId, patientRecord, patientRecordStatusID);
        }
    }

    private void insertOrUpdateStatus(Sample sample, Patient patient, RecordStatus status, String sysUserId,
            ObservationHistory record, String historyTypeId) {

        if (record == null) {
            record = new ObservationHistory();
            record.setObservationHistoryTypeId(historyTypeId);
            record.setPatientId(patient.getId());
            record.setSampleId(sample.getId());
            record.setSysUserId(sysUserId);
            record.setValue(getDictionaryID(status));
            record.setValueType(ValueType.DICTIONARY);
            observationHistoryService.insert(record);
        } else {
            record.setSysUserId(sysUserId);
            record.setValue(getDictionaryID(status));
            observationHistoryService.update(record);
        }
    }

    @Override
    @Transactional
    public void deleteRecordStatus(Sample sample, Patient patient, String sysUserId) {

        if (sample == null || patient == null) {
            return;
        }

        List<ObservationHistory> observations = observationHistoryService.getAll(patient, sample);

        List<ObservationHistory> records = new ArrayList<>();

        for (ObservationHistory observation : observations) {
            if (observation.getObservationHistoryTypeId().equals(orderRecordStatusID)
                    || observation.getObservationHistoryTypeId().equals(patientRecordStatusID)) {
                observation.setSysUserId(sysUserId);
                records.add(observation);
            }
        }

        observationHistoryService.deleteAll(records);
    }

    @Override
    public String getStatusNameFromId(String id) {
        if (idToAnalysisStatusMap.get(id) != null) {
            return getStatusName(idToAnalysisStatusMap.get(id));
        } else if (idToOrderStatusMap.get(id) != null) {
            return getStatusName(idToOrderStatusMap.get(id));
        } else if (idToSampleStatusMap.get(id) != null) {
            return getStatusName(idToSampleStatusMap.get(id));
        } else if (idToRecordStatusMap.get(id) != null) {
            return getStatusName(idToRecordStatusMap.get(id));
        } else if (idToExternalOrderStatusMap.get(id) != null) {
            return getStatusName(idToExternalOrderStatusMap.get(id));
        }
        return null;
    }

    private void buildStatusToIdMaps() {

        List<StatusOfSample> statusList = statusOfSampleService.getAllStatusOfSamples();

        // sorry about this but it is only done once and until Java 7 we have to
        // use if..else
        for (StatusOfSample status : statusList) {
            if (status.getStatusType().equals("ORDER")) {
                addToOrderMap(status);
            } else if (status.getStatusType().equals("ANALYSIS")) {
                addToAnalysisMap(status);
            } else if (status.getStatusType().equals("SAMPLE")) {
                addToSampleMap(status);
            } else if (status.getStatusType().equals("EXTERNAL_ORDER")) {
                addToExternalOrderMap(status);
            }
        }

        List<Dictionary> dictionaryList = dictionaryService
                .getDictionaryEntrysByCategoryNameLocalizedSort("REC_STATUS");

        for (Dictionary dictionary : dictionaryList) {
            addToRecordMap(dictionary);
        }
    }

    private void addToOrderMap(StatusOfSample status) {
        String name = status.getStatusOfSampleName();

        if (name.equals("Test Entered")) {
            orderStatusToObjectMap.put(OrderStatus.Entered, status);
        } else if (name.equals("Testing Started")) {
            orderStatusToObjectMap.put(OrderStatus.Started, status);
        } else if (name.equals("Testing finished")) {
            orderStatusToObjectMap.put(OrderStatus.Finished, status);
        } else if (name.equals("NonConforming")) {
            orderStatusToObjectMap.put(OrderStatus.NonConforming_depricated, status);
        }
    }

    private void addToAnalysisMap(StatusOfSample status) {
        String name = status.getStatusOfSampleName();

        if (name.equals("Not Tested")) {
            analysisStatusToObjectMap.put(AnalysisStatus.NotStarted, status);
        } else if (name.equals("Test Canceled")) {
            analysisStatusToObjectMap.put(AnalysisStatus.Canceled, status);
        } else if (name.equals("Technical Acceptance")) {
            analysisStatusToObjectMap.put(AnalysisStatus.TechnicalAcceptance, status);
        } else if (name.equals("Technical Rejected")) {
            analysisStatusToObjectMap.put(AnalysisStatus.TechnicalRejected, status);
        } else if (name.equals("Biologist Rejection")) {
            analysisStatusToObjectMap.put(AnalysisStatus.BiologistRejected, status);
        } else if (name.equals("Finalized")) {
            analysisStatusToObjectMap.put(AnalysisStatus.Finalized, status);
        } else if (name.equals("NonConforming")) {
            analysisStatusToObjectMap.put(AnalysisStatus.NonConforming_depricated, status);
        } else if (name.equals("Sample Rejected")) {
            analysisStatusToObjectMap.put(AnalysisStatus.SampleRejected, status);
        }
    }

    private void addToExternalOrderMap(StatusOfSample status) {
        String name = status.getStatusOfSampleName();

        if (name.equals("Entered")) {
            externalOrderStatusToObjectMap.put(ExternalOrderStatus.Entered, status);
        } else if (name.equals("Cancelled")) {
            externalOrderStatusToObjectMap.put(ExternalOrderStatus.Cancelled, status);
        } else if (name.equals("Realized")) {
            externalOrderStatusToObjectMap.put(ExternalOrderStatus.Realized, status);
        } else if (name.equals("NonConforming")) {
            externalOrderStatusToObjectMap.put(ExternalOrderStatus.NonConforming, status);
        }
    }

    private void addToSampleMap(StatusOfSample status) {
        String name = status.getStatusOfSampleName();

        if (name.equals("SampleEntered")) {
            sampleStatusToObjectMap.put(SampleStatus.Entered, status);
        } else if (name.equals("SampleCanceled")) {
            sampleStatusToObjectMap.put(SampleStatus.Canceled, status);
        } else if (name.equals("Sample Rejected")) {
            sampleStatusToObjectMap.put(SampleStatus.SampleRejected, status);
        }
    }

    private void addToRecordMap(Dictionary dictionary) {
        String name = dictionary.getLocalAbbreviation();

        if (name.equals("Not Start")) {
            recordStatusToObjectMap.put(RecordStatus.NotRegistered, dictionary);
        } else if (name.equals("Init Ent")) {
            recordStatusToObjectMap.put(RecordStatus.InitialRegistration, dictionary);
        } else if (name.equals("Valid Ent")) {
            recordStatusToObjectMap.put(RecordStatus.ValidationRegistration, dictionary);
        }
    }

    private void buildIdToStatusMapsFromStatusToIdMaps() {
        for (Entry<OrderStatus, StatusOfSample> status : orderStatusToObjectMap.entrySet()) {
            idToOrderStatusMap.put(status.getValue().getId(), status.getKey());
        }
        for (Entry<SampleStatus, StatusOfSample> status : sampleStatusToObjectMap.entrySet()) {
            idToSampleStatusMap.put(status.getValue().getId(), status.getKey());
        }

        for (Entry<AnalysisStatus, StatusOfSample> status : analysisStatusToObjectMap.entrySet()) {
            idToAnalysisStatusMap.put(status.getValue().getId(), status.getKey());
        }
        for (Entry<RecordStatus, Dictionary> status : recordStatusToObjectMap.entrySet()) {
            idToRecordStatusMap.put(status.getValue().getId(), status.getKey());
        }
        for (Entry<ExternalOrderStatus, StatusOfSample> status : externalOrderStatusToObjectMap.entrySet()) {
            idToExternalOrderStatusMap.put(status.getValue().getId(), status.getKey());
        }
    }

    @Transactional(readOnly = true)
    private void getObservationHistoryTypeIDs() {
        List<ObservationHistoryType> obsrvationTypeList = observationTypeService.getAll();

        for (ObservationHistoryType observationType : obsrvationTypeList) {
            if ("SampleRecordStatus".equals(observationType.getTypeName())) {
                orderRecordStatusID = observationType.getId();
            } else if ("PatientRecordStatus".equals(observationType.getTypeName())) {
                patientRecordStatusID = observationType.getId();
            }
        }
    }

    private StatusSet buildStatusSet(Sample sample) {
        StatusSet statusSet = new StatusSet();
        if (sample == null || sample.getId() == null) {
            statusSet.setPatientRecordStatus(null);
            statusSet.setSampleRecordStatus(null);
        } else {

            statusSet.setSampleStatus(getOrderStatusForID(sample.getStatusId()));

            setAnalysisStatus(statusSet, sample);

            setRecordStatus(statusSet, sample);
        }

        return statusSet;
    }

    @Transactional(readOnly = true)
    private void setAnalysisStatus(StatusSet statusSet, Sample sample) {
        List<Analysis> analysisList = analysisService.getAnalysesBySampleId(sample.getId());

        Map<Analysis, AnalysisStatus> analysisStatusMap = new HashMap<>();

        for (Analysis analysis : analysisList) {
            analysisStatusMap.put(analysis, getAnalysisStatusForID(analysis.getStatusId()));
        }

        statusSet.setAnalysisStatus(analysisStatusMap);
    }

    @Transactional(readOnly = true)
    private void setRecordStatus(StatusSet statusSet, Sample sample) {
        if ("H".equals(sample.getDomain())) {
            SampleHuman sampleHuman = new SampleHuman();
            sampleHuman.setSampleId(sample.getId());
            sampleHuman = sampleHumanService.getDataBySample(sampleHuman);

            String patientId = sampleHuman.getPatientId();

            statusSet.setSampleId(sample.getId());
            statusSet.setPatientId(patientId);

            if (patientId != null) {
                Patient patient = new Patient();
                patient.setId(patientId);

                List<ObservationHistory> observations = observationHistoryService.getAll(patient, sample);

                for (ObservationHistory observation : observations) {
                    if (observation.getObservationHistoryTypeId().equals(orderRecordStatusID)) {
                        statusSet.setSampleRecordStatus(getRecordStatusForID(observation.getValue()));
                    } else if (observation.getObservationHistoryTypeId().equals(patientRecordStatusID)) {
                        statusSet.setPatientRecordStatus(getRecordStatusForID(observation.getValue()));
                    }
                }
            }
        }
    }
}
