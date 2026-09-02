import React from "react";
import { fireEvent, render, screen, within } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../languages/en.json";

// ---------------------------------------------------------------------------
// OGC-285 M5b — AddOrder mounts ONE order-level LabelsSection (API mode), fed by
// POST /api/orderEntry/labelRequest, and lifts the section's persistPayload onto
// orderFormValues.labelPersistRequest so Index.jsx's save POST carries it.
//
// We mock the server layer (so the aggregation POST returns a canned response
// and the on-mount GETs are inert) and the heavy presentational children that
// are irrelevant to this wiring, then assert on the real rendered output and the
// real setOrderFormValues call.
// ---------------------------------------------------------------------------

const { utilsMock } = vi.hoisted(() => ({
  utilsMock: {
    getFromOpenElisServer: vi.fn(),
    postToOpenElisServerFormData: vi.fn(),
    postToOpenElisServerJsonResponse: vi.fn(),
    deleteFromOpenElisServer: vi.fn(),
  },
}));

vi.mock("../utils/Utils", () => utilsMock);

vi.mock("../layout/Layout", () => ({
  NotificationContext: React.createContext({
    notificationVisible: false,
    setNotificationVisible: vi.fn(),
    addNotification: vi.fn(),
  }),
  ConfigurationContext: React.createContext({
    configurationProperties: { restrictFreeTextProviderEntry: "false" },
  }),
}));

vi.mock("../common/CustomNotification", () => ({
  NotificationKinds: { success: "success", error: "error", warning: "warning" },
}));

// Heavy / unrelated children — replaced with inert stubs so the render is light
// and deterministic. None participate in the label-aggregation wiring.
vi.mock("../common/AutoComplete", () => ({ default: () => <div /> }));
vi.mock("../common/CustomDatePicker", () => ({ default: () => <div /> }));
vi.mock("../common/CustomTimePicker", () => ({ default: () => <div /> }));
vi.mock("../common/CustomLabNumberInput", () => ({ default: () => <div /> }));
vi.mock("./OrderResultReporting", () => ({ default: () => <div /> }));

import AddOrder from "./AddOrder";

// Mirrors the POST /api/orderEntry/labelRequest response shape (snake_case wire
// keys). sample_id_local "0"/"1" are the positional keys AddOrder sends.
const labelRequestFixture = () => ({
  order_columns: [
    { preset_id: 1, name: "Order Label", is_system: true, max: 10 },
  ],
  sample_columns: [
    { preset_id: 17, name: "Specimen Label", is_system: true, max: 5 },
  ],
  order_row: {
    cells: [
      {
        preset_id: 1,
        default: 2,
        max: 10,
        locked: false,
        source: "preset_default",
      },
    ],
  },
  sample_rows: [
    {
      sample_id_local: "0",
      cells: [
        {
          preset_id: 17,
          default: 1,
          max: 5,
          locked: false,
          source: "test",
          source_test_id: 1,
          source_test_name: "CBC",
        },
      ],
    },
    {
      sample_id_local: "1",
      cells: [
        {
          preset_id: 17,
          default: 1,
          max: 5,
          locked: false,
          source: "test",
          source_test_id: 2,
          source_test_name: "ESR",
        },
      ],
    },
  ],
});

// Two samples, each with one test — the filtered list the backend correlates by.
const samplesFixture = () => [
  {
    index: 1,
    sampleTypeId: "3",
    name: "Blood",
    tests: [{ id: "1", name: "CBC" }],
    panels: [],
    referralItems: [],
    sampleXML: {},
  },
  {
    index: 2,
    sampleTypeId: "5",
    name: "Urine",
    tests: [{ id: "2", name: "ESR" }],
    panels: [],
    referralItems: [],
    sampleXML: {},
  },
];

const baseOrderFormValues = () => ({
  sampleOrderItems: {
    labNo: "",
    providersList: [],
    paymentOptions: [],
    referringSiteList: [],
    testLocationCodeList: [],
  },
  patientProperties: {},
});

const renderAddOrder = (overrides = {}) => {
  const setOrderFormValues = vi.fn();
  const utils = render(
    <IntlProvider locale="en" messages={messages}>
      <AddOrder
        orderFormValues={baseOrderFormValues()}
        setOrderFormValues={setOrderFormValues}
        samples={samplesFixture()}
        error={() => null}
        isModifyOrder={false}
        changed={{}}
        setChanged={vi.fn()}
        stagedAttachments={[]}
        setStagedAttachments={vi.fn()}
        {...overrides}
      />
    </IntlProvider>,
  );
  return { setOrderFormValues, ...utils };
};

// Resolve every labelRequest POST callback with the fixture; ignore other POSTs.
const wireAggregationResponse = (response) => {
  utilsMock.postToOpenElisServerJsonResponse.mockImplementation(
    (endPoint, _body, callback) => {
      if (endPoint === "/api/orderEntry/labelRequest") {
        callback(response);
      }
    },
  );
};

describe("AddOrder — order-level label aggregation (OGC-285 M5b)", () => {
  beforeEach(() => {
    utilsMock.getFromOpenElisServer.mockReset();
    utilsMock.postToOpenElisServerJsonResponse.mockReset();
    utilsMock.getFromOpenElisServer.mockImplementation(() => {});
  });

  test("POSTs the aggregation with positional sample_id_local + deduped numeric test ids", () => {
    wireAggregationResponse(labelRequestFixture());
    renderAddOrder();

    const call = utilsMock.postToOpenElisServerJsonResponse.mock.calls.find(
      (c) => c[0] === "/api/orderEntry/labelRequest",
    );
    expect(call).toBeTruthy();
    const body = JSON.parse(call[1]);
    expect(body.test_ids).toEqual([1, 2]);
    expect(body.samples).toEqual([
      { sample_id_local: "0", sample_type: "3" },
      { sample_id_local: "1", sample_type: "5" },
    ]);
  });

  test("renders the API-mode order-level LabelsSection from the response", () => {
    wireAggregationResponse(labelRequestFixture());
    renderAddOrder();

    // The order-level section heading + both dynamic tables render.
    expect(screen.getByText("LABELS")).toBeInTheDocument();
    expect(screen.getByText("Order Labels")).toBeInTheDocument();
    expect(screen.getByText("Sample Labels")).toBeInTheDocument();

    // Column headers come straight from the aggregation response.
    const tables = screen.getAllByRole("table");
    expect(tables).toHaveLength(2);
    expect(
      within(tables[0]).getByRole("columnheader", { name: "Order Label" }),
    ).toBeInTheDocument();
    expect(
      within(tables[1]).getByRole("columnheader", { name: "Specimen Label" }),
    ).toBeInTheDocument();

    // Sample rows are labelled by the sample type name (our formatter).
    expect(
      within(tables[1]).getByRole("rowheader", { name: "Blood" }),
    ).toBeInTheDocument();
    expect(
      within(tables[1]).getByRole("rowheader", { name: "Urine" }),
    ).toBeInTheDocument();
  });

  test("does not render the section when no sample carries tests", () => {
    wireAggregationResponse(labelRequestFixture());
    renderAddOrder({
      samples: [
        {
          index: 1,
          sampleTypeId: "3",
          name: "Blood",
          tests: [],
          panels: [],
          referralItems: [],
          sampleXML: {},
        },
      ],
    });

    expect(screen.queryByText("LABELS")).not.toBeInTheDocument();
    expect(
      utilsMock.postToOpenElisServerJsonResponse.mock.calls.some(
        (c) => c[0] === "/api/orderEntry/labelRequest",
      ),
    ).toBe(false);
  });

  test("lifts the chosen quantities onto orderFormValues.labelPersistRequest on edit", () => {
    wireAggregationResponse(labelRequestFixture());
    const { setOrderFormValues } = renderAddOrder();

    // Edit a sample-label quantity (Sample 1 / Specimen Label) to 4.
    const tables = screen.getAllByRole("table");
    const sampleInput = within(tables[1]).getByRole("spinbutton", {
      name: /Sample 1 Specimen Label quantity/i,
    });
    fireEvent.change(sampleInput, { target: { value: "4" } });

    // setOrderFormValues was called with a functional updater that injects the
    // persistPayload as the top-level labelPersistRequest.
    const updater = setOrderFormValues.mock.calls
      .map((c) => c[0])
      .reverse()
      .find((arg) => typeof arg === "function");
    expect(updater).toBeTruthy();

    const next = updater(baseOrderFormValues());
    expect(next.labelPersistRequest).toBeTruthy();
    expect(next.labelPersistRequest.order_cells).toEqual([
      { preset_id: 1, qty: 2 },
    ]);
    // The edited sample-0 cell is 4; sample-1 stays at its seeded default 1.
    expect(next.labelPersistRequest.sample_rows).toEqual([
      { sample_id_local: "0", cells: [{ preset_id: 17, qty: 4 }] },
      { sample_id_local: "1", cells: [{ preset_id: 17, qty: 1 }] },
    ]);
  });
});

// ---------------------------------------------------------------------------
// Result Reporting sample headings — a sample is named by the specimen the user
// chose on it, not only by the position of the box it sits in. "Sample 2" says
// nothing about what is in it; on an order carrying three specimens the reader
// has to scroll back up and count to find out.
// ---------------------------------------------------------------------------
describe("AddOrder — Result Reporting sample headings", () => {
  const SAMPLE_TYPES = [
    { id: "3", value: "Sputum" },
    { id: "5", value: "Serum" },
    { id: "7", value: "Plasma" },
  ];

  const wireSampleTypes = (types = SAMPLE_TYPES) => {
    utilsMock.getFromOpenElisServer.mockImplementation((endPoint, callback) => {
      if (endPoint === "/rest/user-sample-types") {
        callback(types);
      }
    });
  };

  beforeEach(() => {
    utilsMock.getFromOpenElisServer.mockReset();
    utilsMock.postToOpenElisServerJsonResponse.mockReset();
    wireSampleTypes();
    wireAggregationResponse(labelRequestFixture());
  });

  const heading = (text) =>
    screen
      .getAllByRole("heading", { level: 4 })
      .find((node) => node.textContent.replace(/\s+/g, " ").trim() === text);

  test("names each sample by the specimen type selected on it", () => {
    renderAddOrder();

    expect(heading("Sample 1: Sputum")).toBeInTheDocument();
    expect(heading("Sample 2: Serum")).toBeInTheDocument();
  });

  test("supports any number of samples", () => {
    renderAddOrder({
      samples: [
        ...samplesFixture(),
        {
          index: 3,
          sampleTypeId: "7",
          tests: [{ id: "3", name: "LFT" }],
          panels: [],
          referralItems: [],
          sampleXML: {},
        },
      ],
    });

    expect(heading("Sample 1: Sputum")).toBeInTheDocument();
    expect(heading("Sample 2: Serum")).toBeInTheDocument();
    expect(heading("Sample 3: Plasma")).toBeInTheDocument();
  });

  test("follows the picker when the selected specimen changes", () => {
    const { rerender } = renderAddOrder();
    expect(heading("Sample 1: Sputum")).toBeInTheDocument();

    const reselected = samplesFixture();
    reselected[0].sampleTypeId = "7";
    rerender(
      <IntlProvider locale="en" messages={messages}>
        <AddOrder
          orderFormValues={baseOrderFormValues()}
          setOrderFormValues={vi.fn()}
          samples={reselected}
          error={() => null}
          isModifyOrder={false}
          changed={{}}
          setChanged={vi.fn()}
          stagedAttachments={[]}
          setStagedAttachments={vi.fn()}
        />
      </IntlProvider>,
    );

    expect(heading("Sample 1: Plasma")).toBeInTheDocument();
    expect(heading("Sample 1: Sputum")).toBeUndefined();
  });

  test("keeps the plain numbering when no specimen is selected yet", () => {
    renderAddOrder({
      samples: [
        {
          index: 1,
          sampleTypeId: "",
          tests: [{ id: "1", name: "CBC" }],
          panels: [],
          referralItems: [],
          sampleXML: {},
        },
      ],
    });

    expect(heading("Sample 1")).toBeInTheDocument();
  });

  test("does not invent a name for a specimen the list does not carry", () => {
    renderAddOrder({
      samples: [
        {
          index: 1,
          sampleTypeId: "999",
          tests: [{ id: "1", name: "CBC" }],
          panels: [],
          referralItems: [],
          sampleXML: {},
        },
      ],
    });

    expect(heading("Sample 1")).toBeInTheDocument();
  });
});

// ---------------------------------------------------------------------------
// OGC-1191 — Editing an existing order must never SILENTLY reassign the
// specimen's accession number. On the modify path the inline editable Lab
// Number input (bound to newAccessionNumber, the SampleEdit reassignment field)
// and its inline "Generate" link are not rendered; the number is shown as
// static text. Reassignment is still possible, but only as a deliberate action:
// a "Reassign Lab Number" button opens a confirmation dialog, and only an
// explicit Confirm writes the candidate to newAccessionNumber. On the add path
// the inline input + Generate link are unchanged and no Reassign button shows.
// ---------------------------------------------------------------------------
describe("AddOrder — Lab Number reassignment is deliberate on modify (OGC-1191)", () => {
  beforeEach(() => {
    utilsMock.getFromOpenElisServer.mockReset();
    utilsMock.postToOpenElisServerJsonResponse.mockReset();
    utilsMock.getFromOpenElisServer.mockImplementation(() => {});
    utilsMock.postToOpenElisServerJsonResponse.mockImplementation(() => {});
  });

  const modifyValues = (extra = {}) => ({
    ...baseOrderFormValues(),
    accessionNumber: "DEV01260000000000519",
    newAccessionNumber: "",
    ...extra,
  });

  test("add order offers the inline Generate accession link and no Reassign button", () => {
    renderAddOrder({ isModifyOrder: false });
    expect(
      document.querySelector('[data-cy="generate-labNumber"]'),
    ).toBeInTheDocument();
    expect(
      document.querySelector('[data-cy="reassign-labNumber-open"]'),
    ).not.toBeInTheDocument();
  });

  test("modify order does NOT offer the inline Generate link", () => {
    renderAddOrder({ isModifyOrder: true, orderFormValues: modifyValues() });
    expect(
      document.querySelector('[data-cy="generate-labNumber"]'),
    ).not.toBeInTheDocument();
  });

  test("modify order shows the existing accession number as static text", () => {
    renderAddOrder({ isModifyOrder: true, orderFormValues: modifyValues() });
    const headings = screen.getAllByRole("heading", { level: 5 });
    expect(
      headings.some((h) => h.textContent.includes("DEV01260000000000519")),
    ).toBe(true);
  });

  test("modify order offers a deliberate Reassign Lab Number button", () => {
    renderAddOrder({ isModifyOrder: true, orderFormValues: modifyValues() });
    expect(
      document.querySelector('[data-cy="reassign-labNumber-open"]'),
    ).toBeInTheDocument();
  });

  test("confirming the dialog stages the generated number onto newAccessionNumber", () => {
    utilsMock.getFromOpenElisServer.mockImplementation((endPoint, cb) => {
      if (endPoint === "/rest/SampleEntryGenerateScanProvider") {
        cb({ status: true, body: "DEV01260000000000777" });
      }
    });
    const { setOrderFormValues } = renderAddOrder({
      isModifyOrder: true,
      orderFormValues: modifyValues(),
    });

    fireEvent.click(
      document.querySelector('[data-cy="reassign-labNumber-open"]'),
    );
    // Generate a candidate inside the confirmation dialog...
    fireEvent.click(
      document.querySelector('[data-cy="reassign-generate-labNumber"]'),
    );
    // ...then explicitly confirm (the danger modal's primary button).
    fireEvent.click(screen.getByRole("button", { name: /Reassign$/ }));

    const staged = setOrderFormValues.mock.calls
      .map((c) => c[0])
      .reverse()
      .find((arg) => arg && "newAccessionNumber" in arg);
    expect(staged.newAccessionNumber).toBe("DEV01260000000000777");
  });

  test("a staged reassignment shows a pending warning with an Undo that clears it", () => {
    const { setOrderFormValues } = renderAddOrder({
      isModifyOrder: true,
      orderFormValues: modifyValues({
        newAccessionNumber: "DEV01260000000000777",
      }),
    });

    expect(
      screen.getByText("Pending Lab Number reassignment"),
    ).toBeInTheDocument();
    expect(screen.getByText(/DEV01260000000000777/)).toBeInTheDocument();

    fireEvent.click(
      document.querySelector('[data-cy="reassign-labNumber-undo"]'),
    );
    const cleared = setOrderFormValues.mock.calls
      .map((c) => c[0])
      .reverse()
      .find((arg) => arg && "newAccessionNumber" in arg);
    expect(cleared.newAccessionNumber).toBe("");
  });
});
