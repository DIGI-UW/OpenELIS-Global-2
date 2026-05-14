import React from "react";
import { FormattedMessage } from "react-intl";
import { useHistory } from "react-router-dom";
import { ClickableTile, Column, Grid } from "@carbon/react";
import {
  ArrowRight,
  ConnectionSignal,
  ContainerSoftware,
  ResultNew,
  Settings,
  TableOfContents,
  User,
} from "@carbon/icons-react";

const ADMIN_DASHBOARD_LINKS = [
  {
    messageId: "unifiedSystemUser.browser.title",
    path: "userManagement",
    icon: User,
  },
  {
    messageId: "organization.main.title",
    path: "organizationManagement",
    icon: ContainerSoftware,
  },
  {
    messageId: "master.lists.page.test.management",
    path: "testManagementConfigMenu",
    icon: ResultNew,
  },
  {
    messageId: "sidenav.label.admin.menu",
    path: "globalMenuManagement",
    icon: TableOfContents,
  },
  {
    messageId: "sidenav.label.admin.commonproperties",
    path: "commonproperties",
    icon: Settings,
  },
  {
    messageId: "externalconnections.browse.title",
    path: "externalConnections",
    icon: ConnectionSignal,
  },
];

export default function AdminDashboard({ basePath }) {
  const history = useHistory();

  const handleNavigation = (targetPath) => (event) => {
    event.preventDefault();
    history.push(targetPath);
  };

  return (
    <section className="admin-dashboard" data-testid="admin-dashboard">
      <h2>
        <FormattedMessage id="admin.dashboard.title" />
      </h2>
      <p className="admin-dashboard__subtitle">
        <FormattedMessage id="admin.dashboard.subtitle" />
      </p>
      <Grid className="admin-dashboard__grid">
        {ADMIN_DASHBOARD_LINKS.map((link) => {
          const targetPath = `${basePath}/${link.path}`;
          const Icon = link.icon;
          return (
            <Column key={link.path} lg={5} md={4} sm={4}>
              <ClickableTile
                className="admin-dashboard__tile"
                data-testid="admin-dashboard-tile"
                href={targetPath}
                onClick={handleNavigation(targetPath)}
              >
                <span className="admin-dashboard__tile-main">
                  <Icon className="admin-dashboard__tile-icon" size={24} />
                  <span className="admin-dashboard__tile-label">
                    <FormattedMessage id={link.messageId} />
                  </span>
                </span>
                <ArrowRight className="admin-dashboard__tile-arrow" size={20} />
              </ClickableTile>
            </Column>
          );
        })}
      </Grid>
    </section>
  );
}
