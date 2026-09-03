import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../../languages/en.json";
import ProgramManagement from "../ProgramManagement";
import UserSessionDetailsContext from "../../../../UserSessionDetailsContext";
import ProgramForm from "../ProgramForm";
import {
  getFromOpenElisServer,
  postToOpenElisServerFullResponse,
  putToOpenElisServerFullResponse,
} from "../../../utils/Utils";

vi.mock("../../../../components/common/PageBreadCrumb", () => {
  return {
    default: function MockBreadCrumb() {
      return <div data-testid="breadcrumb" />;
    },
  };
});

vi.mock("../../../utils/Utils", async () => {
  const actual = await vi.importActual("../../../utils/Utils");
  return {
    ...actual,
    getFromOpenElisServer: vi.fn(),
    postToOpenElisServerFullResponse: vi.fn(),
    putToOpenElisServerFullResponse: vi.fn(),
  };
});

// Replaced inline utils require

const renderWithIntl = (
  component,
  { permissions = ["qa.view.eqa", "qa.eqa.provider"], roles = [] } = {},
) => {
  return render(
    <IntlProvider locale="en" messages={messages}>
      <UserSessionDetailsContext.Provider
        value={{
          userSessionDetails: { authenticated: true, roles, permissions },
          errorLoadingSessionDetails: false,
          isCheckingLogin: () => false,
          logout: vi.fn(),
        }}
      >
        {component}
      </UserSessionDetailsContext.Provider>
    </IntlProvider>,
  );
};

const mockPrograms = [
  {
    id: 1,
    name: "Chemistry PT",
    description: "Chemistry proficiency testing",
    provider: "CAP",
    isActive: true,
  },
  {
    id: 2,
    name: "Hematology PT",
    description: "Hematology proficiency testing",
    provider: "UKNEQAS",
    isActive: false,
  },
];

describe("ProgramManagement", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    getFromOpenElisServer.mockImplementation((url, callback) => {
      callback(mockPrograms);
    });
  });

  test("renders title", () => {
    renderWithIntl(<ProgramManagement />);
    expect(screen.getByText("Program Administration")).toBeTruthy();
  });

  test("renders add program button", () => {
    renderWithIntl(<ProgramManagement />);
    expect(screen.getByText("Add Program")).toBeTruthy();
  });

  test("hides create and edit controls from a reader without the provider grant", () => {
    // Program CRUD is provider-lane, so a bench reader sees the list but no
    // controls that would answer 403.
    renderWithIntl(<ProgramManagement />, { permissions: ["qa.view.eqa"] });
    expect(screen.queryByText("Add Program")).toBeNull();
    // byRole, not byLabelText: Carbon icon-only buttons name themselves through
    // aria-labelledby, which queryByLabelText does not resolve — the assertion
    // would pass with the gate removed.
    expect(
      screen.queryAllByRole("button", { name: /edit program/i }),
    ).toHaveLength(0);
    expect(screen.getAllByText("Chemistry PT").length).toBeGreaterThan(0);
  });

  test("renders program list from API", () => {
    renderWithIntl(<ProgramManagement />);
    expect(screen.getAllByText("Chemistry PT").length).toBeGreaterThanOrEqual(
      1,
    );
    expect(screen.getAllByText("Hematology PT").length).toBeGreaterThanOrEqual(
      1,
    );
  });

  test("renders provider column", () => {
    renderWithIntl(<ProgramManagement />);
    expect(screen.getByText("CAP")).toBeTruthy();
    expect(screen.getByText("UKNEQAS")).toBeTruthy();
  });

  test("renders active/inactive status tags", () => {
    const { container } = renderWithIntl(<ProgramManagement />);
    expect(screen.getByText("Active")).toBeTruthy();
    expect(screen.getByText("Inactive")).toBeTruthy();
    const greenTags = container.querySelectorAll(".cds--tag--green");
    expect(greenTags.length).toBeGreaterThanOrEqual(1);
  });

  test("renders summary tiles", () => {
    renderWithIntl(<ProgramManagement />);
    expect(screen.getByText("Active Programs")).toBeTruthy();
    expect(
      screen.getAllByText("Enrolled Participants").length,
    ).toBeGreaterThanOrEqual(1);
    expect(screen.getByText("Total Participants")).toBeTruthy();
  });

  test("renders tabs", () => {
    renderWithIntl(<ProgramManagement />);
    expect(screen.getAllByText("EQA Programs").length).toBeGreaterThanOrEqual(
      1,
    );
    // The participants tab was removed: enrollment administration lives on
    // the standalone /qa/eqa/participants page.
    expect(screen.queryByText("Participants")).toBeNull();
    expect(
      screen.getAllByText("System Settings").length,
    ).toBeGreaterThanOrEqual(1);
  });

  test("shows empty state when no programs", () => {
    getFromOpenElisServer.mockImplementation((url, callback) => {
      callback([]);
    });

    renderWithIntl(<ProgramManagement />);
    expect(screen.getByText("No EQA programs found")).toBeTruthy();
  });

  test("opens create form when button clicked", () => {
    renderWithIntl(<ProgramManagement />);
    fireEvent.click(screen.getByText("Add Program"));
    expect(screen.getByText("Add New EQA Program")).toBeTruthy();
  });
});

describe("ProgramForm", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  const catalog = [
    { id: 55, value: "HIV Viral Load" },
    { id: 56, value: "CD4 count" },
  ];

  const serveCatalog = (assignments = []) =>
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url === "/rest/displayList/ALL_TESTS") callback(catalog);
      else if (url.endsWith("/tests")) callback(assignments);
      else callback([]);
    });

  test("an in-house scheme needs no provider and says so on the way out", () => {
    serveCatalog();
    const { container } = renderWithIntl(
      <ProgramForm program={null} onClose={vi.fn()} />,
    );

    fireEvent.change(container.querySelector("#program-scheme-type"), {
      target: { value: "IN_HOUSE" },
    });
    expect(container.querySelector("#program-provider")).toBeNull();

    fireEvent.change(container.querySelector("#program-name"), {
      target: { value: "In-house blinded PT" },
    });
    fireEvent.click(screen.getByText("Add Program"));

    expect(screen.queryByText("Provider is required")).toBeNull();
    const [, payload] = postToOpenElisServerFullResponse.mock.calls[0];
    expect(JSON.parse(payload)).toMatchObject({
      name: "In-house blinded PT",
      schemeType: "IN_HOUSE",
      provider: "",
    });
  });

  test("an external scheme carries the type the user picked", () => {
    serveCatalog();
    const { container } = renderWithIntl(
      <ProgramForm program={null} onClose={vi.fn()} />,
    );

    fireEvent.change(container.querySelector("#program-name"), {
      target: { value: "Regional serology" },
    });
    fireEvent.change(container.querySelector("#program-scheme-type"), {
      target: { value: "REGIONAL_PT" },
    });
    fireEvent.change(container.querySelector("#program-provider"), {
      target: { value: "CPHL" },
    });
    fireEvent.click(screen.getByText("Add Program"));

    const [, payload] = postToOpenElisServerFullResponse.mock.calls[0];
    expect(JSON.parse(payload).schemeType).toBe("REGIONAL_PT");
  });

  test("adding a test to the scheme writes the new map", async () => {
    serveCatalog([{ id: 1, testId: 55, isActive: true }]);
    putToOpenElisServerFullResponse.mockImplementation((url, body, callback) =>
      callback({ ok: true, json: () => Promise.resolve({}) }),
    );
    const onClose = vi.fn();

    const { container } = renderWithIntl(
      <ProgramForm
        program={{
          id: 7,
          name: "Regional serology",
          provider: "CPHL",
          schemeType: "REGIONAL_PT",
          isActive: true,
        }}
        onClose={onClose}
      />,
    );

    // The prefilled map is loaded from the scheme, so clearing it is a real
    // change the endpoint has to be told about.
    const user = userEvent.setup();
    await user.click(
      container.querySelector("#program-tests .cds--list-box__field"),
    );
    await user.click(screen.getByRole("option", { name: /CD4 count/ }));
    await user.click(screen.getByText("Save Program"));

    await waitFor(() =>
      expect(
        putToOpenElisServerFullResponse.mock.calls.some(
          ([url]) => url === "/rest/eqa/programs/7/tests",
        ),
      ).toBe(true),
    );
    const [, body] = putToOpenElisServerFullResponse.mock.calls.find(
      ([called]) => called === "/rest/eqa/programs/7/tests",
    );
    expect(JSON.parse(body).testIds.sort()).toEqual([55, 56]);
  });

  test("saving a scheme with an unchanged test map writes no test map", async () => {
    serveCatalog([{ id: 1, testId: 55, isActive: true }]);
    putToOpenElisServerFullResponse.mockImplementation((url, body, callback) =>
      callback({ ok: true, json: () => Promise.resolve({ id: 7 }) }),
    );
    const onClose = vi.fn();

    renderWithIntl(
      <ProgramForm
        program={{
          id: 7,
          name: "Regional serology",
          provider: "CPHL",
          schemeType: "REGIONAL_PT",
          isActive: true,
        }}
        onClose={onClose}
      />,
    );

    fireEvent.click(screen.getByText("Save Program"));
    await waitFor(() => expect(onClose).toHaveBeenCalled());

    expect(
      putToOpenElisServerFullResponse.mock.calls.some(
        ([url]) => url === "/rest/eqa/programs/7/tests",
      ),
    ).toBe(false);
  });

  test("renders create mode with correct heading", () => {
    renderWithIntl(<ProgramForm program={null} onClose={vi.fn()} />);
    expect(screen.getByText("Add New EQA Program")).toBeTruthy();
  });

  test("renders edit mode with program data", () => {
    const program = {
      id: 1,
      name: "Chemistry PT",
      description: "Test desc",
      provider: "CAP",
      isActive: true,
    };
    renderWithIntl(<ProgramForm program={program} onClose={vi.fn()} />);
    expect(screen.getByText("Edit EQA Program")).toBeTruthy();
  });

  test("shows validation error when name is empty", () => {
    renderWithIntl(<ProgramForm program={null} onClose={vi.fn()} />);
    fireEvent.click(screen.getByText("Add Program"));
    expect(screen.getByText("Program name is required")).toBeTruthy();
  });

  test("renders provider field", () => {
    renderWithIntl(<ProgramForm program={null} onClose={vi.fn()} />);
    expect(screen.getByText("Provider")).toBeTruthy();
  });

  test("renders toggle only in edit mode", () => {
    const { container: createContainer } = renderWithIntl(
      <ProgramForm program={null} onClose={vi.fn()} />,
    );
    expect(createContainer.querySelector("#program-active")).toBeNull();

    const program = {
      id: 1,
      name: "Test",
      description: "",
      isActive: true,
    };
    const { container: editContainer } = renderWithIntl(
      <ProgramForm program={program} onClose={vi.fn()} />,
    );
    expect(editContainer.querySelector("#program-active")).toBeTruthy();
  });

  test("calls onClose when cancel is clicked", () => {
    const onClose = vi.fn();
    renderWithIntl(<ProgramForm program={null} onClose={onClose} />);
    fireEvent.click(screen.getByText("Cancel"));
    expect(onClose).toHaveBeenCalled();
  });
});
