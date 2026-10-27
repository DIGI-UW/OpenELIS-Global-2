import React, { useEffect, useState } from "react";
import {
  EXTENSIONS_CHANGED,
  loadExtensions,
  type AddonExtension,
} from "./addonApi";
import { extensionsForSlot } from "./slots";
import AddonFrame from "./AddonFrame";
import "./addons.scss";

/** The extensions of enabled add-ons; reloaded when an add-on is switched. */
export function useExtensions(active = true): AddonExtension[] {
  const [all, setAll] = useState<AddonExtension[]>([]);
  useEffect(() => {
    if (!active) {
      setAll([]);
      return undefined;
    }
    let live = true;
    const load = () =>
      loadExtensions().then((found) => {
        if (live) setAll(found);
      });
    load();
    window.addEventListener(EXTENSIONS_CHANGED, load);
    return () => {
      live = false;
      window.removeEventListener(EXTENSIONS_CHANGED, load);
    };
  }, [active]);
  return all;
}

export function useSlot(name: string): AddonExtension[] {
  return extensionsForSlot(useExtensions(), name);
}

/**
 * A named slot, like `<ExtensionSlot name state>` in OpenMRS 3. With no
 * extension it renders nothing at all, so pages look exactly as before for a
 * laboratory without add-ons.
 */
export default function ExtensionSlot({
  name,
  state,
  className,
}: {
  name: string;
  state?: Record<string, unknown>;
  className?: string;
}) {
  const extensions = useSlot(name);
  if (!extensions.length) return null;
  return (
    <div
      className={`addon-slot ${className || ""}`.trim()}
      data-extension-slot-name={name}
    >
      {extensions.map((e) => (
        <AddonFrame
          key={`${e.addonId}/${e.name}`}
          extension={e}
          state={state}
        />
      ))}
    </div>
  );
}
