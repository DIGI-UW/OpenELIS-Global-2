package org.openelisglobal.odoo.controller;

import java.util.List;
import java.util.stream.Collectors;
import org.openelisglobal.odoo.client.OdooConnection;
import org.openelisglobal.odoo.dto.OdooSyncQueueEntryDTO;
import org.openelisglobal.odoo.dto.OdooSyncQueueResponseDTO;
import org.openelisglobal.odoo.entity.OdooSyncQueue;
import org.openelisglobal.odoo.scheduler.OdooRetryJob;
import org.openelisglobal.odoo.service.OdooSyncQueueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/odoo/queue")
public class OdooSyncQueueController {

    @Autowired
    private OdooSyncQueueService odooSyncQueueService;

    @Autowired
    private OdooRetryJob odooRetryJob;

    @Autowired
    private OdooConnection odooConnection;

    @GetMapping
    public ResponseEntity<OdooSyncQueueResponseDTO> getQueue() {
        List<OdooSyncQueue> queueEntries = odooSyncQueueService.getAllOrdered(List.of("createdDate"), true);

        List<OdooSyncQueueEntryDTO> entryDtos = queueEntries.stream().map(OdooSyncQueueEntryDTO::fromEntity)
                .collect(Collectors.toList());

        OdooSyncQueueResponseDTO response = new OdooSyncQueueResponseDTO();
        response.setEntries(entryDtos);
        response.setPendingCount(odooSyncQueueService.getPendingCount());
        response.setFailedCount(odooSyncQueueService.getFailedCount());
        response.setOdooAvailable(odooConnection.isAvailable());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/retry")
    public ResponseEntity<OdooSyncQueueResponseDTO> triggerManualRetry() {
        String statusMessage = odooRetryJob.triggerManualRetry();
        List<OdooSyncQueue> queueEntries = odooSyncQueueService.getAllOrdered(List.of("createdDate"), true);
        List<OdooSyncQueueEntryDTO> entryDtos = queueEntries.stream().map(OdooSyncQueueEntryDTO::fromEntity)
                .collect(Collectors.toList());

        OdooSyncQueueResponseDTO response = new OdooSyncQueueResponseDTO();
        response.setEntries(entryDtos);
        response.setPendingCount(odooSyncQueueService.getPendingCount());
        response.setFailedCount(odooSyncQueueService.getFailedCount());
        response.setOdooAvailable(odooConnection.isAvailable());
        response.setStatusMessage(statusMessage);

        return ResponseEntity.ok(response);
    }
}
