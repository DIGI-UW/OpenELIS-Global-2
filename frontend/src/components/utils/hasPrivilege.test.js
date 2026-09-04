import { describe, expect, it } from "vitest";
import {
  computeRouteAccess,
  hasPrivilege,
  Privileges,
  RoleEquivalentPrivileges,
  Roles,
} from "./Utils";

// Spec 012 T043 — hasPrivilege() checks the resolved privilege set delivered
// by /session. The backend expands the Global Administrator sentinel to the
// full catalog before it reaches the client, so an admin session is simply a
// fully-populated privileges array.

describe("hasPrivilege", () => {
  const receptionSession = {
    authenticated: true,
    privileges: [
      Privileges.ORDER_CREATE,
      Privileges.ORDER_VIEW,
      Privileges.PATIENT_CREATE,
      Privileges.PATIENT_EDIT,
    ],
  };

  it("grants a privilege present in the session set", () => {
    expect(hasPrivilege(receptionSession, Privileges.ORDER_CREATE)).toBe(true);
  });

  it("denies a privilege missing from the session set", () => {
    expect(hasPrivilege(receptionSession, Privileges.RESULT_VALIDATE)).toBe(
      false,
    );
  });

  it("grants when ANY of several requested privileges is held", () => {
    expect(
      hasPrivilege(
        receptionSession,
        Privileges.RESULT_VALIDATE,
        Privileges.ORDER_VIEW,
      ),
    ).toBe(true);
  });

  it("denies on an empty privileges array", () => {
    expect(
      hasPrivilege({ authenticated: true, privileges: [] }, "order:view"),
    ).toBe(false);
  });

  it("denies when the session has no privileges field (pre-login poll)", () => {
    expect(hasPrivilege({ authenticated: false }, "order:view")).toBe(false);
  });

  it("denies on null/undefined session", () => {
    expect(hasPrivilege(null, "order:view")).toBe(false);
    expect(hasPrivilege(undefined, "order:view")).toBe(false);
  });

  it("grants everything for an admin session (sentinel expanded server-side)", () => {
    const adminSession = {
      authenticated: true,
      privileges: Object.values(Privileges),
    };
    Object.values(Privileges).forEach((privilege) => {
      expect(hasPrivilege(adminSession, privilege)).toBe(true);
    });
  });
});

describe("RoleEquivalentPrivileges", () => {
  it("maps every legacy route role to privileges that exist in the catalog", () => {
    const catalog = new Set(Object.values(Privileges));
    Object.entries(RoleEquivalentPrivileges).forEach(([role, privileges]) => {
      expect(privileges.length).toBeGreaterThan(0);
      privileges.forEach((privilege) => {
        expect(catalog.has(privilege), `${role} -> ${privilege}`).toBe(true);
      });
    });
  });

  it("covers the roles used by SecureRoute route definitions", () => {
    [
      Roles.RECEPTION,
      Roles.RESULTS,
      Roles.VALIDATION,
      Roles.REPORTS,
      Roles.GLOBAL_ADMIN,
    ].forEach((role) => {
      expect(RoleEquivalentPrivileges[role]).toBeDefined();
    });
  });

  it("grants a Results route to a session holding result:enter (the Results equivalent)", () => {
    // Base roles ship flat: the shipped Validation role does NOT hold
    // result:enter, so it does NOT open a Results-gated route. A session that
    // does hold result:enter (a Results user, or a custom role that inherits
    // it) is granted — the route gate is privilege-driven, not role-name-driven.
    const resultsSession = {
      authenticated: true,
      roles: [Roles.RESULTS],
      privileges: [Privileges.RESULT_ENTER, Privileges.RESULT_VIEW],
    };
    const validationOnlySession = {
      authenticated: true,
      roles: [Roles.VALIDATION],
      privileges: [Privileges.RESULT_VALIDATE, Privileges.RESULT_VIEW],
    };
    const equivalents = RoleEquivalentPrivileges[Roles.RESULTS];
    expect(hasPrivilege(resultsSession, ...equivalents)).toBe(true);
    expect(hasPrivilege(validationOnlySession, ...equivalents)).toBe(false);
  });
});

describe("computeRouteAccess", () => {
  // Route guard mirroring the Pathology dashboard: reachable by the global
  // Pathologist role/privilege OR by the Pathology unit's Results lab role.
  const PATHOLOGY_GUARD = {
    role: Roles.PATHOLOGIST,
    labUnitRole: { Pathology: [Roles.RESULTS] },
  };

  const globalPathologist = {
    authenticated: true,
    roles: [Roles.PATHOLOGIST],
    privileges: [
      Privileges.RESULT_PATHOLOGY_SIGN_OFF,
      Privileges.RESULT_VIEW,
      Privileges.ORDER_VIEW,
    ],
    userLabRolesMap: {},
  };

  const pathologyUnitTech = {
    authenticated: true,
    roles: [],
    privileges: [Privileges.RESULT_ENTER],
    userLabRolesMap: { Pathology: [Roles.RESULTS] },
  };

  const receptionOnly = {
    authenticated: true,
    roles: [Roles.RECEPTION],
    privileges: [Privileges.ORDER_CREATE],
    userLabRolesMap: {},
  };

  it("grants the Pathology route to a GLOBAL Pathologist (the bug fix — no lab-unit role)", () => {
    expect(computeRouteAccess(globalPathologist, PATHOLOGY_GUARD)).toBe(true);
  });

  it("grants the Pathology route to a Pathology-unit Results technician", () => {
    expect(computeRouteAccess(pathologyUnitTech, PATHOLOGY_GUARD)).toBe(true);
  });

  it("still denies the Pathology route to an unrelated role", () => {
    expect(computeRouteAccess(receptionOnly, PATHOLOGY_GUARD)).toBe(false);
  });

  it("grants via the sign-off privilege even without the role name", () => {
    const privOnly = {
      authenticated: true,
      roles: [],
      privileges: [Privileges.RESULT_PATHOLOGY_SIGN_OFF],
      userLabRolesMap: {},
    };
    expect(computeRouteAccess(privOnly, PATHOLOGY_GUARD)).toBe(true);
  });

  it("honors the AllLabUnits wildcard lab-role", () => {
    const wildcard = {
      authenticated: true,
      roles: [],
      privileges: [],
      userLabRolesMap: { AllLabUnits: [Roles.RESULTS] },
    };
    expect(computeRouteAccess(wildcard, PATHOLOGY_GUARD)).toBe(true);
  });

  it("grants a route with no guard props to any authenticated user", () => {
    expect(computeRouteAccess(receptionOnly, {})).toBe(true);
  });

  it("keeps AND-of-one semantics: a lab-unit-only guard still requires that lab role", () => {
    const labOnlyGuard = { labUnitRole: { Pathology: [Roles.RESULTS] } };
    expect(computeRouteAccess(globalPathologist, labOnlyGuard)).toBe(false);
    expect(computeRouteAccess(pathologyUnitTech, labOnlyGuard)).toBe(true);
  });
});
