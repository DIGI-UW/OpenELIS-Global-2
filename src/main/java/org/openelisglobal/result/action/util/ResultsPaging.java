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
package org.openelisglobal.result.action.util;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.paging.IPageDivider;
import org.openelisglobal.common.paging.IPageFlattener;
import org.openelisglobal.common.paging.IPageUpdater;
import org.openelisglobal.common.paging.PagingBean;
import org.openelisglobal.common.paging.PagingProperties;
import org.openelisglobal.common.paging.PagingUtility;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.result.form.ResultsPagingForm;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.beanItems.TestResultItem;

public class ResultsPaging {
    private PagingUtility<List<TestResultItem>> paging = new PagingUtility<>();

    private static TestItemPageHelper pagingHelper = new TestItemPageHelper();

    public void setDatabaseResults(HttpServletRequest request, ResultsPagingForm form, List<TestResultItem> tests)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {

        paging.setDatabaseResults(request.getSession(), tests, pagingHelper);

        List<TestResultItem> resultPage = paging.getPage(1, request.getSession());
        if (resultPage != null) {
            form.setTestResult(resultPage);
            form.setPaging(paging.getPagingBeanWithSearchMapping(1, request.getSession()));
        }
    }

    public void page(HttpServletRequest request, ResultsPagingForm form, int newPage)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {

        request.getSession().setAttribute(IActionConstants.SAVE_DISABLED, IActionConstants.FALSE);
        // List<TestResultItem> clientTests = form.getTestResult();
        // PagingBean bean = form.getPaging();
        // paging.updatePagedResults(request.getSession(), clientTests, bean,
        // pagingHelper);

        if (newPage < 0) {
            newPage = 0;
        }
        List<TestResultItem> resultPage = paging.getPage(newPage, request.getSession());
        if (resultPage != null) {
            form.setTestResult(resultPage);
            form.setTestSectionId("0");
            form.setPaging(paging.getPagingBeanWithSearchMapping(newPage, request.getSession()));
        }
    }

    public void updatePagedResults(HttpServletRequest request, ResultsPagingForm form) {
        List<TestResultItem> clientTests = form.getTestResult();
        PagingBean bean = form.getPaging();

        paging.updatePagedResults(request.getSession(), clientTests, bean, pagingHelper);
    }

    public List<TestResultItem> getResults(HttpServletRequest request) {
        return paging.getAllResults(request.getSession(), pagingHelper);
    }

    private static class TestItemPageHelper implements IPageDivider<List<TestResultItem>>,
            IPageUpdater<List<TestResultItem>>, IPageFlattener<List<TestResultItem>> {

        @Override
        public void createPages(List<TestResultItem> tests, List<List<TestResultItem>> pagedResults) {
            List<TestResultItem> page = new ArrayList<>();

            String accessionSequenceNumber = null;
            int resultCount = 0;

            for (TestResultItem item : tests) {
                if (accessionSequenceNumber != null
                        && !accessionSequenceNumber.equals(item.getSequenceAccessionNumber())) {
                    resultCount = 0;
                    accessionSequenceNumber = null;
                    pagedResults.add(page);
                    page = new ArrayList<>();
                }
                if (resultCount >= SpringContext.getBean(PagingProperties.class).getResultsPageSize()) {
                    accessionSequenceNumber = item.getSequenceAccessionNumber();
                }

                page.add(item);
                resultCount++;
            }

            if (!page.isEmpty() || pagedResults.isEmpty()) {
                pagedResults.add(page);
            }
        }

        @Override
        public void updateCache(List<TestResultItem> cacheItems, List<TestResultItem> clientItems) {
            for (int i = 0; i < clientItems.size(); i++) {
                if (clientItems.get(i).getIsModified()) {
                    cacheItems.set(i, clientItems.get(i));
                }
            }
        }

        @Override
        public List<TestResultItem> flattenPages(List<List<TestResultItem>> pages) {

            List<TestResultItem> allResults = new ArrayList<>();

            for (List<TestResultItem> page : pages) {
                for (TestResultItem item : page) {
                    allResults.add(item);
                }
            }

            return allResults;
        }

        @Override
        public List<IdValuePair> createSearchToPageMapping(List<List<TestResultItem>> allPages) {
            List<IdValuePair> mappingList = new ArrayList<>();

            int page = 0;
            for (List<TestResultItem> resultList : allPages) {
                page++;
                String pageString = String.valueOf(page);

                String currentAccession = null;

                for (TestResultItem resultItem : resultList) {
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
