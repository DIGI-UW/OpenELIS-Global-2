import { describe, expect, it } from "vitest";
import {
  FRONTEND_CONTRACT_VERSION,
  exposeName,
  frontendCompatibility,
  planModules,
  pickModuleMessages,
} from "./moduleRegistry";

const started = (overrides = {}) => ({
  id: "specimen-followup",
  name: "Specimen Follow-up",
  version: "1.0.0",
  kind: "module",
  state: "STARTED",
  requires: { openelis: ">=3.4.0", frontendContract: "^1.0" },
  remoteName: "specimen_followup",
  assetBase: "/modules/specimen-followup/",
  frontend: {
    entry: "remoteEntry.js",
    messages: "messages/{locale}.json",
    routes: [
      {
        path: "/m/specimen-followup/followups",
        expose: "./FollowUpListPage",
        permission: "view",
      },
      {
        path: "/m/specimen-followup/create",
        expose: "./CreateFollowUpPage",
        permission: "manage",
        exact: true,
      },
      {
        path: "/m/specimen-followup/help",
        expose: "./HelpPage",
        role: "RESULTS",
      },
    ],
    slots: [
      {
        slot: "patient.header.actions",
        expose: "./PatientAction",
        order: 50,
        permission: "manage",
      },
      { slot: "patient.header.actions", expose: "./PatientBadge", order: 10 },
    ],
  },
  ...overrides,
});

describe("frontendCompatibility", () => {
  it("accepts a started module whose contract range covers the shell", () => {
    expect(frontendCompatibility(started())).toEqual({
      loadable: true,
      reason: null,
    });
    expect(FRONTEND_CONTRACT_VERSION).toMatch(/^\d+\.\d+\.\d+$/);
  });

  it("refuses modules that are not started, have no frontend or declare an incompatible range", () => {
    expect(
      frontendCompatibility(started({ state: "INCOMPATIBLE" })).loadable,
    ).toBe(false);
    expect(frontendCompatibility(started({ frontend: null })).reason).toBe(
      "no frontend",
    );
    expect(
      frontendCompatibility(started({ requires: { openelis: ">=3.4.0" } }))
        .reason,
    ).toBe("no requires.frontendContract");
    const major = frontendCompatibility(
      started({ requires: { openelis: ">=3.4.0", frontendContract: "^2.0" } }),
    );
    expect(major.loadable).toBe(false);
    expect(major.reason).toContain("^2.0");
    expect(
      frontendCompatibility(
        started({
          requires: { openelis: ">=3.4.0", frontendContract: "latest" },
        }),
      ).reason,
    ).toContain("invalid");
  });
});

describe("planModules", () => {
  it("turns the registry into remotes, gated routes and ordered slot contributions", () => {
    const plan = planModules(
      {
        modules: [
          started(),
          started({
            id: "broken",
            remoteName: "broken",
            state: "FAILED",
            reason: "boom",
          }),
        ],
      },
      "/api/OpenELIS-Global",
    );

    expect(plan.modules.map((m) => m.id)).toEqual(["specimen-followup"]);
    expect(plan.skipped.map((m) => [m.id, m.frontendReason])).toEqual([
      ["broken", "module is FAILED"],
    ]);
    expect(plan.remotes).toEqual([
      {
        name: "specimen_followup",
        entry: "/api/OpenELIS-Global/modules/specimen-followup/remoteEntry.js",
        type: "module",
      },
    ]);
    expect(plan.routes).toEqual([
      {
        moduleId: "specimen-followup",
        remoteName: "specimen_followup",
        path: "/m/specimen-followup/followups",
        exact: true,
        expose: "./FollowUpListPage",
        permission: "mod.specimen-followup.view",
        role: null,
      },
      {
        moduleId: "specimen-followup",
        remoteName: "specimen_followup",
        path: "/m/specimen-followup/create",
        exact: true,
        expose: "./CreateFollowUpPage",
        permission: "mod.specimen-followup.manage",
        role: null,
      },
      {
        moduleId: "specimen-followup",
        remoteName: "specimen_followup",
        path: "/m/specimen-followup/help",
        exact: true,
        expose: "./HelpPage",
        permission: null,
        role: "RESULTS",
      },
    ]);
    expect(
      plan.slots["patient.header.actions"].map((c) => [
        c.expose,
        c.order,
        c.permission,
      ]),
    ).toEqual([
      ["./PatientBadge", 10, null],
      ["./PatientAction", 50, "mod.specimen-followup.manage"],
    ]);
  });

  it("derives the remote name and asset base when the registry omits them", () => {
    const plan = planModules(
      {
        modules: [
          started({
            remoteName: undefined,
            assetBase: undefined,
            id: "my-mod-x",
          }),
        ],
      },
      "/api/OpenELIS-Global",
    );
    expect(plan.modules[0].remoteName).toBe("my_mod_x");
    expect(plan.remotes[0].entry).toBe(
      "/api/OpenELIS-Global/modules/my-mod-x/remoteEntry.js",
    );
  });

  it("copes with an empty or missing registry", () => {
    expect(planModules(null, "/api").routes).toEqual([]);
    expect(planModules({ modules: [] }, "/api").remotes).toEqual([]);
  });
});

describe("exposeName", () => {
  it("drops the ./ prefix loadRemote does not want", () => {
    expect(exposeName("./FollowUpListPage")).toBe("FollowUpListPage");
    expect(exposeName("Page")).toBe("Page");
  });
});

describe("pickModuleMessages", () => {
  const bundles = {
    en: { "mod.alpha.menu": "Alpha" },
    fr: { "mod.alpha.menu": "Alpha (fr)" },
    fr_CA: { "mod.alpha.menu": "Alpha (CA)" },
  };

  it("takes the exact locale, in either spelling, before the base language", () => {
    expect(pickModuleMessages(bundles, "fr-CA")["mod.alpha.menu"]).toBe(
      "Alpha (CA)",
    );
    expect(pickModuleMessages(bundles, "fr-FR")["mod.alpha.menu"]).toBe(
      "Alpha (fr)",
    );
  });

  it("falls back to English and then to nothing", () => {
    expect(pickModuleMessages(bundles, "de-DE")["mod.alpha.menu"]).toBe(
      "Alpha",
    );
    expect(pickModuleMessages({}, "en-US")).toEqual({});
    expect(pickModuleMessages(undefined, "en")).toEqual({});
  });

  it("planModules carries the registry's bundles through", () => {
    const module = {
      id: "alpha",
      state: "STARTED",
      frontend: { entry: "remoteEntry.js" },
      requires: { frontendContract: "^1.0" },
      messages: { en: { "mod.alpha.menu": "Alpha" } },
    };
    const plan = planModules({ modules: [module] }, "/api");
    expect(plan.modules[0].messages.en["mod.alpha.menu"]).toBe("Alpha");
    expect(
      planModules({ modules: [{ ...module, messages: undefined }] }, "/api")
        .modules[0].messages,
    ).toEqual({});
  });
});
