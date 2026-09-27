import React from "react";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { IntlProvider } from "react-intl";
import { expect, it, vi } from "vitest";
import messages from "../../languages/en.json";
import ServerDataState from "./ServerDataState";

it("shows a retry action when a required server read fails", async () => {
  const refetch = vi.fn();
  render(
    <IntlProvider locale="en" messages={messages}>
      <ServerDataState query={{ isError: true, refetch }} />
    </IntlProvider>,
  );

  await userEvent.click(screen.getByRole("button", { name: "Retry" }));

  expect(screen.getByText(messages["server.error.msg"])).toBeInTheDocument();
  expect(refetch).toHaveBeenCalledOnce();
});
