package org.openelisglobal.panel.service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.openelisglobal.common.domain.Domain;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.common.util.CsvParsingUtil;
import org.openelisglobal.configuration.service.CsvLoadSummary;
import org.openelisglobal.configuration.service.DomainConfigurationHandler;
import org.openelisglobal.configuration.service.ImportRunContext;
import org.openelisglobal.configuration.service.LoadedRow;
import org.openelisglobal.configuration.service.RowTransactionRunner;
import org.openelisglobal.configuration.service.UnresolvedReferenceService;
import org.openelisglobal.configuration.valueholder.UnresolvedReference;
import org.openelisglobal.localization.service.LocalizationService;
import org.openelisglobal.localization.service.LocalizationValueService;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.panel.valueholder.Panel;
import org.openelisglobal.panelitem.service.PanelItemService;
import org.openelisglobal.panelitem.valueholder.PanelItem;
import org.openelisglobal.panelterminology.service.PanelTerminologyMappingService;
import org.openelisglobal.test.service.LegacyTestVariantFinder;
import org.openelisglobal.test.service.LegacyTestVariantFinder.LegacyTestVariant;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.typeofsample.service.TypeOfSamplePanelService;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.openelisglobal.typeofsample.valueholder.TypeOfSamplePanel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Loads the {@code panels} catalog domain from CSV. Members in the
 * {@code tests} column are plain test names: a name resolves to the test with
 * that exact or normalized description (or localized name), or to the legacy
 * {@code Name(SampleType)} variants an earlier loader wrote for the panel's
 * sample types. Every row runs in its own transaction and each file ends with a
 * {@code SUMMARY file=... domain=panels created= updated= skipped=} line.
 * <p>
 * A panel's {@code domain} is set by the file, never derived from the specimens
 * its members use: which testing domain a panel belongs to is an editorial
 * choice, and a panel may legitimately reach a specimen from another domain.
 * Omitting the column leaves the panel's stored domain alone, which for a new
 * panel is the {@code CLINICAL} default.
 * <p>
 * A panel never mixes domains (OGC-224), and the import holds that rule exactly
 * as the Panel Editor does (OGC-1232): a row whose domain, stored or from the
 * file, is not the domain of every member it would leave in the panel is
 * skipped with a reason naming those tests, and nothing of the row is written.
 * Load the tests file with a {@code domain} column first, or fix the panel's
 * domain.
 */
@Component
public class PanelConfigurationHandler implements DomainConfigurationHandler {

    private static final String LOCALIZATION_COLUMN_PREFIX = "localization:";

    private static final Set<String> DOMAINS = Set.of("CLINICAL", "ENVIRONMENTAL", "VECTOR");

    @Autowired
    private PanelService panelService;

    @Autowired
    private PanelItemService panelItemService;

    @Autowired
    private TestService testService;

    @Autowired
    private LegacyTestVariantFinder legacyVariantFinder;

    @Autowired(required = false)
    private UnresolvedReferenceService unresolvedReferenceService;

    @Autowired
    private LocalizationService localizationService;

    @Autowired
    private LocalizationValueService localizationValueService;

    @Autowired
    private TypeOfSampleService typeOfSampleService;

    @Autowired
    private TypeOfSamplePanelService typeOfSamplePanelService;

    @Autowired
    private PanelTerminologyMappingService panelTerminologyMappingService;

    @Autowired(required = false)
    private PlatformTransactionManager transactionManager;

    private volatile CsvLoadSummary lastSummary;

    @Override
    public String getDomainName() {
        return "panels";
    }

    @Override
    public CsvLoadSummary getLastSummary() {
        return lastSummary;
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
    public boolean supportsDryRun() {
        return true;
    }

    @Override
    public void processConfiguration(InputStream inputStream, String fileName) throws Exception {
        processConfiguration(inputStream, fileName, false);
    }

    @Override
    public void processConfiguration(InputStream inputStream, String fileName, boolean dryRun) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

        String headerLine = reader.readLine();
        if (headerLine == null) {
            throw new IllegalArgumentException("Panel configuration file " + fileName + " is empty");
        }

        String[] headers = CsvParsingUtil.parseCsvLine(headerLine);

        int panelNameIndex = findColumnIndex(headers, "panelName");
        int sampleTypesIndex = findColumnIndex(headers, "sampleTypes");
        int testsIndex = findColumnIndex(headers, "tests");
        int isActiveIndex = findColumnIndex(headers, "isActive");
        int sortOrderIndex = findColumnIndex(headers, "sortOrder");
        int loincIndex = findColumnIndex(headers, "loinc");
        int domainIndex = findColumnIndex(headers, "domain");
        Map<String, Integer> localizationColumns = detectLocalizationColumns(headers);

        if (panelNameIndex < 0) {
            throw new IllegalArgumentException(
                    "Panel configuration file " + fileName + " must have a 'panelName' column");
        }

        CsvLoadSummary summary = new CsvLoadSummary(getDomainName(), fileName);
        RowTransactionRunner rowTransaction = new RowTransactionRunner(transactionManager, dryRun);
        String line;
        int lineNumber = 1;

        while ((line = reader.readLine()) != null) {
            lineNumber++;
            if (line.trim().isEmpty() || line.trim().startsWith("#")) {
                continue;
            }
            String[] values = CsvParsingUtil.parseCsvLine(line);
            int rowLine = lineNumber;
            LoadedRow<Panel> result;
            try {
                result = rowTransaction
                        .run(() -> processRow(values, panelNameIndex, sampleTypesIndex, testsIndex, isActiveIndex,
                                sortOrderIndex, loincIndex, domainIndex, localizationColumns, rowLine, fileName));
            } catch (Exception e) {
                result = LoadedRow.skipped(CsvLoadSummary.reason(e));
            }
            summary.record(result, getClass().getSimpleName(), rowLine);
            recordUnresolved(fileName, rowLine);
        }

        DisplayListService.getInstance().refreshLists();
        summary.log(getClass().getSimpleName());
        lastSummary = summary;
    }

    /**
     * Writes the names this row could not resolve into the decision queue, once the
     * row's own transaction has ended. Without a Spring context, as in plain unit
     * tests, there is no queue to write to.
     */
    private void recordUnresolved(String fileName, int lineNumber) {
        if (unresolvedReferenceService != null) {
            unresolvedReferenceService.recordPending(getDomainName(), fileName, lineNumber);
        }
    }

    private LoadedRow<Panel> processRow(String[] values, int panelNameIndex, int sampleTypesIndex, int testsIndex,
            int isActiveIndex, int sortOrderIndex, int loincIndex, int domainIndex,
            Map<String, Integer> localizationColumns, int lineNumber, String fileName) {

        String panelName = getValueOrEmpty(values, panelNameIndex);
        if (panelName.isEmpty()) {
            return LoadedRow.skipped("missing panelName");
        }

        boolean isActive = !"N".equalsIgnoreCase(getValueOrEmpty(values, isActiveIndex));
        String sortOrderStr = getValueOrEmpty(values, sortOrderIndex);
        boolean hasLoincColumn = loincIndex >= 0;
        String loinc = getValueOrEmpty(values, loincIndex);
        String domain = getValueOrEmpty(values, domainIndex);
        if (!domain.isEmpty() && !DOMAINS.contains(domain.toUpperCase())) {
            return LoadedRow.skipped("domain '" + domain + "' must be one of " + DOMAINS);
        }

        Panel panel = panelService.getPanelByName(panelName);
        boolean created = panel == null;

        String sampleTypesValue = getValueOrEmpty(values, sampleTypesIndex);
        String testsValue = getValueOrEmpty(values, testsIndex);
        Map<String, List<Test>> members = resolveMemberTests(testsValue, resolveSampleTypeIds(sampleTypesValue),
                panelName, lineNumber, fileName);

        String effectiveDomain = !domain.isEmpty() ? domain.toUpperCase()
                : created ? Domain.DEFAULT.name() : Domain.normalize(panel.getDomain());
        List<Test> outsideDomain = membersOutsideDomain(effectiveDomain,
                created ? List.of() : panelItemService.getPanelItemsForPanel(panel.getId()), members);
        if (!outsideDomain.isEmpty()) {
            return LoadedRow.skipped(domainConflictReason(effectiveDomain, outsideDomain));
        }

        if (created) {
            panel = createPanel(panelName, isActive, sortOrderStr, hasLoincColumn ? loinc : null, values,
                    localizationColumns);
        } else {
            updatePanel(panel, isActive, sortOrderStr, hasLoincColumn ? loinc : null, values, localizationColumns);
        }
        if (!domain.isEmpty()) {
            panel.setDomain(domain.toUpperCase());
            panel.setSysUserId("1");
            panelService.update(panel);
        }

        if (hasLoincColumn) {
            syncLoincMapping(panel, loinc);
        }

        reconcilePanelItems(panel, members);
        reconcileSampleTypeLinks(panel, sampleTypesValue);

        return created ? LoadedRow.created(panel) : LoadedRow.updated(panel);
    }

    /**
     * The member tests, existing and desired, whose own domain is not the domain
     * the panel has or is being given. Each test is named once.
     */
    private List<Test> membersOutsideDomain(String panelDomain, List<PanelItem> existing,
            Map<String, List<Test>> desired) {
        Map<String, Test> outside = new LinkedHashMap<>();
        for (PanelItem item : existing) {
            Test test = item.getTest();
            if (test != null && !panelDomain.equals(Domain.normalize(test.getDomain()))) {
                outside.put(test.getId(), test);
            }
        }
        for (List<Test> tests : desired.values()) {
            for (Test test : tests) {
                if (!panelDomain.equals(Domain.normalize(test.getDomain()))) {
                    outside.put(test.getId(), test);
                }
            }
        }
        return new ArrayList<>(outside.values());
    }

    private String domainConflictReason(String panelDomain, List<Test> outsideDomain) {
        StringBuilder reason = new StringBuilder("domain ").append(panelDomain)
                .append(" is not the domain of every member test:");
        for (int i = 0; i < outsideDomain.size(); i++) {
            Test test = outsideDomain.get(i);
            reason.append(i == 0 ? " " : ", ").append(test.getDescription()).append(" (")
                    .append(Domain.normalize(test.getDomain())).append(")");
        }
        return reason.toString();
    }

    // Bridge the legacy panel.loinc value into the panel terminology mappings as a
    // LOINC / SAME_AS entry, exactly as the test loader does via
    // TestTerminologyMappingService. A mapping failure must never fail the load.
    private void syncLoincMapping(Panel panel, String loinc) {
        try {
            panelTerminologyMappingService.syncLegacyLoinc(panel.getId(), loinc, "1");
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "syncLoincMapping",
                    "Failed to sync LOINC mapping for panel '" + panel.getPanelName() + "': " + e.getMessage());
        }
    }

    private Panel createPanel(String panelName, boolean isActive, String sortOrderStr, String loinc, String[] values,
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
        if (loinc != null) {
            panel.setLoinc(loinc);
        }

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

    private void updatePanel(Panel panel, boolean isActive, String sortOrderStr, String loinc, String[] values,
            Map<String, Integer> localizationColumns) {

        panel.setIsActive(isActive ? "Y" : "N");
        panel.setSysUserId("1");
        if (loinc != null) {
            panel.setLoinc(loinc);
        }

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

    private Set<String> resolveSampleTypeIds(String sampleTypesValue) {
        Set<String> ids = new LinkedHashSet<>();
        if (sampleTypesValue.isEmpty()) {
            return ids;
        }
        for (String sampleTypeName : sampleTypesValue.split("\\|")) {
            TypeOfSample typeOfSample = findSampleTypeByName(sampleTypeName.trim());
            if (typeOfSample != null) {
                ids.add(typeOfSample.getId());
            }
        }
        return ids;
    }

    /**
     * The tests the row's {@code tests} column names, in file order, keyed by the
     * name as written. A name that resolves to nothing maps to an empty list and is
     * queued as an unresolved reference for the decision queue.
     */
    private Map<String, List<Test>> resolveMemberTests(String testsValue, Set<String> panelSampleTypeIds,
            String panelName, int lineNumber, String fileName) {
        Map<String, List<Test>> members = new LinkedHashMap<>();
        if (testsValue.isEmpty()) {
            return members;
        }
        for (String testName : testsValue.split("\\|")) {
            testName = testName.trim();
            if (testName.isEmpty() || members.containsKey(testName)) {
                continue;
            }
            List<Test> tests = findTests(testName, panelSampleTypeIds);
            if (tests.isEmpty()) {
                ImportRunContext.addPending(UnresolvedReference.TYPE_TEST, testName,
                        fileName + " line " + lineNumber + " (panel " + panelName + ")");
                LogEvent.logWarn(this.getClass().getSimpleName(), "resolveMemberTests",
                        "Test '" + testName + "' not found (line " + lineNumber + " of " + fileName + "). Skipping.");
            }
            members.put(testName, tests);
        }
        return members;
    }

    private void reconcilePanelItems(Panel panel, Map<String, List<Test>> members) {
        List<PanelItem> existing = panelItemService.getPanelItemsForPanel(panel.getId());

        Map<String, PanelItem> existingByTestId = new HashMap<>();
        for (PanelItem item : existing) {
            Test t = item.getTest();
            if (t != null && t.getId() != null) {
                existingByTestId.put(t.getId(), item);
            }
        }

        int position = 0;
        for (List<Test> tests : members.values()) {
            position++;
            for (Test test : tests) {
                if (existingByTestId.containsKey(test.getId())) {
                    continue;
                }
                PanelItem item = new PanelItem();
                item.setPanel(panel);
                item.setPanelName(panel.getPanelName());
                item.setTest(test);
                String desc = test.getDescription();
                item.setTestName(desc != null && desc.length() > 20 ? desc.substring(0, 20) : desc);
                item.setSortOrder(String.valueOf(position));
                item.setSysUserId("1");
                panelItemService.insert(item);
                existingByTestId.put(test.getId(), item);
            }
        }
    }

    /**
     * The tests a plain member name stands for: the one test with that exact or
     * normalized description (or localized name), else the legacy
     * {@code name(SampleType)} variants for the panel's sample types (every variant
     * when the panel lists none).
     */
    private List<Test> findTests(String testName, Set<String> panelSampleTypeIds) {
        List<Test> matches = new ArrayList<>();
        Test exact = testService.getTestByDescription(testName);
        if (exact == null) {
            exact = testService.getTestByNormalizedDescription(testName);
        }
        if (exact != null) {
            matches.add(exact);
            return matches;
        }
        for (LegacyTestVariant variant : legacyVariantFinder.find(testName)) {
            if (panelSampleTypeIds.isEmpty() || panelSampleTypeIds.contains(variant.specimen().getId())) {
                matches.add(variant.test());
            }
        }
        if (!matches.isEmpty()) {
            return matches;
        }
        // Last resort, kept for catalogs that name members by their display name:
        // legacy variants all share one display name, so this must not run first.
        for (Test test : testService.getAllTests(false)) {
            if (testName.equalsIgnoreCase(test.getLocalizedName())) {
                matches.add(test);
                return matches;
            }
        }
        return matches;
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
