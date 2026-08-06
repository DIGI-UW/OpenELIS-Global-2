import React from "react";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { IntlProvider } from "react-intl";
import { vi } from "vitest";
import CaseTimelinePanel from "../CaseTimelinePanel";
import messages from "../../../languages/en.json";

describe("CaseTimelinePanel", () => {
  it("offers only a note action and labels system versus manual history", async () => {
    const user = userEvent.setup();
    const onAddNote = vi.fn().mockResolvedValue({});
    render(
      <IntlProvider locale="en" messages={messages}>
        <CaseTimelinePanel
          timelineSectionId="timeline"
          activities={[
            {
              id: "a1",
              activityType: "INOCULATION_RECORDED",
              note: "BOTTLE-001",
            },
            {
              id: "n1",
              activityType: "MANUAL_NOTE",
              note: "Plate remains negative",
            },
          ]}
          onAddNote={onAddNote}
        />
      </IntlProvider>,
    );

    expect(screen.queryByLabelText("Culture action")).not.toBeInTheDocument();
    expect(screen.getByText("Auto")).toBeInTheDocument();
    expect(screen.getByText("Manual")).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "Add note" }));
    await user.type(
      screen.getByLabelText("Note or observation"),
      "Colonies visible at 18 hours",
    );
    await user.click(screen.getByRole("button", { name: "Save note" }));

    expect(onAddNote).toHaveBeenCalledWith("Colonies visible at 18 hours");
  });

  it("shows the newest 30 events by default and preserves full history on demand", async () => {
    const user = userEvent.setup();
    const activities = Array.from({ length: 35 }, (_, index) => ({
      id: `activity-${index + 1}`,
      activityType: "AST_READING_RECORDED",
      note: `Timeline event ${index + 1}`,
    }));
    render(
      <IntlProvider locale="en" messages={messages}>
        <CaseTimelinePanel
          timelineSectionId="timeline"
          activities={activities}
          onAddNote={vi.fn()}
        />
      </IntlProvider>,
    );

    expect(screen.queryByText(": Timeline event 1")).not.toBeInTheDocument();
    expect(screen.getByText(": Timeline event 35")).toBeInTheDocument();
    await user.click(
      screen.getByRole("button", { name: "Show all 35 events" }),
    );
    expect(screen.getByText(": Timeline event 1")).toBeInTheDocument();
    await user.click(
      screen.getByRole("button", { name: "Show recent 30 events" }),
    );
    expect(screen.queryByText(": Timeline event 1")).not.toBeInTheDocument();
  });
});
