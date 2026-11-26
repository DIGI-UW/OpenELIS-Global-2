package org.openelisglobal.typeofsample.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.Test;
import org.openelisglobal.common.management.controller.rest.SampleTypeManagementRestController;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.openelisglobal.typeofsample.valueholder.TypeOfSampleTest;
import org.springframework.test.util.ReflectionTestUtils;

public class SampleTypeManagementCountTest {

    @Test
    public void listAndDetailCountAssignmentsWithoutLoadingTestCatalogMetadata() {
        TypeOfSampleTestService assignments = mock(TypeOfSampleTestService.class);
        TestService tests = mock(TestService.class);
        TypeOfSampleServiceImpl service = spy(new TypeOfSampleServiceImpl());
        ReflectionTestUtils.setField(service, "typeOfSampleTestService", assignments);
        ReflectionTestUtils.setField(service, "testService", tests);
        TypeOfSample populated = sampleType("1");
        TypeOfSample empty = sampleType("2");
        doReturn(List.of(populated, empty)).when(service).getAllTypeOfSamplesSortOrdered();
        doReturn(populated).when(service).getTypeOfSampleById("1");
        when(assignments.getTypeOfSampleTestsForSampleType("1"))
                .thenReturn(List.of(new TypeOfSampleTest(), new TypeOfSampleTest()));
        when(assignments.getTypeOfSampleTestsForSampleType("2")).thenReturn(List.of());
        SampleTypeManagementRestController controller = new SampleTypeManagementRestController();
        ReflectionTestUtils.setField(controller, "typeOfSampleService", service);

        var rows = controller.getAllSampleTypes().getBody().getData();
        assertEquals(2, rows.get(0).getTestCount());
        assertEquals(0, rows.get(1).getTestCount());
        assertEquals(2, controller.getSampleType("1").getBody().getData().getTestCount());
        verifyZeroInteractions(tests);
    }

    private TypeOfSample sampleType(String id) {
        TypeOfSample sampleType = new TypeOfSample();
        sampleType.setId(id);
        sampleType.setDescription("Specimen " + id);
        sampleType.setDomain("H");
        sampleType.setIsActive(true);
        return sampleType;
    }
}
