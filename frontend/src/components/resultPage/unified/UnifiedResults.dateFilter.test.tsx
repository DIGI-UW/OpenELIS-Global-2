import React from "react";
import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../languages/en.json";

/**
 * The Test date filter is sent as text that the server reads in the lab's date
 * format. It was always picked and sent day first, so on a month-first site the
 * 15th of October came back as a server error and the 5th as the 10th of May.
 */

const { utilsMock, config } = vi.hoisted(() => ({
  utilsMock: {
    getFromOpenElisServer: vi.fn(),
    postToOpenElisServerJsonResponse: vi.fn(),
    postToOpenElisServerFormData: vi.fn(),
    deleteFromOpenElisServer: vi.fn(),
  },
  config: {
    configurationProperties: {
      allowResultRejection: "false",
      DEFAULT_DATE_LOCALE: "en-US",
    },
  },
}));

vi.mock("../../utils/Utils", async (importOriginal) => ({
  ...((await importOriginal()) as object),
  ...utilsMock,
}));

vi.mock("../../layout/Layout", () => ({
  NotificationContext: React.createContext({
    notificationVisible: false,
    setNotificationVisible: vi.fn(),
    addNotification: vi.fn(),
  }),
  ConfigurationContext: React.createContext(config),
}));

vi.mock("../../common/CustomNotification", () => ({
  AlertDialog: () => <div />,
  NotificationKinds: { success: "success", error: "error", warning: "warning" },
}));

vi.mock("../../esignature/ESignatureButton", () => ({
  __esModule: true,
  default: ({ children }: any) => <button type="button">{children}</button>,
  SignatureMeaning: { AUTHORED: "AUTHORED", MODIFIED: "MODIFIED" },
}));

vi.mock("../../common/PageBreadCrumb", () => ({ default: () => <div /> }));

import { ConfigurationContext } from "../../layout/Layout";
import UnifiedResults from "./UnifiedResults";

const renderPage = () => {
  utilsMock.getFromOpenElisServer.mockImplementation(
    (endPoint: string, callback: any) => {
      if (endPoint.startsWith("/rest/LogbookResults"))
        return callback({ testResult: [] });
      return callback([]);
    },
  );
  render(
    <IntlProvider locale="en" messages={messages}>
      <UnifiedResults />
    </IntlProvider>,
  );
};

const sentCollectionDate = () => {
  const calls = utilsMock.getFromOpenElisServer.mock.calls
    .map((c: any[]) => String(c[0]))
    .filter((url) => url.startsWith("/rest/LogbookResults"));
  return new URLSearchParams(calls.slice(-1)[0].split("?")[1]).get(
    "collectionDate",
  );
};

const pickTestDate = (typed: string) => {
  const date = screen.getByLabelText(/Test date/i);
  fireEvent.change(date, { target: { value: typed } });
  fireEvent.blur(date);
  fireEvent.click(screen.getByRole("button", { name: /^Load results$/i }));
};

describe("Test date filter on the unified worklist", () => {
  beforeEach(() => {
    cleanup();
    utilsMock.getFromOpenElisServer.mockReset();
    window.localStorage.clear();
    window.history.replaceState(null, "", "/Results");
  });

  it("asks for and sends the date month first on a month-first site", () => {
    config.configurationProperties.DEFAULT_DATE_LOCALE = "en-US";
    renderPage();

    expect(screen.getByLabelText(/Test date/i)).toHaveAttribute(
      "placeholder",
      "mm/dd/yyyy",
    );
    pickTestDate("10/15/2026");

    expect(sentCollectionDate()).toBe("10/15/2026");
  });

  it("reads a typed date in the site's order once the settings arrive after the first render", () => {
    utilsMock.getFromOpenElisServer.mockImplementation(
      (endPoint: string, callback: any) => {
        if (endPoint.startsWith("/rest/LogbookResults"))
          return callback({ testResult: [] });
        return callback([]);
      },
    );
    const page = (configurationProperties: Record<string, string>) => (
      <ConfigurationContext.Provider value={{ configurationProperties }}>
        <IntlProvider locale="en" messages={messages}>
          <UnifiedResults />
        </IntlProvider>
      </ConfigurationContext.Provider>
    );
    const { rerender } = render(page({}));
    rerender(page({ DEFAULT_DATE_LOCALE: "fr-FR" }));

    pickTestDate("15/10/2026");

    expect(sentCollectionDate()).toBe("15/10/2026");
  });

  it("asks for and sends the date day first on a day-first site", () => {
    config.configurationProperties.DEFAULT_DATE_LOCALE = "fr-FR";
    renderPage();

    expect(screen.getByLabelText(/Test date/i)).toHaveAttribute(
      "placeholder",
      "dd/mm/yyyy",
    );
    pickTestDate("15/10/2026");

    expect(sentCollectionDate()).toBe("15/10/2026");
  });
});
