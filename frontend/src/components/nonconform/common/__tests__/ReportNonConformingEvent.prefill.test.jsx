import React from "react";
import { act, render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import messages from "../../../../languages/en.json";
import { ReportNonConformingEvent } from "../ReportNonConformingEvent";
import { NotificationContext } from "../../../layout/Layout";
import { getFromOpenElisServer } from "../../../utils/Utils";

vi.mock("../../../utils/Utils", async (importOriginal) => {
  const actual = await importOriginal();
  return {
    ...actual,
    getFromOpenElisServer: vi.fn(),
    postToOpenElisServerJsonResponse: vi.fn(),
    postToOpenElisServerFormData: vi.fn(),
  };
});

const renderAt = async (url) => {
  await act(async () =>
    render(
      <IntlProvider locale="en" messages={messages}>
        <NotificationContext.Provider
          value={{
            notificationVisible: false,
            setNotificationVisible: vi.fn(),
            addNotification: vi.fn(),
          }}
        >
          <MemoryRouter initialEntries={[url]}>
            <ReportNonConformingEvent />
          </MemoryRouter>
        </NotificationContext.Provider>
      </IntlProvider>,
    ),
  );
};

describe("ReportNonConformingEvent pre-fill", () => {
  beforeEach(() => {
    getFromOpenElisServer.mockReset();
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url.startsWith("/rest/nonconformevents?")) {
        callback([
          {
            id: "1",
            labOrderNumber: "DEV01",
            sampleItems: [{ id: "5", number: "1", type: "Serum" }],
          },
        ]);
      } else if (url === "/rest/nce/generate-number") {
        callback({ nceNumber: "NCE-1" });
      } else {
        callback([]);
      }
    });
  });

  it("searches the order and drafts the description from the link", async () => {
    await renderAt(
      "/ReportNonConformingEvent?labNumber=DEV01&description=Patient%20sex%20not%20recorded",
    );
    expect(getFromOpenElisServer).toHaveBeenCalledWith(
      "/rest/nonconformevents?labNumber=DEV01",
      expect.any(Function),
    );
    expect(screen.getByDisplayValue("Patient sex not recorded")).toBeVisible();
    expect(screen.getByText("Serum (DEV01-1)")).toBeInTheDocument();
  });

  it("opens empty without a link", async () => {
    await renderAt("/ReportNonConformingEvent");
    expect(getFromOpenElisServer).not.toHaveBeenCalledWith(
      expect.stringMatching(/^\/rest\/nonconformevents\?/),
      expect.any(Function),
    );
  });
});
