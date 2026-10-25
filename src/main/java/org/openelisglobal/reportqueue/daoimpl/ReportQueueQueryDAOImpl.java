package org.openelisglobal.reportqueue.daoimpl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.reportqueue.dao.ReleasedNumericResultRecord;
import org.openelisglobal.reportqueue.dao.ReportQueueCriteria;
import org.openelisglobal.reportqueue.dao.ReportQueueQueryDAO;
import org.openelisglobal.reportqueue.dao.ReportQueueRowRecord;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * One native query computes, per patient order with a released result, the
 * counts, the latest release, and against the last printed version whether
 * content was added, changed or removed. Those decide Unprinted/Printed and
 * Partial/Final/Amended (FRS BR-3), so filters, paging and tile counts all
 * agree because they read the same rows. A printed result that is back awaiting
 * validation holds the order as it was printed: nothing counts as changed until
 * the correction is validated (OGC-1529).
 */
@Component
@Transactional(readOnly = true)
public class ReportQueueQueryDAOImpl implements ReportQueueQueryDAO {

    @PersistenceContext
    private EntityManager entityManager;

    private static final String ROWS_CTE = """
            WITH scope AS (
              SELECT s.id AS sample_id, s.accession_number,
                     COUNT(*) FILTER (WHERE a.status_id = :finalized) AS released_count,
                     COUNT(*) AS total_count,
                     MAX(a.released_date) FILTER (WHERE a.status_id = :finalized) AS latest_release,
                     BOOL_OR(a.corrected = true AND a.status_id = :finalized) AS any_corrected,
                     (SELECT sh.patient_id FROM clinlims.sample_human sh WHERE sh.samp_id = s.id ORDER BY sh.id LIMIT 1) AS patient_id
              FROM clinlims.sample s
              JOIN clinlims.sample_item si ON si.samp_id = s.id
              JOIN clinlims.analysis a ON a.sampitem_id = si.id
              WHERE a.status_id NOT IN (:excluded)
                AND EXISTS (SELECT 1 FROM clinlims.sample_human shx WHERE shx.samp_id = s.id)
                %SECTION_SCOPE%
              GROUP BY s.id, s.accession_number
              HAVING COUNT(*) FILTER (WHERE a.status_id = :finalized) > 0
            ),
            lastv AS (
              SELECT DISTINCT ON (v.sample_id) v.id, v.sample_id, v.version, v.amended, v.legacy, v.issued_at
              FROM clinlims.report_version v
              WHERE v.report_type = 'PATIENT'
              ORDER BY v.sample_id, v.version DESC
            ),
            state AS (
              SELECT sc.*, lv.id AS version_id, lv.version AS version_number,
                     COALESCE(lv.legacy, false) AS legacy, lv.issued_at,
                     COALESCE(lv.amended, false) AS version_amended,
                     (lv.id IS NOT NULL AND NOT lv.legacy AND EXISTS (
                        SELECT 1 FROM clinlims.report_version_item i
                        JOIN clinlims.analysis a2 ON a2.id = i.analysis_id
                        JOIN clinlims.test t2 ON t2.id = a2.test_id
                        WHERE i.report_version_id = lv.id
                          AND (a2.status_id IN (:excluded)
                               OR (a2.status_id = :finalized
                                   AND (COALESCE((SELECT string_agg(r.value, '|' ORDER BY r.id)
                                                  FROM clinlims.result r WHERE r.analysis_id = a2.id), '')
                                        <> COALESCE(i.printed_value, '')
                                        OR COALESCE(CAST(t2.uom_id AS TEXT), '') <> COALESCE(i.printed_unit, '')))))) AS changed_or_removed,
                     (lv.id IS NOT NULL AND NOT lv.legacy AND EXISTS (
                        SELECT 1 FROM clinlims.report_version_item iw
                        JOIN clinlims.analysis aw ON aw.id = iw.analysis_id
                        WHERE iw.report_version_id = lv.id
                          AND aw.status_id <> :finalized AND aw.status_id NOT IN (:excluded))) AS awaiting_validation,
                     (lv.id IS NOT NULL AND NOT lv.legacy AND EXISTS (
                        SELECT 1 FROM clinlims.analysis a3
                        JOIN clinlims.sample_item si3 ON si3.id = a3.sampitem_id
                        WHERE si3.samp_id = sc.sample_id AND a3.status_id = :finalized
                          AND NOT EXISTS (SELECT 1 FROM clinlims.report_version_item i2
                                          WHERE i2.report_version_id = lv.id AND i2.analysis_id = a3.id))) AS added
              FROM scope sc
              LEFT JOIN lastv lv ON lv.sample_id = sc.sample_id
            ),
            rows AS (
              SELECT st.*,
                     (st.version_id IS NULL
                      OR (st.legacy AND st.latest_release > st.issued_at)
                      OR (NOT st.awaiting_validation AND (st.added OR st.changed_or_removed))) AS unprinted,
                     (st.version_amended OR (NOT st.awaiting_validation AND st.changed_or_removed)
                      OR (st.legacy AND st.any_corrected)) AS amended,
                     (st.released_count < st.total_count) AS partial
              FROM state st
            )
            """;

    private static final String SELECT_COLUMNS = """
            SELECT r.sample_id, r.accession_number, r.released_count, r.total_count, r.latest_release,
                   r.version_id, r.version_number, r.legacy, r.unprinted, r.amended, r.partial,
                   r.patient_id, pp.last_name, pp.first_name, p.national_id,
                   (SELECT string_agg(o.name, ' / ' ORDER BY o.id) FROM clinlims.sample_requester sr
                      JOIN clinlims.organization o ON o.id = sr.requester_id
                      WHERE sr.sample_id = r.sample_id AND sr.requester_type_id = :orgType
                        AND NOT EXISTS (SELECT 1 FROM clinlims.sample_requester sr0
                                        WHERE sr0.sample_id = r.sample_id AND sr0.requester_type_id = :orgType
                                          AND sr0.requester_id = o.org_id)) AS facility_name,
                   (SELECT string_agg(o.name, ' / ' ORDER BY o.id) FROM clinlims.sample_requester sr
                      JOIN clinlims.organization o ON o.id = sr.requester_id
                      WHERE sr.sample_id = r.sample_id AND sr.requester_type_id = :orgType
                        AND EXISTS (SELECT 1 FROM clinlims.sample_requester sr0
                                    WHERE sr0.sample_id = r.sample_id AND sr0.requester_type_id = :orgType
                                      AND sr0.requester_id = o.org_id)) AS ward_name,
                   (SELECT string_agg(TRIM(BOTH ', ' FROM COALESCE(rp.last_name, '') || ', ' || COALESCE(rp.first_name, '')), ' / ' ORDER BY rp.id)
                      FROM clinlims.sample_requester sr2
                      JOIN clinlims.person rp ON rp.id = sr2.requester_id
                      WHERE sr2.sample_id = r.sample_id AND sr2.requester_type_id = :providerType) AS requester_name,
                   r.awaiting_validation
            FROM rows r
            LEFT JOIN clinlims.patient p ON p.id = r.patient_id
            LEFT JOIN clinlims.person pp ON pp.id = p.person_id
            """;

    @Override
    public List<ReportQueueRowRecord> list(ReportQueueCriteria criteria) {
        Map<String, Object> params = new HashMap<>();
        String sql = ROWS_CTE.replace("%SECTION_SCOPE%", sectionScope(criteria, params)) + SELECT_COLUMNS
                + " WHERE 1 = 1 " + filters(criteria, params, true) + orderBy(criteria)
                + " LIMIT :limit OFFSET :offset";
        params.put("limit", criteria.getPageSize());
        params.put("offset", Math.max(0, criteria.getPage() - 1) * criteria.getPageSize());
        try {
            Query query = bind(sql, criteria, params);
            @SuppressWarnings("unchecked")
            List<Object[]> raw = query.getResultList();
            List<ReportQueueRowRecord> rows = new ArrayList<>();
            for (Object[] columns : raw) {
                rows.add(toRecord(columns));
            }
            return rows;
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ReportQueueQuery list()", e);
        }
    }

    @Override
    public long count(ReportQueueCriteria criteria) {
        Map<String, Object> params = new HashMap<>();
        String sql = ROWS_CTE.replace("%SECTION_SCOPE%", sectionScope(criteria, params))
                + " SELECT COUNT(*) FROM rows r WHERE 1 = 1 " + filters(criteria, params, true);
        try {
            return toLong(bind(sql, criteria, params).getSingleResult());
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ReportQueueQuery count()", e);
        }
    }

    @Override
    public long[] tiles(ReportQueueCriteria criteria) {
        Map<String, Object> params = new HashMap<>();
        String sql = ROWS_CTE.replace("%SECTION_SCOPE%", sectionScope(criteria, params))
                + " SELECT COUNT(*) FILTER (WHERE r.unprinted) AS to_print,"
                + " COUNT(*) FILTER (WHERE r.unprinted AND r.amended) AS amended_to_print" + " FROM rows r WHERE 1 = 1 "
                + filters(criteria, params, false);
        try {
            Object[] counts = (Object[]) bind(sql, criteria, params).getSingleResult();
            return new long[] { toLong(counts[0]), toLong(counts[1]) };
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ReportQueueQuery tiles()", e);
        }
    }

    @Override
    public Optional<ReportQueueRowRecord> findRow(Long sampleId, ReportQueueCriteria criteria) {
        Map<String, Object> params = new HashMap<>();
        String sql = ROWS_CTE.replace("%SECTION_SCOPE%", "") + SELECT_COLUMNS + " WHERE r.sample_id = :sampleId";
        params.put("sampleId", sampleId);
        try {
            Query query = bind(sql, criteria, params);
            @SuppressWarnings("unchecked")
            List<Object[]> raw = query.getResultList();
            return raw.isEmpty() ? Optional.empty() : Optional.of(toRecord(raw.get(0)));
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ReportQueueQuery findRow()", e);
        }
    }

    private static final String PARTIES_SQL = """
            SELECT (SELECT string_agg(o.name, ' / ' ORDER BY o.id) FROM clinlims.sample_requester sr
                      JOIN clinlims.organization o ON o.id = sr.requester_id
                      WHERE sr.sample_id = :sampleId AND sr.requester_type_id = :orgType
                        AND NOT EXISTS (SELECT 1 FROM clinlims.sample_requester sr0
                                        WHERE sr0.sample_id = :sampleId AND sr0.requester_type_id = :orgType
                                          AND sr0.requester_id = o.org_id)) AS facility_name,
                   (SELECT string_agg(o.name, ' / ' ORDER BY o.id) FROM clinlims.sample_requester sr
                      JOIN clinlims.organization o ON o.id = sr.requester_id
                      WHERE sr.sample_id = :sampleId AND sr.requester_type_id = :orgType
                        AND EXISTS (SELECT 1 FROM clinlims.sample_requester sr0
                                    WHERE sr0.sample_id = :sampleId AND sr0.requester_type_id = :orgType
                                      AND sr0.requester_id = o.org_id)) AS ward_name,
                   (SELECT string_agg(TRIM(BOTH ', ' FROM COALESCE(rp.last_name, '') || ', ' || COALESCE(rp.first_name, '')), ' / ' ORDER BY rp.id)
                      FROM clinlims.sample_requester sr2
                      JOIN clinlims.person rp ON rp.id = sr2.requester_id
                      WHERE sr2.sample_id = :sampleId AND sr2.requester_type_id = :providerType) AS requester_name
            """;

    @Override
    public Optional<ReportQueueRowRecord> findParties(Long sampleId, ReportQueueCriteria criteria) {
        try {
            Query query = entityManager.createNativeQuery(PARTIES_SQL);
            query.setParameter("sampleId", sampleId);
            query.setParameter("orgType", BigDecimal.valueOf(criteria.getOrganizationRequesterTypeId()));
            query.setParameter("providerType", BigDecimal.valueOf(criteria.getProviderRequesterTypeId()));
            Object[] raw = (Object[]) query.getSingleResult();
            ReportQueueRowRecord record = new ReportQueueRowRecord();
            record.setSampleId(sampleId);
            record.setFacilityName((String) raw[0]);
            record.setWardName((String) raw[1]);
            record.setRequesterName((String) raw[2]);
            return Optional.of(record);
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ReportQueueQuery findParties()", e);
        }
    }

    private static final int PRINT_LOCK_NAMESPACE = 431;

    @Override
    public void lockForPrint(Long sampleId) {
        try {
            entityManager.createNativeQuery("SELECT CAST(pg_advisory_xact_lock(:namespace, :sampleId) AS text)")
                    .setParameter("namespace", PRINT_LOCK_NAMESPACE).setParameter("sampleId", sampleId.intValue())
                    .getSingleResult();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ReportQueueQuery lockForPrint()", e);
        }
    }

    @Override
    public boolean isPrinted(Long sampleId, String finalizedStatusId, List<String> excludedStatusIds) {
        ReportQueueCriteria criteria = new ReportQueueCriteria();
        criteria.setFinalizedStatusId(finalizedStatusId);
        criteria.setExcludedStatusIds(excludedStatusIds);
        Map<String, Object> params = new HashMap<>();
        params.put("sampleId", sampleId);
        String sql = ROWS_CTE.replace("%SECTION_SCOPE%", "")
                + " SELECT r.unprinted FROM rows r WHERE r.sample_id = :sampleId";
        try {
            @SuppressWarnings("unchecked")
            List<Object> raw = bind(sql, criteria, params).getResultList();
            return !raw.isEmpty() && !Boolean.TRUE.equals(raw.get(0));
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ReportQueueQuery isPrinted()", e);
        }
    }

    @Override
    public boolean isInScope(Long sampleId, ReportQueueCriteria criteria) {
        if (criteria.getSectionIds() == null) {
            return true;
        }
        if (criteria.getSectionIds().isEmpty()) {
            return false;
        }
        try {
            Object count = entityManager
                    .createNativeQuery("SELECT COUNT(*) FROM clinlims.analysis ax"
                            + " JOIN clinlims.sample_item six ON six.id = ax.sampitem_id"
                            + " WHERE six.samp_id = :sampleId AND ax.test_sect_id IN (:sections)")
                    .setParameter("sampleId", sampleId).setParameter("sections", toLongs(criteria.getSectionIds()))
                    .getSingleResult();
            return toLong(count) > 0;
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ReportQueueQuery isInScope()", e);
        }
    }

    private static final String FIRST_RESULTS_SQL = """
            SELECT * FROM (
              SELECT DISTINCT ON (a.id) si.samp_id, a.id AS analysis_id, a.test_id, si.typeosamp_id,
                     (SELECT sh.patient_id FROM clinlims.sample_human sh WHERE sh.samp_id = si.samp_id
                        ORDER BY sh.id LIMIT 1) AS patient_id,
                     tr.component_id, r.value, r.result_type
              FROM clinlims.result r
              JOIN clinlims.analysis a ON a.id = r.analysis_id
              JOIN clinlims.sample_item si ON si.id = a.sampitem_id
              LEFT JOIN clinlims.test_result tr ON tr.id = r.test_result_id
              WHERE si.samp_id IN (:sampleIds) AND a.status_id = :finalized
              ORDER BY a.id, r.id
            ) first
            WHERE first.result_type = 'N' AND first.value IS NOT NULL AND first.value <> ''
            ORDER BY first.analysis_id
            """;

    @Override
    public List<ReleasedNumericResultRecord> releasedNumericResults(List<Long> sampleIds, String finalizedStatusId) {
        if (sampleIds == null || sampleIds.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            @SuppressWarnings("unchecked")
            List<Object[]> raw = entityManager.createNativeQuery(FIRST_RESULTS_SQL).setParameter("sampleIds", sampleIds)
                    .setParameter("finalized", new BigDecimal(finalizedStatusId)).getResultList();
            List<ReleasedNumericResultRecord> records = new ArrayList<>();
            for (Object[] c : raw) {
                ReleasedNumericResultRecord record = new ReleasedNumericResultRecord();
                record.setSampleId(toLong(c[0]));
                record.setAnalysisId(toLong(c[1]));
                record.setTestId(idOrNull(c[2]));
                record.setSampleTypeId(idOrNull(c[3]));
                record.setPatientId(idOrNull(c[4]));
                record.setComponentId(idOrNull(c[5]));
                record.setValue((String) c[6]);
                records.add(record);
            }
            return records;
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ReportQueueQuery releasedNumericResults()", e);
        }
    }

    /**
     * Numeric ids without a scale; a component id is a UUID and comes back as text.
     */
    private static String idOrNull(Object value) {
        if (value == null) {
            return null;
        }
        return value instanceof Number ? String.valueOf(((Number) value).longValue()) : value.toString();
    }

    private String sectionScope(ReportQueueCriteria criteria, Map<String, Object> params) {
        if (criteria.getSectionIds() == null) {
            return "";
        }
        if (criteria.getSectionIds().isEmpty()) {
            return " AND 1 = 0 ";
        }
        params.put("sections", toLongs(criteria.getSectionIds()));
        return " AND EXISTS (SELECT 1 FROM clinlims.analysis ax JOIN clinlims.sample_item six ON six.id = ax.sampitem_id"
                + " WHERE six.samp_id = s.id AND ax.test_sect_id IN (:sections)) ";
    }

    private String filters(ReportQueueCriteria criteria, Map<String, Object> params, boolean withStateFilters) {
        StringBuilder where = new StringBuilder();
        boolean lookup = criteria.isLookup();
        if (criteria.getLabNo() != null && !criteria.getLabNo().isBlank()) {
            if (criteria.getLabNoTo() != null && !criteria.getLabNoTo().isBlank()) {
                where.append(" AND r.accession_number BETWEEN :labNo AND :labNoTo ");
                params.put("labNo", criteria.getLabNo().trim());
                params.put("labNoTo", criteria.getLabNoTo().trim());
            } else {
                where.append(" AND r.accession_number = :labNo ");
                params.put("labNo", criteria.getLabNo().trim());
            }
        }
        if (criteria.getPatientId() != null && !criteria.getPatientId().isBlank()) {
            where.append(" AND r.patient_id = :patientId ");
            params.put("patientId", new BigDecimal(criteria.getPatientId().trim()));
        }
        if (!lookup) {
            if (criteria.getInstallDate() != null) {
                where.append(" AND NOT (r.version_id IS NULL AND r.latest_release < :installDate) ");
                params.put("installDate", criteria.getInstallDate());
            }
            if (criteria.getWindowFrom() != null) {
                where.append(" AND (r.unprinted OR r.latest_release >= :windowFrom) ");
                params.put("windowFrom", criteria.getWindowFrom());
            }
            if (criteria.getWindowTo() != null) {
                where.append(" AND (r.unprinted OR r.latest_release < :windowTo) ");
                params.put("windowTo", criteria.getWindowTo());
            }
            if (!criteria.getFacilityIds().isEmpty()) {
                where.append(
                        " AND EXISTS (SELECT 1 FROM clinlims.sample_requester srf WHERE srf.sample_id = r.sample_id"
                                + " AND srf.requester_type_id = :orgType AND srf.requester_id IN (:facilityIds)) ");
                params.put("facilityIds", criteria.getFacilityIds());
            }
            if (!criteria.getWardIds().isEmpty()) {
                where.append(
                        " AND EXISTS (SELECT 1 FROM clinlims.sample_requester srw WHERE srw.sample_id = r.sample_id"
                                + " AND srw.requester_type_id = :orgType AND srw.requester_id IN (:wardIds)) ");
                params.put("wardIds", criteria.getWardIds());
            }
            if (!criteria.getRequesterIds().isEmpty()) {
                where.append(
                        " AND EXISTS (SELECT 1 FROM clinlims.sample_requester srp WHERE srp.sample_id = r.sample_id"
                                + " AND srp.requester_type_id = :providerType AND srp.requester_id IN (:requesterIds)) ");
                params.put("requesterIds", criteria.getRequesterIds());
            }
        }
        if (withStateFilters) {
            if (!criteria.getStates().isEmpty()) {
                where.append(" AND (CASE WHEN r.amended THEN 'AMENDED' WHEN r.partial THEN 'PARTIAL' ELSE 'FINAL' END)"
                        + " IN (:states) ");
                params.put("states", criteria.getStates());
            }
            if ("UNPRINTED".equals(criteria.getPrintStatus())) {
                where.append(" AND r.unprinted = true ");
            } else if ("PRINTED".equals(criteria.getPrintStatus())) {
                where.append(" AND r.unprinted = false ");
            }
        }
        return where.toString();
    }

    private String orderBy(ReportQueueCriteria criteria) {
        String direction = criteria.isSortDescending() ? " DESC" : " ASC";
        String nulls = criteria.isSortDescending() ? " NULLS LAST" : " NULLS FIRST";
        String primary;
        switch (criteria.getSortKey() == null ? ReportQueueCriteria.SortKey.RELEASED : criteria.getSortKey()) {
        case LAB_NO:
            primary = "r.accession_number" + direction;
            break;
        case SUBJECT:
            primary = "pp.last_name" + direction + nulls + ", pp.first_name" + direction + nulls;
            break;
        case STATE:
            primary = "(CASE WHEN r.amended THEN 0 WHEN r.partial THEN 1 ELSE 2 END)" + direction;
            break;
        case PRINT_STATUS:
            primary = "r.unprinted" + direction;
            break;
        case RELEASED:
        default:
            primary = "r.latest_release" + direction + nulls;
            break;
        }
        return " ORDER BY " + primary + ", r.latest_release DESC NULLS LAST, r.sample_id DESC";
    }

    private Query bind(String sql, ReportQueueCriteria criteria, Map<String, Object> params) {
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("finalized", new BigDecimal(criteria.getFinalizedStatusId()));
        query.setParameter("excluded", toDecimals(criteria.getExcludedStatusIds()));
        if (sql.contains(":orgType")) {
            query.setParameter("orgType", BigDecimal.valueOf(criteria.getOrganizationRequesterTypeId()));
        }
        if (sql.contains(":providerType")) {
            query.setParameter("providerType", BigDecimal.valueOf(criteria.getProviderRequesterTypeId()));
        }
        for (Map.Entry<String, Object> param : params.entrySet()) {
            query.setParameter(param.getKey(), param.getValue());
        }
        return query;
    }

    private static List<BigDecimal> toDecimals(List<String> ids) {
        List<BigDecimal> decimals = new ArrayList<>();
        for (String id : ids) {
            decimals.add(new BigDecimal(id));
        }
        if (decimals.isEmpty()) {
            decimals.add(BigDecimal.valueOf(-1));
        }
        return decimals;
    }

    private static List<BigDecimal> toLongs(List<String> ids) {
        return toDecimals(ids);
    }

    private static ReportQueueRowRecord toRecord(Object[] c) {
        ReportQueueRowRecord row = new ReportQueueRowRecord();
        row.setSampleId(toLong(c[0]));
        row.setAccessionNumber((String) c[1]);
        row.setReleasedCount((int) toLong(c[2]));
        row.setTotalCount((int) toLong(c[3]));
        row.setLatestRelease(toOffsetDateTime(c[4]));
        row.setVersionId(c[5] == null ? null : toLong(c[5]));
        row.setVersionNumber(c[6] == null ? null : (int) toLong(c[6]));
        row.setLegacy(Boolean.TRUE.equals(c[7]));
        row.setUnprinted(Boolean.TRUE.equals(c[8]));
        row.setAmended(Boolean.TRUE.equals(c[9]));
        row.setPartial(Boolean.TRUE.equals(c[10]));
        row.setPatientId(c[11] == null ? null : String.valueOf(toLong(c[11])));
        row.setPatientLastName((String) c[12]);
        row.setPatientFirstName((String) c[13]);
        row.setPatientNationalId((String) c[14]);
        row.setFacilityName((String) c[15]);
        row.setWardName((String) c[16]);
        row.setRequesterName((String) c[17]);
        row.setAwaitingValidation(Boolean.TRUE.equals(c[18]));
        return row;
    }

    private static long toLong(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return Long.parseLong(value.toString());
    }

    private static OffsetDateTime toOffsetDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof OffsetDateTime) {
            return (OffsetDateTime) value;
        }
        if (value instanceof Instant) {
            return ((Instant) value).atZone(ZoneId.systemDefault()).toOffsetDateTime();
        }
        if (value instanceof Timestamp) {
            return ((Timestamp) value).toInstant().atZone(ZoneId.systemDefault()).toOffsetDateTime();
        }
        if (value instanceof java.util.Date) {
            return ((java.util.Date) value).toInstant().atZone(ZoneId.systemDefault()).toOffsetDateTime();
        }
        return OffsetDateTime.parse(value.toString());
    }
}
