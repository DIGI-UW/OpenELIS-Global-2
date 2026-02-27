package org.openelisglobal.report.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.report.ReportingData;
import org.openelisglobal.report.service.PatientReportService;
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
    private PatientReportService patientReportService;

    @GetMapping("/{patientId}/results")
    public ResponseEntity<ReportingData> getPatientResults(@PathVariable String patientId, HttpServletRequest request) {
        ReportingData data = patientReportService.buildPatientResultsReport(patientId, getSysUserId(request));
        if (data == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(data);
    }
}
