import React from "react";
import { vi } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../../../languages/en.json";
import TemplatesTab from "../TemplatesTab";

const TEMPLATES = {
  templates: [
    {
      type: "REFERRAL_OUT",
      subjectTemplate: "Sample referred out: [sampleAccessionNumber]",
      messageTemplate:
        "Sample [sampleAccessionNumber] was referred to [labName].",
      defaultSubject: "Sample referred out: [sampleAccessionNumber]",
      defaultMessage:
        "Sample [sampleAccessionNumber] was referred to [labName].",
      sampleValues: {
        sampleAccessionNumber: "DEV0126000000001",
        labName: "CPHL",
      },
      availableVariables: [
        {
          token: "sampleAccessionNumber",
          description: "Sample / specimen ID (lab number)",
        },
        { token: "labName", description: "Name of the external laboratory" },
      ],
    },
  ],
};

const calls = [];
const addNotification = vi.fn();

vi.mock("../../../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn((url, callback) => {
    calls.push(url);
    callback(
      url === "/rest/NotificationTriggerConfig/templates" ? TEMPLATES : null,
    );
  }),
  putToOpenElisServerJsonResponse: vi.fn((url, body, callback) => {
    calls.push(`PUT ${url} ${body}`);
    callback({
      error: "fields not offered to this slot: patientFirstName",
      disallowedFields: ["patientFirstName"],
    });
  }),
}));

vi.mock("../../../../../config.json", () => ({
  default: { serverBaseUrl: "/api/OpenELIS-Global" },
}));

vi.mock("../../../../layout/Layout", () => ({
  NotificationContext: React.createContext({
    notificationVisible: false,
    setNotificationVisible: () => {},
    addNotification: (notice) => addNotification(notice),
  }),
}));

vi.mock("../../../../common/CustomNotification", () => ({
  AlertDialog: () => null,
  NotificationKinds: { success: "success", error: "error" },
}));

describe("TemplatesTab", () => {
  beforeEach(() => {
    calls.length = 0;
    addNotification.mockClear();
  });

  it("names the refused merge fields when a template is not saved", async () => {
    render(
      <IntlProvider locale="en" messages={messages}>
        <TemplatesTab />
      </IntlProvider>,
    );
    const body = await screen.findByLabelText("Message Body");
    fireEvent.change(body, {
      target: {
        value:
          "Sample [sampleAccessionNumber] for [patientFirstName] was referred.",
      },
    });
    fireEvent.click(screen.getByRole("button", { name: "Save Template" }));

    await waitFor(() => expect(addNotification).toHaveBeenCalled());
    const notice = addNotification.mock.calls[0][0];
    expect(notice.kind).toBe("error");
    expect(notice.message).toContain("[patientFirstName]");
    expect(
      calls.some((call) =>
        call.startsWith(
          "PUT /rest/NotificationTriggerConfig/templates/REFERRAL_OUT",
        ),
      ),
    ).toBe(true);
  });
});
