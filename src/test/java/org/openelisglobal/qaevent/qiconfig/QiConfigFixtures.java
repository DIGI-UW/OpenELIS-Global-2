package org.openelisglobal.qaevent.qiconfig;

import java.math.BigDecimal;
import org.openelisglobal.qaevent.qiconfig.dto.QiConfigView;

/**
 * The bundle payload {@code QiConfigService.saveIndicator} takes, built without
 * four setter lines at every call site. Shared by the configuration and the
 * threshold-breach integration tests, which both have to write a config before
 * they can assert anything about it.
 */
final class QiConfigFixtures {

    private QiConfigFixtures() {
    }

    static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    static QiConfigView view(boolean enabled, BigDecimal target, BigDecimal action,
            QiConfigView.Override... overrides) {
        QiConfigView v = new QiConfigView();
        v.setEnabled(enabled);
        v.setTarget(target);
        v.setAction(action);
        for (QiConfigView.Override o : overrides) {
            v.getOverrides().add(o);
        }
        return v;
    }

    /** A per-lab-unit override of the indicator's default thresholds. */
    static QiConfigView.Override ov(String sectionId, BigDecimal target, BigDecimal action) {
        QiConfigView.Override o = new QiConfigView.Override();
        o.setTestCategoryId(sectionId);
        o.setTarget(target);
        o.setAction(action);
        return o;
    }
}
