package org.openelisglobal.compliance.controller.rest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.compliance.valueholder.ComplianceThreshold;
import org.openelisglobal.compliance.valueholder.ComplianceThresholdValueMap;

/**
 * Slim response wrapper for the compliance-threshold endpoints.
 *
 * <p>
 * Returning the {@link ComplianceThreshold} entity directly fails Jackson
 * serialization (HttpMessageNotWritableException) because the entity carries a
 * lazy {@code valueMappings} collection plus back-references through
 * {@code group} → {@code standard}; the proxies + cycle break the writer
 * outside the transaction. This DTO copies only the fields the admin UI reads
 * (name, type, limits, units, plus a flattened group/standard summary and the
 * select-map options). The shape preserves {@code t.group.standard.name} style
 * lookups so the frontend code doesn't change.
 */
public class ComplianceThresholdListItem {

    public static class StandardSummary {
        private final String id;
        private final String name;
        private final String regulationNumber;
        private final String status;
        private final LocalDate effectiveDate;

        StandardSummary(String id, String name, String regulationNumber, String status, LocalDate effectiveDate) {
            this.id = id;
            this.name = name;
            this.regulationNumber = regulationNumber;
            this.status = status;
            this.effectiveDate = effectiveDate;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getRegulationNumber() {
            return regulationNumber;
        }

        public String getStatus() {
            return status;
        }

        public LocalDate getEffectiveDate() {
            return effectiveDate;
        }
    }

    public static class GroupSummary {
        private final String id;
        private final String name;
        private final StandardSummary standard;

        GroupSummary(String id, String name, StandardSummary standard) {
            this.id = id;
            this.name = name;
            this.standard = standard;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public StandardSummary getStandard() {
            return standard;
        }
    }

    public static class ValueMapping {
        private final String id;
        private final String optionValue;
        private final String complianceStatus;

        ValueMapping(String id, String optionValue, String complianceStatus) {
            this.id = id;
            this.optionValue = optionValue;
            this.complianceStatus = complianceStatus;
        }

        public String getId() {
            return id;
        }

        public String getOptionValue() {
            return optionValue;
        }

        public String getComplianceStatus() {
            return complianceStatus;
        }
    }

    private final String id;
    private final String parameterCode;
    private final String displayName;
    private final String thresholdType;
    private final BigDecimal minValue;
    private final BigDecimal maxValue;
    private final BigDecimal targetValue;
    private final BigDecimal detectionLimit;
    private final String valueDescriptive;
    private final String units;
    private final String methodReference;
    private final String notes;
    private final Boolean isMandatory;
    private final Integer sortOrder;
    private final String testId;
    private final GroupSummary group;
    private final List<ValueMapping> valueMappings;

    public ComplianceThresholdListItem(ComplianceThreshold t) {
        this.id = t.getId();
        this.parameterCode = t.getParameterCode();
        this.displayName = t.getDisplayName();
        this.thresholdType = t.getThresholdType() != null ? t.getThresholdType().name() : null;
        this.minValue = t.getMinValue();
        this.maxValue = t.getMaxValue();
        this.targetValue = t.getTargetValue();
        this.detectionLimit = t.getDetectionLimit();
        this.valueDescriptive = t.getValueDescriptive();
        this.units = t.getUnits();
        this.methodReference = t.getMethodReference();
        this.notes = t.getNotes();
        this.isMandatory = t.getIsMandatory();
        this.sortOrder = t.getSortOrder();
        this.testId = t.getTestId();
        this.group = buildGroup(t);
        this.valueMappings = buildValueMappings(t);
    }

    private static GroupSummary buildGroup(ComplianceThreshold t) {
        if (t.getGroup() == null) {
            return null;
        }
        StandardSummary standard = null;
        if (t.getGroup().getStandard() != null) {
            standard = new StandardSummary(t.getGroup().getStandard().getId(), t.getGroup().getStandard().getName(),
                    t.getGroup().getStandard().getRegulationNumber(),
                    t.getGroup().getStandard().getStatus() != null ? t.getGroup().getStandard().getStatus().name()
                            : null,
                    t.getGroup().getStandard().getEffectiveDate());
        }
        return new GroupSummary(t.getGroup().getId(), t.getGroup().getName(), standard);
    }

    private static List<ValueMapping> buildValueMappings(ComplianceThreshold t) {
        List<ValueMapping> out = new ArrayList<>();
        if (t.getValueMappings() == null) {
            return out;
        }
        for (ComplianceThresholdValueMap m : t.getValueMappings()) {
            out.add(new ValueMapping(m.getId(), m.getOptionValue(),
                    m.getComplianceStatus() != null ? m.getComplianceStatus().name() : null));
        }
        return out;
    }

    public String getId() {
        return id;
    }

    public String getParameterCode() {
        return parameterCode;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getThresholdType() {
        return thresholdType;
    }

    public BigDecimal getMinValue() {
        return minValue;
    }

    public BigDecimal getMaxValue() {
        return maxValue;
    }

    public BigDecimal getTargetValue() {
        return targetValue;
    }

    public BigDecimal getDetectionLimit() {
        return detectionLimit;
    }

    public String getValueDescriptive() {
        return valueDescriptive;
    }

    public String getUnits() {
        return units;
    }

    public String getMethodReference() {
        return methodReference;
    }

    public String getNotes() {
        return notes;
    }

    public Boolean getIsMandatory() {
        return isMandatory;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public String getTestId() {
        return testId;
    }

    public GroupSummary getGroup() {
        return group;
    }

    public List<ValueMapping> getValueMappings() {
        return valueMappings;
    }
}
