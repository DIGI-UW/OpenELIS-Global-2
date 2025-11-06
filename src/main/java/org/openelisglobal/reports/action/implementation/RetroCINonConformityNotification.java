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
package org.openelisglobal.reports.action.implementation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.provider.validation.AccessionNumberValidatorFactory.AccessionFormat;
import org.openelisglobal.common.provider.validation.AlphanumAccessionValidator;
import org.openelisglobal.common.services.QAService;
import org.openelisglobal.common.services.QAService.QAObservationType;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.project.valueholder.Project;
import org.openelisglobal.qaevent.service.NonConformityHelper;
import org.openelisglobal.reports.action.implementation.reportBeans.NonConformityReportData;
import org.openelisglobal.reports.action.util.ReportUtil;
import org.openelisglobal.reports.form.ReportForm;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleorganization.service.SampleOrganizationService;
import org.openelisglobal.sampleorganization.valueholder.SampleOrganization;
import org.openelisglobal.sampleqaevent.service.SampleQaEventService;
import org.openelisglobal.sampleqaevent.valueholder.SampleQaEvent;
import org.openelisglobal.spring.util.SpringContext;

public class RetroCINonConformityNotification extends RetroCIReport implements IReportCreator, IReportParameterSetter {

    private static SampleQaEventService sampleQAService = SpringContext.getBean(SampleQaEventService.class);
    private static SampleOrganizationService sampleOrgService = SpringContext.getBean(SampleOrganizationService.class);
    private static SampleService sampleService = SpringContext.getBean(SampleService.class);

    private List<NonConformityReportData> reportItems;
    private String requestedAccessionNumber;
    private List<String> sampleQaEventIds;
    private Set<String> checkIdsForPriorPrintRecord;

    public RetroCINonConformityNotification() {
        super();
    }

    @Override
    public void setRequestParameters(ReportForm form) {
        try {
            form.setReportName(MessageUtil.getMessage("reports.nonConformity.notification.report"));
            form.setSelectList(
                    new ReportSpecificationList(getSiteList(), MessageUtil.getMessage("report.select.site")));
            form.setUseAccessionDirect(Boolean.TRUE);
            form.setInstructions(MessageUtil.getMessage("reports.nonConformity.notification.report.instructions"));
        } catch (RuntimeException e) {
            LogEvent.logDebug(e);
        }
    }

    private List<IdValuePair> getSiteList() {
        List<IdValuePair> sites = new ArrayList<>();

        Set<String> orgIds = new HashSet<>();
        List<Organization> services = new ArrayList<>();
        List<SampleQaEvent> events = sampleQAService.getAllUncompleatedEvents();

        events = filterReportedEvents(events);

        for (SampleQaEvent event : events) {
            SampleOrganization sampleOrg = sampleOrgService.getDataBySample(event.getSample());
            if (sampleOrg != null) {
                if (!orgIds.contains(sampleOrg.getOrganization().getId())) {
                    orgIds.add(sampleOrg.getOrganization().getId());
                    services.add(sampleOrg.getOrganization());
                }
            }
        }

        Collections.sort(services, new Comparator<Organization>() {
            @Override
            public int compare(Organization o1, Organization o2) {
                return o1.getOrganizationName().compareTo(o2.getOrganizationName());
            }
        });

        for (Organization org : services) {
            sites.add(new IdValuePair(org.getId(), org.getOrganizationName()));
        }

        return sites;
    }

    private List<SampleQaEvent> filterReportedEvents(List<SampleQaEvent> events) {
        List<SampleQaEvent> filteredList = new ArrayList<>();

        for (SampleQaEvent event : events) {
            if (!ReportUtil.documentHasBeenPrinted(ReportUtil.DocumentTypes.NON_CONFORMITY_NOTIFCATION,
                    event.getId())) {
                filteredList.add(event);
            }
        }

        return filteredList;
    }

    @Override
    public void initializeReport(ReportForm form) {
        super.initializeReport();
        sampleQaEventIds = new ArrayList<>();
        checkIdsForPriorPrintRecord = new HashSet<>();
        errorFound = false;
        requestedAccessionNumber = form.getAccessionDirectNoSuffix();
        ReportSpecificationList specificationList = form.getSelectList();

        createReportParameters();

        errorFound = !validateSubmitParameters(specificationList.getSelection());
        if (errorFound) {
            return;
        }
        createReportItems(specificationList.getSelection());
        if (reportItems.size() == 0) {
            add1LineErrorMessage("report.error.message.noPrintableItems");
        }
    }

    private boolean validateSubmitParameters(String serviceId) {
        if (GenericValidator.isBlankOrNull(requestedAccessionNumber)
                && (GenericValidator.isBlankOrNull(serviceId) || "0".equals(serviceId))) {
            add1LineErrorMessage("report.error.message.noParameters");
            return false;
        }

        if (!GenericValidator.isBlankOrNull(requestedAccessionNumber)) {
            Sample sample = sampleService.getSampleByAccessionNumber(requestedAccessionNumber);
            if (sample == null) {
                add1LineErrorMessage("report.error.message.accession.not.valid");
                return false;
            }
        }

        return true;
    }

    private void createReportItems(String serviceId) {
        reportItems = new ArrayList<>();
        List<Sample> samples = getNonConformingSamples(serviceId);

        samples = sortAndFilterSamples(samples);

        for (Sample sample : samples) {
            reportItems.addAll(createNonconformityItem(sample));
        }
    }

    private List<Sample> getNonConformingSamples(String serviceId) {
        List<Sample> samples = new ArrayList<>();

        if (!GenericValidator.isBlankOrNull(requestedAccessionNumber)) {
            // we've already checked to make sure there is a sample for the accessionNumber
            samples.add(sampleService.getSampleByAccessionNumber(requestedAccessionNumber));
        }

        if (!GenericValidator.isBlankOrNull(serviceId)) {
            List<Sample> sampleList = sampleService.getSamplesWithPendingQaEventsByService(serviceId);
            samples.addAll(sampleList);
        }

        return samples;
    }

    private List<Sample> sortAndFilterSamples(List<Sample> samples) {
        Collections.sort(samples, new Comparator<Sample>() {
            @Override
            public int compare(Sample o1, Sample o2) {
                return o1.getAccessionNumber().compareTo(o2.getAccessionNumber());
            }
        });

        List<Sample> filteredSamples = new ArrayList<>();

        String previousAccessionNumber = "";

        for (Sample sample : samples) {
            if (!previousAccessionNumber.equals(sample.getAccessionNumber())) {
                filteredSamples.add(sample);
                previousAccessionNumber = sample.getAccessionNumber();
            }
        }

        return filteredSamples;
    }

    private List<NonConformityReportData> createNonconformityItem(Sample sample) {

        List<NonConformityReportData> items = new ArrayList<>();

        Patient patient = ReportUtil.findPatient(sample);
        Project project = ReportUtil.findProject(sample);

        String sampleAccessionNumber = sample.getAccessionNumber();
        String receivedDate = sample.getReceivedDateForDisplay();
        String receivedHour = sample.getReceivedTimeForDisplay();
        String doctor = ReportUtil.findDoctorForSample(sample);

        String orgName = "";
        SampleOrganization sampleOrg = sampleOrgService.getDataBySample(sample);
        if (sampleOrg != null && sampleOrg.getOrganization() != null) {
            orgName = sampleOrg.getOrganization().getOrganizationName();
        }

        List<SampleQaEvent> sampleQaEvents = sampleQAService.getSampleQaEventsBySample(sample);

        for (SampleQaEvent event : sampleQaEvents) {
            if (eventPrintable(sampleAccessionNumber, event)) {
                NonConformityReportData item = new NonConformityReportData();
                QAService qa = new QAService(event);
                if (AccessionFormat.ALPHANUM.toString()
                        .equals(ConfigurationProperties.getInstance().getPropertyValue(Property.AccessionFormat))) {
                    item.setAccessionNumber(
                            AlphanumAccessionValidator.convertAlphaNumLabNumForDisplay(sampleAccessionNumber));
                } else {
                    item.setAccessionNumber(sampleAccessionNumber);
                }
                item.setReceivedDate(receivedDate);
                item.setReceivedHour(receivedHour);
                item.setService(orgName);

                item.setBiologist(qa.getObservationForDisplay(QAObservationType.AUTHORIZER));
                item.setNonConformityDate(DateUtil.convertTimestampToStringDate(qa.getLastupdated()));
                item.setSection(qa.getObservationForDisplay(QAObservationType.SECTION));
                item.setSubjectNumber(patient.getNationalId());
                item.setSiteSubjectNumber(patient.getExternalId());
                item.setStudy((project != null) ? project.getLocalizedName() : "");

                item.setSampleType(ReportUtil.getSampleType(event));
                item.setQaNote(NonConformityHelper.getNoteForSampleQaEvent(event));
                item.setSampleNote(NonConformityHelper.getNoteForSample(sample));
                item.setNonConformityReason(qa.getQAEvent().getLocalizedName());
                item.setDoctor(doctor);
                items.add(item);

                sampleQaEventIds.add(qa.getEventId());
                if (sampleAccessionNumber.equals(requestedAccessionNumber)) {
                    checkIdsForPriorPrintRecord.add(qa.getEventId());
                }
            }
        }

        return items;
    }

    private boolean eventPrintable(String sampleAccessionNumber, SampleQaEvent event) {
        if (sampleAccessionNumber.equals(requestedAccessionNumber)) {
            return true;
        }

        return !ReportUtil.documentHasBeenPrinted(ReportUtil.DocumentTypes.NON_CONFORMITY_NOTIFCATION, event.getId());
    }

    @Override
    public JRDataSource getReportDataSource() throws IllegalStateException {
        if (errorFound) {
            return new JRBeanCollectionDataSource(errorMsgs);
        } else {
            ReportUtil.markDocumentsAsPrinted(ReportUtil.DocumentTypes.NON_CONFORMITY_NOTIFCATION, sampleQaEventIds,
                    "1", checkIdsForPriorPrintRecord);
            return new JRBeanCollectionDataSource(reportItems);
        }
    }

    @Override
    protected String reportFileName() {
        return "NonConformityNotification";
    }
}
