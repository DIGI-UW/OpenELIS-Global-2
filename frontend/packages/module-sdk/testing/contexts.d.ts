/**
 * The `openelis_core/contexts` double (contexts.js): the three contexts with
 * the shell's shape, defaulting to an administrator session and an empty
 * configuration.
 */
import type { UserSessionDetails } from "openelis_core/contexts";
import type { ActiveLocale } from "openelis_core/hooks";

export * from "openelis_core/contexts";

/** The session UserSessionDetailsContext defaults to. */
export const testSession: UserSessionDetails;

/** The one locale ConfigurationContext defaults to. */
export const testLocale: ActiveLocale;
