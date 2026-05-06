package org.openelisglobal.compliance.valueholder;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.common.valueholder.SimpleBaseEntity;
import org.openelisglobal.validation.annotations.SafeHtml;

/**
 * ComplianceThresholdValueMap entity for mapping select list test result
 * options to compliance statuses.
 *
 * Used when tests have predefined result options (e.g., Absent/Present,
 * severity scales) instead of numeric values. Each row maps one result option
 * to COMPLIANT, BORDERLINE, or NON_COMPLIANT status.
 *
 * Required by FR-3-013 and FR-3-014 in the FRS.
 */
@Entity
@Table(name = "compliance_threshold_value_map")
public class ComplianceThresholdValueMap extends BaseObject<String> implements SimpleBaseEntity<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", precision = 10, scale = 0)
    @GeneratedValue(generator = "compliance_threshold_value_map_seq_gen")
    @GenericGenerator(name = "compliance_threshold_value_map_seq_gen", strategy = "org.openelisglobal.hibernate.resources.StringSequenceGenerator", parameters = @Parameter(name = "sequence_name", value = "compliance_threshold_value_map_seq"))
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "threshold_id", nullable = false)
    @JsonBackReference("threshold-mappings")
    @NotNull(message = "Threshold is required")
    private ComplianceThreshold threshold;

    @NotBlank(message = "Option value is required")
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "option_value", nullable = false)
    private String optionValue;

    @NotNull(message = "Compliance status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "compliance_status", nullable = false)
    private ComplianceStatus complianceStatus;

    @Column(name = "sys_user_id", nullable = false)
    private Integer systemUserId;

    public ComplianceThresholdValueMap() {
        super();
    }

    public ComplianceThresholdValueMap(String optionValue, ComplianceStatus complianceStatus) {
        this();
        this.optionValue = optionValue;
        this.complianceStatus = complianceStatus;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public ComplianceThreshold getThreshold() {
        return threshold;
    }

    public void setThreshold(ComplianceThreshold threshold) {
        this.threshold = threshold;
    }

    public String getOptionValue() {
        return optionValue;
    }

    public void setOptionValue(String optionValue) {
        this.optionValue = optionValue;
    }

    public ComplianceStatus getComplianceStatus() {
        return complianceStatus;
    }

    public void setComplianceStatus(ComplianceStatus complianceStatus) {
        this.complianceStatus = complianceStatus;
    }

    public Integer getSystemUserId() {
        return systemUserId;
    }

    public void setSystemUserId(Integer systemUserId) {
        this.systemUserId = systemUserId;
    }

    @Override
    public void setSysUserId(String sysUserId) {
        this.systemUserId = sysUserId != null ? Integer.parseInt(sysUserId) : null;
        super.setSysUserId(sysUserId);
    }

    @Override
    public String toString() {
        return String.format("ComplianceThresholdValueMap{id='%s', optionValue='%s', complianceStatus=%s}", id,
                optionValue, complianceStatus);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;

        ComplianceThresholdValueMap that = (ComplianceThresholdValueMap) obj;

        if (id != null) {
            return id.equals(that.id);
        }

        // Business equality for unsaved entities
        return optionValue != null && optionValue.equals(that.optionValue) && threshold != null
                && threshold.equals(that.threshold);
    }

    @Override
    public int hashCode() {
        if (id != null) {
            return id.hashCode();
        }

        // Business hash for unsaved entities
        int result = optionValue != null ? optionValue.hashCode() : 0;
        result = 31 * result + (threshold != null ? threshold.hashCode() : 0);
        return result;
    }
}