package org.openelisglobal.analyzerimport.action;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ca.uhn.fhir.context.FhirContext;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analyzer.service.AnalyzerService;
import org.openelisglobal.analyzerresults.service.AnalyzerResultsService;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

public class AnalyzerFhirImportControllerTest extends BaseWebContextSensitiveTest {

    @Mock
    private AnalyzerResultsService analyzerResultsService;

    @Mock
    private AnalyzerService analyzerService;

    private AnalyzerFhirImportController controller;
    private Object originalAnalyzerResultsService;
    private Object originalAnalyzerService;
    private Object originalFhirContext;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        SecurityContextHolder.clearContext();
        MockitoAnnotations.initMocks(this);
        controller = webApplicationContext.getBean(AnalyzerFhirImportController.class);
        originalAnalyzerResultsService = ReflectionTestUtils.getField(controller, "analyzerResultsService");
        originalAnalyzerService = ReflectionTestUtils.getField(controller, "analyzerService");
        originalFhirContext = ReflectionTestUtils.getField(controller, "fhirContext");
        ReflectionTestUtils.setField(controller, "analyzerResultsService", analyzerResultsService);
        ReflectionTestUtils.setField(controller, "analyzerService", analyzerService);
        ReflectionTestUtils.setField(controller, "fhirContext", FhirContext.forR4());
    }

    @After
    public void tearDown() {
        ReflectionTestUtils.setField(controller, "analyzerResultsService", originalAnalyzerResultsService);
        ReflectionTestUtils.setField(controller, "analyzerService", originalAnalyzerService);
        ReflectionTestUtils.setField(controller, "fhirContext", originalFhirContext);
    }

    @Test
    public void importFhirBundle_NoMappableObservations_ReturnsBadRequest() throws Exception {
        String bundleJson = "{\n" + "  \"resourceType\": \"Bundle\",\n" + "  \"type\": \"transaction\",\n"
                + "  \"entry\": [\n" + "    {\n" + "      \"fullUrl\": \"urn:uuid:specimen-1\",\n"
                + "      \"resource\": {\n" + "        \"resourceType\": \"Specimen\",\n"
                + "        \"identifier\": [{\"value\": \"CNEG\"}]\n" + "      }\n" + "    },\n" + "    {\n"
                + "      \"resource\": {\n" + "        \"resourceType\": \"Observation\",\n"
                + "        \"specimen\": {\"reference\": \"urn:uuid:specimen-1\"},\n"
                + "        \"code\": {\"coding\": [{\"code\": \"CONTROL\"}]}\n" + "      }\n" + "    }\n" + "  ]\n"
                + "}";

        mockMvc.perform(post("/analyzer/fhir").contentType(MediaType.APPLICATION_JSON).content(bundleJson))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorKey").value("analyzer.fhirImport.error.noObservations"));

        verify(analyzerResultsService, never()).insertAnalyzerResults(anyList(), eq("1"));
    }

    @Test
    public void importFhirBundle_ValidObservation_InsertsAnalyzerResults() throws Exception {
        String bundleJson = "{\n" + "  \"resourceType\": \"Bundle\",\n" + "  \"type\": \"transaction\",\n"
                + "  \"entry\": [\n" + "    {\n" + "      \"fullUrl\": \"urn:uuid:specimen-1\",\n"
                + "      \"resource\": {\n" + "        \"resourceType\": \"Specimen\",\n"
                + "        \"identifier\": [{\"value\": \"HARN-FC-2026-00003\"}]\n" + "      }\n" + "    },\n"
                + "    {\n" + "      \"resource\": {\n" + "        \"resourceType\": \"Observation\",\n"
                + "        \"specimen\": {\"reference\": \"urn:uuid:specimen-1\"},\n"
                + "        \"code\": {\"coding\": [{\"code\": \"WBC\"}]},\n" + "        \"valueString\": \"Detected\"\n"
                + "      }\n" + "    }\n" + "  ]\n" + "}";

        mockMvc.perform(post("/analyzer/fhir").contentType(MediaType.APPLICATION_JSON).content(bundleJson))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.resultsInserted").value(1));

        verify(analyzerResultsService).insertAnalyzerResults(anyList(), eq("1"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void importFhirBundle_QcTaggedObservation_PersistsControlResult() throws Exception {
        String bundleJson = "{\n" + "  \"resourceType\": \"Bundle\",\n" + "  \"type\": \"transaction\",\n"
                + "  \"entry\": [\n" + "    {\n" + "      \"fullUrl\": \"urn:uuid:specimen-1\",\n"
                + "      \"resource\": {\n" + "        \"resourceType\": \"Specimen\",\n"
                + "        \"identifier\": [{\"value\": \"CNEG\"}]\n" + "      }\n" + "    },\n" + "    {\n"
                + "      \"resource\": {\n" + "        \"resourceType\": \"Observation\",\n"
                + "        \"meta\": {\"tag\": [{\"system\": \"http://openelis-global.org/fhir/tags\", \"code\": \"QC\", \"display\": \"Quality Control\"}]},\n"
                + "        \"specimen\": {\"reference\": \"urn:uuid:specimen-1\"},\n"
                + "        \"code\": {\"coding\": [{\"code\": \"VIH-1\"}]},\n"
                + "        \"valueString\": \"Positive\"\n" + "      }\n" + "    }\n" + "  ]\n" + "}";

        mockMvc.perform(post("/analyzer/fhir").contentType(MediaType.APPLICATION_JSON).content(bundleJson))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.resultsInserted").value(1));

        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(analyzerResultsService).insertAnalyzerResults(captor.capture(), eq("1"));
        List<AnalyzerResults> inserted = (List<AnalyzerResults>) captor.getValue();
        org.junit.Assert.assertEquals(1, inserted.size());
        org.junit.Assert.assertTrue("QC tag should map to isControl=true", inserted.get(0).getIsControl());
    }
}
