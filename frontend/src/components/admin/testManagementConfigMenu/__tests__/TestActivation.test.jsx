import React from "react";
import { render, screen, fireEvent, act } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import userEvent from "@testing-library/user-event";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import { QueryClientProvider } from "@tanstack/react-query";
import { beforeEach, describe, expect, it, vi } from "vitest";
import messages from "../../../../languages/en.json";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../../utils/Utils";
import { createQueryClient } from "../../../utils/queryClient";
import { NotificationContext } from "../../../layout/Layout";
import TestActivation from "../TestActivation";

const notify = vi.fn();

vi.mock("../../../utils/Utils", async () => {
  const actual = await vi.importActual("../../../utils/Utils");
  const getFromOpenElisServer = vi.fn();
  return {
    ...actual,
    getFromOpenElisServer,
    fetchFromOpenElisServer: vi.fn(
      (url) =>
        new Promise((resolve, reject) =>
          getFromOpenElisServer(url, (response) =>
            response === undefined
              ? reject(new Error("read failed"))
              : resolve(response),
          ),
        ),
    ),
    postToOpenElisServerJsonResponse: vi.fn(),
  };
});

// A sample type keeps its section only while it still has an active test, so
// two of them are the smallest fixture that survives one being turned off.
const activation = () => ({
  activeTestList: [
    {
      sampleType: { id: "1", value: "Blood" },
      activeTests: [
        { id: "10", value: "Glucose" },
        { id: "11", value: "Urea" },
      ],
      inactiveTests: [],
    },
  ],
  inactiveTestList: [],
});

const glucose = () => document.getElementById("1-10");

// The screen offers the same Submit and Cancel above the active tests and
// below the inactive ones, both wired to the same handler.
const submitButtons = () => screen.getAllByRole("button", { name: "Submit" });
const cancelButton = () => screen.getAllByRole("button", { name: "Cancel" })[0];

describe("TestActivation", () => {
  let queryClient;
  let reload;
  let onServer;

  const renderScreen = () =>
    render(
      <MemoryRouter>
        <IntlProvider locale="en" messages={messages}>
          <QueryClientProvider client={queryClient}>
            <NotificationContext.Provider
              value={{
                notificationVisible: false,
                setNotificationVisible: vi.fn(),
                addNotification: notify,
              }}
            >
              <TestActivation />
            </NotificationContext.Provider>
          </QueryClientProvider>
        </IntlProvider>
      </MemoryRouter>,
    );

  const turnGlucoseOff = async () => {
    renderScreen();
    await waitFor(() => expect(glucose()).toBeInTheDocument());
    expect(glucose()).toBeChecked();
    // Carbon hides the input behind its label, so the click goes to the input.
    fireEvent.click(glucose());
    await waitFor(() => expect(glucose()).not.toBeChecked());
  };

  beforeEach(() => {
    queryClient = createQueryClient();
    onServer = activation();
    getFromOpenElisServer.mockReset();
    getFromOpenElisServer.mockImplementation((url, callback) =>
      url.startsWith("/rest/TestActivation")
        ? callback(onServer)
        : callback(undefined),
    );
    postToOpenElisServerJsonResponse.mockReset();
    reload = vi.fn();
    Object.defineProperty(window, "location", {
      configurable: true,
      value: { ...window.location, reload, assign: vi.fn() },
    });
  });

  const threeSampleTypes = () => ({
    activeTestList: [
      ...activation().activeTestList,
      {
        sampleType: { id: "2", value: "Urine" },
        activeTests: [{ id: "20", value: "Protein" }],
        inactiveTests: [{ id: "21", value: "Ketones" }],
      },
      {
        sampleType: { id: "5", value: "Swab" },
        activeTests: [],
        inactiveTests: [{ id: "50", value: "COVID PCR" }],
      },
    ],
    inactiveTestList: [
      {
        sampleType: { id: "3", value: "Stool" },
        activeTests: [],
        inactiveTests: [{ id: "30", value: "Occult blood" }],
      },
    ],
  });

  it("lists active and inactive sample types without a React key warning", async () => {
    const consoleError = vi.spyOn(console, "error");
    onServer = threeSampleTypes();

    renderScreen();

    await waitFor(() => expect(glucose()).toBeInTheDocument());
    expect(document.getElementById("2-21")).not.toBeChecked();
    expect(document.getElementById("3-30")).toBeInTheDocument();
    fireEvent.click(document.getElementById("2-21"));
    await waitFor(() => expect(document.getElementById("2-21")).toBeChecked());
    fireEvent.click(submitButtons()[0]);
    expect(
      await screen.findByText("The following tests will be activated:"),
    ).toBeInTheDocument();
    const keyWarnings = consoleError.mock.calls
      .map((args) => args.map(String).join(" "))
      .filter((text) => text.includes('unique "key"'));
    consoleError.mockRestore();
    expect(keyWarnings).toEqual([]);
  });

  it("names only the sample types that were inactive as about to be activated", async () => {
    onServer = threeSampleTypes();
    renderScreen();
    await waitFor(() => expect(glucose()).toBeInTheDocument());

    fireEvent.click(document.getElementById("2-21"));
    fireEvent.click(document.getElementById("3-30"));
    await waitFor(() => expect(document.getElementById("3-30")).toBeChecked());
    fireEvent.click(submitButtons()[0]);

    const toActivate = await screen.findByTestId(
      "test-activation-sample-types-to-activate",
    );
    expect(toActivate).toHaveTextContent("Stool");
    expect(toActivate).not.toHaveTextContent("Urine");
  });

  const headings = {
    testsOn: "The following tests will be activated:",
    typesOn: "The following sample types will be activated:",
    testsOff: "The following tests will be deactivated:",
    typesOff:
      "The following sample types will be deactivated because they have no tests:",
  };

  const shownHeadings = () =>
    Object.keys(headings).filter((key) => screen.queryByText(headings[key]));

  it("heads only the lists that have entries when one more test of an active type is turned on", async () => {
    onServer = threeSampleTypes();
    renderScreen();
    await waitFor(() => expect(glucose()).toBeInTheDocument());

    fireEvent.click(document.getElementById("2-21"));
    await waitFor(() => expect(document.getElementById("2-21")).toBeChecked());
    fireEvent.click(submitButtons()[0]);

    await screen.findByText(headings.testsOn);
    expect(shownHeadings()).toEqual(["testsOn"]);
    expect(
      screen.queryByTestId("test-activation-sample-types-to-activate"),
    ).not.toBeInTheDocument();
  });

  it.each([
    ["a test of an active type", "2-21"],
    ["a test of an inactive type", "3-30"],
    ["the only test of an active type with none on", "5-50"],
  ])(
    "leaves nothing to submit once %s is turned on and off again",
    async (_, checkboxId) => {
      onServer = threeSampleTypes();
      renderScreen();
      await waitFor(() => expect(glucose()).toBeInTheDocument());

      fireEvent.click(document.getElementById(checkboxId));
      await waitFor(() =>
        submitButtons().forEach((button) => expect(button).toBeEnabled()),
      );
      fireEvent.click(document.getElementById(checkboxId));

      await waitFor(() =>
        submitButtons().forEach((button) => expect(button).toBeDisabled()),
      );
      expect(document.getElementById(checkboxId)).not.toBeChecked();
    },
  );

  it("names only the later change after an earlier one was undone", async () => {
    onServer = threeSampleTypes();
    renderScreen();
    await waitFor(() => expect(glucose()).toBeInTheDocument());

    fireEvent.click(document.getElementById("3-30"));
    await waitFor(() => expect(document.getElementById("3-30")).toBeChecked());
    fireEvent.click(document.getElementById("3-30"));
    await waitFor(() =>
      submitButtons().forEach((button) => expect(button).toBeDisabled()),
    );
    fireEvent.click(document.getElementById("2-21"));
    await waitFor(() => expect(document.getElementById("2-21")).toBeChecked());
    fireEvent.click(submitButtons()[0]);

    await screen.findByText(headings.testsOn);
    expect(shownHeadings()).toEqual(["testsOn"]);
    expect(screen.getByRole("dialog")).not.toHaveTextContent("Occult blood");
  });

  it.each([
    ["an active type with none on", "5-50", "COVID PCR"],
    ["an inactive type", "3-30", "Occult blood"],
  ])(
    "names and sends only the change that stands after a test of %s was turned on and off around it",
    async (_, undoneId, undoneName) => {
      onServer = threeSampleTypes();
      renderScreen();
      await waitFor(() => expect(glucose()).toBeInTheDocument());

      fireEvent.click(document.getElementById(undoneId));
      await waitFor(() =>
        expect(document.getElementById(undoneId)).toBeChecked(),
      );
      fireEvent.click(document.getElementById("2-21"));
      await waitFor(() =>
        expect(document.getElementById("2-21")).toBeChecked(),
      );
      fireEvent.click(document.getElementById(undoneId));
      await waitFor(() =>
        expect(document.getElementById(undoneId)).not.toBeChecked(),
      );
      fireEvent.click(submitButtons()[0]);

      await screen.findByText(headings.testsOn);
      expect(shownHeadings()).toEqual(["testsOn"]);
      expect(screen.getByRole("dialog")).toHaveTextContent("Ketones");
      expect(screen.getByRole("dialog")).not.toHaveTextContent(undoneName);

      postToOpenElisServerJsonResponse.mockImplementation(
        (url, payload, callback) => callback(true),
      );
      await userEvent.click(screen.getByRole("button", { name: "Accept" }));
      const [, payload] = postToOpenElisServerJsonResponse.mock.calls[0];
      const sent = JSON.parse(JSON.parse(payload).jsonChangeList);
      const ids = (key) => JSON.parse(sent[key]).map((item) => String(item.id));
      expect(ids("activateTest")).toEqual(["21"]);
      expect(ids("deactivateTest")).toEqual([]);
      expect(ids("deactivateSample")).toEqual([]);
      expect(ids("activateSample")).not.toContain("3");
    },
  );

  const sharedTest = () => ({
    activeTestList: [
      ...activation().activeTestList,
      {
        sampleType: { id: "6", value: "Plasma" },
        activeTests: [
          { id: "10", value: "Glucose" },
          { id: "60", value: "Creatinine" },
        ],
        inactiveTests: [],
      },
    ],
    inactiveTestList: [],
  });

  const accept = async () => {
    postToOpenElisServerJsonResponse.mockImplementation(
      (url, payload, callback) => callback(true),
    );
    await userEvent.click(screen.getByRole("button", { name: "Accept" }));
    const [, payload] = postToOpenElisServerJsonResponse.mock.calls[0];
    const sent = JSON.parse(JSON.parse(payload).jsonChangeList);
    return (key) => JSON.parse(sent[key]).map((item) => String(item.id));
  };

  it("turns off a test listed under two sample types when it is unchecked under one", async () => {
    onServer = sharedTest();
    await turnGlucoseOff();
    expect(document.getElementById("6-10")).toBeChecked();
    fireEvent.click(submitButtons()[0]);

    await screen.findByText(headings.testsOff);
    expect(shownHeadings()).toEqual(["testsOff"]);
    expect(screen.getByRole("dialog")).toHaveTextContent("Glucose");
    const sent = await accept();
    expect(sent("deactivateTest")).toEqual(["10"]);
  });

  it("lists and sends a test unchecked under two sample types once", async () => {
    const consoleError = vi.spyOn(console, "error");
    onServer = sharedTest();
    await turnGlucoseOff();
    fireEvent.click(document.getElementById("6-10"));
    await waitFor(() =>
      expect(document.getElementById("6-10")).not.toBeChecked(),
    );
    fireEvent.click(submitButtons()[0]);

    await screen.findByText(headings.testsOff);
    expect(
      screen.getByRole("dialog").textContent.match(/Glucose/g),
    ).toHaveLength(1);
    const keyWarnings = consoleError.mock.calls
      .map((args) => args.map(String).join(" "))
      .filter((text) => text.includes("same key"));
    consoleError.mockRestore();
    expect(keyWarnings).toEqual([]);
    const sent = await accept();
    expect(sent("deactivateTest")).toEqual(["10"]);
  });

  it("keeps the changes when Back returns from the confirmation", async () => {
    await turnGlucoseOff();
    fireEvent.click(submitButtons()[0]);
    await screen.findByText(headings.testsOff);

    await userEvent.click(screen.getByRole("button", { name: "Back" }));

    await waitFor(() =>
      expect(document.querySelector(".cds--modal.is-visible")).toBeNull(),
    );
    expect(glucose()).not.toBeChecked();
    submitButtons().forEach((button) => expect(button).toBeEnabled());
    fireEvent.click(submitButtons()[0]);
    const sent = await accept();
    expect(sent("deactivateTest")).toEqual(["10"]);
  });

  it("heads only the deactivated tests when a test is turned off and its type keeps others", async () => {
    await turnGlucoseOff();
    fireEvent.click(submitButtons()[0]);

    await screen.findByText(headings.testsOff);
    expect(shownHeadings()).toEqual(["testsOff"]);
  });

  it("shows each active test as turned on", async () => {
    renderScreen();

    await waitFor(() => expect(glucose()).toBeInTheDocument());
    expect(glucose()).toBeChecked();
    expect(document.getElementById("1-11")).toBeChecked();
  });

  it("keeps the visible draft and submitted changes together after a refetch", async () => {
    await turnGlucoseOff();
    onServer = activation();
    onServer.activeTestList[0].activeTests[1].value = "Urea updated";
    await act(async () => {
      await queryClient.invalidateQueries({ queryKey: ["serverData"] });
    });

    expect(glucose()).not.toBeChecked();
    await userEvent.click(screen.getAllByRole("button", { name: "Submit" })[0]);
    await userEvent.click(screen.getByRole("button", { name: "Accept" }));
    const [, payload] = postToOpenElisServerJsonResponse.mock.calls[0];
    const sent = JSON.parse(JSON.parse(payload).jsonChangeList);
    expect(JSON.parse(sent.deactivateTest)).toEqual([{ id: 10 }]);
  });

  it("turns a test back on when the change is cancelled", async () => {
    await turnGlucoseOff();

    // Cancelling is the case where nothing on the server changed, so only
    // dropping the pending change can bring the stored state back.
    await userEvent.click(cancelButton());

    await waitFor(() => expect(glucose()).toBeChecked());
    expect(reload).not.toHaveBeenCalled();
  });

  // OGC-1234: a refusal arrives as an object carrying its HTTP status; it used
  // to be read as saved, which dropped the change and said so.
  it("keeps the change and reports an error when the save is refused", async () => {
    notify.mockReset();
    await turnGlucoseOff();

    postToOpenElisServerJsonResponse.mockImplementation(
      (url, payload, callback) =>
        callback({ error: "validation", fieldErrors: [], status: 400 }),
    );
    await userEvent.click(submitButtons()[0]);
    await userEvent.click(screen.getByRole("button", { name: "Accept" }));

    await waitFor(() =>
      expect(notify).toHaveBeenCalledWith(
        expect.objectContaining({ kind: "error" }),
      ),
    );
    expect(notify).not.toHaveBeenCalledWith(
      expect.objectContaining({ kind: "success" }),
    );
    expect(glucose()).not.toBeChecked();
  });

  it("sends the test that was turned off and forgets it once saved", async () => {
    await turnGlucoseOff();

    postToOpenElisServerJsonResponse.mockImplementation(
      (url, payload, callback) => callback(true),
    );
    await userEvent.click(submitButtons()[0]);
    await userEvent.click(screen.getByRole("button", { name: "Accept" }));

    expect(postToOpenElisServerJsonResponse).toHaveBeenCalledTimes(1);
    const [, payload] = postToOpenElisServerJsonResponse.mock.calls[0];
    const sent = JSON.parse(JSON.parse(payload).jsonChangeList);
    expect(JSON.parse(sent.deactivateTest)).toEqual([{ id: 10 }]);

    // With the change saved there is nothing left to submit.
    await waitFor(() =>
      submitButtons().forEach((button) => expect(button).toBeDisabled()),
    );
    expect(reload).not.toHaveBeenCalled();
  });
});
