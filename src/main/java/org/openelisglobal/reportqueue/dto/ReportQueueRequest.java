package org.openelisglobal.reportqueue.dto;

import java.util.ArrayList;
import java.util.List;

/** The filters, sort and page the queue page asks for. */
public class ReportQueueRequest {

    private String labNo;
    private String labNoTo;
    private String patientId;
    private List<String> states = new ArrayList<>();
    private String printStatus;
    /** 24h, 7d, 30d, all or custom; null keeps the saved preference. */
    private String window;
    private String from;
    private String to;
    private List<Long> facilityIds = new ArrayList<>();
    private List<Long> wardIds = new ArrayList<>();
    private List<Long> requesterIds = new ArrayList<>();
    private String sort;
    private String sortDir;
    private int page = 1;
    /** Null keeps the saved preference. */
    private Integer pageSize;

    public String getLabNo() {
        return labNo;
    }

    public void setLabNo(String labNo) {
        this.labNo = labNo;
    }

    public String getLabNoTo() {
        return labNoTo;
    }

    public void setLabNoTo(String labNoTo) {
        this.labNoTo = labNoTo;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public List<String> getStates() {
        return states;
    }

    public void setStates(List<String> states) {
        this.states = states == null ? new ArrayList<>() : states;
    }

    public String getPrintStatus() {
        return printStatus;
    }

    public void setPrintStatus(String printStatus) {
        this.printStatus = printStatus;
    }

    public String getWindow() {
        return window;
    }

    public void setWindow(String window) {
        this.window = window;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public List<Long> getFacilityIds() {
        return facilityIds;
    }

    public void setFacilityIds(List<Long> facilityIds) {
        this.facilityIds = facilityIds == null ? new ArrayList<>() : facilityIds;
    }

    public List<Long> getWardIds() {
        return wardIds;
    }

    public void setWardIds(List<Long> wardIds) {
        this.wardIds = wardIds == null ? new ArrayList<>() : wardIds;
    }

    public List<Long> getRequesterIds() {
        return requesterIds;
    }

    public void setRequesterIds(List<Long> requesterIds) {
        this.requesterIds = requesterIds == null ? new ArrayList<>() : requesterIds;
    }

    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }

    public String getSortDir() {
        return sortDir;
    }

    public void setSortDir(String sortDir) {
        this.sortDir = sortDir;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }
}
