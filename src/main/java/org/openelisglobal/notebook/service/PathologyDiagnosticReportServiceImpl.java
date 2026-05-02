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
import com.itextpdf.text.pdf.draw.LineSeparator;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.notebook.valueholder.NoteBookPage;
import org.openelisglobal.notebook.valueholder.NotebookEntry;
import org.openelisglobal.notebook.valueholder.NotebookPageSample;
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

    @Autowired
    private NotebookEntryService notebookEntryService;

    @Autowired
    private NoteBookPageService noteBookPageService;

    @Autowired
    private NotebookPageSampleService notebookPageSampleService;

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
        patientCol.append("DOB: ").append(getString(primaryData, "dateOfBirth")).append("\n");
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
            specimenCol.append("Procedure Date: ").append(getString(sd, "collectionDateTime")).append("\n");
            specimenCol.append("Received Date: ").append(getString(sd, "receivedDateTime")).append("\n");
            specimenCol.append("Reported Date: ").append(getString(sd, "diag_verificationDate"));
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

        // 7. Pathologist signature block
        document.add(Chunk.NEWLINE);
        Map<String, Object> lastSampleData = sampleDataList.get(sampleDataList.size() - 1);
        String pathologistName = getString(lastSampleData, "diag_diagnosingPathologist");
        String credentials = getString(lastSampleData, "diag_pathologistCredentials");
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
        sigBlock.add(new Chunk("________________", signValueFont));
        document.add(sigBlock);

        // 8. Footer
        document.add(Chunk.NEWLINE);
        LineSeparator line = new LineSeparator(0.75f, 100, BaseColor.BLACK, Element.ALIGN_CENTER, -2);
        document.add(line);

        Paragraph footer = new Paragraph();
        footer.setSpacingBefore(4);
        footer.add(new Chunk("Generated: " + LocalDateTime.now().toString(), smallFont));
        footer.add(Chunk.NEWLINE);
        footer.add(new Chunk("This is a computer-generated document.", smallFont));
        document.add(footer);

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
        String finalDiagnosis = getString(data, "diag_finalDiagnosis");
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
        String description = getString(data, "diag_microscopicDescription");
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
                        String diag = getString(ps.getData(), "finalDiagnosis");
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
     * Find the best diagnosis data for a sample on Page 8. Handles composite IDs
     * (e.g., "4_cassette_0_block_0_slide_0"). Prefers the record with
     * reportFinalized=true or the most complete data.
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

        // Prefer finalized report
        for (NotebookPageSample ps : matching) {
            Object finalized = ps.getData().get("reportFinalized");
            if (Boolean.TRUE.equals(finalized)) {
                return ps.getData();
            }
        }

        // Prefer one with finalDiagnosis
        for (NotebookPageSample ps : matching) {
            if (!getString(ps.getData(), "finalDiagnosis").isEmpty()) {
                return ps.getData();
            }
        }

        // Return first with any data
        return matching.get(0).getData();
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
                    img.scaleToFit(240, 68);
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
