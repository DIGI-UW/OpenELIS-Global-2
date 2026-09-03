package org.openelisglobal.shipment.fhir;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * The scheduled consignment import is a deployment choice: unset, the poll must
 * not run at all; set, each pass hands off to the same import the Reception
 * screen's button uses.
 */
public class ShipmentFhirImportScheduleTest {

    @Test
    public void scheduledImportStaysOffUnlessTheDeploymentTurnsItOn() {
        ShipmentFhirImportService service = spy(new ShipmentFhirImportService());
        ReflectionTestUtils.setField(service, "self", service);
        doNothing().when(service).pollAndImportShipments();

        ReflectionTestUtils.setField(service, "importScheduled", false);
        service.importShipmentsOnSchedule();
        verify(service, never()).pollAndImportShipments();

        ReflectionTestUtils.setField(service, "importScheduled", true);
        service.importShipmentsOnSchedule();
        service.importShipmentsOnSchedule();
        verify(service, times(2)).pollAndImportShipments();
    }
}
