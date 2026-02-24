package org.openelisglobal.qaevent.valueholder;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import java.util.Objects;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * Entity class for linking lab results to Non-Conformity Events (NCEs). This
 * association tracks the relationship between specific lab results and quality
 * issues reported through the NCE system.
 */
@Entity
@Table(name = "nce_result_association")
public class NceResultAssociation extends BaseObject<Integer> {

    /**
     * Types of associations between results and NCEs
     */
    public enum AssociationType {
        RESULT_TRIGGERED_NCE("Result triggered NCE creation"),
        RESULT_AFFECTED_BY_NCE("Result affected by existing NCE"),
        RESULT_PART_OF_NCE("Result is part of NCE investigation"),
        DELTA_CHECK_ESCALATION("Delta check alert escalated to NCE");

        private final String description;

        AssociationType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Basic
    @Column(name = "result_id")
    private String resultId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nce_id", referencedColumnName = "id")
    private NcEvent ncEvent;

    @Basic
    @Column(name = "association_type", length = 50)
    private String associationType;

    @Basic
    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Basic
    @Column(name = "created_date")
    private Timestamp createdDate;

    @Basic
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    public NceResultAssociation() {
        super();
    }

    public NceResultAssociation(String resultId, NcEvent ncEvent, AssociationType type, String createdBy) {
        this();
        this.resultId = resultId;
        this.ncEvent = ncEvent;
        this.associationType = type.name();
        this.createdBy = createdBy;
        this.createdDate = new Timestamp(System.currentTimeMillis());
    }

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public String getResultId() {
        return resultId;
    }

    public void setResultId(String resultId) {
        this.resultId = resultId;
    }

    public NcEvent getNcEvent() {
        return ncEvent;
    }

    public void setNcEvent(NcEvent ncEvent) {
        this.ncEvent = ncEvent;
    }

    public String getAssociationType() {
        return associationType;
    }

    public void setAssociationType(String associationType) {
        this.associationType = associationType;
    }

    public AssociationType getAssociationTypeEnum() {
        try {
            return AssociationType.valueOf(associationType);
        } catch (IllegalArgumentException e) {
            return AssociationType.RESULT_TRIGGERED_NCE; // Default fallback
        }
    }

    public void setAssociationTypeEnum(AssociationType associationType) {
        this.associationType = associationType.name();
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Timestamp getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Timestamp createdDate) {
        this.createdDate = createdDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        NceResultAssociation that = (NceResultAssociation) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "NceResultAssociation{" + "id=" + id + ", resultId=" + resultId + ", nceId="
                + (ncEvent != null ? ncEvent.getId() : null) + ", associationType='" + associationType + '\''
                + ", createdBy='" + createdBy + '\'' + ", createdDate=" + createdDate + '}';
    }
}