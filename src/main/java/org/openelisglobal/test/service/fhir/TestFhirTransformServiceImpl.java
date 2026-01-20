package org.openelisglobal.test.service.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.BasicAuthInterceptor;
import org.apache.commons.validator.GenericValidator;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.ObservationDefinition;
import org.hl7.fhir.r4.model.Range;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.SimpleQuantity;
import org.hl7.fhir.r4.model.ValueSet;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemComponent;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemType;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemAnswerOptionComponent;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.resultlimit.service.ResultLimitService;
import org.openelisglobal.resultlimits.valueholder.ResultLimit;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.testresult.valueholder.TestResult;
import org.openelisglobal.typeoftestresult.service.TypeOfTestResultServiceImpl.ResultType;
import org.openelisglobal.unitofmeasure.valueholder.UnitOfMeasure;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Implementation of TestFhirTransformService.
 * Transforms OpenELIS Test entities to FHIR R4 ObservationDefinition resources
 * and syncs them to OpenMRS.
 * 
 * Phase 1-5: Supports TEXT, NUMERIC, and CODED tests
 * - TEXT: permittedDataType = STRING
 * - NUMERIC: permittedDataType = Quantity with quantitativeDetails and
 * qualifiedInterval
 * - CODED: permittedDataType = CodeableConcept with validCodedValueSet
 */
@Service
public class TestFhirTransformServiceImpl implements TestFhirTransformService {

    @Autowired
    private FhirConfig fhirConfig;

    @Autowired
    private FhirContext fhirContext;

    @Autowired
    private TestService testService;

    @Autowired
    private ResultLimitService resultLimitService;

    @Autowired
    private TestResultService testResultService;

    @Autowired
    private DictionaryService dictionaryService;

    @Value("${org.openelisglobal.fhir.test.sync.enabled:false}")
    private boolean syncEnabled;

    @Override
    public ObservationDefinition transformTestToObservationDefinition(Test test) {
        if (test == null) {
            throw new IllegalArgumentException("Test cannot be null");
        }

        ObservationDefinition obsDef = new ObservationDefinition();

        // 1. Set ID (use test.guid as stable identifier)
        if (!GenericValidator.isBlankOrNull(test.getGuid())) {
            obsDef.setId(test.getGuid());
        } else {
            LogEvent.logWarn(this.getClass().getSimpleName(), "transformTestToObservationDefinition",
                    "Test has no GUID, using ID: " + test.getId());
            obsDef.setId(test.getId());
        }

        // 2. Set code (test name + LOINC if available)
        CodeableConcept code = new CodeableConcept();

        // Get test name (prefer localized, fallback to description)
        String testName = getTestName(test);
        code.setText(testName);

        // Add local coding (using guid or id)
        Coding localCoding = new Coding();
        String codeValue = !GenericValidator.isBlankOrNull(test.getGuid()) ? test.getGuid() : test.getId();
        localCoding.setCode(codeValue);
        localCoding.setDisplay(testName);
        code.addCoding(localCoding);

        // Add LOINC coding if available
        if (!GenericValidator.isBlankOrNull(test.getLoinc())) {
            Coding loincCoding = new Coding();
            loincCoding.setSystem("http://loinc.org");
            loincCoding.setCode(test.getLoinc());
            loincCoding.setDisplay(testName); // Use test name as display
            code.addCoding(loincCoding);

            LogEvent.logDebug(this.getClass().getSimpleName(), "transformTestToObservationDefinition",
                    "Added LOINC coding: " + test.getLoinc());
        }

        obsDef.setCode(code);

        // 3. Set permittedDataType based on test result type
        // IMPORTANT: FHIR enum values are case-sensitive!
        String resultType = determineResultType(test);

        if (ResultType.NUMERIC.matches(resultType)) {
            // NUMERIC test - use Quantity
            obsDef.addPermittedDataType(ObservationDefinition.ObservationDataType.QUANTITY);

            // Add quantitative details (units, precision)
            addQuantitativeDetails(obsDef, test);

            // Add qualified intervals (reference ranges)
            addQualifiedIntervals(obsDef, test);

            LogEvent.logInfo(this.getClass().getSimpleName(), "transformTestToObservationDefinition",
                    "Test " + testName + " is NUMERIC, added quantitativeDetails and qualifiedInterval");

        } else if (ResultType.isDictionaryVariant(resultType)) {
            // CODED test - use CodeableConcept
            obsDef.addPermittedDataType(ObservationDefinition.ObservationDataType.CODEABLECONCEPT);
            // Note: Answers are now handled via Questionnaire sync, not
            // ObservationDefinition
            LogEvent.logInfo(this.getClass().getSimpleName(), "transformTestToObservationDefinition",
                    "Test " + testName + " is CODED, answers will be synced via Questionnaire");
        } else {
            // TEXT test - use string
            obsDef.addPermittedDataType(ObservationDefinition.ObservationDataType.STRING);
        }

        LogEvent.logInfo(this.getClass().getSimpleName(), "transformTestToObservationDefinition",
                "Transformed test: " + testName + " (guid=" + test.getGuid() + ", loinc=" + test.getLoinc()
                        + ", permittedDataType=" +
                        (ResultType.NUMERIC.matches(resultType) ? "Quantity" : "string") + ")");

        return obsDef;
    }

    /**
     * Add quantitativeDetails for numeric tests.
     * Maps unitOfMeasure to FHIR unit and sets decimal precision.
     */
    private void addQuantitativeDetails(ObservationDefinition obsDef, Test test) {
        ObservationDefinition.ObservationDefinitionQuantitativeDetailsComponent quantDetails = new ObservationDefinition.ObservationDefinitionQuantitativeDetailsComponent();

        // Get unit of measure
        UnitOfMeasure uom = test.getUnitOfMeasure();
        if (uom != null && !GenericValidator.isBlankOrNull(uom.getName())) {
            CodeableConcept unitConcept = new CodeableConcept();
            unitConcept.setText(uom.getName());
            quantDetails.setUnit(unitConcept);

            LogEvent.logDebug(this.getClass().getSimpleName(), "addQuantitativeDetails",
                    "Added unit: " + uom.getName());
        }

        // Set decimal precision (hardcode to 1 for Phase 4)
        quantDetails.setDecimalPrecision(1);

        obsDef.setQuantitativeDetails(quantDetails);
    }

    /**
     * Add qualifiedInterval (reference ranges) for numeric tests.
     * Maps OpenELIS result_limits to FHIR qualifiedInterval with extensions.
     */
    private void addQualifiedIntervals(ObservationDefinition obsDef, Test test) {
        // Get result limits for this test
        List<ResultLimit> resultLimits = resultLimitService.getAllResultLimitsForTest(test.getId());

        if (resultLimits == null || resultLimits.isEmpty()) {
            LogEvent.logDebug(this.getClass().getSimpleName(), "addQualifiedIntervals",
                    "No result limits defined for test " + test.getId());
            return; // No ranges defined
        }

        // Use first result limit (most tests have one)
        ResultLimit limit = resultLimits.get(0);

        int intervalCount = 0;

        // Add NORMAL range if defined
        if (isValidRange(limit.getLowNormal(), limit.getHighNormal())) {
            obsDef.addQualifiedInterval(createQualifiedInterval(
                    limit.getLowNormal(), limit.getHighNormal(), "normal"));
            intervalCount++;
            LogEvent.logDebug(this.getClass().getSimpleName(), "addQualifiedIntervals",
                    "Added NORMAL range: " + limit.getLowNormal() + " - " + limit.getHighNormal());
        }

        // Add TREATMENT range if defined
        if (isValidRange(limit.getLowCritical(), limit.getHighCritical())) {
            obsDef.addQualifiedInterval(createQualifiedInterval(
                    limit.getLowCritical(), limit.getHighCritical(), "treatment"));
            intervalCount++;
            LogEvent.logDebug(this.getClass().getSimpleName(), "addQualifiedIntervals",
                    "Added TREATMENT range: " + limit.getLowCritical() + " - " + limit.getHighCritical());
        }

        // Add ABSOLUTE range if defined
        if (isValidRange(limit.getLowValid(), limit.getHighValid())) {
            obsDef.addQualifiedInterval(createQualifiedInterval(
                    limit.getLowValid(), limit.getHighValid(), "absolute"));
            intervalCount++;
            LogEvent.logDebug(this.getClass().getSimpleName(), "addQualifiedIntervals",
                    "Added ABSOLUTE range: " + limit.getLowValid() + " - " + limit.getHighValid());
        }

        LogEvent.logInfo(this.getClass().getSimpleName(), "addQualifiedIntervals",
                "Added " + intervalCount + " qualified intervals for test " + test.getId());
    }

    /**
     * Check if a range is valid (not null and not infinite).
     */
    private boolean isValidRange(Double low, Double high) {
        return low != null && high != null &&
                !Double.isInfinite(low) && !Double.isInfinite(high);
    }

    /**
     * Create a qualifiedInterval component with range and extension.
     */
    private ObservationDefinition.ObservationDefinitionQualifiedIntervalComponent createQualifiedInterval(
            Double low, Double high, String rangeType) {

        ObservationDefinition.ObservationDefinitionQualifiedIntervalComponent interval = new ObservationDefinition.ObservationDefinitionQualifiedIntervalComponent();

        // Set range
        Range range = new Range();
        range.setLow(new SimpleQuantity().setValue(low));
        range.setHigh(new SimpleQuantity().setValue(high));
        interval.setRange(range);

        // Add extension for range type
        Extension ext = new Extension();
        ext.setUrl("http://fhir.openmrs.org/ext/obs/reference-range");
        ext.setValue(new CodeType(rangeType));
        interval.addExtension(ext);

        return interval;
    }

    /**
     * Get dictionary entries for a coded/dictionary test.
     * Extracts dictionary IDs from test results where tst_rslt_type='D'.
     */
    private List<Dictionary> getDictionaryEntriesForTest(Test test) {
        List<Dictionary> dictEntries = new ArrayList<>();

        try {
            // Get all test results for this test
            List<TestResult> testResults = testResultService.getActiveTestResultsByTest(test.getId());

            for (TestResult tr : testResults) {
                // Check if this is a dictionary result (tst_rslt_type='D')
                if ("D".equals(tr.getTestResultType()) && !GenericValidator.isBlankOrNull(tr.getValue())) {
                    // Value contains dictionary ID
                    Dictionary dict = dictionaryService.get(tr.getValue());
                    if (dict != null && "Y".equals(dict.getIsActive())) {
                        dictEntries.add(dict);
                    }
                }
            }

            LogEvent.logDebug(this.getClass().getSimpleName(), "getDictionaryEntriesForTest",
                    "Found " + dictEntries.size() + " dictionary entries for test " + test.getId());

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "getDictionaryEntriesForTest",
                    "Error getting dictionary entries for test " + test.getId() + ": " + e.getMessage());
        }

        return dictEntries;
    }

    /**
     * Generate a stable GUID for a dictionary entry.
     * Uses UUID v5 (name-based) with dictionary ID as the name.
     */
    private String generateStableGuidForDictionary(Dictionary dict) {
        // Use dictionary ID to generate stable UUID
        // Format: dict-{id} hashed to UUID v5
        String namespace = "openelis-dictionary";
        String name = "dict-" + dict.getId();

        // Simple UUID generation based on dictionary ID
        // This ensures same dictionary entry always gets same UUID
        return UUID.nameUUIDFromBytes((namespace + ":" + name).getBytes()).toString();
    }

    /**
     * Transform Coded Test to FHIR Questionnaire.
     * This enables diff-based answer updates and cleaner concept management in
     * OpenMRS.
     */
    private Questionnaire transformTestToQuestionnaire(Test test, List<Dictionary> dictEntries) {
        Questionnaire q = new Questionnaire();

        // 1. Set ID (use test GUID)
        q.setId(test.getGuid());

        // 2. Set title (test name)
        String testName = getTestName(test);
        q.setTitle(testName);
        q.setStatus(Enumerations.PublicationStatus.ACTIVE);

        // 3. Create single item for the test
        QuestionnaireItemComponent item = new QuestionnaireItemComponent();
        item.setLinkId(test.getGuid());
        item.setText(testName);
        item.setType(QuestionnaireItemType.CHOICE);

        // 4. Add answers
        for (Dictionary entry : dictEntries) {
            QuestionnaireItemAnswerOptionComponent option = new QuestionnaireItemAnswerOptionComponent();
            Coding coding = new Coding();
            coding.setSystem("urn:uuid");
            coding.setCode(generateStableGuidForDictionary(entry));
            coding.setDisplay(entry.getDictEntry());
            option.setValue(coding);
            item.addAnswerOption(option);

            LogEvent.logDebug(this.getClass().getSimpleName(), "transformTestToQuestionnaire",
                    "Added answer: " + entry.getDictEntry());
        }

        q.addItem(item);

        LogEvent.logInfo(this.getClass().getSimpleName(), "transformTestToQuestionnaire",
                "Transformed coded test " + testName + " to Questionnaire with " + dictEntries.size() + " answers");

        return q;
    }

    /**
     * Sync Questionnaire to OpenMRS via PUT upsert.
     */
    private void syncQuestionnaireToServer(Questionnaire questionnaire, String serverUrl) {
        try {
            IGenericClient fhirClient = fhirContext.newRestfulGenericClient(serverUrl);

            // Add authentication if configured
            if (!GenericValidator.isBlankOrNull(fhirConfig.getUsername())) {
                IClientInterceptor authInterceptor = new BasicAuthInterceptor(fhirConfig.getUsername(),
                        fhirConfig.getPassword());
                fhirClient.registerInterceptor(authInterceptor);
            }

            // PUT upsert
            MethodOutcome outcome = fhirClient.update()
                    .resource(questionnaire)
                    .execute();

            if (outcome.getCreated() != null && outcome.getCreated()) {
                LogEvent.logInfo(this.getClass().getSimpleName(), "syncQuestionnaireToServer",
                        "✅ Created Questionnaire in OpenMRS: " + serverUrl + " (id=" + questionnaire.getId() + ")");
            } else {
                LogEvent.logInfo(this.getClass().getSimpleName(), "syncQuestionnaireToServer",
                        "✅ Updated Questionnaire in OpenMRS: " + serverUrl + " (id=" + questionnaire.getId() + ")");
            }

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "syncQuestionnaireToServer",
                    "Error syncing Questionnaire: " + e.getMessage());
            throw new RuntimeException("Failed to sync Questionnaire", e);
        }
    }

    @Override
    @Async
    public void syncTestToFhir(Test test, boolean isUpdate) {
        if (!syncEnabled) {
            LogEvent.logDebug(this.getClass().getSimpleName(), "syncTestToFhir",
                    "FHIR test sync is disabled, skipping");
            return;
        }

        if (test == null) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "syncTestToFhir",
                    "Cannot sync null test");
            return;
        }

        try {
            // Determine result type
            String resultType = determineResultType(test);

            // Get remote FHIR servers (OpenMRS)
            String[] remotePaths = fhirConfig.getRemoteStorePaths();
            if (remotePaths == null || remotePaths.length == 0) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "syncTestToFhir",
                        "No remote FHIR servers configured, skipping test sync");
                return;
            }

            // BRANCH LOGIC: Coded vs Numeric/Text
            if (ResultType.isDictionaryVariant(resultType)) {
                // CODED TEST -> Sync as Questionnaire
                List<Dictionary> dictEntries = getDictionaryEntriesForTest(test);
                Questionnaire questionnaire = transformTestToQuestionnaire(test, dictEntries);

                for (String remotePath : remotePaths) {
                    if (!GenericValidator.isBlankOrNull(remotePath)) {
                        syncQuestionnaireToServer(questionnaire, remotePath);
                    }
                }

            } else {
                // NUMERIC/TEXT TEST -> Sync as ObservationDefinition
                ObservationDefinition obsDef = transformTestToObservationDefinition(test);

                for (String remotePath : remotePaths) {
                    if (!GenericValidator.isBlankOrNull(remotePath)) {
                        syncToServer(obsDef, remotePath, isUpdate);
                    }
                }
            }

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "syncTestToFhir",
                    "Error syncing test to FHIR: " + e.getMessage());
            LogEvent.logError(this.getClass().getSimpleName(), "syncTestToFhir",
                    "Full stack trace: " + e.toString());
            // Don't throw - keep async and non-blocking
        }
    }

    /**
     * Sync ObservationDefinition to a specific FHIR server.
     * 
     * IMPORTANT: Uses PUT upsert for BOTH create and update operations.
     * This ensures OpenELIS GUID becomes the OpenMRS Concept.uuid (no UUID
     * mismatch).
     * 
     * From OpenMRS FHIR2 docs:
     * "PUT /ObservationDefinition/{uuid} supports upsert semantics for test
     * definitions.
     * When creating via PUT, the translator sets the OpenMRS Concept.uuid to {uuid}
     * from the URL/body BEFORE the concept is persisted."
     */
    private void syncToServer(ObservationDefinition obsDef, String serverUrl, boolean isUpdate) {
        try {
            IGenericClient fhirClient = fhirContext.newRestfulGenericClient(serverUrl);

            // Add authentication if configured
            if (!GenericValidator.isBlankOrNull(fhirConfig.getUsername())) {
                IClientInterceptor authInterceptor = new BasicAuthInterceptor(fhirConfig.getUsername(),
                        fhirConfig.getPassword());
                fhirClient.registerInterceptor(authInterceptor);
            }

            // TEXT/NUMERIC TEST - use PUT upsert
            // ALWAYS use PUT upsert (for both create and update)
            // This preserves OpenELIS GUID as OpenMRS UUID
            try {
                ca.uhn.fhir.rest.api.MethodOutcome outcome = fhirClient.update()
                        .resource(obsDef)
                        .execute();

                // Check if this was a create or update
                if (outcome.getCreated() != null && outcome.getCreated()) {
                    LogEvent.logInfo(this.getClass().getSimpleName(), "syncToServer",
                            "✅ Created ObservationDefinition in OpenMRS via PUT upsert: " + serverUrl +
                                    " (OpenELIS GUID=" + obsDef.getId() + " preserved as OpenMRS UUID)");
                } else {
                    LogEvent.logInfo(this.getClass().getSimpleName(), "syncToServer",
                            "✅ Updated ObservationDefinition in OpenMRS: " + serverUrl + " (id=" + obsDef.getId()
                                    + ")");
                }

                // Log the response ID if available
                if (outcome.getId() != null) {
                    LogEvent.logInfo(this.getClass().getSimpleName(), "syncToServer",
                            "OpenMRS confirmed UUID: " + outcome.getId().getIdPart());
                }

            } catch (ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException e) {
                LogEvent.logError(this.getClass().getSimpleName(), "syncToServer",
                        "❌ FHIR Server Error - HTTP " + e.getStatusCode() + ": " + e.getMessage());
                LogEvent.logError(this.getClass().getSimpleName(), "syncToServer",
                        "Response body: " + e.getResponseBody());
                LogEvent.logError(this.getClass().getSimpleName(), "syncToServer",
                        "OperationOutcome: "
                                + (e.getOperationOutcome() != null ? e.getOperationOutcome().toString() : "null"));
                throw e;
            } catch (Exception e) {
                LogEvent.logError(this.getClass().getSimpleName(), "syncToServer",
                        "Error syncing ObservationDefinition: " + e.getMessage());
                LogEvent.logError(this.getClass().getSimpleName(), "syncToServer",
                        "Full error: " + e.toString());
                throw e;
            }

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "syncToServer",
                    "Error syncing to FHIR server " + serverUrl + ": " + e.getMessage());
            // Don't throw - continue with other servers
        }
    }

    /**
     * Get the display name for a test.
     * Prefers localized name, falls back to description.
     */
    private String getTestName(Test test) {
        // Try localized test name first
        Localization localizedName = test.getLocalizedTestName();
        if (localizedName != null) {
            String localized = localizedName.getLocalizedValue();
            if (!GenericValidator.isBlankOrNull(localized)) {
                return localized.trim();
            }
        }

        // Fallback to description
        if (!GenericValidator.isBlankOrNull(test.getDescription())) {
            return test.getDescription().trim();
        }

        // Last resort: use ID
        return "Test " + test.getId();
    }

    /**
     * Determine the result type of a test.
     * Uses TestService.getResultType() which checks TestResult entries.
     */
    private String determineResultType(Test test) {
        try {
            return testService.getResultType(test);
        } catch (Exception e) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "determineResultType",
                    "Error determining result type for test " + test.getId() + ", defaulting to ALPHA: "
                            + e.getMessage());
            // Default to ALPHA (text) if we can't determine
            return ResultType.ALPHA.getCharacterValue();
        }
    }
}
