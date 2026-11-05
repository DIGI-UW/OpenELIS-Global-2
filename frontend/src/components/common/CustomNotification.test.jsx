import React, { useState } from "react";
import { act, fireEvent, render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { vi } from "vitest";
import { AlertDialog, NotificationKinds } from "./CustomNotification";
import { NotificationContext } from "../layout/contexts";
import { useNotificationList } from "../layout/notificationList";

let api;

function Page() {
  const list = useNotificationList();
  const [visible, setVisible] = useState(true);
  api = { ...list, setVisible };
  return (
    <NotificationContext.Provider
      value={{
        ...list,
        notificationVisible: visible,
        setNotificationVisible: setVisible,
      }}
    >
      {visible ? <AlertDialog /> : null}
    </NotificationContext.Provider>
  );
}

const say = (message) =>
  act(() =>
    api.addNotification({
      title: "Notification Message",
      message,
      kind: NotificationKinds.success,
    }),
  );

describe("AlertDialog", () => {
  beforeEach(() => vi.useFakeTimers());
  afterEach(() => vi.useRealTimers());

  it("removes two notifications that close together, so neither shows again", () => {
    render(<Page />);
    say("First saved.");
    say("Second saved.");
    expect(screen.getAllByText(/saved\./)).toHaveLength(2);

    act(() => vi.advanceTimersByTime(2500));
    expect(api.notifications).toHaveLength(0);

    say("Third saved.");
    expect(screen.getAllByText(/saved\./)).toHaveLength(1);
    expect(screen.getByText("Third saved.")).toBeInTheDocument();
  });

  it("does not show again a notification that was hidden before it closed", () => {
    render(<Page />);
    say("Invalid login name.");
    act(() => api.setVisible(false));
    act(() => vi.advanceTimersByTime(2500));
    act(() => api.setVisible(true));
    say("User information added successfully.");

    expect(screen.queryByText("Invalid login name.")).not.toBeInTheDocument();
    expect(
      screen.getByText("User information added successfully."),
    ).toBeInTheDocument();
  });

  it("shows on the next page a notification added just before the page changed", () => {
    render(<Page />);
    say("Patient Saved Successfully");
    act(() => api.setVisible(false));
    act(() => api.setVisible(true));

    expect(screen.getByText("Patient Saved Successfully")).toBeInTheDocument();
  });

  it("does not wait for a dialog to start the time a shown notification has left", () => {
    render(<Page />);
    say("Saved before the page changed.");
    act(() => api.setVisible(false));
    act(() => vi.advanceTimersByTime(1500));
    act(() => api.setVisible(true));
    act(() => vi.advanceTimersByTime(600));

    expect(api.notifications).toHaveLength(0);
  });

  it("closes the notification whose close button was pressed", () => {
    render(<Page />);
    say("Kept.");
    say("Closed.");

    const closeButtons = screen.getAllByRole("button", {
      name: /close notification/i,
    });
    fireEvent.click(closeButtons[1]);

    expect(api.notifications.map((n) => n.message)).toEqual(["Kept."]);
  });
});
