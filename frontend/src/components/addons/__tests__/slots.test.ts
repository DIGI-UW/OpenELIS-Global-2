import { describe, expect, it } from "vitest";
import {
  extensionPagePath,
  extensionsForSlot,
  pageOfPath,
  SLOT_APP_MENU,
  SLOT_PATIENT_RESULTS,
  withAddonMenu,
} from "../slots";
import { clampHeight, parseFrameMessage, stateKey } from "../bridge";
import { configToSave, initialConfig } from "../configForm";
import type { AddonExtension, ConfigField } from "../addonApi";

interface MenuItem {
  menu: Record<string, unknown>;
  childMenus: MenuItem[];
}

const ext = (p: Partial<AddonExtension>): AddonExtension => ({
  addonId: "hello",
  addonName: "Hello",
  version: "1.0.0",
  name: "panel",
  slot: SLOT_PATIENT_RESULTS,
  page: "panel.html",
  order: 100,
  title: "Hello",
  height: 200,
  ...p,
});

describe("extension slots", () => {
  it("keeps the slot's extensions, ordered by order then add-on", () => {
    const all = [
      ext({ addonId: "b", order: 10 }),
      ext({ addonId: "a", order: 10 }),
      ext({ addonId: "c", order: 1 }),
      ext({ addonId: "m", slot: SLOT_APP_MENU }),
    ];
    expect(
      extensionsForSlot(all, SLOT_PATIENT_RESULTS).map((e) => e.addonId),
    ).toEqual(["c", "a", "b"]);
  });

  it("returns the very same menu when no add-on adds an entry", () => {
    const menus: MenuItem[] = [
      { menu: { elementId: "menu_home" }, childMenus: [] },
    ];
    expect(withAddonMenu(menus, [ext({})], "addon.admin.title")).toBe(menus);
    expect(
      withAddonMenu(undefined, [ext({ slot: SLOT_APP_MENU })], "x"),
    ).toBeUndefined();
  });

  it("adds one section with an entry per menu extension", () => {
    const menus: MenuItem[] = [
      { menu: { elementId: "menu_home" }, childMenus: [] },
    ];
    const out = withAddonMenu(
      menus,
      [ext({ slot: SLOT_APP_MENU, name: "board", title: "Board" })],
      "addon.admin.title",
    )!;
    expect(out).toHaveLength(2);
    expect(out[1].menu).toMatchObject({
      presentationStyle: "section",
      displayKey: "addon.admin.title",
    });
    expect(out[1].childMenus[0].menu).toMatchObject({
      displayKey: "Board",
      actionURL: "/extensions/hello/board",
      isActive: true,
    });
  });

  it("encodes the page path", () => {
    expect(extensionPagePath({ addonId: "a b", name: "c/d" })).toBe(
      "/extensions/a%20b/c%2Fd",
    );
  });
});

describe("frame protocol", () => {
  it("accepts only the known messages", () => {
    expect(parseFrameMessage({ type: "ready" })).toEqual({ type: "ready" });
    expect(parseFrameMessage({ type: "token-refresh" })).toEqual({
      type: "token-refresh",
    });
    expect(parseFrameMessage({ type: "resize", height: 5000 })).toEqual({
      type: "resize",
      height: 900,
    });
    expect(parseFrameMessage({ type: "resize", height: "x" })).toBeNull();
    expect(parseFrameMessage({ type: "navigate", url: "/" })).toBeNull();
    expect(parseFrameMessage("ready")).toBeNull();
    expect(parseFrameMessage(null)).toBeNull();
  });

  it("keeps frame heights within bounds", () => {
    expect(clampHeight(10)).toBe(80);
    expect(clampHeight(300.4)).toBe(300);
  });

  it("gives a new state key when the state changes", () => {
    expect(stateKey({ accessionNumbers: ["A"] })).not.toBe(
      stateKey({ accessionNumbers: ["B"] }),
    );
    expect(stateKey(undefined)).toBe(stateKey({}));
  });
});

describe("configuration form", () => {
  it("starts from saved values, then defaults, then empty values", () => {
    const schema: ConfigField[] = [
      {
        key: "greeting",
        type: "string",
        label: "G",
        required: false,
        defaultValue: "Hi",
        options: [],
        min: null,
        max: null,
      },
      {
        key: "limit",
        type: "number",
        label: "L",
        required: false,
        defaultValue: null,
        options: [],
        min: 3,
        max: 9,
      },
      {
        key: "on",
        type: "boolean",
        label: "O",
        required: false,
        defaultValue: null,
        options: [],
        min: null,
        max: null,
      },
    ];
    expect(initialConfig(schema, { greeting: "Hello" })).toEqual({
      greeting: "Hello",
      limit: 3,
      on: false,
    });
  });
});

describe("header extensions", () => {
  it("are told only the screen, as a kebab-case page name", () => {
    expect(pageOfPath("/ResultValidation")).toBe("result-validation");
    expect(pageOfPath("/PatientResults/42?x=1")).toBe("patient-results");
    expect(pageOfPath("/")).toBe("home");
    expect(pageOfPath("/k37/TraCuu")).toBe("k37");
    expect(
      pageOfPath("/a".repeat(3) + "x".repeat(60)).length,
    ).toBeLessThanOrEqual(40);
  });
});

describe("secret configuration fields", () => {
  const schema: ConfigField[] = [
    {
      key: "model",
      type: "string",
      label: "Model",
      required: false,
      defaultValue: "m",
      options: [],
      min: null,
      max: null,
    },
    {
      key: "apiKey",
      type: "secret",
      label: "Key",
      required: false,
      defaultValue: null,
      options: [],
      min: null,
      max: null,
    },
  ];

  it("start empty whatever was saved", () => {
    expect(initialConfig(schema, { model: "x", apiKey: "leak" })).toEqual({
      model: "x",
      apiKey: "",
    });
  });

  it("are sent only when typed or cleared", () => {
    expect(configToSave(schema, { model: "x", apiKey: "" })).toEqual({
      model: "x",
    });
    expect(configToSave(schema, { model: "x", apiKey: "k" })).toEqual({
      model: "x",
      apiKey: "k",
    });
    expect(
      configToSave(schema, { model: "x", apiKey: "" }, new Set(["apiKey"])),
    ).toEqual({ model: "x", apiKey: "" });
  });
});
