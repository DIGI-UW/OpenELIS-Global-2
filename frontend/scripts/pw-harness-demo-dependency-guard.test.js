import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import { afterEach, describe, expect, test } from "vitest";
import { findHarnessDemoDependencyViolations } from "./pw-harness-demo-dependency-guard.mjs";

const temporaryRoots = [];

function createFrontend(files) {
  const frontendRoot = fs.mkdtempSync(
    path.join(os.tmpdir(), "pw-harness-demo-guard-"),
  );
  temporaryRoots.push(frontendRoot);

  for (const [relativePath, contents] of Object.entries(files)) {
    const destination = path.join(frontendRoot, relativePath);
    fs.mkdirSync(path.dirname(destination), { recursive: true });
    fs.writeFileSync(destination, contents);
  }

  return frontendRoot;
}

afterEach(() => {
  for (const root of temporaryRoots.splice(0)) {
    fs.rmSync(root, { recursive: true, force: true });
  }
});

describe("harness demo dependency guard", () => {
  test("rejects backend access hidden in an imported helper", () => {
    const frontendRoot = createFrontend({
      "playwright/tests/demo/harness/story.spec.ts":
        "import { push } from '../../../helpers/push'; await push(page);",
      "playwright/helpers/push.ts":
        "export async function push(page) { await page.request.post('/simulate'); }",
    });

    expect(findHarnessDemoDependencyViolations({ frontendRoot })).toEqual([
      expect.objectContaining({
        dependencyPath: "playwright/helpers/push.ts",
        messageId: "backendRequest",
        specPath: "playwright/tests/demo/harness/story.spec.ts",
      }),
    ]);
  });

  test("follows transitive local imports", () => {
    const frontendRoot = createFrontend({
      "playwright/tests/demo/harness/story.spec.ts":
        "import { drive } from '../../../helpers/drive'; await drive(page);",
      "playwright/helpers/drive.ts":
        "export { poll } from './poll'; export async function drive(page) { await poll(page); }",
      "playwright/helpers/poll.ts":
        "export async function poll(page) { await page.waitForResponse('/rest/state'); }",
    });

    expect(findHarnessDemoDependencyViolations({ frontendRoot })).toEqual([
      expect.objectContaining({
        dependencyPath: "playwright/helpers/poll.ts",
        messageId: "waitForResponse",
      }),
    ]);
  });

  test("follows dynamic local imports", () => {
    const frontendRoot = createFrontend({
      "playwright/tests/demo/harness/story.spec.ts":
        "const { push } = await import('../../../helpers/push'); await push(page);",
      "playwright/helpers/push.ts":
        "export async function push(page) { await page.request.post('/simulate'); }",
    });

    expect(findHarnessDemoDependencyViolations({ frontendRoot })).toEqual([
      expect.objectContaining({
        dependencyPath: "playwright/helpers/push.ts",
        messageId: "backendRequest",
      }),
    ]);
  });

  test("allows visible UI helpers and ignores type-only imports", () => {
    const frontendRoot = createFrontend({
      "playwright/tests/demo/harness/story.spec.ts": [
        "import type { BackendFixture } from '../../../helpers/backend-fixture';",
        "import { save } from '../../../helpers/save';",
        "await save(page);",
      ].join("\n"),
      "playwright/helpers/backend-fixture.ts":
        "export type BackendFixture = { request: unknown }; await fetch('/never-executed');",
      "playwright/helpers/save.ts":
        "export async function save(page) { await page.getByRole('button', { name: 'Save' }).click(); }",
    });

    expect(findHarnessDemoDependencyViolations({ frontendRoot })).toEqual([]);
  });

  test("does not inspect foundational harness scenarios", () => {
    const frontendRoot = createFrontend({
      "playwright/tests/foundational/harness/transport.spec.ts":
        "await page.request.post('/simulate');",
    });

    expect(findHarnessDemoDependencyViolations({ frontendRoot })).toEqual([]);
  });
});
