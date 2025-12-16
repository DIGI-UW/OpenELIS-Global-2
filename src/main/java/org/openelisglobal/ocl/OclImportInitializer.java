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
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.hibernate.HibernateException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.panel.service.PanelService;
import org.openelisglobal.panel.valueholder.Panel;
import org.openelisglobal.panelitem.service.PanelItemService;
import org.openelisglobal.panelitem.valueholder.PanelItem;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testconfiguration.action.TestAddControllerUtills;
import org.openelisglobal.testconfiguration.action.TestAddControllerUtills.TestAddParams;
import org.openelisglobal.testconfiguration.controller.TestAddController.TestSet;
import org.openelisglobal.testconfiguration.form.TestAddForm;
import org.openelisglobal.testconfiguration.service.TestAddService;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
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
    private static final String MARKER_FILE = "ocl_imported.flag";
    private static final String MARKER_FILE_PATH = OCL_IMPORT_DIR + "/" + MARKER_FILE;
    private static final String MARKER_VALUE = "TRUE";

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

    @Autowired
    private PanelService panelService;

    @Autowired
    private PanelItemService panelItemService;

    @Autowired
    private TestService testService;

    @Autowired
    private DisplayListService displayListService;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (!autocreateOn) {
            log.info("OCL Import: Auto-import is disabled. Skipping OCL import.");
            return;
        }
        if (isOCLImported(MARKER_FILE_PATH)) {
            return;
        }
        log.info("OCL Import: Starting OCL import process...");
        performOclImport(OCL_IMPORT_DIR, MARKER_FILE_PATH);

    }

    /**
     * Public method to trigger OCL import manually This can be called from REST
     * endpoints
     */
    public void performOclImport(String fileDir, String markerFilePath) {
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
        performImport(oclNodes, markerFilePath);
    }

    /**
     * Internal method that contains the actual import logic
     */
    private void performImport(List<JsonNode> oclNodes, String markerFilePath) {

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
        updateFlag(markerFilePath);
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
        log.info("OCL Import: Finished processing. Total concepts processed: {}, Tests created: {}, Tests skipped: {}",
                conceptCount, testsCreated, testsSkipped);
    }

    private boolean isOCLImported(String markerFile) {
        String content = "";
        boolean isImported = false;
        File file = new File(markerFile);
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

    private void updateFlag(String markerFilePath) {
        File file = new File(markerFilePath);
        try (FileWriter writer = new FileWriter(file, false)) { // false = overwrite
            writer.write(MARKER_VALUE);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            // e.printStackTrace();
        }

    }

    private void mapLabsetPannels(OclToOpenElisMapper mapper) {
        for (JsonNode panel : mapper.getLabSetPanelNodes()) {
            Map<String, String> names = mapper.extractNames(panel);
            String englishName = names.get("englishName");
            Panel dbPanel = panelService.getPanelByName(englishName);
            log.info("Mapping tests for Panel " + englishName);

            if (dbPanel != null) {
                List<PanelItem> panelItems = panelItemService.getPanelItemsForPanel(dbPanel.getId());

                List<Test> newTests = new ArrayList<>();
                Set<String> memebers = mapper.getLabSetMemebrs(panel);
                log.info("Mapped Lab Set Memebers: " + memebers);
                for (String testName : mapper.getLabSetMemebrs(panel)) {
                    log.info("Adding Test " + testName + " to Pannel " + englishName);
                    Test test = testService.getTestByLocalizedName(testName, Locale.ENGLISH);
                    if (test != null) {
                        log.info("Test " + testName + "Added to Pannel " + englishName);
                        newTests.add(test);
                    }
                }
                try {
                    panelItemService.updatePanelItems(panelItems, dbPanel, false, "1", newTests);
                } catch (LIMSRuntimeException e) {
                    LogEvent.logDebug(e);
                }
            }

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