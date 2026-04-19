package org.openelisglobal.common.rest.provider.bean.homedashboard;

public class OrderDisplayBean {

    private String priority;

    private String orderDate;

    private String patientId;

    private String patientName;

    private String labNumber;

    private String testName;

    private String userFirstName;

    private String userLastName;

    private int countOfOrdersEntered;

    private String id;

    private String testSection;

    private int testCount;

    private int pendingResultCount;

    private int pendingValidationCount;

    private boolean completed;

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

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
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

    public int getTestCount() {
        return testCount;
    }

    public void setTestCount(int testCount) {
        this.testCount = testCount;
    }

    public int getPendingResultCount() {
        return pendingResultCount;
    }

    public void setPendingResultCount(int pendingResultCount) {
        this.pendingResultCount = pendingResultCount;
    }

    public int getPendingValidationCount() {
        return pendingValidationCount;
    }

    public void setPendingValidationCount(int pendingValidationCount) {
        this.pendingValidationCount = pendingValidationCount;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
}
