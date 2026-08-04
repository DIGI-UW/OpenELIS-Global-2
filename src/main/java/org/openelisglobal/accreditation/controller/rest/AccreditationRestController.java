package org.openelisglobal.accreditation.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URLConnection;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import org.openelisglobal.accreditation.dto.AccreditationSummary;
import org.openelisglobal.accreditation.dto.AccreditingBodyView;
import org.openelisglobal.accreditation.dto.TestAccreditationView;
import org.openelisglobal.accreditation.service.AccreditingBodyService;
import org.openelisglobal.accreditation.service.TestAccreditationService;
import org.openelisglobal.accreditation.valueholder.AccreditingBody;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.common.util.ControllerUtills;
import org.openelisglobal.image.service.ImageService;
import org.openelisglobal.image.valueholder.Image;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * OGC-686 — Test Accreditation API.
 *
 * <p>
 * Reads are gated on {@code qa.view.qms}: Accreditation Status is a QMS-pillar
 * page, and inspectors/QA Officers who hold the pillar permission must be able
 * to see the portfolio without any edit right. Writes are gated on the narrower
 * {@code qa.manage.accreditation} (registered in {@code liquibase/qa/004}),
 * which is what lets the edit controls appear on the same page for the people
 * who own the data. Both accept {@code GLOBAL_ADMIN} as the standard fallback.
 *
 * <p>
 * The FRS's {@code TEST_CATALOG_MANAGE} is deliberately not used: it does not
 * exist in this codebase, and the {@code /admin} shell it assumed is
 * GLOBAL_ADMIN-only, which would lock out the QA Officer the feature is for.
 *
 * <p>
 * Bad input is 400 throughout — the services throw
 * {@link IllegalArgumentException} for unknown ids, malformed codes, duplicate
 * enrollments and delete-while-referenced, and a
 * {@link DataIntegrityViolationException} (a race past those checks straight
 * into the unique constraint) is mapped to 400 rather than surfacing as a 500.
 */
@RestController
@RequestMapping("/rest/accreditation")
public class AccreditationRestController extends BaseRestController {

    /** FRS FR-7: raster only, ≤ 500 KB. SVG is excluded — see the note below. */
    private static final long MAX_LOGO_BYTES = 500L * 1024L;

    private static final int MIN_LOGO_PIXELS = 64;

    private final AccreditingBodyService accreditingBodyService;

    private final TestAccreditationService testAccreditationService;

    private final ImageService imageService;

    public AccreditationRestController(AccreditingBodyService accreditingBodyService,
            TestAccreditationService testAccreditationService, ImageService imageService) {
        this.accreditingBodyService = accreditingBodyService;
        this.testAccreditationService = testAccreditationService;
        this.imageService = imageService;
    }

    // ---------- reads ----------

    @GetMapping(value = "/bodies", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('qa.view.qms') or hasRole('GLOBAL_ADMIN')")
    public List<AccreditingBodyView> listBodies() {
        return accreditingBodyService.getBodyViews();
    }

    @GetMapping(value = "/summary", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('qa.view.qms') or hasRole('GLOBAL_ADMIN')")
    public AccreditationSummary summary() {
        return accreditingBodyService.getSummary();
    }

    @GetMapping(value = "/enrollments", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('qa.view.qms') or hasRole('GLOBAL_ADMIN')")
    public List<TestAccreditationView> listEnrollments(@RequestParam(value = "bodyId", required = false) Long bodyId,
            @RequestParam(value = "testId", required = false) String testId) {
        return testAccreditationService.getEnrollmentViews(bodyId, testId);
    }

    /**
     * Raw logo bytes for the admin thumbnail. {@code DBImageController} only
     * resolves images by {@code site_information} name, so per-body logos need
     * this.
     */
    @GetMapping(value = "/logo/{imageId}")
    @PreAuthorize("hasAuthority('qa.view.qms') or hasRole('GLOBAL_ADMIN')")
    public ResponseEntity<byte[]> logo(@PathVariable String imageId) {
        Image image = imageService.get(imageId);
        if (image == null || image.getImage() == null) {
            return ResponseEntity.notFound().build();
        }
        // The image row stores no content type, so derive it from the bytes rather
        // than claiming PNG for a JPEG upload.
        String contentType = URLConnection.guessContentTypeFromName("logo");
        try (java.io.InputStream in = new ByteArrayInputStream(image.getImage())) {
            String sniffed = URLConnection.guessContentTypeFromStream(in);
            if (sniffed != null) {
                contentType = sniffed;
            }
        } catch (IOException e) {
            LogEvent.logError(e);
        }
        return ResponseEntity.ok()
                .contentType(contentType == null ? MediaType.IMAGE_PNG : MediaType.parseMediaType(contentType))
                .body(image.getImage());
    }

    // ---------- body writes ----------

    @PostMapping(value = "/bodies", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('qa.manage.accreditation') or hasRole('GLOBAL_ADMIN')")
    public ResponseEntity<AccreditingBodyView> createBody(@RequestBody AccreditingBody body,
            HttpServletRequest request) {
        AccreditingBody saved = accreditingBodyService.createBody(body, ControllerUtills.getSysUserId(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(viewOf(saved.getId()));
    }

    @PutMapping(value = "/bodies/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('qa.manage.accreditation') or hasRole('GLOBAL_ADMIN')")
    public AccreditingBodyView updateBody(@PathVariable Long id, @RequestBody AccreditingBody body,
            HttpServletRequest request) {
        accreditingBodyService.updateBody(id, body, ControllerUtills.getSysUserId(request));
        return viewOf(id);
    }

    @DeleteMapping("/bodies/{id}")
    @PreAuthorize("hasAuthority('qa.manage.accreditation') or hasRole('GLOBAL_ADMIN')")
    public ResponseEntity<Void> deleteBody(@PathVariable Long id, HttpServletRequest request) {
        accreditingBodyService.deleteBody(id, ControllerUtills.getSysUserId(request));
        return ResponseEntity.noContent().build();
    }

    /**
     * Upload/replace a body's report logo.
     *
     * <p>
     * PNG/JPEG only, and validated by actually decoding it: JasperReports has no
     * SVG renderer on this classpath (no Batik), so an SVG would upload cleanly and
     * then render as an empty logo slot on every patient report. Rejecting it here
     * is the only place the failure is visible.
     */
    @PostMapping(value = "/bodies/{id}/logo", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('qa.manage.accreditation') or hasRole('GLOBAL_ADMIN')")
    public AccreditingBodyView uploadLogo(@PathVariable Long id, @RequestParam("file") MultipartFile file,
            HttpServletRequest request) {
        byte[] bytes = validateLogo(file);
        Image image = new Image();
        image.setImage(bytes);
        image.setDescription("accreditation-logo-body-" + id);
        Image saved = imageService.save(image);
        accreditingBodyService.setLogo(id, saved.getId(), ControllerUtills.getSysUserId(request));
        return viewOf(id);
    }

    @DeleteMapping(value = "/bodies/{id}/logo", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('qa.manage.accreditation') or hasRole('GLOBAL_ADMIN')")
    public AccreditingBodyView removeLogo(@PathVariable Long id, HttpServletRequest request) {
        accreditingBodyService.setLogo(id, null, ControllerUtills.getSysUserId(request));
        return viewOf(id);
    }

    // ---------- enrollment writes ----------

    @PostMapping(value = "/enrollments", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('qa.manage.accreditation') or hasRole('GLOBAL_ADMIN')")
    public ResponseEntity<Void> enroll(@RequestBody EnrollmentRequest body, HttpServletRequest request) {
        testAccreditationService.enroll(body.testId, body.accreditingBodyId, body.effectiveFrom,
                ControllerUtills.getSysUserId(request));
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/enrollments/{id}")
    @PreAuthorize("hasAuthority('qa.manage.accreditation') or hasRole('GLOBAL_ADMIN')")
    public ResponseEntity<Void> unenroll(@PathVariable Long id, HttpServletRequest request) {
        testAccreditationService.unenroll(id, ControllerUtills.getSysUserId(request));
        return ResponseEntity.noContent().build();
    }

    // ---------- helpers ----------

    /** Re-reads through the view assembler so responses carry counts and status. */
    private AccreditingBodyView viewOf(Long id) {
        return accreditingBodyService.getBodyViews().stream().filter(v -> id.equals(v.id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No accrediting body with id " + id));
    }

    private byte[] validateLogo(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("A logo file is required");
        }
        if (file.getSize() > MAX_LOGO_BYTES) {
            throw new IllegalArgumentException("File exceeds 500 KB");
        }
        byte[] bytes;
        java.awt.image.BufferedImage decoded;
        try {
            bytes = file.getBytes();
            decoded = ImageIO.read(new ByteArrayInputStream(bytes));
        } catch (IOException e) {
            LogEvent.logError(e);
            throw new IllegalArgumentException("Could not read the uploaded file");
        }
        if (decoded == null) {
            throw new IllegalArgumentException("Logo must be a PNG or JPEG image (SVG is not supported on reports)");
        }
        if (decoded.getWidth() < MIN_LOGO_PIXELS || decoded.getHeight() < MIN_LOGO_PIXELS) {
            throw new IllegalArgumentException("Logo must be at least 64×64 pixels");
        }
        return bytes;
    }

    /** Create-enrollment payload. */
    public static class EnrollmentRequest {
        public String testId;
        public Long accreditingBodyId;
        public LocalDate effectiveFrom;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleBadInput(IllegalArgumentException e) {
        return Map.of("error", e.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleConstraint(DataIntegrityViolationException e) {
        LogEvent.logError(e);
        return Map.of("error", "Invalid or duplicate accreditation data");
    }
}
