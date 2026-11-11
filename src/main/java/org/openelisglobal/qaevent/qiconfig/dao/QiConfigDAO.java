package org.openelisglobal.qaevent.qiconfig.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.qaevent.qiconfig.valueholder.QiConfig;

public interface QiConfigDAO extends BaseDAO<QiConfig, Long> {

    /** All config rows, defaults before their overrides, for the admin list. */
    List<QiConfig> getAllOrderedByIndicator();

    /**
     * The indicator-wide default row ({@code test_category_id IS NULL}), or null.
     */
    QiConfig getDefault(String indicatorKey);

    /** The per-section override row for this indicator, or null. */
    QiConfig getOverride(String indicatorKey, String testSectionId);
}
