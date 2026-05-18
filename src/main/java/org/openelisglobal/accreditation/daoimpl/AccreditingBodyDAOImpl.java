package org.openelisglobal.accreditation.daoimpl;

import java.util.List;
import org.openelisglobal.accreditation.dao.AccreditingBodyDAO;
import org.openelisglobal.accreditation.valueholder.AccreditingBody;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class AccreditingBodyDAOImpl extends BaseDAOImpl<AccreditingBody, Long> implements AccreditingBodyDAO {

    public AccreditingBodyDAOImpl() {
        super(AccreditingBody.class);
    }

    @Override
    public AccreditingBody findByCode(String code) {
        String hql = "FROM AccreditingBody ab WHERE ab.code = :code";
        List<AccreditingBody> results = entityManager.createQuery(hql, AccreditingBody.class).setParameter("code", code)
                .getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    @Override
    public List<AccreditingBody> findAllActive() {
        String hql = "FROM AccreditingBody ab WHERE ab.active = true ORDER BY ab.displayOrder ASC";
        return entityManager.createQuery(hql, AccreditingBody.class).getResultList();
    }

    @Override
    public List<AccreditingBody> findAllOrderedByDisplayOrder() {
        String hql = "FROM AccreditingBody ab ORDER BY ab.displayOrder ASC, ab.id ASC";
        return entityManager.createQuery(hql, AccreditingBody.class).getResultList();
    }

    @Override
    public long countTestAccreditationsByBodyId(Long accreditingBodyId) {
        String hql = "SELECT COUNT(ta.id) FROM TestAccreditation ta WHERE ta.accreditingBody.id = :bodyId";
        return entityManager.createQuery(hql, Long.class).setParameter("bodyId", accreditingBodyId).getSingleResult();
    }

    @Override
    public List<AccreditingBody> getAll() {
        return findAllOrderedByDisplayOrder();
    }
}
