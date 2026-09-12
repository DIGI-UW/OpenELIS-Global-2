import React, { useState } from "react";
import { render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import userEvent from "@testing-library/user-event";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import messages from "../../languages/en.json";
import { ConfigurationContext, NotificationContext } from "../layout/contexts";
import { getFromOpenElisServer, postToOpenElisServer } from "../utils/Utils";
import { SampleItemsDisplay, SearchSampleForm } from "./AliquotForm";

vi.mock("../utils/Utils", async () => ({
  ...(await vi.importActual("../utils/Utils")),
  getFromOpenElisServer: vi.fn(),
  postToOpenElisServer: vi.fn(),
}));

let addNotification;

const renderScreen = (screen) =>
  render(
    <MemoryRouter>
      <ConfigurationContext.Provider value={{ configurationProperties: {} }}>
        <NotificationContext.Provider
          value={{
            notificationVisible: false,
            setNotificationVisible: vi.fn(),
            addNotification,
          }}
        >
          <IntlProvider locale="en" messages={messages}>
            {screen}
          </IntlProvider>
        </NotificationContext.Provider>
      </ConfigurationContext.Provider>
    </MemoryRouter>,
  );

const SearchHarness = () => {
  const [resetVersion, setResetVersion] = useState(0);
  return (
    <>
      <SearchSampleForm
        resetVersion={resetVersion}
        setParam={vi.fn()}
        setSearchBy={vi.fn()}
        setSampleData={vi.fn()}
      />
      <button
        type="button"
        onClick={() => setResetVersion((value) => value + 1)}
      >
        complete aliquot
      </button>
    </>
  );
};

describe("Aliquot completion state", () => {
  beforeEach(() => {
    addNotification = vi.fn();
    getFromOpenElisServer.mockReset();
    postToOpenElisServer.mockReset();
    window.history.replaceState({}, "", "/Aliquot");
  });

  it("clears a typed accession number when aliquoting completes", async () => {
    renderScreen(<SearchHarness />);
    const accession = document.getElementById("accessionNumber");
    await userEvent.type(accession, "ACC-7");
    expect(accession).toHaveValue("ACC-7");

    await userEvent.click(
      screen.getByRole("button", { name: "complete aliquot" }),
    );

    await waitFor(() =>
      expect(document.getElementById("accessionNumber")).toHaveValue(""),
    );
  });

  it("clears a URL-prefilled accession without repeating the search", async () => {
    window.history.replaceState({}, "", "/Aliquot?accessionNumber=ACC-URL");
    getFromOpenElisServer.mockImplementation((url, callback) =>
      callback({ sampleItems: [] }),
    );
    renderScreen(<SearchHarness />);
    const accession = document.getElementById("accessionNumber");
    await waitFor(() => expect(accession).toHaveValue("ACC-URL"));

    await userEvent.click(
      screen.getByRole("button", { name: "complete aliquot" }),
    );

    await waitFor(() =>
      expect(document.getElementById("accessionNumber")).toHaveValue(""),
    );
    expect(getFromOpenElisServer).toHaveBeenCalledOnce();
  });

  it("requests the parent workflow reset only after a successful save", async () => {
    const onSaveSuccess = vi.fn();
    postToOpenElisServer.mockImplementation((url, body, callback) =>
      callback(200),
    );
    renderScreen(
      <SampleItemsDisplay
        sampleData={{
          accessionNumber: "ACC-7",
          sampleItems: [
            {
              externalId: "SPEC-7",
              typeOfSample: "Serum",
              quantity: 0,
              analysis: [],
              aliquots: [
                {
                  id: "SPEC-7.1",
                  externalId: "SPEC-7.1",
                  quantity: 0,
                  analyses: [{ id: "AN-7" }],
                },
              ],
            },
          ],
        }}
        setSampleData={vi.fn()}
        onSaveSuccess={onSaveSuccess}
      />,
    );

    await userEvent.click(
      screen.getByRole("button", { name: "Save Aliquot Changes" }),
    );

    expect(postToOpenElisServer).toHaveBeenCalledWith(
      "/rest/Aliquot",
      JSON.stringify({
        accessionNumber: "ACC-7",
        sampleItems: [
          {
            externalId: "SPEC-7",
            aliquots: [
              {
                externalId: "SPEC-7.1",
                quantity: 0,
                analyses: ["AN-7"],
              },
            ],
          },
        ],
      }),
      expect.any(Function),
    );
    expect(onSaveSuccess).toHaveBeenCalledOnce();
  });

  it("keeps the workflow state when saving fails", async () => {
    const onSaveSuccess = vi.fn();
    postToOpenElisServer.mockImplementation((url, body, callback) =>
      callback(500),
    );
    renderScreen(
      <SampleItemsDisplay
        sampleData={{
          accessionNumber: "ACC-7",
          sampleItems: [
            {
              externalId: "SPEC-7",
              typeOfSample: "Serum",
              quantity: 0,
              analysis: [],
              aliquots: [
                {
                  id: "SPEC-7.1",
                  externalId: "SPEC-7.1",
                  quantity: 0,
                  analyses: [{ id: "AN-7" }],
                },
              ],
            },
          ],
        }}
        setSampleData={vi.fn()}
        onSaveSuccess={onSaveSuccess}
      />,
    );

    await userEvent.click(
      screen.getByRole("button", { name: "Save Aliquot Changes" }),
    );

    expect(onSaveSuccess).not.toHaveBeenCalled();
    expect(addNotification).toHaveBeenCalledWith(
      expect.objectContaining({ kind: "error" }),
    );
  });
});
