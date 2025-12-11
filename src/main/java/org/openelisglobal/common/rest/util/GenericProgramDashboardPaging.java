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
import org.openelisglobal.common.rest.provider.bean.ViewItems;
import org.openelisglobal.common.rest.provider.form.GenericProgramDashboardForm;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.spring.util.SpringContext;

public class GenericProgramDashboardPaging {

    private final PagingUtility<List<ViewItems>> paging = new PagingUtility<>();

    private static final GenericProgramDashboardPageHelper pagingHelper = new GenericProgramDashboardPageHelper(); // Implement
                                                                                                                   // helper
    // class

    public void setDatabaseResults(HttpServletRequest request, GenericProgramDashboardForm form, List<ViewItems> orders)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {

        paging.setDatabaseResults(request.getSession(), orders, pagingHelper);

        List<ViewItems> resultPage = paging.getPage(1, request.getSession());
        if (resultPage != null) {
            form.setViewItems(resultPage);
            form.setPaging(paging.getPagingBeanWithSearchMapping(1, request.getSession()));
        }
    }

    public void page(HttpServletRequest request, GenericProgramDashboardForm form, int newPage)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {

        request.getSession().setAttribute(IActionConstants.SAVE_DISABLED, IActionConstants.FALSE);

        if (newPage < 0) {
            newPage = 0;
        }
        List<ViewItems> resultPage = paging.getPage(newPage, request.getSession());
        if (resultPage != null) {
            form.setViewItems(resultPage);
            form.setPaging(paging.getPagingBeanWithSearchMapping(newPage, request.getSession()));
        }
    }

    public List<ViewItems> getResults(HttpServletRequest request) {
        return paging.getAllResults(request.getSession(), pagingHelper);
    }

    private static class GenericProgramDashboardPageHelper
            implements IPageDivider<List<ViewItems>>, IPageUpdater<List<ViewItems>>, IPageFlattener<List<ViewItems>> {

        @Override
        public void createPages(List<ViewItems> orders, List<List<ViewItems>> pagedResults) {

            int pageSize = SpringContext.getBean(PagingProperties.class).getGenericPageSize();

            List<ViewItems> page = new ArrayList<>();
            int count = 0;

            for (ViewItems item : orders) {

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
        public void updateCache(List<ViewItems> cacheItems, List<ViewItems> clientItems) {
            for (int i = 0; i < clientItems.size(); i++) {
                cacheItems.set(i, clientItems.get(i));
            }
        }

        @Override
        public List<ViewItems> flattenPages(List<List<ViewItems>> pages) {

            List<ViewItems> allResults = new ArrayList<>();
            for (List<ViewItems> page : pages) {
                for (ViewItems item : page) {
                    allResults.add(item);
                }
            }

            return allResults;
        }

        @Override
        public List<IdValuePair> createSearchToPageMapping(List<List<ViewItems>> allPages) {
            List<IdValuePair> mappingList = new ArrayList<>();

            int page = 0;
            for (List<ViewItems> resultList : allPages) {
                page++;
                String pageString = String.valueOf(page);

                String orderID = null;

                for (ViewItems resultItem : resultList) {
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