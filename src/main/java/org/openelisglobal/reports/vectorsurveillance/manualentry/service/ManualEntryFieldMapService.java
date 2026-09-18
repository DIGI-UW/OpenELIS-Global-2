package org.openelisglobal.reports.vectorsurveillance.manualentry.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.reports.vectorsurveillance.manualentry.valueholder.ManualEntryFieldMap;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Manages the admin-configurable Manual Entry field map (US5 / FR-009).
 */
public interface ManualEntryFieldMapService extends BaseObjectService<ManualEntryFieldMap, Integer> {

    /** All rows ordered by {@code fieldOrder} (admin screen — incl. hidden). */
    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<ManualEntryFieldMap> getAllOrdered();

    /** Visible rows only, ordered by {@code fieldOrder} (helper view). */
    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<ManualEntryFieldMap> getVisibleOrdered();

    /** Persist a new field-map row. */
    @PreAuthorize("hasAuthority('PRIV_RESULT_ENTER')")
    Integer create(ManualEntryFieldMap fieldMap, String sysUserId);

    /** Update order / visibility / label / portal tag of an existing row. */
    @PreAuthorize("hasAuthority('PRIV_RESULT_ENTER')")
    ManualEntryFieldMap patchUpdate(Integer id, ManualEntryFieldMap patch, String sysUserId);
}
