import fs from "fs";
import path from "path";
import { describe, expect, it } from "vitest";
import {
  ROUTE_GUARDS,
  Privileges,
  Roles,
  menuEntryVisible,
  menuSubtreeVisible,
} from "./Utils";

/**
 * ROUTE_GUARDS duplicates the guard props in App.jsx, and the sidebar hides
 * menu rows based on it. If the two drift, the menu goes back to promising
 * links that SecureRoute refuses (the bug this fixed: 74 dead rows for
 * Reception, 88 for Results and Validation, 55 for Reports, out of 153) or,
 * worse, starts hiding rows that actually work.
 *
 * Parsing App.jsx rather than hand-listing the routes is the point: the test
 * fails the moment a guard is added, removed or repointed without updating the
 * map. Both guard shapes are read, because 31 routes are still guarded by
 * `role=` rather than `privilege=`.
 */
const readGuardsFromApp = () => {
  const source = fs.readFileSync(
    path.join(__dirname, "..", "..", "App.jsx"),
    "utf8",
  );
  const guards = {};
  const starts = [...source.matchAll(/<SecureRoute\b/g)].map((m) => m.index);
  starts.forEach((start, i) => {
    const end = i + 1 < starts.length ? starts[i + 1] : start + 2500;
    const block = source.slice(start, end);
    const routePath = block.match(/path="([^"]+)"/);
    if (!routePath || routePath[1] in guards) {
      return;
    }
    const privilege = block.match(/privilege=\{Privileges\.(\w+)\}/);
    const role = block.match(/role=\{([^}]*)\}/);
    if (!privilege && !role) {
      return;
    }
    const guard = {};
    if (privilege) {
      // A guard naming a constant that does not exist grants to nobody, since
      // `undefined` matches no privilege in the session.
      expect(
        Privileges[privilege[1]],
        `App.jsx guards ${routePath[1]} with Privileges.${privilege[1]}, which is not declared`,
      ).toBeDefined();
      guard.privilege = Privileges[privilege[1]];
    }
    if (role) {
      const names = [...role[1].matchAll(/Roles\.(\w+)/g)].map((m) => m[1]);
      if (names.length) {
        guard.role = names.map((name) => {
          expect(
            Roles[name],
            `App.jsx guards ${routePath[1]} with Roles.${name}, which is not declared`,
          ).toBeDefined();
          return Roles[name];
        });
      } else if (/ANALYZER_RESULTS_ROLES/.test(role[1])) {
        // App.jsx names a shared constant here. ROUTE_GUARDS inlines its value
        // (importing it would be a cycle), so resolve it the same way and let
        // the equality check below catch any divergence.
        guard.role = [Roles.GLOBAL_ADMIN, Roles.ANALYSER_IMPORT];
      } else {
        // Some other shared constant: compared by presence only, below.
        guard.role = "SHARED_CONSTANT";
      }
    }
    guards[routePath[1]] = guard;
  });
  return guards;
};

describe("ROUTE_GUARDS stays in step with App.jsx", () => {
  it("covers exactly the routes App.jsx guards", () => {
    const fromApp = readGuardsFromApp();

    // Inversion: if the parser silently matched nothing, every assertion below
    // would pass against an empty object and the map could rot unnoticed.
    expect(Object.keys(fromApp).length).toBeGreaterThan(90);

    const expected = { ...fromApp };
    const actual = { ...ROUTE_GUARDS };

    // Routes whose role= is a shared constant are checked for presence only.
    Object.entries(fromApp)
      .filter(([, guard]) => guard.role === "SHARED_CONSTANT")
      .forEach(([routePath]) => {
        expect(
          actual[routePath],
          `${routePath} is guarded in App.jsx but missing from ROUTE_GUARDS`,
        ).toBeDefined();
        delete expected[routePath];
        delete actual[routePath];
      });

    expect(actual).toEqual(expected);
  });

  it("includes the role-guarded routes, not just the privilege-guarded ones", () => {
    // /inventory is guarded by role={[Roles.RESULTS, Roles.GLOBAL_ADMIN]}. An
    // earlier version of the map read only `privilege=`, so this route was
    // absent and the menu kept offering a page that 500s for most roles.
    expect(ROUTE_GUARDS["/inventory"]).toBeDefined();
    expect(ROUTE_GUARDS["/inventory"].role).toContain(Roles.RESULTS);
  });
});

describe("menuEntryVisible mirrors SecureRoute", () => {
  const reception = {
    privileges: ["order:create", "order:view", "patient:view"],
    roles: ["Reception"],
  };

  it("shows an unguarded route to any authenticated user", () => {
    // Not in the map at all: SecureRoute would not refuse it either.
    expect(menuEntryVisible("/SomeUnguardedPage", reception)).toBe(true);
    expect(menuEntryVisible("", reception)).toBe(true);
    expect(menuEntryVisible(undefined, reception)).toBe(true);
  });

  it("hides a guarded route whose privilege the user lacks", () => {
    // /AccessionValidation is guarded on result:validate, which Reception has
    // no business holding; this row was one of its 74 dead menu entries.
    expect(menuEntryVisible("/AccessionValidation", reception)).toBe(false);
    expect(menuEntryVisible("/AuditTrailReport?type=system", reception)).toBe(
      false,
    );
  });

  it("shows a guarded route whose privilege the user holds", () => {
    const validator = {
      privileges: ["result:validate"],
      roles: ["Validation"],
    };
    expect(menuEntryVisible("/AccessionValidation", validator)).toBe(true);
  });

  it("satisfies a role guard through the privilege it maps to", () => {
    // /inventory is role-guarded on Results, which RoleEquivalentPrivileges
    // maps to result:enter. A user holding that privilege under a different
    // role name (Lab Technician inherits it) must still see it.
    const labTech = { privileges: ["result:enter"], roles: ["Lab Technician"] };
    expect(menuEntryVisible("/inventory", labTech)).toBe(true);
    expect(menuEntryVisible("/inventory", reception)).toBe(false);
  });

  it("ignores the query string a menu row carries but a route path does not", () => {
    const withQuery = menuEntryVisible("/AuditTrailReport?type=order", {
      privileges: ["system:configure"],
    });
    const withoutQuery = menuEntryVisible("/AuditTrailReport", {
      privileges: ["system:configure"],
    });
    expect(withQuery).toBe(withoutQuery);
    expect(withQuery).toBe(true);
  });

  it("matches a parameterised route by the part before the parameter", () => {
    // App.jsx guards "/PathologyCaseView/:pathologySampleId"; a case link
    // carries a real id.
    const pathologist = { privileges: ["result:pathology-sign-off"] };
    expect(menuEntryVisible("/PathologyCaseView/42", pathologist)).toBe(true);
    expect(menuEntryVisible("/PathologyCaseView/42", reception)).toBe(false);
  });
});

describe("menuSubtreeVisible keeps sections with reachable contents", () => {
  const leaf = (url) => ({
    menu: { isActive: true, actionURL: url, elementId: url },
    childMenus: [],
  });
  const parent = (url, children) => ({
    menu: { isActive: true, actionURL: url, elementId: url },
    childMenus: children,
  });
  const reception = { privileges: ["order:create"], roles: ["Reception"] };

  it("hides a section whose every descendant is out of reach", () => {
    const section = parent("", [
      parent("", [leaf("/AccessionValidation"), leaf("/AuditTrailReport")]),
    ]);
    expect(menuSubtreeVisible(section, reception)).toBe(false);
  });

  it("keeps a section when something three levels down is reachable", () => {
    const section = parent("", [
      parent("", [leaf("/AccessionValidation"), leaf("/SomeOpenPage")]),
    ]);
    expect(menuSubtreeVisible(section, reception)).toBe(true);
  });

  it("hides an inactive row regardless of privilege", () => {
    const inactive = {
      menu: { isActive: false, actionURL: "/SomeOpenPage" },
      childMenus: [],
    };
    expect(menuSubtreeVisible(inactive, reception)).toBe(false);
  });
});

/**
 * The point of the filter, stated per role: each of the four seeded workbench
 * roles keeps every entry point it needs, and sees none of another role's. A
 * filter that hid too much would be a worse bug than the dead links it
 * replaced, and these are the exact paths the live walkthroughs exercised.
 *
 * Privilege sets are the seeded ones as of 012-004p; if a grant changes, update
 * them here rather than loosening the assertions.
 */
describe("the four workbench roles keep their own menus", () => {
  const SEEDED = {
    Reception: [
      "order:create",
      "order:view",
      "order:edit",
      "patient:view",
      "patient:create",
      "patient:edit",
      "provider:view",
      "organization:view",
      "sample_requester:view",
      "nce:view",
      "nce:create",
      "nce:edit",
      "shipment:view",
      "storage:view",
      "user_role:view",
      "alert:view",
      "catalogue:view",
    ],
    Results: [
      "alert:view",
      "catalogue:view",
      "esig:use",
      "inventory:view",
      "micro:bench",
      "micro:view",
      "nce:view",
      "order:view",
      "organization:view",
      "patient:view",
      "referral:view",
      "result:enter",
      "result:modify",
      "result:view",
      "storage:view",
      "user_role:view",
    ],
    Validation: [
      "alert:view",
      "catalogue:view",
      "esig:use",
      "inventory:view",
      "micro:bench",
      "micro:supervise",
      "micro:view",
      "nce:edit",
      "nce:view",
      "order:view",
      "organization:view",
      "patient:view",
      "referral:view",
      "result:validate",
      "result:view",
      "user_role:view",
    ],
    Reports: [
      "alert:view",
      "analyte:view",
      "catalogue:view",
      "coldstorage:view",
      "inventory:view",
      "nce:view",
      "order:view",
      "organization:view",
      "patient:view",
      "provider:view",
      "referral:view",
      "report:export",
      "report:run",
      "result:view",
      "sample_requester:view",
      "sample_status:view",
      "site_info:view",
      "user_role:view",
    ],
  };
  const session = (role) => ({ privileges: SEEDED[role], roles: [role] });

  const ESSENTIALS = {
    Reception: ["/order/clinical", "/SamplePatientEntry", "/PatientManagement"],
    Results: ["/LogbookResults", "/AccessionResults", "/PatientResults"],
    Validation: [
      "/ResultValidation",
      "/AccessionValidation",
      "/AccessionValidationRange",
    ],
    Reports: ["/Report", "/CustomDataExport"],
  };

  Object.entries(ESSENTIALS).forEach(([role, paths]) => {
    it(`${role} still sees its own workbench`, () => {
      paths.forEach((routePath) =>
        expect(
          menuEntryVisible(routePath, session(role)),
          `${role} must still see ${routePath}`,
        ).toBe(true),
      );
    });
  });

  it("and no role sees another's", () => {
    // Each of these was a dead menu row before the filter existed.
    expect(menuEntryVisible("/AccessionValidation", session("Reception"))).toBe(
      false,
    );
    expect(menuEntryVisible("/AccessionResults", session("Validation"))).toBe(
      false,
    );
    expect(menuEntryVisible("/AuditTrailReport", session("Reports"))).toBe(
      false,
    );
  });
});
