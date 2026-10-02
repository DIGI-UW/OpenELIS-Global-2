/**
 * OGC-1418 — Validation's one search. However the page is reached (the menu,
 * the breadcrumb, a bare /validation or an old address), the whole search
 * area shows; the Lab Unit list holds only the units the user validates;
 * criteria combine, are kept in the address, and clearing one keeps the rest.
 */
import React from "react";
import { vi } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../languages/en.json";
import SearchForm from "./SearchForm";
import { ConfigurationContext, NotificationContext } from "../layout/Layout";

vi.mock("../utils/Utils", async (importOriginal) => {
  const actual = await importOriginal();
  return { ...actual, getFromOpenElisServer: vi.fn() };
});

vi.mock("../patient/SearchPatientForm", () => ({
  default: ({ getSelectedPatient }) => (
    <button
      type="button"
      data-testid="pick-patient"
      onClick={() =>
        getSelectedPatient({
          patientPK: "42",
          firstName: "Ida",
          lastName: "Walker",
        })
      }
    >
      pick
    </button>
  ),
}));

import { getFromOpenElisServer } from "../utils/Utils";

const UNITS = [
  { id: "7", value: "Biochemistry" },
  { id: "9", value: "Hematology" },
];

const queueCalls = () =>
  getFromOpenElisServer.mock.calls
    .map(([url]) => url)
    .filter((url) => url.startsWith("/rest/AccessionValidation?"));

const lastQueueParams = () =>
  Object.fromEntries(
    new URLSearchParams(queueCalls()[queueCalls().length - 1].split("?")[1]),
  );

const renderAt = (address) => {
  window.history.pushState({}, "", address);
  const setParams = vi.fn();
  render(
    <ConfigurationContext.Provider
      value={{ configurationProperties: { DEFAULT_DATE_LOCALE: "fr-FR" } }}
    >
      <NotificationContext.Provider
        value={{ setNotificationVisible: vi.fn(), addNotification: vi.fn() }}
      >
        <IntlProvider locale="en" messages={messages}>
          <SearchForm setResults={vi.fn()} setParams={setParams} />
        </IntlProvider>
      </NotificationContext.Provider>
    </ConfigurationContext.Provider>,
  );
  return { setParams };
};

describe("Validation one-page search (OGC-1418)", () => {
  beforeEach(() => {
    getFromOpenElisServer.mockReset();
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url === "/rest/user-test-sections/Validation") {
        return callback(UNITS);
      }
      return callback({ resultList: [{ analysisId: "1" }] });
    });
  });

  it("shows the whole search area on a bare /validation, and loads nothing yet", () => {
    renderAt("/validation");

    expect(screen.getByTestId("validation-search-area")).toBeInTheDocument();
    expect(document.querySelector("#validationSearch")).not.toBeNull();
    expect(screen.getByLabelText("Lab Unit")).toBeInTheDocument();
    expect(
      document.querySelector('label[for="validationFromDate"]'),
    ).not.toBeNull();
    expect(
      document.querySelector('label[for="validationToDate"]'),
    ).not.toBeNull();
    expect(
      screen.getByTestId("validation-search-by-patient"),
    ).toBeInTheDocument();
    expect(queueCalls()).toHaveLength(0);
  });

  it("lists only the Lab Units the user validates", () => {
    renderAt("/validation");

    const options = [...screen.getByLabelText("Lab Unit").options]
      .map((option) => option.textContent)
      .filter(Boolean);
    expect(options).toEqual(["Biochemistry", "Hematology"]);
    expect(getFromOpenElisServer.mock.calls[0][0]).toBe(
      "/rest/user-test-sections/Validation",
    );
  });

  it("combines Lab Unit and a lab number range, keeps both in the address, and keeps the unit when the box is cleared", () => {
    const { setParams } = renderAt("/validation");

    fireEvent.change(screen.getByLabelText("Lab Unit"), {
      target: { value: "7" },
    });
    expect(lastQueueParams()).toMatchObject({
      testSectionId: "7",
      labNumberFrom: "",
    });
    expect(window.location.search).toBe("?testSectionId=7");

    const box = document.querySelector("#validationSearch");
    fireEvent.change(box, { target: { value: "ACC1..ACC9" } });
    fireEvent.keyDown(box, { key: "Enter" });
    expect(lastQueueParams()).toMatchObject({
      testSectionId: "7",
      labNumberFrom: "ACC1",
      labNumberTo: "ACC9",
    });
    expect(new URLSearchParams(window.location.search).get("labNumber")).toBe(
      "ACC1..ACC9",
    );
    expect(setParams).toHaveBeenLastCalledWith(
      "?labNumber=ACC1..ACC9&testSectionId=7",
    );

    fireEvent.change(box, { target: { value: "" } });
    fireEvent.keyDown(box, { key: "Enter" });
    expect(lastQueueParams()).toMatchObject({
      testSectionId: "7",
      labNumberFrom: "",
    });
    expect(window.location.search).toBe("?testSectionId=7");
  });

  it("adds a chosen patient to the search, and clearing it keeps the rest", () => {
    renderAt("/validation?testSectionId=7");

    fireEvent.click(screen.getByTestId("validation-search-by-patient"));
    fireEvent.click(screen.getByTestId("pick-patient"));
    expect(lastQueueParams()).toMatchObject({
      testSectionId: "7",
      patientId: "42",
    });
    expect(screen.getByTestId("validation-selected-patient")).toHaveTextContent(
      "Ida Walker",
    );

    fireEvent.click(screen.getByTestId("validation-clear-patient"));
    expect(lastQueueParams()).toMatchObject({
      testSectionId: "7",
      patientId: "",
    });
  });

  it.each([
    [
      "/validation?type=order&accessionNumber=ACC1",
      { labNumberFrom: "ACC1", labNumberTo: "ACC1" },
      "?labNumber=ACC1",
    ],
    [
      "/AccessionValidationRange?accessionNumber=ACC1",
      { labNumberFrom: "ACC1", labNumberTo: "" },
      "?labNumber=ACC1..",
    ],
    [
      "/ResultValidation?testSectionId=9",
      { testSectionId: "9" },
      "?testSectionId=9",
    ],
    [
      "/ResultValidationByTestDate?date=01/09/2026",
      { fromDate: "01/09/2026", toDate: "01/09/2026" },
      "?fromDate=01%2F09%2F2026&toDate=01%2F09%2F2026",
    ],
  ])(
    "opens the old address %s on the one page with the equivalent filter",
    (address, expected, normalized) => {
      renderAt(address);

      expect(queueCalls()).toHaveLength(1);
      expect(lastQueueParams()).toMatchObject(expected);
      expect(window.location.pathname).toBe("/validation");
      expect(window.location.search).toBe(normalized);
    },
  );
});
