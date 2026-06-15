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
