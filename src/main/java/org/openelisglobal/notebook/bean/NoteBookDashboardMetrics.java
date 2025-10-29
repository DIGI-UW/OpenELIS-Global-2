package org.openelisglobal.notebook.bean;

public class NoteBookDashboardMetrics {
    private Long total;
    private Long drafts;
    private Long pending;
    private Long finalized;

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }

    public Long getDrafts() {
        return drafts;
    }

    public void setDrafts(Long drafts) {
        this.drafts = drafts;
    }

    public Long getPending() {
        return pending;
    }

    public void setPending(Long pending) {
        this.pending = pending;
    }

    public Long getFinalized() {
        return finalized;
    }

    public void setFinalized(Long finalized) {
        this.finalized = finalized;
    }
}
