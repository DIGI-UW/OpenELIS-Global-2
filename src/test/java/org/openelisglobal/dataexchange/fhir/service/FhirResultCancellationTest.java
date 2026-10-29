package org.openelisglobal.dataexchange.fhir.service;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.List;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.StringType;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.openelisglobal.dataexchange.fhir.service.FhirPersistanceServiceImpl.FhirOperations;
import org.openelisglobal.fhir.service.FhirCommonTransformService;
import org.openelisglobal.fhir.service.ObservationTransformService;
import org.openelisglobal.result.action.util.ResultsUpdateDataSet;
import org.springframework.test.util.ReflectionTestUtils;

public class FhirResultCancellationTest {
    @Test
    public void clearedResultUpdatesExistingObservationWithoutReloadingDeletedResultOrKeepingItsValue()
            throws Exception {
        var service = new FhirTransformServiceImpl();
        var store = mock(FhirPersistanceService.class);
        var transformer = mock(ObservationTransformService.class);
        ReflectionTestUtils.setField(service, "fhirPersistanceService", store);
        ReflectionTestUtils.setField(service, "common", mock(FhirCommonTransformService.class));
        ReflectionTestUtils.setField(service, "observationTransformService", transformer);
        var snapshot = new Observation();
        snapshot.setId("existing-result-uuid");
        snapshot.getCode().setText("Cleared component");
        snapshot.setStatus(Observation.ObservationStatus.PRELIMINARY);
        snapshot.setValue(new StringType("stale result"));
        snapshot.addComponent().setValue(new StringType("stale component"));
        service.transformPersistResultsEntryFhirObjects(mock(ResultsUpdateDataSet.class), List.of(snapshot));
        var captured = ArgumentCaptor.forClass(FhirOperations.class);
        verify(store).createUpdateFhirResourcesInFhirStore(captured.capture());
        assertEquals(0, captured.getValue().createResources.size());
        assertEquals(1, captured.getValue().updateResources.size());
        var cancelled = (Observation) captured.getValue().updateResources.get("Observation/existing-result-uuid");
        assertEquals(Observation.ObservationStatus.CANCELLED, cancelled.getStatus());
        assertEquals("Cleared component", cancelled.getCode().getText());
        assertFalse(cancelled.hasValue());
        assertFalse(cancelled.hasComponent());
        assertEquals("stale result", snapshot.getValueStringType().getValue());
        verifyZeroInteractions(transformer);
    }
}
