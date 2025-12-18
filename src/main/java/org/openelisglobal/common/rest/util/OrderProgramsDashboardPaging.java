package org.openelisglobal.common.rest.util;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.paging.IPageDivider;
import org.openelisglobal.common.paging.IPageFlattener;
import org.openelisglobal.common.paging.IPageUpdater;
import org.openelisglobal.common.paging.PagingProperties;
import org.openelisglobal.common.paging.PagingUtility;
import org.openelisglobal.common.rest.provider.bean.OrderPrograms;
import org.openelisglobal.common.rest.provider.form.OrderProgramsDashboardForm;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.spring.util.SpringContext;

public class OrderProgramsDashboardPaging {

    private final PagingUtility<List<OrderPrograms>> paging = new PagingUtility<>();

    private static final OrderProgramsDashboardPageHelper pagingHelper = new OrderProgramsDashboardPageHelper();

    public void setDatabaseOrderPrograms(HttpServletRequest request, OrderProgramsDashboardForm form,
            List<OrderPrograms> orders)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {

        paging.setDatabaseResults(request.getSession(), orders, pagingHelper);

        List<OrderPrograms> orderProgramsPage = paging.getPage(1, request.getSession());
        if (orderProgramsPage != null) {
            form.setOrderPrograms(orderProgramsPage);
            form.setPaging(paging.getPagingBeanWithSearchMapping(1, request.getSession()));
        }
    }

    public void page(HttpServletRequest request, OrderProgramsDashboardForm form, int newPage)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {

        request.getSession().setAttribute(IActionConstants.SAVE_DISABLED, IActionConstants.FALSE);

        if (newPage < 0) {
            newPage = 0;
        }
        List<OrderPrograms> orderProgramsPage = paging.getPage(newPage, request.getSession());
        if (orderProgramsPage != null) {
            form.setOrderPrograms(orderProgramsPage);
            form.setPaging(paging.getPagingBeanWithSearchMapping(newPage, request.getSession()));
        }
    }

    public List<OrderPrograms> getOrderPrograms(HttpServletRequest request) {
        return paging.getAllResults(request.getSession(), pagingHelper);
    }

    private static class OrderProgramsDashboardPageHelper implements IPageDivider<List<OrderPrograms>>,
            IPageUpdater<List<OrderPrograms>>, IPageFlattener<List<OrderPrograms>> {

        @Override
        public void createPages(List<OrderPrograms> orders, List<List<OrderPrograms>> pagedResults) {

            int pageSize = SpringContext.getBean(PagingProperties.class).getOrderProgramsPageSize();

            List<OrderPrograms> page = new ArrayList<>();
            int count = 0;

            for (OrderPrograms item : orders) {

                if (count == pageSize) {
                    pagedResults.add(page);
                    page = new ArrayList<>();
                    count = 0;
                }

                page.add(item);
                count++;
            }

            if (!page.isEmpty()) {
                pagedResults.add(page);
            }
        }

        @Override
        public void updateCache(List<OrderPrograms> cacheItems, List<OrderPrograms> clientItems) {
            for (int i = 0; i < clientItems.size(); i++) {
                cacheItems.set(i, clientItems.get(i));
            }
        }

        @Override
        public List<OrderPrograms> flattenPages(List<List<OrderPrograms>> pages) {

            List<OrderPrograms> allResults = new ArrayList<>();
            for (List<OrderPrograms> page : pages) {
                for (OrderPrograms item : page) {
                    allResults.add(item);
                }
            }

            return allResults;
        }

        @Override
        public List<IdValuePair> createSearchToPageMapping(List<List<OrderPrograms>> allPages) {
            List<IdValuePair> mappingList = new ArrayList<>();

            int page = 0;
            for (List<OrderPrograms> resultList : allPages) {
                page++;
                String pageString = String.valueOf(page);

                String orderID = null;

                for (OrderPrograms resultItem : resultList) {
                    if (!resultItem.getProgramSampleId().equals(orderID)) {
                        orderID = resultItem.getProgramSampleId();
                        mappingList.add(new IdValuePair(orderID, pageString));
                    }
                }
            }

            return mappingList;
        }
    }
}