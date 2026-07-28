package org.openelisglobal.microbiology.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.openelisglobal.alert.event.AlertAcknowledgedEvent;
import org.openelisglobal.alert.event.AlertResolvedEvent;
import org.openelisglobal.alert.valueholder.Alert;
import org.openelisglobal.alert.valueholder.AlertType;

public class MicrobiologyCriticalAlertLifecycleListenerTest {

    @Test
    public void alertAcknowledgementSynchronizesTheClinicalRecord() {
        MicroCriticalCommunicationService service = org.mockito.Mockito.mock(MicroCriticalCommunicationService.class);
        MicrobiologyCriticalAlertLifecycleListener listener = new MicrobiologyCriticalAlertLifecycleListener(service);
        Alert alert = microbiologyAlert();

        listener.onAlertAcknowledged(new AlertAcknowledgedEvent(this, alert, 42L));

        ArgumentCaptor<String> communicationId = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> performedBy = ArgumentCaptor.forClass(String.class);
        verify(service).synchronizeAcknowledgementFromAlert(communicationId.capture(), performedBy.capture());
        assertEquals("comm-1", communicationId.getValue());
        assertEquals("42", performedBy.getValue());
    }

    @Test
    public void alertResolutionClosesTheClinicalRecord() {
        MicroCriticalCommunicationService service = org.mockito.Mockito.mock(MicroCriticalCommunicationService.class);
        MicrobiologyCriticalAlertLifecycleListener listener = new MicrobiologyCriticalAlertLifecycleListener(service);
        Alert alert = microbiologyAlert();

        listener.onAlertResolved(new AlertResolvedEvent(this, alert, 42L, "Follow-up complete"));

        ArgumentCaptor<String> communicationId = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> resolution = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> performedBy = ArgumentCaptor.forClass(String.class);
        verify(service).synchronizeResolutionFromAlert(communicationId.capture(), resolution.capture(),
                performedBy.capture());
        assertEquals("comm-1", communicationId.getValue());
        assertEquals("Follow-up complete", resolution.getValue());
        assertEquals("42", performedBy.getValue());
    }

    private Alert microbiologyAlert() {
        Alert alert = new Alert();
        alert.setAlertType(AlertType.MICROBIOLOGY_CRITICAL);
        alert.setAlertEntityType("MicrobiologyCriticalCommunication");
        alert.setAlertEntityRef("comm-1");
        return alert;
    }
}
