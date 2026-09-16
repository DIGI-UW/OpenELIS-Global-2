package org.openelisglobal.test.service;

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
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.configuration.service.CsvLoadSummary;
import org.openelisglobal.configuration.service.DomainConfigurationHandler;
import org.openelisglobal.configuration.service.LoadedRow;
import org.openelisglobal.configuration.service.RowTransactionRunner;
import org.openelisglobal.localization.service.LocalizationValueService;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.test.service.LegacyTestVariantFinder.LegacyTestVariant;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.test.valueholder.TestSection;
import org.openelisglobal.testcatalog.service.TestCatalogCreationService;
import org.openelisglobal.testcatalog.service.TestCatalogCreationService.CreateTestParams;
import org.openelisglobal.testresultcomponent.service.TestResultComponentService;
import org.openelisglobal.testterminology.service.TestTerminologyMappingService;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.service.TypeOfSampleTestService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.openelisglobal.typeofsample.valueholder.TypeOfSampleTest;
import org.openelisglobal.unitofmeasure.service.UnitOfMeasureService;
import org.openelisglobal.unitofmeasure.valueholder.UnitOfMeasure;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Loads the {@code tests} catalog domain from CSV.
 * <p>
 * Expected CSV format (header on line 1, columns in any order, unknown columns
 * ignored):
 * testName,testSection,sampleType,loinc,isActive,isOrderable,sortOrder,unitOfMeasure,isReportable,notifyResults,localCode,localization:en,localization:fr
 * Glucose,Biochemistry,Serum|Plasma|Whole
 * Blood,2345-7,Y,Y,1,mg/dL,Y,N,GLU,Glucose,Glucose
 * <p>
 * One row is one test. A row that lists several sample types (separated by
 * {@code |}) creates one test, described by its plain {@code testName}, linked
 * to every listed sample type, which is the specimen model the Test Catalog
 * editor uses. New tests are written through {@link TestCatalogCreationService}
 * so they take the same shape as tests created in the editor.
 * <p>
 * Identity, in order: {@code localCode} when the column is filled, then the
 * plain {@code testName} by exact and then normalized description. Translations
 * never identify a test. Records created by earlier loaders in the
 * {@code Name(SampleType)} form are recognised through
 * {@link LegacyTestVariantFinder} (the name compared in its normalized form,
 * the parenthesised part a known sample type). Those whose specimen the row
 * lists are updated in place and take the row's spelling of the name, and no
 * plain-named duplicate is created for that row; a variant for a specimen the
 * row does not list is a different test and is left alone.
 * <p>
 * Every row runs in its own transaction, so one row the database rejects is
 * skipped with its line number and reason while the rest of the file loads.
 * Each file ends with a
 * {@code SUMMARY file=... domain=tests created= updated= skipped=} line.
 * <p>
 * Optional columns: {@code loinc}; {@code isActive} (default Y);
 * {@code isOrderable} (default Y); {@code sortOrder} (auto-assigned);
 * {@code unitOfMeasure} (must already exist); {@code isReportable} (default Y);
 * {@code notifyResults}; {@code localCode} (at most 10 characters);
 * {@code localization:xx} display names, falling back to {@code testName} for
 * the fallback locale.
 */
@Component
public class TestConfigurationHandler implements DomainConfigurationHandler {

    private static final String LOCALIZATION_COLUMN_PREFIX = "localization:";
    private static final int LOCAL_CODE_MAX_LENGTH = 10;
    private static final int DESCRIPTION_MAX_LENGTH = 60;
    private static final String DEFAULT_DOMAIN = "CLINICAL";
    private static final String SYS_USER_ID = "1";

    @Autowired
    private TestService testService;

    @Autowired
    private TestSectionService testSectionService;

    @Autowired
    private LocalizationValueService localizationValueService;

    @Autowired
    private TypeOfSampleService typeOfSampleService;

    @Autowired
    private TypeOfSampleTestService typeOfSampleTestService;

    @Autowired
    private UnitOfMeasureService unitOfMeasureService;

    @Autowired
    private TestCatalogCreationService testCatalogCreationService;

    // Bridge loaded tests into the new editor model (PRIMARY component under
    // Sample & Results, LOINC under Terminology) — config-loaded tests otherwise
    // exist only in the legacy shape.
    @Autowired
    private TestResultComponentService testResultComponentService;

    @Autowired
    private TestTerminologyMappingService terminologyMappingService;

    @Autowired
    private LegacyTestVariantFinder legacyVariantFinder;

    @Autowired(required = false)
    private PlatformTransactionManager transactionManager;

    private volatile CsvLoadSummary lastSummary;

    private record Columns(int testName, int testSection, int sampleType, int loinc, int isActive, int isOrderable,
            int sortOrder, int unitOfMeasure, int isReportable, int notifyResults, int localCode,
            Map<String, Integer> localization) {
    }

    @Override
    public String getDomainName() {
        return "tests";
    }

    @Override
    public String getFileExtension() {
        return "csv";
    }

    @Override
    public int getLoadOrder() {
        return 200; // Depends on test sections and sample types
    }

    @Override
    public CsvLoadSummary getLastSummary() {
        return lastSummary;
    }

    @Override
    public void processConfiguration(InputStream inputStream, String fileName) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

        String headerLine = reader.readLine();
        if (headerLine == null) {
            throw new IllegalArgumentException("Test configuration file " + fileName + " is empty");
        }

        String[] headers = parseCsvLine(headerLine);
        validateHeaders(headers, fileName);
        Columns columns = new Columns(findColumnIndex(headers, "testName"), findColumnIndex(headers, "testSection"),
                findColumnIndex(headers, "sampleType"), findColumnIndex(headers, "loinc"),
                findColumnIndex(headers, "isActive"), findColumnIndex(headers, "isOrderable"),
                findColumnIndex(headers, "sortOrder"), findColumnIndex(headers, "unitOfMeasure"),
                findColumnIndex(headers, "isReportable"), findColumnIndex(headers, "notifyResults"),
                findColumnIndex(headers, "localCode"), detectLocalizationColumns(headers));

        CsvLoadSummary summary = new CsvLoadSummary(getDomainName(), fileName);
        RowTransactionRunner rowTransaction = new RowTransactionRunner(transactionManager);
        Set<String> touchedTestIds = new LinkedHashSet<>();
        String line;
        int lineNumber = 1;
        int nextSortOrder = 1;

        while ((line = reader.readLine()) != null) {
            lineNumber++;
            if (line.trim().isEmpty() || line.trim().startsWith("#")) {
                continue;
            }

            String[] values = parseCsvLine(line);
            int rowLine = lineNumber;
            int defaultSortOrder = nextSortOrder;
            LoadedRow<List<String>> result;
            try {
                result = rowTransaction.run(() -> processRow(values, columns, rowLine, fileName, defaultSortOrder));
            } catch (Exception e) {
                result = LoadedRow.skipped(CsvLoadSummary.reason(e));
            }
            summary.record(result, getClass().getSimpleName(), rowLine);
            if (!result.isSkipped()) {
                touchedTestIds.addAll(result.value());
                nextSortOrder++;
            }
        }

        bridgeToEditorModel(touchedTestIds);

        testService.refreshTestNames();
        DisplayListService.getInstance().refreshLists();

        summary.log(getClass().getSimpleName());
        lastSummary = summary;
    }

    private LoadedRow<List<String>> processRow(String[] values, Columns columns, int lineNumber, String fileName,
            int defaultSortOrder) {
        String testName = getValueOrEmpty(values, columns.testName());
        if (testName.isEmpty()) {
            return LoadedRow.skipped("missing testName");
        }

        String testSectionName = getValueOrEmpty(values, columns.testSection());
        if (testSectionName.isEmpty()) {
            return LoadedRow.skipped("missing testSection");
        }
        TestSection testSection = testSectionService.getTestSectionByName(testSectionName);
        if (testSection == null) {
            return LoadedRow.skipped("test section '" + testSectionName + "' not found");
        }

        String localCode = getValueOrEmpty(values, columns.localCode());
        if (localCode.length() > LOCAL_CODE_MAX_LENGTH) {
            return LoadedRow
                    .skipped("localCode '" + localCode + "' is longer than " + LOCAL_CODE_MAX_LENGTH + " characters");
        }

        String sampleTypesValue = getValueOrEmpty(values, columns.sampleType());
        List<TypeOfSample> sampleTypes = resolveSampleTypes(sampleTypesValue, lineNumber, fileName);
        if (!sampleTypesValue.isEmpty() && sampleTypes.isEmpty()) {
            return LoadedRow.skipped("none of the sample types '" + sampleTypesValue + "' exist");
        }

        Test existing = localCode.isEmpty() ? null : testService.getTestByLocalCode(localCode);
        boolean matchedByCode = existing != null;
        if (existing == null) {
            existing = findTestByPlainName(testName);
        }
        if (existing != null) {
            applyRow(existing, values, columns, testName, testSection, localCode, defaultSortOrder, false,
                    matchedByCode);
            linkSampleTypes(existing, sampleTypes);
            LogEvent.logInfo(this.getClass().getSimpleName(), "processRow",
                    "Updated existing test: " + existing.getDescription());
            return LoadedRow.updated(List.of(existing.getId()));
        }

        List<LegacyTestVariant> variants = variantsForListedSpecimens(legacyVariantFinder.find(testName), sampleTypes);
        if (!variants.isEmpty()) {
            List<String> ids = new ArrayList<>();
            boolean collapsed = variants.size() == 1;
            for (LegacyTestVariant variant : variants) {
                renameVariant(variant, testName);
                applyRow(variant.test(), values, columns, testName, testSection, collapsed ? localCode : "",
                        defaultSortOrder, false, false);
                linkSampleTypes(variant.test(), collapsed ? sampleTypes : List.of(variant.specimen()));
                ids.add(variant.test().getId());
            }
            LogEvent.logInfo(this.getClass().getSimpleName(), "processRow", "Recognised " + variants.size()
                    + " legacy variant(s) of '" + testName + "' and updated them in place");
            return LoadedRow.updated(ids);
        }

        String testId = createTest(testName, testSection, localCode, sampleTypes, values, columns);
        Test created = testService.get(testId);
        applyRow(created, values, columns, testName, testSection, localCode, defaultSortOrder, true, false);
        LogEvent.logInfo(this.getClass().getSimpleName(), "processRow",
                "Created new test: " + testName + " with " + sampleTypes.size() + " sample type(s)");
        return LoadedRow.created(List.of(testId));
    }

    private String createTest(String testName, TestSection testSection, String localCode,
            List<TypeOfSample> sampleTypes, String[] values, Columns columns) {
        CreateTestParams params = new CreateTestParams();
        params.name = testName;
        params.reportingName = testName;
        params.description = testName;
        params.code = localCode.isEmpty() ? null : localCode;
        params.labUnitId = testSection.getId();
        params.domain = domainFor(testSection);
        params.orderable = parseFlag(getValueOrEmpty(values, columns.isOrderable()), true);
        List<String> sampleTypeIds = new ArrayList<>();
        for (TypeOfSample sampleType : sampleTypes) {
            sampleTypeIds.add(sampleType.getId());
        }
        params.sampleTypeIds = sampleTypeIds;
        return testCatalogCreationService.createInactiveTest(params, SYS_USER_ID);
    }

    /**
     * Writes the row's attributes onto a test. A new test receives the column
     * defaults; an existing test keeps whatever a blank cell leaves unsaid. The
     * description follows the row only for a new test or one matched by its local
     * code, so a legacy {@code Name(SampleType)} record keeps its name.
     */
    private void applyRow(Test test, String[] values, Columns columns, String testName, TestSection testSection,
            String localCode, int defaultSortOrder, boolean isNew, boolean matchedByCode) {
        if (matchedByCode && !testName.equals(test.getDescription())) {
            test.setDescription(testName);
        }
        test.setTestSection(testSection);
        if (GenericValidator.isBlankOrNull(test.getDomain())) {
            test.setDomain(domainFor(testSection));
        }

        String loinc = getValueOrEmpty(values, columns.loinc());
        if (!loinc.isEmpty()) {
            test.setLoinc(loinc);
        }

        String isActive = getValueOrEmpty(values, columns.isActive());
        if (!isActive.isEmpty() || isNew) {
            test.setIsActive(parseFlag(isActive, true) ? "Y" : "N");
        }

        String isOrderable = getValueOrEmpty(values, columns.isOrderable());
        if (!isOrderable.isEmpty() || isNew) {
            test.setOrderable(parseFlag(isOrderable, true));
        }

        String sortOrder = getValueOrEmpty(values, columns.sortOrder());
        if (!sortOrder.isEmpty()) {
            test.setSortOrder(sortOrder);
        } else if (isNew) {
            test.setSortOrder(String.valueOf(defaultSortOrder));
        }

        String uomName = getValueOrEmpty(values, columns.unitOfMeasure());
        if (!uomName.isEmpty()) {
            UnitOfMeasure uom = findUnitOfMeasure(uomName);
            if (uom != null) {
                test.setUnitOfMeasure(uom);
            }
        }

        String isReportable = getValueOrEmpty(values, columns.isReportable());
        if (!isReportable.isEmpty() || isNew) {
            test.setIsReportable(parseFlag(isReportable, true) ? "Y" : "N");
        }

        String notifyResults = getValueOrEmpty(values, columns.notifyResults());
        if (!notifyResults.isEmpty()) {
            test.setNotifyResults(parseFlag(notifyResults, false));
        }

        if (!localCode.isEmpty()) {
            test.setLocalCode(localCode);
        }

        applyTranslations(test, values, testName, columns.localization());

        test.setSysUserId(SYS_USER_ID);
        testService.update(test);
    }

    private void applyTranslations(Test test, String[] values, String testName,
            Map<String, Integer> localizationColumns) {
        Map<String, String> translations = buildTranslationsMap(values, testName, localizationColumns);
        for (Localization localization : new Localization[] { test.getLocalizedTestName(),
                test.getLocalizedReportingName() }) {
            if (localization == null || localization.getId() == null) {
                continue;
            }
            for (Map.Entry<String, String> entry : translations.entrySet()) {
                localizationValueService.setTranslation(localization.getId(), entry.getKey(), entry.getValue(),
                        SYS_USER_ID);
            }
        }
    }

    private void bridgeToEditorModel(Set<String> testIds) {
        for (String testId : testIds) {
            try {
                testResultComponentService.syncPrimaryComponentFromLegacy(testId, SYS_USER_ID);
                Test loaded = testService.get(testId);
                if (loaded != null && !GenericValidator.isBlankOrNull(loaded.getLoinc())) {
                    terminologyMappingService.syncLegacyLoinc(testId, loaded.getLoinc(), SYS_USER_ID);
                }
            } catch (Exception e) {
                LogEvent.logError(this.getClass().getSimpleName(), "bridgeToEditorModel",
                        "Failed to bridge test " + testId + " to the new editor model: " + e.getMessage());
            }
        }
    }

    /** Plain-name identity: exact description, then normalized description. */
    private Test findTestByPlainName(String testName) {
        Test test = testService.getTestByDescription(testName);
        if (test == null) {
            test = testService.getTestByNormalizedDescription(testName);
        }
        return test;
    }

    /**
     * A legacy record belongs to the row only when the row lists its specimen; a
     * {@code Albumin(Urines)} record is a different test from an {@code Albumin}
     * row that lists Serum and is left alone.
     */
    private List<LegacyTestVariant> variantsForListedSpecimens(List<LegacyTestVariant> variants,
            List<TypeOfSample> sampleTypes) {
        List<LegacyTestVariant> listed = new ArrayList<>();
        for (LegacyTestVariant variant : variants) {
            for (TypeOfSample sampleType : sampleTypes) {
                if (sampleType.getId().equals(variant.specimen().getId())) {
                    listed.add(variant);
                    break;
                }
            }
        }
        return listed;
    }

    /**
     * A recognised variant takes the row's spelling of the name and keeps its
     * specimen suffix ({@code HIVVIRALLOAD(Serum)} becomes
     * {@code HIV Viral Load(Serum)}), unless another test already carries that
     * description or it would not fit the column.
     */
    private void renameVariant(LegacyTestVariant variant, String testName) {
        String canonical = testName + "(" + variant.specimenLabel() + ")";
        Test test = variant.test();
        if (canonical.equals(test.getDescription()) || canonical.length() > DESCRIPTION_MAX_LENGTH) {
            return;
        }
        Test taken = testService.getTestByDescription(canonical);
        if (taken == null || taken.getId().equals(test.getId())) {
            test.setDescription(canonical);
        }
    }

    private List<TypeOfSample> resolveSampleTypes(String sampleTypesValue, int lineNumber, String fileName) {
        Map<String, TypeOfSample> resolved = new LinkedHashMap<>();
        if (sampleTypesValue.isEmpty()) {
            return new ArrayList<>(resolved.values());
        }
        for (String sampleTypeName : sampleTypesValue.split("\\|")) {
            sampleTypeName = sampleTypeName.trim();
            if (sampleTypeName.isEmpty()) {
                continue;
            }
            TypeOfSample sampleType = findSampleType(sampleTypeName);
            if (sampleType == null) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "resolveSampleTypes", "Sample type '" + sampleTypeName
                        + "' not found in line " + lineNumber + " of " + fileName + ". Ignoring it.");
                continue;
            }
            resolved.put(sampleType.getId(), sampleType);
        }
        return new ArrayList<>(resolved.values());
    }

    private void linkSampleTypes(Test test, List<TypeOfSample> sampleTypes) {
        for (TypeOfSample sampleType : sampleTypes) {
            createSingleSampleTypeMapping(test, sampleType);
        }
    }

    private String domainFor(TestSection testSection) {
        return GenericValidator.isBlankOrNull(testSection.getDomain()) ? DEFAULT_DOMAIN : testSection.getDomain();
    }

    private boolean parseFlag(String value, boolean defaultValue) {
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        return "Y".equalsIgnoreCase(value) || "true".equalsIgnoreCase(value);
    }

    private String[] parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder currentValue = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                values.add(currentValue.toString().trim());
                currentValue = new StringBuilder();
            } else {
                currentValue.append(c);
            }
        }
        values.add(currentValue.toString().trim());

        return values.toArray(new String[0]);
    }

    private void validateHeaders(String[] headers, String fileName) {
        boolean hasTestNameColumn = false;
        boolean hasTestSectionColumn = false;

        for (String header : headers) {
            if ("testName".equalsIgnoreCase(header)) {
                hasTestNameColumn = true;
            }
            if ("testSection".equalsIgnoreCase(header)) {
                hasTestSectionColumn = true;
            }
        }

        if (!hasTestNameColumn) {
            throw new IllegalArgumentException(
                    "Test configuration file " + fileName + " must have a 'testName' column");
        }
        if (!hasTestSectionColumn) {
            throw new IllegalArgumentException(
                    "Test configuration file " + fileName + " must have a 'testSection' column");
        }
    }

    private int findColumnIndex(String[] headers, String columnName) {
        for (int i = 0; i < headers.length; i++) {
            if (columnName.equalsIgnoreCase(headers[i])) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Detects localization columns from headers. Columns must be in the format
     * "localization:xx" where xx is a locale code (e.g., en, fr, es).
     *
     * @param headers the CSV header row
     * @return map of locale code to column index
     */
    private Map<String, Integer> detectLocalizationColumns(String[] headers) {
        Map<String, Integer> localizationColumns = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            String header = headers[i].trim().toLowerCase();
            if (header.startsWith(LOCALIZATION_COLUMN_PREFIX)) {
                String locale = header.substring(LOCALIZATION_COLUMN_PREFIX.length());
                if (!locale.isEmpty()) {
                    localizationColumns.put(locale, i);
                }
            }
        }
        return localizationColumns;
    }

    private String getValueOrEmpty(String[] values, int index) {
        if (index >= 0 && index < values.length) {
            String value = values[index];
            return value != null ? value : "";
        }
        return "";
    }

    /**
     * Builds a map of translations from localization columns. If no translations
     * are provided, uses the default name as the fallback (en) value.
     */
    private Map<String, String> buildTranslationsMap(String[] values, String defaultName,
            Map<String, Integer> localizationColumns) {
        Map<String, String> translations = new HashMap<>();

        if (localizationColumns.isEmpty()) {
            translations.put("en", defaultName);
        } else {
            for (Map.Entry<String, Integer> entry : localizationColumns.entrySet()) {
                String locale = entry.getKey();
                String translationValue = getValueOrEmpty(values, entry.getValue());
                if (!translationValue.isEmpty()) {
                    translations.put(locale, translationValue);
                }
            }

            if (translations.isEmpty()) {
                translations.put("en", defaultName);
            }
        }

        return translations;
    }

    private UnitOfMeasure findUnitOfMeasure(String uomName) {
        List<UnitOfMeasure> allUom = unitOfMeasureService.getAll();
        for (UnitOfMeasure uom : allUom) {
            if (uom.getUnitOfMeasureName() != null && uom.getUnitOfMeasureName().equalsIgnoreCase(uomName)) {
                return uom;
            }
            if (uom.getDescription() != null && uom.getDescription().equalsIgnoreCase(uomName)) {
                return uom;
            }
        }
        return null;
    }

    private void createSingleSampleTypeMapping(Test test, TypeOfSample sampleType) {
        if (!mappingExists(test.getId(), sampleType.getId())) {
            TypeOfSampleTest mapping = new TypeOfSampleTest();
            mapping.setTestId(test.getId());
            mapping.setTypeOfSampleId(sampleType.getId());
            mapping.setSysUserId(SYS_USER_ID);
            typeOfSampleTestService.insert(mapping);
            LogEvent.logDebug(this.getClass().getSimpleName(), "createSingleSampleTypeMapping",
                    "Created mapping: test '" + test.getDescription() + "' -> sample type '"
                            + sampleType.getLocalizedName() + "'");
        }
    }

    private TypeOfSample findSampleType(String sampleTypeName) {
        List<TypeOfSample> allSampleTypes = typeOfSampleService.getAllTypeOfSamples();

        for (TypeOfSample sampleType : allSampleTypes) {
            if (sampleType.getLocalizedName() != null
                    && sampleType.getLocalizedName().equalsIgnoreCase(sampleTypeName)) {
                return sampleType;
            }
            if (sampleType.getDescription() != null && sampleType.getDescription().equalsIgnoreCase(sampleTypeName)) {
                return sampleType;
            }
            if (sampleType.getLocalAbbreviation() != null
                    && sampleType.getLocalAbbreviation().equalsIgnoreCase(sampleTypeName)) {
                return sampleType;
            }
        }
        return null;
    }

    private boolean mappingExists(String testId, String sampleTypeId) {
        List<TypeOfSampleTest> existingMappings = typeOfSampleTestService.getTypeOfSampleTestsForTest(testId);
        for (TypeOfSampleTest mapping : existingMappings) {
            if (mapping.getTypeOfSampleId().equals(sampleTypeId)) {
                return true;
            }
        }
        return false;
    }
}
