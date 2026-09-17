import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../../languages/en.json";

const { getFromOpenElisServer } = vi.hoisted(() => ({
  getFromOpenElisServer: vi.fn(),
}));

vi.mock("../../../utils/Utils", () => ({ getFromOpenElisServer }));

import SampleTestSection from "./SampleTestSection";

// The sample-type-filtered catalogue the server returns for ST1: only T1 is
// configured for the sample type; the panel P1 additionally lists T2, which is
// NOT valid for ST1 (the OGC-1189 leak).
function mockPanelServer() {
  getFromOpenElisServer.mockImplementation((url, cb) => {
    if (url.startsWith("/rest/user-sample-types")) {
      cb([{ id: "ST1", value: "Serum" }]);
    } else if (url.startsWith("/rest/sample-type-tests")) {
      cb({
        tests: [{ id: "T1", name: "Alpha" }],
        panels: [{ id: "P1", name: "Panel One", testIds: "T1,T2" }],
      });
    }
  });
}

function renderPanelSection({ samples, setSamples }) {
  return render(
    <IntlProvider locale="en" messages={messages}>
      <SampleTestSection
        samples={samples}
        setSamples={setSamples}
        orderData={{}}
        setOrderData={vi.fn()}
        isReadOnly={false}
        workflowType="clinical"
      />
    </IntlProvider>,
  );
}

describe("SampleTestSection — panel selection (OGC-1189)", () => {
  beforeEach(() => {
    getFromOpenElisServer.mockReset();
    mockPanelServer();
  });

  it("adds only panel members configured for the sample type, dropping foreign ones", async () => {
    const setSamples = vi.fn();
    renderPanelSection({
      samples: [{ sampleTypeId: "ST1", tests: [], panels: [] }],
      setSamples,
    });

    const panelCheckbox = await screen.findByLabelText("Panel One");
    fireEvent.click(panelCheckbox);

    expect(setSamples).toHaveBeenCalled();
    const updatedSamples = setSamples.mock.calls.at(-1)[0];
    const addedTestIds = updatedSamples[0].tests.map((t) => t.id);

    expect(addedTestIds).toContain("T1"); // valid member added
    expect(addedTestIds).not.toContain("T2"); // foreign member dropped (the leak)
    // No raw-id-as-name fallback: every added test carries its catalogue name.
    expect(updatedSamples[0].tests).toEqual([{ id: "T1", name: "Alpha" }]);
  });
});

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
        microbiologyOrderDetail: undefined,
        sampleOrderItems: expect.objectContaining({ programId: "1" }),
      }),
    );
  });
});
