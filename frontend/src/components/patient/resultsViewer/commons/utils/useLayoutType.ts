/** @module @category UI */
import { useEffect, useState } from "react";

export type LayoutType = "phone" | "tablet" | "small-desktop" | "large-desktop";

function getLayout() {
  let layout: LayoutType = "large-desktop";

  document.body.classList.forEach((cls) => {
    switch (cls) {
      case "omrs-breakpoint-lt-tablet":
        layout = "phone";
        break;
      case "omrs-breakpoint-gt-small-desktop":
        layout = "large-desktop";
        break;
      case "omrs-breakpoint-gt-tablet":
        layout = "small-desktop";
        break;
    }
  });

  return layout;
}

export function useLayoutType() {
  const [type, setType] = useState<LayoutType>(getLayout);

  useEffect(() => {
    const handler = () => {
      setType(getLayout());
    };
    window.addEventListener("resize", handler);
    return () => window.removeEventListener("resize", handler);
  }, []);
  //let layoutLg : LayoutType = "large-desktop";
  return type;
  //return layoutLg
}

export const isDesktop = (layout: LayoutType) =>
  layout === "small-desktop" || layout === "large-desktop";
