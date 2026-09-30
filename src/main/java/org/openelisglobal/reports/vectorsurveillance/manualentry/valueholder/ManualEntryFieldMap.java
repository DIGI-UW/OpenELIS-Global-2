package org.openelisglobal.reports.vectorsurveillance.manualentry.valueholder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import org.hibernate.annotations.DynamicUpdate;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * Admin-configurable field map driving the Manual Entry Helper (FR-009 / US5):
 * which surveillance metric the helper shows, in what order, with what label /
 * portal tag, and whether it is visible. One row per metric.
 *
 * <p>
 * Extends {@link BaseObject} (optimistic-locked via {@code lastupdated}) and is
 * audited via {@code AuditableBaseObjectServiceImpl} +
 * {@code reference_tables.keep_history='Y'}.
 */
@Entity
@Table(name = "manual_entry_field_map", schema = "clinlims")
@DynamicUpdate
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
@AttributeOverride(name = "lastupdated", column = @Column(name = "lastupdated"))
public class ManualEntryFieldMap extends BaseObject<Integer> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "manual_entry_field_map_seq_gen")
    @SequenceGenerator(name = "manual_entry_field_map_seq_gen", sequenceName = "manual_entry_field_map_seq", schema = "clinlims", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @Column(name = "metric_key", length = 100, nullable = false)
    private String metricKey;

    @Column(name = "field_order", nullable = false)
    private Integer fieldOrder;

    @Column(name = "label", length = 255, nullable = false)
    private String label;

    @Column(name = "portal_tag", length = 50)
    private String portalTag;

    @Column(name = "visible", nullable = false)
    private Boolean visible = Boolean.TRUE;

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public String getMetricKey() {
        return metricKey;
    }

    public void setMetricKey(String metricKey) {
        this.metricKey = metricKey;
    }

    public Integer getFieldOrder() {
        return fieldOrder;
    }

    public void setFieldOrder(Integer fieldOrder) {
        this.fieldOrder = fieldOrder;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getPortalTag() {
        return portalTag;
    }

    public void setPortalTag(String portalTag) {
        this.portalTag = portalTag;
    }

    public Boolean getVisible() {
        return visible;
    }

    public void setVisible(Boolean visible) {
        this.visible = visible;
    }

    @Override
    protected String getDefaultLocalizedName() {
        return label;
    }
}
