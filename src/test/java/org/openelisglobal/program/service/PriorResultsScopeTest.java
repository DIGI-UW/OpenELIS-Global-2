package org.openelisglobal.program.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.program.dao.ProgramSampleDAO;
import org.openelisglobal.program.valueholder.AnatomicPathologyBench;
import org.openelisglobal.program.valueholder.PriorCase;
import org.openelisglobal.program.valueholder.PriorResults;
import org.openelisglobal.program.valueholder.ProgramSample;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.service.SampleItemService;

/**
 * A user who reads only some lab units sees, in the prior results panel, only
 * the cases whose order those units cover, and the total counts only those. The
 * order is asked about once, however many cases it holds. The DAO and the scope
 * are mocked because the rule under test is how the service asks the scope, and
 * every call asks for every row, so the row setting is never read.
 */
@RunWith(MockitoJUnitRunner.class)
public class PriorResultsScopeTest {

    private static final String RESTRICTED = "7";
    private static final String WHOLE_LAB = "1";
    private static final int ON_SCREEN = 1;

    @Mock
    private ProgramSampleDAO programSampleDAO;
    @Mock
    private ProgramSampleLabUnitScope programSampleLabUnitScope;
    @Mock
    private SampleItemService sampleItemService;
    @Mock
    private DictionaryService dictionaryService;

    private PriorResultsService service;

    @Before
    public void aCaseWithThreeEarlierCasesOnTwoOrders() {
        service = new PriorResultsServiceImpl(programSampleDAO, programSampleLabUnitScope, sampleItemService,
                dictionaryService);
        Sample onScreen = order("500", "2026-06-01");
        ProgramSample caseOnScreen = new ProgramSample();
        caseOnScreen.setId(ON_SCREEN);
        caseOnScreen.setSample(onScreen);
        when(programSampleDAO.getCase(AnatomicPathologyBench.PATHOLOGY, ON_SCREEN))
                .thenReturn(Optional.of(caseOnScreen));
        // Two cases on order 400, each carrying its own copy of the order, so a check
        // made once per order cannot be one made once per object.
        when(programSampleDAO.getCasesSharingPatientWith("500"))
                .thenReturn(List.of(new PriorCase(AnatomicPathologyBench.PATHOLOGY, ON_SCREEN, "GROSSING", onScreen),
                        new PriorCase(AnatomicPathologyBench.PATHOLOGY, 2, "GROSSING", order("400", "2026-05-01")),
                        new PriorCase(AnatomicPathologyBench.IMMUNOHISTOCHEMISTRY, 3, "IN_PROGRESS",
                                order("400", "2026-05-01")),
                        new PriorCase(AnatomicPathologyBench.CYTOLOGY, 4, "SCREENING", order("300", "2026-04-01"))));
    }

    @Test
    public void aRestrictedUser_getsOnlyTheOrdersTheyMayRead_andTheTotalCountsOnlyThose() {
        restrictedToOrder400();

        PriorResults results = service.priorResultsOf(AnatomicPathologyBench.PATHOLOGY, ON_SCREEN, true, RESTRICTED);

        assertEquals(List.of(2, 3), caseIds(results));
        assertEquals("the case outside the user's units is not counted either", 2, results.total());
        assertTrue("the panel is told some cases may not be listed", results.restricted());
    }

    @Test
    public void aRestrictedUser_isAskedOncePerOrder_notOncePerCase() {
        restrictedToOrder400();

        service.priorResultsOf(AnatomicPathologyBench.PATHOLOGY, ON_SCREEN, true, RESTRICTED);

        verify(programSampleLabUnitScope, times(1)).isOrderVisible(eq(RESTRICTED),
                argThat(sample -> "400".equals(sample.getId())));
        verify(programSampleLabUnitScope, times(1)).isOrderVisible(eq(RESTRICTED),
                argThat(sample -> "300".equals(sample.getId())));
        verify(programSampleLabUnitScope, never()).isOrderVisible(eq(RESTRICTED),
                argThat(sample -> "500".equals(sample.getId())));
    }

    @Test
    public void anUnrestrictedUser_isNeverCheckedRowByRow() {
        when(programSampleLabUnitScope.isRestricted(WHOLE_LAB)).thenReturn(false);

        PriorResults results = service.priorResultsOf(AnatomicPathologyBench.PATHOLOGY, ON_SCREEN, true, WHOLE_LAB);

        assertEquals(List.of(2, 3, 4), caseIds(results));
        assertEquals(3, results.total());
        assertFalse("nothing is left out for a user of the whole lab", results.restricted());
        verify(programSampleLabUnitScope, never()).isOrderVisible(any(), any());
    }

    /**
     * A restricted user whose patient has earlier cases only in other units gets no
     * row, and is told the list is narrowed rather than that there is nothing.
     */
    @Test
    public void everyAnswerSaysWhetherTheUserIsRestricted_theEmptyOnesIncluded() {
        when(programSampleLabUnitScope.isRestricted(RESTRICTED)).thenReturn(true);
        when(programSampleLabUnitScope.isRestricted(WHOLE_LAB)).thenReturn(false);

        PriorResults noneReadable = service.priorResultsOf(AnatomicPathologyBench.PATHOLOGY, ON_SCREEN, true,
                RESTRICTED);

        assertEquals(0, noneReadable.total());
        assertTrue(noneReadable.restricted());
        assertTrue("an unknown case answers the flag too",
                service.priorResultsOf(AnatomicPathologyBench.PATHOLOGY, 999, true, RESTRICTED).restricted());
        assertFalse(service.priorResultsOf(AnatomicPathologyBench.PATHOLOGY, 999, true, WHOLE_LAB).restricted());
        assertFalse(service.priorResultsOf(AnatomicPathologyBench.PATHOLOGY, ON_SCREEN, true, WHOLE_LAB).restricted());
    }

    // helpers

    private void restrictedToOrder400() {
        when(programSampleLabUnitScope.isRestricted(RESTRICTED)).thenReturn(true);
        when(programSampleLabUnitScope.isOrderVisible(eq(RESTRICTED), argThat(sample -> "400".equals(sample.getId()))))
                .thenReturn(true);
    }

    /**
     * The request date is returned rather than set: the setter also formats it for
     * display, which reads the site's messages this test does not load.
     */
    private static Sample order(String id, String requestDate) {
        java.sql.Date entered = java.sql.Date.valueOf(requestDate);
        Sample sample = new Sample() {
            @Override
            public java.sql.Date getEnteredDate() {
                return entered;
            }
        };
        sample.setId(id);
        sample.setAccessionNumber("LAB-" + id);
        return sample;
    }

    private static List<Integer> caseIds(PriorResults results) {
        return results.rows().stream().map(row -> row.caseId()).collect(Collectors.toList());
    }
}
