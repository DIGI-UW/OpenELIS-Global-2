package org.openelisglobal.odoo.config;

import jakarta.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import lombok.Getter;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.stereotype.Component;

@Component
public class TestProductMapping {

    private static final String FIXED_CSV_PATH = "/var/lib/openelis-global/odoo/odoo-test-product-mapping.csv";
    private final Map<String, TestProductInfo> testToProductInfo = new HashMap<>();

    @Getter
    public static class TestProductInfo {
        private final String productName;
        private final double priceUnit;
        private final double quantity;

        public TestProductInfo(String productName, double quantity, double priceUnit) {
            this.productName = productName;
            this.quantity = quantity;
            this.priceUnit = priceUnit;
        }
    }

    @PostConstruct
    public void init() {
        boolean loaded = loadCsvFromFixedPath();
        int mappingsLoaded;
        if (loaded) {
            mappingsLoaded = testToProductInfo.size();
            LogEvent.logInfo(getClass().getSimpleName(), "init",
                    "Loaded CSV mapping. Total mappings loaded: " + mappingsLoaded);
        } else {
            LogEvent.logError(getClass().getSimpleName(), "init",
                    "No CSV mapping file could be loaded from fixed path: " + FIXED_CSV_PATH);
            return;
        }

        if (mappingsLoaded == 0) {
            LogEvent.logWarn(getClass().getSimpleName(), "init", "No valid mappings found.");
        } else {
            LogEvent.logInfo(getClass().getSimpleName(), "init",
                    "Available mapping keys: " + String.join(", ", getAllMappedLoincCodes()));
        }
    }

    private boolean loadCsvFromFixedPath() {
        try (InputStream in = new FileInputStream(FIXED_CSV_PATH)) {
            int count = parseCsv(in);
            LogEvent.logInfo(getClass().getSimpleName(), "loadCsvFromFixedPath",
                    "Loaded CSV mapping from fixed path: " + FIXED_CSV_PATH + ", rows=" + count);
            return count > 0;
        } catch (IOException e) {
            LogEvent.logError(getClass().getSimpleName(), "loadCsvFromFixedPath",
                    "Failed to load CSV from fixed path " + FIXED_CSV_PATH + ": " + e.getMessage());
            return false;
        }
    }

    private int parseCsv(InputStream in) throws IOException {
        CSVFormat format = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).setIgnoreEmptyLines(true)
                .setTrim(true).setIgnoreHeaderCase(true).build();
        int count = 0;
        try (CSVParser parser = CSVParser.parse(in, java.nio.charset.StandardCharsets.UTF_8, format)) {
            for (CSVRecord record : parser) {
                String key = record.get("TEST_NAME").trim();
                String productName = record.get("PRODUCT_NAME").trim();
                String quantityStr = record.get("QUANTITY").trim();
                String priceStr = record.get("PRICE_UNIT").trim();

                if (key.isEmpty() || productName.isEmpty()) {
                    continue;
                }
                try {
                    testToProductInfo.put(key, new TestProductInfo(productName, Double.parseDouble(quantityStr),
                            Double.parseDouble(priceStr)));
                    count++;
                } catch (NumberFormatException ex) {
                    LogEvent.logError(getClass().getSimpleName(), "parseCsv", "Invalid numeric value in row for key '"
                            + key + "': quantity='" + quantityStr + "', price_unit='" + priceStr + "'");
                }
            }
        }
        return count;
    }

    public boolean hasValidMapping(String testCode) {
        boolean hasMapping = testToProductInfo.containsKey(testCode);
        LogEvent.logInfo(getClass().getSimpleName(), "hasValidMapping",
                "Checking testCode='" + testCode + "' -> hasMapping=" + hasMapping);
        return hasMapping;
    }

    public TestProductInfo getProductName(String testCode) {
        return testToProductInfo.get(testCode);
    }

    public Set<String> getAllMappedLoincCodes() {
        return testToProductInfo.keySet();
    }
}
