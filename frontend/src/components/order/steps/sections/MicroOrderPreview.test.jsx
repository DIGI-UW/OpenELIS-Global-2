import React from "react";
import { act, render, screen, within } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import userEvent from "@testing-library/user-event";
import { IntlProvider } from "react-intl";
import { vi } from "vitest";
import messages from "../../../../languages/en.json";
import MicroOrderPreview, {
  MicroSampleCaseNote,
  useMicroOrderPreview,
} from "./MicroOrderPreview";
import { previewMicrobiologyOrder } from "../../../microbiology/MicrobiologyService";

vi.mock("../../../microbiology/MicrobiologyService", () => ({
  previewMicrobiologyOrder: vi.fn(),
}));
const samples = [
  { sampleTypeId: "5", tests: [{ id: "gram" }, { id: "culture" }] },
];
const response = {
  cases: [
    {
      labUnitId: "1",
      labUnitName: "Microbiology",
      testNames: ["Gram stain", "Culture"],
      specimens: [{ index: 0, sampleTypeName: "Blood" }],
    },
  ],
  newUnitWarnings: [],
  ordinaryTests: [],
  warnings: [],
  reflexRules: [],
};
const Harness = ({ samples: value, ...context }) => {
  const preview = useMicroOrderPreview({ samples: value, ...context });
  return (
    <>
      {value.map((_, index) => (
        <div key={index} data-testid={`sample-${index}`}>
          <MicroSampleCaseNote preview={preview} sampleIndex={index} />
        </div>
      ))}
      <div data-testid="order-end">
        <MicroOrderPreview preview={preview} />
      </div>
    </>
  );
};
const view = (value = samples, savedOrder = false, context = {}) => (
  <IntlProvider locale="en" messages={messages}>
    <Harness samples={value} savedOrder={savedOrder} {...context} />
  </IntlProvider>
);
const textOf = (testId) => {
  const copy = screen.getByTestId(testId).cloneNode(true);
  copy.querySelectorAll("svg").forEach((icon) => icon.remove());
  return copy.textContent;
};
const sampleText = (index) => textOf(`sample-${index}`);
beforeEach(() => vi.clearAllMocks());

it("tells the sample which tests open a case and in which lab unit", async () => {
  previewMicrobiologyOrder.mockResolvedValueOnce(response);
  render(view());
  await waitFor(() =>
    expect(sampleText(0)).toBe(
      "Gram stain and Culture open a microbiology case in Microbiology.",
    ),
  );
  const note = screen.getByTestId("sample-0");
  for (const name of ["Gram stain", "Culture", "Microbiology"]) {
    expect(within(note).getByText(name).tagName).toBe("STRONG");
  }
  expect(previewMicrobiologyOrder).toHaveBeenCalledWith({
    specimens: [{ sampleTypeId: "5", testIds: ["gram", "culture"] }],
  });
});

it("uses the singular verb for one test", async () => {
  previewMicrobiologyOrder.mockResolvedValueOnce({
    ...response,
    cases: [{ ...response.cases[0], testNames: ["Culture"] }],
  });
  render(view());
  await waitFor(() =>
    expect(sampleText(0)).toBe(
      "Culture opens a microbiology case in Microbiology.",
    ),
  );
});

it("warns on the sample when it opens cases in two lab units", async () => {
  previewMicrobiologyOrder.mockResolvedValueOnce({
    ...response,
    cases: [
      { ...response.cases[0], testNames: ["Culture"] },
      {
        labUnitId: "2",
        labUnitName: "TB unit",
        testNames: ["TB culture"],
        specimens: [{ index: 0, sampleTypeName: "Blood" }],
      },
    ],
    warnings: [{ specimenIndex: 0, labUnits: ["Microbiology", "TB unit"] }],
  });
  render(view());
  const note = screen.getByTestId("sample-0");
  await within(note).findByText("This sample opens 2 microbiology cases");
  expect(
    within(note).getByText("Culture in Microbiology, TB culture in TB unit"),
  ).toBeTruthy();
  expect(note.querySelector(".cds--inline-notification--warning")).toBeTruthy();
  expect(within(note).queryByText(/open a microbiology/)).toBeNull();
});

it("names the split's lab units once, without a separate no-other-work warning for each", async () => {
  previewMicrobiologyOrder.mockResolvedValueOnce({
    ...response,
    cases: [
      { ...response.cases[0], testNames: ["Culture"] },
      {
        labUnitId: "2",
        labUnitName: "TB unit",
        testNames: ["TB culture"],
        specimens: [{ index: 0, sampleTypeName: "Blood" }],
      },
    ],
    warnings: [{ specimenIndex: 0, labUnits: ["Microbiology", "TB unit"] }],
    newUnitWarnings: [
      { labUnitId: "1", labUnitName: "Microbiology", testName: "Culture" },
      { labUnitId: "2", labUnitName: "TB unit", testName: "TB culture" },
    ],
  });
  render(view());
  const note = screen.getByTestId("sample-0");
  await within(note).findByText("This sample opens 2 microbiology cases");
  expect(within(note).queryByText(/No other test on this order/)).toBeNull();
});

it("describes two cases in one lab unit as information, not a warning", async () => {
  previewMicrobiologyOrder.mockResolvedValueOnce({
    ...response,
    cases: [
      { ...response.cases[0], testNames: ["Blood culture"] },
      { ...response.cases[0], testNames: ["Fungal culture"] },
    ],
    warnings: [],
  });
  render(view());
  await waitFor(() =>
    expect(sampleText(0)).toBe(
      "Blood culture opens a microbiology case in Microbiology." +
        "Fungal culture opens a microbiology case in Microbiology.",
    ),
  );
  expect(
    screen
      .getByTestId("sample-0")
      .querySelector(".cds--inline-notification--warning"),
  ).toBeNull();
});

it("lists the sample's tests that stay in Results", async () => {
  previewMicrobiologyOrder.mockResolvedValueOnce({
    ...response,
    ordinaryTests: [
      { specimenIndex: 0, testId: "rpr", testName: "RPR" },
      { specimenIndex: 0, testId: "glu", testName: "Glucose" },
    ],
  });
  render(view());
  await within(screen.getByTestId("sample-0")).findByText(
    "Stays in Results: RPR, Glucose",
  );
});

it("warns when a lab unit has no other work on an order that has other work", async () => {
  previewMicrobiologyOrder.mockResolvedValueOnce({
    ...response,
    cases: [{ ...response.cases[0], testNames: ["Culture"] }],
    ordinaryTests: [{ specimenIndex: 0, testId: "rpr", testName: "RPR" }],
    newUnitWarnings: [
      { labUnitId: "1", labUnitName: "Microbiology", testName: "Culture" },
    ],
  });
  render(view());
  const note = screen.getByTestId("sample-0");
  await within(note).findByText(
    "No other test on this order goes to Microbiology",
  );
  expect(note.querySelector(".cds--inline-notification--warning")).toBeTruthy();
});

it("leaves out the no-other-work warning when the case is the order's only work", async () => {
  previewMicrobiologyOrder.mockResolvedValueOnce({
    ...response,
    cases: [{ ...response.cases[0], testNames: ["Culture"] }],
    newUnitWarnings: [
      { labUnitId: "1", labUnitName: "Microbiology", testName: "Culture" },
    ],
  });
  render(view());
  await waitFor(() =>
    expect(sampleText(0)).toBe(
      "Culture opens a microbiology case in Microbiology.",
    ),
  );
});

it("says nothing more at the end of the order in the normal case", async () => {
  previewMicrobiologyOrder.mockResolvedValueOnce(response);
  render(view());
  await waitFor(() => expect(sampleText(0)).not.toBe(""));
  expect(textOf("order-end")).toBe("");
});

it("matches each note to its sample when an earlier sample has no type", async () => {
  previewMicrobiologyOrder.mockResolvedValueOnce(response);
  render(view([{ tests: [] }, { sampleTypeId: "5", tests: [{ id: "gram" }] }]));
  await waitFor(() => expect(sampleText(1)).not.toBe(""));
  expect(sampleText(0)).toBe("");
  expect(previewMicrobiologyOrder).toHaveBeenCalledWith({
    specimens: [{ sampleTypeId: "5", testIds: ["gram"] }],
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
  await waitFor(() =>
    expect(sampleText(0)).toBe(
      "TB culture opens a microbiology case in Microbiology.",
    ),
  );
  await act(async () => oldResolve(response));
  expect(sampleText(0)).toBe(
    "TB culture opens a microbiology case in Microbiology.",
  );
});

it("removes the note when all tests are removed", async () => {
  previewMicrobiologyOrder.mockResolvedValueOnce(response);
  const { rerender } = render(view());
  await waitFor(() => expect(sampleText(0)).not.toBe(""));
  rerender(view([{ sampleTypeId: "5", tests: [] }]));
  expect(sampleText(0)).toBe("");
  expect(previewMicrobiologyOrder).toHaveBeenCalledTimes(1);
});

it("replaces stale notes with an error when a changed selection fails", async () => {
  previewMicrobiologyOrder
    .mockResolvedValueOnce(response)
    .mockRejectedValueOnce(new Error("offline"));
  const { rerender } = render(view());
  await waitFor(() => expect(sampleText(0)).not.toBe(""));
  rerender(view([{ sampleTypeId: "5", tests: [{ id: "tb" }] }]));
  await screen.findByText("Order preview unavailable");
  expect(sampleText(0)).toBe("");
});

it("does not use new-order grouping for saved orders", () => {
  render(view(samples, true));
  expect(previewMicrobiologyOrder).not.toHaveBeenCalled();
  expect(sampleText(0)).toBe("");
  expect(textOf("order-end")).toBe("");
});

it("says nothing when the server finds only ordinary tests", async () => {
  previewMicrobiologyOrder.mockResolvedValueOnce({ ...response, cases: [] });
  render(view());
  await waitFor(() => expect(previewMicrobiologyOrder).toHaveBeenCalled());
  await waitFor(() => expect(textOf("order-end")).toBe(""));
  expect(sampleText(0)).toBe("");
});

it("retries a failed preview without changing the order", async () => {
  previewMicrobiologyOrder
    .mockRejectedValueOnce(new Error("offline"))
    .mockResolvedValueOnce(response);
  render(view());
  await screen.findByText("Order preview unavailable");
  await userEvent.click(screen.getByRole("button", { name: "Retry" }));
  await waitFor(() => expect(sampleText(0)).not.toBe(""));
  expect(previewMicrobiologyOrder).toHaveBeenCalledTimes(2);
  expect(previewMicrobiologyOrder.mock.calls[0]).toEqual(
    previewMicrobiologyOrder.mock.calls[1],
  );
});

it.each([
  ["ALL", "and"],
  ["ANY", "or"],
])(
  "lists %s reflex conditions with component and specimen scope at the end of the order",
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
    await within(screen.getByTestId("order-end")).findByText(
      `Repeat rule: if (Culture — Colony count (Sputum) result is between 10 and 20) ${connector} (Culture result is outside the normal range), may add Repeat culture.`,
    );
  },
);

it("shows set counts and warnings at the end of the order and refreshes them when a bottle assignment changes", async () => {
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
  const end = screen.getByTestId("order-end");
  await within(end).findByText("1 set, 1 bottle");
  expect(within(end).getByText("Only one bottle in this set")).toBeTruthy();
  expect(previewMicrobiologyOrder).toHaveBeenLastCalledWith({
    specimens: [
      {
        sampleTypeId: "5",
        testIds: ["gram", "culture"],
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
          testIds: ["gram", "culture"],
          cultureSetNumber: 2,
        },
      ],
    }),
  );
});

it("sends environmental site overrides and names the case site on the sample", async () => {
  previewMicrobiologyOrder.mockResolvedValueOnce({
    ...response,
    cases: [{ ...response.cases[0], siteName: "Ward 2" }],
  });
  render(
    view(
      [
        {
          sampleTypeId: "5",
          collectionLocationId: "12",
          tests: [{ id: "culture" }],
        },
      ],
      false,
      {
        domain: "ENVIRONMENTAL",
        samplingSiteId: "11",
        samplingSiteName: "Ward 1",
      },
    ),
  );
  await within(screen.getByTestId("sample-0")).findByText("Site: Ward 2");
  expect(previewMicrobiologyOrder).toHaveBeenCalledWith({
    domain: "ENVIRONMENTAL",
    samplingSiteId: "11",
    samplingSiteName: "Ward 1",
    specimens: [
      { sampleTypeId: "5", collectionLocationId: "12", testIds: ["culture"] },
    ],
  });
});
