package org.openelisglobal.calendar.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.ArgumentCaptor;
import org.openelisglobal.calendar.controller.rest.CalendarManagementRestController;
import org.openelisglobal.calendar.service.PublicHolidayService;
import org.openelisglobal.calendar.service.WeekendConfigService;
import org.openelisglobal.calendar.valueholder.PublicHoliday;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

@RunWith(Parameterized.class)
public class CalendarManagementRestControllerDateTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> dates() {
        return Arrays
                .asList(new Object[][] { { "2026-02-30", false }, { "2027-02-29", false }, { "2028-02-29", true } });
    }

    private final String date;
    private final boolean valid;
    private CalendarManagementRestController controller;
    private PublicHolidayService holidays;
    private MockHttpServletRequest request;

    public CalendarManagementRestControllerDateTest(String date, boolean valid) {
        this.date = date;
        this.valid = valid;
    }

    @Before
    public void setUp() {
        controller = new CalendarManagementRestController();
        holidays = mock(PublicHolidayService.class);
        WeekendConfigService weekends = mock(WeekendConfigService.class);
        when(weekends.getWeekendDayNumbers()).thenReturn(List.of(0, 6));
        ReflectionTestUtils.setField(controller, "publicHolidayService", holidays);
        ReflectionTestUtils.setField(controller, "weekendConfigService", weekends);
        request = new MockHttpServletRequest();
        UserSessionData user = new UserSessionData();
        user.setSytemUserId(1);
        request.getSession().setAttribute(IActionConstants.USER_SESSION_DATA, user);
    }

    @Test
    public void createValidatesDateBeforeSaving() {
        Map<String, Object> body = Map.of("date", date, "name", "Test holiday");
        when(holidays.create(any(PublicHoliday.class), eq(1))).thenAnswer(invocation -> invocation.getArgument(0));
        if (!valid) {
            assertBadDate(assertThrows(ResponseStatusException.class, () -> controller.createHoliday(body, request)));
            verify(holidays, never()).create(any(PublicHoliday.class), anyInt());
            return;
        }
        ResponseEntity<Map<String, Object>> response = controller.createHoliday(body, request);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(date, response.getBody().get("date"));
        ArgumentCaptor<PublicHoliday> saved = ArgumentCaptor.forClass(PublicHoliday.class);
        verify(holidays).create(saved.capture(), eq(1));
        assertEquals(date, saved.getValue().getHolidayDate().toString());
    }

    @Test
    public void updateValidatesDateBeforeMutatingOrSaving() {
        PublicHoliday existing = new PublicHoliday();
        existing.setId(10);
        existing.setHolidayDate(Date.valueOf("2026-01-01"));
        existing.setHolidayName("Original holiday");
        when(holidays.getById(10)).thenReturn(existing);
        when(holidays.update(existing, 1)).thenReturn(existing);
        Map<String, Object> body = Map.of("date", date, "name", "Updated holiday");
        if (!valid) {
            assertBadDate(
                    assertThrows(ResponseStatusException.class, () -> controller.updateHoliday(10, body, request)));
            verify(holidays, never()).update(any(PublicHoliday.class), anyInt());
            assertEquals("2026-01-01", existing.getHolidayDate().toString());
            assertEquals("Original holiday", existing.getHolidayName());
            return;
        }
        ResponseEntity<Map<String, Object>> response = controller.updateHoliday(10, body, request);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(date, response.getBody().get("date"));
        verify(holidays).update(existing, 1);
    }

    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void csvRejectsInvalidRowBeforeImporting() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "holidays.csv", "text/csv",
                ("date,name,recurring\n" + date + ",Test holiday,false\n").getBytes(StandardCharsets.UTF_8));
        int year = Integer.parseInt(date.substring(0, 4));
        when(holidays.importHolidays(anyList(), eq(year), eq(1)))
                .thenReturn(new PublicHolidayService.ImportResult(valid ? 1 : 0, 0, List.of()));

        ResponseEntity<Map<String, Object>> response = controller.importHolidays(file, year, request);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(valid ? 1 : 0, response.getBody().get("imported"));
        assertEquals(valid ? 0 : 1, response.getBody().get("skipped"));
        ArgumentCaptor<List<PublicHoliday>> imported = ArgumentCaptor.forClass((Class) List.class);
        verify(holidays).importHolidays(imported.capture(), eq(year), eq(1));
        List<?> errors = (List<?>) response.getBody().get("errors");
        if (valid) {
            assertTrue(errors.isEmpty());
            assertEquals(1, imported.getValue().size());
            assertEquals(date, imported.getValue().get(0).getHolidayDate().toString());
        } else {
            assertTrue(imported.getValue().isEmpty());
            assertEquals(1, errors.size());
            Map<?, ?> error = (Map<?, ?>) errors.get(0);
            assertEquals(2, error.get("row"));
            assertTrue(error.get("reason").toString().contains("Invalid date"));
            assertTrue(error.get("reason").toString().contains(date));
        }
    }

    private void assertBadDate(ResponseStatusException error) {
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        assertTrue(error.getReason().contains("Invalid date"));
        assertTrue(error.getReason().contains("yyyy-MM-dd"));
    }
}
