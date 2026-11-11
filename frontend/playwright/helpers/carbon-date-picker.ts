import { Page, expect } from "@playwright/test";

/**
 * Choose a day on the flatpickr calendar a Carbon DatePicker has open.
 *
 * The EQA date inputs spread `calendarOnlyInput`, which drops every
 * printable keystroke and paste, so `fill()` leaves the picker's state empty
 * even though the text lands in the box. Click the input first to open the
 * calendar, then call this once per date; a range picker stays open after
 * the first day. `monthsAhead` pages the calendar forward before choosing,
 * which is how a spec asks for a date that is always in the future.
 */
export async function pickCalendarDay(
  page: Page,
  day: number,
  monthsAhead = 0,
): Promise<void> {
  const calendar = page.locator(".flatpickr-calendar.open");
  await expect(calendar).toBeVisible();
  for (let i = 0; i < monthsAhead; i++) {
    await calendar.locator(".flatpickr-next-month").click();
  }
  // Leading and trailing cells belong to the neighbouring months and carry
  // the same numbers, so only the month on display is eligible.
  await calendar
    .locator(".flatpickr-day:not(.prevMonthDay):not(.nextMonthDay)")
    .filter({ hasText: new RegExp(`^${day}$`) })
    .click();
}
