import { test, expect } from "../../../helpers/test-base";

/**
 * OGC-1198 — behind a reverse proxy on a non-standard port, the login
 * redirect and the FHIR links dropped the port the browser used, sending it
 * to port 443. The webapp now takes the port, host and protocol a proxy
 * reports in X-Forwarded-* or Forwarded.
 *
 * Run with:
 *   cd frontend && npm run pw:test:core-foundational -- ogc-1198-forwarded-port
 */

const API = "/api/OpenELIS-Global";
const FORWARDED_PORT = {
  "X-Forwarded-Proto": "https",
  "X-Forwarded-Port": "8443",
};

test.describe("OGC-1198 forwarded port, logged out", () => {
  test.use({ storageState: { cookies: [], origins: [] } });

  test("the login redirect keeps the port the proxy reports", async ({
    page,
    baseURL,
  }) => {
    const host = new URL(baseURL!).hostname;

    const forwarded = await page.request.get(`${API}/Dashboard`, {
      headers: FORWARDED_PORT,
      maxRedirects: 0,
    });
    expect(forwarded.headers().location).toBe(
      `https://${host}:8443${API}/LoginPage`,
    );

    const direct = await page.request.get(`${API}/Dashboard`, {
      maxRedirects: 0,
    });
    expect(direct.headers().location).toBe(`${baseURL}${API}/LoginPage`);
  });

  test("the logout redirect keeps the port the proxy reports", async ({
    page,
    baseURL,
  }) => {
    const host = new URL(baseURL!).hostname;

    const response = await page.request.post(`${API}/Logout`, {
      headers: FORWARDED_PORT,
      maxRedirects: 0,
    });
    expect(response.headers().location).toBe(
      `https://${host}:8443${API}/LoginPage`,
    );
  });

  test("a forwarded host that is not a host name is ignored", async ({
    page,
    baseURL,
  }) => {
    const response = await page.request.get(`${API}/Dashboard`, {
      headers: { Forwarded: 'proto=https;host="__"' },
      maxRedirects: 0,
    });
    expect(response.headers().location).toBe(`${baseURL}${API}/LoginPage`);
  });
});

test.describe("OGC-1198 forwarded port, logged in", () => {
  test("FHIR links keep the port the proxy reports", async ({
    page,
    baseURL,
  }) => {
    const host = new URL(baseURL!).hostname;

    const bundle = await (
      await page.request.get(`${API}/fhir/Patient?_count=1`, {
        headers: { ...FORWARDED_PORT, Accept: "application/fhir+json" },
      })
    ).json();

    const self = (bundle.link as { relation: string; url: string }[]).find(
      (link) => link.relation === "self",
    );
    expect(self?.url).toMatch(
      new RegExp(`^https://${host}:8443${API}/fhir/Patient\\?`),
    );
  });
});
