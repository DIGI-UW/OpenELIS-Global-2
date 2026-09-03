package org.openelisglobal.shipment.fhir;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.Location;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.SimpleQuantity;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.SupplyDelivery;
import org.hl7.fhir.r4.model.SupplyDelivery.SupplyDeliveryStatus;
import org.hl7.fhir.r4.model.SupplyDelivery.SupplyDeliverySuppliedItemComponent;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.dataexchange.fhir.exception.FhirLocalPersistingException;
import org.openelisglobal.dataexchange.fhir.service.FhirPersistanceService;
import org.openelisglobal.eqa.service.EQACycleService;
import org.openelisglobal.eqa.valueholder.EQACycle;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.shipment.dao.BoxSampleItemDAO;
import org.openelisglobal.shipment.valueholder.BoxSampleItem;
import org.openelisglobal.shipment.valueholder.BoxState;
import org.openelisglobal.shipment.valueholder.ReceptionStatus;
import org.openelisglobal.shipment.valueholder.ShippingBox;
import org.openelisglobal.siteinformation.service.SiteInformationService;
import org.openelisglobal.siteinformation.valueholder.SiteInformation;
import org.openelisglobal.spring.util.SpringContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Transforms ShippingBox entities to FHIR R4 SupplyDelivery resources. Maps box
 * lifecycle states to SupplyDelivery.status per the spec's FHIR alignment
 * requirements. Includes individual Specimen references for each SampleItem in
 * the box.
 */
@Service
public class ShippingBoxFhirTransform {

    private static final String OPENELIS_SHIPMENT_SYSTEM = "http://openelis.org/shipment";
    private static final String EXT_TEMPERATURE = "http://openelis.org/fhir/extension/shipment-temperature";
    private static final String EXT_CAPACITY = "http://openelis.org/fhir/extension/shipment-capacity";
    private static final String EXT_NOTES = "http://openelis.org/fhir/extension/shipment-notes";
    private static final String EXT_SPECIMEN = "http://openelis.org/fhir/extension/shipment-specimen";
    private static final String EXT_SPECIMEN_TYPE_SUMMARY = "http://openelis.org/fhir/extension/shipment-specimen-type-summary";
    private static final String EXT_NON_CONFORMITY = "http://openelis.org/fhir/extension/shipment-non-conformity";
    /**
     * T-42: one per contents row, nested {label, type} — what a receiving site
     * renders as the box's manifest, since the row FKs mean nothing to it. Carries
     * panel material too, which has no Specimen resource and so no EXT_SPECIMEN.
     */
    public static final String EXT_CONTENT_ITEM = "http://openelis.org/fhir/extension/shipment-content-item";
    /**
     * The EQA cycle a provider consignment belongs to — scheme name, cycle number
     * and name, distribution date, submission deadline — so a participant OpenELIS
     * can open the matching local cycle on import instead of waiting for a REST
     * call.
     */
    public static final String EXT_EQA_CYCLE = "http://openelis.org/fhir/extension/eqa-cycle";
    /** Anchor for the contained Location the destination reference points at. */
    static final String CONTAINED_DESTINATION_ID = "destination-facility";

    @Autowired
    private BoxSampleItemDAO boxSampleItemDAO;

    @Autowired
    private SiteInformationService siteInformationService;

    /**
     * Transform a ShippingBox to a FHIR SupplyDelivery resource, including
     * references to contained Specimen resources (SampleItem FHIR UUIDs).
     *
     * <p>
     * Transactional in its own right: the transform walks lazy associations (panel
     * sample → panel, sample item → sample), and a future caller outside a
     * transaction would otherwise die on LazyInitializationException. Inside
     * {@link #syncToFhir} it simply joins the transaction already open.
     */
    @Transactional(readOnly = true)
    public SupplyDelivery transformToSupplyDelivery(ShippingBox box) {
        SupplyDelivery supplyDelivery = new SupplyDelivery();

        // Resource ID = FHIR UUID
        supplyDelivery.setId(box.getFhirUuidAsString());

        // Identifier — the human-readable box ID
        Identifier boxIdentifier = new Identifier();
        boxIdentifier.setSystem(OPENELIS_SHIPMENT_SYSTEM + "/box-id");
        boxIdentifier.setValue(box.getBoxId());
        supplyDelivery.addIdentifier(boxIdentifier);

        // Status mapping: BoxState → SupplyDeliveryStatus
        supplyDelivery.setStatus(mapBoxStateToFhirStatus(box.getState()));

        // Type — specimen shipment
        CodeableConcept type = new CodeableConcept();
        type.addCoding(new Coding().setSystem("http://terminology.hl7.org/CodeSystem/supply-item-type")
                .setCode("medication").setDisplay("Specimen Shipment"));
        type.setText("Specimen Shipment Box");
        supplyDelivery.setType(type);

        // Load box sample items to get specimen count and references
        List<BoxSampleItem> boxSampleItems = boxSampleItemDAO.findByShippingBoxId(box.getId());
        int specimenCount = boxSampleItems.size();

        // Supplied item — number of specimens
        SupplyDeliverySuppliedItemComponent suppliedItem = new SupplyDeliverySuppliedItemComponent();
        SimpleQuantity quantity = new SimpleQuantity();
        quantity.setValue(specimenCount);
        quantity.setUnit("specimens");
        quantity.setSystem("http://unitsofmeasure.org");
        quantity.setCode("{specimens}");
        suppliedItem.setQuantity(quantity);

        // Item type — specimen container (configurable SNOMED CT code)
        CodeableConcept itemType = new CodeableConcept();
        String containerCode = getConfigValue("fhirContainerTypeCode", "434711009");
        String containerDisplay = getConfigValue("fhirContainerTypeDisplay", "Specimen container");
        itemType.addCoding(
                new Coding().setSystem("http://snomed.info/sct").setCode(containerCode).setDisplay(containerDisplay));
        suppliedItem.setItem(itemType);
        supplyDelivery.setSuppliedItem(suppliedItem);

        // Occurrence — sent date if available, otherwise created date
        if (box.getSentDate() != null) {
            supplyDelivery.setOccurrence(new DateTimeType(new Date(box.getSentDate().getTime())));
        } else if (box.getCreatedDate() != null) {
            supplyDelivery.setOccurrence(new DateTimeType(new Date(box.getCreatedDate().getTime())));
        }

        // Destination — the laboratory this box is going to, carrying its FHIR UUID so
        // a remote site can recognise its own consignments.
        //
        // R4 types destination as Reference(Location) and receiver as
        // Reference(Practitioner|PractitionerRole) — an Organization in either is
        // rejected outright by a validating server ("HAPI-0931: Invalid reference
        // found at path 'SupplyDelivery.destination'"), which is why no box ever
        // reached the store before. The receiving laboratory therefore travels as a
        // contained Location whose managingOrganization is that laboratory: legal R4,
        // and it needs no second resource written to the partner's server.
        if (box.getDestinationFacility() != null) {
            String facilityName = box.getDestinationFacility().getOrganizationName();
            Location destination = new Location();
            destination.setId(CONTAINED_DESTINATION_ID);
            destination.setName(facilityName);
            if (box.getDestinationFacility().getFhirUuid() != null) {
                destination.setManagingOrganization(
                        new Reference("Organization/" + box.getDestinationFacility().getFhirUuid().toString())
                                .setDisplay(facilityName));
            } else {
                LogEvent.logWarn(this.getClass().getSimpleName(), "transformToSupplyDelivery", "Destination facility '"
                        + facilityName + "' has no FHIR UUID — remote sites may not be able to match it");
            }
            supplyDelivery.addContained(destination);
            supplyDelivery.setDestination(new Reference("#" + CONTAINED_DESTINATION_ID).setDisplay(facilityName));
        }

        // Supplier — this laboratory, when it knows which Organization represents it.
        // Without it a receiving site can tell a consignment is for them but not who
        // sent it.
        String siteOrgUuid = getConfigValue("siteOrganizationFhirUuid", "");
        if (!siteOrgUuid.isBlank()) {
            supplyDelivery.setSupplier(new Reference("Organization/" + siteOrgUuid));
        } else {
            LogEvent.logWarn(this.getClass().getSimpleName(), "transformToSupplyDelivery",
                    "siteOrganizationFhirUuid not configured — box " + box.getBoxId()
                            + " exports with no supplier; receiving sites cannot tell who shipped it");
        }

        // Extensions — temperature requirement
        if (box.getTemperatureRequirement() != null) {
            Extension tempExt = new Extension(EXT_TEMPERATURE);
            tempExt.setValue(new StringType(box.getTemperatureRequirement()));
            supplyDelivery.addExtension(tempExt);
        }

        // Extensions — capacity
        if (box.getCapacity() != null) {
            Extension capExt = new Extension(EXT_CAPACITY);
            capExt.setValue(new IntegerType(box.getCapacity()));
            supplyDelivery.addExtension(capExt);
        }

        // Extensions — notes
        if (box.getNotes() != null && !box.getNotes().isEmpty()) {
            Extension notesExt = new Extension(EXT_NOTES);
            notesExt.setValue(new StringType(box.getNotes()));
            supplyDelivery.addExtension(notesExt);
        }

        // Extensions — specimen references and type summary
        addSpecimenExtensions(supplyDelivery, boxSampleItems);

        addEqaCycleExtension(supplyDelivery, box);

        return supplyDelivery;
    }

    private void addEqaCycleExtension(SupplyDelivery supplyDelivery, ShippingBox box) {
        if (box.getEqaCycleId() == null) {
            return;
        }
        EQACycle cycle = SpringContext.getBean(EQACycleService.class).get(box.getEqaCycleId());
        if (cycle == null || cycle.getScheme() == null) {
            return;
        }
        Extension ext = new Extension(EXT_EQA_CYCLE);
        ext.addExtension(new Extension("scheme", new StringType(cycle.getScheme().getName())));
        if (cycle.getCycleNumber() != null) {
            ext.addExtension(new Extension("number", new IntegerType(cycle.getCycleNumber())));
        }
        if (cycle.getCycleName() != null) {
            ext.addExtension(new Extension("name", new StringType(cycle.getCycleName())));
        }
        if (cycle.getPlannedStartDate() != null) {
            ext.addExtension(new Extension("distributionDate", new DateType(cycle.getPlannedStartDate())));
        }
        if (cycle.getPlannedEndDate() != null) {
            ext.addExtension(new Extension("submissionDeadline", new DateType(cycle.getPlannedEndDate())));
        }
        supplyDelivery.addExtension(ext);
    }

    /**
     * Add individual Specimen reference extensions and a type summary extension to
     * the SupplyDelivery resource.
     */
    private void addSpecimenExtensions(SupplyDelivery supplyDelivery, List<BoxSampleItem> boxSampleItems) {
        Map<String, Integer> specimenTypeCounts = new HashMap<>();
        Map<String, String> nonConformityOverrides = null;

        for (BoxSampleItem bsi : boxSampleItems) {
            // T-42: every contents row — patient specimen or panel material — travels
            // as a labelled content item so the receiver can render a manifest.
            String label = null;
            String typeDescription = null;
            SampleItem sampleItem = bsi.getSampleItem();
            if (bsi.getEqaPanelSample() != null) {
                label = bsi.getEqaPanelSample().getSampleCode();
                if (bsi.getEqaPanelSample().getPanel() != null) {
                    typeDescription = bsi.getEqaPanelSample().getPanel().getPanelName();
                }
            } else if (sampleItem != null) {
                typeDescription = getTypeDescription(sampleItem);
                if (sampleItem.getSample() != null) {
                    label = sampleItem.getSample().getAccessionNumber();
                }
            }
            if (label != null || typeDescription != null) {
                Extension contentExt = new Extension(EXT_CONTENT_ITEM);
                if (label != null) {
                    contentExt.addExtension(new Extension("label", new StringType(label)));
                }
                if (typeDescription != null) {
                    contentExt.addExtension(new Extension("type", new StringType(typeDescription)));
                }
                supplyDelivery.addExtension(contentExt);
            }

            if (sampleItem == null || sampleItem.getFhirUuid() == null) {
                String typeKey = typeDescription != null ? typeDescription : "Unknown";
                specimenTypeCounts.put(typeKey, specimenTypeCounts.getOrDefault(typeKey, 0) + 1);
                continue;
            }

            Reference specimenRef = new Reference("Specimen/" + sampleItem.getFhirUuidAsString());
            if (typeDescription != null) {
                specimenRef.setDisplay(typeDescription);
            }

            supplyDelivery.addExtension(new Extension(EXT_SPECIMEN, specimenRef));

            // Non-conformity extension with SNOMED CT codes (Rule 6)
            if (bsi.getReceptionStatus() != null && bsi.getReceptionStatus() != ReceptionStatus.PENDING
                    && bsi.getReceptionStatus() != ReceptionStatus.RECEIVED_GOOD) {
                if (nonConformityOverrides == null) {
                    nonConformityOverrides = parseNonConformityCodes(getConfigValue("fhirNonConformityCodes", ""));
                }
                Extension ncExt = new Extension(EXT_NON_CONFORMITY);
                CodeableConcept ncCode = mapReceptionStatusToSnomedCt(bsi.getReceptionStatus(), nonConformityOverrides);
                ncExt.setValue(ncCode);
                supplyDelivery.addExtension(ncExt);
            }

            String typeKey = typeDescription != null ? typeDescription : "Unknown";
            specimenTypeCounts.put(typeKey, specimenTypeCounts.getOrDefault(typeKey, 0) + 1);
        }

        for (Map.Entry<String, Integer> entry : specimenTypeCounts.entrySet()) {
            Extension typeSummaryExt = new Extension(EXT_SPECIMEN_TYPE_SUMMARY);
            typeSummaryExt.addExtension(new Extension("type", new StringType(entry.getKey())));
            typeSummaryExt.addExtension(new Extension("count", new IntegerType(entry.getValue())));
            supplyDelivery.addExtension(typeSummaryExt);
        }
    }

    private String getTypeDescription(SampleItem sampleItem) {
        if (sampleItem.getTypeOfSample() != null && sampleItem.getTypeOfSample().getDescription() != null) {
            return sampleItem.getTypeOfSample().getDescription();
        }
        return null;
    }

    /**
     * Map ReceptionStatus to SNOMED CT CodeableConcept for non-conformity reporting
     * (Rule 6). Codes are configurable via SiteInformation
     * 'fhirNonConformityCodes'.
     */
    private CodeableConcept mapReceptionStatusToSnomedCt(ReceptionStatus status, Map<String, String> overrides) {
        CodeableConcept concept = new CodeableConcept();
        Coding coding = new Coding().setSystem("http://snomed.info/sct");

        // Default codes — overridden by SiteInformation config
        java.util.Map<String, String> defaults = java.util.Map.of("RECEIVED_DAMAGED", "281411007", "RECEIVED_LEAKED",
                "281412000", "MISSING", "281264009", "REJECTED", "123840003");

        String code = overrides.getOrDefault(status.name(), defaults.getOrDefault(status.name(), "281411007"));
        coding.setCode(code).setDisplay(status.name().replace("_", " ").toLowerCase());

        concept.addCoding(coding);
        concept.setText(status.name());
        return concept;
    }

    /**
     * Parse the SiteInformation 'fhirNonConformityCodes' JSON config (a flat
     * {"STATUS":"snomedCode"} object) into a map. Malformed JSON logs one warn and
     * yields an empty map so default codes apply.
     */
    static Map<String, String> parseNonConformityCodes(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(json,
                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, String>>() {
                    });
        } catch (Exception e) {
            LogEvent.logWarn(ShippingBoxFhirTransform.class.getSimpleName(), "parseNonConformityCodes",
                    "fhirNonConformityCodes is not a valid JSON object of string pairs — using default SNOMED codes: "
                            + e.getMessage());
            return Map.of();
        }
    }

    /**
     * Read a value from SiteInformation, with fallback to default.
     */
    private String getConfigValue(String name, String defaultValue) {
        try {
            SiteInformation si = siteInformationService.getSiteInformationByName(name);
            if (si != null && si.getValue() != null && !si.getValue().isBlank()) {
                return si.getValue();
            }
        } catch (Exception e) {
            // Fall through to default
        }
        return defaultValue;
    }

    /**
     * Map BoxState enum to FHIR SupplyDeliveryStatus
     */
    private SupplyDeliveryStatus mapBoxStateToFhirStatus(BoxState state) {
        if (state == null) {
            return SupplyDeliveryStatus.NULL;
        }
        switch (state) {
        case DRAFT:
        case READY_TO_SEND:
            return SupplyDeliveryStatus.INPROGRESS;
        case SENT:
        case IN_TRANSIT:
            return SupplyDeliveryStatus.INPROGRESS;
        case RECEIVED:
        case PARTIALLY_RECEIVED:
        case RECONCILED:
            return SupplyDeliveryStatus.COMPLETED;
        case CANCELLED:
        case LOST_IN_TRANSIT:
            return SupplyDeliveryStatus.ABANDONED;
        default:
            return SupplyDeliveryStatus.NULL;
        }
    }

    /**
     * Transform and persist ShippingBox to FHIR server. Called from service layer
     * on state changes. Runs within a read-only transaction to resolve lazy
     * associations for the database read portion.
     */
    @Transactional(readOnly = true)
    public void syncToFhir(ShippingBox box, boolean isCreate) {
        try {
            SupplyDelivery supplyDelivery = transformToSupplyDelivery(box);
            persistSupplyDelivery(supplyDelivery, isCreate);
        } catch (Exception e) {
            LogEvent.logError("Error syncing ShippingBox to FHIR: " + e.getMessage(), e);
        }
    }

    private void persistSupplyDelivery(SupplyDelivery supplyDelivery, boolean isCreate)
            throws FhirLocalPersistingException {
        try {
            FhirPersistanceService fhirPersistanceService = SpringContext.getBean(FhirPersistanceService.class);
            if (fhirPersistanceService == null) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "persistSupplyDelivery",
                        "FhirPersistanceService not available, skipping FHIR sync");
                return;
            }

            Map<String, Resource> resourceMap = new HashMap<>();
            String resourceId = supplyDelivery.getIdElement().getValue();
            if (resourceId == null || resourceId.isEmpty()) {
                resourceId = supplyDelivery.getId();
            }
            resourceMap.put(resourceId != null ? resourceId : "", supplyDelivery);

            // Always a PUT to the box's own FHIR UUID, create or not: the create path
            // mints a random resource id and ignores the one on the resource, so a box
            // synced on creation and again on every state change ends up as several
            // resources — and a partner site polling for consignments can then import
            // the oldest snapshot of a box (empty, not yet packed) as if it were
            // current. One box is one SupplyDelivery; PUT to a fresh id creates it.
            fhirPersistanceService.updateFhirResourcesInFhirStore(resourceMap);
        } catch (Exception e) {
            LogEvent.logError("Error persisting SupplyDelivery to FHIR server: " + e.getMessage(), e);
            throw new FhirLocalPersistingException(e);
        }
    }
}
