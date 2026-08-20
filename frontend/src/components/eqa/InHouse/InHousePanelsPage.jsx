import React, { useEffect, useState } from "react";
import {
  Button,
  Column,
  Grid,
  InlineNotification,
  Select,
  SelectItem,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
  Tag,
  Tile,
} from "@carbon/react";
import { Locked } from "@carbon/react/icons";
import { useIntl } from "react-intl";
import { useHistory } from "react-router-dom";
import PageBreadCrumb from "../../common/PageBreadCrumb";
import {
  downloadLabelSheet,
  fetchInHouseSchemes,
  fetchPanelsForScheme,
  unblindPanel,
} from "./inHouseApi";
import { panelKpis, sealState } from "./blindingRules";

const breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "banner.menu.eqa.inHouse", link: "/qa/eqa/in-house" },
];

const STATUS_TAG = {
  PREPARING: "gray",
  SEALED: "purple",
  DISTRIBUTED: "blue",
  UNBLINDED: "teal",
  SCORED: "green",
  CLOSED: "gray",
};

const InHousePanelsPage = () => {
  const intl = useIntl();
  const history = useHistory();
  const [schemes, setSchemes] = useState([]);
  const [schemeId, setSchemeId] = useState("");
  const [panels, setPanels] = useState([]);
  const [notification, setNotification] = useState(null);

  const label = (id, fallback) =>
    intl.formatMessage({ id, defaultMessage: fallback });

  const kpis = panelKpis(panels);

  const targetValuesCell = (panel) => {
    const seal = sealState(panel);
    if (!seal.key) {
      return "—";
    }
    if (seal.sealed) {
      return (
        <Tag type="purple">
          <Locked size={12} /> {label("eqa.inhouse.seal.sealed", "Sealed")}
        </Tag>
      );
    }
    return `${label("eqa.inhouse.seal.unsealed", "Unsealed")} ${seal.date || ""}`.trim();
  };

  useEffect(() => {
    fetchInHouseSchemes((data) => {
      setSchemes(data);
      if (data.length > 0) {
        setSchemeId(String(data[0].id));
      }
    });
  }, []);

  const reload = (id) => fetchPanelsForScheme(id, setPanels);

  useEffect(() => {
    if (!schemeId) {
      setPanels([]);
      return;
    }
    reload(schemeId);
  }, [schemeId]);

  const unblind = (panelId) => {
    unblindPanel(panelId, (response) => {
      if (
        !response ||
        response.error ||
        (response.status && response.status >= 400)
      ) {
        setNotification({
          kind: "error",
          message:
            response?.error ||
            label("eqa.inhouse.unblind.error", "Could not unblind this panel"),
        });
        return;
      }
      setNotification({
        kind: "success",
        message: label(
          "eqa.inhouse.unblind.done",
          "Panel unblinded and scored",
        ),
      });
      reload(schemeId);
    });
  };

  return (
    <>
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <Grid fullWidth>
        <Column lg={16} md={8} sm={4}>
          <h3>{label("banner.menu.eqa.inHouse", "In-House Blinding")}</h3>
        </Column>

        {notification && (
          <Column lg={16} md={8} sm={4}>
            <InlineNotification
              kind={notification.kind}
              title={notification.message}
              onCloseButtonClick={() => setNotification(null)}
            />
          </Column>
        )}

        <Column lg={8} md={4} sm={4}>
          <Select
            id="inhouse-scheme-filter"
            labelText={label("eqa.inhouse.scheme", "In-house scheme")}
            value={schemeId}
            onChange={(e) => setSchemeId(e.target.value)}
          >
            <SelectItem value="" text="" />
            {schemes.map((scheme) => (
              <SelectItem
                key={scheme.id}
                value={scheme.id}
                text={scheme.name}
              />
            ))}
          </Select>
        </Column>
        <Column lg={8} md={4} sm={4}>
          <Button onClick={() => history.push("/qa/eqa/in-house/new")}>
            {label("eqa.inhouse.launchWizard", "Launch blinding wizard")}
          </Button>
        </Column>

        {panels.length > 0 &&
          [
            [
              "eqa.inhouse.kpi.awaitingDistribution",
              "Sealed, awaiting distribution",
              kpis.awaitingDistribution,
            ],
            ["eqa.inhouse.kpi.inTesting", "In testing", kpis.inTesting],
            [
              "eqa.inhouse.kpi.unblindingSoon",
              "Unblinding within 7 days",
              kpis.unblindingSoon,
            ],
            ["eqa.inhouse.kpi.closed", "Scored or closed", kpis.closed],
          ].map(([key, fallback, value]) => (
            <Column key={key} lg={4} md={2} sm={2}>
              <Tile>
                <div>{label(key, fallback)}</div>
                <h4>{value}</h4>
              </Tile>
            </Column>
          ))}

        <Column lg={16} md={8} sm={4}>
          {schemes.length === 0 ? (
            <Tile>
              {label(
                "eqa.inhouse.noSchemes",
                "No in-house scheme exists yet. Create one under EQA scheme management first.",
              )}
            </Tile>
          ) : (
            <Table size="sm">
              <TableHead>
                <TableRow>
                  <TableHeader>
                    {label("eqa.inhouse.panel", "Panel")}
                  </TableHeader>
                  <TableHeader>
                    {label("eqa.inhouse.cycle", "Cycle")}
                  </TableHeader>
                  <TableHeader>
                    {label("eqa.inhouse.samples", "Samples")}
                  </TableHeader>
                  <TableHeader>
                    {label("eqa.inhouse.targetValues", "Target values")}
                  </TableHeader>
                  <TableHeader>
                    {label("eqa.inhouse.status", "Status")}
                  </TableHeader>
                  <TableHeader>
                    {label(
                      "eqa.inhouse.unblindDate",
                      "Unblind date (submission deadline)",
                    )}
                  </TableHeader>
                  <TableHeader>
                    {label("eqa.inhouse.actions", "Actions")}
                  </TableHeader>
                </TableRow>
              </TableHead>
              <TableBody>
                {panels.map((panel) => (
                  <TableRow key={panel.id}>
                    <TableCell>{panel.panelName}</TableCell>
                    <TableCell>
                      {panel.cycleName || panel.cycleNumber || "—"}
                    </TableCell>
                    <TableCell>{panel.sampleCount}</TableCell>
                    <TableCell>{targetValuesCell(panel)}</TableCell>
                    <TableCell>
                      <Tag type={STATUS_TAG[panel.status] || "gray"}>
                        {panel.status}
                      </Tag>
                    </TableCell>
                    <TableCell>{panel.unblindDate || "—"}</TableCell>
                    <TableCell>
                      <Button
                        kind="ghost"
                        size="sm"
                        onClick={() =>
                          downloadLabelSheet(panel.id, () =>
                            setNotification({
                              kind: "error",
                              message: label(
                                "eqa.inhouse.labels.error",
                                "Could not generate the label sheet",
                              ),
                            }),
                          )
                        }
                      >
                        {label("eqa.inhouse.labels.print", "Print label sheet")}
                      </Button>
                      {panel.status === "DISTRIBUTED" && (
                        <Button
                          kind="ghost"
                          size="sm"
                          onClick={() => unblind(panel.id)}
                        >
                          {label("eqa.inhouse.unblind.now", "Unblind now")}
                        </Button>
                      )}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </Column>
      </Grid>
    </>
  );
};

export default InHousePanelsPage;
