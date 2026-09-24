package org.openelisglobal.testconfiguration.controller.rest;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

/**
 * Regression: /rest/SampleTypeCreate used to 500 when the auto-derived
 * abbreviation (first 10 chars of the name) collided with an existing sample
 * type's abbreviation. The insert path throws LIMSDuplicateRecordException on
 * either a name-in-domain OR abbrev-in-domain match, and the raw exception used
 * to bubble out as HTTP 500, leaving admins with an opaque banner. Now the
 * controller catches it, looks up which field collided, and answers 409 with a
 * body the UI can render as an inline validation message.
 */
public class SampleTypeCreateRestControllerDuplicateIntegrationTest extends BaseWebContextSensitiveTest {

    private static final String NAME_PREFIX = "STCDIT_";

    private static final long RESULTS_ROLE_ID = 96461L;
    private static final long VALIDATION_ROLE_ID = 96462L;

    @Autowired
    private DataSource dataSource;
    @Autowired
    private TypeOfSampleService typeOfSampleService;

    private JdbcTemplate jdbc;
    private MockHttpSession session;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        jdbc = new JdbcTemplate(dataSource);
        session = buildAdminSession();
        cleanup();
        ensureRole(RESULTS_ROLE_ID, Constants.ROLE_RESULTS);
        ensureRole(VALIDATION_ROLE_ID, Constants.ROLE_VALIDATION);
        typeOfSampleService.clearCache();
    }

    @After
    public void tearDown() {
        cleanup();
        jdbc.update("DELETE FROM clinlims.system_role WHERE id IN (?, ?)", RESULTS_ROLE_ID, VALIDATION_ROLE_ID);
        typeOfSampleService.clearCache();
    }

    @Test
    public void create_secondNameSharingTheAbbreviationPrefix_is409_notServerError() throws Exception {
        String first = NAME_PREFIX + "OGC1152_20260804";
        String colliding = NAME_PREFIX + "OGC1152_RETEST2";

        mockMvc.perform(post("/rest/SampleTypeCreate").contentType(MediaType.APPLICATION_JSON)
                .content(body(first, first)).session(session)).andExpect(status().isOk());

        // Both names share the same 10-char prefix ("STCDIT_OGC"), so the second
        // insert used to 500 on the abbreviation-in-domain duplicate check.
        mockMvc.perform(post("/rest/SampleTypeCreate").contentType(MediaType.APPLICATION_JSON)
                .content(body(colliding, colliding)).session(session)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("duplicate")).andExpect(jsonPath("$.field").value("name"))
                .andExpect(
                        jsonPath("$.message").value(org.hamcrest.CoreMatchers.containsString("first 10 characters")));

        assertEquals("the second create is refused, not silently persisted", Integer.valueOf(1),
                jdbc.queryForObject("SELECT count(*) FROM clinlims.type_of_sample WHERE description LIKE ?",
                        Integer.class, NAME_PREFIX + "%"));
    }

    @Test
    public void create_sameNameTwice_is409_withNameSpecificMessage() throws Exception {
        String name = NAME_PREFIX + "Twice";

        mockMvc.perform(post("/rest/SampleTypeCreate").contentType(MediaType.APPLICATION_JSON).content(body(name, name))
                .session(session)).andExpect(status().isOk());

        mockMvc.perform(post("/rest/SampleTypeCreate").contentType(MediaType.APPLICATION_JSON).content(body(name, name))
                .session(session)).andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("duplicate"))
                .andExpect(jsonPath("$.field").value("name"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.CoreMatchers.containsString("already exists")));
    }

    @Test
    public void create_distinctPrefix_stillSucceeds() throws Exception {
        String first = NAME_PREFIX + "OGC1152_20260804";
        String distinct = "Z" + NAME_PREFIX + "DIFFPREFIX";

        mockMvc.perform(post("/rest/SampleTypeCreate").contentType(MediaType.APPLICATION_JSON)
                .content(body(first, first)).session(session)).andExpect(status().isOk());

        mockMvc.perform(post("/rest/SampleTypeCreate").contentType(MediaType.APPLICATION_JSON)
                .content(body(distinct, distinct)).session(session)).andExpect(status().isOk());

        assertEquals("both distinct-prefix rows persist", Integer.valueOf(2), jdbc.queryForObject(
                "SELECT count(*) FROM clinlims.type_of_sample WHERE description LIKE ? OR" + " description LIKE ?",
                Integer.class, NAME_PREFIX + "%", "Z" + NAME_PREFIX + "%"));
    }

    private String body(String english, String french) {
        return "{\"sampleTypeEnglishName\":\"" + english + "\",\"sampleTypeFrenchName\":\"" + french
                + "\",\"domain\":\"CLINICAL\",\"active\":true}";
    }

    /**
     * The legacy create grants Results and Validation roles on the new type's
     * modules by role NAME, so both must exist for the insert to complete. Other
     * fixtures truncate the shared role seed, so they are seeded here when absent.
     */
    private void ensureRole(long id, String name) {
        Integer present = jdbc.queryForObject("SELECT count(*) FROM clinlims.system_role WHERE trim(name) = ?",
                Integer.class, name);
        if (present == 0) {
            jdbc.update(
                    "INSERT INTO clinlims.system_role (id, name, description, is_grouping_role, display_key,"
                            + " active, editable) VALUES (?, ?, ?, false, ?, true, false)",
                    id, name, name + " role", "role." + name.toLowerCase());
        }
    }

    private void cleanup() {
        String pattern = NAME_PREFIX + "%";
        String distinctPattern = "Z" + NAME_PREFIX + "%";
        java.util.List<Long> localizationIds = jdbc.queryForList(
                "SELECT name_localization_id FROM clinlims.type_of_sample WHERE description LIKE ? OR description LIKE ?",
                Long.class, pattern, distinctPattern);
        jdbc.update(
                "DELETE FROM clinlims.system_role_module WHERE system_module_id IN"
                        + " (SELECT id FROM clinlims.system_module WHERE name LIKE ? OR name LIKE ?)",
                "%:" + pattern, "%:" + distinctPattern);
        jdbc.update("DELETE FROM clinlims.system_module WHERE name LIKE ? OR name LIKE ?", "%:" + pattern,
                "%:" + distinctPattern);
        jdbc.update("DELETE FROM clinlims.type_of_sample WHERE description LIKE ? OR description LIKE ?", pattern,
                distinctPattern);
        for (Long localizationId : localizationIds) {
            if (localizationId != null) {
                jdbc.update("DELETE FROM clinlims.localization_value WHERE localization_id = ?", localizationId);
                jdbc.update("DELETE FROM clinlims.localization WHERE id = ?", localizationId);
            }
        }
    }

    private static MockHttpSession buildAdminSession() {
        UserDetails userDetails = User.withUsername("admin").password("N/A").authorities("ROLE_ADMIN").build();
        SecurityContext securityContext = new SecurityContextImpl();
        securityContext.setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, "N/A", userDetails.getAuthorities()));

        UserSessionData sessionData = new UserSessionData();
        sessionData.setSytemUserId(1);
        sessionData.setAdmin(true);

        MockHttpSession httpSession = new MockHttpSession();
        httpSession.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);
        httpSession.setAttribute(IActionConstants.USER_SESSION_DATA, sessionData);
        return httpSession;
    }
}
