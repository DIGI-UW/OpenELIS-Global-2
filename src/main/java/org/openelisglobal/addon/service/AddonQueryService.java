package org.openelisglobal.addon.service;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

/**
 * Laboratory-wide reads for add-ons: order lists and worklists
 * ({@code orders.search}), patients by name or identifier
 * ({@code patient.identity}), aggregates ({@code stats.read}) and the test
 * catalog ({@code catalog.read}). Scope checks and argument limits are the
 * controller's; this service assumes both were done.
 */
public interface AddonQueryService {

    Map<String, Object> orders(LocalDate from, LocalDate to, String status, String section, int limit, int offset,
            boolean withPatients);

    Map<String, Object> worklist(String stage, String section, int limit);

    Map<String, Object> orderCounts(LocalDate from, LocalDate to, String groupBy);

    Map<String, Object> turnaround(LocalDate from, LocalDate to, String groupBy);

    Map<String, Object> tests(String section, Boolean active);

    /** At most {@code AddonCatalog.MAX_PATIENT_MATCHES} patients. */
    Map<String, Object> findPatients(String query);

    Optional<Map<String, Object>> patient(String patientId);

    Optional<Map<String, Object>> patientOrders(String patientId, int limit);

    /** The patient of an order, with names and identifiers. */
    Optional<Map<String, Object>> patientOfOrder(String accessionNumber);
}
