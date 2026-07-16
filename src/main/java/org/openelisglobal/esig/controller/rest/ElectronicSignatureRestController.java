package org.openelisglobal.esig.controller.rest;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.ColumnText;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfPageEventHelper;
import com.itextpdf.text.pdf.PdfWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.StringUtil;
import org.openelisglobal.esig.service.ElectronicSignatureService;
import org.openelisglobal.esig.valueholder.ElectronicSignature;
import org.openelisglobal.esig.valueholder.EsigFirstUseCertification;
import org.openelisglobal.esig.valueholder.SignatureMeaning;
import org.openelisglobal.internationalization.MessageUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for electronic signatures per 21 CFR Part 11.
 *
 * <p>
 * Endpoints:
 * <ul>
 * <li>POST /rest/esig/sign - Execute a signature</li>
 * <li>POST /rest/esig/certify - Complete first-use certification</li>
 * <li>GET /rest/esig/signatures - Query signatures for a record</li>
 * <li>GET /rest/esig/session-status - Get signing session status</li>
 * <li>GET /rest/esig/certifications - Admin: list all certifications</li>
 * <li>DELETE /rest/esig/admin/certifications/{username} - Admin: revoke
 * certification</li>
 * </ul>
 */
@RestController
@RequestMapping("/rest/esig")
public class ElectronicSignatureRestController extends BaseRestController {

    private static final int MAX_LOG_PAGE_SIZE = 200;
    private static final long MAX_LOG_DATE_RANGE_DAYS = 366;
    private static final int MAX_EXPORT_ROWS = 10000;

    @Autowired
    private ElectronicSignatureService electronicSignatureService;

    // Injected rather than ConfigurationProperties.getInstance(): the static
    // path routes through SpringContext's static holder, which test slices
    // must not touch (registering SpringContext in a slice overwrites the
    // holder for every later test in the JVM).
    @Autowired
    private ConfigurationProperties configurationProperties;

    // ========================
    // Signature Execution
    // ========================

    /**
     * Execute an electronic signature.
     *
     * @param request the signature request containing credentials and signature
     *                details
     * @return the created signature record
     */
    @PostMapping(value = "/sign", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> executeSignature(@RequestBody SignatureRequest request, HttpServletRequest httpRequest) {

        try {
            // The username is part of the two-component identification per
            // §11.200(a). Validate it matches the authenticated session to
            // prevent signing as another user.
            String authenticatedUser = getAuthenticatedUsername();
            if (authenticatedUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(errorResponse("UNAUTHORIZED", "No authenticated user"));
            }
            if (request.getUsername() == null || !request.getUsername().equals(authenticatedUser)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                        errorResponse("IDENTITY_MISMATCH", "Supplied username does not match authenticated user"));
            }
            String username = authenticatedUser;

            // Get client metadata
            String clientIp = getClientIp(httpRequest);
            String userAgent = httpRequest.getHeader("User-Agent");

            ElectronicSignature signature = electronicSignatureService.executeSignature(username, request.getPassword(),
                    request.getSignatureMeaning(), request.getRecordType(), request.getRecordId(),
                    request.getRejectionReason(), clientIp, userAgent);

            return ResponseEntity.ok(toSignatureResponse(signature));

        } catch (IllegalStateException e) {
            // E-signatures not enabled
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(errorResponse("ESIG_DISABLED", e.getMessage()));
        } catch (IllegalArgumentException e) {
            // Generic message to prevent user enumeration (actual error logged server-side)
            LogEvent.logWarn(getClass().getSimpleName(), "executeSignature", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(errorResponse("INVALID_REQUEST", "Invalid credentials or request"));
        } catch (UnsupportedOperationException e) {
            // Keycloak not supported yet
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                    .body(errorResponse("NOT_IMPLEMENTED", e.getMessage()));
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse("INTERNAL_ERROR", "An unexpected error occurred"));
        }
    }

    // ========================
    // First-Use Certification
    // ========================

    /**
     * Complete first-use certification.
     *
     * @param request the certification request containing credentials and
     *                certification text
     * @return the created certification record
     */
    @PostMapping(value = "/certify", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> certifyUser(@RequestBody CertificationRequest request, HttpServletRequest httpRequest) {

        try {
            // Validate identity matches authenticated session
            String authenticatedUser = getAuthenticatedUsername();
            if (authenticatedUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(errorResponse("UNAUTHORIZED", "No authenticated user"));
            }
            if (request.getUsername() == null || !request.getUsername().equals(authenticatedUser)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                        errorResponse("IDENTITY_MISMATCH", "Supplied username does not match authenticated user"));
            }
            String username = authenticatedUser;

            String clientIp = getClientIp(httpRequest);
            String userAgent = httpRequest.getHeader("User-Agent");

            EsigFirstUseCertification certification = electronicSignatureService.certifyUser(username,
                    request.getPassword(), request.getCertificationText(), clientIp, userAgent);

            return ResponseEntity.ok(toCertificationResponse(certification));

        } catch (IllegalArgumentException e) {
            LogEvent.logWarn(getClass().getSimpleName(), "certifyUser", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(errorResponse("INVALID_REQUEST", "Invalid credentials or request"));
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse("INTERNAL_ERROR", "An unexpected error occurred"));
        }
    }

    /**
     * Check if current user is certified. Only the authenticated user can check
     * their own certification status.
     */
    @GetMapping(value = "/certified/{username}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> isUserCertified(@PathVariable String username) {
        String authenticatedUser = getAuthenticatedUsername();
        if (authenticatedUser == null || !authenticatedUser.equals(username)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(errorResponse("FORBIDDEN", "Can only check own certification status"));
        }
        boolean certified = electronicSignatureService.isUserCertified(username);
        Map<String, Object> response = new HashMap<>();
        response.put("username", username);
        response.put("certified", certified);
        return ResponseEntity.ok(response);
    }

    // ========================
    // Signature Queries
    // ========================

    /**
     * Get signatures for a specific record. Requires authentication — signature
     * records contain signer identity and IP metadata.
     */
    @GetMapping(value = "/signatures", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getSignaturesForRecord(@RequestParam String recordType, @RequestParam Long recordId) {

        List<ElectronicSignature> signatures = electronicSignatureService.getSignaturesForRecord(recordType, recordId);

        return ResponseEntity.ok(signatures.stream().map(this::toSignatureResponse).toList());
    }

    /**
     * Filterable, paginated signature log (E-Sig Log page, OGC-702). Gated on the
     * QMS pillar permission from the QA permission registry.
     */
    @GetMapping(value = "/log", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('qa.view.qms') or hasRole('GLOBAL_ADMIN')")
    public ResponseEntity<?> getSignatureLog(@RequestParam String fromDate, @RequestParam String toDate,
            @RequestParam(required = false) Long signerId, @RequestParam(required = false) String meaning,
            @RequestParam(required = false) String recordType, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int pageSize) {

        if (page < 0) {
            page = 0;
        }
        if (pageSize < 1) {
            pageSize = 25;
        }
        if (pageSize > MAX_LOG_PAGE_SIZE) {
            pageSize = MAX_LOG_PAGE_SIZE;
        }

        LogFilter filter;
        try {
            filter = parseLogFilter(fromDate, toDate, meaning, recordType);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(errorResponse("INVALID_REQUEST", e.getMessage()));
        }

        List<ElectronicSignature> signatures = electronicSignatureService.searchSignatures(filter.start(), filter.end(),
                signerId, filter.meaning(), filter.recordType(), page, pageSize);
        long totalCount = electronicSignatureService.countSearchSignatures(filter.start(), filter.end(), signerId,
                filter.meaning(), filter.recordType());

        Map<String, Object> response = new HashMap<>();
        response.put("items", signatures.stream().map(this::toSignatureResponse).toList());
        response.put("totalCount", totalCount);
        return ResponseEntity.ok(response);
    }

    /**
     * CSV export of the signature log (OGC-703). Applies the same filters as /log,
     * capped at {@link #MAX_EXPORT_ROWS} rows.
     */
    @GetMapping(value = "/log/export")
    @PreAuthorize("hasAuthority('qa.view.qms') or hasRole('GLOBAL_ADMIN')")
    public void exportSignatureLogCsv(@RequestParam String fromDate, @RequestParam String toDate,
            @RequestParam(required = false) Long signerId, @RequestParam(required = false) String meaning,
            @RequestParam(required = false) String recordType, HttpServletResponse response) throws IOException {

        LogFilter filter;
        try {
            filter = parseLogFilter(fromDate, toDate, meaning, recordType);
        } catch (IllegalArgumentException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
            return;
        }

        List<ElectronicSignature> signatures = electronicSignatureService.searchSignatures(filter.start(), filter.end(),
                signerId, filter.meaning(), filter.recordType(), 0, MAX_EXPORT_ROWS);

        response.setContentType("text/csv");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Content-Disposition", "attachment; filename=\"e-signature-log.csv\"");

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        PrintWriter writer = response.getWriter();
        writer.printf("%s,%s,%s,%s,%s%n", MessageUtil.getMessage("esig.export.header.signedAt"),
                MessageUtil.getMessage("esig.export.header.signer"),
                MessageUtil.getMessage("esig.export.header.action"),
                MessageUtil.getMessage("esig.export.header.subject"),
                MessageUtil.getMessage("esig.export.header.reason"));
        for (ElectronicSignature sig : signatures) {
            writer.printf("%s,%s,%s,%s,%s%n",
                    StringUtil.csvEscape(sig.getSignedAt() != null ? sdf.format(sig.getSignedAt()) : ""),
                    StringUtil.csvEscape(sig.getSignerNamePrinted()), StringUtil.csvEscape(meaningLabel(sig)),
                    StringUtil.csvEscape(subjectLabel(sig)), StringUtil.csvEscape(sig.getRejectionReason()));
        }
        writer.flush();
    }

    /**
     * PDF export of the signature log (OGC-703): CAP-style header (lab name, date
     * range, total record count, generated-at) and page-numbered footer.
     */
    @GetMapping(value = "/log/exportPdf")
    @PreAuthorize("hasAuthority('qa.view.qms') or hasRole('GLOBAL_ADMIN')")
    public void exportSignatureLogPdf(@RequestParam String fromDate, @RequestParam String toDate,
            @RequestParam(required = false) Long signerId, @RequestParam(required = false) String meaning,
            @RequestParam(required = false) String recordType, HttpServletResponse response) throws IOException {

        LogFilter filter;
        try {
            filter = parseLogFilter(fromDate, toDate, meaning, recordType);
        } catch (IllegalArgumentException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
            return;
        }

        List<ElectronicSignature> signatures = electronicSignatureService.searchSignatures(filter.start(), filter.end(),
                signerId, filter.meaning(), filter.recordType(), 0, MAX_EXPORT_ROWS);

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=\"e-signature-log.pdf\"");

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter pdfWriter = PdfWriter.getInstance(document, response.getOutputStream());
            pdfWriter.setPageEvent(new PageNumberFooter());
            document.open();

            Font titleFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD);
            Font metaFont = new Font(Font.FontFamily.HELVETICA, 9);
            Font headerFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, BaseColor.WHITE);
            Font cellFont = new Font(Font.FontFamily.HELVETICA, 9);

            document.add(new Phrase(MessageUtil.getMessage("esig.export.title") + "\n", titleFont));
            String labName = configurationProperties.getPropertyValue(ConfigurationProperties.Property.SiteName);
            document.add(new Phrase(
                    MessageUtil.getMessage("esig.export.labName") + ": " + (labName == null ? "" : labName) + "\n",
                    metaFont));
            document.add(new Phrase(
                    MessageUtil.getMessage("esig.export.dateRange") + ": " + fromDate + " — " + toDate + "\n",
                    metaFont));
            document.add(new Phrase(
                    MessageUtil.getMessage("esig.export.totalRecords") + ": " + signatures.size() + "\n", metaFont));
            document.add(new Phrase(MessageUtil.getMessage("esig.export.generatedAt") + ": "
                    + sdf.format(new java.util.Date()) + "\n\n", metaFont));

            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setWidths(new float[] { 2f, 2.2f, 2f, 2.4f, 3.4f });

            String[] headers = { MessageUtil.getMessage("esig.export.header.signedAt"),
                    MessageUtil.getMessage("esig.export.header.signer"),
                    MessageUtil.getMessage("esig.export.header.action"),
                    MessageUtil.getMessage("esig.export.header.subject"),
                    MessageUtil.getMessage("esig.export.header.reason") };
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
                cell.setBackgroundColor(new BaseColor(51, 102, 179));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setPadding(5);
                table.addCell(cell);
            }

            for (ElectronicSignature sig : signatures) {
                table.addCell(new Phrase(sig.getSignedAt() != null ? sdf.format(sig.getSignedAt()) : "", cellFont));
                table.addCell(new Phrase(Objects.toString(sig.getSignerNamePrinted(), ""), cellFont));
                table.addCell(new Phrase(meaningLabel(sig), cellFont));
                table.addCell(new Phrase(subjectLabel(sig), cellFont));
                table.addCell(new Phrase(Objects.toString(sig.getRejectionReason(), ""), cellFont));
            }

            document.add(table);
            document.close();
        } catch (DocumentException e) {
            LogEvent.logError(e);
            throw new IOException("Error generating PDF", e);
        }
    }

    /** Footer with page number on every page of the PDF export (CAP layout). */
    private static class PageNumberFooter extends PdfPageEventHelper {
        private static final Font FOOTER_FONT = new Font(Font.FontFamily.HELVETICA, 8);

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            Phrase footer = new Phrase(MessageUtil.getMessage("esig.export.page") + " " + writer.getPageNumber(),
                    FOOTER_FONT);
            ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_CENTER, footer,
                    (document.right() + document.left()) / 2, document.bottom() - 12, 0);
        }
    }

    /** Validated /log filter set, shared by the list and export endpoints. */
    private record LogFilter(Timestamp start, Timestamp end, SignatureMeaning meaning, String recordType) {
    }

    private LogFilter parseLogFilter(String fromDate, String toDate, String meaning, String recordType) {
        LocalDate from;
        LocalDate to;
        try {
            from = LocalDate.parse(fromDate);
            to = LocalDate.parse(toDate);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date: " + e.getMessage());
        }
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("fromDate must not be after toDate");
        }
        if (ChronoUnit.DAYS.between(from, to) > MAX_LOG_DATE_RANGE_DAYS) {
            throw new IllegalArgumentException("Date range must not exceed 1 year");
        }

        SignatureMeaning meaningFilter = null;
        if (meaning != null && !meaning.isBlank()) {
            try {
                meaningFilter = SignatureMeaning.valueOf(meaning);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Unknown meaning: " + meaning);
            }
        }
        String recordTypeFilter = (recordType == null || recordType.isBlank()) ? null : recordType;

        return new LogFilter(Timestamp.valueOf(from.atStartOfDay()), Timestamp.valueOf(to.atTime(LocalTime.MAX)),
                meaningFilter, recordTypeFilter);
    }

    private String meaningLabel(ElectronicSignature sig) {
        if (sig.getSignatureMeaning() == null) {
            return "";
        }
        return MessageUtil.getMessage("esig.meaning." + sig.getSignatureMeaning().name());
    }

    private String subjectLabel(ElectronicSignature sig) {
        if (sig.getRecordType() == null) {
            return "";
        }
        return sig.getRecordType() + " #" + sig.getRecordId();
    }

    // ========================
    // Session Status
    // ========================

    /**
     * Get signing session status for the current user.
     */
    @GetMapping(value = "/session-status/{username}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getSessionStatus(@PathVariable String username) {
        String authenticatedUser = getAuthenticatedUsername();
        if (authenticatedUser == null || !authenticatedUser.equals(username)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(errorResponse("FORBIDDEN", "Can only check own session status"));
        }
        Map<String, Object> response = new HashMap<>();
        response.put("username", username);
        response.put("sessionActive", electronicSignatureService.hasActiveSigningSession(username));
        response.put("signingCount", electronicSignatureService.getSessionSigningCount(username));
        return ResponseEntity.ok(response);
    }

    // ========================
    // Admin Endpoints
    // ========================

    /**
     * Get all certifications (admin).
     */
    @GetMapping(value = "/admin/certifications", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllCertifications() {
        List<EsigFirstUseCertification> certifications = electronicSignatureService.getAllCertifications();

        return ResponseEntity.ok(certifications.stream().map(this::toCertificationResponse).toList());
    }

    /**
     * Revoke a user's certification (admin).
     */
    @DeleteMapping(value = "/admin/certifications/{username}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> revokeCertification(@PathVariable String username) {
        try {
            String adminUser = getAuthenticatedUsername();
            electronicSignatureService.revokeCertification(username);
            LogEvent.logInfo(getClass().getSimpleName(), "revokeCertification",
                    "Certification revoked for user '" + username + "' by admin '" + adminUser + "'");
            Map<String, Object> response = new HashMap<>();
            response.put("status", "REVOKED");
            response.put("username", username);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse("NOT_FOUND", e.getMessage()));
        }
    }

    /**
     * Check if e-signatures are enabled.
     */
    @GetMapping(value = "/enabled", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> isEsigEnabled() {
        Map<String, Object> response = new HashMap<>();
        response.put("enabled", electronicSignatureService.isEsigEnabled());
        return ResponseEntity.ok(response);
    }

    // ========================
    // Helper Methods
    // ========================

    /**
     * Get the login name of the currently authenticated user from Spring Security.
     */
    private String getAuthenticatedUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return auth.getName();
    }

    /**
     * Get client IP from the direct connection. We intentionally do NOT trust
     * X-Forwarded-For here — it is a client-controlled header and can be forged.
     * For 21 CFR Part 11 audit records, we record the IP the server actually sees.
     */
    private String getClientIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }

    private Map<String, Object> toSignatureResponse(ElectronicSignature sig) {
        Map<String, Object> response = new HashMap<>();
        response.put("signatureId", sig.getId());
        response.put("signerId", sig.getSignerId());
        response.put("signerNamePrinted", sig.getSignerNamePrinted());
        response.put("signatureMeaning", sig.getSignatureMeaning());
        response.put("signedAt", sig.getSignedAt());
        response.put("recordType", sig.getRecordType());
        response.put("recordId", sig.getRecordId());
        response.put("rejectionReason", sig.getRejectionReason());
        response.put("sessionSigningSequence", sig.getSessionSigningSequence());
        response.put("authMethod", sig.getAuthMethod());
        return response;
    }

    private Map<String, Object> toCertificationResponse(EsigFirstUseCertification cert) {
        Map<String, Object> response = new HashMap<>();
        response.put("certificationId", cert.getId());
        response.put("userId", cert.getUserId());
        response.put("certifiedAt", cert.getCertifiedAt());
        return response;
    }

    private Map<String, Object> errorResponse(String code, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", code);
        response.put("message", message);
        return response;
    }

    // ========================
    // Request DTOs
    // ========================

    /**
     * Request body for signature execution.
     */
    public static class SignatureRequest {
        private String username;
        private String password;
        private SignatureMeaning signatureMeaning;
        private String recordType;
        private Long recordId;
        private String rejectionReason;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public SignatureMeaning getSignatureMeaning() {
            return signatureMeaning;
        }

        public void setSignatureMeaning(SignatureMeaning signatureMeaning) {
            this.signatureMeaning = signatureMeaning;
        }

        public String getRecordType() {
            return recordType;
        }

        public void setRecordType(String recordType) {
            this.recordType = recordType;
        }

        public Long getRecordId() {
            return recordId;
        }

        public void setRecordId(Long recordId) {
            this.recordId = recordId;
        }

        public String getRejectionReason() {
            return rejectionReason;
        }

        public void setRejectionReason(String rejectionReason) {
            this.rejectionReason = rejectionReason;
        }
    }

    /**
     * Request body for first-use certification.
     */
    public static class CertificationRequest {
        private String username;
        private String password;
        private String certificationText;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getCertificationText() {
            return certificationText;
        }

        public void setCertificationText(String certificationText) {
            this.certificationText = certificationText;
        }
    }
}
