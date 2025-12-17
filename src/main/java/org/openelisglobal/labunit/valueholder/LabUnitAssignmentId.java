package org.openelisglobal.labunit.valueholder;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class LabUnitAssignmentId implements Serializable {
    private Long id;
    private String labUnitId;

    public LabUnitAssignmentId() {
    }

    public LabUnitAssignmentId(Long id, String labUnitId) {
        this.id = id;
        this.labUnitId = labUnitId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLabUnitId() {
        return labUnitId;
    }

    public void setLabUnitId(String labUnitId) {
        this.labUnitId = labUnitId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        LabUnitAssignmentId that = (LabUnitAssignmentId) o;
        return Objects.equals(id, that.id) && Objects.equals(labUnitId, that.labUnitId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, labUnitId);
    }
}