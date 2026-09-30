import React from "react";
import { render, screen, fireEvent, within } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import GlobalMenuManagement from "./GlobalMenuManagement";
import {
  fetchFromOpenElisServer,
  postToOpenElisServerFullResponse,
} from "../../utils/Utils";
import messages from "../../../languages/en.json";

vi.mock("../../utils/Utils", () => ({
  fetchFromOpenElisServer: vi.fn(),
  postToOpenElisServerFullResponse: vi.fn(),
}));

const editable = {
  menu: {
    elementId: "custom",
    displayKey: "banner.menu.patient",
    isActive: true,
    actionURL: "/Patient",
    icon: null,
    presentationStyle: null,
    configurationFields: [],
  },
  childMenus: [],
};
const configured = {
  menu: {
    elementId: "managed",
    displayKey: "sidenav.label.reports",
    isActive: true,
    actionURL: "/reports/custom-data-export",
    icon: "reports",
    presentationStyle: "section",
    configurationFields: ["icon", "presentationStyle", "isActive"],
  },
  childMenus: [],
};

function mount() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
    logger: { log: () => {}, warn: () => {}, error: () => {} },
  });
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter>
        <IntlProvider locale="en" messages={messages}>
          <GlobalMenuManagement />
        </IntlProvider>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

beforeEach(() => {
  vi.clearAllMocks();
  fetchFromOpenElisServer.mockResolvedValue([editable, configured]);
});

test("persists only edited entries and reloads the returned effective menu", async () => {
  postToOpenElisServerFullResponse.mockImplementation((url, body, done) => {
    const [changed] = JSON.parse(body);
    done({ ok: true, json: async () => [changed, configured] });
  });
  const view = mount();
  fireEvent.click(
    await screen.findByRole("button", { name: "Patient", exact: true }),
  );
  const patient = screen
    .getByRole("button", { name: "Patient", exact: true })
    .closest("li");
  fireEvent.change(within(patient).getByLabelText("Icon"), {
    target: { value: "patient" },
  });
  fireEvent.change(within(patient).getByLabelText("Display as"), {
    target: { value: "section" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Save", exact: true }));
  expect(await screen.findByText("Menu settings saved.")).toBeTruthy();
  expect(postToOpenElisServerFullResponse).toHaveBeenCalledTimes(1);
  const [url, body] = postToOpenElisServerFullResponse.mock.calls[0];
  expect(url).toBe("/rest/admin/menu");
  expect(JSON.parse(body)).toEqual([
    {
      menu: { ...editable.menu, icon: "patient", presentationStyle: "section" },
      childMenus: [],
    },
  ]);
  expect(
    screen.getByRole("button", { name: "Save", exact: true }),
  ).toBeDisabled();
  view.unmount();
  fetchFromOpenElisServer.mockResolvedValue([
    {
      ...editable,
      menu: { ...editable.menu, icon: "patient", presentationStyle: "section" },
    },
    configured,
  ]);
  mount();
  fireEvent.click(
    await screen.findByRole("button", { name: "Patient", exact: true }),
  );
  expect(
    within(
      screen
        .getByRole("button", { name: "Patient", exact: true })
        .closest("li"),
    ).getByLabelText("Icon"),
  ).toHaveValue("patient");
});

test("instance-controlled settings stay visible and cannot be edited", async () => {
  mount();
  fireEvent.click(
    await screen.findByRole("button", { name: "Reports", exact: true }),
  );
  const reports = screen
    .getByRole("button", { name: "Reports", exact: true })
    .closest("li");
  expect(within(reports).getByLabelText("Icon")).toBeDisabled();
  expect(within(reports).getByLabelText("Display as")).toBeDisabled();
  expect(within(reports).getByRole("switch")).toBeDisabled();
  expect(
    within(reports).getAllByText("Managed by instance configuration").length,
  ).toBeGreaterThan(0);
  expect(
    screen.getByRole("button", { name: "Save", exact: true }),
  ).toBeDisabled();
});

test("a failed save retains edits and lets the user retry", async () => {
  postToOpenElisServerFullResponse.mockImplementation((url, body, done) =>
    done({ ok: false }),
  );
  mount();
  fireEvent.click(
    await screen.findByRole("button", { name: "Patient", exact: true }),
  );
  const patient = screen
    .getByRole("button", { name: "Patient", exact: true })
    .closest("li");
  fireEvent.change(within(patient).getByLabelText("Icon"), {
    target: { value: "patient" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Save", exact: true }));
  await screen.findByText("Save failed");
  expect(within(patient).getByLabelText("Icon")).toHaveValue("patient");
  await waitFor(() =>
    expect(
      screen.getByRole("button", { name: "Save", exact: true }),
    ).toBeEnabled(),
  );
  expect(fetchFromOpenElisServer).toHaveBeenCalledTimes(1);
  postToOpenElisServerFullResponse.mockImplementation((url, body, done) => {
    done({ ok: true, json: async () => [JSON.parse(body)[0], configured] });
  });
  fireEvent.click(screen.getByRole("button", { name: "Save", exact: true }));
  await screen.findByText("Menu settings saved.");
  expect(postToOpenElisServerFullResponse.mock.calls[1][1]).toBe(
    postToOpenElisServerFullResponse.mock.calls[0][1],
  );
});

test("configuration-only groups stay compact while their database children remain editable", async () => {
  fetchFromOpenElisServer.mockResolvedValue([
    {
      ...configured,
      menu: { ...configured.menu, configurationOnly: true },
      childMenus: [editable],
    },
  ]);
  mount();
  fireEvent.click(
    await screen.findByRole("button", { name: "Reports", exact: true }),
  );
  expect(screen.queryByTestId("menu-fields-managed")).toBeNull();
  expect(screen.getByText("Managed by instance configuration")).toBeTruthy();
  fireEvent.click(screen.getByRole("button", { name: "Patient", exact: true }));
  expect(
    within(screen.getByTestId("menu-fields-custom")).getByLabelText("Icon"),
  ).toBeEnabled();
});
