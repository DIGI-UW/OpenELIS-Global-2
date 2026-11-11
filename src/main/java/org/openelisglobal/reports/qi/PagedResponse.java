package org.openelisglobal.reports.qi;

import java.util.List;

/**
 * One page of a quality-indicator detail list. The field names are the wire
 * contract the QI screens read ({@code items}, {@code totalCount},
 * {@code page}, {@code pageSize}); subclasses add report-specific fields
 * alongside them.
 */
public class PagedResponse<T> {

    private List<T> items;
    private long totalCount;
    private int page;
    private int pageSize;

    public List<T> getItems() {
        return items;
    }

    public void setItems(List<T> items) {
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
