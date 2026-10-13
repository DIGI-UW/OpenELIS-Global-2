package org.openelisglobal.result.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analyzerresults.action.beanitems.AnalyzerResultItem;
import org.openelisglobal.result.action.util.ResultEntryAlert;
import org.openelisglobal.result.service.ResultEntryAcknowledgementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@Transactional
public class AnalyzerResultsControllerTest extends BaseWebContextSensitiveTest {

    private AnnotationConfigWebApplicationContext securityContext;

    @Autowired
    private javax.sql.DataSource dataSource;

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @EnableMethodSecurity
    static class TestConfig {
        @Bean
        AnalyzerResultsController analyzerResultsController(ApplicationContext context) {
            return context.getParent().getBean(AnalyzerResultsController.class);
        }

        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated());
            return http.build();
        }
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
        // Add the real security interceptors without bootstrapping another
        // database-backed application.
        securityContext = new AnnotationConfigWebApplicationContext();
        securityContext.setParent(webApplicationContext);
        securityContext.setServletContext(webApplicationContext.getServletContext());
        securityContext.register(TestConfig.class);
        securityContext.refresh();
        mockMvc = MockMvcBuilders.webAppContextSetup(securityContext).apply(springSecurity()).build();
        executeDataSetWithStateManagement("testdata/analyzer-results.xml");
    }

    @After
    public void closeSecurityContext() {
        if (securityContext != null) {
            securityContext.close();
        }
    }

    @Test
    public void showRestAnalyzerResults_ShouldReturnResultList_WhenQueriedByAnalyzerId() throws Exception {
        mockMvc.perform(get("/rest/AnalyzerResults").with(user("admin").roles("ADMIN")).param("id", "2001"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.resultList").isArray())
                .andExpect(jsonPath("$.resultList[0].accessionNumber").value("ACC123456"))
                .andExpect(jsonPath("$.resultList[1].importIssueReason").value("UNKNOWN_RESULT_VALUE"))
                .andExpect(jsonPath("$.resultList[1].sourceProfileId").value("genexpert-astm"))
                .andExpect(jsonPath("$.resultList[1].sourceProfileRevision").value(3))
                .andExpect(jsonPath("$.resultList[1].rawTestCode").value("QUAL_RESULT"))
                .andExpect(jsonPath("$.resultList[1].rawResultValue").value("POSITIVE"))
                .andExpect(jsonPath("$.resultList[1].instrumentNote").value("Reagent lot near expiry"));
    }

    @Test
    public void aReviewRowCarriesWhatTheInstrumentReportedAboutTheResult() throws Exception {
        new JdbcTemplate(dataSource).update(
                "UPDATE clinlims.analyzer_results SET instrument_flags = ?, assay_name = ?,"
                        + " assay_version = ?, instrument_operator = ? WHERE id = 1001",
                "H", "Xpert HIV-1 Viral Load", "4", "Operator 12");

        mockMvc.perform(get("/rest/AnalyzerResults").with(user("admin").roles("ADMIN")).param("id", "2001"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.resultList[0].instrumentFlags").value("H"))
                .andExpect(jsonPath("$.resultList[0].assayName").value("Xpert HIV-1 Viral Load"))
                .andExpect(jsonPath("$.resultList[0].assayVersion").value("4"))
                .andExpect(jsonPath("$.resultList[0].instrumentOperator").value("Operator 12"));
    }

    @Test
    public void aReviewRowOnAComponentCarriesTheComponentsLabel() throws Exception {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.update("INSERT INTO clinlims.test_result_component (id, test_id, code, label, is_primary, is_active,"
                + " lastupdated) VALUES ('comp-log-review', 4001, 'LOG', 'Log viral load', false, 'Y', NOW())");
        try {
            jdbc.update("UPDATE clinlims.analyzer_results SET component_id = 'comp-log-review' WHERE id = 1001");

            mockMvc.perform(get("/rest/AnalyzerResults").with(user("admin").roles("ADMIN")).param("id", "2001"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.resultList[0].componentLabel").value("Log viral load"));
        } finally {
            jdbc.update("UPDATE clinlims.analyzer_results SET component_id = NULL WHERE id = 1001");
            jdbc.update("DELETE FROM clinlims.test_result_component WHERE id = 'comp-log-review'");
        }
    }

    /**
     * A viral load is reported in whole copies and its log to two decimals: a
     * number on a component keeps that component's digits, not the test's main
     * result's.
     */
    @Test
    public void aNumberOnAComponentKeepsTheComponentsDigits() throws Exception {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.update("INSERT INTO clinlims.test_result (id, test_id, tst_rslt_type, significant_digits, is_active,"
                + " lastupdated) VALUES (4901, 4001, 'N', 0, true, NOW())");
        jdbc.update("INSERT INTO clinlims.test_result_component (id, test_id, code, label, is_primary, is_active,"
                + " significant_digits, lastupdated) VALUES ('comp-log-digits', 4001, 'LOG', 'Log viral load', false,"
                + " 'Y', 2, NOW())");
        jdbc.update("UPDATE clinlims.analyzer_results SET component_id = 'comp-log-digits', result = '3.00'"
                + " WHERE id = 1001");

        mockMvc.perform(get("/rest/AnalyzerResults").with(user("admin").roles("ADMIN")).param("id", "2001"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.resultList[0].result").value("3.00"))
                .andExpect(jsonPath("$.resultList[0].significantDigits").value("2"));
    }

    @Test
    public void awaitingSpecimenKeepsItsMappedValueAvailableForReview() throws Exception {
        new JdbcTemplate(dataSource).update(
                "UPDATE clinlims.analyzer_results SET import_issue_reason = ?" + " WHERE id = 1001",
                "awaiting_specimen");

        mockMvc.perform(get("/rest/AnalyzerResults").with(user("admin").roles("ADMIN")).param("id", "2001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultList[0].importIssueReason").value("awaiting_specimen"))
                .andExpect(jsonPath("$.resultList[0].readOnly").value(false))
                .andExpect(jsonPath("$.resultList[0].result").value("5.6"));
    }

    @Test
    public void anOrderedTestIsPlacedOnItsAnalysisAndPreTicked() throws Exception {
        long analysis = orderFor("ACC123456", 4001, true);

        mockMvc.perform(get("/rest/AnalyzerResults").with(user("admin").roles("ADMIN")).param("id", "2001"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.resultList[0].placement.state").value("RESOLVED"))
                .andExpect(jsonPath("$.resultList[0].placement.match").value("ACCESSION"))
                .andExpect(jsonPath("$.resultList[0].placement.proposedAnalysisId").value(String.valueOf(analysis)))
                .andExpect(jsonPath("$.resultList[0].placement.tubes.length()").value(1))
                .andExpect(jsonPath("$.resultList[0].isAccepted").value(true));
    }

    @Test
    public void aGroupingWithOneUnorderedResultIsNotPreTickedForAnyOfItsResults() throws Exception {
        orderFor("ACC123456", 4001, true);
        new JdbcTemplate(dataSource).update("INSERT INTO clinlims.analyzer_results (id, analyzer_id, accession_number,"
                + " test_name, result, units, iscontrol, last_updated, read_only, complete_date, test_result_type,"
                + " test_id) VALUES (1101, 2001, 'ACC123456', 'Urea', '4.1', 'mmol/L', false, NOW(), false, NOW(),"
                + " 'N', 4002)");

        mockMvc.perform(get("/rest/AnalyzerResults").with(user("admin").roles("ADMIN")).param("id", "2001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultList[?(@.id == 1001)].placement.state").value("RESOLVED"))
                .andExpect(jsonPath("$.resultList[?(@.id == 1101)].placement.state").value("UNORDERED_ONE_FITS"))
                .andExpect(jsonPath("$.resultList[?(@.id == 1001)].isAccepted").value(false))
                .andExpect(jsonPath("$.resultList[?(@.id == 1101)].isAccepted").value(false));
    }

    @Test
    public void aResultThatNeedsADecisionIsNotPreTicked() throws Exception {
        orderFor("ACC123456", 4001, false);

        mockMvc.perform(get("/rest/AnalyzerResults").with(user("admin").roles("ADMIN")).param("id", "2001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultList[0].placement.state").value("RETEST_CHOICE"))
                .andExpect(jsonPath("$.resultList[0].isAccepted").value(false));
    }

    @Test
    public void aSpecimenNoOrderCarriesIsANewSampleAndNotPreTicked() throws Exception {
        mockMvc.perform(get("/rest/AnalyzerResults").with(user("admin").roles("ADMIN")).param("id", "2002"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultList[?(@.accessionNumber == 'ACC345678')].placement.state")
                        .value("NEW_SAMPLE"))
                .andExpect(jsonPath("$.resultList[?(@.accessionNumber == 'ACC345678')].isAccepted").value(false));
    }

    @Test
    public void theInstrumentsOwnSpecimenIdAndItsDeliveryAreOnTheRow() throws Exception {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.update("INSERT INTO clinlims.analyzer_delivery_receipt (id, connection_id, message_id, analyzer_id,"
                + " profile_id, profile_revision, results_staged, results_held, controls_processed, accepted_by,"
                + " accepted_at, bundle_json) VALUES ('receipt-1001', 'bridge-2001', 'msg-1001', 2001,"
                + " 'genexpert-astm', 3, 1, 0, 0, '1', NOW(), '{}')");
        jdbc.update("UPDATE clinlims.analyzer_results SET instrument_specimen_id = 'ACC123456-2',"
                + " source_connection_id = 'bridge-2001', source_message_id = 'msg-1001' WHERE id = 1001");

        mockMvc.perform(get("/rest/AnalyzerResults").with(user("admin").roles("ADMIN")).param("id", "2001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultList[0].instrumentSpecimenId").value("ACC123456-2"))
                .andExpect(jsonPath("$.resultList[0].deliveryReceiptId").value("receipt-1001"));
    }

    @Test
    public void aMismatchedInstrumentPatientIsShownAndKeepsTheGroupingUnticked() throws Exception {
        orderFor("ACC123456", 4001, true);
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        long person = jdbc.queryForObject("SELECT nextval('person_seq')", Long.class);
        jdbc.update("INSERT INTO clinlims.person (id, last_name, first_name, lastupdated)"
                + " VALUES (?, 'Doe', 'Jane', NOW())", person);
        long patient = jdbc.queryForObject("SELECT nextval('patient_seq')", Long.class);
        jdbc.update("INSERT INTO clinlims.patient (id, person_id, gender, birth_date, external_id, lastupdated)"
                + " VALUES (?, ?, 'F', '1990-01-01', 'EXT-REAL-1', NOW())", patient, person);
        long sample = jdbc.queryForObject("SELECT id FROM clinlims.sample WHERE accession_number = 'ACC123456'",
                Long.class);
        jdbc.update("INSERT INTO clinlims.sample_human (id, patient_id, samp_id, lastupdated)"
                + " VALUES (nextval('sample_human_seq'), ?, ?, NOW())", patient, sample);
        jdbc.update("UPDATE clinlims.analyzer_results SET instrument_patient_id = 'SOMEONE-ELSE',"
                + " instrument_patient_name = 'Roe, Rick' WHERE id = 1001");

        mockMvc.perform(get("/rest/AnalyzerResults").with(user("admin").roles("ADMIN")).param("id", "2001"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.resultList[0].placement.state").value("RESOLVED"))
                .andExpect(jsonPath("$.resultList[0].placement.patient.status").value("MISMATCH"))
                .andExpect(jsonPath("$.resultList[0].placement.patient.instrumentId").value("SOMEONE-ELSE"))
                .andExpect(jsonPath("$.resultList[0].placement.patient.orderName").isNotEmpty())
                .andExpect(jsonPath("$.resultList[0].isAccepted").value(false));
    }

    private long orderFor(String accession, long testId, boolean awaitingResult) {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.update("INSERT INTO clinlims.status_of_sample (id, name, code, status_type, description, is_active,"
                + " lastupdated) VALUES (9001, 'Not Tested', 1, 'ANALYSIS', 'Not tested', 'Y', NOW()),"
                + " (9002, 'Technical Acceptance', 2, 'ANALYSIS', 'Accepted', 'Y', NOW())");
        org.openelisglobal.common.services.StatusService.getInstance().refreshCache();
        long sample = jdbc.queryForObject("SELECT nextval('sample_seq')", Long.class);
        jdbc.update(
                "INSERT INTO clinlims.sample (id, accession_number, domain, entered_date, received_date,"
                        + " status_id, lastupdated) VALUES (?, ?, 'H', CURRENT_DATE, CURRENT_DATE, 7001, NOW())",
                sample, accession);
        long tube = jdbc.queryForObject("SELECT nextval('sample_item_seq')", Long.class);
        jdbc.update("INSERT INTO clinlims.sample_item (id, samp_id, sort_order, status_id, lastupdated)"
                + " VALUES (?, ?, 1, 7001, NOW())", tube, sample);
        long analysis = jdbc.queryForObject("SELECT nextval('analysis_seq')", Long.class);
        jdbc.update(
                "INSERT INTO clinlims.analysis (id, sampitem_id, test_id, status_id, analysis_type, revision,"
                        + " is_reportable, lastupdated) VALUES (?, ?, ?, ?, 'MANUAL', '0', 'Y', NOW())",
                analysis, tube, testId, awaitingResult ? 9001 : 9002);
        return analysis;
    }

    @Test
    public void showRestAnalyzerResults_RejectsUnrelatedAuthenticatedRole() throws Exception {
        mockMvc.perform(get("/rest/AnalyzerResults").with(user("admin").roles("RESULTS")).param("id", "2001"))
                .andExpect(status().isForbidden());
    }

    @Test
    public void showRestAnalyzerResults_AllowsEstablishedAnalyzerRole() throws Exception {
        mockMvc.perform(get("/rest/AnalyzerResults").with(user("admin").roles("ANALYSER_IMPORT")).param("id", "2001"))
                .andExpect(status().isOk());
    }

    /**
     * OGC-1417 — a reviewer who retypes an analyzer value into the critical range
     * acknowledges it before the batch is accepted; the refusal names the review
     * row so the page can mark it.
     */
    @Test
    public void acceptingARetypedCriticalValue_isRefusedUntilAcknowledged() throws Exception {
        seedGlucoseCriticalLimit();
        MockHttpSession session = new MockHttpSession();
        String loaded = mockMvc.perform(
                get("/rest/AnalyzerResults").with(user("admin").roles("ADMIN")).param("id", "2001").session(session))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        mockMvc.perform(post("/rest/AnalyzerResults").with(user("admin").roles("ADMIN")).with(csrf()).session(session)
                .contentType(MediaType.APPLICATION_JSON).content(acceptRow(loaded, "1001", "35")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.acknowledgementRequired[0].kind").value("CRITICAL"))
                .andExpect(jsonPath("$.acknowledgementRequired[0].rowId").value("1001"))
                .andExpect(jsonPath("$.acknowledgementRequired[0].value").value("35"));
    }

    /**
     * OGC-1417 — a value the instrument sent and the reviewer left alone was not
     * entered by a person and owes nothing, even when it is critical; the same row
     * retyped to another critical value owes the acknowledgement.
     */
    @Test
    public void anUneditedCriticalInstrumentValue_owesNothing_butTheSameRowRetypedDoes() {
        seedGlucoseCriticalLimit();
        new JdbcTemplate(dataSource).update("UPDATE clinlims.analyzer_results SET result = '25' WHERE id = 1001");
        ResultEntryAcknowledgementService acknowledgementService = securityContext.getParent()
                .getBean(ResultEntryAcknowledgementService.class);

        AnalyzerResultItem unedited = new AnalyzerResultItem();
        unedited.setId("1001");
        unedited.setResult("25");
        unedited.setIsAccepted(true);
        org.junit.Assert.assertEquals(List.of(), acknowledgementService.alertsForAnalyzerItems(List.of(unedited)));

        AnalyzerResultItem retyped = new AnalyzerResultItem();
        retyped.setId("1001");
        retyped.setResult("30");
        retyped.setIsAccepted(true);
        List<ResultEntryAlert> owed = acknowledgementService.alertsForAnalyzerItems(List.of(retyped));
        org.junit.Assert.assertEquals(1, owed.size());
        org.junit.Assert.assertEquals(ResultEntryAlert.KIND_CRITICAL, owed.get(0).getKind());
        new JdbcTemplate(dataSource).update("UPDATE clinlims.analyzer_results SET result = '5.6' WHERE id = 1001");
    }

    private void seedGlucoseCriticalLimit() {
        new JdbcTemplate(dataSource).update("INSERT INTO clinlims.result_limits (id, test_id, test_result_type_id,"
                + " min_age, max_age, low_normal, high_normal, low_valid, high_valid, low_reporting_range,"
                + " high_reporting_range, low_critical, high_critical, always_validate, lastupdated) VALUES (9601,"
                + " 4001, 4, 0, 'Infinity', 3, 7, '-Infinity', 'Infinity', '-Infinity', 'Infinity', 2, 20, false,"
                + " NOW()) ON CONFLICT (id) DO NOTHING");
    }

    private static String acceptRow(String loaded, String rowId, String value) throws Exception {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        com.fasterxml.jackson.databind.node.ObjectNode form = (com.fasterxml.jackson.databind.node.ObjectNode) mapper
                .readTree(loaded);
        for (com.fasterxml.jackson.databind.JsonNode row : form.withArray("resultList")) {
            if (rowId.equals(row.path("id").asText())) {
                ((com.fasterxml.jackson.databind.node.ObjectNode) row).put("result", value);
                ((com.fasterxml.jackson.databind.node.ObjectNode) row).put("isAccepted", true);
            }
        }
        return mapper.writeValueAsString(form);
    }
}
