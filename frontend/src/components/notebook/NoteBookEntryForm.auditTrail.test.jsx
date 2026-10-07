import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import messages from "../../languages/en.json";
import UserSessionDetailsContext from "../../UserSessionDetailsContext";
import { NotificationContext } from "../layout/Layout";

/**
 * getFromOpenElisServer takes (endPoint, callback, abortSignal). Handing it an
 * error callback as the third argument made fetch() throw before the request
 * left, so a notebook with an audit trail showed "No Audit Trail Available".
 * The stand-in below refuses a function there the way the browser does.
 */
const { utilsMock } = vi.hoisted(() => ({
  utilsMock: {
    getFromOpenElisServer: vi.fn(),
    postToOpenElisServerFullResponse: vi.fn(),
    toBase64: vi.fn(),
  },
}));

vi.mock("../utils/Utils", () => utilsMock);

vi.mock("react-router-dom", async (importOriginal) => {
  const actual = await importOriginal();
  return {
    ...actual,
    useParams: () => ({ notebookid: "1" }),
    useHistory: () => ({ push: vi.fn() }),
  };
});

import NoteBookEntryForm from "./NoteBookEntryForm";

const NOTEBOOK = {
  id: 1,
  title: "Probe NB Template",
  status: "DRAFT",
  isTemplate: true,
  pages: [],
  samples: [],
  comments: [],
  tags: [],
  files: [],
  analyzers: [],
};

const AUDIT_TRAIL = {
  log: [
    {
      timeStamp: 1790703320367,
      action: "notebook.auditTrail.action.created",
      user: "ELIS,Open",
      item: "notebook.heading.notebooks",
      identifier: "Probe NB Template",
    },
  ],
};

const serve = (url, callback, signal) => {
  if (typeof signal === "function") {
    throw new TypeError("Failed to convert value to 'AbortSignal'");
  }
  if (url === "/rest/notebook/view/1") {
    callback(NOTEBOOK);
  } else if (url.startsWith("/rest/notebook/auditTrail?notebookId=1")) {
    callback(AUDIT_TRAIL);
  } else {
    callback([]);
  }
};

const renderForm = () =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <UserSessionDetailsContext.Provider
        value={{
          userSessionDetails: {
            userId: "1",
            firstName: "Open",
            lastName: "ELIS",
            roles: [],
          },
        }}
      >
        <NotificationContext.Provider
          value={{
            notificationVisible: false,
            setNotificationVisible: vi.fn(),
            addNotification: vi.fn(),
          }}
        >
          <MemoryRouter>
            <NoteBookEntryForm />
          </MemoryRouter>
        </NotificationContext.Provider>
      </UserSessionDetailsContext.Provider>
    </IntlProvider>,
  );

describe("NoteBookEntryForm audit trail", () => {
  beforeEach(() => {
    utilsMock.getFromOpenElisServer.mockReset();
    utilsMock.getFromOpenElisServer.mockImplementation(serve);
  });

  it("lists the notebook's audit entries", async () => {
    renderForm();

    await waitFor(() =>
      expect(
        utilsMock.getFromOpenElisServer.mock.calls.some(([url]) =>
          url.startsWith("/rest/notebook/auditTrail?notebookId=1"),
        ),
      ).toBe(true),
    );
    fireEvent.click(screen.getByRole("tab", { name: "Audit Trail" }));

    expect(await screen.findByText("ELIS,Open")).toBeInTheDocument();
    expect(screen.queryByText("No Audit Trail Available")).toBeNull();
  });
});
