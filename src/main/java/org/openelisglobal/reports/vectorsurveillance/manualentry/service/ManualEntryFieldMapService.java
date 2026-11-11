package org.openelisglobal.reports.vectorsurveillance.manualentry.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.reports.vectorsurveillance.manualentry.valueholder.ManualEntryFieldMap;

/**
 * Manages the admin-configurable Manual Entry field map (US5 / FR-009).
 */
public interface ManualEntryFieldMapService extends BaseObjectService<ManualEntryFieldMap, Integer> {

    /** All rows ordered by {@code fieldOrder} (admin screen — incl. hidden). */
    List<ManualEntryFieldMap> getAllOrdered();

    /** Visible rows only, ordered by {@code fieldOrder} (helper view). */
    List<ManualEntryFieldMap> getVisibleOrdered();

    /** Persist a new field-map row. */
    Integer create(ManualEntryFieldMap fieldMap, String sysUserId);

    /** Update order / visibility / label / portal tag of an existing row. */
    ManualEntryFieldMap patchUpdate(Integer id, ManualEntryFieldMap patch, String sysUserId);
}
