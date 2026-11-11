package org.openelisglobal.compliance.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.exception.LIMSDuplicateRecordException;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.compliance.dao.ComplianceStandardDAO;
import org.openelisglobal.compliance.dao.ComplianceThresholdDAO;
import org.openelisglobal.compliance.dao.ParameterGroupDAO;
import org.openelisglobal.compliance.valueholder.ComplianceStandard;
import org.openelisglobal.compliance.valueholder.ComplianceStandard.ComplianceStandardStatus;
import org.openelisglobal.compliance.valueholder.ComplianceThreshold;
import org.openelisglobal.compliance.valueholder.ComplianceThresholdValueMap;
import org.openelisglobal.compliance.valueholder.ParameterGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for ComplianceStandard operations.
 *
 * Constitutional compliance: extends AuditableBaseObjectServiceImpl,
 * declares @Transactional boundaries at service level (not controller),
 * delegates to the DAO for data access, and enforces business-rule validation
 * (BR-002 / BR-004 / FR-1-007a / natural-key duplicates) before persisting.
 */
@Service
public class ComplianceStandardServiceImpl extends AuditableBaseObjectServiceImpl<ComplianceStandard, String>
        implements ComplianceStandardService {

    @Autowired
    protected ComplianceStandardDAO baseObjectDAO;

    // Used for BR-002 — checks whether any threshold rows reference this
    // standard before allowing delete. Direct DAO injection (not service) so
    // we don't introduce a service-layer cycle.
    @Autowired
    private ComplianceThresholdDAO complianceThresholdDAO;

    // FR-7-004 deep copy walks parameter groups + thresholds for the source
    // standard via these DAOs. Service-level call would re-introduce the
    // cycle ParameterGroupService → ComplianceThresholdService → … so use
    // DAOs directly.
    @Autowired
    private ParameterGroupDAO parameterGroupDAO;

    ComplianceStandardServiceImpl() {
        super(ComplianceStandard.class);
    }

    @Override
    protected ComplianceStandardDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceStandard> getPageOfStandards(int startingRecNo) {
        return baseObjectDAO.getPageOfStandards(startingRecNo);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getDistinctCountryRegions() {
        return baseObjectDAO.getDistinctCountryRegions();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceStandard> searchStandards(String name, String issuingBody, String regulationNumber,
            ComplianceStandardStatus status, String countryRegion, String sampleType) {
        return baseObjectDAO.searchStandards(name, issuingBody, regulationNumber, status, countryRegion, sampleType);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceStandard> getActiveComplianceStandards() {
        return baseObjectDAO.getStandardsByStatus(ComplianceStandardStatus.ACTIVE);
    }

    @Override
    @Transactional(readOnly = true)
    public ComplianceStandard getByRegulationNumberAndName(String regulationNumber, String name) {
        return baseObjectDAO.getByRegulationNumberAndName(regulationNumber, name);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getLinkedTests(String standardId) {
        return baseObjectDAO.getLinkedTests(standardId);
    }

    @Override
    @Transactional
    public void archive(String standardId) {
        ComplianceStandard standard = get(standardId);
        if (standard != null) {
            standard.setStatus(ComplianceStandardStatus.ARCHIVED);
            update(standard);
        }
    }

    @Override
    public void validateStandard(ComplianceStandard standard) {
        if (standard == null) {
            throw new IllegalArgumentException(ComplianceMessages.standardNotNull());
        }

        if (standard.getName() == null || standard.getName().trim().isEmpty()) {
            throw new IllegalArgumentException(ComplianceMessages.standardNameRequired());
        }

        if (standard.getVersion() == null || standard.getVersion().trim().isEmpty()) {
            throw new IllegalArgumentException(ComplianceMessages.standardVersionRequired());
        }

        if (standard.getIssuingBody() == null || standard.getIssuingBody().trim().isEmpty()) {
            throw new IllegalArgumentException(ComplianceMessages.standardIssuingBodyRequired());
        }

        if (standard.getRegulationNumber() == null || standard.getRegulationNumber().trim().isEmpty()) {
            throw new IllegalArgumentException(ComplianceMessages.standardRegulationNumberRequired());
        }

        if (standard.getCountryRegion() == null || standard.getCountryRegion().trim().isEmpty()) {
            throw new IllegalArgumentException(ComplianceMessages.standardCountryRegionRequired());
        }

        if (standard.getStatus() == null) {
            throw new IllegalArgumentException(ComplianceMessages.standardStatusRequired());
        }

        // BR-004: SUPERSEDED status must reference a replacement standard. The
        // generic PUT path used to accept status=SUPERSEDED with a null
        // supersededByStandard, leaving the standard in a state where the UI
        // banner (FR-7-002) had nothing to link to.
        if (standard.getStatus() == ComplianceStandardStatus.SUPERSEDED
                && (standard.getSupersededByStandard() == null || standard.getSupersededByStandard().getId() == null)) {
            throw new IllegalArgumentException(ComplianceMessages.standardSupersededRequiresReplacement());
        }

        // FR-1-007a: at least one sample type before the standard goes ACTIVE.
        // DRAFT is allowed to be empty so admins can iterate; SUPERSEDED /
        // ARCHIVED preserve their historical sample types and don't re-validate.
        if (standard.getStatus() == ComplianceStandardStatus.ACTIVE
                && (standard.getSampleTypes() == null || standard.getSampleTypes().isEmpty())) {
            throw new IllegalArgumentException(ComplianceMessages.standardSampleTypesRequiredForActive());
        }

        if (baseObjectDAO.duplicateStandardExists(standard)) {
            throw new LIMSDuplicateRecordException(ComplianceMessages.standardDuplicate());
        }
    }

    /**
     * FR-7-004: deep copy of a standard.
     *
     * <p>
     * Walks the parameter groups + thresholds + threshold value mappings explicitly
     * so admins can iterate on a draft revision without disturbing the live
     * standard. Three behaviour pins from the spec:
     * <ul>
     * <li>Status of the copy is forced to {@code DRAFT} regardless of source —
     * copies are never live.</li>
     * <li>Version is suffixed with {@code " - Copy"} (FRS literal text) so the
     * natural-key uniqueness on (name, regulationNumber, version) doesn't collide
     * with the source.</li>
     * <li>{@code isPreSeeded} on the copy is always false — copies are
     * admin-created and freely deletable.</li>
     * </ul>
     */
    @Override
    @Transactional
    public ComplianceStandard copyStandard(ComplianceStandard original, String userId) {
        if (original == null) {
            throw new IllegalArgumentException(ComplianceMessages.standardNotNull());
        }

        ComplianceStandard copy = new ComplianceStandard();
        copy.setName(original.getName());
        copy.setIssuingBody(original.getIssuingBody());
        copy.setRegulationNumber(original.getRegulationNumber());
        copy.setVersion((original.getVersion() != null ? original.getVersion() : "") + " - Copy");
        copy.setDescription(original.getDescription());
        copy.setCountryRegion(original.getCountryRegion());
        if (original.getSampleTypes() != null) {
            copy.setSampleTypes(new LinkedHashSet<>(original.getSampleTypes()));
        }
        copy.setEffectiveDate(original.getEffectiveDate());
        copy.setExpiryDate(original.getExpiryDate());
        copy.setStatus(ComplianceStandardStatus.DRAFT);
        copy.setIsPreSeeded(false);
        copy.setSysUserId(userId);
        baseObjectDAO.insert(copy);

        // Deep-clone children. Threshold value mappings ride along on the
        // threshold cascade (cascade=ALL on ComplianceThreshold.valueMappings).
        List<ParameterGroup> sourceGroups = parameterGroupDAO.getGroupsByStandardId(original.getId());
        if (sourceGroups != null) {
            for (ParameterGroup origGroup : sourceGroups) {
                ParameterGroup groupCopy = new ParameterGroup();
                groupCopy.setStandard(copy);
                groupCopy.setName(origGroup.getName());
                groupCopy.setDescription(origGroup.getDescription());
                groupCopy.setSortOrder(origGroup.getSortOrder());
                groupCopy.setIsMandatory(origGroup.getIsMandatory());
                groupCopy.setSysUserId(userId);
                parameterGroupDAO.insert(groupCopy);

                List<ComplianceThreshold> origThresholds = complianceThresholdDAO
                        .getThresholdsByGroupId(origGroup.getId());
                if (origThresholds == null) {
                    continue;
                }
                for (ComplianceThreshold origThreshold : origThresholds) {
                    ComplianceThreshold tCopy = new ComplianceThreshold();
                    tCopy.setGroup(groupCopy);
                    tCopy.setTest(origThreshold.getTest());
                    tCopy.setParameterCode(origThreshold.getParameterCode());
                    tCopy.setDisplayName(origThreshold.getDisplayName());
                    tCopy.setThresholdType(origThreshold.getThresholdType());
                    tCopy.setMinValue(origThreshold.getMinValue());
                    tCopy.setMaxValue(origThreshold.getMaxValue());
                    tCopy.setTargetValue(origThreshold.getTargetValue());
                    tCopy.setDetectionLimit(origThreshold.getDetectionLimit());
                    tCopy.setValueDescriptive(origThreshold.getValueDescriptive());
                    tCopy.setUnits(origThreshold.getUnits());
                    tCopy.setMethodReference(origThreshold.getMethodReference());
                    tCopy.setNotes(origThreshold.getNotes());
                    tCopy.setIsMandatory(origThreshold.getIsMandatory());
                    tCopy.setIsActive(origThreshold.getIsActive());
                    tCopy.setSortOrder(origThreshold.getSortOrder());
                    tCopy.setValidationRules(origThreshold.getValidationRules());
                    tCopy.setSysUserId(userId);

                    if (origThreshold.getValueMappings() != null) {
                        for (ComplianceThresholdValueMap origMap : origThreshold.getValueMappings()) {
                            ComplianceThresholdValueMap mapCopy = new ComplianceThresholdValueMap(
                                    origMap.getOptionValue(), origMap.getComplianceStatus());
                            mapCopy.setThreshold(tCopy);
                            mapCopy.setSysUserId(userId);
                            tCopy.getValueMappings().add(mapCopy);
                        }
                    }
                    complianceThresholdDAO.insert(tCopy);
                }
            }
        }

        return copy;
    }

    @Override
    @Transactional
    public ComplianceStandard save(ComplianceStandard standard) {
        validateStandard(standard);
        return super.save(standard);
    }

    @Override
    @Transactional
    public ComplianceStandard update(ComplianceStandard standard) {
        // The PUT path went straight to super.update() and bypassed
        // validateStandard, so required-field checks and the BR-004
        // SUPERSEDED-needs-replacement check were skipped on update.
        validateStandard(standard);
        return super.update(standard);
    }

    /**
     * BR-002: a standard can only be deleted if no thresholds reference it.
     * Previously we blocked on "has parameter groups", which over-blocked (a
     * standard with empty groups but no thresholds was un-deletable). Switch to
     * checking the actual referencing rows.
     */
    @Override
    @Transactional
    public void delete(ComplianceStandard standard) {
        if (standard != null) {
            if (Boolean.TRUE.equals(standard.getIsPreSeeded())) {
                throw new LIMSRuntimeException(ComplianceMessages.standardCannotDeletePreSeeded());
            }
            if (standard.getId() != null && complianceThresholdDAO.standardHasThresholds(standard.getId())) {
                throw new LIMSRuntimeException(ComplianceMessages.standardCannotDeleteWithThresholds());
            }
        }
        super.delete(standard);
    }
}
