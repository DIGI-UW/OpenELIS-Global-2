/**
 * PanelEditor + PanelBasicInfoSection — OGC-224 C2 (FRS v2.2).
 *
 * - the editor shell shows the PANEL badge, name, domain tag and LOINC;
 * - create mode ("new") titles "New panel" and skips the envelope fetch;
 * - Basic Info: only Clinical is enabled (Env/Vector disabled, later-phase
 *   note), sample types render read-only (derived), the Active toggle is
 *   disabled with helper text while the panel has zero tests (activation
 *   rule), and Save PUTs the basic-info payload;
 * - create flow: POST {name, active:false} then basic-info PUT.
 */

// ========== MOCKS (before imports) ==========
const mockHistory = {
  push: vi.fn(),
  replace: vi.fn(),
  location: {
    pathname: "/MasterListsPage/TestCatalogEditor/panel/1/basic-info",
    search: "",
  },
};
let mockParams = { panelId: "1", section: "basic-info" };

vi.mock("react-router-dom", async (importOriginal) => {
  const actual = await importOriginal();
  return {
    ...actual,
    useHistory: () => mockHistory,
    useParams: () => mockParams,
  };
});

vi.mock("../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
  postToOpenElisServerJsonResponse: vi.fn(),
  putToOpenElisServerFullResponse: vi.fn(),
}));

vi.mock("../../common/PageBreadCrumb", () => ({ default: () => null }));

// ========== IMPORTS ==========
import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { BrowserRouter } from "react-router-dom";
import PanelEditor from "./PanelEditor";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
  putToOpenElisServerFullResponse,
} from "../../utils/Utils";
import { NotificationContext } from "../../layout/Layout";
import messages from "../../../languages/en.json";

const PANEL = {
  id: "1",
  name: "Bilan Biochimique",
  description: "Bilan Biochimique",
  loinc: "24323-8",
  domain: "CLINICAL",
  active: true,
  testCount: 9,
  sampleTypes: ["Serum", "Plasma"],
};

const notification = {
  addNotification: vi.fn(),
  setNotificationVisible: vi.fn(),
};

const wrap = () =>
  render(
    <BrowserRouter>
      <IntlProvider locale="en" messages={messages}>
        <NotificationContext.Provider value={notification}>
          <PanelEditor />
        </NotificationContext.Provider>
      </IntlProvider>
    </BrowserRouter>,
  );

beforeEach(() => {
  vi.clearAllMocks();
  mockParams = { panelId: "1", section: "basic-info" };
  getFromOpenElisServer.mockImplementation((url, cb) => {
    if (url.startsWith("/rest/test-catalog/panels/1")) {
      cb(PANEL);
    } else {
      cb(undefined);
    }
  });
});

describe("PanelEditor shell (OGC-224 C2)", () => {
  it("shows the PANEL badge, name, domain tag and LOINC", async () => {
    wrap();
    expect(await screen.findByTestId("panel-editor-title")).toHaveTextContent(
      "Bilan Biochimique",
    );
    expect(screen.getByText("PANEL")).toBeInTheDocument();
    expect(screen.getAllByText("Clinical").length).toBeGreaterThan(0);
    expect(screen.getByTestId("panel-editor-loinc")).toHaveTextContent(
      "24323-8",
    );
  });

  it("create mode titles New panel and skips the envelope fetch", async () => {
    mockParams = { panelId: "new", section: "basic-info" };
    wrap();
    expect(await screen.findByTestId("panel-editor-title")).toHaveTextContent(
      "New panel",
    );
    expect(
      getFromOpenElisServer.mock.calls.every(
        ([url]) => !url.startsWith("/rest/test-catalog/panels/new"),
      ),
    ).toBe(true);
  });

  it("canonicalizes an unknown section to basic-info", async () => {
    mockParams = { panelId: "1", section: "bogus" };
    wrap();
    await waitFor(() =>
      expect(mockHistory.replace).toHaveBeenCalledWith(
        "/MasterListsPage/TestCatalogEditor/panel/1/basic-info",
      ),
    );
  });
});

describe("PanelBasicInfoSection (FRS rules)", () => {
  it("only Clinical is enabled; Environmental and Vector are disabled with the later-phase note", async () => {
    wrap();
    await screen.findByTestId("panel-editor-title");
    expect(screen.getByLabelText("Clinical")).toBeEnabled();
    expect(screen.getByLabelText("Environmental")).toBeDisabled();
    expect(screen.getByLabelText("Vector")).toBeDisabled();
    expect(screen.getByText(/enabled in a later phase/i)).toBeInTheDocument();
  });

  it("derived sample types render read-only", async () => {
    wrap();
    await screen.findByTestId("panel-editor-title");
    const tile = screen.getByTestId("panel-derived-sample-types");
    expect(tile).toHaveTextContent("Serum");
    expect(tile).toHaveTextContent("Plasma");
    expect(
      screen.getByText(messages["note.panel.sampleTypesDerived"]),
    ).toBeInTheDocument();
  });

  it("the Active toggle is disabled with helper text while the panel has zero tests", async () => {
    getFromOpenElisServer.mockImplementation((url, cb) =>
      cb({ ...PANEL, testCount: 0, active: false, sampleTypes: [] }),
    );
    wrap();
    await screen.findByTestId("panel-editor-title");
    expect(document.querySelector("#panel-active")).toBeDisabled();
    expect(screen.getByTestId("panel-needs-test-helper")).toHaveTextContent(
      messages["helper.panel.needsTest"],
    );
  });

  it("Save PUTs the basic-info payload", async () => {
    wrap();
    await screen.findByTestId("panel-editor-title");
    fireEvent.change(document.querySelector("#panel-description"), {
      target: { value: "Chem bundle" },
    });
    fireEvent.click(screen.getByRole("button", { name: /^save$/i }));
    expect(putToOpenElisServerFullResponse).toHaveBeenCalledWith(
      "/rest/test-catalog/panels/1/basic-info",
      JSON.stringify({
        name: "Bilan Biochimique",
        description: "Chem bundle",
        domain: "CLINICAL",
        active: true,
      }),
      expect.any(Function),
    );
  });

  it("create flow POSTs {name, active:false} first (never active with zero tests)", async () => {
    mockParams = { panelId: "new", section: "basic-info" };
    wrap();
    await screen.findByTestId("panel-editor-title");
    fireEvent.change(document.querySelector("#panel-name"), {
      target: { value: "Anemia Workup" },
    });
    fireEvent.click(screen.getByRole("button", { name: /^save$/i }));
    expect(postToOpenElisServerJsonResponse).toHaveBeenCalledWith(
      "/rest/test-catalog/panels",
      JSON.stringify({ name: "Anemia Workup", active: false }),
      expect.any(Function),
    );
  });
});
