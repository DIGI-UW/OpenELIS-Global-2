import React from "react";
import { render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../languages/en.json";
import { RequiredMarker, requiredProps } from "./RequiredMarker";

/**
 * OGC-1240: the asterisk is the visual cue only. It is hidden from assistive
 * technology, which learns about the requirement from aria-required, or from
 * the announced word where there is no single input to carry it.
 */
const renderMarker = (props) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <label>
        Lab Number
        <RequiredMarker {...props} />
      </label>
    </IntlProvider>,
  );

describe("RequiredMarker", () => {
  it("shows an asterisk that screen readers skip", () => {
    const { container } = renderMarker({});

    const star = container.querySelector(".requiredlabel");
    expect(star).toHaveTextContent("*");
    expect(star).toHaveAttribute("aria-hidden", "true");
    expect(screen.queryByText("required")).not.toBeInTheDocument();
  });

  it("announces the requirement where there is no input to carry it", () => {
    renderMarker({ announce: true });

    expect(screen.getByText("required")).toHaveClass("cds--visually-hidden");
  });

  it("shows nothing for a field that is not required", () => {
    const { container } = renderMarker({ required: false, announce: true });

    expect(container.querySelector(".requiredlabel")).toBeNull();
    expect(screen.queryByText("required")).not.toBeInTheDocument();
  });

  it("gives a required input aria-required and an optional one nothing", () => {
    expect(requiredProps()).toEqual({ "aria-required": true });
    expect(requiredProps(true)).toEqual({ "aria-required": true });
    expect(requiredProps(false)).toEqual({});
  });
});
