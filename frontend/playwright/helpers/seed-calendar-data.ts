import { Page } from "@playwright/test";
import { csrfToken as getCsrfToken } from "./api-session";

/** API context path — must use /api/OpenELIS-Global prefix so the
 *  JSESSIONID (scoped to that webapp context) is recognized by Tomcat. */
const API_PREFIX = "/api/OpenELIS-Global";

interface Holiday {
  id?: number;
  date: string;
  name: string;
  isRecurring: boolean;
}

const SAMPLE_HOLIDAYS: Omit<Holiday, "id">[] = [
  { date: "2026-01-01", name: "New Year's Day", isRecurring: true },
  { date: "2026-04-07", name: "Genocide Memorial Day", isRecurring: true },
  { date: "2026-05-01", name: "Labour Day", isRecurring: true },
  { date: "2026-12-25", name: "Christmas Day", isRecurring: true },
  { date: "2026-03-31", name: "Eid al-Fitr (2026)", isRecurring: false },
];

/**
 * Seed sample holidays via the Calendar Management REST API.
 * Skips holidays that already exist (409 Conflict).
 */
export async function seedHolidays(
  page: Page,
  _year: number = 2026,
): Promise<Holiday[]> {
  const created: Holiday[] = [];
  const csrfToken = await getCsrfToken(page);

  for (const h of SAMPLE_HOLIDAYS) {
    const response = await page.request.post(
      `${API_PREFIX}/rest/calendar/holidays`,
      {
        data: { date: h.date, name: h.name, isRecurring: h.isRecurring },
        headers: { "X-CSRF-Token": csrfToken },
      },
    );

    if (response.ok()) {
      const ct = response.headers()["content-type"] || "";
      if (ct.includes("application/json")) {
        const body = await response.json();
        created.push({ id: body.id, ...h });
      }
    } else if (response.status() !== 409) {
      // Log unexpected failures (not duplicates)
      const text = await response.text().catch(() => "");
      console.warn(
        `seedHolidays: POST ${h.name} → HTTP ${response.status()} ${text.slice(0, 200)}`,
      );
    }
    // 409 = duplicate — skip silently (idempotent)
  }

  return created;
}

/**
 * Clean up all holidays for a given year via API.
 * Fetches the list then deletes each one.
 */
export async function cleanupHolidays(
  page: Page,
  year: number = 2026,
): Promise<void> {
  const csrfToken = await getCsrfToken(page);
  const response = await page.request.get(
    `${API_PREFIX}/rest/calendar/holidays?year=${year}&includeInactive=true`,
    { headers: { "X-CSRF-Token": csrfToken } },
  );

  if (!response.ok()) return;

  // Guard against HTML login page redirect (302 → 200 with HTML body)
  const contentType = response.headers()["content-type"] || "";
  if (!contentType.includes("application/json")) return;

  const body = await response.json();
  const holidays = body.holidays || [];

  for (const h of holidays) {
    await page.request.delete(`${API_PREFIX}/rest/calendar/holidays/${h.id}`, {
      headers: { "X-CSRF-Token": csrfToken },
    });
  }
}
