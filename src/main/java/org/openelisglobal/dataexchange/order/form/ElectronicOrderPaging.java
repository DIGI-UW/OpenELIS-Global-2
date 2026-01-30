package org.openelisglobal.dataexchange.order.form;

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
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrderDisplayItem;
import org.openelisglobal.spring.util.SpringContext;

/**
 * Responsible for building a page by accepting all display and paging
 * information. Based on the page size and the page number, it will return the
 * appropriate page.
 */
public class ElectronicOrderPaging {

    private final PagingUtility<List<ElectronicOrderDisplayItem>> paging = new PagingUtility<>(); // Adjust type based
                                                                                                  // on your
    // data
    private static final ElectronicOrderPageHelper pagingHelper = new ElectronicOrderPageHelper(); // Implement helper
                                                                                                   // class

    public void setDatabaseResults(HttpServletRequest request, ElectronicOrderViewForm form,
            List<ElectronicOrderDisplayItem> orders)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {

        paging.setDatabaseResults(request.getSession(), orders, pagingHelper);

        List<ElectronicOrderDisplayItem> resultPage = paging.getPage(1, request.getSession());
        if (resultPage != null) {
            form.seteOrders(resultPage);
            form.setPaging(paging.getPagingBeanWithSearchMapping(1, request.getSession()));
        }
    }

    public void page(HttpServletRequest request, ElectronicOrderViewForm form, int newPage)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {

        request.getSession().setAttribute(IActionConstants.SAVE_DISABLED, IActionConstants.FALSE);

        if (newPage < 0) {
            newPage = 0;
        }
        List<ElectronicOrderDisplayItem> resultPage = paging.getPage(newPage, request.getSession());
        if (resultPage != null) {
            form.seteOrders(resultPage);

            form.setPaging(paging.getPagingBeanWithSearchMapping(newPage, request.getSession()));
        }
    }

    public List<ElectronicOrderDisplayItem> getResults(HttpServletRequest request) {
        return paging.getAllResults(request.getSession(), pagingHelper);
    }

    private static class ElectronicOrderPageHelper implements IPageDivider<List<ElectronicOrderDisplayItem>>,
            IPageUpdater<List<ElectronicOrderDisplayItem>>, IPageFlattener<List<ElectronicOrderDisplayItem>> {

        @Override
        public void createPages(List<ElectronicOrderDisplayItem> orders,
                List<List<ElectronicOrderDisplayItem>> pagedResults) {
            List<ElectronicOrderDisplayItem> page = new ArrayList<>();

            Boolean createNewPage = false;
            int resultCount = 0;

            for (ElectronicOrderDisplayItem item : orders) {
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
        public void updateCache(List<ElectronicOrderDisplayItem> cacheItems,
                List<ElectronicOrderDisplayItem> clientItems) {
            for (int i = 0; i < clientItems.size(); i++) {
                cacheItems.set(i, clientItems.get(i));
            }
        }

        @Override
        public List<ElectronicOrderDisplayItem> flattenPages(List<List<ElectronicOrderDisplayItem>> pages) {

            List<ElectronicOrderDisplayItem> allResults = new ArrayList<>();

            for (List<ElectronicOrderDisplayItem> page : pages) {
                for (ElectronicOrderDisplayItem item : page) {
                    allResults.add(item);
                }
            }

            return allResults;
        }

        @Override
        public List<IdValuePair> createSearchToPageMapping(List<List<ElectronicOrderDisplayItem>> allPages) {
            List<IdValuePair> mappingList = new ArrayList<>();

            int page = 0;
            for (List<ElectronicOrderDisplayItem> resultList : allPages) {
                page++;
                String pageString = String.valueOf(page);

                String orderID = null;

                for (ElectronicOrderDisplayItem resultItem : resultList) {
                    if (!resultItem.getElectronicOrderId().equals(orderID)) {
                        orderID = resultItem.getElectronicOrderId();
                        mappingList.add(new IdValuePair(orderID, pageString));
                    }
                }
            }
            return mappingList;
        }
    }
}
