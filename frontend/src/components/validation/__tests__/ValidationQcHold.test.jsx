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
 * Copy-level assertions cover the words a technician reads; the mounted-component
 * test covers the DEF-1 regression — the reason sentence must actually render on
 * the held row, not sit in a title prop Carbon's Tag silently discards.
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

describe("Validation QC-hold copy", () => {
  test("the tooltip says what happened and what to do about it", () => {
    const tooltip = messages["validation.qcHold.tooltip"];
    // The existing nonconforming marker is a bare GIF with a legend. A hold that a
    // tech cannot explain gets overridden out of confusion, which would defeat the
    // whole patient-safety argument for the signal.
    expect(tooltip).toMatch(/control run/i);
    expect(tooltip).toMatch(/before releasing/i);
  });

  test("the legend distinguishes a QC hold from a nonconforming sample", () => {
    expect(messages["validation.legend.qcHold"]).toMatch(/quality control/i);
    expect(messages["validation.legend.qcHold"]).toMatch(/still open/i);
    // Must not be confused with the sample-level nonconformity marker.
    expect(messages["validation.label.nonconform"]).not.toEqual(
      messages["validation.legend.qcHold"],
    );
  });

  test("the blocked-save message tells the tech both ways out", () => {
    const blocked = messages["validation.qcHold.blocked"];
    expect(blocked).toMatch(/close the non-conformity/i);
    expect(blocked).toMatch(/reject/i);
  });

  test("the site-flag help text explains warn-only versus blocking", () => {
    const help = messages["instructions.qc.fail.blocks.validation"];
    expect(help).toMatch(/cannot be accepted/i);
    expect(help).toMatch(/can still be rejected/i);
    expect(help).toMatch(/warning only/i);
  });

  test("the held row renders the reason sentence in the tag's popover (DEF-1)", () => {
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
