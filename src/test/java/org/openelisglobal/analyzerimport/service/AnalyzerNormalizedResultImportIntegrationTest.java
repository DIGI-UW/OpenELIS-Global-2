package org.openelisglobal.analyzerimport.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.context.FhirContext;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.sql.DataSource;
import org.hl7.fhir.r4.model.Bundle;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

public class AnalyzerNormalizedResultImportIntegrationTest extends BaseWebContextSensitiveTest {

    private static final long PROFILE_BINDING_ID = 98401L;
    private static final long SITE_BINDING_ID = 98402L;
    private static final long SITE_BINDING_REVISION_ID = 98403L;
    private static final long ANALYZER_ID = 98404L;
    private static final String CONNECTION_ID = "bridge-connection-7f3c";
    private static final String ACCESSION = "ACC-UNKNOWN-TEST-001";
    private static final Path FIXTURE = Path.of("tools", "openelis-analyzer-bridge", "contracts", "analyzer", "v1",
            "fixtures", "normalized-unknown-test.fhir.json");
    private static final FhirContext REAL_FHIR = FhirContext.forR4();

    @Autowired
    private AnalyzerNormalizedResultImportService importService;
    @Autowired
    private DataSource dataSource;
    @Autowired
    private FhirContext fhirContext;

    private JdbcTemplate jdbc;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        when(fhirContext.newJsonParser()).thenAnswer(invocation -> REAL_FHIR.newJsonParser());
        jdbc = new JdbcTemplate(dataSource);
        cleanup();
        jdbc.update(
                "INSERT INTO clinlims.analyzer_profile_binding"
                        + " (id, profile_id, profile_revision, profile_fingerprint, last_updated)"
                        + " VALUES (?, 'site.unknown-capable', 3, ?, NOW())",
                PROFILE_BINDING_ID, "sha256:" + "1".repeat(64));
        jdbc.update("INSERT INTO clinlims.analyzer_site_binding"
                + " (id, profile_binding_id, created_by, created_at, last_updated) VALUES (?, ?, '1', NOW(), NOW())",
                SITE_BINDING_ID, PROFILE_BINDING_ID);
        jdbc.update("INSERT INTO clinlims.analyzer_site_binding_revision"
                + " (id, site_binding_id, revision_number, binding_fingerprint, created_by, created_at, last_updated)"
                + " VALUES (?, ?, 1, ?, '1', NOW(), NOW())", SITE_BINDING_REVISION_ID, SITE_BINDING_ID,
                "sha256:" + "2".repeat(64));
        jdbc.update(
                "INSERT INTO clinlims.analyzer"
                        + " (id, name, is_active, bridge_connection_id, site_binding_revision_id, last_updated)"
                        + " VALUES (?, 'Normalized import test', true, ?, ?, NOW())",
                ANALYZER_ID, CONNECTION_ID, SITE_BINDING_REVISION_ID);
    }

    @After
    public void tearDown() {
        cleanup();
    }

    @Test
    public void unknownResultIsHeldWithExactBridgeSourceEvidence() throws Exception {
        Bundle bundle = REAL_FHIR.newJsonParser().parseResource(Bundle.class, Files.readString(FIXTURE));

        AnalyzerNormalizedResultImportSummary summary = importService.importBundle(bundle, "1");

        assertEquals(String.valueOf(ANALYZER_ID), summary.analyzerId());
        assertEquals(1, summary.resultsStaged());
        assertEquals(1, summary.resultsHeld());
        assertEquals(AnalyzerResults.IMPORT_ISSUE_UNKNOWN_TEST,
                jdbc.queryForObject(
                        "SELECT import_issue_reason FROM clinlims.analyzer_results"
                                + " WHERE analyzer_id = ? AND accession_number = ?",
                        String.class, ANALYZER_ID, ACCESSION));
        assertEquals(CONNECTION_ID,
                jdbc.queryForObject(
                        "SELECT source_connection_id FROM clinlims.analyzer_results"
                                + " WHERE analyzer_id = ? AND accession_number = ?",
                        String.class, ANALYZER_ID, ACCESSION));
        assertEquals("VENDOR-NEW-42",
                jdbc.queryForObject(
                        "SELECT raw_test_code FROM clinlims.analyzer_results"
                                + " WHERE analyzer_id = ? AND accession_number = ?",
                        String.class, ANALYZER_ID, ACCESSION));
        assertEquals("PATIENT",
                jdbc.queryForObject(
                        "SELECT result_classification FROM clinlims.analyzer_results"
                                + " WHERE analyzer_id = ? AND accession_number = ?",
                        String.class, ANALYZER_ID, ACCESSION));
        assertNotNull(jdbc.queryForObject("SELECT source_payload FROM clinlims.analyzer_results"
                + " WHERE analyzer_id = ? AND accession_number = ?", String.class, ANALYZER_ID, ACCESSION));
    }

    private void cleanup() {
        if (jdbc == null) {
            return;
        }
        jdbc.update("DELETE FROM clinlims.analyzer_results WHERE analyzer_id = ?", ANALYZER_ID);
        jdbc.update("DELETE FROM clinlims.analyzer WHERE id = ?", ANALYZER_ID);
        jdbc.update("DELETE FROM clinlims.analyzer_site_binding_revision WHERE id = ?", SITE_BINDING_REVISION_ID);
        jdbc.update("DELETE FROM clinlims.analyzer_site_binding WHERE id = ?", SITE_BINDING_ID);
        jdbc.update("DELETE FROM clinlims.analyzer_profile_binding WHERE id = ?", PROFILE_BINDING_ID);
    }
}
