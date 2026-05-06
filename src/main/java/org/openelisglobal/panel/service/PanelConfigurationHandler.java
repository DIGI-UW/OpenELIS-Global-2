package org.openelisglobal.panel.service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.configuration.service.DomainConfigurationHandler;
import org.openelisglobal.localization.service.LocalizationService;
import org.openelisglobal.localization.service.LocalizationValueService;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.panel.valueholder.Panel;
import org.openelisglobal.panelitem.service.PanelItemService;
import org.openelisglobal.panelitem.valueholder.PanelItem;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.typeofsample.service.TypeOfSamplePanelService;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.openelisglobal.typeofsample.valueholder.TypeOfSamplePanel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PanelConfigurationHandler implements DomainConfigurationHandler {

    private static final String LOCALIZATION_COLUMN_PREFIX = "localization:";

    @Autowired
    private PanelService panelService;

    @Autowired
    private PanelItemService panelItemService;

    @Autowired
    private TestService testService;

    @Autowired
    private LocalizationService localizationService;

    @Autowired
    private LocalizationValueService localizationValueService;

    @Autowired
    private TypeOfSampleService typeOfSampleService;

    @Autowired
    private TypeOfSamplePanelService typeOfSamplePanelService;

    @Override
    public String getDomainName() {
        return "panels";
    }

    @Override
    public String getFileExtension() {
        return "csv";
    }

    @Override
    public int getLoadOrder() {
        return 300; // After tests (200)
    }

    @Override
    @Transactional
    public void processConfiguration(InputStream inputStream, String fileName) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

        String headerLine = reader.readLine();
        if (headerLine == null) {
            throw new IllegalArgumentException("Panel configuration file " + fileName + " is empty");
        }

        String[] headers = parseCsvLine(headerLine);

        int panelNameIndex = findColumnIndex(headers, "panelName");
        int sampleTypesIndex = findColumnIndex(headers, "sampleTypes");
        int testsIndex = findColumnIndex(headers, "tests");
        int isActiveIndex = findColumnIndex(headers, "isActive");
        int sortOrderIndex = findColumnIndex(headers, "sortOrder");
        Map<String, Integer> localizationColumns = detectLocalizationColumns(headers);

        if (panelNameIndex < 0) {
            throw new IllegalArgumentException(
                    "Panel configuration file " + fileName + " must have a 'panelName' column");
        }

        int processed = 0;
        String line;
        int lineNumber = 1;

        while ((line = reader.readLine()) != null) {
            lineNumber++;
            if (line.trim().isEmpty() || line.trim().startsWith("#")) {
                continue;
            }
            try {
                String[] values = parseCsvLine(line);
                processRow(values, panelNameIndex, sampleTypesIndex, testsIndex, isActiveIndex, sortOrderIndex,
                        localizationColumns, lineNumber, fileName);
                processed++;
            } catch (Exception e) {
                LogEvent.logError(this.getClass().getSimpleName(), "processConfiguration",
                        "Error processing line " + lineNumber + " in " + fileName + ": " + e.getMessage());
            }
        }

        DisplayListService.getInstance().refreshLists();
        LogEvent.logInfo(this.getClass().getSimpleName(), "processConfiguration",
                "Successfully loaded " + processed + " panels from " + fileName);
    }

    private void processRow(String[] values, int panelNameIndex, int sampleTypesIndex, int testsIndex,
            int isActiveIndex, int sortOrderIndex, Map<String, Integer> localizationColumns, int lineNumber,
            String fileName) {

        String panelName = getValueOrEmpty(values, panelNameIndex);
        if (panelName.isEmpty()) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "processRow",
                    "Skipping row " + lineNumber + " in " + fileName + ": missing panelName");
            return;
        }

        boolean isActive = !"N".equalsIgnoreCase(getValueOrEmpty(values, isActiveIndex));
        String sortOrderStr = getValueOrEmpty(values, sortOrderIndex);

        Panel panel = panelService.getPanelByName(panelName);
        if (panel == null) {
            panel = createPanel(panelName, isActive, sortOrderStr, values, localizationColumns);
        } else {
            updatePanel(panel, isActive, sortOrderStr, values, localizationColumns);
        }

        String testsValue = getValueOrEmpty(values, testsIndex);
        reconcilePanelItems(panel, testsValue, lineNumber, fileName);

        String sampleTypesValue = getValueOrEmpty(values, sampleTypesIndex);
        reconcileSampleTypeLinks(panel, sampleTypesValue);
    }

    private Panel createPanel(String panelName, boolean isActive, String sortOrderStr, String[] values,
            Map<String, Integer> localizationColumns) {

        Map<String, String> translations = buildTranslations(values, panelName, localizationColumns);

        Localization localization = new Localization();
        localization.setDescription("panel name");
        localization.setEnglish(translations.getOrDefault("en", panelName));
        localization.setFrench(translations.getOrDefault("fr", translations.getOrDefault("en", panelName)));
        localization.setSysUserId("1");
        String locId = localizationService.insert(localization);
        localization.setId(locId);

        for (Map.Entry<String, String> entry : translations.entrySet()) {
            localizationValueService.setTranslation(locId, entry.getKey(), entry.getValue(), "1");
        }

        Panel panel = new Panel();
        panel.setPanelName(panelName);
        panel.setDescription(panelName);
        panel.setLocalization(localization);
        panel.setIsActive(isActive ? "Y" : "N");
        panel.setSysUserId("1");

        if (!sortOrderStr.isEmpty()) {
            try {
                panel.setSortOrderInt(Integer.parseInt(sortOrderStr));
            } catch (NumberFormatException ignored) {
            }
        }

        String panelId = panelService.insert(panel);
        panel.setId(panelId);

        LogEvent.logInfo(this.getClass().getSimpleName(), "createPanel", "Created panel: " + panelName);
        return panel;
    }

    private void updatePanel(Panel panel, boolean isActive, String sortOrderStr, String[] values,
            Map<String, Integer> localizationColumns) {

        panel.setIsActive(isActive ? "Y" : "N");
        panel.setSysUserId("1");

        if (!sortOrderStr.isEmpty()) {
            try {
                panel.setSortOrderInt(Integer.parseInt(sortOrderStr));
            } catch (NumberFormatException ignored) {
            }
        }

        Map<String, String> translations = buildTranslations(values, panel.getPanelName(), localizationColumns);
        Localization localization = panel.getLocalization();
        if (localization != null) {
            for (Map.Entry<String, String> entry : translations.entrySet()) {
                localizationValueService.setTranslation(localization.getId(), entry.getKey(), entry.getValue(), "1");
            }
        }

        panelService.update(panel);
        LogEvent.logInfo(this.getClass().getSimpleName(), "updatePanel", "Updated panel: " + panel.getPanelName());
    }

    private void reconcileSampleTypeLinks(Panel panel, String sampleTypesValue) {
        if (sampleTypesValue.isEmpty()) {
            return;
        }

        List<TypeOfSamplePanel> existing = typeOfSamplePanelService.getTypeOfSamplePanelsForPanel(panel.getId());
        List<String> existingSampleTypeIds = new ArrayList<>();
        for (TypeOfSamplePanel link : existing) {
            existingSampleTypeIds.add(link.getTypeOfSampleId());
        }

        for (String sampleTypeName : sampleTypesValue.split("\\|")) {
            sampleTypeName = sampleTypeName.trim();
            if (sampleTypeName.isEmpty()) {
                continue;
            }
            TypeOfSample typeOfSample = findSampleTypeByName(sampleTypeName);
            if (typeOfSample == null) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "reconcileSampleTypeLinks", "Sample type '"
                        + sampleTypeName + "' not found for panel '" + panel.getPanelName() + "'. Skipping.");
                continue;
            }
            if (!existingSampleTypeIds.contains(typeOfSample.getId())) {
                TypeOfSamplePanel link = new TypeOfSamplePanel();
                link.setPanelId(panel.getId());
                link.setTypeOfSampleId(typeOfSample.getId());
                link.setSysUserId("1");
                typeOfSamplePanelService.insert(link);
            }
        }
    }

    private TypeOfSample findSampleTypeByName(String name) {
        List<TypeOfSample> allTypes = typeOfSampleService.getAllTypeOfSamples();
        for (TypeOfSample t : allTypes) {
            if (name.equalsIgnoreCase(t.getDescription()) || name.equalsIgnoreCase(t.getLocalAbbreviation())) {
                return t;
            }
        }
        return null;
    }

    private void reconcilePanelItems(Panel panel, String testsValue, int lineNumber, String fileName) {
        List<PanelItem> existing = panelItemService.getPanelItemsForPanel(panel.getId());
        List<String> desiredTestNames = new ArrayList<>();

        if (!testsValue.isEmpty()) {
            for (String testName : testsValue.split("\\|")) {
                testName = testName.trim();
                if (!testName.isEmpty()) {
                    desiredTestNames.add(testName);
                }
            }
        }

        Map<String, PanelItem> existingByTestDesc = new HashMap<>();
        for (PanelItem item : existing) {
            Test t = item.getTest();
            if (t != null && t.getDescription() != null) {
                existingByTestDesc.put(t.getDescription(), item);
            }
        }

        for (String testName : desiredTestNames) {
            if (existingByTestDesc.containsKey(testName)) {
                continue;
            }
            Test test = findTest(testName);
            if (test == null) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "reconcilePanelItems",
                        "Test '" + testName + "' not found (line " + lineNumber + " of " + fileName + "). Skipping.");
                continue;
            }
            PanelItem item = new PanelItem();
            item.setPanel(panel);
            item.setPanelName(panel.getPanelName());
            item.setTest(test);
            String desc = test.getDescription();
            item.setTestName(desc != null && desc.length() > 20 ? desc.substring(0, 20) : desc);
            item.setSortOrder(String.valueOf(desiredTestNames.indexOf(testName) + 1));
            item.setSysUserId("1");
            panelItemService.insert(item);
        }
    }

    private Test findTest(String testName) {
        List<Test> allTests = testService.getAllTests(false);
        for (Test test : allTests) {
            if (testName.equalsIgnoreCase(test.getDescription())) {
                return test;
            }
            if (testName.equalsIgnoreCase(test.getLocalizedName())) {
                return test;
            }
        }
        return null;
    }

    private Map<String, String> buildTranslations(String[] values, String defaultName,
            Map<String, Integer> localizationColumns) {
        Map<String, String> translations = new HashMap<>();
        for (Map.Entry<String, Integer> entry : localizationColumns.entrySet()) {
            String val = getValueOrEmpty(values, entry.getValue());
            if (!val.isEmpty()) {
                translations.put(entry.getKey(), val);
            }
        }
        if (translations.isEmpty()) {
            translations.put("en", defaultName);
        }
        return translations;
    }

    private String[] parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                values.add(current.toString().trim());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        values.add(current.toString().trim());
        return values.toArray(new String[0]);
    }

    private int findColumnIndex(String[] headers, String name) {
        for (int i = 0; i < headers.length; i++) {
            if (name.equalsIgnoreCase(headers[i])) {
                return i;
            }
        }
        return -1;
    }

    private Map<String, Integer> detectLocalizationColumns(String[] headers) {
        Map<String, Integer> cols = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            String h = headers[i].trim().toLowerCase();
            if (h.startsWith(LOCALIZATION_COLUMN_PREFIX)) {
                String locale = h.substring(LOCALIZATION_COLUMN_PREFIX.length());
                if (!locale.isEmpty()) {
                    cols.put(locale, i);
                }
            }
        }
        return cols;
    }

    private String getValueOrEmpty(String[] values, int index) {
        if (index >= 0 && index < values.length) {
            String v = values[index];
            return v != null ? v : "";
        }
        return "";
    }
}
