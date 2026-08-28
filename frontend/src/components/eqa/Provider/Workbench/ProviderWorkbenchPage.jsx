import React, { useCallback, useEffect, useState } from "react";
import {
  Column,
  Grid,
  Heading,
  InlineNotification,
  Loading,
  Section,
  Tab,
  TabList,
  TabPanel,
  TabPanels,
  Tabs,
} from "@carbon/react";
import { useIntl } from "react-intl";
import { useParams } from "react-router-dom";
import PageBreadCrumb from "../../../common/PageBreadCrumb";
import CycleStateBanner from "../../CycleStateBanner";
import { fetchPrepStatus, fetchShipmentRows } from "./workbenchApi";
import PrepWorkbench from "./PrepWorkbench";
import ReceiptMonitor from "./ReceiptMonitor";
import ReportComments from "./ReportComments";
import ShipmentWorkbench from "./ShipmentWorkbench";

const breadcrumbs = (cycleId) => [
  { label: "home.label", link: "/" },
  { label: "banner.menu.eqa.provider", link: "/qa/eqa/provider/schemes" },
  {
    label: "eqa.provider.workbench.cycle",
    link: `/qa/eqa/provider/cycles/${cycleId}/workbench`,
  },
];

/**
 * Provider prep + shipment workbenches for one cycle (T-25, FR-V2.5-12/13).
 * Reached from the provider scheme list (T-24), which owns cycle selection.
 */
const ProviderWorkbenchPage = () => {
  const intl = useIntl();
  const t = (id, defaultMessage, values) =>
    intl.formatMessage({ id, defaultMessage }, values);
  const { cycleId } = useParams();

  const [prep, setPrep] = useState(null);
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [notice, setNotice] = useState(null);

  // Reloading after a save keeps the rendered page; only a change of cycle goes
  // back to the spinner, since none of the current page's numbers survive it.
  const reload = useCallback(
    (withSpinner = false) => {
      if (withSpinner) {
        setLoading(true);
      }
      fetchPrepStatus(cycleId, (data) => {
        setPrep(data);
        setLoading(false);
      });
      fetchShipmentRows(cycleId, setRows);
    },
    [cycleId],
  );

  useEffect(() => {
    reload(true);
  }, [reload]);

  if (loading) {
    return <Loading />;
  }

  return (
    <>
      <PageBreadCrumb breadcrumbs={breadcrumbs(cycleId)} />
      <Grid fullWidth>
        <Column lg={16} md={8} sm={4}>
          <Section>
            <Heading>
              {t(
                "eqa.provider.workbench.forCycle",
                "Prep & shipping — {name}",
                {
                  name: prep?.cycleName || `#${cycleId}`,
                },
              )}
            </Heading>
          </Section>
          {notice && (
            <InlineNotification
              kind={notice.kind}
              lowContrast
              title={notice.text}
              onCloseButtonClick={() => setNotice(null)}
            />
          )}
        </Column>
      </Grid>

      <Grid fullWidth>
        <Column lg={16} md={8} sm={4}>
          <CycleStateBanner
            cycleId={cycleId}
            status={prep?.cycleStatus}
            distributionMethod={prep?.distributionMethod}
            hint={t(
              "eqa.provider.workbench.stateHint",
              "Prep must clear the inventory and QC gate before any panel can be dispatched.",
            )}
          />
          {/* Deliberate divergence from FR-V2.5-16's "no in-page Tabs — use
              sidebar children" (decided with the user, 2026-08-28): the four
              workbench surfaces share one cycle banner and one state, so
              sidebar child routes would multiply route plumbing for no
              workflow gain. */}
          <Tabs>
            <TabList
              aria-label={t("eqa.provider.workbench.tabs", "Workbenches")}
            >
              <Tab>{t("eqa.prep.tab", "Prep")}</Tab>
              <Tab>{t("eqa.shipment.tab", "Shipments")}</Tab>
              <Tab>{t("eqa.receipt.tab", "Receipts & scoring")}</Tab>
              <Tab>{t("eqa.report.comments.tab", "Report comments")}</Tab>
            </TabList>
            <TabPanels>
              <TabPanel>
                <PrepWorkbench
                  prep={prep}
                  onChanged={(updated) =>
                    updated ? setPrep(updated) : reload()
                  }
                  onNotice={setNotice}
                />
              </TabPanel>
              <TabPanel>
                <ShipmentWorkbench
                  cycleId={cycleId}
                  prep={prep}
                  rows={rows}
                  onChanged={reload}
                  onNotice={setNotice}
                />
              </TabPanel>
              <TabPanel>
                <ReceiptMonitor
                  cycleId={cycleId}
                  cycleStatus={prep?.cycleStatus}
                  onChanged={reload}
                  onNotice={setNotice}
                />
              </TabPanel>
              <TabPanel>
                <ReportComments cycleId={cycleId} onNotice={setNotice} />
              </TabPanel>
            </TabPanels>
          </Tabs>
        </Column>
      </Grid>
    </>
  );
};

export default ProviderWorkbenchPage;
