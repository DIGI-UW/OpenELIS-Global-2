package org.openelisglobal.odoo.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.services.SampleAddService;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.odoo.client.OdooConnection;
import org.openelisglobal.odoo.config.TestProductMapping;
import org.openelisglobal.odoo.config.TestProductMapping.TestProductInfo;
import org.openelisglobal.odoo.exception.OdooOperationException;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.person.valueholder.Person;
import org.openelisglobal.sample.action.util.SamplePatientUpdateData;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.test.service.TestService;
import org.springframework.test.util.ReflectionTestUtils;

public class OdooInvoiceDataTest extends BaseWebContextSensitiveTest {

    private OdooConnection odoo;
    private TestProductMapping mapping;
    private SampleHumanService sampleHumanService;
    private PatientService patientService;
    private TestService testService;
    private OdooIntegrationService service;
    private Person person;

    @Before
    public void setUp() {
        odoo = mock(OdooConnection.class);
        mapping = mock(TestProductMapping.class);
        sampleHumanService = mock(SampleHumanService.class);
        patientService = mock(PatientService.class);
        testService = mock(TestService.class);
        service = new OdooIntegrationService();
        ReflectionTestUtils.setField(service, "odooConnection", odoo);
        ReflectionTestUtils.setField(service, "testProductMapping", mapping);
        ReflectionTestUtils.setField(service, "sampleHumanService", sampleHumanService);
        ReflectionTestUtils.setField(service, "patientService", patientService);
        ReflectionTestUtils.setField(service, "testService", testService);
        ReflectionTestUtils.setField(service, "testMapLocale", "en");

        when(odoo.isAvailable()).thenReturn(true);
        when(odoo.searchAndRead(any(), anyList(), anyList())).thenReturn(new Object[0]);
        when(mapping.getAllMappedLoincCodes()).thenReturn(Set.of("ARV resistance"));
        when(mapping.hasValidMapping("ARV resistance")).thenReturn(true);
        when(mapping.getProductName("ARV resistance")).thenReturn(new TestProductInfo("ARV resistance", 1, 57));

        person = new Person();
        person.setFirstName("Invoice");
        person.setLastName("Odootest");
        Patient patient = new Patient();
        patient.setId("11");
        patient.setPerson(person);
        when(sampleHumanService.getPatientForSample(any())).thenReturn(patient);
        when(patientService.getNationalId(patient)).thenReturn("NID-1");
        when(odoo.create(eq("res.partner"), anyList())).thenReturn(7);
        when(odoo.create(eq("account.move"), anyList())).thenReturn(1);
    }

    @Test
    public void invoiceLineUsesTheOdooProductAndNoFixedAccount() {
        when(odoo.searchAndRead("product.product", List.of("name", "=", "ARV resistance"), List.of("id")))
                .thenReturn(new Object[] { Map.of("id", 31) });

        Map<?, ?> line = createInvoiceAndReturnFirstLine();

        assertEquals(31, line.get("product_id"));
        assertFalse("no hard-coded account", line.containsKey("account_id"));
        assertEquals("ARV resistance", line.get("name"));
        assertEquals(57.0, line.get("price_unit"));
    }

    @Test
    public void lineWithoutAMatchingProductLeavesTheAccountToOdoo() {
        Map<?, ?> line = createInvoiceAndReturnFirstLine();

        assertFalse(line.containsKey("product_id"));
        assertFalse(line.containsKey("account_id"));
        assertEquals(57.0, line.get("price_unit"));
    }

    @Test
    public void customerCountryAndStateAreSentAsOdooRecordIds() {
        person.setCountry("Uganda");
        person.setState("Central");
        when(odoo.searchAndRead("res.country", List.of("name", "=ilike", "Uganda"), List.of("id")))
                .thenReturn(new Object[] { Map.of("id", 225) });
        when(odoo.searchAndRead("res.country.state", List.of("name", "=ilike", "Central"), List.of("id", "country_id")))
                .thenReturn(new Object[] { Map.of("id", 900, "country_id", new Object[] { 12, "Elsewhere" }),
                        Map.of("id", 901, "country_id", new Object[] { 225, "Uganda" }) });

        Map<?, ?> customer = createInvoiceAndReturnCustomer();

        assertEquals(225, customer.get("country_id"));
        assertEquals(901, customer.get("state_id"));
    }

    @Test
    public void unknownCountryAndStateAreLeftOffTheCustomer() {
        person.setCountry("Atlantis");
        person.setState("Nowhere");

        Map<?, ?> customer = createInvoiceAndReturnCustomer();

        assertFalse(customer.containsKey("country_id"));
        assertFalse(customer.containsKey("state_id"));
    }

    @Test
    public void customerFailureCreatesNoInvoiceForTheLabItself() {
        when(odoo.create(eq("res.partner"), anyList())).thenThrow(new RuntimeException("Invalid field"));

        try {
            service.createInvoice(order());
            fail("expected the invoice to fail");
        } catch (OdooOperationException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("Failed to create invoice"));
        }
        verify(odoo, never()).create(eq("account.move"), anyList());
    }

    @Test
    public void aNamesakeCustomerIsNeverReused() {
        when(odoo.searchAndRead(eq("res.partner"), anyList(), anyList()))
                .thenAnswer(call -> List.of("ref", "=", "NID-1").equals(call.getArgument(1)) ? new Object[0]
                        : new Object[] { Map.of("id", 8, "name", "Joann Leeds") });

        Map<?, ?> customer = createInvoiceAndReturnCustomer();

        assertEquals("NID-1", customer.get("ref"));
        verify(odoo, never()).searchAndRead(eq("res.partner"), eq(List.of("name", "ilike", "Invoice Odootest")),
                anyList());
    }

    @Test
    public void theCustomerWithThePatientsNationalIdIsReused() {
        when(odoo.searchAndRead("res.partner", List.of("ref", "=", "NID-1"), List.of("id")))
                .thenReturn(new Object[] { Map.of("id", 7) });

        service.createInvoice(order());

        verify(odoo, never()).create(eq("res.partner"), anyList());
        assertEquals(7, createdInvoice().get("partner_id"));
    }

    @Test
    public void aPatientWithoutNationalIdIsKeptAsOneCustomerByPatientId() {
        when(patientService.getNationalId(any())).thenReturn(null);

        Map<?, ?> customer = createInvoiceAndReturnCustomer();

        assertEquals("OE-PATIENT-11", customer.get("ref"));
        verify(odoo).searchAndRead("res.partner", List.of("ref", "=", "OE-PATIENT-11"), List.of("id"));
    }

    @Test
    public void aFailedCustomerSearchFailsTheInvoiceInsteadOfCreatingADuplicate() {
        when(odoo.searchAndRead(eq("res.partner"), anyList(), anyList()))
                .thenThrow(new RuntimeException("Odoo is down"));

        try {
            service.createInvoice(order());
            fail("expected the invoice to fail");
        } catch (OdooOperationException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("Failed to create invoice"));
        }
        verify(odoo, never()).create(eq("res.partner"), anyList());
        verify(odoo, never()).create(eq("account.move"), anyList());
    }

    @Test
    public void aStateIsOnlyLookedUpWithinAKnownCountry() {
        person.setState("CA");
        when(odoo.searchAndRead(eq("res.country.state"), anyList(), anyList()))
                .thenReturn(new Object[] { Map.of("id", 13, "country_id", new Object[] { 233, "United States" }) });

        Map<?, ?> customer = createInvoiceAndReturnCustomer();

        assertFalse(customer.containsKey("state_id"));
    }

    private Map<?, ?> createdInvoice() {
        @SuppressWarnings({ "unchecked", "rawtypes" })
        ArgumentCaptor<List<Map<String, Object>>> invoice = ArgumentCaptor.forClass((Class) List.class);
        verify(odoo).create(eq("account.move"), invoice.capture());
        return (Map<?, ?>) invoice.getValue().get(0);
    }

    private Map<?, ?> createInvoiceAndReturnFirstLine() {
        service.createInvoice(order());
        @SuppressWarnings({ "unchecked", "rawtypes" })
        ArgumentCaptor<List<Map<String, Object>>> invoice = ArgumentCaptor.forClass((Class) List.class);
        verify(odoo).create(eq("account.move"), invoice.capture());
        Map<?, ?> data = (Map<?, ?>) invoice.getValue().get(0);
        List<?> lines = (List<?>) data.get("invoice_line_ids");
        assertEquals(1, lines.size());
        return (Map<?, ?>) ((List<?>) lines.get(0)).get(2);
    }

    private Map<?, ?> createInvoiceAndReturnCustomer() {
        service.createInvoice(order());
        @SuppressWarnings({ "unchecked", "rawtypes" })
        ArgumentCaptor<List<Map<String, Object>>> customer = ArgumentCaptor.forClass((Class) List.class);
        verify(odoo).create(eq("res.partner"), customer.capture());
        return (Map<?, ?>) customer.getValue().get(0);
    }

    private SamplePatientUpdateData order() {
        org.openelisglobal.test.valueholder.Test test = new org.openelisglobal.test.valueholder.Test();
        test.setId("38");
        Localization name = new Localization();
        name.setLocalizedValue("en", "ARV resistance");
        test.setLocalizedTestName(name);
        when(testService.get("38")).thenReturn(test);

        Sample sample = new Sample();
        sample.setAccessionNumber("DEV01260000000000002");
        SamplePatientUpdateData updateData = new SamplePatientUpdateData("1");
        updateData.setSample(sample);
        updateData.setAccessionNumber("DEV01260000000000002");
        SampleAddService outer = new SampleAddService(null, "1", sample, null);
        updateData.setSampleItemsTests(
                List.of(outer.new SampleTestCollection(new SampleItem(), List.of(test), null, null, null, null, null)));
        return updateData;
    }
}
