import React from "react";
import { render, screen, within } from "@testing-library/react";
import "@testing-library/jest-dom";
import userEvent from "@testing-library/user-event";
import { IntlProvider } from "react-intl";
import { describe, it, expect, vi } from "vitest";
import messages from "../../languages/en.json";
import ProgressRail from "./ProgressRail";

const GROSS_EXAM = messages["pathology.label.grossexam"];
const BLOCKS = messages["pathology.label.blocks"];
const SLIDES = messages["pathology.label.slides"];
const PENDING = "cassette A4 outstanding";

const wrap = (props) => (
  <IntlProvider locale="en" messages={messages}>
    <ProgressRail {...props} />
  </IntlProvider>
);

const renderRail = (props) => {
  const result = render(wrap(props));
  return {
    ...result,
    rerenderRail: (nextProps) => result.rerender(wrap(nextProps)),
  };
};

// Carbon assembles a step's accessible name out of the label, the secondary
// label and its own state word ("Complete", "Current", "Incomplete"), so a
// step is found here by the stage label on screen and walked up to the button
// Carbon renders around it. No test then has to assume how that name comes
// out, which leaves the name assertions below free to prove something.
const step = (label) => screen.getByText(label).closest("button");
const row = (label) => step(label).closest("li");

const threeStages = [
  { id: "grossing", labelKey: "pathology.label.grossexam" },
  { id: "blocks", labelKey: "pathology.label.blocks" },
  { id: "slides", labelKey: "pathology.label.slides" },
];

describe("ProgressRail", () => {
  it("calls onNavigate with the id of the step that was clicked and with no other id", async () => {
    const user = userEvent.setup();
    const onNavigate = vi.fn();
    renderRail({ items: threeStages, currentIndex: 0, onNavigate });

    await user.click(step(SLIDES));

    expect(onNavigate).toHaveBeenCalledTimes(1);
    expect(onNavigate).toHaveBeenCalledWith("slides");
  });

  it("takes each step's completion from the item and only its position from currentIndex", () => {
    // The third stage is flagged complete although it sits after the current
    // one, and the second is not flagged although the case has reached it, so
    // neither assertion below can be satisfied by the rail's position alone.
    renderRail({
      items: [
        {
          id: "grossing",
          labelKey: "pathology.label.grossexam",
          complete: true,
        },
        { id: "blocks", labelKey: "pathology.label.blocks" },
        { id: "slides", labelKey: "pathology.label.slides", complete: true },
      ],
      currentIndex: 1,
    });

    expect(row(GROSS_EXAM)).toHaveClass("cds--progress-step--complete");
    expect(row(GROSS_EXAM)).not.toHaveClass("cds--progress-step--current");

    expect(row(BLOCKS)).toHaveClass("cds--progress-step--current");
    expect(row(BLOCKS)).not.toHaveClass("cds--progress-step--complete");

    expect(row(SLIDES)).toHaveClass("cds--progress-step--complete");
    expect(row(SLIDES)).not.toHaveClass("cds--progress-step--current");
  });

  it("does not report a stage this deployment does not track as completed once the case has moved past it", () => {
    // The rail must never state that bench work was done on a stage nobody
    // records. Carbon completes every step before the index it is given, so an
    // untracked stage early in the sequence is exactly where that claim would
    // appear: a checkmark and the word "Complete" beside its own "N/A".
    renderRail({
      items: [
        {
          id: "coverslipping",
          labelKey: "pathology.label.slides",
          notApplicable: true,
        },
        { id: "blocks", labelKey: "pathology.label.blocks" },
        { id: "grossing", labelKey: "pathology.label.grossexam" },
      ],
      currentIndex: 2,
    });

    const untracked = row(SLIDES);

    expect(untracked).not.toHaveClass("cds--progress-step--complete");
    expect(within(untracked).queryByText("Complete")).toBeNull();
    expect(within(untracked).getByText("Incomplete")).toBeInTheDocument();
    expect(
      within(untracked).getByText(messages["caseView.badge.notApplicable"]),
    ).toBeInTheDocument();

    // The case really has moved past it, so the assertions above are not
    // passing because the rail failed to place the case at all.
    expect(row(GROSS_EXAM)).toHaveClass("cds--progress-step--current");
  });

  it("keeps a stage this deployment does not track in the rail, disabled and marked n/a, rather than removing it", () => {
    // An untracked stage stays listed so the case reads the same shape on
    // every deployment; it is disabled in place, never hidden.
    renderRail({
      items: [
        { id: "grossing", labelKey: "pathology.label.grossexam" },
        {
          id: "coverslipping",
          labelKey: "pathology.label.slides",
          notApplicable: true,
        },
      ],
      currentIndex: 0,
    });

    const untracked = step(SLIDES);

    expect(untracked).toBeInTheDocument();
    expect(untracked).toBeDisabled();
    expect(
      within(untracked).getByText(messages["caseView.badge.notApplicable"]),
    ).toBeInTheDocument();
  });

  it("names what is pending in both the step's accessible name and its hover title", () => {
    renderRail({
      items: [
        { id: "grossing", labelKey: "pathology.label.grossexam" },
        {
          id: "blocks",
          labelKey: "pathology.label.blocks",
          pendingLabel: PENDING,
        },
      ],
      currentIndex: 0,
    });

    const pendingStep = step(BLOCKS);

    expect(pendingStep).toHaveAccessibleName(expect.stringContaining(PENDING));
    expect(pendingStep.getAttribute("title")).toBe(PENDING);
  });

  it("leaves a step with nothing pending unnamed by the pending text", () => {
    // Inversion test. The two steps differ only in that the first was given a
    // pendingLabel; if the naming text reached the accessible name from
    // anywhere else, the second step would carry it too.
    renderRail({
      items: [
        {
          id: "blocks",
          labelKey: "pathology.label.blocks",
          pendingLabel: PENDING,
        },
        { id: "slides", labelKey: "pathology.label.slides" },
      ],
      currentIndex: 0,
    });

    const quietStep = step(SLIDES);

    expect(step(BLOCKS)).toHaveAccessibleName(expect.stringContaining(PENDING));
    expect(quietStep).toHaveAccessibleName(expect.stringContaining(SLIDES));
    expect(quietStep).not.toHaveAccessibleName(
      expect.stringContaining(PENDING),
    );
    expect(quietStep).not.toHaveAttribute("title");
  });

  it("formats step labels through react-intl rather than showing a raw key", () => {
    renderRail({
      items: [{ id: "grossing", labelKey: "pathology.label.grossexam" }],
    });

    expect(screen.getByText(GROSS_EXAM)).toBeInTheDocument();
    expect(screen.queryByText("pathology.label.grossexam")).toBeNull();
  });

  it("labels the rail landmark and leaves the list itself to Carbon", () => {
    renderRail({ items: threeStages, currentIndex: 0 });

    const rail = screen.getByRole("navigation", {
      name: messages["caseView.label.caseProgress"],
    });

    expect(within(rail).getAllByRole("list")).toHaveLength(1);
    expect(within(rail).getAllByRole("listitem")).toHaveLength(3);
  });

  it("wires no step click handler at all when the screen passed no onNavigate", () => {
    // Without a handler every step would still carry Carbon's click affordance
    // and invite a technician to press something that does nothing.
    const { rerenderRail } = renderRail({
      items: threeStages,
      currentIndex: 0,
    });

    [GROSS_EXAM, BLOCKS, SLIDES].forEach((label) => {
      expect(step(label)).toHaveClass("cds--progress-step-button--unclickable");
    });

    rerenderRail({
      items: threeStages,
      currentIndex: 0,
      onNavigate: vi.fn(),
    });

    expect(step(SLIDES)).not.toHaveClass(
      "cds--progress-step-button--unclickable",
    );
  });

  it("still shows the step after it is clicked when no onNavigate handler was given", async () => {
    const user = userEvent.setup();
    renderRail({ items: threeStages, currentIndex: 0 });

    const target = step(SLIDES);
    await user.click(target);

    expect(target).toBeInTheDocument();
    expect(target).toHaveAccessibleName(expect.stringContaining(SLIDES));
  });

  it("navigates from the current step by keyboard but not by mouse, which is Carbon's behaviour and not this rail's", async () => {
    // Recorded rather than corrected: Carbon withholds the button's click
    // handler from the current step while its key handler still fires. The
    // adopting screen needs to know that the step the case is sitting on does
    // not respond to a click.
    const user = userEvent.setup();
    const onNavigate = vi.fn();
    renderRail({ items: threeStages, currentIndex: 1, onNavigate });

    const current = step(BLOCKS);

    await user.click(current);
    expect(onNavigate).not.toHaveBeenCalled();

    current.focus();
    await user.keyboard("{Enter}");
    expect(onNavigate).toHaveBeenCalledTimes(1);
    expect(onNavigate).toHaveBeenCalledWith("blocks");
  });
});
