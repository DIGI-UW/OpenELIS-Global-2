package org.openelisglobal.batchworkplan.daoimpl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.batchworkplan.dao.BatchWorkplanDAO;
import org.openelisglobal.batchworkplan.form.PendingTestsQuery;
import org.openelisglobal.batchworkplan.valueholder.BatchWorkplan;
import org.openelisglobal.batchworkplan.valueholder.BatchWorkplanStatus;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.sample.valueholder.OrderPriority;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class BatchWorkplanDAOImpl extends BaseDAOImpl<BatchWorkplan, Long> implements BatchWorkplanDAO {

    public BatchWorkplanDAOImpl() {
        super(BatchWorkplan.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BatchWorkplan> getForUserInStatuses(Integer createdByUserId, List<BatchWorkplanStatus> statuses) {
        // An unresolvable caller owns nothing. Answering "every batch" here would
        // undo the point of the predicate.
        if (createdByUserId == null || statuses == null || statuses.isEmpty()) {
            return List.of();
        }
        try {
            String hql = "SELECT DISTINCT b FROM BatchWorkplan b LEFT JOIN FETCH b.items"
                    + " WHERE b.createdByUserId = :createdByUserId AND b.status IN (:statuses)"
                    + " ORDER BY b.createdAt DESC";
            Query<BatchWorkplan> query = entityManager.unwrap(Session.class).createQuery(hql, BatchWorkplan.class);
            query.setParameter("createdByUserId", createdByUserId);
            query.setParameterList("statuses", statuses);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error getting batch workplans", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BatchWorkplan> getWithItems(Long id) {
        try {
            String hql = "SELECT DISTINCT b FROM BatchWorkplan b LEFT JOIN FETCH b.items WHERE b.id = :id";
            Query<BatchWorkplan> query = entityManager.unwrap(Session.class).createQuery(hql, BatchWorkplan.class);
            query.setParameter("id", id);
            query.setMaxResults(1);
            return query.uniqueResultOptional();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error getting batch workplan", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, String> getPatientNamesBySampleIds(Collection<String> sampleIds) {
        Map<String, String> names = new HashMap<>();
        if (sampleIds == null || sampleIds.isEmpty()) {
            return names;
        }
        try {
            String hql = "SELECT sh.sampleId, p.person.lastName, p.person.firstName FROM SampleHuman sh, Patient p"
                    + " WHERE p.id = sh.patientId AND sh.sampleId IN (:sampleIds)";
            Query<Object[]> query = entityManager.unwrap(Session.class).createQuery(hql, Object[].class);
            query.setParameterList("sampleIds", sampleIds);
            for (Object[] row : query.list()) {
                String last = StringUtils.trimToEmpty((String) row[1]);
                String first = StringUtils.trimToEmpty((String) row[2]);
                String name = last.isEmpty() || first.isEmpty() ? last + first : last + ", " + first;
                if (!name.isEmpty()) {
                    names.put(String.valueOf(row[0]), name);
                }
            }
            return names;
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error getting patient names for batch workplan samples", e);
        }
    }

    /** The list's column keys, and what each sorts on. */
    private static final Map<String, String> SORT_COLUMNS = Map.of("accessionNumber", "s.accessionNumber",
            "patientName", "per.lastName", "testName", "t.name", "panelName", "p.panelName", "testSectionName",
            "ts.testSectionName", "priority", "s.priority", "orderDate", "s.enteredDate", "sampleType",
            "tos.description");

    private static final Map<String, String> FILTER_VALUES = Map.of("test", "t.id, t.name", "panel",
            "p.id, p.panelName", "unit", "ts.id, ts.testSectionName", "priority", "s.priority, s.priority");

    @Override
    @Transactional(readOnly = true)
    public List<String> getPendingAnalysisIds(List<String> statusIds, List<String> testIds,
            Collection<String> excludedAnalysisIds, PendingTestsQuery pendingQuery) {
        if (nothingVisible(statusIds, testIds)) {
            return List.of();
        }
        try {
            String order = SORT_COLUMNS.getOrDefault(StringUtils.defaultString(pendingQuery.sort()),
                    "s.accessionNumber");
            String direction = pendingQuery.ascending() ? " ASC" : " DESC";
            String hql = "SELECT a.id, " + order + " " + pendingFrom(excludedAnalysisIds, pendingQuery) + " ORDER BY "
                    + order + direction + " NULLS LAST, a.id";
            Query<Object[]> query = entityManager.unwrap(Session.class).createQuery(hql, Object[].class);
            bindPending(query, statusIds, testIds, excludedAnalysisIds, pendingQuery);
            query.setFirstResult(Math.max(0, pendingQuery.page() - 1) * pendingQuery.pageSize());
            query.setMaxResults(pendingQuery.pageSize());
            List<String> ids = new ArrayList<>();
            for (Object[] row : query.list()) {
                ids.add(String.valueOf(row[0]));
            }
            return ids;
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error getting pending tests for batch workplans", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public long countPendingAnalyses(List<String> statusIds, List<String> testIds,
            Collection<String> excludedAnalysisIds, PendingTestsQuery pendingQuery) {
        if (nothingVisible(statusIds, testIds)) {
            return 0;
        }
        try {
            String hql = "SELECT count(a.id) " + pendingFrom(excludedAnalysisIds, pendingQuery);
            Query<Long> query = entityManager.unwrap(Session.class).createQuery(hql, Long.class);
            bindPending(query, statusIds, testIds, excludedAnalysisIds, pendingQuery);
            return query.uniqueResult();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error counting pending tests for batch workplans", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Object[]> getPendingFilterValues(List<String> statusIds, List<String> testIds,
            Collection<String> excludedAnalysisIds, String field) {
        String columns = FILTER_VALUES.get(field);
        if (columns == null || nothingVisible(statusIds, testIds)) {
            return List.of();
        }
        try {
            String hql = "SELECT DISTINCT " + columns + " " + pendingFrom(excludedAnalysisIds, null);
            Query<Object[]> query = entityManager.unwrap(Session.class).createQuery(hql, Object[].class);
            bindPending(query, statusIds, testIds, excludedAnalysisIds, null);
            List<Object[]> values = new ArrayList<>();
            for (Object[] row : query.list()) {
                if (row[0] != null) {
                    values.add(row);
                }
            }
            return values;
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error getting pending test filters for batch workplans", e);
        }
    }

    /**
     * An empty test list means the user holds no lab unit, which is "sees nothing",
     * not "sees everything".
     */
    private boolean nothingVisible(List<String> statusIds, List<String> testIds) {
        return statusIds == null || statusIds.isEmpty() || testIds == null || testIds.isEmpty();
    }

    private String pendingFrom(Collection<String> excludedAnalysisIds, PendingTestsQuery pendingQuery) {
        StringBuilder hql = new StringBuilder("FROM Analysis a JOIN a.sampleItem si JOIN si.sample s JOIN a.test t"
                + " LEFT JOIN a.testSection ts LEFT JOIN a.panel p LEFT JOIN si.typeOfSample tos"
                + " LEFT JOIN SampleHuman sh ON sh.sampleId = s.id LEFT JOIN Patient pat ON pat.id = sh.patientId"
                + " LEFT JOIN pat.person per WHERE a.statusId IN (:statusIds) AND t.id IN (:testIds)");
        if (excludedAnalysisIds != null && !excludedAnalysisIds.isEmpty()) {
            hql.append(" AND a.id NOT IN (:excludedAnalysisIds)");
        }
        if (pendingQuery == null) {
            return hql.toString();
        }
        if (!isEmpty(pendingQuery.testIds())) {
            hql.append(" AND t.id IN (:filterTestIds)");
        }
        if (!isEmpty(pendingQuery.panelIds())) {
            hql.append(" AND p.id IN (:filterPanelIds)");
        }
        if (!isEmpty(pendingQuery.testSectionIds())) {
            hql.append(" AND ts.id IN (:filterSectionIds)");
        }
        if (!isEmpty(pendingQuery.priorities())) {
            hql.append(" AND s.priority IN (:filterPriorities)");
        }
        if (pendingQuery.fromDate() != null) {
            hql.append(" AND s.enteredDate >= :fromDate");
        }
        if (pendingQuery.toDate() != null) {
            hql.append(" AND s.enteredDate < :toDateExclusive");
        }
        if (StringUtils.isNotBlank(pendingQuery.search())) {
            hql.append(" AND (lower(s.accessionNumber) LIKE :search OR lower(s.accessionNumber) LIKE :labNumber"
                    + " OR lower(t.name) LIKE :search OR lower(t.description) LIKE :search"
                    + " OR lower(p.panelName) LIKE :search OR lower(ts.testSectionName) LIKE :search"
                    + " OR lower(tos.description) LIKE :search OR lower(per.lastName) LIKE :search"
                    + " OR lower(per.firstName) LIKE :search)");
        }
        return hql.toString();
    }

    private void bindPending(Query<?> query, List<String> statusIds, List<String> testIds,
            Collection<String> excludedAnalysisIds, PendingTestsQuery pendingQuery) {
        query.setParameterList("statusIds", statusIds);
        query.setParameterList("testIds", testIds);
        if (excludedAnalysisIds != null && !excludedAnalysisIds.isEmpty()) {
            query.setParameterList("excludedAnalysisIds", excludedAnalysisIds);
        }
        if (pendingQuery == null) {
            return;
        }
        if (!isEmpty(pendingQuery.testIds())) {
            query.setParameterList("filterTestIds", pendingQuery.testIds());
        }
        if (!isEmpty(pendingQuery.panelIds())) {
            query.setParameterList("filterPanelIds", pendingQuery.panelIds());
        }
        if (!isEmpty(pendingQuery.testSectionIds())) {
            query.setParameterList("filterSectionIds", pendingQuery.testSectionIds());
        }
        if (!isEmpty(pendingQuery.priorities())) {
            query.setParameterList("filterPriorities",
                    pendingQuery.priorities().stream().map(OrderPriority::valueOf).collect(Collectors.toList()));
        }
        if (pendingQuery.fromDate() != null) {
            query.setParameter("fromDate", java.sql.Date.valueOf(pendingQuery.fromDate()));
        }
        if (pendingQuery.toDate() != null) {
            query.setParameter("toDateExclusive", java.sql.Date.valueOf(pendingQuery.toDate().plusDays(1)));
        }
        if (StringUtils.isNotBlank(pendingQuery.search())) {
            String needle = pendingQuery.search().trim().toLowerCase();
            query.setParameter("search", "%" + escapeLike(needle) + "%");
            query.setParameter("labNumber", "%" + escapeLike(needle.replaceAll("[^a-z0-9]", "")) + "%");
        }
    }

    private static boolean isEmpty(List<String> values) {
        return values == null || values.isEmpty();
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
