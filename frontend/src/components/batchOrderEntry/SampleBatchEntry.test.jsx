import React from "react";
import { render, screen } from "@testing-library/react";
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

jest.mock("../utils/Utils", () => ({
  getFromOpenElisServer: jest.fn(),
  postToOpenElisServerJsonResponse: jest.fn(),
}));

jest.mock("react-router-dom", () => ({
  ...jest.requireActual("react-router-dom"),
  useNavigate: () => jest.fn(),
  useLocation: () => ({ pathname: "/" }),
  useParams: () => ({}),
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
              setNotificationVisible: jest.fn(),
              addNotification: jest.fn(),
            }}
          >
            <SampleBatchEntry
              orderFormValues={baseOrderFormValues}
              setOrderFormValues={jest.fn()}
            />
          </NotificationContext.Provider>
        </ConfigurationContext.Provider>
      </IntlProvider>
    </BrowserRouter>,
  );

describe("SampleBatchEntry rollout", () => {
  beforeEach(() => {
    window.scrollTo = jest.fn();
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
    jest.clearAllMocks();
  });

  test("renders shared labels section in batch flow", () => {
    renderSampleBatchEntry();

    expect(screen.getByText("Label quantities")).toBeInTheDocument();
  });
});
