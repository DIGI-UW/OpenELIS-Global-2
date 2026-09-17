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

        List<TestCatalogEditorRestController.LabUnitOption> result = controller.listLabUnits();

        assertEquals(1, result.size());
        assertEquals("7", result.get(0).id);
        assertEquals("Molecular Biology", result.get(0).name);
    }
}
