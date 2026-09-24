import React, { useContext, useEffect, useState } from "react";
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
import UserSessionDetailsContext from "../../../UserSessionDetailsContext";
import { hasQaPermission } from "../../utils/Utils";
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
  const { userSessionDetails } = useContext(UserSessionDetailsContext);
  // Sealing a panel is a manage-grant write; the wizard's every step ends there.
  const canManage = hasQaPermission(userSessionDetails, "qa.manage.eqa");
  const intl = useIntl();
  const history = useHistory();
  const [schemes, setSchemes] = useState([]);
  const [schemeId, setSchemeId] = useState("");
  const [panels, setPanels] = useState([]);
  const [notification, setNotification] = useState(null);

  const kpis = panelKpis(panels);

  const targetValuesCell = (panel) => {
    const seal = sealState(panel);
    if (!seal.key) {
      return "—";
    }
    if (seal.sealed) {
      return (
        <Tag type="purple">
          <Locked size={12} />{" "}
          {intl.formatMessage({ id: "eqa.inhouse.seal.sealed" })}
        </Tag>
      );
    }
    return `${intl.formatMessage({ id: "eqa.inhouse.seal.unsealed" })} ${seal.date || ""}`.trim();
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
            intl.formatMessage({ id: "eqa.inhouse.unblind.error" }),
        });
        return;
      }
      setNotification({
        kind: "success",
        message: intl.formatMessage({ id: "eqa.inhouse.unblind.done" }),
      });
      reload(schemeId);
    });
  };

  return (
    <>
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <Grid fullWidth>
        <Column lg={16} md={8} sm={4}>
          <h3>{intl.formatMessage({ id: "banner.menu.eqa.inHouse" })}</h3>
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
            labelText={intl.formatMessage({ id: "eqa.inhouse.scheme" })}
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
          {canManage ? (
            <Button onClick={() => history.push("/qa/eqa/in-house/new")}>
              {intl.formatMessage({ id: "eqa.inhouse.launchWizard" })}
            </Button>
          ) : (
            // Four steps of panel design in front of a seal this persona cannot
            // perform is worse than no launcher at all.
            <InlineNotification
              kind="info"
              lowContrast
              hideCloseButton
              title={intl.formatMessage({ id: "eqa.inhouse.readOnly.title" })}
              subtitle={intl.formatMessage({ id: "eqa.inhouse.readOnly.body" })}
            />
          )}
        </Column>

        {panels.length > 0 &&
          [
            ["eqa.inhouse.kpi.awaitingDistribution", kpis.awaitingDistribution],
            ["eqa.inhouse.kpi.inTesting", kpis.inTesting],
            ["eqa.inhouse.kpi.unblindingSoon", kpis.unblindingSoon],
            ["eqa.inhouse.kpi.closed", kpis.closed],
          ].map(([key, value]) => (
            <Column key={key} lg={4} md={2} sm={2}>
              <Tile>
                <div>{intl.formatMessage({ id: key })}</div>
                <h4>{value}</h4>
              </Tile>
            </Column>
          ))}

        <Column lg={16} md={8} sm={4}>
          {schemes.length === 0 ? (
            <Tile>{intl.formatMessage({ id: "eqa.inhouse.noSchemes" })}</Tile>
          ) : (
            <Table size="sm">
              <TableHead>
                <TableRow>
                  <TableHeader>
                    {intl.formatMessage({ id: "eqa.inhouse.panel" })}
                  </TableHeader>
                  <TableHeader>
                    {intl.formatMessage({ id: "eqa.inhouse.cycle" })}
                  </TableHeader>
                  <TableHeader>
                    {intl.formatMessage({ id: "eqa.inhouse.samples" })}
                  </TableHeader>
                  <TableHeader>
                    {intl.formatMessage({ id: "eqa.inhouse.targetValues" })}
                  </TableHeader>
                  <TableHeader>
                    {intl.formatMessage({ id: "eqa.inhouse.status" })}
                  </TableHeader>
                  <TableHeader>
                    {intl.formatMessage({ id: "eqa.inhouse.unblindDate" })}
                  </TableHeader>
                  <TableHeader>
                    {intl.formatMessage({ id: "eqa.inhouse.actions" })}
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
                              message: intl.formatMessage({
                                id: "eqa.inhouse.labels.error",
                              }),
                            }),
                          )
                        }
                      >
                        {intl.formatMessage({ id: "eqa.inhouse.labels.print" })}
                      </Button>
                      {panel.status === "DISTRIBUTED" && (
                        <Button
                          kind="ghost"
                          size="sm"
                          onClick={() => unblind(panel.id)}
                        >
                          {intl.formatMessage({
                            id: "eqa.inhouse.unblind.now",
                          })}
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
