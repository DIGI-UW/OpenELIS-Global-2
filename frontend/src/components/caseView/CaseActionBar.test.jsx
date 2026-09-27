import React from "react";
import { render, screen, fireEvent, act } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter, useHistory, useLocation } from "react-router-dom";
import { describe, it, expect, vi } from "vitest";
import messages from "../../languages/en.json";
import CaseActionBar from "./CaseActionBar";

const CASE_PATH = "/PathologyCaseView/1";
const ELSEWHERE_PATH = "/PathologyDashboard";

// The bar renders a router Prompt, so it only works inside a Router. Every
// screen that shows it is a routed screen; the tests have to supply the same
// thing rather than render the bar bare.
const renderBar = (props = {}) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <MemoryRouter initialEntries={[CASE_PATH]}>
        <CaseActionBar {...props} />
      </MemoryRouter>
    </IntlProvider>,
  );

// react-router v5 asks getUserConfirmation whenever a blocked navigation is
// attempted, and holds the navigation until the callback it is handed says
// to proceed. Driving the real router this way is what proves the guard
// blocks, rather than only proving a Prompt element was rendered.
const renderRoutedBar = (props = {}, getUserConfirmation) => {
  const captured = {};

  const Probe = () => {
    captured.history = useHistory();
    const { pathname } = useLocation();
    return <span data-testid="pathname">{pathname}</span>;
  };

  const tree = (barProps) => (
    <IntlProvider locale="en" messages={messages}>
      <MemoryRouter
        initialEntries={[CASE_PATH]}
        getUserConfirmation={getUserConfirmation}
      >
        <Probe />
        <CaseActionBar {...barProps} />
      </MemoryRouter>
    </IntlProvider>
  );

  const { rerender } = render(tree(props));

  return {
    navigateAway: () => act(() => captured.history.push(ELSEWHERE_PATH)),
    setProps: (nextProps) => rerender(tree({ ...props, ...nextProps })),
  };
};

describe("CaseActionBar", () => {
  it("renders exactly three buttons when a primary action is given", () => {
    // A fourth button is exactly the thing this component exists to
    // prevent: competing next steps are expressed as one relabelled
    // primary, never as an extra button beside it.
    renderBar({
      primary: { labelKey: "pathology.label.release", onClick: vi.fn() },
    });

    expect(screen.getAllByRole("button")).toHaveLength(3);
  });

  it("renders exactly two buttons when no primary action is given", () => {
    renderBar();

    expect(screen.getAllByRole("button")).toHaveLength(2);
  });

  it("calls onDiscard once when Discard changes is clicked", () => {
    const onDiscard = vi.fn();
    renderBar({ dirty: true, onDiscard });

    fireEvent.click(screen.getByText(messages["caseView.action.discard"]));

    expect(onDiscard).toHaveBeenCalledTimes(1);
  });

  it("calls onSaveDraft once when Save draft is clicked", () => {
    const onSaveDraft = vi.fn();
    renderBar({ onSaveDraft });

    fireEvent.click(screen.getByText(messages["caseView.action.saveDraft"]));

    expect(onSaveDraft).toHaveBeenCalledTimes(1);
  });

  it("calls the primary action's onClick once when it is clicked", () => {
    const onClick = vi.fn();
    renderBar({ primary: { labelKey: "pathology.label.release", onClick } });

    fireEvent.click(screen.getByText(messages["pathology.label.release"]));

    expect(onClick).toHaveBeenCalledTimes(1);
  });

  it("disables Discard changes on a clean form", () => {
    renderBar({ dirty: false });

    expect(
      screen.getByText(messages["caseView.action.discard"]).closest("button"),
    ).toBeDisabled();
  });

  it("enables Discard changes on a dirty form", () => {
    renderBar({ dirty: true });

    expect(
      screen.getByText(messages["caseView.action.discard"]).closest("button"),
    ).toBeEnabled();
  });

  it("shows the unsaved-changes text when the form is dirty", () => {
    renderBar({ dirty: true });

    expect(
      screen.getByText(messages["caseView.label.unsavedChanges"]),
    ).toBeInTheDocument();
  });

  it("shows no unsaved-changes text when the form is clean", () => {
    renderBar({ dirty: false });

    expect(
      screen.queryByText(messages["caseView.label.unsavedChanges"]),
    ).not.toBeInTheDocument();
  });

  it("puts the disabled primary's reason on its title and on the element its aria-describedby points at", () => {
    renderBar({
      primary: {
        labelKey: "pathology.label.release",
        disabledReasonKey: "caseView.locked.awaitingStage",
        disabledReasonValues: { stage: "Grossing" },
        onClick: vi.fn(),
      },
    });

    const reasonText = messages["caseView.locked.awaitingStage"].replace(
      "{stage}",
      "Grossing",
    );

    const primaryButton = screen
      .getByText(messages["pathology.label.release"])
      .closest("button");

    expect(primaryButton).toBeDisabled();
    expect(primaryButton).toHaveAttribute("title", reasonText);

    const describedById = primaryButton.getAttribute("aria-describedby");
    expect(describedById).toBeTruthy();

    const describedByElement = document.getElementById(describedById);
    expect(describedByElement).toHaveTextContent(reasonText);
  });

  // A primary with no reason key has no way to be disabled at all: there is
  // no separate disabled prop left to set. This is what makes a reasonless
  // disabled button structurally impossible rather than merely undocumented.
  it("keeps the primary enabled and clickable when no disabledReasonKey is given", () => {
    const onClick = vi.fn();
    renderBar({
      primary: {
        labelKey: "pathology.label.release",
        onClick,
      },
    });

    const primaryButton = screen
      .getByText(messages["pathology.label.release"])
      .closest("button");

    expect(primaryButton).toBeEnabled();

    fireEvent.click(primaryButton);

    expect(onClick).toHaveBeenCalledTimes(1);
  });

  // Inversion test: an enabled primary must never carry the leftover shape
  // of a disabled reason. If the reason text were always rendered, only
  // hidden by styling, this would still find it; it must find nothing.
  it("renders no reason text anywhere and no title when the primary has no disabledReasonKey", () => {
    renderBar({
      primary: {
        labelKey: "pathology.label.release",
        onClick: vi.fn(),
      },
    });

    const reasonText = messages["caseView.locked.awaitingStage"].replace(
      "{stage}",
      "Grossing",
    );

    expect(screen.queryByText(reasonText)).not.toBeInTheDocument();

    const primaryButton = screen
      .getByText(messages["pathology.label.release"])
      .closest("button");

    expect(primaryButton).not.toHaveAttribute("title");
    expect(primaryButton).not.toHaveAttribute("aria-describedby");
  });

  it("labels the standing buttons from the real translation bundle", () => {
    renderBar();

    expect(
      screen.getByText(messages["caseView.action.discard"]),
    ).toBeInTheDocument();
    expect(
      screen.getByText(messages["caseView.action.saveDraft"]),
    ).toBeInTheDocument();
  });

  it("shows the status label and the status content on the left of the bar", () => {
    renderBar({ status: "Ready for Pathologist" });

    expect(screen.getByText(messages["common.status"])).toBeInTheDocument();
    expect(screen.getByText("Ready for Pathologist")).toBeInTheDocument();
  });

  // The commonest way to lose a half-written case is not a reload but a
  // click in the side navigation, which never unloads the document and so
  // never reaches the beforeunload listener. The navigation has to be held,
  // not merely commented on afterwards, and it has to be held with the
  // wording the translators own.
  it("holds an in-app navigation away from a dirty case and asks with the unsaved-changes message", () => {
    const getUserConfirmation = vi.fn();
    const { navigateAway } = renderRoutedBar(
      { dirty: true },
      getUserConfirmation,
    );

    navigateAway();

    expect(getUserConfirmation).toHaveBeenCalledTimes(1);
    expect(getUserConfirmation).toHaveBeenCalledWith(
      messages["caseView.banner.unsavedChanges"],
      expect.any(Function),
    );
    expect(screen.getByTestId("pathname")).toHaveTextContent(CASE_PATH);
  });

  it("lets the held navigation finish once the warning is confirmed", () => {
    const getUserConfirmation = vi.fn((message, proceed) => proceed(true));
    const { navigateAway } = renderRoutedBar(
      { dirty: true },
      getUserConfirmation,
    );

    navigateAway();

    expect(screen.getByTestId("pathname")).toHaveTextContent(ELSEWHERE_PATH);
  });

  it("keeps the case where it is when the warning is declined", () => {
    const getUserConfirmation = vi.fn((message, proceed) => proceed(false));
    const { navigateAway } = renderRoutedBar(
      { dirty: true },
      getUserConfirmation,
    );

    navigateAway();

    expect(screen.getByTestId("pathname")).toHaveTextContent(CASE_PATH);
  });

  // Inversion test: a guard that warned whatever the form's state would pass
  // the tests above and be useless, because users learn to dismiss a warning
  // that always appears.
  it("lets an in-app navigation away from a clean case through without asking", () => {
    const getUserConfirmation = vi.fn();
    const { navigateAway } = renderRoutedBar(
      { dirty: false },
      getUserConfirmation,
    );

    navigateAway();

    expect(getUserConfirmation).not.toHaveBeenCalled();
    expect(screen.getByTestId("pathname")).toHaveTextContent(ELSEWHERE_PATH);
  });

  it("stops asking once the case has been saved, so the warning does not outlive the work it protects", () => {
    const getUserConfirmation = vi.fn();
    const { navigateAway, setProps } = renderRoutedBar(
      { dirty: true },
      getUserConfirmation,
    );

    setProps({ dirty: false });
    navigateAway();

    expect(getUserConfirmation).not.toHaveBeenCalled();
    expect(screen.getByTestId("pathname")).toHaveTextContent(ELSEWHERE_PATH);
  });
});
