import { IntlShape } from "react-intl";

/**
 * OGC-1020 (R1) — cross-domain label resolution (FR-M4).
 *
 * i18n keys follow `label.foo` / `label.foo.env` / `label.foo.vector` with
 * clinical fallback: for ENVIRONMENTAL the `.env` variant is used when it
 * exists, for VECTOR the `.vector` variant; otherwise the base (clinical) key.
 */

export type ResultsDomain = "CLINICAL" | "ENVIRONMENTAL" | "VECTOR";

const DOMAIN_SUFFIX: Record<ResultsDomain, string> = {
  CLINICAL: "",
  ENVIRONMENTAL: ".env",
  VECTOR: ".vector",
};

export function domainMessageId(
  intl: IntlShape,
  baseId: string,
  domain: ResultsDomain,
): string {
  const suffix = DOMAIN_SUFFIX[domain] || "";
  const candidate = baseId + suffix;
  if (suffix && intl.messages && candidate in intl.messages) {
    return candidate;
  }
  return baseId;
}

export function formatDomainMessage(
  intl: IntlShape,
  baseId: string,
  domain: ResultsDomain,
): string {
  return intl.formatMessage({ id: domainMessageId(intl, baseId, domain) });
}

/** Lab-unit payloads may carry unknown/legacy domains; default clinical. */
export function normalizeDomain(domain?: string | null): ResultsDomain {
  return domain === "ENVIRONMENTAL" || domain === "VECTOR"
    ? domain
    : "CLINICAL";
}
