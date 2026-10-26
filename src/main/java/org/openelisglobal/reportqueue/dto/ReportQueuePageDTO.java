package org.openelisglobal.reportqueue.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * A page of the queue with its total, the tile counts and the caller's saved
 * preferences.
 */
public class ReportQueuePageDTO {

    private List<ReportQueueRowDTO> rows = new ArrayList<>();
    private long total;
    private int page;
    private int pageSize;
    private long toPrint;
    private long amendedToPrint;
    private String timeWindow;
    private boolean lookup;

    public List<ReportQueueRowDTO> getRows() {
        return rows;
    }

    public void setRows(List<ReportQueueRowDTO> rows) {
        this.rows = rows;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
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

    public long getToPrint() {
        return toPrint;
    }

    public void setToPrint(long toPrint) {
        this.toPrint = toPrint;
    }

    public long getAmendedToPrint() {
        return amendedToPrint;
    }

    public void setAmendedToPrint(long amendedToPrint) {
        this.amendedToPrint = amendedToPrint;
    }

    public String getTimeWindow() {
        return timeWindow;
    }

    public void setTimeWindow(String timeWindow) {
        this.timeWindow = timeWindow;
    }

    public boolean isLookup() {
        return lookup;
    }

    public void setLookup(boolean lookup) {
        this.lookup = lookup;
    }
}
