import React from "react";
import { fireEvent, render, screen, within } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter, Route, Switch } from "react-router-dom";
import messages from "../../../languages/en.json";
import BoxCreation from "../BoxCreation";
import AddToBoxModal from "../AddToBoxModal";

vi.mock("../../common/PageBreadCrumb", () => ({
  default: function MockBreadCrumb() {
    return <div data-testid="breadcrumb" />;
  },
}));

vi.mock("../../layout/Layout", async () => {
  const { createContext } = await import("react");
  return {
    NotificationContext: createContext({
      notifications: [],
      addNotification: () => {},
      removeNotification: () => {},
    }),
  };
});

const DESTINATION_MESSAGE = "The destination in this link could not be loaded.";
const SAMPLE_MESSAGE = "The sample in this link could not be loaded.";
const SELECT_DESTINATION = "Select a destination facility";
const ADD_SAMPLE = "Add at least one sample";

const referralLabs = [
  { id: "11", value: "Alpha Lab" },
  { id: "12", value: "Bravo Lab" },
];

const sampleForAlpha = {
  sampleItemId: "101",
  accessionNumber: "DEV0100001-1",
  typeOfSample: "Serum",
  referralTests: [{ testName: "HIV Viral Load" }],
  destinationFacilityId: "11",
};

const sampleForInactiveLab = {
  sampleItemId: "103",
  accessionNumber: "DEV0100003-1",
  typeOfSample: "Serum",
  referralTests: [{ testName: "CD4" }],
  destinationFacilityId: "13",
};

const jsonResponse = (body, status = 200) =>
  new Response(JSON.stringify(body), {
    status,
    headers: { "content-type": "application/json" },
  });

const serve = (overrides = {}) => {
  const routes = {
    "/rest/displayList/REFERRAL_ORGANIZATIONS": () =>
      jsonResponse(referralLabs),
    "/rest/displayList/REFERRAL_REASONS": () => jsonResponse([]),
    "/rest/shipping-box/box-label-prefix": () => jsonResponse("BOX"),
    "/rest/shipping-box/generate-box-number": () => jsonResponse("BOX-0007"),
    "/rest/unassigned-sample/items": () =>
      jsonResponse([sampleForAlpha, sampleForInactiveLab]),
    "/rest/shipping-box/by-facility/11": () => jsonResponse([]),
    ...overrides,
  };
  const fetchMock = vi.fn(async (url) => {
    const path = Object.keys(routes).find((p) => url.endsWith(p));
    return path ? routes[path]() : jsonResponse({}, 404);
  });
  vi.stubGlobal("fetch", fetchMock);
  return fetchMock;
};

const requested = (fetchMock, path) =>
  fetchMock.mock.calls.some(([url]) => url.endsWith(path));

const renderAt = (url) =>
  render(
    <MemoryRouter initialEntries={[url]}>
      <IntlProvider locale="en" messages={messages}>
        <Route path="/SampleShipment/create-box">
          <BoxCreation />
        </Route>
      </IntlProvider>
    </MemoryRouter>,
  );

const settled = (fetchMock) =>
  waitFor(() => {
    expect(requested(fetchMock, "/rest/shipping-box/generate-box-number")).toBe(
      true,
    );
    expect(screen.getByText("BOX-0007")).toBeInTheDocument();
  });

const summaryValue = (label) =>
  screen
    .getByText(`${label}:`, { selector: ".summary-label" })
    .closest(".summary-row")
    .querySelector(".summary-value").textContent;

const destinationField = () =>
  screen.getByRole("combobox", { name: "Destination" });

const saveButtons = () => [
  screen.getByRole("button", { name: /Save as Draft/ }),
  screen.getByRole("button", { name: /Save & Mark Ready to Send/ }),
];

const expectEmptyForm = () => {
  expect(destinationField()).toHaveTextContent("Select");
  expect(summaryValue("Destination")).toBe("-");
  expect(summaryValue("Samples Added")).toBe("0");
  expect(screen.getByText(SELECT_DESTINATION)).toBeInTheDocument();
  expect(screen.getByText(ADD_SAMPLE)).toBeInTheDocument();
  saveButtons().forEach((button) => expect(button).toBeDisabled());
};

const expectNoLinkMessages = () => {
  expect(screen.queryByText(DESTINATION_MESSAGE)).toBeNull();
  expect(screen.queryByText(SAMPLE_MESSAGE)).toBeNull();
};

describe("BoxCreation opened from an Add to Box link", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
  });

  test("preselects the sample's lab and stages the sample", async () => {
    const fetchMock = serve();
    renderAt("/SampleShipment/create-box?facilityId=11&sampleItemId=101");
    await settled(fetchMock);

    await waitFor(() => expect(summaryValue("Samples Added")).toBe("1"));
    expect(destinationField()).toHaveTextContent("Alpha Lab");
    expect(summaryValue("Destination")).toBe("Alpha Lab");
    const table = screen.getByRole("table");
    expect(within(table).getByText("DEV0100001-1")).toBeInTheDocument();
    expect(within(table).getByText("HIV Viral Load")).toBeInTheDocument();
    expect(screen.queryByText(SELECT_DESTINATION)).toBeNull();
    expect(screen.queryByText(ADD_SAMPLE)).toBeNull();
    saveButtons().forEach((button) => expect(button).toBeEnabled());
    expectNoLinkMessages();
  });

  test("takes the lab from the sample when the link names another lab", async () => {
    const fetchMock = serve();
    renderAt("/SampleShipment/create-box?facilityId=12&sampleItemId=101");
    await settled(fetchMock);

    await waitFor(() => expect(summaryValue("Samples Added")).toBe("1"));
    expect(destinationField()).toHaveTextContent("Alpha Lab");
    expect(summaryValue("Destination")).toBe("Alpha Lab");
    expectNoLinkMessages();
  });

  test("says the destination could not be loaded for an unknown facilityId", async () => {
    const fetchMock = serve();
    renderAt("/SampleShipment/create-box?facilityId=999999");
    await settled(fetchMock);

    expect(await screen.findByText(DESTINATION_MESSAGE)).toBeInTheDocument();
    expect(screen.queryByText(SAMPLE_MESSAGE)).toBeNull();
    expectEmptyForm();
    expect(requested(fetchMock, "/rest/unassigned-sample/items")).toBe(false);
  });

  test("says the sample could not be loaded when the link's sample is not waiting for a box", async () => {
    const fetchMock = serve();
    renderAt("/SampleShipment/create-box?facilityId=11&sampleItemId=555");
    await settled(fetchMock);

    expect(await screen.findByText(SAMPLE_MESSAGE)).toBeInTheDocument();
    expect(screen.queryByText(DESTINATION_MESSAGE)).toBeNull();
    expect(destinationField()).toHaveTextContent("Alpha Lab");
    expect(summaryValue("Samples Added")).toBe("0");
    expect(screen.getByText(ADD_SAMPLE)).toBeInTheDocument();
    saveButtons().forEach((button) => expect(button).toBeDisabled());
  });

  test("says the sample could not be loaded when the unassigned list fails", async () => {
    const fetchMock = serve({
      "/rest/unassigned-sample/items": () => jsonResponse({}, 500),
    });
    renderAt("/SampleShipment/create-box?facilityId=11&sampleItemId=101");
    await settled(fetchMock);

    expect(await screen.findByText(SAMPLE_MESSAGE)).toBeInTheDocument();
    expect(screen.queryByText(DESTINATION_MESSAGE)).toBeNull();
    expect(destinationField()).toHaveTextContent("Alpha Lab");
    expect(summaryValue("Samples Added")).toBe("0");
  });

  test("stages nothing when the destination list fails", async () => {
    const fetchMock = serve({
      "/rest/displayList/REFERRAL_ORGANIZATIONS": () => jsonResponse({}, 500),
    });
    renderAt("/SampleShipment/create-box?facilityId=11&sampleItemId=101");
    await settled(fetchMock);

    expect(await screen.findByText(DESTINATION_MESSAGE)).toBeInTheDocument();
    expect(screen.queryByText(SAMPLE_MESSAGE)).toBeNull();
    expectEmptyForm();
    expect(screen.queryByText("DEV0100001-1")).toBeNull();
  });

  test("stages nothing when the sample's lab is not a destination", async () => {
    const fetchMock = serve();
    renderAt("/SampleShipment/create-box?facilityId=13&sampleItemId=103");
    await settled(fetchMock);

    expect(await screen.findByText(DESTINATION_MESSAGE)).toBeInTheDocument();
    expect(screen.queryByText(SAMPLE_MESSAGE)).toBeNull();
    expectEmptyForm();
    expect(screen.queryByText("DEV0100003-1")).toBeNull();
  });

  test("treats empty link values as no link", async () => {
    const fetchMock = serve();
    renderAt("/SampleShipment/create-box?facilityId=&sampleItemId=");
    await settled(fetchMock);

    expectEmptyForm();
    expectNoLinkMessages();
    expect(requested(fetchMock, "/rest/unassigned-sample/items")).toBe(false);
  });

  test("opens the plain form without a query string", async () => {
    const fetchMock = serve();
    renderAt("/SampleShipment/create-box");
    await settled(fetchMock);

    expectEmptyForm();
    expectNoLinkMessages();
    expect(requested(fetchMock, "/rest/unassigned-sample/items")).toBe(false);
  });
});

describe("Add to Box hands the sample to Create Box", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
  });

  test("Create New Box opens Create Box with the sample's lab and the sample", async () => {
    const fetchMock = serve();
    render(
      <MemoryRouter initialEntries={["/SampleShipment/unassigned"]}>
        <IntlProvider locale="en" messages={messages}>
          <Switch>
            <Route path="/SampleShipment/create-box">
              <BoxCreation />
            </Route>
            <Route>
              <AddToBoxModal open sample={sampleForAlpha} onClose={() => {}} />
            </Route>
          </Switch>
        </IntlProvider>
      </MemoryRouter>,
    );

    const createNewBox = await screen.findByRole("button", {
      name: "Create New Box",
    });
    await waitFor(() => expect(createNewBox).toBeEnabled());
    fireEvent.click(createNewBox);

    await settled(fetchMock);
    await waitFor(() => expect(summaryValue("Samples Added")).toBe("1"));
    expect(destinationField()).toHaveTextContent("Alpha Lab");
    expect(summaryValue("Destination")).toBe("Alpha Lab");
    expectNoLinkMessages();
  });
});
