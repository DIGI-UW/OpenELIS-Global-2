package org.openelisglobal.patient.service;

import java.util.List;
import java.util.Optional;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.patient.valueholder.PatientIdDocument;
import org.springframework.beans.factory.annotation.Autowired;

public class PatientIdDocumentServiceTest extends BaseWebContextSensitiveTest {

    private static final String TINY_PNG_BASE64 = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==";
    private static final String TINY_PDF_BASE64 = "data:application/pdf;base64,JVBERi0xLjQKJGNvbnRlbnQ=";
    private static final String CLEAN_PNG_BASE64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==";

    @Autowired
    private PatientIdDocumentService patientIdDocumentService;

    @Before
    public void init() throws Exception {
        executeDataSetWithStateManagement("testdata/patient.xml");
        ensureReferenceTables("PATIENT_ID_DOCUMENT");
    }

    @Test
    public void saveDocument_shouldSaveImageDocumentWithExtractedTypeAndThumbnail() {
        PatientIdDocument doc = patientIdDocumentService.saveDocument("101", TINY_PNG_BASE64, "NATIONAL_ID",
                "National ID Card", "1");

        Assert.assertEquals("101", doc.getPatientId());
        Assert.assertEquals("image/png", doc.getDocumentType());
        Assert.assertEquals("NATIONAL_ID", doc.getDocumentCategory());
        Assert.assertEquals("National ID Card", doc.getDescription());
        Assert.assertEquals(CLEAN_PNG_BASE64, doc.getDocumentData());
        Assert.assertFalse("Newly saved document should not be soft-deleted", doc.isDeleted());
        Assert.assertEquals("1", doc.getSysUserId());
    }

    @Test
    public void saveDocument_shouldReturnNullWhenBase64DataIsNull() {
        PatientIdDocument docNull = patientIdDocumentService.saveDocument("101", null, "PASSPORT", "Passport", "1");
        Assert.assertEquals("Null base64 data should produce null document", null, docNull);
    }

    @Test
    public void saveDocument_shouldReturnNullWhenBase64DataIsEmpty() {
        PatientIdDocument docEmpty = patientIdDocumentService.saveDocument("101", "", "PASSPORT", "Passport", "1");
        Assert.assertEquals("Empty base64 data should produce null document", null, docEmpty);
    }

    @Test
    public void saveDocument_shouldDefaultCategoryToOtherWhenCategoryIsNull() {
        PatientIdDocument doc = patientIdDocumentService.saveDocument("101", TINY_PNG_BASE64, null, "Driver License",
                "1");

        Assert.assertEquals("OTHER", doc.getDocumentCategory());
        Assert.assertEquals("Driver License", doc.getDescription());
    }

    @Test
    public void saveDocument_shouldSavePdfDocumentWithPdfType() {
        PatientIdDocument doc = patientIdDocumentService.saveDocument("101", TINY_PDF_BASE64, "MEDICAL_RECORD",
                "Health Summary", "1");

        Assert.assertEquals("application/pdf", doc.getDocumentType());
        Assert.assertEquals("MEDICAL_RECORD", doc.getDocumentCategory());
        Assert.assertEquals("JVBERi0xLjQKJGNvbnRlbnQ=", doc.getDocumentData());
    }

    @Test
    public void getDocumentsByPatientId_shouldReturnExactMatchingDocumentsForPatient() {
        patientIdDocumentService.saveDocument("201", TINY_PNG_BASE64, "NATIONAL_ID", "Doc 1", "1");
        patientIdDocumentService.saveDocument("201", TINY_PNG_BASE64, "PASSPORT", "Doc 2", "1");
        patientIdDocumentService.saveDocument("202", TINY_PNG_BASE64, "OTHER", "Doc Other", "1");

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
        PatientIdDocument doc = patientIdDocumentService.saveDocument("301", TINY_PNG_BASE64, "VOTER_CARD", "Voter ID",
                "1");
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
        PatientIdDocument doc = patientIdDocumentService.saveDocument("401", TINY_PNG_BASE64, "OTHER",
                "Initial Description", "1");
        Integer docId = doc.getId();

        PatientIdDocument updated = patientIdDocumentService.updateDocumentCategory(docId, "INSURANCE",
                "Updated Description", "3");

        Assert.assertEquals(docId, updated.getId());
        Assert.assertEquals("INSURANCE", updated.getDocumentCategory());
        Assert.assertEquals("Updated Description", updated.getDescription());
        Assert.assertEquals("3", updated.getSysUserId());
    }

    @Test
    public void updateDocument_shouldUpdateDataAndMetadata() {
        PatientIdDocument doc = patientIdDocumentService.saveDocument("501", TINY_PNG_BASE64, "OTHER", "Old Info", "1");
        Integer docId = doc.getId();

        PatientIdDocument updated = patientIdDocumentService.updateDocument(docId, TINY_PDF_BASE64, "WORK_PERMIT",
                "New Work Permit", "4");

        Assert.assertEquals("application/pdf", updated.getDocumentType());
        Assert.assertEquals("WORK_PERMIT", updated.getDocumentCategory());
        Assert.assertEquals("New Work Permit", updated.getDescription());
        Assert.assertEquals("JVBERi0xLjQKJGNvbnRlbnQ=", updated.getDocumentData());
        Assert.assertEquals("4", updated.getSysUserId());
    }

    @Test
    public void updateDocument_shouldReturnNullWhenDocumentIdDoesNotExist() {
        PatientIdDocument result = patientIdDocumentService.updateDocument(999999, TINY_PNG_BASE64, "PASSPORT",
                "Non-existent", "1");
        Assert.assertEquals("Non-existent ID should return null", null, result);
    }
}
