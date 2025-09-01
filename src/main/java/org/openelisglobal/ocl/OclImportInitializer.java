// File: OclImportInitializer.java
package org.openelisglobal.ocl;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.HibernateException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.testconfiguration.action.TestAddControllerUtills;
import org.openelisglobal.testconfiguration.action.TestAddControllerUtills.TestAddParams;
import org.openelisglobal.testconfiguration.controller.TestAddController.TestSet;
import org.openelisglobal.testconfiguration.form.TestAddForm;
import org.openelisglobal.testconfiguration.service.TestAddService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

@Component
public class OclImportInitializer implements ApplicationListener<ContextRefreshedEvent> {
    private static final Logger log = LoggerFactory.getLogger(OclImportInitializer.class);
    private static final String OCL_IMPORT_DIR = "/var/lib/openelis-global/ocl";
    private static final String MARKER_FILE = "/var/lib/openelis-global/ocl/ocl_imported.flag";
    private static final String MARKER_VALUE = "TRUE";
    File file = new File(MARKER_FILE);

    @Value("${org.openelisglobal.ocl.import.autocreate:false}")
    private boolean autocreateOn;

    @Value("${org.openelisglobal.ocl.import.default.testsection:Hematology}")
    private String defaultTestSection;

    @Value("${org.openelisglobal.ocl.import.default.sampletype:Whole Blood}")
    private String defaultSampleType;

    @Autowired
    private OclZipImporter oclZipImporter;

    @Autowired
    private TestAddService testAddService;

    @Autowired
    private TestAddControllerUtills testAddControllerUtills;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (!autocreateOn) {
            log.info("OCL Import: Auto-import is disabled. Skipping OCL import.");
            return;
        }
        if (isOCLImported()) {
            return;
        }
        log.info("OCL Import: Starting OCL import process...");
        performOclImport(OCL_IMPORT_DIR);

    }

    /**
     * Public method to trigger OCL import manually This can be called from REST
     * endpoints
     */
    public void performOclImport(String fileDir) {
        log.info("OCL Import: Manual import triggered");
        Path configDir = Paths.get(fileDir);
        if (!Files.exists(configDir)) {
            return;
        }
        File[] zipFiles = configDir.toFile().listFiles((dir, name) -> name.toLowerCase().endsWith(".zip"));
        if (zipFiles == null || zipFiles.length == 0) {
            return;
        }
        List<JsonNode> oclNodes = new ArrayList<>();
        for (File file : zipFiles) {
            //
            try {
                oclZipImporter.importOclZip(file.getAbsolutePath(), oclNodes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        performImport(oclNodes);
    }

    /**
     * Internal method that contains the actual import logic
     */
    private void performImport(List<JsonNode> oclNodes) {

        // oclZipImporter.importOclZip(file);
        log.info("OCL Import: Found {} nodes to process.", oclNodes.size());

        int conceptCount = 0;
        int testsCreated = 0;
        int testsSkipped = 0;
        OclToOpenElisMapper mapper = new OclToOpenElisMapper(defaultTestSection, defaultSampleType);
        for (JsonNode node : oclNodes) {
            // If the node is a Collection Version, get its concepts array
            if (node.has("concepts") && node.get("concepts").isArray()) {
                log.info("OCL Import: Node has a concepts array of size {}.", node.get("concepts").size());

                // Map all concepts in this node to TestAddForms
                List<TestAddForm> testForms = mapper.mapConceptsToTestAddForms(node);

                for (TestAddForm form : testForms) {
                    conceptCount++;

                    try {
                        log.info("OCL Import: Processing concept #{} - attempting to create test", conceptCount);

                        handlenNewTests(form);
                        updateFlag();

                    } catch (Exception ex) {
                        testsSkipped++;
                        log.error("OCL Import: Failed to create test for concept #{}", conceptCount, ex);
                    }
                }
            }
        }

        log.info("OCL Import: Finished processing. Total concepts processed: {}, Tests created: {}, Tests skipped: {}",
                conceptCount, testsCreated, testsSkipped);
    }

    private boolean isOCLImported() {
        String content = "";
        boolean isImported = false;
        try {
            content = Files.readString(file.toPath()).trim();
            if (MARKER_VALUE.equals(content)) {
                isImported = true;
            }
        } catch (IOException e) {
            /// e.printStackTrace();
            isImported = false;
        }
        return isImported;
    }

    private void updateFlag() {

        try (FileWriter writer = new FileWriter(file, false)) { // false = overwrite
            writer.write(MARKER_VALUE);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            // e.printStackTrace();
        }

    }

    public TestAddForm handlenNewTests(TestAddForm form) {

        String jsonString = (form.getJsonWad());
        JSONParser parser = new JSONParser();
        JSONObject obj = null;
        try {
            obj = (JSONObject) parser.parse(jsonString);
        } catch (ParseException e) {
            LogEvent.logError(e.getMessage(), e);
        }
        TestAddParams testAddParams = testAddControllerUtills.extractTestAddParms(obj, parser);
        List<TestSet> testSets = testAddControllerUtills.createTestSets(testAddParams);
        Localization nameLocalization = testAddControllerUtills.createNameLocalization(testAddParams);
        Localization reportingNameLocalization = testAddControllerUtills.createReportingNameLocalization(testAddParams);
        try {
            testAddService.addTests(testSets, nameLocalization, reportingNameLocalization, "1");
        } catch (HibernateException e) {
            LogEvent.logDebug(e);
        }
        return form;
    }

}