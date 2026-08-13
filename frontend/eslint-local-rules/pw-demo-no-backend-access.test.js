import { Linter } from "eslint";
import tsParser from "@typescript-eslint/parser";
import { describe, expect, test } from "vitest";
import rule from "./pw-demo-no-backend-access.js";

const RULE_ID = "local/pw-demo-no-backend-access";

const verify = (code) => {
  const linter = new Linter({ configType: "flat" });
  return linter.verify(code, [
    {
      languageOptions: {
        ecmaVersion: "latest",
        parser: tsParser,
        sourceType: "module",
        parserOptions: { ecmaFeatures: { jsx: true } },
      },
      plugins: {
        local: {
          rules: { "pw-demo-no-backend-access": rule },
        },
      },
      rules: {
        [RULE_ID]: "error",
      },
    },
  ]);
};

describe("pw-demo-no-backend-access", () => {
  test.each([
    [
      "page request",
      "await page.request.post('/rest/analyzer')",
      "backendRequest",
    ],
    [
      "request fixture",
      "await request.get('/rest/analyzer')",
      "backendRequest",
    ],
    [
      "renamed request fixture",
      "test('demo', async ({ request: api }) => { await api.put('/rest/analyzer') })",
      "backendRequest",
    ],
    [
      "request context alias",
      "const api = page.request; await api.patch('/rest/analyzer')",
      "backendRequest",
    ],
    [
      "computed request call",
      "await page['request']['delete']('/rest/analyzer')",
      "backendRequest",
    ],
    [
      "request client passed to a helper",
      "await pushAnalyzerResult(page.request)",
      "backendRequestAccess",
    ],
    [
      "request fixture passed to a helper",
      "test('demo', async ({ request }) => { await pushAnalyzerResult(request) })",
      "backendRequestAccess",
    ],
    [
      "renamed imported request client",
      "import { request as api } from '@playwright/test'; await api.newContext()",
      "backendRequest",
    ],
    [
      "destructured request method",
      "const { post } = page.request; await post('/rest/analyzer')",
      "backendRequest",
    ],
    [
      "cast request client",
      "const api = page.request as APIRequestContext; await api.post('/rest/analyzer')",
      "backendRequestAccess",
    ],
    ["browser fetch", "await fetch('/rest/analyzer')", "backendFetch"],
    [
      "evaluate fetch",
      "await page.evaluate(() => fetch('/rest/analyzer'))",
      "backendFetch",
    ],
    ["window fetch", "await window.fetch('/rest/analyzer')", "backendFetch"],
    [
      "response synchronization",
      "await page.waitForResponse('/rest/analyzer')",
      "waitForResponse",
    ],
    [
      "request synchronization",
      "await page.waitForRequest('/rest/analyzer')",
      "waitForRequest",
    ],
    [
      "response event listener",
      "page.on('response', response => inspect(response))",
      "networkListener",
    ],
    [
      "context response event listener",
      "const ctx = page.context(); ctx.on('response', response => inspect(response))",
      "networkListener",
    ],
    [
      "poll synchronization",
      "await expect.poll(async () => getBackendState()).toBe('ready')",
      "backendPoll",
    ],
    [
      "page network stub",
      "await page.route('**/rest/**', route => route.fulfill({ status: 200 }))",
      "networkStub",
    ],
    [
      "context network stub",
      "await context.route('**/rest/**', route => route.continue())",
      "networkStub",
    ],
    [
      "aliased page network stub",
      "const appPage = page; await appPage.route('**/rest/**', route => route.continue())",
      "networkStub",
    ],
    [
      "aliased context network stub",
      "const ctx = page.context(); await ctx.route('**/rest/**', route => route.continue())",
      "networkStub",
    ],
    [
      "assigned page alias network stub",
      "let appPage; appPage = page; await appPage.route('**/rest/**', route => route.continue())",
      "networkStub",
    ],
    [
      "assigned context alias network stub",
      "let ctx; ctx = page.context(); await ctx.route('**/rest/**', route => route.continue())",
      "networkStub",
    ],
    [
      "aliased browser fetch",
      "const backendFetch = fetch; await backendFetch('/rest/analyzer')",
      "backendFetch",
    ],
    [
      "aliased backend poll",
      "const poll = expect.poll; await poll(async () => backendState())",
      "backendPoll",
    ],
    ["arbitrary timeout", "await page.waitForTimeout(1000)", "arbitraryWait"],
    [
      "forced control",
      "await page.getByRole('button').click({ force: true })",
      "forcedAction",
    ],
    [
      "computed local import",
      "await import(`../../../helpers/${helperName}`)",
      "unresolvedLocalImport",
    ],
  ])("rejects %s", (_name, code, messageId) => {
    expect(verify(code)).toEqual(
      expect.arrayContaining([
        expect.objectContaining({
          ruleId: RULE_ID,
          messageId,
        }),
      ]),
    );
  });

  test.each([
    [
      "visible controls and assertions",
      "await page.getByRole('button', { name: 'Save' }).click(); await expect(page.getByText('Saved')).toBeVisible();",
    ],
    [
      "visible navigation and URL assertions",
      "await page.goto('/analyzers'); await expect(page).toHaveURL(/analyzers/);",
    ],
    [
      "unrelated route helper",
      "router.route('/analyzers'); await expect(page.getByText('Analyzers')).toBeVisible();",
    ],
    [
      "unrelated request property",
      "const { request: requestId } = payload; requestId.post('/not-playwright')",
    ],
    ["unrelated context route", "router.context().route('/analyzers')"],
    [
      "same alias name in another scope",
      "function fixture({ request: api }) { return 'unused'; } function domain(api) { api.post('/not-playwright'); }",
    ],
  ])("allows %s", (_name, code) => {
    expect(verify(code)).toEqual([]);
  });
});
