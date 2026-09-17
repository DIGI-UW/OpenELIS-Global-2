import React from "react";
import { render, screen } from "@testing-library/react";
import { IntlProvider } from "react-intl";
import { FlagChip, accentClass, isResultFlag } from "./flags";
import messages from "../../../languages/en.json";

/** OGC-1022 (R3) — FR-L1: flags are icon + tag + bold, never tint alone. */
describe("flags (FR-L1)", () => {
  const renderChip = (flag?: unknown) =>
    render(
      <IntlProvider locale="en" messages={messages}>
        <FlagChip flag={flag} />
      </IntlProvider>,
    );

  it("renders one chip per known flag with icon and label", () => {
    renderChip("CRITICAL");
    expect(screen.getByTestId("flag-CRITICAL")).toBeInTheDocument();
    expect(screen.getByText("Critical")).toBeInTheDocument();
    expect(
      screen.getByTestId("flag-CRITICAL").querySelector("svg"),
    ).not.toBeNull();
  });

  it("renders nothing for unknown or missing flags", () => {
    const { container } = renderChip(undefined);
    expect(container.querySelector(".unifiedFlagTag")).toBeNull();
    const { container: c2 } = renderChip("SOMETHING_ELSE");
    expect(c2.querySelector(".unifiedFlagTag")).toBeNull();
  });

  it("maps flags to their accent modifier and defaults safely", () => {
    expect(accentClass("NORMAL")).toContain("unifiedValueAccent--normal");
    expect(accentClass("ABNORMAL")).toContain("unifiedValueAccent--abnormal");
    expect(accentClass("CRITICAL")).toContain("unifiedValueAccent--critical");
    expect(accentClass("INVALID")).toContain("unifiedValueAccent--invalid");
    expect(accentClass(undefined)).toBe("unifiedValueAccent");
    expect(accentClass("bogus")).toBe("unifiedValueAccent");
  });

  it("isResultFlag accepts exactly the four flags", () => {
    expect(isResultFlag("NORMAL")).toBe(true);
    expect(isResultFlag("ABNORMAL")).toBe(true);
    expect(isResultFlag("CRITICAL")).toBe(true);
    expect(isResultFlag("INVALID")).toBe(true);
    expect(isResultFlag("normal")).toBe(false);
    expect(isResultFlag(null)).toBe(false);
  });
});
