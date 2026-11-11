package org.openelisglobal.referral.dto;

public class ReferenceLabMetricsDTO {

    private long outstanding;
    private long returned;
    private long reconciledToday;
    private long rejectedThisWeek;
    private int referralStuckThresholdDays;

    public ReferenceLabMetricsDTO() {
    }

    public ReferenceLabMetricsDTO(long outstanding, long returned, long reconciledToday, long rejectedThisWeek,
            int referralStuckThresholdDays) {
        this.outstanding = outstanding;
        this.returned = returned;
        this.reconciledToday = reconciledToday;
        this.rejectedThisWeek = rejectedThisWeek;
        this.referralStuckThresholdDays = referralStuckThresholdDays;
    }

    public long getOutstanding() {
        return outstanding;
    }

    public void setOutstanding(long outstanding) {
        this.outstanding = outstanding;
    }

    public long getReturned() {
        return returned;
    }

    public void setReturned(long returned) {
        this.returned = returned;
    }

    public long getReconciledToday() {
        return reconciledToday;
    }

    public void setReconciledToday(long reconciledToday) {
        this.reconciledToday = reconciledToday;
    }

    public long getRejectedThisWeek() {
        return rejectedThisWeek;
    }

    public void setRejectedThisWeek(long rejectedThisWeek) {
        this.rejectedThisWeek = rejectedThisWeek;
    }

    public int getReferralStuckThresholdDays() {
        return referralStuckThresholdDays;
    }

    public void setReferralStuckThresholdDays(int referralStuckThresholdDays) {
        this.referralStuckThresholdDays = referralStuckThresholdDays;
    }
}
