package org.openelisglobal.dataexchange.order.form;

import java.util.List;
import org.openelisglobal.common.form.BaseForm;
import org.openelisglobal.common.util.IdValuePair;

public class ElectronicOrderRejectForm extends BaseForm {

    private static final long serialVersionUID = 1L;

    public enum SearchType {
        IDENTIFIER, DATE_STATUS
    }

    private SearchType searchType;

    private String qaEventId;

    private String externalOrderId;

    private String qaAuthorizer;

    private String qaNote;
    private List<IdValuePair> qaEvents;

    public ElectronicOrderRejectForm() {
        setFormName("ElectronicOrderRejectForm");
    }

    public SearchType getSearchType() {
        return searchType;
    }

    public void setSearchType(SearchType searchType) {
        this.searchType = searchType;
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

    public String getExternalOrderId() {
        return externalOrderId;
    }

    public void setExternalOrderId(String externalOrderId) {
        this.externalOrderId = externalOrderId;
    }
}
