import React from "react";
import { act, render, screen } from "@testing-library/react";
import { vi } from "vitest";
import { IntlProvider } from "react-intl";
import messages from "../../../languages/en.json";
import OfflineNotice from "../OfflineNotice";

it("shows the offline notice while the browser is offline and hides it when the network returns", () => {
  const onLine = vi
    .spyOn(window.navigator, "onLine", "get")
    .mockReturnValue(false);
  try {
    render(
      <IntlProvider locale="en" messages={messages}>
        <OfflineNotice />
      </IntlProvider>,
    );
    expect(
      screen.getByText("Working offline; refresh when network returns."),
    ).toBeInTheDocument();
    onLine.mockReturnValue(true);
    act(() => {
      window.dispatchEvent(new Event("online"));
    });
    expect(
      screen.queryByText("Working offline; refresh when network returns."),
    ).not.toBeInTheDocument();
  } finally {
    onLine.mockRestore();
  }
});
