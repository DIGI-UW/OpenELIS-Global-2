import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import messages from "../../languages/en.json";
import BatchWorkplan from "./BatchWorkplan";
import {
  getFromOpenElisServer,
  postToOpenElisServerFullResponse,
  putToOpenElisServerFullResponse,
} from "../utils/Utils";

vi.mock("../utils/Utils", () => ({
  convertAlphaNumLabNumForDisplay: vi.fn((value) => value),
  getFromOpenElisServer: vi.fn(),
  postToOpenElisServerFullResponse: vi.fn(),
  putToOpenElisServerFullResponse: vi.fn(),
}));

const renderWithIntl = () =>
  render(
    <MemoryRouter>
      <IntlProvider locale="en" messages={messages}>
        <BatchWorkplan />
      </IntlProvider>
    </MemoryRouter>,
  );

const pending = [
  {
    analysisId: "11",
    accessionNumber: "DEV-001",
    testName: "Hemoglobin",
    testSectionName: "Hematology",
    methodName: "Manual",
    sampleType: "Whole blood",
    receivedDate: "2026-06-28",
    statusName: "Not started",
    nonconforming: false,
  },
];

const batches = [
  {
    id: 7,
    name: "Morning chemistry",
    status: "DRAFT",
    itemCount: 2,
    createdAt: "2026-06-28T10:00:00Z",
  },
];

beforeEach(() => {
  getFromOpenElisServer.mockReset();
  postToOpenElisServerFullResponse.mockReset();
  putToOpenElisServerFullResponse.mockReset();
  getFromOpenElisServer.mockImplementation((url, callback) => {
    if (url.includes("pending-tests")) {
      callback(pending);
      return;
    }
    if (url.includes("batches")) {
      callback(batches);
      return;
    }
    callback([]);
  });
});

describe("BatchWorkplan", () => {
  it("renders pending tests from the batch workplan API", () => {
    renderWithIntl();

    expect(screen.getAllByText("Batch Workplan").length).toBeGreaterThan(0);
    expect(screen.getByText("DEV-001")).toBeInTheDocument();
    expect(screen.getByText("Hemoglobin")).toBeInTheDocument();
    expect(screen.getByText("Hematology")).toBeInTheDocument();
  });

  it("posts selected analyses when creating a batch", async () => {
    postToOpenElisServerFullResponse.mockImplementation(
      (_url, _body, callback) => callback({ ok: true }),
    );

    renderWithIntl();

    fireEvent.click(screen.getByLabelText("Select DEV-001"));
    fireEvent.click(
      screen.getByRole("button", { name: /create batch \(1\)/i }),
    );

    expect(postToOpenElisServerFullResponse).toHaveBeenCalledWith(
      "/rest/batch-workplans/batches",
      JSON.stringify({ analysisIds: ["11"] }),
      expect.any(Function),
    );
  });
});
