package org.openelisglobal.audittrail.controller.rest;

import java.util.List;
import org.openelisglobal.audittrail.action.workers.AuditTrailItem;
import org.openelisglobal.audittrail.action.workers.AuditTrailViewWorker;
import org.openelisglobal.audittrail.form.AuditTrailViewForm;
import org.openelisglobal.spring.util.SpringContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuditTrailReportRestController {

    @GetMapping("/rest/AuditTrailReport")
    public ResponseEntity<AuditTrailViewForm> getAuditTrailReport(@RequestParam String accessionNumber) {
        AuditTrailViewForm response = new AuditTrailViewForm();

        AuditTrailViewWorker worker = SpringContext.getBean(AuditTrailViewWorker.class);
        worker.setAccessionNumber(accessionNumber);
        List<AuditTrailItem> items = worker.getAuditTrail();

        if (items.size() == 0) {
            // Set error message if accession number is not found
            return ResponseEntity.ok(response);
        }

        // Populate the response object
        response.setAccessionNumber(accessionNumber);
        response.setLog(items);
        response.setSampleOrderItems(worker.getSampleOrderSnapshot());
        response.setPatientProperties(worker.getPatientSnapshot());
        return ResponseEntity.ok(response);
    }
}
