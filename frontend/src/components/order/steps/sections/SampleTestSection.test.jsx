import React from "react";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { IntlProvider } from "react-intl";
import messages from "../../../../languages/en.json";

const { getFromOpenElisServer } = vi.hoisted(() => ({
  getFromOpenElisServer: vi.fn(),
}));

vi.mock("../../../utils/Utils", () => ({ getFromOpenElisServer }));

import SampleTestSection from "./SampleTestSection";

const cultureTest = {
  id: "42",
  name: "Blood culture",
  cultureWorkflowType: "BACTERIOLOGY",
  methods: [
    {
      methodId: "7",
      methodName: "Blood Culture Standard",
      methodCode: "BCSTD",
      isDefault: true,
    },
  ],
};

const sample = {
  index: 0,
  sampleTypeId: "5",
  sampleTypeName: "Blood",
  panels: [],
  tests: [],
  requestReferralEnabled: false,
  referralItems: [],
};

const renderSection = (
  setSamples,
  { currentSamples = [sample], orderData = {}, setOrderData = vi.fn() } = {},
) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <SampleTestSection
        samples={currentSamples}
        setSamples={setSamples}
        orderData={orderData}
        setOrderData={setOrderData}
        isReadOnly={false}
      />
    </IntlProvider>,
  );

describe("SampleTestSection microbiology metadata", () => {
  beforeEach(() => {
    getFromOpenElisServer.mockReset();
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url === "/rest/user-sample-types") {
        callback([{ id: "5", value: "Blood" }]);
      }
      if (url === "/rest/sample-type-tests?sampleType=5") {
        callback({ tests: [cultureTest], panels: [] });
      }
    });
  });

  it("retains workflow and linked Method metadata when a test is selected", async () => {
    const user = userEvent.setup();
    const setSamples = vi.fn();
    renderSection(setSamples);

    await screen.findAllByText("Blood culture");
    await user.click(document.querySelector('label[for="test-0-42"]'));

    expect(setSamples).toHaveBeenLastCalledWith([
      expect.objectContaining({
        tests: [cultureTest],
      }),
    ]);
  });

  it("retains the same metadata when a panel selects the culture test", async () => {
    const user = userEvent.setup();
    const setSamples = vi.fn();
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url === "/rest/user-sample-types") {
        callback([{ id: "5", value: "Blood" }]);
      }
      if (url === "/rest/sample-type-tests?sampleType=5") {
        callback({
          tests: [cultureTest],
          panels: [{ id: "9", name: "Sepsis panel", testIds: "42" }],
        });
      }
    });
    renderSection(setSamples);

    await user.click(
      (await screen.findByText("Sepsis panel")).closest("label"),
    );

    expect(setSamples).toHaveBeenLastCalledWith([
      expect.objectContaining({
        tests: [cultureTest],
      }),
    ]);
  });

  it("confirms before discarding details with the final culture test", async () => {
    const user = userEvent.setup();
    const setSamples = vi.fn();
    const setOrderData = vi.fn();
    renderSection(setSamples, {
      currentSamples: [{ ...sample, tests: [cultureTest] }],
      orderData: {
        microbiologyOrderDetail: {
          cultureMethodId: "7",
          clinicalHistory: "Fever and hypotension",
        },
        sampleOrderItems: {
          programId: "8",
          microbiologyPreviousProgramId: "1",
        },
      },
      setOrderData,
    });

    await screen.findAllByText("Blood culture");
    await user.click(document.querySelector('label[for="test-0-42"]'));

    expect(
      screen.getByRole("heading", { name: "Remove microbiology workflow?" }),
    ).toBeInTheDocument();
    expect(setSamples).not.toHaveBeenCalled();

    await user.click(screen.getByRole("button", { name: /Discard details$/ }));

    expect(setSamples).toHaveBeenCalledWith([
      expect.objectContaining({ tests: [] }),
    ]);
    const clearOrder = setOrderData.mock.calls.at(-1)[0];
    expect(
      clearOrder({
        microbiologyOrderDetail: { clinicalHistory: "Fever" },
        sampleOrderItems: {
          programId: "8",
          microbiologyPreviousProgramId: "1",
        },
      }),
    ).toEqual(
      expect.objectContaining({
        microbiologyOrderDetail: expect.objectContaining({
          cultureMethodId: "",
          clinicalHistory: "",
        }),
        sampleOrderItems: expect.objectContaining({ programId: "1" }),
      }),
    );
  });
});
