import { test, expect } from "../../../helpers/test-base";
import {
  StorageManagement,
  type StorageLevel,
} from "../../../fixtures/storage-management";

/**
 * Storage hierarchy levels on the Storage Management dashboard.
 *
 * The five levels (Rooms/Devices/Shelves/Racks/Boxes) no longer have a page
 * each. They are tiles on the Dashboard tab of /Storage, and the tile you
 * pick decides which level the single table below shows. The old
 * /Storage/{level} URLs still resolve — they deep-link to that tile.
 *
 * This spec proves what the five per-page smoke tests used to prove: each
 * level is reachable at its canonical URL and renders *that* level's table.
 * Level identity is asserted on the table's own column headers (only Boxes
 * has Capacity, only Racks is parented by a Shelf, and so on), which is the
 * assertion that replaces the per-page <h1>.
 */

const LEVELS: Array<{ level: StorageLevel; columns: string[] }> = [
  { level: "rooms", columns: ["Name", "Code", "Status"] },
  { level: "devices", columns: ["Name", "Code", "Room", "Status"] },
  { level: "shelves", columns: ["Label", "Code", "Device", "Status"] },
  { level: "racks", columns: ["Label", "Code", "Shelf", "Status"] },
  { level: "boxes", columns: ["Label", "Code", "Rack", "Capacity", "Status"] },
];

test.describe("Storage Management — hierarchy levels", () => {
  for (const { level, columns } of LEVELS) {
    test(`/Storage/${level} deep-links to the ${level} tile and table`, async ({
      page,
    }) => {
      const storage = new StorageManagement(page);

      // Breadcrumb + heading + Dashboard tab + exactly this tile pressed.
      await storage.gotoLevel(level);

      await expect(storage.columnHeaders).toHaveText(columns);
    });
  }

  test("picking a tile swaps the level the dashboard table shows", async ({
    page,
  }) => {
    const storage = new StorageManagement(page);

    await test.step("/Storage opens on the Dashboard tab showing Rooms", async () => {
      await page.goto("/Storage", { waitUntil: "domcontentloaded" });
      await storage.expectContainer();
      await storage.expectTabSelected("Dashboard");
      await storage.expectLevelSelected("rooms");
      await expect(storage.columnHeaders).toHaveText([
        "Name",
        "Code",
        "Status",
      ]);
    });

    await test.step("clicking the Boxes tile shows the boxes table", async () => {
      await storage.selectLevel("boxes");
      await expect(storage.columnHeaders).toHaveText([
        "Label",
        "Code",
        "Rack",
        "Capacity",
        "Status",
      ]);
    });

    await test.step("clicking the Rooms tile switches back", async () => {
      await storage.selectLevel("rooms");
      await expect(storage.columnHeaders).toHaveText([
        "Name",
        "Code",
        "Status",
      ]);
    });
  });
});
