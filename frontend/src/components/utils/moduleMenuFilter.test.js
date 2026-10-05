import { describe, expect, it } from "vitest";
import {
  MODULE_GUARDED_PATHS,
  ROUTE_GUARDS,
  menuEntryVisible,
  moduleEntryVisible,
} from "./Utils";

/**
 * The sidebar has to answer to BOTH authorization layers.
 *
 * SecureRoute only knows role/privilege/permission guards, so the menu filter
 * built on it still offered pages that ModuleAuthenticationInterceptor refuses:
 * that layer checks the URL against system_module_url before method security
 * runs, and its denial never reaches the frontend as an error it can see. It is
 * either a full-page redirect to /Home?access=denied or a page that renders and
 * then 403s its own data calls.
 *
 * Verified live against the dev stack before this fix: the Reception session was
 * shown /CytologyDashboard, /ResultValidationRetroC, /WorkPlanByTest* and
 * /ReportPrint, none of which it can open.
 */
describe("moduleEntryVisible mirrors ModuleAuthenticationInterceptor", () => {
  const withModules = (modules, roles = ["Reception"]) => ({
    modules,
    roles,
    privileges: [],
  });

  it("admits a path the caller holds a module for", () => {
    // The interceptor permits on ANY one match, not all.
    const path = "/CytologyDashboard";
    const required = MODULE_GUARDED_PATHS[path];
    expect(required, `${path} must be in the module map`).toBeDefined();
    expect(moduleEntryVisible(path, withModules([required[0]]))).toBe(true);
  });

  it("refuses a path the caller holds no module for", () => {
    expect(
      moduleEntryVisible(
        "/CytologyDashboard",
        withModules(["SomeOtherModule"]),
      ),
    ).toBe(false);
  });

  it("admits any path with no module mapping", () => {
    // Absent from system_module_url means the interceptor auto-allows it, so
    // hiding it would remove a working page.
    expect(moduleEntryVisible("/NotMappedAnywhere", withModules([]))).toBe(
      true,
    );
  });

  it("admits Global Administrator regardless of modules", () => {
    // isUserAdmin() bypasses the interceptor entirely.
    expect(
      moduleEntryVisible(
        "/CytologyDashboard",
        withModules([], ["Global Administrator"]),
      ),
    ).toBe(true);
  });

  it("stays visible when the session carries no modules field", () => {
    // An older session shape must not blank the whole menu.
    expect(moduleEntryVisible("/CytologyDashboard", { roles: [] })).toBe(true);
  });
});

describe("menuEntryVisible consults both layers", () => {
  /**
   * The paths that regressed. Each has NO SecureRoute guard, so the
   * guard-based filter returned true for everyone; only the module layer
   * refuses them.
   */
  const MODULE_ONLY_PATHS = [
    "/CytologyDashboard",
    "/ResultValidationRetroC",
    "/WorkPlanByTest",
    "/ReportPrint",
  ];

  it("the regressed paths are module-gated and carry no route guard", () => {
    MODULE_ONLY_PATHS.forEach((path) => {
      expect(
        ROUTE_GUARDS[path],
        `${path} is expected to have no SecureRoute guard; if one was added, this test should move to the guard suite`,
      ).toBeUndefined();
      expect(
        MODULE_GUARDED_PATHS[path],
        `${path} must be module-gated or the filter cannot hide it`,
      ).toBeDefined();
    });
  });

  it("hides them from a session without the module", () => {
    const reception = {
      roles: ["Reception"],
      privileges: ["order:create"],
      modules: ["SamplePatientEntry"],
    };
    MODULE_ONLY_PATHS.forEach((path) => {
      expect(
        menuEntryVisible(path, reception),
        `${path} must not be offered to a session lacking its module`,
      ).toBe(false);
    });
  });

  it("shows them once the module is held", () => {
    MODULE_ONLY_PATHS.forEach((path) => {
      const session = {
        roles: ["Results"],
        privileges: [],
        modules: MODULE_GUARDED_PATHS[path],
      };
      expect(
        menuEntryVisible(path, session),
        `${path} must be offered to a session holding its module`,
      ).toBe(true);
    });
  });

  it("still applies route guards where a route has one", () => {
    // The module check must not become a bypass: a guarded route is decided by
    // its guard, whatever modules the caller holds.
    const everyModule = {
      roles: ["Reception"],
      privileges: ["order:create"],
      modules: Object.values(MODULE_GUARDED_PATHS).flat(),
    };
    expect(menuEntryVisible("/AccessionValidation", everyModule)).toBe(false);
  });
});
