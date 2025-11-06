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

import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.analyte.service.AnalyteService;
import org.openelisglobal.analyte.valueholder.Analyte;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.MathUtil;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.reports.action.implementation.reportBeans.HaitiHIVSummaryData;
import org.openelisglobal.reports.form.ReportForm;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestServiceImpl;

public class IndicatorHIV extends IndicatorReport implements IReportCreator, IReportParameterSetter {

    private static final long INFANT_TIME = 1000L * 60L * 60L * 24L * 28L;
    protected static final String HIV = "VCT";
    protected static String HIV_POSITIVE_ID;
    protected static String HIV_NEGATIVE_ID;
    protected static String HIV_INDETERMINATE_ID;
    protected static String TEST_HIV_POS_ID;
    protected static String TEST_HIV_NEG_ID;
    protected static String TEST_HIV_IND_ID;
    protected static String CLINICAL_POSITIVE_ID;
    protected static String CLINICAL_NEGATIVE_ID;
    protected static String ANALYTE_CONCULSION_ID;
    protected List<HaitiHIVSummaryData> testData;
    protected List<Analysis> analysisList;
    protected static List<String> HIV_TESTS;

    private SampleHumanService sampleHumanService = SpringContext.getBean(SampleHumanService.class);
    private ResultService resultService = SpringContext.getBean(ResultService.class);
    private AnalysisService analysisService = SpringContext.getBean(AnalysisService.class);
    private static AnalyteService analyteService = SpringContext.getBean(AnalyteService.class);
    private static DictionaryService dictionaryService = SpringContext.getBean(DictionaryService.class);

    static {
        Dictionary dictionary = dictionaryService.getDictionaryEntrysByNameAndCategoryDescription("Positive",
                "Conclusion");
        if (dictionary != null) {
            HIV_POSITIVE_ID = dictionary.getId();
        }

        dictionary = dictionaryService.getDictionaryEntrysByNameAndCategoryDescription("Negative", "Conclusion");
        if (dictionary != null) {
            HIV_NEGATIVE_ID = dictionary.getId();
        }

        dictionary = dictionaryService.getDictionaryEntrysByNameAndCategoryDescription("Indeterminate", "Conclusion");
        if (dictionary != null) {
            HIV_INDETERMINATE_ID = dictionary.getId();
        }

        dictionary = dictionaryService.getDictionaryEntrysByNameAndCategoryDescription("POSITIF", "Haiti Lab");
        if (dictionary != null) {
            TEST_HIV_POS_ID = dictionary.getId();
        } else {

            dictionary = dictionaryService.getDictionaryEntrysByNameAndCategoryDescription("Positif", "Haiti Lab");
            if (dictionary != null) {
                TEST_HIV_POS_ID = dictionary.getId();
            }
        }

        dictionary = dictionaryService.getDictionaryEntrysByNameAndCategoryDescription("NEGATIF", "Haiti Lab");
        if (dictionary != null) {
            TEST_HIV_NEG_ID = dictionary.getId();
        } else {
            dictionary = dictionaryService.getDictionaryEntrysByNameAndCategoryDescription("Negatif", "Haiti Lab");
            if (dictionary != null) {
                TEST_HIV_NEG_ID = dictionary.getId();
            }
        }

        dictionary = dictionaryService.getDictionaryEntrysByNameAndCategoryDescription("INDETERMINE", "Haiti Lab");
        if (dictionary != null) {
            TEST_HIV_IND_ID = dictionary.getId();
        } else {
            dictionary = dictionaryService.getDictionaryEntrysByNameAndCategoryDescription("Indeterminé", "Haiti Lab");
            if (dictionary != null) {
                TEST_HIV_IND_ID = dictionary.getId();
            }
        }

        dictionary = dictionaryService.getDictionaryEntrysByNameAndCategoryDescription("Positif", "CLINICAL GENERAL");
        if (dictionary != null) {
            CLINICAL_POSITIVE_ID = dictionary.getId();
        }

        dictionary = dictionaryService.getDictionaryEntrysByNameAndCategoryDescription("Négatif", "CLINICAL GENERAL");
        if (dictionary != null) {
            CLINICAL_NEGATIVE_ID = dictionary.getId();
        }

        HIV_TESTS = new ArrayList<>();
        HIV_TESTS.add("CD4 en mm3");
        HIV_TESTS.add("CD4 en %");
        HIV_TESTS.add("Colloidal Gold / Shangai Kehua");
        HIV_TESTS.add("Determine");
        HIV_TESTS.add("HIV test rapide");
        HIV_TESTS.add("VIH Test - Oraquick");
        HIV_TESTS.add("Test de VIH");
        HIV_TESTS.add("Test rapide HIV 1 + HIV 2");
        HIV_TESTS.add("Dénombrement des lymphocytes CD4 (mm3)");
        HIV_TESTS.add("Dénombrement des lymphocytes  CD4 (%)");

        Analyte analyte = new Analyte();
        analyte.setAnalyteName("Conclusion");
        analyte = analyteService.getAnalyteByName(analyte, false);
        ANALYTE_CONCULSION_ID = analyte.getId();
    }

    @Override
    protected String reportFileName() {
        return "HIVSummary";
    }

    @Override
    public JRDataSource getReportDataSource() throws IllegalStateException {

        return errorFound ? new JRBeanCollectionDataSource(errorMsgs) : new JRBeanCollectionDataSource(testData);
    }

    @Override
    public void initializeReport(ReportForm form) {
        super.initializeReport();
        setDateRange(form);

        findAnalysis();

        setReportParameters();

        setHIVByTest();
    }

    protected void findAnalysis() {
        analysisList = analysisService.getAnalysisByTestNamesAndCompletedDateRange(HIV_TESTS, lowDate, highDate);
    }

    protected void setHIVByTest() {
        testData = new ArrayList<>();

        Map<String, TestBucket> testBuckets = new HashMap<>();

        for (Analysis analysis : analysisList) {

            String testName = TestServiceImpl.getUserLocalizedTestName(analysis.getTest());

            TestBucket bucket = testBuckets.get(testName);

            if (bucket == null) {
                bucket = new TestBucket(testName);
                testBuckets.put(testName, bucket);
            }

            List<Result> resultList = resultService.getResultsByAnalysis(analysis);

            if (resultList.isEmpty()) {
                continue;
            }

            Result firstResult = resultList.get(0);

            if (testName.equals("CD4 en mm3") || testName.equals("CD4 en %") || testName.equals("CD4  Compte Abs")
                    || testName.equals("CD4 Compte en %") || testName.equals("Dénombrement des lymphocytes CD4 (mm3)")
                    || testName.equals("Dénombrement des lymphocytes  CD4 (%)")) {
                if (MathUtil.isEqual(firstResult.getMinNormal(), firstResult.getMaxNormal())) {
                    continue;
                }

                try {
                    Double value = Double.valueOf(firstResult.getValue(true));

                    if (value >= firstResult.getMinNormal() && value <= firstResult.getMaxNormal()) {
                        bucket.negative++;
                    } else {
                        bucket.positive++;
                    }
                } catch (NumberFormatException e) {
                    continue;
                }

            } else if (testName.equals("Colloidal Gold / Shangai Kehua") || testName.equals("Determine")
                    || testName.equals("HIV test rapide") || testName.equals("Test Rapide VIH")
                    || testName.equals("Test rapide HIV 1 + HIV 2")) {
                String value = firstResult.getValue(true);
                if (isPositive(value)) {
                    bucket.positive++;
                } else if (TEST_HIV_NEG_ID.equals(value)) {
                    bucket.negative++;
                } else if (isIndeterminate(value)) {
                    bucket.indeterminate++;
                }

            } else if (testName.equals("Test de VIH")) {
                Result result = resultList.get(0);
                String analyteName = result.getAnalyte() == null ? "" : result.getAnalyte().getAnalyteName();

                if ("Result 1".equals(analyteName)) {
                    if (TEST_HIV_NEG_ID.equals(result.getValue(true))) {
                        bucket.negative++;
                    } else {
                        bucket.pending++;
                    }
                } else {
                    bucket.pending--;
                    if (TEST_HIV_NEG_ID.equals(result.getValue(true))) {
                        bucket.negative++;
                    } else if (TEST_HIV_POS_ID.equals(result.getValue(true))) {
                        bucket.positive++;
                    }
                    if (TEST_HIV_IND_ID.equals(result.getValue(true))) {
                        bucket.indeterminate++;
                    }
                }
            } else if (testName.equals("Oraquick")) {
                Result result = resultList.get(0);
                if (CLINICAL_POSITIVE_ID.equals(result.getValue(true))) {
                    bucket.positive++;
                } else if (CLINICAL_NEGATIVE_ID.equals(result.getValue(true))) {
                    bucket.negative++;
                }
            }
        }

        for (TestBucket testBucket : testBuckets.values()) {

            HaitiHIVSummaryData data = new HaitiHIVSummaryData();
            double total = testBucket.pending + testBucket.indeterminate + testBucket.positive + testBucket.negative;
            data.setPending(testBucket.pending);
            data.setIndeterminate(testBucket.indeterminate);
            data.setPositive(testBucket.positive);
            data.setNegative(testBucket.negative);
            data.setTestName(testBucket.testName);
            data.setTotal((int) total);

            if (total > 0) {
                data.setPendingPer(percentage(total, testBucket.pending));
                data.setIndeterminatePer(percentage(total, testBucket.indeterminate));
                data.setPositivePer(percentage(total, testBucket.positive));
                data.setNegativePer(percentage(total, testBucket.negative));
            }

            testData.add(data);
        }
    }

    protected boolean isIndeterminate(String value) {
        return TEST_HIV_IND_ID.equals(value);
    }

    protected boolean isPositive(String value) {
        return TEST_HIV_POS_ID.equals(value);
    }

    protected double percentage(double total, int value) {
        return value == 0 ? 0.0 : ((int) (value / total * 10000.0)) / 100.0;
    }

    protected void setReportParameters() {
        super.createReportParameters();

        Set<String> patientSet = new HashSet<>();
        List<PatientTestDate> patientTestList = new ArrayList<>();

        for (Analysis analysis : analysisList) {
            Patient patient = sampleHumanService.getPatientForSample(analysis.getSampleItem().getSample());
            if (!patientSet.contains(patient.getId())) {
                patientSet.add(patient.getId());
                patientTestList.add(new PatientTestDate(patient, analysis.getCompletedDate()));
            }
        }
        // This is dependent on the outcome of the results
        int male = 0;
        int female = 0;
        int infant = 0;

        for (PatientTestDate patientTestDate : patientTestList) {
            if ("M".equals(patientTestDate.patient.getGender())) {
                male++;
            } else {
                female++;
            }

            if (patientIsEnfant(patientTestDate)) {
                infant++;
            }
        }

        reportParameters.put("male", String.valueOf(male));
        reportParameters.put("female", String.valueOf(female));
        reportParameters.put("infant", String.valueOf(infant));
        reportParameters.put("populationTotal", String.valueOf(patientSet.size()));
    }

    private boolean patientIsEnfant(PatientTestDate patientTestDate) {
        if (patientTestDate.patient == null || patientTestDate.patient.getBirthDate() == null) {
            return false;
        }

        long birthDate = patientTestDate.patient.getBirthDate().getTime();
        long testDate = patientTestDate.testDate.getTime();

        return (testDate - birthDate) <= INFANT_TIME;
    }

    private class PatientTestDate {
        public Patient patient;
        public Date testDate;

        public PatientTestDate(Patient patient, Date testDate) {
            this.patient = patient;
            this.testDate = testDate;
        }
    }

    class TestBucket {
        public String testName;
        public int positive = 0;
        public int negative = 0;
        public int indeterminate = 0;
        public int pending = 0;

        public TestBucket(String name) {
            testName = name;
        }
    }

    @Override
    protected String getNameForReportRequest() {
        return MessageUtil.getContextualMessage("openreports.hiv.aggregate");
    }

    @Override
    protected String getNameForReport() {
        return MessageUtil.getMessage("openreports.hiv.aggregate");
    }

    @Override
    protected String getLabNameLine1() {
        return ConfigurationProperties.getInstance().getPropertyValue(Property.SiteName);
    }

    @Override
    protected String getLabNameLine2() {
        return "";
    }
}
