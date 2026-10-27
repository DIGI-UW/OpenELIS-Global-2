package org.openelisglobal.testcatalog.controller;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.valueholder.TestSection;
import org.openelisglobal.testcatalog.controller.rest.TestCatalogEditorRestController;
import org.springframework.test.util.ReflectionTestUtils;

public class TestCatalogEditorLabUnitsTest {

    private TestSectionService testSectionService;
    private TestCatalogEditorRestController controller;

    @Before
    public void setUp() {
        testSectionService = mock(TestSectionService.class);
        controller = new TestCatalogEditorRestController(null, null, null, null, null, null, null, null, null, null,
                null, null, null);
        ReflectionTestUtils.setField(controller, "testSectionService", testSectionService);
    }

    @Test
    public void listLabUnitsReturnsOnlyActiveSelectableUnits() {
        TestSection active = mock(TestSection.class);
        when(active.getId()).thenReturn("7");
        when(active.getLocalizedName()).thenReturn("Molecular Biology");
        when(testSectionService.getAllActiveTestSections()).thenReturn(List.of(active));

        List<TestCatalogEditorRestController.LabUnitOption> result = controller.listLabUnits(false);

        assertEquals(1, result.size());
        assertEquals("7", result.get(0).id);
        assertEquals("Molecular Biology", result.get(0).name);
    }

    /**
     * OGC-1459: the test editor asks for the deactivated units too, so a test kept
     * in one shows it (marked inactive) rather than a blank picker. The user
     * sentinel section is never offered.
     */
    @Test
    public void listLabUnitsWithInactiveAddsTheDeactivatedOnes_markedInactive() {
        TestSection active = mock(TestSection.class);
        when(active.getId()).thenReturn("7");
        when(active.getLocalizedName()).thenReturn("Molecular Biology");
        when(active.getIsActive()).thenReturn("Y");
        TestSection retired = mock(TestSection.class);
        when(retired.getId()).thenReturn("9");
        when(retired.getLocalizedName()).thenReturn("Parasitology");
        when(retired.getIsActive()).thenReturn("N");
        when(retired.getTestSectionName()).thenReturn("Parasitology");
        TestSection sentinel = mock(TestSection.class);
        when(sentinel.getTestSectionName()).thenReturn(TestSectionService.USER_SENTINEL_SECTION_NAME);
        when(testSectionService.getAllActiveTestSections()).thenReturn(List.of(active));
        when(testSectionService.getAllInActiveTestSections()).thenReturn(List.of(retired, sentinel));

        List<TestCatalogEditorRestController.LabUnitOption> withInactive = controller.listLabUnits(true);

        assertEquals(2, withInactive.size());
        TestCatalogEditorRestController.LabUnitOption parasitology = withInactive.stream().filter(o -> "9".equals(o.id))
                .findFirst().get();
        assertEquals(Boolean.FALSE, parasitology.isActive);
        assertEquals("the default list stays active-only", 1, controller.listLabUnits(false).size());
    }
}
