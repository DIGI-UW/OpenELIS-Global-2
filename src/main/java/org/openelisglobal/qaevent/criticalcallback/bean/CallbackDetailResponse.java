package org.openelisglobal.qaevent.criticalcallback.bean;

import java.util.Map;
import org.openelisglobal.reports.qi.PagedResponse;

public class CallbackDetailResponse extends PagedResponse<CallbackEvent> {

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
