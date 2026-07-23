package org.openelisglobal.analyzerimport.action;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ca.uhn.fhir.context.FhirContext;
import java.util.List;
import java.util.Optional;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analyzer.service.AnalyzerService;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.Analyzer.AnalyzerStatus;
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

    @Test
    @SuppressWarnings("unchecked")
    public void importFhirBundle_MissingAnalyzerRowForNumericHeader_StillStagesResult() throws Exception {
        when(analyzerService.get("999")).thenThrow(new org.hibernate.ObjectNotFoundException("999",
                org.openelisglobal.analyzer.valueholder.Analyzer.class.getName()));
        String bundleJson = "{\n" + "  \"resourceType\": \"Bundle\",\n" + "  \"type\": \"transaction\",\n"
                + "  \"entry\": [\n" + "    {\n" + "      \"fullUrl\": \"urn:uuid:specimen-1\",\n"
                + "      \"resource\": {\n" + "        \"resourceType\": \"Specimen\",\n"
                + "        \"identifier\": [{\"value\": \"DEV01269990000000001\"}]\n" + "      }\n" + "    },\n"
                + "    {\n" + "      \"resource\": {\n" + "        \"resourceType\": \"Observation\",\n"
                + "        \"specimen\": {\"reference\": \"urn:uuid:specimen-1\"},\n"
                + "        \"code\": {\"coding\": [{\"code\": \"WBC\"}]},\n" + "        \"valueString\": \"42\"\n"
                + "      }\n" + "    }\n" + "  ]\n" + "}";

        mockMvc.perform(post("/analyzer/fhir").header("X-Analyzer-Id", "999").contentType(MediaType.APPLICATION_JSON)
                .content(bundleJson)).andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.resultsInserted").value(1));

        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(analyzerResultsService).insertAnalyzerResults(captor.capture(), eq("1"));
        List<AnalyzerResults> inserted = (List<AnalyzerResults>) captor.getValue();
        org.junit.Assert.assertEquals(1, inserted.size());
        org.junit.Assert.assertNull(inserted.get(0).getAnalyzerId());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void importFhirBundle_UnknownSourceWithDevice_CreatesStubAndStagesResult() throws Exception {
        // Transparent-pipe contract: first message from a new analyzer (no existing
        // match by id/name/serial) must land in staging via a find-or-create
        // PENDING_REGISTRATION stub keyed on Device.identifier[source-ip].
        when(analyzerService.findByDiscoveredSourceId("10.0.0.42")).thenReturn(Optional.empty());
        when(analyzerService.insert(any(Analyzer.class))).thenReturn("777");
        Analyzer createdStub = new Analyzer();
        createdStub.setId("777");
        createdStub.setName("GeneXpert");
        createdStub.setStatus(AnalyzerStatus.PENDING_REGISTRATION);
        createdStub.setDiscoveredSourceId("10.0.0.42");
        when(analyzerService.get("777")).thenReturn(createdStub);

        String bundleJson = "{"
                + "\"resourceType\":\"Bundle\",\"type\":\"transaction\",\"entry\":["
                + "{\"fullUrl\":\"urn:uuid:device-1\",\"resource\":{"
                + "\"resourceType\":\"Device\","
                + "\"identifier\":[{\"system\":\"https://openelis-global.org/fhir/source-ip\",\"value\":\"10.0.0.42\"}],"
                + "\"deviceName\":[{\"name\":\"GeneXpert\",\"type\":\"manufacturer-name\"}]"
                + "}},"
                + "{\"fullUrl\":\"urn:uuid:specimen-1\",\"resource\":{"
                + "\"resourceType\":\"Specimen\",\"identifier\":[{\"value\":\"XPERT-001\"}]"
                + "}},"
                + "{\"resource\":{\"resourceType\":\"Observation\","
                + "\"specimen\":{\"reference\":\"urn:uuid:specimen-1\"},"
                + "\"code\":{\"coding\":[{\"code\":\"EV\"}]},"
                + "\"valueString\":\"DETECTED\"}}"
                + "]}";

        mockMvc.perform(post("/analyzer/fhir").contentType(MediaType.APPLICATION_JSON).content(bundleJson))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.resultsInserted").value(1))
                .andExpect(jsonPath("$.analyzerId").value("777"));

        ArgumentCaptor<Analyzer> stubCaptor = ArgumentCaptor.forClass(Analyzer.class);
        verify(analyzerService).insert(stubCaptor.capture());
        Analyzer submittedStub = stubCaptor.getValue();
        org.junit.Assert.assertEquals(AnalyzerStatus.PENDING_REGISTRATION, submittedStub.getStatus());
        org.junit.Assert.assertEquals("10.0.0.42", submittedStub.getDiscoveredSourceId());
        org.junit.Assert.assertEquals("GeneXpert", submittedStub.getName());

        ArgumentCaptor<List> resultsCaptor = ArgumentCaptor.forClass(List.class);
        verify(analyzerResultsService).insertAnalyzerResults(resultsCaptor.capture(), eq("1"));
        List<AnalyzerResults> inserted = (List<AnalyzerResults>) resultsCaptor.getValue();
        org.junit.Assert.assertEquals(1, inserted.size());
        org.junit.Assert.assertEquals("777", inserted.get(0).getAnalyzerId());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void importFhirBundle_QCObservationWithLotNumberAndControlLevelExtensions_setsFieldsOnAR() throws Exception {
        // FHIR extensions on a QC observation must propagate to the staged
        // AnalyzerResults so QCResultProcessingService can use them for the
        // Tier 1 (lotNumber) and Tier 2 (controlLevel) lot resolution.
        String bundleJson = "{" + "\"resourceType\":\"Bundle\",\"type\":\"transaction\",\"entry\":["
                + "{\"fullUrl\":\"urn:uuid:specimen-1\",\"resource\":{"
                + "\"resourceType\":\"Specimen\",\"identifier\":[{\"value\":\"QC-RUN-001\"}]" + "}},"
                + "{\"resource\":{\"resourceType\":\"Observation\","
                + "\"meta\":{\"tag\":[{\"system\":\"http://openelis-global.org/fhir/tags\",\"code\":\"QC\",\"display\":\"Quality Control\"}]},"
                + "\"extension\":["
                + "{\"url\":\"http://openelis-global.org/fhir/qc/lot-number\",\"valueString\":\"LOT-LPC-001\"},"
                + "{\"url\":\"http://openelis-global.org/fhir/qc/control-level\",\"valueString\":\"LPC\"}" + "],"
                + "\"specimen\":{\"reference\":\"urn:uuid:specimen-1\"},"
                + "\"code\":{\"coding\":[{\"code\":\"GLU\"}]}," + "\"valueString\":\"105\"" + "}}" + "]}";

        mockMvc.perform(post("/analyzer/fhir").contentType(MediaType.APPLICATION_JSON).content(bundleJson))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.resultsInserted").value(1));

        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(analyzerResultsService).insertAnalyzerResults(captor.capture(), eq("1"));
        List<AnalyzerResults> inserted = (List<AnalyzerResults>) captor.getValue();
        org.junit.Assert.assertEquals(1, inserted.size());
        AnalyzerResults ar = inserted.get(0);
        org.junit.Assert.assertTrue("QC tag should map to isControl=true", ar.getIsControl());
        org.junit.Assert.assertEquals("lot-number extension must populate AR.lotNumber", "LOT-LPC-001",
                ar.getLotNumber());
        org.junit.Assert.assertEquals("control-level extension must populate AR.controlLevel", "LPC",
                ar.getControlLevel());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void importFhirBundle_QCObservationWithoutExtensions_leavesLotAndLevelNull() throws Exception {
        // Backwards-compat: a QC observation without the new extensions must
        // not poison AR fields with stale state — both must read null on the
        // staged record so QCResultProcessingService falls through to Tier 3.
        String bundleJson = "{" + "\"resourceType\":\"Bundle\",\"type\":\"transaction\",\"entry\":["
                + "{\"fullUrl\":\"urn:uuid:specimen-1\",\"resource\":{"
                + "\"resourceType\":\"Specimen\",\"identifier\":[{\"value\":\"QC-RUN-002\"}]" + "}},"
                + "{\"resource\":{\"resourceType\":\"Observation\","
                + "\"meta\":{\"tag\":[{\"system\":\"http://openelis-global.org/fhir/tags\",\"code\":\"QC\"}]},"
                + "\"specimen\":{\"reference\":\"urn:uuid:specimen-1\"},"
                + "\"code\":{\"coding\":[{\"code\":\"GLU\"}]}," + "\"valueString\":\"100\"" + "}}" + "]}";

        mockMvc.perform(post("/analyzer/fhir").contentType(MediaType.APPLICATION_JSON).content(bundleJson))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));

        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(analyzerResultsService).insertAnalyzerResults(captor.capture(), eq("1"));
        List<AnalyzerResults> inserted = (List<AnalyzerResults>) captor.getValue();
        org.junit.Assert.assertNull("Absent lot-number extension must leave AR.lotNumber null",
                inserted.get(0).getLotNumber());
        org.junit.Assert.assertNull("Absent control-level extension must leave AR.controlLevel null",
                inserted.get(0).getControlLevel());
    }

    @Test
    public void importFhirBundle_ReusesExistingStubForKnownSourceId() throws Exception {
        // Idempotency: subsequent bundles from the same source hit the fast path
        // via findByDiscoveredSourceId and do NOT create a duplicate stub.
        Analyzer existingStub = new Analyzer();
        existingStub.setId("555");
        existingStub.setName("GeneXpert");
        existingStub.setStatus(AnalyzerStatus.PENDING_REGISTRATION);
        existingStub.setDiscoveredSourceId("10.0.0.42");
        when(analyzerService.findByDiscoveredSourceId("10.0.0.42")).thenReturn(Optional.of(existingStub));

        String bundleJson = "{" + "\"resourceType\":\"Bundle\",\"type\":\"transaction\",\"entry\":["
                + "{\"fullUrl\":\"urn:uuid:device-1\",\"resource\":{" + "\"resourceType\":\"Device\","
                + "\"identifier\":[{\"system\":\"https://openelis-global.org/fhir/source-ip\",\"value\":\"10.0.0.42\"}],"
                + "\"deviceName\":[{\"name\":\"GeneXpert\",\"type\":\"manufacturer-name\"}]" + "}},"
                + "{\"fullUrl\":\"urn:uuid:specimen-1\",\"resource\":{"
                + "\"resourceType\":\"Specimen\",\"identifier\":[{\"value\":\"XPERT-002\"}]" + "}},"
                + "{\"resource\":{\"resourceType\":\"Observation\","
                + "\"specimen\":{\"reference\":\"urn:uuid:specimen-1\"}," + "\"code\":{\"coding\":[{\"code\":\"EV\"}]},"
                + "\"valueString\":\"DETECTED\"}}" + "]}";

        mockMvc.perform(post("/analyzer/fhir").contentType(MediaType.APPLICATION_JSON).content(bundleJson))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.analyzerId").value("555"));

        verify(analyzerService, never()).insert(any(Analyzer.class));
    }
}
