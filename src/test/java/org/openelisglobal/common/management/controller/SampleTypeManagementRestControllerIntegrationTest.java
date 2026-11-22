package org.openelisglobal.common.management.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;

/**
 * OGC-296 — Sample Type Management REST, exercised over HTTP/JSON (MockMvc).
 * Regression focus: updating a sample type to a name/description another type
 * already uses is a client-correctable conflict — it used to surface as a blank
 * 500 (uncaught LIMSDuplicateRecordException from
 * TypeOfSampleServiceImpl.save).
 */
public class SampleTypeManagementRestControllerIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private javax.sql.DataSource dataSource;

    @Autowired
    private org.openelisglobal.typeofsample.service.TypeOfSampleService typeOfSampleService;

    private JdbcTemplate jdbc;
    private MockHttpSession session;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        jdbc = new JdbcTemplate(dataSource);
        cleanup();
        jdbc.update("INSERT INTO clinlims.localization (id, description, lastupdated) VALUES (95601, 'type of"
                + " sample name', NOW())");
        jdbc.update("INSERT INTO clinlims.localization (id, description, lastupdated) VALUES (95602, 'type of"
                + " sample name', NOW())");
        jdbc.update("INSERT INTO clinlims.localization_value (id, localization_id, locale, value) VALUES (956011,"
                + " 95601, 'en', 'STMgmtIT Alpha')");
        jdbc.update("INSERT INTO clinlims.localization_value (id, localization_id, locale, value) VALUES (956021,"
                + " 95602, 'en', 'STMgmtIT Beta')");
        jdbc.update("INSERT INTO clinlims.type_of_sample (id, description, domain, is_active, sort_order,"
                + " name_localization_id, lastupdated) VALUES (95601, 'STMgmtIT Alpha', 'H', true, 901, 95601,"
                + " NOW())");
        jdbc.update("INSERT INTO clinlims.type_of_sample (id, description, domain, is_active, sort_order,"
                + " name_localization_id, lastupdated) VALUES (95602, 'STMgmtIT Beta', 'H', true, 902, 95602,"
                + " NOW())");
        typeOfSampleService.clearCache();
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
        jdbc.update("DELETE FROM clinlims.sampletype_test WHERE test_id = 95603");
        jdbc.update("DELETE FROM clinlims.test WHERE id = 95603");
        jdbc.update("DELETE FROM clinlims.type_of_sample WHERE id IN (95601, 95602)");
        jdbc.update("DELETE FROM clinlims.localization_value WHERE localization_id IN (95601, 95602)");
        jdbc.update("DELETE FROM clinlims.localization WHERE id IN (95601, 95602)");
    }

    @Test
    public void update_editableFields_persistAndReload() throws Exception {
        mockMvc.perform(put("/rest/sample-types/95601").contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":\"95601\",\"name\":\"STMgmtIT Alpha\",\"description\":\"STMgmtIT Alpha v2\","
                        + "\"domain\":\"CLINICAL\",\"abbreviation\":\"STA\",\"isActive\":true,\"sortOrder\":905}")
                .session(session)).andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.description").value("STMgmtIT Alpha v2"))
                .andExpect(jsonPath("$.data.abbreviation").value("STA"));

        mockMvc.perform(get("/rest/sample-types/95601").session(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.description").value("STMgmtIT Alpha v2"))
                .andExpect(jsonPath("$.data.sortOrder").value(901));
    }

    /** Regression: duplicate description is a 409 conflict, not a blank 500. */
    @Test
    public void update_duplicateDescription_is409NotServerError() throws Exception {
        mockMvc.perform(put("/rest/sample-types/95602").contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":\"95602\",\"name\":\"STMgmtIT Beta\",\"description\":\"STMgmtIT Alpha\","
                        + "\"domain\":\"CLINICAL\",\"abbreviation\":\"\",\"isActive\":true,\"sortOrder\":902}")
                .session(session)).andExpect(status().isConflict()).andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").exists());
    }

    /**
     * OGC-1463: a save from a page loaded before another admin deactivated the
     * type.
     */
    @Test
    public void update_fromAnOldPageIsRefusedAsStale_andTheTypeStaysInactive() throws Exception {
        String loaded = com.jayway.jsonpath.JsonPath
                .read(mockMvc.perform(get("/rest/sample-types/95601").session(session)).andReturn().getResponse()
                        .getContentAsString(), "$.data.lastUpdated");
        mockMvc.perform(put("/rest/sample-types/95601").contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":\"95601\",\"name\":\"STMgmtIT Alpha\",\"description\":\"STMgmtIT Alpha\","
                        + "\"domain\":\"CLINICAL\",\"abbreviation\":\"\",\"isActive\":false,\"sortOrder\":901}")
                .session(session)).andExpect(status().isOk());

        mockMvc.perform(put("/rest/sample-types/95601").contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":\"95601\",\"name\":\"STMgmtIT Alpha A\",\"description\":\"STMgmtIT Alpha\","
                        + "\"domain\":\"CLINICAL\",\"abbreviation\":\"\",\"isActive\":true,\"sortOrder\":901,"
                        + "\"lastUpdated\":\"" + loaded + "\"}")
                .session(session)).andExpect(status().isConflict()).andExpect(jsonPath("$.field").value("stale"));

        mockMvc.perform(get("/rest/sample-types/95601").session(session))
                .andExpect(jsonPath("$.data.isActive").value(false));
    }

    /**
     * A type with no abbreviation could not be saved at all (500 in the duplicate
     * check).
     */
    @Test
    public void update_aTypeWithoutAnAbbreviation_saves() throws Exception {
        mockMvc.perform(put("/rest/sample-types/95601").contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":\"95601\",\"name\":\"STMgmtIT Alpha\",\"description\":\"STMgmtIT Alpha v3\","
                        + "\"domain\":\"CLINICAL\",\"abbreviation\":\"\",\"isActive\":true,\"sortOrder\":901}")
                .session(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.description").value("STMgmtIT Alpha v3"));
    }

    /**
     * OGC-1465: the position in lists belongs to Display Order, so a Basic Info
     * save from a page loaded before a move cannot undo it.
     */
    @Test
    public void update_leavesThePositionToDisplayOrder() throws Exception {
        mockMvc.perform(put("/rest/sample-types/95601").contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":\"95601\",\"name\":\"STMgmtIT Alpha\",\"description\":\"STMgmtIT Alpha\","
                        + "\"domain\":\"CLINICAL\",\"abbreviation\":\"\",\"isActive\":true,\"sortOrder\":3}")
                .session(session)).andExpect(status().isOk());

        mockMvc.perform(get("/rest/sample-types/95601").session(session))
                .andExpect(jsonPath("$.data.sortOrder").value(901));
    }

    /**
     * OGC-1467: a description over the column's 40 characters is a field error, and
     * the answer carries no database message.
     */
    @Test
    public void update_aDescriptionOver40Characters_is400OnTheField() throws Exception {
        String body = mockMvc
                .perform(put("/rest/sample-types/95601").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"95601\",\"name\":\"STMgmtIT Alpha\",\"description\":\"" + "d".repeat(41)
                                + "\",\"domain\":\"CLINICAL\",\"isActive\":true}")
                        .session(session))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.field").value("description")).andReturn()
                .getResponse().getContentAsString();
        org.junit.Assert.assertFalse(body, body.matches("(?is).*(value too long|varying|SQL).*"));
        org.junit.Assert.assertEquals("STMgmtIT Alpha",
                jdbc.queryForObject("SELECT description FROM clinlims.type_of_sample WHERE id = 95601", String.class));
    }

    /** OGC-1465: an unknown domain is refused instead of becoming CLINICAL. */
    @Test
    public void update_anUnknownDomainIsRefused() throws Exception {
        mockMvc.perform(put("/rest/sample-types/95601").contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":\"95601\",\"name\":\"STMgmtIT Alpha\",\"description\":\"STMgmtIT Alpha\","
                        + "\"domain\":\"XYZ\",\"isActive\":true}")
                .session(session)).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.field").value("domain"));
    }

    /**
     * OGC-1465: the Sample Type side applies the domain rule that test creation
     * does.
     */
    @Test
    public void linkingAVectorTestToAClinicalSampleTypeIsRefused() throws Exception {
        jdbc.update(
                "INSERT INTO clinlims.test (id, name, description, is_active, guid, domain, lastupdated)"
                        + " VALUES (95603, 'STMgmtIT vector', 'STMgmtIT vector', 'N', ?, 'VECTOR', NOW())",
                java.util.UUID.randomUUID().toString());

        mockMvc.perform(put("/rest/sample-types/95601/tests/95603").session(session))
                .andExpect(status().isUnprocessableEntity());
        org.junit.Assert.assertEquals(Integer.valueOf(0), jdbc
                .queryForObject("SELECT count(*) FROM clinlims.sampletype_test WHERE test_id = 95603", Integer.class));
    }
}
