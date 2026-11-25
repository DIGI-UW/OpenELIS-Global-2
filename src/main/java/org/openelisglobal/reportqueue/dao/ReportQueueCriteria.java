package org.openelisglobal.reportqueue.dao;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Everything the queue query needs to list patient reports: the status ids that
 * decide released and countable analyses, the caller's lab-unit scope, the
 * filters, the sort and the page.
 */
public class ReportQueueCriteria {

    public enum SortKey {
        RELEASED, LAB_NO, SUBJECT, STATE, PRINT_STATUS
    }

    private String finalizedStatusId;
    private List<String> excludedStatusIds = new ArrayList<>();
    /**
     * Null when the whole lab is in scope; otherwise the test section ids the
     * caller holds.
     */
    private List<String> sectionIds;
    private Long organizationRequesterTypeId;
    private Long providerRequesterTypeId;

    private String labNo;
    private String labNoTo;
    private String patientId;
    private List<String> states = new ArrayList<>();
    /** "UNPRINTED", "PRINTED" or null for both. */
    private String printStatus;
    private OffsetDateTime windowFrom;
    private OffsetDateTime windowTo;
    private List<Long> facilityIds = new ArrayList<>();
    private List<Long> wardIds = new ArrayList<>();
    private List<Long> requesterIds = new ArrayList<>();
    private OffsetDateTime installDate;

    private SortKey sortKey = SortKey.RELEASED;
    private boolean sortDescending = true;
    private int page = 1;
    private int pageSize = 20;

    public boolean isLookup() {
        return labNo != null && !labNo.isBlank() || patientId != null && !patientId.isBlank();
    }

    public String getFinalizedStatusId() {
        return finalizedStatusId;
    }

    public void setFinalizedStatusId(String finalizedStatusId) {
        this.finalizedStatusId = finalizedStatusId;
    }

    public List<String> getExcludedStatusIds() {
        return excludedStatusIds;
    }

    public void setExcludedStatusIds(List<String> excludedStatusIds) {
        this.excludedStatusIds = excludedStatusIds;
    }

    public List<String> getSectionIds() {
        return sectionIds;
    }

    public void setSectionIds(List<String> sectionIds) {
        this.sectionIds = sectionIds;
    }

    public Long getOrganizationRequesterTypeId() {
        return organizationRequesterTypeId;
    }

    public void setOrganizationRequesterTypeId(Long organizationRequesterTypeId) {
        this.organizationRequesterTypeId = organizationRequesterTypeId;
    }

    public Long getProviderRequesterTypeId() {
        return providerRequesterTypeId;
    }

    public void setProviderRequesterTypeId(Long providerRequesterTypeId) {
        this.providerRequesterTypeId = providerRequesterTypeId;
    }

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
        this.states = states;
    }

    public String getPrintStatus() {
        return printStatus;
    }

    public void setPrintStatus(String printStatus) {
        this.printStatus = printStatus;
    }

    public OffsetDateTime getWindowFrom() {
        return windowFrom;
    }

    public void setWindowFrom(OffsetDateTime windowFrom) {
        this.windowFrom = windowFrom;
    }

    public OffsetDateTime getWindowTo() {
        return windowTo;
    }

    public void setWindowTo(OffsetDateTime windowTo) {
        this.windowTo = windowTo;
    }

    public List<Long> getFacilityIds() {
        return facilityIds;
    }

    public void setFacilityIds(List<Long> facilityIds) {
        this.facilityIds = facilityIds;
    }

    public List<Long> getWardIds() {
        return wardIds;
    }

    public void setWardIds(List<Long> wardIds) {
        this.wardIds = wardIds;
    }

    public List<Long> getRequesterIds() {
        return requesterIds;
    }

    public void setRequesterIds(List<Long> requesterIds) {
        this.requesterIds = requesterIds;
    }

    public OffsetDateTime getInstallDate() {
        return installDate;
    }

    public void setInstallDate(OffsetDateTime installDate) {
        this.installDate = installDate;
    }

    public SortKey getSortKey() {
        return sortKey;
    }

    public void setSortKey(SortKey sortKey) {
        this.sortKey = sortKey;
    }

    public boolean isSortDescending() {
        return sortDescending;
    }

    public void setSortDescending(boolean sortDescending) {
        this.sortDescending = sortDescending;
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
}
