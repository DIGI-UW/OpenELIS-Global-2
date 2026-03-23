package org.openelisglobal.shipment.fhir;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.SupplyDelivery;
import org.hl7.fhir.r4.model.SupplyDelivery.SupplyDeliveryStatus;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.dataexchange.fhir.FhirUtil;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.shipment.dao.ShippingBoxDAO;
import org.openelisglobal.shipment.valueholder.BoxState;
import org.openelisglobal.shipment.valueholder.ShippingBox;
import org.openelisglobal.siteinformation.service.SiteInformationService;
import org.openelisglobal.siteinformation.valueholder.SiteInformation;
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

    @Autowired
    private FhirConfig fhirConfig;

    @Autowired
    private FhirUtil fhirUtil;

    @Autowired
    private ShippingBoxDAO shippingBoxDAO;

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private SiteInformationService siteInformationService;

    /**
     * Poll all configured remote FHIR servers for SupplyDelivery resources with
     * status in-progress (SENT/IN_TRANSIT boxes). Import them as local ShippingBox
     * with state IN_TRANSIT for reception.
     *
     * @return number of new boxes imported
     */
    @Async
    @Transactional
    public int pollAndImportShipments() {
        int totalImported = 0;
        for (String remoteStorePath : fhirConfig.getRemoteStorePaths()) {
            if (remoteStorePath == null || remoteStorePath.isBlank()) {
                continue;
            }
            try {
                totalImported += importFromRemote(remoteStorePath);
            } catch (Exception e) {
                LogEvent.logError(this.getClass().getSimpleName(), "pollAndImportShipments",
                        "Error importing shipments from: " + remoteStorePath + " - " + e.getMessage());
            }
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
            box.setSystemUserId(1); // System user for automated import

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

            // Specimen count from supplied item quantity
            if (delivery.hasSuppliedItem() && delivery.getSuppliedItem().hasQuantity()) {
                box.setActualSampleCount(delivery.getSuppliedItem().getQuantity().getValue().intValue());
            }

            // Sent date from occurrence
            if (delivery.hasOccurrenceDateTimeType()) {
                box.setSentDate(new Timestamp(delivery.getOccurrenceDateTimeType().getValue().getTime()));
            }

            // Destination facility — match by FHIR UUID first, fallback to name
            Organization destinationOrg = null;
            String destinationUuid = null;

            if (delivery.hasDestination() && delivery.getDestination().hasReference()) {
                // Extract UUID from "Organization/{uuid}" reference
                String ref = delivery.getDestination().getReference();
                if (ref != null && ref.startsWith("Organization/")) {
                    destinationUuid = ref.substring("Organization/".length());
                }
            }

            // Filter: only accept boxes destined for THIS lab
            String siteOrgUuid = getSiteOrganizationFhirUuid();
            if (siteOrgUuid != null && !siteOrgUuid.isBlank()) {
                if (destinationUuid == null || !destinationUuid.equalsIgnoreCase(siteOrgUuid)) {
                    // This box is not destined for us — skip
                    return false;
                }
            }

            // Match destination organization: by UUID first
            if (destinationUuid != null) {
                destinationOrg = findOrganizationByFhirUuid(destinationUuid);
            }

            // Fallback: match by name
            if (destinationOrg == null && delivery.hasDestination() && delivery.getDestination().hasDisplay()) {
                destinationOrg = findOrganizationByName(delivery.getDestination().getDisplay());
            }

            if (destinationOrg != null) {
                box.setDestinationFacility(destinationOrg);
            } else {
                LogEvent.logWarn(this.getClass().getSimpleName(), "importSupplyDelivery",
                        "No matching local organization for box " + boxId + ", skipping import");
                return false;
            }

            shippingBoxDAO.insert(box);
            LogEvent.logInfo(this.getClass().getSimpleName(), "importSupplyDelivery",
                    "Imported shipment box: " + boxId + " with state IN_TRANSIT");

            return true;
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "importSupplyDelivery",
                    "Error importing SupplyDelivery: " + e.getMessage());
            return false;
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

    private Organization findOrganizationByName(String name) {
        try {
            List<Organization> orgs = organizationService.getAllOrganizations();
            for (Organization org : orgs) {
                if (org.getOrganizationName() != null && org.getOrganizationName().equalsIgnoreCase(name)) {
                    return org;
                }
            }
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "findOrganizationByName",
                    "Error searching organization: " + e.getMessage());
        }
        return null;
    }

    /**
     * Find a local Organization by its FHIR UUID string.
     */
    private Organization findOrganizationByFhirUuid(String uuidString) {
        try {
            UUID uuid = UUID.fromString(uuidString);
            List<Organization> orgs = organizationService.getAllOrganizations();
            for (Organization org : orgs) {
                if (org.getFhirUuid() != null && org.getFhirUuid().equals(uuid)) {
                    return org;
                }
            }
        } catch (IllegalArgumentException e) {
            // Not a valid UUID
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "findOrganizationByFhirUuid",
                    "Error searching organization by UUID: " + e.getMessage());
        }
        return null;
    }

    /**
     * Get the FHIR UUID of the Organization representing this laboratory
     * installation. Stored in SiteInformation as 'siteOrganizationFhirUuid'.
     *
     * @return UUID string or null if not configured
     */
    private String getSiteOrganizationFhirUuid() {
        try {
            SiteInformation siteInfo = siteInformationService.getSiteInformationByName("siteOrganizationFhirUuid");
            if (siteInfo != null && siteInfo.getValue() != null && !siteInfo.getValue().isBlank()) {
                return siteInfo.getValue().trim();
            }
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "getSiteOrganizationFhirUuid",
                    "Error reading site organization UUID: " + e.getMessage());
        }
        return null;
    }
}
