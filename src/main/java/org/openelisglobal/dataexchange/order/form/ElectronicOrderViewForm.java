package org.openelisglobal.dataexchange.order.form;

import java.util.List;
import org.openelisglobal.common.form.BaseForm;
import org.openelisglobal.common.form.IPagingForm;
import org.openelisglobal.common.paging.PagingBean;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrderDisplayItem;
import org.openelisglobal.organization.valueholder.Organization;

public class ElectronicOrderViewForm extends BaseForm implements IPagingForm {

    public enum SearchType {
        IDENTIFIER, DATE_STATUS
    }

    private SearchType searchType;

    private boolean searchFinished;

    private boolean useAllInfo;

    private String searchValue;

    private String startDate;

    private String endDate;

    private String organizationId;

    private List<String> facilityIds;

    private List<Organization> organizationList;

    private List<String> testIds;

    private String statusId;

    private List<ElectronicOrderDisplayItem> eOrders;

    private List<IdValuePair> referralFacilitySelectionList;

    private List<IdValuePair> testSelectionList;

    private List<IdValuePair> statusSelectionList;

    private String qaEventId;

    private List<IdValuePair> qaEvents;

    private String qaAuthorizer;

    private String qaNote;

    private PagingBean paging;

    public ElectronicOrderViewForm() {
        setFormName("ElectronicOrderViewForm");
    }

    public String getSearchValue() {
        return searchValue;
    }

    public void setSearchValue(String searchValue) {
        this.searchValue = searchValue;
    }

    public List<IdValuePair> getReferralFacilitySelectionList() {
        return referralFacilitySelectionList;
    }

    public void setReferralFacilitySelectionList(List<IdValuePair> referralFacilitySelectionList) {
        this.referralFacilitySelectionList = referralFacilitySelectionList;
    }

    public List<IdValuePair> getTestSelectionList() {
        return testSelectionList;
    }

    public void setTestSelectionList(List<IdValuePair> testSelectionList) {
        this.testSelectionList = testSelectionList;
    }

    public List<IdValuePair> getStatusSelectionList() {
        return statusSelectionList;
    }

    public void setStatusSelectionList(List<IdValuePair> statusSelectionList) {
        this.statusSelectionList = statusSelectionList;
    }

    public List<String> getTestIds() {
        return testIds;
    }

    public void setTestIds(List<String> testIds) {
        this.testIds = testIds;
    }

    public String getStatusId() {
        return statusId;
    }

    public void setStatus(String status) {
        this.statusId = status;
    }

    public List<String> getFacilityIds() {
        return facilityIds;
    }

    public void setFacilityIds(List<String> facilityIds) {
        this.facilityIds = facilityIds;
    }

    public boolean isSearchFinished() {
        return searchFinished;
    }

    public void setSearchFinished(boolean searchFinished) {
        this.searchFinished = searchFinished;
    }

    public SearchType getSearchType() {
        return searchType;
    }

    public void setSearchType(SearchType searchType) {
        this.searchType = searchType;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public List<ElectronicOrderDisplayItem> geteOrders() {
        return eOrders;
    }

    public void seteOrders(List<ElectronicOrderDisplayItem> eOrders) {
        this.eOrders = eOrders;
    }

    public void setStatusId(String statusId) {
        this.statusId = statusId;
    }

    public boolean getUseAllInfo() {
        return useAllInfo;
    }

    public void setUseAllInfo(boolean useAllInfo) {
        this.useAllInfo = useAllInfo;
    }

    public List<Organization> getOrganizationList() {
        return organizationList;
    }

    public void setOrganizationList(List<Organization> organizationList) {
        this.organizationList = organizationList;
    }

    public String getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }

    public List<IdValuePair> getQaEvents() {
        return qaEvents;
    }

    public void setQaEvents(List<IdValuePair> qaEvents) {
        this.qaEvents = qaEvents;
    }

    public String getQaAuthorizer() {
        return qaAuthorizer;
    }

    public void setQaAuthorizer(String qaAuthorizer) {
        this.qaAuthorizer = qaAuthorizer;
    }

    public String getQaNote() {
        return qaNote;
    }

    public void setQaNote(String qaNote) {
        this.qaNote = qaNote;
    }

    public String getQaEventId() {
        return qaEventId;
    }

    public void setQaEventId(String qaEventId) {
        this.qaEventId = qaEventId;
    }

    @Override
    public void setPaging(PagingBean paging) {
        this.paging = paging;
    }

    @Override
    public PagingBean getPaging() {
        return paging;
    }
}
