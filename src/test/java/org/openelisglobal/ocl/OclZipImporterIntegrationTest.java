package org.openelisglobal.ocl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class OclZipImporterIntegrationTest extends BaseWebContextSensitiveTest {
    private static final Logger log = LoggerFactory.getLogger(OclZipImporterIntegrationTest.class);

    @Autowired
    private OclZipImporter oclZipImporter;

    private static String oclDirPath;

    @Before
    public void setUp() {
        if (oclZipImporter == null) {
            fail("OclZipImporter bean not autowired. Check Spring configuration.");
        }
        oclDirPath = this.getClass().getClassLoader().getResource("ocl").getFile();

    }

    @Test
    public void testImportOclPackage_validZip() throws IOException {
        byte[] zipData = createZipWithFiles(new String[] { "file1.json", "{\"name\": \"file1\"}" },
                new String[] { "file2.json", "{\"name\": \"file2\"}" });
        String tempZipPath = createTempZipFile(zipData);
        List<JsonNode> nodes = new ArrayList<>();
        oclZipImporter.importOclPackage(tempZipPath, nodes);
        log.info("Parsed {} nodes", nodes.size());
        for (JsonNode node : nodes) {
            log.info("Node content: {}", node.toPrettyString());
        }

        assertEquals("Should import two JSON files", 2, nodes.size());
        assertEquals("First JSON should have name 'file1'", "file1", nodes.get(0).get("name").asText());
        assertEquals("Second JSON should have name 'file2'", "file2", nodes.get(1).get("name").asText());
    }

    @Test
    public void testImportOclPackage_zipWithNonJson() throws IOException {
        byte[] zipData = createZipWithFiles(new String[] { "file1.json", "{\"name\": \"file1\"}" },
                new String[] { "file2.txt", "This is a text file" });
        String tempZipPath = createTempZipFile(zipData);

        List<JsonNode> nodes = new ArrayList<>();
        oclZipImporter.importOclPackage(tempZipPath, nodes);
        log.info("Parsed {} nodes", nodes.size());
        for (JsonNode node : nodes) {
            log.info("Node content: {}", node.toPrettyString());
        }

        assertEquals("Should import only one JSON file", 1, nodes.size());
        assertEquals("JSON file should have name 'file1'", "file1", nodes.get(0).get("name").asText());
    }

    @Test(expected = IOException.class)
    public void testImportOclPackage_nonExistentZip() throws IOException {
        List<JsonNode> nodes = new ArrayList<>();
        oclZipImporter.importOclPackage("/non/existent/file.zip", nodes);
    }

    @Test
    public void testImportOclPackage_noJsonFiles() throws IOException {
        byte[] zipData = createZipWithFiles(new String[] { "file1.txt", "This is a text file" });
        String tempZipPath = createTempZipFile(zipData);
        List<JsonNode> nodes = new ArrayList<>();
        oclZipImporter.importOclPackage(tempZipPath, nodes);
        log.info("Parsed {} nodes", nodes.size());
        assertEquals("Should return an empty list when no JSON or CSV files are present", 0, nodes.size());
    }

    @Test
    public void testImportRealOclZip() throws IOException {
        File oclDir = new File(oclDirPath);
        if (oclDir.exists() && oclDir.isDirectory()) {
            File[] zipFiles = oclDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".zip"));
            if (zipFiles != null && zipFiles.length > 0) {
                for (File zipFile : zipFiles) {
                    try {
                        log.info("Attempting to import OCL ZIP from OCL directory: {}", zipFile.getAbsolutePath());
                        List<JsonNode> nodes = new ArrayList<>();
                        oclZipImporter.importOclPackage(zipFile.getAbsolutePath(), nodes);
                        log.info("Parsed {} nodes from {}:", nodes.size(), zipFile.getName());
                        for (JsonNode node : nodes) {
                            log.info("Node content: {}", node.toPrettyString());
                        }
                    } catch (Exception e) {
                        log.error("Error importing {}: {}", zipFile.getName(), e.getMessage(), e);
                    }
                }
            } else {
                log.warn("No ZIP files found in OCL directory: {}", oclDirPath);
            }
        } else {
            log.warn("OCL directory not found: {}", oclDirPath);
        }
    }

    private byte[] createZipWithFiles(String[]... entries) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (String[] entry : entries) {
                String fileName = entry[0];
                String content = entry[1];
                zos.putNextEntry(new ZipEntry(fileName));
                zos.write(content.getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }
        }
        return baos.toByteArray();
    }

    private String createTempZipFile(byte[] zipData) throws IOException {
        java.io.File tempFile = java.io.File.createTempFile("test-ocl-", ".zip");
        tempFile.deleteOnExit();
        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(tempFile)) {
            fos.write(zipData);
        }
        return tempFile.getAbsolutePath();
    }
}