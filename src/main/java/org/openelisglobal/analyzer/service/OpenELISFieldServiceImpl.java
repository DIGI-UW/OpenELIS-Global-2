package org.openelisglobal.analyzer.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.openelisglobal.analyzer.form.OpenELISFieldForm;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.localization.service.LocalizationService;
import org.openelisglobal.localization.service.LocalizationServiceImpl;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.test.dao.TestDAO;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for creating OpenELIS fields inline from the analyzer
 * mapping interface.
 * 
 * Supports creation of TEST, PANEL, RESULT, ORDER, SAMPLE, QC, METADATA, UNIT
 * entities. Currently implements TEST entity type; other types to be added
 * incrementally.
 * 
 * Status (2025-01-27): Only TEST entity type is fully implemented per FR-019.
 * Other 7 entity types (PANEL, RESULT, ORDER, SAMPLE, QC, METADATA, UNIT) throw
 * "not yet implemented" exceptions. Frontend InlineFieldCreationModal has basic
 * validation but does not adapt form fields based on selected entity type.
 * TODO: Implement remaining 7 entity types with full validation per FR-019
 * specification (spec.md lines 570-621).
 */
@Service
@Transactional
public class OpenELISFieldServiceImpl implements OpenELISFieldService {

    @Autowired
    private TestService testService;

    @Autowired
    private TestDAO testDAO;

    @Autowired
    private LocalizationService localizationService;

    @Override
    public String createField(OpenELISFieldForm form) throws LIMSRuntimeException {
        if (!validateFieldUniqueness(form)) {
            throw new LIMSRuntimeException("Field with the same name, code, or LOINC already exists");
        }

        switch (form.getEntityType()) {
        case TEST:
            return createTestField(form);
        case PANEL:
        case RESULT:
        case ORDER:
        case SAMPLE:
        case QC:
        case METADATA:
        case UNIT:
            throw new LIMSRuntimeException("Entity type " + form.getEntityType() + " not yet implemented");
        default:
            throw new LIMSRuntimeException("Unknown entity type: " + form.getEntityType());
        }
    }

    @Override
    public boolean validateFieldUniqueness(OpenELISFieldForm form) {
        if (form == null || form.getEntityType() == null) {
            return false;
        }
        switch (form.getEntityType()) {
        case TEST:
            return validateTestUniqueness(form);
        case PANEL:
        case RESULT:
        case ORDER:
        case SAMPLE:
        case QC:
        case METADATA:
        case UNIT:
            // Only TEST entity validated; other types return true (FR-019 Phase 2)
            return true;
        default:
            return false;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getFieldById(String fieldId, OpenELISFieldForm.EntityType entityType) {
        try {
            switch (entityType) {
            case TEST:
                // Try to parse ID as integer first (Test IDs are numeric)
                try {
                    Integer.parseInt(fieldId);
                } catch (NumberFormatException e) {
                    // Invalid ID format - return null (not found)
                    return null;
                }
                java.util.Optional<Test> testOpt = testDAO.get(fieldId);
                if (testOpt.isPresent()) {
                    Test test = testOpt.get();
                    Map<String, Object> result = new HashMap<>();
                    result.put("id", test.getId());
                    result.put("name", test.getDescription());
                    result.put("code", test.getLocalCode());
                    result.put("loincCode", test.getLoinc());
                    result.put("entityType", "TEST");
                    return result;
                }
                return null;
            case PANEL:
            case RESULT:
            case ORDER:
            case SAMPLE:
            case QC:
            case METADATA:
            case UNIT:
                // Only TEST entity retrieval implemented; others return null (FR-019 Phase 2)
                return null;
            default:
                return null;
            }
        } catch (NumberFormatException e) {
            // Invalid ID format - return null (not found)
            return null;
        } catch (Exception e) {
            // If any other error occurs, return null (field not found)
            // Don't log here to avoid noise - let controller handle logging if needed
            return null;
        }
    }

    /**
     * Creates a new Test entity from the form data.
     */
    private String createTestField(OpenELISFieldForm form) throws LIMSRuntimeException {
        try {
            Localization nameLocalization = LocalizationServiceImpl.createNewLocalization(form.getFieldName(), // English
                    form.getFieldName(), // French (uses English value as default)
                    LocalizationServiceImpl.LocalizationType.TEST_NAME);
            String nameLocalizationId = localizationService.insert(nameLocalization);
            nameLocalization.setId(nameLocalizationId);

            Localization reportingNameLocalization = LocalizationServiceImpl.createNewLocalization(form.getFieldName(), // English
                    form.getFieldName(), // French
                    LocalizationServiceImpl.LocalizationType.REPORTING_TEST_NAME);
            String reportingNameLocalizationId = localizationService.insert(reportingNameLocalization);
            reportingNameLocalization.setId(reportingNameLocalizationId);

            Test test = new Test();
            test.setDescription(form.getFieldName());
            test.setLocalCode(form.getTestCode());
            test.setLoinc(form.getLoincCode());
            test.setLocalizedTestName(nameLocalization);
            test.setLocalizedReportingName(reportingNameLocalization);
            test.setGuid(String.valueOf(UUID.randomUUID()));

            test.setIsActive("Y");
            test.setOrderable(true);

            // Default system user; SecurityContext integration deferred
            test.setSysUserId("1");

            String testId = testService.insert(test);

            LogEvent.logInfo(this.getClass().getSimpleName(), "createTestField",
                    "Created new test field: " + testId + " (" + form.getFieldName() + ")");

            return testId;
        } catch (Exception e) {
            LogEvent.logError("Error creating test field: " + e.getMessage(), e);
            throw new LIMSRuntimeException("Error creating test field: " + e.getMessage(), e);
        }
    }

    /**
     * Validates that a Test field is unique (by description, localCode, and LOINC).
     */
    private boolean validateTestUniqueness(OpenELISFieldForm form) {
        Test existingByDescription = testService.getTestByDescription(form.getFieldName());
        if (existingByDescription != null) {
            return false;
        }

        if (form.getTestCode() != null && !form.getTestCode().trim().isEmpty()) {
            List<Test> testsByName = testDAO.getTestsByName(form.getFieldName());
            for (Test test : testsByName) {
                if (form.getTestCode().equals(test.getLocalCode())) {
                    return false;
                }
            }
        }

        if (form.getLoincCode() != null && !form.getLoincCode().trim().isEmpty()) {
            List<Test> testsByLoinc = testService.getTestsByLoincCode(form.getLoincCode());
            if (!testsByLoinc.isEmpty()) {
                return false;
            }
        }

        return true;
    }
}
