import { test, expect } from "../../../helpers/test-base";
import { LONG_TIMEOUT } from "../../../helpers/timeouts";

/**
 * Storage resource pages smoke.
 *
 * Five per-resource pages (Rooms/Devices/Shelves/Racks/Boxes) render
 * a read-only table at their canonical /Storage/{resource} URL. This
 * spec proves the route resolves + the shared StorageResourcePage
 * shell renders breadcrumb, h1, and a <table>. Row/column detail is
 * left for richer specs.
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

test.describe("Storage resource pages", () => {
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
