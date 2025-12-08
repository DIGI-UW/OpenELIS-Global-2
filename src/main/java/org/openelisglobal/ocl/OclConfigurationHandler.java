package org.openelisglobal.ocl;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.configuration.service.DomainConfigurationHandler;
import org.openelisglobal.panel.service.PanelService;
import org.openelisglobal.panelitem.service.PanelItemService;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.testconfiguration.action.TestAddControllerUtills;
import org.openelisglobal.testconfiguration.service.TestAddService;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Handler for loading OCL (Open Concept Lab) configuration files. Supports ZIP
 * format containing OCL concept collections.
 *
 * OCL ZIP files are expected to contain JSON files with OCL concept
 * definitions. The handler processes these concepts and creates corresponding
 * tests, panels, and dictionaries in OpenELIS.
 */
@Component
public class OclConfigurationHandler implements DomainConfigurationHandler {

    private static final Logger log = LoggerFactory.getLogger(OclConfigurationHandler.class);

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

    @Autowired
    private PanelService panelService;

    @Autowired
    private PanelItemService panelItemService;

    @Autowired
    private TestService testService;

    @Autowired
    private DisplayListService displayListService;

    @Override
    public String getDomainName() {
        return "ocl";
    }

    @Override
    public String getFileExtension() {
        return "zip";
    }

    @Override
    public int getLoadOrder() {
        return 400; // Load after dictionaries (300) but before higher-level configs
    }

    @Override
    public void processConfiguration(InputStream inputStream, String fileName) throws Exception {
        // OCL files are ZIP files, so we need to handle them specially.
        // The ConfigurationInitializationService passes an InputStream, but for ZIP
        // files
        // we need the actual file path to use OclZipImporter. We'll create a temp file
        // from the InputStream and process it.

        File tempFile = null;
        try {
            // Create a temporary file to hold the ZIP contents
            tempFile = File.createTempFile("ocl-", ".zip");
            tempFile.deleteOnExit();

            // Copy InputStream to temp file
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(tempFile)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
            }

            // Process the ZIP file
            List<JsonNode> oclNodes = new ArrayList<>();
            oclZipImporter.importOclZip(tempFile.getAbsolutePath(), oclNodes);
            performImport(oclNodes);
        } finally {
            // Clean up temp file
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    /**
     * Internal method that contains the actual import logic. Made public for use by
     * OclImportInitializer for manual imports.
     */
    public void performImport(List<JsonNode> oclNodes) {
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
                List<org.openelisglobal.testconfiguration.form.TestAddForm> testForms = mapper
                        .mapConceptsToTestAddForms(node);

                for (org.openelisglobal.testconfiguration.form.TestAddForm form : testForms) {
                    conceptCount++;

                    try {
                        log.info("OCL Import: Processing concept #{} - attempting to create test", conceptCount);
                        handlenNewTests(form);
                        testsCreated++;
                    } catch (Exception ex) {
                        testsSkipped++;
                        log.error("OCL Import: Failed to create test for concept #{}", conceptCount, ex);
                    }
                }
                try {
                    mapLabsetPannels(mapper);
                } catch (Exception ex) {
                    log.error("Error while Handling Lab sets", ex);
                }
            }
        }
        refreshDisplayLists();
        log.info("OCL Import: Finished processing. Total concepts processed: {}, Tests created: {}, Tests skipped: {}",
                conceptCount, testsCreated, testsSkipped);
    }

    private void refreshDisplayLists() {
        displayListService.refreshList(DisplayListService.ListType.PANELS);
        displayListService.refreshList(DisplayListService.ListType.PANELS_INACTIVE);
        testService.refreshTestNames();
        displayListService.refreshList(DisplayListService.ListType.SAMPLE_TYPE_ACTIVE);
        displayListService.refreshList(DisplayListService.ListType.SAMPLE_TYPE_INACTIVE);
        displayListService.refreshList(DisplayListService.ListType.PANELS_ACTIVE);
        displayListService.refreshList(DisplayListService.ListType.PANELS_INACTIVE);
        displayListService.refreshList(DisplayListService.ListType.PANELS);
        displayListService.refreshList(DisplayListService.ListType.TEST_SECTION_ACTIVE);
        displayListService.refreshList(DisplayListService.ListType.TEST_SECTION_BY_NAME);
        displayListService.refreshList(DisplayListService.ListType.TEST_SECTION_INACTIVE);
        SpringContext.getBean(TypeOfSampleService.class).clearCache();
    }

    private void mapLabsetPannels(OclToOpenElisMapper mapper) {
        for (JsonNode panel : mapper.getLabSetPanelNodes()) {
            java.util.Map<String, String> names = mapper.extractNames(panel);
            String englishName = names.get("englishName");
            org.openelisglobal.panel.valueholder.Panel dbPanel = panelService.getPanelByName(englishName);
            log.info("Mapping tests for Panel " + englishName);

            if (dbPanel != null) {
                List<org.openelisglobal.panelitem.valueholder.PanelItem> panelItems = panelItemService
                        .getPanelItemsForPanel(dbPanel.getId());

                List<org.openelisglobal.test.valueholder.Test> newTests = new ArrayList<>();
                java.util.Set<String> memebers = mapper.getLabSetMemebrs(panel);
                log.info("Mapped Lab Set Memebrs: " + memebers);
                for (String testName : mapper.getLabSetMemebrs(panel)) {
                    log.info("Adding Test " + testName + " to Pannel " + englishName);
                    org.openelisglobal.test.valueholder.Test test = testService.getTestByLocalizedName(testName,
                            java.util.Locale.ENGLISH);
                    if (test != null) {
                        log.info("Test " + testName + "Added to Pannel " + englishName);
                        newTests.add(test);
                    }
                }
                try {
                    panelItemService.updatePanelItems(panelItems, dbPanel, false, "1", newTests);
                } catch (org.openelisglobal.common.exception.LIMSRuntimeException e) {
                    LogEvent.logDebug(e);
                }
            }

        }
    }

    public org.openelisglobal.testconfiguration.form.TestAddForm handlenNewTests(
            org.openelisglobal.testconfiguration.form.TestAddForm form) {

        String jsonString = (form.getJsonWad());
        org.json.simple.parser.JSONParser parser = new org.json.simple.parser.JSONParser();
        org.json.simple.JSONObject obj = null;
        try {
            obj = (org.json.simple.JSONObject) parser.parse(jsonString);
        } catch (org.json.simple.parser.ParseException e) {
            LogEvent.logError(e.getMessage(), e);
        }
        TestAddControllerUtills.TestAddParams testAddParams = testAddControllerUtills.extractTestAddParms(obj, parser);
        List<org.openelisglobal.testconfiguration.controller.TestAddController.TestSet> testSets = testAddControllerUtills
                .createTestSets(testAddParams);
        org.openelisglobal.localization.valueholder.Localization nameLocalization = testAddControllerUtills
                .createNameLocalization(testAddParams);
        org.openelisglobal.localization.valueholder.Localization reportingNameLocalization = testAddControllerUtills
                .createReportingNameLocalization(testAddParams);
        try {
            testAddService.addTests(testSets, nameLocalization, reportingNameLocalization, "1");
        } catch (org.hibernate.HibernateException e) {
            LogEvent.logDebug(e);
        }
        return form;
    }
}
