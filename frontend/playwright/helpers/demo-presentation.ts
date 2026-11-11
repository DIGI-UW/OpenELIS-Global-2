import * as fs from "fs";
import * as path from "path";
import { Locator, Page, TestInfo } from "@playwright/test";
import {
  showCaption,
  showHighlight,
  showSceneLabel,
  showStepCard,
  showTitleCard,
} from "./title-card";
import { isVideoProject, videoPause } from "./video-pause";

/** Directory where loose screenshot evidence files are saved (video mode). */
const EVIDENCE_DIR = new URL("../../e2e-evidence", import.meta.url).pathname;

export type DemoPresentation = {
  readonly isVideo: boolean;
  chapter: (options: {
    eyebrow: string;
    title: string;
    subtitle?: string;
    accent?: string;
    durationMs?: number;
  }) => Promise<void>;
  title: (
    title: string,
    subtitle?: string,
    durationMs?: number,
  ) => Promise<void>;
  step: (
    stepNumber: number,
    description: string,
    durationMs?: number,
  ) => Promise<void>;
  scene: (label: string) => Promise<void>;
  /**
   * Opens a story: its card stays up while off-screen preparation runs, under
   * the captions, until the first page load.
   */
  intro: (title: string, subtitle: string) => Promise<void>;
  /** Narrates what happens next; the line stays, across page loads, until replaced. */
  caption: (text: string | null) => Promise<void>;
  /** Draws the eye to the element that shows the outcome. */
  highlight: (locator: Locator, durationMs?: number) => Promise<void>;
  /** Closes a story on the outcome its assertions just read back. */
  verified: (title: string, subtitle?: string) => Promise<void>;
  pause: (ms: number) => Promise<void>;
  evidence: (
    name: string,
    options?: { fullPage?: boolean; locator?: Locator },
  ) => Promise<void>;
};

export function createDemoPresentation(
  page: Page,
  testInfo: TestInfo,
): DemoPresentation {
  const isVideo = isVideoProject(testInfo);
  let caption: string | null = null;
  let repaintsOnLoad = false;

  return {
    isVideo,
    chapter: ({
      eyebrow,
      title,
      subtitle,
      accent = "#0f62fe",
      durationMs = 3500,
    }) =>
      showTitleCard(page, title, subtitle, durationMs, testInfo, {
        eyebrow,
        accent,
      }),
    title: (title, subtitle, durationMs = 4500) =>
      showTitleCard(page, title, subtitle, durationMs, testInfo),
    step: (stepNumber, description, durationMs = 3000) =>
      showStepCard(page, stepNumber, description, durationMs, testInfo),
    scene: (label) => showSceneLabel(page, label, testInfo),
    intro: (title, subtitle) =>
      showTitleCard(page, title, subtitle, 3000, testInfo, { hold: true }),
    caption: async (text) => {
      if (!isVideo) return;
      caption = text;
      if (!repaintsOnLoad) {
        repaintsOnLoad = true;
        page.on("domcontentloaded", () => {
          // A load that starts while this paints destroys the context; the next load repaints.
          showCaption(page, caption, testInfo).catch(() => undefined);
        });
      }
      await showCaption(page, text, testInfo);
      // Long enough to read before the screen moves on.
      if (text)
        await videoPause(
          page,
          Math.min(3500, 1200 + 30 * text.length),
          testInfo,
        );
    },
    highlight: (locator, durationMs = 2500) =>
      showHighlight(locator, durationMs, testInfo),
    verified: async (title, subtitle) => {
      if (!isVideo) return;
      caption = null;
      await showCaption(page, null, testInfo);
      await showTitleCard(page, title, subtitle, 4500, testInfo, {
        eyebrow: "Verified",
        accent: "#24a148",
      });
    },
    pause: (ms) => videoPause(page, ms, testInfo),
    evidence: async (
      name: string,
      options: { fullPage?: boolean; locator?: Locator } = {},
    ) => {
      if (!isVideo) return;
      const screenshot = options.locator
        ? await options.locator.screenshot()
        : await page.screenshot({ fullPage: options.fullPage ?? false });
      // Attach to HTML report
      await testInfo.attach(name, {
        body: screenshot,
        contentType: "image/png",
      });
      // Also save as loose file for direct viewing
      fs.mkdirSync(EVIDENCE_DIR, { recursive: true });
      const safeName = name.replace(/[^a-zA-Z0-9._-]/g, "-");
      fs.writeFileSync(path.join(EVIDENCE_DIR, `${safeName}.png`), screenshot);
    },
  };
}
