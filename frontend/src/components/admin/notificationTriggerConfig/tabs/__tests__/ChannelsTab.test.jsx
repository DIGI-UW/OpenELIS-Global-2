import React from "react";
import { vi } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../../../languages/en.json";
import ChannelsTab from "../ChannelsTab";
import DeliverySettingsTab from "../DeliverySettingsTab";

const CHANNELS = [
  {
    channel: "EMAIL",
    active: true,
    status: "NOT_SET_UP",
    host: null,
    port: null,
    security: "STARTTLS",
    username: null,
    secretSet: true,
    senderAddress: null,
    editable: true,
  },
  {
    channel: "SMS",
    active: true,
    status: "NOT_SET_UP",
    editable: false,
    secretSet: false,
  },
  {
    channel: "WHATSAPP",
    active: true,
    status: "FAILING",
    lastError: "Authentication failed: check the username and password",
    lastSuccessAt: "2026-10-15T10:00:00+03:00",
    lastFailureAt: "2026-10-16T10:00:00+03:00",
    editable: true,
    secretSet: true,
  },
];

const SETTINGS = {
  retryDelays: [1, 5, 15],
  defaultFormat: "MASKED",
  holdMinutes: 10,
  sendPartials: true,
  labContact: "",
  location: "LOCAL",
  centralAddress: "",
  secureLinks: false,
  linkDays: 14,
  countryCode: "",
};

const calls = [];
const addNotification = vi.fn();

vi.mock("../../../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn((url, callback) => {
    calls.push(url);
    if (url === "/rest/delivery/channels") {
      callback(CHANNELS);
    } else if (url === "/rest/delivery/settings") {
      callback(SETTINGS);
    } else {
      callback(null);
    }
  }),
  putToOpenElisServerJsonResponse: vi.fn((url, body, callback) => {
    calls.push(`PUT ${url} ${body}`);
    if (url === "/rest/delivery/settings") {
      callback(SETTINGS);
    } else if (JSON.parse(body).senderAddress === "not-an-address") {
      callback({ field: "senderAddress", status: 400 });
    } else {
      callback({ ...CHANNELS[0], ...JSON.parse(body), status: "READY" });
    }
  }),
  postToOpenElisServerJsonResponse: vi.fn((url, body, callback) => {
    calls.push(`POST ${url} ${body}`);
    callback({
      success: false,
      message: "Could not reach smtp.example.org on port 587",
      sentMessageId: 9,
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

const wrap = (node) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      {node}
    </IntlProvider>,
  );

describe("ChannelsTab", () => {
  beforeEach(() => {
    calls.length = 0;
    addNotification.mockClear();
  });

  it("lists the three channels with their status and last error", async () => {
    wrap(<ChannelsTab />);
    await waitFor(() =>
      expect(screen.getByTestId("channel-row-EMAIL")).toBeInTheDocument(),
    );
    expect(screen.getByTestId("channel-status-EMAIL")).toHaveTextContent(
      "Not set up",
    );
    expect(screen.getByTestId("channel-status-WHATSAPP")).toHaveTextContent(
      "Failing",
    );
    expect(
      screen.getByText(
        "Authentication failed: check the username and password",
      ),
    ).toBeInTheDocument();
    expect(screen.getAllByText(/Never/).length).toBeGreaterThan(0);
  });

  it("edits Email without ever showing the stored password and keeps it when the field stays blank", async () => {
    wrap(<ChannelsTab />);
    await waitFor(() =>
      expect(screen.getByTestId("channel-edit-EMAIL")).toBeInTheDocument(),
    );
    fireEvent.click(screen.getByTestId("channel-edit-EMAIL"));
    const password = screen.getByLabelText(/Password \(Set\)/);
    expect(password).toHaveValue("");
    expect(
      screen.getByText("A secret is stored. Leave this blank to keep it."),
    ).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText("SMTP host"), {
      target: { value: "smtp.example.org" },
    });
    fireEvent.change(screen.getByLabelText("Port"), {
      target: { value: "587" },
    });
    fireEvent.change(screen.getByLabelText("Sender address"), {
      target: { value: "lab@example.org" },
    });

    fireEvent.click(screen.getByTestId("channel-save-EMAIL"));

    await waitFor(() =>
      expect(
        calls.some((call) =>
          call.startsWith("PUT /rest/delivery/channels/email"),
        ),
      ).toBe(true),
    );
    const put = calls.find((call) =>
      call.startsWith("PUT /rest/delivery/channels/email"),
    );
    const body = JSON.parse(
      put.replace("PUT /rest/delivery/channels/email ", ""),
    );
    expect(body.host).toBe("smtp.example.org");
    expect(body.port).toBe(587);
    expect(body.senderAddress).toBe("lab@example.org");
    expect(body.secret).toBe("");
    expect(addNotification.mock.calls[0][0].message).toBe("Channel saved.");
  });

  it("names the field the server refused and marks it", async () => {
    wrap(<ChannelsTab />);
    await waitFor(() =>
      expect(screen.getByTestId("channel-edit-EMAIL")).toBeInTheDocument(),
    );
    fireEvent.click(screen.getByTestId("channel-edit-EMAIL"));
    fireEvent.change(screen.getByLabelText("SMTP host"), {
      target: { value: "smtp.example.org" },
    });
    fireEvent.change(screen.getByLabelText("Sender address"), {
      target: { value: "not-an-address" },
    });
    fireEvent.click(screen.getByTestId("channel-save-EMAIL"));

    await waitFor(() => expect(addNotification).toHaveBeenCalled());
    expect(addNotification.mock.calls[0][0].kind).toBe("error");
    expect(addNotification.mock.calls[0][0].message).toContain(
      "Sender address",
    );
    expect(screen.getByLabelText("Sender address")).toHaveAttribute(
      "aria-invalid",
      "true",
    );
  });

  it("refuses a port that is not a number before saving", async () => {
    wrap(<ChannelsTab />);
    await waitFor(() =>
      expect(screen.getByTestId("channel-edit-EMAIL")).toBeInTheDocument(),
    );
    fireEvent.click(screen.getByTestId("channel-edit-EMAIL"));
    fireEvent.change(screen.getByLabelText("Port"), {
      target: { value: "abc" },
    });
    fireEvent.click(screen.getByTestId("channel-save-EMAIL"));

    await waitFor(() => expect(addNotification).toHaveBeenCalled());
    expect(addNotification.mock.calls[0][0].message).toContain("Port");
    expect(
      calls.some((call) => call.startsWith("PUT /rest/delivery/channels")),
    ).toBe(false);
  });

  it("sends a test message and shows the server's plain-language outcome", async () => {
    wrap(<ChannelsTab />);
    await waitFor(() =>
      expect(screen.getByTestId("channel-test-EMAIL")).toBeInTheDocument(),
    );
    fireEvent.click(screen.getByTestId("channel-test-EMAIL"));
    fireEvent.change(screen.getByLabelText("Address"), {
      target: { value: "me@example.org" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Send" }));
    await waitFor(() =>
      expect(screen.getByTestId("channel-test-outcome")).toBeInTheDocument(),
    );
    expect(screen.getByTestId("channel-test-outcome")).toHaveTextContent(
      "Could not reach smtp.example.org on port 587",
    );
    expect(calls).toContain(
      'POST /rest/delivery/channels/email/test {"address":"me@example.org"}',
    );
  });
});

describe("DeliverySettingsTab", () => {
  beforeEach(() => {
    calls.length = 0;
    addNotification.mockClear();
  });

  it("loads the settings and saves the parsed retry delays", async () => {
    wrap(<DeliverySettingsTab />);
    await waitFor(() =>
      expect(screen.getByTestId("delivery-settings-save")).toBeInTheDocument(),
    );
    const delays = screen.getByLabelText("Retry after (minutes, in order)");
    expect(delays).toHaveValue("1,5,15");
    fireEvent.change(delays, { target: { value: "2, 10" } });
    fireEvent.change(screen.getByLabelText("Lab contact line"), {
      target: { value: "CPHL 7000 0000" },
    });

    fireEvent.click(screen.getByTestId("delivery-settings-save"));

    await waitFor(() =>
      expect(
        calls.some((call) => call.startsWith("PUT /rest/delivery/settings")),
      ).toBe(true),
    );
    const body = JSON.parse(
      calls
        .find((call) => call.startsWith("PUT /rest/delivery/settings"))
        .replace("PUT /rest/delivery/settings ", ""),
    );
    expect(body.retryDelays).toEqual([2, 10]);
    expect(body.labContact).toBe("CPHL 7000 0000");
    expect(body.location).toBe("LOCAL");
    expect(addNotification.mock.calls[0][0].message).toBe("Settings saved.");
  });

  it("refuses retry delays that are not positive minutes", async () => {
    wrap(<DeliverySettingsTab />);
    await waitFor(() =>
      expect(screen.getByTestId("delivery-settings-save")).toBeInTheDocument(),
    );
    fireEvent.change(screen.getByLabelText("Retry after (minutes, in order)"), {
      target: { value: "0,x" },
    });
    fireEvent.click(screen.getByTestId("delivery-settings-save"));
    expect(calls.some((call) => call.startsWith("PUT"))).toBe(false);
    expect(
      screen.getByLabelText("Retry after (minutes, in order)"),
    ).toBeInvalid();
  });
});
