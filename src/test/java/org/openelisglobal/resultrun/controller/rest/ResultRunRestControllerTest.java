package org.openelisglobal.resultrun.controller.rest;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

/**
 * OGC-1200: the run endpoints answer a user with Results rights in the run's
 * lab units, and hand back the accepted message as JSON, not as quoted text.
 */
public class ResultRunRestControllerTest extends BaseWebContextSensitiveTest {

    @Autowired
    private javax.sql.DataSource dataSource;

    private JdbcTemplate jdbc;
    private MockHttpSession session;
    private long runId;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/result.xml");
        jdbc = new JdbcTemplate(dataSource);
        jdbc.update("INSERT INTO clinlims.analyzer (id, name, is_active, last_updated)"
                + " VALUES (9251, 'Run endpoint analyzer', true, NOW()) ON CONFLICT (id) DO NOTHING");
        jdbc.update("INSERT INTO clinlims.user_lab_unit_roles (system_user_id, last_updated) VALUES (1, NOW())"
                + " ON CONFLICT (system_user_id) DO NOTHING");
        jdbc.update("INSERT INTO clinlims.lab_unit_role_map (lab_unit_role_map_id, lab_unit) VALUES (9351,"
                + " 'AllLabUnits') ON CONFLICT (lab_unit_role_map_id) DO NOTHING");
        jdbc.update("INSERT INTO clinlims.lab_unit_roles (system_user_id, lab_unit_role_map_id) VALUES (1, 9351)"
                + " ON CONFLICT DO NOTHING");
        jdbc.update("INSERT INTO clinlims.lab_roles (lab_unit_role_map_id, role) VALUES (9351, '5')"
                + " ON CONFLICT DO NOTHING");
        jdbc.update("DELETE FROM clinlims.analyzer_delivery_receipt WHERE id = 'receipt-run-endpoint'");
        jdbc.update("INSERT INTO clinlims.analyzer_delivery_receipt (id, connection_id, message_id, analyzer_id,"
                + " profile_id, profile_revision, results_staged, results_held, controls_processed, accepted_by,"
                + " accepted_at, bundle_json) VALUES ('receipt-run-endpoint', 'conn-run-endpoint', 'msg-run-endpoint',"
                + " 9251, 'profile', 1, 1, 0, 0, '1', NOW(), '{\"resourceType\":\"Bundle\",\"type\":\"message\"}')");
        runId = jdbc.queryForObject("SELECT nextval('clinlims.result_run_seq')", Long.class);
        jdbc.update("INSERT INTO clinlims.result_run (id, source, state, analyzer_id, delivery_receipt_id,"
                + " source_connection_id, source_message_id, row_count, opened_at, last_activity_at)"
                + " VALUES (?, 'ANALYZER', 'OPEN', 9251, 'receipt-run-endpoint', 'conn-run-endpoint',"
                + " 'msg-run-endpoint', 1, NOW(), NOW())", runId);
        jdbc.update("DELETE FROM clinlims.test_result_component WHERE id = 'rrc-run-part'");
        jdbc.update("INSERT INTO clinlims.test_result_component (id, test_id, code, label, display_order, is_active,"
                + " lastupdated, is_primary) VALUES ('rrc-run-part', 1, 'LOG', 'Log viral load', 1, 'Y', NOW(), false)");
        jdbc.update("INSERT INTO clinlims.analyzer_results (id, analyzer_id, accession_number, test_name, result,"
                + " iscontrol, test_id, component_id, run_id, raw_sub_identity, last_updated)"
                + " VALUES (nextval('analyzer_results_seq'), 9251, '12345', 'Endpoint value', '91', false, 1,"
                + " 'rrc-run-part', ?, '', NOW())", runId);
        session = authenticatedSession();
    }

    @After
    public void removeRunData() {
        jdbc.update("DELETE FROM clinlims.analyzer_results WHERE run_id = ?", runId);
        jdbc.update("DELETE FROM clinlims.result_run WHERE id = ?", runId);
        jdbc.update("DELETE FROM clinlims.analyzer_delivery_receipt WHERE id = 'receipt-run-endpoint'");
        jdbc.update("DELETE FROM clinlims.test_result_component WHERE id = 'rrc-run-part'");
        jdbc.update("DELETE FROM clinlims.analyzer WHERE id = 9251");
    }

    private static MockHttpSession authenticatedSession() {
        UserDetails userDetails = User.withUsername("admin").password("N/A").authorities("ROLE_ADMIN", "ROLE_RESULTS")
                .build();
        SecurityContext context = new SecurityContextImpl();
        context.setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, "N/A", userDetails.getAuthorities()));
        UserSessionData userSessionData = new UserSessionData();
        userSessionData.setSytemUserId(1);
        MockHttpSession httpSession = new MockHttpSession();
        httpSession.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        httpSession.setAttribute(IActionConstants.USER_SESSION_DATA, userSessionData);
        return httpSession;
    }

    @Test
    public void theRawMessageIsReturnedAsJsonNotAsQuotedText() throws Exception {
        mockMvc.perform(get("/rest/result-runs/" + runId + "/raw").session(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$.resourceType").value("Bundle")).andExpect(jsonPath("$.type").value("message"));
    }

    @Test
    public void theRunIsListedHeadedAndPagedForAUserWithResultsRights() throws Exception {
        mockMvc.perform(get("/rest/result-runs").session(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$.runs[*].label").value(hasItem("RUN-" + runId)));
        mockMvc.perform(get("/rest/result-runs/" + runId).session(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$.label").value("RUN-" + runId)).andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.rawAvailable").value(true));
        mockMvc.perform(get("/rest/result-runs/" + runId + "/rows?page=1&pageSize=25").session(session))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalRows").value(1))
                .andExpect(jsonPath("$.rows[0].value").value("91"))
                .andExpect(jsonPath("$.rows[0].part").value("Log viral load"));
    }

    @Test
    public void aRunThatDoesNotExistIsNotFound() throws Exception {
        mockMvc.perform(get("/rest/result-runs/999999999").session(session)).andExpect(status().isNotFound());
        mockMvc.perform(get("/rest/result-runs/999999999/raw").session(session)).andExpect(status().isNotFound());
    }
}
