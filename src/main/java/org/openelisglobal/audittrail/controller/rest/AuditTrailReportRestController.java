package org.openelisglobal.audittrail.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.openelisglobal.audittrail.action.workers.AuditTrailItem;
import org.openelisglobal.audittrail.action.workers.AuditTrailViewWorker;
import org.openelisglobal.audittrail.form.AuditTrailViewForm;
import org.openelisglobal.rbac.RbacAction;
import org.openelisglobal.rbac.RbacPermissionService;
import org.openelisglobal.spring.util.SpringContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuditTrailReportRestController {

    @Autowired
    private RbacPermissionService rbacPermissionService;

    @GetMapping("/rest/AuditTrailReport")
    public ResponseEntity<AuditTrailViewForm> getAuditTrailReport(@RequestParam String accessionNumber,
            HttpServletRequest request) {
        if (!rbacPermissionService.hasPermission(request, RbacAction.VIEW_AUDIT_TRAIL)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

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
