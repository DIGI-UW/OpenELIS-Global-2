package org.openelisglobal.batchworkplan.valueholder;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import org.hibernate.annotations.Type;
import org.openelisglobal.common.valueholder.BaseObject;

@Entity
@Access(AccessType.FIELD)
@Table(name = "batch_workplan_item", schema = "clinlims")
public class BatchWorkplanItem extends BaseObject<Long> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "batch_workplan_item_generator")
    @SequenceGenerator(name = "batch_workplan_item_generator", sequenceName = "batch_workplan_item_seq", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_workplan_id", nullable = false)
    private BatchWorkplan batchWorkplan;

    @Column(name = "analysis_id", nullable = false, precision = 10, scale = 0)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String analysisId;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public BatchWorkplan getBatchWorkplan() {
        return batchWorkplan;
    }

    public void setBatchWorkplan(BatchWorkplan batchWorkplan) {
        this.batchWorkplan = batchWorkplan;
    }

    public String getAnalysisId() {
        return analysisId;
    }

    public void setAnalysisId(String analysisId) {
        this.analysisId = analysisId;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    @Override
    public String getSysUserId() {
        return batchWorkplan != null ? batchWorkplan.getSysUserId() : super.getSysUserId();
    }
}
