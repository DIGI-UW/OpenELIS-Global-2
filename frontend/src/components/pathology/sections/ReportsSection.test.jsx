/**
 * The Reports section (FR-15): the list of report versions, the generate and
 * upload controls above it, and the withdrawal of an uploaded file.
 *
 * The section owns no request: it is handed the rows and the callbacks, so
 * these tests pin what it shows, what it calls, and what it holds.
 */
import React from "react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../languages/en.json";
import ReportsSection from "./ReportsSection";

const row = (version, type, state = "CURRENT", extra = {}) => ({
  id: 100 + version,
  version,
  type,
  state,
  generatedAt: `26/11/2026 10:0${version}`,
  generatedBy: "Moses, Olara",
  fileType: "data:application/pdf",
  ...extra,
});

const fileUrl = (id) => `/files/${id}`;

const renderSection = (props = {}) => {
  const callbacks = {
    onGenerate: vi.fn(),
    onUpload: vi.fn(),
    onWithdraw: vi.fn(),
  };
  const all = {
    reports: [],
    canGenerate: true,
    dirty: false,
    busy: false,
    fileUrl,
    uploadError: undefined,
    ...callbacks,
    ...props,
  };
  const view = render(
    <IntlProvider locale="en" messages={messages}>
      <ReportsSection {...all} />
    </IntlProvider>,
  );
  const rerender = (next) =>
    view.rerender(
      <IntlProvider locale="en" messages={messages}>
        <ReportsSection {...all} {...next} />
      </IntlProvider>,
    );
  return { ...view, ...callbacks, ...all, rerender };
};

// By its test id: jsdom names a disabled button with a title by the title,
// where a browser names it by its words.
const generateButton = () => screen.getByTestId("report-generate");
const uploadButton = () =>
  screen.getByRole("button", { name: "Upload report" });
const fileInput = (container) => container.querySelector('input[type="file"]');

const SAVE_FIRST = "Save the case before generating a report.";
const NEEDS_FINDINGS =
  "Enter the macroscopic and microscopic descriptions and at least one conclusion.";

describe("ReportsSection", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("noRows_showTheEmptySentence_andBothButtons", () => {
    renderSection();

    expect(
      screen.getByText(
        "No reports yet. Complete the findings and conclusion to generate one.",
      ),
    ).toBeInTheDocument();
    expect(generateButton()).toBeEnabled();
    expect(uploadButton()).toBeEnabled();
    expect(screen.queryByTestId("reports-table")).not.toBeInTheDocument();
  });

  it("generate_isHeldByUnsavedWork_withThatReason", () => {
    // Unsaved work is named before missing findings: saving comes first.
    renderSection({ dirty: true, canGenerate: false });

    expect(generateButton()).toBeDisabled();
    expect(generateButton()).toHaveAttribute("title", SAVE_FIRST);
    expect(screen.getByText(SAVE_FIRST)).toBeInTheDocument();
    expect(screen.queryByText(NEEDS_FINDINGS)).not.toBeInTheDocument();
  });

  it("generate_isHeldByMissingFindings_withThatReason", () => {
    renderSection({ canGenerate: false });

    expect(generateButton()).toBeDisabled();
    expect(generateButton()).toHaveAttribute("title", NEEDS_FINDINGS);
    expect(screen.getByText(NEEDS_FINDINGS)).toBeInTheDocument();
  });

  it("theFirstGenerate_doesNotAsk", async () => {
    // An uploaded file is not a generated version: nothing would read
    // superseded, so nothing is asked.
    const { onGenerate } = renderSection({
      reports: [row(1, "EXTERNAL")],
    });

    await userEvent.click(generateButton());

    expect(onGenerate).toHaveBeenCalledTimes(1);
    expect(screen.queryByRole("group")).not.toBeInTheDocument();
    expect(generateButton()).toHaveFocus();
  });

  it("aLaterGenerate_asksAndNamesTheVersion", async () => {
    const { onGenerate } = renderSection({
      reports: [
        row(3, "EXTERNAL"),
        row(1, "DRAFT", "SUPERSEDED"),
        row(2, "FINAL"),
      ],
    });

    await userEvent.click(generateButton());

    expect(onGenerate).not.toHaveBeenCalled();
    const question = screen.getByRole("group", {
      name: "Generate a new version? Version 2 stays in the list as superseded.",
    });

    // Cancel sends nothing and hands focus back to Generate.
    await userEvent.click(
      within(question).getByRole("button", { name: "Cancel" }),
    );
    expect(onGenerate).not.toHaveBeenCalled();
    expect(screen.queryByRole("group")).not.toBeInTheDocument();
    expect(generateButton()).toHaveFocus();

    await userEvent.click(generateButton());
    await userEvent.click(
      screen.getByRole("button", { name: "Generate new version" }),
    );
    expect(onGenerate).toHaveBeenCalledTimes(1);
    expect(screen.queryByRole("group")).not.toBeInTheDocument();
  });

  it("aReopenedCase_whoseOnlyGeneratedVersionIsVoided_generatesWithoutAsking", async () => {
    // A voided version is never superseded: the question would be false, so
    // the generate goes as a first one does.
    const { onGenerate } = renderSection({
      reports: [row(2, "EXTERNAL"), row(1, "FINAL", "VOIDED")],
    });

    await userEvent.click(generateButton());

    expect(onGenerate).toHaveBeenCalledTimes(1);
    expect(screen.queryByRole("group")).not.toBeInTheDocument();
    expect(
      screen.queryByText(/stays in the list as superseded/),
    ).not.toBeInTheDocument();
  });

  it("aReopenedCase_withASupersededDraftAndAVoidedFinal_generatesWithoutAsking", async () => {
    // Only a current version would read superseded after a new one, and none
    // is current here, so nothing is asked.
    const { onGenerate } = renderSection({
      reports: [row(2, "FINAL", "VOIDED"), row(1, "DRAFT", "SUPERSEDED")],
    });

    await userEvent.click(generateButton());

    expect(onGenerate).toHaveBeenCalledTimes(1);
    expect(screen.queryByRole("group")).not.toBeInTheDocument();
    expect(
      screen.queryByText(/stays in the list as superseded/),
    ).not.toBeInTheDocument();
  });

  it("rows_showTypeAndStateInWords_newestFirst", () => {
    renderSection({
      reports: [
        row(1, "DRAFT", "SUPERSEDED"),
        row(3, "EXTERNAL"),
        row(2, "FINAL", "VOIDED"),
        row(4, "FINAL"),
      ],
    });

    const table = screen.getByTestId("reports-table");
    expect(
      within(table)
        .getAllByRole("columnheader")
        .map((header) => header.textContent),
    ).toEqual(["Version", "Generated", "By", "Type", "Actions"]);
    expect(
      screen
        .getAllByTestId(/^report-row-/)
        .map((element) => element.getAttribute("data-testid")),
    ).toEqual(["report-row-4", "report-row-3", "report-row-2", "report-row-1"]);

    const words = (version) =>
      within(screen.getByTestId(`report-row-${version}`));
    expect(words(4).getByText("Final")).toBeInTheDocument();
    expect(words(4).queryByText("Superseded")).not.toBeInTheDocument();
    expect(words(3).getByText("Uploaded")).toBeInTheDocument();
    expect(words(2).getByText("Final")).toBeInTheDocument();
    expect(words(2).getByText("Voided")).toBeInTheDocument();
    expect(words(1).getByText("Draft")).toBeInTheDocument();
    expect(words(1).getByText("Superseded")).toBeInTheDocument();
    expect(words(1).getByText("26/11/2026 10:01")).toBeInTheDocument();
    expect(words(1).getByText("Moses, Olara")).toBeInTheDocument();
  });

  it("aRowClick_opensItsFileInANewTab", async () => {
    const open = vi.spyOn(window, "open").mockReturnValue(null);
    renderSection({
      reports: [row(2, "FINAL"), row(1, "DRAFT", "SUPERSEDED")],
    });

    await userEvent.click(
      within(screen.getByTestId("report-row-1")).getByText("Moses, Olara"),
    );

    expect(open).toHaveBeenCalledTimes(1);
    expect(open).toHaveBeenCalledWith(
      "/files/101",
      "_blank",
      "noopener,noreferrer",
    );
  });

  it("theVersionLink_isARealLink_namedForANewTab", async () => {
    const open = vi.spyOn(window, "open").mockReturnValue(null);
    renderSection({ reports: [row(2, "FINAL"), row(1, "FINAL", "VOIDED")] });

    const link = screen.getByRole("link", {
      name: "Open report version 2 in a new tab",
    });
    expect(link.tagName).toBe("A");
    expect(link).toHaveAttribute("href", "/files/102");
    expect(link).toHaveAttribute("target", "_blank");
    expect(link).toHaveAttribute("rel", "noopener noreferrer");
    expect(link).toHaveTextContent("v2");

    const voided = screen.getByRole("link", {
      name: "Open voided report version 1 in a new tab",
    });
    expect(voided).toHaveAttribute("href", "/files/101");

    // The link opens the file itself; the row does not open it again.
    link.addEventListener("click", (event) => event.preventDefault());
    await userEvent.click(link);
    expect(open).not.toHaveBeenCalled();
  });

  it("noRow_hasViewDownloadPrintOrEmail", () => {
    renderSection({
      reports: [
        row(4, "EXTERNAL"),
        row(3, "FINAL"),
        row(2, "DRAFT", "SUPERSEDED"),
        row(1, "EXTERNAL", "VOIDED"),
      ],
    });

    for (const version of [4, 3, 2, 1]) {
      const inRow = within(screen.getByTestId(`report-row-${version}`));
      const names = inRow
        .queryAllByRole("button")
        .map((button) => button.textContent);
      expect(
        names.filter((name) => /view|download|print|email/i.test(name)),
      ).toEqual([]);
      // Only a current upload carries a control besides its link.
      expect(names).toEqual(version === 4 ? ["Withdraw"] : []);
    }
    expect(
      screen.queryByRole("button", { name: /view|download|print|email/i }),
    ).not.toBeInTheDocument();
  });

  it("aChosenFile_isSentAtOnce_andARefusalPrintsBesideTheUploader", async () => {
    const { container, onUpload, onGenerate, rerender } = renderSection();
    const file = new File(["%PDF-1.4 report"], "outside.pdf", {
      type: "application/pdf",
    });

    await userEvent.upload(fileInput(container), file);

    await waitFor(() => expect(onUpload).toHaveBeenCalledTimes(1));
    expect(onUpload).toHaveBeenCalledWith({
      base64Image: expect.stringMatching(/^data:application\/pdf;base64,/),
    });
    expect(onGenerate).not.toHaveBeenCalled();
    // The uploader is cleared for the next file.
    await waitFor(() =>
      expect(screen.queryByText("outside.pdf")).not.toBeInTheDocument(),
    );

    const refusal = "The file must be a PDF, PNG or JPEG of at most 10 MB.";
    rerender({ uploadError: refusal });
    const printed = screen.getByRole("alert");
    expect(printed).toHaveTextContent(refusal);
    expect(screen.getByTestId("report-upload")).toContainElement(printed);
  });

  it("withdraw_showsOnlyOnACurrentUpload_andAsksFirst", async () => {
    const { onWithdraw, unmount } = renderSection({
      reports: [
        row(3, "EXTERNAL"),
        row(2, "EXTERNAL", "VOIDED"),
        row(1, "FINAL"),
      ],
    });

    expect(screen.getByTestId("report-withdraw-3")).toBeInTheDocument();
    expect(screen.queryByTestId("report-withdraw-2")).not.toBeInTheDocument();
    expect(screen.queryByTestId("report-withdraw-1")).not.toBeInTheDocument();

    await userEvent.click(screen.getByTestId("report-withdraw-3"));
    expect(onWithdraw).not.toHaveBeenCalled();
    let question = screen.getByRole("group", {
      name: "Withdraw version 3? It stays in the list as voided.",
    });

    await userEvent.click(
      within(question).getByRole("button", { name: "Cancel" }),
    );
    expect(onWithdraw).not.toHaveBeenCalled();
    await waitFor(() =>
      expect(screen.getByTestId("report-withdraw-3")).toHaveFocus(),
    );

    await userEvent.click(screen.getByTestId("report-withdraw-3"));
    question = screen.getByRole("group", {
      name: "Withdraw version 3? It stays in the list as voided.",
    });
    await userEvent.click(
      within(question).getByRole("button", { name: "Withdraw" }),
    );
    expect(onWithdraw).toHaveBeenCalledTimes(1);
    expect(onWithdraw).toHaveBeenCalledWith(103);
    unmount();

    // No current upload: no Actions column at all.
    renderSection({ reports: [row(2, "EXTERNAL", "VOIDED"), row(1, "FINAL")] });
    expect(
      screen.queryByRole("columnheader", { name: "Actions" }),
    ).not.toBeInTheDocument();
  });

  it("theWithdrawClick_doesNotOpenTheFile", async () => {
    const open = vi.spyOn(window, "open").mockReturnValue(null);
    const { onWithdraw } = renderSection({
      reports: [row(2, "EXTERNAL"), row(1, "FINAL")],
    });

    await userEvent.click(screen.getByTestId("report-withdraw-2"));
    await userEvent.click(screen.getByRole("button", { name: "Cancel" }));
    await waitFor(() =>
      expect(screen.getByTestId("report-withdraw-2")).toHaveFocus(),
    );
    await userEvent.click(screen.getByTestId("report-withdraw-2"));
    await userEvent.click(screen.getByRole("button", { name: "Withdraw" }));

    expect(onWithdraw).toHaveBeenCalledWith(102);
    expect(open).not.toHaveBeenCalled();
  });

  it("busy_disablesEveryControl", async () => {
    const { container, rerender } = renderSection({
      reports: [row(3, "EXTERNAL"), row(2, "EXTERNAL"), row(1, "DRAFT")],
    });
    // Both questions open, so their buttons are held too; version 2 keeps
    // its own Withdraw button on the screen.
    await userEvent.click(generateButton());
    await userEvent.click(screen.getByTestId("report-withdraw-3"));

    // Unsaved work as well: a send in flight is named first, with no words.
    rerender({ busy: true, dirty: true });

    const buttons = screen.getAllByRole("button");
    expect(buttons.map((button) => button.textContent)).toEqual(
      expect.arrayContaining([
        "Generate report",
        "Upload report",
        "Generate new version",
        "Withdraw",
        "Cancel",
      ]),
    );
    expect(screen.getByTestId("report-withdraw-2")).toBeInTheDocument();
    for (const button of buttons) {
      expect(button).toBeDisabled();
    }
    expect(fileInput(container)).toBeDisabled();
    expect(screen.queryByText(SAVE_FIRST)).not.toBeInTheDocument();
    // In flight is not a reason to explain: the answer is on its way.
    expect(generateButton()).not.toHaveAttribute("aria-describedby");
    expect(generateButton()).not.toHaveAttribute("title");
  });

  it("focus_returnsToGenerate_whenTheSendEnds", async () => {
    // A browser drops focus from a button that is disabled under it; jsdom
    // keeps it, and will not blur a disabled button either, so the test
    // drops it by hand just before the send holds the section.
    const dropFocus = () => document.activeElement?.blur();

    // The first generate.
    let view = renderSection({ reports: [row(1, "EXTERNAL")] });
    await userEvent.click(generateButton());
    dropFocus();
    view.rerender({ busy: true });
    expect(document.body).toHaveFocus();
    view.rerender({ busy: false });
    expect(generateButton()).toHaveFocus();
    view.unmount();

    // A new version, through the question.
    view = renderSection({ reports: [row(1, "DRAFT")] });
    await userEvent.click(generateButton());
    await userEvent.click(
      screen.getByRole("button", { name: "Generate new version" }),
    );
    dropFocus();
    view.rerender({ busy: true });
    view.rerender({ busy: false });
    expect(generateButton()).toHaveFocus();
    view.unmount();

    // Focus moved on purpose while the send was out stays where it was put.
    view = renderSection({ reports: [row(1, "EXTERNAL")] });
    await userEvent.click(generateButton());
    view.rerender({ busy: true });
    const link = screen.getByRole("link", {
      name: "Open report version 1 in a new tab",
    });
    link.focus();
    view.rerender({ busy: false });
    expect(link).toHaveFocus();
    view.unmount();

    // A send this section did not start takes no focus.
    dropFocus();
    view = renderSection({ busy: true });
    view.rerender({ busy: false });
    expect(document.body).toHaveFocus();
    view.unmount();

    // An upload hands focus back to the uploader's button.
    view = renderSection();
    await userEvent.upload(
      fileInput(view.container),
      new File(["%PDF-1.4 report"], "outside.pdf", { type: "application/pdf" }),
    );
    await waitFor(() => expect(view.onUpload).toHaveBeenCalledTimes(1));
    dropFocus();
    view.rerender({ busy: true });
    view.rerender({ busy: false });
    expect(uploadButton()).toHaveFocus();
  });

  it("focus_goesToTheRowsLink_afterAWithdrawal", async () => {
    const listed = [row(2, "EXTERNAL"), row(1, "FINAL")];
    // The confirm button leaves with its question, which drops focus.
    const withdraw = async (view) => {
      await userEvent.click(screen.getByTestId("report-withdraw-2"));
      await userEvent.click(screen.getByRole("button", { name: "Withdraw" }));
      document.activeElement?.blur();
      view.rerender({ busy: true });
    };

    // Withdrawn: the row's link, now named voided.
    let view = renderSection({ reports: listed });
    await withdraw(view);
    expect(document.body).toHaveFocus();
    view.rerender({
      busy: false,
      reports: [row(2, "EXTERNAL", "VOIDED"), row(1, "FINAL")],
    });
    expect(
      screen.getByRole("link", {
        name: "Open voided report version 2 in a new tab",
      }),
    ).toHaveFocus();
    view.unmount();

    // Refused: the row is as it was, and its link is still the target.
    view = renderSection({ reports: listed });
    await withdraw(view);
    view.rerender({ busy: false });
    expect(screen.getByTestId("report-withdraw-2")).toBeInTheDocument();
    expect(
      screen.getByRole("link", { name: "Open report version 2 in a new tab" }),
    ).toHaveFocus();
    view.unmount();

    // Focus moved on purpose while the send was out stays where it was put.
    view = renderSection({ reports: listed });
    await withdraw(view);
    const other = screen.getByRole("link", {
      name: "Open report version 1 in a new tab",
    });
    other.focus();
    view.rerender({ busy: false });
    expect(other).toHaveFocus();
    view.unmount();

    // A row no longer listed takes no focus.
    view = renderSection({ reports: listed });
    await withdraw(view);
    view.rerender({ busy: false, reports: [row(1, "FINAL")] });
    expect(document.body).toHaveFocus();
  });

  it("theDisabledReason_isReachableByAssistiveTechnology", () => {
    const { rerender } = renderSection({ dirty: true });

    const button = generateButton();
    const describedBy = button.getAttribute("aria-describedby");
    expect(describedBy).toBeTruthy();
    expect(document.getElementById(describedBy)).toHaveTextContent(SAVE_FIRST);
    expect(button).toHaveAccessibleDescription(SAVE_FIRST);

    rerender({ dirty: false, canGenerate: false });
    expect(generateButton()).toHaveAccessibleDescription(NEEDS_FINDINGS);

    rerender({ dirty: false, canGenerate: true });
    expect(generateButton()).toBeEnabled();
    expect(generateButton()).not.toHaveAttribute("aria-describedby");
  });
});
