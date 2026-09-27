package org.openelisglobal.shipment.service;

import java.util.ArrayList;
import java.util.List;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.hl7.fhir.r4.model.Specimen;
import org.hl7.fhir.r4.model.SupplyDelivery;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.UserContextHolder;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.dataexchange.fhir.service.FhirPersistanceService;
import org.openelisglobal.dataexchange.service.order.ElectronicOrderService;
import org.openelisglobal.localization.service.LocalizationService;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.shipment.dto.ExpectedSpecimenDTO;
import org.openelisglobal.shipment.valueholder.BoxSampleItem;
import org.openelisglobal.shipment.valueholder.ShippingBox;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShipmentReceptionServiceImpl implements ShipmentReceptionService {

    private static final String EXT_SPECIMEN = "http://openelis.org/fhir/extension/shipment-specimen";

    @Autowired
    private ShippingBoxService shippingBoxService;

    @Autowired
    private BoxSampleItemService boxSampleItemService;

    @Autowired
    private FhirPersistanceService fhirPersistanceService;

    @Autowired
    private ElectronicOrderService electronicOrderService;

    @Autowired
    private SampleService sampleService;

    @Autowired
    private SampleItemService sampleItemService;

    @Autowired
    private TypeOfSampleService typeOfSampleService;

    @Autowired
    private LocalizationService localizationService;

    @Autowired
    private FhirConfig fhirConfig;

    @Autowired
    private UserContextHolder userContextHolder;

    /**
     * Whether an unknown sender sample type may be auto-created during inbound
     * shipment ingestion. Default on so referred-in orders from a peer with a
     * catalog we do not carry still pre-populate; set to {@code false} on
     * deployments that require the sample-type catalog to be curated locally.
     */
    @Value("${org.openelisglobal.shipment.reception.autocreate-sample-type:true}")
    private boolean autoCreateSampleType;

    @Override
    @Transactional
    public List<ExpectedSpecimenDTO> reconcileAndGetExpectedSpecimens(Integer shippingBoxId, Integer systemUserId) {
        List<ExpectedSpecimenDTO> results = new ArrayList<>();

        ShippingBox box = shippingBoxService.getBoxById(shippingBoxId);
        if (box == null || box.getFhirUuid() == null) {
            return results;
        }

        SupplyDelivery delivery = fhirPersistanceService.getSupplyDeliveryByUuid(box.getFhirUuidAsString())
                .orElse(null);
        if (delivery == null) {
            return results;
        }

        List<BoxSampleItem> existingLinks = boxSampleItemService.getBoxSampleItemsByShippingBoxId(shippingBoxId);

        for (Extension ext : delivery.getExtensionsByUrl(EXT_SPECIMEN)) {
            if (!(ext.getValue() instanceof Reference ref)) {
                continue;
            }
            String specimenUuid = extractSpecimenUuid(ref.getReference());
            if (specimenUuid == null) {
                continue;
            }

            ensureSampleTypeExists(specimenUuid);

            ExpectedSpecimenDTO dto = new ExpectedSpecimenDTO();
            dto.setSpecimenUuid(specimenUuid);
            dto.setTypeDisplay(ref.getDisplay());

            String externalOrderNumber = resolveExternalOrderNumber(specimenUuid);
            dto.setExternalOrderNumber(externalOrderNumber);

            if (externalOrderNumber == null
                    || electronicOrderService.getElectronicOrdersByExternalId(externalOrderNumber).isEmpty()) {
                dto.setStatus(ExpectedSpecimenDTO.Status.UNRESOLVED);
                results.add(dto);
                continue;
            }

            reconcileOne(dto, externalOrderNumber, shippingBoxId, systemUserId, existingLinks);
            results.add(dto);
        }
        return results;
    }

    /**
     * Link an accepted Sample to the box (LINKED), else mark it for the operator to
     * accept (PENDING).
     */
    private void reconcileOne(ExpectedSpecimenDTO dto, String externalOrderNumber, Integer shippingBoxId,
            Integer systemUserId, List<BoxSampleItem> existingLinks) {
        Sample sample = findSampleByReferringId(externalOrderNumber);
        if (sample == null) {
            dto.setStatus(ExpectedSpecimenDTO.Status.PENDING);
            return;
        }

        List<SampleItem> sampleItems = sampleItemService.getSampleItemsBySampleId(sample.getId());
        if (sampleItems == null || sampleItems.isEmpty()) {
            dto.setStatus(ExpectedSpecimenDTO.Status.PENDING);
            return;
        }

        // Already linked to this box?
        for (BoxSampleItem link : existingLinks) {
            for (SampleItem si : sampleItems) {
                if (link.getSampleItem() != null && si.getId().equals(link.getSampleItem().getId())) {
                    dto.setStatus(ExpectedSpecimenDTO.Status.LINKED);
                    dto.setBoxSampleItemId(link.getId());
                    return;
                }
            }
        }

        // Link the first unassigned SampleItem (a referred sample normally has one).
        for (SampleItem si : sampleItems) {
            if (!boxSampleItemService.isSampleItemInBox(si.getId())) {
                try {
                    BoxSampleItem created = boxSampleItemService.addSampleItemToBox(shippingBoxId, si.getId(),
                            systemUserId);
                    existingLinks.add(created);
                    dto.setStatus(ExpectedSpecimenDTO.Status.LINKED);
                    dto.setBoxSampleItemId(created.getId());
                    return;
                } catch (RuntimeException e) {
                    LogEvent.logWarn(this.getClass().getSimpleName(), "reconcileOne", "could not link sample item "
                            + si.getId() + " to box " + shippingBoxId + ": " + e.getMessage());
                }
            }
        }
        // Sample's items already assigned elsewhere — leave pending.
        dto.setStatus(ExpectedSpecimenDTO.Status.PENDING);
    }

    private String resolveExternalOrderNumber(String specimenUuid) {
        ServiceRequest sr = fhirPersistanceService.getServiceRequestBySpecimenUuid(specimenUuid).orElse(null);
        if (sr == null) {
            return null;
        }

        // Try identifierFirstRep first (works when the SR itself is the original
        // referral)
        if (sr.hasIdentifier()) {
            String candidateId = sr.getIdentifierFirstRep().getValue();
            if (candidateId != null && !electronicOrderService.getElectronicOrdersByExternalId(candidateId).isEmpty()) {
                return candidateId;
            }
        }

        // Follow basedOn — the SR may be the sender's completed SR that references
        // the original referral SR via basedOn
        if (sr.hasBasedOn()) {
            for (Reference basedOnRef : sr.getBasedOn()) {
                String refId = extractIdFromReference(basedOnRef.getReference());
                if (refId != null && !electronicOrderService.getElectronicOrdersByExternalId(refId).isEmpty()) {
                    return refId;
                }
            }
        }

        // Last resort: return identifierFirstRep even without e-order match
        return sr.hasIdentifier() ? sr.getIdentifierFirstRep().getValue() : null;
    }

    private String extractIdFromReference(String reference) {
        if (reference == null) {
            return null;
        }
        int slashIdx = reference.lastIndexOf('/');
        return slashIdx >= 0 ? reference.substring(slashIdx + 1) : reference;
    }

    private Sample findSampleByReferringId(String referringId) {
        try {
            return sampleService.getSampleByReferringId(referringId);
        } catch (RuntimeException e) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "findSampleByReferringId",
                    "lookup failed for referringId " + referringId + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Read the Specimen FHIR resource and ensure the sample type exists locally. If
     * the sender's abbreviation is unknown, create a new TypeOfSample so that
     * LabOrderSearchProvider can resolve it when pre-populating the form.
     *
     * <p>
     * The reference data written here is driven by an external FHIR peer, so it is
     * attributed through {@link UserContextHolder} rather than a hardcoded admin id
     * and is skipped entirely when {@code autoCreateSampleType} is disabled.
     */
    private void ensureSampleTypeExists(String specimenUuid) {
        if (!autoCreateSampleType) {
            return;
        }
        try {
            Specimen specimen = fhirPersistanceService.getSpecimenByUuid(specimenUuid).orElse(null);
            if (specimen == null || !specimen.hasType()) {
                return;
            }
            String sampleTypeSystem = fhirConfig.getOeFhirSystem() + "/sampleType";
            for (Coding coding : specimen.getType().getCoding()) {
                if (!sampleTypeSystem.equals(coding.getSystem()) || coding.getCode() == null) {
                    continue;
                }
                String abbreviation = coding.getCode();
                String existingId = typeOfSampleService.getTypeOfSampleIdForLocalAbbreviation(abbreviation);
                if (existingId != null && !existingId.isBlank()) {
                    return; // already exists
                }
                String displayName = coding.hasDisplay() ? coding.getDisplay() : abbreviation;
                String sysUserId = resolveIngestionUserId();

                Localization localization = new Localization();
                localization.setDescription("sampleType name: " + displayName);
                localization.setLocalizedValue("en", displayName);
                localization.setSysUserId(sysUserId);
                localizationService.insert(localization);

                TypeOfSample newType = new TypeOfSample();
                newType.setLocalAbbreviation(abbreviation);
                newType.setDescription(displayName);
                newType.setDomain("H");
                newType.setIsActive(true);
                newType.setSortOrder(Integer.MAX_VALUE);
                newType.setLocalization(localization);
                newType.setSysUserId(sysUserId);
                typeOfSampleService.insert(newType);
                typeOfSampleService.clearCache();
                LogEvent.logInfo(this.getClass().getSimpleName(), "ensureSampleTypeExists",
                        "Created local TypeOfSample: " + abbreviation + " (" + newType.getDescription() + ")");
                return;
            }
        } catch (Exception e) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "ensureSampleTypeExists",
                    "Could not ensure sample type for specimen " + specimenUuid + ": " + e.getMessage());
        }
    }

    /**
     * Attribution for reference data created while ingesting an inbound shipment.
     * Uses the authenticated operator when reception runs inside a request, and
     * otherwise the daemon user — ingestion also runs from non-request threads,
     * where there is no operator to attribute the write to.
     */
    private String resolveIngestionUserId() {
        String currentUserId = userContextHolder.getCurrentSysUserId();
        if (currentUserId != null && !currentUserId.isEmpty()) {
            return currentUserId;
        }
        return userContextHolder.getDaemonSysUserId();
    }

    private String extractSpecimenUuid(String reference) {
        if (reference == null) {
            return null;
        }
        int slash = reference.indexOf('/');
        return slash >= 0 ? reference.substring(slash + 1) : reference;
    }
}
