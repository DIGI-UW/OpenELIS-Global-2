package org.openelisglobal.testalertrule.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.testalertrule.controller.rest.TestAlertRuleRestController;
import org.openelisglobal.testalertrule.controller.rest.TestAlertRuleRestController.AlertRuleDto;
import org.openelisglobal.testalertrule.controller.rest.TestAlertRuleRestController.AlertRuleRequest;
import org.openelisglobal.testalertrule.service.TestAlertRuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.server.ResponseStatusException;

/**
 * OGC-949 / OGC-763 — per-test alert rule REST endpoints, round-tripped against
 * a real DB. Covers create + list, the 400 validation guards, the 404 guards,
 * update, delete — AND the full HTTP/JSON layer via MockMvc: the original
 * direct-invocation tests bypassed Jackson entirely, which is how the
 * un-PUT-able GET representation (raw entity leaking id/testId/lastupdated
 * against a strict-whitelist request DTO) shipped unnoticed.
 *
 * <p>
 * Contract under test: GET returns a plain DTO (id, testId + the 13 editable
 * fields — never lastupdated/sysUserId); PUT/POST accept the editable fields
 * and tolerate-but-ignore the server-managed id/testId/lastupdated so a client
 * may round-trip the GET representation; truly unknown fields still 400.
 */
public class TestAlertRuleRestControllerIntegrationTest extends BaseWebContextSensitiveTest {

    private static final long TEST_ID = 95421L;

    @Autowired
    private TestAlertRuleService alertRuleService;

    @Autowired
    private TestService testService;

    @Autowired
    private org.openelisglobal.role.service.RoleService roleService;

    @Autowired
    private javax.sql.DataSource dataSource;

    private TestAlertRuleRestController controller;
    private JdbcTemplate jdbc;
    private MockHttpSession session;
    private final ObjectMapper json = new ObjectMapper();

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        jdbc = new JdbcTemplate(dataSource);
        controller = new TestAlertRuleRestController(alertRuleService, testService, roleService);
        cleanup();
        jdbc.update(
                "INSERT INTO clinlims.test (id, name, description, is_active, guid, lastupdated)"
                        + " VALUES (?, ?, ?, 'Y', ?, NOW())",
                TEST_ID, "AlertRuleIT", "AlertRuleIT desc", UUID.randomUUID().toString());
        UserSessionData usd = new UserSessionData();
        usd.setSytemUserId(1);
        session = new MockHttpSession();
        session.setAttribute(IActionConstants.USER_SESSION_DATA, usd);
    }

    @After
    public void tearDown() {
        cleanup();
    }

    private void cleanup() {
        jdbc.update("DELETE FROM clinlims.test_alert_rule WHERE test_id = ?", TEST_ID);
        jdbc.update("DELETE FROM clinlims.test WHERE id = ?", TEST_ID);
    }

    private static MockHttpServletRequest authedRequest() {
        UserSessionData usd = new UserSessionData();
        usd.setSytemUserId(1);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(IActionConstants.USER_SESSION_DATA, usd);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(session);
        return request;
    }

    private String testId() {
        return String.valueOf(TEST_ID);
    }

    private String alertsPath() {
        return "/rest/test-catalog/" + testId() + "/alerts";
    }

    private AlertRuleRequest req(String name, String triggerType, String triggerValue) {
        AlertRuleRequest body = new AlertRuleRequest();
        body.name = name;
        body.triggerType = triggerType;
        body.triggerValue = triggerValue;
        body.notifyEmail = true;
        body.notifyOrderingPhysician = true;
        body.acknowledgmentRequired = true;
        return body;
    }

    private ObjectNode jsonRule(String name, String triggerType) {
        ObjectNode node = json.createObjectNode();
        node.put("name", name);
        node.put("triggerType", triggerType);
        node.put("enabled", true);
        node.put("notifyEmail", true);
        node.put("acknowledgmentRequired", false);
        return node;
    }

    // ---------- direct-invocation coverage (validation + 404 guards) ----------

    @Test
    public void create_thenList_returnsRule() {
        AlertRuleDto created = controller.create(testId(), req("Critical SMS", "CRITICAL", null), authedRequest())
                .getBody();
        assertEquals("Critical SMS", created.name);
        assertEquals("CRITICAL", created.triggerType);
        assertTrue(created.acknowledgmentRequired);

        List<AlertRuleDto> rules = controller.list(testId());
        assertEquals(1, rules.size());
        assertEquals("Critical SMS", rules.get(0).name);
    }

    @Test
    public void create_invalidTriggerType_throwsBadRequest() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.create(testId(), req("Bad", "SOMETIMES", null), authedRequest()));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    public void create_specificValueWithoutValue_throwsBadRequest() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.create(testId(), req("Positive", "SPECIFIC_VALUE", null), authedRequest()));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    public void create_missingName_throwsBadRequest() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.create(testId(), req("  ", "ALL", null), authedRequest()));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    public void create_unknownTest_throwsNotFound() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.create("99999999", req("X", "ALL", null), authedRequest()));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    public void update_changesFields() {
        AlertRuleDto created = controller.create(testId(), req("R1", "ALL", null), authedRequest()).getBody();
        AlertRuleRequest upd = req("R1 renamed", "SPECIFIC_VALUE", "Positive");
        AlertRuleDto updated = controller.update(testId(), created.id, upd, authedRequest());
        assertEquals("R1 renamed", updated.name);
        assertEquals("SPECIFIC_VALUE", updated.triggerType);
        assertEquals("Positive", updated.triggerValue);
    }

    @Test
    public void update_unknownRule_throwsNotFound() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.update(testId(), "no-such-rule", req("X", "ALL", null), authedRequest()));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    public void delete_removesRule() {
        AlertRuleDto created = controller.create(testId(), req("R1", "ALL", null), authedRequest()).getBody();
        assertEquals(204, controller.delete(testId(), created.id, authedRequest()).getStatusCode().value());
        assertTrue(controller.list(testId()).isEmpty());
    }

    // ---------- HTTP/JSON layer (MockMvc — exercises Jackson binding) ----------

    @Test
    public void http_create_thenGet_leaksNoServerFields() throws Exception {
        mockMvc.perform(post(alertsPath()).contentType(MediaType.APPLICATION_JSON)
                .content(jsonRule("Json Rule", "ALL").toString()).session(session)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Json Rule")).andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.lastupdated").doesNotExist()).andExpect(jsonPath("$.sysUserId").doesNotExist());

        mockMvc.perform(get(alertsPath()).session(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].lastupdated").doesNotExist())
                .andExpect(jsonPath("$[0].sysUserId").doesNotExist());
    }

    /**
     * THE regression: GET the rule, PUT the returned representation back verbatim
     * (no field changed). This is what the editor's edit modal and enable/disable
     * toggle effectively do.
     */
    @Test
    public void http_editWithoutChanges_roundTripsGetRepresentation() throws Exception {
        mockMvc.perform(post(alertsPath()).contentType(MediaType.APPLICATION_JSON)
                .content(jsonRule("Round Trip", "ABNORMAL").toString()).session(session))
                .andExpect(status().isCreated());

        MvcResult listResult = mockMvc.perform(get(alertsPath()).session(session)).andExpect(status().isOk())
                .andReturn();
        JsonNode rule = json.readTree(listResult.getResponse().getContentAsString()).get(0);

        mockMvc.perform(put(alertsPath() + "/" + rule.get("id").asText()).contentType(MediaType.APPLICATION_JSON)
                .content(rule.toString()).session(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Round Trip"));
    }

    /**
     * A legacy consumer that stored the OLD entity representation (which carried
     * lastupdated) must still be able to PUT it — the server-managed fields are
     * declared on the contract and ignored.
     */
    @Test
    public void http_editPayloadCarryingLastupdated_isAcceptedAndIgnored() throws Exception {
        mockMvc.perform(post(alertsPath()).contentType(MediaType.APPLICATION_JSON)
                .content(jsonRule("Legacy Shape", "ALL").toString()).session(session)).andExpect(status().isCreated());
        MvcResult listResult = mockMvc.perform(get(alertsPath()).session(session)).andReturn();
        ObjectNode rule = (ObjectNode) json.readTree(listResult.getResponse().getContentAsString()).get(0);

        rule.put("lastupdated", 1754899200000L);
        rule.put("name", "Legacy Shape renamed");
        mockMvc.perform(put(alertsPath() + "/" + rule.get("id").asText()).contentType(MediaType.APPLICATION_JSON)
                .content(rule.toString()).session(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Legacy Shape renamed"));
    }

    /** Every editable field survives an edit round-trip through JSON. */
    @Test
    public void http_editEachEditableField_persists() throws Exception {
        mockMvc.perform(post(alertsPath()).contentType(MediaType.APPLICATION_JSON)
                .content(jsonRule("Fields", "ALL").toString()).session(session)).andExpect(status().isCreated());
        MvcResult listResult = mockMvc.perform(get(alertsPath()).session(session)).andReturn();
        ObjectNode rule = (ObjectNode) json.readTree(listResult.getResponse().getContentAsString()).get(0);
        String id = rule.get("id").asText();

        rule.put("name", "Fields v2");
        rule.put("triggerType", "SPECIFIC_VALUE");
        rule.put("triggerValue", "Positive");
        rule.put("enabled", false);
        rule.put("notifySms", true);
        rule.put("notifyEmail", false);
        rule.put("notifyOrderingPhysician", true);
        rule.put("notifyPatient", true);
        rule.put("notifyReferringFacility", true);
        rule.put("notifyCustomPhone", "+256700000000");
        rule.put("notifyCustomEmail", "lab@example.org");
        rule.put("acknowledgmentRequired", true);

        mockMvc.perform(put(alertsPath() + "/" + id).contentType(MediaType.APPLICATION_JSON).content(rule.toString())
                .session(session)).andExpect(status().isOk());

        mockMvc.perform(get(alertsPath()).session(session)).andExpect(jsonPath("$[0].name").value("Fields v2"))
                .andExpect(jsonPath("$[0].triggerType").value("SPECIFIC_VALUE"))
                .andExpect(jsonPath("$[0].triggerValue").value("Positive"))
                .andExpect(jsonPath("$[0].enabled").value(false)).andExpect(jsonPath("$[0].notifySms").value(true))
                .andExpect(jsonPath("$[0].notifyEmail").value(false))
                .andExpect(jsonPath("$[0].notifyCustomPhone").value("+256700000000"))
                .andExpect(jsonPath("$[0].notifyCustomEmail").value("lab@example.org"))
                .andExpect(jsonPath("$[0].acknowledgmentRequired").value(true));
    }

    /** The inline enable/disable toggle — full representation, flag flipped. */
    @Test
    public void http_enableDisableToggle_roundTrips() throws Exception {
        mockMvc.perform(post(alertsPath()).contentType(MediaType.APPLICATION_JSON)
                .content(jsonRule("Toggle", "CRITICAL").toString()).session(session)).andExpect(status().isCreated());
        MvcResult listResult = mockMvc.perform(get(alertsPath()).session(session)).andReturn();
        ObjectNode rule = (ObjectNode) json.readTree(listResult.getResponse().getContentAsString()).get(0);
        String id = rule.get("id").asText();

        rule.put("enabled", false);
        mockMvc.perform(put(alertsPath() + "/" + id).contentType(MediaType.APPLICATION_JSON).content(rule.toString())
                .session(session)).andExpect(status().isOk()).andExpect(jsonPath("$.enabled").value(false));
        rule.put("enabled", true);
        mockMvc.perform(put(alertsPath() + "/" + id).contentType(MediaType.APPLICATION_JSON).content(rule.toString())
                .session(session)).andExpect(status().isOk()).andExpect(jsonPath("$.enabled").value(true));
    }

    /** Multiple rules coexist on one test; editing one leaves the other alone. */
    @Test
    public void http_multipleRulesOnOneTest_editIsolated() throws Exception {
        mockMvc.perform(post(alertsPath()).contentType(MediaType.APPLICATION_JSON)
                .content(jsonRule("Rule A", "ALL").toString()).session(session)).andExpect(status().isCreated());
        mockMvc.perform(post(alertsPath()).contentType(MediaType.APPLICATION_JSON)
                .content(jsonRule("Rule B", "CRITICAL").toString()).session(session)).andExpect(status().isCreated());

        MvcResult listResult = mockMvc.perform(get(alertsPath()).session(session))
                .andExpect(jsonPath("$.length()").value(2)).andReturn();
        JsonNode rules = json.readTree(listResult.getResponse().getContentAsString());
        ObjectNode ruleA = (ObjectNode) (rules.get(0).get("name").asText().equals("Rule A") ? rules.get(0)
                : rules.get(1));
        ruleA.put("name", "Rule A v2");
        mockMvc.perform(put(alertsPath() + "/" + ruleA.get("id").asText()).contentType(MediaType.APPLICATION_JSON)
                .content(ruleA.toString()).session(session)).andExpect(status().isOk());

        mockMvc.perform(get(alertsPath()).session(session)).andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.name == 'Rule B')].triggerType").value("CRITICAL"));
    }

    /** A legacy rule inserted before this contract existed edits fine. */
    @Test
    public void http_legacyRowFromOldWorkflow_editable() throws Exception {
        String legacyId = UUID.randomUUID().toString();
        jdbc.update("INSERT INTO clinlims.test_alert_rule (id, test_id, name, is_enabled, trigger_type, notify_sms,"
                + " notify_email, notify_ordering_physician, notify_patient, notify_referring_facility,"
                + " acknowledgment_required, last_updated) VALUES (?, ?, 'Legacy rule', true, 'ALL', false, true,"
                + " false, false, false, false, NOW())", legacyId, TEST_ID);

        MvcResult listResult = mockMvc.perform(get(alertsPath()).session(session)).andExpect(status().isOk())
                .andReturn();
        ObjectNode rule = (ObjectNode) json.readTree(listResult.getResponse().getContentAsString()).get(0);
        assertEquals("Legacy rule", rule.get("name").asText());
        assertNull(rule.get("lastupdated"));

        rule.put("name", "Legacy rule renamed");
        mockMvc.perform(put(alertsPath() + "/" + legacyId).contentType(MediaType.APPLICATION_JSON)
                .content(rule.toString()).session(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Legacy rule renamed"));
    }

    /** Validation still speaks JSON: bad trigger type 400s over HTTP too. */
    @Test
    public void http_validationErrors_are400() throws Exception {
        mockMvc.perform(post(alertsPath()).contentType(MediaType.APPLICATION_JSON)
                .content(jsonRule("Bad", "SOMETIMES").toString()).session(session)).andExpect(status().isBadRequest());
        ObjectNode noValue = jsonRule("NoVal", "SPECIFIC_VALUE");
        mockMvc.perform(
                post(alertsPath()).contentType(MediaType.APPLICATION_JSON).content(noValue.toString()).session(session))
                .andExpect(status().isBadRequest());
    }

    /** The contract stays explicit — a truly unknown field is still rejected. */
    @Test
    public void http_trulyUnknownField_isStillRejected() throws Exception {
        ObjectNode bogus = jsonRule("Bogus", "ALL");
        bogus.put("bogusField", "nope");
        mockMvc.perform(
                post(alertsPath()).contentType(MediaType.APPLICATION_JSON).content(bogus.toString()).session(session))
                .andExpect(status().isBadRequest());
    }

    /** DELETE over HTTP removes the rule. */
    @Test
    public void http_delete_removesRule() throws Exception {
        mockMvc.perform(post(alertsPath()).contentType(MediaType.APPLICATION_JSON)
                .content(jsonRule("Doomed", "ALL").toString()).session(session)).andExpect(status().isCreated());
        MvcResult listResult = mockMvc.perform(get(alertsPath()).session(session)).andReturn();
        String id = json.readTree(listResult.getResponse().getContentAsString()).get(0).get("id").asText();

        mockMvc.perform(delete(alertsPath() + "/" + id).session(session)).andExpect(status().isNoContent());
        mockMvc.perform(get(alertsPath()).session(session)).andExpect(jsonPath("$.length()").value(0));
    }
}
