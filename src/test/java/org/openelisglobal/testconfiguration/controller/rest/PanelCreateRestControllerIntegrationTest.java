package org.openelisglobal.testconfiguration.controller.rest;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import javax.sql.DataSource;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.localization.service.LocalizationService;
import org.openelisglobal.localization.service.LocalizationServiceImpl;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.springframework.aop.framework.AopProxyUtils;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

/**
 * OGC-1232 — the legacy Create New Panel write ({@code POST /rest/PanelCreate})
 * carries the panel's domain and answers with the outcome. Before this the form
 * could not express a domain (a request naming one was unreadable, 400) and
 * every failure, validation or insert, came back as 200 with the form echoed,
 * so the screen reported success for panels that did not exist.
 */
public class PanelCreateRestControllerIntegrationTest extends BaseWebContextSensitiveTest {

    private static final String NAME_PREFIX = "PCIT";

    private static final long RESULTS_ROLE_ID = 95461L;

    private static final long VALIDATION_ROLE_ID = 95462L;

    private static final long SAMPLE_TYPE_ID = 95463L;

    @Autowired
    private DataSource dataSource;
    @Autowired
    private LocalizationService localizationService;
    @Autowired
    private TypeOfSampleService typeOfSampleService;
    @Autowired
    private PanelCreateRestController panelCreateRestController;

    private JdbcTemplate jdbc;
    private MockHttpSession session;
    private String sampleTypeId;

    /**
     * The shared test web context has no Jakarta EL, so its validator degrades to a
     * no-op and {@code @Valid @RequestBody} never fires there (see
     * LabelPresetRestControllerValidationTest). The bean-validation cases run
     * through this standalone MockMvc with an EL-free validator instead, against
     * the same controller bean and database.
     */
    private MockMvc validatingMvc;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        jdbc = new JdbcTemplate(dataSource);
        session = buildAdminSession();
        cleanupPanels();
        ensureRole(RESULTS_ROLE_ID, Constants.ROLE_RESULTS);
        ensureRole(VALIDATION_ROLE_ID, Constants.ROLE_VALIDATION);
        sampleTypeId = ensureSampleType();

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.setMessageInterpolator(new ParameterMessageInterpolator());
        validator.afterPropertiesSet();
        PanelCreateRestController target = (PanelCreateRestController) AopProxyUtils
                .getSingletonTarget(panelCreateRestController);
        validatingMvc = MockMvcBuilders.standaloneSetup(target != null ? target : panelCreateRestController)
                .setValidator(validator).build();
    }

    @After
    public void tearDown() {
        cleanupPanels();
        jdbc.update("DELETE FROM clinlims.system_role WHERE id IN (?, ?)", RESULTS_ROLE_ID, VALIDATION_ROLE_ID);
        List<Long> localizationIds = jdbc.queryForList(
                "SELECT name_localization_id FROM clinlims.type_of_sample WHERE id = ?", Long.class, SAMPLE_TYPE_ID);
        jdbc.update("DELETE FROM clinlims.type_of_sample WHERE id = ?", SAMPLE_TYPE_ID);
        deleteLocalizations(localizationIds);
        typeOfSampleService.clearCache();
    }

    @Test
    public void create_withADomain_persistsThatDomain() throws Exception {
        mockMvc.perform(post("/rest/PanelCreate").contentType(MediaType.APPLICATION_JSON)
                .content(body(NAME_PREFIX + " Water", "99991-1", "\"domain\":\"ENVIRONMENTAL\"")).session(session))
                .andExpect(status().isOk()).andExpect(jsonPath("$.createdPanelId").isNotEmpty());

        assertEquals("ENVIRONMENTAL", storedDomain(NAME_PREFIX + " Water"));
        assertEquals(Integer.valueOf(1), panelCount(NAME_PREFIX + " Water"));
    }

    @Test
    public void create_acceptsTheDomainInAnyCase() throws Exception {
        mockMvc.perform(post("/rest/PanelCreate").contentType(MediaType.APPLICATION_JSON)
                .content(body(NAME_PREFIX + " Mosquito", "99992-2", "\"domain\":\"vector\"")).session(session))
                .andExpect(status().isOk());

        assertEquals("VECTOR", storedDomain(NAME_PREFIX + " Mosquito"));
    }

    @Test
    public void create_withoutADomain_isClinical() throws Exception {
        mockMvc.perform(post("/rest/PanelCreate").contentType(MediaType.APPLICATION_JSON)
                .content(body(NAME_PREFIX + " Chem", "99993-3", null)).session(session)).andExpect(status().isOk());

        assertEquals("CLINICAL", storedDomain(NAME_PREFIX + " Chem"));
    }

    @Test
    public void create_withAnUnknownDomain_is400AndCreatesNothing() throws Exception {
        mockMvc.perform(post("/rest/PanelCreate").contentType(MediaType.APPLICATION_JSON)
                .content(body(NAME_PREFIX + " Marine", "99994-4", "\"domain\":\"MARINE\"")).session(session))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$[0].field").value("domain"));

        assertEquals(Integer.valueOf(0), panelCount(NAME_PREFIX + " Marine"));
    }

    @Test
    public void create_withABlankLoinc_is400AndCreatesNothing() throws Exception {
        validatingMvc
                .perform(post("/rest/PanelCreate").contentType(MediaType.APPLICATION_JSON)
                        .content(body(NAME_PREFIX + " NoLoinc", "", null)).session(session))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$[0].field").value("panelLoinc"));

        assertEquals(Integer.valueOf(0), panelCount(NAME_PREFIX + " NoLoinc"));
    }

    @Test
    public void create_withABlankName_is400AndCreatesNothing() throws Exception {
        validatingMvc
                .perform(post("/rest/PanelCreate").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"panelEnglishName\":\"\",\"panelFrenchName\":\"\",\"sampleTypeId\":\""
                                + sampleTypeId + "\",\"panelLoinc\":\"99997-7\"}")
                        .session(session))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$[0].field").value("panelEnglishName"));

        assertEquals(Integer.valueOf(0),
                jdbc.queryForObject("SELECT count(*) FROM clinlims.panel WHERE loinc = '99997-7'", Integer.class));
    }

    @Test
    public void create_ofADuplicateName_is409AndCreatesNoSecondRow() throws Exception {
        mockMvc.perform(post("/rest/PanelCreate").contentType(MediaType.APPLICATION_JSON)
                .content(body(NAME_PREFIX + " Twice", "99995-5", null)).session(session)).andExpect(status().isOk());

        mockMvc.perform(post("/rest/PanelCreate").contentType(MediaType.APPLICATION_JSON)
                .content(body(NAME_PREFIX + " Twice", "99996-6", null)).session(session))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("duplicate"));

        assertEquals(Integer.valueOf(1), panelCount(NAME_PREFIX + " Twice"));
    }

    private String body(String name, String loinc, String extra) {
        return "{\"panelEnglishName\":\"" + name + "\",\"panelFrenchName\":\"" + name + "\",\"sampleTypeId\":\""
                + sampleTypeId + "\",\"panelLoinc\":\"" + loinc + "\"" + (extra == null ? "" : "," + extra) + "}";
    }

    private String storedDomain(String name) {
        return jdbc.queryForObject("SELECT domain FROM clinlims.panel WHERE name = ?", String.class, name);
    }

    private Integer panelCount(String name) {
        return jdbc.queryForObject("SELECT count(*) FROM clinlims.panel WHERE name = ?", Integer.class, name);
    }

    /**
     * The legacy create grants the Results and Validation roles on the panel's
     * modules by role NAME, so both must exist; other fixtures truncate the shared
     * role seed, so they are seeded here when absent rather than assumed.
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

    private String ensureSampleType() {
        List<Long> existing = jdbc.queryForList(
                "SELECT id FROM clinlims.type_of_sample WHERE is_active = true ORDER BY id LIMIT 1", Long.class);
        if (!existing.isEmpty()) {
            return String.valueOf(existing.get(0));
        }
        Localization nameLocalization = LocalizationServiceImpl.createNewLocalization("PCIT sample type",
                "PCIT sample type", LocalizationServiceImpl.LocalizationType.TEST_NAME);
        nameLocalization.setSysUserId("1");
        String localizationId = localizationService.insert(nameLocalization);
        jdbc.update(
                "INSERT INTO clinlims.type_of_sample (id, description, name_localization_id, is_active, lastupdated)"
                        + " VALUES (?, ?, ?, true, NOW())",
                SAMPLE_TYPE_ID, "PCIT sample type", Long.parseLong(localizationId));
        typeOfSampleService.clearCache();
        return String.valueOf(SAMPLE_TYPE_ID);
    }

    private void cleanupPanels() {
        String panelNames = NAME_PREFIX + " %";
        List<Long> localizationIds = jdbc.queryForList(
                "SELECT name_localization_id FROM clinlims.panel WHERE name LIKE ?", Long.class, panelNames);
        jdbc.update("DELETE FROM clinlims.system_role_module WHERE system_module_id IN"
                + " (SELECT id FROM clinlims.system_module WHERE name LIKE ?)", "%:" + panelNames);
        jdbc.update("DELETE FROM clinlims.system_module WHERE name LIKE ?", "%:" + panelNames);
        jdbc.update("DELETE FROM clinlims.sampletype_panel WHERE panel_id IN"
                + " (SELECT id FROM clinlims.panel WHERE name LIKE ?)", panelNames);
        jdbc.update("DELETE FROM clinlims.panel_terminology_mapping WHERE panel_id IN"
                + " (SELECT id FROM clinlims.panel WHERE name LIKE ?)", panelNames);
        jdbc.update("DELETE FROM clinlims.panel WHERE name LIKE ?", panelNames);
        deleteLocalizations(localizationIds);
    }

    private void deleteLocalizations(List<Long> localizationIds) {
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
