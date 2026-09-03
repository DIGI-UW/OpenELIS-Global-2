package org.openelisglobal.shipment.fhir;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.ResourceGoneException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.Location;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.SupplyDelivery;
import org.hl7.fhir.r4.model.SupplyDelivery.SupplyDeliveryStatus;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.dataexchange.fhir.FhirUtil;
import org.openelisglobal.eqa.service.EQAShipmentService;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.shipment.dao.ShippingBoxDAO;
import org.openelisglobal.shipment.service.ShipmentService;
import org.openelisglobal.shipment.service.ShippingBoxService;
import org.openelisglobal.shipment.valueholder.BoxState;
import org.openelisglobal.shipment.valueholder.Shipment;
import org.openelisglobal.shipment.valueholder.ShipmentStatus;
import org.openelisglobal.shipment.valueholder.ShippingBox;
import org.openelisglobal.siteinformation.service.SiteInformationService;
import org.openelisglobal.siteinformation.valueholder.SiteInformation;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service responsible for importing SupplyDelivery resources from remote FHIR
 * servers and creating/updating local ShippingBox entries for reception
 * reconciliation.
 *
 * Follows the same polling pattern as FhirApiWorkFlowServiceImpl for Task
 * resources — including status backflow (T-41): when this lab receives an
 * imported box, the origin store's SupplyDelivery is completed, and a sender
 * polls its own store to learn its consignments arrived. Both directions ride
 * the same {@code org.openelisglobal.remote.source.updateStatus} flag the Task
 * workflow honours, so switching it off restores strictly manual confirmation.
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

    @Autowired
    private SystemUserService systemUserService;

    @Value("${org.openelisglobal.remote.source.updateStatus:false}")
    private Optional<Boolean> remoteStoreUpdateStatus;

    /**
     * T-43: consignments whose occurrence is older than this many days are not
     * imported — dedup is local-only, so a fresh install would otherwise resurrect
     * long-delivered consignments whose sender never advanced the status.
     */
    @Value("${org.openelisglobal.shipment.import.maxAgeDays:30}")
    private int importMaxAgeDays;

    /**
     * Off by default: the consignment import stays a button on the Reception screen
     * unless the deployment turns the poll on. It then runs on the same cadence as
     * the delivery reconcile below.
     */
    @Value("${org.openelisglobal.shipment.import.scheduled:false}")
    private boolean importScheduled;

    /**
     * The scheduled poll goes through the proxy so pollAndImportShipments keeps
     * its @Async and @Transactional behaviour; a plain self-call would bypass both.
     */
    @Autowired
    @Lazy
    private ShipmentFhirImportService self;

    private boolean statusBackflowEnabled() {
        return remoteStoreUpdateStatus.isPresent() && remoteStoreUpdateStatus.get();
    }

    /**
     * Poll all configured remote FHIR servers for SupplyDelivery resources with
     * status in-progress (SENT/IN_TRANSIT boxes). Import them as local ShippingBox
     * with state IN_TRANSIT for reception. Runs asynchronously.
     */
    @Async
    @Transactional
    public void pollAndImportShipments() {
        // T-45: with no site organization configured the addressed-to-us filter is
        // off, and any consignment matching any local organization is imported. Said
        // once per poll so the log distinguishes over-import from intent.
        if (getSiteOrganizationFhirUuid() == null) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "pollAndImportShipments",
                    "site organization not configured — accepting all consignments (set siteOrganizationFhirUuid on"
                            + " Shipment Settings to filter to this site)");
        }
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
        if (totalImported > 0) {
            LogEvent.logInfo(this.getClass().getSimpleName(), "pollAndImportShipments",
                    "Total shipments imported: " + totalImported);
        }
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

        // Always reported: a poll that finds consignments and imports none of them is
        // the failure mode that looks exactly like success from the outside.
        LogEvent.logInfo(this.getClass().getSimpleName(), "importFromRemote", "Polled " + remoteStorePath + ": "
                + allDeliveries.size() + " in-progress deliveries, " + imported + " imported");

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

            // T-43: staleness window. A consignment sent long ago whose status never
            // advanced is history, not an arrival — importing it as IN_TRANSIT invents
            // a ghost. No occurrence date means age is unknowable: imported as today,
            // matching prior behaviour.
            if (delivery.hasOccurrenceDateTimeType() && delivery.getOccurrenceDateTimeType().getValue() != null) {
                long ageDays = java.time.temporal.ChronoUnit.DAYS
                        .between(delivery.getOccurrenceDateTimeType().getValue().toInstant(), java.time.Instant.now());
                if (ageDays > importMaxAgeDays) {
                    LogEvent.logInfo(this.getClass().getSimpleName(), "importSupplyDelivery", "Box " + boxId
                            + " occurrence is " + ageDays + " days old, beyond the " + importMaxAgeDays
                            + "-day import window (org.openelisglobal.shipment.import.maxAgeDays); skipping (age)");
                    return false;
                }
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

            // Specimen count from supplied item quantity
            if (delivery.hasSuppliedItem() && delivery.getSuppliedItem().hasQuantity()) {
                box.setActualSampleCount(delivery.getSuppliedItem().getQuantity().getValue().intValue());
            }

            // T-42: the consignment's manifest. Contents rows cannot be created here
            // (their FKs name rows only the sender has), so what the payload says is
            // inside is kept read-side for BoxDetails to render.
            box.setImportedContents(extractImportedContents(delivery));

            // Sent date from occurrence
            if (delivery.hasOccurrenceDateTimeType()) {
                box.setSentDate(new Timestamp(delivery.getOccurrenceDateTimeType().getValue().getTime()));
            }

            // Destination facility — match by FHIR UUID first, fallback to name.
            // The receiving lab rides on receiver: destination is Reference(Location) in
            // R4 and an Organization there is rejected by the server outright.
            Organization destinationOrg = null;
            String destinationUuid = destinationOrganizationUuid(delivery);

            // Filter: only accept boxes destined for THIS lab
            String siteOrgUuid = getSiteOrganizationFhirUuid();
            if (siteOrgUuid != null && !siteOrgUuid.isBlank()) {
                if (destinationUuid == null || !destinationUuid.equalsIgnoreCase(siteOrgUuid)) {
                    // Not addressed to us. Said out loud, because a site that has its own
                    // organization configured wrongly cannot otherwise tell this apart from
                    // "the partner has sent nothing".
                    LogEvent.logInfo(this.getClass().getSimpleName(), "importSupplyDelivery",
                            "Box " + boxId + " is addressed to " + destinationUuid + ", not to this site ("
                                    + siteOrgUuid + "); skipping [destinationRef="
                                    + (delivery.hasDestination() ? delivery.getDestination().getReference() : "none")
                                    + ", contained=" + delivery.getContained().size() + "]");
                    return false;
                }
            }

            // Match destination organization: by UUID first
            if (destinationUuid != null) {
                destinationOrg = findOrganizationByFhirUuid(destinationUuid);
            }

            // Fallback: match by name
            if (destinationOrg == null) {
                String display = receiverDisplay(delivery);
                if (display == null && delivery.hasDestination()) {
                    display = delivery.getDestination().getDisplay();
                }
                if (display != null) {
                    destinationOrg = findOrganizationByName(display);
                }
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

    /**
     * The laboratory a consignment is addressed to. The sender writes it as the
     * managingOrganization of the contained Location destination points at (R4
     * types destination as Reference(Location)); a bare Organization reference is
     * still read so anything written by an older sender is not silently ignored.
     */
    private String destinationOrganizationUuid(SupplyDelivery delivery) {
        if (!delivery.hasDestination()) {
            return null;
        }
        Reference destination = delivery.getDestination();
        String reference = destination.getReference();
        if (reference != null && reference.startsWith("Organization/")) {
            return reference.substring("Organization/".length());
        }

        // The parser may hand back the contained Location already resolved onto the
        // reference, or leave it as a "#id" pointer into contained — accept either,
        // since which one you get depends on how the resource was parsed.
        Location destinationLocation = null;
        if (destination.getResource() instanceof Location resolved) {
            destinationLocation = resolved;
        } else if (reference != null && reference.startsWith("#")) {
            String containedId = reference.substring(1);
            for (Resource contained : delivery.getContained()) {
                if (contained instanceof Location location && containedId.equals(location.getIdElement().getIdPart())) {
                    destinationLocation = location;
                    break;
                }
            }
        }

        if (destinationLocation != null && destinationLocation.hasManagingOrganization()) {
            String managing = destinationLocation.getManagingOrganization().getReference();
            if (managing != null && managing.startsWith("Organization/")) {
                return managing.substring("Organization/".length());
            }
        }
        return null;
    }

    private String receiverDisplay(SupplyDelivery delivery) {
        for (var receiver : delivery.getReceiver()) {
            if (receiver.hasDisplay()) {
                return receiver.getDisplay();
            }
        }
        return null;
    }

    // ---- T-41: delivery-status backflow ----

    /**
     * Receiver side: this lab has taken delivery of a box, so the store it was
     * imported from should say {@code completed} — that is the only signal the
     * sender's monitor can see. The origin store is discovered rather than stored:
     * the box's FHIR UUID is the origin's resource id, so whichever configured
     * remote answers the read is where it came from. A box created locally exists
     * on no remote and every read misses, which is exactly the no-op it should be.
     */
    @Async
    public void completeRemoteSupplyDelivery(ShippingBox box) {
        if (!statusBackflowEnabled() || box == null || box.getFhirUuid() == null
                || fhirConfig.getRemoteStorePaths() == null) {
            return;
        }
        for (String remoteStorePath : fhirConfig.getRemoteStorePaths()) {
            if (remoteStorePath == null || remoteStorePath.isBlank()) {
                continue;
            }
            try {
                IGenericClient remoteClient = fhirUtil.getFhirClient(remoteStorePath);
                SupplyDelivery delivery = remoteClient.read().resource(SupplyDelivery.class)
                        .withId(box.getFhirUuidAsString()).execute();
                if (delivery.getStatus() == SupplyDeliveryStatus.COMPLETED) {
                    return;
                }
                delivery.setStatus(SupplyDeliveryStatus.COMPLETED);
                remoteClient.update().resource(delivery).execute();
                LogEvent.logInfo(this.getClass().getSimpleName(), "completeRemoteSupplyDelivery",
                        "Completed SupplyDelivery for box " + box.getBoxId() + " on " + remoteStorePath);
                return;
            } catch (ResourceNotFoundException | ResourceGoneException e) {
                // Not this remote's consignment — or not a consignment at all.
            } catch (Exception e) {
                LogEvent.logError(this.getClass().getSimpleName(), "completeRemoteSupplyDelivery",
                        "Could not complete SupplyDelivery for box " + box.getBoxId() + " on " + remoteStorePath + ": "
                                + e.getMessage());
            }
        }
    }

    /**
     * Sender side: a dispatched box whose own-store SupplyDelivery reads
     * {@code completed} was received by the partner (their backflow wrote it), so
     * delivery is recorded here without anyone clicking — an EQA box through the
     * same path the Receipt Monitor's manual confirm uses (cycle auto-advance and
     * audit included), any other box as box RECEIVED + shipment DELIVERED.
     *
     * <p>
     * Collaborators are fetched at call time, not injected: this service sits below
     * ShippingBoxService (which calls the backflow above on every RECEIVED), so
     * injecting them back up would be a bean cycle.
     *
     * <p>
     * Deliberately not atomic across boxes: each delivery is applied in its own
     * transaction, so a fault mid-loop keeps every box already applied and the next
     * run picks up the rest.
     */
    /**
     * Scheduled counterpart of the Reception screen's Import from FHIR button,
     * enabled by {@code org.openelisglobal.shipment.import.scheduled=true}. A
     * participant laboratory then sees a dispatched consignment within one poll
     * with nobody clicking; with the property unset nothing changes.
     */
    @Scheduled(initialDelay = 45 * 1000, fixedRateString = "${org.openelisglobal.remote.poll.frequency:120000}")
    public void importShipmentsOnSchedule() {
        if (!importScheduled) {
            return;
        }
        self.pollAndImportShipments();
    }

    @Scheduled(initialDelay = 30 * 1000, fixedRateString = "${org.openelisglobal.remote.poll.frequency:120000}")
    public void reconcileDeliveredShipments() {
        if (!statusBackflowEnabled()) {
            return;
        }
        List<ShippingBox> inFlight = new ArrayList<>();
        inFlight.addAll(shippingBoxDAO.findByState(BoxState.SENT));
        inFlight.addAll(shippingBoxDAO.findByState(BoxState.IN_TRANSIT));

        int delivered = 0;
        IGenericClient ownStore = fhirUtil.getLocalFhirClient();
        for (ShippingBox box : inFlight) {
            if (box.getFhirUuid() == null) {
                continue;
            }
            try {
                SupplyDelivery delivery = ownStore.read().resource(SupplyDelivery.class)
                        .withId(box.getFhirUuidAsString()).execute();
                if (delivery.getStatus() != SupplyDeliveryStatus.COMPLETED) {
                    continue;
                }
                applyRemoteDelivery(box);
                delivered++;
            } catch (ResourceNotFoundException | ResourceGoneException e) {
                // Never exported (or imported-only) — nothing to reconcile against.
            } catch (Exception e) {
                LogEvent.logError(this.getClass().getSimpleName(), "reconcileDeliveredShipments",
                        "Could not reconcile box " + box.getBoxId() + ": " + e.getMessage());
            }
        }
        if (delivered > 0) {
            LogEvent.logInfo(this.getClass().getSimpleName(), "reconcileDeliveredShipments",
                    delivered + " shipment(s) recorded as delivered from partner receipts");
        }
    }

    private void applyRemoteDelivery(ShippingBox box) {
        String sysUserId = String.valueOf(getAutomatedImportUserId());
        if (box.getEqaCycleId() != null) {
            SpringContext.getBean(EQAShipmentService.class).applyRemoteDelivery(box.getId(), sysUserId);
            return;
        }
        SpringContext.getBean(ShippingBoxService.class).changeBoxState(box.getId(), BoxState.RECEIVED,
                getAutomatedImportUserId());
        ShipmentService shipmentService = SpringContext.getBean(ShipmentService.class);
        Shipment shipment = shipmentService.getShipmentByShippingBoxId(box.getId());
        if (shipment != null && shipment.getStatus() != ShipmentStatus.DELIVERED) {
            shipmentService.updateShipmentStatus(shipment.getId(), ShipmentStatus.DELIVERED);
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

    /**
     * T-42: read the consignment's manifest off the SupplyDelivery as a JSON list
     * of {label, type}. Prefers the content-item extensions (nested label/type);
     * falls back to the bare specimen-reference displays an older sender writes,
     * which carry only a type. Null when the payload names nothing.
     */
    private String extractImportedContents(SupplyDelivery delivery) {
        List<Map<String, String>> items = new ArrayList<>();
        for (Extension ext : delivery.getExtensionsByUrl(ShippingBoxFhirTransform.EXT_CONTENT_ITEM)) {
            Map<String, String> item = new LinkedHashMap<>();
            Extension label = ext.getExtensionByUrl("label");
            if (label != null && label.getValue() instanceof StringType st) {
                item.put("label", st.getValue());
            }
            Extension type = ext.getExtensionByUrl("type");
            if (type != null && type.getValue() instanceof StringType st) {
                item.put("type", st.getValue());
            }
            if (!item.isEmpty()) {
                items.add(item);
            }
        }
        if (items.isEmpty()) {
            for (Extension ext : delivery.getExtensionsByUrl("http://openelis.org/fhir/extension/shipment-specimen")) {
                if (ext.getValue() instanceof Reference ref && ref.hasDisplay()) {
                    Map<String, String> item = new LinkedHashMap<>();
                    item.put("type", ref.getDisplay());
                    items.add(item);
                }
            }
        }
        if (items.isEmpty()) {
            return null;
        }
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(items);
        } catch (Exception e) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "extractImportedContents",
                    "Could not serialise imported contents: " + e.getMessage());
            return null;
        }
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
