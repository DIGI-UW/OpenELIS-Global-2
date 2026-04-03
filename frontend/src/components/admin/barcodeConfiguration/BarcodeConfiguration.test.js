import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import { IntlProvider } from "react-intl";
import { BrowserRouter } from "react-router-dom";
import BarcodeConfiguration from "./BarcodeConfiguration";
import BarcodeConfigurationFormValues from "../../formModel/innitialValues/BarcodeConfigurationFormValues";
import { NotificationContext } from "../../layout/Layout";
import enMessages from "../../../languages/en.json";
import frMessages from "../../../languages/fr.json";
import { getFromOpenElisServer } from "../../utils/Utils";

jest.mock("../../utils/Utils", () => ({
  ...jest.requireActual("../../utils/Utils"),
  getFromOpenElisServer: jest.fn(),
  postToOpenElisServerJsonResponse: jest.fn(),
}));

jest.mock("../../common/PageBreadCrumb.js", () => () => (
  <div data-testid="barcode-breadcrumb" />
));

jest.mock("../../common/CustomNotification.js", () => ({
  AlertDialog: () => null,
  NotificationKinds: {
    success: "success",
    error: "error",
  },
}));

const notificationContextValue = {
  notificationVisible: false,
  setNotificationVisible: jest.fn(),
  addNotification: jest.fn(),
};

const renderWithProviders = () => {
  return render(
    <BrowserRouter>
      <IntlProvider locale="en" messages={enMessages}>
        <NotificationContext.Provider value={notificationContextValue}>
          <BarcodeConfiguration />
        </NotificationContext.Provider>
      </IntlProvider>
    </BrowserRouter>,
  );
};

describe("BarcodeConfiguration", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    const response = {
      ...BarcodeConfigurationFormValues,
      numMaxOrderLabels: 10,
      numDefaultOrderLabels: 1,
      prePrintDontUseAltAccession: true,
      prePrintAltAccessionPrefix: "ABCD",
      sitePrefix: "",
    };
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url === "/rest/BarcodeConfiguration") {
        callback(response);
      }
    });
  });

  test("renders form values from backend response", async () => {
    renderWithProviders();
    await screen.findByText(enMessages["barcodeconfiguration.browse.title"]);
    expect(screen.getByDisplayValue("10")).toBeTruthy();
    expect(screen.getByDisplayValue("ABCD")).toBeTruthy();
  });

  test("enables save button when max order label count changes", async () => {
    renderWithProviders();
    await screen.findByText(enMessages["barcodeconfiguration.browse.title"]);
    const saveButton = document.getElementById("saveButton");
    expect(saveButton).toBeTruthy();
    expect(saveButton.disabled).toBe(true);

    const orderInputs = screen.getAllByLabelText(
      enMessages["siteInfo.title.default.barcode.order"],
    );
    fireEvent.change(orderInputs[1], { target: { value: "11" } });
    expect(saveButton.disabled).toBe(false);
  });

  test("contains required barcode label i18n keys in en and fr", () => {
    const keys = [
      "barcode.label.info.blockNumber",
      "barcode.label.info.slideNumber",
      "barcode.label.info.specimenType",
      "barcode.label.info.storageLocation",
      "barcode.label.info.expiryDate",
    ];

    keys.forEach((key) => {
      expect(enMessages[key]).toBeDefined();
      expect(frMessages[key]).toBeDefined();
    });
  });
});
