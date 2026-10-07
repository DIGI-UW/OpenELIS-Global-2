package org.openelisglobal.configuration.dao;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import java.util.List;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.configuration.valueholder.ReferenceAlias;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class ReferenceAliasDAOImpl extends BaseDAOImpl<ReferenceAlias, String> implements ReferenceAliasDAO {

    public ReferenceAliasDAOImpl() {
        super(ReferenceAlias.class);
    }

    @Override
    public ReferenceAlias getByTypeAndAlias(String referenceType, String normalizedAlias) throws LIMSRuntimeException {
        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<ReferenceAlias> cq = cb.createQuery(ReferenceAlias.class);
            Root<ReferenceAlias> root = cq.from(ReferenceAlias.class);
            cq.where(cb.equal(root.get("referenceType"), referenceType), cb.equal(root.get("alias"), normalizedAlias));
            List<ReferenceAlias> matches = entityManager.createQuery(cq).setMaxResults(1).getResultList();
            return matches.isEmpty() ? null : matches.get(0);
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error looking up alias " + referenceType + " '" + normalizedAlias + "'", e);
        }
    }
}
