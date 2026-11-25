/**
 * OGC-1635: a typed date of event reaches the NCE that is saved, a date the
 * form refuses says why, and a missing date is marked on the field.
 */
import React from "react";
import { act, fireEvent, render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import messages from "../../../../languages/en.json";
import InlineNceForm from "../InlineNceForm";
import { ReportNonConformingEvent } from "../ReportNonConformingEvent";
import { NotificationContext } from "../../../layout/Layout";
import UserSessionDetailsContext from "../../../../UserSessionDetailsContext";
import { getFromOpenElisServer } from "../../../utils/Utils";

vi.mock("../../../utils/Utils", async (importOriginal) => {
  const actual = await importOriginal();
  return {
    ...actual,
    getFromOpenElisServer: vi.fn(),
    postToOpenElisServerJsonResponse: vi.fn(),
    postToOpenElisServerFormData: vi.fn(),
  };
});

const withProviders = (children) => (
  <IntlProvider locale="en" messages={messages}>
    <NotificationContext.Provider
      value={{
        notificationVisible: false,
        setNotificationVisible: vi.fn(),
        addNotification: vi.fn(),
      }}
    >
      <UserSessionDetailsContext.Provider
        value={{ userSessionDetails: { firstName: "A", lastName: "B" } }}
      >
        <MemoryRouter initialEntries={["/ReportNonConformingEvent"]}>
          {children}
        </MemoryRouter>
      </UserSessionDetailsContext.Provider>
    </NotificationContext.Provider>
  </IntlProvider>
);

const FORMS = [
  {
    name: "Report NCE page",
    inputId: "date-of-event",
    submit: /Submit NCE/,
    element: () => <ReportNonConformingEvent />,
  },
  {
    name: "inline NCE form",
    inputId: "inline-nce-date",
    submit: /Create NCE/,
    element: () => (
      <InlineNceForm accessionNumber="DEV01260000000000001" onClose={vi.fn()} />
    ),
  },
];

const typeDate = (input, text) => {
  act(() => input.focus());
  fireEvent.click(input);
  fireEvent.input(input, { target: { value: text } });
  const target = document.body;
  act(() => {
    fireEvent.touchStart(target);
    fireEvent.mouseDown(target);
    input.blur();
  });
};

describe.each(FORMS)("$name date of event", (form) => {
  beforeEach(() => {
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url === "/rest/nce/generate-number") {
        callback({ nceNumber: "NCE-1" });
      } else {
        callback([]);
      }
    });
  });

  const renderForm = async () => {
    await act(async () => {
      render(withProviders(form.element()));
    });
    return document.getElementById(form.inputId);
  };

  it("refuses a typed future date and says why", async () => {
    const input = await renderForm();

    typeDate(input, "12/31/2999");

    expect(input).toHaveValue("12/31/2999");
    expect(input).toHaveAttribute("data-invalid");
    expect(
      screen.getByText("The date cannot be in the future."),
    ).toBeInTheDocument();
  });

  it("keeps saying why a typed date was refused after Submit", async () => {
    const input = await renderForm();

    typeDate(input, "12/31/2999");
    await act(async () => {
      fireEvent.click(screen.getByRole("button", { name: form.submit }));
    });

    expect(
      screen.getByText("The date cannot be in the future."),
    ).toBeInTheDocument();
    expect(screen.queryByText("Date of event is required")).toBeNull();
  });

  it("marks the date field when it is left empty", async () => {
    const input = await renderForm();

    typeDate(input, "");
    await act(async () => {
      fireEvent.click(screen.getByRole("button", { name: form.submit }));
    });

    expect(input).toHaveAttribute("data-invalid");
    expect(screen.getByText("Date of event is required")).toBeInTheDocument();
  });
});
