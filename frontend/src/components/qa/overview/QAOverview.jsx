import React from "react";
import { Column, Grid } from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import PageBreadCrumb from "../../common/PageBreadCrumb";
import AttentionRequired from "./AttentionRequired";
import TodayTiles from "./TodayTiles";
import ThisWeek from "./ThisWeek";
import PillarStatus from "./PillarStatus";
import RecentActivity from "./RecentActivity";
import InspectorReadiness from "./InspectorReadiness";
import "./QAOverview.css";

/**
 * QA Overview landing page (OGC-694).
 *
 * End-state six-section layout shipped as a shell: every slot renders a
 * ticket-annotated ComingSoon placeholder that later features replace
 * with live components (TAT, NCE, rate tiles,
 * aggregators — see the OGC-683 delivery plan).
 */

const breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "sideNav.label.qa", link: "" },
  { label: "sideNav.label.qa.overview", link: "" },
];

const QAOverview = () => {
  const intl = useIntl();
  return (
    <div className="pageContent">
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <Grid fullWidth>
        <Column lg={16} md={8} sm={4}>
          <h2>
            <FormattedMessage id="sideNav.label.qa.overview" />
          </h2>
          <p className="qa-overview-subtitle">
            <FormattedMessage id="qa.overview.subtitle" />
            {" · "}
            {intl.formatDate(new Date(), {
              weekday: "long",
              year: "numeric",
              month: "long",
              day: "numeric",
            })}
          </p>
          <AttentionRequired />
          <TodayTiles />
          <ThisWeek />
          <PillarStatus />
          <RecentActivity />
          <InspectorReadiness />
        </Column>
      </Grid>
    </div>
  );
};

export default QAOverview;
