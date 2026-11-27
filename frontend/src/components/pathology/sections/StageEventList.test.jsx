import React from "react";
import { render, screen, within } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { describe, it, expect } from "vitest";
import messages from "../../../languages/en.json";
import StageEventList from "./StageEventList";

const events = [
  {
    id: 1,
    stage: "UNDER_REVIEW",
    direction: "FORWARD",
    enteredAt: "01/11/2026 09:00",
    enteredBy: "Doe, Jane",
  },
  {
    id: 2,
    stage: "COMPLETED",
    direction: "FORWARD",
    enteredAt: "02/11/2026 10:00",
    enteredBy: "Doe, Jane",
  },
  {
    id: 3,
    stage: "UNDER_REVIEW",
    direction: "REOPEN",
    enteredAt: "03/11/2026 11:00",
    enteredBy: "Doe, Jane",
    reason: "Margin reported wrongly",
  },
];

const renderList = (props) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <StageEventList
        events={events}
        ariaLabelKey="pathology.label.stageHistory"
        {...props}
      />
    </IntlProvider>,
  );

describe("StageEventList", () => {
  it("aReopenEvent_isTaggedReopened_withItsReason", () => {
    renderList();

    const rows = screen.getAllByRole("row");
    // The head row, then the three events in order.
    const reopened = rows[3];
    expect(within(reopened).getByText("Reopened")).toBeInTheDocument();
    expect(
      within(reopened).getByText("Margin reported wrongly"),
    ).toBeInTheDocument();
    expect(within(reopened).queryByText("Returned")).not.toBeInTheDocument();
    // A forward move carries no tag.
    expect(within(rows[2]).queryByText("Reopened")).not.toBeInTheDocument();
    expect(screen.getAllByText("Reopened")).toHaveLength(1);
  });

  it("aReopenEvent_inAOneStageList_keepsItsTagAndReasonWithTheTime", () => {
    renderList({ showStage: false });

    const rows = screen.getAllByRole("row");
    const reopened = rows[3];
    expect(within(reopened).getByText("Reopened")).toBeInTheDocument();
    expect(
      within(reopened).getByText("Margin reported wrongly"),
    ).toBeInTheDocument();
  });
});
