package org.openelisglobal.report.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.provider.service.ProviderService;
import org.openelisglobal.provider.valueholder.Provider;
import org.openelisglobal.report.ReportColumn;
import org.openelisglobal.report.ReportDefinitionColumnParser;
import org.openelisglobal.report.ReportRow;
import org.openelisglobal.report.ReportingData;
import org.openelisglobal.report.service.PatientReportService;
import org.openelisglobal.reportdefinition.service.ReportDefinitionService;
import org.openelisglobal.reportdefinition.valueholder.ReportDefinition;
import org.openelisglobal.result.action.util.ResultsLoadUtility;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.samplehuman.valueholder.SampleHuman;
import org.openelisglobal.sampleorganization.service.SampleOrganizationService;
import org.openelisglobal.sampleorganization.valueholder.SampleOrganization;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.beanItems.TestResultItem;
import org.openelisglobal.test.service.TestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** Report type constant for patient results report definition lookup. */
final class PatientReportConstants {
    static final String REPORT_TYPE_PATIENT = "PATIENT";
}

/**
 * Default implementation of {@link PatientReportService}. Column definitions
 * are read from report_definition when report_type=PATIENT; otherwise a default
 * column set is used.
 */
@Service
public class PatientReportServiceImpl implements PatientReportService {

    @Autowired
    private PatientService patientService;

    @Autowired
    private SampleService sampleService;

    @Autowired
    private SampleHumanService sampleHumanService;

    @Autowired
    private SampleOrganizationService sampleOrganizationService;

    @Autowired
    private ProviderService providerService;

    @Autowired
    private ReportDefinitionService reportDefinitionService;

    @Autowired
    private TestService testService;

    @Autowired
    private org.openelisglobal.systemuser.service.UserService userService;

    @Override
    public ReportingData buildPatientResultsReport(String patientId, String sysUserId) {
        if (sysUserId == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED, "Unauthorized");
        }

        Patient patient = patientService.getData(patientId);
        if (patient == null) {
            return null;
        }

        ResultsLoadUtility resultsUtility = SpringContext.getBean(ResultsLoadUtility.class);
        resultsUtility.setSysUser(sysUserId);

        List<TestResultItem> results = resultsUtility.getGroupedTestsForPatient(patient);
        results = userService.filterResultsByLabUnitRoles(sysUserId, results,
                org.openelisglobal.common.constants.Constants.ROLE_RESULTS);

        List<ReportColumn> columns = resolveColumns();
        return mapToReportingData(results, patient, columns);
    }

    /**
     * Resolve report columns from the active PATIENT report definition, or return
     * default columns if none is configured.
     */
    private List<ReportColumn> resolveColumns() {
        ReportDefinition definition = reportDefinitionService
                .getActiveByReportType(PatientReportConstants.REPORT_TYPE_PATIENT);
        if (definition != null && definition.getDefinitionJson() != null) {
            List<ReportColumn> parsed = ReportDefinitionColumnParser.parseColumns(definition.getDefinitionJson());
            if (!parsed.isEmpty()) {
                return parsed;
            }
        }
        return getDefaultPatientReportColumns();
    }

    private static List<ReportColumn> getDefaultPatientReportColumns() {
        List<ReportColumn> columns = new ArrayList<>();
        columns.add(new ReportColumn("accessionNumber", "Accession Number", "String"));
        columns.add(new ReportColumn("patientName", "Patient Name", "String"));
        columns.add(new ReportColumn("patientExternalId", "External ID", "String"));
        columns.add(new ReportColumn("patientGender", "Gender", "String"));
        columns.add(new ReportColumn("patientDateOfBirth", "Date of Birth", "String"));
        columns.add(new ReportColumn("organizationName", "Organization Name", "String"));
        columns.add(new ReportColumn("sampleCollectionDate", "Collection Date", "String"));
        columns.add(new ReportColumn("sampleReceivedDate", "Received Date", "String"));
        columns.add(new ReportColumn("clinicianName", "Clinician Name", "String"));
        columns.add(new ReportColumn("testName", "Test Name", "String"));
        columns.add(new ReportColumn("testDescription", "Test Description", "String"));
        columns.add(new ReportColumn("analysisStatus", "Status", "String"));
        columns.add(new ReportColumn("resultValue", "Result Value", "String"));
        return columns;
    }

    private ReportingData mapToReportingData(List<TestResultItem> results, Patient patient,
            List<ReportColumn> columns) {
        ReportingData data = new ReportingData();
        data.setColumns(columns);

        // Pre-fetch related metadata to avoid N+1 queries
        java.util.Map<String, String> orgNameMap = new java.util.HashMap<>();
        java.util.Map<String, String> clinicianMap = new java.util.HashMap<>();
        java.util.Map<String, String> collectionDateMap = new java.util.HashMap<>();

        java.util.Set<String> uniqueAccessions = results.stream()
                .filter(item -> !item.getIsGroupSeparator() && item.getAccessionNumber() != null)
                .map(TestResultItem::getAccessionNumber).collect(java.util.stream.Collectors.toSet());

        for (String accNum : uniqueAccessions) {
            Sample sample = new Sample();
            sample.setAccessionNumber(accNum);
            sampleService.getSampleByAccessionNumber(sample);

            if (sample.getId() != null) {
                SampleOrganization sampleOrg = new SampleOrganization();
                sampleOrg.setSample(sample);
                sampleOrganizationService.getDataBySample(sampleOrg);
                if (sampleOrg.getOrganization() != null) {
                    orgNameMap.put(accNum, sampleOrg.getOrganization().getOrganizationName());
                }

                SampleHuman sampleHuman = new SampleHuman();
                sampleHuman.setSampleId(sample.getId());
                sampleHumanService.getDataBySample(sampleHuman);
                if (sampleHuman.getProviderId() != null) {
                    Provider provider = new Provider();
                    provider.setId(sampleHuman.getProviderId());
                    providerService.getData(provider);
                    if (provider.getPerson() != null) {
                        clinicianMap.put(accNum,
                                provider.getPerson().getLastName() + ", " + provider.getPerson().getFirstName());
                    }
                }

                collectionDateMap.put(accNum, sample.getCollectionDateForDisplay());
            }
        }

        // Define Rows
        List<ReportRow> rows = new ArrayList<>();

        // creating and populating a set to store all testids
        Set<String> testIds = new HashSet<>();
        for (TestResultItem item : results) {
            String testid = item.getTestId();
            if (testid != null) {
                testIds.add(testid);
            }
        }

        // fetching all tests by testids
        List<Test> tests = testService.getTestsByIds(testIds);

        // storing all tests in a map for O(1) lookup
        Map<String, Test> testsMap = new HashMap<>();
        for (Test test : tests) {
            testsMap.put(test.getId(), test);
        }

        for (TestResultItem item : results) {
            if (item.getIsGroupSeparator()) {
                continue;
            }

            ReportRow row = new ReportRow();
            String accNum = item.getAccessionNumber();
            String orgName = orgNameMap.getOrDefault(accNum, "");
            String clinician = clinicianMap.getOrDefault(accNum, "");
            String collectionDate = collectionDateMap.getOrDefault(accNum, "");

            // Build row data by column key so order matches definition
            for (ReportColumn col : columns) {
                String value = getCellValue(col.getKey(), item, patient, orgName, clinician, collectionDate, testsMap);
                row.addData(col.getKey(), value);
                row.addCell(value);
            }
            rows.add(row);
        }
        data.setRows(rows);
        return data;
    }

    private String getCellValue(String key, TestResultItem item, Patient patient, String orgName, String clinician,
            String collectionDate, Map<String, Test> testsMap) {
        if (key == null) {
            return "";
        }
        switch (key) {
            case "accessionNumber":
                return item.getAccessionNumber() != null ? item.getAccessionNumber() : "";
            case "patientName":
                return item.getPatientName() != null ? item.getPatientName() : "";
            case "patientExternalId":
                return patient.getExternalId() != null ? patient.getExternalId() : "";
            case "patientGender":
                return patient.getGender() != null ? patient.getGender() : "";
            case "patientDateOfBirth":
                return patient.getBirthDateForDisplay() != null ? patient.getBirthDateForDisplay() : "";
            case "organizationName":
                return orgName != null ? orgName : "";
            case "sampleCollectionDate":
                return collectionDate != null ? collectionDate : "";
            case "sampleReceivedDate":
                return item.getReceivedDate() != null ? item.getReceivedDate() : "";
            case "clinicianName":
                return clinician != null ? clinician : "";
            case "testName":
                return item.getTestName() != null ? item.getTestName() : "";
            case "testDescription":
                if (item.getTestId() != null) {
                    Test test = testsMap.get(item.getTestId());
                    if (test != null && test.getDescription() != null) {
                        return test.getDescription();
                    }
                }
                return "";
            case "analysisStatus":
                return item.getAnalysisStatusId() != null ? item.getAnalysisStatusId() : "";
            case "resultValue":
                return item.getResultValue() != null ? item.getResultValue() : "";
            default:
                return "";
        }
    }
}
