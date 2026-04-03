import { Page, expect } from "@playwright/test";

const BASE_URL = process.env.BASE_URL || "https://localhost";

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
  year: number = 2026,
): Promise<Holiday[]> {
  const created: Holiday[] = [];

  for (const h of SAMPLE_HOLIDAYS) {
    const response = await page.request.post(
      `${BASE_URL}/rest/calendar/holidays`,
      {
        data: { date: h.date, name: h.name, isRecurring: h.isRecurring },
      },
    );

    if (response.ok()) {
      const ct = response.headers()["content-type"] || "";
      if (ct.includes("application/json")) {
        const body = await response.json();
        created.push({ id: body.id, ...h });
      }
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
  const response = await page.request.get(
    `${BASE_URL}/rest/calendar/holidays?year=${year}&includeInactive=true`,
  );

  if (!response.ok()) return;

  // Guard against HTML login page redirect (302 → 200 with HTML body)
  const contentType = response.headers()["content-type"] || "";
  if (!contentType.includes("application/json")) return;

  const body = await response.json();
  const holidays = body.holidays || [];

  for (const h of holidays) {
    await page.request.delete(`${BASE_URL}/rest/calendar/holidays/${h.id}`);
  }
}
