import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
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

describe("OrderSuccessMessage", () => {
  beforeEach(() => {
    notificationMock.addNotification.mockClear();
    notificationMock.setNotificationVisible.mockClear();
  });

  test("forwards backend quantity and sampleNumber into the dialog", () => {
    renderWithIntl(
      <OrderSuccessMessage
        {...baseProps({
          saveResponse: {
            postSavePrintDialog: {
              accessionNumber: "ACC-9",
              printableLabelTypes: [
                {
                  labelType: "order",
                  quantity: 7,
                  printUrl:
                    "/LabelMakerServlet?labNo=ACC-9&type=order&quantity=7",
                },
                {
                  labelType: "specimen",
                  quantity: 4,
                  sampleNumber: 2,
                  printUrl:
                    "/LabelMakerServlet?labNo=ACC-9.2&type=specimen&quantity=4",
                },
              ],
            },
          },
        })}
      />,
    );

    // M6: rows are labelled by the (preset/legacy) type verbatim — the OGC-284
    // hardcoded type->i18n map ("Order label") is gone — and the quantity is an
    // editable NumberInput seeded to the backend value, not static text.
    expect(screen.getByText("order")).toBeInTheDocument();
    expect(screen.getByText("specimen 2")).toBeInTheDocument();
    const inputs = screen.getAllByRole("spinbutton");
    expect(inputs).toHaveLength(2);
    expect(inputs[0]).toHaveValue(7);
    expect(inputs[1]).toHaveValue(4);
  });

  test("opens the backend-supplied printUrl verbatim when Print is clicked", () => {
    const openSpy = vi.spyOn(window, "open").mockReturnValue({});

    renderWithIntl(
      <OrderSuccessMessage
        {...baseProps({
          saveResponse: {
            postSavePrintDialog: {
              accessionNumber: "ACC-9",
              printableLabelTypes: [
                {
                  labelType: "order",
                  quantity: 3,
                  printUrl:
                    "/LabelMakerServlet?labNo=ACC-9&type=order&quantity=3",
                },
              ],
            },
          },
        })}
      />,
    );

    fireEvent.click(screen.getByRole("button", { name: "Print" }));

    expect(openSpy).toHaveBeenCalledWith(
      "/LabelMakerServlet?labNo=ACC-9&type=order&quantity=3",
    );

    openSpy.mockRestore();
  });

  test("renders no print buttons when backend explicitly emits an empty list", () => {
    renderWithIntl(
      <OrderSuccessMessage
        {...baseProps({
          saveResponse: {
            postSavePrintDialog: {
              accessionNumber: "ACC-EMPTY",
              printableLabelTypes: [],
            },
          },
        })}
      />,
    );

    expect(screen.queryByRole("button", { name: "Print" })).toBeNull();
  });

  test("falls back to a single Order entry when the dialog model is absent", () => {
    renderWithIntl(<OrderSuccessMessage {...baseProps()} />);

    // Default fallback is the bare "order" type (no i18n type map in M6).
    expect(screen.getByText("order")).toBeInTheDocument();
    expect(screen.getAllByRole("button", { name: "Print" })).toHaveLength(1);
  });

  test("fallback URL for a specimen entry includes labNo.<sampleNumber>", () => {
    // Backend omits printUrl on the second specimen — the fallback must still
    // target that one specimen, not "every sample item" (which would over-print
    // by N — the original bug class this PR fixes).
    const openSpy = vi.spyOn(window, "open").mockReturnValue({});

    renderWithIntl(
      <OrderSuccessMessage
        {...baseProps({
          saveResponse: {
            postSavePrintDialog: {
              accessionNumber: "ACC-FB",
              printableLabelTypes: [
                {
                  labelType: "specimen",
                  quantity: 4,
                  sampleNumber: 2,
                  // printUrl deliberately missing
                },
              ],
            },
          },
        })}
      />,
    );

    fireEvent.click(screen.getByRole("button", { name: "Print" }));

    const calledUrl = openSpy.mock.calls[0][0];
    expect(calledUrl).toContain("labNo=ACC-FB.2");
    expect(calledUrl).toContain("type=specimen");
    expect(calledUrl).toContain("quantity=4");

    openSpy.mockRestore();
  });

  test("popup-blocked Print surfaces a notification toast", () => {
    const openSpy = vi.spyOn(window, "open").mockReturnValue(null);
    const warnSpy = vi.spyOn(console, "warn").mockImplementation(() => {});

    renderWithIntl(
      <OrderSuccessMessage
        {...baseProps({
          saveResponse: {
            postSavePrintDialog: {
              accessionNumber: "ACC-BLOCK",
              printableLabelTypes: [
                {
                  labelType: "order",
                  quantity: 1,
                  printUrl: "/LabelMakerServlet?labNo=ACC-BLOCK",
                },
              ],
            },
          },
        })}
      />,
    );

    fireEvent.click(screen.getByRole("button", { name: "Print" }));

    expect(notificationMock.addNotification).toHaveBeenCalledTimes(1);
    expect(notificationMock.addNotification.mock.calls[0][0].kind).toBe(
      "error",
    );
    expect(notificationMock.setNotificationVisible).toHaveBeenCalledWith(true);

    openSpy.mockRestore();
    warnSpy.mockRestore();
  });

  test("Done resets the form and returns the user to page 0", () => {
    const setOrderFormValues = vi.fn();
    const setSamples = vi.fn();
    const setPage = vi.fn();

    renderWithIntl(
      <OrderSuccessMessage
        {...baseProps({ setOrderFormValues, setSamples, setPage })}
      />,
    );

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
});
