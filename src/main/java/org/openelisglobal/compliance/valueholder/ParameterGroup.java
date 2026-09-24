package org.openelisglobal.compliance.valueholder;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.common.valueholder.SimpleBaseEntity;
import org.openelisglobal.validation.annotations.SafeHtml;

/**
 * ParameterGroup value holder representing groups of related parameters within
 * a compliance standard.
 *
 * Follows constitutional requirements: - Extends BaseObject for audit trail
 * support - Includes FHIR UUID for interoperability - Uses JPA annotations (no
 * XML mappings) - Implements validation annotations
 */
@Entity
@Table(name = "parameter_group")
public class ParameterGroup extends BaseObject<String> implements SimpleBaseEntity<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", precision = 10, scale = 0)
    @GeneratedValue(generator = "parameter_group_seq_gen")
    @GenericGenerator(name = "parameter_group_seq_gen", strategy = "org.openelisglobal.hibernate.resources.StringSequenceGenerator", parameters = @Parameter(name = "sequence_name", value = "parameter_group_seq"))
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String id;

    @NotNull
    @Column(name = "fhir_uuid", unique = true, nullable = false)
    private UUID fhirUuid;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "standard_id", nullable = false)
    @JsonBackReference("standard-groups")
    @NotNull(message = "Standard is required")
    private ComplianceStandard standard;

    @NotBlank(message = "Group name is required")
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "name", nullable = false)
    private String name;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @NotNull
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @NotNull
    @Column(name = "is_mandatory", nullable = false)
    private Boolean isMandatory = true;

    @Column(name = "sys_user_id", nullable = false)
    private Integer systemUserId;

    // Bidirectional relationship with thresholds
    @OneToMany(mappedBy = "group", fetch = FetchType.LAZY)
    @JsonManagedReference("group-thresholds")
    private List<ComplianceThreshold> complianceThresholds = new ArrayList<>();

    public ParameterGroup() {
        super();
        generateFhirUuid();
    }

    public ParameterGroup(String name, String description, Integer sortOrder) {
        this();
        this.name = name;
        this.description = description;
        this.sortOrder = sortOrder != null ? sortOrder : 0;
    }

    private void generateFhirUuid() {
        if (fhirUuid == null) {
            fhirUuid = UUID.randomUUID();
        }
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public UUID getFhirUuid() {
        return fhirUuid;
    }

    public void setFhirUuid(UUID fhirUuid) {
        this.fhirUuid = fhirUuid;
    }

    public ComplianceStandard getStandard() {
        return standard;
    }

    public void setStandard(ComplianceStandard standard) {
        this.standard = standard;
    }

    public String getStandardId() {
        return standard != null ? standard.getId() : null;
    }

    public void setStandardId(String standardId) {
        if (standardId != null) {
            if (this.standard == null) {
                this.standard = new ComplianceStandard();
            }
            this.standard.setId(standardId);
        } else {
            this.standard = null;
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder != null ? sortOrder : 0;
    }

    public Boolean getIsMandatory() {
        return isMandatory;
    }

    public void setIsMandatory(Boolean isMandatory) {
        this.isMandatory = isMandatory != null ? isMandatory : true;
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

    public List<ComplianceThreshold> getComplianceThresholds() {
        return complianceThresholds;
    }

    public void setComplianceThresholds(List<ComplianceThreshold> complianceThresholds) {
        this.complianceThresholds = complianceThresholds != null ? complianceThresholds : new ArrayList<>();
    }

    /**
     * Get the number of thresholds in this group
     */
    public int getThresholdCount() {
        return complianceThresholds != null ? complianceThresholds.size() : 0;
    }

    /**
     * Check if this group has any thresholds
     */
    public boolean hasThresholds() {
        return getThresholdCount() > 0;
    }

    /**
     * Get the standard name for display purposes
     */
    public String getStandardName() {
        return standard != null ? standard.getName() : null;
    }

    /**
     * Get the standard display name for UI purposes
     */
    public String getStandardDisplayName() {
        return standard != null ? standard.getDisplayName() : null;
    }

    /**
     * Returns display name combining group name and standard
     */
    public String getDisplayName() {
        String standardName = getStandardName();
        if (standardName != null) {
            return String.format("%s (%s)", name, standardName);
        }
        return name;
    }

    /**
     * Add a threshold to this group
     */
    public void addThreshold(ComplianceThreshold threshold) {
        if (threshold != null) {
            if (complianceThresholds == null) {
                complianceThresholds = new ArrayList<>();
            }
            complianceThresholds.add(threshold);
            threshold.setGroup(this);
        }
    }

    /**
     * Remove a threshold from this group
     */
    public void removeThreshold(ComplianceThreshold threshold) {
        if (threshold != null && complianceThresholds != null) {
            complianceThresholds.remove(threshold);
            threshold.setGroup(null);
        }
    }

    /**
     * Clear all thresholds from this group
     */
    public void clearThresholds() {
        if (complianceThresholds != null) {
            for (ComplianceThreshold threshold : complianceThresholds) {
                threshold.setGroup(null);
            }
            complianceThresholds.clear();
        }
    }

    // Convenience methods for test compatibility

    /**
     * Alias for setIsMandatory() for test compatibility
     */
    public void setMandatory(Boolean mandatory) {
        setIsMandatory(mandatory);
    }

    @Override
    public String toString() {
        return "ParameterGroup{" + "id='" + id + '\'' + ", name='" + name + '\'' + ", sortOrder=" + sortOrder
                + ", standardId='" + getStandardId() + '\'' + ", thresholdCount=" + getThresholdCount() + '}';
    }
}