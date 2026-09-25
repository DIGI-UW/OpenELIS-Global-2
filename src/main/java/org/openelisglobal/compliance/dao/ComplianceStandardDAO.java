package org.openelisglobal.compliance.dao;

import java.util.List;
import java.util.Map;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.compliance.valueholder.ComplianceStandard;
import org.openelisglobal.compliance.valueholder.ComplianceStandard.ComplianceStandardStatus;

/**
 * DAO interface for ComplianceStandard entity operations.
 *
 * Follows OpenELIS DAO patterns with proper exception handling and
 * constitutional compliance for transaction management.
 */
public interface ComplianceStandardDAO extends BaseDAO<ComplianceStandard, String> {

    /** Retrieve standards by status. Used by getActiveComplianceStandards. */
    List<ComplianceStandard> getStandardsByStatus(ComplianceStandardStatus status) throws LIMSRuntimeException;

    /** Get paginated standards list. */
    List<ComplianceStandard> getPageOfStandards(int startingRecNo) throws LIMSRuntimeException;

    /** Check if a standard with the same natural key already exists. */
    boolean duplicateStandardExists(ComplianceStandard standard) throws LIMSRuntimeException;

    /**
     * Distinct, alphabetised list of {@code country_region} values across all
     * compliance standards. Drives the FR-1-007 type-ahead ComboBox so admins see
     * existing entries first but can still type free text.
     */
    List<String> getDistinctCountryRegions() throws LIMSRuntimeException;

    /** Search standards by multiple criteria. */
    List<ComplianceStandard> searchStandards(String name, String issuingBody, String regulationNumber,
            ComplianceStandardStatus status, String countryRegion, String sampleType) throws LIMSRuntimeException;

    /** Get compliance standard by regulation number and name. */
    ComplianceStandard getByRegulationNumberAndName(String regulationNumber, String name) throws LIMSRuntimeException;

    /** Get tests linked to a compliance standard. */
    List<Map<String, Object>> getLinkedTests(String standardId) throws LIMSRuntimeException;
}
