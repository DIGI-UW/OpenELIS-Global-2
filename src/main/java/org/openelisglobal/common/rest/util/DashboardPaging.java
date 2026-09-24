package org.openelisglobal.common.rest.util;

import jakarta.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.openelisglobal.common.paging.IPageDivider;
import org.openelisglobal.common.paging.PagingBean;
import org.openelisglobal.common.paging.PagingProperties;
import org.openelisglobal.common.paging.PagingUtility;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.spring.util.SpringContext;

/**
 * Session paging for a dashboard list, in the shape every other paged list
 * uses: the first request caches the whole matching list in the session, cut
 * into pages of {@code paging.results.pageSize}, and each later {@code ?page=k}
 * request re-slices that cache. What is cached is whatever the dashboard finds
 * cheapest to keep, usually record ids, so the rows of a page are built only
 * when the page is asked for.
 *
 * <p>
 * Total pages are read off the cached pages, never off this object, so the same
 * helper serves every session.
 */
public class DashboardPaging<T> {

    private final PagingUtility<List<T>> paging;

    private static final class ResultsPageSizeDivider<T> implements IPageDivider<List<T>> {
        @Override
        public void createPages(List<T> items, List<List<T>> pagedResults) {
            int pageSize = Math.max(SpringContext.getBean(PagingProperties.class).getResultsPageSize(), 1);
            for (int start = 0; start < items.size(); start += pageSize) {
                pagedResults.add(new ArrayList<>(items.subList(start, Math.min(start + pageSize, items.size()))));
            }
        }

        @Override
        public List<IdValuePair> createSearchToPageMapping(List<List<T>> allPages) {
            return Collections.emptyList();
        }
    }

    /**
     * @param cacheName a name unique to the dashboard, so two dashboards open in
     *                  one session never read each other's pages
     */
    public DashboardPaging(String cacheName) {
        this.paging = new PagingUtility<>(cacheName + "Pages", cacheName + "PageMapping");
    }

    /** Caches {@code items} as the session's list and returns its first page. */
    public List<T> cache(HttpSession session, List<T> items) {
        paging.setDatabaseResults(session, items, new ResultsPageSizeDivider<>());
        return page(session, 1);
    }

    /**
     * One page of the cached list, or an empty page when nothing is cached or the
     * page is out of range.
     */
    public List<T> page(HttpSession session, int pageNumber) {
        List<List<T>> pages = paging.getAllPages(session);
        if (pages == null || pageNumber < 1 || pageNumber > pages.size()) {
            return Collections.emptyList();
        }
        return pages.get(pageNumber - 1);
    }

    /**
     * The page announcement for {@code pageNumber}: the page shown and how many
     * pages the session holds.
     */
    public PagingBean pagingBean(HttpSession session, int pageNumber) {
        List<List<T>> pages = paging.getAllPages(session);
        PagingBean bean = new PagingBean();
        bean.setCurrentPage(String.valueOf(Math.max(pageNumber, 1)));
        bean.setTotalPages(String.valueOf(pages == null || pages.isEmpty() ? 1 : pages.size()));
        bean.setSearchTermToPage(Collections.emptyList());
        return bean;
    }

    /** How many items the session's list holds altogether. */
    public int totalItems(HttpSession session) {
        List<List<T>> pages = paging.getAllPages(session);
        if (pages == null) {
            return 0;
        }
        int total = 0;
        for (List<T> page : pages) {
            total += page.size();
        }
        return total;
    }
}
