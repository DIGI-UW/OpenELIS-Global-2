package org.openelisglobal.qaevent.qiconfig.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.qaevent.qiconfig.dto.QiConfigView;
import org.openelisglobal.qaevent.qiconfig.dto.ResolvedConfig;
import org.openelisglobal.qaevent.qiconfig.valueholder.QiConfig;

public interface QiConfigService extends BaseObjectService<QiConfig, Long> {

    /**
     * All four indicators (always present) with their defaults + overrides, for the
     * admin page.
     */
    List<QiConfigView> getAllConfigs();

    /**
     * Resolve the effective thresholds for an (indicator, section) pair,
     * most-specific-wins. Throws {@link IllegalArgumentException} for an unknown
     * indicator or a missing default (both → HTTP 400 at the controller).
     */
    ResolvedConfig resolve(String indicatorKey, String testSectionId);

    /**
     * Atomically upsert one indicator's default + overrides and delete overrides no
     * longer present. Validates the whole bundle first (bad input →
     * {@link IllegalArgumentException} → HTTP 400).
     */
    void saveIndicator(String indicatorKey, QiConfigView view, String sysUserId);
}
