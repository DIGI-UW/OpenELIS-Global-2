import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { vi } from "vitest";
import messages from "../../../languages/en.json";
import { ConfigurationContext } from "../../layout/Layout";

const { getFromOpenElisServer } = vi.hoisted(() => ({
  getFromOpenElisServer: vi.fn(),
}));

vi.mock("../../utils/Utils", () => ({ getFromOpenElisServer }));
vi.mock("../../common/CustomNotification", () => ({
  AlertDialog: () => null,
}));

import AuditTrailReport from "./AuditTrailReport";

/**
 * OGC-1448: the order audit trail names its columns in their sort
 * descriptions, reports a missing or unknown lab number with its own message,
 * and survives a failed request.
 */
const intlErrors = [];

const renderReport = () =>
  render(
    <IntlProvider
      locale="en"
      messages={messages}
      onError={(error) => intlErrors.push(error.message)}
    >
      <ConfigurationContext.Provider value={{ configurationProperties: {} }}>
        <AuditTrailReport id="sideNav.title.audittrail" />
      </ConfigurationContext.Provider>
    </IntlProvider>,
  );

const viewReport = (labNo) => {
  if (labNo) {
    fireEvent.change(screen.getByLabelText("Lab No"), {
      target: { value: labNo },
    });
  }
  fireEvent.click(screen.getByRole("button", { name: /View Report/ }));
};

beforeEach(() => {
  intlErrors.length = 0;
  getFromOpenElisServer.mockReset();
});

describe("AuditTrailReport", () => {
  it("asks for a lab number when none is given, without a missing message", () => {
    renderReport();

    viewReport();

    expect(screen.getByText("Lab No is required!")).toBeInTheDocument();
    expect(getFromOpenElisServer).not.toHaveBeenCalled();
    expect(intlErrors).toEqual([]);
  });

  it("names every column in its sort description", () => {
    getFromOpenElisServer.mockImplementation((url, callback) =>
      callback({
        log: [
          {
            timeStamp: "2026-10-01T10:00:00Z",
            itemDescription: "Sample",
            action: "Insert",
            identifier: "DEV01",
            user: "admin",
          },
        ],
      }),
    );
    renderReport();

    viewReport("DEV01");

    const descriptions = [
      ...document.querySelectorAll(".cds--table-sort__description"),
    ].map((node) => node.textContent);
    expect(descriptions.length).toBeGreaterThan(0);
    descriptions.forEach((text) => {
      expect(text).not.toContain("[object Object]");
      expect(text).not.toMatch(/sort rows by\s+header/);
    });
  });

  it("reports an unknown lab number when the request fails", () => {
    getFromOpenElisServer.mockImplementation((url, callback) =>
      callback(undefined),
    );
    renderReport();

    viewReport("DEV99");

    expect(screen.getByText("Invalid Accession Number")).toBeInTheDocument();
    expect(intlErrors).toEqual([]);
  });
});
