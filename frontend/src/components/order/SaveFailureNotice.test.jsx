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

import SaveFailureNotice from "./SaveFailureNotice";

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
        "sampleOrderItems: Enter at least one of Requesting Organization or Requester contact.",
      ),
    ).toBeInTheDocument();
    expect(screen.queryByText(/errors\./)).not.toBeInTheDocument();
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
