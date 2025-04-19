package org.openelisglobal.reports;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.reports.service.DocumentTypeService;
import org.openelisglobal.reports.valueholder.DocumentType;
import org.springframework.beans.factory.annotation.Autowired;

public class DocumentTypeServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private DocumentTypeService documentTypeService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/document-type.xml");
    }

    @Test
    public void getAll_ShouldReturnAllDocumentTypes() {
        List<DocumentType> documentTypes = documentTypeService.getAll();
        assertNotNull(documentTypes);
        assertFalse(documentTypes.isEmpty());
        assertEquals(3, documentTypes.size());
    }

    @Test
    public void getDocumentTypeByName_ShouldReturnNullWhenNoMatch() {
        DocumentType documentType = documentTypeService.getDocumentTypeByName("Non-Existent");
        assertNull(documentType);
    }

    @Test
    public void get_ShouldReturnDocumentTypeById() {
        DocumentType documentType = documentTypeService.get("2");
        assertNotNull(documentType);
        assertEquals("2", documentType.getId());
        assertEquals("Report Type 2", documentType.getName());
    }

    @Test
    public void save_ShouldCreateNewDocumentType() {
        DocumentType newDocumentType = new DocumentType();
        newDocumentType.setName("New Report Type");

        DocumentType savedDocumentType = documentTypeService.save(newDocumentType);
        assertNotNull(savedDocumentType);
        assertNotNull(savedDocumentType.getId());
        assertEquals("New Report Type", savedDocumentType.getName());

        DocumentType retrievedDocumentType = documentTypeService.getDocumentTypeByName("New Report Type");
        assertNotNull(retrievedDocumentType);
        assertEquals(savedDocumentType.getId(), retrievedDocumentType.getId());
    }

    @Test
    public void update_ShouldModifyExistingDocumentType() {
        DocumentType documentType = documentTypeService.getDocumentTypeByName("Report Type 1");
        assertNotNull(documentType);

        documentType.setName("Updated Report Type");
        DocumentType updatedDocumentType = documentTypeService.update(documentType);

        assertNotNull(updatedDocumentType);
        assertEquals("Updated Report Type", updatedDocumentType.getName());

        DocumentType oldNameType = documentTypeService.getDocumentTypeByName("Report Type 1");
        assertNull(oldNameType);

        DocumentType newNameType = documentTypeService.getDocumentTypeByName("Updated Report Type");
        assertNotNull(newNameType);
    }

    @Test
    public void delete_ShouldRemoveDocument() {
        DocumentType documentType = documentTypeService.getDocumentTypeByName("Report Type 3");
        assertNotNull(documentType);

        documentTypeService.delete(documentType);

        DocumentType deleteType = documentTypeService.getDocumentTypeByName("Report Type 3");
        assertNull(deleteType);
    }
}