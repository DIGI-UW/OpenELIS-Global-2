/**
 * The admin menus are served one fixed-size page at a time, addressed by the
 * number of its first record (startingRecNo), and every page comes back with
 * fromRecordCount, toRecordCount and totalRecordCount. The page size is the
 * server's (the page.defaultPageSize property, 20) and cannot be asked for in
 * the request, so Carbon's page is the server's page and its items per page is
 * pinned to what the server returns.
 */
export const DEFAULT_SERVER_PAGE_SIZE = 20;

/**
 * The server's page size, read off a page that is not the last one (such a page
 * is full). The last, or only, page does not reveal it, so the default stands.
 */
export function serverPageSizeFrom(
  fromRecordCount: string | number | undefined,
  toRecordCount: string | number | undefined,
  totalRecordCount: string | number | undefined,
  fallback: number = DEFAULT_SERVER_PAGE_SIZE,
): number {
  const from = Number(fromRecordCount);
  const to = Number(toRecordCount);
  const total = Number(totalRecordCount);
  if ([from, to, total].every(Number.isFinite) && to >= from && to < total) {
    return to - from + 1;
  }
  return fallback;
}

/** The first record number of server page `page`. */
export function startingRecNoFor(page: number, serverPageSize: number): number {
  return (Math.max(page, 1) - 1) * Math.max(serverPageSize, 1) + 1;
}
