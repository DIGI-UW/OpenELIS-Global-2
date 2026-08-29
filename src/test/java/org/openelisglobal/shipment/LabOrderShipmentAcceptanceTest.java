package org.openelisglobal.shipment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.hl7.fhir.r4.model.SupplyDelivery;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.ExternalOrderStatus;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.dataexchange.fhir.service.FhirPersistanceService;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrder;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrderType;
import org.openelisglobal.dataexchange.service.order.ElectronicOrderService;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.shipment.dao.ShippingBoxDAO;
import org.openelisglobal.shipment.dto.ExpectedSpecimenDTO;
import org.openelisglobal.shipment.service.ShipmentReceptionService;
import org.openelisglobal.shipment.valueholder.BoxState;
import org.openelisglobal.shipment.valueholder.ShippingBox;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Integration test for the shipment specimen acceptance flow.
 *
 * Tests that when a sender stamps a specimen with a sample type abbreviation
 * that does NOT exist locally (e.g. "AIMI" for "Air Minum"), the reconcile flow
 * creates the missing TypeOfSample so that LabOrderSearchProvider can resolve
 * it and pre-populate the SamplePatientEntry form.
 *
 * These tests are expected to FAIL until the fix is implemented.
 */
public class LabOrderShipmentAcceptanceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private ShippingBoxDAO shippingBoxDAO;

    @Autowired
    private ElectronicOrderService electronicOrderService;

    @Autowired
    private FhirConfig fhirConfig;

    @Autowired
    private IStatusService statusService;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private TypeOfSampleService typeOfSampleService;

    @Autowired
    private OrganizationService organizationService;

    // Test data identifiers
    private static final String SPECIMEN_UUID = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeee01";
    private static final String SERVICE_REQUEST_UUID = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeee02";
    private static final String EXTERNAL_ORDER_ID = SERVICE_REQUEST_UUID;
    // The sender uses "AIMI" / "Air Minum" — this abbreviation does NOT exist
    // locally
    private static final String SENDER_SAMPLE_TYPE_CODE = "AIMI";
    private static final String SENDER_SAMPLE_TYPE_DISPLAY = "Air Minum";

    @Autowired
    private ShipmentReceptionService receptionService;

    private ShippingBox testBox;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        // Sibling fixtures truncate status_of_sample, wiping the base-dump
        // EXTERNAL_ORDER
        // statuses. This test loads no DbUnit dataset of its own, so re-seed "Entered"
        // and refresh the cache before getStatusID resolves it below.
        ensureExternalOrderEnteredStatus();
        statusService.refreshCache();

        // Electronic order in DB (as if imported by referral polling)
        ElectronicOrder eOrder = new ElectronicOrder();
        eOrder.setExternalId(EXTERNAL_ORDER_ID);
        eOrder.setStatusId(statusService.getStatusID(ExternalOrderStatus.Entered));
        eOrder.setOrderTimestamp(new Timestamp(System.currentTimeMillis()));
        eOrder.setData(SERVICE_REQUEST_UUID);
        eOrder.setSysUserId("1");
        eOrder.setType(ElectronicOrderType.FHIR);
        electronicOrderService.insert(eOrder);

        // Shipping box in DB (as if imported via ShipmentFhirImportService)
        testBox = new ShippingBox();
        testBox.setBoxId("BOX-TEST-ACCEPT-" + UUID.randomUUID().toString().substring(0, 8));
        testBox.setFhirUuid(UUID.randomUUID());
        testBox.setState(BoxState.IN_TRANSIT);
        testBox.setInbound(true);
        testBox.setOriginFacilityName("Sender Lab");
        testBox.setCreatedDate(new Timestamp(System.currentTimeMillis()));
        testBox.setSystemUserId(1);
        // Own a persisted, session-managed destination facility rather than
        // borrowing organizationService.getAll().get(0): the organization table is
        // neither base-seeded nor protected, so a sibling test's DBUnit load can
        // TRUNCATE ... RESTART IDENTITY it and leave getAll() returning a stale
        // (transient) instance whose row no longer exists — which makes the box
        // insert fail with TransientPropertyValueException under adverse ordering.
        testBox.setDestinationFacility(ensureDestinationFacility());

        Integer boxId = shippingBoxDAO.insert(testBox);
        testBox.setId(boxId);

        // Ensure fhirConfig returns a consistent system URI before building FHIR
        // resources
        when(fhirConfig.getOeFhirSystem()).thenReturn("http://openelis-global.org");

        // Override just the FHIR boundary on the Spring-managed service
        FhirPersistanceService fhirStub = mock(FhirPersistanceService.class);
        ReflectionTestUtils.setField(receptionService, "fhirPersistanceService", fhirStub);

        // SupplyDelivery with EXT_SPECIMEN
        SupplyDelivery sd = new SupplyDelivery();
        sd.setId(testBox.getFhirUuidAsString());
        sd.addExtension(new Extension("http://openelis.org/fhir/extension/shipment-specimen",
                new Reference("Specimen/" + SPECIMEN_UUID).setDisplay(SENDER_SAMPLE_TYPE_DISPLAY)));
        when(fhirStub.getSupplyDeliveryByUuid(testBox.getFhirUuidAsString())).thenReturn(Optional.of(sd));

        // ServiceRequest referencing the specimen
        ServiceRequest sr = new ServiceRequest();
        sr.setId(SERVICE_REQUEST_UUID);
        sr.addIdentifier().setSystem(fhirConfig.getOeFhirSystem()).setValue(EXTERNAL_ORDER_ID);
        sr.addSpecimen(new Reference("Specimen/" + SPECIMEN_UUID));
        when(fhirStub.getServiceRequestBySpecimenUuid(SPECIMEN_UUID)).thenReturn(Optional.of(sr));

        // Specimen with the sender's sample type coding
        org.hl7.fhir.r4.model.Specimen specimen = new org.hl7.fhir.r4.model.Specimen();
        specimen.setId(SPECIMEN_UUID);
        specimen.getType()
                .addCoding(new org.hl7.fhir.r4.model.Coding().setSystem(fhirConfig.getOeFhirSystem() + "/sampleType")
                        .setCode(SENDER_SAMPLE_TYPE_CODE).setDisplay(SENDER_SAMPLE_TYPE_DISPLAY));
        when(fhirStub.getSpecimenByUuid(SPECIMEN_UUID)).thenReturn(Optional.of(specimen));
    }

    // Idempotently ensure the EXTERNAL_ORDER "Entered" status exists (sibling
    // fixtures
    // truncate status_of_sample and don't reseed it). No id is hard-coded —
    // getStatusID
    // resolves whatever id this row gets after refreshCache.
    private void ensureExternalOrderEnteredStatus() throws SQLException {
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement("INSERT INTO clinlims.status_of_sample"
                        + " (id, name, status_type, code, description, is_active, lastupdated)"
                        + " SELECT nextval('clinlims.status_of_sample_seq'), 'Entered', 'EXTERNAL_ORDER', '1',"
                        + " 'Entered', 'Y', now() WHERE NOT EXISTS (SELECT 1 FROM clinlims.status_of_sample"
                        + " WHERE name = 'Entered' AND status_type = 'EXTERNAL_ORDER')")) {
            ps.executeUpdate();
        }
    }

    // Insert (or reuse) a dedicated destination-facility organization and return a
    // freshly fetched, session-managed instance. Persisting through the service
    // keeps the ShippingBox.destinationFacility FK pointing at a real, managed
    // row, so the box insert never trips TransientPropertyValueException the way
    // borrowing organizationService.getAll().get(0) can under adverse test order.
    private Organization ensureDestinationFacility() {
        Organization probe = new Organization();
        probe.setOrganizationName("ShipmentAcceptDestFacility");
        Organization existing = organizationService.getOrganizationByName(probe, true);
        if (existing != null && existing.getId() != null) {
            return organizationService.get(existing.getId());
        }
        // A sibling test's DBUnit load can seed organization rows at fixed ids via
        // TRUNCATE ... RESTART IDENTITY, leaving organization_seq behind the max id;
        // advance it so this insert's nextval() doesn't collide on org_pk.
        resyncSequence("clinlims.organization_seq", "clinlims.organization");
        Organization org = new Organization();
        org.setOrganizationName("ShipmentAcceptDestFacility");
        org.setIsActive(IActionConstants.YES);
        org.setMlsSentinelLabFlag(IActionConstants.NO);
        org.setSysUserId("1");
        String id = organizationService.insert(org);
        return organizationService.get(id);
    }

    /**
     * After reconciling a shipment specimen whose sample type abbreviation doesn't
     * exist locally, the system should create the missing TypeOfSample so that
     * LabOrderSearchProvider can resolve it and pre-populate the form.
     *
     * FAILS until the fix is implemented.
     */
    @Test
    public void reconcile_creates_missing_sample_type_from_specimen() {
        // Pre-condition: "AIMI" does not exist locally
        String beforeId = typeOfSampleService.getTypeOfSampleIdForLocalAbbreviation(SENDER_SAMPLE_TYPE_CODE);
        assertTrue("Pre-condition: sender abbreviation should not exist locally before reconcile",
                beforeId == null || beforeId.isBlank());

        // Act: reconcile the shipment
        receptionService.reconcileAndGetExpectedSpecimens(testBox.getId(), 1);

        // Assert: the sample type now exists locally with correct metadata
        String afterId = typeOfSampleService.getTypeOfSampleIdForLocalAbbreviation(SENDER_SAMPLE_TYPE_CODE);
        assertNotNull("After reconcile, sender sample type abbreviation '" + SENDER_SAMPLE_TYPE_CODE
                + "' should resolve to a local TypeOfSample", afterId);
        assertFalse("TypeOfSample ID should not be blank", afterId.isBlank());

        TypeOfSample created = typeOfSampleService.get(afterId);
        assertNotNull("Created TypeOfSample should be retrievable", created);
        assertEquals("Display name should match sender's specimen type display", SENDER_SAMPLE_TYPE_DISPLAY,
                created.getDescription());
        assertTrue("Created TypeOfSample should be active", created.getIsActive());
    }

    /**
     * Reconcile should resolve the specimen → ServiceRequest → electronic order
     * chain and return a PENDING specimen with the correct externalOrderNumber.
     */
    @Test
    public void reconcile_resolves_specimen_to_electronic_order() {
        List<ExpectedSpecimenDTO> results = receptionService.reconcileAndGetExpectedSpecimens(testBox.getId(), 1);

        assertFalse("Should find at least one expected specimen", results.isEmpty());

        ExpectedSpecimenDTO dto = results.get(0);
        assertEquals(SPECIMEN_UUID, dto.getSpecimenUuid());
        assertEquals(EXTERNAL_ORDER_ID, dto.getExternalOrderNumber());
        assertEquals(ExpectedSpecimenDTO.Status.PENDING, dto.getStatus());
    }
}
