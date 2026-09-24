package org.openelisglobal.compliance.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.compliance.dao.ComplianceThresholdDAO;
import org.openelisglobal.compliance.dao.ParameterGroupDAO;
import org.openelisglobal.compliance.valueholder.ParameterGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of ParameterGroupService following OpenELIS patterns.
 *
 * Constitutional compliance: - Extends AuditableBaseObjectServiceImpl for audit
 * trail support - Uses @Transactional annotations for data integrity -
 * Implements proper exception handling
 */
@Service
public class ParameterGroupServiceImpl extends AuditableBaseObjectServiceImpl<ParameterGroup, String>
        implements ParameterGroupService {

    @Autowired
    protected ParameterGroupDAO baseObjectDAO;

    // Direct DAO injection (rather than going through ComplianceThresholdService)
    // avoids the service-layer cycle ParameterGroupService ↔
    // ComplianceThresholdService
    // — ComplianceThresholdService already depends on ParameterGroupService for
    // the cascade in propagateSysUserIdToValueMappings, and Spring's bean
    // resolver can't break that cycle without @Lazy. The single-method DAO use
    // here keeps the dependency graph acyclic.
    @Autowired
    private ComplianceThresholdDAO complianceThresholdDAO;

    ParameterGroupServiceImpl() {
        super(ParameterGroup.class);
    }

    @Override
    protected ParameterGroupDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParameterGroup> getGroupsByStandardId(String standardId) {
        return getBaseObjectDAO().getGroupsByStandardId(standardId);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Integer> countGroupsByStandardIds(Collection<String> standardIds) {
        return getBaseObjectDAO().countGroupsByStandardIds(standardIds);
    }

    @Override
    @Transactional
    public ParameterGroup save(ParameterGroup parameterGroup) {
        String standardId = parameterGroup.getStandardId();
        if (standardId != null && parameterGroup.getName() != null) {
            // Look up by (standardId, name) and reject only when the row found
            // belongs to a different group. The existence-only check rejected
            // every update that kept the same name, since the row being edited
            // matched itself.
            ParameterGroup existing = getBaseObjectDAO().findByStandardIdAndName(standardId, parameterGroup.getName());
            if (existing != null && !java.util.Objects.equals(existing.getId(), parameterGroup.getId())) {
                throw new LIMSRuntimeException("ParameterGroup with name '" + parameterGroup.getName()
                        + "' already exists for standard " + standardId);
            }
        }
        return super.save(parameterGroup);
    }

    /**
     * BR-003: a parameter group cannot be deleted while any
     * {@link org.openelisglobal.compliance.valueholder.ComplianceThreshold}
     * references it. Without this guard the FK
     * {@code fk_compliance_threshold_group ON DELETE CASCADE} would silently wipe
     * the linked thresholds — the FRS requires the operation to refuse with an
     * inline error so the admin can decide what to do with the thresholds first.
     */
    @Override
    @Transactional
    public void delete(ParameterGroup parameterGroup) {
        if (parameterGroup != null && parameterGroup.getId() != null
                && complianceThresholdDAO.groupHasThresholds(parameterGroup.getId())) {
            throw new LIMSRuntimeException(ComplianceMessages.parameterGroupCannotDeleteWithThresholds());
        }
        super.delete(parameterGroup);
    }
}