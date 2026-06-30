import React from "react";
import { render, screen, RenderResult } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { IntlProvider } from "react-intl";
import { BrowserRouter } from "react-router-dom";
import PageTitle from "./PageTitle";

interface Breadcrumb {
  label: string;
  link?: string;
}

const messages: Record<string, string> = {
  "page.breadcrumb.separator": ">",
  "page.title.back": "Back",
};

const renderWithIntl = (component: React.ReactElement): RenderResult => {
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
    const breadcrumbs: Breadcrumb[] = [
      { label: "Analyzers", link: "/analyzers" },
      { label: "Field Mappings" },
      { label: "Hematology Analyzer 1" },
    ];

    renderWithIntl(<PageTitle breadcrumbs={breadcrumbs} />);

    expect(screen.getByText("Analyzers")).not.toBeNull();
    expect(screen.getByText("Field Mappings")).not.toBeNull();
    expect(screen.getByText("Hematology Analyzer 1")).not.toBeNull();

    const separators = screen.getAllByText(">");
    expect(separators).toHaveLength(2);
  });

  it("should render back arrow when showBackArrow is true", () => {
    const breadcrumbs: Breadcrumb[] = [
      { label: "Analyzers", link: "/analyzers" },
      { label: "Field Mappings" },
    ];

    renderWithIntl(
      <PageTitle breadcrumbs={breadcrumbs} showBackArrow={true} />,
    );

    expect(screen.getByTestId("page-title-back-button")).not.toBeNull();
  });

  it("should render clickable breadcrumb links", async () => {
    const breadcrumbs: Breadcrumb[] = [
      { label: "Analyzers", link: "/analyzers" },
      { label: "Field Mappings" },
    ];

    renderWithIntl(<PageTitle breadcrumbs={breadcrumbs} />);

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
    const mockOnBack = jest.fn<void, []>();
    const breadcrumbs: Breadcrumb[] = [
      { label: "Analyzers", link: "/analyzers" },
      { label: "Field Mappings" },
    ];
    renderWithIntl(
      <PageTitle
        breadcrumbs={breadcrumbs}
        showBackArrow={true}
        onBack={mockOnBack}
      />,
    );

    const backButton = screen.getByTestId("page-title-back-button");
    await userEvent.click(backButton);

    expect(mockOnBack).toHaveBeenCalledTimes(1);
  });
});
