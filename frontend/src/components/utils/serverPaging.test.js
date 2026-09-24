import { describe, expect, it, vi } from "vitest";
import {
  hasServerPages,
  serverPageArrowsProps,
  serverPageSizeOf,
  serverPaginationProps,
} from "./serverPaging";

const intl = {
  formatMessage: ({ id }, values) =>
    values ? `${id}:${JSON.stringify(values)}` : id,
};

describe("serverPageSizeOf", () => {
  it("reads the size off a page that is not the last one", () => {
    expect(
      serverPageSizeOf({ currentPage: "1", totalPages: "5" }, 100, undefined),
    ).toBe(100);
    expect(serverPageSizeOf({ currentPage: 2, totalPages: 5 }, 103, 100)).toBe(
      103,
    );
  });

  it("keeps the size seen before when the last page is shorter", () => {
    expect(
      serverPageSizeOf({ currentPage: "5", totalPages: "5" }, 13, 100),
    ).toBe(100);
  });

  it("takes the rows on a single page when no full page has been seen", () => {
    expect(
      serverPageSizeOf({ currentPage: "1", totalPages: "1" }, 37, undefined),
    ).toBe(37);
    expect(serverPageSizeOf(undefined, 0, undefined)).toBeUndefined();
    expect(serverPageSizeOf(undefined, 0, 100)).toBe(100);
  });
});

describe("hasServerPages", () => {
  it("is true only when the server split the list", () => {
    expect(hasServerPages({ totalPages: "3" })).toBe(true);
    expect(hasServerPages({ totalPages: 1 })).toBe(false);
    expect(hasServerPages(undefined)).toBe(false);
  });
});

describe("serverPaginationProps", () => {
  const props = (overrides = {}) =>
    serverPaginationProps({
      paging: { currentPage: "2", totalPages: "5" },
      rowsOnPage: 100,
      pageSize: 100,
      onPageRequest: vi.fn(),
      intl,
      ...overrides,
    });

  it("makes Carbon's page the server's page with a single, pinned page size", () => {
    const p = props();
    expect(p.page).toBe(2);
    expect(p.pageSize).toBe(100);
    expect(p.pageSizes).toEqual([100]);
    expect(p.pageSizeInputDisabled).toBe(true);
    expect(p.totalItems).toBe(500);
    expect(p.key).toBe("server-page-size-100");
    expect(props({ pageSize: 103 }).key).toBe("server-page-size-103");
  });

  it("asks the server for exactly the page Carbon moves to, and not for the page shown", () => {
    const onPageRequest = vi.fn();
    const p = props({ onPageRequest });
    p.onChange({ page: 4, pageSize: 100 });
    p.onChange({ page: 2, pageSize: 100 });
    expect(onPageRequest).toHaveBeenCalledTimes(1);
    expect(onPageRequest).toHaveBeenCalledWith(4);
  });

  it("labels the rows actually on screen instead of guessing a range", () => {
    const p = props({ rowsOnPage: 103 });
    expect(p.itemRangeText(101, 200, 500)).toBe(
      'pagination.items-on-page:{"count":103}',
    );
    expect(p.pageRangeText(2, 5)).toBe('pagination.page-range:{"total":5}');
  });

  it("falls back to the rows on the page before a full page has been seen", () => {
    const p = props({
      paging: { currentPage: "1", totalPages: "1" },
      rowsOnPage: 37,
      pageSize: undefined,
    });
    expect(p.pageSize).toBe(37);
    expect(p.totalItems).toBe(37);
  });

  it("shows an empty list as no items", () => {
    const p = props({ paging: undefined, rowsOnPage: 0, pageSize: undefined });
    expect(p.totalItems).toBe(0);
    expect(p.page).toBe(1);
  });
});

describe("serverPageArrowsProps", () => {
  it("names the server's page and asks for the neighbouring pages", () => {
    const onPageRequest = vi.fn();
    const arrows = serverPageArrowsProps({
      paging: { currentPage: "2", totalPages: "4" },
      onPageRequest,
    });
    expect(arrows.show).toBe(true);
    expect(arrows.currentPage).toBe(2);
    expect(arrows.totalPages).toBe(4);
    expect(arrows.previousDisabled).toBe(false);
    expect(arrows.nextDisabled).toBe(false);
    arrows.onNext();
    arrows.onPrevious();
    expect(onPageRequest.mock.calls).toEqual([[3], [1]]);
  });

  it("disables the arrow that would leave the list, and hides on a single page", () => {
    expect(
      serverPageArrowsProps({
        paging: { currentPage: 1, totalPages: 4 },
        onPageRequest: vi.fn(),
      }).previousDisabled,
    ).toBe(true);
    expect(
      serverPageArrowsProps({
        paging: { currentPage: 4, totalPages: 4 },
        onPageRequest: vi.fn(),
      }).nextDisabled,
    ).toBe(true);
    expect(
      serverPageArrowsProps({ paging: undefined, onPageRequest: vi.fn() }).show,
    ).toBe(false);
  });
});
