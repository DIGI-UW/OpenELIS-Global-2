package org.openelisglobal.labelpreset.service;

import java.util.List;
import org.openelisglobal.labelpreset.form.LabelPresetForm;
import org.openelisglobal.labelpreset.valueholder.BarcodeType;
import org.openelisglobal.labelpreset.valueholder.LabelPreset;

/**
 * CRUD + lifecycle service for {@link LabelPreset} entities. Implements M3
 * admin operations. @Transactional is in the Impl (inherited from
 * AuditableBaseObjectServiceImpl or annotated per-method).
 */
public interface LabelPresetService {

    /**
     * Returns all presets optionally filtered by status and barcode type. Pass null
     * values to skip the corresponding filter.
     *
     * @param activeOnly  if true, return only active presets
     * @param barcodeType filter by barcode type, or null for all types
     * @return matching presets ordered by name
     */
    List<LabelPreset> list(Boolean activeOnly, BarcodeType barcodeType);

    /**
     * Creates a new preset from the given form. Applies normalizeName before
     * persistence. Throws {@code IllegalArgumentException} if a preset with the
     * same normalized name already exists.
     *
     * @param form      the write request
     * @param sysUserId audit user id
     * @return the persisted preset
     */
    LabelPreset create(LabelPresetForm form, String sysUserId);

    /**
     * Retrieves a preset by id.
     *
     * @param id the preset id
     * @return the preset, or null if not found
     */
    LabelPreset get(Integer id);

    /**
     * Full-replacement update of an existing preset. Throws
     * {@code IllegalArgumentException} if id not found, if attempting to rename a
     * system preset, or if name collision with another preset.
     *
     * @param id        the preset to update
     * @param form      the write request
     * @param sysUserId audit user id
     * @return the updated preset
     */
    LabelPreset update(Integer id, LabelPresetForm form, String sysUserId);

    /**
     * Toggles the is_active flag. Throws {@code IllegalStateException} if
     * attempting to deactivate a system preset.
     *
     * @param id        the preset id
     * @param active    the desired active state
     * @param sysUserId audit user id
     * @return the updated preset
     */
    LabelPreset toggleActive(Integer id, boolean active, String sysUserId);

    /**
     * Duplicates a preset under a new name. The duplicate is always non-system and
     * active. Throws {@code IllegalArgumentException} if name collision.
     *
     * @param id        the source preset id
     * @param newName   the name for the copy (after normalization)
     * @param sysUserId audit user id
     * @return the new preset copy
     */
    LabelPreset duplicate(Integer id, String newName, String sysUserId);

    /**
     * Normalizes a preset name: trim whitespace, then convert to lower-case. Used
     * internally for collision detection.
     *
     * @param input raw name input
     * @return normalized form
     */
    String normalizeName(String input);
}
