package org.openelisglobal.common.rest.provider.form;

import java.util.List;
import org.openelisglobal.common.form.IPagingForm;
import org.openelisglobal.common.paging.PagingBean;
import org.openelisglobal.common.rest.provider.bean.ViewItems;

public class GenericProgramDashboardForm implements IPagingForm {

    private PagingBean pagingBean;

    private List<ViewItems> viewItems;

    @Override
    public void setPaging(PagingBean pagingBean) {
        this.pagingBean = pagingBean;

    }

    @Override
    public PagingBean getPaging() {
        return pagingBean;

    }

    public List<ViewItems> getViewItems() {
        return viewItems;
    }

    public void setViewItems(List<ViewItems> viewItems) {
        this.viewItems = viewItems;
    }

}
