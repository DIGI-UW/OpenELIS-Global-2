package org.openelisglobal.qaevent.qiconfig.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * OGC-709 — one indicator's config as the admin page sees it: the default
 * thresholds + enabled flag, plus its per-section overrides.
 *
 * <p>
 * Deliberately serves as BOTH the list-response shape and the PUT-request body.
 * On input, {@code id} and {@code testSectionName} on each override are ignored
 * (the client omits them); on output they are populated. One DTO, both
 * directions — the section is keyed by {@code testCategoryId} either way.
 */
public class QiConfigView {

    private String indicatorKey;
    private Boolean enabled;
    private BigDecimal target;
    private BigDecimal action;
    private String direction;
    private List<Override> overrides = new ArrayList<>();

    public String getIndicatorKey() {
        return indicatorKey;
    }

    public void setIndicatorKey(String indicatorKey) {
        this.indicatorKey = indicatorKey;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public BigDecimal getTarget() {
        return target;
    }

    public void setTarget(BigDecimal target) {
        this.target = target;
    }

    public BigDecimal getAction() {
        return action;
    }

    public void setAction(BigDecimal action) {
        this.action = action;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public List<Override> getOverrides() {
        return overrides;
    }

    public void setOverrides(List<Override> overrides) {
        this.overrides = overrides;
    }

    /**
     * A per-test-section threshold override. {@code id}/{@code testSectionName} are
     * output-only.
     */
    public static class Override {
        private String id;
        private String testCategoryId;
        private String testSectionName;
        private BigDecimal target;
        private BigDecimal action;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getTestCategoryId() {
            return testCategoryId;
        }

        public void setTestCategoryId(String testCategoryId) {
            this.testCategoryId = testCategoryId;
        }

        public String getTestSectionName() {
            return testSectionName;
        }

        public void setTestSectionName(String testSectionName) {
            this.testSectionName = testSectionName;
        }

        public BigDecimal getTarget() {
            return target;
        }

        public void setTarget(BigDecimal target) {
            this.target = target;
        }

        public BigDecimal getAction() {
            return action;
        }

        public void setAction(BigDecimal action) {
            this.action = action;
        }
    }
}
