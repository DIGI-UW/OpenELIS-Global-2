import React from "react";
import { render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { vi } from "vitest";
import messages from "../../languages/en.json";

const { orderContextValue, location } = vi.hoisted(() => ({
  location: { pathname: "/order/clinical/collect" },
  orderContextValue: {
    samples: [],
    storageSkipped: false,
    labNumber: "LAB-1",
    stepProgress: {},
    progress: { status: null },
    sampleCheckEnabled: true,
  },
}));
vi.mock("react-router-dom", () => ({
  useHistory: () => ({ push: vi.fn() }),
  useLocation: () => location,
}));
vi.mock("./OrderContext", () => ({
  useOrderContext: () => orderContextValue,
}));

import OrderStepper, { stepsForPath, progressReached } from "./OrderStepper";

const renderStepper = (props) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <OrderStepper currentStep={1} {...props} />
    </IntlProvider>,
  );

beforeEach(() => {
  location.pathname = "/order/clinical/collect";
  orderContextValue.stepProgress = {};
  orderContextValue.progress = { status: null };
  orderContextValue.sampleCheckEnabled = true;
});

// OGC-1266 FR-A12: three clinical steps, the last only while the sample
// acceptance setting is not Off (FR-F1).
describe("stepsForPath", () => {
  it("names the clinical steps Enter Order, Prepare Samples, Sample check", () => {
    expect(stepsForPath("/order/clinical/enter").map((s) => s.key)).toEqual([
      "enter",
      "prepare",
      "check",
    ]);
    expect(
      stepsForPath("/order/clinical/enter").map((s) => messages[s.label]),
    ).toEqual(["Enter Order", "Prepare Samples", "Sample check"]);
  });

  it("drops the clinical Sample check when it is off", () => {
    expect(
      stepsForPath("/order/clinical/collect", false).map((s) => s.key),
    ).toEqual(["enter", "prepare"]);
  });

  it("leaves the environmental and vector workflows as they are", () => {
    expect(stepsForPath("/order/environmental/qa", false)).toHaveLength(3);
    expect(stepsForPath("/order/vector/label").map((s) => s.key)).toEqual([
      "enter",
      "label",
      "qa",
      "complete",
    ]);
  });
});

describe("progressReached", () => {
  it("compares recorded statuses in order and never counts a cancelled order", () => {
    expect(progressReached("SAMPLES_PREPARED", "SAMPLES_PREPARED")).toBe(true);
    expect(progressReached("READY_FOR_TESTING", "SAMPLES_PREPARED")).toBe(true);
    expect(progressReached("ENTERED", "SAMPLES_PREPARED")).toBe(false);
    expect(progressReached("CANCELLED", "ENTERED")).toBe(false);
    expect(progressReached(null, "ENTERED")).toBe(false);
  });
});

// FR-F5: each step shows when it was completed, what the current one still
// needs, and that the rest are not started.
describe("the stepper's step notes", () => {
  it("shows the completion times from the recorded progress", () => {
    orderContextValue.progress = {
      status: "SAMPLES_PREPARED",
      enteredAt: "30/09/2026 10:15",
      preparedAt: "30/09/2026 11:02",
    };
    renderStepper({ currentStep: 2 });

    expect(screen.getByText("Done 10:15")).toBeInTheDocument();
    expect(screen.getByText("Done 11:02")).toBeInTheDocument();
    expect(screen.getByText("Not started")).toBeInTheDocument();
  });

  it("puts the to-do count on the current step and Not started on the rest", () => {
    orderContextValue.progress = {
      status: "ENTERED",
      enteredAt: "30/09/2026 10:15",
    };
    renderStepper({ currentStep: 1, currentStepNote: "2 to do" });

    expect(screen.getByText("Done 10:15")).toBeInTheDocument();
    expect(screen.getByText("2 to do")).toBeInTheDocument();
    expect(screen.getByText("Not started")).toBeInTheDocument();
  });

  it("marks every step of a cancelled order", () => {
    orderContextValue.progress = { status: "CANCELLED" };
    renderStepper({ currentStep: 0 });

    expect(screen.getAllByText("Cancelled")).toHaveLength(3);
  });

  it("falls back to the data-derived flags for orders saved before progress was recorded", () => {
    orderContextValue.stepProgress = { collect: true, label: true };
    renderStepper({ currentStep: 2 });

    expect(screen.getAllByText("Done")).toHaveLength(2);
    expect(screen.getByText("Not started")).toBeInTheDocument();
  });

  it("shows two clinical steps when the Sample check is off", () => {
    orderContextValue.sampleCheckEnabled = false;
    renderStepper({ currentStep: 1 });

    expect(screen.getByText("Prepare Samples")).toBeInTheDocument();
    expect(screen.queryByText("Sample check")).not.toBeInTheDocument();
  });
});
