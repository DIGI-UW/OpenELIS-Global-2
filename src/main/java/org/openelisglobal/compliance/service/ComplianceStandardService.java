package org.openelisglobal.compliance.service;

import java.util.List;
import java.util.Map;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.compliance.valueholder.ComplianceStandard;
import org.openelisglobal.compliance.valueholder.ComplianceStandard.ComplianceStandardStatus;

/**
 * Service interface for ComplianceStandard operations.
 *
 * Constitutional compliance: extends BaseObjectService for standardized
 * operations, declares transaction boundaries at service level, and provides
 * domain-specific business logic for compliance standards management.
 */
public interface ComplianceStandardService extends BaseObjectService<ComplianceStandard, String> {

    /**
     * Distinct, alphabetised list of country/region values used by existing
     * standards. Drives the FR-1-007 ComboBox type-ahead.
     */
    List<String> getDistinctCountryRegions();

    /**
     * Get compliance standard by regulation number and name. Used by the seed
     * loader to resolve a standard already present in the database.
     */
    ComplianceStandard getByRegulationNumberAndName(String regulationNumber, String name);

    /** Get paginated list of standards. */
    List<ComplianceStandard> getPageOfStandards(int startingRecNo);

    /** Search standards by multiple criteria. */
    List<ComplianceStandard> searchStandards(String name, String issuingBody, String regulationNumber,
            ComplianceStandardStatus status, String countryRegion, String sampleType);

    /** Archive a standard (set status to ARCHIVED). */
    void archive(String standardId);

    /** Validate standard before save (business rules). */
    void validateStandard(ComplianceStandard standard);

    /** Get all active compliance standards. */
    List<ComplianceStandard> getActiveComplianceStandards();

    /** Get tests linked to a compliance standard. */
    List<Map<String, Object>> getLinkedTests(String standardId);

    /**
     * FR-7-004: deep copy of a standard. Duplicates the standard and all of its
     * parameter groups + thresholds + threshold value mappings as a new
     * {@code DRAFT}-status record with {@code version} suffixed " - Copy" so the
     * natural-key uniqueness on (name, regulationNumber, version) doesn't collide.
     * {@code isPreSeeded} on the copy is always false.
     */
    ComplianceStandard copyStandard(ComplianceStandard original, String userId);
}
