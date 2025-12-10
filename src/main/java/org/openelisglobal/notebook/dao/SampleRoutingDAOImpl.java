package org.openelisglobal.notebook.dao;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.type.StandardBasicTypes;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.notebook.valueholder.SampleRouting;
import org.openelisglobal.notebook.valueholder.SampleRouting.DestinationType;
import org.springframework.stereotype.Component;

/**
 * DAO implementation for SampleRouting entity operations.
 */
@Component
public class SampleRoutingDAOImpl extends BaseDAOImpl<SampleRouting, Integer> implements SampleRoutingDAO {

    public SampleRoutingDAOImpl() {
        super(SampleRouting.class);
    }

    @Override
    public List<SampleRouting> getByNotebookId(Integer notebookId) {
        Session session = entityManager.unwrap(Session.class);
        String hql = "FROM SampleRouting sr " + "LEFT JOIN FETCH sr.box " + "WHERE sr.notebook.id = :notebookId "
                + "ORDER BY sr.sampleItemId";
        return session.createQuery(hql, SampleRouting.class).setParameter("notebookId", notebookId).getResultList();
    }

    @Override
    public List<SampleRouting> getBySampleItemId(Integer sampleItemId) {
        Session session = entityManager.unwrap(Session.class);
        String hql = "FROM SampleRouting sr " + "LEFT JOIN FETCH sr.notebook "
                + "WHERE sr.sampleItemId = :sampleItemId";
        return session.createQuery(hql, SampleRouting.class).setParameter("sampleItemId", sampleItemId).getResultList();
    }

    @Override
    public SampleRouting getByNotebookIdAndSampleItemId(Integer notebookId, Integer sampleItemId) {
        Session session = entityManager.unwrap(Session.class);
        String hql = "FROM SampleRouting sr " + "WHERE sr.notebook.id = :notebookId "
                + "AND sr.sampleItemId = :sampleItemId";
        List<SampleRouting> results = session.createQuery(hql, SampleRouting.class)
                .setParameter("notebookId", notebookId).setParameter("sampleItemId", sampleItemId).getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    @Override
    public List<SampleRouting> getByNotebookIdAndDestinationType(Integer notebookId, DestinationType destinationType) {
        Session session = entityManager.unwrap(Session.class);
        String hql = "FROM SampleRouting sr " + "LEFT JOIN FETCH sr.box " + "WHERE sr.notebook.id = :notebookId "
                + "AND sr.destinationType = :destinationType " + "ORDER BY sr.sampleItemId";
        return session.createQuery(hql, SampleRouting.class).setParameter("notebookId", notebookId)
                // Explicitly bind as string to avoid driver choosing bytea
                .setParameter("destinationType", destinationType.name(), StandardBasicTypes.STRING).getResultList();
    }

    @Override
    public SampleRouting getByBoxAndWell(Integer notebookId, Integer boxId, String wellCoordinate) {
        Session session = entityManager.unwrap(Session.class);
        String hql = "FROM SampleRouting sr " + "WHERE sr.notebook.id = :notebookId " + "AND sr.box.id = :boxId "
                + "AND sr.wellCoordinate = :wellCoordinate";
        List<SampleRouting> results = session.createQuery(hql, SampleRouting.class)
                .setParameter("notebookId", notebookId).setParameter("boxId", boxId)
                .setParameter("wellCoordinate", wellCoordinate).getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    @Override
    public long getUnroutedSampleCount(Integer notebookId) {
        Session session = entityManager.unwrap(Session.class);
        // Use native SQL to count samples in notebook that don't have a routing record
        String sql = "SELECT COUNT(DISTINCT nbs.sample_item_id) FROM clinlims.notebook_samples nbs "
                + "WHERE nbs.notebook_id = :notebookId " + "AND NOT EXISTS ("
                + "  SELECT 1 FROM clinlims.sample_routing sr "
                + "  WHERE sr.notebook_id = nbs.notebook_id AND sr.sample_item_id = nbs.sample_item_id" + ")";
        return ((Number) session.createNativeQuery(sql).setParameter("notebookId", notebookId).getSingleResult())
                .longValue();
    }

    @Override
    public long getCountByDestinationType(Integer notebookId, DestinationType destinationType) {
        Session session = entityManager.unwrap(Session.class);
        String hql = "SELECT COUNT(sr) FROM SampleRouting sr " + "WHERE sr.notebook.id = :notebookId "
                + "AND sr.destinationType = :destinationType";
        return session.createQuery(hql, Long.class).setParameter("notebookId", notebookId)
                // Explicitly bind as string to avoid driver choosing bytea
                .setParameter("destinationType", destinationType.name(), StandardBasicTypes.STRING).getSingleResult();
    }

    @Override
    public List<SampleRouting> getByBoxId(Integer notebookId, Integer boxId) {
        Session session = entityManager.unwrap(Session.class);
        String hql = "FROM SampleRouting sr " + "WHERE sr.notebook.id = :notebookId " + "AND sr.box.id = :boxId "
                + "ORDER BY sr.wellCoordinate";
        return session.createQuery(hql, SampleRouting.class).setParameter("notebookId", notebookId)
                .setParameter("boxId", boxId).getResultList();
    }
}
