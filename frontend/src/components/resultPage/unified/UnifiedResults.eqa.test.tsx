import React from "react";
import {
  cleanup,
  fireEvent,
  render,
  screen,
  within,
} from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../languages/en.json";
import { waitFor } from "@testing-library/dom";

const { utilsMock, notify } = vi.hoisted(() => ({
  utilsMock: {
    getFromOpenElisServer: vi.fn(),
    postToOpenElisServerJsonResponse: vi.fn(),
    postToOpenElisServerFormData: vi.fn(),
    deleteFromOpenElisServer: vi.fn(),
  },
  notify: { addNotification: vi.fn(), setNotificationVisible: vi.fn() },
}));

vi.mock("../../utils/Utils", () => utilsMock);

vi.mock("../../layout/Layout", () => ({
  NotificationContext: React.createContext({
    notificationVisible: false,
    ...notify,
  }),
  ConfigurationContext: React.createContext({
    configurationProperties: { allowResultRejection: "false" },
  }),
}));

vi.mock("../../common/CustomNotification", () => ({
  AlertDialog: () => <div />,
  NotificationKinds: { success: "success", error: "error", warning: "warning" },
}));

const { signed } = vi.hoisted(() => ({ signed: vi.fn() }));

vi.mock("../../esignature/ESignatureButton", () => ({
  __esModule: true,
  default: ({ children, onSign, onBeforeSign, disabled }: any) => (
    <button
      type="button"
      data-testid="save-button"
      disabled={disabled}
      onClick={async () => {
        try {
          await onBeforeSign?.();
        } catch {
          return;
        }
        signed();
        onSign({});
      }}
    >
      {children}
    </button>
  ),
  SignatureMeaning: {
    AUTHORED: "AUTHORED",
    MODIFIED: "MODIFIED",
    VALIDATED_AND_RELEASED: "VALIDATED_AND_RELEASED",
    REJECTED: "REJECTED",
  },
}));

vi.mock("../../common/PageBreadCrumb", () => ({ default: () => <div /> }));

import UnifiedResults from "./UnifiedResults";

const worklistRow = (extra: Record<string, unknown> = {}) => ({
  id: null,
  analysisId: "60",
  accessionNumber: "DEV0126000001",
  testName: "CD4",
  resultType: "N",
  resultValue: "",
  rawResultValue: "",
  significantDigits: 0,
  analysisStatusId: "4",
  analysisLastupdated: "1700000000000",
  normalRange: "",
  sampleType: "Whole Blood",
  eqaSample: true,
  eqaPriority: "STANDARD",
  ...extra,
});

const PER_ANALYST = { eqaPerAnalyst: true, eqaSchemeId: "7" };

const SAVED_CRITICAL = worklistRow({
  resultId: "70",
  resultValue: "5",
  rawResultValue: "5",
  resultFlag: "CRITICAL",
  analysisStatusId: "15",
});

const ANALYSTS = [
  { id: "11", displayName: "Ada Bench" },
  { id: "12", displayName: "Grace Bench" },
];

const renderPage = (row: Record<string, unknown>) => {
  utilsMock.getFromOpenElisServer.mockImplementation(
    (endPoint: string, callback: any) => {
      if (endPoint === "/rest/results-entry/lab-units")
        return callback([
          { id: "165", value: "Immunology", domain: "CLINICAL" },
        ]);
      if (endPoint === "/rest/analysis-status-types")
        return callback([{ id: "4", value: "Not started" }]);
      if (endPoint.startsWith("/rest/LogbookResults"))
        return callback({ testResult: [row] });
      if (endPoint === "/rest/eqa/schemes/7/eligible-analysts")
        return callback(ANALYSTS);
      return callback([]);
    },
  );
  render(
    <IntlProvider locale="en" messages={messages}>
      <UnifiedResults />
    </IntlProvider>,
  );
  fireEvent.change(screen.getByLabelText(/Lab Unit/i), {
    target: { value: "165" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Expand row" }));
};

const analystSelect = () =>
  document.querySelector('select[id^="eqaAnalyst-"]') as HTMLSelectElement;

const typeAndSave = (value: string) => {
  fireEvent.change(
    document.querySelector('input[id^="unifiedResultValue-"]') as Element,
    { target: { value } },
  );
  fireEvent.click(screen.getAllByTestId("save-button")[0]);
};

const savedItem = () =>
  JSON.parse(
    utilsMock.postToOpenElisServerJsonResponse.mock.calls.find((c: any[]) =>
      String(c[0]).startsWith("/rest/results-entry/analysis/60/result"),
    )[1],
  ).testResult;

describe("EQA rows on the Results page", () => {
  beforeEach(() => {
    cleanup();
    utilsMock.getFromOpenElisServer.mockReset();
    utilsMock.postToOpenElisServerJsonResponse.mockReset();
    notify.addNotification.mockReset();
    signed.mockReset();
    window.localStorage.clear();
  });

  it("offers the scheme's analysts, preselects the assigned one and saves the choice", async () => {
    renderPage(worklistRow({ ...PER_ANALYST, eqaAnalystId: "11" }));

    expect(screen.getByText("Analyst (EQA)")).toBeInTheDocument();
    expect(analystSelect().value).toBe("11");
    fireEvent.change(analystSelect(), { target: { value: "12" } });
    typeAndSave("540");

    await waitFor(() => expect(savedItem().eqaAnalystId).toBe("12"));
  });

  it("refuses a save with no analyst before anything is signed or sent", async () => {
    renderPage(worklistRow(PER_ANALYST));

    typeAndSave("540");

    await waitFor(() =>
      expect(notify.addNotification).toHaveBeenCalledWith(
        expect.objectContaining({
          kind: "error",
          message: messages["eqa.result.analystRequired"],
        }),
      ),
    );
    expect(signed).not.toHaveBeenCalled();
    expect(utilsMock.postToOpenElisServerJsonResponse).not.toHaveBeenCalledWith(
      expect.stringContaining("/rest/results-entry/analysis/"),
      expect.anything(),
      expect.anything(),
    );
  });

  it("asks for no analyst when the scheme does not record one", () => {
    renderPage(worklistRow());

    expect(screen.queryByText("Analyst (EQA)")).not.toBeInTheDocument();
    expect(analystSelect()).toBeNull();
  });

  it("tags an EQA sample and names its kind", () => {
    renderPage(worklistRow());

    expect(
      within(
        document.querySelector(".unifiedAccession") as HTMLElement,
      ).getByText("EQA"),
    ).toBeInTheDocument();
    const kind = screen.getByTestId(/^sample-kind-/);
    expect(within(kind).getByText("EQA")).toBeInTheDocument();
    expect(within(kind).queryByText("Client sample")).not.toBeInTheDocument();
  });

  it("tags a blinded in-house sample as EQA and saves it with no analyst picker", async () => {
    // The server sends no eqaPerAnalyst for it: its analyst is sealed with the panel.
    renderPage(worklistRow());

    expect(
      within(
        document.querySelector(".unifiedAccession") as HTMLElement,
      ).getByText("EQA"),
    ).toBeInTheDocument();
    expect(analystSelect()).toBeNull();
    expect(screen.queryByText("Analyst (EQA)")).not.toBeInTheDocument();
    expect(screen.queryByText("Analyst")).not.toBeInTheDocument();

    typeAndSave("540");

    await waitFor(() => expect(signed).toHaveBeenCalledTimes(1));
    expect(notify.addNotification).not.toHaveBeenCalled();
    expect(savedItem().eqaAnalystId).toBeUndefined();
  });

  it("does not ask for a clinician callback on a critical EQA result", () => {
    renderPage(SAVED_CRITICAL);

    expect(screen.queryByTestId("critical-banner")).not.toBeInTheDocument();
    expect(
      screen.queryByTestId("unified-log-callback-button"),
    ).not.toBeInTheDocument();
  });

  it("still asks for it on a critical patient result", () => {
    renderPage({ ...SAVED_CRITICAL, eqaSample: false });

    expect(screen.getByTestId("critical-banner")).toBeInTheDocument();
    expect(
      screen.getByTestId("unified-log-callback-button"),
    ).toBeInTheDocument();
  });
});
