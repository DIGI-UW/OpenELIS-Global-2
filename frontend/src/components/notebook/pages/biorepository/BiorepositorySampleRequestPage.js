import React, { useState, useCallback, useEffect } from "react";
import {
  Grid,
  Column,
  Tabs,
  TabList,
  Tab,
  TabPanels,
  TabPanel,
  Tag,
  InlineNotification,
} from "@carbon/react";
import { Add, CheckmarkOutline, InProgress, Time } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import PropTypes from "prop-types";
import { getFromOpenElisServer } from "../../../utils/Utils";
import RequestSubmissionTab from "./RequestSubmissionTab";
import PendingApprovalsTab from "./PendingApprovalsTab";
import ActiveRetrievalsTab from "./ActiveRetrievalsTab";
import RetrievalHistoryTab from "./RetrievalHistoryTab";

/**
 * BiorepositorySampleRequestPage - Sample Request & Retrieval workflow page
 * Stage 4 of the Biorepository workflow
 *
 * Tabs:
 * 1. Request Submission - Create new retrieval requests
 * 2. Pending Approvals - Supervisor approval queue
 * 3. Active Retrievals - Track checked-out samples
 * 4. History - Past requests (future implementation)
 */
function BiorepositorySampleRequestPage({
  entryId,
  pageData,
  progress,
  onProgressUpdate,
  notebookId,
}) {
  const intl = useIntl();
  const [activeTab, setActiveTab] = useState(0);
  const [stats, setStats] = useState({
    pending: 0,
    checkedOutCount: 0,
    overdueCount: 0,
  });

  // Load stats for badge counts
  const loadStats = useCallback(() => {
    getFromOpenElisServer("/rest/biorepository/retrieval/stats", (data) => {
      if (data && !data.error) {
        setStats(data);
      }
    });
  }, []);

  useEffect(() => {
    loadStats();
  }, [loadStats]);

  // Refresh stats when tab changes or after actions
  const handleRefresh = useCallback(() => {
    loadStats();
  }, [loadStats]);

  return (
    <div className="biorepository-sample-request-page">
      <Grid fullWidth>
        <Column lg={16} md={8} sm={4}>
          <div className="page-header" style={{ marginBottom: "1rem" }}>
            <h3>
              <FormattedMessage
                id="biorepository.retrieval.title"
                defaultMessage="Sample Request & Retrieval"
              />
            </h3>
            <p style={{ color: "#525252", marginTop: "0.5rem" }}>
              <FormattedMessage
                id="biorepository.retrieval.description"
                defaultMessage="Request samples from the biorepository, track approvals, and manage sample checkouts."
              />
            </p>
          </div>

          <Tabs
            selectedIndex={activeTab}
            onChange={({ selectedIndex }) => setActiveTab(selectedIndex)}
          >
            <TabList aria-label="Retrieval workflow tabs">
              <Tab renderIcon={Add}>
                <FormattedMessage
                  id="biorepository.retrieval.tab.request"
                  defaultMessage="New Request"
                />
              </Tab>
              <Tab renderIcon={CheckmarkOutline}>
                <span
                  style={{
                    display: "flex",
                    alignItems: "center",
                    gap: "0.5rem",
                  }}
                >
                  <FormattedMessage
                    id="biorepository.retrieval.tab.pending"
                    defaultMessage="Pending Approvals"
                  />
                  {stats.pending > 0 && (
                    <Tag type="blue" size="sm">
                      {stats.pending}
                    </Tag>
                  )}
                </span>
              </Tab>
              <Tab renderIcon={InProgress}>
                <span
                  style={{
                    display: "flex",
                    alignItems: "center",
                    gap: "0.5rem",
                  }}
                >
                  <FormattedMessage
                    id="biorepository.retrieval.tab.active"
                    defaultMessage="Active Retrievals"
                  />
                  {stats.checkedOutCount > 0 && (
                    <Tag type="green" size="sm">
                      {stats.checkedOutCount}
                    </Tag>
                  )}
                  {stats.overdueCount > 0 && (
                    <Tag type="red" size="sm">
                      {stats.overdueCount}{" "}
                      {intl.formatMessage({
                        id: "biorepository.retrieval.overdue",
                        defaultMessage: "overdue",
                      })}
                    </Tag>
                  )}
                </span>
              </Tab>
              <Tab renderIcon={Time}>
                <FormattedMessage
                  id="biorepository.retrieval.tab.history"
                  defaultMessage="History"
                />
              </Tab>
            </TabList>

            <TabPanels>
              {/* Tab 1: Request Submission */}
              <TabPanel>
                <RequestSubmissionTab
                  onRequestCreated={() => {
                    handleRefresh();
                    setActiveTab(1); // Go to pending approvals
                  }}
                />
              </TabPanel>

              {/* Tab 2: Pending Approvals */}
              <TabPanel>
                <PendingApprovalsTab onActionComplete={handleRefresh} />
              </TabPanel>

              {/* Tab 3: Active Retrievals */}
              <TabPanel>
                <ActiveRetrievalsTab onActionComplete={handleRefresh} />
              </TabPanel>

              {/* Tab 4: History */}
              <TabPanel>
                <RetrievalHistoryTab onActionComplete={handleRefresh} />
              </TabPanel>
            </TabPanels>
          </Tabs>
        </Column>
      </Grid>
    </div>
  );
}

BiorepositorySampleRequestPage.propTypes = {
  entryId: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
  pageData: PropTypes.object,
  progress: PropTypes.object,
  onProgressUpdate: PropTypes.func,
  notebookId: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
};

export default BiorepositorySampleRequestPage;
