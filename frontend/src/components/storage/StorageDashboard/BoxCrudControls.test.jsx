import React from "react";
import { render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../languages/en.json";
import UserSessionDetailsContext from "../../../UserSessionDetailsContext";
import BoxCrudControls from "./BoxCrudControls";

const mockUserSessionDetails = {
  roles: ["Global Administrator"],
};

const renderWithIntl = (component) => {
  return render(
    <IntlProvider locale="en" messages={messages}>
      <UserSessionDetailsContext.Provider
        value={{ userSessionDetails: mockUserSessionDetails }}
      >
        {component}
      </UserSessionDetailsContext.Provider>
    </IntlProvider>,
  );
};

describe("BoxCrudControls", () => {
  test("testAddButton_DisabledWithoutRack", () => {
    renderWithIntl(
      <BoxCrudControls
        selectedRackId=""
        selectedBox={null}
        onCreate={jest.fn()}
        onEdit={jest.fn()}
        onDelete={jest.fn()}
      />,
    );

    const addButton = screen.getByTestId("add-box-button");
    expect(!!addButton.disabled).toBe(true);
    expect(screen.queryByTestId("location-actions-overflow-menu")).toBeNull();
  });

  test("testSelectedBox_ShowsOverflowMenu", () => {
    renderWithIntl(
      <BoxCrudControls
        selectedRackId="30"
        selectedBox={{ id: "101", label: "Box A", code: "BOXA" }}
        onCreate={jest.fn()}
        onEdit={jest.fn()}
        onDelete={jest.fn()}
      />,
    );

    const addButton = screen.getByTestId("add-box-button");
    expect(!!addButton.disabled).toBe(false);

    const menu = screen.getByTestId("location-actions-overflow-menu");
    expect(menu).toBeTruthy();
  });
});
