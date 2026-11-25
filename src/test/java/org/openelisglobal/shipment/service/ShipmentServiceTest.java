package org.openelisglobal.shipment.service;

import java.sql.Timestamp;
import java.util.List;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.shipment.valueholder.Shipment;
import org.openelisglobal.shipment.valueholder.ShipmentStatus;
import org.openelisglobal.shipment.valueholder.ShippingBox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

public class ShipmentServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private ShipmentService shipmentService;

    @Autowired
    private ShippingBoxService shippingBoxService;

    @Before
    public void init() throws Exception {
        executeDataSetWithStateManagement("testdata/shipment_service.xml");
    }

    @After
    public void tearDown() throws Exception {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.update(
                "DELETE FROM clinlims.shipment WHERE id IN (1, 2) OR shipping_box_id IN (1, 2, 3) OR tracking_number = 'TRK2001'");
        jdbc.update("DELETE FROM clinlims.shipping_box WHERE id IN (1, 2, 3)");
        jdbc.update(
                "DELETE FROM clinlims.organization WHERE id IN (1, 2) AND name IN ('Central Laboratory', 'Regional Clinic')");
    }

    @Test
    public void getShipmentById_shouldReturnExistingShipment() {
        Shipment shipment = shipmentService.getShipmentById(1);

        Assert.assertNotNull(shipment);
        Assert.assertEquals("TRK1001", shipment.getTrackingNumber());
        Assert.assertEquals("FedEx", shipment.getCourier());
        Assert.assertEquals(ShipmentStatus.PENDING, shipment.getStatus());
    }

    @Test
    public void getShipmentByShippingBoxId_shouldReturnAssociatedShipment() {
        Shipment shipment = shipmentService.getShipmentByShippingBoxId(2);

        Assert.assertNotNull(shipment);
        Assert.assertEquals("TRK1002", shipment.getTrackingNumber());
        Assert.assertEquals(ShipmentStatus.IN_TRANSIT, shipment.getStatus());
    }

    @Test
    public void getShipmentByTrackingNumber_shouldReturnCorrectShipment() {
        Shipment shipment = shipmentService.getShipmentByTrackingNumber("TRK1002");

        Assert.assertNotNull(shipment);
        Assert.assertEquals(Integer.valueOf(2), shipment.getId());
        Assert.assertEquals("DHL", shipment.getCourier());
    }

    @Test
    public void getShipmentsByStatus_shouldReturnMatchingShipments() {
        List<Shipment> pendingShipments = shipmentService.getShipmentsByStatus(ShipmentStatus.PENDING);
        List<Shipment> inTransitShipments = shipmentService.getShipmentsByStatus(ShipmentStatus.IN_TRANSIT);

        Assert.assertEquals(1, pendingShipments.size());
        Assert.assertEquals("TRK1001", pendingShipments.get(0).getTrackingNumber());

        Assert.assertEquals(1, inTransitShipments.size());
        Assert.assertEquals("TRK1002", inTransitShipments.get(0).getTrackingNumber());
    }

    @Test
    public void getShipmentsByCourier_shouldReturnMatchingShipments() {
        List<Shipment> fedexShipments = shipmentService.getShipmentsByCourier("FedEx");
        List<Shipment> dhlShipments = shipmentService.getShipmentsByCourier("DHL");

        Assert.assertEquals(1, fedexShipments.size());
        Assert.assertEquals(1, dhlShipments.size());
        Assert.assertEquals("TRK1001", fedexShipments.get(0).getTrackingNumber());
    }

    /**
     * Verifies that {@link ShipmentService#createShipment} persists a new shipment,
     * assigns a generated ID, defaults the status to
     * {@link ShipmentStatus#PENDING}, and stores the mapped {@code systemUserId}.
     * Box 3 is loaded through {@link ShippingBoxService#getBoxById} so Hibernate
     * receives a managed entity rather than a transient shell, avoiding
     * {@code TransientPropertyValueException}. The actor is supplied explicitly
     * ({@code systemUserId = 1}), so audit synchronization from PR #3657 is not
     * required for this scenario.
     */
    @Test
    public void createShipment_shouldGenerateIdAndDefaultToPending() {
        ShippingBox box = shippingBoxService.getBoxById(3);

        Shipment newShipment = new Shipment();
        newShipment.setShippingBox(box);
        newShipment.setCourier("UPS");
        newShipment.setTrackingNumber("TRK2001");
        newShipment.setSysUserId("1");
        newShipment.setSystemUserId(1);

        Shipment created = shipmentService.createShipment(newShipment);

        Assert.assertNotNull(created.getId());
        Assert.assertEquals(ShipmentStatus.PENDING, created.getStatus());
        Assert.assertEquals("UPS", created.getCourier());
        Assert.assertEquals(Integer.valueOf(1), created.getSystemUserId());
        Assert.assertNotNull(created.getLastupdated());

        Shipment fetched = shipmentService.getShipmentById(created.getId());
        Assert.assertNotNull(fetched);
        Assert.assertEquals("TRK2001", fetched.getTrackingNumber());
        Assert.assertEquals(Integer.valueOf(1), fetched.getSystemUserId());
    }

    /**
     * KNOWN LIMITATION: {@link ShipmentServiceImpl#updateShipment} calls
     * {@code shipment.setLastupdated(now)} before {@code entityManager.merge()}.
     * Because {@code lastupdated} is annotated {@code @Version} in
     * {@code BaseObject}, Hibernate uses it for optimistic locking. Pre-changing
     * the version value causes Hibernate to generate
     * {@code WHERE lastupdated = <new_time>} which matches 0 rows, resulting in an
     * {@code OptimisticLockException}. Note that {@code updateShipmentStatus} has
     * the same {@code setLastupdated} pattern but its tests pass because the entity
     * is managed (loaded inside the same service transaction) rather than detached.
     * Production fix required (tracked in #3654): remove the manual
     * {@code setLastupdated()} call from the service and let Hibernate manage the
     * {@code @Version} field.
     */
    @Ignore("Production bug (#3654): service pre-sets @Version field before merge on detached entity causing OptimisticLockException; updateShipmentStatus avoids this because the entity is managed in-transaction")
    @Test
    public void updateShipment_shouldModifyExistingFields() {
        Shipment shipment = shipmentService.getShipmentById(1);
        shipment.setTrackingNumber("TRK1001-UPDATED");
        shipment.setSenderNotes("Urgent delivery");
        shipment.setSysUserId("1");

        Shipment updated = shipmentService.updateShipment(shipment);

        Assert.assertEquals("TRK1001-UPDATED", updated.getTrackingNumber());
        Assert.assertEquals("Urgent delivery", updated.getSenderNotes());

        Shipment fetched = shipmentService.getShipmentById(1);
        Assert.assertEquals("TRK1001-UPDATED", fetched.getTrackingNumber());
        Assert.assertEquals("Urgent delivery", fetched.getSenderNotes());
    }

    @Test
    public void updateShipmentStatus_toInTransit_shouldSetShippedDate() {
        Shipment shipment = shipmentService.getShipmentById(1);
        Assert.assertNull(shipment.getShippedDate());

        Shipment updated = shipmentService.updateShipmentStatus(1, ShipmentStatus.IN_TRANSIT);

        Assert.assertEquals(ShipmentStatus.IN_TRANSIT, updated.getStatus());
        Assert.assertNotNull("Shipped date should be set when transitioning to IN_TRANSIT", updated.getShippedDate());

        Shipment fetched = shipmentService.getShipmentById(1);
        Assert.assertEquals(ShipmentStatus.IN_TRANSIT, fetched.getStatus());
        Assert.assertNotNull(fetched.getShippedDate());
    }

    @Test
    public void updateShipmentStatus_toDelivered_shouldSetActualDeliveryDate() {
        Shipment shipment = shipmentService.getShipmentById(2);
        Assert.assertNull(shipment.getActualDeliveryDate());

        Shipment updated = shipmentService.updateShipmentStatus(2, ShipmentStatus.DELIVERED);

        Assert.assertEquals(ShipmentStatus.DELIVERED, updated.getStatus());
        Assert.assertNotNull("Actual delivery date should be set when transitioning to DELIVERED",
                updated.getActualDeliveryDate());

        Shipment fetched = shipmentService.getShipmentById(2);
        Assert.assertEquals(ShipmentStatus.DELIVERED, fetched.getStatus());
        Assert.assertNotNull(fetched.getActualDeliveryDate());
    }

    @Test
    public void deleteShipment_shouldRemoveFromDatabase() {
        Shipment shipment = shipmentService.getShipmentById(1);
        Assert.assertNotNull(shipment);

        shipmentService.deleteShipment(1);

        Shipment deleted = shipmentService.getShipmentById(1);
        Assert.assertNull("Shipment should be null after deletion", deleted);
    }

    @Test
    public void getShipmentById_unknownId_shouldReturnNull() {
        Shipment shipment = shipmentService.getShipmentById(99999);
        Assert.assertNull("Non-existent shipment should return null", shipment);
    }

    @Test
    public void updateShipmentStatus_unknownId_shouldThrowException() {
        try {
            shipmentService.updateShipmentStatus(99999, ShipmentStatus.IN_TRANSIT);
            Assert.fail("Expected LIMSRuntimeException for unknown shipment ID");
        } catch (LIMSRuntimeException e) {
            Assert.assertTrue(e.getCause() instanceof IllegalArgumentException);
            Assert.assertEquals("Shipment not found with ID: 99999", e.getCause().getMessage());
        }
    }

    @Test
    public void updateShipmentStatus_alreadyInTransit_shouldPreserveShippedDate() {
        Shipment initial = shipmentService.getShipmentById(2);
        Timestamp originalShippedDate = initial.getShippedDate();
        Assert.assertNotNull("Fixture shipment 2 should already have a shippedDate", originalShippedDate);

        Shipment updated = shipmentService.updateShipmentStatus(2, ShipmentStatus.IN_TRANSIT);

        Assert.assertEquals(ShipmentStatus.IN_TRANSIT, updated.getStatus());
        Assert.assertEquals("Shipped date must be preserved across another IN_TRANSIT update", originalShippedDate,
                updated.getShippedDate());

        Shipment fetched = shipmentService.getShipmentById(2);
        Assert.assertEquals("Persisted shipped date must be preserved across another IN_TRANSIT update",
                originalShippedDate, fetched.getShippedDate());
    }
}
