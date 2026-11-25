import { test, expect } from "../../../helpers/test-base";

/**
 * OGC-1680: Custom Data Export names tests as the rest of the application
 * does. The demo catalog stores Creatinine's description in French
 * ("Créatinine(Serum)") with an English translation; an English user finds it
 * in the field list as "Creatinine(Serum)", the name order entry shows.
 */

test("the field list names a test as order entry does, in the user's language", async ({
  page,
}) => {
  const errors: string[] = [];
  page.on("pageerror", (error) => errors.push(error.message));
  await page.goto("/reports/custom-data-export");
  await page
    .getByRole("button", { name: "Start a new export", exact: true })
    .click();
  await page.getByRole("radio", { name: /^Sample & Testing/ }).click();
  const search = page.getByRole("searchbox", {
    name: "Find a field",
    exact: true,
  });
  await search.fill("atinine");
  const available = page.getByRole("region", { name: "Available fields" });
  await expect(
    available.getByRole("button", {
      name: "Add Creatinine(Serum)",
      exact: true,
    }),
  ).toBeVisible();
  await expect(
    available.getByRole("button", { name: /^Add Créatinine/ }),
  ).toHaveCount(0);
  expect(errors).toEqual([]);
});
