package org.openelisglobal.reports;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.reports.service.DocumentTrackService;
import org.openelisglobal.reports.valueholder.DocumentTrack;
import org.springframework.beans.factory.annotation.Autowired;

public class DocumentTrackServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private DocumentTrackService documentTrackService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/document-track.xml");
    }

    @Test
    public void getAll_ShouldReturnAllDocumentTracks() {
        List<DocumentTrack> documents = documentTrackService.getAll();
        assertNotNull(documents);
        assertFalse(documents.isEmpty());
        assertEquals(3, documents.size());
    }

    @Test
    public void getByTypeRecordAndTable_ShouldReturnMatchingDocuments() {
        List<DocumentTrack> typeOneDocuments = documentTrackService.getByTypeRecordAndTable("1", "1", "1");
        assertNotNull(typeOneDocuments);
        assertFalse(typeOneDocuments.isEmpty());
        assertEquals(1, typeOneDocuments.size());
        assertEquals("Test Doc 1", typeOneDocuments.get(0).getDocumentName());

        List<DocumentTrack> typeTwoDocuments = documentTrackService.getByTypeRecordAndTable("2", "1", "1");
        assertNotNull(typeTwoDocuments);
        assertFalse(typeTwoDocuments.isEmpty());
        assertEquals(1, typeTwoDocuments.size());
        assertEquals("Test Doc 3", typeTwoDocuments.get(0).getDocumentName());
    }

    @Test
    public void getByTypeRecordAndTableAndName_ShoulrReturnMatchingDocument() {
        List<DocumentTrack> documents = documentTrackService.getByTypeRecordAndTableAndName("1", "1", "1",
                "Test Doc 1");

        assertNotNull(documents);
        assertFalse(documents.isEmpty());
        assertEquals(1, documents.size());

        DocumentTrack document = documents.get(0);
        assertEquals("Test Doc 1", document.getDocumentName());
        assertEquals("1", document.getId());
        assertEquals("1", document.getTableId());
        assertEquals("1", document.getRecordId());
        assertEquals("1", document.getDocumentTypeId());
    }

    @Test
    public void getByTypeRecordAndTableAndName_ShouldReturnEmptyList() {
        List<DocumentTrack> documents = documentTrackService.getByTypeRecordAndTableAndName("1", "1", "1",
                "Non Existent Document");

        assertNotNull(documents);
        assertTrue(documents.isEmpty());
    }

    @Test
    public void update_ShouldModifyExistingDocument() {
        List<DocumentTrack> documents = documentTrackService.getByTypeRecordAndTableAndName("1", "1", "1",
                "Test Doc 1");
        assertFalse(documents.isEmpty());

        DocumentTrack document = documents.get(0);
        String originalName = document.getDocumentName();

        document.setDocumentName("Updated Test Doc");
        DocumentTrack updatedDocument = documentTrackService.update(document);

        assertNotNull(updatedDocument);
        assertEquals("Updated Test Doc", updatedDocument.getDocumentName());

        documents = documentTrackService.getByTypeRecordAndTableAndName("1", "1", "1", originalName);
        assertTrue(documents.isEmpty());

        documents = documentTrackService.getByTypeRecordAndTableAndName("1", "1", "1", "Updated Test Doc");
        assertFalse(documents.isEmpty());
    }

    @Test
    public void delete_ShouldRemoveDocument() {
        List<DocumentTrack> documents = documentTrackService.getByTypeRecordAndTableAndName("1", "1", "1",
                "Test Doc 1");
        assertFalse(documents.isEmpty());

        DocumentTrack document = documents.get(0);
        documentTrackService.delete(document);

        documents = documentTrackService.getByTypeRecordAndTableAndName("1", "1", "1", "Test Doc 1");
        assertTrue(documents.isEmpty());
    }
}
