package org.openelisglobal.qachecklist.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.qachecklist.service.SampleQaChecklistService;
import org.openelisglobal.qachecklist.valueholder.SampleQaChecklist;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for SampleQaChecklistRestController. Uses MockitoJUnitRunner with
 * manual dependency injection — no Spring context required.
 */
@RunWith(MockitoJUnitRunner.class)
public class SampleQaChecklistRestControllerTest {

    @Mock
    private SampleQaChecklistService sampleQaChecklistService;

    @Mock
    private SampleService sampleService;

    @Mock
    private HttpServletRequest httpRequest;

    @Mock
    private HttpSession httpSession;

    private SampleQaChecklistRestController controller;

    @Before
    public void setUp() {
        controller = new SampleQaChecklistRestController();
        ReflectionTestUtils.setField(controller, "sampleQaChecklistService", sampleQaChecklistService);
        ReflectionTestUtils.setField(controller, "sampleService", sampleService);
        ReflectionTestUtils.setField(controller, "httpRequest", httpRequest);

        when(httpRequest.getSession()).thenReturn(httpSession);
    }

    // ---- GET /rest/qa-checklist/config ----

    @Test
    public void getChecklistConfig_shouldReturnActiveItems() {
        List<Dictionary> items = buildActiveChecklistItems();
        when(sampleQaChecklistService.getActiveChecklistItems()).thenReturn(items);

        ResponseEntity<?> response = controller.getChecklistConfig();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> body = (List<Map<String, Object>>) response.getBody();
        assertNotNull(body);
        assertEquals(2, body.size());
        assertEquals("patientInfoVerified", body.get(0).get("itemKey"));
        assertEquals("Patient Info", body.get(0).get("label"));
        assertEquals(true, body.get(0).get("isActive"));
    }

    @Test
    public void getChecklistConfig_whenServiceThrows_returns500() {
        when(sampleQaChecklistService.getActiveChecklistItems()).thenThrow(new RuntimeException("DB error"));

        ResponseEntity<?> response = controller.getChecklistConfig();

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    // ---- GET /rest/qa-checklist/{sampleId} ----

    @Test
    public void getQaChecklist_withExistingChecklist_returnsData() {
        SampleQaChecklist checklist = buildChecklist(42, true);
        when(sampleQaChecklistService.findBySampleId("42")).thenReturn(checklist);
        when(sampleQaChecklistService.getActiveChecklistItems()).thenReturn(buildActiveChecklistItems());

        ResponseEntity<?> response = controller.getQaChecklist("42");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals("42", body.get("sampleId"));
        assertEquals(true, body.get("allRequiredVerified"));
        @SuppressWarnings("unchecked")
        Map<String, Boolean> items = (Map<String, Boolean>) body.get("verifiedItems");
        assertTrue(items.get("patientInfoVerified"));
    }

    @Test
    public void getQaChecklist_withNoChecklist_returnsEmptyItems() {
        when(sampleQaChecklistService.findBySampleId("99")).thenReturn(null);
        when(sampleQaChecklistService.getActiveChecklistItems()).thenReturn(buildActiveChecklistItems());

        ResponseEntity<?> response = controller.getQaChecklist("99");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals(false, body.get("allRequiredVerified"));
        @SuppressWarnings("unchecked")
        Map<String, Boolean> items = (Map<String, Boolean>) body.get("verifiedItems");
        assertFalse(items.get("patientInfoVerified"));
        assertFalse(items.get("samplesVerified"));
    }

    @Test
    public void getQaChecklist_withNonNumericSampleId_returns400() {
        ResponseEntity<?> response = controller.getQaChecklist("abc");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertNotNull(body);
        assertTrue(body.get("error").contains("numeric"));
    }

    // ---- GET /rest/qa-checklist/by-lab-number/{labNumber} ----

    @Test
    public void getQaChecklistByLabNumber_withValidLabNumber_returnsChecklist() {
        Sample sample = new Sample();
        sample.setId("5");
        when(sampleService.getSampleByAccessionNumber("LAB001")).thenReturn(sample);
        when(sampleQaChecklistService.findBySampleId("5")).thenReturn(null);
        when(sampleQaChecklistService.getActiveChecklistItems()).thenReturn(new ArrayList<>());

        ResponseEntity<?> response = controller.getQaChecklistByLabNumber("LAB001");

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void getQaChecklistByLabNumber_withUnknownLabNumber_returns404() {
        when(sampleService.getSampleByAccessionNumber("UNKNOWN")).thenReturn(null);

        ResponseEntity<?> response = controller.getQaChecklistByLabNumber("UNKNOWN");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    // ---- POST /rest/qa-checklist ----

    @Test
    public void saveQaChecklist_withNumericSampleId_saves() {
        SampleQaChecklist saved = buildChecklist(42, true);
        saved.setId(1);
        when(sampleQaChecklistService.saveOrUpdateChecklist(eq(42), any(), any())).thenReturn(saved);

        Map<String, Object> body = new HashMap<>();
        body.put("sampleId", 42);
        body.put("verifiedItems", new HashMap<>());

        ResponseEntity<?> response = controller.saveQaChecklist(body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) response.getBody();
        assertNotNull(result);
        assertEquals(true, result.get("success"));
        assertEquals(42, result.get("sampleId"));
    }

    @Test
    public void saveQaChecklist_withStringSampleId_saves() {
        SampleQaChecklist saved = buildChecklist(7, false);
        saved.setId(2);
        when(sampleQaChecklistService.saveOrUpdateChecklist(eq(7), any(), any())).thenReturn(saved);

        Map<String, Object> body = new HashMap<>();
        body.put("sampleId", "7");
        body.put("verifiedItems", new HashMap<>());

        ResponseEntity<?> response = controller.saveQaChecklist(body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void saveQaChecklist_withLabNumber_resolvesAndSaves() {
        Sample sample = new Sample();
        sample.setId("10");
        when(sampleService.getSampleByAccessionNumber("LAB002")).thenReturn(sample);

        SampleQaChecklist saved = buildChecklist(10, false);
        saved.setId(3);
        when(sampleQaChecklistService.saveOrUpdateChecklist(eq(10), any(), any())).thenReturn(saved);

        Map<String, Object> body = new HashMap<>();
        body.put("labNumber", "LAB002");
        body.put("verifiedItems", new HashMap<>());

        ResponseEntity<?> response = controller.saveQaChecklist(body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void saveQaChecklist_withInvalidStringSampleId_returns400() {
        Map<String, Object> body = new HashMap<>();
        body.put("sampleId", "not-a-number");
        body.put("verifiedItems", new HashMap<>());

        ResponseEntity<?> response = controller.saveQaChecklist(body);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void saveQaChecklist_withNoIdentifier_returns400() {
        Map<String, Object> body = new HashMap<>();
        body.put("verifiedItems", new HashMap<>());

        ResponseEntity<?> response = controller.saveQaChecklist(body);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void saveQaChecklist_withUnknownLabNumber_returns404() {
        when(sampleService.getSampleByAccessionNumber("MISSING")).thenReturn(null);

        Map<String, Object> body = new HashMap<>();
        body.put("labNumber", "MISSING");
        body.put("verifiedItems", new HashMap<>());

        ResponseEntity<?> response = controller.saveQaChecklist(body);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    public void saveQaChecklist_whenServiceThrows_returns500() {
        when(sampleQaChecklistService.saveOrUpdateChecklist(anyInt(), any(), any()))
                .thenThrow(new RuntimeException("DB error"));

        Map<String, Object> body = new HashMap<>();
        body.put("sampleId", 1);
        body.put("verifiedItems", new HashMap<>());

        ResponseEntity<?> response = controller.saveQaChecklist(body);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    // ---- Helpers ----

    private List<Dictionary> buildActiveChecklistItems() {
        List<Dictionary> items = new ArrayList<>();
        Dictionary d1 = new Dictionary();
        d1.setId("1");
        d1.setDictEntry("patientInfoVerified");
        d1.setSortOrder(1);
        d1.setIsActive("Y");
        d1.setLocalAbbreviation("Patient Info");
        items.add(d1);

        Dictionary d2 = new Dictionary();
        d2.setId("2");
        d2.setDictEntry("samplesVerified");
        d2.setSortOrder(2);
        d2.setIsActive("Y");
        d2.setLocalAbbreviation("Samples");
        items.add(d2);

        return items;
    }

    private SampleQaChecklist buildChecklist(int sampleId, boolean allVerified) {
        SampleQaChecklist checklist = new SampleQaChecklist();
        checklist.setId(1);
        checklist.setSampleId(sampleId);

        Map<String, Boolean> verifiedItems = new HashMap<>();
        verifiedItems.put("patientInfoVerified", true);
        verifiedItems.put("samplesVerified", allVerified);
        checklist.setVerifiedItems(verifiedItems);
        checklist.setAllRequiredVerified(allVerified);
        checklist.setVerifiedByUserId(1);

        return checklist;
    }
}
