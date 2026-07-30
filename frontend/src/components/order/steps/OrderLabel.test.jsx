import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../languages/en.json";

const noop = () => {};

// Stable mocks so each useHistory()/useContext() call returns the same fns
// across renders — needed for tests that assert against history.push or
// addNotification.
const { historyMock, notificationMock, orderContextValue } = vi.hoisted(() => {
  const history = { push: () => {}, replace: () => {} };
  const notification = {
    notificationVisible: false,
    setNotificationVisible: () => {},
    addNotification: () => {},
  };
  return {
    historyMock: history,
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
  useLocation: () => ({ search: "" }),
}));

vi.mock("../OrderContext", () => ({
  useOrderContext: () => orderContextValue,
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
});
