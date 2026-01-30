package org.openelisglobal.tb.controller;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.tb.controller.rest.TbInitialProcessingController;
import org.openelisglobal.tb.service.TbCultureReadingService;
import org.openelisglobal.tb.service.TbMediaPreparationService;
import org.openelisglobal.tb.service.TbSampleProcessingService;
import org.openelisglobal.tb.valueholder.TbCultureReading;
import org.openelisglobal.tb.valueholder.TbEnums.MediaQcStatus;
import org.openelisglobal.tb.valueholder.TbEnums.MediaType;
import org.openelisglobal.tb.valueholder.TbMediaPreparation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for TbInitialProcessingController. Tests type conversion for JSON
 * parameters and inoculation endpoint.
 */
@RunWith(MockitoJUnitRunner.class)
public class TbInitialProcessingControllerTest {

    @Mock
    private TbMediaPreparationService tbMediaPreparationService;

    @Mock
    private TbSampleProcessingService tbSampleProcessingService;

    @Mock
    private TbCultureReadingService tbCultureReadingService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpSession session;

    @Mock
    private UserSessionData userSessionData;

    @InjectMocks
    private TbInitialProcessingController controller;

    private TbMediaPreparation mockMediaBatch;

    @Before
    public void setUp() {
        // Setup mock media batch
        mockMediaBatch = new TbMediaPreparation();
        mockMediaBatch.setId(1);
        mockMediaBatch.setBatchId("LJ-001");
        mockMediaBatch.setMediaType(MediaType.LJ);
        mockMediaBatch.setQcStatus(MediaQcStatus.PASSED);
    }

    // ==================== parseInteger Helper Method Tests ====================

    @Test
    public void testParseInteger_WithInteger() {
        Integer result = invokeParseInteger(Integer.valueOf(42));
        assertEquals(Integer.valueOf(42), result);
    }

    @Test
    public void testParseInteger_WithLong() {
        Integer result = invokeParseInteger(Long.valueOf(123L));
        assertEquals(Integer.valueOf(123), result);
    }

    @Test
    public void testParseInteger_WithDouble() {
        Integer result = invokeParseInteger(Double.valueOf(99.9));
        assertEquals(Integer.valueOf(99), result);
    }

    @Test
    public void testParseInteger_WithString() {
        Integer result = invokeParseInteger("456");
        assertEquals(Integer.valueOf(456), result);
    }

    @Test
    public void testParseInteger_WithInvalidString() {
        Integer result = invokeParseInteger("not-a-number");
        assertNull(result);
    }

    @Test
    public void testParseInteger_WithNull() {
        Integer result = invokeParseInteger(null);
        assertNull(result);
    }

    @Test
    public void testParseInteger_WithEmptyString() {
        Integer result = invokeParseInteger("");
        assertNull(result);
    }

    // ==================== Inoculate Endpoint Tests ====================

    @Test
    public void testInoculateSample_WithIntegerMediaBatchId() {
        // Arrange
        setupAuthenticatedRequest();
        when(tbMediaPreparationService.get(1)).thenReturn(mockMediaBatch);

        TbCultureReading mockReading = new TbCultureReading();
        mockReading.setId(100);
        when(tbCultureReadingService.inoculate(eq("46"), eq(mockMediaBatch), isNull(), anyString()))
                .thenReturn(mockReading);

        Map<String, Object> body = new HashMap<>();
        body.put("sampleItemId", "46");
        body.put("mediaBatchId", Integer.valueOf(1)); // Integer type
        body.put("processingId", null);

        // Act
        ResponseEntity<Map<String, Object>> response = controller.inoculateSample(body, request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(100, response.getBody().get("id"));
        verify(tbCultureReadingService).inoculate(eq("46"), eq(mockMediaBatch), isNull(), anyString());
    }

    @Test
    public void testInoculateSample_WithLongMediaBatchId() {
        // Arrange - JSON parsers often return Long for numbers
        setupAuthenticatedRequest();
        when(tbMediaPreparationService.get(1)).thenReturn(mockMediaBatch);

        TbCultureReading mockReading = new TbCultureReading();
        mockReading.setId(101);
        when(tbCultureReadingService.inoculate(eq("47"), eq(mockMediaBatch), isNull(), anyString()))
                .thenReturn(mockReading);

        Map<String, Object> body = new HashMap<>();
        body.put("sampleItemId", "47");
        body.put("mediaBatchId", Long.valueOf(1L)); // Long type (common from JSON)
        body.put("processingId", null);

        // Act
        ResponseEntity<Map<String, Object>> response = controller.inoculateSample(body, request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(101, response.getBody().get("id"));
    }

    @Test
    public void testInoculateSample_WithStringMediaBatchId() {
        // Arrange - Frontend might send string
        setupAuthenticatedRequest();
        when(tbMediaPreparationService.get(1)).thenReturn(mockMediaBatch);

        TbCultureReading mockReading = new TbCultureReading();
        mockReading.setId(102);
        when(tbCultureReadingService.inoculate(eq("48"), eq(mockMediaBatch), isNull(), anyString()))
                .thenReturn(mockReading);

        Map<String, Object> body = new HashMap<>();
        body.put("sampleItemId", "48");
        body.put("mediaBatchId", "1"); // String type
        body.put("processingId", null);

        // Act
        ResponseEntity<Map<String, Object>> response = controller.inoculateSample(body, request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(102, response.getBody().get("id"));
    }

    @Test
    public void testInoculateSample_WithNumericSampleItemId() {
        // Arrange - sampleItemId could come as number from JSON
        setupAuthenticatedRequest();
        when(tbMediaPreparationService.get(1)).thenReturn(mockMediaBatch);

        TbCultureReading mockReading = new TbCultureReading();
        mockReading.setId(103);
        when(tbCultureReadingService.inoculate(eq("49"), eq(mockMediaBatch), isNull(), anyString()))
                .thenReturn(mockReading);

        Map<String, Object> body = new HashMap<>();
        body.put("sampleItemId", Integer.valueOf(49)); // Numeric sampleItemId
        body.put("mediaBatchId", 1);
        body.put("processingId", null);

        // Act
        ResponseEntity<Map<String, Object>> response = controller.inoculateSample(body, request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        // String.valueOf(49) should produce "49"
        verify(tbCultureReadingService).inoculate(eq("49"), eq(mockMediaBatch), isNull(), anyString());
    }

    @Test
    public void testInoculateSample_WithoutAuthentication() {
        // Arrange - Session exists but no userSessionData
        when(request.getSession()).thenReturn(session);
        when(session.getAttribute("userSessionData")).thenReturn(null);

        Map<String, Object> body = new HashMap<>();
        body.put("sampleItemId", "46");
        body.put("mediaBatchId", 1);

        // Act
        ResponseEntity<Map<String, Object>> response = controller.inoculateSample(body, request);

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertTrue(response.getBody().containsKey("error"));
    }

    @Test
    public void testInoculateSample_MediaBatchNotFound() {
        // Arrange
        setupAuthenticatedRequest();
        when(tbMediaPreparationService.get(999)).thenReturn(null);

        Map<String, Object> body = new HashMap<>();
        body.put("sampleItemId", "46");
        body.put("mediaBatchId", 999);
        body.put("processingId", null);

        // Act
        ResponseEntity<Map<String, Object>> response = controller.inoculateSample(body, request);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Media batch not found", response.getBody().get("error"));
    }

    @Test
    public void testInoculateSample_MediaBatchNotAvailable() {
        // Arrange
        setupAuthenticatedRequest();
        TbMediaPreparation unavailableBatch = new TbMediaPreparation();
        unavailableBatch.setId(2);
        unavailableBatch.setQcStatus(MediaQcStatus.FAILED); // Failed QC = not available
        when(tbMediaPreparationService.get(2)).thenReturn(unavailableBatch);

        Map<String, Object> body = new HashMap<>();
        body.put("sampleItemId", "46");
        body.put("mediaBatchId", 2);
        body.put("processingId", null);

        // Act
        ResponseEntity<Map<String, Object>> response = controller.inoculateSample(body, request);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Media batch not available for inoculation", response.getBody().get("error"));
    }

    // ==================== Helper Methods ====================

    private void setupAuthenticatedRequest() {
        when(request.getSession()).thenReturn(session);
        when(session.getAttribute("userSessionData")).thenReturn(userSessionData);
        when(userSessionData.getSystemUserId()).thenReturn(1);
    }

    /**
     * Use reflection to test the private parseInteger method.
     */
    private Integer invokeParseInteger(Object value) {
        return ReflectionTestUtils.invokeMethod(controller, "parseInteger", value);
    }
}
