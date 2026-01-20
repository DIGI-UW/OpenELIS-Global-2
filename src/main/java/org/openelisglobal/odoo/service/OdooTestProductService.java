package org.openelisglobal.odoo.service;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.openelisglobal.odoo.client.OdooClient;
import org.openelisglobal.odoo.client.OdooConnection;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.test.valueholder.TestSection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class OdooTestProductService {

    @Value("${org.openelisglobal.odoo.map.testname.locale:en}")
    private String testMapLocale;

    @Autowired
    private OdooConnection odooConnection;

    @Autowired
    private OdooClient odooClient;

    public void syncTestToOdoo(Test test) {
        if (test == null) {
            log.warn("Cannot sync null test to Odoo");
            return;
        }

        if (!odooConnection.isAvailable()) {
            log.info("Odoo is not available, skipping product sync for test {}", test.getId());
            return;
        }

        try {
            String testCode = resolveTestCode(test);
            String testName = resolveTestName(test);

            if (testName == null || testName.trim().isEmpty()) {
                log.warn("No localized name found for test {}, skipping Odoo product sync", test.getId());
                return;
            }

            // Check if product already exists by default_code
            List<Object> criteria = List.of("default_code", "=", testCode);
            List<String> fields = List.of("id", "name", "default_code");
            Object[] existing = odooConnection.searchAndRead("product.template", criteria, fields);

            Integer existingProductId = extractIdFromSearchResult(existing);

            Map<String, Object> product = new HashMap<>();
            product.put("name", testName);
            product.put("default_code", testCode);
            product.put("type", "service");
            product.put("sale_ok", true);
            product.put("purchase_ok", false);

            if (test.getLoinc() != null && !test.getLoinc().trim().isEmpty()) {
                product.put("loinc_code", test.getLoinc());
            }

            // Derive list_price from the ELIS test price when available, otherwise default
            // to 0.0
            Double listPrice = 0.0;
            java.math.BigDecimal testPrice = test.getPrice();
            if (testPrice != null) {
                listPrice = testPrice.doubleValue();
            }
            product.put("list_price", listPrice);

            // Map ELIS active flag to Odoo 'active' field
            boolean active = test.getIsActive() == null || !"N".equalsIgnoreCase(test.getIsActive());
            product.put("active", active);

            // Map TestSection to product.category (categ_id)
            Integer categoryId = resolveCategoryId(test.getTestSection());
            if (categoryId != null) {
                product.put("categ_id", categoryId);
            }

            if (existingProductId != null) {
                // Update existing product using low-level OdooClient.write
                List<Object> writeParams = List.of(List.of(existingProductId), product);
                Boolean success = odooClient.write("product.template", writeParams);
                log.info("Updated Odoo product with ID {} for test {} ({}), success={}", existingProductId,
                        test.getId(), testCode, success);
            } else {
                // Create new product via OdooConnection abstraction
                Integer productId = odooConnection.create("product.template", List.of(product));
                log.info("Created Odoo product with ID {} for test {} ({})", productId, test.getId(), testCode);
            }
        } catch (Exception e) {
            log.error("Error syncing test {} to Odoo: {}", test.getId(), e.getMessage(), e);
        }
    }

    private String resolveTestCode(Test test) {
        if (test.getGuid() != null && !test.getGuid().trim().isEmpty()) {
            return test.getGuid();
        }
        if (test.getLocalCode() != null && !test.getLocalCode().trim().isEmpty()) {
            return test.getLocalCode();
        }
        if (test.getLoinc() != null && !test.getLoinc().trim().isEmpty()) {
            return test.getLoinc();
        }
        return "OE_TEST_" + test.getId();
    }

    private String resolveTestName(Test test) {
        try {
            if (test.getLocalizedTestName() != null) {
                Locale locale = testMapLocale.equalsIgnoreCase("EN") ? Locale.ENGLISH : Locale.FRENCH;
                String localized = test.getLocalizedTestName().getLocalizedValue(locale);
                if (localized != null && !localized.trim().isEmpty()) {
                    return localized;
                }
            }
        } catch (Exception e) {
            log.warn("Failed to resolve localized test name for test {}: {}", test.getId(), e.getMessage());
        }

        if (test.getDescription() != null && !test.getDescription().trim().isEmpty()) {
            return test.getDescription();
        }

        return "Test " + test.getId();
    }

    private Integer extractIdFromSearchResult(Object[] result) {
        if (result == null || result.length == 0) {
            return null;
        }
        Object first = result[0];
        if (first instanceof Map) {
            Map<?, ?> data = (Map<?, ?>) first;
            Object id = data.get("id");
            if (id instanceof Integer) {
                return (Integer) id;
            }
        }
        return null;
    }

    private Integer resolveCategoryId(TestSection testSection) {
        if (testSection == null) {
            return null;
        }

        String categoryName = null;
        // Prefer the configured test section name as the category label in Odoo,

        if (testSection.getTestSectionName() != null && !testSection.getTestSectionName().trim().isEmpty()) {
            categoryName = testSection.getTestSectionName().trim();
        } else if (testSection.getDescription() != null && !testSection.getDescription().trim().isEmpty()) {
            categoryName = testSection.getDescription().trim();
        }

        if (categoryName == null || categoryName.isEmpty()) {
            return null;
        }

        try {
            // Look for existing product.category with this name
            List<Object> criteria = List.of("name", "=", categoryName);
            List<String> fields = List.of("id");
            Object[] existing = odooConnection.searchAndRead("product.category", criteria, fields);
            Integer existingId = extractIdFromSearchResult(existing);
            if (existingId != null) {
                return existingId;
            }

            // Create new category
            Map<String, Object> category = new HashMap<>();
            category.put("name", categoryName);
            Integer categoryId = odooConnection.create("product.category", List.of(category));
            log.info("Created Odoo product category with ID {} for test section {}", categoryId, categoryName);
            return categoryId;
        } catch (Exception e) {
            log.warn("Failed to resolve or create Odoo category for test section {}: {}", categoryName,
                    e.getMessage());
            return null;
        }
    }
}
