/**
 * The sample XML Add Order submits: each sample carries only its own panels.
 */
import React, { useEffect } from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import messages from "../../languages/en.json";

const { utilsMock, stepSamples, orderProps } = vi.hoisted(() => ({
  utilsMock: {
    getFromOpenElisServer: vi.fn(),
    postToOpenElisServerFormData: vi.fn(),
    postToOpenElisServerJsonResponse: vi.fn(),
    resolveApiErrorMessage: vi.fn(),
  },
  stepSamples: { value: [] },
  orderProps: [],
}));
vi.mock("../utils/Utils", () => utilsMock);

vi.mock("../layout/Layout", () => ({
  NotificationContext: React.createContext({
    notificationVisible: false,
    setNotificationVisible: vi.fn(),
    addNotification: vi.fn(),
  }),
  ConfigurationContext: React.createContext({
    configurationProperties: { AUTOFILL_COLLECTION_DATE: "false" },
  }),
}));

vi.mock("../common/CustomNotification", () => ({
  AlertDialog: () => <div />,
  NotificationKinds: { success: "success", error: "error" },
}));

vi.mock("./PatientInfo", () => ({ default: () => <div /> }));
vi.mock("./AddSample", () => ({
  default: ({ setSamples }) => {
    useEffect(() => {
      setSamples(stepSamples.value);
    }, []);
    return <div />;
  },
}));
vi.mock("./AddOrder", () => ({
  default: (props) => {
    orderProps.push(props);
    return <div />;
  },
}));
vi.mock("./OrderEntryAdditionalQuestions", () => ({ default: () => <div /> }));
vi.mock("./OrderSuccessMessage", () => ({ default: () => <div /> }));
vi.mock("../eqa/EQASampleEntry", () => ({ default: () => <div /> }));
vi.mock("../eqa/EQAOrderForm", () => ({ default: () => <div /> }));
vi.mock("../common/PageBreadCrumb", () => ({ default: () => <div /> }));

import Index from "./Index";

const sample = (sampleTypeId, tests, panels = []) => ({
  index: 0,
  sampleRejected: false,
  rejectionReason: "",
  sampleTypeId,
  sampleXML: {
    collectionDate: "",
    collectionTime: "",
    collector: "",
    quantity: "",
    uom: "",
    rejected: false,
    rejectionReason: "",
  },
  panels,
  tests,
  requestReferralEnabled: false,
  referralItems: [],
});

const submittedSampleXml = async () => {
  window.history.pushState({}, "", "/SamplePatientEntry");
  orderProps.length = 0;
  render(
    <MemoryRouter>
      <IntlProvider locale="en" messages={messages}>
        <Index />
      </IntlProvider>
    </MemoryRouter>,
  );
  fireEvent.click(screen.getByRole("button", { name: /^Add Sample/ }));
  fireEvent.click(screen.getByRole("button", { name: /^Add Order/ }));
  await waitFor(() =>
    expect(orderProps.at(-1)?.orderFormValues?.sampleXML).toBeTruthy(),
  );
  return orderProps.at(-1).orderFormValues.sampleXML;
};

describe("Add Order sample XML", () => {
  it("does not give a sample the panels of the sample before it", async () => {
    stepSamples.value = [
      sample(
        "2",
        [{ id: "39", name: "Western blot VIH(Serum)" }],
        [{ id: "4", name: "Serologie VIH", testIds: "39,40" }],
      ),
      sample("3", [{ id: "40", name: "Western blot VIH(Plasma)" }]),
    ];

    const xml = await submittedSampleXml();

    expect(xml).toMatch(/<sample sampleID='2'[^>]* tests='39'[^>]* panels='4'/);
    expect(xml).toMatch(/<sample sampleID='3'[^>]* tests='40'[^>]* panels=''/);
  });

  it("keeps a sample added after a blank Sample 1 (OGC-1406)", async () => {
    stepSamples.value = [
      sample("", []),
      sample("37", [{ id: "322", name: "Histopathology examination" }]),
    ];

    const xml = await submittedSampleXml();

    expect(xml).toMatch(/<sample sampleID='37'[^>]* tests='322'/);
    expect(xml).not.toMatch(/sampleID=''/);
  });

  it("names a sample with a type but no test on the last step (OGC-1406)", async () => {
    stepSamples.value = [
      sample("2", [{ id: "4", name: "Creatinine" }]),
      sample("37", []),
    ];

    await submittedSampleXml();

    expect(
      screen.getByText(
        "Sample 2 has a sample type but no test. Add a test or remove the sample before saving.",
      ),
    ).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Submit" })).toBeDisabled();
  });
});
