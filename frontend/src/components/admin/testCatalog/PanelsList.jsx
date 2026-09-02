import React, { useEffect, useMemo, useState } from "react";
import { useHistory } from "react-router-dom";
import {
  Grid,
  Column,
  Section,
  Heading,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  TableContainer,
  TableToolbar,
  TableToolbarContent,
  Search,
  Dropdown,
  Button,
  Tag,
  Loading,
  InlineNotification,
} from "@carbon/react";
import { Add } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import { getFromOpenElisServer } from "../../utils/Utils";
import PageBreadCrumb from "../../common/PageBreadCrumb";
import useDomains from "../../common/useDomains";

/**
 * OGC-224 (Panel Management Domain Upgrade v2.2) — the Panels context of the
 * Test Catalog Management shell (/admin/TestCatalogList?entity=panels).
 *
 * Columns per the FRS: Panel Name · LOINC · Tests (count) · Domain (tag) ·
 * Sample Types (derived from member tests, read-only) · Status · Actions.
 * Filters: search (name / LOINC), Domain, Status. A row opens the panel
 * editor; "Add Panel" opens a blank panel in the same editor shell.
 */

const ALL_DOMAINS_OPTION = {
  id: "",
  label: "label.testCatalog.list.filter.allDomains",
};

const STATUS_OPTIONS = [
  { id: "all", label: "label.testCatalog.list.filter.allStatus" },
  { id: "active", label: "label.testCatalog.basicInfo.active" },
  { id: "inactive", label: "label.testCatalog.list.filter.inactive" },
];

export const domainTagType = (domain) =>
  domain === "CLINICAL" ? "blue" : "teal";

/** search (name/LOINC) + domain + status, applied client-side. */
export const filterPanels = (panels, { search, domain, status }) => {
  const needle = (search || "").trim().toLowerCase();
  return (panels || []).filter((panel) => {
    if (
      needle &&
      !(panel.name || "").toLowerCase().includes(needle) &&
      !(panel.loinc || "").toLowerCase().includes(needle)
    ) {
      return false;
    }
    if (domain && panel.domain !== domain) {
      return false;
    }
    if (status === "active" && !panel.active) {
      return false;
    }
    if (status === "inactive" && panel.active) {
      return false;
    }
    return true;
  });
};

const PanelsList = () => {
  const intl = useIntl();
  const history = useHistory();
  const domainOptions = [
    ALL_DOMAINS_OPTION,
    ...useDomains().map((d) => ({ id: d.id, label: d.labelKey })),
  ];

  const initParams = new URLSearchParams(history.location.search);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const [panels, setPanels] = useState([]);
  const [search, setSearch] = useState(initParams.get("search") || "");
  const [domain, setDomain] = useState(initParams.get("domain") || "");
  const [status, setStatus] = useState(initParams.get("status") || "all");

  useEffect(() => {
    setLoading(true);
    setError(false);
    getFromOpenElisServer(
      "/rest/test-catalog/panels?includeInactive=true",
      (res) => {
        setLoading(false);
        if (Array.isArray(res)) {
          setPanels(res);
        } else {
          setError(true);
          setPanels([]);
        }
      },
    );
  }, []);

  // Mirror the filters into the URL (keeping entity=panels) so a reload
  // restores them — same contract as the Tests list.
  useEffect(() => {
    const params = new URLSearchParams();
    params.set("entity", "panels");
    if (search) params.set("search", search);
    if (domain) params.set("domain", domain);
    if (status && status !== "all") params.set("status", status);
    history.replace({ search: params.toString() });
  }, [search, domain, status, history]);

  const visiblePanels = useMemo(
    () => filterPanels(panels, { search, domain, status }),
    [panels, search, domain, status],
  );

  const openEditor = (panelId) =>
    history.push(
      `/MasterListsPage/TestCatalogEditor/panel/${panelId}/basic-info`,
    );

  const breadcrumbs = [
    { label: "home.label", link: "/" },
    { label: "breadcrums.admin.managment", link: "/MasterListsPage" },
    {
      label: "label.testCatalog.entity.panels",
      link: "/MasterListsPage/TestCatalogList?entity=panels",
    },
  ];

  return (
    <>
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <Grid fullWidth>
        <Column lg={16} md={8} sm={4}>
          <Section>
            <Heading>
              <FormattedMessage id="label.testCatalog.entity.panels" />
            </Heading>
          </Section>
          <InlineNotification
            kind="info"
            lowContrast
            hideCloseButton
            title={intl.formatMessage({ id: "note.panel.domainUpgrade" })}
            subtitle={intl.formatMessage({ id: "note.panel.domainLaterPhase" })}
          />
          {error && (
            <InlineNotification
              kind="error"
              lowContrast
              hideCloseButton
              title={intl.formatMessage({ id: "label.testCatalog.list.error" })}
            />
          )}
          <TableContainer
            description={intl.formatMessage({
              id: "label.panel.list.description",
            })}
          >
            <TableToolbar>
              <TableToolbarContent>
                <Search
                  size="lg"
                  labelText={intl.formatMessage({ id: "label.panel.search" })}
                  placeholder={intl.formatMessage({
                    id: "label.panel.search",
                  })}
                  value={search}
                  onChange={(e) => setSearch(e.target.value)}
                  data-testid="panel-search"
                />
                <Dropdown
                  id="panel-filter-domain"
                  size="lg"
                  titleText=""
                  label={intl.formatMessage({
                    id: "label.testCatalog.list.filter.allDomains",
                  })}
                  items={domainOptions}
                  itemToString={(item) =>
                    item ? intl.formatMessage({ id: item.label }) : ""
                  }
                  selectedItem={
                    domainOptions.find((o) => o.id === domain) ||
                    ALL_DOMAINS_OPTION
                  }
                  onChange={({ selectedItem }) =>
                    setDomain(selectedItem ? selectedItem.id : "")
                  }
                />
                <Dropdown
                  id="panel-filter-status"
                  size="lg"
                  titleText=""
                  label={intl.formatMessage({
                    id: "label.testCatalog.list.filter.allStatus",
                  })}
                  items={STATUS_OPTIONS}
                  itemToString={(item) =>
                    item ? intl.formatMessage({ id: item.label }) : ""
                  }
                  selectedItem={
                    STATUS_OPTIONS.find((o) => o.id === status) ||
                    STATUS_OPTIONS[0]
                  }
                  onChange={({ selectedItem }) =>
                    setStatus(selectedItem ? selectedItem.id : "all")
                  }
                />
                <Button
                  renderIcon={Add}
                  size="lg"
                  onClick={() => openEditor("new")}
                  data-testid="panel-add"
                >
                  <FormattedMessage id="button.panel.add" />
                </Button>
              </TableToolbarContent>
            </TableToolbar>
            {loading ? (
              <Loading small withOverlay={false} />
            ) : (
              <Table size="lg" data-testid="panels-table">
                <TableHead>
                  <TableRow>
                    <TableHeader>
                      <FormattedMessage id="label.panel.col.name" />
                    </TableHeader>
                    <TableHeader>LOINC</TableHeader>
                    <TableHeader>
                      <FormattedMessage id="label.panel.col.tests" />
                    </TableHeader>
                    <TableHeader>
                      <FormattedMessage id="label.panel.col.domain" />
                    </TableHeader>
                    <TableHeader>
                      <FormattedMessage id="label.panel.col.sampleTypes" />
                    </TableHeader>
                    <TableHeader>
                      <FormattedMessage id="label.panel.col.status" />
                    </TableHeader>
                    <TableHeader />
                  </TableRow>
                </TableHead>
                <TableBody>
                  {visiblePanels.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={7}>
                        <FormattedMessage id="label.panel.list.empty" />
                      </TableCell>
                    </TableRow>
                  ) : (
                    visiblePanels.map((panel) => (
                      <TableRow key={panel.id}>
                        <TableCell>
                          <Button
                            kind="ghost"
                            size="sm"
                            onClick={() => openEditor(panel.id)}
                          >
                            {panel.name}
                          </Button>
                        </TableCell>
                        <TableCell>
                          {panel.loinc ? <code>{panel.loinc}</code> : "—"}
                        </TableCell>
                        <TableCell>{panel.testCount}</TableCell>
                        <TableCell>
                          <Tag type={domainTagType(panel.domain)} size="sm">
                            {intl.formatMessage({
                              id: `label.domain.${panel.domain}`,
                              defaultMessage: panel.domain,
                            })}
                          </Tag>
                        </TableCell>
                        <TableCell>
                          {(panel.sampleTypes || []).length
                            ? panel.sampleTypes.map((typeName) => (
                                <Tag key={typeName} type="gray" size="sm">
                                  {typeName}
                                </Tag>
                              ))
                            : "—"}
                        </TableCell>
                        <TableCell>
                          <Tag type={panel.active ? "green" : "gray"} size="sm">
                            <FormattedMessage
                              id={
                                panel.active
                                  ? "label.testCatalog.basicInfo.active"
                                  : "label.testCatalog.list.filter.inactive"
                              }
                            />
                          </Tag>
                        </TableCell>
                        <TableCell>
                          <Button
                            kind="ghost"
                            size="sm"
                            onClick={() => openEditor(panel.id)}
                          >
                            <FormattedMessage id="label.button.edit" />
                          </Button>
                        </TableCell>
                      </TableRow>
                    ))
                  )}
                </TableBody>
              </Table>
            )}
          </TableContainer>
        </Column>
      </Grid>
    </>
  );
};

export default PanelsList;
