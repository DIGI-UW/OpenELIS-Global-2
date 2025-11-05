import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import { IntlProvider } from "react-intl";
import CompactLocationView from "./CompactLocationView";
import messages from "../../../languages/en.json";

const renderWithIntl = (component) => {
  return render(
    <IntlProvider locale="en" messages={messages}>
      {component}
    </IntlProvider>,
  );
};

describe("CompactLocationView", () => {
  const mockOnExpand = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
  });

  /**
   * T061a: Test displays location path when location is selected
   */
  test("testDisplaysLocationPath", () => {
    const locationPath =
      "Main Laboratory > Freezer Unit 1 > Shelf-A > Rack R1 > Position A5";

    renderWithIntl(
      <CompactLocationView
        locationPath={locationPath}
        onExpand={mockOnExpand}
      />,
    );

    expect(screen.getByText(locationPath)).toBeTruthy();
  });

  /**
   * T061a: Test displays 'Not assigned' when no location
   */
  test("testDisplaysNotAssignedWhenEmpty", () => {
    renderWithIntl(
      <CompactLocationView locationPath={null} onExpand={mockOnExpand} />,
    );

    expect(screen.getByText(/not assigned/i)).toBeTruthy();
  });

  /**
   * T061a: Test shows expand button
   */
  test("testShowsExpandButton", () => {
    renderWithIntl(
      <CompactLocationView locationPath={null} onExpand={mockOnExpand} />,
    );

    const expandButton = screen.getByTestId("expand-button");
    expect(expandButton).toBeTruthy();
  });

  /**
   * T061a: Test calls onExpand when button clicked
   */
  test("testCallsOnExpandWhenButtonClicked", () => {
    renderWithIntl(
      <CompactLocationView locationPath={null} onExpand={mockOnExpand} />,
    );

    const expandButton = screen.getByTestId("expand-button");
    fireEvent.click(expandButton);

    expect(mockOnExpand).toHaveBeenCalledTimes(1);
  });
});
