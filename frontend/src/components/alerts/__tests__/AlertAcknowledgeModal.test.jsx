import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../languages/en.json";
import AlertAcknowledgeModal from "../AlertAcknowledgeModal";

const renderWithIntl = (component) => {
  return render(
    <IntlProvider locale="en" messages={messages}>
      {component}
    </IntlProvider>,
  );
};

describe("AlertAcknowledgeModal", () => {
  const mockOnClose = jest.fn();
  const mockOnSubmit = jest.fn();

  const warningAlert = {
    id: 1,
    alertType: "EQA_DEADLINE",
    severity: "WARNING",
    status: "OPEN",
    message: "EQA deadline approaching",
  };

  const criticalAlert = {
    id: 2,
    alertType: "EQA_DEADLINE",
    severity: "CRITICAL",
    status: "OPEN",
    message: "EQA sample OVERDUE",
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  test("renders modal when open", () => {
    renderWithIntl(
      <AlertAcknowledgeModal
        open={true}
        alert={warningAlert}
        onClose={mockOnClose}
        onSubmit={mockOnSubmit}
      />,
    );
    expect(screen.getByText("Acknowledge Alert")).toBeTruthy();
  });

  test("does not render when closed", () => {
    renderWithIntl(
      <AlertAcknowledgeModal
        open={false}
        alert={warningAlert}
        onClose={mockOnClose}
        onSubmit={mockOnSubmit}
      />,
    );
    expect(screen.queryByText("Acknowledge Alert")).toBeNull();
  });

  test("shows required message for critical alerts", () => {
    renderWithIntl(
      <AlertAcknowledgeModal
        open={true}
        alert={criticalAlert}
        onClose={mockOnClose}
        onSubmit={mockOnSubmit}
      />,
    );
    expect(
      screen.getByText("Resolution comment is required for critical alerts"),
    ).toBeTruthy();
  });

  test("does not show required message for non-critical alerts", () => {
    renderWithIntl(
      <AlertAcknowledgeModal
        open={true}
        alert={warningAlert}
        onClose={mockOnClose}
        onSubmit={mockOnSubmit}
      />,
    );
    expect(
      screen.queryByText("Resolution comment is required for critical alerts"),
    ).toBeNull();
  });

  test("displays alert message in modal", () => {
    renderWithIntl(
      <AlertAcknowledgeModal
        open={true}
        alert={warningAlert}
        onClose={mockOnClose}
        onSubmit={mockOnSubmit}
      />,
    );
    expect(screen.getByText("EQA deadline approaching")).toBeTruthy();
  });

  test("renders comment textarea", () => {
    const { container } = renderWithIntl(
      <AlertAcknowledgeModal
        open={true}
        alert={warningAlert}
        onClose={mockOnClose}
        onSubmit={mockOnSubmit}
      />,
    );
    const textarea = container.querySelector("textarea");
    expect(textarea).toBeTruthy();
  });

  test("calls onClose when cancel button is clicked", () => {
    renderWithIntl(
      <AlertAcknowledgeModal
        open={true}
        alert={warningAlert}
        onClose={mockOnClose}
        onSubmit={mockOnSubmit}
      />,
    );
    const cancelButton = screen.getByText("Cancel");
    fireEvent.click(cancelButton);
    expect(mockOnClose).toHaveBeenCalled();
  });
});
