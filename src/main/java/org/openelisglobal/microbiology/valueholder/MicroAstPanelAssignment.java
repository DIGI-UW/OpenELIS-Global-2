package org.openelisglobal.microbiology.valueholder;

import jakarta.persistence.*;
import java.sql.Timestamp;
import java.util.UUID;
import org.openelisglobal.common.valueholder.BaseObject;

@Entity
@Table(name = "micro_ast_panel_assignment", schema = "clinlims")
public class MicroAstPanelAssignment extends BaseObject<String> {
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

    @Column(name = "isolate_id", nullable = false, length = 36)
    private String isolateId;

    public String getIsolateId() {
        return isolateId;
    }

    public void setIsolateId(String value) {
        isolateId = value;
    }

    @Column(name = "panel_id", nullable = false, length = 36)
    private String panelId;

    public String getPanelId() {
        return panelId;
    }

    public void setPanelId(String value) {
        panelId = value;
    }

    @Column(name = "panel_family", nullable = false, length = 36)
    private String panelFamily;

    public String getPanelFamily() {
        return panelFamily;
    }

    public void setPanelFamily(String value) {
        panelFamily = value;
    }

    @Column(name = "provenance", nullable = false, length = 20)
    private String provenance;

    public String getProvenance() {
        return provenance;
    }

    public void setProvenance(String value) {
        provenance = value;
    }

    @Column(name = "assigned_by", nullable = false, length = 20)
    private String assignedBy;

    public String getAssignedBy() {
        return assignedBy;
    }

    public void setAssignedBy(String value) {
        assignedBy = value;
    }

    @Column(name = "assigned_at", nullable = false)
    private Timestamp assignedAt;

    public Timestamp getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(Timestamp value) {
        assignedAt = value;
    }

    @Column(name = "retired_at", nullable = true)
    private Timestamp retiredAt;

    public Timestamp getRetiredAt() {
        return retiredAt;
    }

    public void setRetiredAt(Timestamp value) {
        retiredAt = value;
    }
}
