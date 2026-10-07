import React from "react";
import { act, render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import userEvent from "@testing-library/user-event";
import { IntlProvider } from "react-intl";
import { vi } from "vitest";
import messages from "../../../../languages/en.json";
import MicroOrderPreview from "./MicroOrderPreview";
import { previewMicrobiologyOrder } from "../../../microbiology/MicrobiologyService";

vi.mock("../../../microbiology/MicrobiologyService", () => ({
  previewMicrobiologyOrder: vi.fn(),
}));
const samples = [
  { sampleTypeId: "5", tests: [{ id: "culture" }, { id: "rpr" }] },
];
const response = {
  cases: [
    {
      labUnitId: "1",
      labUnitName: "Microbiology",
      testNames: ["Culture"],
      specimens: [{ index: 0, sampleTypeName: "Blood" }],
    },
  ],
  ordinaryTests: [{ specimenIndex: 0, testId: "rpr", testName: "RPR" }],
  warnings: [{ specimenIndex: 0, labUnits: ["Microbiology", "TB unit"] }],
  reflexRules: [{ name: "Positive bottle", addedTests: ["Gram stain"] }],
};
const view = (value = samples, savedOrder = false) => (
  <IntlProvider locale="en" messages={messages}>
    <MicroOrderPreview samples={value} savedOrder={savedOrder} />
  </IntlProvider>
);
beforeEach(() => vi.clearAllMocks());

it("shows server-derived case, ordinary Results, split and named reflex lines", async () => {
  previewMicrobiologyOrder.mockResolvedValueOnce(response);
  render(view());
  await screen.findByText("Culture case in Microbiology: Blood sample 1");
  expect(screen.getByText("RPR stays in Results")).toBeTruthy();
  expect(
    screen.getByText("Sample 1 opens 2 cases: Microbiology, TB unit."),
  ).toBeTruthy();
  expect(
    screen.getByText(
      "Positive bottle: may add Gram stain when its conditions are met.",
    ),
  ).toBeTruthy();
  expect(screen.getByText("Opens a case")).toBeTruthy();
  expect(previewMicrobiologyOrder).toHaveBeenCalledWith({
    specimens: [{ sampleTypeId: "5", testIds: ["culture", "rpr"] }],
  });
});

it("clears an old answer immediately and ignores a late response after selection changes", async () => {
  let oldResolve;
  previewMicrobiologyOrder.mockImplementationOnce(
    () =>
      new Promise((resolve) => {
        oldResolve = resolve;
      }),
  );
  previewMicrobiologyOrder.mockResolvedValueOnce({
    ...response,
    cases: [{ ...response.cases[0], testNames: ["TB culture"] }],
  });
  const { rerender } = render(view());
  rerender(view([{ sampleTypeId: "5", tests: [{ id: "tb" }] }]));
  await screen.findByText("TB culture case in Microbiology: Blood sample 1");
  await act(async () => oldResolve(response));
  expect(
    screen.queryByText("Culture case in Microbiology: Blood sample 1"),
  ).toBeNull();
});

it("removes the panel when all tests are removed", async () => {
  previewMicrobiologyOrder.mockResolvedValueOnce(response);
  const { rerender } = render(view());
  await screen.findByText("Opens a case");
  rerender(view([{ sampleTypeId: "5", tests: [] }]));
  expect(screen.queryByText("What this order will open")).toBeNull();
  expect(previewMicrobiologyOrder).toHaveBeenCalledTimes(1);
});

it("replaces stale predictions with an error when a changed selection fails", async () => {
  previewMicrobiologyOrder
    .mockResolvedValueOnce(response)
    .mockRejectedValueOnce(new Error("offline"));
  const { rerender } = render(view());
  await screen.findByText("Opens a case");
  rerender(view([{ sampleTypeId: "5", tests: [{ id: "tb" }] }]));
  await screen.findByText("Order preview unavailable");
  expect(screen.queryByText("Opens a case")).toBeNull();
});

it("does not use new-order grouping for saved orders", () => {
  render(view(samples, true));
  expect(previewMicrobiologyOrder).not.toHaveBeenCalled();
  expect(screen.queryByText("What this order will open")).toBeNull();
});

it("hides the micro panel when the server finds only ordinary tests", async () => {
  previewMicrobiologyOrder.mockResolvedValueOnce({ ...response, cases: [] });
  render(view());
  await waitFor(() =>
    expect(screen.queryByText("What this order will open")).toBeNull(),
  );
});

it("retries a failed preview without changing the order", async () => {
  previewMicrobiologyOrder
    .mockRejectedValueOnce(new Error("offline"))
    .mockResolvedValueOnce(response);
  render(view());
  await screen.findByText("Order preview unavailable");
  await userEvent.click(screen.getByRole("button", { name: "Retry" }));
  await screen.findByText("Opens a case");
  expect(previewMicrobiologyOrder).toHaveBeenCalledTimes(2);
  expect(previewMicrobiologyOrder.mock.calls[0]).toEqual(
    previewMicrobiologyOrder.mock.calls[1],
  );
});
