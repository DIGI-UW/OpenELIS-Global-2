import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
// This project's @testing-library/react is v9, which predates waitFor; the dom
// package is where the rest of the suite takes it from.
import { waitFor } from "@testing-library/dom";
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
    getFromOpenElisServer.mockImplementation((url, callback) => callback([]));
  });

  test("an in-house scheme needs no provider and says so on the way out", () => {
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

describe("ProgramForm test assignments", () => {
  const catalog = [
    { id: 41, value: "CD4 count" },
    { id: 57, value: "Determine(Serum)" },
    { id: 45, value: "Genie III(Serum)" },
  ];

  // Answers each read by URL, so the form's two fetches cannot be confused for
  // one another.
  const serveCatalogAnd = (assignments) =>
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url === "/rest/displayList/ALL_TESTS") return callback(catalog);
      if (/\/rest\/eqa\/programs\/\d+\/tests$/.test(url))
        return callback(assignments);
      return callback([]);
    });

  const ok = (body) => ({ ok: true, json: () => Promise.resolve(body) });

  beforeEach(() => {
    vi.clearAllMocks();
    putToOpenElisServerFullResponse.mockImplementation((url, body, cb) =>
      cb(ok({ id: 3 })),
    );
    postToOpenElisServerFullResponse.mockImplementation((url, body, cb) =>
      cb(ok({ id: 7 })),
    );
  });

  const editableProgram = {
    id: 3,
    name: "HIV Serology",
    provider: "CPHL",
    description: "",
    isActive: true,
  };

  test("offers the catalog when editing a program", async () => {
    serveCatalogAnd([]);
    const { container } = renderWithIntl(
      <ProgramForm program={editableProgram} onClose={vi.fn()} />,
    );
    await waitFor(() =>
      expect(container.querySelector("#program-tests")).toBeTruthy(),
    );
    expect(screen.getByText("Assigned Tests")).toBeTruthy();
  });

  test("saving an untouched form leaves the existing assignments alone", async () => {
    // The write is a delete-and-recreate, so a rename must not reach it. If the
    // assigned tests failed to preselect, the guard would see an empty selection
    // and clear the program's tests here.
    serveCatalogAnd([
      { id: 11, testId: 57, isActive: true },
      { id: 12, testId: 45, isActive: true },
    ]);
    const onClose = vi.fn();
    const { container } = renderWithIntl(
      <ProgramForm program={editableProgram} onClose={onClose} />,
    );
    await waitFor(() =>
      expect(container.querySelector("#program-tests")).toBeTruthy(),
    );

    fireEvent.click(screen.getByText("Save Program"));

    await waitFor(() => expect(onClose).toHaveBeenCalled());
    const urls = putToOpenElisServerFullResponse.mock.calls.map((c) => c[0]);
    expect(urls).toEqual(["/rest/eqa/programs/3"]);
  });

  test("a new program's tests are written against the id the server assigned", async () => {
    serveCatalogAnd([]);
    const onClose = vi.fn();
    const { container } = renderWithIntl(
      <ProgramForm program={null} onClose={onClose} />,
    );
    await waitFor(() =>
      expect(container.querySelector("#program-tests")).toBeTruthy(),
    );

    fireEvent.change(screen.getByLabelText("Program Name"), {
      target: { value: "HIV Viral Load" },
    });
    fireEvent.change(screen.getByLabelText("Provider"), {
      target: { value: "CPHL" },
    });
    // The menu opens off the combobox input, not the wrapper the id sits on.
    fireEvent.click(screen.getByPlaceholderText("Select tests to assign"));
    fireEvent.click(await screen.findByText("Determine(Serum)"));
    fireEvent.click(screen.getByText("Add Program"));

    await waitFor(() => expect(onClose).toHaveBeenCalled());
    expect(putToOpenElisServerFullResponse).toHaveBeenCalledWith(
      "/rest/eqa/programs/7/tests",
      JSON.stringify({ testIds: [57] }),
      expect.any(Function),
    );
  });
});
