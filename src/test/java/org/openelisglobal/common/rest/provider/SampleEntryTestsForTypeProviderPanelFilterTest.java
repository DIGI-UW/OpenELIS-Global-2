package org.openelisglobal.common.rest.provider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.rest.provider.SampleEntryTestsForTypeProviderRestController.PanelTestMap;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.service.TestServiceImpl;
import org.openelisglobal.typeofsample.valueholder.TypeOfSamplePanel;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * OGC-1189: a panel offered during sample entry must not leak tests that are
 * not configured for the chosen sample type. {@code linkTestsToPanels} builds
 * the per-panel test-id string from only the members whose localized name is
 * present in the sample-type-filtered {@code tests} list; a member outside that
 * list is dropped.
 *
 * <p>
 * This exercises the controller method directly (it lives in this package and
 * is package-private) rather than through the {@code /rest/sample-type-tests}
 * endpoint, which would need a full authenticated session. The panel list is
 * built in code so no {@code type_of_sample}/{@code sampletype_test} wiring is
 * needed — the fixture only has to make panel P and its two members resolvable.
 */
public class SampleEntryTestsForTypeProviderPanelFilterTest extends BaseWebContextSensitiveTest {

    private static final String PANEL_P_ID = "4001";
    private static final String TEST_IN_ID = "3001";
    private static final String TEST_OUT_ID = "3002";

    @Autowired
    private TestService testService;

    private SampleEntryTestsForTypeProviderRestController controller;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/sample-entry-panel-filter.xml");
        controller = new SampleEntryTestsForTypeProviderRestController();
    }

    @Test
    public void linkTestsToPanels_dropsPanelMemberNotConfiguredForSampleType() {
        org.openelisglobal.test.valueholder.Test testIn = testService.get(TEST_IN_ID);
        assertNotNull("fixture must load T_IN", testIn);

        List<org.openelisglobal.test.valueholder.Test> sampleTypeFilteredTests = List.of(testIn);

        TypeOfSamplePanel samplePanel = new TypeOfSamplePanel();
        samplePanel.setPanelId(PANEL_P_ID);

        List<PanelTestMap> result = controller.linkTestsToPanels(List.of(samplePanel), sampleTypeFilteredTests);

        assertEquals("panel P must be selected because T_IN is in-type", 1, result.size());
        PanelTestMap panelMap = result.get(0);
        assertEquals(PANEL_P_ID, panelMap.getId());
        assertEquals("T_OUT (not configured for the sample type) must be dropped from the panel", TEST_IN_ID,
                panelMap.getTestIds());

        assertEquals("Test In", TestServiceImpl.getUserLocalizedTestName(testIn));
        assertEquals("Test Out", TestServiceImpl.getUserLocalizedTestName(testService.get(TEST_OUT_ID)));
    }
}
