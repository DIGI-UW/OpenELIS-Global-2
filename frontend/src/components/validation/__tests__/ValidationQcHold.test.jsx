import React from "react";
import { render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../languages/en.json";
import Validation from "../Validation";
import { ConfigurationContext, NotificationContext } from "../../layout/Layout";

/**
 * OGC-1147 — the QC-hold annotation on a validation row.
 *
 * The regression these cover: the reason sentence must actually render on the
 * held row, not sit in a title prop Carbon's Tag silently discards.
 */
const renderWithIntl = (component) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      {component}
    </IntlProvider>,
  );

const renderValidation = (results) =>
  renderWithIntl(
    <ConfigurationContext.Provider
      value={{ configurationProperties: { AccessionFormat: "SITEYEARNUM" } }}
    >
      <NotificationContext.Provider
        value={{
          setNotificationVisible: () => {},
          addNotification: () => {},
        }}
      >
        <Validation params="" results={results} />
      </NotificationContext.Provider>
    </ConfigurationContext.Provider>,
  );

const heldRow = {
  id: "0",
  analysisId: "1201",
  accessionNumber: "DEV01260000000000010",
  testName: "WBC(Whole blood)",
  result: "5.2",
  resultType: "N",
  qcHold: true,
  normal: true,
};

describe("Validation QC-hold", () => {
  test("the held row renders the reason sentence in the tag's popover", () => {
    renderValidation({ resultList: [heldRow] });
    // The tag itself…
    expect(screen.getAllByText("QC failed").length).toBeGreaterThan(0);
    // …and the full reason, rendered in the DOM as DefinitionTooltip content —
    // the old title-prop approach left this sentence nowhere on the page.
    expect(
      screen.getByText(messages["validation.qcHold.tooltip"]),
    ).toBeInTheDocument();
  });

  test("a row without a hold renders no QC-failed tag beyond the legend", () => {
    renderValidation({ resultList: [{ ...heldRow, qcHold: false }] });
    // Only the legend's tag remains — the row itself is clean. Finding the
    // rendered copy also proves the i18n keys resolve (a missing key would
    // emit the raw id instead).
    expect(screen.getAllByText("QC failed")).toHaveLength(1);
    expect(
      screen.getByText(messages["validation.legend.qcHold"]),
    ).toBeInTheDocument();
    expect(
      screen.queryByText(messages["validation.qcHold.tooltip"]),
    ).not.toBeInTheDocument();
  });
});
