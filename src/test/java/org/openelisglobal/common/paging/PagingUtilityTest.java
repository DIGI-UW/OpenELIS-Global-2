package org.openelisglobal.common.paging;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.openelisglobal.common.action.IActionConstants;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.server.ResponseStatusException;

/**
 * A paged save writes the posted rows into the page the session served, so a
 * post that names no loaded page is the caller's error and is refused with a
 * 400 saying what is wrong, never a bare 500.
 */
public class PagingUtilityTest {

    private final MockHttpSession session = new MockHttpSession();
    private final PagingUtility<List<String>> paging = new PagingUtility<>();
    private final IPageUpdater<List<String>> updater = (cache, client) -> {
        for (int i = 0; i < client.size(); i++) {
            cache.set(i, client.get(i));
        }
    };

    @Test
    public void aSaveBeforeAnyPageWasLoadedIsRefusedSayingSo() {
        ResponseStatusException refusal = assertThrows(ResponseStatusException.class,
                () -> paging.updatePagedResults(session, List.of("a"), page("1"), updater));

        assertEquals(HttpStatus.BAD_REQUEST, refusal.getStatusCode());
        assertTrue(refusal.getReason(), refusal.getReason().contains("load"));
    }

    @Test
    public void readingTheLoadedPagesBeforeAnyWasLoadedIsRefused() {
        ResponseStatusException refusal = assertThrows(ResponseStatusException.class,
                () -> paging.getLoadedPages(session));

        assertEquals(HttpStatus.BAD_REQUEST, refusal.getStatusCode());
    }

    @Test
    public void aSaveWithoutPagingIsRefusedNamingTheField() {
        loadPages(2);

        ResponseStatusException refusal = assertThrows(ResponseStatusException.class,
                () -> paging.updatePagedResults(session, List.of("a"), null, updater));

        assertEquals(HttpStatus.BAD_REQUEST, refusal.getStatusCode());
        assertTrue(refusal.getReason(), refusal.getReason().contains("paging.currentPage"));
        assertTrue(refusal.getReason(), refusal.getReason().contains("1 to 2"));
    }

    @Test
    public void aCurrentPageThatIsNotALoadedPageIsRefusedNamingIt() {
        loadPages(2);

        for (String currentPage : new String[] { null, "abc", "0", "-1", "3" }) {
            ResponseStatusException refusal = assertThrows(ResponseStatusException.class,
                    () -> paging.updatePagedResults(session, List.of("a"), page(currentPage), updater));

            assertEquals(currentPage, HttpStatus.BAD_REQUEST, refusal.getStatusCode());
            assertTrue(refusal.getReason(), refusal.getReason().endsWith(": " + currentPage));
        }
    }

    @Test
    public void moreRowsThanThePageHoldsIsRefusedNamingBothCounts() {
        loadPages(2);

        ResponseStatusException refusal = assertThrows(ResponseStatusException.class,
                () -> paging.updatePagedResults(session, List.of("a", "b", "c"), page("2"), updater));

        assertEquals(HttpStatus.BAD_REQUEST, refusal.getStatusCode());
        assertTrue(refusal.getReason(), refusal.getReason().contains("3 rows"));
        assertTrue(refusal.getReason(), refusal.getReason().contains("page 2 holds 2"));
    }

    @Test
    public void aLoadedPageTakesThePostedRowsAndLeavesTheOthersAlone() {
        loadPages(2);

        paging.updatePagedResults(session, List.of("edited"), page("2"), updater);

        List<List<String>> pages = paging.getLoadedPages(session);
        assertEquals(List.of("p1-r1", "p1-r2"), pages.get(0));
        assertEquals(List.of("edited", "p2-r2"), pages.get(1));
    }

    private void loadPages(int count) {
        List<List<String>> pages = new ArrayList<>();
        for (int p = 1; p <= count; p++) {
            pages.add(new ArrayList<>(List.of("p" + p + "-r1", "p" + p + "-r2")));
        }
        session.setAttribute(IActionConstants.RESULTS_SESSION_CACHE, pages);
    }

    private static PagingBean page(String currentPage) {
        PagingBean bean = new PagingBean();
        bean.setCurrentPage(currentPage);
        bean.setTotalPages("2");
        return bean;
    }
}
