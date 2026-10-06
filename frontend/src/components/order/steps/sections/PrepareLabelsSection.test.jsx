import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { vi } from "vitest";
import messages from "../../../../languages/en.json";

vi.mock("../../../utils/Utils", () => ({
  postToOpenElisServerJsonResponse: vi.fn(),
}));

const orderContext = {
  orderId: "77",
  labNumber: "DEV0126000000000099",
  samples: [],
  isDirty: false,
  saveStatus: "idle",
  setLabelPersistRequest: vi.fn(),
};

vi.mock("../../OrderContext", () => ({
  useOrderContext: () => orderContext,
  SaveStatus: {
    IDLE: "idle",
    SAVING: "saving",
    SAVED: "saved",
    ERROR: "error",
  },
}));

vi.mock("../../../layout/Layout", async () => {
  const { createContext } = await import("react");
  return {
    NotificationContext: createContext({
      addNotification: () => {},
      setNotificationVisible: () => {},
    }),
  };
});

import { postToOpenElisServerJsonResponse } from "../../../utils/Utils";
import { NotificationContext } from "../../../layout/Layout";
import PrepareLabelsSection, {
  buildLabelRequestBody,
  labelSamplesOf,
  localIdOf,
} from "./PrepareLabelsSection";

const aggregation = () => ({
  order_columns: [
    { preset_id: 1, name: "Order Label", is_system: true, max: 10 },
  ],
  sample_columns: [
    { preset_id: 2, name: "Specimen Label", is_system: true, max: 10 },
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
      sample_id_local: "item-501",
      cells: [
        {
          preset_id: 2,
          default: 1,
          max: 10,
          locked: false,
          source: "preset_default",
        },
      ],
    },
  ],
});

const savedSample = {
  index: 0,
  sampleItemId: "501",
  sampleTypeId: "12",
  sampleTypeName: "Serum",
  tests: [{ id: "7", name: "Glucose" }],
};

const renderSection = (props = {}, notification = {}) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <NotificationContext.Provider
        value={{
          addNotification: vi.fn(),
          setNotificationVisible: vi.fn(),
          ...notification,
        }}
      >
        <PrepareLabelsSection {...props} />
      </NotificationContext.Provider>
    </IntlProvider>,
  );

describe("PrepareLabelsSection helpers", () => {
  test("labelSamplesOf keeps physical tubes with their positions", () => {
    const samples = [
      savedSample,
      { sampleTypeId: "3", qcMetadata: { qcType: "BLANK" } },
      { sampleTypeId: "4", sampleRejected: true },
      { sampleTypeId: "" },
      { sampleTypeId: "5", sampleItemId: "" },
    ];
    expect(labelSamplesOf(samples).map((e) => e.index)).toEqual([0, 4]);
  });

  test("localIdOf addresses a saved tube by item id and an unsaved one by position", () => {
    expect(localIdOf({ sample: savedSample, index: 3 })).toBe("item-501");
    expect(localIdOf({ sample: { sampleTypeId: "5" }, index: 3 })).toBe("3");
  });

  test("buildLabelRequestBody carries the tests of the order and of each tube", () => {
    const body = buildLabelRequestBody(labelSamplesOf([savedSample]));
    expect(body).toEqual({
      test_ids: [7],
      samples: [
        { sample_id_local: "item-501", sample_type: "12", test_ids: [7] },
      ],
    });
  });
});

describe("PrepareLabelsSection", () => {
  let openedWindow;

  beforeEach(() => {
    vi.clearAllMocks();
    orderContext.samples = [savedSample];
    orderContext.isDirty = false;
    orderContext.saveStatus = "idle";
    orderContext.orderId = "77";
    openedWindow = { location: { href: "" }, close: vi.fn() };
    vi.spyOn(window, "open").mockReturnValue(openedWindow);
    global.URL.createObjectURL = vi.fn(() => "blob:labels");
    global.fetch = vi.fn();
    postToOpenElisServerJsonResponse.mockImplementation((url, body, cb) =>
      cb(aggregation()),
    );
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  test("shows the empty hint when the order has no tube yet", () => {
    orderContext.samples = [];
    renderSection();
    expect(
      screen.getByText(messages["orderEntry.labels.empty"]),
    ).toBeInTheDocument();
    expect(postToOpenElisServerJsonResponse).not.toHaveBeenCalled();
  });

  test("loads the presets for the tubes and stages their default quantities for the save", async () => {
    renderSection();
    await waitFor(() => {
      expect(postToOpenElisServerJsonResponse).toHaveBeenCalledWith(
        "/api/orderEntry/labelRequest",
        JSON.stringify({
          test_ids: [7],
          samples: [
            { sample_id_local: "item-501", sample_type: "12", test_ids: [7] },
          ],
        }),
        expect.any(Function),
      );
    });
    expect(screen.getAllByText("Order Label").length).toBeGreaterThan(0);
    expect(screen.getAllByText("Specimen Label").length).toBeGreaterThan(0);
    expect(screen.getByText("DEV0126000000000099-1 Serum")).toBeInTheDocument();
    expect(orderContext.setLabelPersistRequest).toHaveBeenCalledWith({
      order_cells: [{ preset_id: 1, qty: 2 }],
      sample_rows: [
        { sample_id_local: "item-501", cells: [{ preset_id: 2, qty: 1 }] },
      ],
    });
    expect(screen.getByTestId("labels-pending-save")).toBeInTheDocument();
  });

  test("printing a tube saves the step first, then opens that tube's PDF in the window opened on click", async () => {
    const onSaveBeforePrint = vi.fn().mockResolvedValue(true);
    global.fetch.mockResolvedValue({
      ok: true,
      status: 200,
      blob: () =>
        Promise.resolve(new Blob(["%PDF"], { type: "application/pdf" })),
    });
    renderSection({ onSaveBeforePrint });
    await screen.findByTestId("sample-label-print-row-item-501");

    fireEvent.click(screen.getByTestId("sample-label-print-row-item-501"));

    await waitFor(() => {
      expect(global.fetch).toHaveBeenCalledWith(
        "/api/OpenELIS-Global/api/orders/77/labels/pdf?sampleItemId=501&scope=sample",
        { credentials: "include" },
      );
    });
    expect(onSaveBeforePrint).toHaveBeenCalledTimes(1);
    expect(window.open).toHaveBeenCalledWith("", "_blank");
    await waitFor(() => expect(openedWindow.location.href).toBe("blob:labels"));
    expect(openedWindow.close).not.toHaveBeenCalled();
  });

  test("print column and print all narrow the PDF by preset and scope", async () => {
    const onSaveBeforePrint = vi.fn().mockResolvedValue(true);
    global.fetch.mockResolvedValue({
      ok: true,
      status: 200,
      blob: () => Promise.resolve(new Blob(["%PDF"])),
    });
    renderSection({ onSaveBeforePrint });
    await screen.findByTestId("order-label-print-col-1");

    fireEvent.click(screen.getByTestId("order-label-print-col-1"));
    await waitFor(() =>
      expect(global.fetch).toHaveBeenLastCalledWith(
        "/api/OpenELIS-Global/api/orders/77/labels/pdf?presetId=1&scope=order",
        { credentials: "include" },
      ),
    );

    await waitFor(() =>
      expect(screen.getByTestId("labels-print-all")).toBeEnabled(),
    );
    fireEvent.click(screen.getByTestId("labels-print-all"));
    await waitFor(() =>
      expect(global.fetch).toHaveBeenLastCalledWith(
        "/api/OpenELIS-Global/api/orders/77/labels/pdf",
        { credentials: "include" },
      ),
    );
  });

  test("a failed save stops the print and closes the window", async () => {
    const onSaveBeforePrint = vi.fn().mockResolvedValue(false);
    renderSection({ onSaveBeforePrint });
    await screen.findByTestId("labels-print-all");

    fireEvent.click(screen.getByTestId("labels-print-all"));

    await waitFor(() => expect(openedWindow.close).toHaveBeenCalled());
    expect(global.fetch).not.toHaveBeenCalled();
  });

  test("a server failure names the status and offers Retry, which prints again", async () => {
    const onSaveBeforePrint = vi.fn().mockResolvedValue(true);
    global.fetch
      .mockResolvedValueOnce({ ok: false, status: 500 })
      .mockResolvedValueOnce({
        ok: true,
        status: 200,
        blob: () => Promise.resolve(new Blob(["%PDF"])),
      });
    renderSection({ onSaveBeforePrint });
    await screen.findByTestId("labels-print-all");

    fireEvent.click(screen.getByTestId("labels-print-all"));

    const notice = await screen.findByTestId("prepare-labels-print-error");
    expect(notice).toHaveTextContent(messages["orderEntry.labels.printFailed"]);
    expect(notice).toHaveTextContent("500");
    expect(openedWindow.close).toHaveBeenCalled();

    fireEvent.click(screen.getByTestId("prepare-labels-retry"));
    await waitFor(() => expect(global.fetch).toHaveBeenCalledTimes(2));
    await waitFor(() =>
      expect(screen.queryByTestId("prepare-labels-print-error")).toBeNull(),
    );
  });

  test("an unreachable server is named as such and offers Retry", async () => {
    const onSaveBeforePrint = vi.fn().mockResolvedValue(true);
    global.fetch.mockRejectedValueOnce(new TypeError("Failed to fetch"));
    renderSection({ onSaveBeforePrint });
    await screen.findByTestId("labels-print-all");

    fireEvent.click(screen.getByTestId("labels-print-all"));

    const notice = await screen.findByTestId("prepare-labels-print-error");
    expect(notice).toHaveTextContent(
      messages["orderEntry.labels.printFailed.unreachable"],
    );
    expect(notice).not.toHaveTextContent("answered");
    expect(screen.getByTestId("prepare-labels-retry")).toBeVisible();
  });

  test("an empty result says no labels are configured", async () => {
    const onSaveBeforePrint = vi.fn().mockResolvedValue(true);
    global.fetch.mockResolvedValue({ ok: false, status: 404 });
    renderSection({ onSaveBeforePrint });
    await screen.findByTestId("labels-print-all");

    fireEvent.click(screen.getByTestId("labels-print-all"));

    const notice = await screen.findByTestId("prepare-labels-print-error");
    expect(notice).toHaveTextContent(
      messages["orderEntry.labels.nothingToPrint"],
    );
    expect(screen.queryByTestId("prepare-labels-retry")).toBeNull();
  });

  test("a blocked print window falls back to a download and says so", async () => {
    window.open.mockReturnValue(null);
    const onSaveBeforePrint = vi.fn().mockResolvedValue(true);
    const addNotification = vi.fn();
    global.fetch.mockResolvedValue({
      ok: true,
      status: 200,
      blob: () => Promise.resolve(new Blob(["%PDF"])),
    });
    const clickSpy = vi
      .spyOn(HTMLAnchorElement.prototype, "click")
      .mockImplementation(() => {});
    renderSection({ onSaveBeforePrint }, { addNotification });
    await screen.findByTestId("labels-print-all");

    fireEvent.click(screen.getByTestId("labels-print-all"));

    await waitFor(() => expect(clickSpy).toHaveBeenCalled());
    expect(addNotification).toHaveBeenCalledWith(
      expect.objectContaining({
        message: messages["orderEntry.labels.downloaded"],
      }),
    );
  });

  test("the registered row printer prints the tube at that position", async () => {
    const onSaveBeforePrint = vi.fn().mockResolvedValue(true);
    global.fetch.mockResolvedValue({
      ok: true,
      status: 200,
      blob: () => Promise.resolve(new Blob(["%PDF"])),
    });
    let printRow;
    renderSection({
      onSaveBeforePrint,
      registerPrintRow: (fn) => {
        printRow = fn;
      },
    });
    await screen.findByTestId("labels-print-all");

    printRow(0);

    await waitFor(() =>
      expect(global.fetch).toHaveBeenCalledWith(
        "/api/OpenELIS-Global/api/orders/77/labels/pdf?sampleItemId=501&scope=sample",
        { credentials: "include" },
      ),
    );
  });

  test("read-only renders the quantities without print actions", async () => {
    renderSection({ isReadOnly: true });
    await screen.findAllByText("Order Label");
    expect(screen.queryByTestId("labels-print-all")).toBeNull();
    expect(screen.queryByTestId("order-label-print-col-1")).toBeNull();
  });
});
