import React from "react";
import { fireEvent, render, screen, within } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { vi } from "vitest";
import messages from "../../languages/en.json";

vi.mock("../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
}));

vi.mock("../layout/Layout", async () => {
  const { createContext } = await import("react");
  return {
    NotificationContext: createContext({
      notificationVisible: false,
      setNotificationVisible: () => {},
      addNotification: () => {},
    }),
    ConfigurationContext: createContext({ configurationProperties: {} }),
  };
});

import { getFromOpenElisServer } from "../utils/Utils";
import { ConfigurationContext } from "../layout/Layout";
import ExistingOrder from "./ExistingOrder";

const LAB = "DEV01260000000000050";
const PATIENT = {
  patientSearchResults: [
    {
      firstName: "Esig",
      lastName: "Testpatient",
      birthdate: "01/01/1990",
      gender: "M",
      nationalId: "12345",
    },
  ],
};
const TESTS = {
  existingTests: [
    {
      accessionNumber: `${LAB}-1`,
      sampleType: "Serum",
      sampleItemId: "10109",
      testName: "Creatinine",
    },
  ],
};
const PRESETS = [
  {
    id: 1,
    name: "Order Label",
    heightMm: 25,
    widthMm: 76,
    maxPerOrder: 4,
    maxPerSample: 10,
  },
  {
    id: 2,
    name: "Specimen Label",
    heightMm: 30,
    widthMm: 60,
    maxPerOrder: 10,
    maxPerSample: 6,
  },
];
const SAVED = [
  {
    id: 501,
    parent_sample_id: "1029",
    sample_item_id: null,
    preset_id: 1,
    qty: 2,
    preset_snapshot: {
      preset: { id: 1, name: "Order Label", heightMm: 25, widthMm: 76 },
    },
  },
  {
    id: 502,
    parent_sample_id: "1029",
    sample_item_id: "10109",
    preset_id: 2,
    qty: 1,
    preset_snapshot: {
      preset: { id: 2, name: "Specimen Label", heightMm: 30, widthMm: 60 },
    },
  },
];

const SETTINGS = {
  DEFAULT_ORDER_LABEL_PRINTED: "2",
  MAX_ORDER_LABEL_PRINTED: "5",
  DEFAULT_SPECIMEN_LABEL_PRINTED: "1",
  MAX_SPECIMEN_LABEL_PRINTED: "3",
};

const serve = (saved) =>
  getFromOpenElisServer.mockImplementation((url, callback) => {
    if (url.startsWith("/rest/patient-search-results")) callback(PATIENT);
    else if (url.startsWith("/rest/SampleEdit")) callback(TESTS);
    else if (url.startsWith("/api/orders/by-accession/")) callback(saved);
    else if (url === "/api/labelPresets") callback(PRESETS);
  });

const renderPage = () =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <ConfigurationContext.Provider
        value={{ configurationProperties: SETTINGS }}
      >
        <ExistingOrder />
      </ConfigurationContext.Provider>
    </IntlProvider>,
  );

const search = async () => {
  fireEvent.change(screen.getByRole("textbox", { name: /Accession/i }), {
    target: { value: LAB },
  });
  fireEvent.click(
    screen.getByRole("button", { name: messages["label.button.submit"] }),
  );
  await screen.findByTestId("order-labels");
};

const frameSrc = () => document.querySelector("iframe")?.getAttribute("src");

describe("ExistingOrder labels (OGC-1169)", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  test("an older order lists Order label and Specimen label by name, one stepper and one Print per row, no reprint block", async () => {
    serve([]);
    renderPage();
    await search();

    expect(screen.queryByText("Reprint labels")).toBeNull();
    expect(screen.queryByText("Print Sets")).toBeNull();
    const order = screen.getByTestId("label-row-order");
    expect(within(order).getByText("Order label")).toBeInTheDocument();
    expect(within(order).getByText(LAB)).toBeInTheDocument();
    const specimen = screen.getByTestId("label-row-specimen-10109");
    expect(within(specimen).getByText("Specimen label")).toBeInTheDocument();
    expect(within(specimen).getByText("Serum")).toBeInTheDocument();
    expect(screen.queryByText(/^order$/)).toBeNull();
    expect(screen.queryByText("specimen-1")).toBeNull();

    const orderQty = within(order).getByRole("spinbutton");
    const specimenQty = within(specimen).getByRole("spinbutton");
    expect(orderQty).toHaveValue(2);
    expect(orderQty).toHaveAttribute("max", "5");
    expect(specimenQty).toHaveValue(1);
    expect(specimenQty).toHaveAttribute("max", "3");
    expect(screen.getAllByRole("button", { name: /^Print/ })).toHaveLength(3);
    expect(
      screen.getByText("Prints 2 order labels and 1 labels per specimen."),
    ).toBeInTheDocument();
  });

  test("a changed specimen quantity prints that many through the servlet", async () => {
    serve([]);
    renderPage();
    await search();

    const specimen = screen.getByTestId("label-row-specimen-10109");
    fireEvent.change(within(specimen).getByRole("spinbutton"), {
      target: { value: "3" },
    });
    fireEvent.click(screen.getByTestId("print-row-specimen-10109"));
    await waitFor(() => {
      expect(frameSrc()).toBe(
        `/LabelMakerServlet?labNo=${LAB}-1&type=specimen&quantity=3`,
      );
    });

    fireEvent.click(screen.getByTestId("print-all-labels"));
    await waitFor(() => {
      expect(frameSrc()).toBe(
        `/LabelMakerServlet?labNo=${LAB}&type=default&quantity=`,
      );
    });
  });

  test("a typed quantity survives a re-render of the configuration context", async () => {
    serve([]);
    const Harness = () => {
      const [tick, setTick] = React.useState(0);
      return (
        <IntlProvider locale="en" messages={messages}>
          <ConfigurationContext.Provider
            value={{ configurationProperties: { ...SETTINGS, tick } }}
          >
            <button type="button" onClick={() => setTick(tick + 1)}>
              rerender
            </button>
            <ExistingOrder />
          </ConfigurationContext.Provider>
        </IntlProvider>
      );
    };
    render(<Harness />);
    await search();

    const specimen = screen.getByTestId("label-row-specimen-10109");
    fireEvent.change(within(specimen).getByRole("spinbutton"), {
      target: { value: "3" },
    });
    expect(within(specimen).getByRole("spinbutton")).toHaveValue(3);
    fireEvent.click(screen.getByRole("button", { name: "rerender" }));
    expect(within(specimen).getByRole("spinbutton")).toHaveValue(3);
  });

  test("a quantity above the maximum is clamped", async () => {
    serve([]);
    renderPage();
    await search();

    const order = screen.getByTestId("label-row-order");
    fireEvent.change(within(order).getByRole("spinbutton"), {
      target: { value: "9" },
    });
    fireEvent.click(screen.getByTestId("print-row-order"));
    await waitFor(() => {
      expect(frameSrc()).toBe(
        `/LabelMakerServlet?labNo=${LAB}&type=order&quantity=5`,
      );
    });
  });

  test("an order saved with label requests lists its presets by name and size and reprints at the chosen quantity", async () => {
    serve(SAVED);
    renderPage();
    await search();

    const orderRow = screen.getByTestId("label-row-saved-501");
    expect(
      within(orderRow).getByText("Order Label (25 × 76 mm)"),
    ).toBeInTheDocument();
    expect(within(orderRow).getByRole("spinbutton")).toHaveValue(2);
    expect(within(orderRow).getByRole("spinbutton")).toHaveAttribute(
      "max",
      "4",
    );

    const tubeRow = screen.getByTestId("label-row-saved-502");
    expect(
      within(tubeRow).getByText("Specimen Label (30 × 60 mm)"),
    ).toBeInTheDocument();
    expect(within(tubeRow).getByText(`${LAB}-1`)).toBeInTheDocument();
    expect(within(tubeRow).getByText("Serum")).toBeInTheDocument();
    expect(within(tubeRow).getByRole("spinbutton")).toHaveAttribute("max", "6");

    fireEvent.change(within(tubeRow).getByRole("spinbutton"), {
      target: { value: "3" },
    });
    fireEvent.click(screen.getByTestId("print-row-saved-502"));
    await waitFor(() => {
      expect(frameSrc()).toBe(
        "/api/OpenELIS-Global/api/orders/1029/labels/pdf?presetId=2&scope=sample&sampleItemId=10109&quantity=3",
      );
    });

    fireEvent.click(screen.getByTestId("print-all-labels"));
    await waitFor(() => {
      expect(frameSrc()).toBe(
        "/api/OpenELIS-Global/api/orders/1029/labels/pdf",
      );
    });
    expect(
      screen.getByText(messages["barcode.print.all.saved"]),
    ).toBeInTheDocument();
  });
});
