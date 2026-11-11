package org.openelisglobal.dataexchange.fhir.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.hibernate.ObjectNotFoundException;
import org.hl7.fhir.r4.model.Task;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.dataexchange.fhir.exception.FhirLocalPersistingException;
import org.openelisglobal.dataexchange.fhir.service.FhirPersistanceServiceImpl.FhirOperations;
import org.openelisglobal.fhir.service.FhirCommonTransformService;
import org.openelisglobal.fhir.service.TaskTransformService;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.springframework.test.util.ReflectionTestUtils;

public class FhirSampleReplayTest {

    private final FhirTransformServiceImpl service = new FhirTransformServiceImpl();
    private final SampleService samples = mock(SampleService.class);
    private final TaskTransformService tasks = mock(TaskTransformService.class);
    private final FhirPersistanceService persistence = mock(FhirPersistanceService.class);

    @Before
    public void setUp() {
        ReflectionTestUtils.setField(service, "sampleService", samples);
        ReflectionTestUtils.setField(service, "taskTransformService", tasks);
        ReflectionTestUtils.setField(service, "fhirPersistanceService", persistence);
        ReflectionTestUtils.setField(service, "sampleHumanService", mock(SampleHumanService.class));
        ReflectionTestUtils.setField(service, "sampleItemService", mock(SampleItemService.class));
        ReflectionTestUtils.setField(service, "analysisService", mock(AnalysisService.class));
        ReflectionTestUtils.setField(service, "resultService", mock(ResultService.class));
        ReflectionTestUtils.setField(service, "common", mock(FhirCommonTransformService.class));
    }

    @Test
    public void missingLaterSamplePreventsAllTransformationAndUuidAssignment() {
        Sample first = new Sample();
        first.setId("1154");
        when(samples.get("1154")).thenReturn(first);
        when(samples.get("1157")).thenThrow(new ObjectNotFoundException("1157", "Sample"));

        assertThrows(ObjectNotFoundException.class,
                () -> service.transformPersistObjectsUnderSamples(List.of("1154", "1157")));

        assertNull("Preflight must finish before assigning any missing UUID", first.getFhirUuid());
        verifyZeroInteractions(tasks, persistence);
    }

    @Test
    public void onlySelectedSamplesAreTransformedWithExistingIdentities() throws Exception {
        Sample first = sample("1154", "47900000-0000-4000-8000-000000000001");
        Sample second = sample("1157", "47900000-0000-4000-8000-000000000002");

        service.transformPersistObjectsUnderSamples(List.of("1154", "1157"));

        ArgumentCaptor<FhirOperations> captured = ArgumentCaptor.forClass(FhirOperations.class);
        verify(persistence).createUpdateFhirResourcesInFhirStore(captured.capture());
        assertEquals(Set.of("Task/" + first.getFhirUuid(), "Task/" + second.getFhirUuid()),
                captured.getValue().updateResources.keySet());
        assertEquals(0, captured.getValue().createResources.size());
        assertEquals(UUID.fromString("47900000-0000-4000-8000-000000000001"), first.getFhirUuid());
        assertEquals(UUID.fromString("47900000-0000-4000-8000-000000000002"), second.getFhirUuid());
        verify(samples).get("1154");
        verify(samples).get("1157");
        verifyNoMoreInteractions(samples);
    }

    @Test
    public void persistenceFailurePropagatesToTheCaller() throws Exception {
        sample("1154", "47900000-0000-4000-8000-000000000001");
        FhirLocalPersistingException failure = new FhirLocalPersistingException("store unavailable");
        // Capture the assembled transaction while arranging a failing external store.
        ArgumentCaptor<FhirOperations> captured = ArgumentCaptor.forClass(FhirOperations.class);
        when(persistence.createUpdateFhirResourcesInFhirStore(captured.capture())).thenThrow(failure);

        assertSame(failure, assertThrows(FhirLocalPersistingException.class,
                () -> service.transformPersistObjectsUnderSamples(List.of("1154"))));
        assertEquals(Set.of("Task/47900000-0000-4000-8000-000000000001"), captured.getValue().updateResources.keySet());
    }

    private Sample sample(String id, String uuid) {
        Sample sample = new Sample();
        sample.setId(id);
        sample.setFhirUuid(UUID.fromString(uuid));
        when(samples.get(id)).thenReturn(sample);
        Task task = new Task();
        task.setId(uuid);
        when(tasks.transformToTask(sample)).thenReturn(task);
        return sample;
    }
}
