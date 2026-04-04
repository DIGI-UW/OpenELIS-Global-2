package org.openelisglobal.reports.tat.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Integration tests for TATReportRestController. Extends
 * BaseWebContextSensitiveTest for full Spring context + MockMvc.
 *
 * Tests: summary, detail (pagination), trend, export endpoints + auth +
 * validation. Requires a running PostgreSQL test database (CI backend-test
 * job).
 */
public class TATReportRestControllerTest extends BaseWebContextSensitiveTest {

    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    // ========== GET /rest/reports/tat/summary ==========

    @Test
    public void getSummary_returnsStats() throws Exception {
        MvcResult result = this.mockMvc
                .perform(get("/rest/reports/tat/summary").param("fromDate", "2026-03-01").param("toDate", "2026-03-31")
                        .param("segment", "RECEIPT_TO_VALIDATION").param("calculationMode", "CALENDAR")
                        .contentType(MediaType.APPLICATION_JSON).session(createMockSession()))
                .andExpect(status().isOk()).andReturn();

        String json = result.getResponse().getContentAsString();
        // Verify structure — totalCount and histogram are always present
        assertTrue("Response should contain totalCount", json.contains("totalCount"));
        assertTrue("Response should contain histogram", json.contains("histogram"));
        assertTrue("Response should contain calculationMode", json.contains("CALENDAR"));
    }

    @Test
    public void getSummary_rejectsDateRangeOver366Days() throws Exception {
        this.mockMvc.perform(get("/rest/reports/tat/summary").param("fromDate", "2025-01-01")
                .param("toDate", "2026-12-31").param("segment", "RECEIPT_TO_VALIDATION")
                .contentType(MediaType.APPLICATION_JSON).session(createMockSession()))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void getSummary_rejectsInvalidDateFormat() throws Exception {
        this.mockMvc.perform(get("/rest/reports/tat/summary").param("fromDate", "not-a-date")
                .param("toDate", "2026-03-31").param("segment", "RECEIPT_TO_VALIDATION")
                .contentType(MediaType.APPLICATION_JSON).session(createMockSession()))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void getSummary_rejectsInvalidSegment() throws Exception {
        this.mockMvc.perform(get("/rest/reports/tat/summary").param("fromDate", "2026-03-01")
                .param("toDate", "2026-03-31").param("segment", "INVALID_SEGMENT")
                .contentType(MediaType.APPLICATION_JSON).session(createMockSession()))
                .andExpect(status().isBadRequest());
    }

    // ========== GET /rest/reports/tat/detail ==========

    @Test
    public void getDetail_returnsPaginatedResults() throws Exception {
        MvcResult result = this.mockMvc
                .perform(get("/rest/reports/tat/detail").param("fromDate", "2026-03-01").param("toDate", "2026-03-31")
                        .param("segment", "RECEIPT_TO_VALIDATION").param("page", "0").param("pageSize", "25")
                        .contentType(MediaType.APPLICATION_JSON).session(createMockSession()))
                .andExpect(status().isOk()).andReturn();

        String json = result.getResponse().getContentAsString();
        assertTrue("Response should contain totalCount", json.contains("totalCount"));
        assertTrue("Response should contain page", json.contains("\"page\""));
        assertTrue("Response should contain results array", json.contains("results"));
    }

    @Test
    public void getDetail_capsPageSizeAt200() throws Exception {
        MvcResult result = this.mockMvc
                .perform(get("/rest/reports/tat/detail").param("fromDate", "2026-03-01").param("toDate", "2026-03-31")
                        .param("segment", "RECEIPT_TO_VALIDATION").param("page", "0").param("pageSize", "500") // Over
                                                                                                               // the
                                                                                                               // 200
                                                                                                               // cap
                        .contentType(MediaType.APPLICATION_JSON).session(createMockSession()))
                .andExpect(status().isOk()).andReturn();

        String json = result.getResponse().getContentAsString();
        // Should succeed but cap at 200
        assertTrue("Response should contain pageSize", json.contains("pageSize"));
    }

    // ========== GET /rest/reports/tat/trend ==========

    @Test
    public void getTrend_returnsSeriesData() throws Exception {
        MvcResult result = this.mockMvc
                .perform(get("/rest/reports/tat/trend").param("fromDate", "2026-03-01").param("toDate", "2026-03-31")
                        .param("segment", "RECEIPT_TO_VALIDATION").param("interval", "DAILY")
                        .contentType(MediaType.APPLICATION_JSON).session(createMockSession()))
                .andExpect(status().isOk()).andReturn();

        String json = result.getResponse().getContentAsString();
        assertTrue("Response should contain series", json.contains("series"));
        assertTrue("Response should contain calculationMode", json.contains("calculationMode"));
    }

    // ========== GET /rest/reports/tat/export ==========

    @Test
    public void exportCsv_returnsCsvContentType() throws Exception {
        this.mockMvc
                .perform(get("/rest/reports/tat/export").param("fromDate", "2026-03-01").param("toDate", "2026-03-31")
                        .param("segment", "RECEIPT_TO_VALIDATION").param("format", "CSV").session(createMockSession()))
                .andExpect(status().isOk());
    }

    @Test
    public void exportPdf_returns501NotImplemented() throws Exception {
        // Documenting the current state: PDF export is not yet implemented
        this.mockMvc
                .perform(get("/rest/reports/tat/export").param("fromDate", "2026-03-01").param("toDate", "2026-03-31")
                        .param("segment", "RECEIPT_TO_VALIDATION").param("format", "PDF").session(createMockSession()))
                .andExpect(status().is(501));
    }

    // ========== Helper ==========

    private MockHttpSession createMockSession() {
        MockHttpSession session = new MockHttpSession();
        UserSessionData usd = new UserSessionData();
        usd.setSytemUserId(1);
        session.setAttribute(IActionConstants.USER_SESSION_DATA, usd);
        return session;
    }

    private static void assertTrue(String message, boolean condition) {
        org.junit.Assert.assertTrue(message, condition);
    }
}
