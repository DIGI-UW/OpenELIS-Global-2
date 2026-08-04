package org.openelisglobal.accreditation.daoimpl;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
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

    /** FR-31 order: display_order, ties broken alphabetically on code. */
    @Override
    @Transactional(readOnly = true)
    public List<AccreditingBody> getAllOrdered() {
        return entityManager.unwrap(Session.class)
                .createQuery("from AccreditingBody b order by b.displayOrder, b.code", AccreditingBody.class).list();
    }

    @Override
    @Transactional(readOnly = true)
    public AccreditingBody getByCode(String code) {
        Query<AccreditingBody> query = entityManager.unwrap(Session.class)
                .createQuery("from AccreditingBody b where upper(b.code) = :code", AccreditingBody.class);
        query.setParameter("code", code == null ? null : code.trim().toUpperCase());
        return query.uniqueResult();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Object[]> countEnrolledTestsByBody() {
        // Grouped count rather than N per-body queries: the bodies list renders every
        // body's enrolled-test total in one page load.
        String hql = "select ta.accreditingBodyId, count(ta.id) from TestAccreditation ta"
                + " group by ta.accreditingBodyId";
        return entityManager.unwrap(Session.class).createQuery(hql, Object[].class).list();
    }
}
