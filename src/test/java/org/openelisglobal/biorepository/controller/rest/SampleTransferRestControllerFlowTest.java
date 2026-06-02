package org.openelisglobal.biorepository.controller.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.biorepository.service.SampleTransferService;
import org.openelisglobal.biorepository.valueholder.BioSample;
import org.openelisglobal.biorepository.valueholder.SampleTransferItem;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.notebook.service.NoteBookPageService;
import org.openelisglobal.notebook.service.NotebookPageSampleService;
import org.openelisglobal.notebook.valueholder.NoteBookPage;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

@RunWith(MockitoJUnitRunner.Silent.class)
public class SampleTransferRestControllerFlowTest {

    @Mock
    private SampleTransferService transferService;
    @Mock
    private NoteBookPageService noteBookPageService;
    @Mock
    private NotebookPageSampleService notebookPageSampleService;

    @InjectMocks
    private SampleTransferRestController controller;

    private HttpServletRequest requestWithUser;

    @Before
    public void setUp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        UserSessionData usd = new UserSessionData();
        usd.setSytemUserId(7);
        request.getSession().setAttribute("userSessionData", usd);
        requestWithUser = request;
    }

    @Test
    public void acceptItem_NonNumericSampleItemId_DoesNotFailAndLinksToStoragePage() {
        SampleTransferItem item = new SampleTransferItem();
        item.setId(1001);
        item.setStatus(SampleTransferItem.ItemStatus.ACCEPTED);

        SampleItem sampleItem = new SampleItem();
        sampleItem.setId("ABC-100");
        item.setSampleItem(sampleItem);

        BioSample bioSample = new BioSample();
        bioSample.setId(501);
        item.setBioSample(bioSample);

        NoteBookPage storagePage = new NoteBookPage();
        storagePage.setId(22);
        storagePage.setOrder(2);

        when(transferService.acceptItem(eq(1001), any(BioSample.class), eq("7"))).thenReturn(item);
        when(noteBookPageService.getByNotebookId(117)).thenReturn(List.of(storagePage));
        when(notebookPageSampleService.getBySampleItemIdAndPageId("ABC-100", 22)).thenReturn(null);

        SampleTransferRestController.BioSampleMetadata metadata = new SampleTransferRestController.BioSampleMetadata();
        metadata.setNotebookId(117);
        metadata.setBiosafetyLevel("BSL_1");

        ResponseEntity<?> response = controller.acceptItem(1001, metadata, requestWithUser);
        assertEquals(200, response.getStatusCode().value());

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals("ABC-100", body.get("sampleItemId"));
        assertEquals(Boolean.TRUE, body.get("storagePageLinked"));
        assertFalse(body.containsKey("error"));

        verify(notebookPageSampleService).createPageSampleForPageString(22, "ABC-100",
                org.openelisglobal.notebook.valueholder.NotebookPageSample.Status.PENDING);
    }

    @Test
    public void acceptItem_WhenPageOrderDiffers_FallsBackToStorageAssignPageId() {
        SampleTransferItem item = new SampleTransferItem();
        item.setId(1002);
        item.setStatus(SampleTransferItem.ItemStatus.ACCEPTED);

        SampleItem sampleItem = new SampleItem();
        sampleItem.setId("ABC-200");
        item.setSampleItem(sampleItem);

        BioSample bioSample = new BioSample();
        bioSample.setId(502);
        item.setBioSample(bioSample);

        NoteBookPage nonStoragePage = new NoteBookPage();
        nonStoragePage.setId(30);
        nonStoragePage.setOrder(2);
        nonStoragePage.setPageId("shipment_reception");
        nonStoragePage.setTitle("Shipment Reception");

        NoteBookPage storagePage = new NoteBookPage();
        storagePage.setId(31);
        storagePage.setOrder(7);
        storagePage.setPageId("storage_assign");
        storagePage.setTitle("Storage Assignment");

        when(transferService.acceptItem(eq(1002), any(BioSample.class), eq("7"))).thenReturn(item);
        when(noteBookPageService.getByNotebookId(118)).thenReturn(List.of(nonStoragePage, storagePage));
        when(notebookPageSampleService.getBySampleItemIdAndPageId("ABC-200", 31)).thenReturn(null);

        SampleTransferRestController.BioSampleMetadata metadata = new SampleTransferRestController.BioSampleMetadata();
        metadata.setNotebookId(118);
        metadata.setBiosafetyLevel("BSL_1");

        ResponseEntity<?> response = controller.acceptItem(1002, metadata, requestWithUser);
        assertEquals(200, response.getStatusCode().value());

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals(Boolean.TRUE, body.get("storagePageLinked"));
        assertEquals(31, body.get("storagePageId"));

        verify(notebookPageSampleService).createPageSampleForPageString(31, "ABC-200",
                org.openelisglobal.notebook.valueholder.NotebookPageSample.Status.PENDING);
    }
}
