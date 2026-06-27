package org.openelisglobal.microbiology.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.UUID;
import org.openelisglobal.common.valueholder.BaseObject;

@Entity
@Table(name = "micro_ast_reading", schema = "clinlims")
public class MicroAstReading extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", length = 36)
    private String id = UUID.randomUUID().toString();

    @Column(name = "ast_run_id", nullable = false, length = 36)
    private String astRunId;

    @Column(name = "antibiotic_id", nullable = false, length = 36)
    private String antibioticId;

    @Column(name = "method", nullable = false, length = 20)
    private String method;

    @Column(name = "raw_value", precision = 12, scale = 4)
    private BigDecimal rawValue;

    @Column(name = "raw_text")
    private String rawText;

    @Column(name = "interpretation", nullable = false, length = 40)
    private String interpretation;

    @Column(name = "breakpoint_rule_id", length = 36)
    private String breakpointRuleId;

    @Column(name = "override_interpretation", length = 40)
    private String overrideInterpretation;

    @Column(name = "override_reason")
    private String overrideReason;

    @Column(name = "created_at", nullable = false)
    private Timestamp createdAt = new Timestamp(System.currentTimeMillis());

    @Column(name = "created_by", length = 20)
    private String createdBy;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getAstRunId() {
        return astRunId;
    }

    public void setAstRunId(String astRunId) {
        this.astRunId = astRunId;
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

    public BigDecimal getRawValue() {
        return rawValue;
    }

    public void setRawValue(BigDecimal rawValue) {
        this.rawValue = rawValue;
    }

    public String getRawText() {
        return rawText;
    }

    public void setRawText(String rawText) {
        this.rawText = rawText;
    }

    public String getInterpretation() {
        return interpretation;
    }

    public void setInterpretation(String interpretation) {
        this.interpretation = interpretation;
    }

    public String getBreakpointRuleId() {
        return breakpointRuleId;
    }

    public void setBreakpointRuleId(String breakpointRuleId) {
        this.breakpointRuleId = breakpointRuleId;
    }

    public String getOverrideInterpretation() {
        return overrideInterpretation;
    }

    public void setOverrideInterpretation(String overrideInterpretation) {
        this.overrideInterpretation = overrideInterpretation;
    }

    public String getOverrideReason() {
        return overrideReason;
    }

    public void setOverrideReason(String overrideReason) {
        this.overrideReason = overrideReason;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
}
