import { Page, TestInfo } from "@playwright/test";

/**
 * Returns true when the current test is recording video (demo-video project).
 * Used to gate video-only behaviors: pacing pauses, title cards, step banners.
 */
export function isVideoProject(testInfo: TestInfo): boolean {
  return testInfo.project.name === "demo-video";
}

/**
 * Pause only during video recording. No-op in demo/harness/core-app projects.
 * Use this instead of page.waitForTimeout() for video pacing between actions.
 * This helper is presentation-only and must not be used as a readiness signal.
 */
export async function videoPause(
  page: Page,
  ms: number,
  testInfo: TestInfo,
): Promise<void> {
  if (isVideoProject(testInfo)) {
    await page.waitForTimeout(ms);
  }
}
