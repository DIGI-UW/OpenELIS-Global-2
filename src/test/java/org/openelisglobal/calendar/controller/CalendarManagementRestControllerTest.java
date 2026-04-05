package org.openelisglobal.calendar.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.calendar.controller.rest.CalendarManagementRestController;
import org.openelisglobal.calendar.service.PublicHolidayService;
import org.openelisglobal.calendar.service.WeekendConfigService;
import org.openelisglobal.calendar.valueholder.PublicHoliday;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;

/**
 * Unit tests for CalendarManagementRestController.
 *
 * Uses Mockito to test controller logic directly without Spring context,
 * avoiding interceptor/permission issues that require a running database.
 * Integration-level permission testing is covered by E2E Playwright tests.
 */
@RunWith(MockitoJUnitRunner.class)
public class CalendarManagementRestControllerTest {

    @InjectMocks
    private CalendarManagementRestController controller;

    @Mock
    private PublicHolidayService publicHolidayService;

    @Mock
    private WeekendConfigService weekendConfigService;

    private MockHttpServletRequest request;

    @Before
    public void setUp() {
        request = new MockHttpServletRequest();
        MockHttpSession session = new MockHttpSession();
        org.openelisglobal.login.valueholder.UserSessionData usd = new org.openelisglobal.login.valueholder.UserSessionData();
        usd.setSytemUserId(1);
        session.setAttribute(org.openelisglobal.common.action.IActionConstants.USER_SESSION_DATA, usd);
        request.setSession(session);

        // Default: weekends are Saturday(6) and Sunday(0)
        when(weekendConfigService.getWeekendDayNumbers()).thenReturn(Arrays.asList(0, 6));
    }

    // ========== GET /rest/calendar/holidays ==========

    @Test
    public void getHolidays_returnsListForYear() {
        List<PublicHoliday> holidays = new ArrayList<>();
        PublicHoliday h = new PublicHoliday();
        h.setId(1);
        h.setHolidayDate(Date.valueOf("2026-06-15"));
        h.setHolidayName("Test Holiday");
        h.setIsRecurring(false);
        h.setIsActive(true);
        holidays.add(h);

        when(publicHolidayService.getHolidaysForYear(2026, false)).thenReturn(holidays);

        ResponseEntity<Map<String, Object>> response = controller.getHolidays(2026, false, request);

        Assert.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assert.assertNotNull(response.getBody());
        Assert.assertEquals(2026, response.getBody().get("year"));
        List<?> list = (List<?>) response.getBody().get("holidays");
        Assert.assertEquals(1, list.size());
    }

    @Test
    public void getHolidays_returnsEmptyForYearWithNoHolidays() {
        when(publicHolidayService.getHolidaysForYear(2099, false)).thenReturn(new ArrayList<>());

        ResponseEntity<Map<String, Object>> response = controller.getHolidays(2099, false, request);

        Assert.assertEquals(HttpStatus.OK, response.getStatusCode());
        List<?> list = (List<?>) response.getBody().get("holidays");
        Assert.assertTrue(list.isEmpty());
    }

    // ========== POST /rest/calendar/holidays ==========

    @Test
    public void createHoliday_returnsCreatedHoliday() {
        Map<String, Object> body = new HashMap<>();
        body.put("date", "2026-07-04");
        body.put("name", "Independence Day");
        body.put("isRecurring", false);

        PublicHoliday created = new PublicHoliday();
        created.setId(10);
        created.setHolidayDate(Date.valueOf("2026-07-04"));
        created.setHolidayName("Independence Day");
        created.setIsRecurring(false);
        created.setIsActive(true);

        when(publicHolidayService.create(any(PublicHoliday.class), anyInt())).thenReturn(created);

        ResponseEntity<Map<String, Object>> response = controller.createHoliday(body, request);

        Assert.assertEquals(HttpStatus.CREATED, response.getStatusCode());
        Assert.assertNotNull(response.getBody());
        Assert.assertEquals("Independence Day", response.getBody().get("name"));
    }

    @Test
    public void createHoliday_rejectsDuplicate_returns409() {
        Map<String, Object> body = new HashMap<>();
        body.put("date", "2026-08-01");
        body.put("name", "Test Holiday");
        body.put("isRecurring", false);

        when(publicHolidayService.create(any(PublicHoliday.class), anyInt()))
                .thenThrow(new LIMSRuntimeException("Holiday already exists for date 2026-08-01"));

        ResponseEntity<Map<String, Object>> response = controller.createHoliday(body, request);

        Assert.assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    @Test(expected = org.springframework.web.server.ResponseStatusException.class)
    public void createHoliday_rejectsEmptyName_returns400() {
        Map<String, Object> body = new HashMap<>();
        body.put("date", "2026-09-01");
        body.put("name", "");
        body.put("isRecurring", false);

        controller.createHoliday(body, request);
    }

    // ========== DELETE /rest/calendar/holidays/{id} ==========

    @Test
    public void deleteHoliday_returnsNoContent() {
        PublicHoliday existing = new PublicHoliday();
        existing.setId(5);
        existing.setHolidayName("ToDelete");
        when(publicHolidayService.getById(5)).thenReturn(existing);

        ResponseEntity<Void> response = controller.deleteHoliday(5, request);

        Assert.assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(publicHolidayService).delete(5);
    }

    @Test
    public void deleteHoliday_notFound_returns404() {
        when(publicHolidayService.getById(999)).thenReturn(null);

        ResponseEntity<Void> response = controller.deleteHoliday(999, request);

        Assert.assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    // ========== GET/PUT /rest/calendar/weekends ==========

    @Test
    public void getWeekends_returnsWeekendDays() {
        ResponseEntity<Map<String, Object>> response = controller.getWeekends(request);

        Assert.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assert.assertNotNull(response.getBody());
        Assert.assertTrue(response.getBody().containsKey("weekendDays"));
    }

    @Test
    public void updateWeekends_changesConfig() {
        Map<String, Object> body = new HashMap<>();
        body.put("weekendDays", Arrays.asList(5, 6));

        ResponseEntity<Map<String, Object>> response = controller.updateWeekends(body, request);

        Assert.assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(weekendConfigService).updateWeekendDays(Arrays.asList(5, 6), 1);
    }

    // ========== Auth Ordering Tests ==========

    @Test
    public void testCreateHoliday_unauthenticated_returns401_noServiceInteraction() {
        MockHttpServletRequest unauthRequest = new MockHttpServletRequest();
        // No session or user data set — getSysUserId should return null

        Map<String, Object> body = new HashMap<>();
        body.put("date", "2026-07-04");
        body.put("name", "Independence Day");
        body.put("isRecurring", false);

        try {
            controller.createHoliday(body, unauthRequest);
            Assert.fail("Expected ResponseStatusException for unauthenticated request");
        } catch (org.springframework.web.server.ResponseStatusException e) {
            Assert.assertEquals(HttpStatus.UNAUTHORIZED, e.getStatusCode());
        }

        // Auth check must happen BEFORE any service interaction
        verifyZeroInteractions(publicHolidayService);
    }

    @Test
    public void testUpdateHoliday_unauthenticated_returns401_noServiceInteraction() {
        MockHttpServletRequest unauthRequest = new MockHttpServletRequest();
        // No session or user data set — getSysUserId should return null

        Map<String, Object> body = new HashMap<>();
        body.put("date", "2026-07-04");
        body.put("name", "Updated Holiday");

        try {
            controller.updateHoliday(1, body, unauthRequest);
            Assert.fail("Expected ResponseStatusException for unauthenticated request");
        } catch (org.springframework.web.server.ResponseStatusException e) {
            Assert.assertEquals(HttpStatus.UNAUTHORIZED, e.getStatusCode());
        }

        // Auth check must happen BEFORE any service interaction (including getById)
        verifyZeroInteractions(publicHolidayService);
    }
}
