package org.openelisglobal.testResult;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.testresult.dao.TestResultDAO;
import org.openelisglobal.testresult.service.TestResultServiceImpl;
import org.openelisglobal.testresult.valueholder.TestResult;

/**
 * A component's options read back in a stable order: by sort order, and by
 * creation (id) where sort orders are equal or missing. Before, ties came back
 * in whatever order the database returned, so a test copied from another could
 * list the same options in a different order (OGC-1234 Copy from test, seen in
 * the OGC-1424 E2E run).
 */
@RunWith(MockitoJUnitRunner.class)
public class TestResultOptionOrderTest {

    @Mock
    private TestResultDAO testResultDAO;

    @InjectMocks
    private TestResultServiceImpl testResultService;

    private static TestResult option(String id, String sortOrder) {
        TestResult option = new TestResult();
        option.setId(id);
        option.setSortOrder(sortOrder);
        option.setTestResultType("D");
        return option;
    }

    @Test
    public void tiesAndMissingSortOrdersFollowCreationOrder() {
        List<TestResult> scrambled = new ArrayList<>(List.of(option("309", null), option("12", "2"),
                option("301", null), option("15", "1"), option("120", "2"), option("305", null)));
        when(testResultDAO.getAllMatching(anyMap())).thenReturn(scrambled);

        List<String> ids = testResultService.getActiveOptionsByComponentId("7").stream().map(TestResult::getId)
                .collect(Collectors.toList());

        assertEquals(List.of("15", "12", "120", "301", "305", "309"), ids);
    }
}
