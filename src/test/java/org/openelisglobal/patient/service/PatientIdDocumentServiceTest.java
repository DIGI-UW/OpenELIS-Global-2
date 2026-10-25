package org.openelisglobal.patient.service;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import javax.imageio.ImageIO;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.patient.valueholder.PatientIdDocument;
import org.springframework.beans.factory.annotation.Autowired;

public class PatientIdDocumentServiceTest extends BaseWebContextSensitiveTest {

    private static final String TINY_PNG_BASE64 = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==";
    private static final String TINY_PDF_BASE64 = "data:application/pdf;base64,JVBERi0xLjQKJGNvbnRlbnQ=";

    @Autowired
    private PatientIdDocumentService patientIdDocumentService;

    private final List<Integer> createdDocumentIds = new ArrayList<>();

    @Before
    public void init() throws Exception {
        executeDataSetWithStateManagement("testdata/patient.xml");
        ensureReferenceTables("PATIENT_ID_DOCUMENT");
    }

    @After
    public void tearDown() {
        for (Integer id : createdDocumentIds) {
            try {
                patientIdDocumentService.delete(id, "1");
            } catch (Exception ignored) {
            }
        }
        createdDocumentIds.clear();
    }

    private PatientIdDocument saveDocument(String patientId, String documentBase64, String documentCategory,
            String description, String sysUserId) {
        PatientIdDocument doc = patientIdDocumentService.saveDocument(patientId, documentBase64, documentCategory,
                description, sysUserId);
        if (doc != null && doc.getId() != null) {
            createdDocumentIds.add(doc.getId());
        }
        return doc;
    }

    private static String createNonSquareImageBase64(int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.BLUE);
        g.fillRect(0, 0, width, height);
        g.dispose();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    @Test
    public void saveDocument_shouldSaveImageDocumentWithExtractedTypeAndThumbnail() throws Exception {
        String nonSquareBase64 = createNonSquareImageBase64(300, 200);
        PatientIdDocument doc = saveDocument("101", nonSquareBase64, "NATIONAL_ID", "National ID Card", "1");

        Assert.assertNotNull("Saved document must not be null", doc);
        Assert.assertNotNull("Saved document must have an ID", doc.getId());

        Optional<PatientIdDocument> fetched = patientIdDocumentService.getMatch("id", doc.getId());
        Assert.assertTrue("Stored document must be present in database", fetched.isPresent());
        PatientIdDocument stored = fetched.get();

        Assert.assertEquals("101", stored.getPatientId());
        Assert.assertEquals("image/png", stored.getDocumentType());
        Assert.assertEquals("NATIONAL_ID", stored.getDocumentCategory());
        Assert.assertEquals("National ID Card", stored.getDescription());
        Assert.assertFalse("Newly saved document should not be soft-deleted", stored.isDeleted());

        Assert.assertNotNull("Thumbnail data should not be null", stored.getThumbnailData());
        byte[] thumbBytes = Base64.getDecoder().decode(stored.getThumbnailData());
        BufferedImage thumbImage = ImageIO.read(new ByteArrayInputStream(thumbBytes));
        Assert.assertNotNull("Thumbnail should be a valid decodable image", thumbImage);

        int thumbWidth = thumbImage.getWidth();
        int thumbHeight = thumbImage.getHeight();
        int maxDim = Math.max(thumbWidth, thumbHeight);
        Assert.assertEquals("Thumbnail max dimension should be 150px", 150, maxDim);
        Assert.assertTrue("Thumbnail width must not exceed 150px", thumbWidth <= 150);
        Assert.assertTrue("Thumbnail height must not exceed 150px", thumbHeight <= 150);

        double originalAspect = 300.0 / 200.0;
        double thumbAspect = (double) thumbWidth / (double) thumbHeight;
        Assert.assertEquals("Thumbnail aspect ratio should be preserved", originalAspect, thumbAspect, 0.02);
    }

    @Test
    public void saveDocument_shouldReturnNullWhenBase64DataIsNull() {
        PatientIdDocument docNull = saveDocument("101", null, "PASSPORT", "Passport", "1");
        Assert.assertEquals("Null base64 data should produce null document", null, docNull);
    }

    @Test
    public void saveDocument_shouldReturnNullWhenBase64DataIsEmpty() {
        PatientIdDocument docEmpty = saveDocument("101", "", "PASSPORT", "Passport", "1");
        Assert.assertEquals("Empty base64 data should produce null document", null, docEmpty);
    }

    @Test
    public void saveDocument_shouldDefaultCategoryToOtherWhenCategoryIsNull() {
        PatientIdDocument doc = saveDocument("101", TINY_PNG_BASE64, null, "Driver License", "1");

        Optional<PatientIdDocument> fetched = patientIdDocumentService.getMatch("id", doc.getId());
        Assert.assertTrue(fetched.isPresent());
        PatientIdDocument stored = fetched.get();

        Assert.assertEquals("OTHER", stored.getDocumentCategory());
        Assert.assertEquals("Driver License", stored.getDescription());
    }

    @Test
    public void saveDocument_shouldSavePdfDocumentWithPdfType() {
        PatientIdDocument doc = saveDocument("101", TINY_PDF_BASE64, "MEDICAL_RECORD", "Health Summary", "1");

        Optional<PatientIdDocument> fetched = patientIdDocumentService.getMatch("id", doc.getId());
        Assert.assertTrue("Stored PDF document must be present in database", fetched.isPresent());
        PatientIdDocument stored = fetched.get();

        Assert.assertEquals("101", stored.getPatientId());
        Assert.assertEquals("application/pdf", stored.getDocumentType());
        Assert.assertEquals("MEDICAL_RECORD", stored.getDocumentCategory());
        Assert.assertEquals("Health Summary", stored.getDescription());
        Assert.assertEquals("JVBERi0xLjQKJGNvbnRlbnQ=", stored.getDocumentData());
        Assert.assertEquals("PDF document thumbnail should fall back to clean original data",
                "JVBERi0xLjQKJGNvbnRlbnQ=", stored.getThumbnailData());
    }

    @Test
    public void saveDocument_shouldFallbackToCleanOriginalDataWhenImageIsUnreadable() {
        String corruptPayload = Base64.getEncoder().encodeToString("corrupted-not-an-image-content".getBytes());
        String corruptImageBase64 = "data:image/png;base64," + corruptPayload;

        PatientIdDocument doc = saveDocument("101", corruptImageBase64, "OTHER", "Unreadable Image Document", "1");
        Assert.assertNotNull(doc);

        Optional<PatientIdDocument> fetched = patientIdDocumentService.getMatch("id", doc.getId());
        Assert.assertTrue("Stored document must be present in database", fetched.isPresent());
        PatientIdDocument stored = fetched.get();

        Assert.assertEquals("image/png", stored.getDocumentType());
        Assert.assertEquals(corruptPayload, stored.getDocumentData());
        Assert.assertEquals("Unreadable image should fall back to clean original data when thumbnail creation fails",
                corruptPayload, stored.getThumbnailData());
    }

    @Test
    public void getDocumentsByPatientId_shouldReturnExactMatchingDocumentsForPatient() {
        saveDocument("201", TINY_PNG_BASE64, "NATIONAL_ID", "Doc 1", "1");
        saveDocument("201", TINY_PNG_BASE64, "PASSPORT", "Doc 2", "1");
        saveDocument("202", TINY_PNG_BASE64, "OTHER", "Doc Other", "1");

        List<PatientIdDocument> patient201Docs = patientIdDocumentService.getDocumentsByPatientId("201");
        Assert.assertEquals(2, patient201Docs.size());
        Assert.assertTrue("All returned docs should belong to patient 201",
                patient201Docs.stream().allMatch(d -> "201".equals(d.getPatientId())));

        List<PatientIdDocument> patient202Docs = patientIdDocumentService.getDocumentsByPatientId("202");
        Assert.assertEquals(1, patient202Docs.size());
        Assert.assertEquals("202", patient202Docs.get(0).getPatientId());
    }

    @Test
    public void getDocumentsByPatientId_shouldReturnEmptyListWhenPatientIdIsNull() {
        List<PatientIdDocument> docs = patientIdDocumentService.getDocumentsByPatientId(null);
        Assert.assertEquals(0, docs.size());
    }

    @Test
    public void softDeleteDocument_shouldMarkDocumentAsDeletedAndExcludeFromActiveList() {
        PatientIdDocument doc = saveDocument("301", TINY_PNG_BASE64, "VOTER_CARD", "Voter ID", "1");
        Integer docId = doc.getId();

        patientIdDocumentService.softDeleteDocument(docId, "2");

        Optional<PatientIdDocument> fetched = patientIdDocumentService.getMatch("id", docId);
        Assert.assertTrue("Soft-deleted document should still exist in the database", fetched.isPresent());
        Assert.assertTrue("Document should be marked as deleted", fetched.get().isDeleted());

        List<PatientIdDocument> activeDocs = patientIdDocumentService.getDocumentsByPatientId("301");
        Assert.assertEquals("Active docs list should exclude soft-deleted documents", 0, activeDocs.size());
    }

    @Test
    public void updateDocumentCategory_shouldUpdateCategoryAndDescription() {
        PatientIdDocument doc = saveDocument("401", TINY_PNG_BASE64, "OTHER", "Initial Description", "1");
        Integer docId = doc.getId();

        PatientIdDocument updated = patientIdDocumentService.updateDocumentCategory(docId, "INSURANCE",
                "Updated Description", "3");

        Assert.assertNotNull(updated);
        Assert.assertEquals(docId, updated.getId());

        Optional<PatientIdDocument> fetched = patientIdDocumentService.getMatch("id", docId);
        Assert.assertTrue("Document should be found after category update", fetched.isPresent());
        PatientIdDocument persisted = fetched.get();

        Assert.assertEquals(docId, persisted.getId());
        Assert.assertEquals("INSURANCE", persisted.getDocumentCategory());
        Assert.assertEquals("Updated Description", persisted.getDescription());
    }

    @Test
    public void updateDocument_shouldUpdateDataAndMetadata() {
        PatientIdDocument doc = saveDocument("501", TINY_PNG_BASE64, "OTHER", "Old Info", "1");
        Integer docId = doc.getId();

        PatientIdDocument updated = patientIdDocumentService.updateDocument(docId, TINY_PDF_BASE64, "WORK_PERMIT",
                "New Work Permit", "4");

        Assert.assertNotNull(updated);
        Assert.assertEquals(docId, updated.getId());

        Optional<PatientIdDocument> fetched = patientIdDocumentService.getMatch("id", docId);
        Assert.assertTrue("Document should be found after update", fetched.isPresent());
        PatientIdDocument persisted = fetched.get();

        Assert.assertEquals(docId, persisted.getId());
        Assert.assertEquals("application/pdf", persisted.getDocumentType());
        Assert.assertEquals("WORK_PERMIT", persisted.getDocumentCategory());
        Assert.assertEquals("New Work Permit", persisted.getDescription());
        Assert.assertEquals("JVBERi0xLjQKJGNvbnRlbnQ=", persisted.getDocumentData());
        Assert.assertEquals("JVBERi0xLjQKJGNvbnRlbnQ=", persisted.getThumbnailData());
    }

    @Test
    public void updateDocument_shouldReturnNullWhenDocumentIdDoesNotExist() {
        PatientIdDocument result = patientIdDocumentService.updateDocument(999999, TINY_PNG_BASE64, "PASSPORT",
                "Non-existent", "1");
        Assert.assertEquals("Non-existent ID should return null", null, result);
    }
}
