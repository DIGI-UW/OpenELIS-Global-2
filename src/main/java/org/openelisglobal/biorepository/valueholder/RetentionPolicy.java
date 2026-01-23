package org.openelisglobal.biorepository.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * Retention policy configuration for biorepository samples. Policies can be
 * defined by project, sample type, or both.
 *
 * Priority rule: Project-specific policies take precedence over sample type
 * policies. If a sample has a project, the project policy is applied. If no
 * project, the sample type policy is used as fallback.
 */
@Entity
@Table(name = "biorepository_retention_policy", schema = "clinlims")
public class RetentionPolicy extends BaseObject<Integer> {

    /**
     * Time unit for retention period.
     */
    public enum PeriodUnit {
        DAYS, MONTHS, YEARS
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "retention_policy_generator")
    @SequenceGenerator(name = "retention_policy_generator", sequenceName = "biorepository_retention_policy_seq", schema = "clinlims", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    /**
     * Human-readable policy name for display.
     */
    @NotNull(message = "Policy name is required")
    @Size(max = 100)
    @Column(name = "policy_name", nullable = false, length = 100)
    private String policyName;

    /**
     * Project ID this policy applies to. If null, policy is sample-type based.
     */
    @Column(name = "project_id")
    private Integer projectId;

    /**
     * Project name for display purposes (denormalized for easier querying/display).
     */
    @Size(max = 255)
    @Column(name = "project_name", length = 255)
    private String projectName;

    /**
     * Sample type ID this policy applies to. If null, policy is project-based.
     */
    @Column(name = "sample_type_id")
    private Integer sampleTypeId;

    /**
     * Sample type name for display purposes (denormalized for easier
     * querying/display).
     */
    @Size(max = 255)
    @Column(name = "sample_type_name", length = 255)
    private String sampleTypeName;

    /**
     * Numeric value of the retention period.
     */
    @NotNull(message = "Period value is required")
    @Column(name = "period_value", nullable = false)
    private Integer periodValue;

    /**
     * Unit for the retention period (DAYS, MONTHS, YEARS).
     */
    @NotNull(message = "Period unit is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "period_unit", nullable = false, length = 10)
    private PeriodUnit periodUnit = PeriodUnit.YEARS;

    /**
     * Whether this policy is currently active.
     */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    /**
     * Optional description or notes about this policy.
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "sys_user_id", nullable = false, length = 36)
    private String sysUserId;

    // Default constructor required by JPA
    public RetentionPolicy() {
    }

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    @Override
    public String getSysUserId() {
        return sysUserId;
    }

    @Override
    public void setSysUserId(String sysUserId) {
        this.sysUserId = sysUserId;
    }

    public String getPolicyName() {
        return policyName;
    }

    public void setPolicyName(String policyName) {
        this.policyName = policyName;
    }

    public Integer getProjectId() {
        return projectId;
    }

    public void setProjectId(Integer projectId) {
        this.projectId = projectId;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public Integer getSampleTypeId() {
        return sampleTypeId;
    }

    public void setSampleTypeId(Integer sampleTypeId) {
        this.sampleTypeId = sampleTypeId;
    }

    public String getSampleTypeName() {
        return sampleTypeName;
    }

    public void setSampleTypeName(String sampleTypeName) {
        this.sampleTypeName = sampleTypeName;
    }

    public Integer getPeriodValue() {
        return periodValue;
    }

    public void setPeriodValue(Integer periodValue) {
        this.periodValue = periodValue;
    }

    public PeriodUnit getPeriodUnit() {
        return periodUnit;
    }

    public void setPeriodUnit(PeriodUnit periodUnit) {
        this.periodUnit = periodUnit;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Returns a human-readable period string (e.g., "5 years", "18 months").
     */
    public String getPeriodDisplay() {
        String unitStr = periodUnit.name().toLowerCase();
        if (periodValue == 1) {
            // Singular: "1 year" instead of "1 years"
            unitStr = unitStr.substring(0, unitStr.length() - 1);
        }
        return periodValue + " " + unitStr;
    }

    /**
     * Calculates the retention expiry date from a given start date.
     *
     * @param fromDate the start date (typically sample collection or receipt date)
     * @return the calculated expiry date
     */
    public LocalDate calculateExpiryDate(LocalDate fromDate) {
        if (fromDate == null) {
            return null;
        }
        return switch (periodUnit) {
        case DAYS -> fromDate.plusDays(periodValue);
        case MONTHS -> fromDate.plusMonths(periodValue);
        case YEARS -> fromDate.plusYears(periodValue);
        };
    }

    /**
     * Parses a period string like "5 years", "18 months", "30 days", or just "5"
     * (defaults to years).
     *
     * @param periodStr the period string to parse
     * @return array of [value, unit] where unit is PeriodUnit
     */
    public static Object[] parsePeriodString(String periodStr) {
        if (periodStr == null || periodStr.trim().isEmpty()) {
            throw new IllegalArgumentException("Period string cannot be empty");
        }

        String normalized = periodStr.trim().toLowerCase();

        // Try to extract number and unit
        String[] parts = normalized.split("\\s+");

        int value;
        PeriodUnit unit = PeriodUnit.YEARS; // Default to years

        try {
            value = Integer.parseInt(parts[0]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid period value: " + parts[0]);
        }

        if (parts.length > 1) {
            String unitStr = parts[1];
            if (unitStr.startsWith("day")) {
                unit = PeriodUnit.DAYS;
            } else if (unitStr.startsWith("month")) {
                unit = PeriodUnit.MONTHS;
            } else if (unitStr.startsWith("year")) {
                unit = PeriodUnit.YEARS;
            } else {
                throw new IllegalArgumentException("Unknown period unit: " + unitStr);
            }
        }

        return new Object[] { value, unit };
    }

    /**
     * Checks if this policy applies to a project (vs. sample type only).
     */
    public boolean isProjectPolicy() {
        return projectId != null;
    }

    /**
     * Checks if this policy applies to a sample type (vs. project only).
     */
    public boolean isSampleTypePolicy() {
        return sampleTypeId != null;
    }
}
