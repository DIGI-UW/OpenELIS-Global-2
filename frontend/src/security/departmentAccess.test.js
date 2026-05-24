import { Roles } from "../constants/roles";
import {
  hasUnrestrictedDepartmentAccess,
  hasActiveDepartmentScope,
  canPerformDepartmentScopedAction,
} from "./departmentAccess";
import { sessionHasAnyRole } from "./routeAccess";

describe("departmentAccess", () => {
  const restrictedUser = {
    authenticated: true,
    roles: [],
    loginLabUnit: "Pathology",
    userLabRolesMap: {
      Pathology: [Roles.UPDATE_SAMPLES],
      Immunology: [Roles.MANAGE_EQUIPMENT],
    },
  };

  const systemAdminUser = {
    authenticated: true,
    roles: [Roles.SYSTEM_ADMIN],
    userLabRolesMap: {},
  };

  it("does not treat System Admin alone as unrestricted lab access", () => {
    expect(hasUnrestrictedDepartmentAccess(systemAdminUser)).toBe(false);
    expect(hasActiveDepartmentScope(systemAdminUser)).toBe(false);
  });

  it("treats Global Administrator as unrestricted department access", () => {
    expect(
      hasUnrestrictedDepartmentAccess({
        authenticated: true,
        roles: [Roles.GLOBAL_ADMIN],
        userLabRolesMap: {},
      }),
    ).toBe(true);
  });

  it("treats AllLabUnits assignment as unrestricted department access", () => {
    expect(
      hasUnrestrictedDepartmentAccess({
        authenticated: true,
        roles: [Roles.SYSTEM_ADMIN],
        userLabRolesMap: { AllLabUnits: [Roles.LAB_MANAGER] },
      }),
    ).toBe(true);
  });

  it("requires active department when multiple lab units are assigned", () => {
    expect(
      hasActiveDepartmentScope({
        authenticated: true,
        roles: [],
        userLabRolesMap: {
          Pathology: [Roles.TECHNICIAN],
          Immunology: [Roles.MANAGE_EQUIPMENT],
        },
      }),
    ).toBe(false);
  });

  it("allows restricted user with login lab unit", () => {
    expect(hasActiveDepartmentScope(restrictedUser)).toBe(true);
  });

  it("does not grant RBAC from non-active departments", () => {
    const userWithoutLogin = {
      authenticated: true,
      roles: [],
      userLabRolesMap: {
        Pathology: [Roles.UPDATE_SAMPLES],
        Immunology: [Roles.MANAGE_EQUIPMENT],
      },
    };
    expect(
      canPerformDepartmentScopedAction(
        userWithoutLogin,
        [Roles.MANAGE_EQUIPMENT],
        sessionHasAnyRole,
      ),
    ).toBe(false);
    expect(
      canPerformDepartmentScopedAction(
        restrictedUser,
        [Roles.UPDATE_SAMPLES],
        sessionHasAnyRole,
      ),
    ).toBe(true);
  });
});
