package org.openelisglobal.labelpreset.service;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.openelisglobal.barcode.BarcodeLabelMaker;
import org.openelisglobal.barcode.labeltype.Label;
import org.openelisglobal.barcode.labeltype.SnapshotLabel;
import org.openelisglobal.labelpreset.dao.OrderLabelRequestDAO;
import org.openelisglobal.labelpreset.dto.OrderLabelRequestView;
import org.openelisglobal.labelpreset.valueholder.OrderLabelRequest;
import org.openelisglobal.labelpreset.valueholder.PresetSnapshotDto;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reprint implementation (OGC-285 M6). Reads only from the frozen
 * {@link PresetSnapshotDto} carried on each {@code order_label_request} row —
 * deliberately never autowires {@code LabelPresetDAO} and never calls
 * {@code row.getPreset()} for dimensions/fields, so a future edit that
 * re-introduces a live lookup is impossible to do here without an obvious new
 * dependency. This is the AC-20 enforcement boundary.
 */
@Service
public class OrderLabelReprintServiceImpl implements OrderLabelReprintService {

    @Autowired
    private OrderLabelRequestDAO orderLabelRequestDAO;

    @Override
    @Transactional(readOnly = true)
    public List<OrderLabelRequestView> listByOrder(String orderId) {
        List<OrderLabelRequestView> views = new ArrayList<>();
        if (orderId == null) {
            return views;
        }
        for (OrderLabelRequest row : orderLabelRequestDAO.listByParentSampleId(orderId)) {
            // Built inside the tx so LAZY parentSample/sampleItem/preset resolve to ids.
            views.add(OrderLabelRequestView.from(row));
        }
        return views;
    }

    @Override
    @Transactional(readOnly = true)
    public ByteArrayOutputStream renderFromSnapshot(String orderId, Integer presetId) {
        ArrayList<Label> labels = new ArrayList<>();
        BarcodeLabelMaker.BarcodeType barcodeType = BarcodeLabelMaker.BarcodeType.BARCODE;

        if (orderId != null && presetId != null) {
            for (OrderLabelRequest row : orderLabelRequestDAO.listByParentSampleId(orderId)) {
                if (row.getPreset() == null || !presetId.equals(row.getPreset().getId())) {
                    continue;
                }
                PresetSnapshotDto snapshot = row.getPresetSnapshot();
                if (snapshot == null || snapshot.getPreset() == null) {
                    continue;
                }
                String labNo = resolveBarcodePayload(row);
                SnapshotLabel label = buildSnapshotLabel(snapshot, labNo);
                int qty = row.getQty() == null ? 1 : row.getQty();
                label.setNumLabels(qty);
                labels.add(label);
                // Barcode symbology lives on the maker, not the Label. Apply the
                // snapshot's frozen type; the last matching row wins (rows of the
                // same preset share a symbology).
                barcodeType = mapBarcodeType(snapshot.getPreset().getBarcodeType());
            }
        }

        if (labels.isEmpty()) {
            return new ByteArrayOutputStream();
        }
        // createLabelsAsStream() is the production render path with no DB coupling
        // and no print accounting — exactly what reprint needs.
        BarcodeLabelMaker maker = new BarcodeLabelMaker(labels);
        maker.setBarcodeType(barcodeType);
        return maker.createLabelsAsStream();
    }

    @Override
    @Transactional
    public OrderLabelRequest decreaseQty(Integer requestId, Integer newQty) {
        if (requestId == null) {
            throw new IllegalArgumentException("requestId is required");
        }
        if (newQty == null || newQty < 1) {
            throw new IllegalArgumentException("newQty must be >= 1");
        }
        OrderLabelRequest row = orderLabelRequestDAO.get(requestId)
                .orElseThrow(() -> new IllegalArgumentException("No order_label_request with id " + requestId));
        int saved = row.getQty() == null ? 0 : row.getQty();
        if (newQty > saved) {
            throw new IllegalArgumentException(
                    "qty may only be decreased: requested " + newQty + " exceeds saved " + saved);
        }
        row.setQty(newQty);
        return orderLabelRequestDAO.update(row);
    }

    @Override
    public SnapshotLabel buildSnapshotLabel(PresetSnapshotDto snapshot, String labNo) {
        return new SnapshotLabel(snapshot, labNo);
    }

    /**
     * Barcode payload for the row: per-sample rows append the sample-item sort
     * order ({@code accession.sortOrder}); per-order rows use the bare accession.
     * Falls back to the parent-sample id only if the accession is blank.
     */
    private String resolveBarcodePayload(OrderLabelRequest row) {
        Sample parent = row.getParentSample();
        String accession = parent == null ? null : parent.getAccessionNumber();
        if (StringUtils.isBlank(accession)) {
            accession = parent == null ? "" : StringUtils.defaultString(parent.getId());
        }
        SampleItem item = row.getSampleItem();
        if (item != null && StringUtils.isNotBlank(item.getSortOrder())) {
            return accession + "." + item.getSortOrder();
        }
        return accession;
    }

    /** Map the snapshot's STRING barcode_type to the maker's symbology enum. */
    private BarcodeLabelMaker.BarcodeType mapBarcodeType(String snapshotType) {
        if (snapshotType != null && "QR".equalsIgnoreCase(snapshotType.trim())) {
            return BarcodeLabelMaker.BarcodeType.QR;
        }
        // CODE_128, DATAMATRIX (maker has no datamatrix), null → 1D barcode.
        return BarcodeLabelMaker.BarcodeType.BARCODE;
    }
}
