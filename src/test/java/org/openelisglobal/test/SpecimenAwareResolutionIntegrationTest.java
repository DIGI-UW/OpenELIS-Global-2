package org.openelisglobal.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.testterminology.service.TestTerminologyMappingService;
import org.openelisglobal.testterminology.valueholder.TestTerminologyMapping;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * OGC-1145 Phase 1 (P1a) — specimen-aware test↔sample-type resolution:
 * SAMPLETYPE_TEST used as the true m:n it is (FR-5), the LOINC+specimen
 * disambiguator, and FR-14 specimen-scoped terminology routing (a
 * specimen-specific mapping wins over the shared/legacy code path).
 */
public class SpecimenAwareResolutionIntegrationTest extends BaseWebContextSensitiveTest {

    private static final long WATER = 9101L;
    private static final long DRINKING_WATER = 9102L;
    private static final long TURBIDITY = 9201L; // linked to BOTH water types
    private static final long GLUCOSE_CSF_LIKE = 9202L; // linked to DRINKING_WATER only

    private static final String SHARED_LOINC = "L-SHARED-1145";
    private static final String SCOPED_LOINC = "L-SCOPED-1145";

    @Autowired
    private TestService testService;
    @Autowired
    private TestTerminologyMappingService terminologyService;
    @Autowired
    private org.openelisglobal.typeofsample.service.TypeOfSampleService typeOfSampleService;
    @Autowired
    private javax.sql.DataSource dataSource;

    private JdbcTemplate jdbc;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        jdbc = new JdbcTemplate(dataSource);
        cleanup();
        jdbc.update(
                "INSERT INTO clinlims.localization (id, description, lastupdated)" + " VALUES (?, 'Water 1145', NOW())",
                WATER);
        jdbc.update("INSERT INTO clinlims.localization (id, description, lastupdated)"
                + " VALUES (?, 'Drinking Water 1145', NOW())", DRINKING_WATER);
        jdbc.update("INSERT INTO clinlims.type_of_sample (id, description, domain, local_abbrev, is_active,"
                + " sort_order, name_localization_id, lastupdated)"
                + " VALUES (?, 'Water 1145', 'E', 'W1145', 'true', 9101, ?, NOW())", WATER, WATER);
        jdbc.update(
                "INSERT INTO clinlims.type_of_sample (id, description, domain, local_abbrev, is_active,"
                        + " sort_order, name_localization_id, lastupdated)"
                        + " VALUES (?, 'Drinking Water 1145', 'E', 'DW1145', 'true', 9102, ?, NOW())",
                DRINKING_WATER, DRINKING_WATER);
        jdbc.update(
                "INSERT INTO clinlims.test (id, name, description, loinc, is_active, guid, lastupdated)"
                        + " VALUES (?, 'Turbidity 1145', 'Turbidity 1145', ?, 'Y', ?, NOW())",
                TURBIDITY, SHARED_LOINC, UUID.randomUUID().toString());
        jdbc.update(
                "INSERT INTO clinlims.test (id, name, description, is_active, guid, lastupdated)"
                        + " VALUES (?, 'ScopedOnly 1145', 'ScopedOnly 1145', 'Y', ?, NOW())",
                GLUCOSE_CSF_LIKE, UUID.randomUUID().toString());
        // the true m:n shape: one test, two sample types
        insertJunction(WATER, TURBIDITY);
        insertJunction(DRINKING_WATER, TURBIDITY);
        insertJunction(DRINKING_WATER, GLUCOSE_CSF_LIKE);
        // FR-14: a specimen-SCOPED terminology mapping routes to its (test,
        // specimen) — the test row itself carries NO legacy loinc
        jdbc.update("INSERT INTO clinlims.test_terminology_mapping (id, test_id, source, code, relationship,"
                + " is_active, sample_type_id, last_updated) VALUES (?, ?, 'LOINC', ?, 'SAME_AS', 'Y', ?, NOW())",
                UUID.randomUUID().toString(), GLUCOSE_CSF_LIKE, SCOPED_LOINC, DRINKING_WATER);
        // the service caches the testId→sampleTypes map; the raw-JDBC seeding
        // above bypasses the write paths that would invalidate it
        typeOfSampleService.clearCache();
    }

    private void insertJunction(long sampleTypeId, long testId) {
        jdbc.update("INSERT INTO clinlims.sampletype_test (id, sample_type_id, test_id, is_panel)"
                + " VALUES (nextval('sample_type_test_seq'), ?, ?, 'false')", sampleTypeId, testId);
    }

    @After
    public void tearDown() {
        cleanup();
    }

    private void cleanup() {
        jdbc.update("DELETE FROM clinlims.test_terminology_mapping WHERE test_id IN (?, ?)", TURBIDITY,
                GLUCOSE_CSF_LIKE);
        jdbc.update("DELETE FROM clinlims.sampletype_test WHERE test_id IN (?, ?)", TURBIDITY, GLUCOSE_CSF_LIKE);
        jdbc.update("DELETE FROM clinlims.test WHERE id IN (?, ?)", TURBIDITY, GLUCOSE_CSF_LIKE);
        jdbc.update("DELETE FROM clinlims.type_of_sample WHERE id IN (?, ?)", WATER, DRINKING_WATER);
        jdbc.update("DELETE FROM clinlims.localization WHERE id IN (?, ?)", WATER, DRINKING_WATER);
    }

    @Test
    public void getTypeOfSamples_returnsEveryAssociatedSampleType() {
        List<TypeOfSample> types = testService.getTypeOfSamples(testService.get(String.valueOf(TURBIDITY)));
        assertEquals("one test, two sample types (m:n)", 2, types.size());
        assertTrue(types.stream().anyMatch(t -> String.valueOf(WATER).equals(t.getId())));
        assertTrue(types.stream().anyMatch(t -> String.valueOf(DRINKING_WATER).equals(t.getId())));
    }

    @Test
    public void loincAndSampleType_resolveTheSameTestUnderEachOfItsSpecimens() {
        Optional<org.openelisglobal.test.valueholder.Test> underWater = testService
                .getActiveTestByLoincCodeAndSampleType(SHARED_LOINC, String.valueOf(WATER));
        Optional<org.openelisglobal.test.valueholder.Test> underDrinking = testService
                .getActiveTestByLoincCodeAndSampleType(SHARED_LOINC, String.valueOf(DRINKING_WATER));

        assertTrue(underWater.isPresent());
        assertTrue(underDrinking.isPresent());
        assertEquals(String.valueOf(TURBIDITY), underWater.get().getId());
        assertEquals(String.valueOf(TURBIDITY), underDrinking.get().getId());
    }

    @Test
    public void loincAndSampleType_isEmptyForASpecimenTheTestDoesNotRunOn() {
        // 9999 exists nowhere — never first-match to an unrelated specimen
        assertFalse(testService.getActiveTestByLoincCodeAndSampleType(SHARED_LOINC, "9999").isPresent());
    }

    @Test
    public void specimenScopedTerminologyMapping_routesDirectlyToItsTestAndSpecimen() {
        // FR-14: the scoped mapping is the ONLY route to this test (its legacy
        // test.loinc is null), and it answers only for its own specimen
        Optional<org.openelisglobal.test.valueholder.Test> scoped = testService
                .getActiveTestByLoincCodeAndSampleType(SCOPED_LOINC, String.valueOf(DRINKING_WATER));
        assertTrue("specimen-scoped mapping resolves (test, specimen)", scoped.isPresent());
        assertEquals(String.valueOf(GLUCOSE_CSF_LIKE), scoped.get().getId());

        assertFalse("the scoped code answers only for its own specimen",
                testService.getActiveTestByLoincCodeAndSampleType(SCOPED_LOINC, String.valueOf(WATER)).isPresent());
    }

    @Test
    public void fhirIntake_flagsSpecimenClarification_onlyWhenAmbiguousAndNoSpecimen() {
        // FR-8: multi-specimen test + no specimen coding → clarification needed
        org.openelisglobal.dataexchange.fhir.service.TaskInterpreter interpreter = freshInterpreter();
        interpreter.interpret(new org.hl7.fhir.r4.model.Task(), serviceRequestForLoinc(SHARED_LOINC, false),
                new org.hl7.fhir.r4.model.Patient());
        assertTrue("multi-specimen test with no specimen must flag clarification",
                interpreter.isSpecimenClarificationNeeded());

        // the same order carrying a Specimen reference resolves at accession
        interpreter = freshInterpreter();
        interpreter.interpret(new org.hl7.fhir.r4.model.Task(), serviceRequestForLoinc(SHARED_LOINC, true),
                new org.hl7.fhir.r4.model.Patient());
        assertFalse("an order carrying a specimen never needs clarification",
                interpreter.isSpecimenClarificationNeeded());

        // a single-specimen test is unambiguous with or without a specimen
        jdbc.update("UPDATE clinlims.test SET loinc = 'L-SINGLE-1145' WHERE id = ?", GLUCOSE_CSF_LIKE);
        interpreter = freshInterpreter();
        interpreter.interpret(new org.hl7.fhir.r4.model.Task(), serviceRequestForLoinc("L-SINGLE-1145", false),
                new org.hl7.fhir.r4.model.Patient());
        assertFalse("a single-specimen test never needs clarification", interpreter.isSpecimenClarificationNeeded());
    }

    /**
     * A fresh prototype interpreter with a REAL FhirContext — the test context
     * mocks the FhirContext bean, whose newJsonParser() returns null.
     */
    private org.openelisglobal.dataexchange.fhir.service.TaskInterpreter freshInterpreter() {
        org.openelisglobal.dataexchange.fhir.service.TaskInterpreter interpreter = org.openelisglobal.spring.util.SpringContext
                .getBean(org.openelisglobal.dataexchange.fhir.service.TaskInterpreter.class);
        org.springframework.test.util.ReflectionTestUtils.setField(interpreter, "fhirContext",
                ca.uhn.fhir.context.FhirContext.forR4Cached());
        return interpreter;
    }

    private org.hl7.fhir.r4.model.ServiceRequest serviceRequestForLoinc(String loincCode, boolean withSpecimen) {
        org.hl7.fhir.r4.model.ServiceRequest serviceRequest = new org.hl7.fhir.r4.model.ServiceRequest();
        serviceRequest.addIdentifier(new org.hl7.fhir.r4.model.Identifier().setValue("ORD-1145-" + loincCode));
        serviceRequest.getCode()
                .addCoding(new org.hl7.fhir.r4.model.Coding().setSystem("http://loinc.org").setCode(loincCode));
        if (withSpecimen) {
            serviceRequest.addSpecimen(new org.hl7.fhir.r4.model.Reference("Specimen/spec-1145"));
        }
        return serviceRequest;
    }

    @Test
    public void terminologyReconcile_keysOnSampleTypeScope_andLegacyLoincIgnoresScopedRows() {
        String testId = String.valueOf(TURBIDITY);

        TestTerminologyMapping shared = new TestTerminologyMapping();
        shared.setSource("LOINC");
        shared.setCode(SHARED_LOINC);
        shared.setRelationship("SAME_AS");

        TestTerminologyMapping scoped = new TestTerminologyMapping();
        scoped.setSource("LOINC");
        scoped.setCode("L-OVERRIDE-1145");
        scoped.setRelationship("SAME_AS");
        scoped.setSampleTypeId(String.valueOf(DRINKING_WATER));

        terminologyService.saveMappingsForTest(testId, List.of(shared, scoped), "1");

        List<TestTerminologyMapping> active = terminologyService.getActiveByTestId(testId);
        assertEquals(2, active.size());
        assertTrue(active.stream().anyMatch(m -> String.valueOf(DRINKING_WATER).equals(m.getSampleTypeId())
                && "L-OVERRIDE-1145".equals(m.getCode())));

        // the legacy test.loinc column follows the SHARED mapping, never the
        // specimen-scoped override
        String legacyLoinc = jdbc.queryForObject("SELECT loinc FROM clinlims.test WHERE id = ?", String.class,
                TURBIDITY);
        assertEquals(SHARED_LOINC, legacyLoinc);

        // re-saving without the scoped row soft-deletes it (reconcile keys on
        // the sample-type scope)
        terminologyService.saveMappingsForTest(testId, List.of(shared), "1");
        List<TestTerminologyMapping> afterRemove = terminologyService.getActiveByTestId(testId);
        assertEquals(1, afterRemove.size());
        assertEquals(null, afterRemove.get(0).getSampleTypeId());
    }
}
