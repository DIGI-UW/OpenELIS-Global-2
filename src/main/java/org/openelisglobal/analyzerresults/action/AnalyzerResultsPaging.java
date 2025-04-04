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
package org.openelisglobal.analyzerresults.action;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.analyzerresults.action.beanitems.AnalyzerResultItem;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.form.IPagingForm;
import org.openelisglobal.common.paging.IPageDivider;
import org.openelisglobal.common.paging.IPageFlattener;
import org.openelisglobal.common.paging.IPageUpdater;
import org.openelisglobal.common.paging.PagingBean;
import org.openelisglobal.common.paging.PagingProperties;
import org.openelisglobal.common.paging.PagingUtility;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.result.form.AnalyzerResultsForm;
import org.openelisglobal.spring.util.SpringContext;

public class AnalyzerResultsPaging {

    private PagingUtility<List<AnalyzerResultItem>> paging = new PagingUtility<>();
    private static TestItemPageHelper pagingHelper = new TestItemPageHelper();

    public void setDatabaseResults(HttpServletRequest request, AnalyzerResultsForm form, List<AnalyzerResultItem> tests)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {

        paging.setDatabaseResults(request.getSession(), tests, pagingHelper);

        List<AnalyzerResultItem> resultPage = paging.getPage(1, request.getSession());
        if (resultPage != null) {
            form.setResultList(resultPage);
            form.setPaging(paging.getPagingBeanWithSearchMapping(1, request.getSession()));
        }
    }

    public void page(HttpServletRequest request, AnalyzerResultsForm form, int newPage)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {

        if (newPage < 0) {
            newPage = 0;
        }

        request.getSession().setAttribute(IActionConstants.SAVE_DISABLED, IActionConstants.FALSE);
        // List<AnalyzerResultItem> clientTests = form.getResultList();
        // PagingBean bean = form.getPaging();

        // paging.updatePagedResults(request.getSession(), clientTests, bean,
        // pagingHelper);

        List<AnalyzerResultItem> resultPage = paging.getPage(newPage, request.getSession());
        if (resultPage != null) {
            form.setResultList(resultPage);
            form.setPaging(paging.getPagingBeanWithSearchMapping(newPage, request.getSession()));
        }
    }

    public void updatePagedResults(HttpServletRequest request, AnalyzerResultsForm form) {
        List<AnalyzerResultItem> clientTests = form.getResultList();
        PagingBean bean = form.getPaging();

        paging.updatePagedResults(request.getSession(), clientTests, bean, pagingHelper);
    }

    public List<AnalyzerResultItem> getResults(HttpServletRequest request) {
        return paging.getAllResults(request.getSession(), pagingHelper);
    }

    public void setEmptyPageBean(HttpServletRequest request, IPagingForm form)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        form.setPaging(paging.getPagingBeanWithSearchMapping(0, request.getSession()));
    }

    private static class TestItemPageHelper implements IPageDivider<List<AnalyzerResultItem>>,
            IPageUpdater<List<AnalyzerResultItem>>, IPageFlattener<List<AnalyzerResultItem>> {

        @Override
        public void createPages(List<AnalyzerResultItem> tests, List<List<AnalyzerResultItem>> pagedResults) {
            List<AnalyzerResultItem> page = new ArrayList<>();

            int sampleGroupingNumber = -1;
            int resultCount = 0;

            for (AnalyzerResultItem item : tests) {
                if (sampleGroupingNumber != -1 && sampleGroupingNumber != item.getSampleGroupingNumber()) {
                    resultCount = 0;
                    sampleGroupingNumber = -1;
                    pagedResults.add(page);
                    page = new ArrayList<>();
                }
                if (resultCount >= SpringContext.getBean(PagingProperties.class).getResultsPageSize()) {
                    sampleGroupingNumber = item.getSampleGroupingNumber();
                }

                page.add(item);
                resultCount++;
            }

            if (!page.isEmpty() || pagedResults.isEmpty()) {
                pagedResults.add(page);
            }
        }

        @Override
        public void updateCache(List<AnalyzerResultItem> cacheItems, List<AnalyzerResultItem> clientItems) {
            for (int i = 0; i < clientItems.size(); i++) {
                cacheItems.set(i, clientItems.get(i));
            }
        }

        @Override
        public List<AnalyzerResultItem> flattenPages(List<List<AnalyzerResultItem>> pages) {

            List<AnalyzerResultItem> allResults = new ArrayList<>();

            for (List<AnalyzerResultItem> page : pages) {
                for (AnalyzerResultItem item : page) {
                    allResults.add(item);
                }
            }
            return allResults;
        }

        @Override
        public List<IdValuePair> createSearchToPageMapping(List<List<AnalyzerResultItem>> allPages) {
            List<IdValuePair> mappingList = new ArrayList<>();

            int page = 0;
            for (List<AnalyzerResultItem> resultList : allPages) {
                page++;
                String pageString = String.valueOf(page);

                String currentAccession = null;

                for (AnalyzerResultItem resultItem : resultList) {
                    if (!resultItem.getAccessionNumber().equals(currentAccession)) {
                        currentAccession = resultItem.getAccessionNumber();
                        mappingList.add(new IdValuePair(currentAccession, pageString));
                    }
                }
            }

            return mappingList;
        }
    }
}
