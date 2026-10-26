package org.openelisglobal.microbiology.valueholder;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.openelisglobal.common.valueholder.BaseObject;

@Entity
@Table(name = "micro_reporting_rule", schema = "clinlims")
public class MicroReportingRule extends BaseObject<String> {
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

    @Column(name = "clinical_context", length = 40)
    private String clinicalContext;

    public String getClinicalContext() {
        return clinicalContext;
    }

    public void setClinicalContext(String value) {
        clinicalContext = value;
    }

    @Column(name = "report_behavior", nullable = false, length = 30)
    private String reportBehavior;

    public String getReportBehavior() {
        return reportBehavior;
    }

    public void setReportBehavior(String value) {
        reportBehavior = value;
    }

    @Column(name = "cascade_condition", length = 1000)
    private String cascadeCondition;

    public String getCascadeCondition() {
        return cascadeCondition;
    }

    public void setCascadeCondition(String value) {
        cascadeCondition = value;
    }

    @Column(name = "note", length = 1000)
    private String note;

    public String getNote() {
        return note;
    }

    public void setNote(String value) {
        note = value;
    }

    @Column(name = "note_when", length = 10)
    private String noteWhen;

    public String getNoteWhen() {
        return noteWhen;
    }

    public void setNoteWhen(String value) {
        noteWhen = value;
    }

    public String noteFor(String interpretation) {
        if (!active || note == null || noteWhen == null) {
            return null;
        }
        return switch (noteWhen) {
        case "ALWAYS" -> note;
        case "WHEN_R" -> "RESISTANT".equals(interpretation) ? note : null;
        case "WHEN_S" -> "SUSCEPTIBLE".equals(interpretation) ? note : null;
        default -> null;
        };
    }

    @Column(name = "add_automatically", nullable = false)
    private boolean addAutomatically;

    public boolean isAddAutomatically() {
        return addAutomatically;
    }

    public void setAddAutomatically(boolean value) {
        addAutomatically = value;
    }

    @Column(name = "active", nullable = false)
    private boolean active = true;

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean value) {
        active = value;
    }

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "micro_reporting_rule_sample_type", schema = "clinlims", joinColumns = @JoinColumn(name = "rule_id"))
    @Column(name = "sample_type_id", length = 20)
    private Set<String> sampleTypeIds = new HashSet<>();

    public Set<String> getSampleTypeIds() {
        return sampleTypeIds;
    }

    public void setSampleTypeIds(Set<String> value) {
        sampleTypeIds = value == null ? new HashSet<>() : value;
    }
}
