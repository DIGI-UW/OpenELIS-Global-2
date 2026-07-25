import { Linter } from "eslint";
import { describe, expect, test } from "vitest";
import rule from "./pw-demo-no-backend-access.js";

const verify = (code) => {
  const linter = new Linter({ configType: "flat" });
  return linter.verify(code, [
    {
      languageOptions: {
        ecmaVersion: "latest",
        sourceType: "module",
        parserOptions: { ecmaFeatures: { jsx: true } },
      },
      plugins: {
        local: {
          rules: { "pw-demo-no-backend-access": rule },
        },
      },
      rules: {
        "local/pw-demo-no-backend-access": "error",
      },
    },
  ]);
};

describe("pw-demo-no-backend-access", () => {
  test.each([
    ["page request post", "await page.request.post('/rest/analyzer')"],
    ["request fixture get", "await request.get('/rest/analyzer')"],
    [
      "aliased request fixture",
      "test('demo', async ({ request: api }) => { await api.put('/rest/analyzer') })",
    ],
    ["browser fetch", "await fetch('/rest/analyzer')"],
    ["evaluate fetch", "await page.evaluate(() => fetch('/rest/analyzer'))"],
  ])("rejects %s", (name, code) => {
    expect(verify(code).map((message) => message.ruleId)).toContain(
      "local/pw-demo-no-backend-access",
    );
  });

  test("allows visible UI interaction and assertions", () => {
    expect(
      verify(
        "await page.getByRole('button', { name: 'Save' }).click(); await expect(page.getByText('Saved')).toBeVisible();",
      ),
    ).toEqual([]);
  });
});
