import {
  Add,
  Dashboard,
  Download,
  Report,
  Settings,
} from "@carbon/icons-react";
import { Tab, TabList, Tabs } from "@carbon/react";
import { useIntl } from "react-intl";
import { useHistory, useLocation } from "react-router-dom";
import "./ShipmentNavigation.css";

const ShipmentNavigation = () => {
  const intl = useIntl();
  const history = useHistory();
  const location = useLocation();

  const navigationOptions = [
    {
      key: "dashboard",
      label: intl.formatMessage({ id: "shipment.nav.dashboard" }),
      icon: Dashboard,
      path: "/SampleShipment",
    },
    {
      key: "create",
      label: intl.formatMessage({ id: "shipment.nav.createBox" }),
      icon: Add,
      path: "/SampleShipment/create-box",
    },
    {
      key: "receive",
      label: intl.formatMessage({ id: "shipment.nav.receiveBox" }),
      icon: Download,
      path: "/SampleShipment/receive",
    },
    {
      key: "reports",
      label: intl.formatMessage({ id: "shipment.nav.reports" }),
      icon: Report,
      path: "/SampleShipment/reports",
    },
    {
      key: "settings",
      label: intl.formatMessage({ id: "shipment.nav.settings" }),
      icon: Settings,
      path: "/SampleShipment/settings",
    },
  ];

  // Determine which tab is currently active based on pathname
  const getActiveIndex = () => {
    const path = location.pathname;
    if (path === "/SampleShipment") return 0;
    if (path.includes("/create-box")) return 1;
    if (path.includes("/receive")) return 2;
    if (path.includes("/reports")) return 3;
    if (path.includes("/settings")) return 4;
    // Default to dashboard if viewing box details
    if (path.includes("/SampleShipment/box/")) return 0;
    return 0;
  };

  const handleTabChange = (evt) => {
    const selectedIndex = evt.selectedIndex;
    const selectedOption = navigationOptions[selectedIndex];
    history.push(selectedOption.path);
  };

  return (
    <div className="shipment-navigation">
      <Tabs
        selectedIndex={getActiveIndex()}
        onChange={handleTabChange}
        autoWidth
      >
        <TabList aria-label="Shipment navigation tabs" contained>
          {navigationOptions.map((option) => {
            const IconComponent = option.icon;
            return (
              <Tab key={option.key}>
                <IconComponent size={16} style={{ marginRight: "0.5rem" }} />
                {option.label}
              </Tab>
            );
          })}
        </TabList>
      </Tabs>
    </div>
  );
};

export default ShipmentNavigation;
