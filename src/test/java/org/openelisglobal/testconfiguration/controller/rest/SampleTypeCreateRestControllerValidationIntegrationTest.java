package org.openelisglobal.testconfiguration.controller.rest;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import javax.sql.DataSource;
import org.hamcrest.CoreMatchers;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

/**
 * OGC-1234 — {@code POST /rest/SampleTypeCreate} with a form that bean
 * validation refuses (markup in the name, refused by {@code @SafeHtml}) used to
 * answer 200 with the form echoed back, so the Sample Type Editor reported
 * "saved successfully" for a sample type that was never created. It now answers
 * 400 with the field errors and creates nothing.
 *
 * <p>
 * The shared test web context has no Jakarta EL, so its validator is a no-op
 * and {@code @Valid} never fires there (see
 * PanelCreateRestControllerIntegrationTest); these cases run through a
 * standalone MockMvc with an EL-free validator, against the same controller
 * bean and database.
 */
public class SampleTypeCreateRestControllerValidationIntegrationTest extends BaseWebContextSensitiveTest {

    private static final String NAME_PREFIX = "STVIT";

    private static final long RESULTS_ROLE_ID = 95481L;

    private static final long VALIDATION_ROLE_ID = 95482L;

    @Autowired
    private DataSource dataSource;
    @Autowired
    private TypeOfSampleService typeOfSampleService;
    @Autowired
    private SampleTypeCreateRestController sampleTypeCreateRestController;

    private JdbcTemplate jdbc;
    private MockMvc validatingMvc;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        jdbc = new JdbcTemplate(dataSource);
        cleanup();
        ensureRole(RESULTS_ROLE_ID, Constants.ROLE_RESULTS);
        ensureRole(VALIDATION_ROLE_ID, Constants.ROLE_VALIDATION);
        typeOfSampleService.clearCache();

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.setMessageInterpolator(new ParameterMessageInterpolator());
        validator.afterPropertiesSet();
        SampleTypeCreateRestController target = (SampleTypeCreateRestController) AopProxyUtils
                .getSingletonTarget(sampleTypeCreateRestController);
        validatingMvc = MockMvcBuilders.standaloneSetup(target != null ? target : sampleTypeCreateRestController)
                .setValidator(validator).build();
    }

    @After
    public void tearDown() {
        cleanup();
        jdbc.update("DELETE FROM clinlims.system_role WHERE id IN (?, ?)", RESULTS_ROLE_ID, VALIDATION_ROLE_ID);
        typeOfSampleService.clearCache();
    }

    @Test
    public void create_withMarkupInTheName_is400WithTheFieldErrors_andCreatesNothing() throws Exception {
        String name = NAME_PREFIX + "<b>RV</b>0923";
        validatingMvc
                .perform(post("/rest/SampleTypeCreate").contentType(MediaType.APPLICATION_JSON).content(body(name))
                        .session(adminSession()))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("validation"))
                .andExpect(jsonPath("$.fieldErrors[*].field", CoreMatchers.hasItem("sampleTypeEnglishName")));

        assertEquals(Integer.valueOf(0), sampleTypeCount());
    }

    @Test
    public void create_withAPlainName_is200_andCreatesTheSampleType() throws Exception {
        validatingMvc.perform(post("/rest/SampleTypeCreate").contentType(MediaType.APPLICATION_JSON)
                .content(body(NAME_PREFIX + " Plain")).session(adminSession())).andExpect(status().isOk());

        assertEquals(Integer.valueOf(1), sampleTypeCount());
    }

    private String body(String name) {
        String json = name.replace("\"", "\\\"");
        return "{\"sampleTypeEnglishName\":\"" + json + "\",\"sampleTypeFrenchName\":\"" + json
                + "\",\"domain\":\"CLINICAL\",\"active\":true}";
    }

    private Integer sampleTypeCount() {
        return jdbc.queryForObject("SELECT count(*) FROM clinlims.type_of_sample WHERE description LIKE ?",
                Integer.class, NAME_PREFIX + "%");
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
        String pattern = NAME_PREFIX + "%";
        List<Long> localizationIds = jdbc.queryForList(
                "SELECT name_localization_id FROM clinlims.type_of_sample WHERE description LIKE ?", Long.class,
                pattern);
        jdbc.update("DELETE FROM clinlims.system_role_module WHERE system_module_id IN"
                + " (SELECT id FROM clinlims.system_module WHERE name LIKE ?)", "%:" + pattern);
        jdbc.update("DELETE FROM clinlims.system_module WHERE name LIKE ?", "%:" + pattern);
        jdbc.update("DELETE FROM clinlims.type_of_sample WHERE description LIKE ?", pattern);
        for (Long localizationId : localizationIds) {
            if (localizationId != null) {
                jdbc.update("DELETE FROM clinlims.localization_value WHERE localization_id = ?", localizationId);
                jdbc.update("DELETE FROM clinlims.localization WHERE id = ?", localizationId);
            }
        }
    }

    private static MockHttpSession adminSession() {
        UserSessionData sessionData = new UserSessionData();
        sessionData.setSytemUserId(1);
        sessionData.setAdmin(true);
        MockHttpSession httpSession = new MockHttpSession();
        httpSession.setAttribute(IActionConstants.USER_SESSION_DATA, sessionData);
        return httpSession;
    }
}
