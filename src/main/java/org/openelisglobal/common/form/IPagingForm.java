package org.openelisglobal.common.form;

import org.openelisglobal.common.paging.PagingBean;

public interface IPagingForm {

    void setPaging(PagingBean pagingBean);

    PagingBean getPaging();
}
