/**
 * The Findings & Conclusion section (FR-12, FR-13).
 *
 * The section edits the case it is handed through updateCase; these tests
 * hold the case in state the way the case view does and pin what the
 * section shows and what it writes back.
 */
import React, { useEffect, useState } from "react";
import { vi } from "vitest";
import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../languages/en.json";
import FindingsSection from "./FindingsSection";
import { fetchFromOpenElisServer } from "../../utils/Utils";

vi.mock("../../common/textMacro/TextMacroService", () => ({
  getTextMacros: vi.fn(() => Promise.resolve([])),
}));

vi.mock("../../utils/Utils", async (importOriginal) => ({
  ...(await importOriginal()),
  fetchFromOpenElisServer: vi.fn(),
}));

const he = { id: "41", value: "H&E" };
const pas = { id: "42", value: "PAS" };
const ki67 = { id: "77", value: "Ki-67" };
const cd20 = { id: "78", value: "CD20" };
const adeno = { id: "9101", value: "Adenocarcinoma", code: "C80.1" };

const baseCase = {
  status: "UNDER_REVIEW",
  releasable: true,
  assignedPathologistId: "5",
  assignedTechnicianId: "6",
  grossExam: "Tan nodule, 2 cm.",
  microscopyExam: "Glands in a desmoplastic stroma.",
  techniques: [],
  conclusions: [],
  conclusionText: "",
  referToImmunoHistoChemistry: false,
  referredToIhc: false,
  release: false,
};

let latestCase;
const recordCase = (caseInfo) => {
  latestCase = caseInfo;
};

const Harness = ({ initial, ...props }) => {
  const [caseInfo, setCaseInfo] = useState(initial);
  useEffect(() => recordCase(caseInfo));
  const updateCase = (change) =>
    setCaseInfo((prev) => ({
      ...prev,
      ...(typeof change === "function" ? change(prev) : change),
    }));
  return (
    <FindingsSection caseInfo={caseInfo} updateCase={updateCase} {...props} />
  );
};

const renderSection = ({ caseInfo = {}, ...props } = {}) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <Harness
        initial={{ ...baseCase, ...caseInfo }}
        readOnly={false}
        techniques={[he, pas]}
        immunoHistoChemistryTests={[ki67]}
        formVersion={0}
        canAddDiagnosis={false}
        {...props}
      />
    </IntlProvider>,
  );

const techniquesBox = () =>
  screen.getByRole("combobox", { name: /Techniques Used/ });

const releaseBox = () =>
  screen.getByLabelText(messages["pathology.label.release"]);

const releaseReason =
  "A case is signed out only once both the macroscopic and the microscopic description are written.";

beforeEach(() => {
  fetchFromOpenElisServer.mockReset();
  fetchFromOpenElisServer.mockImplementation(() =>
    Promise.resolve({ items: [adeno], total: 1 }),
  );
});

describe("FindingsSection", () => {
  it("picking a technique adds its chip, and removing the chip unticks it in the picker", async () => {
    renderSection();

    await userEvent.click(techniquesBox());
    await userEvent.click(await screen.findByRole("option", { name: "PAS" }));
    const chips = screen.getByTestId("findings-technique-chips");
    expect(within(chips).getByText("PAS")).toBeInTheDocument();
    expect(latestCase.techniques).toEqual([pas]);

    await userEvent.keyboard("{Escape}");
    await userEvent.click(
      within(chips).getByRole("button", { name: "Remove PAS" }),
    );
    expect(within(chips).queryByText("PAS")).not.toBeInTheDocument();
    expect(latestCase.techniques).toEqual([]);

    await userEvent.click(techniquesBox());
    const option = await screen.findByRole("option", { name: "PAS" });
    expect(option).toHaveAttribute("aria-selected", "false");
  });

  it("removing a technique chip moves focus to the next chip, then to the techniques box", async () => {
    renderSection({ caseInfo: { techniques: [he, pas] } });
    const chips = screen.getByTestId("findings-technique-chips");

    await userEvent.click(
      within(chips).getByRole("button", { name: "Remove H&E" }),
    );
    expect(latestCase.techniques).toEqual([pas]);
    await waitFor(() =>
      expect(
        within(chips).getByRole("button", { name: "Remove PAS" }),
      ).toHaveFocus(),
    );

    await userEvent.click(
      within(chips).getByRole("button", { name: "Remove PAS" }),
    );
    expect(latestCase.techniques).toEqual([]);
    await waitFor(() => expect(techniquesBox()).toHaveFocus());
  });

  it("a stored referral shows the markers already referred as read-only chips and leaves them out of the markers picker", async () => {
    renderSection({
      immunoHistoChemistryTests: [ki67, cd20],
      caseInfo: {
        referredToIhc: true,
        referToImmunoHistoChemistry: true,
        ihcReferredTestIds: ["77"],
      },
    });

    expect(screen.getByText("Markers referred")).toBeInTheDocument();
    const referred = screen.getByTestId("findings-ihc-referred-markers");
    expect(within(referred).getByText("Ki-67")).toBeInTheDocument();
    expect(within(referred).queryAllByRole("button")).toHaveLength(0);

    await userEvent.click(
      screen.getByRole("combobox", { name: /Markers to order/ }),
    );
    expect(
      await screen.findByRole("option", { name: "CD20" }),
    ).toBeInTheDocument();
    expect(
      screen.queryByRole("option", { name: "Ki-67" }),
    ).not.toBeInTheDocument();
  });

  it("an empty technique list says so", () => {
    renderSection({ techniques: [] });

    expect(
      screen.getByText("No technique is in the dictionary yet."),
    ).toBeInTheDocument();
    expect(techniquesBox()).toBeDisabled();
  });

  it("the narrative counts to 255", async () => {
    renderSection();

    const narrative = screen.getByLabelText("Narrative conclusion");
    expect(narrative).toHaveAttribute("maxlength", "255");
    expect(screen.getByText("0/255")).toBeInTheDocument();
    await userEvent.type(narrative, "Adenocarcinoma");
    expect(screen.getByText("14/255")).toBeInTheDocument();
    expect(latestCase.conclusionText).toBe("Adenocarcinoma");
  });

  it("the referral: a tick shows the markers picker; a stored referral shows ticked, fixed and named, with the markers picker still offered; the payload flag stays true", async () => {
    const { unmount } = renderSection();

    expect(
      screen.queryByRole("combobox", { name: /Markers to order/ }),
    ).not.toBeInTheDocument();
    await userEvent.click(screen.getByText("Refer to Immunohistochemistry"));
    expect(latestCase.referToImmunoHistoChemistry).toBe(true);
    expect(
      screen.getByRole("combobox", { name: /Markers to order/ }),
    ).toBeEnabled();
    unmount();

    renderSection({
      caseInfo: { referredToIhc: true, referToImmunoHistoChemistry: true },
    });
    const referral = screen.getByLabelText("Refer to Immunohistochemistry");
    expect(referral).toBeChecked();
    expect(referral).toBeDisabled();
    expect(screen.getByTestId("findings-ihc-referred")).toHaveTextContent(
      "Referred to Immunohistochemistry",
    );
    // A read that names no referred markers shows none.
    expect(
      screen.queryByTestId("findings-ihc-referred-markers"),
    ).not.toBeInTheDocument();
    expect(
      screen.getByRole("combobox", { name: /Markers to order/ }),
    ).toBeEnabled();

    await userEvent.click(screen.getByText("Refer to Immunohistochemistry"));
    expect(referral).toBeChecked();
    expect(latestCase.referToImmunoHistoChemistry).toBe(true);
  });

  it("the release box says why while either description is blank, and is offered once both are written", async () => {
    const { unmount } = renderSection({
      caseInfo: { releasable: false, grossExam: "" },
    });
    // A case the server will not release says so first.
    expect(
      screen.getByText(messages["pathology.error.releaseBeforeReview"]),
    ).toBeInTheDocument();
    expect(screen.queryByText(releaseReason)).not.toBeInTheDocument();
    unmount();

    renderSection({ caseInfo: { grossExam: "   ", microscopyExam: "" } });
    expect(releaseBox()).toBeDisabled();
    expect(screen.getByText(releaseReason)).toBeInTheDocument();

    await userEvent.type(
      screen.getByLabelText("Macroscopic description"),
      "Tan nodule",
    );
    expect(releaseBox()).toBeDisabled();
    expect(screen.getByText(releaseReason)).toBeInTheDocument();

    await userEvent.type(
      screen.getByLabelText("Microscopic description"),
      "Benign glands",
    );
    expect(releaseBox()).toBeEnabled();
    expect(screen.queryByText(releaseReason)).not.toBeInTheDocument();
  });

  it("read-only: every control disabled and no chip has a remove button", async () => {
    renderSection({
      readOnly: true,
      caseInfo: {
        techniques: [he],
        conclusions: [adeno],
        referredToIhc: true,
        referToImmunoHistoChemistry: true,
        immunoHistoChemistryTestIds: [ki67],
      },
    });

    for (const textbox of screen.getAllByRole("textbox")) {
      expect(textbox).toBeDisabled();
    }
    for (const combobox of screen.getAllByRole("combobox")) {
      expect(combobox).toBeDisabled();
    }
    for (const checkbox of screen.getAllByRole("checkbox")) {
      expect(checkbox).toBeDisabled();
    }
    expect(screen.getAllByRole("combobox")).toHaveLength(3);
    expect(document.getElementById("grossExam")).toBeDisabled();
    expect(document.getElementById("microscopyExam")).toBeDisabled();
    expect(document.getElementById("conclusionText")).toBeDisabled();
    expect(
      within(screen.getByTestId("findings-technique-chips")).getByText("H&E"),
    ).toBeInTheDocument();
    expect(
      within(screen.getByTestId("findings-conclusion-chips")).getByText(
        "C80.1 Adenocarcinoma",
      ),
    ).toBeInTheDocument();
    expect(screen.queryAllByRole("button", { name: /^Remove / })).toHaveLength(
      0,
    );
    await waitFor(() => expect(fetchFromOpenElisServer).not.toHaveBeenCalled());
  });
});
