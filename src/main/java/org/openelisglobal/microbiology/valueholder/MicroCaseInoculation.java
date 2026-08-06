package org.openelisglobal.microbiology.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import java.util.UUID;
import org.openelisglobal.common.valueholder.BaseObject;

@Entity
@Table(name = "micro_case_inoculation", schema = "clinlims")
public class MicroCaseInoculation extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", length = 36)
    private String id = UUID.randomUUID().toString();

    @Column(name = "case_id", nullable = false, length = 36)
    private String caseId;

    @Column(name = "source_inoculation_id", length = 36)
    private String sourceInoculationId;

    @Column(name = "activity_id", nullable = false, length = 36)
    private String activityId;

    @Column(name = "method_id", length = 20)
    private String methodId;

    @Column(name = "container_identifier", nullable = false, length = 80)
    private String containerIdentifier;

    @Column(name = "media", nullable = false)
    private String media;

    @Column(name = "incubation")
    private String incubation;

    @Column(name = "atmosphere")
    private String atmosphere;

    @Column(name = "occurred_at", nullable = false)
    private Timestamp occurredAt = new Timestamp(System.currentTimeMillis());

    @Column(name = "performed_by", nullable = false, length = 20)
    private String performedBy;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getCaseId() {
        return caseId;
    }

    public void setCaseId(String caseId) {
        this.caseId = caseId;
    }

    public String getSourceInoculationId() {
        return sourceInoculationId;
    }

    public void setSourceInoculationId(String sourceInoculationId) {
        this.sourceInoculationId = sourceInoculationId;
    }

    public String getActivityId() {
        return activityId;
    }

    public void setActivityId(String activityId) {
        this.activityId = activityId;
    }

    public String getMethodId() {
        return methodId;
    }

    public void setMethodId(String methodId) {
        this.methodId = methodId;
    }

    public String getContainerIdentifier() {
        return containerIdentifier;
    }

    public void setContainerIdentifier(String containerIdentifier) {
        this.containerIdentifier = containerIdentifier;
    }

    public String getMedia() {
        return media;
    }

    public void setMedia(String media) {
        this.media = media;
    }

    public String getIncubation() {
        return incubation;
    }

    public void setIncubation(String incubation) {
        this.incubation = incubation;
    }

    public String getAtmosphere() {
        return atmosphere;
    }

    public void setAtmosphere(String atmosphere) {
        this.atmosphere = atmosphere;
    }

    public Timestamp getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Timestamp occurredAt) {
        this.occurredAt = occurredAt;
    }

    public String getPerformedBy() {
        return performedBy;
    }

    public void setPerformedBy(String performedBy) {
        this.performedBy = performedBy;
    }
}
