package org.openelisglobal.shipment.fhir;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.SupplyDelivery;
import org.hl7.fhir.r4.model.SupplyDelivery.SupplyDeliveryStatus;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.dataexchange.fhir.FhirUtil;
import org.openelisglobal.dataexchange.fhir.service.FhirPersistanceService;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.shipment.dao.ShippingBoxDAO;
import org.openelisglobal.shipment.valueholder.BoxState;
import org.openelisglobal.shipment.valueholder.ShippingBox;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service responsible for importing SupplyDelivery resources from remote FHIR
 * servers and creating/updating local ShippingBox entries for reception
 * reconciliation.
 *
 * Follows the same polling pattern as FhirApiWorkFlowServiceImpl for Task
 * resources.
 */
@Service
public class ShipmentFhirImportService {

    private static final String OPENELIS_SHIPMENT_SYSTEM = "http://openelis.org/shipment";
    private static final String EXT_TEMPERATURE = "http://openelis.org/fhir/extension/shipment-temperature";
    private static final String EXT_CAPACITY = "http://openelis.org/fhir/extension/shipment-capacity";
    private static final String EXT_NOTES = "http://openelis.org/fhir/extension/shipment-notes";
    private static final String EXT_DESTINATION_ORG = "http://openelis.org/fhir/extension/shipment-destination-org";
    private static final String EXT_SOURCE_ORG = "http://openelis.org/fhir/extension/shipment-source-org";

    @Autowired
    private FhirConfig fhirConfig;

    @Autowired
    private FhirUtil fhirUtil;

    @Autowired
    private ShippingBoxDAO shippingBoxDAO;

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private SystemUserService systemUserService;

    @Autowired
    private FhirPersistanceService fhirPersistanceService;

    /**
     * Poll all configured remote FHIR servers for SupplyDelivery resources with
     * status in-progress (SENT/IN_TRANSIT boxes). Import them as local ShippingBox
     * with state IN_TRANSIT for reception. Runs asynchronously.
     */
    @Async
    @Transactional
    public void pollAndImportShipments() {
        importShipments();
    }

    /**
     * Synchronous variant of {@link #pollAndImportShipments()} returning the number
     * of boxes imported.
     */
    @Transactional
    public int importShipments() {
        int totalImported = 0;
        for (String remoteStorePath : fhirConfig.getRemoteStorePaths()) {
            if (remoteStorePath == null || remoteStorePath.isBlank()) {
                continue;
            }
            try {
                totalImported += importFromRemote(remoteStorePath);
            } catch (Exception e) {
                LogEvent.logError(this.getClass().getSimpleName(), "importShipments",
                        "Error importing shipments from: " + remoteStorePath + " - " + e.getMessage());
            }
        }
        if (totalImported > 0) {
            LogEvent.logInfo(this.getClass().getSimpleName(), "importShipments",
                    "Total shipments imported: " + totalImported);
        }
        return totalImported;
    }

    /**
     * Import SupplyDelivery resources from a single remote FHIR server.
     */
    private int importFromRemote(String remoteStorePath) {
        IGenericClient fhirClient = fhirUtil.getFhirClient(remoteStorePath);
        int imported = 0;

        // Search for SupplyDelivery with status=in-progress (SENT boxes)
        Bundle searchBundle = fhirClient.search().forResource(SupplyDelivery.class).returnBundle(Bundle.class)
                .where(SupplyDelivery.STATUS.exactly().code(SupplyDeliveryStatus.INPROGRESS.toCode())).execute();

        List<SupplyDelivery> allDeliveries = new ArrayList<>();
        extractSupplyDeliveries(searchBundle, allDeliveries);

        // Handle pagination
        while (searchBundle.getLink(IBaseBundle.LINK_NEXT) != null) {
            searchBundle = fhirClient.loadPage().next(searchBundle).execute();
            extractSupplyDeliveries(searchBundle, allDeliveries);
        }

        for (SupplyDelivery delivery : allDeliveries) {
            if (importSupplyDelivery(delivery)) {
                imported++;
            }
        }

        if (imported > 0) {
            LogEvent.logInfo(this.getClass().getSimpleName(), "importFromRemote",
                    "Imported " + imported + " shipments from " + remoteStorePath);
        }

        return imported;
    }

    private void extractSupplyDeliveries(Bundle bundle, List<SupplyDelivery> target) {
        if (bundle == null || !bundle.hasEntry()) {
            return;
        }
        for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
            if (entry.hasResource() && entry.getResource() instanceof SupplyDelivery sd) {
                target.add(sd);
            }
        }
    }

    /**
     * Import a single SupplyDelivery resource as a local ShippingBox. Skips if the
     * box already exists locally (matched by FHIR UUID or box ID).
     *
     * @return true if a new box was created
     */
    @Transactional
    public boolean importSupplyDelivery(SupplyDelivery delivery) {
        try {
            // Extract box identifier
            String boxId = extractBoxId(delivery);
            if (boxId == null || boxId.isBlank()) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "importSupplyDelivery",
                        "SupplyDelivery has no box ID identifier, skipping");
                return false;
            }

            // Check if already imported — by FHIR UUID
            UUID fhirUuid = extractFhirUuid(delivery);
            if (fhirUuid != null) {
                ShippingBox existing = shippingBoxDAO.findByFhirUuid(fhirUuid);
                if (existing != null) {
                    return false; // Already imported
                }
            }

            // Check by box ID
            ShippingBox existingByBoxId = shippingBoxDAO.findByBoxId(boxId);
            if (existingByBoxId != null) {
                return false; // Already exists
            }

            // Create new local ShippingBox from SupplyDelivery
            ShippingBox box = new ShippingBox();
            box.setBoxId(boxId);
            if (fhirUuid != null) {
                box.setFhirUuid(fhirUuid);
            }
            box.setState(BoxState.IN_TRANSIT);
            box.setCreatedDate(new Timestamp(System.currentTimeMillis()));
            box.setSystemUserId(getAutomatedImportUserId());

            // Temperature
            String temperature = extractExtensionString(delivery, EXT_TEMPERATURE);
            if (temperature != null) {
                box.setTemperatureRequirement(temperature);
            }

            // Capacity
            Integer capacity = extractExtensionInteger(delivery, EXT_CAPACITY);
            if (capacity != null) {
                box.setCapacity(capacity);
            }

            // Notes
            String notes = extractExtensionString(delivery, EXT_NOTES);
            if (notes != null) {
                box.setNotes(notes);
            }

            // Direction + origin
            box.setInbound(true);
            box.setOriginFacilityName(extractExtensionString(delivery, EXT_SOURCE_ORG));

            // Specimen count from supplied item quantity
            if (delivery.hasSuppliedItem() && delivery.getSuppliedItem().hasQuantity()) {
                box.setActualSampleCount(delivery.getSuppliedItem().getQuantity().getValue().intValue());
            }

            // Sent date from occurrence
            if (delivery.hasOccurrenceDateTimeType()) {
                box.setSentDate(new Timestamp(delivery.getOccurrenceDateTimeType().getValue().getTime()));
            }

            // Destination org UUID travels in an extension (R4 forbids an Organization ref
            // here).
            String destinationUuid = extractExtensionString(delivery, EXT_DESTINATION_ORG);
            if (destinationUuid == null || destinationUuid.isBlank()) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "importSupplyDelivery",
                        "Box " + boxId + " has no destination organization UUID, skipping import");
                return false;
            }

            // Recognition is config-driven, like referral's Task.owner vs
            // remote.source.identifier.
            List<String> selfIdentifiers = fhirConfig.getRemoteStoreIdentifier();
            if (selfIdentifiers.isEmpty()) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "importSupplyDelivery",
                        "remote.source.identifier is not configured; cannot determine box ownership, skipping import");
                return false;
            }
            if (!matchesSelfIdentity(destinationUuid, selfIdentifiers)) {
                return false; // not destined for this lab
            }

            // Resolve the destination org by shared UUID; materialize it if absent (like
            // referral's
            // referring org), so no manual provisioning is needed.
            Organization destinationOrg = organizationService.getOrganizationByFhirId(destinationUuid);
            if (destinationOrg == null) {
                destinationOrg = createDestinationOrganization(destinationUuid,
                        delivery.hasDestination() ? delivery.getDestination().getDisplay() : null);
            }
            box.setDestinationFacility(destinationOrg);

            shippingBoxDAO.insert(box);
            LogEvent.logInfo(this.getClass().getSimpleName(), "importSupplyDelivery",
                    "Imported shipment box: " + boxId + " with state IN_TRANSIT");

            // Store the SupplyDelivery locally so reception can read its EXT_SPECIMEN refs.
            persistSupplyDeliveryLocally(delivery);

            return true;
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "importSupplyDelivery",
                    "Error importing SupplyDelivery: " + e.getMessage());
            return false;
        }
    }

    /**
     * Store the SupplyDelivery locally (PUT under its own id) so reception can read
     * EXT_SPECIMEN.
     */
    private void persistSupplyDeliveryLocally(SupplyDelivery delivery) {
        try {
            String id = delivery.getIdElement().getIdPart();
            if (id == null || id.isBlank()) {
                return;
            }
            Map<String, Resource> resources = new java.util.HashMap<>();
            resources.put(id, delivery);
            fhirPersistanceService.updateFhirResourcesInFhirStore(resources);
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "persistSupplyDeliveryLocally",
                    "Could not persist SupplyDelivery to local store: " + e.getMessage());
        }
    }

    private String extractBoxId(SupplyDelivery delivery) {
        if (delivery.hasIdentifier()) {
            for (var identifier : delivery.getIdentifier()) {
                if (identifier.hasSystem() && identifier.getSystem().equals(OPENELIS_SHIPMENT_SYSTEM + "/box-id")) {
                    return identifier.getValue();
                }
            }
            // Fallback: first identifier value
            if (!delivery.getIdentifier().isEmpty()) {
                return delivery.getIdentifier().get(0).getValue();
            }
        }
        return null;
    }

    private UUID extractFhirUuid(SupplyDelivery delivery) {
        try {
            String id = delivery.getIdElement().getIdPart();
            if (id != null && !id.isBlank()) {
                return UUID.fromString(id);
            }
        } catch (IllegalArgumentException e) {
            // Not a valid UUID, ignore
        }
        return null;
    }

    private String extractExtensionString(SupplyDelivery delivery, String url) {
        Extension ext = delivery.getExtensionByUrl(url);
        if (ext != null && ext.hasValue() && ext.getValue() instanceof StringType st) {
            return st.getValue();
        }
        return null;
    }

    private Integer extractExtensionInteger(SupplyDelivery delivery, String url) {
        Extension ext = delivery.getExtensionByUrl(url);
        if (ext != null && ext.hasValue() && ext.getValue() instanceof IntegerType it) {
            return it.getValue();
        }
        return null;
    }

    /**
     * Create the destination org from the SupplyDelivery's shared UUID + display
     * name.
     */
    private Organization createDestinationOrganization(String fhirUuid, String displayName) {
        Organization org = new Organization();
        org.setOrganizationName(displayName != null && !displayName.isBlank() ? displayName : fhirUuid);
        org.setFhirUuid(UUID.fromString(fhirUuid));
        org.setIsActive(IActionConstants.YES);
        org.setMlsLabFlag(IActionConstants.NO);
        org.setMlsSentinelLabFlag(IActionConstants.NO);
        organizationService.save(org);
        LogEvent.logInfo(this.getClass().getSimpleName(), "createDestinationOrganization",
                "Materialized destination organization " + org.getOrganizationName() + " (" + fhirUuid + ")");
        return org;
    }

    /**
     * Resolve the system user ID for automated FHIR import operations. Looks up the
     * "admin" login user via SystemUserService. Falls back to user ID 1 if lookup
     * fails (consistent with other automated services in the codebase).
     */
    private Integer getAutomatedImportUserId() {
        try {
            SystemUser systemUser = systemUserService.getDataForLoginUser("admin");
            if (systemUser != null && systemUser.getId() != null) {
                return Integer.parseInt(systemUser.getId());
            }
        } catch (Exception e) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "getAutomatedImportUserId",
                    "Could not resolve system user for automated import, falling back to ID 1: " + e.getMessage());
        }
        return 1;
    }

    /**
     * True if the destination UUID matches a configured self-identity (id part of
     * remote.source.identifier).
     */
    private boolean matchesSelfIdentity(String destinationUuid, List<String> selfIdentifiers) {
        for (String identifier : selfIdentifiers) {
            if (identifier == null) {
                continue;
            }
            String idPart = identifier.contains("/") ? identifier.substring(identifier.lastIndexOf('/') + 1)
                    : identifier;
            if (idPart.equalsIgnoreCase(destinationUuid)) {
                return true;
            }
        }
        return false;
    }
}
