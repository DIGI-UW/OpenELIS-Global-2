import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../languages/en.json";
import { ConfigurationContext } from "../../layout/contexts";

const noop = () => {};

// Stable mocks so each useHistory()/useContext() call returns the same fns
// across renders — needed for tests that assert against history.push or
// addNotification.
const { historyMock, notificationMock, orderContextValue, locationMock } =
  vi.hoisted(() => {
    const history = { push: () => {}, replace: () => {} };
    const notification = {
      notificationVisible: false,
      setNotificationVisible: () => {},
      addNotification: () => {},
    };
    return {
      historyMock: history,
      locationMock: { search: "" },
      notificationMock: notification,
      orderContextValue: {
        orderData: {
          sampleOrderItems: { labNo: "LAB-100" },
          patientProperties: {},
        },
        samples: [
          { sampleTypeName: "Blood", sortOrder: 1, sampleItemId: "1" },
          { sampleTypeName: "Urine", sortOrder: 2, sampleItemId: "2" },
        ],
        setSamples: () => {},
        saveOrder: () => Promise.resolve({ samples: [] }),
        setCurrentStep: () => {},
        labNumber: "LAB-100",
        stepProgress: { label: false },
        markStepComplete: () => {},
        orderId: 1,
        storageSkipped: false,
        setStorageSkipped: () => {},
        loadOrder: () => {},
        isLoading: false,
      },
    };
  });

vi.mock("react-router-dom", () => ({
  useHistory: () => historyMock,
  useLocation: () => locationMock,
}));

vi.mock("../OrderContext", () => ({
  useOrderContext: () => orderContextValue,
  useWorkflowPrefix: () => "/order/clinical",
}));

vi.mock("../../layout/Layout", () => ({
  NotificationContext: React.createContext(notificationMock),
}));

vi.mock("../../common/CustomNotification", () => ({
  AlertDialog: () => null,
  NotificationKinds: { success: "success", error: "error" },
}));

vi.mock("../../storage/LocationPicker/LocationPickerInline", () => ({
  default: () => <div data-testid="location-picker" />,
}));

vi.mock("../../storage/LocationPicker/locationSelectionMapper", () => ({
  getDeepestLocationSelection: vi.fn(),
  positionToCoordinate: vi.fn(),
  selectionToHierarchicalPath: vi.fn(),
}));

vi.mock("../../utils/Utils", () => ({
  postToOpenElisServerJsonResponse: vi.fn(),
  patchToOpenElisServerJsonResponse: vi.fn(),
}));

vi.mock("../OrderWorkflowLayout", () => ({
  default: ({ children }) => <div>{children}</div>,
}));

import OrderLabel from "./OrderLabel";

const renderWithIntl = (ui) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      {ui}
    </IntlProvider>,
  );

const lastOpenedUrl = (openSpy) =>
  openSpy.mock.calls[openSpy.mock.calls.length - 1][0];

describe("OrderLabel print URLs", () => {
  let openSpy;
  let addNotificationSpy;
  beforeEach(() => {
    openSpy = vi.spyOn(window, "open").mockImplementation(noop);
    addNotificationSpy = vi.fn();
    notificationMock.addNotification = addNotificationSpy;
    notificationMock.setNotificationVisible = vi.fn();
  });
  afterEach(() => {
    openSpy.mockRestore();
  });

  test("per-row order print carries quantity and does not force override", () => {
    renderWithIntl(<OrderLabel />);

    const printButtons = screen.getAllByRole("button", { name: "Print Label" });
    fireEvent.click(printButtons[0]);

    const url = lastOpenedUrl(openSpy);
    expect(url).toMatch(/[?&]type=order(&|$)/);
    expect(url).toMatch(/[?&]quantity=1(&|$)/);
    expect(url).not.toContain("override=true");
  });

  test("per-sample print uses labNo.<sortOrder> with quantity and no override", () => {
    renderWithIntl(<OrderLabel />);

    const printButtons = screen.getAllByRole("button", { name: "Print Label" });
    fireEvent.click(printButtons[2]); // 0=order, 1=sample 1, 2=sample 2

    const url = lastOpenedUrl(openSpy);
    expect(url).toMatch(/[?&]labNo=LAB-100\.2(&|$)/);
    expect(url).toMatch(/[?&]type=specimen(&|$)/);
    expect(url).not.toContain("override=true");
  });

  test("Print All carries quantity and respects the safety cap (no override flag)", () => {
    renderWithIntl(<OrderLabel />);

    fireEvent.click(screen.getByRole("button", { name: "Print All Labels" }));

    const url = lastOpenedUrl(openSpy);
    expect(url).toMatch(/[?&]type=default(&|$)/);
    expect(url).toMatch(/[?&]quantity=\d+(&|$)/);
    expect(url).not.toContain("override=true");
  });

  test("popup-blocked per-row print surfaces an error toast and skips success", () => {
    openSpy.mockReturnValue(null);
    const warnSpy = vi.spyOn(console, "warn").mockImplementation(noop);

    renderWithIntl(<OrderLabel />);

    const printButtons = screen.getAllByRole("button", { name: "Print Label" });
    fireEvent.click(printButtons[0]);

    expect(warnSpy).toHaveBeenCalled();
    expect(addNotificationSpy).toHaveBeenCalledTimes(1);
    expect(addNotificationSpy.mock.calls[0][0].kind).toBe("error");

    warnSpy.mockRestore();
  });

  test("popup-blocked Print All surfaces an error toast and skips success", () => {
    openSpy.mockReturnValue(null);
    const warnSpy = vi.spyOn(console, "warn").mockImplementation(noop);

    renderWithIntl(<OrderLabel />);

    fireEvent.click(screen.getByRole("button", { name: "Print All Labels" }));

    expect(warnSpy).toHaveBeenCalled();
    expect(addNotificationSpy).toHaveBeenCalledTimes(1);
    expect(addNotificationSpy.mock.calls[0][0].kind).toBe("error");

    warnSpy.mockRestore();
  });

  // OGC-1201 S: a blocked popup left the user with no way to print at all,
  // and a blocker handing back a stub window instead of null was read as
  // success.
  test("a blocked popup still offers the labels as a link", () => {
    openSpy.mockReturnValue(null);
    const warnSpy = vi.spyOn(console, "warn").mockImplementation(noop);

    renderWithIntl(<OrderLabel />);
    fireEvent.click(screen.getAllByRole("button", { name: "Print Label" })[0]);

    const link = screen.getByRole("link", {
      name: "Open the labels in a new tab",
    });
    expect(link).toHaveAttribute("href", expect.stringContaining("labNo="));
    expect(link).toHaveAttribute("target", "_blank");

    warnSpy.mockRestore();
  });

  test("treats a stub window from a blocker as a blocked print", () => {
    openSpy.mockReturnValue({ closed: true });
    const warnSpy = vi.spyOn(console, "warn").mockImplementation(noop);

    renderWithIntl(<OrderLabel />);
    fireEvent.click(screen.getAllByRole("button", { name: "Print Label" })[0]);

    expect(addNotificationSpy).toHaveBeenCalledTimes(1);
    expect(addNotificationSpy.mock.calls[0][0].kind).toBe("error");
    expect(
      screen.getByRole("link", { name: "Open the labels in a new tab" }),
    ).toBeInTheDocument();

    warnSpy.mockRestore();
  });
});

// OGC-1192 walk: an environmental order has no patient, and its Order Label
// read "Lab Nr: DEV01260000000000314 | Patient: ,".
describe("OrderLabel order label content", () => {
  const originalOrderData = orderContextValue.orderData;
  afterEach(() => {
    orderContextValue.orderData = originalOrderData;
  });

  test("a patientless order names its sampling site instead of an empty patient", () => {
    orderContextValue.orderData = {
      sampleOrderItems: {
        labNo: "LAB-100",
        environmentalFields: { samplingSiteName: "Bekasi Tepi Sungai" },
      },
      patientProperties: {},
    };
    renderWithIntl(<OrderLabel />);

    expect(
      screen.getByText("Lab Nr: LAB-100 | Bekasi Tepi Sungai"),
    ).toBeInTheDocument();
    expect(screen.queryByText(/Patient: ,/)).toBeNull();
  });

  test("a clinical order still names its patient", () => {
    orderContextValue.orderData = {
      sampleOrderItems: { labNo: "LAB-100" },
      patientProperties: { lastName: "Doe", firstName: "Jane" },
    };
    renderWithIntl(<OrderLabel />);

    expect(
      screen.getByText("Lab Nr: LAB-100 | Patient: Doe, Jane"),
    ).toBeInTheDocument();
  });
});

// OGC-1657: the label rows printed the collection date as stored (yyyy-MM-dd)
// whatever the site's date format.
describe("OrderLabel sample label date", () => {
  const originalSamples = orderContextValue.samples;
  afterEach(() => {
    orderContextValue.samples = originalSamples;
  });

  const renderOnSite = (dateLocale) =>
    render(
      <ConfigurationContext.Provider
        value={{ configurationProperties: { DEFAULT_DATE_LOCALE: dateLocale } }}
      >
        <IntlProvider locale="en" messages={messages}>
          <OrderLabel />
        </IntlProvider>
      </ConfigurationContext.Provider>,
    );

  test.each([
    ["fr-FR", "05/03/2026"],
    ["en-US", "03/05/2026"],
  ])("%s: a sample label shows its collection day as %s", (locale, shown) => {
    orderContextValue.samples = [
      {
        sampleTypeName: "Water",
        sortOrder: 1,
        sampleItemId: "1",
        collectionDate: "2026-03-05",
      },
    ];
    renderOnSite(locale);

    expect(screen.getByText(`Water | ${shown}`)).toBeInTheDocument();
    expect(screen.queryByText(/2026-03-05/)).toBeNull();
  });
});

// OGC-1657 walk: opening or refreshing Label & Store at ?order=<lab number>
// returned to a blank Enter Order (and a new lab number): this step redirected
// before OrderContext had loaded the order the URL names.
describe("OrderLabel without a loaded order", () => {
  const saved = {};
  beforeEach(() => {
    ["orderId", "labNumber", "isLoading", "orderData"].forEach((key) => {
      saved[key] = orderContextValue[key];
    });
    orderContextValue.orderId = null;
    orderContextValue.labNumber = null;
    orderContextValue.isLoading = false;
    orderContextValue.orderData = {
      sampleOrderItems: {},
      patientProperties: {},
    };
    historyMock.replace = vi.fn();
  });
  afterEach(() => {
    Object.assign(orderContextValue, saved);
    locationMock.search = "";
  });

  test("returns to Enter Order when the address names no order", () => {
    renderWithIntl(<OrderLabel />);

    expect(historyMock.replace).toHaveBeenCalledWith("/order/clinical/enter");
  });

  test.each(["?order=DEV-1", "?labNumber=DEV-1"])(
    "waits on %s for the order to load, then stays",
    (search) => {
      locationMock.search = search;
      const view = renderWithIntl(<OrderLabel />);
      expect(historyMock.replace).not.toHaveBeenCalled();

      orderContextValue.isLoading = true;
      view.rerender(
        <IntlProvider locale="en" messages={messages}>
          <OrderLabel />
        </IntlProvider>,
      );
      orderContextValue.isLoading = false;
      orderContextValue.orderId = 7;
      orderContextValue.labNumber = "DEV-1";
      view.rerender(
        <IntlProvider locale="en" messages={messages}>
          <OrderLabel />
        </IntlProvider>,
      );

      expect(historyMock.replace).not.toHaveBeenCalled();
    },
  );

  test("returns to Enter Order when the named order does not load", () => {
    locationMock.search = "?order=DEV-404";
    const view = renderWithIntl(<OrderLabel />);
    orderContextValue.isLoading = true;
    view.rerender(
      <IntlProvider locale="en" messages={messages}>
        <OrderLabel />
      </IntlProvider>,
    );
    expect(historyMock.replace).not.toHaveBeenCalled();

    orderContextValue.isLoading = false;
    view.rerender(
      <IntlProvider locale="en" messages={messages}>
        <OrderLabel />
      </IntlProvider>,
    );

    expect(historyMock.replace).toHaveBeenCalledWith("/order/clinical/enter");
  });
});
