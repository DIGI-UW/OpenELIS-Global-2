import { Grid, Column } from "@carbon/react";
import { FormattedMessage } from "react-intl";
import PageBreadCrumb from "../common/PageBreadCrumb";
import EquipmentUsageLog from "./EquipmentUsageLog";
import "./EquipmentUsage.css";

const breadcrumbs = [
  { label: "home.label", link: "/", defaultMessage: "Home" },
  {
    label: "sidenav.label.inventory.management",
    link: "/inventory",
    defaultMessage: "Inventory Management",
  },
  {
    label: "equipment.usage.title",
    link: "/equipment-usage",
    defaultMessage: "Equipment Usage",
  },
];

/**
 * EquipmentUsageManagement Component
 *
 * Main container for equipment (cartridge) usage tracking feature.
 * Displays the Equipment Usage Log form for recording cartridge consumption.
 */
const EquipmentUsageManagement = () => {
  return (
    <>
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <div className="equipmentUsageManagementContainer">
            <h2>
              <FormattedMessage
                id="equipment.usage.title"
                defaultMessage="Equipment Usage Log"
              />
            </h2>

            <EquipmentUsageLog />
          </div>
        </Column>
      </Grid>
    </>
  );
};

export default EquipmentUsageManagement;
