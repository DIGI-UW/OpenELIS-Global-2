import React from "react";
import { render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
// ShipmentNavigation reads the router location, so the screen needs one.
import { MemoryRouter } from "react-router-dom";
import messages from "../../../languages/en.json";
import ShipmentSettings from "../ShipmentSettings";
import { getFromOpenElisServer } from "../../utils/Utils";

vi.mock("../../utils/Utils", async () => {
  const actual = await vi.importActual("../../utils/Utils");
  return {
    ...actual,
    getFromOpenElisServer: vi.fn(),
    putToOpenElisServerFullResponse: vi.fn(),
  };
});

vi.mock("../../common/PageBreadCrumb", () => ({
  default: function MockBreadCrumb() {
    return <div data-testid="breadcrumb" />;
  },
}));

// vi.mock is hoisted, so the context is created inside the factory.
vi.mock("../../layout/Layout", async () => {
  const { createContext } = await import("react");
  return {
    NotificationContext: createContext({
      addNotification: () => {},
      notificationVisible: false,
      setNotificationVisible: () => {},
    }),
  };
});

// The row representing this laboratory carries no organization type, which is why
// the referral list this screen used to read could never offer it.
const organizations = [
  { id: 2, organizationName: "Test LIMS", fhirUuid: "f21b8d74" },
  { id: 26, organizationName: "PMGH Lab", fhirUuid: "895c8e6b" },
  { id: 3, organizationName: "ACEH" },
];

const serve = ({ siteOrg }) =>
  getFromOpenElisServer.mockImplementation((url, callback) => {
    if (url === "/rest/organization-list") return callback(organizations);
    if (url === "/rest/shipping-box/site-organization-uuid")
      return callback(siteOrg);
    if (url === "/rest/shipping-box/box-label-prefix") return callback("CPHL");
    if (url === "/rest/shipping-box/fhir-mapping-config")
      return callback({ nonConformityCodes: "{}" });
    return callback(null);
  });

const renderSettings = () =>
  render(
    <MemoryRouter initialEntries={["/SampleShipment/settings"]}>
      <IntlProvider locale="en" messages={messages}>
        <ShipmentSettings />
      </IntlProvider>
    </MemoryRouter>,
  );

describe("ShipmentSettings site organization", () => {
  beforeEach(() => vi.clearAllMocks());

  test("offers this laboratory's own organization, not just referral destinations", async () => {
    serve({ siteOrg: { fhirUuid: "f21b8d74", orgId: "2" } });
    renderSettings();
    // Named as the current selection, which is only possible if the list holds it.
    await waitFor(() =>
      expect(screen.getAllByText("Test LIMS").length).toBeGreaterThan(0),
    );
  });

  test("says so when the configured UUID belongs to no local organization", async () => {
    // The cross-instance case: a partner addresses this site by a UUID this
    // instance does not hold, so the control has nothing to preselect.
    serve({ siteOrg: { fhirUuid: "895c8e6b-not-local", orgId: "" } });
    renderSettings();
    await waitFor(() =>
      expect(
        screen.getByText("Set to an organization this instance does not hold"),
      ).toBeTruthy(),
    );
    expect(screen.queryByText("Site organization not set")).toBeNull();
  });

  test("still warns when nothing is configured at all", async () => {
    serve({ siteOrg: { fhirUuid: "", orgId: "" } });
    renderSettings();
    await waitFor(() =>
      expect(screen.getByText("Site organization not set")).toBeTruthy(),
    );
    expect(
      screen.queryByText("Set to an organization this instance does not hold"),
    ).toBeNull();
  });
});
