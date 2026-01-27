package org.openelisglobal.odoo.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openelisglobal.common.services.SampleAddService.SampleTestCollection;
import org.openelisglobal.odoo.client.OdooConnection;
import org.openelisglobal.odoo.config.TestProductMapping;
import org.openelisglobal.odoo.exception.OdooOperationException;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.person.valueholder.Person;
import org.openelisglobal.sample.action.util.SamplePatientUpdateData;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service class for integrating OpenELIS with Odoo for billing functionality.
 * This service handles the creation of invoices in Odoo when orders are created
 * in OpenELIS.
 */
@Service
public class OdooIntegrationService {

    private static final Logger log = LogManager.getLogger(OdooIntegrationService.class);
    @Value("${org.openelisglobal.odoo.map.testname.locale:en}")
    private String testMapLocale;

    @Autowired
    private OdooConnection odooConnection;

    @Autowired
    private TestProductMapping testProductMapping;

    @Autowired
    private SampleHumanService sampleHumanService;

    @Autowired
    private PatientService patientService;

    /**
     * Creates an invoice in Odoo for the given sample data.
     * 
     * @param updateData The sample data containing order information
     * @throws OdooOperationException if there's an error creating the invoice
     */
    public void createInvoice(SamplePatientUpdateData updateData) {
        // Check if Odoo connection is available
        if (!odooConnection.isAvailable()) {
            log.info("Odoo connection is not available. Skipping invoice creation for sample: {}",
                    updateData.getAccessionNumber());
            return;
        }

        try {
            Map<String, Object> invoiceData = createInvoiceData(updateData);
            Integer invoiceId = odooConnection.create("account.move", List.of(invoiceData));
            if (invoiceId == null) {
                throw new OdooOperationException(
                        "Odoo returned null invoice ID for sample: " + updateData.getAccessionNumber());
            }
            log.info("Successfully created invoice in Odoo with ID: {} for sample: {}", invoiceId,
                    updateData.getAccessionNumber());
        } catch (Exception e) {
            log.error("Error creating invoice in Odoo for sample {}: {}", updateData.getAccessionNumber(),
                    e.getMessage(), e);
            throw new OdooOperationException("Failed to create invoice in Odoo", e);
        }
    }

    private Map<String, Object> createInvoiceData(SamplePatientUpdateData updateData) {
        Map<String, Object> invoiceData = new HashMap<>();
        invoiceData.put("move_type", "out_invoice");

        Integer partnerId = getOrCreatePatientPartner(updateData);
        invoiceData.put("partner_id", partnerId);

        invoiceData.put("invoice_date", java.time.LocalDate.now().toString());
        invoiceData.put("ref", "OE-" + updateData.getAccessionNumber());

        List<Object> formattedInvoiceLines = new ArrayList<>();
        List<Map<String, Object>> invoiceLines = createInvoiceLines(updateData);
        for (Map<String, Object> line : invoiceLines) {
            formattedInvoiceLines.add(List.of(0, 0, line));
        }
        invoiceData.put("invoice_line_ids", formattedInvoiceLines);
        return invoiceData;
    }

    /**
     * Gets or creates a partner in Odoo for the patient associated with the sample.
     * 
     * @param updateData The sample data containing patient information
     * @return The partner ID in Odoo
     */
    private Integer getOrCreatePatientPartner(SamplePatientUpdateData updateData) {
        try {
            Sample sample = updateData.getSample();
            if (sample == null) {
                log.warn("No sample found in updateData, using default partner ID 1");
                return 1;
            }

            Patient patient = sampleHumanService.getPatientForSample(sample);
            if (patient == null) {
                log.warn("No patient found for sample {}, using default partner ID 1", sample.getAccessionNumber());
                return 1;
            }

            Person person = patient.getPerson();
            if (person == null) {
                log.warn("No person found for patient {}, using default partner ID 1", patient.getId());
                return 1;
            }

            String nationalId = patientService.getNationalId(patient);
            if (nationalId != null && !nationalId.trim().isEmpty()) {
                Integer existingPartnerId = findPartnerByNationalId(nationalId);
                if (existingPartnerId != null) {
                    log.info("Found existing partner with national ID {}: {}", nationalId, existingPartnerId);
                    return existingPartnerId;
                } else {
                    // Try alternative search by name as fallback
                    existingPartnerId = findPartnerByName(person.getFirstName(), person.getLastName());
                    if (existingPartnerId != null) {
                        log.info("Found existing partner by name for national ID {}: {}", nationalId,
                                existingPartnerId);
                        return existingPartnerId;
                    }
                }
            }

            Map<String, Object> partnerData = createPartnerData(patient, person);
            Integer partnerId = odooConnection.create("res.partner", List.of(partnerData));

            if (partnerId == null) {
                log.warn("Failed to create partner for patient {}, using default partner ID 1", patient.getId());
                return 1;
            }

            log.info("Created new partner in Odoo with ID: {} for patient: {} {}", partnerId, person.getFirstName(),
                    person.getLastName());
            return partnerId;

        } catch (Exception e) {
            log.error("Error getting or creating patient partner: {}", e.getMessage(), e);
            return 1;
        }
    }

    /**
     * Finds a partner in Odoo by national ID.
     * 
     * @param nationalId The patient's national ID
     * @return The partner ID if found, null otherwise
     */
    private Integer findPartnerByNationalId(String nationalId) {
        try {
            List<Object> criteria = List.of("ref", "=", nationalId);
            List<String> fields = List.of("id");

            log.debug("Searching for partner with national ID: {} using criteria: {}", nationalId, criteria);

            Object[] result = odooConnection.searchAndRead("res.partner", criteria, fields);

            log.debug("Search result for national ID {}: {} records found", nationalId,
                    result != null ? result.length : 0);

            if (result != null && result.length > 0) {
                Object firstResult = result[0];
                log.debug("First result type: {}, content: {}", firstResult.getClass().getSimpleName(), firstResult);

                if (firstResult instanceof Map) {
                    Map<?, ?> partnerData = (Map<?, ?>) firstResult;
                    Object id = partnerData.get("id");
                    if (id instanceof Integer) {
                        log.info("Found existing partner with national ID {}: partner ID {}", nationalId, id);
                        return (Integer) id;
                    } else {
                        log.warn("Partner ID is not an Integer: {} (type: {})", id,
                                id != null ? id.getClass() : "null");
                    }
                } else {
                    log.warn("First result is not a Map: {} (type: {})", firstResult, firstResult.getClass());
                }
            } else {
                log.debug("No partner found with national ID: {}", nationalId);
            }

            return null;
        } catch (Exception e) {
            log.error("Error searching for partner by national ID {}: {}", nationalId, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Finds a partner in Odoo by name as a fallback.
     * 
     * @param firstName The patient's first name
     * @param lastName  The patient's last name
     * @return The partner ID if found, null otherwise
     */
    private Integer findPartnerByName(String firstName, String lastName) {
        if (firstName == null && lastName == null) {
            return null;
        }

        try {
            String fullName = (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");
            fullName = fullName.trim();

            if (fullName.isEmpty()) {
                return null;
            }

            List<Object> criteria = List.of("name", "ilike", fullName);
            List<String> fields = List.of("id", "name");

            log.debug("Searching for partner by name: '{}' using criteria: {}", fullName, criteria);

            Object[] result = odooConnection.searchAndRead("res.partner", criteria, fields);

            log.debug("Search result for name '{}': {} records found", fullName, result != null ? result.length : 0);

            if (result != null && result.length > 0) {
                Object firstResult = result[0];
                if (firstResult instanceof Map) {
                    Map<?, ?> partnerData = (Map<?, ?>) firstResult;
                    Object id = partnerData.get("id");
                    if (id instanceof Integer) {
                        log.info("Found existing partner by name '{}': partner ID {}", fullName, id);
                        return (Integer) id;
                    }
                }
            }

            return null;
        } catch (Exception e) {
            log.error("Error searching for partner by name {}: {}", firstName + " " + lastName, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Creates partner data for Odoo from patient and person information.
     * 
     * @param patient The patient
     * @param person  The person associated with the patient
     * @return Map containing partner data for Odoo
     */
    private Map<String, Object> createPartnerData(Patient patient, Person person) {
        Map<String, Object> partnerData = new HashMap<>();

        String firstName = person.getFirstName();
        String lastName = person.getLastName();
        String fullName = (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");
        partnerData.put("name", fullName.trim());

        String nationalId = patientService.getNationalId(patient);
        if (nationalId != null && !nationalId.trim().isEmpty()) {
            partnerData.put("ref", nationalId);
        }

        if (person.getEmail() != null && !person.getEmail().trim().isEmpty()) {
            partnerData.put("email", person.getEmail());
        }

        if (person.getPrimaryPhone() != null && !person.getPrimaryPhone().trim().isEmpty()) {
            partnerData.put("phone", person.getPrimaryPhone());
        } else if (person.getCellPhone() != null && !person.getCellPhone().trim().isEmpty()) {
            partnerData.put("phone", person.getCellPhone());
        } else if (person.getHomePhone() != null && !person.getHomePhone().trim().isEmpty()) {
            partnerData.put("phone", person.getHomePhone());
        }

        if (person.getStreetAddress() != null && !person.getStreetAddress().trim().isEmpty()) {
            partnerData.put("street", person.getStreetAddress());
        }

        if (person.getCity() != null && !person.getCity().trim().isEmpty()) {
            partnerData.put("city", person.getCity());
        }

        if (person.getState() != null && !person.getState().trim().isEmpty()) {
            partnerData.put("state_id", person.getState());
        }

        if (person.getZipCode() != null && !person.getZipCode().trim().isEmpty()) {
            partnerData.put("zip", person.getZipCode());
        }

        if (person.getCountry() != null && !person.getCountry().trim().isEmpty()) {
            partnerData.put("country_id", person.getCountry());
        }

        partnerData.put("customer_rank", 1);
        partnerData.put("is_company", false);
        partnerData.put("comment", "OpenELIS Patient ID: " + patient.getId());

        return partnerData;
    }

    private List<Map<String, Object>> createInvoiceLines(SamplePatientUpdateData updateData) {
        List<Map<String, Object>> invoiceLines = new ArrayList<>();

        log.info("Available test mappings: {}", testProductMapping.getAllMappedLoincCodes());

        if (updateData.getSampleItemsTests() != null) {
            for (SampleTestCollection sampleTest : updateData.getSampleItemsTests()) {
                for (Test test : sampleTest.tests) {
                    TestService testService = SpringContext.getBean(TestService.class);
                    String testName = testService.get(test.getId()).getLocalizedTestName()
                            .getLocalizedValue(testMapLocale.equalsIgnoreCase("EN") ? Locale.ENGLISH : Locale.FRENCH);

                    log.info("Processing test: Name={}", testName);

                    String mappingKey = null;
                    if (testProductMapping.hasValidMapping(testName)) {
                        mappingKey = testName;
                    }

                    if (mappingKey != null) {
                        TestProductMapping.TestProductInfo productInfo = testProductMapping.getProductName(mappingKey);
                        Map<String, Object> invoiceLine = new HashMap<>();
                        invoiceLine.put("name", productInfo.getProductName());
                        invoiceLine.put("quantity", productInfo.getQuantity());
                        invoiceLine.put("price_unit", productInfo.getPriceUnit());
                        invoiceLine.put("account_id", 1);
                        invoiceLines.add(invoiceLine);
                        log.info(
                                "Added invoice line for test: {} (mapped from: {}) with product: {}, quantity: {}, price: {}",
                                testName, mappingKey, productInfo.getProductName(), productInfo.getQuantity(),
                                productInfo.getPriceUnit());
                    } else {
                        log.warn("No Odoo product mapping found for test: {}", testName);
                    }
                }
            }
        }
        return invoiceLines;
    }
}
