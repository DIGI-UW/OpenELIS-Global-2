import React, { useCallback, useEffect, useState } from "react";
import {
  Column,
  Grid,
  Heading,
  InlineNotification,
  Loading,
  Section,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
  Tab,
  TabList,
  TabPanel,
  TabPanels,
  Tabs,
  Tag,
  Tile,
} from "@carbon/react";
import { useIntl } from "react-intl";
import { Link as RouterLink, useParams } from "react-router-dom";
import PageBreadCrumb from "../../../common/PageBreadCrumb";
import {
  fetchPrepStatus,
  fetchProviderCycles,
  fetchShipmentRows,
} from "./workbenchApi";
import PrepWorkbench from "./PrepWorkbench";
import ShipmentWorkbench from "./ShipmentWorkbench";

const STATUS_TAG = {
  PLANNED: "gray",
  PREP_IN_PROGRESS: "blue",
  READY_TO_SHIP: "purple",
  SHIPPED: "teal",
  DELIVERED: "cyan",
  SUBMISSIONS_OPEN: "cyan",
  SUBMISSIONS_CLOSED: "magenta",
  SCORING: "warm-gray",
  SCORED: "green",
  CLOSED: "gray",
};

const hintStyle = { fontSize: "0.75rem", color: "#525252" };

const breadcrumbs = (cycleId) => {
  const crumbs = [
    { label: "home.label", link: "/" },
    {
      label: "eqa.provider.workbench.title",
      link: "/qa/eqa/provider/workbench",
    },
  ];
  return cycleId
    ? [
        ...crumbs,
        {
          label: "eqa.provider.workbench.cycle",
          link: `/qa/eqa/provider/cycles/${cycleId}/workbench`,
        },
      ]
    : crumbs;
};

/**
 * Provider prep + shipment workbenches for one cycle (T-25, FR-V2.5-12/13).
 *
 * ponytail: with no :cycleId this page lists the provider cycles so the
 * workbench is reachable today — qa/019's provider menu row points at T-24's
 * scheme list, which does not exist yet. Delete this picker when T-24 lands.
 */
const ProviderWorkbenchPage = () => {
  const intl = useIntl();
  const t = (id, defaultMessage, values) =>
    intl.formatMessage({ id, defaultMessage }, values);
  const { cycleId } = useParams();

  const [cycles, setCycles] = useState([]);
  const [prep, setPrep] = useState(null);
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [notice, setNotice] = useState(null);

  const reload = useCallback(() => {
    if (!cycleId) {
      fetchProviderCycles((data) => {
        setCycles(data);
        setLoading(false);
      });
      return;
    }
    fetchPrepStatus(cycleId, (data) => {
      setPrep(data);
      setLoading(false);
    });
    fetchShipmentRows(cycleId, setRows);
  }, [cycleId]);

  // loading starts true and every fetch path clears it; the two routes are
  // separate Route paths, so a cycle change remounts rather than re-fetching.
  useEffect(() => {
    reload();
  }, [reload]);

  const statusTag = (status) => (
    <Tag type={STATUS_TAG[status] || "gray"} size="sm">
      {t(
        `eqa.cycle.status.${(status || "").toLowerCase()}`,
        (status || "").replace(/_/g, " "),
      )}
    </Tag>
  );

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
              {cycleId
                ? t(
                    "eqa.provider.workbench.forCycle",
                    "Prep & shipping — {name}",
                    { name: prep?.cycleName || `#${cycleId}` },
                  )
                : t("eqa.provider.workbench.title", "Prep & shipping")}
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

      {!cycleId && (
        <Grid fullWidth>
          <Column lg={16} md={8} sm={4}>
            {cycles.length === 0 ? (
              <InlineNotification
                kind="info"
                lowContrast
                hideCloseButton
                title={t(
                  "eqa.provider.noCycles.title",
                  "No provider cycles yet",
                )}
                subtitle={t(
                  "eqa.provider.noCycles.body",
                  "A cycle appears here once its scheme has at least one enrolled participant laboratory.",
                )}
              />
            ) : (
              <Table size="sm">
                <TableHead>
                  <TableRow>
                    <TableHeader>
                      {t("eqa.provider.cycle", "Cycle")}
                    </TableHeader>
                    <TableHeader>
                      {t("eqa.provider.scheme", "Scheme")}
                    </TableHeader>
                    <TableHeader>{t("eqa.cycle.status", "Status")}</TableHeader>
                    <TableHeader>
                      {t("eqa.prep.participants", "Participants")}
                    </TableHeader>
                    <TableHeader>{t("eqa.prep.panels", "Panels")}</TableHeader>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {cycles.map((cycle) => (
                    <TableRow key={cycle.id}>
                      <TableCell>
                        <RouterLink
                          to={`/qa/eqa/provider/cycles/${cycle.id}/workbench`}
                        >
                          {cycle.cycleName || `#${cycle.cycleNumber}`}
                        </RouterLink>
                      </TableCell>
                      <TableCell>{cycle.schemeName}</TableCell>
                      <TableCell>{statusTag(cycle.status)}</TableCell>
                      <TableCell>{cycle.participantCount}</TableCell>
                      <TableCell>{cycle.panelCount}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            )}
          </Column>
        </Grid>
      )}

      {cycleId && (
        <Grid fullWidth>
          <Column lg={16} md={8} sm={4}>
            <Tile style={{ marginBottom: "1rem" }}>
              {statusTag(prep?.cycleStatus)}
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
      )}
    </>
  );
};

export default ProviderWorkbenchPage;
