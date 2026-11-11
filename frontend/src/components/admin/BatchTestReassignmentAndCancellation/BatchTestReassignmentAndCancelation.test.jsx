import React from "react";
import { render, screen, fireEvent, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { waitFor } from "@testing-library/dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import { vi } from "vitest";
import messages from "../../../languages/en.json";
import BatchTestReassignmentAndCancelation from "./BatchTestReassignmentAndCancelation";

const api = vi.hoisted(() => ({
  get: vi.fn(),
  post: vi.fn(),
  notify: vi.fn(),
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
      addNotification: (notification) => api.notify(notification),
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

const sampleTypeBox = () =>
  screen.getByRole("combobox", { name: "Sample Type" });
const currentTestBox = () =>
  screen.getByRole("combobox", { name: "Current test" });
const replaceWithBox = () =>
  screen.getByRole("combobox", { name: "Replace with" });

const listedIn = async (box) => {
  await userEvent.click(box);
  const names = within(
    document.getElementById(box.getAttribute("aria-controls")),
  )
    .queryAllByRole("option")
    .map((option) => option.textContent);
  await userEvent.keyboard("{Escape}");
  return names;
};

const choose = async (box, typed, option) => {
  await userEvent.type(box, typed);
  await userEvent.click(screen.getByRole("option", { name: option }));
};

const chooseSerum = async () => {
  await screen.findByRole("combobox", { name: "Sample Type" });
  await choose(sampleTypeBox(), "ser", "Serum");
};

const currentTestOffers = async (name) => {
  await waitFor(() => expect(currentTestBox()).toBeInTheDocument());
  return (await listedIn(currentTestBox())).includes(name);
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
    expect(
      await listedIn(
        await screen.findByRole("combobox", { name: "Sample Type" }),
      ),
    ).toEqual(["Serum", "Urines"]);

    expect(providerCalls()).toEqual([]);
  });

  it("asks for a sample type's tests with the chosen id, never with null", async () => {
    renderPage();

    await chooseSerum();

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
    await chooseSerum();
    expect(await currentTestOffers("Amylase")).toBe(true);

    await choose(currentTestBox(), "amy", "Amylase");

    await waitFor(() =>
      expect(api.get).toHaveBeenCalledWith(
        "/rest/getPendingAnalysisForTestProvider?testId=17",
        expect.any(Function),
      ),
    );
    expect(providerCalls().filter((url) => url.includes("null"))).toEqual([]);
  });

  /**
   * The save answers after the click, outside React's batching, so the reset
   * rendered once per state change: the pending analyses were already gone while
   * the confirmation summary still listed them, and the page crashed on a saved
   * cancellation.
   */
  it("returns to an empty form once a cancellation is saved", async () => {
    api.get.mockImplementation((url, callback) => {
      if (url === "/rest/BatchTestReassignment") {
        callback(batchTestReassignment);
      } else if (url.startsWith("/rest/AllTestsForSampleTypeProvider")) {
        callback({ tests: [{ id: "17", name: "Amylase", isActive: "Y" }] });
      } else if (url.startsWith("/rest/getPendingAnalysisForTestProvider")) {
        callback({
          notStarted: [{ id: "901", labNo: "DEV01260000000000002" }],
          technicianRejection: [],
          biologistRejection: [],
          notValidated: [],
        });
      } else {
        callback({});
      }
    });
    api.post.mockImplementation((url, body, callback) => {
      setTimeout(() => callback({}), 0);
    });
    renderPage();
    await chooseSerum();
    expect(await currentTestOffers("Amylase")).toBe(true);
    await choose(currentTestBox(), "amy", "Amylase");
    fireEvent.click(await screen.findByLabelText("DEV01260000000000002"));
    fireEvent.click(screen.getByRole("button", { name: "Ok" }));

    fireEvent.click(await screen.findByRole("button", { name: "Accept" }));

    await waitFor(() => expect(api.post).toHaveBeenCalled());
    await waitFor(() =>
      expect(screen.queryByRole("button", { name: "Accept" })).toBeNull(),
    );
    expect(sampleTypeBox()).toHaveValue("");
    expect(await listedIn(sampleTypeBox())).toEqual(["Serum", "Urines"]);
    expect(api.notify).toHaveBeenCalledWith(
      expect.objectContaining({ message: "Successfully saved" }),
    );
  });
});

/**
 * The sample type, current test and replacement lists are searched by typing
 * (OGC-1515); "Include inactive tests" decides whether inactive tests are
 * offered as the current test, and only active tests are offered as
 * replacements.
 */
describe("BatchTestReassignmentAndCancelation searchable lists", () => {
  beforeEach(() => {
    api.get.mockReset();
    api.post.mockReset();
    api.get.mockImplementation((url, callback) => {
      if (url === "/rest/BatchTestReassignment") {
        callback(batchTestReassignment);
      } else if (url.startsWith("/rest/AllTestsForSampleTypeProvider")) {
        callback({
          tests: [
            { id: "17", name: "Amylase", isActive: "Y" },
            { id: "18", name: "Créatinine", isActive: "Y" },
            { id: "19", name: "Old Amylase", isActive: "N" },
          ],
        });
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

  it("narrows the sample types to what is typed", async () => {
    renderPage();
    const box = await screen.findByRole("combobox", { name: "Sample Type" });

    await userEvent.type(box, "URI");

    expect(
      screen.getAllByRole("option").map((option) => option.textContent),
    ).toEqual(["Urines"]);
  });

  it("offers inactive tests as the current test only while Include inactive tests is ticked", async () => {
    renderPage();
    await chooseSerum();
    await waitFor(() =>
      expect(api.get).toHaveBeenCalledWith(
        "/rest/AllTestsForSampleTypeProvider?sampleTypeId=2",
        expect.any(Function),
      ),
    );

    expect(await listedIn(currentTestBox())).toEqual([
      "Amylase",
      "Créatinine",
      "Old Amylase",
    ]);
    await userEvent.click(screen.getByLabelText("Include inactive tests"));

    expect(await listedIn(currentTestBox())).toEqual(["Amylase", "Créatinine"]);
  });

  it("adds each chosen replacement as a tag, offers only active tests not yet chosen, and empties the box", async () => {
    renderPage();
    await chooseSerum();
    await userEvent.click(
      screen.getByLabelText(
        "Cancel test for lab numbers selected, do not assign a new test",
      ),
    );

    await choose(replaceWithBox(), "creat", "Créatinine");

    expect(
      screen.getByText("Créatinine", { selector: ".cds--tag *, .cds--tag" }),
    ).toBeInTheDocument();
    expect(replaceWithBox()).toHaveValue("");
    expect(replaceWithBox()).toHaveFocus();
    expect(await listedIn(replaceWithBox())).toEqual(["Amylase"]);
  });

  it("forgets the current test and the replacements when another sample type is chosen", async () => {
    renderPage();
    await chooseSerum();
    await choose(currentTestBox(), "amy", "Amylase");
    expect(currentTestBox()).toHaveValue("Amylase");

    await userEvent.clear(sampleTypeBox());
    await choose(sampleTypeBox(), "uri", "Urines");

    await waitFor(() =>
      expect(api.get).toHaveBeenCalledWith(
        "/rest/AllTestsForSampleTypeProvider?sampleTypeId=5",
        expect.any(Function),
      ),
    );
    expect(currentTestBox()).toHaveValue("");
  });
});

/**
 * Ticking "do not assign a new test" again sent the chosen replacements as a
 * comma-joined string, which the server refuses; the refusal answered 200 and
 * the page said "Successfully saved" with nothing cancelled. The confirmation
 * also said "cancel" while replacing and never named the new test.
 */
describe("BatchTestReassignmentAndCancelation saving", () => {
  const doNotReplace = () =>
    screen.getByLabelText(
      "Cancel test for lab numbers selected, do not assign a new test",
    );

  const sentWad = () =>
    JSON.parse(JSON.parse(api.post.mock.calls[0][1]).jsonWad);

  beforeEach(() => {
    api.get.mockReset();
    api.post.mockReset();
    api.notify.mockReset();
    api.get.mockImplementation((url, callback) => {
      if (url === "/rest/BatchTestReassignment") {
        callback(batchTestReassignment);
      } else if (url.startsWith("/rest/AllTestsForSampleTypeProvider")) {
        callback({
          tests: [
            { id: "17", name: "Amylase", isActive: "Y" },
            { id: "18", name: "Créatinine", isActive: "Y" },
          ],
        });
      } else if (url.startsWith("/rest/getPendingAnalysisForTestProvider")) {
        callback({
          notStarted: [{ id: "901", labNo: "DEV01260000000000002" }],
          technicianRejection: [],
          biologistRejection: [],
          notValidated: [],
        });
      } else {
        callback({});
      }
    });
  });

  const chooseAmylaseOnOneSample = async () => {
    await chooseSerum();
    await choose(currentTestBox(), "amy", "Amylase");
    fireEvent.click(await screen.findByLabelText("DEV01260000000000002"));
  };

  it("names the replacement in the confirmation and sends it as a list", async () => {
    api.post.mockImplementation((url, body, callback) => callback({}));
    renderPage();
    await chooseAmylaseOnOneSample();
    await userEvent.click(doNotReplace());
    await choose(replaceWithBox(), "creat", "Créatinine");

    fireEvent.click(screen.getByRole("button", { name: "Ok" }));

    expect(
      await screen.findByText(
        /You have selected to replace the test Amylase with Créatinine\./,
      ),
    ).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Accept" }));
    await waitFor(() => expect(api.post).toHaveBeenCalled());
    expect(sentWad().replace).toEqual(["18"]);
  });

  it("drops the chosen replacements when do not assign a new test is ticked again", async () => {
    api.post.mockImplementation((url, body, callback) => callback({}));
    renderPage();
    await chooseAmylaseOnOneSample();
    await userEvent.click(doNotReplace());
    await choose(replaceWithBox(), "creat", "Créatinine");

    await userEvent.click(doNotReplace());
    fireEvent.click(screen.getByRole("button", { name: "Ok" }));

    expect(
      await screen.findByText(/You have selected to cancel the test Amylase/),
    ).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Accept" }));
    await waitFor(() => expect(api.post).toHaveBeenCalled());
    expect(sentWad().replace).toEqual([]);
  });

  it("says the save failed when the server refuses it", async () => {
    api.post.mockImplementation((url, body, callback) =>
      callback({ error: "validation", status: 400, fieldErrors: [] }),
    );
    renderPage();
    await chooseAmylaseOnOneSample();
    fireEvent.click(screen.getByRole("button", { name: "Ok" }));

    fireEvent.click(await screen.findByRole("button", { name: "Accept" }));

    await waitFor(() =>
      expect(api.notify).toHaveBeenCalledWith(
        expect.objectContaining({ message: "Error While saving" }),
      ),
    );
    expect(api.notify).not.toHaveBeenCalledWith(
      expect.objectContaining({ message: "Successfully saved" }),
    );
    expect(screen.getByRole("button", { name: "Accept" })).toBeInTheDocument();
    expect(currentTestBox()).toHaveValue("Amylase");
  });
});
