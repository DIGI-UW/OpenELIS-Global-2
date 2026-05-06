import React from "react";
import { render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../../languages/en.json";
import CreateDistribution from "../CreateDistribution";

vi.mock("../../../utils/Utils", async (importOriginal) => {
  const actual = await importOriginal();
  return {
    ...actual,
    getFromOpenElisServer: vi.fn(),
    getFromOpenElisServerV2: vi.fn(),
    postToOpenElisServerJsonResponse: vi.fn(),
  };
});

const renderWithIntl = (component) => {
  return render(
    <IntlProvider locale="en" messages={messages}>
      {component}
    </IntlProvider>,
  );
};

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
});
