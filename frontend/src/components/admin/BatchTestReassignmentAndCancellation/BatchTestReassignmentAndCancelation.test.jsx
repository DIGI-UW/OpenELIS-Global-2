import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import { vi } from "vitest";
import messages from "../../../languages/en.json";
import BatchTestReassignmentAndCancelation from "./BatchTestReassignmentAndCancelation";

const api = vi.hoisted(() => ({
  get: vi.fn(),
  post: vi.fn(),
}));

vi.mock("../../utils/Utils", () => ({
  getFromOpenElisServer: api.get,
  postToOpenElisServerJsonResponse: api.post,
}));

// vi.mock is hoisted so we can't use React.createContext inside the factory directly
vi.mock("../../layout/Layout", async () => {
  const { createContext } = await import("react");
  return {
    NotificationContext: createContext({
      addNotification: () => {},
      notificationVisible: false,
      setNotificationVisible: () => {},
    }),
  };
});

const batchTestReassignment = {
  formName: "batchTestReassignmentForm",
  formMethod: "POST",
  cancelAction: "",
  submitOnCancel: false,
  cancelMethod: "",
  sampleList: [
    { id: "2", value: "Serum" },
    { id: "5", value: "Urines" },
  ],
  statusChangedSampleType: "",
  statusChangedCurrentTest: "",
  statusChangedNextTest: "",
  jsonWad: "",
};

const providerCalls = () =>
  api.get.mock.calls
    .map(([url]) => url)
    .filter(
      (url) =>
        url.includes("AllTestsForSampleTypeProvider") ||
        url.includes("getPendingAnalysisForTestProvider"),
    );

const renderPage = () =>
  render(
    <MemoryRouter>
      <IntlProvider locale="en" messages={messages}>
        <BatchTestReassignmentAndCancelation />
      </IntlProvider>
    </MemoryRouter>,
  );

const sampleTypeSelect = () => document.getElementById("selectSampleType");
const currentTestSelect = () => document.getElementById("selectSampleType1");

// The tests of a sample type feed both the Current test and the Replace with
// lists, so the name shows up more than once.
const currentTestOffers = async (name) => {
  const options = await screen.findAllByText(name);
  return options.some((option) => currentTestSelect().contains(option));
};

/**
 * OGC-1187 — opening the page fired both provider endpoints with the literal
 * string "null" as the id (the initial state interpolated into the URL) and
 * both answered 500 on every visit.
 */
describe("BatchTestReassignmentAndCancelation provider calls", () => {
  beforeEach(() => {
    api.get.mockReset();
    api.post.mockReset();
    api.get.mockImplementation((url, callback) => {
      if (url === "/rest/BatchTestReassignment") {
        callback(batchTestReassignment);
      } else if (url.startsWith("/rest/AllTestsForSampleTypeProvider")) {
        callback({ tests: [{ id: "17", name: "Amylase", isActive: "Y" }] });
      } else if (url.startsWith("/rest/getPendingAnalysisForTestProvider")) {
        callback({
          notStarted: [],
          technicianRejection: [],
          biologistRejection: [],
          notValidated: [],
        });
      } else {
        callback({});
      }
    });
  });

  it("asks for nothing by sample type or test until an id has been chosen", async () => {
    renderPage();

    await waitFor(() =>
      expect(api.get).toHaveBeenCalledWith(
        "/rest/BatchTestReassignment",
        expect.any(Function),
      ),
    );
    expect(await screen.findByText("Serum")).toBeInTheDocument();

    expect(providerCalls()).toEqual([]);
  });

  it("asks for a sample type's tests with the chosen id, never with null", async () => {
    renderPage();
    await screen.findByText("Serum");

    fireEvent.change(sampleTypeSelect(), { target: { value: "2" } });

    await waitFor(() =>
      expect(api.get).toHaveBeenCalledWith(
        "/rest/AllTestsForSampleTypeProvider?sampleTypeId=2",
        expect.any(Function),
      ),
    );
    expect(await currentTestOffers("Amylase")).toBe(true);
    expect(providerCalls().filter((url) => url.includes("null"))).toEqual([]);
    expect(
      providerCalls().filter((url) =>
        url.includes("getPendingAnalysisForTestProvider"),
      ),
    ).toEqual([]);
  });

  it("asks for a test's pending analyses only once a test is chosen", async () => {
    renderPage();
    await screen.findByText("Serum");
    fireEvent.change(sampleTypeSelect(), { target: { value: "2" } });
    expect(await currentTestOffers("Amylase")).toBe(true);

    fireEvent.change(currentTestSelect(), { target: { value: "17" } });

    await waitFor(() =>
      expect(api.get).toHaveBeenCalledWith(
        "/rest/getPendingAnalysisForTestProvider?testId=17",
        expect.any(Function),
      ),
    );
    expect(providerCalls().filter((url) => url.includes("null"))).toEqual([]);
  });
});
