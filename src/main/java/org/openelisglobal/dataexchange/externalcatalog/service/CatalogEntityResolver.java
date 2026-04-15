package org.openelisglobal.dataexchange.externalcatalog.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.dictionarycategory.service.DictionaryCategoryService;
import org.openelisglobal.dictionarycategory.valueholder.DictionaryCategory;
import org.openelisglobal.localization.service.LocalizationServiceImpl;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.valueholder.TestSection;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.openelisglobal.typeoftestresult.service.TypeOfTestResultService;
import org.openelisglobal.typeoftestresult.service.TypeOfTestResultServiceImpl;
import org.openelisglobal.unitofmeasure.service.UnitOfMeasureService;
import org.openelisglobal.unitofmeasure.valueholder.UnitOfMeasure;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CatalogEntityResolver {

    @Autowired
    private TypeOfSampleService typeOfSampleService;
    @Autowired
    private TestSectionService testSectionService;
    @Autowired
    private UnitOfMeasureService unitOfMeasureService;
    @Autowired
    private TypeOfTestResultService typeOfTestResultService;
    @Autowired
    private DictionaryService dictionaryService;
    @Autowired
    private DictionaryCategoryService dictionaryCategoryService;
    @Autowired
    private org.openelisglobal.localization.service.LocalizationService localizationService;

    @Value("${org.openelisglobal.ocl.import.default.sampletype:Whole Blood}")
    private String defaultSampleTypeName;

    @Value("${org.openelisglobal.ocl.import.default.testsection:Hematology}")
    private String defaultTestSectionName;

    public TypeOfSample resolveSampleType(String id, String name, String currentUserId) {
        if (!GenericValidator.isBlankOrNull(id)) {
            TypeOfSample tos = typeOfSampleService.getTypeOfSampleById(id);
            if (tos != null)
                return tos;
        }

        String targetName = GenericValidator.isBlankOrNull(name) ? defaultSampleTypeName : name;
        // Hibernate length is 20 for description
        if (targetName.length() > 20)
            targetName = targetName.substring(0, 20);

        List<TypeOfSample> all = typeOfSampleService.getAllTypeOfSamples();
        if (all == null)
            all = new ArrayList<>();

        String finalTargetName = targetName;
        Optional<TypeOfSample> match = all.stream().filter(
                t -> t.getDescription() != null && t.getDescription().trim().equalsIgnoreCase(finalTargetName.trim()))
                .findFirst();

        if (match.isPresent())
            return match.get();

        // Auto-create
        TypeOfSample newTos = new TypeOfSample();
        newTos.setDescription(finalTargetName);

        // Generate collision-safe abbreviation (max 10 chars)
        String baseAbbrev = finalTargetName.length() > 10 ? finalTargetName.substring(0, 10) : finalTargetName;
        String finalAbbrev = baseAbbrev;
        int suffix = 1;
        while (isAbbreviationTaken(all, finalAbbrev)) {
            String suffixStr = String.valueOf(suffix);
            int availableLength = 10 - suffixStr.length();
            String prefix = baseAbbrev.length() > availableLength ? baseAbbrev.substring(0, availableLength)
                    : baseAbbrev;
            finalAbbrev = prefix + suffixStr;
            suffix++;
        }
        newTos.setLocalAbbreviation(finalAbbrev);
        newTos.setDomain("H"); // Domain length is 1 in HBM
        newTos.setActive(true);
        newTos.setSysUserId(currentUserId);

        // Required localization
        Localization localization = LocalizationServiceImpl.createNewLocalization(finalTargetName, finalTargetName,
                LocalizationServiceImpl.LocalizationType.SAMPLE_TYPE_NAME);
        localization.setSysUserId(currentUserId);
        localizationService.insert(localization);
        newTos.setLocalization(localization);

        typeOfSampleService.insert(newTos);
        return newTos;
    }

    public TestSection resolveTestSection(String id, String name, String currentUserId) {
        if (!GenericValidator.isBlankOrNull(id)) {
            TestSection ts = testSectionService.getTestSectionById(id);
            if (ts != null)
                return ts;
        }

        String targetName = GenericValidator.isBlankOrNull(name) ? defaultTestSectionName : name;
        // Hibernate length is 20 for name, 60 for description
        if (targetName.length() > 20)
            targetName = targetName.substring(0, 20);

        TestSection match = testSectionService.getTestSectionByName(targetName);
        if (match != null)
            return match;

        // Auto-create
        TestSection newTs = new TestSection();
        newTs.setTestSectionName(targetName);
        newTs.setDescription(targetName);
        newTs.setIsActive("Y");
        newTs.setSysUserId(currentUserId);

        // Required localization
        Localization localization = LocalizationServiceImpl.createNewLocalization(targetName, targetName,
                LocalizationServiceImpl.LocalizationType.TEST_NAME); // Fallback to TEST_NAME as TEST_SECTION_NAME is
                                                                     // missing
        localization.setSysUserId(currentUserId);
        localizationService.insert(localization);
        newTs.setLocalization(localization);

        testSectionService.insert(newTs);
        return newTs;
    }

    public UnitOfMeasure resolveUOM(String id, String name, String currentUserId) {
        if (!GenericValidator.isBlankOrNull(id)) {
            UnitOfMeasure uom = unitOfMeasureService.getUnitOfMeasureById(id);
            if (uom != null)
                return uom;
        }

        if (GenericValidator.isBlankOrNull(name))
            return null;

        String targetName = name;
        if (targetName.length() > 20)
            targetName = targetName.substring(0, 20);

        // UOMs don't have a simple list fetch by name, so we check all
        for (UnitOfMeasure uom : (List<UnitOfMeasure>) unitOfMeasureService.getAll()) {
            if (uom.getUnitOfMeasureName().trim().equalsIgnoreCase(targetName.trim())) {
                return uom;
            }
        }

        // Auto-create
        UnitOfMeasure newUom = new UnitOfMeasure();
        newUom.setUnitOfMeasureName(targetName);
        newUom.setDescription(targetName);
        newUom.setSysUserId(currentUserId);
        unitOfMeasureService.insert(newUom);
        return newUom;
    }

    public String resolveResultTypeId(String id, String name) {
        if (!GenericValidator.isBlankOrNull(id)) {
            if (typeOfTestResultService.getResultTypeById(id) != null)
                return id;
        }

        if (!GenericValidator.isBlankOrNull(name)) {
            for (TypeOfTestResultServiceImpl.ResultType type : TypeOfTestResultServiceImpl.ResultType.values()) {
                if (type.name().equalsIgnoreCase(name.trim())) {
                    return type.getId();
                }
            }
        }

        // No silent fallback — the validator should have caught this before we get
        // here.
        // If somehow we reach this point (e.g. called outside the normal pipeline),
        // throw explicitly so the transaction rolls back cleanly.
        throw new org.openelisglobal.dataexchange.externalcatalog.exception.CatalogValidationException(
                "Cannot resolve resultType — no valid 'resultTypeId' or 'resultTypeName' provided");
    }

    public Dictionary resolveDictionaryEntry(String id, String uuid, String loincCode, String name,
            String currentUserId) {
        // Step 1: numeric DB id
        if (!GenericValidator.isBlankOrNull(id)) {
            Dictionary dict = dictionaryService.getDictionaryById(id);
            if (dict != null)
                return dict;
        }

        // Step 2: GUID / UUID
        if (!GenericValidator.isBlankOrNull(uuid)) {
            Dictionary dict = dictionaryService.getDictionaryByGuid(uuid);
            if (dict != null)
                return dict;
        }

        // Step 3: LOINC code
        if (!GenericValidator.isBlankOrNull(loincCode)) {
            Dictionary dict = dictionaryService.getDictionaryByLoincCode(loincCode);
            if (dict != null)
                return dict;
        }

        // Step 4a: exact name match (case-sensitive, fast)
        if (!GenericValidator.isBlankOrNull(name)) {
            Dictionary dict = dictionaryService.getDictionaryByDictEntry(name);
            if (dict != null)
                return dict;
        }

        // Step 4b: case-insensitive name match
        if (!GenericValidator.isBlankOrNull(name)) {
            Dictionary dict = dictionaryService.getDictionaryByDictEntryIgnoreCase(name);
            if (dict != null)
                return dict;
        }

        // Step 5: auto-create under "Test Result" category — same as OCL importer
        if (GenericValidator.isBlankOrNull(name))
            return null;

        DictionaryCategory testResultCategory = dictionaryCategoryService.getDictionaryCategoryByName("Test Result");
        if (testResultCategory == null) {
            // Safety net: if category doesn't exist, don't crash
            return null;
        }

        Dictionary newDict = new Dictionary();
        newDict.setDictEntry(name.trim());
        newDict.setLocalAbbreviation(name.trim().length() > 10 ? name.trim().substring(0, 10) : name.trim());
        newDict.setIsActive("Y");
        newDict.setSortOrder(1);
        newDict.setDictionaryCategory(testResultCategory);
        newDict.setSysUserId(currentUserId);
        if (!GenericValidator.isBlankOrNull(uuid)) {
            newDict.setGuid(uuid);
        }
        if (!GenericValidator.isBlankOrNull(loincCode)) {
            newDict.setLoincCode(loincCode);
        }

        // Duplicate guard — same as OCL.
        // The category description in the DB seed is "General test result"
        // (category name is "Test Result").
        // getDictionaryEntrysByNameAndCategoryDescription
        // queries the description column, so "General test result" is the correct
        // value.
        if (dictionaryService.duplicateDictionaryExists(newDict)) {
            Dictionary existing = dictionaryService.getDictionaryEntrysByNameAndCategoryDescription(name.trim(),
                    "General test result");
            if (existing == null) {
                // The duplicate check fired but the retrieval returned nothing — this means
                // the entry exists in a different category. Log it so it's visible, but do
                // NOT crash. The caller's null-check will skip this entry gracefully.
                LogEvent.logWarn(this.getClass().getSimpleName(), "resolveDictionaryEntry",
                        "Duplicate dictionary entry detected for '" + name.trim()
                                + "' but retrieval by category description returned null — entry will be skipped");
            }
            return existing;
        }

        // Localization — same as OCL
        Localization localization = LocalizationServiceImpl.createNewLocalization(name.trim(), name.trim(),
                LocalizationServiceImpl.LocalizationType.DICTIONARY_NAME);
        localization.setSysUserId(currentUserId);
        localizationService.insert(localization);
        newDict.setLocalizedDictionaryName(localization);

        return dictionaryService.save(newDict);
    }

    private boolean isAbbreviationTaken(List<TypeOfSample> all, String abbrev) {
        if (all == null || abbrev == null)
            return false;
        return all.stream().filter(t -> t.getLocalAbbreviation() != null)
                .anyMatch(t -> abbrev.equalsIgnoreCase(t.getLocalAbbreviation()));
    }
}
