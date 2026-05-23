import { Roles } from "../constants/roles";
import { sessionHasAnyRole } from "./routeAccess";

describe("sessionHasAnyRole", () => {
  it("uses only active login lab unit for department-scoped roles", () => {
    const user = {
      authenticated: true,
      roles: [],
      loginLabUnit: "Pathology",
      userLabRolesMap: {
        Pathology: [Roles.UPDATE_SAMPLES],
        Immunology: [Roles.MANAGE_EQUIPMENT],
      },
    };
    expect(sessionHasAnyRole(user, [Roles.UPDATE_SAMPLES])).toBe(true);
    expect(sessionHasAnyRole(user, [Roles.MANAGE_EQUIPMENT])).toBe(false);
  });

  it("denies lab-unit roles when no active department is set", () => {
    const user = {
      authenticated: true,
      roles: [],
      userLabRolesMap: {
        Pathology: [Roles.UPDATE_SAMPLES],
        Immunology: [Roles.MANAGE_EQUIPMENT],
      },
    };
    expect(sessionHasAnyRole(user, [Roles.UPDATE_SAMPLES])).toBe(false);
  });
});
