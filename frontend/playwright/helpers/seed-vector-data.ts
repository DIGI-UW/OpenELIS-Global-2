import { Page } from "@playwright/test";

/** API context path — the JSESSIONID is scoped to the /api/OpenELIS-Global webapp. */
const API_PREFIX = "/api/OpenELIS-Global";

/** CSRF token saved by auth.setup into storageState localStorage (key "CSRF"). */
async function getCsrfToken(page: Page): Promise<string> {
  const state = await page.context().storageState();
  for (const origin of state.origins) {
    for (const item of origin.localStorage) {
      if (item.name === "CSRF") return item.value;
    }
  }
  return "";
}

export interface VectorSite {
  id?: number;
  code: string;
  name: string;
}

/**
 * Create a vector sampling site via the admin REST API (POST
 * /rest/admin/vector/sampling-sites) so an add-order E2E has a site to select.
 * Reference/transactional test data is seeded via the API, not SQL fixtures.
 * The caller should pass a unique code per run to avoid collisions.
 */
export async function seedVectorSite(
  page: Page,
  site: VectorSite,
): Promise<VectorSite> {
  const csrfToken = await getCsrfToken(page);
  const response = await page.request.post(
    `${API_PREFIX}/rest/admin/vector/sampling-sites`,
    {
      data: { code: site.code, name: site.name },
      headers: { "X-CSRF-Token": csrfToken },
    },
  );
  if (!response.ok()) {
    const text = await response.text().catch(() => "");
    throw new Error(
      `seedVectorSite: POST ${site.code} → HTTP ${response.status()} ${text.slice(0, 200)}`,
    );
  }
  const body = await response.json();
  return { id: body.id, code: site.code, name: site.name };
}
