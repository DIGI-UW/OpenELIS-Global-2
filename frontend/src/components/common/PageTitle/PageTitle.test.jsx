import React from "react";
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
const renderWithIntl = (component) => {
  return render(
    <BrowserRouter>
      <IntlProvider locale="en" messages={messages}>
        {component}
      </IntlProvider>
    </BrowserRouter>,
  );
};

describe("PageTitle Component", () => {
  it("should render simple breadcrumb without back arrow", () => {
    renderWithIntl(<PageTitle breadcrumbs={[{ label: "Analyzers" }]} />);

    expect(screen.getByText("Analyzers")).not.toBeNull();
    expect(screen.queryByTestId("page-title-back-button")).toBeNull();
  });

  it("should render hierarchical breadcrumbs with separator", () => {
    renderWithIntl(
      <PageTitle
        breadcrumbs={[
          { label: "Analyzers", link: "/analyzers" },
          { label: "Field Mappings" },
          { label: "Hematology Analyzer 1" },
        ]}
      />,
    );

    expect(screen.getByText("Analyzers")).not.toBeNull();
    expect(screen.getByText("Field Mappings")).not.toBeNull();
    expect(screen.getByText("Hematology Analyzer 1")).not.toBeNull();

    // Check separators
    const separators = screen.getAllByText(">");
    expect(separators).toHaveLength(2);
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

  it("should render clickable breadcrumb links", async () => {
    renderWithIntl(
      <PageTitle
        breadcrumbs={[
          { label: "Analyzers", link: "/analyzers" },
          { label: "Field Mappings" },
        ]}
      />,
    );

    const link = screen.getByTestId("breadcrumb-link-0");
    expect(link).not.toBeNull();
    await userEvent.click(link);
    // Navigation verified by router (not tested here)
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
    const mockOnBack = jest.fn();
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
