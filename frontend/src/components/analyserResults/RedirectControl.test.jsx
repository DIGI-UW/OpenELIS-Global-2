import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { vi } from "vitest";
import messages from "../../languages/en.json";
import RedirectControl from "./RedirectControl";

const show = (row, onChange = vi.fn()) => {
  render(
    <IntlProvider locale="en" messages={messages}>
      <RedirectControl row={row} onChange={onChange} />
    </IntlProvider>,
  );
  return onChange;
};

describe("RedirectControl", () => {
  it("stays collapsed until the reviewer asks to place the result elsewhere", () => {
    show({ id: "1001" });
    expect(
      screen.queryByLabelText("Order (lab number)"),
    ).not.toBeInTheDocument();
    fireEvent.click(
      screen.getByRole("button", { name: "Place on another order" }),
    );
    expect(screen.getByLabelText("Order (lab number)")).toBeInTheDocument();
  });

  it("reports the order and the reason as they are typed", () => {
    const onChange = show({ id: "1001" });
    fireEvent.click(
      screen.getByRole("button", { name: "Place on another order" }),
    );

    fireEvent.change(screen.getByLabelText("Order (lab number)"), {
      target: { value: "DEV01260000000000038" },
    });
    fireEvent.change(screen.getByLabelText("Why this belongs to that order"), {
      target: { value: "Label misread" },
    });

    expect(onChange).toHaveBeenCalledWith(
      "redirectAccession",
      "DEV01260000000000038",
      "1001",
    );
    expect(onChange).toHaveBeenCalledWith(
      "redirectReason",
      "Label misread",
      "1001",
    );
  });

  it("asks for the reason once an order is entered", () => {
    show({ id: "1001", redirectAccession: "DEV01260000000000038" });
    expect(
      screen.getByText("Enter the reason before saving."),
    ).toBeInTheDocument();
  });

  it("opens already expanded when the row carries a restored redirect", () => {
    show({
      id: "1001",
      redirectAccession: "DEV01260000000000038",
      redirectReason: "Label misread",
    });
    expect(screen.getByLabelText("Order (lab number)")).toHaveValue(
      "DEV01260000000000038",
    );
    expect(
      screen.queryByText("Enter the reason before saving."),
    ).not.toBeInTheDocument();
  });
});
