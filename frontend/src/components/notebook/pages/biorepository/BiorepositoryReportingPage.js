import React, { useState } from "react";
import { Tabs, TabList, Tab, TabPanels, TabPanel } from "@carbon/react";
import { FormattedMessage } from "react-intl";
import PropTypes from "prop-types";
import OverviewDashboardTab from "./reporting/OverviewDashboardTab";
import DetailedMetricsTab from "./reporting/DetailedMetricsTab";
import AuditTrailTab from "./reporting/AuditTrailTab";
import ExportReportsTab from "./reporting/ExportReportsTab";

/**
 * BiorepositoryReportingPage - Reporting & Audit workflow page
 * Stage 7 of the Biorepository workflow
 *
 * Provides 4 tabs:
 * 1. Overview Dashboard - High-level KPIs and charts
 * 2. Detailed Metrics - Drill-down tables with filtering
 * 3. Audit Trail - Immutable chain of custody log
 * 4. Export Reports - Multi-format export (CSV/Excel/JSON/PDF)
 *
 * @param {Object} props
 * @param {number} props.entryId - The notebook entry ID
 * @param {Object} props.pageData - Page configuration from notebook
 * @param {Object} props.progress - Progress tracking data
 * @param {Function} props.onProgressUpdate - Callback when progress changes
 * @param {number} props.notebookId - The notebook ID
 */
function BiorepositoryReportingPage({
  entryId,
  pageData,
  progress,
  onProgressUpdate,
  notebookId,
}) {
  const [selectedTabIndex, setSelectedTabIndex] = useState(0);

  return (
    <div className="biorepository-reporting-page">
      <Tabs
        selectedIndex={selectedTabIndex}
        onChange={({ selectedIndex }) => setSelectedTabIndex(selectedIndex)}
      >
        <TabList aria-label="Biorepository Reporting" contained>
          <Tab>
            <FormattedMessage
              id="biorepository.reporting.overview"
              defaultMessage="Overview"
            />
          </Tab>
          <Tab>
            <FormattedMessage
              id="biorepository.reporting.metrics"
              defaultMessage="Detailed Metrics"
            />
          </Tab>
          <Tab>
            <FormattedMessage
              id="biorepository.reporting.audit"
              defaultMessage="Audit Trail"
            />
          </Tab>
          <Tab>
            <FormattedMessage
              id="biorepository.reporting.export"
              defaultMessage="Export Reports"
            />
          </Tab>
        </TabList>
        <TabPanels>
          <TabPanel>
            <OverviewDashboardTab
              entryId={entryId}
              notebookId={notebookId}
              pageData={pageData}
            />
          </TabPanel>
          <TabPanel>
            <DetailedMetricsTab
              entryId={entryId}
              notebookId={notebookId}
              pageData={pageData}
            />
          </TabPanel>
          <TabPanel>
            <AuditTrailTab
              entryId={entryId}
              notebookId={notebookId}
              pageData={pageData}
            />
          </TabPanel>
          <TabPanel>
            <ExportReportsTab
              entryId={entryId}
              notebookId={notebookId}
              pageData={pageData}
            />
          </TabPanel>
        </TabPanels>
      </Tabs>
    </div>
  );
}

BiorepositoryReportingPage.propTypes = {
  entryId: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
  pageData: PropTypes.object,
  progress: PropTypes.object,
  onProgressUpdate: PropTypes.func,
  notebookId: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
};

export default BiorepositoryReportingPage;
