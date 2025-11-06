package org.openelisglobal.result.form;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.openelisglobal.analyzer.service.BidirectionalAnalyzer.LISAction;
import org.openelisglobal.analyzerresults.action.beanitems.AnalyzerResultItem;
import org.openelisglobal.common.form.BaseForm;
import org.openelisglobal.common.form.IPagingForm;
import org.openelisglobal.common.paging.PagingBean;
import org.openelisglobal.validation.annotations.SafeHtml;

public class AnalyzerResultsForm extends BaseForm implements IPagingForm {

    public interface AnalyzerResuts {
    }

    @Valid
    private PagingBean paging;

    @Valid
    private List<AnalyzerResultItem> resultList;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String type;

    @NotNull
    private Boolean displayNotFoundMsg = false;

    @NotNull
    private Boolean displayMissingTestMsg = false;

    // display only
    private List<LISAction> supportedLISActions;

    public AnalyzerResultsForm() {
        setFormName("AnalyzerResultsForm");
    }

    @Override
    public PagingBean getPaging() {
        return paging;
    }

    @Override
    public void setPaging(PagingBean paging) {
        this.paging = paging;
    }

    public List<AnalyzerResultItem> getResultList() {
        return resultList;
    }

    public void setResultList(List<AnalyzerResultItem> resultList) {
        this.resultList = resultList;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Boolean getDisplayNotFoundMsg() {
        return displayNotFoundMsg;
    }

    public void setDisplayNotFoundMsg(Boolean displayNotFoundMsg) {
        this.displayNotFoundMsg = displayNotFoundMsg;
    }

    public Boolean getDisplayMissingTestMsg() {
        return displayMissingTestMsg;
    }

    public void setDisplayMissingTestMsg(Boolean displayMissingTestMsg) {
        this.displayMissingTestMsg = displayMissingTestMsg;
    }

    public List<LISAction> getSupportedLISActions() {
        return supportedLISActions;
    }

    public void setSupportedLISActions(List<LISAction> supportedLISActions) {
        this.supportedLISActions = supportedLISActions;
    }
}
