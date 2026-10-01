/**
 * The Validation search form in "by test date" mode gives its date input a
 * real id, so the label is bound to it and Formik can match its change events.
 */
import React from "react";
import { vi } from "vitest";
import { render } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../languages/en.json";
import SearchForm from "./SearchForm";
import { ConfigurationContext, NotificationContext } from "../layout/Layout";

vi.mock("../utils/Utils", async (importOriginal) => {
  const actual = await importOriginal();
  return { ...actual, getFromOpenElisServer: vi.fn() };
});

describe("Validation search form — by test date", () => {
  it("renders the test-date input with an id its label points to", () => {
    window.history.pushState({}, "", "/ResultValidationByTestDate");
    const { container } = render(
      <ConfigurationContext.Provider
        value={{ configurationProperties: { DEFAULT_DATE_LOCALE: "en-US" } }}
      >
        <NotificationContext.Provider
          value={{ setNotificationVisible: vi.fn(), addNotification: vi.fn() }}
        >
          <IntlProvider locale="en" messages={messages}>
            <SearchForm
              setResults={vi.fn()}
              setParams={vi.fn()}
              setIsLoading={vi.fn()}
            />
          </IntlProvider>
        </NotificationContext.Provider>
      </ConfigurationContext.Provider>,
    );

    const input = container.querySelector("#validationTestDate");
    expect(input).not.toBeNull();
    expect(
      container.querySelector('label[for="validationTestDate"]'),
    ).not.toBeNull();
  });
});
