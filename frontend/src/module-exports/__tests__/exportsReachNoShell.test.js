import { describe, expect, it, vi } from "vitest";

/**
 * The core surface a module imports (openelis_core/components, /api, /hooks,
 * /contexts, /sdk) must not pull the shell in behind it. Layout.jsx drags
 * Header, the side navigation, the admin section configs and the locale files
 * into a module's reach, and every component that read a context from it was
 * one more cycle for the bundler to resolve by load order (OGC-1640 step 1).
 * The contexts a module needs are declared in components/layout/contexts.js.
 */
vi.mock("../../components/layout/Layout", () => {
  throw new Error(
    "components/layout/Layout was imported by the module surface",
  );
});

const entryPoints = {
  components: () => import("../components"),
  api: () => import("../api"),
  hooks: () => import("../hooks"),
  contexts: () => import("../contexts"),
  sdk: () => import("../sdk"),
};

describe("module-exports", () => {
  it.each(Object.keys(entryPoints))(
    "openelis_core/%s loads without reaching Layout.jsx",
    async (entry) => {
      await expect(entryPoints[entry]()).resolves.toBeTruthy();
    },
  );

  it("exports the contexts a module reads from the contexts module itself", async () => {
    const exported = await import("../contexts");
    const declared = await import("../../components/layout/contexts");
    expect(exported.ConfigurationContext).toBe(declared.ConfigurationContext);
    expect(exported.NotificationContext).toBe(declared.NotificationContext);
  });
});
