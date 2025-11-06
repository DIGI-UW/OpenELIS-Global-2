/*
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations under
 * the License.
 *
 * The Original Code is OpenELIS code.
 *
 * Copyright (C) The Minnesota Department of Health.  All Rights Reserved.
 *
 * Contributor(s): CIRG, University of Washington, Seattle WA.
 */
package org.openelisglobal.reports.action.implementation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.apache.commons.validator.GenericValidator;
import org.jfree.util.Log;
import org.openelisglobal.common.services.QAService;
import org.openelisglobal.common.services.QAService.QAObservationType;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.note.service.NoteService;
import org.openelisglobal.note.valueholder.Note;
import org.openelisglobal.observationhistory.service.ObservationHistoryService;
import org.openelisglobal.observationhistory.valueholder.ObservationHistory;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.qaevent.worker.NonConformityUpdateWorker;
import org.openelisglobal.referencetables.service.ReferenceTablesService;
import org.openelisglobal.reports.action.implementation.reportBeans.FollowupRequiredData;
import org.openelisglobal.reports.form.ReportForm;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.util.CI.BaseProjectFormMapper;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.sampleorganization.service.SampleOrganizationService;
import org.openelisglobal.sampleorganization.valueholder.SampleOrganization;
import org.openelisglobal.sampleqaevent.service.SampleQaEventService;
import org.openelisglobal.sampleqaevent.valueholder.SampleQaEvent;
import org.openelisglobal.spring.util.SpringContext;

public class RetroCIFollowupRequiredByLocation extends RetroCIReport implements IReportParameterSetter, IReportCreator {

    private static String SAMPLE_TABLE_ID = null;
    private static String SAMPLE_QA_EVENT_TABLE_ID = null;
    private String lowDateStr;
    private String highDateStr;
    private DateRange dateRange;
    private List<FollowupRequiredData> reportItems;
    private SampleOrganizationService sampleOrganizationService = SpringContext
            .getBean(SampleOrganizationService.class);
    private ObservationHistoryService observationHistoryService = SpringContext
            .getBean(ObservationHistoryService.class);
    private NoteService noteService = SpringContext.getBean(NoteService.class);
    private SampleQaEventService sampleQaEventService = SpringContext.getBean(SampleQaEventService.class);
    private SampleService sampleService = SpringContext.getBean(SampleService.class);
    private DictionaryService dictionaryService = SpringContext.getBean(DictionaryService.class);
    private SampleHumanService sampleHumanService = SpringContext.getBean(SampleHumanService.class);

    static {
        SAMPLE_TABLE_ID = SpringContext.getBean(ReferenceTablesService.class).getReferenceTableByName("sample").getId();
        SAMPLE_QA_EVENT_TABLE_ID = SpringContext.getBean(ReferenceTablesService.class)
                .getReferenceTableByName("SAMPLE_QAEVENT").getId();
    }

    @Override
    protected String reportFileName() {
        return "RetroCI_FollowupRequired_ByLocation";
    }

    /**
     * @see org.openelisglobal.reports.action.implementation.IReportParameterSetter#setRequestParameters(org.openelisglobal.common.action.BaseActionForm)
     */
    @Override
    public void setRequestParameters(ReportForm form) {
        try {
            form.setReportName(getReportNameForParameterPage());
            form.setUseLowerDateRange(Boolean.TRUE);
            form.setUseUpperDateRange(Boolean.TRUE);
        } catch (RuntimeException e) {
            Log.error("Error in FollowupRequired_ByLocation.setRequestParemeters: ", e);
            // throw e;
        }
    }

    /**
     * @return
     */
    private String getReportNameForParameterPage() {
        return MessageUtil.getMessage("reports.label.followupRequired.title");
    }

    /**
     * @see org.openelisglobal.reports.action.implementation.RetroCIReport#initializeReport(org.openelisglobal.common.action.BaseActionForm)
     */
    @Override
    public void initializeReport(ReportForm form) {
        super.initializeReport();
        errorFound = false;

        lowDateStr = form.getLowerDateRange();
        highDateStr = form.getUpperDateRange();
        dateRange = new DateRange(lowDateStr, highDateStr);

        createReportParameters();

        errorFound = !validateSubmitParameters();
        if (errorFound) {
            return;
        }

        createReportItems();
        if (reportItems.size() == 0) {
            add1LineErrorMessage("report.error.message.noPrintableItems");
        }
    }

    @Override
    protected void createReportParameters() {
        super.createReportParameters();
        reportParameters.put("reportTitle", getReportNameForParameterPage() + "  -  " + dateRange.toString());
    }

    /** check everything */
    private boolean validateSubmitParameters() {
        return (dateRange.validateHighLowDate("report.error.message.date.received.missing"));
    }

    /** */
    private void createReportItems() {
        reportItems = new ArrayList<>();
        List<Sample> sampleList = sampleService.getSamplesReceivedInDateRange(
                DateUtil.convertSqlDateToStringDate(dateRange.getLowDate()),
                DateUtil.convertSqlDateToStringDate(dateRange.getHighDate()));

        for (Sample sample : sampleList) {
            if (QAService.isOrderNonConforming(sample) || isUnderInvestigation(sample)) {
                FollowupRequiredData item = new FollowupRequiredData();

                item.setCollectiondate(
                        sample.getCollectionDateForDisplay() + " " + sample.getCollectionTimeForDisplay());
                item.setReceivedDate(sample.getReceivedDateForDisplay() + " " + sample.getReceivedTimeForDisplay());
                item.setLabNo(sample.getAccessionNumber());
                item.setDoctor(getOptionalObservationHistory(sample, OBSERVATION_DOCTOR_ID));
                item.setNonConformityNotes(getNonConformingNotes(sample));
                item.setUnderInvestigationNotes(getUnderInvestigationNotes(sample));
                item.setOrgname(getServiceName(sample));
                Patient patient = getPatient(sample);
                item.setSubjectNumber(patient.getNationalId());
                item.setSiteSubjectNumber(patient.getExternalId());
                reportItems.add(item);
            }
        }

        Collections.sort(reportItems, new FollowupRequiredData.OrderByOrgName());
    }

    protected boolean isUnderInvestigation(Sample sample) {
        String entryUnderInvestigationQuestion = getOptionalObservationHistory(sample,
                OBSERVATION_UNDER_INVESTIGATION_ID);
        return BaseProjectFormMapper.YES_ANSWERS.contains(entryUnderInvestigationQuestion);
    }

    private String getNonConformingNotes(Sample sample) {
        StringBuilder allNotes = new StringBuilder();

        NoteService noteService = SpringContext.getBean(NoteService.class);
        String notes = noteService.getNotesAsString(sample,
                MessageUtil.getMessage("report.followup.general.comment") + ": ", "<br/>");
        if (notes != null) {
            allNotes.append(notes);
            allNotes.append("<br/>");
        }

        List<SampleQaEvent> qaEventList = sampleQaEventService.getSampleQaEventsBySample(sample);

        for (SampleQaEvent event : qaEventList) {
            QAService qa = new QAService(event);
            if (qa.getSampleItem() == null) {
                allNotes.append(MessageUtil.getMessage("report.followup.no.sampleType"));
            } else {
                allNotes.append(qa.getSampleItem().getTypeOfSample().getLocalizedName());
            }
            allNotes.append(" : ");

            if ("0".equals(qa.getObservationValue(QAObservationType.SECTION))) {
                allNotes.append(MessageUtil.getMessage("report.followup.no.section"));
            } else {
                allNotes.append(qa.getObservationForDisplay(QAObservationType.SECTION));
            }
            allNotes.append(" : ");

            if (GenericValidator.isBlankOrNull(qa.getObservationValue(QAObservationType.AUTHORIZER))) {
                allNotes.append(MessageUtil.getMessage("report.followup.no.authorizer"));
            } else {
                allNotes.append(qa.getObservationForDisplay(QAObservationType.AUTHORIZER));
            }
            allNotes.append(" : ");

            List<Note> qaNotes = noteService.getNoteByRefIAndRefTableAndSubject(qa.getEventId(),
                    SAMPLE_QA_EVENT_TABLE_ID, NonConformityUpdateWorker.NOTE_SUBJECT);

            if (qaNotes.isEmpty()) {
                allNotes.append(MessageUtil.getMessage("report.followup.no.note"));
            } else {
                allNotes.append(qaNotes.get(0).getText());
            }

            allNotes.append("<br/>");
        }

        return allNotes.length() == 0 ? null : allNotes.toString();
    }

    /**
     * Either the sample is fully defined and has an organization, or there might be
     * some random string defining a service stored in observation history. This
     * second scenerio comes from enter something in the system directly at on the
     * Non Conformant page.
     *
     * @param sample
     * @return a displayable organization name
     */
    private String getServiceName(Sample sample) {

        SampleOrganization so = sampleOrganizationService.getDataBySample(sample);
        String service;
        if (so == null) {
            return null;
        }
        service = so.getOrganization().getOrganizationName();
        if (service != null) {
            return service;
        }

        String serviceOH = getDictionaryValueForObservation(sample, OBSERVATION_SERVICE_ID);
        if (GenericValidator.isBlankOrNull(serviceOH)) {
            return null;
        }
        return service;
    }

    /**
     * @param sample
     * @return
     */
    private Patient getPatient(Sample sample) {
        return sampleHumanService.getPatientForSample(sample);
    }

    private String getUnderInvestigationNotes(Sample sample) {
        String entryUnderInvestigationQuestion = getOptionalObservationHistory(sample,
                OBSERVATION_UNDER_INVESTIGATION_ID);

        if (BaseProjectFormMapper.YES_ANSWERS.contains(entryUnderInvestigationQuestion)) {
            List<Note> noteList = noteService.getNoteByRefIAndRefTableAndSubject(sample.getId(), SAMPLE_TABLE_ID,
                    "UnderInvestigation");
            return noteList == null || noteList.isEmpty() ? null : noteList.get(0).getText();
        }

        return null;
    }

    private String getOptionalObservationHistory(Sample sample, String ohTypeId) {
        List<ObservationHistory> oh = observationHistoryService.getAll(null, sample, ohTypeId);
        if (oh == null || oh.size() == 0) {
            return null;
        }
        return oh.get(0).getValue();
    }

    private String getDictionaryValueForObservation(Sample sample, String ohTypeId) {
        String dictionaryId = getOptionalObservationHistory(sample, ohTypeId);
        if (dictionaryId == null) {
            return null;
        }
        Dictionary dictionary = null;
        try {
            dictionary = dictionaryService.getDictionaryById(dictionaryId);
        } catch (RuntimeException e) {
            return dictionaryId; // I guess it wasn't really a dictionary ID
            // after all, so let's just return it.
        }
        if (dictionary == null) {
            return dictionaryId; // it was a number, but it wasn't in the
            // dictionary.
        }
        return dictionary.getLocalAbbreviation();
    }

    /**
     * @see org.openelisglobal.reports.action.implementation.Report#getReportDataSource()
     */
    @Override
    public JRDataSource getReportDataSource() throws IllegalStateException {
        return errorFound ? new JRBeanCollectionDataSource(errorMsgs) : new JRBeanCollectionDataSource(reportItems);
    }
}
