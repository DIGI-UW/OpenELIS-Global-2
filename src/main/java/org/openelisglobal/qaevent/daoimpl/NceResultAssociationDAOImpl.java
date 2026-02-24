package org.openelisglobal.qaevent.daoimpl;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.qaevent.dao.NceResultAssociationDAO;
import org.openelisglobal.qaevent.valueholder.NcEvent;
import org.openelisglobal.qaevent.valueholder.NceResultAssociation;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class NceResultAssociationDAOImpl extends BaseDAOImpl<NceResultAssociation, Integer>
        implements NceResultAssociationDAO {

    public NceResultAssociationDAOImpl() {
        super(NceResultAssociation.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NceResultAssociation> getNceAssociationsForResult(String resultId) {
        if (resultId == null) {
            throw new LIMSRuntimeException("ResultId cannot be null");
        }

        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<NceResultAssociation> cq = cb.createQuery(NceResultAssociation.class);
            Root<NceResultAssociation> root = cq.from(NceResultAssociation.class);

            cq.select(root).where(cb.equal(root.get("resultId"), resultId)).orderBy(cb.desc(root.get("createdDate")));

            return entityManager.createQuery(cq).getResultList();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error retrieving NCE associations for result: " + resultId, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<NceResultAssociation> getResultAssociationsForNCE(String nceId) {
        if (nceId == null) {
            throw new LIMSRuntimeException("NceId cannot be null");
        }

        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<NceResultAssociation> cq = cb.createQuery(NceResultAssociation.class);
            Root<NceResultAssociation> root = cq.from(NceResultAssociation.class);
            Join<NceResultAssociation, NcEvent> nceJoin = root.join("ncEvent");

            // NcEvent.id is mapped as String but the DB column is INTEGER;
            // pass Integer so Hibernate binds the correct JDBC type
            cq.select(root).where(cb.equal(nceJoin.get("id"), Integer.valueOf(nceId)))
                    .orderBy(cb.desc(root.get("createdDate")));

            return entityManager.createQuery(cq).getResultList();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error retrieving result associations for NCE: " + nceId, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<NceResultAssociation> getAssociationsByType(String associationType) {
        if (associationType == null) {
            throw new LIMSRuntimeException("Association type cannot be null");
        }

        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<NceResultAssociation> cq = cb.createQuery(NceResultAssociation.class);
            Root<NceResultAssociation> root = cq.from(NceResultAssociation.class);

            cq.select(root).where(cb.equal(root.get("associationType"), associationType))
                    .orderBy(cb.desc(root.get("createdDate")));

            return entityManager.createQuery(cq).getResultList();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error retrieving associations by type: " + associationType, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean associationExists(String resultId, String nceId) {
        if (resultId == null || nceId == null) {
            return false;
        }

        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<Long> cq = cb.createQuery(Long.class);
            Root<NceResultAssociation> root = cq.from(NceResultAssociation.class);
            Join<NceResultAssociation, NcEvent> nceJoin = root.join("ncEvent");

            cq.select(cb.count(root)).where(cb.and(cb.equal(root.get("resultId"), resultId),
                    cb.equal(nceJoin.get("id"), Integer.valueOf(nceId))));

            Long count = entityManager.createQuery(cq).getSingleResult();
            return count != null && count > 0;
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException(
                    "Error checking association existence for result: " + resultId + ", NCE: " + nceId, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<NceResultAssociation> getAssociationsCreatedBy(String createdBy) {
        if (createdBy == null) {
            throw new LIMSRuntimeException("CreatedBy cannot be null");
        }

        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<NceResultAssociation> cq = cb.createQuery(NceResultAssociation.class);
            Root<NceResultAssociation> root = cq.from(NceResultAssociation.class);

            cq.select(root).where(cb.equal(root.get("createdBy"), createdBy)).orderBy(cb.desc(root.get("createdDate")));

            return entityManager.createQuery(cq).getResultList();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error retrieving associations created by: " + createdBy, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public int countNCEsForResult(String resultId) {
        if (resultId == null) {
            return 0;
        }

        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<Long> cq = cb.createQuery(Long.class);
            Root<NceResultAssociation> root = cq.from(NceResultAssociation.class);
            Join<NceResultAssociation, NcEvent> nceJoin = root.join("ncEvent");

            cq.select(cb.countDistinct(nceJoin.get("id"))).where(cb.equal(root.get("resultId"), resultId));

            Long count = entityManager.createQuery(cq).getSingleResult();
            return count != null ? count.intValue() : 0;
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error counting NCEs for result: " + resultId, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public int countResultsForNCE(String nceId) {
        if (nceId == null) {
            return 0;
        }

        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<Long> cq = cb.createQuery(Long.class);
            Root<NceResultAssociation> root = cq.from(NceResultAssociation.class);
            Join<NceResultAssociation, NcEvent> nceJoin = root.join("ncEvent");

            cq.select(cb.countDistinct(root.get("resultId")))
                    .where(cb.equal(nceJoin.get("id"), Integer.valueOf(nceId)));

            Long count = entityManager.createQuery(cq).getSingleResult();
            return count != null ? count.intValue() : 0;
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error counting results for NCE: " + nceId, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<NceResultAssociation> getRecentAssociations(int daysSince) {
        if (daysSince < 0) {
            throw new LIMSRuntimeException("Days since must be non-negative");
        }

        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<NceResultAssociation> cq = cb.createQuery(NceResultAssociation.class);
            Root<NceResultAssociation> root = cq.from(NceResultAssociation.class);

            Timestamp cutoffDate = Timestamp.from(Instant.now().minus(daysSince, ChronoUnit.DAYS));

            cq.select(root).where(cb.greaterThanOrEqualTo(root.get("createdDate"), cutoffDate))
                    .orderBy(cb.desc(root.get("createdDate")));

            return entityManager.createQuery(cq).getResultList();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error retrieving recent associations for " + daysSince + " days", e);
        }
    }

    @Override
    public int deleteAssociationsForResult(String resultId) {
        if (resultId == null) {
            throw new LIMSRuntimeException("ResultId cannot be null");
        }

        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaDelete<NceResultAssociation> cd = cb.createCriteriaDelete(NceResultAssociation.class);
            Root<NceResultAssociation> root = cd.from(NceResultAssociation.class);

            cd.where(cb.equal(root.get("resultId"), resultId));

            return entityManager.createQuery(cd).executeUpdate();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error deleting associations for result: " + resultId, e);
        }
    }

    @Override
    public int deleteAssociationsForNCE(String nceId) {
        if (nceId == null) {
            throw new LIMSRuntimeException("NceId cannot be null");
        }

        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaDelete<NceResultAssociation> cd = cb.createCriteriaDelete(NceResultAssociation.class);
            Root<NceResultAssociation> root = cd.from(NceResultAssociation.class);

            // CriteriaDelete supports subqueries but not joins directly;
            // use the FK path through the relationship
            cd.where(cb.equal(root.get("ncEvent").get("id"), Integer.valueOf(nceId)));

            return entityManager.createQuery(cd).executeUpdate();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error deleting associations for NCE: " + nceId, e);
        }
    }
}
