package org.openelisglobal.barcode.labeltype;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.openelisglobal.barcode.LabelField;
import org.openelisglobal.labelpreset.valueholder.PresetSnapshotDto;

/**
 * OGC-1218: a snapshot label prints its content fields by name with the value
 * the order knows, leaves a line to write on where it knows nothing, and does
 * not repeat the lab number, which is the barcode's own text.
 */
public class SnapshotLabelFieldsTest {

    private static PresetSnapshotDto snapshot(PresetSnapshotDto.PresetSnapshotField... fields) {
        PresetSnapshotDto snapshot = new PresetSnapshotDto();
        PresetSnapshotDto.PresetSnapshotPreset preset = new PresetSnapshotDto.PresetSnapshotPreset();
        preset.setHeightMm(25);
        preset.setWidthMm(76);
        preset.setBarcodeType("CODE_128");
        snapshot.setPreset(preset);
        snapshot.setFields(List.of(fields));
        return snapshot;
    }

    private static List<LabelField> rows(SnapshotLabel label) {
        List<LabelField> rows = new ArrayList<>();
        label.getAboveFields().forEach(rows::add);
        return rows;
    }

    private static PresetSnapshotDto.PresetSnapshotField field(String key, String label, int order) {
        PresetSnapshotDto.PresetSnapshotField field = new PresetSnapshotDto.PresetSnapshotField();
        field.setFieldKey(key);
        field.setFieldLabel(label);
        field.setIsRequired(false);
        field.setDisplayOrder(order);
        return field;
    }

    @Test
    public void printsValuesByNameInDisplayOrderAndSkipsTheLabNumberRow() {
        PresetSnapshotDto snapshot = snapshot(field("PATIENT_NAME", "Patient Name", 2),
                field("LAB_NUMBER", "Lab Number", 1), field("TESTS", "Tests", 3));

        SnapshotLabel label = new SnapshotLabel(snapshot, "DEV0126000000000001",
                Map.of("PATIENT_NAME", "Doe, Jane", "TESTS", "Creatinine, GPT/ALAT"));

        List<LabelField> rows = rows(label);
        assertEquals("the lab number is the barcode's text, not a row", 2, rows.size());
        assertEquals("Patient Name", rows.get(0).getName());
        assertEquals("Doe, Jane", rows.get(0).getValue());
        assertTrue(rows.get(0).isDisplayFieldName());
        assertFalse("a printed value is not a line to write on", rows.get(0).isUnderline());
        assertEquals("Tests", rows.get(1).getName());
        assertEquals("Creatinine, GPT/ALAT", rows.get(1).getValue());
        assertEquals("DEV0126000000000001", label.getCode());
    }

    @Test
    public void aFieldWithoutAValueIsALineToWriteOn() {
        PresetSnapshotDto snapshot = snapshot(field("LAB_NUMBER", "Lab Number", 1),
                field("STORAGE_LOCATION", "Storage Location", 2), field("PATIENT_ID", "Patient ID", 3));

        SnapshotLabel label = new SnapshotLabel(snapshot, "DEV0126000000000002", Map.of("PATIENT_ID", ""));

        List<LabelField> rows = rows(label);
        assertEquals(2, rows.size());
        assertEquals("Storage Location", rows.get(0).getName());
        assertEquals("", rows.get(0).getValue());
        assertTrue(rows.get(0).isUnderline());
        assertTrue("a blank value is also a line to write on", rows.get(1).isUnderline());
    }

    @Test
    public void theTwoArgumentConstructorStillRendersNamesOnly() {
        PresetSnapshotDto snapshot = snapshot(field("LAB_NUMBER", "Lab Number", 1),
                field("PATIENT_DOB", "Patient Date of Birth", 2));

        SnapshotLabel label = new SnapshotLabel(snapshot, "DEV0126000000000003");

        assertEquals(1, rows(label).size());
        assertEquals("Patient Date of Birth", rows(label).get(0).getName());
        assertEquals("", rows(label).get(0).getValue());
        assertEquals(25f, label.getHeight(), 0.001f);
    }
}
