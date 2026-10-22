package org.openelisglobal.addon.daoimpl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.openelisglobal.addon.dao.AddonQueryDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
public class AddonQueryDAOImpl implements AddonQueryDAO {

    @PersistenceContext
    private EntityManager entityManager;

    private static final String ANALYSES = """
            FROM clinlims.sample s
            JOIN clinlims.sample_item si ON si.samp_id = s.id
            JOIN clinlims.analysis a ON a.sampitem_id = si.id
            LEFT JOIN clinlims.test_section ts ON ts.id = a.test_sect_id
            """;

    @Override
    @SuppressWarnings("unchecked")
    public Page<OrderRow> orders(LocalDate from, LocalDate to, String status, String section, StatusIds ids, int limit,
            int offset) {
        String having = switch (status) {
        case "pending" -> " HAVING count(a.id) FILTER (WHERE a.status_id IN (:pending)) > 0";
        case "awaiting-validation" -> " HAVING count(a.id) FILTER (WHERE a.status_id = :awaiting) > 0";
        case "finalized" ->
            " HAVING count(a.id) > 0 AND count(a.id) = count(a.id) FILTER (WHERE a.status_id = :finalized)";
        default -> " HAVING count(a.id) > 0";
        };
        if (section != null) {
            having += " AND bool_or(ts.name = :section)";
        }
        String sql = "SELECT s.id AS sample_id, s.accession_number AS accession, s.received_date AS received,"
                + " count(a.id) AS tests, count(a.id) FILTER (WHERE a.status_id IN (:pending)) AS pending,"
                + " count(a.id) FILTER (WHERE a.status_id = :awaiting) AS awaiting,"
                + " count(a.id) FILTER (WHERE a.status_id = :finalized) AS finalized,"
                + " string_agg(DISTINCT ts.name, '|') AS sections, count(*) OVER () AS total " + ANALYSES
                + " WHERE s.received_date >= :from AND s.received_date < :to" + " AND a.status_id NOT IN (:excluded)"
                + " GROUP BY s.id, s.accession_number, s.received_date" + having
                + " ORDER BY s.received_date DESC, s.accession_number LIMIT :limit OFFSET :offset";
        try {
            Query q = entityManager.createNativeQuery(sql);
            bindRange(q, from, to);
            bindStatuses(q, ids);
            if (section != null) {
                q.setParameter("section", section);
            }
            q.setParameter("limit", limit).setParameter("offset", offset);
            List<Object[]> rows = q.getResultList();
            List<OrderRow> out = new ArrayList<>();
            int total = 0;
            for (Object[] r : rows) {
                total = ((Number) r[8]).intValue();
                out.add(new OrderRow(String.valueOf(r[0]), (String) r[1], time(r[2]), num(r[3]), num(r[4]), num(r[5]),
                        num(r[6]), r[7] == null ? List.of() : Arrays.asList(((String) r[7]).split("\\|"))));
            }
            if (rows.isEmpty() && offset > 0) {
                total = -1; // unknown past the end; the caller reports offset as total
            }
            return new Page<>(total, out);
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in AddonQuery orders()", e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Page<WorkItem> worklist(Collection<Integer> statusIds, String section, int limit) {
        String sql = "SELECT s.accession_number AS accession, a.id AS analysis_id, t.local_code AS code,"
                + " t.name AS test_name, ts.name AS section, s.received_date AS received, count(*) OVER () AS total "
                + ANALYSES + " JOIN clinlims.test t ON t.id = a.test_id" + " WHERE a.status_id IN (:statuses)"
                + (section != null ? " AND ts.name = :section" : "")
                + " ORDER BY s.received_date NULLS LAST, s.accession_number, a.id LIMIT :limit";
        try {
            Query q = entityManager.createNativeQuery(sql).setParameter("statuses", statusIds).setParameter("limit",
                    limit);
            if (section != null) {
                q.setParameter("section", section);
            }
            List<Object[]> rows = q.getResultList();
            List<WorkItem> out = new ArrayList<>();
            int total = 0;
            for (Object[] r : rows) {
                total = ((Number) r[6]).intValue();
                out.add(new WorkItem((String) r[0], String.valueOf(r[1]), (String) r[2], (String) r[3], (String) r[4],
                        time(r[5])));
            }
            return new Page<>(total, out);
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in AddonQuery worklist()", e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<CountRow> counts(LocalDate from, LocalDate to, String groupBy, StatusIds ids) {
        String key = switch (groupBy) {
        case "section" -> "coalesce(ts.name, '')";
        case "status" -> "CAST(a.status_id AS varchar)";
        default -> "to_char(s.received_date, 'YYYY-MM-DD')";
        };
        String sql = "SELECT " + key + " AS k, count(DISTINCT s.id) AS orders, count(a.id) AS tests,"
                + " count(a.id) FILTER (WHERE a.status_id = :finalized) AS finalized " + ANALYSES
                + " WHERE s.received_date >= :from AND s.received_date < :to AND a.status_id NOT IN (:excluded)"
                + " GROUP BY k ORDER BY k";
        try {
            Query q = entityManager.createNativeQuery(sql);
            bindRange(q, from, to);
            q.setParameter("finalized", ids.finalized()).setParameter("excluded", ids.excluded());
            List<Object[]> rows = q.getResultList();
            List<CountRow> out = new ArrayList<>();
            for (Object[] r : rows) {
                out.add(new CountRow((String) r[0], num(r[1]), num(r[2]), num(r[3])));
            }
            return out;
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in AddonQuery counts()", e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<TatRow> turnaround(LocalDate from, LocalDate to, String groupBy, int finalizedId) {
        String key = "test".equals(groupBy) ? "coalesce(t.local_code, t.name)" : "coalesce(ts.name, '')";
        String sql = "SELECT " + key + " AS k, count(*) AS n, percentile_cont(0.5) WITHIN GROUP (ORDER BY m) AS p50,"
                + " percentile_cont(0.9) WITHIN GROUP (ORDER BY m) AS p90" + " FROM (SELECT a.test_id, a.test_sect_id,"
                + " EXTRACT(EPOCH FROM (a.released_date - s.received_date)) / 60.0 AS m " + ANALYSES
                + " WHERE a.status_id = :finalized AND a.released_date >= :from AND a.released_date < :to"
                + " AND s.received_date IS NOT NULL AND a.released_date >= s.received_date) x"
                + " LEFT JOIN clinlims.test t ON t.id = x.test_id"
                + " LEFT JOIN clinlims.test_section ts ON ts.id = x.test_sect_id GROUP BY k ORDER BY k";
        try {
            Query q = entityManager.createNativeQuery(sql).setParameter("finalized", finalizedId);
            bindRange(q, from, to);
            List<Object[]> rows = q.getResultList();
            List<TatRow> out = new ArrayList<>();
            for (Object[] r : rows) {
                out.add(new TatRow((String) r[0], num(r[1]), ((Number) r[2]).doubleValue(),
                        ((Number) r[3]).doubleValue()));
            }
            return out;
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in AddonQuery turnaround()", e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<TestRow> tests(String section, Boolean active) {
        String sql = "SELECT t.local_code AS code, t.name AS test_name, t.loinc AS loinc, ts.name AS section,"
                + " string_agg(DISTINCT tos.description, '|') AS sample_types, u.name AS unit, t.is_active AS active"
                + " FROM clinlims.test t" + " LEFT JOIN clinlims.test_section ts ON ts.id = t.test_section_id"
                + " LEFT JOIN clinlims.unit_of_measure u ON u.id = t.uom_id"
                + " LEFT JOIN clinlims.sampletype_test st ON st.test_id = t.id"
                + " LEFT JOIN clinlims.type_of_sample tos ON tos.id = st.sample_type_id WHERE 1 = 1"
                + (section != null ? " AND ts.name = :section" : "")
                + (active != null ? " AND (t.is_active = 'Y') = :active" : "")
                + " GROUP BY t.id, t.local_code, t.name, t.loinc, ts.name, u.name, t.is_active"
                + " ORDER BY ts.name, t.name";
        try {
            Query q = entityManager.createNativeQuery(sql);
            if (section != null) {
                q.setParameter("section", section);
            }
            if (active != null) {
                q.setParameter("active", active);
            }
            List<Object[]> rows = q.getResultList();
            List<TestRow> out = new ArrayList<>();
            for (Object[] r : rows) {
                out.add(new TestRow((String) r[0], (String) r[1], (String) r[2], (String) r[3], (String) r[4],
                        (String) r[5], "Y".equals(String.valueOf(r[6]))));
            }
            return out;
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in AddonQuery tests()", e);
        }
    }

    private static void bindRange(Query q, LocalDate from, LocalDate to) {
        q.setParameter("from", Timestamp.valueOf(from.atStartOfDay()));
        q.setParameter("to", Timestamp.valueOf(to.plusDays(1).atStartOfDay()));
    }

    private static void bindStatuses(Query q, StatusIds ids) {
        q.setParameter("pending", ids.pending()).setParameter("awaiting", ids.awaitingValidation())
                .setParameter("finalized", ids.finalized()).setParameter("excluded", ids.excluded());
    }

    private static int num(Object o) {
        return o == null ? 0 : ((Number) o).intValue();
    }

    private static LocalDateTime time(Object o) {
        if (o instanceof Timestamp t) {
            return t.toLocalDateTime();
        }
        if (o instanceof LocalDateTime t) {
            return t;
        }
        return null;
    }
}
