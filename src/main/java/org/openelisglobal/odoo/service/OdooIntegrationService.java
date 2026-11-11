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
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
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

    @Lazy
    @Autowired
    private TestService testService;

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
                throw new OdooOperationException("No sample to find the patient of");
            }

            Patient patient = sampleHumanService.getPatientForSample(sample);
            if (patient == null) {
                throw new OdooOperationException("No patient for sample " + sample.getAccessionNumber());
            }

            Person person = patient.getPerson();
            if (person == null) {
                throw new OdooOperationException("No person for patient " + patient.getId());
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
                throw new OdooOperationException("Odoo returned no customer id for patient " + patient.getId());
            }

            log.info("Created new partner in Odoo with ID: {} for patient: {} {}", partnerId, person.getFirstName(),
                    person.getLastName());
            return partnerId;

        } catch (OdooOperationException e) {
            throw e;
        } catch (Exception e) {
            throw new OdooOperationException("Could not find or create the patient's customer in Odoo", e);
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

        Integer countryId = findRecordByCodeOrName("res.country", person.getCountry(), null);
        if (countryId != null) {
            partnerData.put("country_id", countryId);
        }

        Integer stateId = findRecordByCodeOrName("res.country.state", person.getState(), countryId);
        if (stateId != null) {
            partnerData.put("state_id", stateId);
        }

        if (person.getZipCode() != null && !person.getZipCode().trim().isEmpty()) {
            partnerData.put("zip", person.getZipCode());
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
                        Integer productId = findProductId(productInfo.getProductName());
                        if (productId != null) {
                            invoiceLine.put("product_id", productId);
                        }
                        invoiceLine.put("name", productInfo.getProductName());
                        invoiceLine.put("quantity", productInfo.getQuantity());
                        invoiceLine.put("price_unit", productInfo.getPriceUnit());
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

    /**
     * The Odoo product named like the mapping's product, so the line takes that
     * product's income account and taxes; without one, Odoo books the line to the
     * sales journal's default income account.
     */
    private Integer findProductId(String productName) {
        if (productName == null || productName.isBlank()) {
            return null;
        }
        return firstId(odooConnection.searchAndRead("product.product", List.of("name", "=", productName.trim()),
                List.of("id")), null);
    }

    /**
     * A country or state given as its code (UG) or its name (Uganda), matched
     * case-insensitively; a state is only taken from the patient's country when
     * that is known. Odoo stores both as record ids, so text it does not know is
     * left off the customer rather than failing the invoice.
     */
    private Integer findRecordByCodeOrName(String model, String value, Integer countryId) {
        if (value == null || value.isBlank()) {
            return null;
        }
        List<String> fields = countryId == null ? List.of("id") : List.of("id", "country_id");
        for (String field : List.of("code", "name")) {
            Integer id = firstId(odooConnection.searchAndRead(model, List.of(field, "=ilike", value.trim()), fields),
                    countryId);
            if (id != null) {
                return id;
            }
        }
        log.warn("No {} in Odoo matches '{}'; the customer is created without it", model, value);
        return null;
    }

    private static Integer firstId(Object[] records, Integer countryId) {
        if (records == null) {
            return null;
        }
        for (Object record : records) {
            if (!(record instanceof Map)) {
                continue;
            }
            Map<?, ?> fields = (Map<?, ?>) record;
            if (countryId != null && !sameCountry(fields.get("country_id"), countryId)) {
                continue;
            }
            Object id = fields.get("id");
            if (id instanceof Integer) {
                return (Integer) id;
            }
        }
        return null;
    }

    private static boolean sameCountry(Object many2one, Integer countryId) {
        Object id = many2one instanceof Object[] && ((Object[]) many2one).length > 0 ? ((Object[]) many2one)[0]
                : many2one;
        return countryId.equals(id);
    }
}
