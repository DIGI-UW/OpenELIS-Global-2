package org.openelisglobal.shipment.service;

import java.util.ArrayList;
import java.util.List;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.hl7.fhir.r4.model.SupplyDelivery;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.dataexchange.fhir.service.FhirPersistanceService;
import org.openelisglobal.dataexchange.service.order.ElectronicOrderService;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.shipment.dto.ExpectedSpecimenDTO;
import org.openelisglobal.shipment.valueholder.BoxSampleItem;
import org.openelisglobal.shipment.valueholder.ShippingBox;
import org.springframework.beans.factory.annotation.Autowired;
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
        if (sr == null || !sr.hasIdentifier()) {
            return null;
        }
        return sr.getIdentifierFirstRep().getValue();
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

    private String extractSpecimenUuid(String reference) {
        if (reference == null) {
            return null;
        }
        int slash = reference.indexOf('/');
        return slash >= 0 ? reference.substring(slash + 1) : reference;
    }
}
