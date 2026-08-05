import React from "react";
import { fireEvent, render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import messages from "../../../languages/en.json";
import UserSessionDetailsContext from "../../../UserSessionDetailsContext";
import Accreditation from "./Accreditation";
import {
  deleteFromOpenElisServer,
  getFromOpenElisServer,
  postToOpenElisServer,
  postToOpenElisServerFullResponse,
  putToOpenElisServerFullResponse,
} from "../../utils/Utils";

vi.mock("../../utils/Utils", async (importOriginal) => {
  const actual = await importOriginal();
  return {
    ...actual,
    getFromOpenElisServer: vi.fn(),
    postToOpenElisServer: vi.fn(),
    postToOpenElisServerFormData: vi.fn(),
    postToOpenElisServerFullResponse: vi.fn(),
    putToOpenElisServerFullResponse: vi.fn(),
    deleteFromOpenElisServer: vi.fn(),
  };
});

const BODIES = [
  {
    id: 1,
    code: "ISO15189",
    name: "ISO 15189",
    logoImageId: "77",
    expiresOn: "2027-01-31",
    logoVisibilityMode: "ANY_ACCREDITED_TEST",
    thresholdPct: 80,
    displayOrder: 0,
    active: true,
    enrolledTestCount: 2,
    status: "ACTIVE",
  },
  {
    id: 2,
    code: "SANAS",
    name: "SANAS",
    logoImageId: null,
    expiresOn: "2026-09-01",
    logoVisibilityMode: "PERCENTAGE",
    thresholdPct: 50,
    displayOrder: 1,
    active: true,
    enrolledTestCount: 0,
    status: "EXPIRING",
  },
];

const ENROLLMENTS = [
  {
    id: 10,
    testId: "42",
    testName: "Malaria RDT (Blood)",
    accreditingBodyId: 1,
    bodyCode: "ISO15189",
    bodyName: "ISO 15189",
    effectiveFrom: "2025-02-01",
    bodyExpiresOn: "2027-01-31",
    status: "ACTIVE",
  },
  {
    id: 11,
    testId: "43",
    testName: "Hemoglobin (Blood)",
    accreditingBodyId: 1,
    bodyCode: "ISO15189",
    bodyName: "ISO 15189",
    effectiveFrom: null,
    bodyExpiresOn: "2027-01-31",
    status: "ACTIVE",
  },
];

const SUMMARY = {
  totalBodies: 2,
  activeBodies: 1,
  expiringBodies: 1,
  expiredBodies: 0,
  inForceBodyNames: ["ISO 15189", "SANAS"],
  worstStatus: "EXPIRING",
};

const mockEndpoints = ({ bodies = BODIES, enrollments = ENROLLMENTS } = {}) => {
  getFromOpenElisServer.mockImplementation((url, callback) => {
    if (url.includes("/rest/accreditation/summary")) {
      callback(SUMMARY);
    } else if (url.includes("/rest/accreditation/bodies")) {
      callback(bodies);
    } else if (url.includes("/rest/accreditation/enrollments")) {
      callback(enrollments);
    } else if (url.includes("/rest/displayList/ALL_TESTS")) {
      callback([
        { id: "42", value: "Malaria RDT (Blood)" },
        { id: "44", value: "Glucose (Serum)" },
      ]);
    } else {
      callback();
    }
  });
};

const renderPage = ({
  permissions = ["qa.view.qms", "qa.manage.accreditation"],
  roles = [],
  entry = "/qa/qms/accreditation",
} = {}) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <UserSessionDetailsContext.Provider
        value={{
          userSessionDetails: { authenticated: true, roles, permissions },
          errorLoadingSessionDetails: false,
          isCheckingLogin: () => false,
          logout: vi.fn(),
        }}
      >
        <MemoryRouter initialEntries={[entry]}>
          <Accreditation />
        </MemoryRouter>
      </UserSessionDetailsContext.Provider>
    </IntlProvider>,
  );

const fetchCount = (fragment) =>
  getFromOpenElisServer.mock.calls.filter((c) => c[0].includes(fragment))
    .length;

// Carbon keeps every Modal mounted and marks the open one is-visible, so the
// page has four sets of Save/Cancel buttons at all times — scope to the open one.
const openModal = () => document.querySelector(".cds--modal.is-visible");

describe("Accreditation page", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  test("renders summary tiles, bodies and enrollments with status tags", () => {
    mockEndpoints();
    renderPage();

    expect(screen.getByText("In force: ISO 15189, SANAS")).toBeInTheDocument();
    expect(
      within(screen.getByTestId("body-1")).getByText("Active"),
    ).toBeInTheDocument();
    expect(
      within(screen.getByTestId("body-2")).getByText("Expiring soon"),
    ).toBeInTheDocument();
    expect(screen.getByTestId("body-1")).toHaveTextContent("ISO15189");
    expect(screen.getByTestId("enrollment-10")).toHaveTextContent(
      "Malaria RDT (Blood)",
    );
    // effectiveFrom is optional — a null renders as an em dash, not "null"
    expect(screen.getByTestId("enrollment-11")).toHaveTextContent("—");
  });

  test("hides write controls without qa.manage.accreditation", () => {
    mockEndpoints();
    renderPage({ permissions: ["qa.view.qms"] });

    expect(screen.queryByTestId("add-body-button")).not.toBeInTheDocument();
    expect(screen.queryByTestId("enroll-tests-button")).not.toBeInTheDocument();
    expect(screen.queryByTestId("edit-body-1")).not.toBeInTheDocument();
    expect(
      screen.queryByTestId("delete-enrollment-10"),
    ).not.toBeInTheDocument();
  });

  test("shows write controls for a Global Administrator without the permission", () => {
    mockEndpoints();
    renderPage({
      permissions: ["qa.view.qms"],
      roles: ["Global Administrator"],
    });

    expect(screen.getByTestId("add-body-button")).toBeInTheDocument();
  });

  test("blocks deleting a body while tests are enrolled and deletes when empty", () => {
    mockEndpoints();
    renderPage();

    expect(screen.getByTestId("delete-body-1")).toBeDisabled();

    fireEvent.click(screen.getByTestId("delete-body-2"));
    const confirm = openModal();
    expect(confirm).toHaveTextContent("Delete SANAS?");
    fireEvent.click(within(confirm).getByRole("button", { name: /Delete/ }));

    expect(deleteFromOpenElisServer).toHaveBeenCalledWith(
      "/rest/accreditation/bodies/2",
      expect.any(Function),
    );
    const before = fetchCount("/rest/accreditation/bodies");
    deleteFromOpenElisServer.mock.calls[0][1](204);
    expect(fetchCount("/rest/accreditation/bodies")).toBe(before + 1);
  });

  test("body modal requires code, name and expiry before posting", () => {
    mockEndpoints();
    renderPage();

    fireEvent.click(screen.getByTestId("add-body-button"));
    fireEvent.click(within(openModal()).getByRole("button", { name: "Save" }));

    expect(
      screen.getByText("Code, name and expiry date are required."),
    ).toBeInTheDocument();
    expect(postToOpenElisServerFullResponse).not.toHaveBeenCalled();
  });

  test("body modal posts a new body and locks the code when editing", () => {
    mockEndpoints();
    renderPage();

    fireEvent.click(screen.getByTestId("add-body-button"));
    const modal = within(openModal());
    fireEvent.change(modal.getByLabelText(/^Code/), {
      target: { value: "cofrac" },
    });
    fireEvent.change(modal.getByLabelText(/^Name/), {
      target: { value: "COFRAC" },
    });
    // Carbon's DatePicker is flatpickr — typing into the input does not reach
    // its onChange in jsdom, so drive the instance the way a click would.
    modal
      .getByLabelText(/Accreditation expires/)
      ._flatpickr.setDate("2028-03-31", true);
    fireEvent.click(modal.getByRole("button", { name: "Save" }));

    expect(postToOpenElisServerFullResponse).toHaveBeenCalledWith(
      "/rest/accreditation/bodies",
      expect.any(String),
      expect.any(Function),
    );
    expect(
      JSON.parse(postToOpenElisServerFullResponse.mock.calls[0][1]),
    ).toMatchObject({
      code: "COFRAC",
      name: "COFRAC",
      expiresOn: "2028-03-31",
      active: true,
    });

    fireEvent.click(screen.getByTestId("edit-body-1"));
    const editModal = within(openModal());
    expect(editModal.getByLabelText(/^Code/)).toBeDisabled();
    fireEvent.click(editModal.getByRole("button", { name: "Save" }));
    expect(putToOpenElisServerFullResponse).toHaveBeenCalledWith(
      "/rest/accreditation/bodies/1",
      expect.any(String),
      expect.any(Function),
    );
  });

  test("a rejected body shows the server's reason, not a generic message", async () => {
    mockEndpoints();
    renderPage();

    fireEvent.click(screen.getByTestId("add-body-button"));
    const modal = within(openModal());
    fireEvent.change(modal.getByLabelText(/^Code/), {
      target: { value: "ISO15189" },
    });
    fireEvent.change(modal.getByLabelText(/^Name/), {
      target: { value: "Duplicate" },
    });
    modal
      .getByLabelText(/Accreditation expires/)
      ._flatpickr.setDate("2028-03-31", true);
    fireEvent.click(modal.getByRole("button", { name: "Save" }));

    // the backend answers 400 {"error": "..."} — that text is what the user needs
    await postToOpenElisServerFullResponse.mock.calls[0][2]({
      ok: false,
      status: 400,
      json: () =>
        Promise.resolve({
          error: "An accrediting body with code ISO15189 exists",
        }),
    });

    expect(
      await screen.findByText("An accrediting body with code ISO15189 exists"),
    ).toBeInTheDocument();
    expect(
      screen.queryByText("The accrediting body could not be saved."),
    ).not.toBeInTheDocument();
  });

  test("enroll modal posts one call per selected test and reports partial failures", async () => {
    mockEndpoints();
    renderPage();
    // downshift's multi-select only reacts to real pointer events, not fireEvent
    const user = userEvent.setup();

    fireEvent.click(screen.getByTestId("enroll-tests-button"));
    const modal = within(openModal());
    fireEvent.click(openModal().querySelector(".cds--list-box__field"));
    fireEvent.click(modal.getByText("ISO15189 — ISO 15189"));
    fireEvent.click(modal.getByPlaceholderText("Tests"));
    await user.click(
      modal.getByRole("option", { name: "Malaria RDT (Blood)" }),
    );
    await user.click(modal.getByRole("option", { name: "Glucose (Serum)" }));
    fireEvent.click(modal.getByRole("button", { name: "Save" }));

    expect(postToOpenElisServer).toHaveBeenCalledTimes(2);
    expect(JSON.parse(postToOpenElisServer.mock.calls[0][1])).toMatchObject({
      accreditingBodyId: 1,
      effectiveFrom: null,
    });

    const before = fetchCount("/rest/accreditation/enrollments");
    postToOpenElisServer.mock.calls[0][2](201);
    postToOpenElisServer.mock.calls[1][2](400);
    expect(screen.getByText(/1 of 2 enrollments failed/)).toBeInTheDocument();
    // the one that landed is still refetched
    expect(fetchCount("/rest/accreditation/enrollments")).toBe(before + 1);
  });

  test("?testId= preselects the enrollment filter and the chip clears it", () => {
    mockEndpoints();
    renderPage({ entry: "/qa/qms/accreditation?testId=42" });

    expect(screen.getByTestId("enrollment-10")).toBeInTheDocument();
    expect(screen.queryByTestId("enrollment-11")).not.toBeInTheDocument();

    const tag = screen.getByTestId("test-filter-tag");
    expect(tag).toHaveTextContent("Test: Malaria RDT (Blood)");
    fireEvent.click(within(tag).getByRole("button"));
    expect(screen.getByTestId("enrollment-11")).toBeInTheDocument();
  });

  test("shows the empty state when no bodies exist", () => {
    mockEndpoints({ bodies: [], enrollments: [] });
    renderPage();

    expect(screen.getByText("No accrediting bodies yet")).toBeInTheDocument();
    expect(screen.queryByTestId("enroll-tests-button")).not.toBeInTheDocument();
  });

  // Carbon's DataTable re-derives its row state in an effect, so the render right
  // after a filter narrows the list still yields the previous ids. Looking cells up
  // from the filtered slice threw here and took the whole page down.
  test("narrowing the enrollment search does not crash the table", () => {
    mockEndpoints();
    renderPage();

    fireEvent.change(screen.getByLabelText("Search tests"), {
      target: { value: "malaria" },
    });

    expect(screen.getByTestId("enrollment-10")).toBeInTheDocument();
    expect(screen.queryByTestId("enrollment-11")).not.toBeInTheDocument();
    expect(screen.getByTestId("accreditation-page")).toBeInTheDocument();
  });

  test("deleting the body currently filtered on resets the filter instead of crashing", async () => {
    mockEndpoints();
    const user = userEvent.setup();
    renderPage();

    await user.click(
      document.querySelector("#accreditation-body-filter button"),
    );
    await user.click(screen.getByRole("option", { name: /SANAS/ }));

    fireEvent.click(screen.getByTestId("delete-body-2"));
    fireEvent.click(
      within(openModal()).getByRole("button", { name: /Delete/ }),
    );
    // the refetch now answers without SANAS, leaving the filter pointing at a ghost
    mockEndpoints({ bodies: [BODIES[0]] });
    deleteFromOpenElisServer.mock.calls[0][1](204);

    expect(screen.queryByTestId("body-2")).not.toBeInTheDocument();
    expect(screen.getByTestId("accreditation-page")).toBeInTheDocument();
    // filter fell back to "all", so the surviving body's enrollments still show
    expect(screen.getByTestId("enrollment-10")).toBeInTheDocument();
  });

  test("a failed enrollments fetch reports an error instead of an empty result", () => {
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url.includes("/rest/accreditation/summary")) {
        callback(SUMMARY);
      } else if (url.includes("/rest/accreditation/bodies")) {
        callback(BODIES);
      } else {
        callback(undefined);
      }
    });
    renderPage();

    expect(
      screen.getByText("Accreditation data could not be loaded."),
    ).toBeInTheDocument();
    expect(
      screen.queryByText("No enrollments match these filters"),
    ).not.toBeInTheDocument();
  });
});
