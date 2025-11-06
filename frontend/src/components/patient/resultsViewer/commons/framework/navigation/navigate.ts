/** @module @category Navigation */
import { navigateToUrl } from "single-spa";
import { interpolateUrl } from "./interpolate-string";
import type {} from "../globals/types";

function trimTrailingSlash(str: string) {
  return str.replace(/\/$/, "");
}

export type TemplateParams = { [key: string]: string };

export interface NavigateOptions {
  to: string;
  templateParams?: TemplateParams;
}

/**
 * Calls `location.assign` for non-SPA paths and [navigateToUrl](https://single-spa.js.org/docs/api/#navigatetourl) for SPA paths
 *
 * #### Example usage:
 * ```js
 * @example
 * const config = useConfig();
 * const submitHandler = () => {
 *   navigate({ to: config.links.submitSuccess });
 * };
 * ```
 * #### Example return values:
 * ```js
 * @example
 * navigate({ to: "/some/path" }); // => window.location.assign("/some/path")
 * navigate({ to: "https://single-spa.js.org/" }); // => window.location.assign("https://single-spa.js.org/")
 * navigate({ to: "${openmrsBase}/some/path" }); // => window.location.assign("/openmrs/some/path")
 * navigate({ to: "/openmrs/spa/foo/page" }); // => navigateToUrl("/openmrs/spa/foo/page")
 * navigate({ to: "${openmrsSpaBase}/bar/page" }); // => navigateToUrl("/openmrs/spa/bar/page")
 * navigate({ to: "/${openmrsSpaBase}/baz/page" }) // => navigateToUrl("/openmrs/spa/baz/page")
 * ```
 *
 * @param to The target path or URL. Supports templating with 'openmrsBase', 'openmrsSpaBase',
 * and any additional template parameters defined in `templateParams`.
 * For example, `${openmrsSpaBase}/home` will resolve to `/openmrs/spa/home`
 * for implementations using the standard OpenMRS and SPA base paths.
 * If `templateParams` contains `{ foo: "bar" }`, then the URL `${openmrsBase}/${foo}`
 * will become `/openmrs/bar`.
 */
export function navigate({ to, templateParams }: NavigateOptions): void {
  //const openmrsSpaBase = trimTrailingSlash(window.getOpenmrsSpaBase());
  const target = interpolateUrl(to, templateParams);
  // const isSpaPath = target.startsWith(openmrsSpaBase);
  const isSpaPath = true;

  if (isSpaPath) {
    navigateToUrl(target);
  } else {
    window.location.assign(target);
  }
}
