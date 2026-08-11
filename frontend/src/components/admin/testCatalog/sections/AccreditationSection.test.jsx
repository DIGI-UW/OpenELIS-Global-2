/**
 * AccreditationSection — OGC-686 (QA-D.3).
 *
 * The per-test view of accreditation: which bodies accredit this test, add and
 * remove. Covers render, the empty and error states, the permission gate, that
 * the add list excludes bodies the test already has, and that a failed write
 * says so instead of silently doing nothing.
 */

// ========== MOCKS (before imports) ==========
vi.mock("../../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
  postToOpenElisServer: vi.fn(),
  deleteFromOpenElisServer: vi.fn(),
}));

// ========== IMPORTS ==========
import React from "react";
import { fireEvent, render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import AccreditationSection from "./AccreditationSection";
import UserSessionDetailsContext from "../../../../UserSessionDetailsContext";
import {
  deleteFromOpenElisServer,
  getFromOpenElisServer,
  postToOpenElisServer,
} from "../../../utils/Utils";
import messages from "../../../../languages/en.json";

const BODIES = [
  {
    id: 1,
    code: "ISO15189",
    name: "ISO 15189",
    status: "ACTIVE",
    active: true,
  },
  {
    id: 2,
    code: "SANAS",
    name: "SANAS General",
    status: "EXPIRING",
    active: true,
  },
  {
    id: 3,
    code: "OLDBODY",
    name: "Retired Body",
    status: "INACTIVE",
    active: false,
  },
];

const ENROLLMENTS = [
  {
    id: 10,
    testId: "42",
    accreditingBodyId: 1,
    bodyCode: "ISO15189",
    bodyName: "ISO 15189",
    effectiveFrom: "2025-02-01",
    bodyExpiresOn: "2027-01-31",
    status: "ACTIVE",
  },
];

const mockEndpoints = ({ enrollments = ENROLLMENTS, bodies = BODIES } = {}) => {
  getFromOpenElisServer.mockImplementation((url, callback) => {
    if (url.includes("/rest/accreditation/enrollments")) {
      callback(enrollments);
    } else if (url.includes("/rest/accreditation/bodies")) {
      callback(bodies);
    } else {
      callback();
    }
  });
};

const renderSection = ({
  permissions = ["qa.view.qms", "qa.manage.accreditation"],
  roles = [],
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
        <AccreditationSection testId="42" />
      </UserSessionDetailsContext.Provider>
    </IntlProvider>,
  );

const openModal = () => document.querySelector(".cds--modal.is-visible");

describe("AccreditationSection", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  test("lists the bodies accrediting this test with the body's status", () => {
    mockEndpoints();
    renderSection();

    const row = screen.getByTestId("accreditation-10");
    expect(row).toHaveTextContent("ISO15189 — ISO 15189");
    expect(row).toHaveTextContent("2027-01-31");
    expect(within(row).getByText("Active")).toBeInTheDocument();
  });

  test("says so when the test is not accredited", () => {
    mockEndpoints({ enrollments: [] });
    renderSection();

    expect(
      screen.getByText("This test is not accredited by any body."),
    ).toBeInTheDocument();
  });

  test("reports a failed load instead of an empty list", () => {
    getFromOpenElisServer.mockImplementation((url, callback) => callback());
    renderSection();

    expect(
      screen.getByText("Accreditation could not be loaded."),
    ).toBeInTheDocument();
    expect(
      screen.queryByText("This test is not accredited by any body."),
    ).not.toBeInTheDocument();
  });

  test("hides write controls without qa.manage.accreditation", () => {
    mockEndpoints();
    renderSection({ permissions: ["qa.view.qms"] });

    expect(
      screen.queryByTestId("add-accreditation-button"),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByTestId("delete-accreditation-10"),
    ).not.toBeInTheDocument();
    // Reading the current state is still the point of the section.
    expect(screen.getByTestId("accreditation-10")).toBeInTheDocument();
  });

  test("the add list omits a body this test already has", async () => {
    const user = userEvent.setup();
    mockEndpoints();
    renderSection();

    await user.click(screen.getByTestId("add-accreditation-button"));
    await user.click(within(openModal()).getByRole("combobox"));

    expect(screen.getByText("SANAS — SANAS General")).toBeInTheDocument();
    // ISO 15189 is already enrolled; offering it again would only produce the
    // backend's duplicate-enrollment 400.
    expect(
      within(openModal()).queryByText("ISO15189 — ISO 15189"),
    ).not.toBeInTheDocument();
    // A deactivated body accredits nothing — no logo, no notes line, no coverage.
    expect(
      within(openModal()).queryByText("OLDBODY — Retired Body"),
    ).not.toBeInTheDocument();
  });

  test("adding posts the enrollment and reloads", async () => {
    const user = userEvent.setup();
    mockEndpoints();
    renderSection();

    await user.click(screen.getByTestId("add-accreditation-button"));
    await user.click(within(openModal()).getByRole("combobox"));
    await user.click(screen.getByText("SANAS — SANAS General"));
    fireEvent.click(within(openModal()).getByText("Save"));

    expect(postToOpenElisServer).toHaveBeenCalledWith(
      "/rest/accreditation/enrollments",
      JSON.stringify({ testId: "42", accreditingBodyId: 2 }),
      expect.any(Function),
    );

    const fetchesBefore = getFromOpenElisServer.mock.calls.length;
    postToOpenElisServer.mock.calls[0][2](201);
    expect(getFromOpenElisServer.mock.calls.length).toBeGreaterThan(
      fetchesBefore,
    );
    expect(screen.getByText("Accreditation added")).toBeInTheDocument();
  });

  test("a rejected add surfaces the error", async () => {
    const user = userEvent.setup();
    mockEndpoints();
    renderSection();

    await user.click(screen.getByTestId("add-accreditation-button"));
    await user.click(within(openModal()).getByRole("combobox"));
    await user.click(screen.getByText("SANAS — SANAS General"));
    fireEvent.click(within(openModal()).getByText("Save"));
    postToOpenElisServer.mock.calls[0][2](400);

    expect(
      screen.getByText("The accreditation could not be added."),
    ).toBeInTheDocument();
  });

  test("removing asks first, then deletes", () => {
    mockEndpoints();
    renderSection();

    fireEvent.click(screen.getByTestId("delete-accreditation-10"));
    expect(deleteFromOpenElisServer).not.toHaveBeenCalled();

    fireEvent.click(within(openModal()).getByText("Remove"));
    expect(deleteFromOpenElisServer).toHaveBeenCalledWith(
      "/rest/accreditation/enrollments/10",
      expect.any(Function),
    );
  });
});
