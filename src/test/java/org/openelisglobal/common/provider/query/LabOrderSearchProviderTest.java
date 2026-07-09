package org.openelisglobal.common.provider.query;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.UUID;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.hl7.fhir.r4.model.Specimen;
import org.hl7.fhir.r4.model.Task;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.services.StatusService.ExternalOrderStatus;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrder;
import org.openelisglobal.dataexchange.service.order.ElectronicOrderService;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

public class LabOrderSearchProviderTest extends BaseWebContextSensitiveTest {

    @Autowired
    private ElectronicOrderService electronicOrderService;

    @Autowired
    private TypeOfSampleService typeOfSampleService;

    @Autowired
    private FhirConfig fhirConfig;

    private LabOrderSearchProvider provider;

    private static final String LOINC_MERCURY = "5685-3";
    private static final String LOINC_LEAD = "7439-92";
    private static final String OE_FHIR_SYSTEM = "http://openelis-global.org";

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/lab-order-search.xml");
        typeOfSampleService.clearCache();

        org.mockito.Mockito.when(fhirConfig.getOeFhirSystem()).thenReturn(OE_FHIR_SYSTEM);

        provider = new LabOrderSearchProvider();
    }

    @Test
    public void singleTestMultipleSampleTypes_specimenDisambiguates() {
        String orderNumber = "ORD-MULTI-SAMPLE";
        List<ElectronicOrder> eOrders = electronicOrderService.getElectronicOrdersByExternalId(orderNumber);
        assertFalse("eOrders should not be empty", eOrders.isEmpty());
        ElectronicOrder eOrder = eOrders.get(eOrders.size() - 1);

        ServiceRequest serviceRequest = new ServiceRequest();
        serviceRequest.setId(UUID.randomUUID().toString());
        serviceRequest.setCode(
                new CodeableConcept().addCoding(new Coding().setSystem("http://loinc.org").setCode(LOINC_MERCURY)));

        Specimen specimen = new Specimen();
        specimen.setId(UUID.randomUUID().toString());
        specimen.setType(
                new CodeableConcept().addCoding(new Coding().setSystem(OE_FHIR_SYSTEM + "/sampleType").setCode("DW9")));

        Patient patient = new Patient();
        patient.setId(UUID.randomUUID().toString());
        patient.addIdentifier(new Identifier().setSystem(OE_FHIR_SYSTEM + "/pat_guid").setValue("test-guid-9001"));

        Task task = buildMinimalTask();

        ReflectionTestUtils.setField(provider, "serviceRequest", serviceRequest);
        ReflectionTestUtils.setField(provider, "specimen", specimen);
        ReflectionTestUtils.setField(provider, "eOrders", eOrders);
        ReflectionTestUtils.setField(provider, "eOrder", eOrder);
        ReflectionTestUtils.setField(provider, "eOrderStatus", ExternalOrderStatus.Entered);
        ReflectionTestUtils.setField(provider, "patient", patient);
        ReflectionTestUtils.setField(provider, "task", task);

        StringBuilder xml = new StringBuilder();
        String result = ReflectionTestUtils.invokeMethod(provider, "createSearchResultXML", orderNumber, xml);

        String xmlStr = xml.toString();
        assertTrue("Result should be valid, was: " + result, "valid".equalsIgnoreCase(result));
        assertTrue(
                "XML should contain sampleType entries between <sampleTypes> tags (bug: single test with "
                        + "multiple sample types not disambiguated). XML was: " + xmlStr,
                xmlStr.contains("<sampleType>") && xmlStr.contains("<sampleTypes>"));
        assertTrue("XML should contain the Drinking Water sample type id",
                xmlStr.contains("Drinking Water") || xmlStr.contains("9003"));
    }

    @Test
    public void singleTestSingleSampleType_resolves() {
        String orderNumber = "ORD-SINGLE-SAMPLE";
        List<ElectronicOrder> eOrders = electronicOrderService.getElectronicOrdersByExternalId(orderNumber);
        assertFalse("eOrders should not be empty", eOrders.isEmpty());
        ElectronicOrder eOrder = eOrders.get(eOrders.size() - 1);

        ServiceRequest serviceRequest = new ServiceRequest();
        serviceRequest.setId(UUID.randomUUID().toString());
        serviceRequest.setCode(
                new CodeableConcept().addCoding(new Coding().setSystem("http://loinc.org").setCode(LOINC_LEAD)));

        Patient patient = new Patient();
        patient.setId(UUID.randomUUID().toString());
        patient.addIdentifier(new Identifier().setSystem(OE_FHIR_SYSTEM + "/pat_guid").setValue("test-guid-9001"));

        Task task = buildMinimalTask();

        ReflectionTestUtils.setField(provider, "serviceRequest", serviceRequest);
        ReflectionTestUtils.setField(provider, "specimen", (Specimen) null);
        ReflectionTestUtils.setField(provider, "eOrders", eOrders);
        ReflectionTestUtils.setField(provider, "eOrder", eOrder);
        ReflectionTestUtils.setField(provider, "eOrderStatus", ExternalOrderStatus.Entered);
        ReflectionTestUtils.setField(provider, "patient", patient);
        ReflectionTestUtils.setField(provider, "task", task);

        StringBuilder xml = new StringBuilder();
        String result = ReflectionTestUtils.invokeMethod(provider, "createSearchResultXML", orderNumber, xml);

        String xmlStr = xml.toString();
        assertTrue("Result should be valid, was: " + result, "valid".equalsIgnoreCase(result));
        assertTrue("XML should contain sampleType entries for single-sample-type test. XML was: " + xmlStr,
                xmlStr.contains("<sampleType>") && xmlStr.contains("<sampleTypes>"));
    }

    /**
     * Specimen has an abbreviation ("AIMI") that doesn't exist locally, but its
     * display ("Drinking Water") matches a configured sample type description. The
     * provider should fall back to display matching and select the correct type.
     *
     * FAILS until addToTestOrPanel uses the display as a fallback.
     */
    @Test
    public void unknownAbbreviation_fallsBackToDisplayMatch() {
        String orderNumber = "ORD-MULTI-SAMPLE";
        List<ElectronicOrder> eOrders = electronicOrderService.getElectronicOrdersByExternalId(orderNumber);
        assertFalse("eOrders should not be empty", eOrders.isEmpty());
        ElectronicOrder eOrder = eOrders.get(eOrders.size() - 1);

        ServiceRequest serviceRequest = new ServiceRequest();
        serviceRequest.setId(UUID.randomUUID().toString());
        serviceRequest.setCode(
                new CodeableConcept().addCoding(new Coding().setSystem("http://loinc.org").setCode(LOINC_MERCURY)));

        // "AIMI" doesn't exist locally — but display "Drinking Water" matches DW9
        Specimen specimen = new Specimen();
        specimen.setId(UUID.randomUUID().toString());
        specimen.setType(new CodeableConcept().addCoding(
                new Coding().setSystem(OE_FHIR_SYSTEM + "/sampleType").setCode("AIMI").setDisplay("Drinking Water")));

        Patient patient = new Patient();
        patient.setId(UUID.randomUUID().toString());
        patient.addIdentifier(new Identifier().setSystem(OE_FHIR_SYSTEM + "/pat_guid").setValue("test-guid-9001"));

        Task task = buildMinimalTask();

        ReflectionTestUtils.setField(provider, "serviceRequest", serviceRequest);
        ReflectionTestUtils.setField(provider, "specimen", specimen);
        ReflectionTestUtils.setField(provider, "eOrders", eOrders);
        ReflectionTestUtils.setField(provider, "eOrder", eOrder);
        ReflectionTestUtils.setField(provider, "eOrderStatus", ExternalOrderStatus.Entered);
        ReflectionTestUtils.setField(provider, "patient", patient);
        ReflectionTestUtils.setField(provider, "task", task);

        StringBuilder xml = new StringBuilder();
        String result = ReflectionTestUtils.invokeMethod(provider, "createSearchResultXML", orderNumber, xml);

        String xmlStr = xml.toString();
        assertTrue("Result should be valid, was: " + result, "valid".equalsIgnoreCase(result));
        assertTrue("XML should contain populated sampleTypes. XML was: " + xmlStr,
                xmlStr.contains("<sampleType>") && xmlStr.contains("<sampleTypes>"));
        assertTrue("XML should contain Drinking Water (matched by display, not abbreviation). XML was: " + xmlStr,
                xmlStr.contains("Drinking Water"));
    }

    private Task buildMinimalTask() {
        Task task = new Task();
        task.setId(UUID.randomUUID().toString());
        String practitionerId = UUID.randomUUID().toString();
        task.setOwner(new Reference("Practitioner/" + practitionerId));
        task.getRestriction().addRecipient(new Reference("Organization/" + UUID.randomUUID().toString()));
        task.setLocation(new Reference("Location/" + UUID.randomUUID().toString()));
        return task;
    }
}
