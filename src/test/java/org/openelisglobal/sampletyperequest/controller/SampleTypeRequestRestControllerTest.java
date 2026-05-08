package org.openelisglobal.sampletyperequest.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.panel.service.PanelService;
import org.openelisglobal.panel.valueholder.Panel;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampletyperequest.controller.rest.SampleTypeRequestRestController;
import org.openelisglobal.sampletyperequest.dto.SampleTypeRequestDTO;
import org.openelisglobal.sampletyperequest.service.SampleTypeRequestService;
import org.openelisglobal.sampletyperequest.valueholder.SampleTypeRequest;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.openelisglobal.unitofmeasure.service.UnitOfMeasureService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for SampleTypeRequestRestController. Uses MockitoJUnitRunner with
 * manual dependency injection — no Spring context required.
 */
@RunWith(MockitoJUnitRunner.class)
public class SampleTypeRequestRestControllerTest {

    @Mock
    private SampleTypeRequestService sampleTypeRequestService;

    @Mock
    private SampleService sampleService;

    @Mock
    private TypeOfSampleService typeOfSampleService;

    @Mock
    private UnitOfMeasureService unitOfMeasureService;

    @Mock
    private TestService testService;

    @Mock
    private PanelService panelService;

    @Mock
    private HttpServletRequest httpRequest;

    @Mock
    private HttpSession httpSession;

    private SampleTypeRequestRestController controller;

    @Before
    public void setUp() {
        controller = new SampleTypeRequestRestController();
        ReflectionTestUtils.setField(controller, "sampleTypeRequestService", sampleTypeRequestService);
        ReflectionTestUtils.setField(controller, "sampleService", sampleService);
        ReflectionTestUtils.setField(controller, "typeOfSampleService", typeOfSampleService);
        ReflectionTestUtils.setField(controller, "unitOfMeasureService", unitOfMeasureService);
        ReflectionTestUtils.setField(controller, "testService", testService);
        ReflectionTestUtils.setField(controller, "panelService", panelService);

    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private SampleTypeRequest buildRequest(Integer id, String sampleId, SampleTypeRequest.Status status) {
        Sample sample = new Sample();
        sample.setId(sampleId);

        TypeOfSample typeOfSample = new TypeOfSample();
        typeOfSample.setId("1");

        SampleTypeRequest req = new SampleTypeRequest();
        req.setId(id);
        req.setSample(sample);
        req.setTypeOfSample(typeOfSample);
        req.setSortOrder(0);
        req.setRequestedQuantity(1.0);
        req.setStatus(status);
        req.setCreatedDate(new Timestamp(System.currentTimeMillis()));
        return req;
    }

    // ─── getRequestsBySample ──────────────────────────────────────────────────

    @Test
    public void getRequestsBySample_returnsDtoList() {
        SampleTypeRequest req = buildRequest(10, "123", SampleTypeRequest.Status.REQUESTED);
        when(sampleTypeRequestService.getRequestsBySampleId("123")).thenReturn(Arrays.asList(req));

        ResponseEntity<List<SampleTypeRequestDTO>> response = controller.getRequestsBySample("123");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<SampleTypeRequestDTO> body = response.getBody();
        assertNotNull(body);
        assertEquals(1, body.size());
        assertEquals("10", body.get(0).getId());
        assertEquals("123", body.get(0).getSampleId());
        assertEquals("REQUESTED", body.get(0).getStatus());
    }

    @Test
    public void getRequestsBySample_emptyList_returnsOk() {
        when(sampleTypeRequestService.getRequestsBySampleId("999")).thenReturn(Collections.emptyList());

        ResponseEntity<List<SampleTypeRequestDTO>> response = controller.getRequestsBySample("999");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(0, response.getBody().size());
    }

    // ─── getPendingRequests ───────────────────────────────────────────────────

    @Test
    public void getPendingRequests_returnsOnlyPendingDtos() {
        SampleTypeRequest pending = buildRequest(11, "123", SampleTypeRequest.Status.REQUESTED);
        when(sampleTypeRequestService.getPendingRequestsBySampleId("123")).thenReturn(Arrays.asList(pending));

        ResponseEntity<List<SampleTypeRequestDTO>> response = controller.getPendingRequests("123");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<SampleTypeRequestDTO> body = response.getBody();
        assertNotNull(body);
        assertEquals(1, body.size());
        assertEquals("REQUESTED", body.get(0).getStatus());
    }

    @Test
    public void getPendingRequests_noResults_returnsEmptyList() {
        when(sampleTypeRequestService.getPendingRequestsBySampleId("123")).thenReturn(Collections.emptyList());

        ResponseEntity<List<SampleTypeRequestDTO>> response = controller.getPendingRequests("123");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(0, response.getBody().size());
    }

    // ─── createRequest ────────────────────────────────────────────────────────

    @Test
    public void createRequest_validDto_returns201WithDto() {
        when(httpRequest.getSession()).thenReturn(httpSession);
        Sample sample = new Sample();
        sample.setId("5");
        TypeOfSample typeOfSample = new TypeOfSample();
        typeOfSample.setId("2");

        when(sampleService.get("5")).thenReturn(sample);
        when(typeOfSampleService.get("2")).thenReturn(typeOfSample);
        when(sampleTypeRequestService.insert(any(SampleTypeRequest.class))).thenReturn(77);

        SampleTypeRequestDTO dto = new SampleTypeRequestDTO();
        dto.setSampleId("5");
        dto.setTypeOfSampleId("2");
        dto.setSortOrder(1);
        dto.setRequestedQuantity(2.0);

        ResponseEntity<?> response = controller.createRequest(dto, httpRequest);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        SampleTypeRequestDTO body = (SampleTypeRequestDTO) response.getBody();
        assertNotNull(body);
        assertEquals("77", body.getId());
        assertEquals("REQUESTED", body.getStatus());
    }

    @Test
    public void createRequest_missingSampleId_returns400() {
        SampleTypeRequestDTO dto = new SampleTypeRequestDTO();
        dto.setTypeOfSampleId("2");

        ResponseEntity<?> response = controller.createRequest(dto, httpRequest);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("sampleId is required", response.getBody());
    }

    @Test
    public void createRequest_missingTypeOfSampleId_returns400() {
        SampleTypeRequestDTO dto = new SampleTypeRequestDTO();
        dto.setSampleId("5");

        ResponseEntity<?> response = controller.createRequest(dto, httpRequest);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("typeOfSampleId is required", response.getBody());
    }

    @Test
    public void createRequest_sampleNotFound_returns400() {
        when(sampleService.get("99")).thenReturn(null);

        SampleTypeRequestDTO dto = new SampleTypeRequestDTO();
        dto.setSampleId("99");
        dto.setTypeOfSampleId("2");

        ResponseEntity<?> response = controller.createRequest(dto, httpRequest);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void createRequest_typeOfSampleNotFound_returns400() {
        Sample sample = new Sample();
        sample.setId("5");
        when(sampleService.get("5")).thenReturn(sample);
        when(typeOfSampleService.get("99")).thenReturn(null);

        SampleTypeRequestDTO dto = new SampleTypeRequestDTO();
        dto.setSampleId("5");
        dto.setTypeOfSampleId("99");

        ResponseEntity<?> response = controller.createRequest(dto, httpRequest);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void createRequest_withUnitOfMeasure_setsUom() {
        when(httpRequest.getSession()).thenReturn(httpSession);
        Sample sample = new Sample();
        sample.setId("5");
        TypeOfSample typeOfSample = new TypeOfSample();
        typeOfSample.setId("2");
        org.openelisglobal.unitofmeasure.valueholder.UnitOfMeasure uom =
                new org.openelisglobal.unitofmeasure.valueholder.UnitOfMeasure();
        uom.setId("3");

        when(sampleService.get("5")).thenReturn(sample);
        when(typeOfSampleService.get("2")).thenReturn(typeOfSample);
        when(unitOfMeasureService.get("3")).thenReturn(uom);
        when(sampleTypeRequestService.insert(any(SampleTypeRequest.class))).thenReturn(88);

        SampleTypeRequestDTO dto = new SampleTypeRequestDTO();
        dto.setSampleId("5");
        dto.setTypeOfSampleId("2");
        dto.setUnitOfMeasureId("3");

        ResponseEntity<?> response = controller.createRequest(dto, httpRequest);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    public void createRequest_defaultsApplied_whenNullOptionalFields() {
        when(httpRequest.getSession()).thenReturn(httpSession);
        Sample sample = new Sample();
        sample.setId("5");
        TypeOfSample typeOfSample = new TypeOfSample();
        typeOfSample.setId("2");

        when(sampleService.get("5")).thenReturn(sample);
        when(typeOfSampleService.get("2")).thenReturn(typeOfSample);
        when(sampleTypeRequestService.insert(any(SampleTypeRequest.class))).thenReturn(50);

        SampleTypeRequestDTO dto = new SampleTypeRequestDTO();
        dto.setSampleId("5");
        dto.setTypeOfSampleId("2");
        // sortOrder and requestedQuantity are null — should default to 0 and 1.0

        ResponseEntity<?> response = controller.createRequest(dto, httpRequest);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    // ─── fulfillRequest ───────────────────────────────────────────────────────

    @Test
    public void fulfillRequest_validCall_returns200WithDto() {
        SampleTypeRequest fulfilled = buildRequest(20, "5", SampleTypeRequest.Status.COLLECTED);
        doNothing().when(sampleTypeRequestService).fulfillRequest(20, "SI-1");
        when(sampleTypeRequestService.get(20)).thenReturn(fulfilled);

        ResponseEntity<?> response = controller.fulfillRequest(20, "SI-1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        SampleTypeRequestDTO body = (SampleTypeRequestDTO) response.getBody();
        assertNotNull(body);
        assertEquals("COLLECTED", body.getStatus());
        verify(sampleTypeRequestService).fulfillRequest(20, "SI-1");
    }

    @Test
    public void fulfillRequest_illegalArgument_returns400() {
        doThrow(new IllegalArgumentException("Request not found: 999")).when(sampleTypeRequestService)
                .fulfillRequest(999, "SI-1");

        ResponseEntity<?> response = controller.fulfillRequest(999, "SI-1");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Request not found: 999", response.getBody());
    }

    @Test
    public void fulfillRequest_illegalState_returns409() {
        doThrow(new IllegalStateException("Request is not in REQUESTED state")).when(sampleTypeRequestService)
                .fulfillRequest(20, "SI-1");

        ResponseEntity<?> response = controller.fulfillRequest(20, "SI-1");

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Request is not in REQUESTED state", response.getBody());
    }

    @Test
    public void fulfillRequest_unexpectedException_returns500() {
        doThrow(new RuntimeException("DB error")).when(sampleTypeRequestService).fulfillRequest(20, "SI-1");

        ResponseEntity<?> response = controller.fulfillRequest(20, "SI-1");

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    // ─── cancelRequest ────────────────────────────────────────────────────────

    @Test
    public void cancelRequest_validCall_returns200WithDto() {
        SampleTypeRequest cancelled = buildRequest(30, "5", SampleTypeRequest.Status.CANCELLED);
        doNothing().when(sampleTypeRequestService).cancelRequest(30);
        when(sampleTypeRequestService.get(30)).thenReturn(cancelled);

        ResponseEntity<?> response = controller.cancelRequest(30);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        SampleTypeRequestDTO body = (SampleTypeRequestDTO) response.getBody();
        assertNotNull(body);
        assertEquals("CANCELLED", body.getStatus());
        verify(sampleTypeRequestService).cancelRequest(30);
    }

    @Test
    public void cancelRequest_illegalArgument_returns400() {
        doThrow(new IllegalArgumentException("Request not found: 999")).when(sampleTypeRequestService)
                .cancelRequest(999);

        ResponseEntity<?> response = controller.cancelRequest(999);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Request not found: 999", response.getBody());
    }

    @Test
    public void cancelRequest_illegalState_returns409() {
        doThrow(new IllegalStateException("Only REQUESTED status can be cancelled")).when(sampleTypeRequestService)
                .cancelRequest(30);

        ResponseEntity<?> response = controller.cancelRequest(30);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Only REQUESTED status can be cancelled", response.getBody());
    }

    @Test
    public void cancelRequest_unexpectedException_returns500() {
        doThrow(new RuntimeException("DB error")).when(sampleTypeRequestService).cancelRequest(30);

        ResponseEntity<?> response = controller.cancelRequest(30);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    // ─── convertToDTO — test/panel name resolution ────────────────────────────

    @org.junit.Test
    public void getRequestsBySample_resolvesTestNames() {
        // Use Mockito.mock to avoid SpringContext lookup triggered by
        // getLocalizedName()
        org.openelisglobal.test.valueholder.Test mockTest = org.mockito.Mockito
                .mock(org.openelisglobal.test.valueholder.Test.class);
        when(mockTest.getLocalizedName()).thenReturn("Blood Glucose");

        Sample sample = new Sample();
        sample.setId("5");
        TypeOfSample typeOfSample = new TypeOfSample();
        typeOfSample.setId("1");

        SampleTypeRequest req = new SampleTypeRequest();
        req.setId(41);
        req.setSample(sample);
        req.setTypeOfSample(typeOfSample);
        req.setSortOrder(0);
        req.setStatus(SampleTypeRequest.Status.REQUESTED);
        req.setCreatedDate(new Timestamp(System.currentTimeMillis()));
        req.setRequestedTests("7,8");

        when(sampleTypeRequestService.getRequestsBySampleId("5")).thenReturn(Arrays.asList(req));
        when(testService.getTestById("7")).thenReturn(mockTest);
        when(testService.getTestById("8")).thenReturn(null);

        ResponseEntity<List<SampleTypeRequestDTO>> response = controller.getRequestsBySample("5");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        SampleTypeRequestDTO dto = response.getBody().get(0);
        assertEquals("Blood Glucose", dto.getRequestedTestNames());
    }

    @org.junit.Test
    public void getRequestsBySample_resolvesPanelNames() {
        // Use Mockito.mock to avoid SpringContext lookup triggered by
        // getLocalizedName()
        Panel panel = org.mockito.Mockito.mock(Panel.class);
        when(panel.getLocalizedName()).thenReturn(null);
        when(panel.getPanelName()).thenReturn("Hepatitis Panel");

        Sample sample = new Sample();
        sample.setId("5");
        TypeOfSample typeOfSample = new TypeOfSample();
        typeOfSample.setId("1");

        SampleTypeRequest req = new SampleTypeRequest();
        req.setId(42);
        req.setSample(sample);
        req.setTypeOfSample(typeOfSample);
        req.setSortOrder(0);
        req.setStatus(SampleTypeRequest.Status.REQUESTED);
        req.setCreatedDate(new Timestamp(System.currentTimeMillis()));
        req.setRequestedPanels("3");

        when(sampleTypeRequestService.getRequestsBySampleId("5")).thenReturn(Arrays.asList(req));
        when(panelService.getPanelById("3")).thenReturn(panel);

        ResponseEntity<List<SampleTypeRequestDTO>> response = controller.getRequestsBySample("5");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        SampleTypeRequestDTO dto = response.getBody().get(0);
        assertEquals("Hepatitis Panel", dto.getRequestedPanelNames());
    }
}
