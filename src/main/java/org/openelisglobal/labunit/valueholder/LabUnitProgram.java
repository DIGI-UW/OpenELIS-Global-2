package org.openelisglobal.labunit.valueholder;

import java.sql.Timestamp;
import java.util.Objects;
import org.openelisglobal.common.valueholder.BaseObject;

@jakarta.persistence.Entity
@jakarta.persistence.Table(name = "lab_unit_program")
public class LabUnitProgram extends BaseObject<String> {
    private static final long serialVersionUID = 1L;

    @jakarta.persistence.Id
    @jakarta.persistence.Column(name = "id", length = 36)
    private String id;

    @jakarta.persistence.Column(name = "lab_unit_id", length = 36, nullable = false)
    private String labUnitId;

    @jakarta.persistence.Column(name = "program_id", length = 36, nullable = false)
    private String programId;

    @jakarta.persistence.Column(name = "default_order_form_id", length = 36)
    private String defaultOrderFormId;

    @jakarta.persistence.Column(name = "created_at")
    private Timestamp createdAt;

    public LabUnitProgram() {
        super();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLabUnitId() {
        return labUnitId;
    }

    public void setLabUnitId(String labUnitId) {
        this.labUnitId = labUnitId;
    }

    public String getProgramId() {
        return programId;
    }

    public void setProgramId(String programId) {
        this.programId = programId;
    }

    public String getDefaultOrderFormId() {
        return defaultOrderFormId;
    }

    public void setDefaultOrderFormId(String defaultOrderFormId) {
        this.defaultOrderFormId = defaultOrderFormId;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        LabUnitProgram that = (LabUnitProgram) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}