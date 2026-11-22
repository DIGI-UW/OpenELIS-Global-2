import React, { type ReactElement } from "react";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { IntlProvider } from "react-intl";
import { BrowserRouter } from "react-router-dom";
import PageTitle from "./PageTitle";

// Mock messages
const messages = {
  "page.breadcrumb.separator": ">",
  "page.title.back": "Back",
};

// Helper to render with providers
const renderWithIntl = (component: ReactElement) => {
  return render(
    <BrowserRouter>
      <IntlProvider locale="en" messages={messages}>
        {component}
      </IntlProvider>
    </BrowserRouter>,
  );
};

describe("PageTitle Component", () => {
  it("should render the current page name as the page heading", () => {
    renderWithIntl(<PageTitle breadcrumbs={[{ label: "Analyzers" }]} />);

    expect(screen.getByRole("heading", { name: "Analyzers" })).not.toBeNull();
    expect(screen.queryByTestId("page-title-back-button")).toBeNull();
  });

  it("should render only the last segment, leaving the trail to the breadcrumb", () => {
    renderWithIntl(
      <PageTitle
        breadcrumbs={[
          { label: "Analyzers", link: "/analyzers" },
          { label: "Field Mappings" },
          { label: "Hematology Analyzer 1" },
        ]}
      />,
    );

    expect(
      screen.getByRole("heading", { name: "Hematology Analyzer 1", level: 1 }),
    ).not.toBeNull();
    expect(screen.queryByText("Analyzers")).toBeNull();
    expect(screen.queryByText("Field Mappings")).toBeNull();
    expect(screen.queryByText(">")).toBeNull();
  });

  it("should render back arrow when showBackArrow is true", () => {
    renderWithIntl(
      <PageTitle
        breadcrumbs={[
          { label: "Analyzers", link: "/analyzers" },
          { label: "Field Mappings" },
        ]}
        showBackArrow={true}
      />,
    );

    expect(screen.getByTestId("page-title-back-button")).not.toBeNull();
  });

  it("should render subtitle when provided", () => {
    renderWithIntl(
      <PageTitle
        breadcrumbs={[{ label: "Analyzers" }]}
        subtitle="Configure analyzer settings"
      />,
    );

    expect(screen.getByText("Configure analyzer settings")).not.toBeNull();
  });

  it("should call custom onBack handler when provided", async () => {
    const mockOnBack = vi.fn();
    renderWithIntl(
      <PageTitle
        breadcrumbs={[
          { label: "Analyzers", link: "/analyzers" },
          { label: "Field Mappings" },
        ]}
        showBackArrow={true}
        onBack={mockOnBack}
      />,
    );

    const backButton = screen.getByTestId("page-title-back-button");
    await userEvent.click(backButton);
    expect(mockOnBack).toHaveBeenCalledTimes(1);
  });
});
