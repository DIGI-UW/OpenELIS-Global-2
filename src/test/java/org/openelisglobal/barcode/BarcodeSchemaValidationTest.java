package org.openelisglobal.barcode;

import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

/**
 * Validates barcode schema expectations from existing OGC-284 Liquibase changes
 * by checking changeset contents and inclusion wiring.
 */
public class BarcodeSchemaValidationTest {

    @Test
    public void testSampleBarcodeInfoSchemaColumnsExist() throws Exception {
        Set<String> expectedColumns = new HashSet<>(Arrays.asList("id", "sample_id", "print_order_num"));
        String changesetXml = readClassPathFile("liquibase/3.3.x.x/028-barcode-info-tables.xml");
        for (String column : expectedColumns) {
            assertTrue("028-barcode-info-tables.xml should define sample_barcode_info column " + column,
                    changesetXml.contains("name=\"" + column + "\""));
        }
    }

    @Test
    public void testSampleItemBarcodeInfoSchemaColumnsExist() throws Exception {
        Set<String> expectedColumns = new HashSet<>(Arrays.asList("id", "sample_item_id", "print_specimen_num",
                "print_block_num", "print_slide_num", "print_freezer_num"));
        String changesetXml = readClassPathFile("liquibase/3.3.x.x/028-barcode-info-tables.xml");
        for (String column : expectedColumns) {
            assertTrue("028-barcode-info-tables.xml should define sample_item_barcode_info column " + column,
                    changesetXml.contains("name=\"" + column + "\""));
        }
    }

    @Test
    public void testBaseLiquibaseIncludesBarcodeChangesets() throws Exception {
        String baseXml = readClassPathFile("liquibase/3.3.x.x/base.xml");
        assertTrue("base.xml should include 028-barcode-info-tables.xml",
                baseXml.contains("028-barcode-info-tables.xml"));
        assertTrue("base.xml should include barcode_expansion.xml", baseXml.contains("barcode_expansion.xml"));
    }

    private String readClassPathFile(String path) throws Exception {
        ClassPathResource resource = new ClassPathResource(path);
        try (InputStream inputStream = resource.getInputStream()) {
            return StreamUtils.copyToString(inputStream, java.nio.charset.StandardCharsets.UTF_8);
        }
    }
}
