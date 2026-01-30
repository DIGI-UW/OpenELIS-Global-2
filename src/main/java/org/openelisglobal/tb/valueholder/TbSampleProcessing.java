package org.openelisglobal.tb.valueholder;

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
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.openelisglobal.tb.valueholder.TbEnums.DecontaminationMethod;
import org.openelisglobal.tb.valueholder.TbEnums.ProcessingStatus;

/**
 * Sample processing/decontamination tracking for TB laboratory workflow.
 * Records decontamination method and status before inoculation.
 */
@Entity
@Table(name = "tb_sample_processing")
public class TbSampleProcessing extends BaseObject<Integer> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tb_sample_processing_generator")
    @SequenceGenerator(name = "tb_sample_processing_generator", sequenceName = "tb_sample_processing_seq", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sample_item_id", nullable = false, unique = true)
    private SampleItem sampleItem;

    @Column(name = "processing_date", nullable = false)
    private Timestamp processingDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "decontamination_method", nullable = false, length = 20)
    private DecontaminationMethod decontaminationMethod;

    @Column(name = "method_notes", columnDefinition = "TEXT")
    private String methodNotes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_by")
    private SystemUser processedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false, length = 30)
    private ProcessingStatus processingStatus = ProcessingStatus.PENDING;

    // Getters and setters
    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public SampleItem getSampleItem() {
        return sampleItem;
    }

    public void setSampleItem(SampleItem sampleItem) {
        this.sampleItem = sampleItem;
    }

    public String getSampleItemId() {
        return sampleItem != null ? sampleItem.getId() : null;
    }

    public Timestamp getProcessingDate() {
        return processingDate;
    }

    public void setProcessingDate(Timestamp processingDate) {
        this.processingDate = processingDate;
    }

    public DecontaminationMethod getDecontaminationMethod() {
        return decontaminationMethod;
    }

    public void setDecontaminationMethod(DecontaminationMethod decontaminationMethod) {
        this.decontaminationMethod = decontaminationMethod;
    }

    public String getMethodNotes() {
        return methodNotes;
    }

    public void setMethodNotes(String methodNotes) {
        this.methodNotes = methodNotes;
    }

    public SystemUser getProcessedBy() {
        return processedBy;
    }

    public void setProcessedBy(SystemUser processedBy) {
        this.processedBy = processedBy;
    }

    public ProcessingStatus getProcessingStatus() {
        return processingStatus;
    }

    public void setProcessingStatus(ProcessingStatus processingStatus) {
        this.processingStatus = processingStatus != null ? processingStatus : ProcessingStatus.PENDING;
    }

    /**
     * Returns true if sample is ready for inoculation.
     */
    public boolean isReadyForInoculation() {
        return processingStatus == ProcessingStatus.READY_FOR_INOCULATION;
    }

    /**
     * Returns true if sample has been processed (decontaminated).
     */
    public boolean isProcessed() {
        return processingStatus == ProcessingStatus.PROCESSED
                || processingStatus == ProcessingStatus.READY_FOR_INOCULATION;
    }

    /**
     * Marks the sample as processed and ready for inoculation.
     */
    public void markReadyForInoculation() {
        this.processingStatus = ProcessingStatus.READY_FOR_INOCULATION;
    }
}
