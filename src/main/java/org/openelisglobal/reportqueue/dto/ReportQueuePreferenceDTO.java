package org.openelisglobal.reportqueue.dto;

/** The queue settings kept per user: the time window and the rows per page. */
public class ReportQueuePreferenceDTO {

    private String timeWindow;
    private Integer pageSize;

    public ReportQueuePreferenceDTO() {
    }

    public ReportQueuePreferenceDTO(String timeWindow, Integer pageSize) {
        this.timeWindow = timeWindow;
        this.pageSize = pageSize;
    }

    public String getTimeWindow() {
        return timeWindow;
    }

    public void setTimeWindow(String timeWindow) {
        this.timeWindow = timeWindow;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }
}
