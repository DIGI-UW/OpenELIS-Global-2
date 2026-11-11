import React from "react";
import {
  act,
  cleanup,
  fireEvent,
  render,
  screen,
  within,
} from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../languages/en.json";

/**
 * The merge screen reuses the shared patient search, so its two panels page
 * the server's results like every other patient search, while still hiding
 * the patient already chosen on the other side.
 */

const { utilsMock, mergeServiceMock } = vi.hoisted(() => ({
  utilsMock: {
    getFromOpenElisServer: vi.fn(),
    postToOpenElisServer: vi.fn(),
    postToOpenElisServerJsonResponse: vi.fn(),
    postToOpenElisServerFormData: vi.fn(),
  },
  mergeServiceMock: {
    getPatientMergeDetails: vi.fn(),
  },
}));

vi.mock("../../utils/Utils", () => utilsMock);
vi.mock("./patientMergeService", () => mergeServiceMock);

vi.mock("../../layout/Layout", () => ({
  NotificationContext: React.createContext({
    notificationVisible: false,
    setNotificationVisible: vi.fn(),
    addNotification: vi.fn(),
  }),
  ConfigurationContext: React.createContext({
    configurationProperties: {
      UseExternalPatientInfo: "false",
      ENABLE_CLIENT_REGISTRY: "false",
    },
  }),
}));

vi.mock("../../common/CustomNotification", () => ({
  AlertDialog: () => <div />,
  NotificationKinds: { success: "success", error: "error", warning: "warning" },
}));

vi.mock("../photoManagement/photoAvatar/AyncAvatar", () => ({
  default: () => <span />,
}));

import PatientSearchPanel from "./PatientSearchPanel";

const patient = (id: string, lastName = "PAGEPATIENT") => ({
  patientID: id,
  lastName,
  firstName: `Number${id}`,
  gender: "F",
  dob: "01/02/1980",
  nationalId: `PAGEPAT${id}`,
  dataSourceName: "OpenElis",
});

const PAGE_ONE = {
  paging: { currentPage: "1", totalPages: "2" },
  patientSearchResults: [patient("35"), patient("36"), patient("37")],
};
const PAGE_TWO = {
  paging: { currentPage: "2", totalPages: "2" },
  patientSearchResults: [patient("38")],
};

const requests = () =>
  utilsMock.getFromOpenElisServer.mock.calls.map((c: any[]) => String(c[0]));

const wire = () => {
  utilsMock.getFromOpenElisServer.mockImplementation(
    (endPoint: string, callback: any) => {
      if (endPoint.startsWith("/rest/patient-search-results")) {
        return callback(endPoint.includes("&page=2") ? PAGE_TWO : PAGE_ONE);
      }
      if (endPoint.startsWith("/rest/patient-details")) {
        const id = endPoint.split("patientID=")[1];
        return callback({
          patientPK: id,
          lastName: "PAGEPATIENT",
          firstName: `Number${id}`,
          gender: "F",
          birthDateForDisplay: "01/02/1980",
          nationalId: `PAGEPAT${id}`,
        });
      }
      if (endPoint.startsWith("/rest/patient-photos")) {
        return callback({ data: "" });
      }
      return callback([]);
    },
  );
  mergeServiceMock.getPatientMergeDetails.mockResolvedValue({
    patientId: "35",
    dataSummary: { totalOrders: 4 },
  });
};

const renderPanel = (
  props: Partial<React.ComponentProps<typeof PatientSearchPanel>> = {},
) => {
  const onPatientSelect = vi.fn();
  const utils = render(
    <IntlProvider locale="en" messages={messages}>
      <PatientSearchPanel
        panelId="patient2"
        title="Select second patient"
        selectedPatient={null}
        onPatientSelect={onPatientSelect}
        otherSelectedPatient={null}
        {...props}
      />
    </IntlProvider>,
  );
  return { ...utils, onPatientSelect };
};

/** Formik submits after its own validation promise, so the click is flushed. */
const search = async () => {
  fireEvent.change(document.getElementById("patient2-lastName")!, {
    target: { value: "PAGEPATIENT" },
  });
  await act(async () => {
    fireEvent.click(document.getElementById("patient2-local_search")!);
    await new Promise((resolve) => setTimeout(resolve, 0));
  });
};

const rowIds = () =>
  Array.from(document.querySelectorAll("tbody tr")).map((row) =>
    (row.getAttribute("data-cy") || "").replace("patient-result-row-", ""),
  );

describe("PatientSearchPanel on the merge screen", () => {
  beforeEach(() => {
    cleanup();
    utilsMock.getFromOpenElisServer.mockReset();
    mergeServiceMock.getPatientMergeDetails.mockReset();
    wire();
  });

  it("prefixes the shared form's element ids with the panel id", () => {
    renderPanel();
    expect(document.getElementById("patient2-lastName")).not.toBeNull();
    expect(document.getElementById("patient2-local_search")).not.toBeNull();
    expect(document.getElementById("lastName")).toBeNull();
  });

  it("hides the patient already chosen on the other side and keeps the rest", async () => {
    renderPanel({ otherSelectedPatient: { patientID: "36", patientPK: "36" } });
    await search();

    expect(rowIds()).toEqual(["35", "37"]);
  });

  it("walks the server's pages from the arrows like every other patient search", async () => {
    renderPanel();
    await search();
    expect(rowIds()).toEqual(["35", "36", "37"]);
    expect(screen.getByText("1 / 2")).toBeInTheDocument();

    fireEvent.click(document.getElementById("loadnextresults")!);

    expect(rowIds()).toEqual(["38"]);
    expect(requests().filter((url) => url.includes("&page=2")).length).toBe(1);
    expect(screen.getByText("2 / 2")).toBeInTheDocument();
  });

  it("hands the chosen patient on with its key and merge summary", async () => {
    const { onPatientSelect } = renderPanel();
    await search();
    expect(rowIds()).toEqual(["35", "36", "37"]);

    await act(async () => {
      fireEvent.click(
        within(
          document.querySelector('[data-cy="patient-result-row-35"]')!,
        ).getByRole("radio"),
      );
      await Promise.resolve();
    });

    expect(onPatientSelect).toHaveBeenCalledTimes(1);
    expect(mergeServiceMock.getPatientMergeDetails).toHaveBeenCalledWith("35");
    const selected = onPatientSelect.mock.calls[0][0];
    expect(selected.patientPK).toBe("35");
    expect(selected.patientID).toBe("35");
    expect(selected.dob).toBe("01/02/1980");
    expect(selected.dataSummary).toEqual({ totalOrders: 4 });
  });

  it("replaces the search with the change-selection button once a patient is chosen", () => {
    renderPanel({ selectedPatient: { patientID: "35", patientPK: "35" } });
    expect(document.getElementById("patient2-lastName")).toBeNull();
    expect(
      screen.getByRole("button", { name: /Search for different patient/i }),
    ).toBeInTheDocument();
  });
});
