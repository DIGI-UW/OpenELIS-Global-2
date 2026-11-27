package org.openelisglobal.analyzerresults.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analyzerresults.action.beanitems.AnalyzerResultItem;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;

/** A reviewer's decision applies to one test on one tube. */
public class AnalyzerResultsAcceptDecisionTest extends BaseWebContextSensitiveTest {

    private AnalyzerResultsAcceptServiceImpl service;

    // The service reads configuration while its class loads, so it is built once
    // the
    // application context is up.
    @Before
    public void createService() {
        service = new AnalyzerResultsAcceptServiceImpl(mock(TypeOfSampleService.class));
    }

    @Test
    public void aDecisionOnOneTubeLeavesTheSameTestOnAnotherTubeUndecided() {
        AnalyzerResultItem first = item("1", "TUBE-1", null);
        first.setIsAccepted(true);
        AnalyzerResultItem second = item("2", "TUBE-2", null);

        List<AnalyzerResultItem> actionable = service.extractActionableResult(List.of(first, second));

        assertEquals(List.of(first), actionable);
    }

    @Test
    public void aTestsComponentsOnItsTubeFollowItsDecision() {
        AnalyzerResultItem main = item("1", "TUBE-1", null);
        main.setIsAccepted(true);
        AnalyzerResultItem part = item("2", "TUBE-1", "component-log");

        List<AnalyzerResultItem> actionable = service.extractActionableResult(List.of(main, part));

        assertEquals(List.of(main, part), actionable);
    }

    @Test
    public void rowsStagedWithoutATubeIdShareTheirOrdersDecision() {
        AnalyzerResultItem main = item("1", null, null);
        main.setIsAccepted(true);
        AnalyzerResultItem part = item("2", null, "component-log");

        List<AnalyzerResultItem> actionable = service.extractActionableResult(List.of(main, part));

        assertEquals(List.of(main, part), actionable);
    }

    private static AnalyzerResultItem item(String id, String tube, String componentId) {
        AnalyzerResultItem item = new AnalyzerResultItem();
        item.setId(id);
        item.setSampleGroupingNumber(1);
        item.setAccessionNumber("ACC-1");
        item.setTestId("7");
        item.setInstrumentSpecimenId(tube);
        item.setComponentId(componentId);
        return item;
    }
}
