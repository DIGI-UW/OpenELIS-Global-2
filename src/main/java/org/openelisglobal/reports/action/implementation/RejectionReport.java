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
 * Copyright (C) ITECH, University of Washington, Seattle WA.  All Rights Reserved.
 */

package org.openelisglobal.reports.action.implementation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.provider.validation.AccessionNumberValidatorFactory.AccessionFormat;
import org.openelisglobal.common.provider.validation.AlphanumAccessionValidator;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.common.util.PdfExportSupport;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.reports.action.implementation.reportBeans.RejectionReportBean;
import org.openelisglobal.reports.form.ReportForm;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.util.AccessionNumberUtil;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestServiceImpl;

public abstract class RejectionReport extends Report implements IReportCreator {
    private int PREFIX_LENGTH = AccessionNumberUtil.getMainAccessionNumberGenerator().getInvarientLength();
    protected List<RejectionReportBean> rejections;
    protected DateRange dateRange;

    protected AnalysisService analysisService = SpringContext.getBean(AnalysisService.class);

    @Override
    protected byte[] renderReport() {
        String labNumberTitle = MessageUtil.getContextualMessage("quick.entry.accession.number");
        String prefix = AccessionNumberUtil.getMainAccessionNumberGenerator().getPrefix();
        List<String> headers = Arrays.asList(MessageUtil.getMessage("barcode.label.info.collectionDate"),
                MessageUtil.getMessage("report.receptionDate"),
                GenericValidator.isBlankOrNull(prefix) ? labNumberTitle : labNumberTitle + " (" + prefix + ")",
                isReportByTest() ? MessageUtil.getMessage("report.patientCode")
                        : MessageUtil.getMessage("report.patientCode") + " / "
                                + MessageUtil.getMessage("report.testName"),
                MessageUtil.getMessage("report.reasonForRejection"), MessageUtil.getMessage("report.techId"));
        List<List<String>> rows = new ArrayList<>();
        RejectionReportBean above = new RejectionReportBean();
        for (RejectionReportBean item : rejections) {
            boolean firstOfOrder = !Objects.equals(item.getAccessionNumber(), above.getAccessionNumber());
            rows.add(Arrays.asList(item.getCollectionDate(),
                    ManagementReportPdf.whenChanged(item.getReceivedDate(), above.getReceivedDate(), firstOfOrder),
                    firstOfOrder ? item.getAccessionNumber() : "",
                    GenericValidator.isBlankOrNull(item.getPatientOrTestName())
                            ? MessageUtil.getMessage("report.patient.not.registered")
                            : item.getPatientOrTestName(),
                    item.getRejectionReason(), item.getTechnician()));
            above = item;
        }
        return ManagementReportPdf.render(PdfExportSupport.pageSize(), MessageUtil.getMessage("reject.report"),
                getActivityLabel(), dateRange, headers, new float[] { 1.2f, 1.2f, 1.8f, 2.6f, 2.2f, 1.2f }, rows);
    }

    protected boolean isReportByTest() {
        return Boolean.FALSE;
    }

    protected abstract String getActivityLabel();

    protected abstract void buildReportContent(ReportSpecificationList testSelection);

    @Override
    public void initializeReport(ReportForm form) {
        initialized = true;
        ReportSpecificationList selection = form.getSelectList();
        dateRange = new DateRange(form.getLowerDateRange(), form.getUpperDateRange());

        errorFound = !validateSubmitParameters(selection);
        if (errorFound) {
            return;
        }

        buildReportContent(selection);
        if (rejections.size() == 0) {
            add1LineErrorMessage("report.error.message.noPrintableItems");
        }
    }

    private boolean validateSubmitParameters(ReportSpecificationList selectList) {

        return (dateRange.validateHighLowDate("report.error.message.date.received.missing")
                && validateSelection(selectList));
    }

    private boolean validateSelection(ReportSpecificationList selectList) {
        boolean complete = !GenericValidator.isBlankOrNull(selectList.getSelection())
                && !"0".equals(selectList.getSelection());

        if (!complete) {
            add1LineErrorMessage("report.error.message.activity.missing");
        }

        return complete;
    }

    protected RejectionReportBean createRejectionReportBean(String noteText, Analysis analysis, boolean useTestName) {
        RejectionReportBean item = new RejectionReportBean();

        SampleService sampleService = SpringContext.getBean(SampleService.class);
        Sample sample = analysis.getSampleItem().getSample();
        PatientService patientService = SpringContext.getBean(PatientService.class);
        SampleHumanService sampleHumanService = SpringContext.getBean(SampleHumanService.class);
        Patient patient = sampleHumanService.getPatientForSample(sample);

        List<Result> results = analysisService.getResults(analysis);
        for (Result result : results) {
            ResultService resultResultService = SpringContext.getBean(ResultService.class);
            String signature = resultResultService.getSignature(result);
            if (!GenericValidator.isBlankOrNull(signature)) {
                item.setTechnician(signature);
                break;
            }
        }
        if (AccessionFormat.ALPHANUM.toString()
                .equals(ConfigurationProperties.getInstance().getPropertyValue(Property.AccessionFormat))) {
            item.setAccessionNumber(AlphanumAccessionValidator.convertAlphaNumLabNumForDisplay(
                    sampleService.getAccessionNumber(sample).substring(PREFIX_LENGTH)));
        } else {
            item.setAccessionNumber(sampleService.getAccessionNumber(sample).substring(PREFIX_LENGTH));
        }
        item.setReceivedDate(sampleService.getTwoYearReceivedDateForDisplay(sample));
        item.setCollectionDate(
                DateUtil.convertTimestampToTwoYearStringDate(analysis.getSampleItem().getCollectionDate()));
        item.setRejectionReason(noteText);

        StringBuilder nameBuilder = new StringBuilder(patientService.getLastName(patient).toUpperCase());
        if (!GenericValidator.isBlankOrNull(patientService.getNationalId(patient))) {
            if (nameBuilder.length() > 0) {
                nameBuilder.append(" / ");
            }
            nameBuilder.append(patientService.getNationalId(patient));
        }

        if (useTestName) {
            item.setPatientOrTestName(TestServiceImpl.getUserLocalizedTestName(analysisService.getTest(analysis)));
            item.setNonPrintingPatient(nameBuilder.toString());
        } else {
            item.setPatientOrTestName(nameBuilder.toString());
        }

        return item;
    }

    protected RejectionReportBean createIdentityRejectionBean(RejectionReportBean item, boolean blankCollectionDate) {
        RejectionReportBean filler = new RejectionReportBean();

        filler.setAccessionNumber(item.getAccessionNumber());
        filler.setReceivedDate(item.getReceivedDate());
        filler.setCollectionDate(blankCollectionDate ? " " : item.getCollectionDate());
        filler.setPatientOrTestName(item.getNonPrintingPatient());

        return filler;
    }

    protected String getNameForId(ReportSpecificationList list) {

        String selection = list.getSelection();

        for (IdValuePair pair : list.getList()) {
            if (selection.equals(pair.getId())) {
                return pair.getValue();
            }
        }

        return "";
    }

    protected void injectPatientLineAndCopyToFinalList(ArrayList<RejectionReportBean> rawResults) {
        Collections.sort(rawResults, new Comparator<RejectionReportBean>() {
            @Override
            public int compare(RejectionReportBean o1, RejectionReportBean o2) {
                int sortResult = o1.getAccessionNumber().compareTo(o2.getAccessionNumber());
                return sortResult == 0 ? o1.getPatientOrTestName().compareTo(o2.getPatientOrTestName()) : sortResult;
            }
        });

        String currentAccessionNumber = "";
        for (RejectionReportBean item : rawResults) {
            if (!currentAccessionNumber.equals(item.getAccessionNumber())) {
                rejections.add(createIdentityRejectionBean(item, false));
                currentAccessionNumber = item.getAccessionNumber();
            }
            item.setCollectionDate(null);
            rejections.add(item);
        }
    }
}
