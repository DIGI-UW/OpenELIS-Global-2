import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import messages from "../../languages/en.json";

/**
 * A save the server refuses is reported as refused, with the server's reason,
 * never as "saved": a test whose analyzer value waits in a run is refused with
 * 409 (OGC-1200).
 */

const { utilsMock, notify } = vi.hoisted(() => ({
  utilsMock: {
    getFromOpenElisServer: vi.fn(),
    postToOpenElisServerJsonResponse: vi.fn(),
  },
  notify: { addNotification: vi.fn(), setNotificationVisible: vi.fn() },
}));

vi.mock("../utils/Utils", async (importOriginal) => ({
  ...(await importOriginal()),
  ...utilsMock,
}));

vi.mock("../layout/Layout", () => ({
  NotificationContext: React.createContext({
    notificationVisible: false,
    ...notify,
  }),
  ConfigurationContext: React.createContext({ configurationProperties: {} }),
}));

vi.mock("../common/CustomNotification", () => ({
  AlertDialog: () => <div />,
  NotificationKinds: { success: "success", error: "error", warning: "warning" },
}));

import GenericSampleResults from "./GenericSampleResults";

const ROW = {
  accessionNumber: "DEV0126000000000400",
  sequenceNumber: "1",
  testName: "Glucose",
  analysisId: "61",
  testId: "5",
  resultType: "N",
  resultValue: "",
};

const renderAndSave = (saveResponse) => {
  utilsMock.getFromOpenElisServer.mockImplementation((url, callback) =>
    callback(
      url.startsWith("/rest/LogbookResults") ? { testResult: [ROW] } : [],
    ),
  );
  utilsMock.postToOpenElisServerJsonResponse.mockImplementation(
    (_url, _body, callback) => callback(saveResponse),
  );
  render(
    <IntlProvider locale="en" messages={messages}>
      <MemoryRouter>
        <GenericSampleResults showBreadcrumbs={false} />
      </MemoryRouter>
    </IntlProvider>,
  );
  const labNumber = screen.getAllByRole("textbox")[0];
  fireEvent.change(labNumber, { target: { value: ROW.accessionNumber } });
  fireEvent.click(screen.getAllByRole("button", { name: /search/i })[0]);
};

describe("GenericSampleResults save outcome", () => {
  beforeEach(() => {
    utilsMock.getFromOpenElisServer.mockReset();
    utilsMock.postToOpenElisServerJsonResponse.mockReset();
    notify.addNotification.mockReset();
  });

  it("reports a refused save as refused, with the server's reason", async () => {
    renderAndSave({
      status: 409,
      error: "An analyzer value for this test is waiting in RUN-7.",
    });
    fireEvent.click(await screen.findByRole("button", { name: "Save" }));

    expect(notify.addNotification).toHaveBeenCalledWith(
      expect.objectContaining({
        kind: "error",
        message: "An analyzer value for this test is waiting in RUN-7.",
      }),
    );
    expect(notify.addNotification).not.toHaveBeenCalledWith(
      expect.objectContaining({ kind: "success" }),
    );
  });

  it("reports a save the server accepted as saved", async () => {
    renderAndSave({});
    fireEvent.click(await screen.findByRole("button", { name: "Save" }));

    expect(notify.addNotification).toHaveBeenCalledWith(
      expect.objectContaining({ kind: "success" }),
    );
  });
});
