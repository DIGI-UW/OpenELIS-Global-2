package org.openelisglobal.dataexchange.externalcatalog.controller.rest;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.dataexchange.externalcatalog.dto.CatalogDefinitionRequest;
import org.openelisglobal.dataexchange.externalcatalog.service.CatalogInboundService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Controller-layer tests for {@link CatalogInboundRestController}.
 *
 * Strategy: the service is mocked so every test is purely about the controller
 * + validator pipeline — no DB, no Spring context overhead beyond what
 * BaseWebContextSensitiveTest already provides.
 *
 * Coverage: - Happy path: test upsert, panel upsert - Validation: missing
 * identifier, missing name, LOINC too long, missing resultType, unknown
 * resultType, panel missing sampleType - Multiple errors collected in one shot
 * - Service-level CatalogValidationException bubbles to 400 - Unexpected
 * service exception → 500 with JSON body (no raw string) - Response envelope
 * shape: status, message, guid, errors fields
 */
@TestPropertySource(properties = "org.openelisglobal.external.catalog.inbound.enabled=true")
public class CatalogInboundRestControllerTest extends BaseWebContextSensitiveTest {

    private ObjectMapper objectMapper;
    private CatalogInboundService mockService;
    private CatalogInboundRestController controller;

    @Autowired
    private CatalogInboundService catalogInboundService;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        objectMapper = new ObjectMapper();

        // Replace the real service with a mock for every test — keeps tests fast
        // and deterministic. The validator is the real bean (not mocked) so
        // validation logic is exercised for real.
        mockService = Mockito.mock(CatalogInboundService.class);
        controller = webApplicationContext.getBean(CatalogInboundRestController.class);
        ReflectionTestUtils.setField(controller, "catalogInboundService", mockService);
    }

    // =========================================================================
    // Happy path
    // =========================================================================

    @Test
    public void testUpsertTest_success() throws Exception {
        CatalogDefinitionRequest req = validTestRequest("happy-test-001");
        Mockito.when(mockService.upsert(Mockito.any(), Mockito.any())).thenReturn("happy-test-001");

        mockMvc.perform(post("/rest/catalog").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("Catalog item upserted"))
                .andExpect(jsonPath("$.guid").value("happy-test-001")).andExpect(jsonPath("$.errors").doesNotExist());
    }

    @Test
    public void testUpsertPanel_success() throws Exception {
        CatalogDefinitionRequest req = validPanelRequest("happy-panel-001");
        Mockito.when(mockService.upsert(Mockito.any(), Mockito.any())).thenReturn("happy-panel-001");

        mockMvc.perform(post("/rest/catalog").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.guid").value("happy-panel-001"));
    }

    @Test
    public void testUpsertTest_identifiedByLoincOnly_success() throws Exception {
        // testUuid absent — loincCode alone is a valid identifier
        CatalogDefinitionRequest req = new CatalogDefinitionRequest();
        req.setLoincCode("718-7");
        req.setNameEnglish("Hemoglobin");
        req.setResultTypeName("Numeric");
        Mockito.when(mockService.upsert(Mockito.any(), Mockito.any())).thenReturn("718-7");

        mockMvc.perform(post("/rest/catalog").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    // =========================================================================
    // Rule 1 — at least one identifier required
    // =========================================================================

    @Test
    public void testValidation_missingBothIdentifiers_returns400() throws Exception {
        CatalogDefinitionRequest req = new CatalogDefinitionRequest();
        req.setNameEnglish("Some Test");
        req.setResultTypeName("Numeric");

        mockMvc.perform(post("/rest/catalog").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors", hasItem(containsString("testUuid"))));

        Mockito.verifyZeroInteractions(mockService);
    }

    @Test
    public void testValidation_blankUuidAndBlankLoinc_returns400() throws Exception {
        CatalogDefinitionRequest req = new CatalogDefinitionRequest();
        req.setTestUuid("   ");
        req.setLoincCode("");
        req.setNameEnglish("Some Test");
        req.setResultTypeName("Numeric");

        mockMvc.perform(post("/rest/catalog").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("VALIDATION_ERROR"));

        Mockito.verifyZeroInteractions(mockService);
    }

    // =========================================================================
    // Rule 2 — nameEnglish mandatory
    // =========================================================================

    @Test
    public void testValidation_missingNameEnglish_returns400() throws Exception {
        CatalogDefinitionRequest req = new CatalogDefinitionRequest();
        req.setTestUuid("test-no-name-001");
        req.setResultTypeName("Numeric");
        // nameEnglish intentionally absent

        mockMvc.perform(post("/rest/catalog").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors", hasItem(containsString("nameEnglish"))));

        Mockito.verifyZeroInteractions(mockService);
    }

    @Test
    public void testValidation_blankNameEnglish_returns400() throws Exception {
        CatalogDefinitionRequest req = new CatalogDefinitionRequest();
        req.setTestUuid("test-blank-name-001");
        req.setNameEnglish("   ");
        req.setResultTypeName("Numeric");

        mockMvc.perform(post("/rest/catalog").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors", hasItem(containsString("nameEnglish"))));

        Mockito.verifyZeroInteractions(mockService);
    }

    // =========================================================================
    // Rule 3 — LOINC length guard
    // =========================================================================

    @Test
    public void testValidation_loincExactly10Chars_passes() throws Exception {
        CatalogDefinitionRequest req = new CatalogDefinitionRequest();
        req.setTestUuid("loinc-10-001");
        req.setNameEnglish("Ten Char Loinc Test");
        req.setLoincCode("1234567890"); // exactly 10
        req.setResultTypeName("Numeric");
        Mockito.when(mockService.upsert(Mockito.any(), Mockito.any())).thenReturn("loinc-10-001");

        mockMvc.perform(post("/rest/catalog").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    public void testValidation_loincTooLong_returns400() throws Exception {
        CatalogDefinitionRequest req = new CatalogDefinitionRequest();
        req.setTestUuid("loinc-long-001");
        req.setNameEnglish("Long Loinc Test");
        req.setLoincCode("xx-panel-loinc"); // 14 chars — the exact value from the error log
        req.setResultTypeName("Numeric");

        mockMvc.perform(post("/rest/catalog").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors", hasItem(containsString("loincCode"))));

        Mockito.verifyZeroInteractions(mockService);
    }

    @Test
    public void testValidation_loincExactly11Chars_returns400() throws Exception {
        CatalogDefinitionRequest req = new CatalogDefinitionRequest();
        req.setTestUuid("loinc-11-001");
        req.setNameEnglish("Eleven Char Loinc");
        req.setLoincCode("12345678901"); // 11 chars — one over the limit
        req.setResultTypeName("Numeric");

        mockMvc.perform(post("/rest/catalog").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors", hasItem(containsString("loincCode"))));

        Mockito.verifyZeroInteractions(mockService);
    }

    // =========================================================================
    // Rule 4 — test resultType required and must be a known value
    // =========================================================================

    @Test
    public void testValidation_missingResultType_returns400() throws Exception {
        CatalogDefinitionRequest req = new CatalogDefinitionRequest();
        req.setTestUuid("no-result-type-001");
        req.setNameEnglish("No Result Type Test");
        // resultTypeName intentionally absent, panel=false

        mockMvc.perform(post("/rest/catalog").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors", hasItem(containsString("resultTypeName"))));

        Mockito.verifyZeroInteractions(mockService);
    }

    @Test
    public void testValidation_unknownResultTypeName_returns400() throws Exception {
        CatalogDefinitionRequest req = new CatalogDefinitionRequest();
        req.setTestUuid("bad-result-type-001");
        req.setNameEnglish("Bad Result Type Test");
        req.setResultTypeName("Numericcc"); // typo — the exact scenario from the analysis

        mockMvc.perform(post("/rest/catalog").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors", hasItem(containsString("Numericcc"))));

        Mockito.verifyZeroInteractions(mockService);
    }

    @Test
    public void testValidation_knownResultTypeNumeric_passes() throws Exception {
        CatalogDefinitionRequest req = validTestRequest("numeric-type-001");
        req.setResultTypeName("Numeric");
        Mockito.when(mockService.upsert(Mockito.any(), Mockito.any())).thenReturn("numeric-type-001");

        mockMvc.perform(post("/rest/catalog").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))).andExpect(status().isOk());
    }

    @Test
    public void testValidation_knownResultTypeDictionary_passes() throws Exception {
        CatalogDefinitionRequest req = validTestRequest("dict-type-001");
        req.setResultTypeName("Dictionary");
        Mockito.when(mockService.upsert(Mockito.any(), Mockito.any())).thenReturn("dict-type-001");

        mockMvc.perform(post("/rest/catalog").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))).andExpect(status().isOk());
    }

    @Test
    public void testValidation_knownResultTypeAlpha_passes() throws Exception {
        CatalogDefinitionRequest req = validTestRequest("alpha-type-001");
        req.setResultTypeName("Alpha");
        Mockito.when(mockService.upsert(Mockito.any(), Mockito.any())).thenReturn("alpha-type-001");

        mockMvc.perform(post("/rest/catalog").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))).andExpect(status().isOk());
    }

    @Test
    public void testValidation_resultTypeCaseInsensitive_passes() throws Exception {
        // "numeric" lowercase should resolve fine
        CatalogDefinitionRequest req = validTestRequest("lower-numeric-001");
        req.setResultTypeName("numeric");
        Mockito.when(mockService.upsert(Mockito.any(), Mockito.any())).thenReturn("lower-numeric-001");

        mockMvc.perform(post("/rest/catalog").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))).andExpect(status().isOk());
    }

    // =========================================================================
    // Rule 5 — panel requires sampleType
    // =========================================================================

    @Test
    public void testValidation_panelMissingSampleType_returns400() throws Exception {
        CatalogDefinitionRequest req = new CatalogDefinitionRequest();
        req.setPanel(true);
        req.setTestUuid("panel-no-sample-001");
        req.setNameEnglish("Panel Without Sample Type");
        // sampleTypeName and sampleTypeId both absent

        mockMvc.perform(post("/rest/catalog").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors", hasItem(containsString("sampleTypeName"))));

        Mockito.verifyZeroInteractions(mockService);
    }

    @Test
    public void testValidation_panelWithSampleTypeName_passes() throws Exception {
        CatalogDefinitionRequest req = validPanelRequest("panel-with-sample-001");
        Mockito.when(mockService.upsert(Mockito.any(), Mockito.any())).thenReturn("panel-with-sample-001");

        mockMvc.perform(post("/rest/catalog").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    public void testValidation_panelWithSampleTypeId_passes() throws Exception {
        CatalogDefinitionRequest req = new CatalogDefinitionRequest();
        req.setPanel(true);
        req.setTestUuid("panel-sampleid-001");
        req.setNameEnglish("Panel With Sample Type ID");
        req.setSampleTypeId("42"); // ID provided instead of name
        Mockito.when(mockService.upsert(Mockito.any(), Mockito.any())).thenReturn("panel-sampleid-001");

        mockMvc.perform(post("/rest/catalog").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))).andExpect(status().isOk());
    }

    // =========================================================================
    // Multiple errors collected in one shot
    // =========================================================================

    @Test
    public void testValidation_multipleErrors_allReturnedAtOnce() throws Exception {
        // Missing identifier + missing name + long LOINC — all three should appear
        CatalogDefinitionRequest req = new CatalogDefinitionRequest();
        req.setLoincCode("this-loinc-is-way-too-long"); // >10 chars AND it's the only identifier
        // nameEnglish absent
        // resultTypeName absent

        mockMvc.perform(post("/rest/catalog").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.length()").value(3)); // identifier ok (loinc present), name missing,
                                                                    // loinc too long, resultType missing = 3

        Mockito.verifyZeroInteractions(mockService);
    }

    @Test
    public void testValidation_missingNameAndResultType_twoErrors() throws Exception {
        CatalogDefinitionRequest req = new CatalogDefinitionRequest();
        req.setTestUuid("two-errors-001");
        // nameEnglish absent, resultTypeName absent

        mockMvc.perform(post("/rest/catalog").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.length()").value(2));

        Mockito.verifyZeroInteractions(mockService);
    }

    // =========================================================================
    // Service-level CatalogValidationException → 400 (second line of defence)
    // =========================================================================

    @Test
    public void testServiceValidationException_returns400() throws Exception {
        CatalogDefinitionRequest req = validTestRequest("service-val-001");
        Mockito.when(mockService.upsert(Mockito.any(), Mockito.any()))
                .thenThrow(new org.openelisglobal.dataexchange.externalcatalog.exception.CatalogValidationException(
                        "No valid sample types could be resolved"));

        mockMvc.perform(post("/rest/catalog").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors", hasItem(containsString("sample types"))));
    }

    // =========================================================================
    // Unexpected service exception → 500 with JSON body (no raw string leak)
    // =========================================================================

    @Test
    public void testServiceRuntimeException_returns500AsJson() throws Exception {
        CatalogDefinitionRequest req = validTestRequest("runtime-ex-001");
        Mockito.when(mockService.upsert(Mockito.any(), Mockito.any()))
                .thenThrow(new RuntimeException("Simulated DB failure"));

        mockMvc.perform(post("/rest/catalog").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))).andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value("ERROR"))
                // Must NOT leak the raw exception message to the caller
                .andExpect(jsonPath("$.message").value("Internal error processing catalog item"))
                .andExpect(jsonPath("$.errors").doesNotExist());
    }

    @Test
    public void testServiceRuntimeException_responseIsJson_notPlainString() throws Exception {
        // Regression: before the fix, 500 returned a plain String body which broke
        // JSON parsers on the caller side. Verify Content-Type is application/json.
        CatalogDefinitionRequest req = validTestRequest("json-500-001");
        Mockito.when(mockService.upsert(Mockito.any(), Mockito.any())).thenThrow(new RuntimeException("boom"));

        mockMvc.perform(post("/rest/catalog").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))).andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value("ERROR"));
    }

    // =========================================================================
    // Response envelope shape
    // =========================================================================

    @Test
    public void testSuccessResponse_noErrorsField() throws Exception {
        // On success, the "errors" field must be absent (NON_NULL serialisation)
        CatalogDefinitionRequest req = validTestRequest("envelope-001");
        Mockito.when(mockService.upsert(Mockito.any(), Mockito.any())).thenReturn("envelope-001");

        mockMvc.perform(post("/rest/catalog").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))).andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").doesNotExist()).andExpect(jsonPath("$.guid").value("envelope-001"));
    }

    @Test
    public void testValidationErrorResponse_noGuidField() throws Exception {
        // On validation error, the "guid" field must be absent
        CatalogDefinitionRequest req = new CatalogDefinitionRequest();
        req.setTestUuid("no-name-001");
        // nameEnglish absent

        mockMvc.perform(post("/rest/catalog").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.guid").doesNotExist()).andExpect(jsonPath("$.status").value("VALIDATION_ERROR"));
    }

    @Test
    public void testValidationErrorResponse_hasMessageField() throws Exception {
        CatalogDefinitionRequest req = new CatalogDefinitionRequest();
        req.setTestUuid("msg-check-001");
        // nameEnglish absent

        mockMvc.perform(post("/rest/catalog").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request failed validation"));
    }

    // =========================================================================
    // Idempotency — same valid request twice hits service both times
    // =========================================================================

    @Test
    public void testIdempotentRequest_serviceCalledEachTime() throws Exception {
        CatalogDefinitionRequest req = validTestRequest("idempotent-001");
        Mockito.when(mockService.upsert(Mockito.any(), Mockito.any())).thenReturn("idempotent-001");

        mockMvc.perform(post("/rest/catalog").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))).andExpect(status().isOk());

        mockMvc.perform(post("/rest/catalog").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))).andExpect(status().isOk());

        Mockito.verify(mockService, Mockito.times(2)).upsert(Mockito.any(), Mockito.any());
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /** Minimal valid test request — passes all validation rules. */
    private CatalogDefinitionRequest validTestRequest(String uuid) {
        CatalogDefinitionRequest req = new CatalogDefinitionRequest();
        req.setPanel(false);
        req.setTestUuid(uuid);
        req.setNameEnglish("Test " + uuid);
        req.setResultTypeName("Numeric");
        req.setPrice(BigDecimal.TEN);
        req.setActive(true);
        req.setOrderable(true);
        return req;
    }

    /** Minimal valid panel request — passes all validation rules. */
    private CatalogDefinitionRequest validPanelRequest(String uuid) {
        CatalogDefinitionRequest req = new CatalogDefinitionRequest();
        req.setPanel(true);
        req.setTestUuid(uuid);
        req.setNameEnglish("Panel " + uuid);
        req.setSampleTypeName("Whole Blood");
        req.setPrice(BigDecimal.TEN);
        req.setActive(true);
        return req;
    }
}
