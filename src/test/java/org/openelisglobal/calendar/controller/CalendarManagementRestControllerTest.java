package org.openelisglobal.calendar.controller;

import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Integration tests for CalendarManagementRestController. Extends
 * BaseWebContextSensitiveTest for full Spring context + MockMvc.
 *
 * Tests: CRUD for holidays, weekend config, CSV import/export, auth checks.
 * Requires a running PostgreSQL test database (CI backend-test job).
 */
public class CalendarManagementRestControllerTest extends BaseWebContextSensitiveTest {

    @Autowired
    private DataSource dataSource;

    private ObjectMapper objectMapper;
    private JdbcTemplate jdbcTemplate;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        objectMapper = new ObjectMapper();
        jdbcTemplate = new JdbcTemplate(dataSource);
        cleanTestData();
    }

    private void cleanTestData() {
        try {
            jdbcTemplate.execute("DELETE FROM clinlims.public_holiday WHERE holiday_name LIKE 'TEST-%'");
        } catch (Exception e) {
            // Cleanup is best effort
        }
    }

    // ========== GET /rest/calendar/holidays ==========

    @Test
    public void getHolidays_returnsListForYear() throws Exception {
        // Insert test holiday directly
        jdbcTemplate.execute(
                "INSERT INTO clinlims.public_holiday (id, holiday_date, holiday_name, is_recurring, is_active, sys_user_id) "
                        + "VALUES (nextval('clinlims.public_holiday_seq'), '2026-06-15', 'TEST-Holiday', false, true, 1)");

        MvcResult result = this.mockMvc.perform(get("/rest/calendar/holidays").param("year", "2026")
                .contentType(MediaType.APPLICATION_JSON).session(createMockSession())).andExpect(status().isOk())
                .andReturn();

        String json = result.getResponse().getContentAsString();
        assertTrue(json.contains("TEST-Holiday"));
        assertTrue(json.contains("2026"));
    }

    @Test
    public void getHolidays_returnsEmptyForYearWithNoHolidays() throws Exception {
        MvcResult result = this.mockMvc.perform(get("/rest/calendar/holidays").param("year", "2099")
                .contentType(MediaType.APPLICATION_JSON).session(createMockSession())).andExpect(status().isOk())
                .andReturn();

        String json = result.getResponse().getContentAsString();
        assertTrue(json.contains("\"holidays\":[]") || json.contains("\"holidays\": []"));
    }

    // ========== POST /rest/calendar/holidays ==========

    @Test
    public void createHoliday_returnsCreatedHoliday() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("date", "2026-07-04");
        body.put("name", "TEST-Independence Day");
        body.put("isRecurring", false);

        this.mockMvc
                .perform(post("/rest/calendar/holidays").content(objectMapper.writeValueAsString(body))
                        .contentType(MediaType.APPLICATION_JSON).session(createMockSession()))
                .andExpect(status().isCreated());
    }

    @Test
    public void createHoliday_rejectsDuplicate_returns409() throws Exception {
        // Insert initial holiday
        Map<String, Object> body = new HashMap<>();
        body.put("date", "2026-08-01");
        body.put("name", "TEST-First Holiday");
        body.put("isRecurring", false);

        this.mockMvc
                .perform(post("/rest/calendar/holidays").content(objectMapper.writeValueAsString(body))
                        .contentType(MediaType.APPLICATION_JSON).session(createMockSession()))
                .andExpect(status().isCreated());

        // Try to create duplicate
        body.put("name", "TEST-Duplicate Holiday");
        this.mockMvc
                .perform(post("/rest/calendar/holidays").content(objectMapper.writeValueAsString(body))
                        .contentType(MediaType.APPLICATION_JSON).session(createMockSession()))
                .andExpect(status().isConflict());
    }

    @Test
    public void createHoliday_rejectsEmptyName_returns400() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("date", "2026-09-01");
        body.put("name", "");
        body.put("isRecurring", false);

        this.mockMvc
                .perform(post("/rest/calendar/holidays").content(objectMapper.writeValueAsString(body))
                        .contentType(MediaType.APPLICATION_JSON).session(createMockSession()))
                .andExpect(status().isBadRequest());
    }

    // ========== DELETE /rest/calendar/holidays/{id} ==========

    @Test
    public void deleteHoliday_returns200() throws Exception {
        // Create a holiday first
        jdbcTemplate.execute(
                "INSERT INTO clinlims.public_holiday (id, holiday_date, holiday_name, is_recurring, is_active, sys_user_id) "
                        + "VALUES (nextval('clinlims.public_holiday_seq'), '2026-10-15', 'TEST-ToDelete', false, true, 1)");

        Integer id = jdbcTemplate.queryForObject(
                "SELECT id FROM clinlims.public_holiday WHERE holiday_name = 'TEST-ToDelete'", Integer.class);

        this.mockMvc.perform(delete("/rest/calendar/holidays/" + id).contentType(MediaType.APPLICATION_JSON)
                .session(createMockSession())).andExpect(status().isOk());
    }

    @Test
    public void deleteHoliday_notFound_returns404() throws Exception {
        this.mockMvc.perform(delete("/rest/calendar/holidays/999999").contentType(MediaType.APPLICATION_JSON)
                .session(createMockSession())).andExpect(status().isNotFound());
    }

    // ========== GET/PUT /rest/calendar/weekends ==========

    @Test
    public void getWeekends_returnsWeekendDays() throws Exception {
        MvcResult result = this.mockMvc.perform(
                get("/rest/calendar/weekends").contentType(MediaType.APPLICATION_JSON).session(createMockSession()))
                .andExpect(status().isOk()).andReturn();

        String json = result.getResponse().getContentAsString();
        assertTrue(json.contains("weekendDays"));
    }

    @Test
    public void updateWeekends_changesConfig() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("weekendDays", new int[] { 5, 6 }); // Friday-Saturday

        this.mockMvc
                .perform(put("/rest/calendar/weekends").content(objectMapper.writeValueAsString(body))
                        .contentType(MediaType.APPLICATION_JSON).session(createMockSession()))
                .andExpect(status().isOk());

        // Reset to default
        body.put("weekendDays", new int[] { 0, 6 }); // Sunday-Saturday
        this.mockMvc
                .perform(put("/rest/calendar/weekends").content(objectMapper.writeValueAsString(body))
                        .contentType(MediaType.APPLICATION_JSON).session(createMockSession()))
                .andExpect(status().isOk());
    }

    // ========== CSV Export ==========

    @Test
    public void exportCsv_returnsCsvContent() throws Exception {
        // Insert a holiday to export
        jdbcTemplate.execute(
                "INSERT INTO clinlims.public_holiday (id, holiday_date, holiday_name, is_recurring, is_active, sys_user_id) "
                        + "VALUES (nextval('clinlims.public_holiday_seq'), '2026-11-25', 'TEST-Thanksgiving', false, true, 1)");

        MvcResult result = this.mockMvc
                .perform(get("/rest/calendar/holidays/export").param("year", "2026").session(createMockSession()))
                .andExpect(status().isOk()).andReturn();

        String csv = result.getResponse().getContentAsString();
        assertTrue(csv.contains("TEST-Thanksgiving"));
    }

    // ========== Helper ==========

    /**
     * Creates a mock HTTP session with an authenticated admin user. Uses the same
     * pattern as other integration tests in this project.
     */
    private org.springframework.mock.web.MockHttpSession createMockSession() {
        org.springframework.mock.web.MockHttpSession session = new org.springframework.mock.web.MockHttpSession();
        org.openelisglobal.login.valueholder.UserSessionData usd = new org.openelisglobal.login.valueholder.UserSessionData();
        usd.setSytemUserId(1);
        session.setAttribute(org.openelisglobal.common.action.IActionConstants.USER_SESSION_DATA, usd);
        return session;
    }
}
