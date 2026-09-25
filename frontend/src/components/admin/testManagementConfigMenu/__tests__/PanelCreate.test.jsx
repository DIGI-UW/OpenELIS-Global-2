import React from "react";
import { render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import userEvent from "@testing-library/user-event";
import { IntlProvider } from "react-intl";
import { MemoryRouter, Route } from "react-router-dom";
import { QueryClientProvider } from "@tanstack/react-query";
import { beforeEach, describe, expect, it, vi } from "vitest";
import messages from "../../../../languages/en.json";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../../utils/Utils";
import { createQueryClient } from "../../../utils/queryClient";
import { NotificationContext } from "../../../layout/Layout";
import PanelCreate from "../PanelCreate";

/**
 * OGC-1232 — the legacy Create New Panel screen. It has to offer the panel's
 * domain (it always wrote CLINICAL), send the choice, and report a refused
 * create instead of celebrating it: the server used to answer 200 with the
 * form echoed for every failure, and the screen used to treat any answer as
 * success.
 */

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

const DOMAINS = [
  { id: "CLINICAL", labelKey: "label.domain.CLINICAL" },
  { id: "ENVIRONMENTAL", labelKey: "label.domain.ENVIRONMENTAL" },
  { id: "VECTOR", labelKey: "label.domain.VECTOR" },
];

const FORM = {
  existingPanelList: [
    { typeOfSampleName: "Serum", panels: [{ panelName: "Bilan Biochimique" }] },
  ],
  inactivePanelList: [],
  existingSampleTypeList: [{ id: "1", value: "Serum" }],
};

const english = () => document.getElementById("eng");
const french = () => document.getElementById("fr");
const loinc = () => document.getElementById("loincPost");
const sampleType = () => document.getElementById("smapleTypeSelect");

const fillForm = async () => {
  await userEvent.type(english(), "Water Panel");
  await userEvent.type(french(), "Panel Eau");
  await userEvent.selectOptions(sampleType(), "1");
  await userEvent.type(loinc(), "12345-6");
};

describe("PanelCreate (legacy Create New Panel)", () => {
  let addNotification;

  const renderScreen = () =>
    render(
      <MemoryRouter initialEntries={["/MasterListsPage/PanelCreate"]}>
        <IntlProvider locale="en" messages={messages}>
          <QueryClientProvider client={createQueryClient()}>
            <NotificationContext.Provider
              value={{
                notificationVisible: false,
                setNotificationVisible: vi.fn(),
                addNotification,
              }}
            >
              <Route path="/MasterListsPage/PanelCreate">
                <PanelCreate />
              </Route>
            </NotificationContext.Provider>
          </QueryClientProvider>
        </IntlProvider>
      </MemoryRouter>,
    );

  beforeEach(() => {
    addNotification = vi.fn();
    getFromOpenElisServer.mockReset();
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url.startsWith("/rest/PanelCreate")) {
        callback(FORM);
      } else if (url.startsWith("/rest/domains")) {
        callback(DOMAINS);
      } else {
        callback(undefined);
      }
    });
    postToOpenElisServerJsonResponse.mockReset();
  });

  it("offers every domain, defaulting to Clinical", async () => {
    renderScreen();
    expect(await screen.findByText("Bilan Biochimique")).toBeInTheDocument();

    expect(await screen.findByLabelText("Clinical")).toBeChecked();
    expect(screen.getByLabelText("Environmental")).not.toBeChecked();
    expect(screen.getByLabelText("Vector")).not.toBeChecked();
    expect(screen.getByTestId("panel-create-domain-helper")).toHaveTextContent(
      "Only Clinical-domain tests can be added to this panel.",
    );
  });

  it("sends the chosen domain and reports the created panel", async () => {
    renderScreen();
    expect(await screen.findByText("Bilan Biochimique")).toBeInTheDocument();
    await screen.findByLabelText("Environmental");

    await fillForm();
    await userEvent.click(screen.getByLabelText("Environmental"));
    expect(screen.getByTestId("panel-create-domain-helper")).toHaveTextContent(
      "Only Environmental-domain tests can be added to this panel.",
    );
    postToOpenElisServerJsonResponse.mockImplementation(
      (url, payload, callback) =>
        callback({ ...FORM, createdPanelId: "77", domain: "ENVIRONMENTAL" }),
    );

    await userEvent.click(screen.getByRole("button", { name: "Next" }));
    await userEvent.click(screen.getByRole("button", { name: "Accept" }));

    expect(postToOpenElisServerJsonResponse).toHaveBeenCalledWith(
      "/rest/PanelCreate",
      JSON.stringify({
        panelEnglishName: "Water Panel",
        panelFrenchName: "Panel Eau",
        sampleTypeId: "1",
        panelLoinc: "12345-6",
        domain: "ENVIRONMENTAL",
      }),
      expect.any(Function),
    );
    await waitFor(() =>
      expect(addNotification).toHaveBeenCalledWith(
        expect.objectContaining({
          kind: "success",
          message: messages["success.panel.created"],
        }),
      ),
    );
    expect(english()).toHaveValue("");
  });

  it("reports a duplicate name and keeps the entry for another attempt", async () => {
    renderScreen();
    expect(await screen.findByText("Bilan Biochimique")).toBeInTheDocument();
    await screen.findByLabelText("Clinical");

    await fillForm();
    postToOpenElisServerJsonResponse.mockImplementation(
      (url, payload, callback) =>
        callback({ error: "duplicate", status: 409, statusCode: 409 }),
    );

    await userEvent.click(screen.getByRole("button", { name: "Next" }));
    await userEvent.click(screen.getByRole("button", { name: "Accept" }));

    await waitFor(() =>
      expect(addNotification).toHaveBeenCalledWith(
        expect.objectContaining({
          kind: "error",
          message: messages["configuration.panel.create.duplicate"],
        }),
      ),
    );
    expect(addNotification).not.toHaveBeenCalledWith(
      expect.objectContaining({ kind: "success" }),
    );
    // the same contract as every create screen: the entry is kept and the
    // operator can accept again or reject to start over
    expect(english()).toHaveValue("Water Panel");
    expect(screen.getByRole("button", { name: "Accept" })).toBeEnabled();
  });

  it("reports a rejected create instead of celebrating it", async () => {
    renderScreen();
    expect(await screen.findByText("Bilan Biochimique")).toBeInTheDocument();
    await screen.findByLabelText("Clinical");

    await fillForm();
    postToOpenElisServerJsonResponse.mockImplementation(
      (url, payload, callback) => callback({ status: 400, statusCode: 400 }),
    );

    await userEvent.click(screen.getByRole("button", { name: "Next" }));
    await userEvent.click(screen.getByRole("button", { name: "Accept" }));

    await waitFor(() =>
      expect(addNotification).toHaveBeenCalledWith(
        expect.objectContaining({
          kind: "error",
          message: messages["error.panel.create.invalid"],
        }),
      ),
    );
  });

  it("shows the LOINC format error instead of failing silently", async () => {
    renderScreen();
    expect(await screen.findByText("Bilan Biochimique")).toBeInTheDocument();
    await screen.findByLabelText("Clinical");

    await userEvent.type(english(), "Water Panel");
    await userEvent.type(french(), "Panel Eau");
    await userEvent.selectOptions(sampleType(), "1");
    await userEvent.type(loinc(), "not a loinc");
    await userEvent.click(screen.getByRole("button", { name: "Next" }));

    expect(
      await screen.findByText(
        "Invalid format. Use digits separated by single dashes (e.g. 1-2-3)",
      ),
    ).toBeInTheDocument();
    expect(loinc()).toHaveValue("not a loinc");
    expect(postToOpenElisServerJsonResponse).not.toHaveBeenCalled();
  });
});
