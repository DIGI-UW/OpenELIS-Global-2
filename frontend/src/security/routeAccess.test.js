import { Roles } from "../components/utils/Utils";
import { Roles as ExtRoles } from "../constants/roles";
import {
  canAccessPath,
  getDepartmentLabUnitKeys,
  getEffectiveLabUnitNameForRoleCheck,
  sessionHasAnyRole,
} from "./routeAccess";

const authed = (overrides) => ({
  authenticated: true,
  roles: [],
  userLabRolesMap: {},
  ...overrides,
});

describe("sessionHasAnyRole", () => {
  it("allows global role match", () => {
    const s = authed({ roles: [Roles.RESULTS] });
    expect(sessionHasAnyRole(s, [Roles.RESULTS])).toBe(true);
  });

  it("allows Global Administrator for any list", () => {
    const s = authed({ roles: [Roles.GLOBAL_ADMIN] });
    expect(sessionHasAnyRole(s, ["No Such Role"])).toBe(true);
  });

  it("allows lab-unit-only role in a department map", () => {
    const s = authed({
      userLabRolesMap: {
        Immunology: [ExtRoles.LABORATORY_TECHNICIANS],
      },
    });
    expect(sessionHasAnyRole(s, [ExtRoles.LABORATORY_TECHNICIANS])).toBe(true);
  });

  it("allows role via AllLabUnits", () => {
    const s = authed({
      userLabRolesMap: {
        AllLabUnits: [ExtRoles.STORAGE_MANAGER],
      },
    });
    expect(sessionHasAnyRole(s, [ExtRoles.STORAGE_MANAGER])).toBe(true);
  });

  it("denies when no matching role", () => {
    const s = authed({
      roles: [Roles.RECEPTION],
      userLabRolesMap: { Immunology: ["Sample Collector"] },
    });
    expect(sessionHasAnyRole(s, [Roles.RESULTS])).toBe(false);
  });

  it("empty allowed list is allow", () => {
    expect(sessionHasAnyRole(authed({ roles: [] }), [])).toBe(true);
  });
});

describe("getEffectiveLabUnitNameForRoleCheck", () => {
  it("returns loginLabUnit when set", () => {
    const s = authed({
      loginLabUnit: "Immunology",
      userLabRolesMap: { Biochemistry: [ExtRoles.TECHNICIAN] },
    });
    expect(getEffectiveLabUnitNameForRoleCheck(s)).toBe("Immunology");
  });

  it("returns sole department key when loginLabUnit empty", () => {
    const s = authed({
      userLabRolesMap: { Immunology: [ExtRoles.LABORATORY_TECHNICIAN] },
    });
    expect(getEffectiveLabUnitNameForRoleCheck(s)).toBe("Immunology");
  });

  it("returns null when multiple departments and no loginLabUnit", () => {
    const s = authed({
      userLabRolesMap: {
        Immunology: [ExtRoles.LABORATORY_TECHNICIAN],
        Biochemistry: [ExtRoles.TECHNICIAN],
      },
    });
    expect(getEffectiveLabUnitNameForRoleCheck(s)).toBe(null);
  });
});

describe("getDepartmentLabUnitKeys", () => {
  it("excludes AllLabUnits and empty buckets", () => {
    const s = authed({
      userLabRolesMap: {
        AllLabUnits: [Roles.RESULTS],
        Immunology: [ExtRoles.LABORATORY_TECHNICIANS],
        EmptyDept: [],
      },
    });
    expect(getDepartmentLabUnitKeys(s)).toEqual(["Immunology"]);
  });
});

describe("canAccessPath", () => {
  it("returns null for paths without client rules", () => {
    expect(
      canAccessPath("/admin", authed({ roles: [Roles.GLOBAL_ADMIN] })),
    ).toBe(null);
  });

  it("allows /Storage for lab-only technician", () => {
    const s = authed({
      userLabRolesMap: {
        Immunology: [ExtRoles.LABORATORY_TECHNICIANS],
      },
    });
    expect(canAccessPath("/Storage/rooms", s)).toBe(true);
  });

  it("uses Result entry rules for exact /PatientResults", () => {
    const s = authed({ roles: [Roles.RESULTS] });
    expect(canAccessPath("/PatientResults", s)).toBe(true);
  });

  it("uses Reception rules for /PatientResults/:id", () => {
    const s = authed({ roles: [Roles.RECEPTION] });
    expect(canAccessPath("/PatientResults/abc-123", s)).toBe(true);
  });

  it("denies /PatientResults exact when only Reception global role", () => {
    const s = authed({ roles: [Roles.RECEPTION] });
    expect(canAccessPath("/PatientResults", s)).toBe(false);
  });
});
