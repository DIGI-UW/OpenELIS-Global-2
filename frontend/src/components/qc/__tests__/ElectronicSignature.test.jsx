import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../languages/en.json";
import ElectronicSignature from "../ElectronicSignature";

jest.mock("../../utils/Utils", () => ({
  getFromOpenElisServer: jest.fn(),
  postToOpenElisServerJsonResponse: jest.fn(),
}));

const {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} = require("../../utils/Utils");

const renderWithIntl = (component) => {
  return render(
    <IntlProvider locale="en" messages={messages}>
      {component}
    </IntlProvider>,
  );
};

describe("ElectronicSignature", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    getFromOpenElisServer.mockImplementation((url, callback) => {
      callback([]);
    });
  });

  test("renders title", () => {
    renderWithIntl(<ElectronicSignature reportId={1} />);
    expect(screen.getByText("Electronic Signatures")).toBeTruthy();
  });

  test("renders sign button", () => {
    renderWithIntl(<ElectronicSignature reportId={1} />);
    expect(screen.getAllByText("Sign Report").length).toBeGreaterThanOrEqual(1);
  });

  test("shows no signatures message when empty", () => {
    renderWithIntl(<ElectronicSignature reportId={1} />);
    expect(screen.getByText("No signatures yet")).toBeTruthy();
  });

  test("renders existing signatures", () => {
    getFromOpenElisServer.mockImplementation((url, callback) => {
      callback([
        {
          id: 1,
          userId: "admin",
          signedAt: "2026-01-15T10:00:00",
          ipAddress: "127.0.0.1",
          comment: "Reviewed",
        },
      ]);
    });

    renderWithIntl(<ElectronicSignature reportId={1} />);
    expect(screen.getByText("admin")).toBeTruthy();
    expect(screen.getByText("Reviewed")).toBeTruthy();
  });

  test("opens modal when sign button clicked", () => {
    renderWithIntl(<ElectronicSignature reportId={1} />);
    const signButtons = screen.getAllByText("Sign Report");
    fireEvent.click(signButtons[0]);
    expect(screen.getByText("User ID")).toBeTruthy();
  });

  test("calls sign API with correct data", () => {
    postToOpenElisServerJsonResponse.mockImplementation(
      (url, body, callback) => {
        callback({ id: 2, userId: "tech1", signedAt: "2026-01-15T11:00:00" });
      },
    );

    renderWithIntl(<ElectronicSignature reportId={5} />);
    const signButtons = screen.getAllByText("Sign Report");
    fireEvent.click(signButtons[0]);

    const userInput = screen.getByLabelText("User ID");
    fireEvent.change(userInput, { target: { value: "tech1" } });

    const modalSignButtons = screen.getAllByText("Sign Report");
    fireEvent.click(modalSignButtons[modalSignButtons.length - 1]);

    expect(postToOpenElisServerJsonResponse).toHaveBeenCalledWith(
      "/rest/qc/reports/5/sign",
      expect.any(String),
      expect.any(Function),
    );
  });

  test("fetches signatures on mount", () => {
    renderWithIntl(<ElectronicSignature reportId={10} />);
    expect(getFromOpenElisServer).toHaveBeenCalledWith(
      "/rest/qc/reports/10/signatures",
      expect.any(Function),
    );
  });
});
