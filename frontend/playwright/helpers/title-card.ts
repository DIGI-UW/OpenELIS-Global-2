import { Locator, Page, TestInfo } from "@playwright/test";
import { isVideoProject, videoPause } from "./video-pause";

export type TitleCardOptions = {
  eyebrow?: string;
  accent?: string;
  align?: "left" | "center";
  /** Leave the card up after its duration; the next page load replaces it. */
  hold?: boolean;
};

/**
 * Injects a full-screen title card overlay into the browser viewport.
 * Since Playwright records the viewport, these appear as title/transition
 * screens in the video with no post-processing needed.
 *
 * No-op when not recording video (i.e., outside *-demo-video projects, or
 * without the test's TestInfo), as is every helper here.
 * Uses Carbon Design System dark theme colors and IBM Plex Sans.
 * Presentation only: do not use title cards to gate readiness or assertions.
 */
export async function showTitleCard(
  page: Page,
  title: string,
  subtitle?: string,
  durationMs = 3000,
  testInfo?: TestInfo,
  options: TitleCardOptions = {},
) {
  if (!testInfo || !isVideoProject(testInfo)) return;

  await page.evaluate(
    ({ title, subtitle, eyebrow, accent, align }) => {
      document.getElementById("e2e-title-card")?.remove();
      document.getElementById("e2e-scene-label")?.remove();
      const overlay = document.createElement("div");
      overlay.id = "e2e-title-card";
      Object.assign(overlay.style, {
        position: "fixed",
        inset: "0",
        zIndex: "99999",
        display: "flex",
        flexDirection: "column",
        alignItems: align === "center" ? "center" : "flex-start",
        justifyContent: "center",
        background: "#161616",
        color: "#f4f4f4",
        fontFamily: "'IBM Plex Sans', Arial, sans-serif",
        borderLeft: `6px solid ${accent}`,
        boxSizing: "border-box",
        padding: "0 64px",
        pointerEvents: "none",
      });
      if (eyebrow) {
        const eyebrowText = document.createElement("p");
        eyebrowText.textContent = eyebrow;
        Object.assign(eyebrowText.style, {
          fontSize: "0.95rem",
          fontWeight: "600",
          color: accent,
          margin: "0 0 0.75rem",
          textAlign: align,
        });
        overlay.appendChild(eyebrowText);
      }
      const h1 = document.createElement("h1");
      h1.textContent = title;
      Object.assign(h1.style, {
        fontSize: "2.25rem",
        fontWeight: "600",
        lineHeight: "1.2",
        margin: "0",
        maxWidth: "860px",
        textAlign: align,
      });
      overlay.appendChild(h1);
      if (subtitle) {
        const p = document.createElement("p");
        p.textContent = subtitle;
        Object.assign(p.style, {
          fontSize: "1.25rem",
          lineHeight: "1.5",
          color: "#c6c6c6",
          margin: "1rem 0 0",
          maxWidth: "860px",
          textAlign: align,
        });
        overlay.appendChild(p);
      }
      document.body.appendChild(overlay);
    },
    {
      title,
      subtitle,
      eyebrow: options.eyebrow,
      accent: options.accent ?? "#0f62fe",
      align: options.align ?? "left",
    },
  );
  await videoPause(page, durationMs, testInfo);
  if (options.hold) return;
  await page.evaluate(() =>
    document.getElementById("e2e-title-card")?.remove(),
  );
}

/**
 * Shows a narration line at the bottom of the viewport, replacing the previous
 * one; null removes it. Clicks pass through it.
 * No-op when not recording video.
 */
export async function showCaption(
  page: Page,
  text: string | null,
  testInfo?: TestInfo,
) {
  if (!testInfo || !isVideoProject(testInfo)) return;

  await page.evaluate((captionText) => {
    let el = document.getElementById("e2e-caption");
    if (!captionText) {
      el?.remove();
      return;
    }
    if (!el) {
      el = document.createElement("div");
      el.id = "e2e-caption";
      Object.assign(el.style, {
        position: "fixed",
        bottom: "28px",
        left: "50%",
        transform: "translateX(-50%)",
        maxWidth: "72%",
        zIndex: "100000",
        pointerEvents: "none",
        background: "rgba(22,22,22,0.9)",
        color: "#f4f4f4",
        fontFamily: "'IBM Plex Sans', Arial, sans-serif",
        fontSize: "19px",
        lineHeight: "1.4",
        textAlign: "center",
        padding: "10px 22px",
        borderLeft: "4px solid #0f62fe",
        borderRadius: "4px",
        boxShadow: "0 4px 12px rgba(0,0,0,0.35)",
      });
      document.body.appendChild(el);
    }
    el.textContent = captionText;
  }, text);
}

/**
 * Outlines one element on the page, for as long as `durationMs`.
 * No-op when not recording video.
 */
export async function showHighlight(
  locator: Locator,
  durationMs: number,
  testInfo?: TestInfo,
) {
  if (!testInfo || !isVideoProject(testInfo)) return;

  await locator.scrollIntoViewIfNeeded();
  await locator.evaluate((el: HTMLElement) => {
    el.style.outline = "3px solid #f1c21b";
    el.style.outlineOffset = "2px";
  });
  await videoPause(locator.page(), durationMs, testInfo);
  // The element may have left the page meanwhile.
  await locator
    .evaluate((el: HTMLElement) => {
      el.style.outline = "";
      el.style.outlineOffset = "";
    })
    .catch(() => undefined);
}

/**
 * Shows a step transition banner at the top of the screen.
 * No-op when not recording video.
 * Uses Carbon blue (#0f62fe) for visual consistency.
 * Presentation only: do not use step cards as synchronization.
 */
export async function showStepCard(
  page: Page,
  stepNumber: number,
  description: string,
  durationMs = 2000,
  testInfo?: TestInfo,
) {
  if (!testInfo || !isVideoProject(testInfo)) return;

  await page.evaluate(
    ({ stepNumber, description }) => {
      const banner = document.createElement("div");
      banner.id = "e2e-step-card";
      Object.assign(banner.style, {
        position: "fixed",
        top: "0",
        left: "0",
        right: "0",
        zIndex: "99999",
        padding: "1rem 2rem",
        background: "#0f62fe",
        color: "white",
        fontFamily: "'IBM Plex Sans', Arial, sans-serif",
        fontSize: "1.1rem",
        textAlign: "center",
        boxShadow: "0 4px 8px rgba(0,0,0,0.3)",
      });
      banner.textContent = `Step ${stepNumber}: ${description}`;
      document.body.appendChild(banner);
    },
    { stepNumber, description },
  );
  await videoPause(page, durationMs, testInfo);
  await page.evaluate(() => document.getElementById("e2e-step-card")?.remove());
}

/**
 * Shows a compact scene label pinned to the top-left corner.
 * No-op when not recording video.
 * Presentation only: this should never affect business assertions.
 */
export async function showSceneLabel(
  page: Page,
  label: string,
  testInfo?: TestInfo,
) {
  if (!testInfo || !isVideoProject(testInfo)) return;

  await page.evaluate((sceneLabel) => {
    document.getElementById("e2e-scene-label")?.remove();
    const el = document.createElement("div");
    el.id = "e2e-scene-label";
    Object.assign(el.style, {
      position: "fixed",
      top: "12px",
      left: "12px",
      zIndex: "99998",
      background: "rgba(15,98,254,0.92)",
      color: "#ffffff",
      fontFamily: "'IBM Plex Sans', Arial, sans-serif",
      fontSize: "11px",
      fontWeight: "600",
      letterSpacing: "1px",
      textTransform: "uppercase",
      padding: "5px 12px",
      borderRadius: "4px",
    });
    el.textContent = sceneLabel;
    document.body.appendChild(el);
  }, label);
}
