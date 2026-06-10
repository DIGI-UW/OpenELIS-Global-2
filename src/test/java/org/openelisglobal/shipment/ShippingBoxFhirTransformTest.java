package org.openelisglobal.shipment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.UUID;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.SupplyDelivery;
import org.hl7.fhir.r4.model.SupplyDelivery.SupplyDeliveryStatus;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.shipment.fhir.ShippingBoxFhirTransform;
import org.openelisglobal.shipment.valueholder.BoxState;
import org.openelisglobal.shipment.valueholder.ShippingBox;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Guards that SupplyDelivery.destination carries no Organization ref
 * (R4/HAPI-0931) — UUID goes in an extension.
 */
public class ShippingBoxFhirTransformTest extends BaseWebContextSensitiveTest {

    private static final String EXT_DESTINATION_ORG = "http://openelis.org/fhir/extension/shipment-destination-org";

    @Autowired
    private ShippingBoxFhirTransform shippingBoxFhirTransform;

    @Test
    public void transformToSupplyDelivery_carriesDestinationOrgInExtensionNotReference() {
        UUID orgUuid = UUID.fromString("11111111-2222-3333-4444-555555555555");
        Organization destination = new Organization();
        destination.setOrganizationName("Test Org");
        destination.setFhirUuid(orgUuid);

        ShippingBox box = new ShippingBox();
        box.setId(999999); // non-existent id → no box sample items, isolates destination logic
        box.setFhirUuid(UUID.randomUUID());
        box.setBoxId("BOX-TEST-0001");
        box.setState(BoxState.SENT);
        box.setDestinationFacility(destination);

        SupplyDelivery supplyDelivery = shippingBoxFhirTransform.transformToSupplyDelivery(box);

        assertEquals(SupplyDeliveryStatus.INPROGRESS, supplyDelivery.getStatus());
        assertEquals("BOX-TEST-0001", supplyDelivery.getIdentifierFirstRep().getValue());

        // no Organization reference (the HAPI-0931 regression), display only
        assertNotNull(supplyDelivery.getDestination());
        assertFalse("destination must not carry a resource reference", supplyDelivery.getDestination().hasReference());
        assertEquals("Test Org", supplyDelivery.getDestination().getDisplay());

        // org UUID lives in the extension
        Extension destOrgExt = supplyDelivery.getExtensionByUrl(EXT_DESTINATION_ORG);
        assertNotNull("destination-org extension must be present", destOrgExt);
        assertTrue(destOrgExt.getValue() instanceof StringType);
        assertEquals(orgUuid.toString(), ((StringType) destOrgExt.getValue()).getValue());
    }

    @Test
    public void transformToSupplyDelivery_noDestinationExtensionWhenOrgHasNoFhirUuid() {
        Organization destination = new Organization();
        destination.setOrganizationName("Unmatched Org");
        // no fhirUuid set

        ShippingBox box = new ShippingBox();
        box.setId(999998);
        box.setFhirUuid(UUID.randomUUID());
        box.setBoxId("BOX-TEST-0002");
        box.setState(BoxState.SENT);
        box.setDestinationFacility(destination);

        SupplyDelivery supplyDelivery = shippingBoxFhirTransform.transformToSupplyDelivery(box);

        assertFalse(supplyDelivery.getDestination().hasReference());
        assertEquals("Unmatched Org", supplyDelivery.getDestination().getDisplay());
        assertEquals("no UUID → no destination-org extension", null,
                supplyDelivery.getExtensionByUrl(EXT_DESTINATION_ORG));
    }
}
