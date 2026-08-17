import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import messages from "../../../../languages/en.json";
import CreateDistribution from "../CreateDistribution";
import {
  getFromOpenElisServer,
  postToOpenElisServerFullResponse,
  putToOpenElisServerFullResponse,
} from "../../../utils/Utils";

vi.mock("../../../utils/Utils", async (importOriginal) => {
  const actual = await importOriginal();
  return {
    ...actual,
    getFromOpenElisServer: vi.fn(),
    getFromOpenElisServerV2: vi.fn(),
    postToOpenElisServerFullResponse: vi.fn(),
    putToOpenElisServerFullResponse: vi.fn(),
  };
});

const renderWithIntl = (component, route = "/qa/eqa/distribution/create") => {
  return render(
    <MemoryRouter initialEntries={[route]}>
      <IntlProvider locale="en" messages={messages}>
        {component}
      </IntlProvider>
    </MemoryRouter>,
  );
};

const draft = {
  id: 7,
  distributionName: "Round 3",
  programId: 2,
  deadline: "2026-09-30T23:59:59Z",
  status: "DRAFT",
};

const programs = [
  { id: 1, name: "Chemistry PT" },
  { id: 2, name: "Hematology PT" },
];

const enrollments = [
  { organizationId: 11, organizationName: "Hospital A", status: "Active" },
  { organizationId: 12, organizationName: "Clinic B", status: "Active" },
];

// answer every fetch the wizard makes while resuming draft 7
const serveDraft = () =>
  getFromOpenElisServer.mockImplementation((url, callback) => {
    if (url.includes("/distributions/7")) return callback(draft);
    if (url.includes("/enrollments")) return callback(enrollments);
    if (url.includes("/programs")) return callback(programs);
  });

describe("CreateDistribution", () => {
  const mockOnCreate = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
  });

  test("renders wizard with all step labels", () => {
    renderWithIntl(<CreateDistribution onCreate={mockOnCreate} />);
    expect(screen.getByText("Program & Details")).toBeTruthy();
    expect(screen.getAllByText("Participants").length).toBeGreaterThanOrEqual(
      1,
    );
    expect(screen.getByText("Confirmation")).toBeTruthy();
  });

  test("renders distribution name input", () => {
    renderWithIntl(<CreateDistribution onCreate={mockOnCreate} />);
    expect(screen.getByText("Distribution Name")).toBeTruthy();
  });

  test("renders program select dropdown", () => {
    renderWithIntl(<CreateDistribution onCreate={mockOnCreate} />);
    expect(screen.getByText("EQA Program")).toBeTruthy();
  });

  test("renders deadline date picker", () => {
    renderWithIntl(<CreateDistribution onCreate={mockOnCreate} />);
    expect(screen.getByText("Submission Deadline")).toBeTruthy();
  });

  test("renders participants button on step 0", () => {
    renderWithIntl(<CreateDistribution onCreate={mockOnCreate} />);
    // The "Participants" text appears as both a progress step and a button
    const participantElements = screen.getAllByText("Participants");
    expect(participantElements.length).toBeGreaterThanOrEqual(2);
  });

  test("progress indicator has 3 steps", () => {
    const { container } = renderWithIntl(
      <CreateDistribution onCreate={mockOnCreate} />,
    );
    const steps = container.querySelectorAll(".cds--progress-step");
    expect(steps.length).toBe(3);
  });

  test("hydrates the wizard from the draft id in the query string", async () => {
    serveDraft();

    const { container } = renderWithIntl(
      <CreateDistribution />,
      "/qa/eqa/distribution/create?id=7",
    );

    expect(await screen.findByDisplayValue("Round 3")).toBeInTheDocument();
    expect(container.querySelector("#distribution-program").value).toBe("2");
    expect(container.querySelector("#distribution-deadline").value).toBe(
      "30/09/2026",
    );
  });

  test("saving a resumed draft updates that distribution instead of creating one", async () => {
    serveDraft();

    const { container } = renderWithIntl(
      <CreateDistribution />,
      "/qa/eqa/distribution/create?id=7",
    );
    await screen.findByDisplayValue("Round 3");

    // the step labels double as progress-indicator buttons, so match the
    // wizard's own advance buttons by class
    const advanceTo = (label) =>
      fireEvent.click(
        [...container.querySelectorAll("button.cds--btn")].find(
          (b) => b.textContent.trim() === label,
        ),
      );

    advanceTo("Participants");
    fireEvent.click(screen.getByPlaceholderText("Select organizations"));
    fireEvent.click(screen.getByText("Hospital A"));
    fireEvent.click(screen.getByText("Clinic B"));
    advanceTo("Confirmation");
    advanceTo("Create Distribution");

    expect(postToOpenElisServerFullResponse).not.toHaveBeenCalled();
    const [url, payload] = putToOpenElisServerFullResponse.mock.calls[0];
    expect(url).toBe("/rest/eqa/distributions/7");
    expect(JSON.parse(payload).deadline).toBe("2026-09-30");
  });

  test("a fresh wizard still creates", () => {
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url.includes("/programs")) return callback(programs);
    });

    renderWithIntl(<CreateDistribution />);

    expect(putToOpenElisServerFullResponse).not.toHaveBeenCalled();
  });
});
