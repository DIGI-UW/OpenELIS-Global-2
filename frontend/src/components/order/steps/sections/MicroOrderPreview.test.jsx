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
  newUnitWarnings: [
    { labUnitId: "2", labUnitName: "TB unit", testName: "TB culture" },
  ],
  ordinaryTests: [{ specimenIndex: 0, testId: "rpr", testName: "RPR" }],
  warnings: [{ specimenIndex: 0, labUnits: ["Microbiology", "TB unit"] }],
  reflexRules: [
    {
      name: "Positive bottle",
      overall: "ANY",
      conditions: [
        { testName: "Blood culture", relation: "EQUALS", value: "Positive" },
      ],
      addedTests: ["Gram stain"],
    },
  ],
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
      "Positive bottle: if Blood culture result equals Positive, may add Gram stain.",
    ),
  ).toBeTruthy();
  expect(screen.getByText("Opens a case")).toBeTruthy();
  expect(
    screen.getByText(
      "TB culture opens a case in TB unit; this order has no other work in that lab unit.",
    ),
  ).toBeTruthy();
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

it.each([
  ["ALL", "and"],
  ["ANY", "or"],
])(
  "preserves %s conditions with component and specimen scope",
  async (overall, connector) => {
    previewMicrobiologyOrder.mockResolvedValueOnce({
      ...response,
      reflexRules: [
        {
          name: "Repeat rule",
          overall,
          addedTests: ["Repeat culture"],
          conditions: [
            {
              testName: "Culture",
              componentLabel: "Colony count",
              sampleTypeName: "Sputum",
              relation: "BETWEEN",
              value: "10",
              value2: "20",
            },
            {
              testName: "Culture",
              relation: "OUTSIDE_NORMAL_RANGE",
              value: "ignored",
            },
          ],
        },
      ],
    });
    render(view());
    await screen.findByText(
      `Repeat rule: if (Culture — Colony count (Sputum) result is between 10 and 20) ${connector} (Culture result is outside the normal range), may add Repeat culture.`,
    );
  },
);

it("refreshes set counts and warnings when only a bottle assignment changes", async () => {
  const bottle = {
    collectedInSets: true,
    cultureSetNumber: 1,
    specimenType: "Blood",
  };
  previewMicrobiologyOrder.mockResolvedValue({
    ...response,
    cases: [
      {
        ...response.cases[0],
        bottles: [bottle],
        setWarnings: [
          { setNumber: 1, code: "SINGLE_BOTTLE", intervalMinutes: 30 },
        ],
      },
    ],
  });
  const first = [{ ...samples[0], cultureSetNumber: 1 }];
  const rendered = render(view(first));
  await screen.findByText("1 set, 1 bottle");
  expect(screen.getByText("Only one bottle in this set")).toBeInTheDocument();
  expect(previewMicrobiologyOrder).toHaveBeenLastCalledWith({
    specimens: [
      {
        sampleTypeId: "5",
        testIds: ["culture", "rpr"],
        cultureSetNumber: 1,
      },
    ],
  });
  rendered.rerender(view([{ ...samples[0], cultureSetNumber: 2 }]));
  await waitFor(() =>
    expect(previewMicrobiologyOrder).toHaveBeenLastCalledWith({
      specimens: [
        {
          sampleTypeId: "5",
          testIds: ["culture", "rpr"],
          cultureSetNumber: 2,
        },
      ],
    }),
  );
});
