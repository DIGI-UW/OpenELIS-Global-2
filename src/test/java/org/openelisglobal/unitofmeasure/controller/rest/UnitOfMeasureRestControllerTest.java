package org.openelisglobal.unitofmeasure.controller.rest;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import javax.sql.DataSource;
import org.hamcrest.CoreMatchers;
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

/**
 * The Units of Measure page lists units and adds or edits one through
 * /rest/uom. A blank name, an unknown unit and a name another unit already has
 * are refused (400, 404, 409) and nothing is written; the test editor's inline
 * create refuses a taken name the same way instead of failing with a 500.
 */
public class UnitOfMeasureRestControllerTest extends BaseWebContextSensitiveTest {

    private static final String PREFIX = "UOMRC";

    @Autowired
    private DataSource dataSource;
    @Autowired
    private UnitOfMeasureRestController unitOfMeasureRestController;

    private JdbcTemplate jdbc;
    private MockMvc mvc;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        jdbc = new JdbcTemplate(dataSource);
        cleanup();
        Object target = AopProxyUtils.getSingletonTarget(unitOfMeasureRestController);
        mvc = MockMvcBuilders.standaloneSetup(target != null ? target : unitOfMeasureRestController).build();
    }

    @After
    public void tearDown() {
        cleanup();
    }

    @Test
    public void createsAUnitOnce_andRefusesTheSameNameAgain() throws Exception {
        send(post("/rest/uom"), body(PREFIX + "g/L", "GL", "g/L")).andExpect(status().isCreated())
                .andExpect(jsonPath("$.value").value(PREFIX + "g/L")).andExpect(jsonPath("$.code").value("GL"));
        send(post("/rest/uom"), body(PREFIX + "g/L", "", "")).andExpect(status().isConflict());

        assertEquals(Integer.valueOf(1), count(PREFIX + "g/L"));
    }

    @Test
    public void listsUnitsWithTheirCodes() throws Exception {
        send(post("/rest/uom"), body(PREFIX + "IU/L", "IUL", "[IU]/L")).andExpect(status().isCreated());

        mvc.perform(get("/rest/uom").session(adminSession())).andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.value == '" + PREFIX + "IU/L')].ucumCode", CoreMatchers.hasItem("[IU]/L")));
    }

    @Test
    public void editsAUnit_andRefusesABlankName_aTakenName_andAnUnknownUnit() throws Exception {
        send(post("/rest/uom"), body(PREFIX + "mg", "", "")).andExpect(status().isCreated());
        send(post("/rest/uom"), body(PREFIX + "ug", "", "")).andExpect(status().isCreated());
        String id = idOf(PREFIX + "mg");

        send(put("/rest/uom/" + id), body(PREFIX + "ug", "", "")).andExpect(status().isConflict());
        send(put("/rest/uom/" + id), body(" ", "", "")).andExpect(status().isBadRequest());
        send(put("/rest/uom/99999999"), body(PREFIX + "x", "", "")).andExpect(status().isNotFound());
        assertEquals(Integer.valueOf(1), count(PREFIX + "mg"));

        send(put("/rest/uom/" + id), body(PREFIX + "mg/dL", "MGDL", "mg/dL")).andExpect(status().isOk())
                .andExpect(jsonPath("$.value").value(PREFIX + "mg/dL"));
        assertEquals(PREFIX + "mg/dL", jdbc.queryForObject(
                "SELECT name FROM clinlims.unit_of_measure WHERE id = CAST(? AS numeric)", String.class, id));
        assertEquals("mg/dL", jdbc.queryForObject(
                "SELECT ucum_code FROM clinlims.unit_of_measure WHERE id = CAST(? AS numeric)", String.class, id));
    }

    private ResultActions send(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request,
            String json) throws Exception {
        return mvc.perform(request.contentType(MediaType.APPLICATION_JSON).content(json).session(adminSession()));
    }

    private static String body(String name, String code, String ucumCode) {
        return "{\"name\":\"" + name + "\",\"code\":\"" + code + "\",\"ucumCode\":\"" + ucumCode + "\"}";
    }

    private Integer count(String name) {
        return jdbc.queryForObject("SELECT count(*) FROM clinlims.unit_of_measure WHERE name = ?", Integer.class, name);
    }

    private String idOf(String name) {
        return jdbc.queryForObject("SELECT CAST(id AS varchar) FROM clinlims.unit_of_measure WHERE name = ?",
                String.class, name);
    }

    private void cleanup() {
        jdbc.update("DELETE FROM clinlims.unit_of_measure WHERE name LIKE ?", PREFIX + "%");
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
