package org.openelisglobal.test.controller.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.typeofsample.service.TypeOfSamplePanelService;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.service.TypeOfSampleTestService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.openelisglobal.typeofsample.valueholder.TypeOfSamplePanel;
import org.openelisglobal.typeofsample.valueholder.TypeOfSampleTest;

/**
 * The sample types offered for an ordered test or panel come only from its
 * sample type mapping, active types only, so the Collect step never has to fall
 * back to an arbitrary list.
 */
@RunWith(MockitoJUnitRunner.class)
public class TestSampleTypesRestControllerTest {

    @Mock
    private TestService testService;
    @Mock
    private TypeOfSampleService typeOfSampleService;
    @Mock
    private TypeOfSampleTestService typeOfSampleTestService;
    @Mock
    private TypeOfSamplePanelService typeOfSamplePanelService;

    @InjectMocks
    private TestRestController controller;

    @Test
    public void aTestListsItsActiveMappedSampleTypes() {
        org.openelisglobal.test.valueholder.Test amylase = new org.openelisglobal.test.valueholder.Test();
        amylase.setId("7");
        amylase.setName("Amylase");
        when(testService.get("7")).thenReturn(amylase);
        when(typeOfSampleTestService.getTypeOfSampleTestsForTest("7"))
                .thenReturn(List.of(sampleTest("1"), sampleTest("2")));
        when(typeOfSampleService.get("1")).thenReturn(sampleType("1", "Serum", true));
        when(typeOfSampleService.get("2")).thenReturn(sampleType("2", "Retired plasma", false));

        List<Map<String, Object>> types = compatibleTypes(controller.getTestSampleTypes("7", "").getBody(), "tests");

        assertEquals(1, types.size());
        assertEquals("1", types.getFirst().get("id"));
        assertTrue("no placeholder code is appended to the name", !types.getFirst().containsKey("code"));
    }

    @Test
    public void aPanelListsItsActiveMappedSampleTypes() {
        TypeOfSamplePanel mapping = new TypeOfSamplePanel();
        mapping.setTypeOfSampleId("1");
        mapping.setPanelId("30");
        when(typeOfSamplePanelService.getTypeOfSamplePanelsForPanel("30")).thenReturn(List.of(mapping));
        when(typeOfSampleService.get("1")).thenReturn(sampleType("1", "Whole blood", true));

        List<Map<String, Object>> types = compatibleTypes(controller.getTestSampleTypes("", "30").getBody(), "panels");

        assertEquals(1, types.size());
        assertEquals("1", types.getFirst().get("id"));
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> compatibleTypes(Map<String, Object> body, String key) {
        List<Map<String, Object>> entries = (List<Map<String, Object>>) body.get(key);
        assertEquals(1, entries.size());
        return (List<Map<String, Object>>) entries.getFirst().get("compatibleSampleTypes");
    }

    private TypeOfSampleTest sampleTest(String typeOfSampleId) {
        TypeOfSampleTest mapping = new TypeOfSampleTest();
        mapping.setTypeOfSampleId(typeOfSampleId);
        return mapping;
    }

    private TypeOfSample sampleType(String id, String name, boolean active) {
        TypeOfSample type = new TypeOfSample();
        type.setId(id);
        type.setDescription(name);
        type.setActive(active);
        return type;
    }
}
