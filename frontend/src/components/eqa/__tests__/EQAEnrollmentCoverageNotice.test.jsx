import React from "react";
import { render, screen } from "@testing-library/react";
import { IntlProvider } from "react-intl";
import messages from "../../../languages/en.json";
import EQAEnrollmentCoverageNotice, {
  testsOutsideEnrollment,
} from "../EQAEnrollmentCoverageNotice";
import { getFromOpenElisServer } from "../../utils/Utils";

vi.mock("../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
}));

// One sample type carrying the glucose test on its own, plus a panel that
// brings creatinine with it.
const samples = [
  {
    tests: [
      { id: "100", name: "Glucose" },
      { id: "101", name: "Creatinine" },
    ],
    panels: [{ id: "200", name: "Basic Metabolic Panel", testIds: "101" }],
  },
];

const enrollment = (tests, panels) => ({
  id: 7,
  tests: tests.map((id) => ({ id })),
  panels: panels.map((id) => ({ id })),
});

const renderNotice = (enrollmentId = 7) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <EQAEnrollmentCoverageNotice
        enrollmentId={enrollmentId}
        samples={samples}
      />
    </IntlProvider>,
  );

describe("testsOutsideEnrollment", () => {
  test("names the tests and panels the enrolment does not cover", () => {
    expect(testsOutsideEnrollment(samples, enrollment([], []))).toEqual([
      "Basic Metabolic Panel",
      "Glucose",
      "Creatinine",
    ]);
  });

  test("a test a covered panel brings with it is covered too", () => {
    // The enrolment names the panel but not creatinine, and the panel is what
    // put creatinine on the order.
    expect(
      testsOutsideEnrollment(samples, enrollment(["100"], ["200"])),
    ).toEqual([]);
  });

  test("the same selection is flagged once the panel is not covered", () => {
    // The inverse of the case above: drop the panel from the enrolment and both
    // it and the test it carries have to be named.
    expect(testsOutsideEnrollment(samples, enrollment(["100"], []))).toEqual([
      "Basic Metabolic Panel",
      "Creatinine",
    ]);
  });
});

describe("EQAEnrollmentCoverageNotice", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  test("warns about the test outside the enrolment without refusing the order", () => {
    getFromOpenElisServer.mockImplementation((url, callback) => {
      expect(url).toBe("/rest/eqa/my-programs/7");
      callback(enrollment(["100"], []));
    });

    renderNotice();

    expect(
      screen.getByText("Outside this laboratory's enrolment"),
    ).toBeTruthy();
    expect(
      screen.getByText(
        "The enrolment for this programme does not cover: Basic Metabolic Panel, Creatinine. You can still save the order.",
      ),
    ).toBeTruthy();
  });

  test("says nothing when the enrolment covers everything picked", () => {
    getFromOpenElisServer.mockImplementation((url, callback) =>
      callback(enrollment(["100", "101"], ["200"])),
    );

    renderNotice();

    expect(getFromOpenElisServer).toHaveBeenCalled();
    expect(
      screen.queryByText("Outside this laboratory's enrolment"),
    ).toBeNull();
  });

  test("asks for nothing until a programme is chosen", () => {
    renderNotice("");

    expect(getFromOpenElisServer).not.toHaveBeenCalled();
    expect(
      screen.queryByText("Outside this laboratory's enrolment"),
    ).toBeNull();
  });
});
