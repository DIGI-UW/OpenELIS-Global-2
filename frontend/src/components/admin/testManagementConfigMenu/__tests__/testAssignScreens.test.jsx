import React from "react";
import { render, screen } from "@testing-library/react";
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
import { NotificationContext } from "../../../layout/contexts";
import SampleTypeTestAssign from "../SampleTypeTestAssign";
import TestSectionTestAssign from "../TestSectionTestAssign";

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

/**
 * Both screens move one test from the group it is in to another: pick the test,
 * pick the group, confirm, save. Each used to reload the document after a save,
 * when the modal is closed, and when nothing was picked.
 */
const SCREENS = [
  {
    name: "SampleTypeTestAssign",
    Screen: SampleTypeTestAssign,
    endPoint: "/rest/SampleTypeTestAssign",
    listField: "sampleTypeTestList",
    groupsField: "sampleTypeList",
    selectId: "sampleTypeListSelect",
    sentGroupField: "sampleTypeId",
  },
  {
    name: "TestSectionTestAssign",
    Screen: TestSectionTestAssign,
    endPoint: "/rest/TestSectionTestAssign",
    listField: "sectionTestList",
    groupsField: "testSectionList",
    selectId: "testSectionListSelect",
    sentGroupField: "testSectionId",
  },
];

describe.each(SCREENS)(
  "$name",
  ({ Screen, endPoint, listField, groupsField, selectId, sentGroupField }) => {
    let reload;
    let onServer;

    // The grouped list arrives keyed by the group's printed id/value pair.
    const assignments = (groupName, tests) => ({
      [listField]: { [`id=1, value=${groupName}`]: tests },
      [groupsField]: [
        { id: "1", value: groupName },
        { id: "2", value: "Serology" },
      ],
    });

    const renderScreen = () =>
      render(
        <MemoryRouter>
          <IntlProvider locale="en" messages={messages}>
            <QueryClientProvider client={createQueryClient()}>
              <NotificationContext.Provider
                value={{
                  notificationVisible: false,
                  setNotificationVisible: vi.fn(),
                  addNotification: vi.fn(),
                }}
              >
                <Screen />
              </NotificationContext.Provider>
            </QueryClientProvider>
          </IntlProvider>
        </MemoryRouter>,
      );

    const pickGlucoseAndSerology = async () => {
      await userEvent.click(await screen.findByText("Glucose"));
      await userEvent.selectOptions(document.getElementById(selectId), "2");
    };

    beforeEach(() => {
      onServer = assignments("Chemistry", [{ id: "10", value: "Glucose" }]);
      getFromOpenElisServer.mockReset();
      getFromOpenElisServer.mockImplementation((url, callback) =>
        url.startsWith(endPoint) ? callback(onServer) : callback(undefined),
      );
      postToOpenElisServerJsonResponse.mockReset();
      reload = vi.fn();
      Object.defineProperty(window, "location", {
        configurable: true,
        value: { ...window.location, reload, assign: vi.fn() },
      });
    });

    it("shows each test under the group it is assigned to", async () => {
      renderScreen();

      expect(
        await screen.findByRole("heading", { name: "Chemistry" }),
      ).toBeInTheDocument();
      expect(screen.getByText("Glucose")).toBeInTheDocument();
    });

    it("sends the picked test and group", async () => {
      renderScreen();
      await pickGlucoseAndSerology();

      await userEvent.click(screen.getByRole("button", { name: "Save" }));
      await userEvent.click(screen.getByRole("button", { name: "Accept" }));

      expect(postToOpenElisServerJsonResponse).toHaveBeenCalledTimes(1);
      const [, payload] = postToOpenElisServerJsonResponse.mock.calls[0];
      expect(JSON.parse(payload)).toMatchObject({
        testId: "10",
        [sentGroupField]: "2",
      });
    });

    it("reads the assignments again once the move is saved, without reloading", async () => {
      renderScreen();
      await pickGlucoseAndSerology();

      postToOpenElisServerJsonResponse.mockImplementation(
        (url, payload, callback) => {
          onServer = assignments("Serology", [{ id: "10", value: "Glucose" }]);
          callback(true);
        },
      );

      await userEvent.click(screen.getByRole("button", { name: "Save" }));
      await userEvent.click(screen.getByRole("button", { name: "Accept" }));

      await waitFor(() =>
        expect(
          screen.getByRole("heading", { name: "Serology" }),
        ).toBeInTheDocument(),
      );
      // Saving used to reload the document, which threw away the whole app.
      expect(reload).not.toHaveBeenCalled();
    });

    it("forgets the picked test when the move is abandoned", async () => {
      renderScreen();
      await pickGlucoseAndSerology();
      await userEvent.click(screen.getByRole("button", { name: "Save" }));

      // Closing the modal used to reload, which is how the pick was forgotten.
      await userEvent.click(screen.getByRole("button", { name: "Reject" }));
      await userEvent.click(screen.getByText("Glucose"));

      // Back to the first step, with nothing carried over from last time.
      expect(screen.getByRole("button", { name: "Save" })).toBeInTheDocument();
      expect(reload).not.toHaveBeenCalled();
    });

    it("sends nothing when no group was picked", async () => {
      renderScreen();
      await userEvent.click(await screen.findByText("Glucose"));

      await userEvent.click(screen.getByRole("button", { name: "Save" }));
      await userEvent.click(screen.getByRole("button", { name: "Accept" }));

      expect(postToOpenElisServerJsonResponse).not.toHaveBeenCalled();
      expect(reload).not.toHaveBeenCalled();
    });
  },
);
