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
 * 1,450). The paging the workplan sends now carries the tests of every page.
 */
public class WorkplanPagingTotalTest extends BaseWebContextSensitiveTest {

    private static TestResultItem test(String accessionNumber, boolean groupHeading) {
        TestResultItem item = new TestResultItem();
        item.setAccessionNumber(accessionNumber);
        item.setServingAsTestGroupIdentifier(groupHeading);
        return item;
    }

    @Test
    public void theTotalCountsTheTestsOfEveryPage() throws Exception {
        int tests = SpringContext.getBean(PagingProperties.class).getResultsPageSize() * 2 + 3;
        List<TestResultItem> workplan = new ArrayList<>();
        workplan.add(test("ACC-0", true));
        for (int i = 0; i < tests; i++) {
            workplan.add(test("ACC-" + i, false));
        }
        MockHttpServletRequest request = new MockHttpServletRequest();

        WorkplanForm first = new WorkplanForm();
        new WorkplanPaging().setDatabaseResults(request, first, workplan);
        WorkplanForm second = new WorkplanForm();
        new WorkplanPaging().page(request, second, 2);

        assertTrue(Integer.parseInt(first.getPaging().getTotalPages()) > 1);
        assertTrue(first.getWorkplanTests().size() < tests);
        assertEquals(String.valueOf(tests), first.getPaging().getTotalResults());
        assertEquals(String.valueOf(tests), second.getPaging().getTotalResults());
    }
}
