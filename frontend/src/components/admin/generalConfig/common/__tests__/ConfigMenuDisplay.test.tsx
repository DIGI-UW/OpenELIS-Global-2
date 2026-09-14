import React from "react";
import { render, screen, fireEvent, act } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import userEvent from "@testing-library/user-event";
import { IntlProvider } from "react-intl";
import { MemoryRouter, Route } from "react-router-dom";
import { QueryClientProvider } from "@tanstack/react-query";
import { beforeEach, describe, expect, it, vi } from "vitest";
import messages from "../../../../../languages/en.json";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
  postToOpenElisServerFormData,
} from "../../../../utils/Utils";
import { createQueryClient } from "../../../../utils/queryClient";
import {
  ConfigurationContext,
  NotificationContext,
} from "../../../../layout/contexts";
import ConfigMenuDisplay from "../ConfigMenuDisplay";

vi.mock("../../../../utils/Utils", async () => {
  const actual = await vi.importActual("../../../../utils/Utils");
  const getFromOpenElisServer = vi.fn();
  return {
    ...actual,
    getFromOpenElisServer,
    fetchFromOpenElisServer: vi.fn(
      (url: string) =>
        new Promise((resolve, reject) =>
          getFromOpenElisServer(url, (response: unknown) =>
            response === undefined
              ? reject(new Error("read failed"))
              : resolve(response),
          ),
        ),
    ),
    postToOpenElisServer: vi.fn(),
    postToOpenElisServerFormData: vi.fn(),
  };
});

const MENU = "NonConformityConfigurationMenu";
const CONFIG = "NonConformityConfiguration";

const config = (value: string) => ({
  id: "7",
  name: "resultsOnly",
  description: "Results only",
  value,
  valueType: "boolean",
  paramName: "resultsOnly",
});

describe("ConfigMenuDisplay", () => {
  let onServer: { value: string };
  let reloadConfiguration: ReturnType<typeof vi.fn>;

  const renderScreen = () =>
    render(
      <MemoryRouter>
        <IntlProvider locale="en" messages={messages}>
          <QueryClientProvider client={createQueryClient()}>
            <NotificationContext.Provider
              value={{
                notificationVisible: false,
                setNotificationVisible: vi.fn(),
                addNotification: vi.fn(),
              }}
            >
              <ConfigurationContext.Provider value={{ reloadConfiguration }}>
                <ConfigMenuDisplay
                  id="admin.formEntryConfig"
                  label="Non Conformity"
                  menuType={MENU}
                />
              </ConfigurationContext.Provider>
            </NotificationContext.Provider>
          </QueryClientProvider>
        </IntlProvider>
      </MemoryRouter>,
    );

  const openTheEditor = async () => {
    renderScreen();
    const row = await screen.findByLabelText("selectRow");
    fireEvent.click(row);
    await userEvent.click(screen.getByRole("button", { name: "Modify" }));
    return screen.findByRole("heading", { name: "Edit Record" });
  };

  beforeEach(() => {
    onServer = { value: "false" };
    reloadConfiguration = vi.fn();
    (getFromOpenElisServer as ReturnType<typeof vi.fn>).mockReset();
    (getFromOpenElisServer as ReturnType<typeof vi.fn>).mockImplementation(
      (url: string, callback: (r: unknown) => void) => {
        if (url === `/rest/${MENU}`)
          return callback({ menuList: [config(onServer.value)] });
        if (url.startsWith(`/rest/${CONFIG}?ID=`))
          return callback(config(onServer.value));
        return callback(undefined);
      },
    );
    (postToOpenElisServer as ReturnType<typeof vi.fn>).mockReset();
  });

  it("lists the configuration values", async () => {
    renderScreen();

    expect(await screen.findByText("Results only")).toBeInTheDocument();
  });

  it("returns to the list showing the saved value", async () => {
    await openTheEditor();

    await userEvent.click(screen.getByLabelText("True"));
    (postToOpenElisServer as ReturnType<typeof vi.fn>).mockImplementation(
      (url: string, body: string, callback: (status: number) => void) => {
        onServer = { value: "true" };
        callback(200);
      },
    );
    await userEvent.click(screen.getByRole("button", { name: "Save" }));

    // Reloading the document is what used to close the editor and refresh the
    // list; both now happen without leaving the app.
    await waitFor(() =>
      expect(
        screen.queryByRole("heading", { name: "Edit Record" }),
      ).not.toBeInTheDocument(),
    );
    expect(await screen.findByText("true")).toBeInTheDocument();
    expect(reloadConfiguration).toHaveBeenCalledOnce();
  });

  it("does not refresh runtime configuration when saving fails", async () => {
    await openTheEditor();
    (postToOpenElisServer as ReturnType<typeof vi.fn>).mockImplementation(
      (url: string, body: string, callback: (status: number) => void) =>
        callback(500),
    );
    await userEvent.click(screen.getByRole("button", { name: "Save" }));
    expect(reloadConfiguration).not.toHaveBeenCalled();
    expect(
      screen.getByRole("heading", { name: "Edit Record" }),
    ).toBeInTheDocument();
  });

  it("returns to the list when the edit is abandoned", async () => {
    await openTheEditor();

    await userEvent.click(screen.getByRole("button", { name: "Exit" }));

    await waitFor(() =>
      expect(
        screen.queryByRole("heading", { name: "Edit Record" }),
      ).not.toBeInTheDocument(),
    );
    expect(postToOpenElisServer).not.toHaveBeenCalled();
  });
});

/**
 * A menu whose rows include an image: the list fetches each image separately,
 * and those answers land while the editor for another row is open.
 */
describe("ConfigMenuDisplay with an image row", () => {
  const IMAGE_MENU = "PrintedReportsConfigurationMenu";
  const IMAGE_CONFIG = "PrintedReportsConfiguration";

  const textRow = {
    id: "1",
    name: "labDirector",
    description: "The lab director's name",
    value: "Dr A",
    valueType: "text",
    paramName: "labDirector",
  };
  const imageRow = {
    id: "2",
    name: "headerLeftImage",
    description: "Image for the left side of report header",
    value: "",
    valueType: "logoUpload",
    paramName: "headerLeftImage",
  };

  /** The image answers the list is still waiting on. */
  let pendingImages: Array<() => void>;
  /** Rereads of the record that have not answered yet. */
  let pendingRereads: Array<() => void>;
  let onServerValue: string;
  let recordRead: number;
  /** When set, every reread of a record is held until the test delivers it. */
  let holdRereads: boolean;

  beforeEach(() => {
    pendingImages = [];
    pendingRereads = [];
    onServerValue = textRow.value;
    recordRead = 0;
    holdRereads = false;
    (getFromOpenElisServer as ReturnType<typeof vi.fn>).mockReset();
    (getFromOpenElisServer as ReturnType<typeof vi.fn>).mockImplementation(
      (url: string, callback: (r: unknown) => void) => {
        if (url === `/rest/${IMAGE_MENU}`)
          return callback({ menuList: [textRow, imageRow] });
        if (url.startsWith(`/rest/${IMAGE_CONFIG}?ID=`)) {
          const answer = url.endsWith("ID=2")
            ? imageRow
            : { ...textRow, value: onServerValue };
          recordRead += 1;
          // A reread can be held so it lands while the user is typing, the way
          // a slow backend does.
          if (!holdRereads || recordRead === 1) return callback(answer);
          pendingRereads.push(() => callback(answer));
          return;
        }
        if (url.startsWith("/dbImage/")) {
          pendingImages.push(() =>
            callback({ value: "data:image/png;base64,AA" }),
          );
          return;
        }
        return callback(undefined);
      },
    );
    (postToOpenElisServer as ReturnType<typeof vi.fn>).mockReset();
  });

  const renderImageMenu = () =>
    render(
      <MemoryRouter>
        <IntlProvider locale="en" messages={messages}>
          <QueryClientProvider client={createQueryClient()}>
            <NotificationContext.Provider
              value={{
                notificationVisible: false,
                setNotificationVisible: vi.fn(),
                addNotification: vi.fn(),
              }}
            >
              <ConfigMenuDisplay
                id="admin.formEntryConfig"
                label="Printed Report"
                menuType={IMAGE_MENU}
              />
            </NotificationContext.Provider>
          </QueryClientProvider>
        </IntlProvider>
      </MemoryRouter>,
    );

  const openTheTextEditor = async () => {
    fireEvent.click((await screen.findAllByLabelText("selectRow"))[0]);
    await userEvent.click(screen.getByRole("button", { name: "Modify" }));
    await screen.findByRole("heading", { name: "Edit Record" });
    return document.getElementById("textInput") as HTMLInputElement;
  };

  const openTheImageEditor = async () => {
    renderImageMenu();
    await waitFor(() => expect(pendingImages.length).toBeGreaterThan(0));
    act(() => pendingImages.splice(0).forEach((deliver) => deliver()));
    await waitFor(() =>
      expect(screen.getAllByLabelText("selectRow")).toHaveLength(2),
    );
    fireEvent.click(screen.getAllByLabelText("selectRow")[1]);
    await userEvent.click(screen.getByRole("button", { name: "Modify" }));
    await screen.findByRole("heading", { name: "Edit Record" });
    await waitFor(() => expect(pendingImages.length).toBeGreaterThan(0));
    (postToOpenElisServerFormData as ReturnType<typeof vi.fn>).mockReset();
  };

  it("keeps a removal decision when the stored logo arrives late", async () => {
    await openTheImageEditor();
    fireEvent.click(screen.getByLabelText("Remove Image"));
    await act(async () =>
      pendingImages.splice(0).forEach((deliver) => deliver()),
    );
    expect(screen.getByLabelText("Remove Image")).toBeChecked();
    await userEvent.click(screen.getByRole("button", { name: "Save" }));
    const [, body] = (postToOpenElisServerFormData as ReturnType<typeof vi.fn>)
      .mock.calls[0];
    expect(body.get("removeImage")).toBe("true");
    expect(body.has("logoFile")).toBe(false);
  });

  it("keeps the chosen upload when the stored logo arrives late", async () => {
    await openTheImageEditor();
    const file = new File(["replacement"], "logo.png", { type: "image/png" });
    await userEvent.upload(
      document.querySelector('input[type="file"]') as HTMLInputElement,
      file,
    );
    await act(async () =>
      pendingImages.splice(0).forEach((deliver) => deliver()),
    );
    await userEvent.click(screen.getByRole("button", { name: "Save" }));
    const [, body] = (postToOpenElisServerFormData as ReturnType<typeof vi.fn>)
      .mock.calls[0];
    expect(body.get("logoFile")).toBe(file);
    expect(body.get("removeImage")).toBe("false");
  });

  it("keeps what is typed when a reread of the record answers mid-edit", async () => {
    holdRereads = true;
    renderImageMenu();

    // Cycle one: edit, save. Saving marks every read out of date.
    let input = await openTheTextEditor();
    await userEvent.clear(input);
    await userEvent.type(input, "Dr B");
    (postToOpenElisServer as ReturnType<typeof vi.fn>).mockImplementation(
      (url: string, body: string, callback: (status: number) => void) => {
        onServerValue = "Dr B";
        callback(200);
      },
    );
    await userEvent.click(screen.getByRole("button", { name: "Save" }));
    await waitFor(() =>
      expect(
        screen.queryByRole("heading", { name: "Edit Record" }),
      ).not.toBeInTheDocument(),
    );

    // Cycle two: the record is served from the cache while it rereads.
    input = await openTheTextEditor();
    await userEvent.clear(input);
    await userEvent.type(input, "Dr C");
    expect(pendingRereads.length).toBeGreaterThan(0);
    pendingRereads.forEach((deliver) => deliver());

    await waitFor(() => expect(input.isConnected).toBe(true));
    expect(input.value).toBe("Dr C");
  });

  it("keeps what is typed after an image record was edited first", async () => {
    renderImageMenu();
    await screen.findAllByLabelText("selectRow");
    // Wait for the image row to join the list so it can be selected.
    pendingImages.forEach((deliver) => deliver());
    await waitFor(() =>
      expect(screen.getAllByLabelText("selectRow").length).toBe(2),
    );

    // Edit the image record, then abandon it.
    fireEvent.click(screen.getAllByLabelText("selectRow")[1]);
    await userEvent.click(screen.getByRole("button", { name: "Modify" }));
    await screen.findByRole("heading", { name: "Edit Record" });
    await userEvent.click(screen.getByRole("button", { name: "Exit" }));
    await waitFor(() =>
      expect(
        screen.queryByRole("heading", { name: "Edit Record" }),
      ).not.toBeInTheDocument(),
    );

    // Now edit the text record.
    const input = await openTheTextEditor();
    await userEvent.clear(input);
    await userEvent.type(input, "Dr C");

    expect(input.isConnected).toBe(true);
    expect(input.value).toBe("Dr C");
  });

  it("keeps what is typed when the list's image answers arrive", async () => {
    render(
      <MemoryRouter>
        <IntlProvider locale="en" messages={messages}>
          <QueryClientProvider client={createQueryClient()}>
            <NotificationContext.Provider
              value={{
                notificationVisible: false,
                setNotificationVisible: vi.fn(),
                addNotification: vi.fn(),
              }}
            >
              <ConfigMenuDisplay
                id="admin.formEntryConfig"
                label="Printed Report"
                menuType={IMAGE_MENU}
              />
            </NotificationContext.Provider>
          </QueryClientProvider>
        </IntlProvider>
      </MemoryRouter>,
    );

    fireEvent.click((await screen.findAllByLabelText("selectRow"))[0]);
    await userEvent.click(screen.getByRole("button", { name: "Modify" }));
    await screen.findByRole("heading", { name: "Edit Record" });

    const input = document.getElementById("textInput") as HTMLInputElement;
    await userEvent.clear(input);
    await userEvent.type(input, "Dr B");

    // The image the list asked for on mount answers now, mid-edit.
    expect(pendingImages.length).toBeGreaterThan(0);
    pendingImages.forEach((deliver) => deliver());

    await waitFor(() => expect(input.isConnected).toBe(true));
    expect(input.value).toBe("Dr B");
    expect(
      screen.getByRole("heading", { name: "Edit Record" }),
    ).toBeInTheDocument();
  });
});

/**
 * Admin reaches these screens through a route. Passing the screen to `component`
 * as an inline arrow gives React a new component type on every render of Admin,
 * so anything that re-renders Admin — a notification arriving or timing out —
 * throws the screen away and builds a new one, losing the open editor. The
 * reload used to hide this: saving replaced the document anyway.
 */
describe("ConfigMenuDisplay behind a route", () => {
  const routed = { value: "false" };
  const RouteHost = ({ useRenderProp }: { useRenderProp: boolean }) => {
    const [, setTick] = React.useState(0);
    const screenEl = (
      <ConfigMenuDisplay
        id="admin.formEntryConfig"
        label="Non Conformity"
        menuType={MENU}
      />
    );
    return (
      <MemoryRouter initialEntries={["/menu"]}>
        <IntlProvider locale="en" messages={messages}>
          <QueryClientProvider client={createQueryClient()}>
            <NotificationContext.Provider
              value={{
                notificationVisible: false,
                setNotificationVisible: vi.fn(),
                addNotification: vi.fn(),
              }}
            >
              <button type="button" onClick={() => setTick((n) => n + 1)}>
                notify
              </button>
              {useRenderProp ? (
                <Route path="/menu" render={() => screenEl} />
              ) : (
                <Route path="/menu" component={() => screenEl} />
              )}
            </NotificationContext.Provider>
          </QueryClientProvider>
        </IntlProvider>
      </MemoryRouter>
    );
  };

  beforeEach(() => {
    (getFromOpenElisServer as ReturnType<typeof vi.fn>).mockReset();
    (getFromOpenElisServer as ReturnType<typeof vi.fn>).mockImplementation(
      (url: string, callback: (r: unknown) => void) => {
        if (url === `/rest/${MENU}`)
          return callback({ menuList: [config(routed.value)] });
        if (url.startsWith(`/rest/${CONFIG}?ID=`))
          return callback(config(routed.value));
        return callback(undefined);
      },
    );
  });

  const openTheEditorBehind = async (useRenderProp: boolean) => {
    render(<RouteHost useRenderProp={useRenderProp} />);
    fireEvent.click(await screen.findByLabelText("selectRow"));
    await userEvent.click(screen.getByRole("button", { name: "Modify" }));
    await screen.findByRole("heading", { name: "Edit Record" });
    await userEvent.click(screen.getByRole("button", { name: "notify" }));
  };

  it("keeps the open editor when Admin re-renders", async () => {
    await openTheEditorBehind(true);

    expect(
      screen.getByRole("heading", { name: "Edit Record" }),
    ).toBeInTheDocument();
  });

  it("loses the open editor when the route names the screen inline", async () => {
    await openTheEditorBehind(false);

    expect(
      screen.queryByRole("heading", { name: "Edit Record" }),
    ).not.toBeInTheDocument();
  });
});
