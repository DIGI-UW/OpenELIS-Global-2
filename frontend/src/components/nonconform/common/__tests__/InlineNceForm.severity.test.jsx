/**
 * OGC-1507: the NCE form embedded in the order and result steps lets a
 * keyboard reach and choose the severity, as the Report NCE form does, sends
 * the specimen its step is about, and says why the server refused a save.
 */
import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../../languages/en.json";
import InlineNceForm from "../InlineNceForm";
import { NotificationContext } from "../../../layout/Layout";
import UserSessionDetailsContext from "../../../../UserSessionDetailsContext";
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
    name: "General",
    types: [{ id: "58", name: "Documentation error" }],
  },
];

const renderForm = (props = {}) => {
  getFromOpenElisServer.mockImplementation((url, cb) => {
    if (url === "/rest/nce/categories") {
      cb(CATEGORIES);
    } else if (url.startsWith("/rest/displayList/TEST_SECTION_ACTIVE")) {
      cb([{ id: "136", value: "Hematology" }]);
    } else if (url === "/rest/nce/generate-number") {
      cb({ nceNumber: "NCE-2026-00099" });
    } else {
      cb([]);
    }
  });
  const addNotification = vi.fn();
  render(
    <IntlProvider locale="en" messages={messages}>
      <NotificationContext.Provider
        value={{
          notificationVisible: false,
          setNotificationVisible: vi.fn(),
          addNotification,
        }}
      >
        <UserSessionDetailsContext.Provider
          value={{ userSessionDetails: { firstName: "A", lastName: "B" } }}
        >
          <InlineNceForm
            accessionNumber="DEV01260000000000001"
            onClose={vi.fn()}
            {...props}
          />
        </UserSessionDetailsContext.Provider>
      </NotificationContext.Provider>
    </IntlProvider>,
  );
  return addNotification;
};

const fillAndSubmit = async () => {
  const user = userEvent.setup();
  await user.selectOptions(
    document.getElementById("inline-nce-reporting-unit"),
    "136",
  );
  await user.selectOptions(document.getElementById("inline-nce-category"), "1");
  await user.selectOptions(document.getElementById("inline-nce-type"), "58");
  fireEvent.click(screen.getByTestId("inline-nce-severity-MINOR"));
  await user.type(
    document.getElementById("inline-nce-description"),
    "Logbook page missing.",
  );
  await user.click(screen.getByRole("button", { name: /Create NCE/ }));
};

it("chooses the severity from the keyboard", () => {
  renderForm();

  const group = screen.getByRole("radiogroup", { name: /Severity/ });
  expect(group).toHaveAttribute("aria-required", "true");
  const major = screen.getByTestId("inline-nce-severity-MAJOR");
  expect(major).toHaveAttribute("tabindex", "0");
  expect(major).toHaveAttribute("aria-checked", "false");

  fireEvent.keyDown(major, { key: " " });
  expect(major).toHaveAttribute("aria-checked", "true");

  const minor = screen.getByTestId("inline-nce-severity-MINOR");
  fireEvent.keyDown(minor, { key: "Enter" });
  expect(minor).toHaveAttribute("aria-checked", "true");
  expect(major).toHaveAttribute("aria-checked", "false");
});

it("sends the specimen its step is about", async () => {
  postToOpenElisServerJsonResponse.mockImplementation((url, body, cb) =>
    cb({ success: true }),
  );
  renderForm({ sampleItemId: 731 });

  await fillAndSubmit();

  await waitFor(() =>
    expect(postToOpenElisServerJsonResponse).toHaveBeenCalled(),
  );
  const body = JSON.parse(postToOpenElisServerJsonResponse.mock.calls[0][1]);
  expect(body.labOrderNumber).toBe("DEV01260000000000001");
  expect(body.specimenId).toBe("731");
});

it("says why the server refused the save", async () => {
  postToOpenElisServerJsonResponse.mockImplementation((url, body, cb) =>
    cb({ success: false, error: "No order has the lab number X" }),
  );
  const addNotification = renderForm();

  await fillAndSubmit();

  await waitFor(() =>
    expect(addNotification).toHaveBeenCalledWith(
      expect.objectContaining({
        kind: "error",
        message: "Save failed: No order has the lab number X",
      }),
    ),
  );
});
