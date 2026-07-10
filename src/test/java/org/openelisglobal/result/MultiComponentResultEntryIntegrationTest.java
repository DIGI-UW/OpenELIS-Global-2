package org.openelisglobal.result;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.result.action.util.ResultsLoadUtility;
import org.openelisglobal.test.beanItems.TestResultItem;
import org.openelisglobal.testresultcomponent.service.TestResultComponentService;
import org.openelisglobal.testresultcomponent.valueholder.TestResultComponent;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * v2.4 FRS §Result Components runtime behavior: ordering one multi-component
 * test yields one result-entry field per active component ("User orders one
 * test; result entry shows N component fields"), each bound to its component.
 */
public class MultiComponentResultEntryIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private ResultsLoadUtility resultsLoadUtility;

    @Autowired
    private TestResultComponentService componentService;

    @Autowired
    private AnalysisService analysisService;

    @Before
    public void init() throws Exception {
        executeDataSetWithStateManagement("testdata/analysis.xml");
    }

    private TestResultComponent component(String code, String label, int order, String type) {
        TestResultComponent c = new TestResultComponent();
        c.setCode(code);
        c.setLabel(label);
        c.setDisplayOrder(order);
        c.setResultType(type);
        return c;
    }

    @SuppressWarnings("unchecked")
    private List<TestResultItem> loadItems(Analysis analysis) throws Exception {
        Method loader = ResultsLoadUtility.class.getDeclaredMethod("getTestResultItemFromAnalysis", Analysis.class,
                String.class, String.class, String.class);
        loader.setAccessible(true);
        return (List<TestResultItem>) loader.invoke(resultsLoadUtility, analysis, " ", " ", "");
    }

    @Test
    public void multiComponentTest_rendersOneResultFieldPerComponent() throws Exception {
        // Test 1 (dataset) gains two numeric components: PRIMARY + DIA. The save
        // also seeds one test_result row per component (component-typed).
        componentService.saveSampleResults("1",
                List.of(component("PRIMARY", "Systolic", 0, "N"), component("DIA", "Diastolic", 1, "N")), null, null,
                "1");

        Analysis analysis = analysisService.get("1");
        List<TestResultItem> items = loadItems(analysis);

        assertEquals("one result-entry field per active component", 2, items.size());
        assertNotNull(items.get(0).getTestResultComponentId());
        assertNotNull(items.get(1).getTestResultComponentId());
        assertNotEquals("each field is bound to its own component", items.get(0).getTestResultComponentId(),
                items.get(1).getTestResultComponentId());
        assertTrue("rows are labeled with their component",
                items.stream().anyMatch(i -> i.getTestName().contains("Systolic")));
        assertTrue(items.stream().anyMatch(i -> i.getTestName().contains("Diastolic")));
        assertEquals("N", items.get(0).getResultType());
        assertEquals("N", items.get(1).getResultType());
    }

    @Test
    public void singleComponentTest_keepsSingleResultField() throws Exception {
        componentService.saveSampleResults("1", List.of(component("PRIMARY", "Primary", 0, "N")), null, null, "1");

        Analysis analysis = analysisService.get("1");
        List<TestResultItem> items = loadItems(analysis);

        assertEquals("single-component tests are unchanged", 1, items.size());
    }
}
