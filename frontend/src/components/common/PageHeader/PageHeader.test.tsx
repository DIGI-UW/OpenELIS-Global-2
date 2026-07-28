import React from "react";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import { vi } from "vitest";
import PageHeader from "./PageHeader";

const renderHeader = (props = {}) =>
  render(
    <MemoryRouter>
      <IntlProvider
        locale="en"
        messages={{
          "page.title.back": "Back",
        }}
      >
        <PageHeader
          breadcrumbs={[
            { label: "Analyzers", link: "/analyzers" },
            { label: "Verify" },
          ]}
          subtitle="Review mappings"
          {...props}
        />
      </IntlProvider>
    </MemoryRouter>,
  );

describe("PageHeader", () => {
  it("renders a semantic heading and linked breadcrumb path", () => {
    renderHeader();

    expect(
      screen.getByRole("heading", { level: 1, name: "Verify" }),
    ).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Analyzers" })).toHaveAttribute(
      "href",
      "/analyzers",
    );
    expect(screen.getByText("Review mappings")).toBeInTheDocument();
  });

  it("renders page actions in the shared header", () => {
    renderHeader({ actions: <button type="button">Add analyzer</button> });

    expect(
      screen.getByRole("button", { name: "Add analyzer" }),
    ).toBeInTheDocument();
  });

  it("uses the explicit back action when supplied", async () => {
    const onBack = vi.fn();
    renderHeader({ showBackArrow: true, onBack });

    await userEvent.click(screen.getByRole("button", { name: "Back" }));
    expect(onBack).toHaveBeenCalledOnce();
  });
});
