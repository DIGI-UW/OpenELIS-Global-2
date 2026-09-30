package org.openelisglobal.testconfiguration.controller.rest;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import javax.sql.DataSource;
import org.hamcrest.CoreMatchers;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

/**
 * OGC-1234: the legacy test-management endpoints answered 200 with the form
 * when they refused a save (invalid input, or a failure the service threw), so
 * every one of those screens reported success. A refusal for invalid input is
 * now a 400 naming the fields, a collision with an existing record a 409, and
 * nothing is written.
 *
 * <p>
 * The shared test web context has no Jakarta EL, so {@code @Valid} never fires
 * there; the controllers run through a standalone MockMvc with an EL-free
 * validator, against the same beans and database.
 */
public class LegacyTestConfigurationRefusalIntegrationTest extends BaseWebContextSensitiveTest {

    private static final String UOM_PREFIX = "LTCRIT";

    @Autowired
    private DataSource dataSource;
    @Autowired
    private UomCreateRestController uomCreateRestController;
    @Autowired
    private MethodRenameEntryRestController methodRenameEntryRestController;
    @Autowired
    private TestSectionRenameEntryRestController testSectionRenameEntryRestController;
    @Autowired
    private TestSectionOrderRestController testSectionOrderRestController;

    private JdbcTemplate jdbc;
    private MockMvc validatingMvc;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        jdbc = new JdbcTemplate(dataSource);
        cleanup();
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.setMessageInterpolator(new ParameterMessageInterpolator());
        validator.afterPropertiesSet();
        validatingMvc = MockMvcBuilders
                .standaloneSetup(target(uomCreateRestController), target(methodRenameEntryRestController),
                        target(testSectionRenameEntryRestController), target(testSectionOrderRestController))
                .setValidator(validator).build();
    }

    @After
    public void tearDown() {
        cleanup();
    }

    @Test
    public void uomCreate_withMarkupInTheName_is400WithTheField_andCreatesNothing() throws Exception {
        postJson("/rest/UomCreate", "{\"uomEnglishName\":\"" + UOM_PREFIX + "<b>x</b>\"}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("validation"))
                .andExpect(jsonPath("$.fieldErrors[*].field", CoreMatchers.hasItem("uomEnglishName")));

        assertEquals(Integer.valueOf(0), uomCount());
    }

    @Test
    public void uomCreate_isCreatedOnce_andTheSameNameAgainIs409() throws Exception {
        postJson("/rest/UomCreate", "{\"uomEnglishName\":\"" + UOM_PREFIX + "mL\"}").andExpect(status().isOk());
        postJson("/rest/UomCreate", "{\"uomEnglishName\":\"" + UOM_PREFIX + "mL\"}").andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("duplicate"));

        assertEquals(Integer.valueOf(1), uomCount());
    }

    @Test
    public void methodRename_withABlankEnglishName_is400WithTheField() throws Exception {
        postJson("/rest/MethodRenameEntry", "{\"methodId\":\"1\",\"nameEnglish\":\"\",\"nameFrench\":\"Methode\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[*].field", CoreMatchers.hasItem("nameEnglish")));
    }

    @Test
    public void testSectionRename_withMarkupInTheName_is400WithTheField() throws Exception {
        postJson("/rest/TestSectionRenameEntry",
                "{\"testSectionId\":\"1\",\"nameEnglish\":\"<script>x</script>\",\"nameFrench\":\"Unite\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[*].field", CoreMatchers.hasItem("nameEnglish")));
    }

    @Test
    public void testSectionOrder_withNoChangeList_is400() throws Exception {
        postJson("/rest/TestSectionOrder", "{\"jsonChangeList\":\"\"}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation"));
    }

    private ResultActions postJson(String url, String body) throws Exception {
        return validatingMvc
                .perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body).session(adminSession()));
    }

    private Integer uomCount() {
        return jdbc.queryForObject("SELECT count(*) FROM clinlims.unit_of_measure WHERE name LIKE ?", Integer.class,
                UOM_PREFIX + "%");
    }

    private void cleanup() {
        jdbc.update("DELETE FROM clinlims.unit_of_measure WHERE name LIKE ?", UOM_PREFIX + "%");
    }

    private static Object target(Object bean) {
        Object target = AopProxyUtils.getSingletonTarget(bean);
        return target != null ? target : bean;
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
