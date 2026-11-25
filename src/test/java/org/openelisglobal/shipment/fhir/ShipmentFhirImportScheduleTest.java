package org.openelisglobal.shipment.fhir;

import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.Test;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.dataexchange.fhir.FhirUtil;
import org.openelisglobal.shipment.dao.ShippingBoxDAO;
import org.openelisglobal.shipment.valueholder.BoxState;
import org.openelisglobal.shipment.valueholder.ShippingBox;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * The scheduled consignment import is a deployment choice: unset, the poll must
 * not run at all; set, each pass hands off to the same import the Reception
 * screen's button uses. The delivery reconcile reads this installation's own
 * FHIR store, so it waits for one to be configured and for a box to be in
 * flight.
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

    @Test
    public void reconcileWaitsForALocalFhirStore() {
        FhirUtil fhirUtil = mock(FhirUtil.class);
        ShippingBoxDAO boxes = mock(ShippingBoxDAO.class);
        ShipmentFhirImportService service = reconciler(fhirUtil, boxes, "");
        when(boxes.findByState(BoxState.SENT)).thenReturn(List.of(inFlightBox()));

        service.reconcileDeliveredShipments();

        verify(fhirUtil, never()).getLocalFhirClient();
    }

    @Test
    public void reconcileWithNothingInFlightOpensNoClient() {
        FhirUtil fhirUtil = mock(FhirUtil.class);
        ShippingBoxDAO boxes = mock(ShippingBoxDAO.class);
        ShipmentFhirImportService service = reconciler(fhirUtil, boxes, "https://fhir.example.org/fhir");

        service.reconcileDeliveredShipments();

        verify(fhirUtil, never()).getLocalFhirClient();
    }

    @Test
    public void reconcileReadsTheOwnStoreForABoxInFlight() {
        FhirUtil fhirUtil = mock(FhirUtil.class);
        ShippingBoxDAO boxes = mock(ShippingBoxDAO.class);
        ShipmentFhirImportService service = reconciler(fhirUtil, boxes, "https://fhir.example.org/fhir");
        when(boxes.findByState(BoxState.IN_TRANSIT)).thenReturn(List.of(inFlightBox()));
        when(fhirUtil.getLocalFhirClient()).thenReturn(mock(IGenericClient.class, RETURNS_DEEP_STUBS));

        service.reconcileDeliveredShipments();

        verify(fhirUtil).getLocalFhirClient();
    }

    private static ShipmentFhirImportService reconciler(FhirUtil fhirUtil, ShippingBoxDAO boxes, String localStore) {
        FhirConfig config = mock(FhirConfig.class);
        when(config.getLocalFhirStorePath()).thenReturn(localStore);
        ShipmentFhirImportService service = new ShipmentFhirImportService();
        ReflectionTestUtils.setField(service, "fhirConfig", config);
        ReflectionTestUtils.setField(service, "fhirUtil", fhirUtil);
        ReflectionTestUtils.setField(service, "shippingBoxDAO", boxes);
        ReflectionTestUtils.setField(service, "remoteStoreUpdateStatus", Optional.of(true));
        return service;
    }

    private static ShippingBox inFlightBox() {
        ShippingBox box = new ShippingBox();
        box.setFhirUuid(UUID.randomUUID());
        return box;
    }
}
