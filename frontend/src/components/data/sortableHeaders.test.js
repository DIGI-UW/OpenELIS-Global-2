import { describe, expect, it } from "vitest";
import { createIntl } from "react-intl";
import messages from "../../languages/en.json";
import { OrderCurrentTestsHeaders } from "./orderCurrentTestsHeaders";
import { auditTrailHeaderData } from "../reports/auditTrailReport/AuditTrailTableHeader";

/**
 * OGC-1448: a sortable Carbon column describes its sort from the header text,
 * so header data handed to a sortable table must be translated strings. An
 * element read "Click to sort rows by [object Object] header".
 */
const intl = createIntl({ locale: "en", messages });

describe.each([
  ["Edit Order current tests", OrderCurrentTestsHeaders],
  ["Audit trail", auditTrailHeaderData],
])("%s headers", (_, headersFor) => {
  it("are translated strings", () => {
    const headers = headersFor(intl);

    expect(headers.length).toBeGreaterThan(0);
    headers.forEach(({ key, header }) => {
      expect(typeof header, key).toBe("string");
      expect(header, key).toBe(header.trim());
      expect(header.length, key).toBeGreaterThan(0);
    });
  });
});
