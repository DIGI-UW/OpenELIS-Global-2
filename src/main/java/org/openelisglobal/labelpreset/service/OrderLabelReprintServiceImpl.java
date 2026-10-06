package org.openelisglobal.labelpreset.service;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.barcode.BarcodeLabelMaker;
import org.openelisglobal.barcode.labeltype.Label;
import org.openelisglobal.barcode.labeltype.SnapshotLabel;
import org.openelisglobal.common.services.RequesterService;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.labelpreset.dao.OrderLabelRequestDAO;
import org.openelisglobal.labelpreset.dto.OrderLabelRequestView;
import org.openelisglobal.labelpreset.valueholder.LabelFieldKey;
import org.openelisglobal.labelpreset.valueholder.OrderLabelRequest;
import org.openelisglobal.labelpreset.valueholder.PresetSnapshotDto;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.person.valueholder.Person;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.test.service.TestServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
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

    @Autowired
    @Lazy
    private SampleHumanService sampleHumanService;

    @Autowired
    @Lazy
    private AnalysisService analysisService;

    @Autowired
    @Lazy
    private SampleItemService sampleItemService;

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
        return renderFromSnapshot(orderId, presetId, null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public ByteArrayOutputStream renderFromSnapshot(String orderId, Integer presetId, String sampleItemId,
            String scope) {
        return renderFromSnapshot(orderId, presetId, sampleItemId, scope, null);
    }

    @Override
    @Transactional(readOnly = true)
    public ByteArrayOutputStream renderFromSnapshot(String orderId, Integer presetId, String sampleItemId, String scope,
            Integer quantity) {
        if (quantity != null && quantity < 1) {
            throw new IllegalArgumentException("quantity must be at least 1");
        }
        ArrayList<Label> labels = new ArrayList<>();
        BarcodeLabelMaker.BarcodeType barcodeType = BarcodeLabelMaker.BarcodeType.BARCODE;

        if (orderId != null) {
            for (OrderLabelRequest row : orderLabelRequestDAO.listByParentSampleId(orderId)) {
                if (!matches(row, presetId, sampleItemId, scope)) {
                    continue;
                }
                PresetSnapshotDto snapshot = row.getPresetSnapshot();
                if (snapshot == null || snapshot.getPreset() == null) {
                    continue;
                }
                String labNo = resolveBarcodePayload(row);
                SnapshotLabel label = new SnapshotLabel(snapshot, labNo, fieldValues(row));
                int qty = row.getQty() == null ? 1 : row.getQty();
                if (quantity != null) {
                    int max = maximumFor(row);
                    if (quantity > max) {
                        throw new IllegalArgumentException(
                                "quantity " + quantity + " exceeds the preset maximum of " + max);
                    }
                    qty = quantity;
                }
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

    /**
     * The preset's maximum for the label's scope, read from the live preset
     * (FR-I5).
     */
    static int maximumFor(OrderLabelRequest row) {
        if (row.getPreset() == null) {
            return Integer.MAX_VALUE;
        }
        Integer max = row.getSampleItem() == null ? row.getPreset().getMaxPerOrder()
                : row.getPreset().getMaxPerSample();
        return max == null ? Integer.MAX_VALUE : max;
    }

    private boolean matches(OrderLabelRequest row, Integer presetId, String sampleItemId, String scope) {
        if (presetId != null && (row.getPreset() == null || !presetId.equals(row.getPreset().getId()))) {
            return false;
        }
        boolean perOrder = row.getSampleItem() == null;
        if ("order".equalsIgnoreCase(scope) && !perOrder) {
            return false;
        }
        if ("sample".equalsIgnoreCase(scope) && perOrder) {
            return false;
        }
        if (sampleItemId != null && (perOrder || !sampleItemId.equals(row.getSampleItem().getId()))) {
            return false;
        }
        return true;
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
     * What the order knows at print time, by field key (OGC-1218): the patient from
     * the order, the requesting site, and for a specimen label the tube's
     * collection, collector and type; the tests are the tube's, or the whole
     * order's on an order label. A field the order cannot fill (pathology or
     * storage detail) is left out and prints as a line to write on.
     */
    Map<String, String> fieldValues(OrderLabelRequest row) {
        Map<String, String> values = new HashMap<>();
        Sample sample = row.getParentSample();
        if (sample == null) {
            return values;
        }
        SampleItem sampleItem = row.getSampleItem();
        Patient patient = sampleHumanService.getPatientForSample(sample);
        if (patient != null) {
            Person person = patient.getPerson();
            if (person != null) {
                String name = (StringUtils.defaultString(person.getLastName()) + ", "
                        + StringUtils.defaultString(person.getFirstName())).trim();
                values.put(LabelFieldKey.PATIENT_NAME.name(),
                        ",".equals(name) ? "" : StringUtils.substring(name.replaceAll("( )+", " "), 0, 30));
            }
            values.put(LabelFieldKey.PATIENT_ID.name(),
                    StringUtils.defaultString(StringUtils.isNotBlank(patient.getNationalId()) ? patient.getNationalId()
                            : patient.getExternalId()));
            values.put(LabelFieldKey.PATIENT_DOB.name(), StringUtils.defaultString(patient.getBirthDateForDisplay()));
            values.put(LabelFieldKey.PATIENT_SEX.name(), StringUtils.defaultString(patient.getGender()));
        }
        Organization requester = new RequesterService(sample.getId()).getOrganization();
        if (requester != null) {
            values.put(LabelFieldKey.SITE_ID.name(), StringUtils.defaultString(requester.getOrganizationName()));
        }
        if (sampleItem != null) {
            if (sampleItem.getCollectionDate() != null) {
                values.put(LabelFieldKey.COLLECTION_DATETIME.name(),
                        DateUtil.convertTimestampToStringDateAndTime(sampleItem.getCollectionDate()));
            }
            values.put(LabelFieldKey.COLLECTED_BY.name(), StringUtils.defaultString(sampleItem.getCollector()));
            if (sampleItem.getTypeOfSample() != null) {
                values.put(LabelFieldKey.SPECIMEN_TYPE.name(),
                        StringUtils.defaultString(sampleItem.getTypeOfSample().getLocalizedName()));
            }
        }
        List<SampleItem> items = sampleItem != null ? List.of(sampleItem)
                : sampleItemService.getSampleItemsBySampleId(sample.getId());
        Set<String> tests = new LinkedHashSet<>();
        for (SampleItem item : items) {
            for (Analysis analysis : analysisService.getAnalysesBySampleItem(item)) {
                if (analysis.getTest() != null) {
                    tests.add(TestServiceImpl.getUserLocalizedTestName(analysis.getTest()));
                }
            }
        }
        if (!tests.isEmpty()) {
            values.put(LabelFieldKey.TESTS.name(), String.join(", ", tests));
        }
        return values;
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
