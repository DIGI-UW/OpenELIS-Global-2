/**
 * OGC-1406 — Modify Order saves every added sample that has tests, wherever it
 * sits, and will not submit a sample that has a type but no test.
 */
import React, { useEffect } from "react";
import { act, fireEvent, render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../languages/en.json";

const { utilsMock, stepSamples } = vi.hoisted(() => ({
  utilsMock: {
    getFromOpenElisServer: vi.fn(),
    postToOpenElisServerFullResponse: vi.fn(),
  },
  stepSamples: { value: [] },
}));

vi.mock("../utils/Utils", async (importOriginal) => ({
  ...utilsMock,
  resolveApiErrorMessage: (await importOriginal()).resolveApiErrorMessage,
}));

vi.mock("../layout/Layout", () => ({
  ConfigurationContext: React.createContext({ configurationProperties: {} }),
  NotificationContext: React.createContext({
    notificationVisible: false,
    setNotificationVisible: vi.fn(),
    addNotification: vi.fn(),
  }),
}));

vi.mock("../common/CustomNotification", () => ({
  AlertDialog: () => <div />,
  NotificationKinds: { success: "success", error: "error", warning: "warning" },
}));

vi.mock("../addOrder/AddOrder", () => ({ default: () => <div /> }));
vi.mock("./EditSample", () => ({
  default: ({ setSamples }) => {
    useEffect(() => {
      setSamples(stepSamples.value);
    }, []);
    return <div />;
  },
}));
vi.mock("./EditOrderEntryAdditionalQuestions", () => ({
  default: () => <div />,
}));
vi.mock("../addOrder/OrderSuccessMessage", () => ({ default: () => <div /> }));
vi.mock("../common/PatientHeader", () => ({ default: () => <div /> }));
vi.mock("../common/PageBreadCrumb", () => ({ default: () => <div /> }));
vi.mock("../addOrder/Index", () => ({
  sampleObject: { sampleTypeId: "", tests: [], sampleXML: {} },
}));

import ModifyOrder from "./ModifyOrder";

const addedSample = (sampleTypeId, tests) => ({
  sampleTypeId,
  tests,
  panels: [],
  referralItems: [],
  sampleXML: {
    collectionDate: "",
    collectionTime: "",
    collector: "",
    rejected: false,
    rejectionReason: "",
  },
});

const blankSample = () => addedSample("", []);

const openOrderStep = () => {
  utilsMock.getFromOpenElisServer.mockImplementation((url, cb) => {
    if (url.includes("/rest/order/search")) {
      cb({ labNumber: "DEV01260000000000454", sampleOrderItems: {} });
    }
    if (url.includes("/rest/SampleEdit")) {
      cb({
        accessionNumber: "DEV01260000000000454",
        sampleOrderItems: {
          labNo: "DEV01260000000000454",
          referringSiteName: "CAMES",
          referringSiteId: "42",
          providerLastName: "Jam",
          providerFirstName: "Jim",
        },
      });
    }
  });
  render(
    <IntlProvider locale="en" messages={messages}>
      <ModifyOrder />
    </IntlProvider>,
  );
  fireEvent.click(screen.getByRole("button", { name: /next/i }));
  fireEvent.click(screen.getByRole("button", { name: /next/i }));
};

beforeEach(() => {
  window.scrollTo = vi.fn();
  utilsMock.getFromOpenElisServer.mockReset();
  utilsMock.postToOpenElisServerFullResponse.mockReset();
});

describe("Modify Order added samples (OGC-1406)", () => {
  test("a sample added after a blank Sample 1 is submitted", async () => {
    stepSamples.value = [
      blankSample(),
      addedSample("37", [{ id: "322", name: "Histopathology examination" }]),
    ];
    let body;
    utilsMock.postToOpenElisServerFullResponse.mockImplementation(
      (url, payload) => {
        body = JSON.parse(payload);
      },
    );
    openOrderStep();

    await act(async () => {
      fireEvent.click(screen.getByRole("button", { name: /submit/i }));
    });

    expect(body.sampleXML).toMatch(/<sample sampleID='37'[^>]* tests='322'/);
    expect(body.sampleXML).not.toMatch(/sampleID=''/);
  });

  test("a sample with a type but no test blocks Submit and is named", () => {
    stepSamples.value = [
      addedSample("37", [{ id: "322", name: "Histopathology examination" }]),
      addedSample("2", []),
    ];
    openOrderStep();

    expect(
      screen.getByText(
        "Sample 2 has a sample type but no test. Add a test or remove the sample before saving.",
      ),
    ).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /submit/i })).toBeDisabled();
  });

  test("a blank Sample 1 on its own does not block Submit", () => {
    stepSamples.value = [blankSample()];
    openOrderStep();

    expect(screen.getByRole("button", { name: /submit/i })).toBeEnabled();
  });
});
