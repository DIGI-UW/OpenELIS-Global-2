import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import { IntlProvider } from "react-intl";
import { MemoryRouter, Route } from "react-router-dom";
import SampleTypeManagement from "./SampleTypeManagement";

const mockIntl = {
  formatMessage: ({ id, defaultMessage }) => defaultMessage || id,
};

// The editor is URL-driven: /MasterListsPage/SampleTypeManagement/:sampleTypeId?/:section?
// Wrap in a matching Route so useParams() sees the current segment when the
// user navigates from the list to the editor.
const renderPage = () =>
  render(
    <MemoryRouter initialEntries={["/MasterListsPage/SampleTypeManagement"]}>
      <IntlProvider locale="en" messages={{}}>
        <Route
          path="/MasterListsPage/SampleTypeManagement/:sampleTypeId?/:section?"
          render={() => <SampleTypeManagement intl={mockIntl} />}
        />
      </IntlProvider>
    </MemoryRouter>,
  );

describe("SampleTypeManagement", () => {
  test("renders sample type management page after loading", async () => {
    renderPage();

    // Header and Add button only mount once the async fetch settles
    // (component starts with isLoading=true).
    expect(
      await screen.findByText("Sample Type Management"),
    ).toBeInTheDocument();
    expect(screen.getByText("Add Sample Type")).toBeInTheDocument();
  });

  test("opens add sample type form when Add button is clicked", async () => {
    renderPage();

    const addButton = await screen.findByText("Add Sample Type");
    fireEvent.click(addButton);

    expect(screen.getByText("Add New Sample Type")).toBeInTheDocument();
  });

  test("can navigate back to list from add form", async () => {
    renderPage();

    const addButton = await screen.findByText("Add Sample Type");
    fireEvent.click(addButton);

    const backButton = screen.getByText("← Back to List");
    fireEvent.click(backButton);

    expect(screen.getByText("Sample Type Management")).toBeInTheDocument();
    expect(screen.getByText("Add Sample Type")).toBeInTheDocument();
  });
});
