package org.openelisglobal.shipment.fhir;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.SupplyDelivery;
import org.junit.Test;
import org.openelisglobal.eqa.valueholder.EQAPanelSample;
import org.openelisglobal.shipment.dao.BoxSampleItemDAO;
import org.openelisglobal.shipment.valueholder.BoxSampleItem;
import org.openelisglobal.shipment.valueholder.BoxState;
import org.openelisglobal.shipment.valueholder.ReceptionStatus;
import org.openelisglobal.shipment.valueholder.ShippingBox;
import org.openelisglobal.siteinformation.service.SiteInformationService;
import org.openelisglobal.siteinformation.valueholder.SiteInformation;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * T-44: the fhirNonConformityCodes SiteInformation config must be parsed as
 * real JSON, not scanned by indexOf — whitespace or formatting variations must
 * not silently drop custom SNOMED codes.
 */
public class ShippingBoxFhirTransformNonConformityCodesTest {

    @Test
    public void compactJson_parsesAllEntries() {
        Map<String, String> codes = ShippingBoxFhirTransform
                .parseNonConformityCodes("{\"RECEIVED_DAMAGED\":\"12345\",\"MISSING\":\"67890\"}");
        assertEquals("12345", codes.get("RECEIVED_DAMAGED"));
        assertEquals("67890", codes.get("MISSING"));
    }

    @Test
    public void spacedJson_parsesCorrectly() {
        Map<String, String> codes = ShippingBoxFhirTransform
                .parseNonConformityCodes("{ \"RECEIVED_DAMAGED\" : \"12345\" }");
        assertEquals("12345", codes.get("RECEIVED_DAMAGED"));
    }

    @Test
    public void malformedJson_yieldsEmptyMapSoDefaultsApply() {
        Map<String, String> codes = ShippingBoxFhirTransform.parseNonConformityCodes("{'RECEIVED_DAMAGED': not json");
        assertTrue(codes.isEmpty());
    }

    @Test
    public void blankOrNull_yieldsEmptyMap() {
        assertTrue(ShippingBoxFhirTransform.parseNonConformityCodes("").isEmpty());
        assertTrue(ShippingBoxFhirTransform.parseNonConformityCodes(null).isEmpty());
    }

    /**
     * The parser tests above pass on a build that never reaches the parser. EQA
     * panel material has no SampleItem, and the export used to key non-conformity
     * off the Specimen reference that material never gets — so a vial recorded as
     * damaged travelled as an ordinary consignment. This drives the transform
     * itself, which is the only place that distinction shows.
     */
    @Test
    public void damagedPanelMaterialExportsItsConfiguredCode() {
        ShippingBoxFhirTransform transform = new ShippingBoxFhirTransform();

        BoxSampleItemDAO boxSampleItemDAO = mock(BoxSampleItemDAO.class);
        when(boxSampleItemDAO.findByShippingBoxId(7)).thenReturn(panelMaterial());
        ReflectionTestUtils.setField(transform, "boxSampleItemDAO", boxSampleItemDAO);

        SiteInformationService siteInformationService = mock(SiteInformationService.class);
        when(siteInformationService.getSiteInformationByName(anyString())).thenReturn(null);
        when(siteInformationService.getSiteInformationByName("fhirNonConformityCodes"))
                .thenReturn(siteInfo("{ \"RECEIVED_DAMAGED\" : \"398056004\" }"));
        ReflectionTestUtils.setField(transform, "siteInformationService", siteInformationService);

        SupplyDelivery delivery = transform.transformToSupplyDelivery(box());

        List<Extension> nonConformities = delivery
                .getExtensionsByUrl("http://openelis.org/fhir/extension/shipment-non-conformity");
        assertEquals("one damaged vial, one code", 1, nonConformities.size());
        org.hl7.fhir.r4.model.CodeableConcept code = (org.hl7.fhir.r4.model.CodeableConcept) nonConformities.get(0)
                .getValue();
        assertEquals("398056004", code.getCodingFirstRep().getCode());
        assertEquals("http://snomed.info/sct", code.getCodingFirstRep().getSystem());
        assertEquals("RECEIVED_DAMAGED", code.getText());

        // The manifest the receiving site renders must still carry both vials, so
        // moving the code above the Specimen skip cannot have cost a content item.
        assertEquals(2, delivery.getExtensionsByUrl("http://openelis.org/fhir/extension/shipment-content-item").size());
    }

    private static List<BoxSampleItem> panelMaterial() {
        BoxSampleItem damaged = new BoxSampleItem();
        damaged.setEqaPanelSample(panelSample("VL-A"));
        damaged.setReceptionStatus(ReceptionStatus.RECEIVED_DAMAGED);

        BoxSampleItem intact = new BoxSampleItem();
        intact.setEqaPanelSample(panelSample("VL-B"));
        intact.setReceptionStatus(ReceptionStatus.RECEIVED_GOOD);

        return Arrays.asList(damaged, intact);
    }

    private static EQAPanelSample panelSample(String code) {
        EQAPanelSample sample = new EQAPanelSample();
        sample.setSampleCode(code);
        return sample;
    }

    private static ShippingBox box() {
        ShippingBox box = new ShippingBox();
        box.setId(7);
        box.setBoxId("EQA-C5-28-R1");
        box.setFhirUuid(UUID.randomUUID());
        box.setState(BoxState.SENT);
        return box;
    }

    private static SiteInformation siteInfo(String value) {
        SiteInformation info = new SiteInformation();
        info.setValue(value);
        return info;
    }
}
