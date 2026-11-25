/**
 * The `openelis_core/hooks` doubles (hooks.js): no router, no server.
 */
import type { ActiveLocale, useDomains } from "openelis_core/hooks";

export * from "openelis_core/hooks";

/** Every href the useInAppNavigation double was asked to open, oldest first. */
export const navigations: string[];

/** What the useActiveLocales double answers. */
export const testLocales: ActiveLocale[];

/** What the useDomains double answers. */
export const testDomains: ReturnType<typeof useDomains>;
