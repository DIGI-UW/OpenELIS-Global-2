package org.openelisglobal.reportqueue.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.openelisglobal.common.util.ControllerUtills;
import org.openelisglobal.reportqueue.dto.ReportQueuePreferenceDTO;
import org.openelisglobal.reportqueue.dto.ReportQueueRequest;
import org.openelisglobal.reportqueue.dto.ReportQueueRowDetailDTO;
import org.openelisglobal.reportqueue.service.ReportQueueService;
import org.openelisglobal.reportqueue.service.ReportQueueService.NothingReleasedException;
import org.openelisglobal.reportqueue.service.ReportQueueService.PrintOutcome;
import org.openelisglobal.reportqueue.valueholder.ReportVersion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The Report Print Queue (OGC-431): the paged list, a row's detail, printing
 * through the version pipeline, archived PDFs and per-user preferences.
 */
@RestController
@RequestMapping("/rest/reportQueue")
@PreAuthorize("hasAnyRole('REPORTS', 'ADMIN')")
public class ReportQueueRestController {

    @Autowired
    private ReportQueueService reportQueueService;

    public static class PrintRequest {
        private Long sampleId;

        public Long getSampleId() {
            return sampleId;
        }

        public void setSampleId(Long sampleId) {
            this.sampleId = sampleId;
        }
    }

    @GetMapping
    public ResponseEntity<?> list(HttpServletRequest request, @RequestParam(required = false) String labNo,
            @RequestParam(required = false) String labNoTo, @RequestParam(required = false) String patientId,
            @RequestParam(required = false) List<String> states, @RequestParam(required = false) String printStatus,
            @RequestParam(required = false) String window, @RequestParam(required = false) String from,
            @RequestParam(required = false) String to, @RequestParam(required = false) List<Long> facilityIds,
            @RequestParam(required = false) List<Long> wardIds, @RequestParam(required = false) List<Long> requesterIds,
            @RequestParam(required = false) String sort, @RequestParam(required = false) String sortDir,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false) Integer pageSize) {
        if (patientId != null && !patientId.isBlank() && !patientId.trim().matches("\\d+")) {
            return ResponseEntity.badRequest().body(Map.of("message", "patientId must be a number"));
        }
        if (labNo != null && !labNo.isBlank() && labNoTo != null && !labNoTo.isBlank()
                && labNoTo.trim().compareTo(labNo.trim()) < 0) {
            return ResponseEntity.badRequest().body(
                    Map.of("code", "labRangeReversed", "message", "the To lab number is before the From lab number"));
        }
        ReportQueueRequest queueRequest = new ReportQueueRequest();
        queueRequest.setLabNo(labNo);
        queueRequest.setLabNoTo(labNoTo);
        queueRequest.setPatientId(patientId);
        queueRequest.setStates(states);
        queueRequest.setPrintStatus(printStatus);
        queueRequest.setWindow(window);
        queueRequest.setFrom(from);
        queueRequest.setTo(to);
        queueRequest.setFacilityIds(facilityIds);
        queueRequest.setWardIds(wardIds);
        queueRequest.setRequesterIds(requesterIds);
        queueRequest.setSort(sort);
        queueRequest.setSortDir(sortDir);
        queueRequest.setPage(page);
        queueRequest.setPageSize(pageSize);
        return ResponseEntity.ok(reportQueueService.list(queueRequest, ControllerUtills.getSysUserId(request)));
    }

    @GetMapping("/orders/{sampleId}")
    public ResponseEntity<ReportQueueRowDetailDTO> detail(HttpServletRequest request, @PathVariable Long sampleId) {
        return reportQueueService.detail(sampleId, ControllerUtills.getSysUserId(request)).map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * POST, not GET: a print creates a version and an archive row, and a GET is
     * open to prefetching and to CSRF.
     */
    @PostMapping("/print")
    public ResponseEntity<?> print(HttpServletRequest request, @RequestBody PrintRequest printRequest) {
        if (printRequest == null || printRequest.getSampleId() == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "sampleId is required"));
        }
        Optional<PrintOutcome> outcome;
        try {
            outcome = reportQueueService.print(printRequest.getSampleId(), ControllerUtills.getSysUserId(request));
        } catch (NothingReleasedException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("code", "nothingReleased", "labNumber", e.getMessage()));
        }
        if (outcome.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        PrintOutcome print = outcome.get();
        String safeLabel = print.getLabNumber() == null ? "report"
                : print.getLabNumber().replaceAll("[^a-zA-Z0-9-]", "");
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                .header("Content-Disposition", "inline; filename=\"" + safeLabel + "-v" + print.getVersion() + ".pdf\"")
                .header("X-Report-Version", String.valueOf(print.getVersion()))
                .header("X-Report-Reprint", String.valueOf(print.isReprint())).body(print.getPdf());
    }

    @GetMapping("/versions/{versionId}/pdf")
    public ResponseEntity<byte[]> archivedPdf(HttpServletRequest request, @PathVariable Long versionId) {
        Optional<ReportVersion> version = reportQueueService.version(versionId, ControllerUtills.getSysUserId(request));
        if (version.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        if (version.get().getPdfContent() == null) {
            return ResponseEntity.status(HttpStatus.GONE).build();
        }
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                .header("Content-Disposition", "inline; filename=\"version-" + version.get().getVersion() + ".pdf\"")
                .body(version.get().getPdfContent());
    }

    @GetMapping("/preferences")
    public ReportQueuePreferenceDTO preferences(HttpServletRequest request) {
        return reportQueueService.preferences(ControllerUtills.getSysUserId(request));
    }

    @PutMapping("/preferences")
    public ReportQueuePreferenceDTO savePreferences(HttpServletRequest request,
            @RequestBody ReportQueuePreferenceDTO preference) {
        return reportQueueService.savePreferences(ControllerUtills.getSysUserId(request), preference);
    }
}
