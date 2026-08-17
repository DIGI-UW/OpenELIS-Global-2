package org.openelisglobal.qaevent.qiconfig.dto;

import java.math.BigDecimal;

/**
 * OGC-709 — the resolved thresholds for one (indicator, test section) pair,
 * after applying most-specific-wins. This is the stable read contract that
 * OGC-710 (tiles), OGC-711 (disable cascade), and OGC-712 (threshold auto-NCE)
 * consume; they must not re-derive the resolution rule.
 *
 * <p>
 * When {@code enabled} is false the thresholds carry the default's values but
 * consumers should treat the indicator as not-tracked (711). Thresholds may be
 * null (NCE ships without bands until 712 defines them).
 */
public class ResolvedConfig {

    private final String indicatorKey;
    private final boolean enabled;
    private final BigDecimal target;
    private final BigDecimal action;
    private final String direction;

    public ResolvedConfig(String indicatorKey, boolean enabled, BigDecimal target, BigDecimal action,
            String direction) {
        this.indicatorKey = indicatorKey;
        this.enabled = enabled;
        this.target = target;
        this.action = action;
        this.direction = direction;
    }

    public String getIndicatorKey() {
        return indicatorKey;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public BigDecimal getTarget() {
        return target;
    }

    public BigDecimal getAction() {
        return action;
    }

    public String getDirection() {
        return direction;
    }
}
