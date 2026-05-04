/** @module @category Navigation */
import React, {
  MouseEvent,
  AnchorHTMLAttributes,
  PropsWithChildren,
} from "react";
import { navigate, interpolateUrl, TemplateParams } from "./navigation";

function handleClick(
  event: MouseEvent,
  to: string,
  templateParams?: TemplateParams,
) {
  if (
    !event.metaKey &&
    !event.ctrlKey &&
    !event.shiftKey &&
    event.button == 0
  ) {
    event.preventDefault();
    navigate({ to, templateParams });
  }
}

/**
 * @noInheritDoc
 */
export interface ConfigurableLinkProps
  extends AnchorHTMLAttributes<HTMLAnchorElement> {
  to: string;
  templateParams?: TemplateParams;
}

/**
 * A React link component which calls [[navigate]] when clicked
 *
 * @param to The target path or URL. Supports interpolation. See [[navigate]]
 * @param urlParams: A dictionary of values to interpolate into the URL, in addition to the default keys `openmrsBase` and `openmrsSpaBase`.
 * @param children Inline elements within the link
 * @param otherProps Any other valid props for an <a> tag except `href` and `onClick`
 */
export function ConfigurableLink({
  to,
  templateParams,
  children,
  ...otherProps
}: PropsWithChildren<ConfigurableLinkProps>) {
  return (
    <a
      onClick={(event) => handleClick(event, to, templateParams)}
      // href={interpolateUrl(to, templateParams)}
      href={to}
      {...otherProps}
    >
      {children}
    </a>
  );
}
