import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../../languages/en.json";
import SampleTestSection from "./SampleTestSection";

const { getFromOpenElisServerMock } = vi.hoisted(() => ({
  getFromOpenElisServerMock: vi.fn(),
}));

vi.mock("../../../utils/Utils", () => ({
  getFromOpenElisServer: (...args) => getFromOpenElisServerMock(...args),
}));

// The sample-type-filtered catalogue the server returns for ST1: only T1 is
// configured for the sample type; the panel P1 additionally lists T2, which is
// NOT valid for ST1 (the OGC-1189 leak).
function mockServer() {
  getFromOpenElisServerMock.mockImplementation((url, cb) => {
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

function renderSection({ samples, setSamples }) {
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
    getFromOpenElisServerMock.mockReset();
    mockServer();
  });

  it("adds only panel members configured for the sample type, dropping foreign ones", async () => {
    const setSamples = vi.fn();
    renderSection({
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
