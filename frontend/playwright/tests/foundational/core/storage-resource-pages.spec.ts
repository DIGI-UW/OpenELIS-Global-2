import { test, expect } from "../../../helpers/test-base";
import { LONG_TIMEOUT } from "../../../helpers/timeouts";

/**
 * Phase 9 — Storage resource pages smoke.
 *
 * Five per-resource pages replace the Tab panels that lived inside
 * the 4,902-line StorageDashboard. Each renders a read-only table
 * at its dedicated URL; Edit/Add/Delete actions land in Phase 10+.
 *
 * Phase 9 scope: prove the routes resolve to the right page + the
 * shared StorageResourcePage shell renders breadcrumb, h1, and
 * a <table>. Keeps the regression surface small — any row/column
 * detail is reserved for richer specs once Edit actions exist.
 */

const PAGES = [
  {
    path: "/Storage/rooms",
    heading: /^rooms$/i,
    breadcrumbTrailing: /rooms/i,
  },
  {
    path: "/Storage/devices",
    heading: /^devices$/i,
    breadcrumbTrailing: /devices/i,
  },
  {
    path: "/Storage/shelves",
    heading: /^shelves$/i,
    breadcrumbTrailing: /shelves/i,
  },
  {
    path: "/Storage/racks",
    heading: /^racks$/i,
    breadcrumbTrailing: /racks/i,
  },
  {
    path: "/Storage/boxes",
    heading: /^boxes$/i,
    breadcrumbTrailing: /boxes/i,
  },
];

test.describe("Storage resource pages (Phase 9)", () => {
  for (const page of PAGES) {
    test(`renders ${page.path} with breadcrumb, h1, and table`, async ({
      page: p,
    }) => {
      await p.goto(page.path, { waitUntil: "domcontentloaded" });
      await expect(p).toHaveURL(new RegExp(page.path.replace(/\//g, "\\/")), {
        timeout: LONG_TIMEOUT,
      });
      await expect(
        p.getByRole("heading", { level: 1, name: page.heading }),
      ).toBeVisible({ timeout: LONG_TIMEOUT });
      // Breadcrumb "Storage > <resource>"
      const nav = p.getByRole("navigation", { name: /breadcrumb/i });
      await expect(nav).toBeVisible();
      await expect(nav.getByRole("link", { name: /^storage$/i })).toBeVisible();
      // Table shell rendered (body may be empty if fixtures have no rows
      // for the resource; the thead always renders).
      await expect(p.locator("table")).toBeVisible({ timeout: LONG_TIMEOUT });
    });
  }
});
