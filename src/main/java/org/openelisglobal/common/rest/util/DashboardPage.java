package org.openelisglobal.common.rest.util;

import java.util.List;
import org.openelisglobal.common.paging.PagingBean;

/**
 * One server page of a dashboard list together with the page announcement the
 * client pages by.
 */
public class DashboardPage<T> {

    private List<T> items;
    private PagingBean paging;
    private int totalItems;

    public DashboardPage() {
    }

    public DashboardPage(List<T> items, PagingBean paging, int totalItems) {
        this.items = items;
        this.paging = paging;
        this.totalItems = totalItems;
    }

    public List<T> getItems() {
        return items;
    }

    public void setItems(List<T> items) {
        this.items = items;
    }

    public PagingBean getPaging() {
        return paging;
    }

    public void setPaging(PagingBean paging) {
        this.paging = paging;
    }

    public int getTotalItems() {
        return totalItems;
    }

    public void setTotalItems(int totalItems) {
        this.totalItems = totalItems;
    }
}
