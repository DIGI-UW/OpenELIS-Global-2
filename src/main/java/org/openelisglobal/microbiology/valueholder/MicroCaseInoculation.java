package org.openelisglobal.microbiology.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import java.util.UUID;
import org.hibernate.annotations.Type;
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

    @Column(name = "method_id", precision = 10, scale = 0)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
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

    @lombok.Getter
    @lombok.Setter
    @Column(name = "source_sample_item_id")
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String sourceSampleItemId;

    @lombok.Getter
    @lombok.Setter
    @Column(name = "subculture_purpose")
    private String subculturePurpose;

    @lombok.Getter
    @lombok.Setter
    @Column(name = "medium_item_id")
    private Long mediumItemId;

    @lombok.Getter
    @lombok.Setter
    @Column(name = "lot_id")
    private Long lotId;

    @lombok.Getter
    @lombok.Setter
    @Column(name = "not_tracked")
    private boolean notTracked;

    @lombok.Getter
    @lombok.Setter
    @Column(name = "temperature")
    private java.math.BigDecimal temperature;

    @lombok.Getter
    @lombok.Setter
    @Column(name = "duration")
    private java.math.BigDecimal duration;

    @lombok.Getter
    @lombok.Setter
    @Column(name = "duration_unit")
    private String durationUnit;

    @lombok.Getter
    @lombok.Setter
    @Column(name = "check_interval_hours")
    private java.math.BigDecimal checkIntervalHours;

    @lombok.Getter
    @lombok.Setter
    @Column(name = "loop_volume")
    private java.math.BigDecimal loopVolume;

    @lombok.Getter
    @lombok.Setter
    @Column(name = "positive_at")
    private Timestamp positiveAt;

    @lombok.Getter
    @lombok.Setter
    @Column(name = "positive_source")
    private String positiveSource;

    @lombok.Getter
    @lombok.Setter
    @Column(name = "outcome")
    private String outcome;

    @lombok.Getter
    @lombok.Setter
    @Column(name = "outcome_by")
    private String outcomeBy;

    @lombok.Getter
    @lombok.Setter
    @Column(name = "outcome_at")
    private Timestamp outcomeAt;

    @lombok.Getter
    @jakarta.persistence.OneToMany(mappedBy = "inoculation")
    @jakarta.persistence.OrderBy("recordedAt, id")
    private java.util.List<MicroCultureReading> readings = new java.util.ArrayList<>();

    @lombok.Getter
    @jakarta.persistence.OneToMany(mappedBy = "inoculation")
    @jakarta.persistence.OrderBy("recordedAt, id")
    private java.util.List<MicroCultureExtension> extensions = new java.util.ArrayList<>();

    @lombok.Getter
    @jakarta.persistence.OneToMany(mappedBy = "inoculation")
    @jakarta.persistence.OrderBy("recordedAt, id")
    private java.util.List<MicroCultureProposal> proposals = new java.util.ArrayList<>();

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
