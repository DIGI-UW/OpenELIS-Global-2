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
package org.openelisglobal.audittrail.action.workers;

import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.services.SampleOrderService;
import org.openelisglobal.common.services.historyservices.AbstractHistoryService;
import org.openelisglobal.common.services.historyservices.AnalysisHistoryService;
import org.openelisglobal.common.services.historyservices.NoteHistoryService;
import org.openelisglobal.common.services.historyservices.OrderHistoryService;
import org.openelisglobal.common.services.historyservices.PatientHistoryHistoryService;
import org.openelisglobal.common.services.historyservices.PatientHistoryService;
import org.openelisglobal.common.services.historyservices.ProgramSampleHistoryService;
import org.openelisglobal.common.services.historyservices.QaHistoryService;
import org.openelisglobal.common.services.historyservices.ReportHistoryService;
import org.openelisglobal.common.services.historyservices.ResultHistoryService;
import org.openelisglobal.common.services.historyservices.SampleHistoryService;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.observationhistory.service.ObservationHistoryService;
import org.openelisglobal.observationhistory.service.ObservationHistoryServiceImpl.ObservationType;
import org.openelisglobal.patient.action.bean.PatientManagementBridge;
import org.openelisglobal.patient.action.bean.PatientManagementInfo;
import org.openelisglobal.patient.util.PatientUtil;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.program.service.ProgramSampleService;
import org.openelisglobal.program.valueholder.ProgramSample;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.sample.bean.SampleOrderItem;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.spring.util.SpringContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class AuditTrailViewWorkerImpl implements AuditTrailViewWorker {

    @Autowired
    protected AnalysisService analysisService;
    @Autowired
    protected ResultService resultService;
    @Autowired
    protected SampleService sampleService;
    @Autowired
    protected ProgramSampleService programSampleService;
    @Autowired
    protected ObservationHistoryService observationHistoryService;

    private String accessionNumber = null;
    private Sample sample;

    public void setAccessionNumber(String accessionNumber) {
        this.accessionNumber = accessionNumber;
        sample = null;
    }

    @Transactional
    public List<AuditTrailItem> getAuditTrail() throws IllegalStateException {
        if (GenericValidator.isBlankOrNull(accessionNumber)) {
            throw new IllegalStateException("AuditTrialViewWorker is not initialized");
        }

        getSample();

        List<AuditTrailItem> items = new ArrayList<>();

        if (sample != null) {
            items.addAll(addOrders());
            items.addAll(addSamples());
            items.addAll(addTestsAndResults());
            items.addAll(addReports());
            items.addAll(addPatientHistory());
            items.addAll(addNotes());
            items.addAll(addQAEvents());
            if (!addProgram().isEmpty()) {
                items.addAll(addProgram());
            }

        }

        sortItemsByTime(items);
        return items;
    }

    public SampleOrderItem getSampleOrderSnapshot() {
        if (GenericValidator.isBlankOrNull(accessionNumber)) {
            throw new IllegalStateException("AuditTrialViewWorker is not initialized");
        }

        SampleOrderService orderService = new SampleOrderService(accessionNumber, true);
        return orderService.getSampleOrderItem();
    }

    public PatientManagementInfo getPatientSnapshot() {
        if (GenericValidator.isBlankOrNull(accessionNumber)) {
            throw new IllegalStateException("AuditTrialViewWorker is not initialized");
        }

        getSample();

        if (sample != null) {
            SampleHumanService sampleHumanService = SpringContext.getBean(SampleHumanService.class);
            Patient patient = sampleHumanService.getPatientForSample(sample);
            return new PatientManagementBridge().getPatientManagementInfoFor(patient, true);
        } else {
            return new PatientManagementInfo();
        }
    }

    public List<AuditTrailItem> getPatientHistoryAuditTrail() throws IllegalStateException {
        if (GenericValidator.isBlankOrNull(accessionNumber)) {
            throw new IllegalStateException("AuditTrialViewWorker is not initialized");
        }

        getSample();

        List<AuditTrailItem> items = new ArrayList<>();

        if (sample != null) {
            items.addAll(addPatientHistory());
        }
        return items;
    }

    private void getSample() {
        if (sample == null) {
            sample = sampleService.getSampleByAccessionNumber(accessionNumber);
        }
    }

    private Collection<AuditTrailItem> addReports() {
        List<AuditTrailItem> items = new ArrayList<>();

        if (sample != null) {
            AbstractHistoryService historyService = new ReportHistoryService(sample);
            items.addAll(historyService.getAuditTrailItems());

            // sortItemsByTime(items);
        }

        for (AuditTrailItem auditTrailItem : items) {
            auditTrailItem.setClassName("reportAudit");
            setAttributeNewIfInsert(auditTrailItem);
        }
        return items;
    }

    private Collection<AuditTrailItem> addSamples() {
        List<AuditTrailItem> sampleItems = new ArrayList<>();
        if (sample != null) {
            AbstractHistoryService historyService = new SampleHistoryService(sample);
            sampleItems.addAll(historyService.getAuditTrailItems());

            // sortItems(sampleItems);

            for (AuditTrailItem auditTrailItem : sampleItems) {
                auditTrailItem.setClassName("sampleAudit");
                setAttributeNewIfInsert(auditTrailItem);
            }
        }

        return sampleItems;
    }

    private Collection<AuditTrailItem> addOrders() {
        List<AuditTrailItem> orderItems = new ArrayList<>();
        if (sample != null) {
            AbstractHistoryService historyService = new OrderHistoryService(sample);
            orderItems.addAll(historyService.getAuditTrailItems());

            // sortItems(orderItems);

            for (AuditTrailItem auditTrailItem : orderItems) {
                auditTrailItem.setClassName("orderAudit");
                setAttributeNewIfInsert(auditTrailItem);
            }
        }
        return orderItems;
    }

    private void setAttributeNewIfInsert(AuditTrailItem auditTrailItem) {
        if (auditTrailItem.getAction().equals("I")) {
            auditTrailItem.setAttribute(MessageUtil.getMessage("auditTrail.action.new"));
        }
    }

    private List<AuditTrailItem> addTestsAndResults() {
        List<AuditTrailItem> items = new ArrayList<>();

        List<Analysis> analysisList = analysisService.getAnalysesBySampleId(sample.getId());

        for (Analysis analysis : analysisList) {
            List<Result> resultList = resultService.getResultsByAnalysis(analysis);
            AbstractHistoryService historyService = new AnalysisHistoryService(analysis);
            List<AuditTrailItem> resultItems = historyService.getAuditTrailItems();
            items.addAll(resultItems);

            for (Result result : resultList) {
                historyService = new ResultHistoryService(result, analysis);
                resultItems = historyService.getAuditTrailItems();

                items.addAll(resultItems);
            }
        }

        // sortItems(items);
        for (AuditTrailItem auditTrailItem : items) {
            auditTrailItem.setClassName("testResultAudit");
            setAttributeNewIfInsert(auditTrailItem);
        }
        return items;
    }

    private Collection<AuditTrailItem> addPatientHistory() {
        List<AuditTrailItem> items = new ArrayList<>();
        AbstractHistoryService historyService;
        Patient patient = PatientUtil.getPatientForSample(sample);
        if (patient != null) {
            historyService = new PatientHistoryService(patient);
            items.addAll(historyService.getAuditTrailItems());
        }

        // historyService = new HistoryService(sample, HistoryType.PERSON);
        // items.addAll(historyService.getAuditTrailItems());

        historyService = new PatientHistoryHistoryService(sample);
        items.addAll(historyService.getAuditTrailItems());

        // sortItems(items);

        for (AuditTrailItem auditTrailItem : items) {
            auditTrailItem.setClassName("patientHistoryAudit");
            setAttributeNewIfInsert(auditTrailItem);
        }
        return items;
    }

    private Collection<AuditTrailItem> addNotes() {
        List<AuditTrailItem> notes = new ArrayList<>();
        if (sample != null) {
            AbstractHistoryService historyService = new NoteHistoryService(sample);
            notes.addAll(historyService.getAuditTrailItems());

            // sortItems(notes);

            for (AuditTrailItem auditTrailItem : notes) {
                auditTrailItem.setClassName("noteAudit");
                setAttributeNewIfInsert(auditTrailItem);
            }
        }

        return notes;
    }

    private Collection<AuditTrailItem> addQAEvents() {
        List<AuditTrailItem> qaEvents = new ArrayList<>();
        if (sample != null) {
            QaHistoryService qaService = new QaHistoryService(sample);
            qaEvents = qaService.getAuditTrailItems();

            for (AuditTrailItem auditTrailItem : qaEvents) {
                auditTrailItem.setClassName("qaEvent");
                setAttributeNewIfInsert(auditTrailItem);
            }
        }

        return qaEvents;
    }

    private Collection<AuditTrailItem> addProgram() {
        List<AuditTrailItem> programs = new ArrayList<>();
        if (sample != null) {
            String programName = observationHistoryService.getRawValueForSample(ObservationType.PROGRAM,
                    sample.getId());
            ProgramSample programSample = programSampleService
                    .getProgrammeSampleBySample(Integer.valueOf(sample.getId()), programName);
            if (programSample == null) {
                return programs;
            }
            AbstractHistoryService historyService = new ProgramSampleHistoryService(programSample);
            programs.addAll(historyService.getAuditTrailItems());

            // sortItems(notes);

            for (AuditTrailItem auditTrailItem : programs) {
                auditTrailItem.setClassName("programSampleAudit");
                setAttributeNewIfInsert(auditTrailItem);
            }
        }

        return programs;
    }

    @SuppressWarnings("unused")
    private void sortItems(List<AuditTrailItem> items) {
        Collections.sort(items, new Comparator<AuditTrailItem>() {
            @Override
            public int compare(AuditTrailItem o1, AuditTrailItem o2) {
                int sort = o1.getIdentifier().compareTo(o2.getIdentifier());
                if (sort != 0) {
                    return sort;
                }

                sort = o1.getTimeStamp().compareTo(o2.getTimeStamp());
                if (sort != 0) {
                    return sort;
                }

                return o1.getAction().compareTo(o2.getAction());
            }
        });
    }

    private void sortItemsByTime(List<AuditTrailItem> items) {
        Collections.sort(items, new Comparator<AuditTrailItem>() {
            @Override
            public int compare(AuditTrailItem o1, AuditTrailItem o2) {
                return o1.getTimeStamp().compareTo(o2.getTimeStamp());
            }
        });
    }
}
