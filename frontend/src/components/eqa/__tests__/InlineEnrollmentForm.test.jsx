import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../languages/en.json";
import InlineEnrollmentForm from "../InlineEnrollmentForm";
import { getFromOpenElisServer } from "../../utils/Utils";

vi.mock("../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
}));

const SCHEMES = [
  { id: 5, name: "Chemistry PT", provider: "WHO" },
  { id: 6, name: "Virology PT", provider: "NHLS" },
];

const ENROLLED = [{ id: 1, programName: "Chemistry PT", provider: "WHO" }];

const mockServer = (cycles = []) =>
  getFromOpenElisServer.mockImplementation((url, callback) => {
    if (url === "/rest/eqa/programs") callback(SCHEMES);
    else if (url === "/rest/eqa/cycles/mine") callback(cycles);
    else if (url === "/rest/displayList/TEST_SECTION_ACTIVE")
      callback([{ id: 10, value: "Chemistry" }]);
    else if (url === "/rest/displayList/ALL_TESTS")
      callback([{ id: 100, value: "Glucose" }]);
    else if (url === "/rest/displayList/PANELS")
      callback([{ id: 200, value: "Basic Metabolic Panel" }]);
    else if (url === "/rest/eqa/my-programs/analytes")
      callback([{ id: 900, value: "Glucose (serum)" }]);
    else callback([]);
  });

const renderForm = (props) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <InlineEnrollmentForm onSave={vi.fn()} onCancel={vi.fn()} {...props} />
    </IntlProvider>,
  );

describe("InlineEnrollmentForm", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  test("the scheme picker offers the loaded schemes and leaves out the enrolled one", () => {
    mockServer();
    renderForm({ enrollment: null, enrollments: ENROLLED });

    const options = Array.from(
      screen.getByLabelText("Scheme").querySelectorAll("option"),
    ).map((option) => option.textContent);

    expect(options).toEqual(["Virology PT", "Not listed - type the name"]);
  });

  test("picking a scheme fills the programme name and its provider", () => {
    mockServer();
    const onSave = vi.fn();
    renderForm({ enrollment: null, enrollments: ENROLLED, onSave });

    fireEvent.change(screen.getByLabelText("Scheme"), {
      target: { value: "Virology PT" },
    });

    expect(screen.getByLabelText("Scheme Name")).toHaveValue("Virology PT");
    expect(screen.getByLabelText("Provider")).toHaveValue("NHLS");

    fireEvent.click(screen.getByRole("button", { name: "Save Enrollment" }));
    expect(onSave).toHaveBeenCalledTimes(1);
    expect(onSave.mock.calls[0][0]).toMatchObject({
      programName: "Virology PT",
      provider: "NHLS",
    });
  });

  test("free text stays available for a provider this instance does not carry", () => {
    mockServer();
    const onSave = vi.fn();
    renderForm({ enrollment: null, enrollments: [], onSave });

    fireEvent.change(screen.getByLabelText("Scheme Name"), {
      target: { value: "Regional TB PT" },
    });
    fireEvent.change(screen.getByLabelText("Provider"), {
      target: { value: "SANAS" },
    });

    fireEvent.click(screen.getByRole("button", { name: "Save Enrollment" }));
    expect(onSave.mock.calls[0][0]).toMatchObject({
      programName: "Regional TB PT",
      provider: "SANAS",
    });
  });

  test("an enrollment with cycles against it cannot be renamed", () => {
    // Cycles are matched to an enrollment by programme name, so a rename would
    // silently detach this enrollment from the cycle below.
    mockServer([{ id: 9, schemeName: "Chemistry PT" }]);
    renderForm({
      enrollment: {
        id: 1,
        programName: "Chemistry PT",
        provider: "WHO",
        isActive: true,
      },
      enrollments: ENROLLED,
    });

    expect(screen.getByLabelText("Scheme")).toBeDisabled();
    expect(screen.getByLabelText("Scheme Name")).toBeDisabled();
    expect(screen.getByLabelText("Provider")).toBeDisabled();
    expect(
      screen.getByText(
        "This enrollment has cycles against it, so its scheme and provider cannot be changed.",
      ),
    ).toBeInTheDocument();
  });

  test("an enrollment with no cycles is still editable", () => {
    mockServer([{ id: 9, schemeName: "Virology PT" }]);
    renderForm({
      enrollment: {
        id: 1,
        programName: "Chemistry PT",
        provider: "WHO",
        isActive: true,
      },
      enrollments: ENROLLED,
    });

    expect(screen.getByLabelText("Scheme")).toBeEnabled();
    expect(screen.getByLabelText("Scheme Name")).toBeEnabled();
    expect(screen.getByLabelText("Provider")).toBeEnabled();
  });
});
