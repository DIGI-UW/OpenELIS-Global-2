package org.openelisglobal.calendar.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Date;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.calendar.dao.PublicHolidayDAO;
import org.openelisglobal.calendar.valueholder.PublicHoliday;
import org.openelisglobal.common.exception.LIMSRuntimeException;

@RunWith(MockitoJUnitRunner.class)
public class PublicHolidayServiceTest {

    @InjectMocks
    private PublicHolidayServiceImpl publicHolidayService;

    @Mock
    private PublicHolidayDAO publicHolidayDAO;

    private PublicHoliday sampleHoliday;

    @Before
    public void setUp() {
        sampleHoliday = new PublicHoliday();
        sampleHoliday.setId(1);
        sampleHoliday.setHolidayDate(Date.valueOf("2026-01-01"));
        sampleHoliday.setHolidayName("New Year's Day");
        sampleHoliday.setIsRecurring(true);
        sampleHoliday.setIsActive(true);
    }

    @Test
    public void getHolidaysForYear_delegatesToDAO() {
        when(publicHolidayDAO.getHolidaysForYear(2026, false)).thenReturn(List.of(sampleHoliday));

        List<PublicHoliday> result = publicHolidayService.getHolidaysForYear(2026, false);

        Assert.assertEquals(1, result.size());
        Assert.assertEquals("New Year's Day", result.get(0).getHolidayName());
    }

    @Test
    public void getById_returnsHoliday() {
        when(publicHolidayDAO.get(1)).thenReturn(Optional.of(sampleHoliday));

        PublicHoliday result = publicHolidayService.getById(1);

        Assert.assertNotNull(result);
        Assert.assertEquals("New Year's Day", result.getHolidayName());
    }

    @Test
    public void getById_returnsNullWhenNotFound() {
        when(publicHolidayDAO.get(999)).thenReturn(Optional.empty());

        PublicHoliday result = publicHolidayService.getById(999);

        Assert.assertNull(result);
    }

    @Test
    public void create_insertsHoliday() {
        when(publicHolidayDAO.existsByDateInYear(any(), anyInt(), isNull())).thenReturn(false);
        when(publicHolidayDAO.insert(any())).thenReturn(1);
        when(publicHolidayDAO.get(1)).thenReturn(Optional.of(sampleHoliday));

        PublicHoliday result = publicHolidayService.create(sampleHoliday, 1);

        Assert.assertNotNull(result);
        verify(publicHolidayDAO).insert(any());
    }

    @Test(expected = LIMSRuntimeException.class)
    public void create_rejectsDuplicateDate() {
        when(publicHolidayDAO.existsByDateInYear(any(), anyInt(), isNull())).thenReturn(true);

        publicHolidayService.create(sampleHoliday, 1);
    }

    @Test
    public void update_updatesHoliday() {
        when(publicHolidayDAO.existsByDateInYear(any(), anyInt(), eq(1))).thenReturn(false);
        when(publicHolidayDAO.update(any())).thenReturn(sampleHoliday);

        PublicHoliday result = publicHolidayService.update(sampleHoliday, 1);

        Assert.assertNotNull(result);
        verify(publicHolidayDAO).update(any());
    }

    @Test
    public void delete_deletesHoliday() {
        when(publicHolidayDAO.get(1)).thenReturn(Optional.of(sampleHoliday));

        publicHolidayService.delete(1);

        verify(publicHolidayDAO).delete(sampleHoliday);
    }

    @Test(expected = LIMSRuntimeException.class)
    public void delete_throwsWhenNotFound() {
        when(publicHolidayDAO.get(999)).thenReturn(Optional.empty());

        publicHolidayService.delete(999);
    }

    @Test
    public void importHolidays_importsValidAndSkipsDuplicates() {
        PublicHoliday h1 = new PublicHoliday();
        h1.setHolidayDate(Date.valueOf("2026-05-01"));
        h1.setHolidayName("Labour Day");

        PublicHoliday h2 = new PublicHoliday();
        h2.setHolidayDate(Date.valueOf("2026-01-01"));
        h2.setHolidayName("New Year");

        // h1 is new, h2 is duplicate
        when(publicHolidayDAO.existsByDateInYear(eq(Date.valueOf("2026-05-01")), eq(2026), isNull())).thenReturn(false);
        when(publicHolidayDAO.existsByDateInYear(eq(Date.valueOf("2026-01-01")), eq(2026), isNull())).thenReturn(true);
        when(publicHolidayDAO.insert(any())).thenReturn(2);

        PublicHolidayService.ImportResult result = publicHolidayService.importHolidays(Arrays.asList(h1, h2), 2026, 1);

        Assert.assertEquals(1, result.imported());
        Assert.assertEquals(1, result.skipped());
        Assert.assertEquals(1, result.errors().size());
        Assert.assertTrue(result.errors().get(0).reason().contains("Duplicate"));
    }

    @Test
    public void importHolidays_skipsMissingFields() {
        PublicHoliday h1 = new PublicHoliday();
        // Missing date and name

        PublicHolidayService.ImportResult result = publicHolidayService.importHolidays(Collections.singletonList(h1),
                2026, 1);

        Assert.assertEquals(0, result.imported());
        Assert.assertEquals(1, result.skipped());
        Assert.assertTrue(result.errors().get(0).reason().contains("Missing"));
    }
}
