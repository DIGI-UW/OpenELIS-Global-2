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
package org.openelisglobal.common.paging;

import jakarta.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.util.IdValuePair;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class PagingUtility<E> {
    private int totalPages = 0;
    private String sessionCache = IActionConstants.RESULTS_SESSION_CACHE;
    private String mappingsessionCache = IActionConstants.RESULTS_PAGE_MAPPING_SESSION_CACHE;

    public PagingUtility(String sessionCache, String mappingsessionCache) {
        this.sessionCache = sessionCache;
        this.mappingsessionCache = mappingsessionCache;
    }

    public PagingUtility() {
    }

    /**
     * @param session the Session for the current HttpRequest
     * @param items   The items which will be divided into pages
     * @param divider The object which knows how to divide the objects into pages
     */
    public void setDatabaseResults(HttpSession session, E items, IPageDivider<E> divider) {

        List<E> pagedResults = new ArrayList<>();
        divider.createPages(items, pagedResults);
        session.setAttribute(getSessionCache(), pagedResults);
        List<IdValuePair> searchPageMapping = divider.createSearchToPageMapping(pagedResults);
        session.setAttribute(getMappingsessionCache(), searchPageMapping);
        totalPages = pagedResults.size();
    }

    @SuppressWarnings("unchecked")
    /*
     * @param page First page is page 1
     *
     * @param session Session for this request
     */
    public E getPage(int page, HttpSession session) {
        if (page > 0) {
            List<E> pagedResults = (List<E>) session.getAttribute(getSessionCache());
            if (pagedResults == null) {
                org.openelisglobal.common.log.LogEvent.logWarn(this.getClass().getSimpleName(), "getPage",
                        "pagedResults is null in session cache for page " + page);
                return null;
            }
            totalPages = pagedResults.size();
            org.openelisglobal.common.log.LogEvent.logInfo(this.getClass().getSimpleName(), "getPage",
                    "Requesting page " + page + ", totalPages=" + totalPages + ", pagedResults.size()="
                            + pagedResults.size());
            if (pagedResults.size() >= page) {
                return pagedResults.get(page - 1);
            } else {
                org.openelisglobal.common.log.LogEvent.logWarn(this.getClass().getSimpleName(), "getPage",
                        "Page " + page + " requested but only " + pagedResults.size() + " pages available");
            }
        }

        return null;
    }

    /**
     * @param session     the Session for the current HttpRequest
     * @param clientItems The items from the client which may have been edited
     * @param paging      The paging bean, it knows the current page
     * @param updater     The object which knows how to update the cache
     */
    public void updatePagedResults(HttpSession session, E clientItems, PagingBean paging, IPageUpdater<E> updater) {
        List<E> pagedResults = getLoadedPages(session);
        updateSessionResultCache(pagedResults, clientItems, paging, updater);
        totalPages = pagedResults.size();
    }

    /**
     * @param session the Session for the current HttpRequest
     * @return The pages this session loaded
     * @throws ResponseStatusException 400 when the session loaded none, since a
     *                                 save can only apply its rows to a page the
     *                                 session served
     */
    public List<E> getLoadedPages(HttpSession session) {
        List<E> pagedResults = getAllPages(session);
        if (pagedResults == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No results are loaded in this session; load them before saving");
        }
        return pagedResults;
    }

    /**
     * @param session   the Session for the current HttpRequest
     * @param flattener The object which knows how to take a list of pages and make
     *                  it into a flat list.
     * @return The flattened list
     */
    public E getAllResults(HttpSession session, IPageFlattener<E> flattener) {
        List<E> pagedResults = getAllPages(session);
        return flattener.flattenPages(pagedResults);
    }

    private void updateSessionResultCache(List<E> pagedResults, E clientTests, PagingBean paging,
            IPageUpdater<E> updater) {

        int currentPage = loadedPageIndex(paging, pagedResults.size());

        E sessionTests = pagedResults.get(currentPage);

        if (clientTests instanceof List<?> clientRows && sessionTests instanceof List<?> pageRows
                && clientRows.size() > pageRows.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The save posts " + clientRows.size()
                    + " rows but page " + (currentPage + 1) + " holds " + pageRows.size());
        }

        updater.updateCache(sessionTests, clientTests);
    }

    private static int loadedPageIndex(PagingBean paging, int pageCount) {
        String currentPage = paging == null ? null : paging.getCurrentPage();
        int page;
        try {
            page = Integer.parseInt(currentPage);
        } catch (NumberFormatException e) {
            page = 0;
        }
        if (page < 1 || page > pageCount) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "paging.currentPage must name a loaded page, 1 to " + pageCount + ": " + currentPage);
        }
        return page - 1;
    }

    /**
     * @param currentPage The new current page
     * @return The bean with the new current page and the total pages
     */
    public PagingBean getPagingBean(int currentPage) {
        PagingBean paging = new PagingBean();
        paging.setCurrentPage(String.valueOf(currentPage));
        paging.setTotalPages(String.valueOf(totalPages));
        return paging;
    }

    /**
     * @param currentPage The new current page
     * @param the         session which will cause the mapping from search terms to
     *                    pages to be loaded
     * @return The bean with the new current page and the total pages
     */
    public PagingBean getPagingBeanWithSearchMapping(int currentPage, HttpSession session) {
        PagingBean paging = new PagingBean();
        paging.setCurrentPage(String.valueOf(currentPage));
        paging.setTotalPages(String.valueOf(totalPages));

        paging.setSearchTermToPage(getPageMapping(session));

        return paging;
    }

    /**
     * @param session The session object which holds the pages
     * @return The pages as a list
     */
    @SuppressWarnings("unchecked")
    public List<E> getAllPages(HttpSession session) {
        return (List<E>) session.getAttribute(getSessionCache());
    }

    @SuppressWarnings("unchecked")
    public List<IdValuePair> getPageMapping(HttpSession session) {
        List<IdValuePair> pairList = (List<IdValuePair>) session.getAttribute(getMappingsessionCache());
        if (pairList == null) {
            pairList = new ArrayList<>();
        }

        return pairList;
    }

    private String getSessionCache() {
        return sessionCache;
    }

    private String getMappingsessionCache() {
        return mappingsessionCache;
    }
}
