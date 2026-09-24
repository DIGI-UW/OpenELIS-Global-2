/**
 * Carbon Pagination as a control over the server's own pages.
 *
 * The session-paged endpoints hand out one page per request (`?page=k`) and
 * announce paging.currentPage and paging.totalPages on every response. Carbon
 * page k is the server's page k: every move, whether from the arrows above the
 * table, Carbon's own arrows or its page selector, asks the server for exactly
 * that page and nothing else is fetched. Carbon's items per page is pinned to
 * the number of rows a full server page holds, which the server's paging
 * property decides (a property of 99 makes pages of 100), so it is read off a
 * page that is not the last one rather than sent from the browser.
 */

/**
 * The row count of a full server page, read off a page that is not the last
 * one. The last, or only, page does not tell, so the size seen before stands;
 * until one has been seen, the rows on the page are the best answer.
 */
export function serverPageSizeOf(paging, rowsOnPage, previousSize) {
  const current = Number(paging?.currentPage) || 1;
  const total = Number(paging?.totalPages) || 1;
  if (rowsOnPage > 0 && (current < total || !previousSize)) {
    return rowsOnPage;
  }
  return previousSize;
}

/** Whether the server split the list over more than one page. */
export function hasServerPages(paging) {
  return (Number(paging?.totalPages) || 1) > 1;
}

/**
 * The props of the arrows above a paged table, driven by the same paging
 * announcement as the Carbon Pagination below it, so both name the same page
 * and ask the server for the same one.
 */
export function serverPageArrowsProps({ paging, onPageRequest }) {
  const currentPage = Number(paging?.currentPage) || 1;
  const totalPages = Number(paging?.totalPages) || 1;
  return {
    show: totalPages > 1,
    currentPage,
    totalPages,
    onPrevious: () => onPageRequest(currentPage - 1),
    onNext: () => onPageRequest(currentPage + 1),
    previousDisabled: currentPage <= 1,
    nextDisabled: currentPage >= totalPages,
  };
}

/**
 * Carbon Pagination props that make Carbon's page the server's page. The item
 * count Carbon would print is replaced by the rows actually on screen, since
 * the server does not say how many rows the pages not yet fetched hold.
 *
 * The key remounts the Pagination when the page size is learnt: Carbon checks
 * a new pageSize against the pageSizes it had before, so changing both in one
 * render would leave it on the old size.
 */
export function serverPaginationProps({
  paging,
  rowsOnPage,
  pageSize,
  onPageRequest,
  intl,
}) {
  const page = Number(paging?.currentPage) || 1;
  const totalPages = Math.max(Number(paging?.totalPages) || 1, 1);
  const size = Math.max(Number(pageSize) || rowsOnPage || 1, 1);
  const itemsOnPage = () =>
    intl.formatMessage(
      { id: "pagination.items-on-page" },
      { count: rowsOnPage },
    );
  return {
    key: `server-page-size-${size}`,
    page,
    pageSize: size,
    pageSizes: [size],
    pageSizeInputDisabled: true,
    totalItems: rowsOnPage === 0 && totalPages === 1 ? 0 : totalPages * size,
    onChange: ({ page: requested }) => {
      if (requested !== page) {
        onPageRequest(requested);
      }
    },
    itemRangeText: itemsOnPage,
    itemText: itemsOnPage,
    itemsPerPageText: intl.formatMessage({ id: "pagination.items-per-page" }),
    pageNumberText: intl.formatMessage({ id: "pagination.page-number" }),
    pageRangeText: (_current, total) =>
      intl.formatMessage({ id: "pagination.page-range" }, { total }),
    pageText: (shown, pagesUnknown) =>
      intl.formatMessage(
        { id: "pagination.page" },
        { page: pagesUnknown ? "" : shown },
      ),
    forwardText: intl.formatMessage({ id: "pagination.forward" }),
    backwardText: intl.formatMessage({ id: "pagination.backward" }),
  };
}
