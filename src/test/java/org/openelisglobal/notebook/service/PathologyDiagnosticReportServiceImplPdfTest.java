package org.openelisglobal.notebook.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

/**
 * Exercises {@link PathologyDiagnosticReportServiceImpl#buildPdf} with synthetic merged sample maps
 * (same shape as {@code generateDiagnosticReportPdf} produces) so PDF output is asserted without a DB.
 */
public class PathologyDiagnosticReportServiceImplPdfTest {

    @Test
    public void buildPdf_omitsLegacyFooter_andFormatsDates_andShowsPathologistAttestation() throws Exception {
        PathologyDiagnosticReportServiceImpl svc = new PathologyDiagnosticReportServiceImpl();

        Map<String, Object> sd = new LinkedHashMap<>();
        sd.put("firstName", "Jane");
        sd.put("surname", "Doe");
        sd.put("nationalId", "MRN-001");
        sd.put("phone", "555-0100");
        sd.put("dateOfBirth", "1990-03-15");
        sd.put("sex", "F");
        sd.put("accessionNumber", "ACC-100");
        sd.put("externalId", "EXT-200");
        sd.put("collectionDateTime", "2026-05-13T21:00:00.000Z");
        sd.put("receivedDateTime", "2026-05-14T06:30:00.000Z");
        sd.put("requestingClinician", "Dr. Referrer");
        sd.put("clinicalDetails", "Routine biopsy");
        sd.put("specimenType", "Tissue");
        sd.put("specimenSite", "Skin");
        sd.put("diag_verificationDate", "2026-05-14T08:00:00.000Z");
        sd.put("diag_finalDiagnosis", "Benign process.");
        sd.put("diag_reportFinalized", Boolean.TRUE);
        sd.put("diag_verifyingPathologistName", "Dr. Verifier");
        sd.put("diag_pathologistCredentials", "MD, FCAP");
        sd.put("diag_pathologistSignature", "Dr. Verifier");
        sd.put("diag_diagnosingPathologist", "Dr. Primary");

        Method buildPdf = PathologyDiagnosticReportServiceImpl.class.getDeclaredMethod("buildPdf", List.class);
        buildPdf.setAccessible(true);
        byte[] pdf = (byte[]) buildPdf.invoke(svc, List.of(sd));
        assertNotNull(pdf);
        assertTrue(pdf.length > 2000);

        String prefix = new String(pdf, 0, 5, StandardCharsets.ISO_8859_1);
        assertTrue(prefix.startsWith("%PDF-"));

        StringBuilder extracted = new StringBuilder();
        PdfReader reader = new PdfReader(pdf);
        try {
            for (int p = 1; p <= reader.getNumberOfPages(); p++) {
                extracted.append(PdfTextExtractor.getTextFromPage(reader, p));
            }
        } finally {
            reader.close();
        }
        String text = extracted.toString();
        assertFalse(
                "footer line must not appear",
                text.contains("This is a computer-generated document."));
        assertFalse("raw ISO instant must not appear in rendered text", text.contains("2026-05-13T21:00:00.000Z"));
        assertTrue("procedure date label", text.contains("Procedure Date"));
        assertTrue("formatted date contains calendar day", text.contains("2026-05-13"));
        assertTrue("formatted time fragment", text.contains("21:00"));
        assertTrue("verifying pathologist", text.contains("Dr. Verifier"));
        assertTrue("credentials", text.contains("MD, FCAP"));

        Files.write(Path.of("target/pathology-diagnostic-sanity.pdf"), pdf);
    }
}
