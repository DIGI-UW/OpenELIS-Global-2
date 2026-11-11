package org.openelisglobal.organization.daoimpl;

import jakarta.persistence.TypedQuery;
import java.util.List;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.organization.dao.OrganizationChangeDAO;
import org.openelisglobal.organization.valueholder.OrganizationChange;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class OrganizationChangeDAOImpl extends BaseDAOImpl<OrganizationChange, Integer>
        implements OrganizationChangeDAO {

    public OrganizationChangeDAOImpl() {
        super(OrganizationChange.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrganizationChange> getForOrganization(Integer organizationId) {
        try {
            TypedQuery<OrganizationChange> query = entityManager.createQuery(
                    "select c from OrganizationChange c where c.organizationId = :orgId order by c.changedAt desc, c.id"
                            + " desc",
                    OrganizationChange.class);
            query.setParameter("orgId", organizationId);
            return query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in OrganizationChangeDAOImpl.getForOrganization()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrganizationChange> getNameChanges() {
        try {
            return entityManager.createQuery(
                    "select c from OrganizationChange c where c.changes like :marker order by c.changedAt desc, c.id"
                            + " desc",
                    OrganizationChange.class).setParameter("marker", "%\"field\":\"name\"%").getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in OrganizationChangeDAOImpl.getNameChanges()", e);
        }
    }
}
