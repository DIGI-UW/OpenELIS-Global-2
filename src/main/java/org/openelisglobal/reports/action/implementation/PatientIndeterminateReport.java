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
 * <p>Copyright (C) CIRG, University of Washington, Seattle WA. All Rights Reserved.
 */
package org.openelisglobal.reports.action.implementation;

import java.util.ArrayList;
import java.util.List;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.reports.action.implementation.reportBeans.IndeterminateReportData;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.sampleorganization.service.SampleOrganizationService;
import org.openelisglobal.sampleorganization.valueholder.SampleOrganization;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestServiceImpl;

public abstract class PatientIndeterminateReport extends RetroCIPatientReport {

    protected List<IndeterminateReportData> reportItems;

    private SampleOrganizationService orgService = SpringContext.getBean(SampleOrganizationService.class);
    private AnalysisService analysisService = SpringContext.getBean(AnalysisService.class);
    private DictionaryService dictionaryService = SpringContext.getBean(DictionaryService.class);
    private ResultService resultService = SpringContext.getBean(ResultService.class);

    @Override
    protected String getReportNameForReport() {
        return MessageUtil.getMessage("reports.label.patient.indeterminate");
    }

    @Override
    public JRDataSource getReportDataSource() throws IllegalStateException {
        if (!initialized) {
            throw new IllegalStateException("initializeReport not called first");
        }

        return errorFound ? new JRBeanCollectionDataSource(errorMsgs) : new JRBeanCollectionDataSource(reportItems);
    }

    @Override
    protected void createReportItems() {
        IndeterminateReportData data = new IndeterminateReportData();

        setPatientInfo(data);
        setTestInfo(data);

        reportItems.add(data);
    }

    protected void setPatientInfo(IndeterminateReportData data) {

        String subjectNumber = reportPatient.getNationalId();
        if (GenericValidator.isBlankOrNull(subjectNumber)) {
            subjectNumber = reportPatient.getExternalId();
        }
        data.setSubjectNumber(subjectNumber);
        data.setBirth_date(reportPatient.getBirthDateForDisplay());
        data.setAge(DateUtil.getCurrentAgeForDate(reportPatient.getBirthDate(), reportSample.getCollectionDate()));
        data.setGender(reportPatient.getGender());
        data.setCollectiondate(
                reportSample.getCollectionDateForDisplay() + " " + reportSample.getCollectionTimeForDisplay());
        data.setReceivedDate(reportSample.getReceivedDateForDisplay() + " " + reportSample.getReceivedTimeForDisplay());

        SampleOrganization sampleOrg = new SampleOrganization();
        sampleOrg.setSample(reportSample);
        orgService.getDataBySample(sampleOrg);
        data.setOrgname(sampleOrg.getId() == null ? "" : sampleOrg.getOrganization().getOrganizationName());

        data.setDoctor(getObservationValues(OBSERVATION_DOCTOR_ID));
        data.setLabNo(reportSample.getAccessionNumber());
    }

    protected void setTestInfo(IndeterminateReportData data) {
        boolean atLeastOneAnalysisNotValidated = false;

        List<Analysis> analysisList = analysisService.getAnalysesBySampleId(reportSample.getId());
        String invalidValue = MessageUtil.getMessage("report.test.status.inProgress");

        for (Analysis analysis : analysisList) {
            String testName = TestServiceImpl.getUserLocalizedTestName(analysis.getTest());

            List<Result> resultList = resultService.getResultsByAnalysis(analysis);
            String resultValue = null;

            boolean valid = ANALYSIS_FINALIZED_STATUS_ID.equals(analysis.getStatusId());

            if (!valid) {
                atLeastOneAnalysisNotValidated = true;
                data.setFinalResult(invalidValue);
            }
            // there may be more than one result for an analysis if one of
            // them
            // is a conclusion
            if (resultList.size() > 1) {
                for (Result result : resultList) {

                    Dictionary dictionary = new Dictionary();
                    dictionary.setId(result.getValue());
                    dictionaryService.getData(dictionary);

                    if (result.getAnalyte() != null && result.getAnalyte().getId().equals(CONCLUSION_ID)) {
                        data.setFinalResult(valid ? dictionary.getDictEntry() : invalidValue);
                    } else {
                        resultValue = valid ? dictionary.getDictEntry() : invalidValue;
                    }
                }
            } else if (valid && resultList.size() > 0) {
                Dictionary dictionary = new Dictionary();
                dictionary.setId(resultList.get(0).getValue());
                dictionaryService.getData(dictionary);
                resultValue = dictionary.getDictEntry();
            }

            setIndeterminateData(data, testName, valid ? resultValue : invalidValue);
        }

        data.setStatus(atLeastOneAnalysisNotValidated ? MessageUtil.getMessage("report.status.partial")
                : MessageUtil.getMessage("report.status.complete"));
    }

    private void setIndeterminateData(IndeterminateReportData data, String testName, String resultValue) {
        if (testName.equals("Integral")) {
            data.setIntegral(resultValue);
        } else if (testName.equals("Genscreen")) {
            data.setGenscreen(resultValue);
        } else if (testName.equals("Murex")) {
            data.setMurex(resultValue);
        } else if (testName.equals("Murex Combinaison")) {
            data.setMurex(resultValue);
        } else if (testName.equals("Vironostika")) {
            data.setVironstika(resultValue);
        } else if (testName.equals("Genie II")) {
            data.setGenie_hiv1_hiv2(resultValue);
        } else if (testName.equals("Western Blot 1")) {
            data.setWb1(resultValue);
        } else if (testName.equals("Western Blot 2")) {
            data.setWb2(resultValue);
        } else if (testName.equals("P24 Ag")) {
            data.setP24(resultValue);
        } else if (testName.equals("DNA PCR")) {
            data.setPcr(resultValue);
        } else if (testName.equals("Genie II 10")) {
            data.setGenie10(resultValue);
        } else if (testName.equals("Genie II 100")) {
            data.setGenie100(resultValue);
        } else if (testName.equals("Bioline")) {
            data.setBioline(resultValue);
        }
    }

    @Override
    protected void initializeReportItems() {
        reportItems = new ArrayList<>();
    }

    @Override
    protected String getProjectId() {
        return INDETERMINATE_STUDY_ID;
    }
}
