/** @module @category Navigation */

function trimTrailingSlash(str: string) {
  return str.replace(/\/$/, "");
}

/**
 * Interpolates a string with openmrsBase and openmrsSpaBase.
 *
 * Useful for accepting `${openmrsBase}` or `${openmrsSpaBase}`plus additional template
 * parameters in configurable URLs.
 *
 * Example usage:
 * ```js
 * interpolateUrl("test ${openmrsBase} ${openmrsSpaBase} ok");
 *    // will return "test /openmrs /openmrs/spa ok"
 *
 * interpolateUrl("${openmrsSpaBase}/patient/${patientUuid}", {
 *    patientUuid: "4fcb7185-c6c9-450f-8828-ccae9436bd82",
 * }); // will return "/openmrs/spa/patient/4fcb7185-c6c9-450f-8828-ccae9436bd82"
 * ```
 *
 * This can be used in conjunction with the `navigate` function like so
 * ```js
 * navigate({
 *  to: interpolateUrl(
 *    "${openmrsSpaBase}/patient/${patientUuid}",
 *    { patientUuid: patient.uuid }
 *  )
 * }); // will navigate to "/openmrs/spa/patient/4fcb7185-c6c9-450f-8828-ccae9436bd82"
 * ```
 *
 * @param template A string to interpolate
 * @param additionalParams Additional values to interpolate into the string template
 */
export function interpolateUrl(
  template: string,
  additionalParams?: { [key: string]: string },
): string {
  //const openmrsSpaBase = trimTrailingSlash(window.getOpenmrsSpaBase());
  return interpolateString(template, {
    openmrsBase: window.openmrsBase,
    // openmrsSpaBase: openmrsSpaBase,
    openmrsSpaBase: "https://localhost/api",
    ...additionalParams,
  }).replace(/^\/\//, "/"); // remove extra initial slash if present
}

/**
 * Interpolates values of `params` into the `template` string.
 *
 * Example usage:
 * ```js
 * interpolateString("test ${one} ${two} 3", {
 *    one: "1",
 *    two: "2",
 * }); // will return "test 1 2 3"
 * interpolateString("test ok", { one: "1", two: "2" }) // will return "test ok"
 * ```
 *
 * @param template With optional params wrapped in `${ }`
 * @param params Values to interpolate into the string template
 */
export function interpolateString(
  template: string,
  params: { [key: string]: string },
): string {
  const names = Object.keys(params);
  return names.reduce(
    (prev, curr) => prev.split("${" + curr + "}").join(params[curr]),
    template,
  );
}
