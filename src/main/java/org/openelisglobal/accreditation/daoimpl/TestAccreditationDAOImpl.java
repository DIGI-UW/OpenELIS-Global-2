package org.openelisglobal.accreditation.daoimpl;

import java.time.LocalDate;
import java.util.List;
import org.openelisglobal.accreditation.dao.TestAccreditationDAO;
import org.openelisglobal.accreditation.valueholder.TestAccreditation;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class TestAccreditationDAOImpl extends BaseDAOImpl<TestAccreditation, Long> implements TestAccreditationDAO {

    public TestAccreditationDAOImpl() {
        super(TestAccreditation.class);
    }

    @Override
    public List<TestAccreditation> findByTestId(Long testId) {
        String hql = "FROM TestAccreditation ta " + "LEFT JOIN FETCH ta.accreditingBody "
                + "WHERE ta.test.id = :testId " + "ORDER BY ta.expiresOn DESC";
        return entityManager.createQuery(hql, TestAccreditation.class).setParameter("testId", testId).getResultList();
    }

    @Override
    public List<TestAccreditation> findByAccreditingBodyId(Long accreditingBodyId) {
        String hql = "FROM TestAccreditation ta " + "LEFT JOIN FETCH ta.test "
                + "WHERE ta.accreditingBody.id = :bodyId " + "ORDER BY ta.expiresOn DESC";
        return entityManager.createQuery(hql, TestAccreditation.class).setParameter("bodyId", accreditingBodyId)
                .getResultList();
    }

    @Override
    public TestAccreditation findByTestAndBody(Long testId, Long accreditingBodyId) {
        String hql = "FROM TestAccreditation ta " + "LEFT JOIN FETCH ta.test " + "LEFT JOIN FETCH ta.accreditingBody "
                + "WHERE ta.test.id = :testId AND ta.accreditingBody.id = :bodyId";
        List<TestAccreditation> results = entityManager.createQuery(hql, TestAccreditation.class)
                .setParameter("testId", testId).setParameter("bodyId", accreditingBodyId).getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    @Override
    public List<TestAccreditation> findExpiringOnOrBefore(LocalDate expirationDate) {
        String hql = "FROM TestAccreditation ta " + "LEFT JOIN FETCH ta.test " + "LEFT JOIN FETCH ta.accreditingBody "
                + "WHERE ta.expiresOn <= :expirationDate " + "ORDER BY ta.expiresOn ASC";
        return entityManager.createQuery(hql, TestAccreditation.class).setParameter("expirationDate", expirationDate)
                .getResultList();
    }

    @Override
    public List<TestAccreditation> findAllActive() {
        String hql = "FROM TestAccreditation ta " + "LEFT JOIN FETCH ta.test "
                + "LEFT JOIN FETCH ta.accreditingBody ab " + "WHERE ta.expiresOn >= CURRENT_DATE "
                + "AND ab.active = true " + "ORDER BY ta.expiresOn ASC";
        return entityManager.createQuery(hql, TestAccreditation.class).getResultList();
    }

    @Override
    public long countActiveByTestId(Long testId) {
        String hql = "SELECT COUNT(ta.id) FROM TestAccreditation ta " + "WHERE ta.test.id = :testId "
                + "AND ta.expiresOn >= CURRENT_DATE " + "AND ta.accreditingBody.active = true";
        return entityManager.createQuery(hql, Long.class).setParameter("testId", testId).getSingleResult();
    }

    @Override
    public boolean existsByTestAndBody(Long testId, Long accreditingBodyId) {
        String hql = "SELECT COUNT(ta.id) FROM TestAccreditation ta "
                + "WHERE ta.test.id = :testId AND ta.accreditingBody.id = :bodyId";
        long count = entityManager.createQuery(hql, Long.class).setParameter("testId", testId)
                .setParameter("bodyId", accreditingBodyId).getSingleResult();
        return count > 0;
    }

    @Override
    public List<TestAccreditation> findByFilters(Long testId, Long accreditingBodyId, Long sectionId, String q) {
        StringBuilder hql = new StringBuilder("FROM TestAccreditation ta " + "LEFT JOIN FETCH ta.test t "
                + "LEFT JOIN FETCH ta.accreditingBody " + "WHERE 1=1 ");
        if (testId != null)
            hql.append("AND ta.test.id = :testId ");
        if (accreditingBodyId != null)
            hql.append("AND ta.accreditingBody.id = :bodyId ");
        if (sectionId != null)
            hql.append("AND t.testSection.id = :sectionId ");

        boolean hasSearchQuery = q != null && !q.trim().isEmpty();

        if (hasSearchQuery)
            hql.append("AND (LOWER(t.description) LIKE :q) ");
        hql.append("ORDER BY ta.expiresOn DESC");

        jakarta.persistence.TypedQuery<TestAccreditation> query = entityManager.createQuery(hql.toString(),
                TestAccreditation.class);

        if (testId != null)
            query.setParameter("testId", testId);
        if (accreditingBodyId != null)
            query.setParameter("bodyId", accreditingBodyId);
        if (sectionId != null)
            query.setParameter("sectionId", sectionId);
        if (hasSearchQuery)
            query.setParameter("q", "%" + q.toLowerCase() + "%");

        return query.getResultList();
    }

    public List<TestAccreditation> getAll() {
        String hql = "FROM TestAccreditation ta " + "LEFT JOIN FETCH ta.test " + "LEFT JOIN FETCH ta.accreditingBody "
                + "ORDER BY ta.expiresOn DESC";
        return entityManager.createQuery(hql, TestAccreditation.class).getResultList();
    }
}
