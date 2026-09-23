package org.openelisglobal.reports.vectorsurveillance.manualentry.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.reports.vectorsurveillance.manualentry.valueholder.ManualEntryFieldMap;

public interface ManualEntryFieldMapDAO extends BaseDAO<ManualEntryFieldMap, Integer> {

    /** All rows ordered by {@code fieldOrder} (admin view — includes hidden). */
    List<ManualEntryFieldMap> getAllOrdered();

    /** Visible rows only, ordered by {@code fieldOrder} (helper view). */
    List<ManualEntryFieldMap> getVisibleOrdered();
}
