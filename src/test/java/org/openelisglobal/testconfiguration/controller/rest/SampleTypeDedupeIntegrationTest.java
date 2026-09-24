package org.openelisglobal.testconfiguration.controller.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
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
 * OGC-1234 (the underlying cause of #4408): a sample type is a duplicate when
 * another one, of any domain, has the same name. The local abbreviation is a
 * lookup key (analyzer import, test variants), so it stays unique per domain,
 * but it is generated to be free rather than cut blindly from the first ten
 * characters of the name: two different names that share a ten-character prefix
 * used to fail the second create with a blank 500.
 */
public class SampleTypeDedupeIntegrationTest extends BaseWebContextSensitiveTest {

    private static final String PREFIX = "STDDIT";

    private static final long RESULTS_ROLE_ID = 95491L;

    private static final long VALIDATION_ROLE_ID = 95492L;

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
        session = adminSession();
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
    public void namesSharingTheirFirstTenCharacters_areAllCreated_withDistinctAbbreviations() throws Exception {
        create(PREFIX + " Serum Venous", "CLINICAL").andExpect(status().isOk());
        create(PREFIX + " Serum Arterial", "CLINICAL").andExpect(status().isOk());
        create(PREFIX + " Serum Capillary", "CLINICAL").andExpect(status().isOk());

        List<String> abbreviations = jdbc.queryForList(
                "SELECT local_abbrev FROM clinlims.type_of_sample WHERE description LIKE ? ORDER BY id", String.class,
                PREFIX + "%");
        assertEquals(List.of("STDDIT Ser", "STDDIT Se2", "STDDIT Se3"), abbreviations);
    }

    @Test
    public void theSameNameInTheSameDomain_inAnyCase_isRefusedAs409_andNotCreatedTwice() throws Exception {
        create(PREFIX + " Twice", "CLINICAL").andExpect(status().isOk());
        create(PREFIX.toLowerCase() + " TWICE ", "CLINICAL").andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("duplicate"))
                .andExpect(jsonPath("$.field").value("sampleTypeEnglishName"));

        assertEquals(Integer.valueOf(1),
                jdbc.queryForObject("SELECT count(*) FROM clinlims.type_of_sample WHERE lower(description) LIKE ?",
                        Integer.class, PREFIX.toLowerCase() + " twice%"));
    }

    /**
     * A new sample type's workplan/results/validation modules are named after it,
     * so the same name in another domain is refused cleanly (it used to fail the
     * module insert with a blank 500).
     */
    @Test
    public void theSameNameInAnotherDomain_isRefusedAs409_notA500() throws Exception {
        create(PREFIX + " Water", "CLINICAL").andExpect(status().isOk());
        create(PREFIX + " Water", "ENVIRONMENTAL").andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("duplicate"));

        assertEquals(Integer.valueOf(1),
                jdbc.queryForObject("SELECT count(*) FROM clinlims.type_of_sample WHERE description = ?", Integer.class,
                        PREFIX + " Water"));
    }

    @Test
    public void anUpdateIsRefusedByField_andWritesNothing() throws Exception {
        create(PREFIX + " Alpha", "CLINICAL").andExpect(status().isOk());
        create(PREFIX + " Beta", "CLINICAL").andExpect(status().isOk());
        Long alphaId = jdbc.queryForObject("SELECT id FROM clinlims.type_of_sample WHERE description = ?", Long.class,
                PREFIX + " Alpha");

        update(alphaId,
                "{\"name\":\"" + PREFIX + " Alpha\",\"description\":\"" + PREFIX + " Beta\",\"domain\":\"CLINICAL\"}")
                .andExpect(status().isConflict()).andExpect(jsonPath("$.field").value("description"));
        update(alphaId, "{\"description\":\"" + PREFIX + " Alpha\",\"abbreviation\":\"ELEVENCHARS\"}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.field").value("abbreviation"));
        update(alphaId, "{\"description\":\"" + PREFIX + " Alpha\",\"whonetCode\":\"SIXCHR\"}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.field").value("whonetCode"));

        assertEquals("the refused saves wrote nothing", PREFIX + " Alpha", jdbc
                .queryForObject("SELECT description FROM clinlims.type_of_sample WHERE id = ?", String.class, alphaId));
        assertNotEquals("ELEVENCHARS", jdbc.queryForObject(
                "SELECT local_abbrev FROM clinlims.type_of_sample WHERE id = ?", String.class, alphaId));

        update(alphaId, "{\"description\":\"" + PREFIX + " Alpha renamed\"}").andExpect(status().isOk());
        assertEquals(PREFIX + " Alpha renamed", jdbc
                .queryForObject("SELECT description FROM clinlims.type_of_sample WHERE id = ?", String.class, alphaId));
    }

    @Test
    public void aBlankAbbreviation_keepsTheStoredOne_evenWhenAnotherTypeHasNone() throws Exception {
        create(PREFIX + " Keep", "CLINICAL").andExpect(status().isOk());
        create(PREFIX + " Blank", "CLINICAL").andExpect(status().isOk());
        jdbc.update("UPDATE clinlims.type_of_sample SET local_abbrev = '' WHERE description = ?", PREFIX + " Blank");
        Long keepId = jdbc.queryForObject("SELECT id FROM clinlims.type_of_sample WHERE description = ?", Long.class,
                PREFIX + " Keep");
        String stored = jdbc.queryForObject("SELECT local_abbrev FROM clinlims.type_of_sample WHERE id = ?",
                String.class, keepId);
        typeOfSampleService.clearCache();

        update(keepId, "{\"description\":\"" + PREFIX + " Keep renamed\",\"abbreviation\":\"\"}")
                .andExpect(status().isOk());

        assertEquals(stored, jdbc.queryForObject("SELECT local_abbrev FROM clinlims.type_of_sample WHERE id = ?",
                String.class, keepId));
    }

    private org.springframework.test.web.servlet.ResultActions create(String name, String domain) throws Exception {
        return mockMvc.perform(post("/rest/SampleTypeCreate")
                .contentType(MediaType.APPLICATION_JSON).content("{\"sampleTypeEnglishName\":\"" + name
                        + "\",\"sampleTypeFrenchName\":\"" + name + "\",\"domain\":\"" + domain + "\",\"active\":true}")
                .session(session));
    }

    private org.springframework.test.web.servlet.ResultActions update(Long id, String body) throws Exception {
        return mockMvc.perform(
                put("/rest/sample-types/" + id).contentType(MediaType.APPLICATION_JSON).content(body).session(session));
    }

    /**
     * The create grants the Results and Validation roles on the new type's modules
     * by role NAME, so both must exist; other fixtures truncate the shared role
     * seed, so they are seeded here when absent.
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
        String pattern = PREFIX + "%";
        List<Long> localizationIds = jdbc.queryForList(
                "SELECT name_localization_id FROM clinlims.type_of_sample WHERE upper(description) LIKE upper(?)",
                Long.class, pattern);
        jdbc.update("DELETE FROM clinlims.system_role_module WHERE system_module_id IN"
                + " (SELECT id FROM clinlims.system_module WHERE upper(name) LIKE upper(?))", "%:" + pattern);
        jdbc.update("DELETE FROM clinlims.system_module WHERE upper(name) LIKE upper(?)", "%:" + pattern);
        jdbc.update("DELETE FROM clinlims.type_of_sample WHERE upper(description) LIKE upper(?)", pattern);
        for (Long localizationId : localizationIds) {
            if (localizationId != null) {
                jdbc.update("DELETE FROM clinlims.localization_value WHERE localization_id = ?", localizationId);
                jdbc.update("DELETE FROM clinlims.localization WHERE id = ?", localizationId);
            }
        }
    }

    private static MockHttpSession adminSession() {
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
