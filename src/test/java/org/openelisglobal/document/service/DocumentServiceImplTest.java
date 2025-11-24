package org.openelisglobal.document.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.document.dao.DocumentVersionDAO;
import org.openelisglobal.document.dao.IDDocumentDAO;
import org.openelisglobal.document.storage.DocumentStorageService;
import org.openelisglobal.document.validation.DocumentValidationService;
import org.openelisglobal.document.valueholder.DocumentVersion;
import org.openelisglobal.document.valueholder.IDDocument;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
 

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class DocumentServiceImplTest {

    @Mock
    private IDDocumentDAO idDocumentDAO;

    @Mock
    private DocumentVersionDAO documentVersionDAO;

    @Mock
    private DocumentStorageService storageService;

    @Mock
    private DocumentValidationService validationService;

    @InjectMocks
    private DocumentServiceImpl documentService;

    @Test
    public void testUpload_Success() throws Exception {
        byte[] bytes = "hello".getBytes();
        InputStream in = new ByteArrayInputStream(bytes);

        when(storageService.store(any(InputStream.class), anyString())).thenReturn("/store/path");
        when(documentVersionDAO.saveVersion(any(DocumentVersion.class))).thenAnswer(i -> i.getArguments()[0]);
        when(idDocumentDAO.save(any(IDDocument.class))).thenAnswer(i -> i.getArguments()[0]);

        // no validation exception
        doNothing().when(validationService).validate(any(InputStream.class), anyString(), anyLong());

        var resp = documentService.upload("PAT-1", "National ID", "desc", in, "id.jpg", "image/jpeg", bytes.length, "unittest");

        assertNotNull(resp);
        assertNotNull(resp.getDocumentId());
        assertNotNull(resp.getVersionId());

        verify(storageService, times(1)).store(any(InputStream.class), anyString());
        verify(documentVersionDAO, times(1)).saveVersion(any(DocumentVersion.class));
        verify(idDocumentDAO, atLeastOnce()).save(any(IDDocument.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUpload_SizeExceeded_Throws() throws Exception {
        byte[] bytes = new byte[11 * 1024 * 1024]; // 11MB
        InputStream in = new ByteArrayInputStream(bytes);

        doThrow(new IllegalArgumentException("File exceeds maximum allowed size of 10MB")).when(validationService).validate(any(InputStream.class), anyString(), anyLong());

        documentService.upload("PAT-1", "Insurance", "desc", in, "big.pdf", "application/pdf", bytes.length, "unittest");
    }

    @Test(expected = RuntimeException.class)
    public void testUpload_DAOFailure_Throws() throws Exception {
        byte[] bytes = "hello".getBytes();
        InputStream in = new ByteArrayInputStream(bytes);

        doNothing().when(validationService).validate(any(InputStream.class), anyString(), anyLong());
        when(storageService.store(any(InputStream.class), anyString())).thenReturn("/store/path");
        when(idDocumentDAO.save(any())).thenThrow(new RuntimeException("DB error"));

        documentService.upload("PAT-1", "National ID", "desc", in, "id.jpg", "image/jpeg", bytes.length, "unittest");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDownload_NoVersions_Throws() throws Exception {
        when(documentVersionDAO.getLatestVersion(anyString())).thenReturn(null);
        documentService.downloadLatest("doc-123");
    }
}
