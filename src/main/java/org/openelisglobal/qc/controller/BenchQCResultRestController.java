package org.openelisglobal.qc.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.common.util.ControllerUtills;
import org.openelisglobal.qc.form.BenchQCCaptureForm;
import org.openelisglobal.qc.service.QCResultService;
import org.openelisglobal.qc.valueholder.QCResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * OGC-1147 — the write path for bench control runs (manual quantitative and
 * RDT). Before this, QC results could only be created by the FHIR analyzer
 * import: no endpoint on the whole {@code /rest/qc/*} surface persisted a QC
 * result, and the one service method that did stamped every row with the
 * automation user.
 *
 * <p>
 * Deliberately its own controller rather than another method on
 * {@link QCRestController}: that class installs an {@code @InitBinder} whose
 * {@code setAllowedFields} allowlist names only {@code qc_control_lot}
 * properties. JSON bodies bind through Jackson and so would not actually be
 * filtered by it, but co-locating a differently-shaped payload with a binder
 * locked to another entity's fields is a trap for the next reader.
 *
 * <p>
 * Gated from day one, following {@code QCExportRestController} — the only other
 * gated endpoints on this surface. The rest of {@code /rest/qc/*} is
 * authenticated but otherwise open; that is a real finding, but retro-gating
 * live analyzer traffic does not belong in this story.
 */
@RestController
@RequestMapping("/rest/qc/results")
public class BenchQCResultRestController extends BaseRestController {

    @Autowired
    private QCResultService qcResultService;

    /**
     * Record one bench control run. Returns 201 with the persisted result —
     * including the z-score, when the run was quantitative against a lot with
     * established statistics.
     *
     * <p>
     * Returns 400 for an illegal source/value/outcome combination (for example an
     * RDT control carrying a number, which FR-A3 forbids) or an unusable control
     * lot, rather than letting the database constraint surface as a 500.
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('qa.view.qc') or hasRole('GLOBAL_ADMIN')")
    public ResponseEntity<?> record(@Valid @RequestBody BenchQCCaptureForm capture, HttpServletRequest request) {

        int sysUserId;
        try {
            sysUserId = Integer.parseInt(ControllerUtills.getSysUserId(request));
        } catch (NumberFormatException | NullPointerException e) {
            LogEvent.logError(this.getClass().getName(), "record", "no resolvable session user for QC capture");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("No session user");
        }

        try {
            QCResult saved = qcResultService.createBenchQCResult(capture, sysUserId);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (IllegalArgumentException e) {
            LogEvent.logWarn(this.getClass().getName(), "record", "rejected QC capture: " + e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
