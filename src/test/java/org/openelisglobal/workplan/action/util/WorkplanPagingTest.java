package org.openelisglobal.workplan.action.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.paging.PagingProperties;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.beanItems.TestResultItem;
import org.openelisglobal.workplan.form.WorkplanForm;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * OGC-1647: "Total Tests" counted the rows of the page on screen (100 of about
 * 1,450), and the pages Print read came from the session cache the results
 * screens share.
 */
public class WorkplanPagingTest extends BaseWebContextSensitiveTest {

    private static TestResultItem test(String accessionNumber, boolean groupHeading) {
        TestResultItem item = new TestResultItem();
        item.setAccessionNumber(accessionNumber);
        item.setServingAsTestGroupIdentifier(groupHeading);
        return item;
    }

    private static List<TestResultItem> workplan(String prefix, int tests) {
        List<TestResultItem> workplan = new ArrayList<>();
        for (int i = 0; i < tests; i++) {
            workplan.add(test(prefix + i, false));
        }
        return workplan;
    }

    @Test
    public void theTotalCountsTheTestsOfEveryPage() throws Exception {
        int tests = SpringContext.getBean(PagingProperties.class).getResultsPageSize() * 2 + 3;
        List<TestResultItem> workplan = new ArrayList<>();
        workplan.add(test("ACC-0", true));
        workplan.addAll(workplan("ACC-", tests));
        MockHttpServletRequest request = new MockHttpServletRequest();

        WorkplanForm first = new WorkplanForm();
        new WorkplanPaging("test:5").setDatabaseResults(request, first, workplan);
        WorkplanForm second = new WorkplanForm();
        new WorkplanPaging("test:5").page(request, second, 2);

        assertTrue(Integer.parseInt(first.getPaging().getTotalPages()) > 1);
        assertTrue(first.getWorkplanTests().size() < tests);
        assertEquals(String.valueOf(tests), first.getPaging().getTotalResults());
        assertEquals(String.valueOf(tests), second.getPaging().getTotalResults());
    }

    @Test
    public void anOlderWorkplansPagesAreDroppedAndItsPageComesBackEmpty() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        for (int search = 1; search <= WorkplanPaging.SEARCHES_KEPT + 1; search++) {
            new WorkplanPaging("test:" + search).setDatabaseResults(request, new WorkplanForm(),
                    workplan("ACC-" + search + "-", 2));
        }

        WorkplanForm oldest = new WorkplanForm();
        new WorkplanPaging("test:1").page(request, oldest, 1);
        WorkplanForm kept = new WorkplanForm();
        new WorkplanPaging("test:2").page(request, kept, 1);

        assertEquals(List.of(), oldest.getWorkplanTests());
        assertEquals("ACC-2-0", kept.getWorkplanTests().get(0).getAccessionNumber());
    }
}
