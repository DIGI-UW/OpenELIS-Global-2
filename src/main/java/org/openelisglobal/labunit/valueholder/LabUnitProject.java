package org.openelisglobal.labunit.valueholder;

import java.sql.Timestamp;
import java.util.Objects;
import org.openelisglobal.common.valueholder.BaseObject;

@jakarta.persistence.Entity
@jakarta.persistence.Table(name = "lab_unit_project")
public class LabUnitProject extends BaseObject<String> {
    private static final long serialVersionUID = 1L;

    @jakarta.persistence.Id
    @jakarta.persistence.Column(name = "id", length = 36)
    private String id;

    @jakarta.persistence.Column(name = "lab_unit_id", length = 36, nullable = false)
    private String labUnitId;

    @jakarta.persistence.Column(name = "project_id", length = 36, nullable = false)
    private String projectId;

    @jakarta.persistence.Column(name = "created_at")
    private Timestamp createdAt;

    public LabUnitProject() {
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

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
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
        LabUnitProject that = (LabUnitProject) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}