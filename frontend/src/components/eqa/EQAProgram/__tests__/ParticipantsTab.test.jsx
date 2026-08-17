import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../../languages/en.json";
import ParticipantsTab from "../ParticipantsTab";
import {
  getFromOpenElisServer,
  postToOpenElisServerFullResponse,
} from "../../../utils/Utils";

vi.mock("../../../utils/Utils", async () => {
  const actual = await vi.importActual("../../../utils/Utils");
  return {
    ...actual,
    getFromOpenElisServer: vi.fn(),
    postToOpenElisServerFullResponse: vi.fn(),
    putToOpenElisServerFullResponse: vi.fn(),
  };
});

const programs = [{ id: 1, name: "Chemistry PT", isActive: true }];

const organizations = [
  { id: 100, organizationName: "Hospital A", isActive: "Y" },
];

const renderTab = () =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <ParticipantsTab programs={programs} />
    </IntlProvider>,
  );

const jsonResponse = (ok, status, body) => ({
  ok,
  status,
  json: () => Promise.resolve(body),
});

const selectProgram = (container) =>
  fireEvent.change(container.querySelector("#participant-program-selector"), {
    target: { value: "1" },
  });

// open the enroll modal, pick the only organization and submit
const enrollHospitalA = () => {
  fireEvent.click(screen.getByRole("button", { name: "Enroll Participant" }));
  fireEvent.click(screen.getByPlaceholderText("Search organizations..."));
  fireEvent.click(screen.getByText("Hospital A"));
  fireEvent.click(screen.getByText("Enroll Selected"));
};

const enrollmentFetchCount = () =>
  getFromOpenElisServer.mock.calls.filter(([url]) =>
    url.includes("enrollments"),
  ).length;

describe("ParticipantsTab enrollment", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    getFromOpenElisServer.mockImplementation((url, callback) => {
      callback(url.includes("/enrollments") ? [] : organizations);
    });
  });

  test("sends numeric organization ids", () => {
    postToOpenElisServerFullResponse.mockImplementation(() => {});
    const { container } = renderTab();
    selectProgram(container);

    enrollHospitalA();

    const [, payload] = postToOpenElisServerFullResponse.mock.calls[0];
    expect(JSON.parse(payload)).toEqual({ organizationIds: [100] });
  });

  test("surfaces the server error instead of reporting success", async () => {
    postToOpenElisServerFullResponse.mockImplementation(
      (url, payload, callback) =>
        callback(
          jsonResponse(false, 400, { error: "Organization already enrolled" }),
        ),
    );
    const { container } = renderTab();
    selectProgram(container);
    const fetchesBefore = enrollmentFetchCount();

    enrollHospitalA();

    expect(
      await screen.findByText("Organization already enrolled"),
    ).toBeInTheDocument();
    expect(screen.queryByText("Enrollment successful")).toBeNull();
    expect(enrollmentFetchCount()).toBe(fetchesBefore);
  });

  test("falls back to a generic message when the error body is unreadable", async () => {
    postToOpenElisServerFullResponse.mockImplementation(
      (url, payload, callback) =>
        callback({ ok: false, status: 403, json: () => Promise.reject() }),
    );
    const { container } = renderTab();
    selectProgram(container);

    enrollHospitalA();

    expect(await screen.findByText("Failed to save")).toBeInTheDocument();
  });

  test("reports success and refreshes on 201", async () => {
    postToOpenElisServerFullResponse.mockImplementation(
      (url, payload, callback) =>
        callback(jsonResponse(true, 201, [{ id: 5 }])),
    );
    const { container } = renderTab();
    selectProgram(container);
    const fetchesBefore = enrollmentFetchCount();

    enrollHospitalA();

    expect(
      await screen.findByText("Enrollment successful"),
    ).toBeInTheDocument();
    expect(enrollmentFetchCount()).toBeGreaterThan(fetchesBefore);
  });
});
