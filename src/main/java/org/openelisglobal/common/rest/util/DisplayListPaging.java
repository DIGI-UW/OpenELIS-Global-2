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
import org.openelisglobal.common.rest.provider.form.DisplayListPagingForm;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.spring.util.SpringContext;

public class DisplayListPaging {
    private final PagingUtility<List<IdValuePair>> paging = new PagingUtility<>(
            IActionConstants.DISPLAY_LIST_SESSION_CACHE, IActionConstants.DISPLAY_LIST_MAPPING_SESSION_CACHE);

    private static final DisplayListPageHelper pagingHelper = new DisplayListPageHelper();

    public void setDatabaseResults(HttpServletRequest request, DisplayListPagingForm form,
            List<IdValuePair> displayList)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {

        paging.setDatabaseResults(request.getSession(), displayList, pagingHelper);

        List<IdValuePair> resultPage = paging.getPage(1, request.getSession());
        if (resultPage != null) {
            form.setDisplayListItems(resultPage);
            form.setPaging(paging.getPagingBeanWithSearchMapping(1, request.getSession()));
        }
    }

    public void page(HttpServletRequest request, DisplayListPagingForm form, int newPage)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {

        request.getSession().setAttribute(IActionConstants.SAVE_DISABLED, IActionConstants.FALSE);

        if (newPage < 0) {
            newPage = 0;
        }
        List<IdValuePair> resultPage = paging.getPage(newPage, request.getSession());
        if (resultPage != null) {
            form.setDisplayListItems(resultPage);
            form.setPaging(paging.getPagingBeanWithSearchMapping(newPage, request.getSession()));
        }
    }

    public List<IdValuePair> getResults(HttpServletRequest request) {
        return paging.getAllResults(request.getSession(), pagingHelper);
    }

    private static class DisplayListPageHelper implements IPageDivider<List<IdValuePair>>,
            IPageUpdater<List<IdValuePair>>, IPageFlattener<List<IdValuePair>> {

        @Override
        public void createPages(List<IdValuePair> displayList, List<List<IdValuePair>> pagedResults) {
            List<IdValuePair> page = new ArrayList<>();

            Boolean createNewPage = false;
            int resultCount = 0;

            for (IdValuePair item : displayList) {
                if (createNewPage) {
                    resultCount = 0;
                    createNewPage = false;
                    pagedResults.add(page);
                    page = new ArrayList<>();
                }
                if (resultCount >= SpringContext.getBean(PagingProperties.class).getDisplayListPageSize()) {
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
        public void updateCache(List<IdValuePair> cacheItems, List<IdValuePair> clientItems) {
            for (int i = 0; i < clientItems.size(); i++) {
                cacheItems.set(i, clientItems.get(i));
            }
        }

        @Override
        public List<IdValuePair> flattenPages(List<List<IdValuePair>> pages) {

            List<IdValuePair> allResults = new ArrayList<>();

            for (List<IdValuePair> page : pages) {
                for (IdValuePair item : page) {
                    allResults.add(item);
                }
            }

            return allResults;
        }

        @Override
        public List<IdValuePair> createSearchToPageMapping(List<List<IdValuePair>> allPages) {
            List<IdValuePair> mappingList = new ArrayList<>();

            int page = 0;
            for (List<IdValuePair> resultList : allPages) {
                page++;
                String pageString = String.valueOf(page);

                String orderID = null;

                for (IdValuePair resultItem : resultList) {
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
