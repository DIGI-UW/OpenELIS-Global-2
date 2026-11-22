package org.openelisglobal.qaevent.qiconfig.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.Objects;
import org.hibernate.annotations.Type;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * OGC-709 — one configuration row for a quality indicator.
 *
 * <p>
 * A row is either the indicator-wide <em>default</em> ({@code testCategoryId}
 * is null) or a per-test-section <em>override</em> ({@code testCategoryId} = a
 * {@code test_section.id}). Consumers (OGC-710 tiles, OGC-711 disable cascade,
 * OGC-712 threshold auto-NCE) resolve thresholds most-specific-wins: a matching
 * section override, else the default.
 *
 * <p>
 * {@code testCategoryId} is a {@code numeric} FK mapped to String via
 * {@code LIMSStringNumberUserType} (the OpenELIS idiom, as in
 * {@code TestAlertRule}). The {@code @Version} column {@code last_updated}
 * comes from {@link BaseObject}; the DB-filled {@code lastupdated} (DEFAULT
 * now()) is a distinct wall-clock column and is intentionally not mapped here.
 */
@Entity
@Table(name = "qi_config", schema = "clinlims")
public class QiConfig extends BaseObject<Long> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "qi_config_seq_gen")
    @SequenceGenerator(name = "qi_config_seq_gen", sequenceName = "qi_config_id_seq", schema = "clinlims", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @Column(name = "indicator_key", nullable = false, length = 50)
    private String indicatorKey;

    @Column(name = "test_category_id", precision = 10, scale = 0)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String testCategoryId;

    @Column(name = "is_enabled", nullable = false)
    private Boolean enabled = true;

    @Column(name = "target_threshold", precision = 15, scale = 5)
    private BigDecimal targetThreshold;

    @Column(name = "action_threshold", precision = 15, scale = 5)
    private BigDecimal actionThreshold;

    public QiConfig() {
        super();
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public String getIndicatorKey() {
        return indicatorKey;
    }

    public void setIndicatorKey(String indicatorKey) {
        this.indicatorKey = indicatorKey;
    }

    public String getTestCategoryId() {
        return testCategoryId;
    }

    public void setTestCategoryId(String testCategoryId) {
        this.testCategoryId = testCategoryId;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public BigDecimal getTargetThreshold() {
        return targetThreshold;
    }

    public void setTargetThreshold(BigDecimal targetThreshold) {
        this.targetThreshold = targetThreshold;
    }

    public BigDecimal getActionThreshold() {
        return actionThreshold;
    }

    public void setActionThreshold(BigDecimal actionThreshold) {
        this.actionThreshold = actionThreshold;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        QiConfig that = (QiConfig) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
