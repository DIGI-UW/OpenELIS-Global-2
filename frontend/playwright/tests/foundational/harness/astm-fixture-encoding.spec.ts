import { test, expect } from "../../../helpers/test-base";

const SIMULATOR_URL = "http://localhost:8085";

/**
 * ASTM fixture encoding guard
 *
 * The GeneXpert fixture carries an accented patient name on purpose, so the
 * GeneXpert ASTM lane exercises the non-ASCII path over TCP. That matters
 * because ASTM frames a checksum over the bytes on the wire: a receiver that
 * mis-decodes a Latin-1 byte sums the wrong ones and NAKs the frame.
 *
 * MG-97 was exactly that failure, and the lane could not catch it — the fixture
 * was pure ASCII and the simulator encoded the TCP payload as ASCII, so the
 * accented bytes never reached the socket. The lane passed against a bridge that
 * was failing at a French-language site.
 *
 * These assertions fail if that coverage is removed, rather than letting the
 * lane quietly go back to being ASCII-only.
 */
function hasNonAscii(value: string): boolean {
  return [...value].some((character) => character.codePointAt(0)! > 0x7f);
}

test.describe("ASTM fixture encoding", () => {
  test("GeneXpert fixture generates a message containing non-ASCII text", async ({
    page,
  }) => {
    const response = await page.request.get(
      `${SIMULATOR_URL}/simulate/astm/genexpert_astm`,
    );
    expect(response.ok()).toBeTruthy();

    const message: string = (await response.json())?.message ?? "";
    expect(message.length).toBeGreaterThan(0);

    expect(
      hasNonAscii(message),
      "GeneXpert fixture must contain non-ASCII text so the ASTM lane covers the Latin-1 path (MG-97). " +
        `Generated message was: ${message}`,
    ).toBe(true);
  });

  test("the accented text survives on the patient record segment", async ({
    page,
  }) => {
    const response = await page.request.get(
      `${SIMULATOR_URL}/simulate/astm/genexpert_astm`,
    );
    const message: string = (await response.json())?.message ?? "";

    const patientSegment = message
      .split("\n")
      .find((segment) => segment.startsWith("P|"));

    expect(
      patientSegment,
      "generated message must carry a P record",
    ).toBeTruthy();
    expect(
      hasNonAscii(patientSegment ?? ""),
      `P record must keep its accented characters rather than having them replaced. Was: ${patientSegment}`,
    ).toBe(true);
    expect(
      patientSegment,
      "an accented character replaced by '?' means the payload was encoded as ASCII",
    ).not.toContain("?");
  });
});
