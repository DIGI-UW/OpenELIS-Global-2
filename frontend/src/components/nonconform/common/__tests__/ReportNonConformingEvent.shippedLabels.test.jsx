/**
 * OGC-1507: the Report NCE form lists the categories and types the server
 * labels in the user's language, reports an event that concerns no sample,
 * lets a keyboard choose the severity, and says why a save was refused.
 */
import React from "react";
import { act, fireEvent, render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import messages from "../../../../languages/en.json";
import { ReportNonConformingEvent } from "../ReportNonConformingEvent";
import { NotificationContext } from "../../../layout/Layout";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../../utils/Utils";

vi.mock("../../../utils/Utils", async (importOriginal) => {
  const actual = await importOriginal();
  return {
    ...actual,
    getFromOpenElisServer: vi.fn(),
    postToOpenElisServerJsonResponse: vi.fn(),
    postToOpenElisServerFormData: vi.fn(),
  };
});

const CATEGORIES = [
  {
    id: "1",
    name: "Général",
    displayKey: "nce.category.general",
    types: [
      {
        id: "58",
        name: "Manque de formation",
        displayKey: "nce.type.training_gap",
      },
    ],
  },
  { id: "2", name: "Demande", displayKey: "nce.category.order", types: [] },
];

const renderForm = async () => {
  const addNotification = vi.fn();
  await act(async () =>
    render(
      <IntlProvider locale="en" messages={messages}>
        <NotificationContext.Provider
          value={{
            notificationVisible: false,
            setNotificationVisible: vi.fn(),
            addNotification,
          }}
        >
          <MemoryRouter initialEntries={["/ReportNonConformingEvent"]}>
            <ReportNonConformingEvent />
          </MemoryRouter>
        </NotificationContext.Provider>
      </IntlProvider>,
    ),
  );
  return addNotification;
};

const fill = async (user) => {
  await user.selectOptions(document.getElementById("reporting-unit"), "7");
  await user.selectOptions(document.getElementById("nce-category"), "1");
  await user.selectOptions(document.getElementById("nce-type"), "58");
  await user.type(
    document.getElementById("nce-description"),
    "Two new staff had no NCE training.",
  );
};

describe("Report NCE with shipped labels", () => {
  beforeEach(() => {
    getFromOpenElisServer.mockReset();
    postToOpenElisServerJsonResponse.mockReset();
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url === "/rest/nce/categories") {
        callback(CATEGORIES);
      } else if (url === "/rest/displayList/TEST_SECTION_ACTIVE") {
        callback([{ id: "7", value: "Biochemistry" }]);
      } else if (url === "/rest/nce/generate-number") {
        callback({ nceNumber: "NCE-1" });
      } else {
        callback([]);
      }
    });
  });

  it("lists the server's labels and asks for no list that does not exist", async () => {
    await renderForm();

    const user = userEvent.setup();
    expect(screen.getByRole("option", { name: "Général" })).toBeInTheDocument();
    await user.selectOptions(document.getElementById("nce-category"), "2");
    expect(getFromOpenElisServer).not.toHaveBeenCalledWith(
      expect.stringMatching(/displayList\/NCE_/),
      expect.any(Function),
    );
    await user.selectOptions(document.getElementById("nce-category"), "1");
    expect(
      [...document.getElementById("nce-type").options].map((o) => o.text),
    ).toContain("Manque de formation");
  });

  it("chooses a severity from the keyboard and reports an event with no sample", async () => {
    await renderForm();
    const user = userEvent.setup();
    await fill(user);
    const minor = screen.getByRole("radio", { name: /Minor/ });
    expect(minor).toHaveAttribute("aria-checked", "false");
    minor.focus();
    fireEvent.keyDown(minor, { key: " " });
    expect(minor).toHaveAttribute("aria-checked", "true");
    postToOpenElisServerJsonResponse.mockImplementation((url, body, cb) =>
      cb({ success: true }),
    );

    fireEvent.click(screen.getByRole("button", { name: "Submit NCE" }));

    await waitFor(() =>
      expect(postToOpenElisServerJsonResponse).toHaveBeenCalled(),
    );
    const sent = JSON.parse(postToOpenElisServerJsonResponse.mock.calls[0][1]);
    expect(sent).toMatchObject({
      severity: "MINOR",
      nceCategoryId: "1",
      nceTypeId: "58",
      labOrderNumber: "",
      specimenId: "",
    });
  });

  it("says why the server refused the save", async () => {
    const addNotification = await renderForm();
    await fill(userEvent.setup());
    fireEvent.click(screen.getByRole("radio", { name: /Major/ }));
    postToOpenElisServerJsonResponse.mockImplementation((url, body, cb) =>
      cb({ success: false, error: "dateOfEvent is required" }),
    );

    fireEvent.click(screen.getByRole("button", { name: "Submit NCE" }));

    await waitFor(() =>
      expect(addNotification).toHaveBeenCalledWith(
        expect.objectContaining({
          kind: "error",
          message: "Save failed: dateOfEvent is required",
        }),
      ),
    );
  });
});
