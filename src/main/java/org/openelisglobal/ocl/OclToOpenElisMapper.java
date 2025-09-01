// File: OclToOpenElisMapper.java (Corrected field name "concept class")
package org.openelisglobal.ocl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.dictionarycategory.service.DictionaryCategoryService;
import org.openelisglobal.localization.service.LocalizationService;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.valueholder.TestSection;
import org.openelisglobal.testconfiguration.form.TestAddForm;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.openelisglobal.typeoftestresult.service.TypeOfTestResultService;
import org.openelisglobal.typeoftestresult.valueholder.TypeOfTestResult;
import org.openelisglobal.unitofmeasure.service.UnitOfMeasureService;
import org.openelisglobal.unitofmeasure.valueholder.UnitOfMeasure;

public class OclToOpenElisMapper {
    private static final Log log = LogFactory.getLog(OclToOpenElisMapper.class);

    private String defaultTestSection;
    private String defaultSampleType;
    private JsonNode rootNode;

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private TypeOfTestResultService typeOfTestResultService = SpringContext.getBean(TypeOfTestResultService.class);
    private TestSectionService testSectionService = SpringContext.getBean(TestSectionService.class);
    private UnitOfMeasureService uomSerivice = SpringContext.getBean(UnitOfMeasureService.class);
    private TypeOfSampleService typeOfSampleService = SpringContext.getBean(TypeOfSampleService.class);
    private DictionaryService dictionaryService = SpringContext.getBean(DictionaryService.class);
    private DictionaryCategoryService dictionaryCategoryService = SpringContext
            .getBean(DictionaryCategoryService.class);
    private LocalizationService localizationService = SpringContext.getBean(LocalizationService.class);

    public OclToOpenElisMapper(String defaultTestSection, String defaultSampleType) {
        this.defaultTestSection = defaultTestSection;
        this.defaultSampleType = defaultSampleType;
    }

    private static final Set<String> SUPPORTED_DATATYPES = Set.of("NUMERIC", "NUMBER", "TEXT", "STRING", "CODED",
            "SELECT");

    private static final Set<String> ALLOWED_CONCEPT_CLASSES = Set.of("TEST");

    // Map OCL data types to OpenELIS result type IDs
    private static final Map<String, String> RESULT_TYPE_MAPPING = new HashMap<>();
    static {
        RESULT_TYPE_MAPPING.put("NUMERIC", "N");
        RESULT_TYPE_MAPPING.put("NUMBER", "N");
        RESULT_TYPE_MAPPING.put("CODED", "D");
        RESULT_TYPE_MAPPING.put("SELECT", "D");
        RESULT_TYPE_MAPPING.put("BOOLEAN", "D");
        RESULT_TYPE_MAPPING.put("TEXT", "R");
        RESULT_TYPE_MAPPING.put("STRING", "R");
    }

    /**
     * Creates a wrapper node with concepts array for a single concept
     * 
     * @param singleConcept The JSON node representing a single OCL concept.
     * @return A JSON node wrapper containing the single concept in a "concepts"
     *         array.
     */
    public JsonNode createConceptWrapper(JsonNode singleConcept) {
        ObjectNode wrapper = objectMapper.createObjectNode();
        ArrayNode conceptsArray = objectMapper.createArrayNode();
        conceptsArray.add(singleConcept);
        wrapper.set("concepts", conceptsArray);
        return wrapper;
    }

    /**
     * Maps OCL concepts to TestAddForm objects ready for submission, applying
     * filters for datatype and concept_class.
     * 
     * @param rootNode The root JSON node from OCL export, containing a "concepts"
     *                 array.
     * @return A list of TestAddForm objects that pass the filters.
     */
    public List<TestAddForm> mapConceptsToTestAddForms(JsonNode rootNode) {
        try {
            List<TestAddForm> forms = new ArrayList<>();
            this.rootNode = rootNode;

            // Validate root node structure
            if (!rootNode.has("type") || !"Collection Version".equals(getText(rootNode, "type"))) {
                log.error("Invalid OCL export format. Expected Collection Version type.");
                return forms;
            }

            // Handle concepts array from OCL export
            JsonNode concepts = rootNode.get("concepts");
            if (concepts != null && concepts.isArray()) {
                log.info("Processing " + concepts.size() + " concepts from collection: "
                        + getText(rootNode, "full_name"));

                for (JsonNode conceptNode : concepts) {
                    String conceptId = getText(conceptNode, "id");
                    String displayName = getText(conceptNode, "display_name");
                    log.info("Processing concept: " + displayName + " (ID: " + conceptId + ")");

                    TestAddForm form = mapSingleConceptToForm(conceptNode);
                    if (form != null) {
                        forms.add(form);
                    }
                }
            } else {
                log.error("Expected 'concepts' array in OCL export");
            }

            return forms;
        } catch (Exception e) {
            log.error("Error mapping OCL concepts to TestAddForm: " + e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Maps a single OCL concept to a TestAddForm, applying filters and specific
     * mappings.
     * 
     * @param concept The JSON node representing a single OCL concept.
     * @return A TestAddForm object if the concept passes all filters, otherwise
     *         null.
     */
    private TestAddForm mapSingleConceptToForm(JsonNode concept) {
        try {
            String conceptId = getText(concept, "id");
            String displayName = getText(concept, "display_name");
            String dataType = getText(concept, "datatype");
            String conceptClass = getText(concept, "concept_class");

            // Log detailed concept information for debugging
            logConceptDetails(concept);

            // Datatype filtering: Only map specific test types
            if (dataType == null || !SUPPORTED_DATATYPES.contains(dataType.toUpperCase())) {
                log.info("Skipping concept " + conceptId + " due to unsupported datatype: " + dataType);
                return null;
            }

            if (conceptClass == null || !ALLOWED_CONCEPT_CLASSES.contains(conceptClass.toUpperCase())) {
                log.info("Skipping concept " + conceptId + " (name: " + displayName + ") "
                        + "due to unsupported concept_class: " + conceptClass);
                return null;
            }

            // Log concept class information
            log.info("Processing concept ID: " + conceptId + ", Name: " + displayName);
            log.info("Concept Class: " + conceptClass + ", Data Type: " + dataType);

            TestAddForm form = new TestAddForm();

            // Create the JSON structure expected by TestAddRestController
            ObjectNode jsonWad = objectMapper.createObjectNode();

            // Map all required fields in the exact format
            mapTestNames(concept, jsonWad);
            mapTestSection(concept, jsonWad); // Hardcoded to Hematology
            mapPanels(concept, jsonWad);
            mapUnits(concept, jsonWad);
            mapLoinc(concept, jsonWad);
            mapResultType(concept, jsonWad);
            mapOrderableFlags(concept, jsonWad);
            mapSampleTypes(concept, jsonWad);
            mapNumericValidation(concept, jsonWad);
            mapResultLimits(concept, jsonWad);
            mapDictionaryResults(concept, jsonWad);
            // Convert to JSON string and set in form
            String jsonWadString = objectMapper.writeValueAsString(jsonWad);
            form.setJsonWad(jsonWadString);

            log.info("Successfully mapped OCL concept " + conceptId + " to TestAddForm");
            log.debug("Generated JSON: " + jsonWadString);

            return form;
        } catch (Exception e) {
            log.error("Error mapping single OCL concept: " + e.getMessage(), e);
            return null;
        }
    }

    private void mapTestNames(JsonNode concept, ObjectNode jsonWad) {
        String englishName = null;
        String frenchName = null;

        // Get display_name as initial English name
        englishName = getText(concept, "display_name");

        // Process names array for best matching names
        JsonNode names = concept.get("names");
        if (names != null && names.isArray()) {
            // Process names for both FULLY_SPECIFIED and preferred names
            for (JsonNode nameNode : names) {
                String locale = getText(nameNode, "locale");
                String name = getText(nameNode, "name");
                String nameType = getText(nameNode, "name_type");

                // Handle FULLY_SPECIFIED preferred names first
                if (name != null && "FULLY_SPECIFIED".equals(nameType)) {
                    if ("en".equals(locale)) {
                        englishName = name; // Overwrite display_name with preferred English name
                    } else if ("fr".equals(locale)) {
                        frenchName = name;
                    }
                }
                // Handle other cases
                else if (name != null) {
                    // For English, prefer FULLY_SPECIFIED name if display_name wasn't found
                    if ("en".equals(locale) && englishName == null) {
                        englishName = name;
                    }
                    // For French, prefer FULLY_SPECIFIED name
                    else if ("fr".equals(locale) && frenchName == null) {
                        frenchName = name;
                    }
                }
            }
        }

        // Further fallbacks if still no English name
        if (englishName == null) {
            JsonNode descriptions = concept.get("descriptions");
            if (descriptions != null && descriptions.isArray() && descriptions.size() > 0) {
                englishName = getText(descriptions.get(0), "description");
            }
            if (englishName == null) {
                englishName = getText(concept, "id");
            }
        }

        // Ensure French name has fallback
        if (frenchName == null) {
            frenchName = englishName;
        }

        jsonWad.put("testNameEnglish", englishName != null ? englishName : "");
        jsonWad.put("testNameFrench", frenchName != null ? frenchName : "");
        jsonWad.put("testReportNameEnglish", englishName != null ? englishName : "");
        jsonWad.put("testReportNameFrench", frenchName != null ? frenchName : "");
    }

    private void mapTestSection(JsonNode concept, ObjectNode jsonWad) {
        JsonNode extras = concept.get("extras");
        String testSectionId = null;
        TestSection testSection = null;
        if (extras.has("test_section")) {
            String oclTestSection = getText(extras, "test_section");
            testSection = testSectionService.getTestSectionByName(oclTestSection);
        }

        if (testSection == null) {
            testSection = testSectionService.getTestSectionByName(defaultTestSection);
        }

        if (testSection == null) {
            testSection = testSectionService.getTestSectionByName("Hematology");
        }
        if (testSection != null) {
            testSectionId = testSection.getId();
        }

        jsonWad.put("testSection", testSectionId);
    }

    private void mapPanels(JsonNode concept, ObjectNode jsonWad) {
        // Initialize as empty array - can be enhanced to map OCL panel relationships
        ArrayNode panelsArray = objectMapper.createArrayNode();
        jsonWad.set("panels", panelsArray);
    }

    private void mapUnits(JsonNode concept, ObjectNode jsonWad) {
        String units = null;
        String unitsId = null;

        // Try extras first (OCL standard: units attribute in extras)
        JsonNode extras = concept.get("extras");
        if (extras != null && extras.has("units")) {
            units = getText(extras, "units");
        }

        if (StringUtils.isNotBlank(units)) {
            UnitOfMeasure uom = new UnitOfMeasure();
            uom.setUnitOfMeasureName(units);
            UnitOfMeasure dbUom = uomSerivice.getUnitOfMeasureByName(uom);
            if (dbUom != null) {
                unitsId = dbUom.getId();
            }

        }

        jsonWad.put("uom", unitsId != null ? unitsId : "");
    }

    private void mapLoinc(JsonNode concept, ObjectNode jsonWad) {
        String id = getText(concept, "id");
        String loinc = getLoinc(id);
        jsonWad.put("loinc", loinc != null ? loinc : "");
    }

    private String getLoinc(String id) {
        String loinc = null;

        JsonNode mappings = this.rootNode.get("mappings");
        if (mappings != null && mappings.isArray()) {
            String fallbackLoinc = null;

            for (JsonNode mapping : mappings) {
                if (!id.equals(getText(mapping, "from_concept_code"))) {
                    continue;
                }

                String mapType = getText(mapping, "map_type");
                String toSourceName = getText(mapping, "to_source_name");
                String candidateLoinc = getText(mapping, "to_concept_code");

                // Priority 1: SAME-AS mapping pointing to LOINC
                if ("SAME-AS".equals(mapType) && "LOINC".equals(toSourceName)
                        && StringUtils.isNotBlank(candidateLoinc)) {
                    loinc = candidateLoinc;
                    log.info("Found SAME-AS LOINC code: " + loinc + " for concept " + id);
                    break; // stop immediately since we found the best match
                }

                // Priority 2: any mapping pointing to LOINC (keep as fallback)
                if ("LOINC".equals(toSourceName) && StringUtils.isNotBlank(candidateLoinc)) {
                    fallbackLoinc = candidateLoinc;
                }
            }

            // If we didn’t find a SAME-AS → LOINC, use fallback if available
            if (loinc == null && fallbackLoinc != null) {
                loinc = fallbackLoinc;
                log.info("Found Other LOINC code: " + loinc + " for concept " + id);
            }
        }
        return loinc;
    }

    private void mapResultType(JsonNode concept, ObjectNode jsonWad) {
        String dataType = getText(concept, "datatype");
        String resultType = "R"; // Default to text

        if (dataType != null) {
            String mappedType = RESULT_TYPE_MAPPING.get(dataType.toUpperCase());
            if (mappedType != null) {
                resultType = mappedType;
            }
        }

        // Convert to actual DB ID if TypeOfTestResultService is available via
        // SpringContext
        try {

            TypeOfTestResult typeObj = typeOfTestResultService.getTypeOfTestResultByType(resultType);
            if (typeObj != null && typeObj.getId() != null) {
                jsonWad.put("resultType", typeObj.getId());
            }
        } catch (Exception e) {
            log.error("Error mapping result type (Spring context not available or service failed): " + e.getMessage(),
                    e);
        }
    }

    private void mapOrderableFlags(JsonNode concept, ObjectNode jsonWad) {

        jsonWad.put("orderable", "Y");
        jsonWad.put("notifyResults", "N");
        jsonWad.put("inLabOnly", "N");
        jsonWad.put("antimicrobialResistance", "N");
        Boolean retired = Boolean.valueOf(getText(concept, "retired"));
        jsonWad.put("active", retired ? "N" : "Y");
    }

    private void mapSampleTypes(JsonNode concept, ObjectNode jsonWad) {
        ArrayNode sampleTypesArray = objectMapper.createArrayNode();

        JsonNode extras = concept.get("extras");
        TypeOfSample typeOfSample = null;
        if (extras.has("sample_type")) {
            String ocltypeOfSample = getText(extras, "sample_type");
            TypeOfSample tos = new TypeOfSample();
            tos.setDescription(ocltypeOfSample);
            typeOfSample = typeOfSampleService.getTypeOfSampleByDescriptionAndDomain(tos, true);

            if (typeOfSample == null) {
                typeOfSample = typeOfSampleService.getTypeOfSampleByLocalAbbrevAndDomain(ocltypeOfSample, "H");
            }
        }

        if (typeOfSample == null) {
            TypeOfSample tos = new TypeOfSample();
            tos.setDescription(defaultSampleType);
            typeOfSample = typeOfSampleService.getTypeOfSampleByDescriptionAndDomain(tos, true);

            if (typeOfSample == null) {
                typeOfSample = typeOfSampleService.getTypeOfSampleByLocalAbbrevAndDomain(defaultSampleType, "H");
            }
        }

        if (typeOfSample == null) {
            TypeOfSample tos = new TypeOfSample();
            tos.setDescription("Whole Blood");
            typeOfSample = typeOfSampleService.getTypeOfSampleByDescriptionAndDomain(tos, true);
        }
        if (typeOfSample != null) {
            ObjectNode sampleTypeObj = objectMapper.createObjectNode();
            sampleTypeObj.put("typeId", typeOfSample.getId());

            ArrayNode testsArray = objectMapper.createArrayNode();
            ObjectNode testOrder = objectMapper.createObjectNode();
            testOrder.put("id", 0); // New test placeholder (ID 0 usually means the test being added)
            testsArray.add(testOrder);

            sampleTypeObj.set("tests", testsArray);
            sampleTypesArray.add(sampleTypeObj);
        }

        jsonWad.set("sampleTypes", sampleTypesArray);
    }

    private void mapNumericValidation(JsonNode concept, ObjectNode jsonWad) {
        String dataType = getText(concept, "datatype");
        boolean isNumeric = dataType != null
                && (dataType.toUpperCase().contains("NUMERIC") || dataType.toUpperCase().contains("NUMBER"));

        String lowValid = "-Infinity";
        String highValid = "Infinity";
        String lowReporting = "-Infinity";
        String highReporting = "Infinity";
        String lowCritical = "-Infinity";
        String highCritical = "Infinity";
        String lowNormal = "-Infinity";
        String highNormal = "Infinity";
        String sigDigits = "0";

        if (isNumeric) {
            log.info("Mapping NUMERIC result type for concept: " + getText(concept, "id"));

            // Map validation ranges from 'extras' (OCL standard attributes)
            JsonNode extras = concept.get("extras");

            if (extras != null) {
                String lowAbs = getNumericText(extras, "low_absolute");
                if (isNumeric(lowAbs)) {
                    lowValid = lowAbs;
                }

                String hiAbs = getNumericText(extras, "hi_absolute");
                if (isNumeric(hiAbs)) {
                    highValid = hiAbs;
                }
                String lowReportingValue = getNumericText(extras, "low_reporting");
                if (isNumeric(lowReportingValue)) {
                    lowReporting = lowReportingValue;
                }

                String highReportingValue = getNumericText(extras, "hi_reporting");
                if (isNumeric(highReportingValue)) {
                    highReporting = highReportingValue;
                }

                String lowCriticalValue = getNumericText(extras, "low_critical");
                if (isNumeric(lowCriticalValue)) {
                    lowCritical = lowCriticalValue;
                }

                String highCriticalValue = getNumericText(extras, "hi_critical");
                if (isNumeric(highCriticalValue)) {
                    highCritical = highCriticalValue;
                }

                String lowNormalValue = getNumericText(extras, "low_normal");
                if (isNumeric(lowNormalValue)) {
                    lowNormal = lowNormalValue;
                }

                String highNormalValue = getNumericText(extras, "hi_normal");
                if (isNumeric(highNormalValue)) {
                    highNormal = highNormalValue;
                }

                // Check for allow_decimal which indicates significant digits
                String allowDecimal = getNumericText(extras, "allow_decimal");
                if (allowDecimal != null) {
                    if ("true".equalsIgnoreCase(allowDecimal)) {
                        sigDigits = "2"; // Default to 2 decimal places if decimals are allowed
                    } else if ("false".equalsIgnoreCase(allowDecimal)) {
                        sigDigits = "0"; // No decimal places
                    }
                }
            }

        }
        jsonWad.put("lowValid", lowValid);
        jsonWad.put("highValid", highValid);
        jsonWad.put("lowReportingRange", lowReporting);
        jsonWad.put("highReportingRange", highReporting);
        jsonWad.put("lowCritical", lowCritical);
        jsonWad.put("highCritical", highCritical);
        jsonWad.put("lowNormal", lowNormal);
        jsonWad.put("highNormal", highNormal);
        jsonWad.put("significantDigits", sigDigits);
    }

    private static boolean isNumeric(String str) {
        if (StringUtils.isBlank(str)) {
            return false;
        }
        try {
            Double.parseDouble(str); // or Integer.parseInt(str) if you want only integers
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void mapResultLimits(JsonNode concept, ObjectNode jsonWad) {
        ArrayNode resultLimitsArray = objectMapper.createArrayNode();
        ObjectNode limit = objectMapper.createObjectNode();
        limit.put("ageRange", "0");
        limit.put("highAgeRange", "Infinity");
        limit.put("gender", false);
        limit.put("lowNormal", "-Infinity");
        limit.put("highNormal", "Infinity");
        limit.put("lowNormalFemale", "-Infinity");
        limit.put("highNormalFemale", "Infinity");
        resultLimitsArray.add(limit);
        jsonWad.put("resultLimits", resultLimitsArray);
    }

    private void mapDictionaryResults(JsonNode concept, ObjectNode jsonWad) {
        String dataType = getText(concept, "datatype");
        boolean isDictionary = dataType != null
                && (dataType.toUpperCase().equals("CODED") || dataType.toUpperCase().equals("SELECT"));

        ArrayNode dictionaryArray = objectMapper.createArrayNode();
        if (isDictionary) {
            log.info("Mapping CODED/SELECT result type for concept: " + getText(concept, "id"));
            String id = getText(concept, "id");

            // Try mappings array first (OCL standard format for LOINC)
            JsonNode mappings = this.rootNode.get("mappings");
            if (mappings != null && mappings.isArray()) {
                for (JsonNode mapping : mappings) {
                    String fromConceptCode = getText(mapping, "from_concept_code");
                    String toCoceptCode = getText(mapping, "to_concept_code");
                    String toCoceptName = getText(mapping, "to_concept_name_resolved");
                    String mapType = getText(mapping, "map_type");
                    if (!fromConceptCode.equals(id) || !mapType.equals("Q-AND-A")) {
                        continue;
                    }

                    Dictionary dictionary = new Dictionary();
                    dictionary.setSortOrder(1);
                    dictionary.setIsActive("Y");
                    dictionary.setDictEntry(toCoceptName);
                    dictionary.setLocalAbbreviation(toCoceptCode);
                    dictionary.setSysUserId("1");
                    dictionary.setLoincCode(getLoinc(toCoceptCode));
                    dictionary.setDictionaryCategory(
                            dictionaryCategoryService.getDictionaryCategoryByName("Test Result"));

                    Localization localization = new Localization();
                    localization.setEnglish(toCoceptName);
                    localization.setFrench(toCoceptName);
                    localization.setSysUserId("1");
                    localization = localizationService.save(localization);

                    dictionary.setLocalizedDictionaryName(localization);
                    dictionary = dictionaryService.save(dictionary);

                    ObjectNode dictEntry = objectMapper.createObjectNode();
                    dictEntry.put("id", String.valueOf(dictionary.getId()));
                    dictEntry.put("qualified", "N");
                    dictionaryArray.add(dictEntry);

                }
            }
        }
        jsonWad.put("dictionary", dictionaryArray);
        jsonWad.put("defaultTestResult", "");
        jsonWad.put("dictionaryReference", "");

    }

    /**
     * Helper to safely extract text from a JsonNode field.
     * 
     * @param node  The JsonNode to extract from.
     * @param field The name of the field.
     * @return The text value, or null if the node or field is missing/null.
     */
    private String getText(JsonNode node, String field) {
        if (node != null && node.has(field)) {
            JsonNode value = node.get(field);
            if (value != null && !value.isNull()) {
                return value.asText().trim();
            }
        }
        return null;
    }

    /**
     * Helper method to log detailed concept information for debugging
     */
    private void logConceptDetails(JsonNode concept) {
        try {
            StringBuilder details = new StringBuilder("\nConcept Details:\n");
            details.append("ID: ").append(getText(concept, "id")).append("\n");
            details.append("Display Name: ").append(getText(concept, "display_name")).append("\n");
            details.append("Concept Class: ").append(getText(concept, "concept_class")).append("\n");
            details.append("DataType: ").append(getText(concept, "datatype")).append("\n");

            // Log names
            JsonNode names = concept.get("names");
            if (names != null && names.isArray()) {
                details.append("Names:\n");
                for (JsonNode name : names) {
                    details.append("  - ").append(getText(name, "name")).append(" (").append(getText(name, "locale"))
                            .append(")").append(" [").append(getText(name, "name_type")).append("]\n");
                }
            }

            log.debug(details.toString());
        } catch (Exception e) {
            log.error("Error logging concept details: " + e.getMessage(), e);
        }
    }

    /**
     * Helper to safely extract numeric text from a JsonNode field, handling both
     * number and text nodes.
     * 
     * @param node  The JsonNode to extract from.
     * @param field The name of the field.
     * @return The numeric text value, or null if the node or field is
     *         missing/null/empty.
     */
    private String getNumericText(JsonNode node, String field) {
        if (node != null && node.has(field)) {
            JsonNode value = node.get(field);
            if (value != null && !value.isNull()
                    && (value.isNumber() || (value.isTextual() && !value.asText().isEmpty()))) {
                return value.asText().trim();
            }
        }
        return null;
    }
}