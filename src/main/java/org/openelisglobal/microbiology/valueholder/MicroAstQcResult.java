package org.openelisglobal.microbiology.valueholder;

import jakarta.persistence.*;
import java.util.UUID;
import org.openelisglobal.common.valueholder.BaseObject;

@Entity
@Table(name = "micro_ast_qc_result", schema = "clinlims")
public class MicroAstQcResult extends BaseObject<String> {
    private static final long serialVersionUID = 1L;
    @Id
    @Column(name = "id", length = 36)
    private String id = UUID.randomUUID().toString();

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String value) {
        id = value;
    }

    @Column(name = "control_id", nullable = false, length = 36)
    private String controlId;

    public String getControlId() {
        return controlId;
    }

    public void setControlId(String value) {
        controlId = value;
    }

    @Column(name = "ast_run_id", nullable = false, length = 36)
    private String astRunId;

    public String getAstRunId() {
        return astRunId;
    }

    public void setAstRunId(String value) {
        astRunId = value;
    }

    @Column(name = "antibiotic_id", nullable = false, length = 36)
    private String antibioticId;

    public String getAntibioticId() {
        return antibioticId;
    }

    public void setAntibioticId(String value) {
        antibioticId = value;
    }

    @Column(name = "qc_result_id", nullable = false, length = 36)
    private String qcResultId;

    public String getQcResultId() {
        return qcResultId;
    }

    public void setQcResultId(String value) {
        qcResultId = value;
    }
}
