import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import { vi } from "vitest";
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
  {
    currentSamples = [sample],
    orderData = {},
    setOrderData = vi.fn(),
    workflowType,
  } = {},
) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <SampleTestSection
        samples={currentSamples}
        setSamples={setSamples}
        orderData={orderData}
        setOrderData={setOrderData}
        isReadOnly={false}
        workflowType={workflowType}
      />
    </IntlProvider>,
  );

// ─────────────────────────────────────────────────────────────────────────────
// OGC-189: search behaviour. Kept in the same file as the suites above —
// they cover the same component from different angles and share no
// fixtures, so each suite carries its own mock setup.
// ─────────────────────────────────────────────────────────────────────────────

const WHOLE_BLOOD = "4";
const SERUM = "2";

const CATALOG = {
  [WHOLE_BLOOD]: {
    tests: [
      { id: "926", name: "Malaria Microscopy" },
      { id: "925", name: "Malaria Rapid Test" },
    ],
    panels: [{ id: "10", name: "Malaria Panel" }],
  },
  [SERUM]: {
    tests: [{ id: "500", name: "Glucose" }],
    panels: [{ id: "20", name: "Liver Panel" }],
  },
};

function renderSearchSection(sampleTypeId = WHOLE_BLOOD) {
  const samples = [
    { index: 0, sampleTypeId, tests: [], panels: [], sampleRejected: false },
  ];
  return render(
    <IntlProvider locale="en" messages={messages}>
      <SampleTestSection
        samples={samples}
        setSamples={vi.fn()}
        orderData={{ sampleOrderItems: {} }}
        setOrderData={vi.fn()}
        isReadOnly={false}
        workflowType="clinical"
      />
    </IntlProvider>,
  );
}

/**
 * Carbon's <Search> does not react to fireEvent.change — it listens for the
 * events a real keyboard produces, so state never updates and the assertions
 * pass or fail for the wrong reason. Type (and clear) the way a user would.
 */
async function typeSearch(input, value) {
  await userEvent.clear(input);
  if (value) {
    await userEvent.type(input, value);
  }
}

/**
 * Regression: searching for a test that does not exist used to break the
 * screen permanently.
 *
 * The <Search> input was rendered INSIDE the `filteredTests.length > 0`
 * branch, so a term matching nothing unmounted the search box along with the
 * list — leaving the term stuck in state with no control able to clear it.
 * Changing sample type did not help either, because nothing reset the term.
 */
describe("SampleTestSection — search with no matches", () => {
  beforeEach(() => {
    getFromOpenElisServer.mockReset();
    getFromOpenElisServer.mockImplementation((url, callback) => {
      const match = /sampleType=(\d+)/.exec(url);
      if (url.startsWith("/rest/sample-type-tests") && match) {
        callback(CATALOG[match[1]] || { tests: [], panels: [] });
      }
    });
  });

  it("keeps the test search box mounted when nothing matches", async () => {
    renderSearchSection();
    const search = await screen.findByPlaceholderText(/Choose Available Test/i);

    await typeSearch(search, "zzz");

    // The input must survive — it is the only way back out.
    await waitFor(() =>
      expect(
        screen.getByPlaceholderText(/Choose Available Test/i),
      ).toBeInTheDocument(),
    );
    expect(screen.getByText("No tests match your search")).toBeInTheDocument();
  });

  it("distinguishes 'no match' from 'none for this sample type'", async () => {
    renderSearchSection();
    const search = await screen.findByPlaceholderText(/Choose Available Test/i);

    await typeSearch(search, "zzz");

    // Blaming the sample type sent users hunting for a config problem that
    // did not exist.
    await waitFor(() =>
      expect(
        screen.getByText("No tests match your search"),
      ).toBeInTheDocument(),
    );
    expect(
      screen.queryByText("No tests available for this sample type"),
    ).not.toBeInTheDocument();
  });

  it("clearing the term brings every test back", async () => {
    renderSearchSection();
    const search = await screen.findByPlaceholderText(/Choose Available Test/i);

    await typeSearch(search, "zzz");
    await waitFor(() =>
      expect(
        screen.getByText("No tests match your search"),
      ).toBeInTheDocument(),
    );

    await typeSearch(search, "");

    await waitFor(() =>
      expect(screen.getByLabelText("Malaria Microscopy")).toBeInTheDocument(),
    );
    expect(screen.getByLabelText("Malaria Rapid Test")).toBeInTheDocument();
  });

  it("keeps the panel search box mounted when nothing matches", async () => {
    renderSearchSection();
    const panelSearch = await screen.findByPlaceholderText(
      /Choose Available panel/i,
    );

    await typeSearch(panelSearch, "qqq");

    // The panels block carried the identical flaw.
    await waitFor(() =>
      expect(
        screen.getByPlaceholderText(/Choose Available panel/i),
      ).toBeInTheDocument(),
    );
    expect(screen.getByText("No panels match your search")).toBeInTheDocument();
  });

  it("changing sample type clears a stale search term", async () => {
    // The other half of the bug: nothing reset the term, so a refetch
    // filtered the NEW sample type's tests against the old text and showed
    // nothing — with no way back.
    const { rerender } = renderSearchSection(WHOLE_BLOOD);
    const search = await screen.findByPlaceholderText(/Choose Available Test/i);
    await typeSearch(search, "zzz");
    await waitFor(() =>
      expect(
        screen.getByText("No tests match your search"),
      ).toBeInTheDocument(),
    );

    rerender(
      <IntlProvider locale="en" messages={messages}>
        <SampleTestSection
          samples={[
            {
              index: 0,
              sampleTypeId: SERUM,
              tests: [],
              panels: [],
              sampleRejected: false,
            },
          ]}
          setSamples={vi.fn()}
          orderData={{ sampleOrderItems: {} }}
          setOrderData={vi.fn()}
          isReadOnly={false}
          workflowType="clinical"
        />
      </IntlProvider>,
    );

    await waitFor(() =>
      expect(screen.getByLabelText("Glucose")).toBeInTheDocument(),
    );
    expect(screen.getByPlaceholderText(/Choose Available Test/i)).toHaveValue(
      "",
    );
  });

  it("a search matching nothing still filters correctly when it does match", async () => {
    renderSearchSection();
    const search = await screen.findByPlaceholderText(/Choose Available Test/i);

    // Positive control: without this, a render that dropped every test would
    // satisfy the no-match assertions above vacuously.
    await typeSearch(search, "microscopy");

    await waitFor(() =>
      expect(screen.getByLabelText("Malaria Microscopy")).toBeInTheDocument(),
    );
    expect(
      screen.queryByLabelText("Malaria Rapid Test"),
    ).not.toBeInTheDocument();
  });
});

describe("SampleTestSection selected-tag close buttons", () => {
  const catalogue = {
    tests: [cultureTest],
    panels: [{ id: "9", name: "Sepsis panel", testIds: "42" }],
  };
  const selected = {
    ...sample,
    tests: [cultureTest],
    panels: [{ id: "9", name: "Sepsis panel" }],
  };

  beforeEach(() => {
    getFromOpenElisServer.mockReset();
  });

  // The tags used to pass dismissTooltipLabel alone, so every close button was
  // named Carbon's English "Dismiss"; see removeLabel in the component for why
  // both props are needed.
  it("names each selected test and panel's close button after what it removes", async () => {
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url === "/rest/user-sample-types") {
        callback([{ id: "5", value: "Blood" }]);
      }
      if (url === "/rest/sample-type-tests?sampleType=5") {
        callback(catalogue);
      }
    });
    renderSection(vi.fn(), { currentSamples: [selected] });

    await screen.findAllByText("Blood culture");

    expect(
      screen.getByRole("button", { name: "Remove Blood culture" }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: "Remove Sepsis panel" }),
    ).toBeInTheDocument();
    expect(
      screen.queryByRole("button", { name: "Dismiss" }),
    ).not.toBeInTheDocument();
  });

  it("names them the same way in the per-sample manifest picker", async () => {
    const user = userEvent.setup();
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url === "/rest/environmental-sample-types") {
        callback([{ id: "5", value: "Blood" }]);
      }
      if (url === "/rest/vector/dictionary/sample-containers") {
        callback([]);
      }
      if (url === "/rest/sample-type-tests?sampleType=5") {
        callback(catalogue);
      }
    });
    renderSection(vi.fn(), {
      currentSamples: [selected],
      workflowType: "environmental",
    });

    await user.click(
      await screen.findByRole("button", { name: /Tests & Panels/ }),
    );

    expect(
      screen.getByRole("button", { name: "Remove Blood culture" }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: "Remove Sepsis panel" }),
    ).toBeInTheDocument();
    expect(
      screen.queryByRole("button", { name: "Dismiss" }),
    ).not.toBeInTheDocument();
  });
});
