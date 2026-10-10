import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { vi } from "vitest";
import messages from "../../languages/en.json";
import PlacementNotice from "./PlacementNotice";

const tubes = [
  { sampleItemId: "10", externalId: "ACC-1", typeOfSampleId: "1" },
  { sampleItemId: "11", externalId: "ACC-2", typeOfSampleId: "2" },
];

const row = (placement, extra = {}) => ({
  id: "1001",
  placement: { match: "ACCESSION", tubes, analyses: [], ...placement },
  ...extra,
});

const show = (value, handlers = {}) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <PlacementNotice row={value} {...handlers} />
    </IntlProvider>,
  );

describe("PlacementNotice", () => {
  it("only names the tube when one analysis is waiting for the result", () => {
    show(
      row({
        state: "RESOLVED",
        proposedSampleItemId: "10",
        proposedAnalysisId: "50",
        analyses: [
          {
            analysisId: "50",
            sampleItemId: "10",
            awaitingResult: true,
          },
        ],
      }),
    );
    expect(screen.getByText("Matched")).toBeInTheDocument();
    expect(
      screen.getByText("One analysis on ACC-1 is waiting for this result."),
    ).toBeInTheDocument();
    expect(screen.queryByRole("combobox")).not.toBeInTheDocument();
  });

  it("says a rerun replaces a held result and marks it corrected", () => {
    show(row({ state: "RETEST_CHOICE", proposedSampleItemId: "10" }));
    expect(screen.getByText("Already has a result")).toBeInTheDocument();
    expect(
      screen.getByText(
        "The analysis on ACC-1 already holds a result. Saving replaces it and marks it corrected.",
      ),
    ).toBeInTheDocument();
  });

  it("asks the reviewer to choose among several matching analyses", () => {
    const onChooseAnalysis = vi.fn();
    show(
      row({
        state: "MULTI_TUBE",
        analyses: [
          { analysisId: "50", sampleItemId: "10", awaitingResult: true },
          { analysisId: "51", sampleItemId: "11", awaitingResult: false },
        ],
      }),
      { onChooseAnalysis },
    );
    expect(screen.getByText("Several matches")).toBeInTheDocument();
    expect(
      screen.getByRole("option", { name: "ACC-1, waiting for a result" }),
    ).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText("Analysis this result belongs to"), {
      target: { value: "51" },
    });
    expect(onChooseAnalysis).toHaveBeenCalledWith("51", "1001");
  });

  it("offers no choice when the ID names several tubes and no analysis", () => {
    show(row({ state: "MULTI_TUBE" }));
    expect(
      screen.getByText(
        "More than one tube carries this ID. Correct the duplicate tube labels, then review again.",
      ),
    ).toBeInTheDocument();
    expect(screen.queryByRole("combobox")).not.toBeInTheDocument();
  });

  it("explains that no order carries the ID", () => {
    show(row({ state: "NEW_SAMPLE", match: "NONE", tubes: [] }));
    expect(screen.getByText("New sample")).toBeInTheDocument();
    expect(
      screen.getByText(
        "No order or tube carries this ID. Saving creates a new sample for it.",
      ),
    ).toBeInTheDocument();
  });

  it("opens the delivery bundle only when the row has a delivery", () => {
    const onViewBundle = vi.fn();
    const { rerender } = show(row({ state: "NEW_SAMPLE", tubes: [] }), {
      onViewBundle,
    });
    expect(
      screen.queryByRole("button", { name: "View bundle" }),
    ).not.toBeInTheDocument();
    rerender(
      <IntlProvider locale="en" messages={messages}>
        <PlacementNotice
          row={row(
            { state: "NEW_SAMPLE", tubes: [] },
            { deliveryReceiptId: "receipt-1" },
          )}
          onViewBundle={onViewBundle}
        />
      </IntlProvider>,
    );
    fireEvent.click(screen.getByRole("button", { name: "View bundle" }));
    expect(onViewBundle).toHaveBeenCalledWith("receipt-1");
  });

  it("shows a matching instrument patient without raising an alarm", () => {
    show(
      row({
        state: "RESOLVED",
        proposedSampleItemId: "10",
        patient: {
          status: "MATCH",
          instrumentId: "PAT-77",
          instrumentName: "Doe, Jane",
          orderName: "Doe, Jane",
        },
      }),
    );
    expect(
      screen.getByText(
        "Instrument patient PAT-77 (Doe, Jane) matches the order's patient.",
      ),
    ).toBeInTheDocument();
    expect(screen.queryByText("Patient mismatch")).not.toBeInTheDocument();
  });

  it("flags a patient mismatch and asks for a note before saving", () => {
    show(
      row({
        state: "RESOLVED",
        proposedSampleItemId: "10",
        patient: {
          status: "MISMATCH",
          instrumentId: "OTHER-9",
          instrumentName: "Roe, Rick",
          orderName: "Doe, Jane",
        },
      }),
    );
    expect(screen.getByText("Patient mismatch")).toBeInTheDocument();
    expect(
      screen.getByText(
        "The instrument reported patient OTHER-9 (Roe, Rick), but the order's patient is Doe, Jane. Add a note saying why before saving.",
      ),
    ).toBeInTheDocument();
  });

  it("says when the order has no patient identifier to compare with", () => {
    show(
      row({
        state: "RESOLVED",
        proposedSampleItemId: "10",
        patient: { status: "NO_ORDER_PATIENT", instrumentId: "PAT-77" },
      }),
    );
    expect(
      screen.getByText(
        "The instrument reported patient PAT-77. The order has no patient identifier to compare it with.",
      ),
    ).toBeInTheDocument();
  });

  it("says nothing about the patient when the instrument reported none", () => {
    show(
      row({
        state: "RESOLVED",
        proposedSampleItemId: "10",
        patient: { status: "NOT_REPORTED" },
      }),
    );
    expect(screen.queryByText(/Instrument patient/)).not.toBeInTheDocument();
    expect(
      screen.queryByText(/The instrument reported patient/),
    ).not.toBeInTheDocument();
  });

  it("renders nothing for a row without a placement", () => {
    const { container } = show({ id: "1" });
    expect(container).toBeEmptyDOMElement();
  });
});
