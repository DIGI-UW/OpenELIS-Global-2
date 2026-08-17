package org.openelisglobal.qaevent.criticalcallback.bean;

import java.util.List;
import java.util.Map;

public class CallbackDetailResponse {

    private List<CallbackEvent> items;
    private int totalCount;
    private int page;
    private int pageSize;
    /**
     * Time-to-acknowledge histogram over the whole window (not just the page):
     * CONFIRMED results bucketed by minutes from release ("0-5", "5-15", "15-30",
     * "30-60", "over60"), everything else under "noAck". Insertion-ordered.
     */
    private Map<String, Long> ackDistribution;
    /**
     * Non-compliant results by reason over the whole window: "overTarget"
     * (CONFIRMED past the SLA), "unableToReach", "noReadback", "noCallback".
     * Insertion-ordered.
     */
    private Map<String, Long> failureCounts;

    public List<CallbackEvent> getItems() {
        return items;
    }

    public void setItems(List<CallbackEvent> items) {
        this.items = items;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
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

    public Map<String, Long> getAckDistribution() {
        return ackDistribution;
    }

    public void setAckDistribution(Map<String, Long> ackDistribution) {
        this.ackDistribution = ackDistribution;
    }

    public Map<String, Long> getFailureCounts() {
        return failureCounts;
    }

    public void setFailureCounts(Map<String, Long> failureCounts) {
        this.failureCounts = failureCounts;
    }
}
