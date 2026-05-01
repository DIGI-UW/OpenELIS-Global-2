import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../languages/en.json";

// Index.jsx pulls in the entire add-order tree (NotificationContext,
// AddSample, etc); we only need sampleObject here.
vi.mock("./Index", () => ({
  sampleObject: { index: 0 },
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

    expect(screen.getByText("Order label")).toBeInTheDocument();
    expect(screen.getByText("Specimen label 2")).toBeInTheDocument();
    expect(screen.getByText(/Quantity.*: 7/)).toBeInTheDocument();
    expect(screen.getByText(/Quantity.*: 4/)).toBeInTheDocument();
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

    expect(screen.getByText("Order label")).toBeInTheDocument();
    expect(screen.getAllByRole("button", { name: "Print" })).toHaveLength(1);
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
