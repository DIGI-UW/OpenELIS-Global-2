import fs from "fs";
import path from "path";
import { describe, expect, it } from "vitest";
import {
  ROUTE_PRIVILEGES,
  Privileges,
  menuEntryVisible,
  menuSubtreeVisible,
} from "./Utils";

/**
 * ROUTE_PRIVILEGES duplicates the `privilege=` props in App.jsx, and the
 * sidebar hides menu rows based on it. If the two drift, the menu goes back to
 * promising links that SecureRoute refuses (the bug this fixed: 74 dead rows
 * for Reception, 88 for Results and Validation, 55 for Reports, out of 153) or
 *, worse, starts hiding rows that actually work.
 *
 * Parsing App.jsx rather than hand-listing the routes is the point: the test
 * fails the moment a guard is added, removed or repointed without updating the
 * map.
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
    const privilege = block.match(/privilege=\{Privileges\.(\w+)\}/);
    if (routePath && privilege && !(routePath[1] in guards)) {
      guards[routePath[1]] = privilege[1];
    }
  });
  return guards;
};

describe("ROUTE_PRIVILEGES stays in step with App.jsx", () => {
  it("covers exactly the routes App.jsx guards with a privilege", () => {
    const fromApp = readGuardsFromApp();

    // Inversion: if the parser silently matched nothing, every assertion below
    // would pass against an empty object and the map could rot unnoticed.
    expect(Object.keys(fromApp).length).toBeGreaterThan(50);

    const expected = {};
    Object.entries(fromApp).forEach(([routePath, constantName]) => {
      expected[routePath] = Privileges[constantName];
      // A guard naming a constant that does not exist grants to nobody, since
      // `undefined` matches no privilege in the session.
      expect(
        Privileges[constantName],
        `App.jsx guards ${routePath} with Privileges.${constantName}, which is not declared`,
      ).toBeDefined();
    });

    expect(ROUTE_PRIVILEGES).toEqual(expected);
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
    // App.jsx guards "/PathologyCaseView/:pathologySampleId"; the menu links to
    // "/PathologyDashboard", and a case link carries a real id.
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
 * Privilege sets are the seeded ones as of 012-004o; if a grant changes, update
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
