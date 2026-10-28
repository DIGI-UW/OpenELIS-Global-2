import type { IntlShape } from "react-intl";

/**
 * The name a configuration setting is shown under. Settings are stored by key
 * (pathology.identifier.partScheme), which reads as a raw key on the page; a
 * setting with a translation under siteInformation.name.<key> shows that
 * instead, and any other keeps its stored name (OGC-1547).
 */
export const settingName = (intl: IntlShape, name: string): string => {
  const id = `siteInformation.name.${name}`;
  return intl.messages[id] ? intl.formatMessage({ id }) : name;
};

/**
 * The description a setting is shown with: its translation under
 * siteInformation.description.<key> when there is one, otherwise the stored
 * description, which is in English.
 */
export const settingDescription = (
  intl: IntlShape,
  name: string,
  stored: string,
): string => {
  const id = `siteInformation.description.${name}`;
  return intl.messages[id] ? intl.formatMessage({ id }) : stored;
};
