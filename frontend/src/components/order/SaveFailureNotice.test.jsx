import React from "react";
import { render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { vi, describe, it, expect } from "vitest";
import messages from "../../languages/en.json";

const { orderContextValue } = vi.hoisted(() => ({
  orderContextValue: { saveStatus: "saved", error: null, fieldErrors: {} },
}));
vi.mock("./OrderContext", () => ({
  useOrderContext: () => orderContextValue,
  SaveStatus: {
    SAVED: "saved",
    SAVING: "saving",
    ERROR: "error",
    UNSAVED: "unsaved",
  },
}));

import SaveFailureNotice, { saveFailureMessage } from "./SaveFailureNotice";
import { createIntl } from "react-intl";

const renderNotice = (inlineFields) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <SaveFailureNotice inlineFields={inlineFields} />
    </IntlProvider>,
  );

describe("SaveFailureNotice", () => {
  it("is absent unless a save was blocked", () => {
    orderContextValue.saveStatus = "saved";
    renderNotice([]);
    expect(
      screen.queryByText("The order was not saved"),
    ).not.toBeInTheDocument();
  });

  it("shows the message and the fields not already marked inline", () => {
    orderContextValue.saveStatus = "error";
    orderContextValue.error = "Validation failed";
    orderContextValue.fieldErrors = {
      "sampleOrderItems.labNo": "must not be blank",
      "patientProperties.lastName": "required",
    };
    renderNotice(["sampleOrderItems.labNo"]);
    expect(screen.getByText("The order was not saved")).toBeInTheDocument();
    expect(screen.getByText(/Validation failed/)).toBeInTheDocument();
    expect(
      screen.getByText("patientProperties.lastName: required"),
    ).toBeInTheDocument();
    expect(
      screen.queryByText(/sampleOrderItems.labNo/),
    ).not.toBeInTheDocument();
  });

  // OGC-1201 R: the validator passes the message key as its own default
  // message, so a blocked save used to read "errors.no.sample" to the user.
  it("translates a rejection the server reports as a message key", () => {
    orderContextValue.saveStatus = "error";
    orderContextValue.error = "sampleOrderItems: errors.no.sample";
    orderContextValue.fieldErrors = {
      sampleOrderItems: "errors.requester.org.or.requestor.required",
    };
    renderNotice([]);

    expect(
      screen.getByText(/Select the appropriate sample for each test\./),
    ).toBeInTheDocument();
    expect(
      screen.getByText(
        "Enter at least one of Requesting Organization or Requester contact.",
      ),
    ).toBeInTheDocument();
    expect(screen.queryByText(/errors\./)).not.toBeInTheDocument();
    expect(screen.queryByText(/sampleOrderItems/)).not.toBeInTheDocument();
  });

  // OGC-1266: the server's summary is its first field error, so an
  // environmental order refused for a missing requester said it twice.
  it("says a rejection once when the summary repeats the field error", () => {
    orderContextValue.saveStatus = "error";
    orderContextValue.error =
      "sampleOrderItems: errors.requester.org.or.requestor.required";
    orderContextValue.fieldErrors = {
      sampleOrderItems: "errors.requester.org.or.requestor.required",
    };
    renderNotice([]);

    expect(
      screen.getAllByText(
        /Enter at least one of Requesting Organization or Requester contact\./,
      ),
    ).toHaveLength(1);
  });

  it("keeps a server message that is not a known key", () => {
    orderContextValue.saveStatus = "error";
    orderContextValue.error = "Validation failed";
    orderContextValue.fieldErrors = {
      "sampleOrderItems.labNo": "must not be blank",
    };
    renderNotice([]);

    expect(screen.getByText(/Validation failed/)).toBeInTheDocument();
    expect(
      screen.getByText("sampleOrderItems.labNo: must not be blank"),
    ).toBeInTheDocument();
  });
});

// OGC-1192 walk: a refused environmental save showed the server's reason inline
// and, beside it, a toast saying "Oops, Server error please contact
// administrator".
describe("saveFailureMessage", () => {
  const intl = createIntl({ locale: "en", messages });

  it("gives the server's reason for a refused save", () => {
    expect(
      saveFailureMessage(
        intl,
        new Error(
          "sampleOrderItems.requestorFirstName: invalid name format, possibly illegal character",
        ),
      ),
    ).toBe(
      "sampleOrderItems.requestorFirstName: invalid name format, possibly illegal character",
    );
  });

  it("translates a reason the server sends as a message key", () => {
    expect(
      saveFailureMessage(
        intl,
        new Error(
          "sampleOrderItems: errors.requester.org.or.requestor.required",
        ),
      ),
    ).toBe(messages["errors.requester.org.or.requestor.required"]);
  });

  it("falls back to the generic text when the failure has no reason", () => {
    expect(saveFailureMessage(intl, new Error(""))).toBe(
      messages["server.error.msg"],
    );
    expect(saveFailureMessage(intl, undefined)).toBe(
      messages["server.error.msg"],
    );
  });
});
