package org.openelisglobal.common.rest.util;

/**
 * The contents of this file are subject to the Mozilla Public License Version 1.1 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.mozilla.org/MPL/
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF
 * ANY KIND, either express or implied. See the License for the specific language governing rights
 * and limitations under the License.
 *
 * <p>The Original Code is OpenELIS code.
 *
 * <p>Copyright (C) CIRG, University of Washington, Seattle WA. All Rights Reserved.
 */
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
import org.openelisglobal.common.rest.provider.bean.homedashboard.OrderDisplayBean;
import org.openelisglobal.common.rest.provider.form.PatientDashBoardForm;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.spring.util.SpringContext;

/**
 * Responsible for building a page by accepting all display and paging
 * information. Based on the page size and the page number, it will return the
 * appropriate page.
 */
public class PatientDashBoardPaging {

    private final PagingUtility<List<OrderDisplayBean>> paging = new PagingUtility<>(); // Adjust type based on your
                                                                                        // data
    private static final PatientDashboardPageHelper pagingHelper = new PatientDashboardPageHelper(); // Implement helper
                                                                                                     // class

    public void setDatabaseResults(HttpServletRequest request, PatientDashBoardForm form, List<OrderDisplayBean> orders)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {

        paging.setDatabaseResults(request.getSession(), orders, pagingHelper);

        List<OrderDisplayBean> resultPage = paging.getPage(1, request.getSession());
        if (resultPage != null) {
            form.setOrderDisplayBeans(resultPage);
            form.setPaging(paging.getPagingBeanWithSearchMapping(1, request.getSession()));
        }
    }

    public void page(HttpServletRequest request, PatientDashBoardForm form, int newPage)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {

        request.getSession().setAttribute(IActionConstants.SAVE_DISABLED, IActionConstants.FALSE);

        if (newPage < 0) {
            newPage = 0;
        }
        List<OrderDisplayBean> resultPage = paging.getPage(newPage, request.getSession());
        if (resultPage != null) {
            form.setOrderDisplayBeans(resultPage);
            // form.setTestSectionId("0");
            form.setPaging(paging.getPagingBeanWithSearchMapping(newPage, request.getSession()));
        }
    }

    public List<OrderDisplayBean> getResults(HttpServletRequest request) {
        return paging.getAllResults(request.getSession(), pagingHelper);
    }

    private static class PatientDashboardPageHelper implements IPageDivider<List<OrderDisplayBean>>,
            IPageUpdater<List<OrderDisplayBean>>, IPageFlattener<List<OrderDisplayBean>> {

        @Override
        public void createPages(List<OrderDisplayBean> orders, List<List<OrderDisplayBean>> pagedResults) {
            List<OrderDisplayBean> page = new ArrayList<>();

            Boolean createNewPage = false;
            int resultCount = 0;

            for (OrderDisplayBean item : orders) {
                if (createNewPage) {
                    resultCount = 0;
                    createNewPage = false;
                    pagedResults.add(page);
                    page = new ArrayList<>();
                }
                if (resultCount >= SpringContext.getBean(PagingProperties.class).getResultsPageSize()) {
                    createNewPage = true;
                }

                page.add(item);
                resultCount++;
            }

            if (!page.isEmpty() || pagedResults.isEmpty()) {
                pagedResults.add(page);
            }
        }

        @Override
        public void updateCache(List<OrderDisplayBean> cacheItems, List<OrderDisplayBean> clientItems) {
            for (int i = 0; i < clientItems.size(); i++) {
                cacheItems.set(i, clientItems.get(i));
            }
        }

        @Override
        public List<OrderDisplayBean> flattenPages(List<List<OrderDisplayBean>> pages) {

            List<OrderDisplayBean> allResults = new ArrayList<>();

            for (List<OrderDisplayBean> page : pages) {
                for (OrderDisplayBean item : page) {
                    allResults.add(item);
                }
            }

            return allResults;
        }

        @Override
        public List<IdValuePair> createSearchToPageMapping(List<List<OrderDisplayBean>> allPages) {
            List<IdValuePair> mappingList = new ArrayList<>();

            int page = 0;
            for (List<OrderDisplayBean> resultList : allPages) {
                page++;
                String pageString = String.valueOf(page);

                String orderID = null;

                for (OrderDisplayBean resultItem : resultList) {
                    if (!resultItem.getId().equals(orderID)) {
                        orderID = resultItem.getId();
                        mappingList.add(new IdValuePair(orderID, pageString));
                    }
                }
            }

            return mappingList;
        }
    }
}
