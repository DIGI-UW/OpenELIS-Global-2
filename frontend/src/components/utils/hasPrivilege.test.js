import { hasPrivilege, Privileges, Roles } from "./Utils";

const session = (overrides = {}) => ({
  authenticated: true,
  roles: [],
  privileges: [],
  ...overrides,
});

describe("hasPrivilege", () => {
  describe("unauthenticated / empty session", () => {
    test("returns false for null session", () => {
      expect(hasPrivilege(null, Privileges.RESULT_ENTER)).toBe(false);
    });

    test("returns false for undefined session", () => {
      expect(hasPrivilege(undefined, Privileges.RESULT_ENTER)).toBe(false);
    });

    test("returns false when privileges array is missing", () => {
      expect(
        hasPrivilege({ authenticated: true }, Privileges.RESULT_ENTER),
      ).toBe(false);
    });

    test("returns false when privileges array is empty", () => {
      expect(hasPrivilege(session(), Privileges.RESULT_ENTER)).toBe(false);
    });
  });

  describe("privilege present", () => {
    test("returns true when exact privilege is in the list", () => {
      const s = session({ privileges: [Privileges.RESULT_ENTER] });
      expect(hasPrivilege(s, Privileges.RESULT_ENTER)).toBe(true);
    });

    test("returns false when a different privilege is present but not the required one", () => {
      const s = session({ privileges: [Privileges.RESULT_VIEW] });
      expect(hasPrivilege(s, Privileges.RESULT_ENTER)).toBe(false);
    });

    test("returns true for admin-level privilege when present", () => {
      const s = session({ privileges: [Privileges.SYSTEM_CONFIGURE] });
      expect(hasPrivilege(s, Privileges.SYSTEM_CONFIGURE)).toBe(true);
    });
  });

  describe("Global Administrator short-circuit", () => {
    test("returns true for any privilege when user has Global Admin role", () => {
      const s = session({ roles: [Roles.GLOBAL_ADMIN], privileges: [] });
      expect(hasPrivilege(s, Privileges.RESULT_VALIDATE)).toBe(true);
      expect(hasPrivilege(s, Privileges.SYSTEM_CONFIGURE)).toBe(true);
      expect(hasPrivilege(s, Privileges.AUDIT_VIEW)).toBe(true);
    });

    test("returns true even when privileges array is absent for Global Admin", () => {
      const s = { authenticated: true, roles: [Roles.GLOBAL_ADMIN] };
      expect(hasPrivilege(s, Privileges.RESULT_ENTER)).toBe(true);
    });
  });

  describe("role inheritance via privileges array (backend-resolved)", () => {
    test("Validation user with inherited result:enter privilege passes a Results-level check", () => {
      // The backend resolves Validation → Results → Reception at login and
      // includes all inherited privileges in the session.privileges array.
      const s = session({
        roles: [Roles.VALIDATION],
        privileges: [
          Privileges.RESULT_VALIDATE,
          Privileges.RESULT_ENTER, // inherited from Results
          Privileges.ORDER_CREATE, // inherited from Reception
        ],
      });
      expect(hasPrivilege(s, Privileges.RESULT_ENTER)).toBe(true);
      expect(hasPrivilege(s, Privileges.ORDER_CREATE)).toBe(true);
      expect(hasPrivilege(s, Privileges.RESULT_VALIDATE)).toBe(true);
    });

    test("Reception user cannot access Validation-only privilege", () => {
      const s = session({
        roles: [Roles.RECEPTION],
        privileges: [Privileges.ORDER_CREATE, Privileges.PATIENT_VIEW],
      });
      expect(hasPrivilege(s, Privileges.RESULT_VALIDATE)).toBe(false);
    });

    test("Results user cannot access Validation-only privilege", () => {
      const s = session({
        roles: [Roles.RESULTS],
        privileges: [
          Privileges.RESULT_ENTER,
          Privileges.RESULT_VIEW,
          Privileges.ORDER_CREATE,
        ],
      });
      expect(hasPrivilege(s, Privileges.RESULT_VALIDATE)).toBe(false);
    });

    test("Pathologist user can access pathology sign-off privilege", () => {
      const s = session({
        roles: [Roles.PATHOLOGIST],
        privileges: [Privileges.RESULT_PATHOLOGY_SIGN_OFF],
      });
      expect(hasPrivilege(s, Privileges.RESULT_PATHOLOGY_SIGN_OFF)).toBe(true);
    });

    test("Cytopathologist user cannot access pathology sign-off (different privilege)", () => {
      const s = session({
        roles: [Roles.CYTOPATHOLOGIST],
        privileges: [Privileges.RESULT_CYTOPATHOLOGY_SIGN_OFF],
      });
      expect(hasPrivilege(s, Privileges.RESULT_PATHOLOGY_SIGN_OFF)).toBe(false);
    });
  });

  describe("privilege constant values match backend DB seeds", () => {
    test("privilege strings use colon-namespaced format", () => {
      expect(Privileges.RESULT_ENTER).toBe("result:enter");
      expect(Privileges.RESULT_VALIDATE).toBe("result:validate");
      expect(Privileges.ORDER_CREATE).toBe("order:create");
      expect(Privileges.SYSTEM_CONFIGURE).toBe("system:configure");
      expect(Privileges.RESULT_PATHOLOGY_SIGN_OFF).toBe(
        "result:pathology-sign-off",
      );
      expect(Privileges.RESULT_CYTOPATHOLOGY_SIGN_OFF).toBe(
        "result:cytopathology-sign-off",
      );
    });
  });
});
