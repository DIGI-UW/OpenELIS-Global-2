import {
  NOTEBOOK_STAGE_ACTIONS,
  isActionPermitted,
  resolvePageAllowedRoles,
  resolvePageKey,
} from "../constants/ahriWorkflowRegistry";

describe("registry pageKey + action", () => {
  it("resolves stable pageKey from pageId", () => {
    expect(resolvePageKey({ pageId: "intake", order: 1 })).toBe("intake");
  });

  it("permits VIEW for biorepository intake", () => {
    expect(
      isActionPermitted("biorepository", { pageId: "intake", order: 1 }, "VIEW"),
    ).toBe(true);
  });

  it("fail-closed for unknown workflow", () => {
    expect(resolvePageAllowedRoles("unknown", { pageId: "x", order: 1 }, "VIEW")).toEqual([]);
  });

  it("denies EDIT when action not in registry stage", () => {
    expect(
      isActionPermitted("unknown", { pageId: "intake", order: 1 }, "EDIT"),
    ).toBe(false);
  });
});

describe("NOTEBOOK_STAGE_ACTIONS", () => {
  it("defines VIEW EDIT COMPLETE", () => {
    expect(NOTEBOOK_STAGE_ACTIONS.VIEW).toBe("VIEW");
    expect(NOTEBOOK_STAGE_ACTIONS.EDIT).toBe("EDIT");
    expect(NOTEBOOK_STAGE_ACTIONS.COMPLETE).toBe("COMPLETE");
  });
});
