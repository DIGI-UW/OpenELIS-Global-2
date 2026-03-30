import { Page, TestInfo } from "@playwright/test";

const VIDEO_PROJECTS = new Set(["core-demo-video", "harness-demo-video"]);

/**
 * Returns true when the current test is recording video (`core-demo-video`,
 * `harness-demo-video`).
 * Used to gate video-only behaviors: pacing pauses, title cards, step banners.
 */
export function isVideoProject(testInfo: TestInfo): boolean {
  return VIDEO_PROJECTS.has(testInfo.project.name);
}

/**
 * Pause only during video recording. No-op in non-video demo / harness / core-app projects.
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
