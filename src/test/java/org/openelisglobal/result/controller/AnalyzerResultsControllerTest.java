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
import org.openelisglobal.security.SeededRoleAuthorities;
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
        mockMvc.perform(get("/rest/AnalyzerResults")
                .with(user("admin").authorities(SeededRoleAuthorities.role("ADMIN"))).param("id", "2001"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.resultList").isArray())
                .andExpect(jsonPath("$.resultList[0].accessionNumber").value("ACC123456"))
                .andExpect(jsonPath("$.resultList[1].importIssueReason").value("UNKNOWN_RESULT_VALUE"))
                .andExpect(jsonPath("$.resultList[1].sourceProfileId").value("genexpert-astm"))
                .andExpect(jsonPath("$.resultList[1].sourceProfileRevision").value(3))
                .andExpect(jsonPath("$.resultList[1].rawTestCode").value("QUAL_RESULT"))
                .andExpect(jsonPath("$.resultList[1].rawResultValue").value("POSITIVE"));
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
    public void showRestAnalyzerResults_RejectsUnrelatedAuthenticatedRole() throws Exception {
        mockMvc.perform(get("/rest/AnalyzerResults")
                .with(user("admin").authorities(SeededRoleAuthorities.role("RESULTS"))).param("id", "2001"))
                .andExpect(status().isForbidden());
    }

    @Test
    public void showRestAnalyzerResults_AllowsEstablishedAnalyzerRole() throws Exception {
        mockMvc.perform(get("/rest/AnalyzerResults")
                .with(user("admin").authorities(SeededRoleAuthorities.role("ANALYSER_IMPORT"))).param("id", "2001"))
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
