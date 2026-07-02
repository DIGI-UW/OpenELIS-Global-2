package org.openelisglobal.batchworkplan.valueholder;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.Type;
import org.openelisglobal.common.valueholder.BaseObject;

@Entity
@Access(AccessType.FIELD)
@Table(name = "batch_workplan", schema = "clinlims")
public class BatchWorkplan extends BaseObject<Long> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "batch_workplan_generator")
    @SequenceGenerator(name = "batch_workplan_generator", sequenceName = "batch_workplan_seq", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BatchWorkplanStatus status = BatchWorkplanStatus.DRAFT;

    @Column(name = "test_section_id", precision = 10, scale = 0)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String testSectionId;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "created_at", nullable = false)
    private Timestamp createdAt;

    @Column(name = "activated_at")
    private Timestamp activatedAt;

    @Column(name = "completed_at")
    private Timestamp completedAt;

    @Column(name = "archived_at")
    private Timestamp archivedAt;

    @Column(name = "created_by_user_id")
    private Integer createdByUserId;

    @Column(name = "updated_by_user_id")
    private Integer updatedByUserId;

    @OneToMany(mappedBy = "batchWorkplan", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<BatchWorkplanItem> items = new ArrayList<>();

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BatchWorkplanStatus getStatus() {
        return status;
    }

    public void setStatus(BatchWorkplanStatus status) {
        this.status = status;
    }

    public String getTestSectionId() {
        return testSectionId;
    }

    public void setTestSectionId(String testSectionId) {
        this.testSectionId = testSectionId;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getActivatedAt() {
        return activatedAt;
    }

    public void setActivatedAt(Timestamp activatedAt) {
        this.activatedAt = activatedAt;
    }

    public Timestamp getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Timestamp completedAt) {
        this.completedAt = completedAt;
    }

    public Timestamp getArchivedAt() {
        return archivedAt;
    }

    public void setArchivedAt(Timestamp archivedAt) {
        this.archivedAt = archivedAt;
    }

    public Integer getCreatedByUserId() {
        return createdByUserId;
    }

    public void setCreatedByUserId(Integer createdByUserId) {
        this.createdByUserId = createdByUserId;
    }

    public Integer getUpdatedByUserId() {
        return updatedByUserId;
    }

    public void setUpdatedByUserId(Integer updatedByUserId) {
        this.updatedByUserId = updatedByUserId;
    }

    public List<BatchWorkplanItem> getItems() {
        return items;
    }

    public void setItems(List<BatchWorkplanItem> items) {
        this.items.clear();
        if (items != null) {
            for (BatchWorkplanItem item : items) {
                addItem(item);
            }
        }
    }

    public void addItem(BatchWorkplanItem item) {
        item.setBatchWorkplan(this);
        items.add(item);
    }

    @Override
    public String getSysUserId() {
        return updatedByUserId != null ? updatedByUserId.toString() : super.getSysUserId();
    }

    @Override
    public void setSysUserId(String sysUserId) {
        super.setSysUserId(sysUserId);
        if (sysUserId != null) {
            updatedByUserId = Integer.valueOf(sysUserId);
        }
    }
}
