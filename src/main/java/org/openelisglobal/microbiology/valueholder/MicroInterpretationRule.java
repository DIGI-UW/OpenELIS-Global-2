package org.openelisglobal.microbiology.valueholder;

import jakarta.persistence.*;
import java.util.UUID;
import org.openelisglobal.common.valueholder.BaseObject;

@Entity
@Table(name = "micro_interpretation_rule", schema = "clinlims")
public class MicroInterpretationRule extends BaseObject<String> {
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

    @Column(name = "antibiotic_id", nullable = false, length = 36)
    private String antibioticId;

    public String getAntibioticId() {
        return antibioticId;
    }

    public void setAntibioticId(String value) {
        antibioticId = value;
    }

    @Column(name = "organism_id", length = 36)
    private String organismId;

    public String getOrganismId() {
        return organismId;
    }

    public void setOrganismId(String value) {
        organismId = value;
    }

    @Column(name = "organism_group", length = 100)
    private String organismGroup;

    public String getOrganismGroup() {
        return organismGroup;
    }

    public void setOrganismGroup(String value) {
        organismGroup = value;
    }

    @Column(name = "interpretation", nullable = false, length = 20)
    private String interpretation;

    public String getInterpretation() {
        return interpretation;
    }

    public void setInterpretation(String value) {
        interpretation = value;
    }

    @Column(name = "source", length = 200)
    private String source;

    public String getSource() {
        return source;
    }

    public void setSource(String value) {
        source = value;
    }

    @Column(name = "active", nullable = false)
    private boolean active = true;

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean value) {
        active = value;
    }
}
