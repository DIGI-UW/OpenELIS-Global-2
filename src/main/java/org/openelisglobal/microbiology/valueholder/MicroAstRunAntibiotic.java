package org.openelisglobal.microbiology.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import org.openelisglobal.common.valueholder.BaseObject;

@Entity
@Table(name = "micro_ast_run_antibiotic", schema = "clinlims")
public class MicroAstRunAntibiotic extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", length = 36)
    private String id = UUID.randomUUID().toString();

    @Column(name = "ast_run_id", nullable = false, length = 36)
    private String astRunId;

    @Column(name = "antibiotic_id", nullable = false, length = 36)
    private String antibioticId;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @Column(name = "tier", nullable = false)
    private Integer tier = 1;

    @Column(name = "report_behavior", nullable = false, length = 30)
    private String reportBehavior = "ALWAYS";

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

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public Integer getTier() {
        return tier;
    }

    public void setTier(Integer tier) {
        this.tier = tier;
    }

    public String getReportBehavior() {
        return reportBehavior;
    }

    public void setReportBehavior(String reportBehavior) {
        this.reportBehavior = reportBehavior;
    }
}
