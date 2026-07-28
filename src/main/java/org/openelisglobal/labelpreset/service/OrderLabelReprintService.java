package org.openelisglobal.labelpreset.service;

import java.io.ByteArrayOutputStream;
import java.util.List;
import org.openelisglobal.barcode.labeltype.SnapshotLabel;
import org.openelisglobal.labelpreset.dto.OrderLabelRequestView;
import org.openelisglobal.labelpreset.valueholder.OrderLabelRequest;
import org.openelisglobal.labelpreset.valueholder.PresetSnapshotDto;

/**
 * Reprint service for persisted {@code order_label_request} rows (OGC-285 M6).
 *
 * <p>
 * <b>AC-20 contract — the central invariant of this milestone:</b> every render
 * path here reads dimensions, fields and barcode symbology from the row's
 * frozen {@link PresetSnapshotDto} and MUST NOT re-fetch {@code label_preset}
 * or {@code test_label_preset_link}. A reprint that re-reads the live preset is
 * a defect. The {@code @Transactional(readOnly = true)} implementation resolves
 * LAZY associations and serializes the JSONB inside the transaction.
 */
public interface OrderLabelReprintService {

    /**
     * All persisted label requests for an order (parent sample), projected to a
     * lazy-safe view with the frozen snapshot intact.
     *
     * @param orderId the parent {@code Sample} id (the "order")
     * @return the rows in persistence order (possibly empty)
     */
    List<OrderLabelRequestView> listByOrder(String orderId);

    /**
     * Render a print-ready PDF for every {@code order_label_request} row of the
     * given order and preset, each rendered from its own frozen snapshot at the
     * stored {@code qty}. Dimensions/fields/barcode come from the snapshot only.
     *
     * @param orderId  the parent {@code Sample} id
     * @param presetId the preset whose rows should be rendered
     * @return a PDF stream; empty if no matching rows exist
     */
    ByteArrayOutputStream renderFromSnapshot(String orderId, Integer presetId);

    /**
     * Decrease-only quantity update. Allows lowering {@code qty} to the given value
     * (or leaving it unchanged); rejects any value greater than the currently saved
     * qty.
     *
     * @param requestId the {@code order_label_request} id
     * @param newQty    the requested quantity (must be {@code >= 1} and {@code <=}
     *                  the saved qty)
     * @return the updated row
     * @throws IllegalArgumentException if the row is missing, or the new qty is not
     *                                  a decrease (or equal)
     */
    OrderLabelRequest decreaseQty(Integer requestId, Integer newQty);

    /**
     * Build the hermetic {@link SnapshotLabel} for a snapshot. Exposed so tests can
     * assert the label frame (dimensions) comes from the snapshot, NOT the live
     * preset (AC-20). Does not touch the database.
     *
     * @param snapshot the frozen snapshot
     * @param labNo    the barcode payload
     * @return the label
     */
    SnapshotLabel buildSnapshotLabel(PresetSnapshotDto snapshot, String labNo);
}
