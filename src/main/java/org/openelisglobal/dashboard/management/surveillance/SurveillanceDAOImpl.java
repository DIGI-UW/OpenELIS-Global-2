package org.openelisglobal.dashboard.management.surveillance;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import org.openelisglobal.dashboard.management.ScopeRange;
import org.openelisglobal.reports.qi.QiReportSupport;
import org.springframework.stereotype.Component;

@Component
@SuppressWarnings("unchecked")
public class SurveillanceDAOImpl implements SurveillanceDAO {

    private static final String CODED = "res.result_type IN ('D', 'M', 'C')";

    // A numeric reading may carry "<" or ">" and thousands separators, as viral
    // loads often do. No "?" in the pattern: Hibernate reads it as a parameter.
    private static final String NUMBER = "CASE WHEN NOT " + CODED
            + " AND res.value ~ '^\\s*[<>]{0,1}\\s*-{0,1}[0-9][0-9,]*(\\.[0-9]+){0,1}\\s*$'"
            + " THEN CAST(regexp_replace(res.value, '[<>\\s,]', '', 'g') AS double precision) END";

    // A value can have several options; active ones win, and it is abnormal only
    // when every remaining option says so.
    private static final String OPTIONS = "SELECT is_normal, is_active FROM clinlims.test_result"
            + " WHERE test_id = a.test_id AND value = res.value AND tst_rslt_type IN ('D', 'M', 'C')";

    private static final String CODE_IN = " = ANY(string_to_array(lower(%s), ','))";

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<Object[]> getActivePrograms() {
        return entityManager
                .createNativeQuery("SELECT id, code, display_name, display_order"
                        + " FROM clinlims.surveillance_program WHERE is_active ORDER BY display_order, code")
                .getResultList();
    }

    @Override
    public List<Object[]> getPositivityMetrics() {
        return entityManager
                .createNativeQuery("SELECT program_id, metric_key,"
                        + " (array_agg(metric_label ORDER BY display_order, id))[1], min(display_order)"
                        + " FROM clinlims.test_surveillance_mapping WHERE is_active GROUP BY program_id, metric_key")
                .getResultList();
    }

    @Override
    public List<Object[]> countPositivity(ScopeRange scope) {
        String entry = "lower(d.dict_entry)";
        // Numeric modes also count listed text results; a 0/0 range is no range.
        String listed = "lower(COALESCE(d.dict_entry, res.value))" + String.format(CODE_IN, "m.positive_result_codes");
        String sql = "WITH r AS (SELECT m.program_id, m.metric_key, a.id AS analysis_id,"
                + " CASE WHEN a.released_date >= :from THEN 'C' ELSE 'P' END AS period,"
                + " (m.denominator_result_codes IS NULL OR " + entry
                + String.format(CODE_IN, "m.denominator_result_codes") + ")"
                + " AND (m.match_mode NOT LIKE 'NUMERIC%' OR n.num IS NOT NULL OR COALESCE(" + listed + ", false))"
                + " AS tested," + " COALESCE(CASE m.match_mode" //
                + " WHEN 'SPECIFIC_CODES' THEN " + entry + String.format(CODE_IN, "m.positive_result_codes")
                + " WHEN 'ALL_ABNORMAL' THEN (" + CODED + " AND opt.abnormal)"
                + " OR (res.min_normal <> res.max_normal AND (n.num < res.min_normal OR n.num > res.max_normal))"
                + " WHEN 'NUMERIC_BELOW' THEN n.num < m.numeric_threshold OR " + listed
                + " WHEN 'NUMERIC_ABOVE' THEN n.num > m.numeric_threshold OR " + listed + " END, false) AS positive"
                + " FROM clinlims.test_surveillance_mapping m"
                + " JOIN clinlims.surveillance_program p ON p.id = m.program_id AND p.is_active"
                + " JOIN clinlims.analysis a ON a.test_id = m.test_id"
                + " JOIN clinlims.result res ON res.analysis_id = a.id"
                // result.test_result_id can name a different option than the value chosen
                + " CROSS JOIN LATERAL (SELECT bool_and(COALESCE(NOT o.is_normal, false)) AS abnormal FROM (" + OPTIONS
                + ") o WHERE COALESCE(o.is_active, false) = (SELECT COALESCE(bool_or(o2.is_active), false)" + " FROM ("
                + OPTIONS + ") o2)) opt" //
                + " LEFT JOIN clinlims.dictionary d ON " + CODED
                + " AND d.id = CAST(CASE WHEN res.value ~ '^[0-9]+$' THEN res.value END AS numeric)"
                + " CROSS JOIN LATERAL (SELECT " + NUMBER + " AS num) n"
                + " WHERE m.is_active AND COALESCE(res.value, '') <> ''"
                + " AND a.released_date >= :priorFrom AND a.released_date < :to)"
                + " SELECT program_id, metric_key, period, count(DISTINCT analysis_id) FILTER (WHERE tested) AS tested,"
                + " count(DISTINCT analysis_id) FILTER (WHERE tested AND positive) AS positive FROM r"
                + " GROUP BY GROUPING SETS ((program_id, metric_key, period), (program_id, period))";
        return entityManager.createNativeQuery(sql).setParameter("from", QiReportSupport.startOf(scope.from()))
                .setParameter("priorFrom", QiReportSupport.startOf(scope.priorFrom()))
                .setParameter("to", QiReportSupport.endOf(scope.to())).getResultList();
    }

    @Override
    public List<Object[]> getResistanceMetrics() {
        return entityManager.createNativeQuery("SELECT program_id, metric_key, metric_label, organism_codes,"
                + " organism_group, antibiotic_codes, display_order FROM clinlims.surveillance_resistance_metric"
                + " WHERE is_active").getResultList();
    }

    @Override
    public List<Object[]> getActiveOrganisms() {
        return entityManager
                .createNativeQuery(
                        "SELECT id, whonet_code, organism_group FROM clinlims.micro_organism WHERE is_active = 'Y'")
                .getResultList();
    }

    @Override
    public List<Object[]> getActiveAntibiotics() {
        return entityManager
                .createNativeQuery("SELECT id, whonet_code FROM clinlims.micro_antibiotic WHERE is_active = 'Y'")
                .getResultList();
    }
}
