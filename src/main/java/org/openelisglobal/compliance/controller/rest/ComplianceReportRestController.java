package org.openelisglobal.compliance.controller.rest;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.compliance.service.ComplianceEvaluationResult;
import org.openelisglobal.compliance.service.ComplianceEvaluationService;
import org.openelisglobal.compliance.service.ComplianceReportGenerationService;
import org.openelisglobal.compliance.service.LhuAmendmentService;
import org.openelisglobal.esig.service.ElectronicSignatureService;
import org.openelisglobal.esig.valueholder.ElectronicSignature;
import org.openelisglobal.esig.valueholder.SignatureMeaning;
import org.openelisglobal.observationhistory.service.ObservationHistoryService;
import org.openelisglobal.observationhistory.service.ObservationHistoryServiceImpl.ObservationType;
import org.openelisglobal.sample.dao.SampleComplianceStandardDAO;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sample.valueholder.SampleComplianceStandard;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/complianceReport")
@PreAuthorize("hasRole('ROLE_RESULTS') or hasRole('ROLE_SUPERVISOR') or hasRole('ADMIN')")
public class ComplianceReportRestController {

    private static final DateTimeFormatter DISPLAY_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final String RECORD_TYPE_SAMPLE = "SAMPLE";

    @Autowired
    private SampleService sampleService;

    @Autowired
    private SampleItemService sampleItemService;

    @Autowired
    private SampleComplianceStandardDAO sampleComplianceStandardDAO;

    @Autowired
    private ComplianceEvaluationService complianceEvaluationService;

    @Autowired
    private ComplianceReportGenerationService reportGenerationService;

    @Autowired
    private ElectronicSignatureService electronicSignatureService;

    @Autowired
    private ObservationHistoryService observationHistoryService;

    @Autowired
    private org.openelisglobal.vector.service.VectorSamplingSiteService vectorSamplingSiteService;

    @Autowired
    private org.openelisglobal.result.service.ResultService resultService;

    @Autowired
    private org.openelisglobal.analysis.service.AnalysisService analysisService;

    @Autowired
    private LhuAmendmentService lhuAmendmentService;

    @Autowired
    private org.openelisglobal.compliance.service.ComplianceReportArchiveService archiveService;

    @GetMapping("/compliance-statuses")
    public ResponseEntity<java.util.List<java.util.Map<String, String>>> getComplianceStatuses() {
        java.util.List<java.util.Map<String, String>> statuses = new java.util.ArrayList<>();
        for (org.openelisglobal.compliance.valueholder.ComplianceStatus cs : org.openelisglobal.compliance.valueholder.ComplianceStatus
                .values()) {
            java.util.Map<String, String> entry = new java.util.LinkedHashMap<>();
            entry.put("id", cs.name());
            entry.put("text", cs.toString());
            statuses.add(entry);
        }
        return ResponseEntity.ok(statuses);
    }

    @GetMapping
    public ResponseEntity<ComplianceReportDTO> getReport(@RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo, @RequestParam(required = false) String siteId,
            @RequestParam(required = false) String standardId, @RequestParam(required = false) String complianceStatus,
            @RequestParam(required = false) String generationStatus) {

        List<Sample> samples;
        String from = toSystemDateFormat(dateFrom);
        String to = toSystemDateFormat(dateTo);
        if (from != null && to != null) {
            samples = sampleService.getSamplesReceivedInDateRange(from, to);
        } else {
            java.time.LocalDate today = java.time.LocalDate.now();
            samples = sampleService.getSamplesReceivedInDateRange(formatToSystemDate(today.minusDays(90)),
                    formatToSystemDate(today));
        }

        List<ComplianceReportOrderDTO> orders = new ArrayList<>();
        int ineligibleCount = 0;
        int generatedCount = 0;
        int notYetGeneratedCount = 0;

        for (Sample sample : samples) {
            String workflowType = observationHistoryService.getRawValueForSample(ObservationType.ENV_WORKFLOW_TYPE,
                    sample.getId());
            if (!"environmental".equalsIgnoreCase(workflowType)) {
                continue;
            }

            // Exclude orders not yet collected (e.g. a fresh resample draft awaiting
            // re-collection): no specimen has a collection date.
            boolean collected = sampleItemService.getSampleItemsBySampleId(sample.getId()).stream()
                    .anyMatch(item -> item.getCollectionDate() != null);
            if (!collected) {
                continue;
            }

            List<SampleComplianceStandard> links = sampleComplianceStandardDAO.getAllForSample(sample.getId());
            if (links.isEmpty()) {
                ineligibleCount++;
                // Show ineligible orders in the table unless the user is filtering
                // by a specific compliance status or generation status (ineligible
                // orders can never be generated so they don't match those filters).
                boolean filteringByStatus = complianceStatus != null && !complianceStatus.isEmpty()
                        && !"all".equalsIgnoreCase(complianceStatus);
                boolean filteringByGeneration = generationStatus != null && !generationStatus.isEmpty()
                        && !"all".equalsIgnoreCase(generationStatus);
                if (!filteringByStatus && !filteringByGeneration) {
                    ComplianceReportOrderDTO ineligibleDto = buildOrderDTO(sample, null, null, false);
                    ineligibleDto.setComplianceStatus("INELIGIBLE");
                    orders.add(ineligibleDto);
                }
                continue;
            }

            SampleComplianceStandard primaryLink = links.get(0);
            if (standardId != null && !standardId.isEmpty()
                    && !primaryLink.getComplianceStandard().getId().equals(standardId)) {
                continue;
            }

            ComplianceEvaluationResult eval = complianceEvaluationService.evaluate(sample);
            String orderStatus = eval != null ? eval.getOverallStatus().name() : "NONE";

            if (complianceStatus != null && !complianceStatus.isEmpty() && !"all".equalsIgnoreCase(complianceStatus)
                    && !orderStatus.equalsIgnoreCase(complianceStatus)) {
                continue;
            }

            boolean hasBeenGenerated = reportGenerationService.getLastGenerated(Long.parseLong(sample.getId()))
                    .isPresent();

            if (generationStatus != null && !generationStatus.isEmpty() && !"all".equalsIgnoreCase(generationStatus)) {
                if ("generated".equalsIgnoreCase(generationStatus) && !hasBeenGenerated) {
                    continue;
                }
                if ("notGenerated".equalsIgnoreCase(generationStatus) && hasBeenGenerated) {
                    continue;
                }
            }

            ComplianceReportOrderDTO dto = buildOrderDTO(sample, primaryLink, eval, hasBeenGenerated);

            if (siteId != null && !siteId.isEmpty()) {
                if (!siteId.equals(dto.getSiteCode())) {
                    continue;
                }
            }

            orders.add(dto);

            if (hasBeenGenerated) {
                generatedCount++;
            } else {
                notYetGeneratedCount++;
            }
        }

        ComplianceReportDTO response = new ComplianceReportDTO();
        response.setIneligibleCount(ineligibleCount);
        response.setGeneratedCount(generatedCount);
        response.setNotYetGeneratedCount(notYetGeneratedCount);
        response.setOrders(orders);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/exportPdf")
    public void exportPdf(@RequestParam Long sampleId, HttpServletResponse response) throws IOException {
        Sample sample = sampleService.get(String.valueOf(sampleId));
        if (sample == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        List<SampleComplianceStandard> links = sampleComplianceStandardDAO.getAllForSample(sample.getId());
        SampleComplianceStandard primaryLink = links.isEmpty() ? null : links.get(0);
        ComplianceEvaluationResult eval = complianceEvaluationService.evaluate(sample);
        ComplianceReportOrderDTO dto = buildOrderDTO(sample, primaryLink, eval,
                reportGenerationService.getLastGenerated(sampleId).isPresent());

        String safeFilename = sample.getAccessionNumber().replaceAll("[^a-zA-Z0-9\\-]", "");
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=\"LH-" + safeFilename + ".pdf\"");

        byte[] pdfBytes;
        try {
            pdfBytes = buildOriginalPdfBytes(dto);
        } catch (DocumentException e) {
            LogEvent.logError(e);
            throw new IOException("Error generating PDF", e);
        }

        response.getOutputStream().write(pdfBytes);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userId = auth != null ? auth.getName() : null;
        reportGenerationService.recordGeneration(sampleId, userId);

        // Archive only on the release path — previews of unreleased samples must NOT be
        // archived. Failure here must never break PDF delivery.
        if (lhuAmendmentService.hasBeenReleased(sampleId)) {
            try {
                archiveService.archiveIfAbsent(sampleId, sample.getAmendmentNumber(), pdfBytes, userId);
            } catch (Exception e) {
                LogEvent.logError(e);
            }
        }
    }

    private byte[] buildOriginalPdfBytes(ComplianceReportOrderDTO dto) throws DocumentException, IOException {
        Font titleFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD);
        Font sectionFont = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD);
        Font headerFont = new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD, BaseColor.WHITE);
        Font cellFont = new Font(Font.FontFamily.HELVETICA, 9);
        Font labelFont = new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD);

        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, buf);
        document.open();

        document.add(new Phrase("LAPORAN HASIL — CERTIFICATE OF TEST RESULTS\n\n", titleFont));
        document.add(new Phrase("Lab Number: " + dto.getLabNumber() + "\n", cellFont));
        document.add(new Phrase("Standard: " + safe(dto.getStandardName()) + "\n", cellFont));
        document.add(new Phrase("Collection Date: " + safe(dto.getCollectionDate()) + "\n\n", cellFont));

        addTwoColSection(document, sectionFont, labelFont, cellFont, "SITE INFORMATION",
                new String[][] { { "Site", safe(dto.getSiteName()) },
                        { "GPS Coordinates", safe(dto.getGpsCoordinates()) },
                        { "Collection Date/Time", safe(dto.getCollectionDate()) },
                        { "Collection Method", safe(dto.getCollectionMethod()) },
                        { "PP No.", safe(dto.getRegulationNumber()) }, { "Standard", safe(dto.getStandardName()) } });

        addTwoColSection(document, sectionFont, labelFont, cellFont, "COLLECTION CONDITIONS",
                new String[][] { { "Water Temp", safe(dto.getWaterTemp()) },
                        { "Ambient Temp", safe(dto.getAmbientTemp()) }, { "Weather", safe(dto.getWeather()) },
                        { "Preservation", safe(dto.getPreservation()) } });

        addComplianceTable(document, dto, sectionFont, headerFont, cellFont);
        addSignatureTable(document, dto, sectionFont, labelFont, cellFont);

        document.close();
        return buf.toByteArray();
    }

    public static class ReissueRequest {
        private Long sampleId;
        private String reason;

        public Long getSampleId() {
            return sampleId;
        }

        public void setSampleId(Long sampleId) {
            this.sampleId = sampleId;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }

    @PostMapping("/reissue")
    public ResponseEntity<?> reissue(@RequestBody ReissueRequest request, HttpServletResponse response)
            throws IOException {
        if (request.getReason() == null || request.getReason().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Amendment reason must not be blank");
        }
        if (request.getSampleId() == null) {
            return ResponseEntity.badRequest().body("sampleId is required");
        }
        Sample sample = sampleService.get(String.valueOf(request.getSampleId()));
        if (sample == null) {
            return ResponseEntity.notFound().build();
        }
        if (!lhuAmendmentService.hasBeenReleased(request.getSampleId())) {
            return ResponseEntity.status(404).body("Sample has not been released; nothing to amend");
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userId = auth != null ? auth.getName() : null;

        // Backfill the original (amendment_number=0) archive if the sample was only
        // previewed before release and never formally archived on the exportPdf path.
        if (!archiveService.findBySampleIdAndAmendmentNumber(request.getSampleId(), 0).isPresent()) {
            try {
                List<SampleComplianceStandard> origLinks = sampleComplianceStandardDAO.getAllForSample(sample.getId());
                SampleComplianceStandard origPrimaryLink = origLinks.isEmpty() ? null : origLinks.get(0);
                ComplianceEvaluationResult origEval = complianceEvaluationService.evaluate(sample);
                ComplianceReportOrderDTO origDto = buildOrderDTO(sample, origPrimaryLink, origEval, true);
                byte[] origPdfBytes = buildOriginalPdfBytes(origDto);
                archiveService.archiveIfAbsent(request.getSampleId(), 0, origPdfBytes, userId);
            } catch (Exception e) {
                LogEvent.logError(e);
            }
        }

        String priorCertificateNumber = lhuAmendmentService
                .certificateNumberWithAmendmentSuffix(sample.getAccessionNumber(), sample.getAmendmentNumber());
        lhuAmendmentService.applyLhuAmendment(request.getSampleId(), priorCertificateNumber,
                request.getReason().trim());

        // Refresh sample to get updated amendmentNumber, then regenerate PDF
        sample = sampleService.get(String.valueOf(request.getSampleId()));
        List<SampleComplianceStandard> links = sampleComplianceStandardDAO.getAllForSample(sample.getId());
        SampleComplianceStandard primaryLink = links.isEmpty() ? null : links.get(0);
        ComplianceEvaluationResult eval = complianceEvaluationService.evaluate(sample);
        ComplianceReportOrderDTO dto = buildOrderDTO(sample, primaryLink, eval,
                reportGenerationService.getLastGenerated(request.getSampleId()).isPresent());

        String safeFilename = sample.getAccessionNumber().replaceAll("[^a-zA-Z0-9\\-]", "");
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"LH-" + safeFilename + "-Am" + sample.getAmendmentNumber() + ".pdf\"");

        byte[] pdfBytes;
        try {
            pdfBytes = buildAmendmentPdfBytes(sample, dto);
        } catch (DocumentException e) {
            LogEvent.logError(e);
            throw new IOException("Error generating amendment PDF", e);
        }

        response.getOutputStream().write(pdfBytes);

        reportGenerationService.recordGeneration(request.getSampleId(), userId);
        try {
            archiveService.archiveIfAbsent(request.getSampleId(), sample.getAmendmentNumber(), pdfBytes, userId);
        } catch (Exception e) {
            LogEvent.logError(e);
        }
        return null;
    }

    private byte[] buildAmendmentPdfBytes(Sample sample, ComplianceReportOrderDTO dto)
            throws DocumentException, IOException {
        Font titleFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD);
        Font sectionFont = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD);
        Font headerFont = new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD, BaseColor.WHITE);
        Font cellFont = new Font(Font.FontFamily.HELVETICA, 9);
        Font labelFont = new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD);

        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, buf);
        document.open();

        String certNumber = lhuAmendmentService.certificateNumberWithAmendmentSuffix(sample.getAccessionNumber(),
                sample.getAmendmentNumber());
        document.add(new Phrase("LAPORAN HASIL — CERTIFICATE OF TEST RESULTS (AMENDMENT)\n\n", titleFont));
        document.add(new Phrase("Certificate No.: " + certNumber + "\n", labelFont));
        document.add(new Phrase("Lab Number: " + dto.getLabNumber() + "\n", cellFont));

        if (sample.getAmendmentNumber() != null && sample.getAmendmentNumber() >= 1) {
            document.add(new Phrase("\nAMENDMENT NOTICE\n\n", sectionFont));
            document.add(new Phrase("Amendment No.: " + sample.getAmendmentNumber() + "\n", cellFont));
            document.add(new Phrase("Supersedes: " + safe(sample.getAmendsLhuNumber()) + "\n", cellFont));
            document.add(new Phrase("Amendment reason: " + safe(sample.getAmendmentReason()) + "\n\n", cellFont));
        }

        document.add(new Phrase("Standard: " + safe(dto.getStandardName()) + "\n", cellFont));
        document.add(new Phrase("Collection Date: " + safe(dto.getCollectionDate()) + "\n\n", cellFont));

        addTwoColSection(document, sectionFont, labelFont, cellFont, "SITE INFORMATION",
                new String[][] { { "Site", safe(dto.getSiteName()) },
                        { "GPS Coordinates", safe(dto.getGpsCoordinates()) },
                        { "Collection Date/Time", safe(dto.getCollectionDate()) },
                        { "Collection Method", safe(dto.getCollectionMethod()) },
                        { "PP No.", safe(dto.getRegulationNumber()) }, { "Standard", safe(dto.getStandardName()) } });

        addTwoColSection(document, sectionFont, labelFont, cellFont, "COLLECTION CONDITIONS",
                new String[][] { { "Water Temp", safe(dto.getWaterTemp()) },
                        { "Ambient Temp", safe(dto.getAmbientTemp()) }, { "Weather", safe(dto.getWeather()) },
                        { "Preservation", safe(dto.getPreservation()) } });

        addComplianceTable(document, dto, sectionFont, headerFont, cellFont);
        addSignatureTable(document, dto, sectionFont, labelFont, cellFont);

        document.close();
        return buf.toByteArray();
    }

    private ComplianceReportOrderDTO buildOrderDTO(Sample sample, SampleComplianceStandard link,
            ComplianceEvaluationResult eval, boolean hasBeenGenerated) {
        ComplianceReportOrderDTO dto = new ComplianceReportOrderDTO();
        dto.setSampleId(Long.parseLong(sample.getId()));
        dto.setLabNumber(sample.getAccessionNumber());

        if (link != null) {
            dto.setStandardName(link.getComplianceStandard().getName());
            dto.setRegulationNumber(link.getComplianceStandard().getRegulationNumber());
        }

        List<SampleItem> sampleItems = sampleItemService.getSampleItemsBySampleId(sample.getId());
        if (!sampleItems.isEmpty()) {
            SampleItem item = sampleItems.get(0);
            if (item.getCollectionDate() != null) {
                dto.setCollectionDate(new SimpleDateFormat("yyyy-MM-dd HH:mm").format(item.getCollectionDate()));
            }
        }

        String sampleId = sample.getId();

        // Site name: new orders save as envSamplingSiteName; orders created before
        // the frontend fix saved as vecCollectionSiteName — fall back to vec* if empty.
        String siteName = observationHistoryService.getRawValueForSample(ObservationType.ENV_SAMPLING_SITE_NAME,
                sampleId);
        if (siteName == null || siteName.isEmpty()) {
            siteName = observationHistoryService.getRawValueForSample(ObservationType.VS_COLLECTION_SITE_NAME,
                    sampleId);
        }
        dto.setSiteName(siteName);

        String siteIdStr = observationHistoryService.getRawValueForSample(ObservationType.ENV_SAMPLING_SITE_ID,
                sampleId);
        if (siteIdStr == null || siteIdStr.isEmpty()) {
            siteIdStr = observationHistoryService.getRawValueForSample(ObservationType.VS_COLLECTION_SITE_ID, sampleId);
        }
        dto.setSiteCode(siteIdStr);
        if (siteIdStr != null && !siteIdStr.isEmpty()) {
            try {
                org.openelisglobal.vector.valueholder.VectorSamplingSite site = vectorSamplingSiteService
                        .get(Integer.parseInt(siteIdStr));
                if (site != null) {
                    String lat = site.getGpsLatitude();
                    String lon = site.getGpsLongitude();
                    if (lat != null && !lat.isEmpty() && lon != null && !lon.isEmpty()) {
                        dto.setGpsCoordinates(lat + ", " + lon);
                    }
                }
            } catch (NumberFormatException ignored) {
            }
        }

        dto.setCollectionMethod(
                observationHistoryService.getRawValueForSample(ObservationType.ENV_COLLECTION_METHOD, sampleId));
        dto.setWaterTemp(observationHistoryService.getRawValueForSample(ObservationType.ENV_WATER_TEMP, sampleId));
        dto.setAmbientTemp(observationHistoryService.getRawValueForSample(ObservationType.ENV_AMBIENT_TEMP, sampleId));
        dto.setWeather(observationHistoryService.getRawValueForSample(ObservationType.ENV_WEATHER, sampleId));
        dto.setPreservation(
                observationHistoryService.getRawValueForSample(ObservationType.ENV_PRESERVATION_METHOD, sampleId));

        if (eval != null) {
            dto.setComplianceStatus(eval.getOverallStatus().name());
            dto.setParameterResults(eval.getParameterResults());
            dto.setTestCount(eval.getParameterResults().size());
        } else {
            dto.setComplianceStatus("NONE");
            dto.setTestCount(0);
        }

        reportGenerationService.getLastGenerated(Long.parseLong(sample.getId())).ifPresent(dto::setLastGenerated);
        dto.setHasBeenReleased(lhuAmendmentService.hasBeenReleased(Long.parseLong(sample.getId())));

        // Both RESULT_BATCH (AUTHORED) and VALIDATION_BATCH (VALIDATED_AND_RELEASED)
        // store record_id = analysis.id.
        List<org.openelisglobal.sampleitem.valueholder.SampleItem> items = sampleItemService
                .getSampleItemsBySampleId(sample.getId());
        for (org.openelisglobal.sampleitem.valueholder.SampleItem item : items) {
            List<org.openelisglobal.analysis.valueholder.Analysis> analyses = analysisService
                    .getAnalysesBySampleItem(item);
            for (org.openelisglobal.analysis.valueholder.Analysis analysis : analyses) {
                if (dto.getAnalystSignature() == null) {
                    List<ElectronicSignature> sigs = electronicSignatureService.getSignaturesForRecord("RESULT_BATCH",
                            Long.parseLong(analysis.getId()));
                    for (ElectronicSignature sig : sigs) {
                        if (sig.getSignatureMeaning() == SignatureMeaning.AUTHORED) {
                            dto.setAnalystSignature(toSignatureDTO(sig, "Lab Analyst"));
                            break;
                        }
                    }
                }
                if (dto.getManagerSignature() == null) {
                    List<ElectronicSignature> sigs = electronicSignatureService
                            .getSignaturesForRecord("VALIDATION_BATCH", Long.parseLong(analysis.getId()));
                    for (ElectronicSignature sig : sigs) {
                        if (sig.getSignatureMeaning() == SignatureMeaning.VALIDATED_AND_RELEASED) {
                            dto.setManagerSignature(toSignatureDTO(sig, "Lab Manager"));
                            break;
                        }
                    }
                }
                if (dto.getAnalystSignature() != null && dto.getManagerSignature() != null)
                    break;
            }
            if (dto.getAnalystSignature() != null && dto.getManagerSignature() != null)
                break;
        }

        return dto;
    }

    private ComplianceReportOrderDTO.SignatureDTO toSignatureDTO(ElectronicSignature sig, String role) {
        ComplianceReportOrderDTO.SignatureDTO dto = new ComplianceReportOrderDTO.SignatureDTO();
        dto.setSignerName(sig.getSignerNamePrinted());
        dto.setSignerRole(role);
        if (sig.getSignedAt() != null) {
            dto.setSignedAt(sig.getSignedAt().toLocalDateTime().format(DISPLAY_FMT));
        }
        return dto;
    }

    private void addComplianceTable(Document doc, ComplianceReportOrderDTO dto, Font sectionFont, Font headerFont,
            Font cellFont) throws DocumentException {
        doc.add(new Phrase("\nCOMPLIANCE SUMMARY\n\n", sectionFont));
        PdfPTable compTable = new PdfPTable(4);
        compTable.setWidthPercentage(100);
        compTable.setWidths(new float[] { 3f, 2f, 2.5f, 1.5f });
        for (String h : new String[] { "Parameter", "Result", "Threshold", "Status" }) {
            PdfPCell hCell = new PdfPCell(new Phrase(h, headerFont));
            hCell.setBackgroundColor(new BaseColor(33, 82, 149));
            hCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            hCell.setPadding(4);
            compTable.addCell(hCell);
        }
        if (dto.getParameterResults() != null) {
            for (ComplianceEvaluationResult.ParameterResult pr : dto.getParameterResults()) {
                compTable.addCell(new Phrase(safe(pr.getDisplayName()), cellFont));
                String rv = safe(pr.getResultValue());
                if (pr.getUnits() != null && !pr.getUnits().isEmpty()) {
                    rv += " " + pr.getUnits();
                }
                compTable.addCell(new Phrase(rv, cellFont));
                compTable.addCell(new Phrase(safe(pr.getThresholdDisplay()), cellFont));
                compTable.addCell(new Phrase(pr.getStatus() != null ? pr.getStatus().toString() : "–", cellFont));
            }
        }
        doc.add(compTable);
    }

    private void addSignatureTable(Document doc, ComplianceReportOrderDTO dto, Font sectionFont, Font labelFont,
            Font cellFont) throws DocumentException {
        doc.add(new Phrase("\n\nSIGNATURES\n\n", sectionFont));
        PdfPTable sigTable = new PdfPTable(2);
        sigTable.setWidthPercentage(100);
        sigTable.addCell(buildSignatureCell(dto.getAnalystSignature(), "Lab Analyst", labelFont, cellFont));
        sigTable.addCell(buildSignatureCell(dto.getManagerSignature(), "Lab Manager", labelFont, cellFont));
        doc.add(sigTable);
    }

    private void addTwoColSection(Document doc, Font sectionFont, Font labelFont, Font cellFont, String title,
            String[][] rows) throws DocumentException {
        doc.add(new Phrase("\n" + title + "\n\n", sectionFont));
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[] { 2f, 4f });
        for (String[] row : rows) {
            table.addCell(new Phrase(row[0], labelFont));
            table.addCell(new Phrase(row[1], cellFont));
        }
        doc.add(table);
    }

    private PdfPCell buildSignatureCell(ComplianceReportOrderDTO.SignatureDTO sig, String defaultRole, Font labelFont,
            Font cellFont) {
        PdfPCell cell = new PdfPCell();
        cell.setPadding(8);
        if (sig != null) {
            cell.addElement(new Phrase(safe(sig.getSignerName()), labelFont));
            cell.addElement(new Phrase(safe(sig.getSignerRole()), cellFont));
            cell.addElement(new Phrase(safe(sig.getSignedAt()), cellFont));
        } else {
            cell.addElement(new Phrase(defaultRole + "\n–", cellFont));
        }
        return cell;
    }

    private String safe(String s) {
        return s != null ? s : "–";
    }

    private String toSystemDateFormat(String isoDate) {
        if (isoDate == null || isoDate.isEmpty()) {
            return null;
        }
        try {
            java.time.LocalDate d = java.time.LocalDate.parse(isoDate);
            return formatToSystemDate(d);
        } catch (java.time.format.DateTimeParseException e) {
            return null;
        }
    }

    private String formatToSystemDate(java.time.LocalDate date) {
        return date.format(java.time.format.DateTimeFormatter.ofPattern(DateUtil.getDateFormat()));
    }
}
