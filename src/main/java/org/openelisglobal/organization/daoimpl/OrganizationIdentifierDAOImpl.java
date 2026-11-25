package org.openelisglobal.organization.daoimpl;

import jakarta.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.organization.dao.OrganizationIdentifierDAO;
import org.openelisglobal.organization.valueholder.OrganizationIdentifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class OrganizationIdentifierDAOImpl extends BaseDAOImpl<OrganizationIdentifier, Integer>
        implements OrganizationIdentifierDAO {

    public OrganizationIdentifierDAOImpl() {
        super(OrganizationIdentifier.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrganizationIdentifier> getForOrganization(Integer organizationId) {
        try {
            TypedQuery<OrganizationIdentifier> query = entityManager.createQuery(
                    "select i from OrganizationIdentifier i where i.organizationId = :orgId order by i.reporting desc,"
                            + " lower(i.label)",
                    OrganizationIdentifier.class);
            query.setParameter("orgId", organizationId);
            return query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in OrganizationIdentifierDAOImpl.getForOrganization()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrganizationIdentifier> getForOrganizations(List<Integer> organizationIds) {
        if (organizationIds == null || organizationIds.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            TypedQuery<OrganizationIdentifier> query = entityManager
                    .createQuery(
                            "select i from OrganizationIdentifier i where i.organizationId in (:orgIds) order by"
                                    + " i.organizationId, i.reporting desc, lower(i.label)",
                            OrganizationIdentifier.class);
            query.setParameter("orgIds", organizationIds);
            return query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in OrganizationIdentifierDAOImpl.getForOrganizations()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrganizationIdentifier> getByLabelAndValue(String label, String value) {
        try {
            TypedQuery<OrganizationIdentifier> query = entityManager.createQuery(
                    "select i from OrganizationIdentifier i where lower(i.label) = :label and lower(i.value) = :value",
                    OrganizationIdentifier.class);
            query.setParameter("label", label.trim().toLowerCase(Locale.ROOT));
            query.setParameter("value", value.trim().toLowerCase(Locale.ROOT));
            return query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in OrganizationIdentifierDAOImpl.getByLabelAndValue()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrganizationIdentifier> searchByValue(String text, int limit) {
        try {
            TypedQuery<OrganizationIdentifier> query = entityManager.createQuery(
                    "select i from OrganizationIdentifier i where lower(i.value) like :text",
                    OrganizationIdentifier.class);
            query.setParameter("text", "%" + text.trim().toLowerCase(Locale.ROOT) + "%");
            query.setMaxResults(limit);
            return query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in OrganizationIdentifierDAOImpl.searchByValue()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getDistinctLabels() {
        try {
            return entityManager
                    .createQuery("select distinct i.label from OrganizationIdentifier i order by i.label", String.class)
                    .getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in OrganizationIdentifierDAOImpl.getDistinctLabels()", e);
        }
    }

    @Override
    public void deleteForOrganization(Integer organizationId) {
        try {
            entityManager.createQuery("delete from OrganizationIdentifier i where i.organizationId = :orgId")
                    .setParameter("orgId", organizationId).executeUpdate();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in OrganizationIdentifierDAOImpl.deleteForOrganization()", e);
        }
    }
}
