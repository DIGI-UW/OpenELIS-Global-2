import React from "react";
import { render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { describe, it, expect } from "vitest";
import messages from "../../languages/en.json";
import StatusBadge, { BADGE_KINDS } from "./StatusBadge";

const renderBadge = (props) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <StatusBadge textKey="caseView.badge.readOnly" {...props} />
    </IntlProvider>,
  );

describe("StatusBadge", () => {
  // Every kind must render its meaning as visible text: colour alone fails
  // WCAG 2.1 AA for anyone who cannot tell the seven kinds apart.
  it.each(Object.keys(BADGE_KINDS))(
    "renders its text for the %s kind, not colour alone",
    (kind) => {
      renderBadge({ kind });

      expect(screen.getByText("Read only")).toBeInTheDocument();
    },
  );

  it("falls back to the neutral kind for an unknown kind, instead of an undefined Carbon type", () => {
    const { container } = renderBadge({ kind: "not-a-real-kind" });

    const tag = container.querySelector(`.cds--tag--${BADGE_KINDS.none}`);

    expect(tag).not.toBeNull();
    expect(screen.getByText("Read only")).toBeInTheDocument();
  });

  // The expected types below are written out literally rather than read
  // from BADGE_KINDS. Reading the mapping from the module under test would
  // make this test unable to fail: renaming a kind's colour in StatusBadge
  // would change the expectation along with the code, and the whole point
  // of this test is to pin that mapping down.
  it.each([
    ["complete", "green"],
    ["critical", "red"],
    ["inProgress", "blue"],
    ["pending", "purple"],
    ["verified", "teal"],
    ["partial", "warm-gray"],
    ["none", "gray"],
  ])("carries the Carbon tag class for the %s kind's type", (kind, type) => {
    const { container } = renderBadge({ kind });

    const tag = container.querySelector(`.cds--tag--${type}`);

    expect(tag).not.toBeNull();
  });
});
