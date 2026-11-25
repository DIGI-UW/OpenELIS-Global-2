import React from "react";
import { render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../languages/en.json";
import { ConfigurationContext } from "../layout/contexts";
import useSiteDateFormat from "./useSiteDateFormat";

function Shown({ value, withTime }) {
  const siteDate = useSiteDateFormat();
  return (
    <output>
      {withTime
        ? siteDate.formatIsoDateTime(value)
        : siteDate.formatIsoDate(value)}
    </output>
  );
}

const show = (value, dateLocale, withTime = false) =>
  render(
    <ConfigurationContext.Provider
      value={{ configurationProperties: { DEFAULT_DATE_LOCALE: dateLocale } }}
    >
      <IntlProvider locale="en" messages={messages}>
        <Shown value={value} withTime={withTime} />
      </IntlProvider>
    </ConfigurationContext.Provider>,
  );

describe("useSiteDateFormat formatIsoDate (OGC-1657)", () => {
  test("shows a stored date day first on a dd/MM/yyyy site", () => {
    show("2026-11-05", "fr-FR");
    expect(screen.getByRole("status")).toHaveTextContent("05/11/2026");
  });

  test("shows a stored date month first on a MM/dd/yyyy site", () => {
    show("2026-11-05", "en-US");
    expect(screen.getByRole("status")).toHaveTextContent("11/05/2026");
  });

  test("shows the day of a stored timestamp", () => {
    show("2026-11-05 00:00:00.0", "fr-FR");
    expect(screen.getByRole("status")).toHaveTextContent(/^05\/11\/2026$/);
  });

  test.each([
    ["2026-02-30", "2026-02-30"],
    ["05/11/2026", "05/11/2026"],
    ["", ""],
    [null, ""],
    [undefined, ""],
  ])("leaves %j that is no stored date as %j", (value, expected) => {
    show(value, "fr-FR");
    expect(screen.getByRole("status").textContent).toBe(expected);
  });
});

describe("useSiteDateFormat formatIsoDateTime (OGC-1657)", () => {
  test.each([
    ["2026-11-05 10:00", "fr-FR", "05/11/2026 10:00"],
    ["2026-11-05T10:00:00", "fr-FR", "05/11/2026 10:00"],
    ["2026-11-05 10:00", "en-US", "11/05/2026 10:00"],
    ["2026-11-05", "fr-FR", "05/11/2026"],
    ["10:00", "fr-FR", "10:00"],
    ["", "fr-FR", ""],
  ])("shows %j on a %s site as %j", (value, dateLocale, expected) => {
    show(value, dateLocale, true);
    expect(screen.getByRole("status").textContent).toBe(expected);
  });
});
