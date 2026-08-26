package org.openelisglobal.compliance.service;

import java.util.List;
import java.util.Map;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.compliance.valueholder.ComplianceStandard;
import org.openelisglobal.compliance.valueholder.ComplianceStandard.ComplianceStandardStatus;
import org.springframework.security.access.prepost.PreAuthorize;

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
    @PreAuthorize("hasAuthority('PRIV_SAMPLE_TYPE_VIEW')")
    List<String> getDistinctCountryRegions();

    /**
     * Get compliance standard by regulation number and name. Used by the seed
     * loader to resolve a standard already present in the database.
     */
    @PreAuthorize("hasAuthority('PRIV_SAMPLE_TYPE_VIEW')")
    ComplianceStandard getByRegulationNumberAndName(String regulationNumber, String name);

    /** Get paginated list of standards. */
    @PreAuthorize("hasAuthority('PRIV_SAMPLE_TYPE_VIEW')")
    List<ComplianceStandard> getPageOfStandards(int startingRecNo);

    /** Search standards by multiple criteria. */
    @PreAuthorize("hasAuthority('PRIV_SAMPLE_TYPE_VIEW')")
    List<ComplianceStandard> searchStandards(String name, String issuingBody, String regulationNumber,
            ComplianceStandardStatus status, String countryRegion, String sampleType);

    /** Archive a standard (set status to ARCHIVED). */
    @PreAuthorize("hasAuthority('PRIV_SYSTEM_CONFIGURE')")
    void archive(String standardId);

    /** Validate standard before save (business rules). */
    @PreAuthorize("hasAuthority('PRIV_SYSTEM_CONFIGURE')")
    void validateStandard(ComplianceStandard standard);

    /** Get all active compliance standards. */
    @PreAuthorize("hasAuthority('PRIV_SAMPLE_TYPE_VIEW')")
    List<ComplianceStandard> getActiveComplianceStandards();

    /** Get tests linked to a compliance standard. */
    @PreAuthorize("hasAuthority('PRIV_SAMPLE_TYPE_VIEW')")
    List<Map<String, Object>> getLinkedTests(String standardId);

    /**
     * FR-7-004: deep copy of a standard. Duplicates the standard and all of its
     * parameter groups + thresholds + threshold value mappings as a new
     * {@code DRAFT}-status record with {@code version} suffixed " - Copy" so the
     * natural-key uniqueness on (name, regulationNumber, version) doesn't collide.
     * {@code isPreSeeded} on the copy is always false.
     */
    @PreAuthorize("hasAuthority('PRIV_SYSTEM_CONFIGURE')")
    ComplianceStandard copyStandard(ComplianceStandard original, String userId);
}
