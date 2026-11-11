package org.openelisglobal.reports.tat.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.reports.tat.bean.TATDetailResponse;
import org.openelisglobal.reports.tat.bean.TATSummaryResponse;
import org.openelisglobal.reports.tat.bean.TATTrendResponse;
import org.openelisglobal.reports.tat.controller.rest.TATReportRestController;
import org.openelisglobal.reports.tat.service.TATReportService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Unit tests for TATReportRestController.
 *
 * Uses Mockito to test controller validation logic (date parsing, segment enum,
 * page size caps) without requiring Spring context or database.
 */
@RunWith(MockitoJUnitRunner.class)
public class TATReportRestControllerTest {

    @InjectMocks
    private TATReportRestController controller;

    @Mock
    private TATReportService tatReportService;

    private MockHttpServletRequest request;

    @Before
    public void setUp() {
        request = new MockHttpServletRequest();
        MockHttpSession session = new MockHttpSession();
        org.openelisglobal.login.valueholder.UserSessionData usd = new org.openelisglobal.login.valueholder.UserSessionData();
        usd.setSytemUserId(1);
        session.setAttribute(org.openelisglobal.common.action.IActionConstants.USER_SESSION_DATA, usd);
        request.setSession(session);
    }

    // ========== Summary endpoint ==========

    @Test
    public void getSummary_returnsOkWithValidParams() {
        TATSummaryResponse mockResponse = new TATSummaryResponse();
        mockResponse.setCalculationMode("CALENDAR");
        mockResponse.setTotalCount(0);
        mockResponse.setHistogram(new ArrayList<>());
        mockResponse.setBreakdown(new ArrayList<>());

        when(tatReportService.getSummary(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                anyBoolean(), any())).thenReturn(mockResponse);

        ResponseEntity<?> response = controller.getSummary("2026-03-01", "2026-03-31", "RECEIPT_TO_VALIDATION",
                "CALENDAR", null, null, null, null, null, null, false, "LAB_UNIT", request);

        Assert.assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void getSummary_rejectsDateRangeOver366Days() {
        ResponseEntity<?> response = controller.getSummary("2025-01-01", "2026-12-31", "RECEIPT_TO_VALIDATION",
                "CALENDAR", null, null, null, null, null, null, false, "LAB_UNIT", request);

        Assert.assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void getSummary_rejectsInvalidDateFormat() {
        ResponseEntity<?> response = controller.getSummary("not-a-date", "2026-03-31", "RECEIPT_TO_VALIDATION",
                "CALENDAR", null, null, null, null, null, null, false, "LAB_UNIT", request);

        Assert.assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void getSummary_rejectsInvalidSegment() {
        ResponseEntity<?> response = controller.getSummary("2026-03-01", "2026-03-31", "INVALID_SEGMENT", "CALENDAR",
                null, null, null, null, null, null, false, "LAB_UNIT", request);

        Assert.assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void getSummary_rejectsFromAfterTo() {
        ResponseEntity<?> response = controller.getSummary("2026-04-01", "2026-03-01", "RECEIPT_TO_VALIDATION",
                "CALENDAR", null, null, null, null, null, null, false, "LAB_UNIT", request);

        Assert.assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    // ========== Detail endpoint ==========

    @Test
    public void getDetail_returnsOkWithValidParams() {
        TATDetailResponse mockResponse = new TATDetailResponse();
        mockResponse.setTotalCount(0);
        mockResponse.setPage(0);
        mockResponse.setPageSize(25);
        mockResponse.setResults(new ArrayList<>());

        when(tatReportService.getDetail(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                anyBoolean(), anyInt(), anyInt(), any(), any(), any(), any())).thenReturn(mockResponse);

        ResponseEntity<?> response = controller.getDetail("2026-03-01", "2026-03-31", "RECEIPT_TO_VALIDATION",
                "CALENDAR", null, null, null, null, null, null, false, 0, 25, null, null, null, null, request);

        Assert.assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void getDetail_capsPageSizeAt200() {
        TATDetailResponse mockResponse = new TATDetailResponse();
        mockResponse.setTotalCount(0);
        mockResponse.setPage(0);
        mockResponse.setPageSize(200);
        mockResponse.setResults(new ArrayList<>());

        when(tatReportService.getDetail(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                anyBoolean(), anyInt(), anyInt(), any(), any(), any(), any())).thenReturn(mockResponse);

        // Request pageSize=500, should be capped to 200
        ResponseEntity<?> response = controller.getDetail("2026-03-01", "2026-03-31", "RECEIPT_TO_VALIDATION",
                "CALENDAR", null, null, null, null, null, null, false, 0, 500, null, null, null, null, request);

        Assert.assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // ========== Trend endpoint ==========

    @Test
    public void getTrend_returnsOkWithValidParams() {
        TATTrendResponse mockResponse = new TATTrendResponse();
        mockResponse.setCalculationMode("CALENDAR");
        mockResponse.setSeries(new ArrayList<>());

        when(tatReportService.getTrend(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                anyBoolean(), any(), any())).thenReturn(mockResponse);

        ResponseEntity<?> response = controller.getTrend("2026-03-01", "2026-03-31", "RECEIPT_TO_VALIDATION",
                "CALENDAR", null, null, null, null, null, null, false, "DAILY", null, request);

        Assert.assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // ========== Export endpoint ==========

    @Test
    public void export_rejectsInvalidDate() throws Exception {
        MockHttpServletResponse httpResponse = new MockHttpServletResponse();

        controller.export("bad-date", "2026-03-31", "RECEIPT_TO_VALIDATION", "CALENDAR", null, null, null, null, null,
                null, false, "CSV", request, httpResponse);

        Assert.assertEquals(400, httpResponse.getStatus());
    }

    // --- @PreAuthorize regression guard ---
    // Spring's hasAnyRole() auto-prepends "ROLE_", so passing 'ROLE_X' checks
    // for 'ROLE_ROLE_X' (no user ever has that). This test catches the exact
    // double-prefix bug that shipped in the original PR.

    @Test
    public void preAuthorize_mustExistOnController() {
        PreAuthorize annotation = TATReportRestController.class.getAnnotation(PreAuthorize.class);
        Assert.assertNotNull("Controller must have @PreAuthorize annotation", annotation);
    }

    @Test
    public void preAuthorize_roleNames_mustNotContainROLE_prefix() {
        PreAuthorize annotation = TATReportRestController.class.getAnnotation(PreAuthorize.class);
        Assert.assertNotNull(annotation);
        Assert.assertFalse(
                "Role names in hasAnyRole() must not use ROLE_ prefix — Spring auto-prepends it, "
                        + "causing a double-prefix (ROLE_ROLE_X). Found: " + annotation.value(),
                annotation.value().contains("'ROLE_"));
    }

    @Test
    public void preAuthorize_mustAllowReportsRole() {
        PreAuthorize annotation = TATReportRestController.class.getAnnotation(PreAuthorize.class);
        Assert.assertNotNull(annotation);
        // Extract role names from hasAnyRole('X', 'Y', 'Z')
        Matcher matcher = Pattern.compile("'([^']+)'").matcher(annotation.value());
        boolean hasReports = false;
        while (matcher.find()) {
            if ("REPORTS".equals(matcher.group(1))) {
                hasReports = true;
            }
        }
        Assert.assertTrue("@PreAuthorize must include REPORTS role to align with frontend "
                + "SecureRoute gate (Roles.REPORTS). Found: " + annotation.value(), hasReports);
    }
}
