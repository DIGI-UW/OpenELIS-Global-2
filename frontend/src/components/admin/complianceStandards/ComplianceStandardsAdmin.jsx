import React, { useContext, useEffect, useMemo, useRef, useState } from "react";
import {
  Heading,
  Button,
  Loading,
  Grid,
  Column,
  Section,
  Table,
  TableContainer,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  TableExpandHeader,
  TableExpandRow,
  TableExpandedRow,
  Tag,
  Stack,
  Search,
  Select,
  SelectItem,
  Modal,
  Pagination,
  Tabs,
  TabList,
  Tab,
  TabPanels,
  TabPanel,
} from "@carbon/react";
import {
  Add,
  Archive,
  ArrowUp,
  ArrowDown,
  Copy,
  View,
} from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../utils/Utils";
import { NotificationContext } from "../../layout/Layout";
import {
  AlertDialog,
  NotificationKinds,
} from "../../common/CustomNotification";
import PageBreadCrumb from "../../common/PageBreadCrumb";
import StandardForm from "./StandardForm";
import TestComplianceThresholds from "./TestComplianceThresholds";
import { toDateString } from "./dateUtils";

const STATUS_TAG = {
  ACTIVE: "green",
  DRAFT: "blue",
  SUPERSEDED: "warm-gray",
  ARCHIVED: "gray",
};

const STATUSES = ["ACTIVE", "DRAFT", "SUPERSEDED", "ARCHIVED"];

const breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "breadcrums.admin.managment", link: "/MasterListsPage" },
  {
    label: "master.lists.page.test.management",
    link: "/MasterListsPage/testManagementConfigMenu",
  },
  {
    label: "compliance.admin.title",
    link: "/MasterListsPage/ComplianceStandardsAdmin",
  },
];

function LimitTypesLegend() {
  return (
    <div
      style={{
        display: "flex",
        alignItems: "center",
        gap: "0.5rem",
        padding: "0.5rem 0.75rem",
        background: "#fff",
        border: "1px solid var(--cds-border-subtle)",
        borderRadius: "4px",
        marginBottom: "1rem",
        flexWrap: "wrap",
      }}
    >
      <span
        style={{
          fontSize: "0.75rem",
          fontWeight: 600,
          letterSpacing: "0.05em",
          color: "var(--cds-text-02)",
          marginRight: "0.5rem",
        }}
      >
        <FormattedMessage
          id="compliance.legend.limitTypes"
          defaultMessage="LIMIT TYPES:"
        />
      </span>
      <Tag size="sm" type="red">
        <FormattedMessage
          id="compliance.limitType.high"
          defaultMessage="High Limit ≤"
        />
      </Tag>
      <Tag size="sm" type="blue">
        <FormattedMessage
          id="compliance.limitType.low"
          defaultMessage="Low Limit ≥"
        />
      </Tag>
      <Tag size="sm" type="teal">
        <FormattedMessage
          id="compliance.limitType.range"
          defaultMessage="Normal Range"
        />
      </Tag>
      <Tag size="sm" type="warm-gray">
        <FormattedMessage
          id="compliance.limitType.borderlineShort"
          defaultMessage="Borderline ⚑"
        />
      </Tag>
      <Tag size="sm" type="purple">
        <FormattedMessage
          id="compliance.limitType.descriptiveShort"
          defaultMessage="Qualitative"
        />
      </Tag>
    </div>
  );
}

function SampleTypesCell({ sampleTypes }) {
  if (!sampleTypes || sampleTypes.length === 0) {
    return (
      <em
        style={{
          fontSize: "0.75rem",
          color: "var(--cds-text-placeholder)",
        }}
      >
        <FormattedMessage
          id="compliance.sampleTypes.noneConfigured"
          defaultMessage="None configured"
        />
      </em>
    );
  }
  const visible = sampleTypes.slice(0, 2);
  const overflow = sampleTypes.length - visible.length;
  return (
    <div style={{ display: "flex", flexWrap: "wrap", gap: "0.375rem" }}>
      {visible.map((st) => (
        <Tag key={st} type="blue" size="sm">
          {st}
        </Tag>
      ))}
      {overflow > 0 && (
        <Tag type="blue" size="sm" title={sampleTypes.slice(2).join(", ")}>
          +{overflow}
        </Tag>
      )}
    </div>
  );
}

function ComplianceStandardsAdmin() {
  const intl = useIntl();
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);
  const [selectedTabIndex, setSelectedTabIndex] = useState(0);

  const [loading, setLoading] = useState(true);
  const [standards, setStandards] = useState([]);
  const [searchText, setSearchText] = useState("");
  const [statusFilter, setStatusFilter] = useState("");
  const [regionFilter, setRegionFilter] = useState("");
  // FR-1-008: filter by an applicable sample type — empty string means "any".
  const [sampleTypeFilter, setSampleTypeFilter] = useState("");
  const [addingNew, setAddingNew] = useState(false);
  const [expandedId, setExpandedId] = useState(null);
  const [archiveTarget, setArchiveTarget] = useState(null);
  // FR-4-002: read-only "View Linked Tests" panel — null when closed,
  // {standard, rows} when open. Rows are flat [groupId, groupName, testId,
  // testName, testCode]; the modal groups them client-side.
  const [linkedTestsTarget, setLinkedTestsTarget] = useState(null);
  const [linkedTestsLoading, setLinkedTestsLoading] = useState(false);
  const [sortBy, setSortBy] = useState("name");
  const [sortDir, setSortDir] = useState("asc");
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(25);

  const componentMounted = useRef(false);

  const reload = () => {
    setLoading(true);
    getFromOpenElisServer("/rest/compliance/standards", (data) => {
      if (componentMounted.current) {
        setStandards(Array.isArray(data) ? data : []);
        setLoading(false);
      }
    });
  };

  useEffect(() => {
    componentMounted.current = true;
    reload();
    return () => {
      componentMounted.current = false;
    };
  }, []);

  const regions = useMemo(() => {
    const set = new Set();
    standards.forEach((s) => {
      if (s.countryRegion) set.add(s.countryRegion);
    });
    return Array.from(set).sort();
  }, [standards]);

  // Distinct sample types declared across all loaded standards. Drives the
  // FR-1-008 sample-type filter dropdown. Computed locally so the dropdown
  // never offers a value that won't match anything.
  const sampleTypeOptions = useMemo(() => {
    const set = new Set();
    standards.forEach((s) => {
      (s.sampleTypes || []).forEach((t) => {
        if (t) set.add(t);
      });
    });
    return Array.from(set).sort();
  }, [standards]);

  const filtered = useMemo(() => {
    return standards.filter((s) => {
      if (statusFilter && s.status !== statusFilter) return false;
      if (regionFilter && s.countryRegion !== regionFilter) return false;
      if (
        sampleTypeFilter &&
        !(s.sampleTypes || []).includes(sampleTypeFilter)
      ) {
        return false;
      }
      if (searchText) {
        const q = searchText.toLowerCase();
        if (
          !(
            (s.name || "").toLowerCase().includes(q) ||
            (s.issuingBody || "").toLowerCase().includes(q) ||
            (s.regulationNumber || "").toLowerCase().includes(q) ||
            (s.countryRegion || "").toLowerCase().includes(q)
          )
        ) {
          return false;
        }
      }
      return true;
    });
  }, [standards, statusFilter, regionFilter, sampleTypeFilter, searchText]);

  const sorted = useMemo(() => {
    const arr = [...filtered];
    arr.sort((a, b) => {
      const av = (a[sortBy] || "").toString().toLowerCase();
      const bv = (b[sortBy] || "").toString().toLowerCase();
      const cmp = av < bv ? -1 : av > bv ? 1 : 0;
      return sortDir === "asc" ? cmp : -cmp;
    });
    return arr;
  }, [filtered, sortBy, sortDir]);

  const total = sorted.length;
  const pageStart = (page - 1) * pageSize;
  const pageRows = sorted.slice(pageStart, pageStart + pageSize);

  // Reset to page 1 on filter / sort changes
  useEffect(() => {
    setPage(1);
  }, [
    searchText,
    statusFilter,
    regionFilter,
    sampleTypeFilter,
    sortBy,
    sortDir,
  ]);

  const toggleSort = (col) => {
    if (sortBy === col) {
      setSortDir(sortDir === "asc" ? "desc" : "asc");
    } else {
      setSortBy(col);
      setSortDir("asc");
    }
  };
  const SortIcon = ({ col }) => {
    if (sortBy !== col) return null;
    return sortDir === "asc" ? (
      <ArrowUp size={12} style={{ marginLeft: "0.25rem" }} />
    ) : (
      <ArrowDown size={12} style={{ marginLeft: "0.25rem" }} />
    );
  };
  // Vertical separator between column headers — Carbon's default DataTable
  // header has no divider, which makes a wide table read like one big run-on
  // strip. A subtle right border + slight padding gives each column a clear
  // edge without changing the overall density.
  const headerSeparatorStyle = {
    borderRight: "1px solid var(--cds-border-subtle)",
    paddingRight: "0.75rem",
  };

  const sortableHeader = (col, labelEl) => (
    <TableHeader
      onClick={() => toggleSort(col)}
      style={{ cursor: "pointer", userSelect: "none", ...headerSeparatorStyle }}
      aria-sort={
        sortBy === col
          ? sortDir === "asc"
            ? "ascending"
            : "descending"
          : "none"
      }
    >
      <span style={{ display: "inline-flex", alignItems: "center" }}>
        {labelEl}
        <SortIcon col={col} />
      </span>
    </TableHeader>
  );

  const handleSaved = (saved) => {
    setNotificationVisible(true);
    addNotification({
      kind: NotificationKinds.success,
      title: intl.formatMessage({
        id: "notification.title.success",
        defaultMessage: "Saved",
      }),
      message: intl.formatMessage({
        id: "compliance.standard.saved",
        defaultMessage: "Compliance standard saved.",
      }),
    });
    reload();
    if (addingNew && saved?.id) {
      setAddingNew(false);
      setExpandedId(saved.id);
    } else {
      setExpandedId(null);
    }
  };

  // FR-1-006: archive (status → ARCHIVED) is the destructive surface for the
  // standards list — preserves audit trail and downstream evaluation results
  // while removing the standard from active selection lists. Hard delete
  // remains available server-side for admins cleaning up never-used Drafts,
  // but is no longer wired up from the UI to keep accidental data loss off
  // the table.
  const handleConfirmArchive = () => {
    if (!archiveTarget) return;
    postToOpenElisServerJsonResponse(
      `/rest/compliance/standards/${archiveTarget.id}/archive`,
      JSON.stringify({}),
      (resp) => {
        setArchiveTarget(null);
        if (!resp || resp.error) {
          setNotificationVisible(true);
          addNotification({
            kind: NotificationKinds.error,
            title: intl.formatMessage({
              id: "notification.title.error",
              defaultMessage: "Error",
            }),
            message: intl.formatMessage({
              id: "compliance.standard.archiveError",
              defaultMessage: "Could not archive this standard.",
            }),
          });
        } else {
          setNotificationVisible(true);
          addNotification({
            kind: NotificationKinds.success,
            title: intl.formatMessage({
              id: "notification.title.success",
              defaultMessage: "Archived",
            }),
            message: intl.formatMessage({
              id: "compliance.standard.archived",
              defaultMessage: "Compliance standard archived.",
            }),
          });
        }
        reload();
      },
    );
  };

  // FR-7-004: deep copy of a standard. The backend duplicates the standard
  // along with all parameter groups + thresholds + threshold value mappings
  // and returns a new DRAFT-status record with version suffixed " - Copy".
  // Admins typically use this to draft a new version of an existing
  // regulation without disturbing the live standard.
  const handleCopyStandard = (standard) => {
    if (!standard?.id) return;
    postToOpenElisServerJsonResponse(
      `/rest/compliance/standards/${standard.id}/copy`,
      JSON.stringify({}),
      (resp) => {
        if (!resp || resp.error) {
          setNotificationVisible(true);
          addNotification({
            kind: NotificationKinds.error,
            title: intl.formatMessage({
              id: "notification.title.error",
              defaultMessage: "Error",
            }),
            message: intl.formatMessage({
              id: "compliance.standard.copyError",
              defaultMessage: "Could not copy this standard.",
            }),
          });
        } else {
          setNotificationVisible(true);
          addNotification({
            kind: NotificationKinds.success,
            title: intl.formatMessage({
              id: "notification.title.success",
              defaultMessage: "Copied",
            }),
            message: intl.formatMessage({
              id: "compliance.standard.copied",
              defaultMessage: "Compliance standard copied as a new Draft.",
            }),
          });
        }
        reload();
      },
    );
  };

  const handleTabChange = ({ selectedIndex: idx }) => {
    setSelectedTabIndex(idx);
  };

  // FR-4-002: open the read-only Linked Tests panel for a standard. The
  // server returns one row per (group, test) pair; the modal groups them
  // by parameterGroup so the layout matches "organized by Parameter Group"
  // wording in the spec.
  const openLinkedTests = (standard) => {
    setLinkedTestsTarget({ standard, rows: [] });
    setLinkedTestsLoading(true);
    getFromOpenElisServer(
      `/rest/compliance/standards/${standard.id}/linked-tests`,
      (data) => {
        setLinkedTestsLoading(false);
        if (data && Array.isArray(data.linkedTests)) {
          setLinkedTestsTarget({ standard, rows: data.linkedTests });
        } else {
          setLinkedTestsTarget({ standard, rows: [] });
        }
      },
    );
  };

  return (
    <>
      {notificationVisible === true ? <AlertDialog /> : ""}
      {loading && <Loading />}
      <div className="adminPageContent">
        <PageBreadCrumb breadcrumbs={breadcrumbs} />
        <Grid fullWidth>
          <Column lg={16} md={8} sm={4}>
            <Section>
              <Heading>
                <FormattedMessage
                  id="compliance.admin.title"
                  defaultMessage="Compliance Standards"
                />
              </Heading>
            </Section>
          </Column>
        </Grid>
        <br />
        <div className="orderLegendBody">
          <Tabs selectedIndex={selectedTabIndex} onChange={handleTabChange}>
            <TabList aria-label="Compliance module" contained>
              <Tab>
                <FormattedMessage
                  id="compliance.tabs.standardsAdmin"
                  defaultMessage="Compliance Standards Administration"
                />
              </Tab>
              <Tab>
                <FormattedMessage
                  id="compliance.tabs.testThresholds"
                  defaultMessage="Test Compliance Thresholds"
                />
              </Tab>
            </TabList>
            <TabPanels>
              <TabPanel>
                <Grid fullWidth>
                  <Column lg={16} md={8} sm={4}>
                    <LimitTypesLegend />

                    {/* Filter bar */}
                    <div
                      style={{
                        background: "var(--cds-layer-01)",
                        border: "1px solid var(--cds-border-subtle)",
                        borderRadius: "4px",
                        padding: "0.75rem",
                        marginBottom: "1rem",
                      }}
                    >
                      <Stack gap={3}>
                        <Search
                          id="compliance-search"
                          labelText={intl.formatMessage({
                            id: "compliance.search",
                            defaultMessage: "Search standards",
                          })}
                          placeholder={intl.formatMessage({
                            id: "compliance.search.placeholder",
                            defaultMessage:
                              "Search by name, issuing body, or regulation number…",
                          })}
                          value={searchText}
                          onChange={(e) => setSearchText(e.target.value)}
                          size="lg"
                        />
                        <Select
                          id="compliance-status-filter"
                          labelText={intl.formatMessage({
                            id: "compliance.filter.status",
                            defaultMessage: "Status",
                          })}
                          hideLabel
                          value={statusFilter}
                          onChange={(e) => setStatusFilter(e.target.value)}
                        >
                          <SelectItem
                            value=""
                            text={intl.formatMessage({
                              id: "compliance.filter.allStatuses",
                              defaultMessage: "All Statuses",
                            })}
                          />
                          {STATUSES.map((s) => (
                            <SelectItem
                              key={s}
                              value={s}
                              text={intl.formatMessage({
                                id: `compliance.status.${s}`,
                                defaultMessage: s,
                              })}
                            />
                          ))}
                        </Select>
                        <Select
                          id="compliance-region-filter"
                          labelText={intl.formatMessage({
                            id: "compliance.filter.region",
                            defaultMessage: "Region",
                          })}
                          hideLabel
                          value={regionFilter}
                          onChange={(e) => setRegionFilter(e.target.value)}
                        >
                          <SelectItem
                            value=""
                            text={intl.formatMessage({
                              id: "compliance.filter.allRegions",
                              defaultMessage: "All Regions",
                            })}
                          />
                          {regions.map((r) => (
                            <SelectItem key={r} value={r} text={r} />
                          ))}
                        </Select>
                        <Select
                          id="compliance-sampletype-filter"
                          labelText={intl.formatMessage({
                            id: "compliance.filter.sampleType",
                            defaultMessage: "Sample Type",
                          })}
                          hideLabel
                          value={sampleTypeFilter}
                          onChange={(e) => setSampleTypeFilter(e.target.value)}
                        >
                          <SelectItem
                            value=""
                            text={intl.formatMessage({
                              id: "compliance.filter.allSampleTypes",
                              defaultMessage: "All Sample Types",
                            })}
                          />
                          {sampleTypeOptions.map((t) => (
                            <SelectItem key={t} value={t} text={t} />
                          ))}
                        </Select>
                        <div>
                          <Button
                            kind="primary"
                            renderIcon={Add}
                            onClick={() => {
                              setAddingNew(true);
                              setExpandedId(null);
                            }}
                          >
                            <FormattedMessage
                              id="compliance.button.addStandard"
                              defaultMessage="Add Standard"
                            />
                          </Button>
                        </div>
                      </Stack>
                    </div>

                    {addingNew && (
                      <div
                        style={{
                          background: "#edf2fa",
                          border: "1px solid var(--cds-border-subtle)",
                          borderRadius: "4px",
                          padding: "1rem",
                          marginBottom: "1rem",
                        }}
                      >
                        <h5 style={{ marginBottom: "1rem", marginTop: 0 }}>
                          <FormattedMessage
                            id="compliance.standard.addNew"
                            defaultMessage="Add New Compliance Standard"
                          />
                        </h5>
                        <StandardForm
                          standard={null}
                          isNew
                          hideHeading
                          onSaved={handleSaved}
                          onCancel={() => setAddingNew(false)}
                        />
                      </div>
                    )}

                    <TableContainer>
                      <Table size="md">
                        <TableHead>
                          <TableRow>
                            <TableExpandHeader />
                            {sortableHeader(
                              "name",
                              <FormattedMessage
                                id="compliance.standard.name"
                                defaultMessage="Standard Name"
                              />,
                            )}
                            {sortableHeader(
                              "issuingBody",
                              <FormattedMessage
                                id="compliance.standard.issuingBody"
                                defaultMessage="Issuing Body"
                              />,
                            )}
                            {sortableHeader(
                              "regulationNumber",
                              <FormattedMessage
                                id="compliance.standard.regulationNumber"
                                defaultMessage="Regulation No."
                              />,
                            )}
                            {sortableHeader(
                              "version",
                              <FormattedMessage
                                id="compliance.standard.version"
                                defaultMessage="Version"
                              />,
                            )}
                            {sortableHeader(
                              "effectiveDate",
                              <FormattedMessage
                                id="compliance.standard.effectiveDate"
                                defaultMessage="Effective Date"
                              />,
                            )}
                            {sortableHeader(
                              "status",
                              <FormattedMessage
                                id="compliance.standard.status"
                                defaultMessage="Status"
                              />,
                            )}
                            <TableHeader style={headerSeparatorStyle}>
                              <FormattedMessage
                                id="compliance.sampleTypes.heading"
                                defaultMessage="Sample Types"
                              />
                            </TableHeader>
                            <TableHeader
                              style={{ width: "5rem", ...headerSeparatorStyle }}
                            >
                              <FormattedMessage
                                id="compliance.standard.groups"
                                defaultMessage="Groups"
                              />
                            </TableHeader>
                            <TableHeader
                              style={{ width: "5rem", ...headerSeparatorStyle }}
                            >
                              <FormattedMessage
                                id="compliance.standard.tests"
                                defaultMessage="Tests"
                              />
                            </TableHeader>
                            <TableHeader style={{ width: "3rem" }}>
                              {" "}
                            </TableHeader>
                          </TableRow>
                        </TableHead>
                        <TableBody>
                          {pageRows.length === 0 && !loading && (
                            <TableRow>
                              <TableCell colSpan={11}>
                                <em>
                                  <FormattedMessage
                                    id="compliance.list.empty"
                                    defaultMessage="No compliance standards yet."
                                  />
                                </em>
                              </TableCell>
                            </TableRow>
                          )}
                          {pageRows.map((s) => {
                            const isOpen = expandedId === s.id;
                            return (
                              <React.Fragment key={s.id}>
                                <TableExpandRow
                                  isExpanded={isOpen}
                                  onExpand={() =>
                                    setExpandedId(isOpen ? null : s.id)
                                  }
                                  ariaLabel={s.name}
                                >
                                  <TableCell style={{ fontWeight: 500 }}>
                                    <div>{s.name}</div>
                                    {s.preSeeded && (
                                      <Tag
                                        size="sm"
                                        type="teal"
                                        style={{ marginTop: "0.25rem" }}
                                      >
                                        <FormattedMessage
                                          id="compliance.standard.default"
                                          defaultMessage="Default"
                                        />
                                      </Tag>
                                    )}
                                  </TableCell>
                                  <TableCell>{s.issuingBody}</TableCell>
                                  <TableCell>{s.regulationNumber}</TableCell>
                                  <TableCell>{s.version}</TableCell>
                                  <TableCell>
                                    {toDateString(s.effectiveDate) || "—"}
                                  </TableCell>
                                  <TableCell>
                                    <Tag
                                      size="sm"
                                      type={STATUS_TAG[s.status] || "gray"}
                                    >
                                      {s.status
                                        ? intl.formatMessage({
                                            id: `compliance.status.${s.status}`,
                                            defaultMessage: s.status,
                                          })
                                        : "—"}
                                    </Tag>
                                  </TableCell>
                                  <TableCell>
                                    <SampleTypesCell
                                      sampleTypes={s.sampleTypes}
                                    />
                                  </TableCell>
                                  <TableCell>
                                    {s.parameterGroupCount ??
                                      s.parameterGroups?.length ??
                                      0}
                                  </TableCell>
                                  <TableCell>
                                    {/* FR-1-009: clicking the count opens the
                                        View Linked Tests panel (the same
                                        read-only surface as the action button)
                                        so admins can drill into the list of
                                        tests for this standard from either
                                        affordance. Fallback to plain text
                                        when count is zero. */}
                                    {(s.linkedTestCount ?? 0) > 0 ? (
                                      <Button
                                        kind="ghost"
                                        size="sm"
                                        onClick={(e) => {
                                          e.stopPropagation();
                                          openLinkedTests(s);
                                        }}
                                        style={{
                                          minHeight: 0,
                                          padding: "0 0.25rem",
                                          fontWeight: 600,
                                        }}
                                      >
                                        {s.linkedTestCount}
                                      </Button>
                                    ) : (
                                      0
                                    )}
                                  </TableCell>
                                  <TableCell>
                                    <div
                                      style={{
                                        display: "flex",
                                        flexDirection: "row",
                                        alignItems: "center",
                                        gap: "0.25rem",
                                      }}
                                    >
                                      <Button
                                        kind="ghost"
                                        size="sm"
                                        hasIconOnly
                                        renderIcon={View}
                                        iconDescription={intl.formatMessage({
                                          id: "compliance.standard.viewLinkedTests",
                                          defaultMessage: "View Linked Tests",
                                        })}
                                        onClick={(e) => {
                                          e.stopPropagation();
                                          openLinkedTests(s);
                                        }}
                                      />
                                      <Button
                                        kind="ghost"
                                        size="sm"
                                        hasIconOnly
                                        renderIcon={Copy}
                                        iconDescription={intl.formatMessage({
                                          id: "button.complianceStandard.copy",
                                          defaultMessage: "Copy Standard",
                                        })}
                                        onClick={(e) => {
                                          e.stopPropagation();
                                          handleCopyStandard(s);
                                        }}
                                      />
                                      <Button
                                        kind="ghost"
                                        size="sm"
                                        hasIconOnly
                                        renderIcon={Archive}
                                        iconDescription={intl.formatMessage({
                                          id: "compliance.standard.archive",
                                          defaultMessage: "Archive",
                                        })}
                                        disabled={s.status === "ARCHIVED"}
                                        onClick={(e) => {
                                          e.stopPropagation();
                                          setArchiveTarget(s);
                                        }}
                                      />
                                    </div>
                                  </TableCell>
                                </TableExpandRow>
                                {isOpen && (
                                  <TableExpandedRow colSpan={11}>
                                    <div
                                      style={{
                                        background: "#edf5ff",
                                        borderLeft: "3px solid #0f62fe",
                                        padding: "1rem 1.25rem",
                                        margin: "0 -1rem",
                                      }}
                                    >
                                      <StandardForm
                                        standard={s}
                                        isNew={false}
                                        onSaved={handleSaved}
                                        onCancel={() => setExpandedId(null)}
                                      />
                                    </div>
                                  </TableExpandedRow>
                                )}
                              </React.Fragment>
                            );
                          })}
                        </TableBody>
                      </Table>
                    </TableContainer>
                    <Pagination
                      page={page}
                      pageSize={pageSize}
                      pageSizes={[10, 25, 50, 100]}
                      totalItems={total}
                      onChange={({ page: p, pageSize: ps }) => {
                        setPage(p);
                        setPageSize(ps);
                      }}
                    />
                  </Column>
                </Grid>
              </TabPanel>
              <TabPanel>
                <TestComplianceThresholds />
              </TabPanel>
            </TabPanels>
          </Tabs>
        </div>
      </div>

      <Modal
        open={!!archiveTarget}
        size="sm"
        modalHeading={intl.formatMessage({
          id: "compliance.standard.archiveHeading",
          defaultMessage: "Archive Compliance Standard",
        })}
        primaryButtonText={intl.formatMessage({
          id: "compliance.standard.archive",
          defaultMessage: "Archive",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "label.button.cancel",
          defaultMessage: "Cancel",
        })}
        danger
        onRequestClose={() => setArchiveTarget(null)}
        onRequestSubmit={handleConfirmArchive}
      >
        <p>
          <FormattedMessage
            id="compliance.standard.archivePrompt"
            defaultMessage="Archive this standard? Existing evaluated results stay linked to it for audit, but the standard will be hidden from active selection lists. This action can be reverted by editing the standard's status back to Active."
          />
        </p>
        {archiveTarget && (
          <p style={{ marginTop: "0.5rem", fontWeight: 500 }}>
            {archiveTarget.name}
          </p>
        )}
      </Modal>

      {/* FR-4-002: read-only View Linked Tests panel — tests grouped by
          parameter group, no inline editing. Clicking the test name does
          NOT navigate (the standalone admin tab + per-test editor handle
          navigation); this surface is purely a quick visual summary. */}
      <Modal
        open={!!linkedTestsTarget}
        size="md"
        passiveModal
        modalHeading={intl.formatMessage(
          {
            id: "compliance.standard.viewLinkedTests.heading",
            defaultMessage: "Linked Tests — {name}",
          },
          { name: linkedTestsTarget?.standard?.name || "" },
        )}
        onRequestClose={() => setLinkedTestsTarget(null)}
      >
        {linkedTestsLoading ? (
          <p>
            <FormattedMessage
              id="compliance.button.saving"
              defaultMessage="Loading…"
            />
          </p>
        ) : linkedTestsTarget?.rows && linkedTestsTarget.rows.length === 0 ? (
          <p>
            <FormattedMessage
              id="compliance.standard.viewLinkedTests.empty"
              defaultMessage="No tests are currently linked to this standard."
            />
          </p>
        ) : (
          <div>
            {(() => {
              // Group flat rows by groupId, preserving server-side sort order
              const groups = [];
              const byId = new Map();
              (linkedTestsTarget?.rows || []).forEach((row) => {
                const id = row.groupId;
                if (!byId.has(id)) {
                  const g = { id, name: row.groupName, tests: [] };
                  byId.set(id, g);
                  groups.push(g);
                }
                byId.get(id).tests.push({
                  id: row.testId,
                  name: row.testName,
                  code: row.testCode,
                });
              });
              return groups.map((g) => (
                <div key={g.id} style={{ marginBottom: "1rem" }}>
                  <h6
                    style={{
                      margin: "0 0 0.375rem 0",
                      fontSize: "0.8125rem",
                      fontWeight: 600,
                    }}
                  >
                    {g.name}{" "}
                    <span
                      style={{
                        fontWeight: 400,
                        color: "var(--cds-text-02)",
                      }}
                    >
                      ({g.tests.length})
                    </span>
                  </h6>
                  <ul
                    style={{
                      margin: 0,
                      paddingLeft: "1.25rem",
                      fontSize: "0.875rem",
                    }}
                  >
                    {g.tests.map((t) => (
                      <li key={t.id} style={{ marginBottom: "0.125rem" }}>
                        <strong>{t.name}</strong>
                        {t.code && (
                          <span
                            style={{
                              marginLeft: "0.5rem",
                              fontSize: "0.75rem",
                              color: "var(--cds-text-02)",
                            }}
                          >
                            {t.code}
                          </span>
                        )}
                      </li>
                    ))}
                  </ul>
                </div>
              ));
            })()}
          </div>
        )}
      </Modal>
    </>
  );
}

export default ComplianceStandardsAdmin;
