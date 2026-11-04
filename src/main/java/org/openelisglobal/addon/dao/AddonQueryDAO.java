package org.openelisglobal.addon.dao;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

/**
 * Read-only queries over the whole laboratory for add-ons granted
 * {@code orders.search} or {@code stats.read}. Native SQL, because the
 * aggregates (counts per status, percentiles of turnaround time) have no HQL
 * form. Every query is bounded: a date range of at most
 * {@code AddonCatalog.MAX_RANGE_DAYS} and a row limit.
 */
public interface AddonQueryDAO {

    /** Status ids of the analysis states the queries group by. */
    record StatusIds(Collection<Integer> pending, int awaitingValidation, int finalized, Collection<Integer> excluded) {
    }

    record OrderRow(String sampleId, String accessionNumber, java.time.LocalDateTime received, int tests, int pending,
            int awaitingValidation, int finalized, List<String> sections) {
    }

    record Page<T>(int total, List<T> rows) {
    }

    record WorkItem(String accessionNumber, String analysisId, String testCode, String testName, String section,
            java.time.LocalDateTime received) {
    }

    record CountRow(String key, int orders, int tests, int finalized) {
    }

    record TatRow(String key, int count, double medianMinutes, double p90Minutes) {
    }

    record TestRow(String code, String name, String loinc, String section, String sampleType, String unit,
            boolean active) {
    }

    /**
     * Orders received in [from, to] (whole days), newest first.
     *
     * @param status {@code any}, {@code pending}, {@code awaiting-validation} or
     *               {@code finalized}
     */
    Page<OrderRow> orders(LocalDate from, LocalDate to, String status, String section, StatusIds ids, int limit,
            int offset);

    /** Tests in the given states, oldest order first. */
    Page<WorkItem> worklist(Collection<Integer> statusIds, String section, int limit);

    /** @param groupBy {@code day}, {@code section} or {@code status} */
    List<CountRow> counts(LocalDate from, LocalDate to, String groupBy, StatusIds ids);

    /**
     * Minutes from sample received to result released, of results released in
     * [from, to].
     *
     * @param groupBy {@code section} or {@code test}
     */
    List<TatRow> turnaround(LocalDate from, LocalDate to, String groupBy, int finalizedId);

    /** @param active {@code true}, {@code false} or null for both */
    List<TestRow> tests(String section, Boolean active);
}
