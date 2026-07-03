package org.openelisglobal.dataexport.controller.rest;

import java.util.List;
import org.openelisglobal.dataexport.service.DataExportStatusViewService;
import org.openelisglobal.dataexport.valueholder.DataExportAttemptView;
import org.openelisglobal.dataexport.valueholder.DataExportStatusView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest")
@PreAuthorize("hasRole('ADMIN')")
public class DataExportStatusRestController {

    @Autowired
    private DataExportStatusViewService dataExportStatusViewService;

    @GetMapping(value = "/DataExportStatus", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<DataExportStatusView> getDataExportStatus() {
        return dataExportStatusViewService.getAllStatuses();
    }

    @GetMapping(value = "/DataExportStatus/{taskId}/attempts", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<DataExportAttemptView> getAttempts(@PathVariable Long taskId,
            @RequestParam(name = "limit", defaultValue = "20") int limit) {
        return dataExportStatusViewService.getAttemptsForTask(taskId, limit);
    }

    @PostMapping(value = "/DataExportStatus/{taskId}/trigger")
    public ResponseEntity<Void> triggerExport(@PathVariable Long taskId) {
        return dataExportStatusViewService.triggerExport(taskId) ? ResponseEntity.ok().build()
                : ResponseEntity.notFound().build();
    }
}
