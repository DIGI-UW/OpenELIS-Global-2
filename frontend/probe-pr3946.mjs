import { chromium, request as pwRequest } from "playwright";

const BASE = "https://localhost";
const results = [];
const ok = (n, c, d = "") => results.push([c ? "PASS" : "FAIL", n, d]);

const api = await pwRequest.newContext({ baseURL: BASE, ignoreHTTPSErrors: true });
const login = await api.post("/api/OpenELIS-Global/ValidateLogin?apiCall=true", {
  form: { loginName: "admin", password: "adminADMIN!" },
});
ok("admin login", login.status() === 200, `http ${login.status()}`);
const jsid = login.headersArray().filter(h => h.name.toLowerCase() === "set-cookie")
  .map(h => h.value.match(/JSESSIONID=([^;]+)/)).filter(Boolean)[0][1];

// ── API-level checks ───────────────────────────────────────────────────
const j = async (u) => { const r = await api.get(u); return { s: r.status(), b: await r.json().catch(() => null) }; };

let r = await j("/api/OpenELIS-Global/rest/user-sample-types");
ok("clinical sample types load and do not leak vector rows",
   r.s === 200 && Array.isArray(r.b) && r.b.length > 0 &&
   !r.b.some(t => /mosquito|fly|flea|rodent/i.test(t.value || t.description || "")),
   `${(r.b || []).length} types`);

r = await j("/api/OpenELIS-Global/rest/environmental-sample-types");
ok("environmental sample types seeded", r.s === 200 && (r.b || []).length > 0, `${(r.b || []).length}`);

r = await j("/api/OpenELIS-Global/rest/vector-sample-types");
ok("vector sample types seeded", r.s === 200 && (r.b || []).length > 0, `${(r.b || []).length}`);

r = await j("/api/OpenELIS-Global/rest/test-catalog/tests?page=1&pageSize=5");
ok("test catalogue list API answers", r.s === 200, `http ${r.s}`);

r = await j("/api/OpenELIS-Global/rest/sample-types");
ok("sample type management API answers", r.s === 200, `http ${r.s}`);

r = await j("/api/OpenELIS-Global/rest/test-catalog/panels");
ok("panel list API answers", r.s === 200, `http ${r.s}`);

r = await j("/api/OpenELIS-Global/rest/home-dashboard/metrics");
ok("home dashboard metrics endpoint does not 500", r.s !== 500, `http ${r.s}`);

// ── Browser-level checks ───────────────────────────────────────────────
const browser = await chromium.launch();
const ctx = await browser.newContext({ baseURL: BASE, ignoreHTTPSErrors: true });
await ctx.addCookies([{ name: "JSESSIONID", value: jsid, domain: "localhost", path: "/", httpOnly: true, secure: true, sameSite: "Lax" }]);
const page = await ctx.newPage();
const errors = [];
page.on("pageerror", e => errors.push(String(e).slice(0, 200)));

const go = async (url, ms = 5000) => {
  await page.goto(url, { waitUntil: "domcontentloaded" });
  await page.waitForTimeout(ms);
};
const snap = () => page.evaluate(() => ({
  headings: [...document.querySelectorAll("h1,h2,h3,h4")].map(h => h.textContent.trim()).filter(Boolean).slice(0, 8),
  rows: document.querySelectorAll("tbody tr").length,
  sideNav: [...document.querySelectorAll(".cds--side-nav a, .cds--side-nav__menu-item")]
    .map(a => a.textContent.trim()).filter(Boolean).slice(0, 30),
  body: document.body.innerText.slice(0, 300),
}));

// 1. Unified Result Entry
await go("/Results", 7000);
let s = snap();
console.log("=== /Results ===", JSON.stringify({ headings: s.headings, rows: s.rows }, null, 1));
ok("unified /Results worklist renders", s.headings.length > 0 && !/error/i.test(s.body), s.headings.join(" | "));

// 2. Test Catalogue editor
await go("/MasterListsPage/TestCatalogList", 6000);
s = snap();
ok("Test Catalogue list renders with rows", s.rows > 0, `${s.rows} rows`);
ok("empty-state helper present", s.sideNav.some(t => /Click a test to edit its sections/.test(t)) ||
   (await page.locator("text=Click a test to edit its sections").count()) > 0);
const firstCell = await page.$('[data-cy^="test-row-"] td');
if (firstCell) {
  await firstCell.click(); await page.waitForTimeout(5000);
  ok("clicking a test opens its editor", /TestCatalogEditor\/\d+/.test(page.url()), page.url());
  s = snap();
  ok("editor sections nav present", s.sideNav.some(t => /Basic Info/.test(t)), s.sideNav.filter(t => /Basic|Sample|Range/.test(t)).join(" | "));
}

// 3. Sample Type editor — new + legacy
await go("/MasterListsPage/SampleTypeEditor", 6000);
s = snap();
ok("Sample Type Editor list renders", s.headings.includes("Sample Type Editor") && s.rows > 0,
   `${s.headings[1] || s.headings[0]} rows=${s.rows}`);
const stCell = await page.$('[data-cy^="sampleType-row-"] td');
if (stCell) {
  await stCell.click(); await page.waitForTimeout(4000);
  ok("clicking a sample type opens it", /SampleTypeEditor\/\d+/.test(page.url()), page.url());
}
await go("/MasterListsPage/SampleTypeManagement", 5000);
s = snap();
ok("legacy Manage Sample Types still answers its path",
   !/Sample Type Editor/.test(s.headings.join("|")), s.headings.slice(0, 3).join(" | "));

// 4. Test creation (create-in-place flow)
await go("/MasterListsPage/TestCatalogEditor/new/basic-info", 6000);
s = snap();
ok("New Test create-in-place opens", /new/.test(page.url()) && s.headings.length > 0, s.headings.slice(0, 3).join(" | "));

// 5. Panels
await go("/MasterListsPage/TestCatalogList?entity=panels", 6000);
s = snap();
ok("Panel Editor list renders", s.headings.includes("Panel Editor"), s.headings.join(" | "));
ok("panel rows listed", s.rows > 0, `${s.rows} rows`);
const pCell = await page.$('[data-cy^="panel-row-"] td');
if (pCell) {
  await pCell.click(); await page.waitForTimeout(4000);
  ok("clicking a panel opens its editor", /TestCatalogEditor\/panel\/\d+/.test(page.url()), page.url());
}

// Regression guards: validation page + legacy add order + branch's workflows
await go("/ResultValidation", 6000);
s = snap();
ok("Validation page renders", s.headings.length > 0 && !/error/i.test(s.body), s.headings.slice(0, 2).join(" | "));

await go("/AddOrder", 6000);
s = snap();
ok("clinical Add Order renders", s.headings.length > 0 && !/error/i.test(s.body), s.headings.slice(0, 2).join(" | "));

await go("/order/vector", 6000);
s = snap();
ok("vector order workflow renders (branch feature)", !/error/i.test(s.body), s.headings.slice(0, 2).join(" | "));

console.log("\n=== JS errors ===", errors.length ? errors.slice(0, 6) : "none");
ok("no JS page errors across all pages", errors.length === 0, errors.slice(0, 2).join(" ; "));

console.log("\n=== SUMMARY ===");
for (const [st, n, d] of results) console.log(`${st}  ${n}${d ? "  — " + d : ""}`);
console.log(`\n${results.filter(x => x[0] === "PASS").length}/${results.length} passed`);
await browser.close();
await api.dispose();
