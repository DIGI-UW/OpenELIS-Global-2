package org.openelisglobal.barcode.labeltype;

import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.barcode.LabelField;
import org.openelisglobal.labelpreset.valueholder.PresetSnapshotDto;

/**
 * Hermetic {@link Label} rendered exclusively from a frozen
 * {@link PresetSnapshotDto} (OGC-285 M6, reprint path). This is the AC-20
 * enforcement point: dimensions, fields and barcode symbology are read from the
 * snapshot captured at order-save time and NOTHING is re-fetched from
 * {@code label_preset} / {@code test_label_preset_link} at reprint.
 *
 * <p>
 * Deliberately does NOT extend {@link OrderLabel}/{@link SpecimenLabel}: those
 * constructors read {@code Property.ORDER_LABEL_BARCODE_WIDTH/HEIGHT} from
 * global config and ignore the preset (see {@code OrderLabel} line ~30), which
 * would silently defeat the snapshot freeze. Here {@code height}/{@code width}
 * come straight from {@code snapshot.preset.height_mm/width_mm}.
 *
 * <p>
 * Note: barcode symbology lives on {@link org.openelisglobal.barcode
 * .BarcodeLabelMaker} (set via {@code setBarcodeType}), not on the Label, so
 * the snapshot's {@code barcode_type} is applied by the render service, not
 * here.
 */
public class SnapshotLabel extends Label {

    /**
     * Build a label whose frame and content come only from the snapshot.
     *
     * @param snapshot the frozen preset snapshot (must be non-null with a non-null
     *                 {@code preset} block)
     * @param labNo    the accession/lab number that becomes the barcode payload
     */
    public SnapshotLabel(PresetSnapshotDto snapshot, String labNo) {
        if (snapshot == null || snapshot.getPreset() == null) {
            throw new IllegalArgumentException("SnapshotLabel requires a snapshot with a non-null preset block");
        }
        PresetSnapshotDto.PresetSnapshotPreset preset = snapshot.getPreset();

        // Dimensions are used only as a ratio (pdfWidth is hardcoded to 350 in the
        // maker), but they MUST come from the snapshot — this is what AC-20 proves.
        if (preset.getHeightMm() != null) {
            this.height = preset.getHeightMm().floatValue();
        }
        if (preset.getWidthMm() != null) {
            this.width = preset.getWidthMm().floatValue();
        }

        // Snapshot fields render above the barcode, ordered by display_order. The
        // snapshot list is already ordered (preset.fields @OrderBy displayOrder) but
        // we sort defensively so JSONB read-order can never reshuffle the label.
        aboveFields = new ArrayList<>();
        List<PresetSnapshotDto.PresetSnapshotField> snapFields = snapshot.getFields();
        if (snapFields != null) {
            snapFields.stream().sorted((a, b) -> {
                int orderA = a.getDisplayOrder() == null ? Integer.MAX_VALUE : a.getDisplayOrder();
                int orderB = b.getDisplayOrder() == null ? Integer.MAX_VALUE : b.getDisplayOrder();
                return Integer.compare(orderA, orderB);
            }).forEach(field -> {
                String fieldLabel = field.getFieldLabel() != null ? field.getFieldLabel() : field.getFieldKey();
                LabelField labelField = new LabelField(fieldLabel == null ? "" : fieldLabel, "", 20);
                labelField.setDisplayFieldName(true);
                labelField.setUnderline(true);
                aboveFields.add(labelField);
            });
        }

        belowFields = new ArrayList<>();

        setCode(labNo);
    }

    @Override
    public int getNumTextRowsBefore() {
        return getNumRows(getAboveFields());
    }

    @Override
    public int getNumTextRowsAfter() {
        return 0;
    }

    @Override
    public int getMaxNumLabels() {
        // Reprint render does not enforce a per-label cap (that lives on the
        // configurable order/specimen labels); the frozen qty drives copy count.
        return Integer.MAX_VALUE;
    }
}
