import { Page } from "@playwright/test";

/**
 * Injects a full-screen title card overlay into the browser viewport.
 * Since Playwright records the viewport, these appear as title/transition
 * screens in the video with no post-processing needed.
 *
 * Uses Carbon Design System dark theme colors and IBM Plex Sans.
 */
export async function showTitleCard(
  page: Page,
  title: string,
  subtitle?: string,
  durationMs = 3000,
) {
  await page.evaluate(
    ({ title, subtitle }) => {
      const overlay = document.createElement("div");
      overlay.id = "e2e-title-card";
      Object.assign(overlay.style, {
        position: "fixed",
        inset: "0",
        zIndex: "99999",
        display: "flex",
        flexDirection: "column",
        alignItems: "center",
        justifyContent: "center",
        background: "#161616",
        color: "#f4f4f4",
        fontFamily: "'IBM Plex Sans', Arial, sans-serif",
      });
      const h1 = document.createElement("h1");
      h1.textContent = title;
      Object.assign(h1.style, {
        fontSize: "2.5rem",
        fontWeight: "600",
        marginBottom: "0.5rem",
        textAlign: "center",
        padding: "0 2rem",
      });
      overlay.appendChild(h1);
      if (subtitle) {
        const p = document.createElement("p");
        p.textContent = subtitle;
        Object.assign(p.style, {
          fontSize: "1.2rem",
          color: "#a8a8a8",
          textAlign: "center",
          padding: "0 2rem",
        });
        overlay.appendChild(p);
      }
      document.body.appendChild(overlay);
    },
    { title, subtitle },
  );
  await page.waitForTimeout(durationMs);
  await page.evaluate(() =>
    document.getElementById("e2e-title-card")?.remove(),
  );
}

/**
 * Shows a step transition banner at the top of the screen.
 * Uses Carbon blue (#0f62fe) for visual consistency.
 */
export async function showStepCard(
  page: Page,
  stepNumber: number,
  description: string,
  durationMs = 2000,
) {
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
  await page.waitForTimeout(durationMs);
  await page.evaluate(() => document.getElementById("e2e-step-card")?.remove());
}
