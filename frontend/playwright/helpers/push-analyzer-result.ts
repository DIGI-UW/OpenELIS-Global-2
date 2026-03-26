/**
 * Protocol dispatcher for pushing analyzer results during E2E demos.
 *
 * Routes to the correct push mechanism based on protocol:
 * - ASTM: POST to simulator /simulate/astm/{template}
 * - HL7:  POST to simulator /simulate/hl7/{template}
 * - FILE: Copy fixture file to import directory
 */

import { expect, Page } from "@playwright/test";
import * as fs from "fs";
import * as path from "path";
import type { DemoPresentation } from "./demo-presentation";
import type { PushConfig } from "./analyzer-test-config";

const E2E_FIXTURES_DIR =
  process.env.E2E_FIXTURES_DIR ||
  path.resolve(__dirname, "../../projects/analyzer-harness/e2e-fixtures");

export async function pushAnalyzerResult(
  page: Page,
  push: PushConfig,
  presentation: DemoPresentation,
) {
  switch (push.protocol) {
    case "ASTM": {
      const response = await page.request.post(
        `${push.simulatorUrl}/simulate/astm/${push.template}`,
        { data: { destination: push.destination, count: 1 } },
      );
      expect(response.ok()).toBeTruthy();
      await presentation.pause(1_000);
      break;
    }

    case "HL7": {
      const response = await page.request.post(
        `${push.simulatorUrl}/simulate/hl7/${push.template}`,
        { data: { destination: push.destination, count: 1 } },
      );
      expect(response.ok()).toBeTruthy();
      await presentation.pause(1_000);
      break;
    }

    case "FILE": {
      const fixturePath = path.resolve(E2E_FIXTURES_DIR, push.fixtureFile);
      if (!fs.existsSync(fixturePath)) {
        throw new Error(
          `Fixture file not found: ${fixturePath} (set E2E_FIXTURES_DIR if non-default)`,
        );
      }

      fs.mkdirSync(push.importDir, { recursive: true });
      const ext = path.extname(push.fixtureFile);
      const destFile = path.join(
        push.importDir,
        `${push.filePrefix}${Date.now()}${ext}`,
      );

      // Append unique suffix so the file watcher treats it as new
      const original = fs.readFileSync(fixturePath);
      const uniqueSuffix = Buffer.from(`\n<!-- e2e-${Date.now()} -->`);
      fs.writeFileSync(destFile, Buffer.concat([original, uniqueSuffix]));

      await presentation.pause(2_000); // Wait for file watcher to pick up
      break;
    }
  }
}
