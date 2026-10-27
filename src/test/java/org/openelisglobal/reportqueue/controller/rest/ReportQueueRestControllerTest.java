package org.openelisglobal.reportqueue.controller.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.reportqueue.service.ReportQueueServiceImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

/**
 * The queue's REST surface: the paged list, a row's detail, printing and the
 * archived PDF, preferences.
 */
public class ReportQueueRestControllerTest extends BaseWebContextSensitiveTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    private UserSessionData admin;
    private String originalBillingLabel;

    @Before
    public void loadOrder() throws Exception {
        executeDataSetWithStateManagement("testdata/patient-report-v1.xml");
        originalBillingLabel = ConfigurationProperties.getInstance()
                .getPropertyValue(Property.BILLING_REFERENCE_NUMBER_LABEL);
        ConfigurationProperties.getInstance().setPropertyValue(Property.BILLING_REFERENCE_NUMBER_LABEL, "9309");
        jdbcTemplate.update("DELETE FROM clinlims.report_print_event WHERE report_version_id IN"
                + " (SELECT id FROM clinlims.report_version WHERE sample_id = 9301)");
        jdbcTemplate.update("DELETE FROM clinlims.report_version_item WHERE report_version_id IN"
                + " (SELECT id FROM clinlims.report_version WHERE sample_id = 9301)");
        jdbcTemplate.update("DELETE FROM clinlims.report_version WHERE sample_id = 9301");
        ensureSiteInformation(ReportQueueServiceImpl.INSTALL_DATE_SITE_INFO, "2026-01-01 00:00:00");
        jdbcTemplate.update("UPDATE clinlims.site_information SET value = '2026-01-01 00:00:00' WHERE name = ?",
                ReportQueueServiceImpl.INSTALL_DATE_SITE_INFO);
        ensureSystemRole(Constants.ROLE_GLOBAL_ADMIN);
        jdbcTemplate.update("INSERT INTO clinlims.system_user_role (system_user_id, role_id)"
                + " SELECT 9301, id FROM clinlims.system_role WHERE name = ?", Constants.ROLE_GLOBAL_ADMIN);
        admin = new UserSessionData();
        admin.setSytemUserId(9301);
    }

    @After
    public void restoreBillingLabel() {
        ConfigurationProperties.getInstance().setPropertyValue(Property.BILLING_REFERENCE_NUMBER_LABEL,
                originalBillingLabel);
    }

    @After
    public void revokeAdminRole() {
        jdbcTemplate.update("DELETE FROM clinlims.system_user_role WHERE system_user_id = 9301");
    }

    /**
     * Tests here are not transactional (the base class turns transactions off), so
     * every print's history row and version is removed again: a leftover
     * document_track row dated today would make the order count as reported for the
     * report tests that share this fixture.
     */
    @After
    public void removePrintHistory() {
        jdbcTemplate.update("DELETE FROM clinlims.document_track WHERE row_id = 9301"
                + " AND table_id = (SELECT id FROM clinlims.reference_tables WHERE name = 'SAMPLE')");
        jdbcTemplate.update("DELETE FROM clinlims.report_print_event WHERE report_version_id IN"
                + " (SELECT id FROM clinlims.report_version WHERE sample_id = 9301)");
        jdbcTemplate.update("DELETE FROM clinlims.report_version_item WHERE report_version_id IN"
                + " (SELECT id FROM clinlims.report_version WHERE sample_id = 9301)");
        jdbcTemplate.update("DELETE FROM clinlims.report_version WHERE sample_id = 9301");
    }

    @Test
    public void theListAnswersRowsTilesAndPaging() throws Exception {
        MvcResult result = mockMvc
                .perform(get("/rest/reportQueue").param("window", "all").param("pageSize", "100")
                        .sessionAttr(IActionConstants.USER_SESSION_DATA, admin).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(jsonPath("$.pageSize").value(100))
                .andExpect(jsonPath("$.timeWindow").value("all")).andReturn();
        JsonNode page = JSON.readTree(result.getResponse().getContentAsString());
        JsonNode row = rowFor(page, "V1RPT0000009301");
        assertEquals("PARTIAL", row.get("state").asText());
        assertEquals("UNPRINTED", row.get("printStatus").asText());
        assertEquals(2, row.get("releasedCount").asInt());
        assertTrue(page.get("toPrint").asLong() >= 1);
    }

    @Test
    public void printingAnswersThePdfWithItsVersionAndTheArchivedFileIsServedBack() throws Exception {
        MvcResult printed = mockMvc
                .perform(post("/rest/reportQueue/print").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sampleId\":9301}").sessionAttr(IActionConstants.USER_SESSION_DATA, admin))
                .andExpect(status().isOk()).andExpect(header().string("X-Report-Version", "1"))
                .andExpect(header().string("X-Report-Reprint", "false")).andReturn();
        assertEquals(MediaType.APPLICATION_PDF_VALUE, printed.getResponse().getContentType());
        byte[] pdf = printed.getResponse().getContentAsByteArray();
        assertTrue(pdf.length > 1000);

        MvcResult detail = mockMvc
                .perform(get("/rest/reportQueue/orders/9301").sessionAttr(IActionConstants.USER_SESSION_DATA, admin)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(jsonPath("$.labNumber").value("V1RPT0000009301"))
                .andExpect(jsonPath("$.versions[0].version").value(1))
                .andExpect(jsonPath("$.versions[0].archived").value(true)).andReturn();
        long versionId = JSON.readTree(detail.getResponse().getContentAsString()).get("versions").get(0).get("id")
                .asLong();

        MvcResult archived = mockMvc.perform(get("/rest/reportQueue/versions/" + versionId + "/pdf")
                .sessionAttr(IActionConstants.USER_SESSION_DATA, admin)).andExpect(status().isOk()).andReturn();
        assertEquals(pdf.length, archived.getResponse().getContentAsByteArray().length);

        mockMvc.perform(post("/rest/reportQueue/print").contentType(MediaType.APPLICATION_JSON)
                .content("{\"sampleId\":9301}").sessionAttr(IActionConstants.USER_SESSION_DATA, admin))
                .andExpect(status().isOk()).andExpect(header().string("X-Report-Reprint", "true"));
    }

    @Test
    public void anOrderWithNothingReleasedIsRefusedWithAConflict() throws Exception {
        jdbcTemplate.update("UPDATE clinlims.analysis SET status_id = 15 WHERE id IN (9301, 9305)");
        mockMvc.perform(post("/rest/reportQueue/print").contentType(MediaType.APPLICATION_JSON)
                .content("{\"sampleId\":9301}").sessionAttr(IActionConstants.USER_SESSION_DATA, admin))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("nothingReleased"));
        mockMvc.perform(post("/rest/reportQueue/print").contentType(MediaType.APPLICATION_JSON).content("{}")
                .sessionAttr(IActionConstants.USER_SESSION_DATA, admin)).andExpect(status().isBadRequest());
    }

    @Test
    public void anUnknownOrderIsNotFoundOnPrintAndDetail() throws Exception {
        mockMvc.perform(post("/rest/reportQueue/print").contentType(MediaType.APPLICATION_JSON)
                .content("{\"sampleId\":999999999}").sessionAttr(IActionConstants.USER_SESSION_DATA, admin))
                .andExpect(status().isNotFound());
        mockMvc.perform(
                get("/rest/reportQueue/orders/999999999").sessionAttr(IActionConstants.USER_SESSION_DATA, admin))
                .andExpect(status().isNotFound());
    }

    /**
     * FR-3.3: a range whose To comes before its From is refused, not answered with
     * an empty list.
     */
    @Test
    public void aReversedLabNumberRangeIsRefused() throws Exception {
        mockMvc.perform(get("/rest/reportQueue").param("labNo", "V1RPT0000009305").param("labNoTo", "V1RPT0000009301")
                .sessionAttr(IActionConstants.USER_SESSION_DATA, admin)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("labRangeReversed"));
        mockMvc.perform(get("/rest/reportQueue").param("labNo", "V1RPT0000009301").param("labNoTo", "V1RPT0000009305")
                .sessionAttr(IActionConstants.USER_SESSION_DATA, admin)).andExpect(status().isOk())
                .andExpect(jsonPath("$.rows[0].labNumber").value("V1RPT0000009301"));
    }

    /**
     * BR-9: a user whose lab units do not cover the order gets 404 on its detail,
     * print and archived file.
     */
    @Test
    public void anOrderOutsideTheUsersLabUnitsIsNotFoundOnDetailPrintAndPdf() throws Exception {
        MvcResult detail = mockMvc
                .perform(get("/rest/reportQueue/orders/9301").sessionAttr(IActionConstants.USER_SESSION_DATA, admin))
                .andExpect(status().isOk()).andReturn();
        mockMvc.perform(post("/rest/reportQueue/print").contentType(MediaType.APPLICATION_JSON)
                .content("{\"sampleId\":9301}").sessionAttr(IActionConstants.USER_SESSION_DATA, admin))
                .andExpect(status().isOk());
        detail = mockMvc
                .perform(get("/rest/reportQueue/orders/9301").sessionAttr(IActionConstants.USER_SESSION_DATA, admin))
                .andExpect(status().isOk()).andReturn();
        long versionId = JSON.readTree(detail.getResponse().getContentAsString()).get("versions").get(0).get("id")
                .asLong();

        UserSessionData withoutLabUnits = new UserSessionData();
        withoutLabUnits.setSytemUserId(9302);
        mockMvc.perform(
                get("/rest/reportQueue/orders/9301").sessionAttr(IActionConstants.USER_SESSION_DATA, withoutLabUnits))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/rest/reportQueue/print").contentType(MediaType.APPLICATION_JSON)
                .content("{\"sampleId\":9301}").sessionAttr(IActionConstants.USER_SESSION_DATA, withoutLabUnits))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/rest/reportQueue/versions/" + versionId + "/pdf")
                .sessionAttr(IActionConstants.USER_SESSION_DATA, withoutLabUnits)).andExpect(status().isNotFound());
    }

    @Test
    public void aPatientIdThatIsNotANumberIsRefused() throws Exception {
        mockMvc.perform(get("/rest/reportQueue").param("patientId", "abc")
                .sessionAttr(IActionConstants.USER_SESSION_DATA, admin)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("patientId must be a number"));
        mockMvc.perform(get("/rest/reportQueue").param("patientId", "9301")
                .sessionAttr(IActionConstants.USER_SESSION_DATA, admin)).andExpect(status().isOk());
    }

    @Test
    public void preferencesRoundTrip() throws Exception {
        mockMvc.perform(put("/rest/reportQueue/preferences").contentType(MediaType.APPLICATION_JSON)
                .content("{\"timeWindow\":\"30d\",\"pageSize\":50}")
                .sessionAttr(IActionConstants.USER_SESSION_DATA, admin)).andExpect(status().isOk())
                .andExpect(jsonPath("$.timeWindow").value("30d")).andExpect(jsonPath("$.pageSize").value(50));
        mockMvc.perform(get("/rest/reportQueue/preferences").sessionAttr(IActionConstants.USER_SESSION_DATA, admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.timeWindow").value("30d"));
    }

    private static JsonNode rowFor(JsonNode page, String labNumber) {
        for (JsonNode row : page.get("rows")) {
            if (labNumber.equals(row.get("labNumber").asText())) {
                return row;
            }
        }
        throw new AssertionError("no row for " + labNumber + " in " + page);
    }
}
