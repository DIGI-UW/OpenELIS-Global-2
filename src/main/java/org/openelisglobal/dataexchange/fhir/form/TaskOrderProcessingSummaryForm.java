package org.openelisglobal.dataexchange.fhir.form;

import java.util.ArrayList;
import java.util.List;

public class TaskOrderProcessingSummaryForm {

    private List<OrderStatus> orders = new ArrayList<>();

    public List<OrderStatus> getOrders() {
        return orders;
    }

    public void setOrders(List<OrderStatus> orders) {
        this.orders = orders;
    }

    public static class OrderStatus {

        private String orderNumber;
        private String status;

        public String getOrderNumber() {
            return orderNumber;
        }

        public void setOrderNumber(String orderNumber) {
            this.orderNumber = orderNumber;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }
}
