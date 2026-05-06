package org.openelisglobal.compliance.valueholder;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
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
 * ComplianceThreshold value holder representing individual parameter thresholds
 * within parameter groups.
 *
 * Follows constitutional requirements: - Extends BaseObject for audit trail
 * support - Includes FHIR UUID for interoperability - Uses JPA annotations (no
 * XML mappings) - Implements validation annotations
 */
@Entity
@Table(name = "compliance_threshold")
public class ComplianceThreshold extends BaseObject<String> implements SimpleBaseEntity<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", precision = 10, scale = 0)
    @GeneratedValue(generator = "compliance_threshold_seq_gen")
    @GenericGenerator(name = "compliance_threshold_seq_gen", strategy = "org.openelisglobal.hibernate.resources.StringSequenceGenerator", parameters = @Parameter(name = "sequence_name", value = "compliance_threshold_seq"))
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String id;

    @NotNull
    @Column(name = "fhir_uuid", unique = true, nullable = false)
    private UUID fhirUuid;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    @JsonBackReference("group-thresholds")
    @NotNull(message = "Parameter group is required")
    private ParameterGroup group;

    @NotBlank(message = "Parameter code is required")
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "parameter_code", nullable = false)
    private String parameterCode;

    @NotBlank(message = "Display name is required")
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "display_name", nullable = false)
    private String displayName;

    @NotNull(message = "Threshold type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "threshold_type", nullable = false)
    private ThresholdType thresholdType;

    @DecimalMin(value = "0", inclusive = false, message = "Minimum value must be positive")
    @Column(name = "min_value", precision = 15, scale = 6)
    private BigDecimal minValue;

    @DecimalMin(value = "0", inclusive = false, message = "Maximum value must be positive")
    @Column(name = "max_value", precision = 15, scale = 6)
    private BigDecimal maxValue;

    @DecimalMin(value = "0", inclusive = false, message = "Target value must be positive")
    @Column(name = "target_value", precision = 15, scale = 6)
    private BigDecimal targetValue;

    /**
     * Free-text qualitative condition for DESCRIPTIVE thresholds (e.g. "No odor",
     * "Clear", "Absent"). Null for numeric threshold types.
     */
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "value_descriptive", length = 1024)
    private String valueDescriptive;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "units")
    private String units;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "method_reference")
    private String methodReference;

    @DecimalMin(value = "0", inclusive = false, message = "Detection limit must be positive")
    @Column(name = "detection_limit", precision = 15, scale = 6)
    private BigDecimal detectionLimit;

    @NotNull
    @Column(name = "is_mandatory", nullable = false)
    private Boolean isMandatory = true;

    /**
     * Soft-delete flag (FRS Section 5). FR-3-010 lets an admin archive a threshold
     * instead of hard-deleting once it has been used in evaluations; the archive
     * flow flips this to false. Defaults to true on insert.
     */
    @NotNull
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @NotNull
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "validation_rules", columnDefinition = "TEXT")
    private String validationRules;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "sys_user_id", nullable = false)
    private Integer systemUserId;

    /**
     * Optional FK to the test catalog entry this threshold applies to. Per FRS data
     * model the relationship was a String "testId" originally; promoted here to a
     * real {@link Test} reference so cross-module joins can use a proper FK
     * (FR-3-001 / FR-4-001). Nullable - thresholds can also exist standalone as
     * part of a parameter group.
     *
     * Compatibility: getTestId() / setTestId(String) are kept below as derived
     * shims so existing callers and HQL queries keep working without a full sweep.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "test_id", referencedColumnName = "id", nullable = true)
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    private org.openelisglobal.test.valueholder.Test test;

    // Bidirectional relationship with value mappings for select list tests
    @OneToMany(mappedBy = "threshold", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("threshold-mappings")
    // Batch the SELECT_MAP option-mapping fetches so listing N thresholds in a
    // group doesn't fan out into N selects on compliance_threshold_value_map.
    @org.hibernate.annotations.BatchSize(size = 50)
    private List<ComplianceThresholdValueMap> valueMappings = new ArrayList<>();

    public ComplianceThreshold() {
        super();
        generateFhirUuid();
    }

    public ComplianceThreshold(String parameterCode, String displayName, ThresholdType thresholdType) {
        this();
        this.parameterCode = parameterCode;
        this.displayName = displayName;
        this.thresholdType = thresholdType;
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

    public ParameterGroup getGroup() {
        return group;
    }

    public void setGroup(ParameterGroup group) {
        this.group = group;
    }

    public String getGroupId() {
        return group != null ? group.getId() : null;
    }

    public void setGroupId(String groupId) {
        if (groupId != null) {
            if (this.group == null) {
                this.group = new ParameterGroup();
            }
            this.group.setId(groupId);
        } else {
            this.group = null;
        }
    }

    public String getParameterCode() {
        return parameterCode;
    }

    public void setParameterCode(String parameterCode) {
        this.parameterCode = parameterCode;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public ThresholdType getThresholdType() {
        return thresholdType;
    }

    public void setThresholdType(ThresholdType thresholdType) {
        this.thresholdType = thresholdType;
    }

    public BigDecimal getMinValue() {
        return minValue;
    }

    public void setMinValue(BigDecimal minValue) {
        this.minValue = minValue;
    }

    public BigDecimal getMaxValue() {
        return maxValue;
    }

    public void setMaxValue(BigDecimal maxValue) {
        this.maxValue = maxValue;
    }

    public BigDecimal getTargetValue() {
        return targetValue;
    }

    public void setTargetValue(BigDecimal targetValue) {
        this.targetValue = targetValue;
    }

    public String getValueDescriptive() {
        return valueDescriptive;
    }

    public void setValueDescriptive(String valueDescriptive) {
        this.valueDescriptive = valueDescriptive;
    }

    public String getUnits() {
        return units;
    }

    public void setUnits(String units) {
        this.units = units;
    }

    public String getMethodReference() {
        return methodReference;
    }

    public void setMethodReference(String methodReference) {
        this.methodReference = methodReference;
    }

    public BigDecimal getDetectionLimit() {
        return detectionLimit;
    }

    public void setDetectionLimit(BigDecimal detectionLimit) {
        this.detectionLimit = detectionLimit;
    }

    public Boolean getIsMandatory() {
        return isMandatory;
    }

    public void setIsMandatory(Boolean isMandatory) {
        this.isMandatory = isMandatory != null ? isMandatory : true;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive != null ? isActive : true;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder != null ? sortOrder : 0;
    }

    public String getValidationRules() {
        return validationRules;
    }

    public void setValidationRules(String validationRules) {
        this.validationRules = validationRules;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
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

    public org.openelisglobal.test.valueholder.Test getTest() {
        return test;
    }

    public void setTest(org.openelisglobal.test.valueholder.Test test) {
        this.test = test;
    }

    /**
     * Compatibility shim for the legacy String testId column. Reads
     * {@code test.id}; the setter builds a transient Test stub holding only the id
     * - callers persisting a threshold should preferably use {@link #setTest(Test)}
     * with a managed entity to avoid Hibernate's TransientPropertyValueException
     * when test_id is non-null.
     */
    public String getTestId() {
        return test != null ? test.getId() : null;
    }

    public void setTestId(String testId) {
        if (testId != null && !testId.trim().isEmpty()) {
            if (this.test == null) {
                this.test = new org.openelisglobal.test.valueholder.Test();
            }
            this.test.setId(testId);
        } else {
            this.test = null;
        }
    }

    public List<ComplianceThresholdValueMap> getValueMappings() {
        return valueMappings;
    }

    public void setValueMappings(List<ComplianceThresholdValueMap> valueMappings) {
        this.valueMappings = valueMappings != null ? valueMappings : new ArrayList<>();
    }

    /**
     * Get the parameter group name for display purposes
     */
    public String getGroupName() {
        return group != null ? group.getName() : null;
    }

    /**
     * Get the standard name from the associated group
     */
    public String getStandardName() {
        return group != null ? group.getStandardName() : null;
    }

    /**
     * Returns the full parameter display name with units
     */
    @JsonIgnore
    public String getFullDisplayName() {
        if (units != null && !units.trim().isEmpty()) {
            return String.format("%s (%s)", displayName, units);
        }
        return displayName;
    }

    /**
     * Get the threshold range as a display string
     */
    @JsonIgnore
    public String getThresholdRangeDisplay() {
        switch (thresholdType) {
        case RANGE:
            return String.format("%s - %s %s", formatValue(minValue), formatValue(maxValue),
                    units != null ? units : "");
        case MINIMUM:
            return String.format("≥ %s %s", formatValue(minValue), units != null ? units : "");
        case MAXIMUM:
            return String.format("≤ %s %s", formatValue(maxValue), units != null ? units : "");
        case EXACT:
            return String.format("= %s %s", formatValue(targetValue), units != null ? units : "");
        case BORDERLINE:
            return String.format("(advisory) %s - %s %s", formatValue(minValue), formatValue(maxValue),
                    units != null ? units : "");
        case DESCRIPTIVE:
            return valueDescriptive != null ? valueDescriptive : "";
        case SELECT_MAP:
            return String.format("Select-list mapping (%d option%s)", valueMappings != null ? valueMappings.size() : 0,
                    valueMappings != null && valueMappings.size() == 1 ? "" : "s");
        default:
            return "";
        }
    }

    /**
     * Evaluate a numeric value against this threshold
     */
    public boolean evaluateNumericValue(BigDecimal value) {
        if (thresholdType == null) {
            return false;
        }
        return thresholdType.evaluate(value, minValue, maxValue, targetValue);
    }

    /**
     * Evaluate a text value against this threshold
     */
    public boolean evaluateTextValue(String value) {
        if (thresholdType == null) {
            return false;
        }
        return thresholdType.evaluate(value, null);
    }

    /**
     * Check if this threshold requires validation
     */
    public boolean requiresValidation() {
        return thresholdType != null && (thresholdType.requiresMinValue() || thresholdType.requiresMaxValue()
                || thresholdType.requiresTargetValue());
    }

    /**
     * Validate threshold configuration
     */
    @JsonIgnore
    public boolean isValidConfiguration() {
        if (thresholdType == null) {
            return false;
        }

        switch (thresholdType) {
        case RANGE:
        case BORDERLINE:
            return minValue != null && maxValue != null && minValue.compareTo(maxValue) <= 0;
        case MINIMUM:
            return minValue != null;
        case MAXIMUM:
            return maxValue != null;
        case EXACT:
            return targetValue != null;
        case DESCRIPTIVE:
            return valueDescriptive != null && !valueDescriptive.trim().isEmpty();
        case SELECT_MAP:
            // SELECT_MAP parents are valid as long as they carry at least one
            // option mapping; the mappings themselves live in valueMappings.
            return valueMappings != null && !valueMappings.isEmpty();
        default:
            return false;
        }
    }

    /**
     * Get validation error message if configuration is invalid
     */
    @JsonIgnore
    public String getValidationError() {
        if (isValidConfiguration()) {
            return null;
        }

        if (thresholdType == null) {
            return "Threshold type is required";
        }

        switch (thresholdType) {
        case RANGE:
        case BORDERLINE:
            if (minValue == null || maxValue == null) {
                return thresholdType.getDisplayName() + " threshold requires both minimum and maximum values";
            } else if (minValue.compareTo(maxValue) > 0) {
                return "minimum value cannot exceed maximum value";
            }
            break;
        case MINIMUM:
            if (minValue == null) {
                return "Minimum threshold requires a minimum value";
            }
            break;
        case MAXIMUM:
            if (maxValue == null) {
                return "Maximum threshold requires a maximum value";
            }
            break;
        case EXACT:
            if (targetValue == null) {
                return thresholdType.getDisplayName() + " threshold requires a target value";
            }
            break;
        case DESCRIPTIVE:
            if (valueDescriptive == null || valueDescriptive.trim().isEmpty()) {
                return "Descriptive threshold requires a qualitative value";
            }
            break;
        case SELECT_MAP:
            if (valueMappings == null || valueMappings.isEmpty()) {
                return "Select-list threshold requires at least one option mapping";
            }
            break;
        default:
            break;
        }

        return "Invalid threshold configuration";
    }

    private String formatValue(BigDecimal value) {
        if (value == null) {
            return "";
        }
        // Remove trailing zeros and unnecessary decimal point
        return value.stripTrailingZeros().toPlainString();
    }

    @Override
    public String toString() {
        return "ComplianceThreshold{" + "id='" + id + '\'' + ", parameterCode='" + parameterCode + '\''
                + ", displayName='" + displayName + '\'' + ", thresholdType=" + thresholdType + ", groupId='"
                + getGroupId() + '\'' + '}';
    }
}