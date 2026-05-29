package org.openelisglobal.notebook.service;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.notebook.valueholder.NoteBookPage;
import org.openelisglobal.notebook.valueholder.NotebookEntry;
import org.openelisglobal.notebook.valueholder.NotebookPageSample;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PathologyDiagnosticReportServiceImpl implements PathologyDiagnosticReportService {

    private static final String LOG_CLASS = "PathologyDiagnosticReportServiceImpl";

    // Dark blue matching the reference PDF: #2B4F87
    private static final BaseColor DARK_BLUE = new BaseColor(43, 79, 135);
    private static final BaseColor LIGHT_GRAY = new BaseColor(191, 191, 191);
    private static final BaseColor HEADER_GRAY = new BaseColor(127, 127, 127);
    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DISPLAY_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Autowired
    private NotebookEntryService notebookEntryService;

    @Autowired
    private NoteBookPageService noteBookPageService;

    @Autowired
    private NotebookPageSampleService notebookPageSampleService;

    @Autowired
    private SampleItemService sampleItemService;

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getPatientList(Integer entryId) {
        NotebookEntry entry = notebookEntryService.get(entryId);
        if (entry == null || entry.getNotebook() == null) {
            return List.of();
        }

        Integer notebookId = entry.getNotebook().getId();
        List<NoteBookPage> pages = noteBookPageService.getByNotebookId(notebookId);

        NoteBookPage page1 = findPageByOrder(pages, 1);
        if (page1 == null) {
            return List.of();
        }

        // Microscopy & Diagnosis: order 9 in 13-stage workflow; order 8 in legacy templates
        NoteBookPage microscopyPage = findMicroscopyDiagnosisPage(pages);

        List<NotebookPageSample> page1Samples = notebookPageSampleService.getByPageId(page1.getId());

        // Group samples by patient identity
        Map<String, List<NotebookPageSample>> patientGroups = new LinkedHashMap<>();
        Map<String, Map<String, String>> patientInfo = new LinkedHashMap<>();

        for (NotebookPageSample sample : page1Samples) {
            Map<String, Object> data = sample.getData();
            if (data == null) {
                continue;
            }

            String firstName = getString(data, "firstName");
            String surname = getString(data, "surname");
            String nationalId = getString(data, "nationalId");

            String patientKey = buildPatientKey(firstName, surname, nationalId);
            if (patientKey.isEmpty()) {
                continue;
            }

            patientGroups.computeIfAbsent(patientKey, k -> new ArrayList<>()).add(sample);
            patientInfo.computeIfAbsent(patientKey, k -> {
                Map<String, String> info = new LinkedHashMap<>();
                info.put("firstName", firstName);
                info.put("surname", surname);
                info.put("nationalId", nationalId);
                return info;
            });
        }

        // Build result list
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, List<NotebookPageSample>> group : patientGroups.entrySet()) {
            String key = group.getKey();
            List<NotebookPageSample> samples = group.getValue();
            Map<String, String> info = patientInfo.get(key);

            List<String> sampleIds = samples.stream().map(NotebookPageSample::getSampleItemId)
                    .collect(Collectors.toList());

            boolean hasDiagnosis = false;
            if (microscopyPage != null) {
                hasDiagnosis = checkHasDiagnosis(microscopyPage.getId(), sampleIds);
            }

            Map<String, Object> patient = new LinkedHashMap<>();
            patient.put("patientKey", key);
            patient.put("firstName", info.get("firstName"));
            patient.put("surname", info.get("surname"));
            patient.put("nationalId", info.get("nationalId"));
            patient.put("sampleCount", samples.size());
            patient.put("sampleIds", sampleIds);
            patient.put("hasDiagnosis", hasDiagnosis);
            result.add(patient);
        }

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generateDiagnosticReportPdf(Integer entryId, String patientKey) throws Exception {
        NotebookEntry entry = notebookEntryService.get(entryId);
        if (entry == null || entry.getNotebook() == null) {
            throw new IllegalArgumentException("Notebook entry not found: " + entryId);
        }

        Integer notebookId = entry.getNotebook().getId();
        List<NoteBookPage> pages = noteBookPageService.getByNotebookId(notebookId);

        NoteBookPage page1 = findPageByOrder(pages, 1);
        NoteBookPage page3 = findPageByOrder(pages, 3);
        NoteBookPage microscopyPage = findMicroscopyDiagnosisPage(pages);

        if (page1 == null) {
            throw new IllegalStateException("Sample Creation page not found");
        }

        // Find patient's samples on Page 1
        List<NotebookPageSample> page1Samples = notebookPageSampleService.getByPageId(page1.getId());
        List<NotebookPageSample> patientSamples = page1Samples.stream().filter(s -> {
            Map<String, Object> data = s.getData();
            if (data == null)
                return false;
            String key = buildPatientKey(getString(data, "firstName"), getString(data, "surname"),
                    getString(data, "nationalId"));
            return patientKey.equals(key);
        }).collect(Collectors.toList());

        if (patientSamples.isEmpty()) {
            throw new IllegalArgumentException("No samples found for patient: " + patientKey);
        }

        // Collect data across pages for each sample
        List<Map<String, Object>> sampleDataList = new ArrayList<>();
        for (NotebookPageSample page1Sample : patientSamples) {
            String sampleItemId = page1Sample.getSampleItemId();
            Map<String, Object> combinedData = new LinkedHashMap<>();

            // Page 1 data (patient details, specimen info)
            if (page1Sample.getData() != null) {
                combinedData.putAll(page1Sample.getData());
            }

            // Resolve the real accession number / sample id from the SampleItem when the
            // captured metadata does not carry them (e.g. research specimens). The
            // notebook page sample id is the SampleItem id, so the accession lives on the
            // associated Sample rather than in the page JSON.
            populateAccessionDetails(combinedData, sampleItemId);

            // Page 3 data (gross examination)
            if (page3 != null) {
                NotebookPageSample grossSample = notebookPageSampleService.getBySampleItemIdAndPageId(sampleItemId,
                        page3.getId());
                if (grossSample != null && grossSample.getData() != null) {
                    prefixAndMerge(combinedData, grossSample.getData(), "gross_");
                }
            }

            // Microscopy & diagnosis page data - handle composite IDs
            if (microscopyPage != null) {
                Map<String, Object> diagnosisData = findBestDiagnosisData(microscopyPage.getId(), sampleItemId);
                if (diagnosisData != null) {
                    prefixAndMerge(combinedData, diagnosisData, "diag_");
                }
            }

            sampleDataList.add(combinedData);
        }

        return buildPdf(sampleDataList);
    }

    // ========================================
    // PDF GENERATION
    // ========================================

    private byte[] buildPdf(List<Map<String, Object>> sampleDataList) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 40, 40, 40, 40);
        PdfWriter.getInstance(document, baos);
        document.open();

        // Use the first sample's data as the primary source for patient info
        Map<String, Object> primaryData = sampleDataList.get(0);

        // Fonts
        Font titleFont = new Font(Font.FontFamily.HELVETICA, 13, Font.BOLD, BaseColor.WHITE);
        Font sectionFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, BaseColor.WHITE);
        Font headerFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, HEADER_GRAY);
        Font labelFont = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL);
        Font valueFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);
        Font nameFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
        Font smallFont = new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL);
        Font signLabelFont = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD);
        Font signValueFont = new Font(Font.FontFamily.HELVETICA, 11, Font.NORMAL);

        addInstitutionBanner(document, nameFont, smallFont);

        // Title banner - "PATHOLOGY DIAGNOSTIC REPORT"
        PdfPTable titleBar = new PdfPTable(1);
        titleBar.setWidthPercentage(100);
        titleBar.setSpacingAfter(8);
        PdfPCell titleCell = new PdfPCell(new Phrase("PATHOLOGY DIAGNOSTIC REPORT", titleFont));
        titleCell.setBackgroundColor(DARK_BLUE);
        titleCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        titleCell.setPadding(8);
        titleCell.setBorderColor(DARK_BLUE);
        titleBar.addCell(titleCell);
        document.add(titleBar);

        // 3. Three-column info table: Patient | Specimen | Ordering Physician
        PdfPTable infoTable = new PdfPTable(3);
        infoTable.setWidthPercentage(100);
        infoTable.setWidths(new float[] { 35f, 30f, 35f });
        infoTable.setSpacingAfter(12);

        // Column headers
        addColumnHeader(infoTable, "Patient Details", headerFont);
        addColumnHeader(infoTable, "Specimen Details", headerFont);
        addColumnHeader(infoTable, "Ordering Physician", headerFont);

        // Patient Details column
        StringBuilder patientCol = new StringBuilder();
        String fullName = buildFullName(getString(primaryData, "firstName"), getString(primaryData, "surname"));
        patientCol.append(fullName).append("\n");
        patientCol.append("MRN: ").append(getString(primaryData, "nationalId")).append("\n");
        patientCol.append("Phone: ").append(getString(primaryData, "phone")).append("\n");
        patientCol.append("DOB: ").append(formatDisplayDate(dateFieldRaw(primaryData, "dateOfBirth"))).append("\n");
        patientCol.append("Sex: ").append(getString(primaryData, "sex"));

        // Specimen Details column - aggregate across all samples
        StringBuilder specimenCol = new StringBuilder();
        for (int i = 0; i < sampleDataList.size(); i++) {
            Map<String, Object> sd = sampleDataList.get(i);
            if (i > 0) {
                specimenCol.append("\n---\n");
            }
            specimenCol.append("Accession: ").append(getString(sd, "accessionNumber")).append("\n");
            specimenCol.append("Sample ID: ").append(getString(sd, "externalId")).append("\n");
            specimenCol.append("Procedure Date: ")
                    .append(formatDisplayDateTime(dateFieldRaw(sd, "collectionDateTime")))
                    .append("\n");
            specimenCol.append("Received Date: ")
                    .append(formatDisplayDateTime(dateFieldRaw(sd, "receivedDateTime")))
                    .append("\n");
            specimenCol.append("Reported Date: ")
                    .append(formatDisplayDateTime(dateFieldRaw(sd, "diag_verificationDate")));
        }

        // Ordering physician / provider (matches institutional letterhead)
        StringBuilder physicianCol = new StringBuilder();
        physicianCol.append("Ordering physician:\n");
        physicianCol.append(getString(primaryData, "requestingClinician")).append("\n\n");
        physicianCol.append("Provider / facility:\n");
        physicianCol.append("AHRI\n");
        physicianCol.append("Jimma Street, Addis Ababa, Ethiopia\n\n");
        physicianCol.append(getString(primaryData, "sourceFacility")).append("\n");
        physicianCol.append("Clinical details:\n");
        physicianCol.append(getString(primaryData, "clinicalDetails"));

        addInfoCell(infoTable, patientCol.toString(), valueFont);
        addInfoCell(infoTable, specimenCol.toString(), valueFont);
        addInfoCell(infoTable, physicianCol.toString(), valueFont);

        document.add(infoTable);

        // For each sample, add the diagnostic sections
        for (int i = 0; i < sampleDataList.size(); i++) {
            Map<String, Object> sd = sampleDataList.get(i);

            // Sample separator for multi-sample reports
            if (sampleDataList.size() > 1) {
                String specimenLabel = "Specimen " + (i + 1) + ": " + getString(sd, "specimenType") + " - "
                        + getString(sd, "specimenSite") + " (" + getString(sd, "accessionNumber") + ")";
                Paragraph specimenHeader = new Paragraph(specimenLabel, nameFont);
                specimenHeader.setSpacingBefore(10);
                specimenHeader.setSpacingAfter(6);
                document.add(specimenHeader);
            }

            // 4. DIAGNOSIS section
            addSectionHeader(document, "DIAGNOSIS", sectionFont);
            addDiagnosisContent(document, sd, valueFont, labelFont);

            // 5. MICROSCOPIC DESCRIPTION section
            addSectionHeader(document, "MICROSCOPIC DESCRIPTION", sectionFont);
            addMicroscopicContent(document, sd, valueFont, labelFont);

            // 6. MACROSCOPIC (GROSS) EXAMINATION section
            addSectionHeader(document, "MACROSCOPIC (GROSS) EXAMINATION", sectionFont);
            addGrossContent(document, sd, valueFont);
        }

        // 7. Pathologist signature block (prefer verifying pathologist from any finalized/specimen row)
        document.add(Chunk.NEWLINE);
        String[] pathologistTriple = resolvePathologistAttestation(sampleDataList);
        String pathologistName = pathologistTriple[0];
        String credentials = pathologistTriple[1];
        String pathologistSignature = pathologistTriple[2];
        String displayName = pathologistName;
        if (!credentials.isEmpty()) {
            displayName += ", " + credentials;
        }

        Paragraph sigBlock = new Paragraph();
        sigBlock.setSpacingBefore(20);
        sigBlock.add(new Chunk("Pathologist Name: ", signLabelFont));
        sigBlock.add(new Chunk(displayName, signValueFont));
        sigBlock.add(Chunk.NEWLINE);
        sigBlock.add(Chunk.NEWLINE);
        sigBlock.add(new Chunk("Signature: ", signLabelFont));
        sigBlock.add(new Chunk(pathologistSignature, signValueFont));
        document.add(sigBlock);

        document.close();
        return baos.toByteArray();
    }

    private void addSectionHeader(Document document, String title, Font font) throws Exception {
        PdfPTable bar = new PdfPTable(1);
        bar.setWidthPercentage(100);
        bar.setSpacingBefore(8);
        bar.setSpacingAfter(6);
        PdfPCell cell = new PdfPCell(new Phrase(title, font));
        cell.setBackgroundColor(DARK_BLUE);
        cell.setPadding(6);
        cell.setBorderColor(DARK_BLUE);
        bar.addCell(cell);
        document.add(bar);
    }

    private void addDiagnosisContent(Document document, Map<String, Object> data, Font valueFont, Font labelFont)
            throws Exception {
        // Fall back to the working diagnosis fields when no finalized diagnosis was
        // entered yet, so partially completed cases still render the available text.
        String finalDiagnosis = firstNonEmpty(getString(data, "diag_finalDiagnosis"),
                getString(data, "diag_initialImpression"), getString(data, "diag_differentialDiagnosis"));
        if (!finalDiagnosis.isEmpty()) {
            document.add(new Paragraph(finalDiagnosis, valueFont));
        }

        // Additional diagnosis details
        StringBuilder details = new StringBuilder();
        appendIfPresent(details, "Diagnosis Code", getString(data, "diag_diagnosisCode"));
        appendIfPresent(details, "Tumor Type", getString(data, "diag_tumorType"));
        appendIfPresent(details, "Histologic Grade", getString(data, "diag_histologicGrade"));
        appendIfPresent(details, "Tumor Stage", getString(data, "diag_tumorStage"));
        appendIfPresent(details, "Margin Status", getString(data, "diag_marginStatus"));
        appendIfPresent(details, "Lymphovascular Invasion", getString(data, "diag_lymphovascularInvasion"));
        appendIfPresent(details, "Perineural Invasion", getString(data, "diag_perineuralInvasion"));

        if (details.length() > 0) {
            Paragraph detailsPara = new Paragraph(details.toString(), labelFont);
            detailsPara.setSpacingBefore(4);
            document.add(detailsPara);
        }

        if (finalDiagnosis.isEmpty() && details.length() == 0) {
            Paragraph pending = new Paragraph("Diagnosis pending.", labelFont);
            pending.setSpacingBefore(4);
            document.add(pending);
        }
    }

    private void addMicroscopicContent(Document document, Map<String, Object> data, Font valueFont, Font labelFont)
            throws Exception {
        // The diagnosis form stores "microscopicDescription"; the microscopy test form
        // stores observations under "microscopicFindings". Render whichever is present.
        String description = firstNonEmpty(getString(data, "diag_microscopicDescription"),
                getString(data, "diag_microscopicFindings"));
        if (!description.isEmpty()) {
            document.add(new Paragraph(description, valueFont));
        }

        StringBuilder details = new StringBuilder();
        appendIfPresent(details, "Cellular Features", getString(data, "diag_cellularFeatures"));
        appendIfPresent(details, "Architectural Findings", getString(data, "diag_architecturalFindings"));
        appendIfPresent(details, "Nuclear Features", getString(data, "diag_nuclearFeatures"));
        appendIfPresent(details, "Stromal Findings", getString(data, "diag_stromalFindings"));
        appendIfPresent(details, "Special Stain Results", getString(data, "diag_specialStainResults"));
        appendIfPresent(details, "IHC Results", getString(data, "diag_ihcResults"));

        if (details.length() > 0) {
            Paragraph detailsPara = new Paragraph(details.toString(), labelFont);
            detailsPara.setSpacingBefore(4);
            document.add(detailsPara);
        }

        // Try to embed a slide image
        tryEmbedImage(document, data, "diag_finalSlideImages");
        if (description.isEmpty() && details.length() == 0) {
            tryEmbedImage(document, data, "diag_initialSlideImages");
        }

        if (description.isEmpty() && details.length() == 0) {
            Paragraph pending = new Paragraph("Microscopic examination pending.", labelFont);
            pending.setSpacingBefore(4);
            document.add(pending);
        }
    }

    private void addGrossContent(Document document, Map<String, Object> data, Font valueFont) throws Exception {
        String grossDescription = getString(data, "gross_grossDescription");
        if (!grossDescription.isEmpty()) {
            document.add(new Paragraph(grossDescription, valueFont));
        } else {
            // Fall back to structured gross data
            StringBuilder gross = new StringBuilder();
            appendIfPresent(gross, "Specimen Received", getString(data, "gross_specimenReceived"));
            appendIfPresent(gross, "Description", getString(data, "gross_specimenDescription"));

            String length = getString(data, "gross_dimensionLength");
            String width = getString(data, "gross_dimensionWidth");
            String height = getString(data, "gross_dimensionHeight");
            String unit = getString(data, "gross_dimensionUnit");
            if (!length.isEmpty()) {
                gross.append("Dimensions: ").append(length);
                if (!width.isEmpty())
                    gross.append(" x ").append(width);
                if (!height.isEmpty())
                    gross.append(" x ").append(height);
                if (!unit.isEmpty())
                    gross.append(" ").append(unit);
                gross.append("\n");
            }

            appendIfPresent(gross, "Weight",
                    getString(data, "gross_specimenWeight") + (!getString(data, "gross_weightUnit").isEmpty()
                            ? " " + getString(data, "gross_weightUnit")
                            : ""));
            appendIfPresent(gross, "Color", getString(data, "gross_color"));
            appendIfPresent(gross, "Texture", getString(data, "gross_texture"));
            appendIfPresent(gross, "Margins", getString(data, "gross_margins"));
            appendIfPresent(gross, "Abnormalities", getString(data, "gross_abnormalities"));

            if (gross.length() > 0) {
                document.add(new Paragraph(gross.toString(), valueFont));
            } else {
                Font labelFont = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL);
                document.add(new Paragraph("Gross examination pending.", labelFont));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void tryEmbedImage(Document document, Map<String, Object> data, String fieldName) {
        try {
            Object imagesObj = data.get(fieldName);
            if (imagesObj instanceof List) {
                List<Object> images = (List<Object>) imagesObj;
                if (!images.isEmpty()) {
                    Object firstImage = images.get(0);
                    String base64Data = null;
                    if (firstImage instanceof Map) {
                        base64Data = getString((Map<String, Object>) firstImage, "base64Data");
                    } else if (firstImage instanceof String) {
                        base64Data = (String) firstImage;
                    }

                    if (base64Data != null && !base64Data.isEmpty()) {
                        // Strip data URI prefix if present
                        if (base64Data.contains(",")) {
                            base64Data = base64Data.substring(base64Data.indexOf(",") + 1);
                        }
                        byte[] imageBytes = Base64.getDecoder().decode(base64Data);
                        Image img = Image.getInstance(imageBytes);
                        img.scaleToFit(200, 150);
                        img.setAlignment(Element.ALIGN_RIGHT);
                        img.setSpacingBefore(4);
                        document.add(img);
                    }
                }
            }
        } catch (Exception e) {
            LogEvent.logWarn(LOG_CLASS, "tryEmbedImage",
                    "Could not embed image from " + fieldName + ": " + e.getMessage());
        }
    }

    // ========================================
    // HELPER METHODS
    // ========================================

    private NoteBookPage findPageByOrder(List<NoteBookPage> pages, int order) {
        // Resolve by canonical stage order (title-based) first so subtype layouts
        // (FNAC / Cytology) whose physical page order differs from the 13-stage
        // canonical order still map to the correct stage. Without this, the report
        // reads the wrong page and renders "pending" for diagnosis/microscopy/gross.
        NoteBookPage byCanonical = pages.stream().filter(p -> {
            Integer canonical = PathologyWorkflowTypeConfig.canonicalStageOrder(p.getTitle(), p.getOrder());
            return canonical != null && canonical == order;
        }).findFirst().orElse(null);
        if (byCanonical != null) {
            return byCanonical;
        }
        return pages.stream().filter(p -> p.getOrder() != null && p.getOrder() == order).findFirst().orElse(null);
    }

    /**
     * Resolves the notebook page that holds microscopy / diagnosis data. Current
     * pathology templates use order 9 (13-stage workflow); older templates used 8.
     */
    private NoteBookPage findMicroscopyDiagnosisPage(List<NoteBookPage> pages) {
        NoteBookPage ninth = findPageByOrder(pages, 9);
        if (ninth != null) {
            return ninth;
        }
        return findPageByOrder(pages, 8);
    }

    private String buildPatientKey(String firstName, String surname, String nationalId) {
        if (nationalId != null && !nationalId.isBlank()) {
            return "NID-" + nationalId.trim();
        }
        if (firstName != null && !firstName.isBlank()) {
            String key = firstName.trim();
            if (surname != null && !surname.isBlank()) {
                key += "-" + surname.trim();
            }
            return key;
        }
        return "";
    }

    private String buildFullName(String firstName, String surname) {
        StringBuilder name = new StringBuilder();
        if (!firstName.isEmpty()) {
            name.append(firstName);
        }
        if (!surname.isEmpty()) {
            if (name.length() > 0)
                name.append(" ");
            name.append(surname);
        }
        return name.length() > 0 ? name.toString() : "Unknown";
    }

    private boolean checkHasDiagnosis(Integer page8Id, List<String> sampleIds) {
        List<NotebookPageSample> page8Samples = notebookPageSampleService.getByPageId(page8Id);
        for (NotebookPageSample ps : page8Samples) {
            String psId = ps.getSampleItemId();
            for (String rootId : sampleIds) {
                if (psId.equals(rootId) || psId.startsWith(rootId + "_")) {
                    if (ps.getData() != null) {
                        String diag = firstNonEmpty(getString(ps.getData(), "finalDiagnosis"),
                                getString(ps.getData(), "microscopicDescription"),
                                getString(ps.getData(), "microscopicFindings"),
                                getString(ps.getData(), "initialImpression"));
                        if (!diag.isEmpty()) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Find the best diagnosis data for a sample on the microscopy page. Handles
     * composite IDs (e.g., "4_cassette_0_block_0_slide_0"). Microscopy/diagnosis
     * data for one specimen can be spread across several composite rows (e.g. the
     * root row holds microscopy findings while a slide row holds staining details),
     * so the matching rows are merged into a single map. Rows that are finalized or
     * carry a final diagnosis are applied last so their values take precedence.
     */
    private Map<String, Object> findBestDiagnosisData(Integer page8Id, String rootSampleId) {
        List<NotebookPageSample> page8Samples = notebookPageSampleService.getByPageId(page8Id);

        List<NotebookPageSample> matching = page8Samples.stream().filter(s -> {
            String id = s.getSampleItemId();
            return id.equals(rootSampleId) || id.startsWith(rootSampleId + "_");
        }).filter(s -> s.getData() != null).collect(Collectors.toList());

        if (matching.isEmpty()) {
            return null;
        }

        // Lowest priority first, highest last, so the most authoritative values win.
        matching.sort(Comparator.comparingInt(ps -> {
            Map<String, Object> data = ps.getData();
            if (Boolean.TRUE.equals(data.get("reportFinalized"))) {
                return 2;
            }
            if (!getString(data, "finalDiagnosis").isEmpty()) {
                return 1;
            }
            return 0;
        }));

        Map<String, Object> merged = new LinkedHashMap<>();
        for (NotebookPageSample ps : matching) {
            for (Map.Entry<String, Object> entry : ps.getData().entrySet()) {
                Object value = entry.getValue();
                if (value == null) {
                    continue;
                }
                if (value instanceof String && ((String) value).trim().isEmpty()) {
                    continue;
                }
                merged.put(entry.getKey(), value);
            }
        }
        return merged;
    }

    /**
     * Ensures the report has an accession number and sample id. These are stored on
     * the {@link SampleItem}/{@link org.openelisglobal.sample.valueholder.Sample}
     * rather than in the notebook page JSON, so they are looked up by the page
     * sample id (which is the SampleItem id). Existing JSON values take precedence.
     */
    private void populateAccessionDetails(Map<String, Object> combinedData, String sampleItemId) {
        if (sampleItemId == null || sampleItemId.isBlank()) {
            return;
        }
        boolean needsAccession = getString(combinedData, "accessionNumber").isEmpty();
        boolean needsSampleId = getString(combinedData, "externalId").isEmpty();
        if (!needsAccession && !needsSampleId) {
            return;
        }

        String rootSampleId = sampleItemId.contains("_") ? sampleItemId.split("_")[0] : sampleItemId;
        try {
            SampleItem sampleItem = sampleItemService.getData(rootSampleId);
            if (sampleItem == null || sampleItem.getSample() == null) {
                return;
            }
            String accession = sampleItem.getSample().getAccessionNumber();
            if (accession != null && !accession.isBlank()) {
                if (needsAccession) {
                    combinedData.put("accessionNumber", accession);
                }
                if (needsSampleId) {
                    combinedData.put("externalId", accession);
                }
            }
        } catch (Exception e) {
            LogEvent.logWarn(LOG_CLASS, "populateAccessionDetails",
                    "Could not resolve accession for sample item " + rootSampleId);
        }
    }

    private void prefixAndMerge(Map<String, Object> target, Map<String, Object> source, String prefix) {
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            target.put(prefix + entry.getKey(), entry.getValue());
        }
    }

    private String getString(Map<String, Object> data, String key) {
        if (data == null)
            return "";
        Object val = data.get(key);
        return val != null ? val.toString().trim() : "";
    }

    private String firstNonEmpty(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return "";
    }

    /**
     * Normalizes JSON date fields that may arrive as String, epoch number, or legacy {@code Date}.
     */
    private String dateFieldRaw(Map<String, Object> data, String key) {
        if (data == null) {
            return "";
        }
        Object val = data.get(key);
        if (val == null) {
            return "";
        }
        if (val instanceof String) {
            return ((String) val).trim();
        }
        if (val instanceof java.util.Date) {
            return Instant.ofEpochMilli(((java.util.Date) val).getTime()).toString();
        }
        if (val instanceof Instant) {
            return val.toString();
        }
        if (val instanceof OffsetDateTime) {
            return ((OffsetDateTime) val).toString();
        }
        if (val instanceof LocalDateTime) {
            return ((LocalDateTime) val).toString();
        }
        if (val instanceof Number) {
            long n = ((Number) val).longValue();
            if (Math.abs(n) > 1_000_000_000_000L) {
                return Instant.ofEpochMilli(n).toString();
            }
            if (Math.abs(n) > 1_000_000_000L) {
                return Instant.ofEpochSecond(n).toString();
            }
        }
        return val.toString().trim();
    }

    /**
     * Pathologist line for the letter: prefer verifying pathologist on a finalized report row, then any
     * verifying name, then diagnosing pathologist; credentials and signature follow the chosen row.
     */
    private String[] resolvePathologistAttestation(List<Map<String, Object>> sampleDataList) {
        for (Map<String, Object> sd : sampleDataList) {
            if (Boolean.TRUE.equals(sd.get("diag_reportFinalized"))) {
                String verifying = getString(sd, "diag_verifyingPathologistName");
                if (!verifying.isEmpty()) {
                    return new String[] { verifying, getString(sd, "diag_pathologistCredentials"),
                            getString(sd, "diag_pathologistSignature") };
                }
            }
        }
        for (Map<String, Object> sd : sampleDataList) {
            String verifying = getString(sd, "diag_verifyingPathologistName");
            if (!verifying.isEmpty()) {
                return new String[] { verifying, getString(sd, "diag_pathologistCredentials"),
                        getString(sd, "diag_pathologistSignature") };
            }
        }
        for (Map<String, Object> sd : sampleDataList) {
            String diagnosing = getString(sd, "diag_diagnosingPathologist");
            if (!diagnosing.isEmpty()) {
                return new String[] { diagnosing, getString(sd, "diag_pathologistCredentials"),
                        getString(sd, "diag_pathologistSignature") };
            }
        }
        return new String[] { "", "", "" };
    }

    private String formatDisplayDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String trimmed = raw.trim();
        try {
            return LocalDate.parse(trimmed).format(DISPLAY_DATE);
        } catch (DateTimeParseException ignored) {
        }
        try {
            return Instant.parse(trimmed).atZone(ZoneOffset.UTC).toLocalDate().format(DISPLAY_DATE);
        } catch (DateTimeParseException ignored) {
        }
        try {
            return LocalDateTime.parse(trimmed.replace(' ', 'T')).format(DISPLAY_DATE);
        } catch (DateTimeParseException ignored) {
        }
        try {
            return OffsetDateTime.parse(trimmed.replace(' ', 'T')).toLocalDate().format(DISPLAY_DATE);
        } catch (DateTimeParseException ignored) {
        }
        return stripFractionalSeconds(trimmed);
    }

    private String formatDisplayDateTime(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String trimmed = raw.trim();
        try {
            return Instant.parse(trimmed).atZone(ZoneOffset.UTC).toLocalDateTime().format(DISPLAY_DATE_TIME);
        } catch (DateTimeParseException ignored) {
        }
        try {
            return LocalDateTime.parse(trimmed.replace(' ', 'T')).format(DISPLAY_DATE_TIME);
        } catch (DateTimeParseException ignored) {
        }
        try {
            return OffsetDateTime.parse(trimmed.replace(' ', 'T')).format(DISPLAY_DATE_TIME);
        } catch (DateTimeParseException ignored) {
        }
        try {
            return LocalDate.parse(trimmed).format(DISPLAY_DATE);
        } catch (DateTimeParseException ignored) {
        }
        return stripFractionalSeconds(trimmed);
    }

    private String stripFractionalSeconds(String value) {
        return value.replace('T', ' ').replaceFirst("\\.\\d{3,9}", "").replace("Z", "").trim();
    }

    private void appendIfPresent(StringBuilder sb, String label, String value) {
        if (value != null && !value.isEmpty()) {
            sb.append(label).append(": ").append(value).append("\n");
        }
    }

    private void addInstitutionBanner(Document document, Font nameFont, Font smallFont) throws Exception {
        PdfPTable banner = new PdfPTable(2);
        banner.setWidthPercentage(100);
        banner.setWidths(new float[] { 2.4f, 2.6f });
        banner.setSpacingAfter(10);

        PdfPCell left = new PdfPCell();
        left.setBorder(Rectangle.NO_BORDER);
        left.setVerticalAlignment(Element.ALIGN_MIDDLE);
        boolean logoAdded = false;
        try (InputStream in = PathologyDiagnosticReportServiceImpl.class.getResourceAsStream("/images/ahri-pathology-header.png")) {
            if (in != null) {
                byte[] bytes = in.readAllBytes();
                if (bytes.length > 0) {
                    Image img = Image.getInstance(bytes);
                    img.scaleToFit(200, 90);
                    img.setAlignment(Element.ALIGN_LEFT);
                    left.addElement(img);
                    logoAdded = true;
                }
            }
        } catch (Exception e) {
            LogEvent.logWarn(LOG_CLASS, "addInstitutionBanner", "Could not load AHRI logo: " + e.getMessage());
        }
        if (!logoAdded) {
            left.addElement(new Paragraph("AHRI", nameFont));
            left.addElement(new Paragraph("Armauer Hansen Research Institute", smallFont));
        }
        banner.addCell(left);

        PdfPCell right = new PdfPCell();
        right.setBorder(Rectangle.NO_BORDER);
        Paragraph addr = new Paragraph();
        addr.setAlignment(Element.ALIGN_RIGHT);
        Font addrMuted = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL, HEADER_GRAY);
        addr.add(new Chunk("Armauer Hansen Research Institute\n", nameFont));
        addr.add(new Chunk("Jimma Street, Addis Ababa, Ethiopia\n", addrMuted));
        addr.add(new Chunk("Pathology diagnostic laboratory services\n", addrMuted));
        right.addElement(addr);
        banner.addCell(right);

        document.add(banner);
    }

    private void addColumnHeader(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(LIGHT_GRAY);
        cell.setPadding(5);
        cell.setBorderColor(LIGHT_GRAY);
        table.addCell(cell);
    }

    private void addInfoCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(6);
        cell.setBorderColor(LIGHT_GRAY);
        cell.setBorderWidth(0.5f);
        table.addCell(cell);
    }
}
