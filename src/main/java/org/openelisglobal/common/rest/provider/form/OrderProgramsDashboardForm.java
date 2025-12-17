package org.openelisglobal.common.rest.provider.form;

import java.util.List;
import org.openelisglobal.common.form.IPagingForm;
import org.openelisglobal.common.paging.PagingBean;
import org.openelisglobal.common.rest.provider.bean.OrderPrograms;

public class OrderProgramsDashboardForm implements IPagingForm {

    private PagingBean pagingBean;

    private List<OrderPrograms> orderPrograms;

    @Override
    public void setPaging(PagingBean pagingBean) {
        this.pagingBean = pagingBean;

    }

    @Override
    public PagingBean getPaging() {
        return pagingBean;

    }

    public List<OrderPrograms> getOrderPrograms() {
        return orderPrograms;
    }

    public void setOrderPrograms(List<OrderPrograms> orderPrograms) {
        this.orderPrograms = orderPrograms;
    }

}
