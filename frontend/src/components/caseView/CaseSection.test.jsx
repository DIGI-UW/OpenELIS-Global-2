import React from "react";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import { IntlProvider, createIntl } from "react-intl";
import { Accordion } from "@carbon/react";
import { describe, it, expect } from "vitest";
import messages from "../../languages/en.json";
import CaseSection from "./CaseSection";
import { SECTION_STATE } from "./sectionState";

const CHILD_TEXT = "section body content";
const testIntl = createIntl({ locale: "en", messages });

const buildTree = (props) => (
  <IntlProvider locale="en" messages={messages}>
    <Accordion>
      <CaseSection number={1} titleKey="pathology.label.grossexam" {...props}>
        <p>{CHILD_TEXT}</p>
      </CaseSection>
    </Accordion>
  </IntlProvider>
);

const renderSection = (props) => render(buildTree(props));

describe("CaseSection", () => {
  it("flips aria-expanded on the header when the user clicks it", async () => {
    const user = userEvent.setup();
    renderSection({ state: SECTION_STATE.OPEN, defaultOpen: false });

    const header = screen.getByRole("button", { name: /gross exam/i });
    expect(header).toHaveAttribute("aria-expanded", "false");

    await user.click(header);

    expect(header).toHaveAttribute("aria-expanded", "true");
  });

  it("ignores a click on a disabled section's header, leaving aria-expanded as it was", async () => {
    const user = userEvent.setup();
    renderSection({
      state: SECTION_STATE.DISABLED,
      lockedHintKey: "caseView.locked.awaitingStage",
      lockedHintValues: { stage: "Microtomy" },
      defaultOpen: false,
    });

    const header = screen.getByRole("button", { name: /gross exam/i });
    expect(header).toHaveAttribute("aria-expanded", "false");

    await user.click(header);

    expect(header).toHaveAttribute("aria-expanded", "false");
  });

  // A section becoming complete must not take the user's expansion away
  // mid-edit: the case advancing past a section's stage is not a click, and
  // must not snap an accordion the user deliberately opened shut under them
  // (nor reopen one they had deliberately collapsed). Neither render passes
  // defaultOpen, so the seed comes from `state` alone, the way the adopting
  // screen actually uses this component.
  it("keeps a section the user collapsed collapsed once its state moves from open to complete", async () => {
    const user = userEvent.setup();
    const { rerender } = renderSection({ state: SECTION_STATE.OPEN });

    const header = screen.getByRole("button", { name: /gross exam/i });
    expect(header).toHaveAttribute("aria-expanded", "true");

    await user.click(header);
    expect(header).toHaveAttribute("aria-expanded", "false");

    rerender(buildTree({ state: SECTION_STATE.COMPLETE }));

    expect(header).toHaveAttribute("aria-expanded", "false");
  });

  it("keeps a section the user left open open once its state moves from open to complete", () => {
    const { rerender } = renderSection({ state: SECTION_STATE.OPEN });

    const header = screen.getByRole("button", { name: /gross exam/i });
    expect(header).toHaveAttribute("aria-expanded", "true");

    rerender(buildTree({ state: SECTION_STATE.COMPLETE }));

    expect(header).toHaveAttribute("aria-expanded", "true");
  });

  it("lets keyboard tab reach an enabled section's header", async () => {
    const user = userEvent.setup();
    renderSection({ state: SECTION_STATE.OPEN });

    const header = screen.getByRole("button", { name: /gross exam/i });
    await user.tab();

    expect(header).toHaveFocus();
  });

  it("does not let keyboard tab reach a disabled section's header", async () => {
    const user = userEvent.setup();
    renderSection({
      state: SECTION_STATE.DISABLED,
      lockedHintKey: "caseView.locked.awaitingStage",
      lockedHintValues: { stage: "Microtomy" },
    });

    const header = screen.getByRole("button", { name: /gross exam/i });
    expect(header).toBeDisabled();

    await user.tab();

    expect(header).not.toHaveFocus();
    expect(document.body).toHaveFocus();
  });

  // A section is never removed from the DOM on the basis of state or role,
  // so its children must be present no matter which of the four states the
  // section is rendered in.
  it.each([
    SECTION_STATE.OPEN,
    SECTION_STATE.COMPLETE,
    SECTION_STATE.DISABLED,
    SECTION_STATE.READ_ONLY,
  ])("keeps its children in the document while in state %s", (state) => {
    renderSection({
      state,
      lockedHintKey: "caseView.locked.awaitingStage",
      lockedHintValues: { stage: "Microtomy" },
    });

    expect(screen.getByText(CHILD_TEXT)).toBeInTheDocument();
  });

  // Carbon never shows a disabled AccordionItem's body (its wrapper only
  // becomes visible when the item is both open and not disabled), so the
  // unlock reason is only ever readable from the header.
  it("states the section's unlock condition instead of the bare word Locked, in the header", () => {
    renderSection({
      state: SECTION_STATE.DISABLED,
      lockedHintKey: "caseView.locked.awaitingStage",
      lockedHintValues: { stage: "Microtomy" },
    });

    expect(
      screen.getByText(/available once the case reaches microtomy/i),
    ).toBeInTheDocument();
    expect(screen.queryByText(/^locked$/i)).not.toBeInTheDocument();
  });

  it("interpolates the caller's lockedHintKey and values into the rendered sentence", () => {
    renderSection({
      state: SECTION_STATE.DISABLED,
      lockedHintKey: "caseView.locked.awaitingStage",
      lockedHintValues: { stage: "Microtomy" },
    });

    const headerHint = screen.getByText(/available once the case reaches/i);

    expect(headerHint).toHaveTextContent("Microtomy");
  });

  it("shows the read-only badge text when the section is read-only and no badge was given", () => {
    renderSection({ state: SECTION_STATE.READ_ONLY });

    expect(screen.getByText("Read only")).toBeInTheDocument();
  });

  it("shows the caller's badge text instead of the read-only badge when both could apply", () => {
    renderSection({
      state: SECTION_STATE.READ_ONLY,
      badge: { kind: "complete", textKey: "caseView.label.caseSummary" },
    });

    expect(screen.getByText("Case summary")).toBeInTheDocument();
    expect(screen.queryByText("Read only")).not.toBeInTheDocument();
  });

  // Inversion test: the assertions above (locked hint present, read-only
  // badge present) must NOT hold for a plain open section with no badge.
  // Otherwise the two prior tests could be passing for the wrong reason.
  it("renders neither a read-only badge nor a locked hint for an open section with no badge", () => {
    renderSection({ state: SECTION_STATE.OPEN });

    expect(screen.queryByText("Read only")).not.toBeInTheDocument();
    expect(screen.queryByText(/available once/i)).not.toBeInTheDocument();
  });

  it("formats the section number and title through the numbered-section message, not a hardcoded separator", () => {
    renderSection({ number: 3, state: SECTION_STATE.OPEN });

    const expectedTitle = testIntl.formatMessage(
      { id: "caseView.label.numberedSection" },
      {
        number: 3,
        title: testIntl.formatMessage({ id: "pathology.label.grossexam" }),
      },
    );

    expect(
      screen.getByRole("button", { name: expectedTitle }),
    ).toBeInTheDocument();
  });

  it("puts the given id on the rendered section, for a progress rail to scroll to", () => {
    renderSection({ id: "section-grossing" });

    const header = screen.getByRole("button", { name: /gross exam/i });
    const sectionElement = document.getElementById("section-grossing");

    expect(sectionElement).not.toBeNull();
    expect(sectionElement).toContainElement(header);
  });

  it("shows the generic prerequisite hint when the section is disabled with no locked-hint key of its own", () => {
    renderSection({ state: SECTION_STATE.DISABLED });

    expect(
      screen.getByText(messages["caseView.locked.prerequisite"]),
    ).toBeInTheDocument();
  });
});
