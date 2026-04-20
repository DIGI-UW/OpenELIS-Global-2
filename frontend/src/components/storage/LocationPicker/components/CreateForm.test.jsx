/**
 * CreateForm tests — 5-level cascading dropdowns + inline create.
 *
 * Carbon @carbon/react 1.15 notes:
 *   - Dropdown trigger is a <button>, not role=combobox. Use
 *     getByLabelText('Room').
 *   - `disabled` prop sets aria-disabled="true", not the native
 *     attribute. Assert with toHaveAttribute('aria-disabled', ...).
 *   - Render inside an IntlProvider; Carbon uses react-intl internally.
 */

import React from "react";
import { render, screen, fireEvent, act } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import CreateForm from "./CreateForm";
import * as Utils from "../../../utils/Utils";

vi.mock("../../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
  postToOpenElisServerJsonResponse: vi.fn(),
}));

const renderWithIntl = (component) =>
  render(
    <IntlProvider locale="en" messages={{}}>
      {component}
    </IntlProvider>,
  );

beforeEach(() => {
  Utils.getFromOpenElisServer.mockReset();
  Utils.postToOpenElisServerJsonResponse.mockReset();
});

const mockRoomsApi = (rooms) => {
  Utils.getFromOpenElisServer.mockImplementation((url, cb) => {
    if (url.startsWith("/rest/storage/rooms")) cb(rooms);
    else if (url.startsWith("/rest/storage/devices")) cb([]);
    else if (url.startsWith("/rest/storage/shelves")) cb([]);
    else if (url.startsWith("/rest/storage/racks")) cb([]);
    else if (url.startsWith("/rest/storage/boxes")) cb([]);
    else cb([]);
  });
};

describe("CreateForm — cascading dropdowns", () => {
  it("renders 5 dropdowns (Room/Device/Shelf/Rack/Box) by stable id", () => {
    mockRoomsApi([]);
    renderWithIntl(<CreateForm selection={{}} onLevelChange={vi.fn()} />);
    expect(document.querySelector("#location-picker-room")).toBeInTheDocument();
    expect(
      document.querySelector("#location-picker-device"),
    ).toBeInTheDocument();
    expect(
      document.querySelector("#location-picker-shelf"),
    ).toBeInTheDocument();
    expect(document.querySelector("#location-picker-rack")).toBeInTheDocument();
    expect(document.querySelector("#location-picker-box")).toBeInTheDocument();
  });

  // Carbon @carbon/react 1.15 Dropdown's `disabled` prop sets
  // aria-disabled="true" on the inner trigger button, NOT the native
  // disabled attribute. So we query the trigger and assert on aria-disabled.
  const triggerOf = (level) =>
    document
      .querySelector(`#location-picker-${level}`)
      .querySelector("button.cds--list-box__field");

  // TODO(#3456): Jest→Vitest drift — Carbon v1.15 Dropdown aria-disabled
  // timing differs under Vitest/JSDOM. Passes on develop under Jest.
  it.skip("disables descendant dropdowns until their parent is selected", () => {
    mockRoomsApi([]);
    renderWithIntl(<CreateForm selection={{}} onLevelChange={vi.fn()} />);
    expect(triggerOf("room")).toHaveAttribute("aria-disabled", "false");
    expect(triggerOf("device")).toHaveAttribute("aria-disabled", "true");
    expect(triggerOf("shelf")).toHaveAttribute("aria-disabled", "true");
    expect(triggerOf("rack")).toHaveAttribute("aria-disabled", "true");
    expect(triggerOf("box")).toHaveAttribute("aria-disabled", "true");
  });

  // TODO(#3456): Jest→Vitest drift — Carbon v1.15 Dropdown aria-disabled
  // timing differs under Vitest/JSDOM. Passes on develop under Jest.
  it.skip("enables Device dropdown when Room is selected", () => {
    mockRoomsApi([{ id: 1, name: "Main Lab" }]);
    renderWithIntl(
      <CreateForm
        selection={{ room: { id: 1, name: "Main Lab" } }}
        onLevelChange={vi.fn()}
      />,
    );
    expect(triggerOf("device")).toHaveAttribute("aria-disabled", "false");
    expect(triggerOf("shelf")).toHaveAttribute("aria-disabled", "true");
  });

  it("loads rooms on mount", () => {
    mockRoomsApi([{ id: 1, name: "Main Lab" }]);
    renderWithIntl(<CreateForm selection={{}} onLevelChange={vi.fn()} />);
    const calls = Utils.getFromOpenElisServer.mock.calls.map((c) => c[0]);
    expect(calls.some((url) => url.startsWith("/rest/storage/rooms"))).toBe(
      true,
    );
  });

  it("loads devices for the selected room", () => {
    Utils.getFromOpenElisServer.mockImplementation((url, cb) => {
      if (url.startsWith("/rest/storage/rooms"))
        cb([{ id: 1, name: "Main Lab" }]);
      else if (url.startsWith("/rest/storage/devices?roomId=1"))
        cb([{ id: 5, name: "Freezer 1" }]);
      else cb([]);
    });
    renderWithIntl(
      <CreateForm
        selection={{ room: { id: 1, name: "Main Lab" } }}
        onLevelChange={vi.fn()}
      />,
    );
    const calls = Utils.getFromOpenElisServer.mock.calls.map((c) => c[0]);
    expect(
      calls.some((url) => url.includes("/rest/storage/devices?roomId=1")),
    ).toBe(true);
  });

  it("dispatches SET_LEVEL via onLevelChange when Room is picked", () => {
    const onLevelChange = vi.fn();
    mockRoomsApi([{ id: 1, name: "Main Lab" }]);
    renderWithIntl(<CreateForm selection={{}} onLevelChange={onLevelChange} />);
    // Click the Room dropdown trigger to open the menu, then click the
    // option. Each option in Carbon's listbox has role=option.
    fireEvent.click(triggerOf("room"));
    fireEvent.click(screen.getByRole("option", { name: "Main Lab" }));
    expect(onLevelChange).toHaveBeenCalledWith("room", {
      id: 1,
      name: "Main Lab",
    });
  });
});

describe("CreateForm — inline create", () => {
  it("renders an 'Add new' button per level", () => {
    mockRoomsApi([]);
    renderWithIntl(<CreateForm selection={{}} onLevelChange={vi.fn()} />);
    // 5 levels = 5 "Add new" buttons (only the Room one is enabled when
    // nothing is selected; the rest are disabled)
    expect(
      screen.getAllByRole("button", { name: /add new/i }).length,
    ).toBeGreaterThanOrEqual(5);
  });

  it("opens an inline-create dialog when 'Add new' for Room is clicked", () => {
    mockRoomsApi([]);
    renderWithIntl(<CreateForm selection={{}} onLevelChange={vi.fn()} />);
    const addButtons = screen.getAllByRole("button", { name: /add new/i });
    fireEvent.click(addButtons[0]);
    // Carbon Modal renders role=dialog
    expect(screen.getByRole("dialog")).toBeInTheDocument();
    expect(screen.getByLabelText(/^name$/i)).toBeInTheDocument();
  });

  it("POSTs to the level endpoint and dispatches the new option on submit", async () => {
    mockRoomsApi([]);
    Utils.postToOpenElisServerJsonResponse.mockImplementation(
      (url, body, cb) => {
        cb({ id: 99, name: "New Room", code: "NEW", active: true });
      },
    );
    const onLevelChange = vi.fn();
    renderWithIntl(<CreateForm selection={{}} onLevelChange={onLevelChange} />);
    fireEvent.click(screen.getAllByRole("button", { name: /add new/i })[0]);
    fireEvent.change(screen.getByLabelText(/^name$/i), {
      target: { value: "New Room" },
    });
    await act(async () => {
      fireEvent.click(screen.getByRole("button", { name: /^create$/i }));
    });
    await waitFor(() => {
      expect(Utils.postToOpenElisServerJsonResponse).toHaveBeenCalledWith(
        "/rest/storage/rooms",
        expect.stringContaining("New Room"),
        expect.any(Function),
      );
      expect(onLevelChange).toHaveBeenCalledWith("room", {
        id: 99,
        name: "New Room",
      });
    });
  });

  it("Cancel closes the inline-create dialog without dispatching", () => {
    mockRoomsApi([]);
    const onLevelChange = vi.fn();
    renderWithIntl(<CreateForm selection={{}} onLevelChange={onLevelChange} />);
    fireEvent.click(screen.getAllByRole("button", { name: /add new/i })[0]);
    fireEvent.click(screen.getByRole("button", { name: /^cancel$/i }));
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
    expect(onLevelChange).not.toHaveBeenCalled();
  });

  // TODO(#3456): Jest→Vitest drift — multiple role="alert" elements (Carbon
  // InlineNotification + form-level). Passes on develop under Jest.
  it.skip("surfaces a Carbon InlineNotification when the backend rejects the create", async () => {
    mockRoomsApi([]);
    // Simulate backend rejection: useCreateLocation treats a body without `id`
    // as a failure and rejects with the server-provided message.
    Utils.postToOpenElisServerJsonResponse.mockImplementation(
      (url, body, cb) => {
        cb({ error: "Duplicate name 'Main Lab'" });
      },
    );
    const onLevelChange = vi.fn();
    renderWithIntl(<CreateForm selection={{}} onLevelChange={onLevelChange} />);
    fireEvent.click(screen.getAllByRole("button", { name: /add new/i })[0]);
    fireEvent.change(screen.getByLabelText(/^name$/i), {
      target: { value: "Main Lab" },
    });
    await act(async () => {
      fireEvent.click(screen.getByRole("button", { name: /^create$/i }));
    });
    await waitFor(() => {
      // Modal stays open on error so the user can retry or correct.
      expect(screen.getByRole("dialog")).toBeInTheDocument();
      // Carbon InlineNotification with kind="error" renders role=alert. This
      // disambiguates from Dropdown's hidden a11y live region (role=status).
      const notification = screen.getByRole("alert");
      expect(notification).toHaveTextContent(/Duplicate name/i);
    });
    expect(onLevelChange).not.toHaveBeenCalled();
  });

  // TODO(#3456): Jest→Vitest drift — multiple role="alert" elements.
  it.skip("clears the error when the user edits the name and retries successfully", async () => {
    mockRoomsApi([]);
    let callCount = 0;
    Utils.postToOpenElisServerJsonResponse.mockImplementation(
      (url, body, cb) => {
        callCount += 1;
        if (callCount === 1) {
          cb({ error: "Duplicate name" });
        } else {
          cb({ id: 42, name: "Fresh Lab", active: true });
        }
      },
    );
    const onLevelChange = vi.fn();
    renderWithIntl(<CreateForm selection={{}} onLevelChange={onLevelChange} />);
    fireEvent.click(screen.getAllByRole("button", { name: /add new/i })[0]);
    fireEvent.change(screen.getByLabelText(/^name$/i), {
      target: { value: "Main Lab" },
    });
    await act(async () => {
      fireEvent.click(screen.getByRole("button", { name: /^create$/i }));
    });
    await waitFor(() => expect(screen.getByRole("alert")).toBeInTheDocument());

    fireEvent.change(screen.getByLabelText(/^name$/i), {
      target: { value: "Fresh Lab" },
    });
    await act(async () => {
      fireEvent.click(screen.getByRole("button", { name: /^create$/i }));
    });
    await waitFor(() => {
      expect(onLevelChange).toHaveBeenCalledWith("room", {
        id: 42,
        name: "Fresh Lab",
      });
      expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
    });
  });

  // TODO(#3456): Jest→Vitest drift — multiple role="alert" elements.
  it.skip("clears the error when the user cancels and reopens the dialog", async () => {
    mockRoomsApi([]);
    Utils.postToOpenElisServerJsonResponse.mockImplementation(
      (url, body, cb) => {
        cb({ error: "Duplicate name" });
      },
    );
    renderWithIntl(<CreateForm selection={{}} onLevelChange={vi.fn()} />);
    fireEvent.click(screen.getAllByRole("button", { name: /add new/i })[0]);
    fireEvent.change(screen.getByLabelText(/^name$/i), {
      target: { value: "Main Lab" },
    });
    await act(async () => {
      fireEvent.click(screen.getByRole("button", { name: /^create$/i }));
    });
    await waitFor(() => expect(screen.getByRole("alert")).toBeInTheDocument());

    fireEvent.click(screen.getByRole("button", { name: /^cancel$/i }));
    fireEvent.click(screen.getAllByRole("button", { name: /add new/i })[0]);
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  });
});
