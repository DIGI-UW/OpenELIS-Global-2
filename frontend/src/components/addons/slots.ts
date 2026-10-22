/**
 * Extension slots, after the OpenMRS 3 extension system
 * (https://o3-docs.openmrs.org/docs/extension-system): a page declares a named
 * slot, add-ons contribute extensions to it, and the page passes its state to
 * the slot. Slot names come from the server's catalog (AddonCatalog).
 */
import type { AddonExtension } from "./addonApi";

export const SLOT_RESULT_VALIDATION = "result-validation-side-panel-slot";
export const SLOT_PATIENT_RESULTS = "result-lookup-side-panel-slot";
export const SLOT_APP_MENU = "app-menu-slot";
export const SLOT_TOP_NAV_ACTIONS = "top-nav-actions-slot";
/** The add-on's own settings page, shown on the Extensions page (administrators). */
export const SLOT_ADDON_SETTINGS = "addon-settings-page-slot";

/**
 * The page name a header extension is told about: the first segment of the
 * route in kebab-case ("/ResultValidation" → "result-validation"); "home" for
 * the root. Nothing else of the URL (ids, query) is passed on.
 */
export function pageOfPath(pathname: string): string {
  const first = (pathname || "").split("/").filter(Boolean)[0] || "home";
  const kebab = first
    .replace(/([a-z0-9])([A-Z])/g, "$1-$2")
    .replace(/[^A-Za-z0-9]+/g, "-")
    .toLowerCase()
    .replace(/^-+|-+$/g, "");
  return (kebab || "home").slice(0, 40);
}

export function extensionsForSlot(
  all: AddonExtension[],
  slot: string,
): AddonExtension[] {
  return all
    .filter((e) => e.slot === slot)
    .sort(
      (a, b) =>
        a.order - b.order ||
        a.addonId.localeCompare(b.addonId) ||
        a.name.localeCompare(b.name),
    );
}

/**
 * The extension's title in the user's locale: exact match ("fr-CA"), then the
 * language ("fr"), then English, then the manifest's default title.
 */
export function titleFor(
  e: { title: string; titles?: Record<string, string> },
  locale?: string,
): string {
  const t = e.titles || {};
  const loc = (locale || "en").replace("_", "-");
  const lang = loc.split("-")[0].toLowerCase();
  const exact = Object.keys(t).find(
    (k) => k.replace("_", "-").toLowerCase() === loc.toLowerCase(),
  );
  return (exact && t[exact]) || t[lang] || t.en || e.title;
}

/** Route of the full page that an app-menu-slot extension opens. */
export const extensionPagePath = (e: { addonId: string; name: string }) =>
  `/extensions/${encodeURIComponent(e.addonId)}/${encodeURIComponent(e.name)}`;

interface MenuItem {
  menu: Record<string, unknown>;
  childMenus: MenuItem[];
}

/**
 * Adds a section with one entry per app-menu-slot extension at the end of the
 * main menu. Without such extensions the same array is returned, so a
 * laboratory without add-ons gets exactly the menu it had.
 */
export function withAddonMenu<T extends MenuItem>(
  menus: T[] | undefined,
  all: AddonExtension[],
  sectionKey: string,
  locale?: string,
): T[] | undefined {
  const items = extensionsForSlot(all, SLOT_APP_MENU);
  if (!menus || !items.length) {
    return menus;
  }
  const section = {
    menu: {
      id: "menu_addons",
      elementId: "menu_addons",
      displayKey: sectionKey,
      presentationStyle: "section",
      isActive: true,
    },
    childMenus: items.map((e, i) => ({
      menu: {
        id: `menu_addon_${e.addonId}_${e.name}`,
        elementId: `menu_addon_${e.addonId}_${e.name}`,
        displayKey: titleFor(e, locale),
        actionURL: extensionPagePath(e),
        isActive: true,
        presentationOrder: i,
        icon: "more",
      },
      childMenus: [],
    })),
  } as unknown as T;
  return [...menus, section];
}
