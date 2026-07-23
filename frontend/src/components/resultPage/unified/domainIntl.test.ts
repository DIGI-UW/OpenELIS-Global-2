import { describe, expect, it } from "vitest";
import { createIntl } from "react-intl";
import {
  domainMessageId,
  formatDomainMessage,
  normalizeDomain,
} from "./domainIntl";

/** OGC-1020 (R1) — FR-M4: `.env`/`.vector` suffix with clinical fallback. */
describe("domainIntl", () => {
  const intl = createIntl({
    locale: "en",
    messages: {
      "label.results.subject": "Sample / Patient",
      "label.results.subject.env": "Sample / Site",
      "label.results.subject.vector": "Sample / Trap",
      "label.results.range": "Reference Range",
      "label.results.range.env": "Regulatory Limit",
      // deliberately NO label.results.range.vector — must fall back
    },
  });

  it("uses the domain-suffixed key when it exists", () => {
    expect(
      domainMessageId(intl, "label.results.subject", "ENVIRONMENTAL"),
    ).toBe("label.results.subject.env");
    expect(domainMessageId(intl, "label.results.subject", "VECTOR")).toBe(
      "label.results.subject.vector",
    );
    expect(
      formatDomainMessage(intl, "label.results.range", "ENVIRONMENTAL"),
    ).toBe("Regulatory Limit");
  });

  it("falls back to the clinical key when no suffixed variant exists", () => {
    expect(domainMessageId(intl, "label.results.range", "VECTOR")).toBe(
      "label.results.range",
    );
    expect(formatDomainMessage(intl, "label.results.range", "VECTOR")).toBe(
      "Reference Range",
    );
  });

  it("CLINICAL always uses the base key", () => {
    expect(domainMessageId(intl, "label.results.subject", "CLINICAL")).toBe(
      "label.results.subject",
    );
  });

  it("normalizes unknown domains to CLINICAL", () => {
    expect(normalizeDomain("ENVIRONMENTAL")).toBe("ENVIRONMENTAL");
    expect(normalizeDomain("VECTOR")).toBe("VECTOR");
    expect(normalizeDomain("BOTH")).toBe("CLINICAL");
    expect(normalizeDomain(undefined)).toBe("CLINICAL");
    expect(normalizeDomain(null)).toBe("CLINICAL");
  });
});
