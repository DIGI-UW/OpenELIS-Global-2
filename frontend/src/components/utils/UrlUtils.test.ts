import { describe, expect, it } from "vitest";
import { safeInternalPath } from "./UrlUtils";

describe("safeInternalPath", () => {
  it("preserves internal paths with query state", () => {
    expect(
      safeInternalPath("/analyzers/types/profile/mapping?revision=2"),
    ).toBe("/analyzers/types/profile/mapping?revision=2");
  });

  it("rejects protocol-relative and absolute return targets", () => {
    expect(safeInternalPath("//evil.example", "/analyzers/types")).toBe(
      "/analyzers/types",
    );
    expect(safeInternalPath("https://evil.example", "/analyzers/types")).toBe(
      "/analyzers/types",
    );
  });
});
