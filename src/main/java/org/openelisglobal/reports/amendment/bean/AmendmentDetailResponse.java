package org.openelisglobal.reports.amendment.bean;

import java.util.List;

public class AmendmentDetailResponse {

    private List<AmendmentEvent> items;
    private long totalCount;
    private int page;
    private int pageSize;

    public List<AmendmentEvent> getItems() {
        return items;
    }

    public void setItems(List<AmendmentEvent> items) {
        this.items = items;
    }

    public long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(long totalCount) {
        this.totalCount = totalCount;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }
}
