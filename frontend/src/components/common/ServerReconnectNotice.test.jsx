import React from "react";
import { act, fireEvent, render, screen } from "@testing-library/react";
import { IntlProvider } from "react-intl";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import messages from "../../languages/en.json";
import ServerReconnectNotice from "./ServerReconnectNotice";

const renderNotice = (props) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <ServerReconnectNotice {...props} />
    </IntlProvider>,
  );

describe("ServerReconnectNotice", () => {
  beforeEach(() => {
    vi.useFakeTimers();
    vi.setSystemTime(Date.parse("2031-03-05T08:00:00Z"));
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it("counts down to the next attempt from translated text", () => {
    renderNotice({ retryAt: Date.now() + 8000, onTryNow: vi.fn() });

    expect(screen.getByText("Can't reach the server")).toBeVisible();
    expect(screen.getByText("Trying again in 8 s")).toBeVisible();

    act(() => {
      vi.advanceTimersByTime(3000);
    });
    expect(screen.getByText("Trying again in 5 s")).toBeVisible();
  });

  it("tries now on request", () => {
    const onTryNow = vi.fn();
    renderNotice({ retryAt: Date.now() + 15000, onTryNow });

    fireEvent.click(screen.getByRole("button", { name: "Try now" }));

    expect(onTryNow).toHaveBeenCalledOnce();
  });

  it("says it is trying and disables Try now while an attempt runs", () => {
    renderNotice({ retryAt: Date.now(), onTryNow: vi.fn() });

    expect(screen.getByText("Trying again now…")).toBeVisible();
    expect(screen.getByRole("button", { name: "Try now" })).toBeDisabled();
  });

  it("restarts the countdown when the next attempt is rescheduled", () => {
    const onTryNow = vi.fn();
    const { rerender } = renderNotice({ retryAt: Date.now(), onTryNow });

    rerender(
      <IntlProvider locale="en" messages={messages}>
        <ServerReconnectNotice
          retryAt={Date.now() + 30000}
          onTryNow={onTryNow}
        />
      </IntlProvider>,
    );

    expect(screen.getByText("Trying again in 30 s")).toBeVisible();
    expect(screen.getByRole("button", { name: "Try now" })).toBeEnabled();
  });

  it("does not block the page: no dialog, and nothing is announced every second", () => {
    renderNotice({ retryAt: Date.now() + 8000, onTryNow: vi.fn() });

    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
    expect(screen.getByTestId("server-reconnect-countdown")).toHaveAttribute(
      "aria-hidden",
      "true",
    );
  });
});
