import React from "react";
import { render, screen, fireEvent, cleanup } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import messages from "../../languages/en.json";
import UserSessionDetailsContext from "../../UserSessionDetailsContext";
import { NotificationContext } from "../layout/Layout";

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
    useParams: () => ({ notebookid: "1", notebookentryid: "1" }),
    useHistory: () => ({ push: vi.fn() }),
  };
});

import NoteBookEntryForm from "./NoteBookEntryForm";
import NoteBookInstanceEntryForm from "./NoteBookInstanceEntryForm";

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

const renderForm = (Form) =>
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
            <Form />
          </MemoryRouter>
        </NotificationContext.Provider>
      </UserSessionDetailsContext.Provider>
    </IntlProvider>,
  );

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
});

it.each([
  ["template", NoteBookEntryForm, "/NoteBookEntryForm/7"],
  ["instance", NoteBookInstanceEntryForm, "/NoteBookInstanceEditForm/7"],
])(
  "%s keeps unsaved edits after rejection and redirects only after a successful save",
  async (_, Form, destination) => {
    utilsMock.getFromOpenElisServer.mockReset();
    utilsMock.getFromOpenElisServer.mockImplementation(serve);
    utilsMock.postToOpenElisServerFullResponse.mockReset();
    let status = 400;
    utilsMock.postToOpenElisServerFullResponse.mockImplementation(
      (url, body, callback) =>
        callback({
          status,
          json: async () =>
            status === 200 ? { id: 7 } : { error: "Rejected attachment" },
        }),
    );
    renderForm(Form);
    fireEvent.click(
      await screen.findByRole("tab", {
        name: messages["notebook.tab.comments"],
      }),
    );
    fireEvent.change(
      screen.getByPlaceholderText(messages["notebook.comments.add.label"]),
      { target: { value: "Unsaved notebook edits" } },
    );
    fireEvent.click(
      screen.getByRole("button", {
        name: messages["notebook.comments.add.button"],
      }),
    );
    const originalWindow = window;
    const location = { href: "/current-notebook" };
    vi.stubGlobal(
      "window",
      new Proxy(originalWindow, {
        get(target, key) {
          return key === "location" ? location : Reflect.get(target, key);
        },
      }),
    );
    const save = screen.getByRole("button", {
      name: messages["label.button.save"],
    });
    fireEvent.click(save);
    await waitFor(() =>
      expect(utilsMock.postToOpenElisServerFullResponse).toHaveBeenCalledTimes(
        1,
      ),
    );
    await waitFor(() => expect(save).not.toBeDisabled());
    expect(location.href).toBe("/current-notebook");
    expect(screen.getByText("Unsaved notebook edits")).toBeInTheDocument();
    status = 200;
    fireEvent.click(save);
    await waitFor(() => expect(location.href).toBe(destination));
    expect(
      JSON.parse(utilsMock.postToOpenElisServerFullResponse.mock.calls[1][1])
        .comments,
    ).toEqual([{ id: null, text: "Unsaved notebook edits" }]);
  },
);
