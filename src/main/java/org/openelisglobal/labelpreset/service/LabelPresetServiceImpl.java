package org.openelisglobal.labelpreset.service;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.labelpreset.dao.LabelPresetDAO;
import org.openelisglobal.labelpreset.form.LabelPresetForm;
import org.openelisglobal.labelpreset.valueholder.BarcodeType;
import org.openelisglobal.labelpreset.valueholder.FieldSourceType;
import org.openelisglobal.labelpreset.valueholder.LabelPreset;
import org.openelisglobal.labelpreset.valueholder.LabelPresetField;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** {@inheritDoc} */
@Service
public class LabelPresetServiceImpl extends AuditableBaseObjectServiceImpl<LabelPreset, Integer>
        implements LabelPresetService {

    @Autowired
    protected LabelPresetDAO baseObjectDAO;

    public LabelPresetServiceImpl() {
        super(LabelPreset.class);
    }

    @Override
    protected LabelPresetDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    // ── List ─────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<LabelPreset> list(Boolean activeOnly, BarcodeType barcodeType) {
        List<LabelPreset> all;
        if (barcodeType != null) {
            all = baseObjectDAO.listByBarcodeType(barcodeType);
        } else {
            all = getAll();
        }
        if (Boolean.TRUE.equals(activeOnly)) {
            all = all.stream().filter(p -> Boolean.TRUE.equals(p.getIsActive())).collect(Collectors.toList());
        }
        // Initialize the lazy fields collection within this read transaction so the
        // controller can serialize the (detached) entities to JSON without a
        // LazyInitializationException (constitution: services compile all data within
        // the transaction).
        all.forEach(p -> org.hibernate.Hibernate.initialize(p.getFields()));
        return all;
    }

    // ── Create ───────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public LabelPreset create(LabelPresetForm form, String sysUserId) {
        String normalized = normalizeName(form.getName());
        checkNameConflict(null, normalized);

        LabelPreset preset = new LabelPreset();
        preset.setSysUserId(sysUserId);
        applyForm(preset, form);
        preset.setIsSystem(false);
        insert(preset);
        return preset;
    }

    // ── Get ──────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public LabelPreset get(Integer id) {
        LabelPreset preset = baseObjectDAO.get(id).orElse(null);
        if (preset != null) {
            // Initialize lazy fields within this read transaction (see list()).
            org.hibernate.Hibernate.initialize(preset.getFields());
        }
        return preset;
    }

    // ── Update ───────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public LabelPreset update(Integer id, LabelPresetForm form, String sysUserId) {
        LabelPreset preset = requireExists(id);
        String normalized = normalizeName(form.getName());
        if (Boolean.TRUE.equals(preset.getIsSystem())) {
            // System presets may not be renamed
            if (!normalized.equals(normalizeName(preset.getName()))) {
                throw new IllegalArgumentException("System presets cannot be renamed");
            }
            // System presets may not be deactivated via PUT either
            if (form.getIsActive() != null && !form.getIsActive()) {
                throw new IllegalStateException("System presets cannot be deactivated");
            }
        } else {
            checkNameConflict(id, normalized);
        }
        preset.setSysUserId(sysUserId);
        applyForm(preset, form);
        update(preset);
        return preset;
    }

    // ── Toggle active ────────────────────────────────────────────────────────

    @Override
    @Transactional
    public LabelPreset toggleActive(Integer id, boolean active, String sysUserId) {
        LabelPreset preset = requireExists(id);
        if (!active && Boolean.TRUE.equals(preset.getIsSystem())) {
            throw new IllegalStateException("System presets cannot be deactivated");
        }
        preset.setIsActive(active);
        preset.setSysUserId(sysUserId);
        update(preset);
        return preset;
    }

    // ── Duplicate ────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public LabelPreset duplicate(Integer id, String newName, String sysUserId) {
        LabelPreset source = requireExists(id);
        String normalized = normalizeName(newName);
        checkNameConflict(null, normalized);

        LabelPreset copy = new LabelPreset();
        copy.setSysUserId(sysUserId);
        copy.setName(newName != null ? newName.trim() : normalized);
        copy.setHeightMm(source.getHeightMm());
        copy.setWidthMm(source.getWidthMm());
        copy.setBarcodeType(source.getBarcodeType());
        copy.setPrintsPerOrder(source.getPrintsPerOrder());
        copy.setPrintsPerSample(source.getPrintsPerSample());
        copy.setDefaultPerOrder(source.getDefaultPerOrder());
        copy.setMaxPerOrder(source.getMaxPerOrder());
        copy.setDefaultPerSample(source.getDefaultPerSample());
        copy.setMaxPerSample(source.getMaxPerSample());
        copy.setIsSystem(false);
        copy.setIsActive(true);

        // Copy fields
        for (LabelPresetField srcField : source.getFields()) {
            LabelPresetField f = new LabelPresetField();
            f.setPreset(copy);
            f.setFieldKey(srcField.getFieldKey());
            f.setSourceType(srcField.getSourceType());
            f.setIsRequired(srcField.getIsRequired());
            f.setDisplayOrder(srcField.getDisplayOrder());
            copy.getFields().add(f);
        }

        insert(copy);
        return copy;
    }

    // ── normalizeName ────────────────────────────────────────────────────────

    @Override
    public String normalizeName(String input) {
        if (input == null) {
            return "";
        }
        return input.trim().toLowerCase(Locale.ROOT);
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private LabelPreset requireExists(Integer id) {
        LabelPreset preset = get(id);
        if (preset == null) {
            throw new IllegalArgumentException("LabelPreset not found: " + id);
        }
        return preset;
    }

    /**
     * Checks that no other preset has the same normalized name. Excludes the preset
     * with the given id when non-null (for update scenarios).
     */
    private void checkNameConflict(Integer excludeId, String normalizedName) {
        List<LabelPreset> all = getAll();
        Optional<LabelPreset> conflict = all.stream().filter(p -> normalizeName(p.getName()).equals(normalizedName)
                && (excludeId == null || !excludeId.equals(p.getId()))).findFirst();
        if (conflict.isPresent()) {
            throw new IllegalArgumentException("A preset with name '" + normalizedName + "' already exists");
        }
    }

    /**
     * Applies form values to the preset, replacing fields collection. Stores the
     * trimmed original name (preserves case) — normalization is for collision
     * detection only.
     */
    private void applyForm(LabelPreset preset, LabelPresetForm form) {
        preset.setName(form.getName() != null ? form.getName().trim() : "");
        preset.setHeightMm(form.getHeightMm());
        preset.setWidthMm(form.getWidthMm());
        preset.setBarcodeType(form.getBarcodeType());
        preset.setPrintsPerOrder(form.getPrintsPerOrder() != null ? form.getPrintsPerOrder() : false);
        preset.setPrintsPerSample(form.getPrintsPerSample() != null ? form.getPrintsPerSample() : true);
        preset.setDefaultPerOrder(form.getDefaultPerOrder() != null ? form.getDefaultPerOrder() : 0);
        preset.setMaxPerOrder(form.getMaxPerOrder() != null ? form.getMaxPerOrder() : 10);
        preset.setDefaultPerSample(form.getDefaultPerSample() != null ? form.getDefaultPerSample() : 0);
        preset.setMaxPerSample(form.getMaxPerSample() != null ? form.getMaxPerSample() : 10);
        preset.setIsActive(form.getIsActive() != null ? form.getIsActive() : true);

        // Replace fields
        preset.getFields().clear();
        if (form.getFields() != null) {
            for (LabelPresetForm.FieldEntry entry : form.getFields()) {
                LabelPresetField field = new LabelPresetField();
                field.setPreset(preset);
                field.setFieldKey(entry.getFieldKey());
                field.setSourceType(FieldSourceType.SYSTEM);
                field.setIsRequired(entry.getIsRequired() != null ? entry.getIsRequired() : false);
                field.setDisplayOrder(entry.getDisplayOrder());
                preset.getFields().add(field);
            }
        }
    }
}
