package org.openelisglobal.common.rest.util;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.paging.*;
import org.openelisglobal.common.provider.query.PatientSearchResults;
import org.openelisglobal.common.provider.query.PatientSearchResultsForm;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.spring.util.SpringContext;

public class PatientSearchResultsPaging {

    private final PagingUtility<List<PatientSearchResults>> paging = new PagingUtility<>();

    private static final PatientSearchResultsPageHelper pagingHelper = new PatientSearchResultsPageHelper();

    public void setDatabaseResults(HttpServletRequest request, PatientSearchResultsForm form,
            List<PatientSearchResults> results)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {

        paging.setDatabaseResults(request.getSession(), results, pagingHelper);

        List<PatientSearchResults> patientSearchResults = paging.getPage(1, request.getSession());
        if (patientSearchResults != null) {
            form.setPatientSearchResults(patientSearchResults);
            form.setPaging(paging.getPagingBeanWithSearchMapping(1, request.getSession()));
        }
    }

    public void page(HttpServletRequest request, PatientSearchResultsForm form, int newPage)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        request.getSession().setAttribute(IActionConstants.SAVE_DISABLED, IActionConstants.FALSE);
        if (newPage < 0) {
            newPage = 0;
        }
        List<PatientSearchResults> resultPage = paging.getPage(newPage, request.getSession());
        if (resultPage != null) {
            form.setPatientSearchResults(resultPage);
            form.setPaging(paging.getPagingBeanWithSearchMapping(newPage, request.getSession()));
        }
    }

    private static class PatientSearchResultsPageHelper implements IPageDivider<List<PatientSearchResults>>,
            IPageUpdater<List<PatientSearchResults>>, IPageFlattener<List<PatientSearchResults>> {

        @Override
        public void createPages(List<PatientSearchResults> patientSearchResults,
                List<List<PatientSearchResults>> pagedResults) {
            List<PatientSearchResults> page = new ArrayList<>();
            boolean createNewPage = false;
            int resultCount = 0;

            for (PatientSearchResults item : patientSearchResults) {
                if (createNewPage) {
                    resultCount = 0;
                    createNewPage = false;
                    pagedResults.add(page);
                    page = new ArrayList<>();
                }
                if (resultCount >= SpringContext.getBean(PagingProperties.class).getPatientsPageSize()) {
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
        public List<IdValuePair> createSearchToPageMapping(List<List<PatientSearchResults>> allPages) {
            List<IdValuePair> mappingList = new ArrayList<>();
            int page = 0;

            for (List<PatientSearchResults> resultList : allPages) {
                page++;
                String pageString = String.valueOf(page);
                String currentPatientID = null;
                for (PatientSearchResults resultItem : resultList) {
                    if (!resultItem.getPatientID().equals(currentPatientID)) {
                        currentPatientID = resultItem.getPatientID();
                        mappingList.add(new IdValuePair(currentPatientID, pageString));
                    }
                }
            }

            return mappingList;
        }

        @Override
        public List<PatientSearchResults> flattenPages(List<List<PatientSearchResults>> pages) {
            List<PatientSearchResults> allResults = new ArrayList<>();

            for (List<PatientSearchResults> page : pages) {
                allResults.addAll(page);
            }

            return allResults;
        }

        @Override
        public void updateCache(List<PatientSearchResults> cacheItems, List<PatientSearchResults> clientItems) {
            for (int i = 0; i < clientItems.size(); i++) {
                cacheItems.set(i, clientItems.get(i));
            }
        }
    }
}
