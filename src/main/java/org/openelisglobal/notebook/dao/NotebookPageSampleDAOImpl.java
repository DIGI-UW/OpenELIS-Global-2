package org.openelisglobal.notebook.dao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hibernate.Session;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.notebook.valueholder.NotebookPageSample;
import org.openelisglobal.notebook.valueholder.NotebookPageSample.Status;
import org.springframework.stereotype.Component;

/**
 * DAO implementation for NotebookPageSample entity operations.
 */
@Component
public class NotebookPageSampleDAOImpl extends BaseDAOImpl<NotebookPageSample, Integer>
        implements NotebookPageSampleDAO {

    public NotebookPageSampleDAOImpl() {
        super(NotebookPageSample.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<NotebookPageSample> getByPageId(Integer pageId) {
        Session session = entityManager.unwrap(Session.class);
        // Use native SQL to avoid Hibernate type conversion issues with JSONB/UUID
        // columns
        String sql = "SELECT nps.* FROM clinlims.notebook_page_sample nps " + "WHERE nps.notebook_page_id = :pageId "
                + "ORDER BY nps.sample_item_id";
        return session.createNativeQuery(sql, NotebookPageSample.class).setParameter("pageId", pageId).getResultList();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<NotebookPageSample> getByPageIdAndStatus(Integer pageId, Status status) {
        Session session = entityManager.unwrap(Session.class);
        // Use native SQL to avoid Hibernate type conversion issues with JSONB/UUID
        // columns
        String sql = "SELECT nps.* FROM clinlims.notebook_page_sample nps "
                + "WHERE nps.notebook_page_id = :pageId AND nps.status = :status " + "ORDER BY nps.sample_item_id";
        return session.createNativeQuery(sql, NotebookPageSample.class).setParameter("pageId", pageId)
                .setParameter("status", status.name()).getResultList();
    }

    @Override
    public NotebookPageSample getByPageIdAndSampleItemId(Integer pageId, Integer sampleItemId) {
        Session session = entityManager.unwrap(Session.class);
        String hql = "FROM NotebookPageSample nps " + "WHERE nps.notebookPage.id = :pageId "
                + "AND nps.sampleItemId = :sampleItemId";
        List<NotebookPageSample> results = session.createQuery(hql, NotebookPageSample.class)
                .setParameter("pageId", pageId).setParameter("sampleItemId", sampleItemId.toString()).getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    @Override
    public Map<Status, Long> getStatusCountsByPageId(Integer pageId) {
        Session session = entityManager.unwrap(Session.class);
        String hql = "SELECT nps.status, COUNT(nps) FROM NotebookPageSample nps "
                + "WHERE nps.notebookPage.id = :pageId GROUP BY nps.status";
        List<Object[]> results = session.createQuery(hql, Object[].class).setParameter("pageId", pageId)
                .getResultList();

        Map<Status, Long> counts = new HashMap<>();
        for (Status s : Status.values()) {
            counts.put(s, 0L);
        }
        for (Object[] row : results) {
            counts.put((Status) row[0], (Long) row[1]);
        }
        return counts;
    }

    @Override
    public List<NotebookPageSample> getBySampleItemId(Integer sampleItemId) {
        Session session = entityManager.unwrap(Session.class);
        String hql = "FROM NotebookPageSample nps " + "LEFT JOIN FETCH nps.notebookPage "
                + "WHERE nps.sampleItemId = :sampleItemId";
        return session.createQuery(hql, NotebookPageSample.class).setParameter("sampleItemId", sampleItemId.toString())
                .getResultList();
    }

    @Override
    public int bulkUpdateStatus(Integer pageId, List<Integer> sampleIds, Status status) {
        if (sampleIds == null || sampleIds.isEmpty()) {
            return 0;
        }

        Session session = entityManager.unwrap(Session.class);
        // Convert Integer IDs to String for sampleItemId comparison (stored as VARCHAR
        // in DB)
        List<String> sampleIdStrings = sampleIds.stream().map(String::valueOf).toList();

        // Use native SQL query for UPDATE statement
        String sql = "UPDATE clinlims.notebook_page_sample SET status = :status " + "WHERE notebook_page_id = :pageId "
                + "AND sample_item_id IN (:sampleIds)";
        return session.createNativeQuery(sql).setParameter("status", status.name()).setParameter("pageId", pageId)
                .setParameterList("sampleIds", sampleIdStrings).executeUpdate();
    }

    @Override
    public int bulkUpdateStatusString(Integer pageId, List<String> sampleIds, Status status) {
        if (sampleIds == null || sampleIds.isEmpty()) {
            return 0;
        }

        Session session = entityManager.unwrap(Session.class);
        // Use native SQL query for UPDATE statement with String IDs directly
        String sql = "UPDATE clinlims.notebook_page_sample SET status = :status " + "WHERE notebook_page_id = :pageId "
                + "AND sample_item_id IN (:sampleIds)";
        return session.createNativeQuery(sql).setParameter("status", status.name()).setParameter("pageId", pageId)
                .setParameterList("sampleIds", sampleIds).executeUpdate();
    }

    @Override
    public List<NotebookPageSample> getByPageIdPaginated(Integer pageId, Status status, int offset, int limit) {
        Session session = entityManager.unwrap(Session.class);
        StringBuilder hql = new StringBuilder();
        hql.append("FROM NotebookPageSample nps ");
        hql.append("WHERE nps.notebookPage.id = :pageId ");
        if (status != null) {
            hql.append("AND nps.status = :status ");
        }
        hql.append("ORDER BY nps.sampleItemId");

        var query = session.createQuery(hql.toString(), NotebookPageSample.class).setParameter("pageId", pageId)
                .setFirstResult(offset).setMaxResults(limit);

        if (status != null) {
            query.setParameter("status", status);
        }

        return query.getResultList();
    }

    @Override
    public long getCountByPageId(Integer pageId, Status status) {
        Session session = entityManager.unwrap(Session.class);
        StringBuilder hql = new StringBuilder();
        hql.append("SELECT COUNT(nps) FROM NotebookPageSample nps ");
        hql.append("WHERE nps.notebookPage.id = :pageId ");
        if (status != null) {
            hql.append("AND nps.status = :status");
        }

        var query = session.createQuery(hql.toString(), Long.class).setParameter("pageId", pageId);

        if (status != null) {
            query.setParameter("status", status);
        }

        return query.getSingleResult();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<NotebookPageSample> getByNotebookId(Integer notebookId) {
        Session session = entityManager.unwrap(Session.class);
        // Use native SQL to avoid Hibernate lazy loading issues with nested
        // relationships
        String sql = "SELECT nps.* FROM clinlims.notebook_page_sample nps "
                + "JOIN clinlims.notebook_page np ON nps.notebook_page_id = np.id "
                + "WHERE np.notebook_id = :notebookId " + "ORDER BY np.page_order, nps.sample_item_id";
        return session.createNativeQuery(sql, NotebookPageSample.class).setParameter("notebookId", notebookId)
                .getResultList();
    }

    @Override
    public NotebookPageSample getBySampleItemIdAndPageId(String sampleItemId, Integer pageId) {
        Session session = entityManager.unwrap(Session.class);
        String hql = "FROM NotebookPageSample nps " + "WHERE nps.sampleItemId = :sampleItemId "
                + "AND nps.notebookPage.id = :pageId";
        List<NotebookPageSample> results = session.createQuery(hql, NotebookPageSample.class)
                .setParameter("sampleItemId", sampleItemId).setParameter("pageId", pageId).getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Map<String, String>> getOccupiedWellsByBoxId(Integer boxId) {
        Map<String, Map<String, String>> result = new HashMap<>();

        Session session = entityManager.unwrap(Session.class);
        // Query for archived samples with this boxId in their JSONB data field
        // JSONB query: data->>'boxId' = :boxId AND data->>'wellCoordinate' IS NOT NULL
        String sql = "SELECT nps.sample_item_id, nps.data->>'wellCoordinate' as well_coordinate, "
                + "nps.data->>'externalId' as external_id " + "FROM clinlims.notebook_page_sample nps "
                + "WHERE (nps.data->>'boxId')::integer = ?1 " + "AND nps.data->>'wellCoordinate' IS NOT NULL "
                + "AND nps.data->>'wellCoordinate' != '' " + "AND (nps.data->>'isArchived')::boolean = true";

        List<Object[]> rows = session.createNativeQuery(sql).setParameter(1, boxId).getResultList();

        for (Object[] row : rows) {
            String sampleItemId = row[0] != null ? row[0].toString() : null;
            String wellCoordinate = row[1] != null ? row[1].toString() : null;
            String externalId = row[2] != null ? row[2].toString() : null;

            if (wellCoordinate != null && !wellCoordinate.isEmpty()) {
                Map<String, String> sampleInfo = new HashMap<>();
                sampleInfo.put("sampleItemId", sampleItemId);
                sampleInfo.put("externalId", externalId);
                result.put(wellCoordinate, sampleInfo);
            }
        }

        return result;
    }
}
