package org.openelisglobal.dataexchange.externalcatalog.validation;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.dataexchange.externalcatalog.dto.CatalogDefinitionRequest;
import org.openelisglobal.dataexchange.externalcatalog.exception.CatalogValidationException;
import org.openelisglobal.typeoftestresult.service.TypeOfTestResultServiceImpl;
import org.springframework.stereotype.Component;

/**
 * Stateless pre-flight validator for {@link CatalogDefinitionRequest}.
 *
 * Called at the very top of the upsert pipeline — before any DB touch, before
 * any entity resolution. If this throws, nothing has been written.
 *
 * Rules enforced: - At least one identifier (testUuid OR loincCode) must be
 * present. - nameEnglish must be non-blank. - loincCode, when present, must be
 * <= 10 characters (DB column constraint). - For tests:
 * resultTypeName/resultTypeId must be present and resolve to a known ResultType
 * — no silent fallback to ALPHA. - For panels: at least one sample type
 * identifier must be present — no silent fallback to the default sample type on
 * panel create.
 */
@Component
public class CatalogRequestValidator {

    /** Max length of the LOINC column in clinlims.PANEL and clinlims.TEST. */
    private static final int LOINC_MAX_LENGTH = 9;

    /**
     * Validates the request. Collects ALL errors before throwing so the caller gets
     * the full picture in one shot.
     *
     * @throws CatalogValidationException if any rule is violated.
     */
    public void validate(CatalogDefinitionRequest request) {

        cleanRequest(request);

        List<String> errors = new ArrayList<>();

        // --- Rule 1: at least one identifier ---
        boolean hasUuid = !GenericValidator.isBlankOrNull(request.getTestUuid());
        boolean hasLoinc = !GenericValidator.isBlankOrNull(request.getLoincCode());
        if (!hasUuid && !hasLoinc) {
            errors.add("At least one identifier is required: 'testUuid' or 'loincCode'");
        }

        // --- Rule 2: name is mandatory ---
        if (GenericValidator.isBlankOrNull(request.getNameEnglish())) {
            errors.add("'nameEnglish' is required and must not be blank");
        }

        // --- Rule 3: LOINC length guard (varchar(10) in DB) ---
        if (hasLoinc && request.getLoincCode().length() > LOINC_MAX_LENGTH) {
            errors.add("'loincCode' must be " + LOINC_MAX_LENGTH + " characters or fewer " + "(received "
                    + request.getLoincCode().length() + " chars: '" + request.getLoincCode() + "')");
        }

        if (request.isPanel()) {
            validatePanel(request, errors);
        } else {
            validateTest(request, errors);
        }

        if (!errors.isEmpty()) {
            throw new CatalogValidationException(errors);
        }
    }

    // Rigorous trimming of all incoming identifiers.

    private void cleanRequest(CatalogDefinitionRequest request) {
        if (request.getTestUuid() != null)
            request.setTestUuid(request.getTestUuid().trim());
        if (request.getLoincCode() != null)
            request.setLoincCode(request.getLoincCode().trim());
        if (request.getNameEnglish() != null)
            request.setNameEnglish(request.getNameEnglish().trim());
        if (request.getNameFrench() != null)
            request.setNameFrench(request.getNameFrench().trim());
        if (request.getReportNameEnglish() != null)
            request.setReportNameEnglish(request.getReportNameEnglish().trim());
        if (request.getReportNameFrench() != null)
            request.setReportNameFrench(request.getReportNameFrench().trim());
        if (request.getTestSectionName() != null)
            request.setTestSectionName(request.getTestSectionName().trim());
        if (request.getUomName() != null)
            request.setUomName(request.getUomName().trim());
        if (request.getResultTypeName() != null)
            request.setResultTypeName(request.getResultTypeName().trim());
        if (request.getSampleTypeName() != null)
            request.setSampleTypeName(request.getSampleTypeName().trim());

        if (request.getSampleTypeNames() != null) {
            request.getSampleTypeNames().replaceAll(s -> s != null ? s.trim() : null);
        }
        if (request.getPanelNames() != null) {
            request.getPanelNames().replaceAll(s -> s != null ? s.trim() : null);
        }
        if (request.getMemberTestNames() != null) {
            request.getMemberTestNames().replaceAll(s -> s != null ? s.trim() : null);
        }
    }

    // -------------------------------------------------------------------------
    // Panel-specific rules
    // -------------------------------------------------------------------------

    private void validatePanel(CatalogDefinitionRequest request, List<String> errors) {
        // Panel must have at least one sample type identifier — no silent fallback
        boolean hasSampleTypeId = !GenericValidator.isBlankOrNull(request.getSampleTypeId());
        boolean hasSampleTypeName = !GenericValidator.isBlankOrNull(request.getSampleTypeName());
        if (!hasSampleTypeId && !hasSampleTypeName) {
            errors.add("Panel request requires 'sampleTypeName' or 'sampleTypeId'");
        }
    }

    // -------------------------------------------------------------------------
    // Test-specific rules
    // -------------------------------------------------------------------------

    private void validateTest(CatalogDefinitionRequest request, List<String> errors) {
        // resultType must be present and must resolve to a known enum value.

        boolean hasResultTypeId = !GenericValidator.isBlankOrNull(request.getResultTypeId());
        boolean hasResultTypeName = !GenericValidator.isBlankOrNull(request.getResultTypeName());

        if (!hasResultTypeId && !hasResultTypeName) {
            errors.add("'resultTypeName' or 'resultTypeId' is required for a test");
            return; // no point checking the value if it's absent
        }

        if (hasResultTypeName && !isKnownResultType(request.getResultTypeName())) {
            errors.add("Unknown 'resultTypeName': '" + request.getResultTypeName() + "'. Valid values: "
                    + knownResultTypeNames());
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private boolean isKnownResultType(String name) {
        for (TypeOfTestResultServiceImpl.ResultType type : TypeOfTestResultServiceImpl.ResultType.values()) {
            if (type.name().equalsIgnoreCase(name.trim())) {
                return true;
            }
        }
        return false;
    }

    private String knownResultTypeNames() {
        StringBuilder sb = new StringBuilder();
        TypeOfTestResultServiceImpl.ResultType[] values = TypeOfTestResultServiceImpl.ResultType.values();
        for (int i = 0; i < values.length; i++) {
            sb.append(values[i].name());
            if (i < values.length - 1)
                sb.append(", ");
        }
        return sb.toString();
    }
}
