package org.openelisglobal.pharmaceutical.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import org.hibernate.annotations.DynamicUpdate;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * ProcessingStep entity - Records sample preparation steps.
 * Tracks weighing, grinding, dissolution, centrifugation, streaking/filtration, etc.
 */
@Entity
@Table(name = "PHARMA_PROCESSING_STEP")
@DynamicUpdate
public class ProcessingStep extends BaseObject<Integer> {

    public enum StepType {
        WEIGHING, GRINDING, DISSOLUTION, CENTRIFUGATION, FILTRATION, STREAKING, DILUTION, EXTRACTION, OTHER
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "pharma_proc_step_seq")
    @SequenceGenerator(name = "pharma_proc_step_seq", sequenceName = "pharma_proc_step_seq", allocationSize = 1)
    @Column(name = "ID")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SAMPLE_ID", nullable = false)
    private PharmaceuticalSample sample;

    @Enumerated(EnumType.STRING)
    @Column(name = "STEP_TYPE", length = 30, nullable = false)
    private StepType stepType;

    @Column(name = "STEP_DESCRIPTION", length = 500)
    private String stepDescription;

    @Column(name = "OPERATOR_ID", length = 36)
    private String operatorId;

    @Column(name = "OPERATOR_NAME", length = 255)
    private String operatorName;

    @Column(name = "STARTED_AT")
    private Timestamp startedAt;

    @Column(name = "COMPLETED_AT")
    private Timestamp completedAt;

    @Column(name = "OUTPUT_REF", length = 100)
    private String outputRef;

    @Column(name = "EQUIPMENT_USED", length = 255)
    private String equipmentUsed;

    @Column(name = "PARAMETERS", length = 2000)
    private String parameters;

    @Column(name = "NOTES", length = 1000)
    private String notes;

    @Column(name = "SYS_USER_ID", nullable = false, length = 36)
    private String sysUserIdValue;

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public PharmaceuticalSample getSample() {
        return sample;
    }

    public void setSample(PharmaceuticalSample sample) {
        this.sample = sample;
    }

    public StepType getStepType() {
        return stepType;
    }

    public void setStepType(StepType stepType) {
        this.stepType = stepType;
    }

    public String getStepDescription() {
        return stepDescription;
    }

    public void setStepDescription(String stepDescription) {
        this.stepDescription = stepDescription;
    }

    public String getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(String operatorId) {
        this.operatorId = operatorId;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public void setOperatorName(String operatorName) {
        this.operatorName = operatorName;
    }

    public Timestamp getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Timestamp startedAt) {
        this.startedAt = startedAt;
    }

    public Timestamp getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Timestamp completedAt) {
        this.completedAt = completedAt;
    }

    public String getOutputRef() {
        return outputRef;
    }

    public void setOutputRef(String outputRef) {
        this.outputRef = outputRef;
    }

    public String getEquipmentUsed() {
        return equipmentUsed;
    }

    public void setEquipmentUsed(String equipmentUsed) {
        this.equipmentUsed = equipmentUsed;
    }

    public String getParameters() {
        return parameters;
    }

    public void setParameters(String parameters) {
        this.parameters = parameters;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    @Override
    public String getSysUserId() {
        return sysUserIdValue;
    }

    @Override
    public void setSysUserId(String sysUserId) {
        this.sysUserIdValue = sysUserId;
    }

    @PrePersist
    protected void onCreate() {
        if (startedAt == null) {
            startedAt = new Timestamp(System.currentTimeMillis());
        }
    }
}
