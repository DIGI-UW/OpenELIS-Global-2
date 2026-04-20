import React from "react";
import { render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../languages/en.json";
import EQABadge from "../EQABadge";

const renderWithIntl = (component) => {
  return render(
    <IntlProvider locale="en" messages={messages}>
      {component}
    </IntlProvider>,
  );
};

describe("EQABadge", () => {
  test("renders Carbon Tag with EQA label", () => {
    renderWithIntl(<EQABadge />);
    expect(screen.getByText("EQA")).toBeTruthy();
  });

  test("renders with default blue color for standard priority", () => {
    const { container } = renderWithIntl(<EQABadge priority="STANDARD" />);
    const tag = container.querySelector(".cds--tag");
    expect(tag).toBeTruthy();
    expect(tag.className).toContain("cds--tag--blue");
  });

  test("renders with warm-gray color for urgent priority", () => {
    const { container } = renderWithIntl(<EQABadge priority="URGENT" />);
    const tag = container.querySelector(".cds--tag");
    expect(tag).toBeTruthy();
    expect(tag.className).toContain("cds--tag--warm-gray");
  });

  test("renders with red color for critical priority", () => {
    const { container } = renderWithIntl(<EQABadge priority="CRITICAL" />);
    const tag = container.querySelector(".cds--tag");
    expect(tag).toBeTruthy();
    expect(tag.className).toContain("cds--tag--red");
  });

  test("defaults to blue when no priority is provided", () => {
    const { container } = renderWithIntl(<EQABadge />);
    const tag = container.querySelector(".cds--tag");
    expect(tag).toBeTruthy();
    expect(tag.className).toContain("cds--tag--blue");
  });

  test("applies small size", () => {
    const { container } = renderWithIntl(<EQABadge />);
    const tag = container.querySelector(".cds--tag");
    expect(tag).toBeTruthy();
    expect(tag.className).toContain("cds--tag--sm");
  });
});
