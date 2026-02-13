package org.openelisglobal.report.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.provider.service.ProviderService;
import org.openelisglobal.provider.valueholder.Provider;
import org.openelisglobal.report.ReportColumn;
import org.openelisglobal.report.ReportRow;
import org.openelisglobal.report.ReportingData;
import org.openelisglobal.result.action.util.ResultsLoadUtility;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.samplehuman.valueholder.SampleHuman;
import org.openelisglobal.sampleorganization.service.SampleOrganizationService;
import org.openelisglobal.sampleorganization.valueholder.SampleOrganization;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.beanItems.TestResultItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/reports/patient")
public class PatientReportRestController extends BaseRestController {

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

    @GetMapping("/{patientId}/results")
    public ResponseEntity<ReportingData> getPatientResults(@PathVariable String patientId, HttpServletRequest request) {
        Patient patient = patientService.getData(patientId);
        if (patient == null) {
            return ResponseEntity.notFound().build();
        }

        ResultsLoadUtility resultsUtility = SpringContext.getBean(ResultsLoadUtility.class);
        resultsUtility.setSysUser(getSysUserId(request));

        List<TestResultItem> results = resultsUtility.getGroupedTestsForPatient(patient);

        ReportingData response = mapToReportingData(results, patient);
        return ResponseEntity.ok(response);
    }

    private ReportingData mapToReportingData(List<TestResultItem> results, Patient patient) {
        ReportingData data = new ReportingData();

        // Define Canonical Columns
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
        data.setColumns(columns);

        // Define Rows
        List<ReportRow> rows = new ArrayList<>();
        for (TestResultItem item : results) {
            if (item.getIsGroupSeparator()) {
                continue;
            }

            ReportRow row = new ReportRow();

            // Fetch additional metadata per item (Sample, Organization, Provider)
            Sample sample = new Sample();
            sample.setAccessionNumber(item.getAccessionNumber());
            sampleService.getSampleByAccessionNumber(sample);

            String orgName = "";
            String clinician = "";
            String collectionDate = "";

            if (sample.getId() != null) {
                SampleOrganization sampleOrg = new SampleOrganization();
                sampleOrg.setSample(sample);
                sampleOrganizationService.getDataBySample(sampleOrg);
                if (sampleOrg.getOrganization() != null) {
                    orgName = sampleOrg.getOrganization().getOrganizationName();
                }

                SampleHuman sampleHuman = new SampleHuman();
                sampleHuman.setSampleId(sample.getId());
                sampleHumanService.getDataBySample(sampleHuman);
                if (sampleHuman.getProviderId() != null) {
                    Provider provider = new Provider();
                    provider.setId(sampleHuman.getProviderId());
                    providerService.getData(provider);
                    if (provider.getPerson() != null) {
                        clinician = provider.getPerson().getLastName() + ", " + provider.getPerson().getFirstName();
                    }
                }

                collectionDate = sample.getCollectionDateForDisplay();
            }

            // Populate mapping (Order doesn't matter for the map, but we'll be consistent)
            row.addData("accessionNumber", item.getAccessionNumber());
            row.addData("patientName", item.getPatientName());
            row.addData("patientExternalId", patient.getExternalId());
            row.addData("patientGender", patient.getGender());
            row.addData("patientDateOfBirth", patient.getBirthDateForDisplay());
            row.addData("organizationName", orgName);
            row.addData("sampleCollectionDate", collectionDate);
            row.addData("sampleReceivedDate", item.getReceivedDate());
            row.addData("clinicianName", clinician);
            row.addData("testName", item.getTestName());
            row.addData("testDescription", item.getTestName()); // Mapping description to name for now as proxied in
                                                                // TestResultItem
            row.addData("analysisStatus", item.getAnalysisStatusId());
            row.addData("resultValue", item.getResultValue());

            // Also populate cells in same order as columns for UI compatibility
            row.addCell(item.getAccessionNumber());
            row.addCell(item.getPatientName());
            row.addCell(patient.getExternalId());
            row.addCell(patient.getGender());
            row.addCell(patient.getBirthDateForDisplay());
            row.addCell(orgName);
            row.addCell(collectionDate);
            row.addCell(item.getReceivedDate());
            row.addCell(clinician);
            row.addCell(item.getTestName());
            row.addCell(item.getTestName());
            row.addCell(item.getAnalysisStatusId());
            row.addCell(item.getResultValue());

            rows.add(row);
        }
        data.setRows(rows);

        return data;
    }
}
