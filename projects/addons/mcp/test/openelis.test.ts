import { describe, expect, it } from "vitest";
import { sanitizeActor, sanitizePurpose } from "../src/openelis.js";

describe("headers to OpenELIS", () => {
  it("folds accents and keeps printable ASCII, at most 100", () => {
    expect(sanitizeActor("agent:a7f3 Claude Code — Bác sĩ Đặng Lan\n")).toBe(
      "agent:a7f3 Claude Code Bac si Dang Lan",
    );
    expect(sanitizeActor("x".repeat(150)).length).toBe(100);
    expect(sanitizePurpose("get_order\r\nX: y")).toBe("get_orderX:y");
  });
});
