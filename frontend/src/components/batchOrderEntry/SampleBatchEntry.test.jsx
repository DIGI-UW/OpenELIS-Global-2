import React, { useState } from "react";
import { render, screen } from "@testing-library/react";
import { waitFor, within } from "@testing-library/dom";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import { BrowserRouter } from "react-router-dom";
import { IntlProvider } from "react-intl";
import messages from "../../languages/en.json";
import { ConfigurationContext, NotificationContext } from "../layout/Layout";
import SampleBatchEntry from "./SampleBatchEntry";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../utils/Utils";

vi.mock("../utils/Utils", async (importOriginal) => ({
  ...(await importOriginal()),
  getFromOpenElisServer: vi.fn(),
  postToOpenElisServerJsonResponse: vi.fn(),
}));

const baseOrderFormValues = {
  patientProperties: {},
  sampleOrderItems: {
    labNo: "LAB-001",
    referringSiteId: "",
    receivedDateForDisplay: "",
    receivedTime: "",
    referringSiteName: "",
    referringSiteDepartmentName: "",
  },
  method: "On Demand",
  tests: [],
  sampleTypeSelect: "",
  sampleXML:
    "<?xml version='1.0' encoding='utf-8'?><samples><sample sampleID='1' tests='1' testSectionMap='' date='' time='' testSampleTypeMap='' panels='' numOrderLabels='2' numSpecimenLabels='3' initialConditionIds=''/></samples>",
};

const renderSampleBatchEntry = () =>
  render(
    <BrowserRouter>
      <IntlProvider locale="en" messages={messages}>
        <ConfigurationContext.Provider value={{ configurationProperties: {} }}>
          <NotificationContext.Provider
            value={{
              notificationVisible: false,
              setNotificationVisible: vi.fn(),
              addNotification: vi.fn(),
            }}
          >
            <SampleBatchEntry
              orderFormValues={baseOrderFormValues}
              setOrderFormValues={vi.fn()}
            />
          </NotificationContext.Provider>
        </ConfigurationContext.Provider>
      </IntlProvider>
    </BrowserRouter>,
  );

describe("SampleBatchEntry rollout", () => {
  beforeEach(() => {
    window.scrollTo = vi.fn();
    getFromOpenElisServer.mockImplementation((endpoint, callback) => {
      if (endpoint === "/rest/SamplePatientEntry") {
        callback({
          sampleOrderItems: {
            referringSiteList: [],
          },
        });
        return;
      }
      callback([]);
    });
    postToOpenElisServerJsonResponse.mockReset();
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  test("renders shared labels section in batch flow", () => {
    renderSampleBatchEntry();

    expect(screen.getByText("Label quantities")).toBeInTheDocument();
  });

  it("does not report a half-filled form as a console error while it is being filled in", async () => {
    const errors = vi.spyOn(console, "error");
    renderSampleBatchEntry();
    await new Promise((resolve) => setTimeout(resolve, 0));

    expect(
      errors.mock.calls.some(([message]) =>
        String(message).includes("Validation Errors"),
      ),
    ).toBe(false);
    errors.mockRestore();
  });
});

describe("SampleBatchEntry saving", () => {
  const PRE_PRINTED_LAB_NO = "DEV01260000000000169";
  const notifications = [];

  const Harness = ({ method, extraValues }) => {
    const [values, setValues] = useState({
      ...baseOrderFormValues,
      ...extraValues,
      method,
      sampleOrderItems: { ...baseOrderFormValues.sampleOrderItems, labNo: "" },
    });
    return (
      <SampleBatchEntry
        orderFormValues={values}
        setOrderFormValues={setValues}
      />
    );
  };

  const renderBatch = (method, extraValues) =>
    render(
      <BrowserRouter>
        <IntlProvider locale="en" messages={messages}>
          <ConfigurationContext.Provider
            value={{ configurationProperties: {} }}
          >
            <NotificationContext.Provider
              value={{
                notificationVisible: false,
                setNotificationVisible: vi.fn(),
                addNotification: (n) => notifications.push(n),
              }}
            >
              <Harness method={method} extraValues={extraValues} />
            </NotificationContext.Provider>
          </ConfigurationContext.Provider>
        </IntlProvider>
      </BrowserRouter>,
    );

  const savedLabNos = () =>
    postToOpenElisServerJsonResponse.mock.calls.map(
      ([, body]) => JSON.parse(body).sampleOrderItems.labNo,
    );

  beforeEach(() => {
    window.scrollTo = vi.fn();
    notifications.length = 0;
    getFromOpenElisServer.mockReset();
    getFromOpenElisServer.mockImplementation((endpoint, callback) => {
      if (endpoint === "/rest/SamplePatientEntry") {
        callback({ sampleOrderItems: { referringSiteList: [] } });
      } else if (endpoint === "/rest/SampleEntryGenerateScanProvider") {
        callback({ status: true, body: "DEV01260000000000200" });
      } else {
        callback([]);
      }
    });
    postToOpenElisServerJsonResponse.mockReset();
    postToOpenElisServerJsonResponse.mockImplementation((_url, _body, cb) =>
      cb({ statusCode: 200 }),
    );
  });

  test("typing a pre-printed lab number saves nothing until Save, then saves it once", async () => {
    const user = userEvent.setup();
    renderBatch("Pre-Printed");

    await user.type(screen.getByLabelText(/Lab Number/), PRE_PRINTED_LAB_NO);
    expect(postToOpenElisServerJsonResponse).not.toHaveBeenCalled();

    await user.click(
      screen.getByTestId("generate-barcode-btn-BatchOrderEntry"),
    );

    await waitFor(() => expect(savedLabNos()).toEqual([PRE_PRINTED_LAB_NO]));
  });

  test("an EID order is saved with the specimens and test chosen on the setup screen", async () => {
    const user = userEvent.setup();
    renderBatch("Pre-Printed", {
      sampleXML: "",
      _ProjectDataEID: { dryTubeTaken: true, dbsTaken: "", dnaPCR: true },
    });

    await user.type(screen.getByLabelText(/Lab Number/), PRE_PRINTED_LAB_NO);
    await user.click(
      screen.getByTestId("generate-barcode-btn-BatchOrderEntry"),
    );

    await waitFor(() =>
      expect(postToOpenElisServerJsonResponse).toHaveBeenCalledTimes(1),
    );
    const saved = JSON.parse(postToOpenElisServerJsonResponse.mock.calls[0][1]);
    expect(saved._ProjectDataEID).toEqual({
      dryTubeTaken: true,
      dbsTaken: "",
      dnaPCR: true,
    });
    expect(saved.sampleXML).toBe("");
  });

  test("Save does not save a pre-printed lab number that is blank", async () => {
    renderBatch("Pre-Printed");

    expect(
      screen.getByTestId("generate-barcode-btn-BatchOrderEntry"),
    ).toBeDisabled();
    expect(postToOpenElisServerJsonResponse).not.toHaveBeenCalled();
  });

  test("Generate saves the generated pre-printed number once, and Save afterwards does not save it again", async () => {
    const user = userEvent.setup();
    renderBatch("Pre-Printed");

    await user.click(screen.getByRole("link", { name: "Generate" }));
    await waitFor(() =>
      expect(savedLabNos()).toEqual(["DEV01260000000000200"]),
    );

    await user.click(
      screen.getByTestId("generate-barcode-btn-BatchOrderEntry"),
    );
    expect(savedLabNos()).toEqual(["DEV01260000000000200"]);
    expect(
      within(
        screen.getByText("Previously used Accession Number").closest("li"),
      ).getByText("DEV01260000000000200"),
    ).toBeInTheDocument();
  });

  test("a rejected save reports the error and can be saved again", async () => {
    const user = userEvent.setup();
    postToOpenElisServerJsonResponse.mockImplementationOnce((_u, _b, cb) =>
      cb({ statusCode: 400, error: "accession number is used" }),
    );
    renderBatch("Pre-Printed");

    await user.type(screen.getByLabelText(/Lab Number/), PRE_PRINTED_LAB_NO);
    await user.click(
      screen.getByTestId("generate-barcode-btn-BatchOrderEntry"),
    );
    await waitFor(() =>
      expect(notifications.map((n) => n.kind)).toEqual(["error"]),
    );
    expect(notifications[0].message).toBe("accession number is used");

    await user.click(
      screen.getByTestId("generate-barcode-btn-BatchOrderEntry"),
    );
    await waitFor(() =>
      expect(savedLabNos()).toEqual([PRE_PRINTED_LAB_NO, PRE_PRINTED_LAB_NO]),
    );
  });

  test("On Demand saves once per generated barcode and typing saves nothing", async () => {
    const user = userEvent.setup();
    renderBatch("On Demand");

    await user.type(screen.getByLabelText(/Lab Number/), "DEV0126");
    expect(postToOpenElisServerJsonResponse).not.toHaveBeenCalled();

    await user.click(
      screen.getByTestId("generate-barcode-link-BatchOrderEntry"),
    );
    await waitFor(() =>
      expect(savedLabNos()).toEqual(["DEV01260000000000200"]),
    );
  });
});

describe("SampleBatchEntry notifications", () => {
  const notification = {
    kind: "success",
    title: "Notification Message",
    message: "Order saved once",
  };

  const renderWithPatientInfo = () =>
    render(
      <BrowserRouter>
        <IntlProvider locale="en" messages={messages}>
          <ConfigurationContext.Provider
            value={{ configurationProperties: {} }}
          >
            <NotificationContext.Provider
              value={{
                notificationVisible: true,
                setNotificationVisible: vi.fn(),
                addNotification: vi.fn(),
                removeNotification: vi.fn(),
                notifications: [notification],
              }}
            >
              <SampleBatchEntry
                orderFormValues={{
                  ...baseOrderFormValues,
                  PatientInfoCheck: true,
                  patientProperties: { firstName: "", guid: "" },
                }}
                setOrderFormValues={vi.fn()}
              />
            </NotificationContext.Provider>
          </ConfigurationContext.Provider>
        </IntlProvider>
      </BrowserRouter>,
    );

  beforeEach(() => {
    window.scrollTo = vi.fn();
    getFromOpenElisServer.mockReset();
    getFromOpenElisServer.mockImplementation((endpoint, callback) => {
      if (endpoint === "/rest/SamplePatientEntry") {
        callback({ sampleOrderItems: { referringSiteList: [] } });
      } else {
        callback([]);
      }
    });
  });

  test("shows a notification once while the patient search is open", () => {
    renderWithPatientInfo();

    expect(screen.getAllByText("Order saved once")).toHaveLength(1);
  });

  test("shows a notification once while the new patient form is open", async () => {
    const user = userEvent.setup();
    renderWithPatientInfo();
    await user.click(screen.getByRole("button", { name: "New Patient" }));
    expect(screen.getByText("Patient Information")).toBeInTheDocument();

    expect(screen.getAllByText("Order saved once")).toHaveLength(1);
  });
});
