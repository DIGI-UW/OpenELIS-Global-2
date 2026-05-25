package org.openelisglobal.vector.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.vector.dao.VectorPoolDAO;
import org.openelisglobal.vector.valueholder.VectorPool;
import org.openelisglobal.vector.valueholder.VectorPoolMember;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VectorPoolServiceImpl extends AuditableBaseObjectServiceImpl<VectorPool, Integer>
        implements VectorPoolService {

    @Autowired
    protected VectorPoolDAO baseObjectDAO;

    @PersistenceContext
    private EntityManager entityManager;

    public VectorPoolServiceImpl() {
        super(VectorPool.class);
    }

    @Override
    protected VectorPoolDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<VectorPool> findById(Integer id) {
        if (id == null) {
            return Optional.empty();
        }
        return baseObjectDAO.get(id);
    }

    @Override
    @Transactional
    public VectorPool createPoolWithMembers(VectorPool pool, List<SampleItem> members, String sysUserId) {
        if (pool == null) {
            throw new IllegalArgumentException("pool is required");
        }
        if (members == null) {
            members = new ArrayList<>();
        }
        pool.setSysUserId(sysUserId);
        if (pool.getActive() == null) {
            pool.setActive(Boolean.TRUE);
        }

        // Insert the pool first so it has a generated id, then insert one
        // vector_pool_member row per organism. Pure M:N join — the same
        // organism can sit in multiple pools across V-03 deconvolution rounds
        // without any schema change.
        baseObjectDAO.insert(pool);
        for (SampleItem si : members) {
            if (si == null || si.getId() == null) {
                continue;
            }
            entityManager.persist(new VectorPoolMember(pool, si));
        }
        return pool;
    }

    @Override
    @Transactional(readOnly = true)
    public List<VectorPool> getBySampleId(String sampleId) {
        return baseObjectDAO.getBySampleId(sampleId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VectorPool> getByParentPoolId(Integer parentPoolId) {
        return baseObjectDAO.getByParentPoolId(parentPoolId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SampleItem> getMembersByPoolId(Integer poolId) {
        if (poolId == null) {
            return new ArrayList<>();
        }
        return entityManager
                .createQuery("SELECT m.sampleItem FROM VectorPoolMember m WHERE m.pool.id = :poolId"
                        + " ORDER BY m.sampleItem.sortOrder", SampleItem.class)
                .setParameter("poolId", poolId).getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public int countMembersByPoolId(Integer poolId) {
        if (poolId == null) {
            return 0;
        }
        Long count = entityManager
                .createQuery("SELECT COUNT(m) FROM VectorPoolMember m WHERE m.pool.id = :poolId", Long.class)
                .setParameter("poolId", poolId).getSingleResult();
        return count == null ? 0 : count.intValue();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SampleItem> getFirstNonVoidedMemberByPoolId(Integer poolId) {
        if (poolId == null) {
            return Optional.empty();
        }
        return entityManager
                .createQuery("SELECT m.sampleItem FROM VectorPoolMember m"
                        + " WHERE m.pool.id = :poolId AND m.sampleItem.voided = false"
                        + " ORDER BY m.sampleItem.sortOrder", SampleItem.class)
                .setParameter("poolId", poolId).setMaxResults(1).getResultStream().findFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public VectorPool getIntakePoolBySampleItemId(String sampleItemId) {
        if (sampleItemId == null || sampleItemId.isBlank()) {
            return null;
        }
        return entityManager
                .createQuery(
                        "SELECT m.pool FROM VectorPoolMember m"
                                + " WHERE m.id.sampleItemId = :sampleItemId AND m.pool.parentPool IS NULL",
                        VectorPool.class)
                .setParameter("sampleItemId", sampleItemId).setMaxResults(1).getResultStream().findFirst().orElse(null);
    }
}
