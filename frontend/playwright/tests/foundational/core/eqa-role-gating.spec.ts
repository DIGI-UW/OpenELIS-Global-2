import { test, expect } from "../../../helpers/test-base";
import { UI_TIMEOUT, NAV_TIMEOUT } from "../../../helpers/timeouts";

/**
 * The EQA scheme board seen by a laboratory that takes part in schemes but
 * does not run any (OGC-613).
 *
 * The board composes itself on the provider grant: a provider gets the scheme
 * table, everyone else gets pointers to their own participation. That branch
 * is unreachable for the ordinary test user, because every provider check ORs
 * in the global administrator role — so this spec runs as the participant-only
 * session instead, which is the only way the fallback is exercised at all.
 *
 * It also asserts the page does not fetch provider schemes for such a viewer.
 * That is not a performance nicety: the fetch is what would populate a table
 * of other laboratories' schemes for someone with no business seeing it.
 */

test.use({ storageState: "playwright/.auth/participant.json" });

test.describe("EQA scheme board without the provider grant", () => {
  test("a participant sees their own pages, and no provider fetch is made", async ({
    page,
  }) => {
    test.setTimeout(120_000);

    const providerRequests: string[] = [];
    page.on("request", (request) => {
      if (request.url().includes("/rest/eqa/provider/schemes")) {
        providerRequests.push(request.url());
      }
    });

    await page.goto("/qa/eqa/provider/schemes", { timeout: NAV_TIMEOUT });

    await expect(page.getByText("Participant view only")).toBeVisible({
      timeout: UI_TIMEOUT,
    });
    await expect(
      page.getByText(
        "Provider scheme administration needs the provider grant. Your lab's participation lives on the pages below.",
      ),
    ).toBeVisible();

    // The two offered routes are the viewer's own participation, and both are
    // links rather than dead text.
    const enrollments = page.getByRole("link", {
      name: "My enrollments — schemes this lab takes part in",
    });
    const cycles = page.getByRole("link", {
      name: "My Cycles — panels, results and deadlines",
    });
    await expect(enrollments).toHaveAttribute("href", "/qa/eqa/my-programs");
    await expect(cycles).toHaveAttribute("href", "/qa/eqa/my-cycles");

    // The provider table and its tiles belong to the other branch entirely.
    await expect(
      page.getByRole("heading", { name: "EQA schemes we provide" }),
    ).toHaveCount(0);
    await expect(page.getByTestId("kpi-active-schemes")).toHaveCount(0);
    expect(providerRequests).toEqual([]);

    await test.step("following the participant link lands on My Cycles", async () => {
      await cycles.click();
      await expect(
        page.getByRole("heading", { name: "My EQA Cycles" }),
      ).toBeVisible({ timeout: UI_TIMEOUT });
    });
  });
});
