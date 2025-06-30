package org.openelisglobal.common.rest.provider.form;

import java.util.List;
import org.openelisglobal.common.form.IPagingForm;
import org.openelisglobal.common.paging.PagingBean;
import org.openelisglobal.common.util.IdValuePair;

public class DisplayListPagingForm implements IPagingForm {

    private PagingBean paging;

    private List<IdValuePair> displayItems;

    public void setDisplayListItems(List<IdValuePair> displayItems) {
        this.displayItems = displayItems;
    }

    public List<IdValuePair> getDisplayItems() {
        return displayItems;
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
