package org.openelisglobal.labelpreset.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.openelisglobal.labelpreset.dao.LabelPresetDAO;
import org.openelisglobal.labelpreset.dao.OrderLabelRequestDAO;
import org.openelisglobal.labelpreset.dao.TestLabelPresetLinkDAO;
import org.openelisglobal.labelpreset.dto.OrderLabelPersistRequest;
import org.openelisglobal.labelpreset.valueholder.LabelPreset;
import org.openelisglobal.labelpreset.valueholder.LabelPresetField;
import org.openelisglobal.labelpreset.valueholder.OrderLabelRequest;
import org.openelisglobal.labelpreset.valueholder.PresetSnapshotDto;
import org.openelisglobal.labelpreset.valueholder.TestLabelPresetLink;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persistence implementation (OGC-285 M5). Writes one
 * {@code order_label_request} row per chosen per-order preset cell and one per
 * chosen (sample, preset) cell, each carrying a frozen
 * {@link PresetSnapshotDto} built from the <em>current</em> preset + link
 * state.
 *
 * <p>
 * {@code @Transactional} — joins the caller's order-save transaction.
 */
@Service
public class OrderLabelRequestServiceImpl implements OrderLabelRequestService {

    @Autowired
    private OrderLabelRequestDAO orderLabelRequestDAO;

    @Autowired
    private LabelPresetDAO labelPresetDAO;

    @Autowired
    private TestLabelPresetLinkDAO testLabelPresetLinkDAO;

    @Autowired
    private SampleService sampleService;

    @Autowired
    private SampleItemService sampleItemService;

    @Override
    @Transactional
    public List<OrderLabelRequest> persistRequest(String orderId, Map<String, String> sampleIdMap,
            OrderLabelPersistRequest payload, String sysUserId, Map<String, List<String>> testIdsBySampleLocal) {
        List<OrderLabelRequest> persisted = new ArrayList<>();
        if (payload == null || orderId == null) {
            return persisted;
        }

        Sample parentSample = sampleService.get(orderId);
        if (parentSample == null) {
            throw new IllegalArgumentException("Cannot persist label requests: no Sample with id " + orderId);
        }

        Map<String, String> resolvedSampleIds = sampleIdMap == null ? Map.of() : sampleIdMap;
        Map<String, List<String>> testIdsByLocal = testIdsBySampleLocal == null ? Map.of() : testIdsBySampleLocal;

        // --- Per-order cells: one row each, sampleItem null, no test_link.
        for (OrderLabelPersistRequest.PersistCell cell : payload.getOrderCells()) {
            int qty = normalizeQty(cell.getQty());
            if (qty <= 0) {
                continue;
            }
            LabelPreset preset = requirePreset(cell.getPresetId());
            PresetSnapshotDto snapshot = buildSnapshot(preset, null);
            persisted.add(insertRow(parentSample, null, preset, qty, snapshot, sysUserId));
        }

        // --- Per-sample cells: one row each, bound to the resolved SampleItem.
        for (OrderLabelPersistRequest.PersistSampleRow row : payload.getSampleRows()) {
            String localId = row.getSampleIdLocal();
            String sampleItemId = resolvedSampleIds.get(localId);
            if (sampleItemId == null) {
                // No persisted sample item for this local id — skip (cannot anchor the row).
                continue;
            }
            SampleItem sampleItem = sampleItemService.get(sampleItemId);
            if (sampleItem == null) {
                continue;
            }
            List<String> testIds = testIdsByLocal.getOrDefault(localId, List.of());
            for (OrderLabelPersistRequest.PersistCell cell : row.getCells()) {
                int qty = normalizeQty(cell.getQty());
                if (qty <= 0) {
                    continue;
                }
                LabelPreset preset = requirePreset(cell.getPresetId());
                TestLabelPresetLink sourceLink = resolveSourceLink(preset.getId(), testIds);
                PresetSnapshotDto snapshot = buildSnapshot(preset, sourceLink);
                persisted.add(insertRow(parentSample, sampleItem, preset, qty, snapshot, sysUserId));
            }
        }

        return persisted;
    }

    private OrderLabelRequest insertRow(Sample parentSample, SampleItem sampleItem, LabelPreset preset, int qty,
            PresetSnapshotDto snapshot, String sysUserId) {
        validateSnapshot(snapshot);
        OrderLabelRequest request = new OrderLabelRequest();
        request.setParentSample(parentSample);
        request.setSampleItem(sampleItem);
        request.setPreset(preset);
        request.setQty(qty);
        request.setPresetSnapshot(snapshot);
        request.setSysUserId(sysUserId);
        Integer id = orderLabelRequestDAO.insert(request);
        request.setId(id);
        return request;
    }

    private int normalizeQty(Integer qty) {
        return qty == null ? 0 : qty;
    }

    private LabelPreset requirePreset(Integer presetId) {
        if (presetId == null) {
            throw new IllegalArgumentException("Cannot persist label request: presetId is null");
        }
        Optional<LabelPreset> preset = labelPresetDAO.get(presetId);
        return preset.orElseThrow(
                () -> new IllegalArgumentException("Cannot persist label request: no preset with id " + presetId));
    }

    /**
     * The link that drives this (sample, preset) cell's snapshot test_link block:
     * the link with the highest {@code default_qty} across the sample's ordered
     * tests, mirroring the aggregation's source-test selection. Null if no test in
     * the sample's order links the preset (per-order presets, or system-default
     * fallback).
     */
    private TestLabelPresetLink resolveSourceLink(Integer presetId, List<String> testIds) {
        TestLabelPresetLink best = null;
        int bestDefault = Integer.MIN_VALUE;
        for (String testId : testIds) {
            for (TestLabelPresetLink link : testLabelPresetLinkDAO.listByTestId(testId)) {
                if (link.getPreset() == null || !presetId.equals(link.getPreset().getId())) {
                    continue;
                }
                int linkDefault = link.getDefaultQty() == null ? 0 : link.getDefaultQty();
                if (linkDefault > bestDefault) {
                    bestDefault = linkDefault;
                    best = link;
                }
            }
        }
        return best;
    }

    /**
     * Build a frozen {@link PresetSnapshotDto} from the current preset state. When
     * {@code sourceLink} is non-null, capture its qty source in the
     * {@code test_link} block (FRS §7.3.1).
     */
    private PresetSnapshotDto buildSnapshot(LabelPreset preset, TestLabelPresetLink sourceLink) {
        PresetSnapshotDto snapshot = new PresetSnapshotDto();

        PresetSnapshotDto.PresetSnapshotPreset snapPreset = new PresetSnapshotDto.PresetSnapshotPreset();
        snapPreset.setId(preset.getId());
        snapPreset.setName(preset.getName());
        snapPreset.setHeightMm(preset.getHeightMm());
        snapPreset.setWidthMm(preset.getWidthMm());
        snapPreset.setBarcodeType(preset.getBarcodeType() == null ? null : preset.getBarcodeType().name());
        snapshot.setPreset(snapPreset);

        List<PresetSnapshotDto.PresetSnapshotField> fields = new ArrayList<>();
        for (LabelPresetField field : preset.getFields()) {
            PresetSnapshotDto.PresetSnapshotField snapField = new PresetSnapshotDto.PresetSnapshotField();
            snapField.setFieldKey(field.getFieldKey());
            snapField.setFieldLabel(field.getFieldKey());
            snapField.setIsRequired(field.getIsRequired());
            snapField.setDisplayOrder(field.getDisplayOrder());
            fields.add(snapField);
        }
        snapshot.setFields(fields);

        if (sourceLink != null) {
            PresetSnapshotDto.PresetSnapshotTestLink snapLink = new PresetSnapshotDto.PresetSnapshotTestLink();
            snapLink.setTestId(sourceLink.getTest() == null ? null : parseInt(sourceLink.getTest().getId()));
            snapLink.setDefaultQty(sourceLink.getDefaultQty());
            snapLink.setMaxQty(sourceLink.getMaxQty());
            snapLink.setAllowOverride(sourceLink.getAllowOverride());
            snapshot.setTestLink(snapLink);
        }

        return snapshot;
    }

    /**
     * Validate the JSONB shape against the DTO contract before persist: the preset
     * block and its identifying fields must be present (the NOT-NULL
     * {@code preset_snapshot} column plus FRS §7.3.1 minimum shape).
     */
    private void validateSnapshot(PresetSnapshotDto snapshot) {
        if (snapshot == null || snapshot.getPreset() == null) {
            throw new IllegalStateException("preset_snapshot must contain a 'preset' block before persist");
        }
        PresetSnapshotDto.PresetSnapshotPreset preset = snapshot.getPreset();
        if (preset.getId() == null) {
            throw new IllegalStateException("preset_snapshot.preset.id is required");
        }
        if (preset.getName() == null || preset.getName().isBlank()) {
            throw new IllegalStateException("preset_snapshot.preset.name is required");
        }
        if (preset.getBarcodeType() == null || preset.getBarcodeType().isBlank()) {
            throw new IllegalStateException("preset_snapshot.preset.barcode_type is required");
        }
    }

    private Integer parseInt(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
