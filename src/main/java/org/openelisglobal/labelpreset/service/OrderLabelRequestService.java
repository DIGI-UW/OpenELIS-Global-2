package org.openelisglobal.labelpreset.service;

import java.util.List;
import java.util.Map;
import org.openelisglobal.labelpreset.dto.OrderLabelPersistRequest;
import org.openelisglobal.labelpreset.valueholder.OrderLabelRequest;

/**
 * Persists the technician's chosen label quantities at order-save time as
 * {@code order_label_request} rows, each with a frozen JSONB
 * {@link org.openelisglobal.labelpreset.valueholder.PresetSnapshotDto} (OGC-285
 * M5).
 *
 * <p>
 * The snapshot is the authoritative source for reprint (AC-20): it is built
 * from the <em>current</em> {@code label_preset} + linked
 * {@code test_label_preset_link} + {@code test_label_config} state and frozen.
 * Reprint MUST NOT re-fetch those tables.
 *
 * <p>
 * {@code @Transactional} in the implementation. The write joins the caller's
 * order-save transaction so the rows commit/rollback atomically with the
 * sample.
 */
public interface OrderLabelRequestService {

    /**
     * Persist one {@code order_label_request} row per chosen per-order cell and one
     * per chosen (sample, preset) cell.
     *
     * @param orderId              the persisted parent {@code Sample} id (the
     *                             "order")
     * @param sampleIdMap          map of client-supplied {@code sample_id_local} →
     *                             persisted {@code SampleItem} id; per-sample cells
     *                             whose local id is absent here are skipped
     * @param payload              the chosen per-order + per-sample quantities
     * @param sysUserId            audit user id
     * @param testIdsBySampleLocal optional map of {@code sample_id_local} → the
     *                             test ids ordered for that sample; drives the
     *                             snapshot's {@code test_link} block. May be empty.
     * @return the persisted rows (in insertion order)
     */
    List<OrderLabelRequest> persistRequest(String orderId, Map<String, String> sampleIdMap,
            OrderLabelPersistRequest payload, String sysUserId, Map<String, List<String>> testIdsBySampleLocal);
}
