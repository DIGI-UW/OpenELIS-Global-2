package org.openelisglobal.compliance.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.compliance.controller.rest.ComplianceThresholdListItem;
import org.openelisglobal.compliance.dao.ComplianceThresholdDAO;
import org.openelisglobal.compliance.valueholder.ComplianceThreshold;
import org.openelisglobal.compliance.valueholder.ParameterGroup;
import org.openelisglobal.compliance.valueholder.ThresholdType;
import org.openelisglobal.test.service.TestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of ComplianceThresholdService following OpenELIS patterns.
 *
 * Constitutional compliance: - Extends AuditableBaseObjectServiceImpl for audit
 * trail support - Uses @Transactional annotations for data integrity -
 * Implements proper exception handling - Validates business rules before
 * persistence
 */
@Service
public class ComplianceThresholdServiceImpl extends AuditableBaseObjectServiceImpl<ComplianceThreshold, String>
        implements ComplianceThresholdService {

    @Autowired
    protected ComplianceThresholdDAO baseObjectDAO;

    @Autowired
    private ParameterGroupService parameterGroupService;

    // Used by createThresholdItem/updateThresholdItem to swap Jackson's id-only
    // Test stub for a managed entity before persist. Without this Hibernate
    // refuses the save with TransientPropertyValueException.
    @Autowired
    private TestService testService;

    ComplianceThresholdServiceImpl() {
        super(ComplianceThreshold.class);
    }

    @Override
    protected ComplianceThresholdDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceThreshold> getThresholdsByGroupId(String groupId) {
        return getBaseObjectDAO().getThresholdsByGroupId(groupId);
    }

    @Override
    @Transactional
    public ComplianceThreshold save(ComplianceThreshold threshold) {
        validateThreshold(threshold);
        // Enforce uniqueness on (groupId, parameterCode, thresholdType) for inserts.
        // FRS S-01 v2.3: a single test may have multiple threshold rows in the same
        // group, one per limit type (HIGH + BORDERLINE on the same parameter is
        // valid). The check therefore includes thresholdType so multi-limit saves
        // don't collide.
        if (threshold.getId() == null && threshold.getGroupId() != null
                && getBaseObjectDAO().parameterExistsInGroupForType(threshold.getGroupId(),
                        threshold.getParameterCode(), threshold.getThresholdType())) {
            throw new LIMSRuntimeException(ComplianceMessages.thresholdDuplicateInGroup(threshold.getParameterCode()));
        }
        propagateSysUserIdToValueMappings(threshold);
        return super.save(threshold);
    }

    @Override
    @Transactional
    public ComplianceThreshold update(ComplianceThreshold threshold) {
        validateThreshold(threshold);
        propagateSysUserIdToValueMappings(threshold);
        return super.update(threshold);
    }

    /**
     * The cascade=ALL on {@code ComplianceThreshold.valueMappings} flushes each
     * {@link ComplianceThresholdValueMap} when the parent is persisted, but each
     * child carries its own {@code sys_user_id NOT NULL} column. The REST payload
     * only sets sysUserId on the parent threshold, so without this we'd insert the
     * children with NULL and Postgres rejects them.
     *
     * <p>
     * Stamps mappings whose {@code systemUserId} is currently null with the parent
     * threshold's sysUserId. Existing non-null values are left alone so a mapping
     * that was originally created by user A retains that audit trail even when user
     * B saves an unrelated change to the parent.
     */
    private void propagateSysUserIdToValueMappings(ComplianceThreshold threshold) {
        if (threshold == null || threshold.getValueMappings() == null) {
            return;
        }
        String sysUserId = threshold.getSysUserId();
        if (sysUserId == null || sysUserId.isBlank()) {
            return;
        }
        for (org.openelisglobal.compliance.valueholder.ComplianceThresholdValueMap mapping : threshold
                .getValueMappings()) {
            if (mapping.getSystemUserId() == null) {
                mapping.setSysUserId(sysUserId);
            }
        }
    }

    @Override
    @Transactional
    public void delete(ComplianceThreshold threshold) {
        super.delete(threshold);
    }

    /**
     * Validate business rules for threshold values.
     *
     * <p>
     * FR-3-006 / FR-3-007a / BR-006: RANGE and BORDERLINE both require both a min
     * and a max — saving with one null silently produced a half-defined threshold
     * that would never evaluate correctly. Each type-specific value field is
     * enforced before the lower&lt;upper sanity check.
     */
    private void validateThreshold(ComplianceThreshold threshold) {
        ThresholdType type = threshold.getThresholdType();

        if (type == ThresholdType.RANGE) {
            if (threshold.getMinValue() == null || threshold.getMaxValue() == null) {
                throw new LIMSRuntimeException(ComplianceMessages.thresholdRangeBothRequired());
            }
            if (threshold.getMinValue().compareTo(threshold.getMaxValue()) > 0) {
                throw new LIMSRuntimeException(ComplianceMessages.thresholdInvalidRange());
            }
        }

        if (type == ThresholdType.BORDERLINE) {
            if (threshold.getMinValue() == null || threshold.getMaxValue() == null) {
                throw new LIMSRuntimeException(ComplianceMessages.thresholdBorderlineBothRequired());
            }
            if (threshold.getMinValue().compareTo(threshold.getMaxValue()) > 0) {
                throw new LIMSRuntimeException(ComplianceMessages.thresholdInvalidRange());
            }
        }

        if (type == ThresholdType.MINIMUM && threshold.getMinValue() == null) {
            throw new LIMSRuntimeException(ComplianceMessages.thresholdMinimumRequired());
        }

        if (type == ThresholdType.MAXIMUM && threshold.getMaxValue() == null) {
            throw new LIMSRuntimeException(ComplianceMessages.thresholdMaximumRequired());
        }

        if (type == ThresholdType.EXACT && threshold.getTargetValue() == null) {
            throw new LIMSRuntimeException(ComplianceMessages.thresholdTargetRequired());
        }

        if (type == ThresholdType.DESCRIPTIVE
                && (threshold.getValueDescriptive() == null || threshold.getValueDescriptive().trim().isEmpty())) {
            throw new LIMSRuntimeException(ComplianceMessages.thresholdQualitativeRequired());
        }

        if (type == ThresholdType.SELECT_MAP
                && (threshold.getValueMappings() == null || threshold.getValueMappings().isEmpty())) {
            throw new LIMSRuntimeException(ComplianceMessages.thresholdSelectMappingRequired());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Object[]> getTestThresholdSummary() {
        return getBaseObjectDAO().getTestThresholdSummary();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Integer> countLinkedTestsByStandardIds(Collection<String> standardIds) {
        return getBaseObjectDAO().countLinkedTestsByStandardIds(standardIds);
    }

    // ----- DTO-returning variants -----

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceThresholdListItem> getThresholdItemsByGroupId(String groupId) {
        return toItems(getBaseObjectDAO().getThresholdsByGroupId(groupId));
    }

    @Override
    @Transactional(readOnly = true)
    public ComplianceThresholdListItem getThresholdItem(String thresholdId) {
        ComplianceThreshold t = get(thresholdId);
        return t == null ? null : new ComplianceThresholdListItem(t);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceThresholdListItem> getThresholdItemsByTestId(String testId) {
        return toItems(getBaseObjectDAO().getThresholdsByTestId(testId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceThresholdListItem> getThresholdItemsByTestAndStandard(String testId, String standardId) {
        return toItems(getBaseObjectDAO().getThresholdsByTestAndStandard(testId, standardId));
    }

    @Override
    @Transactional
    public ComplianceThresholdListItem createThresholdItem(ComplianceThreshold threshold, String sysUserId) {
        attachManagedAssociations(threshold);
        if (sysUserId != null) {
            threshold.setSysUserId(sysUserId);
        }
        ComplianceThreshold saved = save(threshold);
        return new ComplianceThresholdListItem(saved);
    }

    @Override
    @Transactional
    public ComplianceThresholdListItem updateThresholdItem(String id, ComplianceThreshold threshold, String sysUserId) {
        ComplianceThreshold existing = get(id);
        if (existing == null) {
            return null;
        }
        threshold.setId(id);
        // Carry @Version forward; the list DTO omits `lastupdated`, so
        // payloads come back null and merge() would throw StaleObjectStateException.
        threshold.setLastupdated(existing.getLastupdated());
        attachManagedAssociations(threshold);
        if (sysUserId != null) {
            threshold.setSysUserId(sysUserId);
        }
        ComplianceThreshold updated = update(threshold);
        return new ComplianceThresholdListItem(updated);
    }

    /**
     * Wraps each entity in the slim DTO inside the active service transaction so
     * the lazy graph (group → standard, valueMappings) is fully resolved before the
     * DTO escapes to the controller layer.
     */
    private List<ComplianceThresholdListItem> toItems(List<ComplianceThreshold> thresholds) {
        if (thresholds == null) {
            return java.util.Collections.emptyList();
        }
        List<ComplianceThresholdListItem> out = new ArrayList<>(thresholds.size());
        for (ComplianceThreshold t : thresholds) {
            out.add(new ComplianceThresholdListItem(t));
        }
        return out;
    }

    /**
     * Replaces the id-only stubs Jackson hydrates from the request body (e.g.
     * {@code "group": {"id": "6"}}) with managed entities loaded from the
     * per-domain services. Without this, Hibernate flags those refs as transient
     * and refuses to persist the threshold + cascaded valueMappings.
     */
    private void attachManagedAssociations(ComplianceThreshold threshold) {
        if (threshold.getGroup() != null && threshold.getGroup().getId() != null) {
            ParameterGroup managedGroup = parameterGroupService.get(threshold.getGroup().getId());
            if (managedGroup != null) {
                threshold.setGroup(managedGroup);
            }
        }
        if (threshold.getTest() != null && threshold.getTest().getId() != null) {
            org.openelisglobal.test.valueholder.Test managedTest = testService.get(threshold.getTest().getId());
            if (managedTest != null) {
                threshold.setTest(managedTest);
            }
        }
    }
}