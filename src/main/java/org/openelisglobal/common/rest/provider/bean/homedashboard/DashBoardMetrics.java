package org.openelisglobal.common.rest.provider.bean.homedashboard;

public class DashBoardMetrics {

    Integer ordersInProgress = 0;

    Integer ordersReadyForValidation = 0;

    Integer ordersCompletedToday = 0;

    Integer patiallyCompletedToday = 0;

    Integer orderEnterdByUserToday = 0;

    Integer ordersRejectedToday = 0;

    Integer unPritendResults = 0;

    Integer incomigOrders = 0;

    Double averageTurnAroudTime = 0.0;

    Integer delayedTurnAround = 0;

    public Integer getOrdersInProgress() {
        return ordersInProgress;
    }

    public void setOrdersInProgress(Integer ordersInProgress) {
        this.ordersInProgress = ordersInProgress;
    }

    public Integer getOrdersReadyForValidation() {
        return ordersReadyForValidation;
    }

    public void setOrdersReadyForValidation(Integer ordersReadyForValidation) {
        this.ordersReadyForValidation = ordersReadyForValidation;
    }

    public Integer getOrdersCompletedToday() {
        return ordersCompletedToday;
    }

    public void setOrdersCompletedToday(Integer ordersCompletedToday) {
        this.ordersCompletedToday = ordersCompletedToday;
    }

    public Integer getPatiallyCompletedToday() {
        return patiallyCompletedToday;
    }

    public void setPatiallyCompletedToday(Integer patiallyCompletedToday) {
        this.patiallyCompletedToday = patiallyCompletedToday;
    }

    public Integer getOrderEnterdByUserToday() {
        return orderEnterdByUserToday;
    }

    public void setOrderEnterdByUserToday(Integer orderEnterdByUserToday) {
        this.orderEnterdByUserToday = orderEnterdByUserToday;
    }

    public Integer getOrdersRejectedToday() {
        return ordersRejectedToday;
    }

    public void setOrdersRejectedToday(Integer ordersRejectedToday) {
        this.ordersRejectedToday = ordersRejectedToday;
    }

    public Integer getUnPritendResults() {
        return unPritendResults;
    }

    public void setUnPritendResults(Integer unPritendResults) {
        this.unPritendResults = unPritendResults;
    }

    public Integer getIncomigOrders() {
        return incomigOrders;
    }

    public void setIncomigOrders(Integer incomigOrders) {
        this.incomigOrders = incomigOrders;
    }

    public Double getAverageTurnAroudTime() {
        return averageTurnAroudTime;
    }

    public void setAverageTurnAroudTime(Double averageTurnAroudTime) {
        this.averageTurnAroudTime = averageTurnAroudTime;
    }

    public Integer getDelayedTurnAround() {
        return delayedTurnAround;
    }

    public void setDelayedTurnAround(Integer delayedTurnAround) {
        this.delayedTurnAround = delayedTurnAround;
    }
}
