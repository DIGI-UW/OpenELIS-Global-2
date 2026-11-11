import React from "react";
import { act, fireEvent, render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../languages/en.json";

// Stable mocks so each useContext() call returns the same spies across renders.
const { notificationMock } = vi.hoisted(() => ({
  notificationMock: {
    notificationVisible: false,
    setNotificationVisible: vi.fn(),
    addNotification: vi.fn(),
  },
}));

// Index.jsx pulls in the entire add-order tree (NotificationContext,
// AddSample, etc); we only need sampleObject here.
vi.mock("./Index", () => ({
  sampleObject: { index: 0 },
}));

// Layout.jsx pulls in the full app shell — we only consume NotificationContext.
vi.mock("../layout/Layout", () => ({
  NotificationContext: React.createContext(notificationMock),
}));

vi.mock("../common/CustomNotification", () => ({
  NotificationKinds: { success: "success", error: "error" },
}));

// OGC-285: OrderSuccessMessage re-reads the just-saved order's persisted
// order_label_request rows from GET /api/orders/by-accession/{labNo}/labels.
const { getFromOpenElisServerMock } = vi.hoisted(() => ({
  getFromOpenElisServerMock: vi.fn(),
}));
vi.mock("../utils/Utils", () => ({
  getFromOpenElisServer: getFromOpenElisServerMock,
}));

import OrderSuccessMessage from "./OrderSuccessMessage";

const renderWithIntl = (ui) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      {ui}
    </IntlProvider>,
  );

const baseProps = (overrides = {}) => ({
  orderFormValues: { sampleOrderItems: { labNo: "ACC-1" } },
  setOrderFormValues: vi.fn(),
  setSamples: vi.fn(),
  setPage: vi.fn(),
  saveResponse: null,
  ...overrides,
});

// Build a persisted-snapshot row in the endpoint's snake_case shape
// (OrderLabelRequestView).
const snapshotRow = ({
  presetId,
  parentSampleId,
  qty,
  name,
  heightMm = 25,
  widthMm = 50,
}) => ({
  preset_id: presetId,
  parent_sample_id: parentSampleId,
  qty,
  preset_snapshot: {
    preset: {
      id: presetId,
      name,
      height_mm: heightMm,
      width_mm: widthMm,
      barcode_type: "CODE_128",
    },
  },
});

// Make the endpoint mock resolve with the given rows for this test.
const mockLabelsEndpoint = (rows) => {
  getFromOpenElisServerMock.mockImplementation((endpoint, callback) => {
    expect(endpoint).toContain("/api/orders/by-accession/");
    expect(endpoint).toContain("/labels");
    callback(rows);
  });
};

describe("OrderSuccessMessage", () => {
  beforeEach(() => {
    notificationMock.addNotification.mockClear();
    notificationMock.setNotificationVisible.mockClear();
    getFromOpenElisServerMock.mockReset();
  });

  // Regression (caught by E2E US3): the real SamplePatientEntry parent applies
  // setOrderFormValues, and OrderSuccessMessage's own mount effect resets the
  // form — clearing sampleOrderItems.labNo. The print dialog must keep
  // rendering the just-saved order's rows after that reset (it unmounted via
  // PostSavePrintDialog's !accessionNumber guard). baseProps' inert vi.fn()
  // cannot reproduce the reset, so drive the component through live parent
  // state.
  test("print dialog survives the mount-effect form reset", async () => {
    mockLabelsEndpoint([
      snapshotRow({
        presetId: 1,
        parentSampleId: "501",
        qty: 2,
        name: "Order Label",
      }),
    ]);

    const StatefulParent = () => {
      const [orderFormValues, setOrderFormValues] = React.useState({
        sampleOrderItems: { labNo: "ACC-9" },
      });
      return (
        <OrderSuccessMessage
          orderFormValues={orderFormValues}
          setOrderFormValues={setOrderFormValues}
          setSamples={vi.fn()}
          setPage={vi.fn()}
        />
      );
    };

    renderWithIntl(<StatefulParent />);

    expect(await screen.findByText("Order Label")).toBeInTheDocument();
    expect(screen.getByText("ACC-9")).toBeInTheDocument();
  });

  test("renders one editable row per persisted snapshot, seeded to its saved qty", async () => {
    mockLabelsEndpoint([
      snapshotRow({
        presetId: 1,
        parentSampleId: "501",
        qty: 7,
        name: "Order Label",
      }),
      snapshotRow({
        presetId: 2,
        parentSampleId: "501",
        qty: 4,
        name: "Specimen Label",
      }),
    ]);

    renderWithIntl(<OrderSuccessMessage {...baseProps()} />);

    // Rows are labelled by the snapshot preset name; quantity is an editable
    // NumberInput seeded to the saved value, not static text.
    expect(await screen.findByText("Order Label")).toBeInTheDocument();
    expect(screen.getByText("Specimen Label")).toBeInTheDocument();
    const inputs = screen.getAllByRole("spinbutton");
    expect(inputs).toHaveLength(2);
    expect(inputs[0]).toHaveValue(7);
    expect(inputs[1]).toHaveValue(4);
  });

  test("Print opens the frozen-snapshot reprint endpoint (never LabelMakerServlet)", async () => {
    const openSpy = vi.spyOn(window, "open").mockReturnValue({});
    mockLabelsEndpoint([
      snapshotRow({
        presetId: 3,
        parentSampleId: "777",
        qty: 3,
        name: "Order Label",
      }),
    ]);

    renderWithIntl(<OrderSuccessMessage {...baseProps()} />);

    fireEvent.click(await screen.findByRole("button", { name: "Print" }));

    const calledUrl = openSpy.mock.calls[0][0];
    expect(calledUrl).toContain("/api/barcode/print/777/3");
    expect(calledUrl).not.toContain("LabelMakerServlet");

    openSpy.mockRestore();
  });

  test("falls back to a single Order entry when the order has no persisted snapshot", async () => {
    mockLabelsEndpoint([]);

    renderWithIntl(<OrderSuccessMessage {...baseProps()} />);

    // No-test order: one Order label fallback via LabelMakerServlet.
    expect(await screen.findByText("order")).toBeInTheDocument();
    const buttons = screen.getAllByRole("button", { name: "Print" });
    expect(buttons).toHaveLength(1);

    const openSpy = vi.spyOn(window, "open").mockReturnValue({});
    fireEvent.click(buttons[0]);
    const calledUrl = openSpy.mock.calls[0][0];
    expect(calledUrl).toContain("/LabelMakerServlet");
    expect(calledUrl).toContain("type=order");
    openSpy.mockRestore();
  });

  test("does not show the legacy fallback while the snapshot fetch is in flight", async () => {
    // Endpoint mock that never invokes the callback → persistedRequests stays
    // null (loading). The legacy LabelMakerServlet fallback must not flash
    // before the snapshot rows arrive — it bypasses the snapshot-freeze model.
    getFromOpenElisServerMock.mockImplementation(() => {});

    renderWithIntl(<OrderSuccessMessage {...baseProps()} />);

    // Done always renders; the in-flight fetch leaves no print rows behind it.
    await screen.findByRole("button", { name: /done/i });
    expect(screen.queryByText("order")).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Print" })).toBeNull();
  });

  test("an all-zero-quantity persist save shows no fallback (respects the user's choice)", async () => {
    // This save included a labelPersistRequest, but every quantity was 0, so
    // the backend intentionally stored no rows. The dialog must NOT fall back
    // to printing one Order label — the user chose none.
    let resolveLabels;
    getFromOpenElisServerMock.mockImplementation((endpoint, callback) => {
      resolveLabels = () => callback([]);
    });

    renderWithIntl(
      <OrderSuccessMessage
        {...baseProps({
          orderFormValues: {
            sampleOrderItems: { labNo: "ACC-1" },
            labelPersistRequest: { order_cells: [], sample_rows: [] },
          },
        })}
      />,
    );

    await act(async () => resolveLabels());
    expect(screen.queryByText("order")).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Print" })).toBeNull();
  });

  test("popup-blocked Print surfaces a notification toast", async () => {
    const openSpy = vi.spyOn(window, "open").mockReturnValue(null);
    const warnSpy = vi.spyOn(console, "warn").mockImplementation(() => {});
    mockLabelsEndpoint([
      snapshotRow({
        presetId: 1,
        parentSampleId: "501",
        qty: 1,
        name: "Order Label",
      }),
    ]);

    renderWithIntl(<OrderSuccessMessage {...baseProps()} />);

    fireEvent.click(await screen.findByRole("button", { name: "Print" }));

    expect(notificationMock.addNotification).toHaveBeenCalledTimes(1);
    expect(notificationMock.addNotification.mock.calls[0][0].kind).toBe(
      "error",
    );
    expect(notificationMock.setNotificationVisible).toHaveBeenCalledWith(true);

    openSpy.mockRestore();
    warnSpy.mockRestore();
  });

  test("Done resets the form and returns the user to page 0", async () => {
    const setOrderFormValues = vi.fn();
    const setSamples = vi.fn();
    const setPage = vi.fn();
    mockLabelsEndpoint([]);

    renderWithIntl(
      <OrderSuccessMessage
        {...baseProps({ setOrderFormValues, setSamples, setPage })}
      />,
    );

    // Wait for the fallback to settle so the dialog is fully rendered.
    await screen.findByRole("button", { name: "Done" });

    // Mount-time useEffect already invokes setOrderFormValues / setSamples;
    // capture those baselines so we assert only the click's contribution.
    const orderFormBaseline = setOrderFormValues.mock.calls.length;
    const samplesBaseline = setSamples.mock.calls.length;
    expect(setPage).not.toHaveBeenCalled();

    fireEvent.click(screen.getByRole("button", { name: "Done" }));

    expect(setPage).toHaveBeenCalledWith(0);
    expect(setOrderFormValues.mock.calls.length).toBe(orderFormBaseline + 1);
    expect(setSamples.mock.calls.length).toBe(samplesBaseline + 1);
  });

  test("re-reads labels from the accession-keyed endpoint on mount", async () => {
    mockLabelsEndpoint([]);

    renderWithIntl(<OrderSuccessMessage {...baseProps()} />);

    // Settle the mount-time fetch + fallback render before asserting.
    await screen.findByRole("button", { name: "Done" });

    expect(getFromOpenElisServerMock).toHaveBeenCalledWith(
      "/api/orders/by-accession/ACC-1/labels",
      expect.any(Function),
    );
  });
});
