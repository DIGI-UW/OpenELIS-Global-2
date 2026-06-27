package org.openelisglobal.microbiology.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import org.hibernate.annotations.Type;
import org.openelisglobal.common.valueholder.BaseObject;

@Entity
@Table(name = "micro_breakpoint_rule", schema = "clinlims")
public class MicroBreakpointRule extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", length = 36)
    private String id = UUID.randomUUID().toString();

    @Column(name = "standard_id", nullable = false, length = 36)
    private String standardId;

    @Column(name = "organism_id", length = 36)
    private String organismId;

    @Column(name = "organism_group", length = 100)
    private String organismGroup;

    @Column(name = "antibiotic_id", nullable = false, length = 36)
    private String antibioticId;

    @Column(name = "method", length = 50)
    private String method;

    @Column(name = "specimen_type_id", precision = 10, scale = 0)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String specimenTypeId;

    @Column(name = "breakpoint_type", nullable = false, length = 20)
    private String breakpointType;

    @Column(name = "susceptible_value", precision = 12, scale = 4)
    private BigDecimal susceptibleValue;

    @Column(name = "intermediate_lower_value", precision = 12, scale = 4)
    private BigDecimal intermediateLowerValue;

    @Column(name = "intermediate_upper_value", precision = 12, scale = 4)
    private BigDecimal intermediateUpperValue;

    @Column(name = "resistant_value", precision = 12, scale = 4)
    private BigDecimal resistantValue;

    @Column(name = "is_active", nullable = false, length = 2)
    private String isActive = "Y";

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getStandardId() {
        return standardId;
    }

    public void setStandardId(String standardId) {
        this.standardId = standardId;
    }

    public String getOrganismId() {
        return organismId;
    }

    public void setOrganismId(String organismId) {
        this.organismId = organismId;
    }

    public String getOrganismGroup() {
        return organismGroup;
    }

    public void setOrganismGroup(String organismGroup) {
        this.organismGroup = organismGroup;
    }

    public String getAntibioticId() {
        return antibioticId;
    }

    public void setAntibioticId(String antibioticId) {
        this.antibioticId = antibioticId;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getSpecimenTypeId() {
        return specimenTypeId;
    }

    public void setSpecimenTypeId(String specimenTypeId) {
        this.specimenTypeId = specimenTypeId;
    }

    public String getBreakpointType() {
        return breakpointType;
    }

    public void setBreakpointType(String breakpointType) {
        this.breakpointType = breakpointType;
    }

    public BigDecimal getSusceptibleValue() {
        return susceptibleValue;
    }

    public void setSusceptibleValue(BigDecimal susceptibleValue) {
        this.susceptibleValue = susceptibleValue;
    }

    public BigDecimal getIntermediateLowerValue() {
        return intermediateLowerValue;
    }

    public void setIntermediateLowerValue(BigDecimal intermediateLowerValue) {
        this.intermediateLowerValue = intermediateLowerValue;
    }

    public BigDecimal getIntermediateUpperValue() {
        return intermediateUpperValue;
    }

    public void setIntermediateUpperValue(BigDecimal intermediateUpperValue) {
        this.intermediateUpperValue = intermediateUpperValue;
    }

    public BigDecimal getResistantValue() {
        return resistantValue;
    }

    public void setResistantValue(BigDecimal resistantValue) {
        this.resistantValue = resistantValue;
    }

    public String getIsActive() {
        return isActive;
    }

    public void setIsActive(String isActive) {
        this.isActive = isActive;
    }
}
