import { describe, expect, it } from "vitest";
import fs from "node:fs";
import path from "node:path";

// Spec 012 — the frontend is privilege-driven. Every SecureRoute guard names a
// privilege, not a role.
//
// Why this is a test and not a convention: role guards fail OPEN in two ways
// that are invisible in review. A role name that does not exist on the Roles
// constant (Roles.LAB_SUPERVISOR did not) evaluates to undefined, which
// computeRouteAccess reads as "no explicit access requested" and grants to any
// authenticated user — that is how seven QC routes sat unguarded. And a role
// guard cannot be satisfied by an inherited role, because ROLE_* authorities
// come only from directly assigned roles while parent_role_id propagates
// privileges, so a combined role silently loses UI access it holds server-side.
const appSource = fs.readFileSync(
  path.join(__dirname, "../../App.jsx"),
  "utf8",
);

const utilsSource = fs.readFileSync(path.join(__dirname, "Utils.ts"), "utf8");
const declaredPrivileges = new Set(
  (utilsSource.match(/^\s{2}([A-Z][A-Z0-9_]*):\s*"/gm) || []).map(
    (line) => line.trim().split(":")[0],
  ),
);

describe("App route guards", () => {
  // Every route this branch guards names a privilege. The 93 role guards
  // below all arrived with develop (QA, QC, EQA V2, workplan) - develop's own
  // App.jsx carries exactly 93, so this merge added none. None was
  // privilege-guarded on the OGC-384 head, so converting them is a decision
  // about develop's features rather than a regression to repair here. Both
  // failure modes in the header comment are live against them TODAY:
  //
  //   fails open   - Roles.LAB_SUPERVISOR was declared on neither side, so
  //                  /qa/qc/reagent-qc granted to any authenticated user until
  //                  this merge declared it.
  //   fails closed - 48 of the 56 role-ONLY routes are refused to a user who HOLDS the required
  //                  privileges, because five seeded roles (Lab Technician,
  //                  Results Validator, Sample Collector, Lab Supervisor,
  //                  Quality Control Officer) inherit privileges through
  //                  parent_role_id while ROLE_* authorities come only from a
  //                  directly assigned role.
  //
  // The list is a ratchet: it may shrink as routes are converted, never grow.
  // A NEW role-guarded route fails this test, which is the point.
  const ROLE_GUARDED_FROM_DEVELOP = 93;

  it("adds no new role-guarded route", () => {
    const roleGuards = appSource.match(/role=\{Roles\.[A-Z_]+\}/g) || [];
    expect(roleGuards.length).toBeLessThanOrEqual(ROLE_GUARDED_FROM_DEVELOP);
  });

  it("names only roles that exist on the Roles constant", () => {
    // The fail-open case. An undeclared name is undefined, which
    // computeRouteAccess reads as "no access requested" and grants to everyone.
    const declaredRoles = new Set(
      (utilsSource.match(/^\s{2}([A-Z][A-Z0-9_]*):\s*"/gm) || []).map(
        (line) => line.trim().split(":")[0],
      ),
    );
    const used = [
      ...new Set(
        (appSource.match(/role=\{Roles\.([A-Z_]+)\}/g) || []).map((m) =>
          m.replace(/.*Roles\./, "").replace("}", ""),
        ),
      ),
    ];
    expect(used.filter((name) => !declaredRoles.has(name))).toEqual([]);
  });

  it("guards routes on privileges", () => {
    // 93 on the OGC-384 head; 17 of those routes were restructured away by
    // develop, and the remaining 76 are guarded here. Lowering this further
    // means privilege guards were lost, which is exactly what this caught when
    // the merge left only 6.
    const privilegeGuards =
      appSource.match(/privilege=\{Privileges\.[A-Z_]+\}/g) || [];
    expect(privilegeGuards.length).toBeGreaterThanOrEqual(67);
  });

  it("names only privileges that exist on the Privileges constant", () => {
    const declared = declaredPrivileges;
    const used = [
      ...new Set(
        (appSource.match(/privilege=\{Privileges\.([A-Z_]+)\}/g) || []).map(
          (m) => m.replace(/.*Privileges\./, "").replace("}", ""),
        ),
      ),
    ];
    expect(used.length).toBeGreaterThan(0);
    const undeclared = used.filter((name) => !declared.has(name));
    expect(undeclared).toEqual([]);
  });
});

/**
 * Privileges.SOMETHING_UNDECLARED silently evaluates to undefined, and
 * hasPrivilege(session, undefined) is always false — so a control gated on a
 * misspelled or missing constant disappears for EVERYONE with no error. That
 * is not hypothetical: Privileges.MICRO_VIEW was used by the WHONET export
 * button while micro:view was absent from the constant, so the button could
 * never render.
 */
describe("Privileges constants referenced by components", () => {
  const componentsDir = path.join(__dirname, "..");

  const walk = (dir) =>
    fs.readdirSync(dir, { withFileTypes: true }).flatMap((entry) => {
      const full = path.join(dir, entry.name);
      if (entry.isDirectory()) return walk(full);
      // Utils.ts is the declaration site; its own prose mentions
      // Privileges.X as an example, which is not a real reference.
      const isSource =
        /\.(jsx?|tsx?)$/.test(entry.name) &&
        !/\.test\./.test(entry.name) &&
        entry.name !== "Utils.ts";
      return isSource ? [full] : [];
    });

  it("are all declared", () => {
    const undeclared = new Map();
    walk(componentsDir).forEach((file) => {
      const source = fs.readFileSync(file, "utf8");
      (source.match(/\bPrivileges\.([A-Z][A-Z0-9_]*)/g) || []).forEach(
        (ref) => {
          const name = ref.replace("Privileges.", "");
          if (!declaredPrivileges.has(name)) {
            undeclared.set(name, path.relative(componentsDir, file));
          }
        },
      );
    });
    expect(Object.fromEntries(undeclared)).toEqual({});
  });
});
