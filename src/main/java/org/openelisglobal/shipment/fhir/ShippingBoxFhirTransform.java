package org.openelisglobal.shipment.fhir;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.IntegerType;
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

    @Autowired
    private BoxSampleItemDAO boxSampleItemDAO;

    @Autowired
    private SiteInformationService siteInformationService;

    /**
     * Transform a ShippingBox to a FHIR SupplyDelivery resource, including
     * references to contained Specimen resources (SampleItem FHIR UUIDs).
     */
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

        // Destination — reference to Organization (must include UUID for cross-site
        // matching)
        if (box.getDestinationFacility() != null) {
            Reference destination = new Reference();
            if (box.getDestinationFacility().getFhirUuid() != null) {
                destination.setReference("Organization/" + box.getDestinationFacility().getFhirUuid().toString());
            } else {
                LogEvent.logWarn(this.getClass().getSimpleName(), "transformToSupplyDelivery",
                        "Destination facility '" + box.getDestinationFacility().getOrganizationName()
                                + "' has no FHIR UUID — remote sites may not be able to match it");
            }
            destination.setDisplay(box.getDestinationFacility().getOrganizationName());
            supplyDelivery.setDestination(destination);
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

        return supplyDelivery;
    }

    /**
     * Add individual Specimen reference extensions and a type summary extension to
     * the SupplyDelivery resource.
     */
    private void addSpecimenExtensions(SupplyDelivery supplyDelivery, List<BoxSampleItem> boxSampleItems) {
        Map<String, Integer> specimenTypeCounts = new HashMap<>();

        for (BoxSampleItem bsi : boxSampleItems) {
            SampleItem sampleItem = bsi.getSampleItem();
            if (sampleItem == null || sampleItem.getFhirUuid() == null) {
                continue;
            }

            Reference specimenRef = new Reference("Specimen/" + sampleItem.getFhirUuidAsString());
            String typeDescription = getTypeDescription(sampleItem);
            if (typeDescription != null) {
                specimenRef.setDisplay(typeDescription);
            }

            supplyDelivery.addExtension(new Extension(EXT_SPECIMEN, specimenRef));

            // Non-conformity extension with SNOMED CT codes (Rule 6)
            if (bsi.getReceptionStatus() != null && bsi.getReceptionStatus() != ReceptionStatus.PENDING
                    && bsi.getReceptionStatus() != ReceptionStatus.RECEIVED_GOOD) {
                Extension ncExt = new Extension(EXT_NON_CONFORMITY);
                CodeableConcept ncCode = mapReceptionStatusToSnomedCt(bsi.getReceptionStatus());
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
    private CodeableConcept mapReceptionStatusToSnomedCt(ReceptionStatus status) {
        CodeableConcept concept = new CodeableConcept();
        Coding coding = new Coding().setSystem("http://snomed.info/sct");

        // Default codes — overridden by SiteInformation config
        java.util.Map<String, String> defaults = java.util.Map.of("RECEIVED_DAMAGED", "281411007", "RECEIVED_LEAKED",
                "281412000", "MISSING", "281264009", "REJECTED", "123840003");

        String code = getNonConformityCode(status.name(), defaults.getOrDefault(status.name(), "281411007"));
        coding.setCode(code).setDisplay(status.name().replace("_", " ").toLowerCase());

        concept.addCoding(coding);
        concept.setText(status.name());
        return concept;
    }

    /**
     * Get a non-conformity SNOMED CT code from config, with fallback to default.
     */
    private String getNonConformityCode(String statusName, String defaultCode) {
        try {
            String json = getConfigValue("fhirNonConformityCodes", "");
            if (!json.isBlank()) {
                // Simple JSON parsing without external library
                // Format: {"RECEIVED_DAMAGED":"281411007","MISSING":"281264009",...}
                String key = "\"" + statusName + "\":\"";
                int start = json.indexOf(key);
                if (start >= 0) {
                    start += key.length();
                    int end = json.indexOf("\"", start);
                    if (end > start) {
                        return json.substring(start, end);
                    }
                }
            }
        } catch (Exception e) {
            // Fall through to default
        }
        return defaultCode;
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

            if (isCreate) {
                fhirPersistanceService.createFhirResourcesInFhirStore(resourceMap);
            } else {
                fhirPersistanceService.updateFhirResourcesInFhirStore(resourceMap);
            }
        } catch (Exception e) {
            LogEvent.logError("Error persisting SupplyDelivery to FHIR server: " + e.getMessage(), e);
            throw new FhirLocalPersistingException(e);
        }
    }
}
