import React from "react";
import { render, screen, fireEvent, within } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import { IntlProvider } from "react-intl";
import { vi } from "vitest";
import CaseAstRunQc from "../CaseAstRunQc";
import messages from "../../../languages/en.json";

const view = (overrides = {}) => ({
  configured: true,
  canRecord: true,
  antibiotics: [
    { id: "a", name: "Ampicillin" },
    { id: "b", name: "Gentamicin" },
  ],
  controlLots: [
    {
      id: "ecoli",
      label: "E. coli ATCC 25922 · L1",
      ranges: { a: { low: 1, high: 4 }, b: { low: 2, high: 8 } },
    },
    { id: "aureus", label: "S. aureus ATCC 29213 · L2", ranges: {} },
  ],
  lots: [
    { id: 11, lotNumber: "CARD-1", itemName: "GN cards", qcStatus: "PASSED" },
  ],
  candidateLots: [
    { id: 12, lotNumber: "CARD-2", itemName: "GN cards", qcStatus: "PENDING" },
  ],
  controls: [],
  ...overrides,
});

const show = (service) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <CaseAstRunQc caseId="c1" runId="run" service={service} />
    </IntlProvider>,
  );

it("shows the run's lots with their QC status and the controls recorded on it", async () => {
  const service = {
    getAstQc: vi.fn().mockResolvedValue(
      view({
        controls: [
          {
            controlId: "k1",
            controlLotLabel: "E. coli ATCC 25922 · L1",
            recordedAt: 1793000000000,
            passed: false,
            rows: [
              {
                antibioticId: "b",
                low: 2,
                high: 8,
                measuredValue: 50,
                outcome: "FAIL",
              },
            ],
          },
        ],
      }),
    ),
  };
  show(service);
  const region = await screen.findByRole("region", { name: "Run QC" });
  expect(within(region).getByText("CARD-1")).toBeInTheDocument();
  expect(within(region).getByText("QC passed")).toBeInTheDocument();
  expect(
    within(region).getByText("E. coli ATCC 25922 · L1"),
  ).toBeInTheDocument();
  expect(within(region).getByText("Failed")).toBeInTheDocument();
  expect(service.getAstQc).toHaveBeenCalledWith("c1", "run");
});

it("names every lot QC status, quarantine included, and keeps tags out of paragraphs", async () => {
  const service = {
    getAstQc: vi.fn().mockResolvedValue(
      view({
        lots: [
          {
            id: 13,
            lotNumber: "CARD-3",
            itemName: "GN cards",
            qcStatus: "QUARANTINED",
          },
        ],
        controls: [
          {
            controlId: "k1",
            controlLotLabel: "E. coli ATCC 25922 · L1",
            recordedAt: 1793000000000,
            passed: true,
            rows: [],
          },
        ],
      }),
    ),
  };
  show(service);
  const region = await screen.findByRole("region", { name: "Run QC" });
  expect(within(region).getByText("QC quarantined")).toBeInTheDocument();
  for (const tag of region.querySelectorAll(".cds--tag")) {
    expect(tag.closest("p")).toBeNull();
  }
});

it("records one value per antibiotic with the lots under test, showing pass or fail as values are typed", async () => {
  const service = {
    getAstQc: vi.fn().mockResolvedValue(view()),
    recordAstQc: vi.fn().mockResolvedValue(view()),
  };
  show(service);
  fireEvent.click(
    await screen.findByRole("button", { name: "Record control result" }),
  );
  const dialog = await screen.findByRole("dialog", {
    name: "Record control result",
  });
  const save = within(dialog).getByRole("button", { name: "Record" });
  expect(save).toBeDisabled();
  fireEvent.change(within(dialog).getByLabelText("Control lot"), {
    target: { value: "ecoli" },
  });
  expect(within(dialog).getByText("1–4")).toBeInTheDocument();
  fireEvent.change(within(dialog).getByLabelText("Measured value Ampicillin"), {
    target: { value: "2" },
  });
  fireEvent.change(within(dialog).getByLabelText("Measured value Gentamicin"), {
    target: { value: "50" },
  });
  expect(within(dialog).getByText("Pass")).toBeInTheDocument();
  expect(within(dialog).getByText("Fail")).toBeInTheDocument();
  expect(within(dialog).getByLabelText(/CARD-1/)).toBeChecked();
  expect(within(dialog).getByLabelText(/CARD-2/)).not.toBeChecked();
  fireEvent.click(within(dialog).getByLabelText(/CARD-2/));
  expect(save).toBeEnabled();
  fireEvent.click(save);
  await waitFor(() =>
    expect(service.recordAstQc).toHaveBeenCalledWith("c1", "run", {
      controlLotId: "ecoli",
      lotIds: [11, 12],
      results: [
        { antibioticId: "a", measuredValue: 2 },
        { antibioticId: "b", measuredValue: 50 },
      ],
    }),
  );
  await waitFor(() =>
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument(),
  );
});

it("cannot record a control lot that has no QC range for every antibiotic", async () => {
  const service = {
    getAstQc: vi.fn().mockResolvedValue(view()),
    recordAstQc: vi.fn(),
  };
  show(service);
  fireEvent.click(
    await screen.findByRole("button", { name: "Record control result" }),
  );
  const dialog = await screen.findByRole("dialog", {
    name: "Record control result",
  });
  fireEvent.change(within(dialog).getByLabelText("Control lot"), {
    target: { value: "aureus" },
  });
  expect(within(dialog).getAllByText("No QC range")).toHaveLength(2);
  expect(
    within(dialog).getByText(
      "Load a QC range for every antibiotic before recording this control.",
    ),
  ).toBeInTheDocument();
  expect(within(dialog).getByRole("button", { name: "Record" })).toBeDisabled();
});

it("says the AST QC test must be chosen before controls can be recorded", async () => {
  const service = {
    getAstQc: vi.fn().mockResolvedValue(view({ configured: false })),
  };
  show(service);
  expect(
    await screen.findByText(
      "Choose the AST QC test in the microbiology settings before recording controls.",
    ),
  ).toBeInTheDocument();
  expect(
    screen.queryByRole("button", { name: "Record control result" }),
  ).not.toBeInTheDocument();
});
