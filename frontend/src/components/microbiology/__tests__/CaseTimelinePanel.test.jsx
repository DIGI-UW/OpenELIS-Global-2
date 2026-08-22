import React from "react";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { IntlProvider } from "react-intl";
import { vi } from "vitest";
import CaseTimelinePanel from "../CaseTimelinePanel";
import messages from "../../../languages/en.json";

describe("CaseTimelinePanel", () => {
  it("moves focus into a note and restores it after cancel", async () => {
    const user = userEvent.setup();
    render(
      <IntlProvider locale="en" messages={messages}>
        <CaseTimelinePanel
          timelineSectionId="timeline"
          activities={[]}
          onAddNote={vi.fn()}
        />
      </IntlProvider>,
    );

    const trigger = screen.getByRole("button", { name: "Add note" });
    trigger.focus();
    await user.keyboard("{Enter}");

    expect(screen.getByLabelText("Note or observation")).toHaveFocus();
    expect(screen.getByRole("status")).toHaveTextContent("Note form expanded");
    await user.keyboard("{Escape}");
    expect(trigger).toHaveFocus();
  });

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
              occurredAt: "2026-08-13T17:15:00Z",
              performedByDisplay: "Olivia Mendez",
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
    const actor = screen.getByText(/Olivia Mendez/);
    const recordedAt = actor.closest("div").querySelector("time");
    expect(recordedAt).not.toBeNull();
    expect(recordedAt).toHaveAttribute("datetime", "2026-08-13T17:15:00Z");
    await user.click(screen.getByRole("button", { name: "Add note" }));
    await user.type(
      screen.getByLabelText("Note or observation"),
      "Colonies visible at 18 hours",
    );
    await user.click(screen.getByRole("button", { name: "Save note" }));

    expect(onAddNote).toHaveBeenCalledWith("Colonies visible at 18 hours");
    expect(screen.getByRole("button", { name: "Add note" })).toHaveFocus();
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
    const showRecent = screen.getByRole("button", {
      name: "Show recent 30 events",
    });
    expect(showRecent).toHaveFocus();
    await user.keyboard("{Enter}");
    expect(showRecent).toHaveFocus();
    expect(screen.queryByText(": Timeline event 1")).not.toBeInTheDocument();
  });

  it("renders an audited culture-purpose correction from structured history", () => {
    render(
      <IntlProvider locale="en" messages={messages}>
        <CaseTimelinePanel
          timelineSectionId="timeline"
          activities={[
            {
              id: "purpose-1",
              activityType: "CULTURE_PURPOSE_CHANGED",
              note: "Culture purpose changed",
              structuredData: JSON.stringify({
                fromPurpose: "CLINICAL_DIAGNOSTIC",
                toPurpose: "ACTIVE_SCREENING",
              }),
            },
          ]}
          onAddNote={vi.fn()}
        />
      </IntlProvider>,
    );

    expect(screen.getByText("Culture purpose changed")).toBeInTheDocument();
    expect(
      screen.getByText(
        "Clinical diagnosis or treatment to Active screening or carriage",
      ),
    ).toBeInTheDocument();
  });
});
