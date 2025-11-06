package org.openelisglobal.common.rest.provider.bean.homedashboard;

public class OrderDisplayBean {

    private String priority;

    private String orderDate;

    private String patientId;

    private String labNumber;

    private String testName;

    private String userFirstName;

    private String userLastName;

    private int countOfOrdersEntered;

    private String id;

    private String testSection;

    public String getUserFirstName() {
        return userFirstName;
    }

    public void setUserFirstName(String userFirstName) {
        this.userFirstName = userFirstName;
    }

    public String getUserLastName() {
        return userLastName;
    }

    public void setUserLastName(String userLastName) {
        this.userLastName = userLastName;
    }

    public int getCountOfOrdersEntered() {
        return countOfOrdersEntered;
    }

    public void setCountOfOrdersEntered(int countOfOrdersEntered) {
        this.countOfOrdersEntered = countOfOrdersEntered;
    }

    public String getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(String orderDate) {
        this.orderDate = orderDate;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getLabNumber() {
        return labNumber;
    }

    public void setLabNumber(String labNumber) {
        this.labNumber = labNumber;
    }

    public String getTestName() {
        return testName;
    }

    public void setTestName(String testName) {
        this.testName = testName;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTestSection() {
        return testSection;
    }

    public void setTestSection(String testSection) {
        this.testSection = testSection;
    }
}
