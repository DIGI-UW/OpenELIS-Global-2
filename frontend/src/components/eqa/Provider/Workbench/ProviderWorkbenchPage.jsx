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
  Tile,
} from "@carbon/react";
import { useIntl } from "react-intl";
import { useParams } from "react-router-dom";
import PageBreadCrumb from "../../../common/PageBreadCrumb";
import { CycleStatusTag, hintStyle } from "../../eqaCommon";
import { fetchPrepStatus, fetchShipmentRows } from "./workbenchApi";
import PrepWorkbench from "./PrepWorkbench";
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
          <Tile style={{ marginBottom: "1rem" }}>
            <CycleStatusTag status={prep?.cycleStatus} />
            <span style={{ ...hintStyle, marginLeft: "0.5rem" }}>
              {t(
                "eqa.provider.workbench.stateHint",
                "Prep must clear the inventory and QC gate before any panel can be dispatched.",
              )}
            </span>
          </Tile>
          <Tabs>
            <TabList
              aria-label={t("eqa.provider.workbench.tabs", "Workbenches")}
            >
              <Tab>{t("eqa.prep.tab", "Prep")}</Tab>
              <Tab>{t("eqa.shipment.tab", "Shipments")}</Tab>
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
            </TabPanels>
          </Tabs>
        </Column>
      </Grid>
    </>
  );
};

export default ProviderWorkbenchPage;
