package org.openelisglobal.dataexchange.fhir;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.analyzer.service.AnalyzerService;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Tests persistence of Analysis.analyzerId and generation/persistence of
 * Analyzer FHIR UUID metadata used to support analyzer device references in
 * FHIR R4 Observation transformations.
 *
 * OGC-530: Analyzer metadata not flowing through FHIR Observation
 */
public class FhirDeviceTransformTest extends BaseWebContextSensitiveTest {

    @Autowired
    private AnalysisService analysisService;
    @Autowired
    private AnalyzerService analyzerService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/fhir-device-transform.xml");
    }

    @Test
    public void analysisWithAnalyzerId_shouldHaveFieldPersisted() {
        Analysis analysis = analysisService.get("1");
        assertNotNull("Analysis should exist", analysis);
        analysis.setAnalyzerId("1");
        analysisService.update(analysis);

        Analysis reloaded = analysisService.get("1");
        assertEquals("analyzerId should persist", "1", reloaded.getAnalyzerId());
    }

    @Test
    public void analysisWithoutAnalyzerId_shouldHaveNullAnalyzerId() {
        Analysis analysis = analysisService.get("1");
        assertNotNull("Analysis should exist", analysis);
        assertNull("New analysis should have null analyzerId", analysis.getAnalyzerId());
    }

    @Test
    public void analyzerFhirUuid_shouldBeGeneratedViaEnsureAndPersisted() {
        Analyzer analyzer = analyzerService.get("1");
        assertNotNull("Analyzer should exist", analyzer);

        // getFhirUuidAsString() is a pure getter — returns null when unset
        assertNull("fhirUuid should initially be null", analyzer.getFhirUuidAsString());

        // ensureFhirUuid() generates if missing
        String uuidStr = analyzer.ensureFhirUuid();
        assertNotNull("ensureFhirUuid should generate UUID", uuidStr);
        UUID.fromString(uuidStr); // validates format

        analyzerService.update(analyzer);
        Analyzer reloaded = analyzerService.get("1");
        assertEquals("fhirUuid should persist", uuidStr, reloaded.getFhirUuidAsString());
    }

    @Test
    public void analyzerFhirUuid_getFhirUuidAsString_returnsNullWhenUnset() {
        Analyzer analyzer = analyzerService.get("1");
        assertNotNull("Analyzer should exist", analyzer);

        // Pure getter returns null when fhirUuid is not set
        assertNull("getFhirUuidAsString should return null for unset uuid", analyzer.getFhirUuidAsString());

        // ensureFhirUuid generates and returns a valid UUID
        String uuid = analyzer.ensureFhirUuid();
        assertNotNull("ensureFhirUuid should generate UUID", uuid);

        // Subsequent calls to getter return the same value
        assertEquals("getter should now return the generated uuid", uuid, analyzer.getFhirUuidAsString());
    }
}
