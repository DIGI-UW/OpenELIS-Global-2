package org.openelisglobal.batchworkplan.form;

public class BatchWorkplanItemResponse extends PendingBatchTestResponse {

    private Long id;
    private Integer sortOrder;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
