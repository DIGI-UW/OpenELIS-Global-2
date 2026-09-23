/**
 * OGC-189: Lab Units Management Redesign — unified list + editor.
 *
 * First increment of the epic:
 * - Lab Units list with search, Domain filter, status filter, per-domain
 *   counts, test counts, and status tags
 * - URL-driven editor (/:labUnitId?/:section?) with Basic Info (names,
 *   description, required Domain radio group with OGC-748-style change
 *   confirmation, activate/deactivate), Assigned Tests, and Display Order
 *   sections
 *
 * Mirrors the SampleTypeManagement (OGC-296) surface so the catalog admin
 * screens stay consistent. Creation posts to /rest/lab-units-management,
 * which wires the same role modules as the legacy /rest/TestSectionCreate
 * flow.
 */

import React, {
  useState,
  useCallback,
  useMemo,
  useRef,
  useEffect,
} from "react";
import { useHistory, useLocation, useParams } from "react-router-dom";
import {
  Grid,
  Column,
  Stack,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  TextInput,
  TextArea,
  Select,
  SelectItem,
  RadioButtonGroup,
  RadioButton,
  Toggle,
  Button,
  InlineNotification,
  Modal,
  Tag,
  Tile,
  Loading,
  Pagination,
} from "@carbon/react";
import {
  DEFAULT_LAB_UNIT_SECTION,
  isValidLabUnitSection,
} from "./sectionConfig";
import AssignedTestsSection from "./sections/AssignedTestsSection";
import DisplayOrderSection from "./sections/DisplayOrderSection";
import { Add, Edit, Save } from "@carbon/react/icons";
import { injectIntl, FormattedMessage } from "react-intl";
import PageBreadCrumb from "../../common/PageBreadCrumb";
import useDomains from "../../common/useDomains";
import useActiveLocales from "../../common/useActiveLocales";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
  putToOpenElisServer,
} from "../../utils/Utils";

let breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "breadcrums.admin.managment", link: "/MasterListsPage" },
  {
    label: "configuration.labUnit.manage",
    link: "/MasterListsPage/LabUnitManagement",
  },
];

// Domain values come from the single /rest/domains source (useDomains); only
// the tag color palette is presentational and assigned by list position.
const DOMAIN_TAG_COLORS = ["green", "purple", "teal", "cyan", "magenta"];

// test_section.NAME is VARCHAR(20); the create flow stores the English name
// there, so new names must fit.
const NAME_MAX_LENGTH = 20;
const DESCRIPTION_MAX_LENGTH = 60;

// Trim every entry; empty strings are kept so the backend can clear a
// translation the admin blanked out.
const trimmedNames = (names) => {
  const result = {};
  Object.entries(names || {}).forEach(([code, value]) => {
    result[code] = (value || "").trim();
  });
  return result;
};

const mapLabUnit = (item) => ({
  id: item.id,
  name: item.name || item.description || "",
  // Locale code → name, from the multi-language localization mechanism.
  names: item.names || {},
  description: item.description || "",
  domain: item.domain || "CLINICAL",
  active: item.isActive !== undefined ? item.isActive : true,
  testCount: item.testCount || 0,
  sortOrder: item.sortOrder || 0,
});

function LabUnitManagement({ intl }) {
  const history = useHistory();
  const location = useLocation();
  const { labUnitId, section } = useParams();
  const basePath = location.pathname.startsWith("/admin")
    ? "/admin"
    : "/MasterListsPage";
  const listUrl = `${basePath}/LabUnitManagement`;

  // View is derived from the URL: no id → list, "new" → add, otherwise → editor.
  const view = !labUnitId ? "list" : labUnitId === "new" ? "add" : "editor";
  const activeSection = isValidLabUnitSection(section)
    ? section
    : DEFAULT_LAB_UNIT_SECTION;

  const [editingUnit, setEditingUnit] = useState(null);

  // Single source for the domain list + presentational helpers.
  const domains = useDomains();

  // Active languages from the multi-language localization mechanism: one
  // name input per locale, with the fallback locale's name required (it is
  // the lab unit's identifying name).
  const locales = useActiveLocales();
  const fallbackLocaleCode = useMemo(() => {
    const fallback = locales.find((locale) => locale.fallback);
    return fallback ? fallback.localeCode : "en";
  }, [locales]);
  const domainColor = useCallback(
    (id) => {
      const index = domains.findIndex((d) => d.id === id);
      return index >= 0
        ? DOMAIN_TAG_COLORS[index % DOMAIN_TAG_COLORS.length]
        : "gray";
    },
    [domains],
  );
  const domainLabel = useCallback(
    (id) => {
      const match = domains.find((d) => d.id === id);
      return match ? intl.formatMessage({ id: match.labelKey }) : id;
    },
    [domains, intl],
  );

  // Filter state
  const [searchText, setSearchText] = useState("");
  const [domainFilter, setDomainFilter] = useState("");
  const [statusFilter, setStatusFilter] = useState("");

  // Pagination state (following repository pattern)
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(20);

  // Data state
  const [labUnits, setLabUnits] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [loadError, setLoadError] = useState(null);

  // Form validation and state
  const [formErrors, setFormErrors] = useState({});
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [showSuccess, setShowSuccess] = useState(false);
  const nameInputRef = useRef(null);

  // OGC-748-style Domain change confirmation (OGC-361 CFG-4): switching the
  // domain of an EXISTING lab unit stages the value behind a modal.
  const [pendingDomain, setPendingDomain] = useState(null);
  // Bumping this key force-remounts the radio group on cancel so the checked
  // radio snaps back to the persisted domain.
  const [domainRadioKey, setDomainRadioKey] = useState(0);

  // Tests assigned to the lab unit currently being edited (drives the
  // deactivate-in-use warning).
  const [assignedTests, setAssignedTests] = useState([]);

  const refreshLabUnits = useCallback(async () => {
    return await new Promise((resolve, reject) => {
      getFromOpenElisServer("/rest/lab-units-management", (response) => {
        if (response && response.success && Array.isArray(response.data)) {
          resolve(response.data);
        } else if (Array.isArray(response)) {
          resolve(response);
        } else {
          reject(new Error("Invalid response format from lab units endpoint"));
        }
      });
    }).then((labUnitList) => {
      const mapped = labUnitList.map(mapLabUnit);
      setLabUnits(mapped);
      return mapped;
    });
  }, []);

  // Load on mount, then re-sync every time we return to the list view: the
  // editor's section components (Assigned Tests, Display Order) mutate test
  // counts, status, and ordering on the server without updating this
  // parent's cached list, so rendering the cached array on "Back to List"
  // would show stale data. Only the first load shows the spinner.
  const hasLoadedRef = useRef(false);
  useEffect(() => {
    if (view !== "list" && hasLoadedRef.current) {
      return;
    }
    const isFirstLoad = !hasLoadedRef.current;
    hasLoadedRef.current = true;
    const fetchLabUnits = async () => {
      try {
        if (isFirstLoad) {
          setIsLoading(true);
        }
        setLoadError(null);
        await refreshLabUnits();
      } catch (error) {
        setLoadError(
          `Database connection failed: ${error.message}. Please ensure you are logged into OpenELIS with admin permissions and try refreshing the page.`,
        );
        setLabUnits([]);
      } finally {
        if (isFirstLoad) {
          setIsLoading(false);
        }
      }
    };

    fetchLabUnits();
  }, [view, refreshLabUnits]);

  // Focus management for add form
  useEffect(() => {
    if (view === "add" && nameInputRef.current) {
      nameInputRef.current.focus();
    }
  }, [view]);

  // Filtered list
  const filteredUnits = useMemo(() => {
    return labUnits.filter((unit) => {
      const matchesSearch =
        !searchText ||
        unit.name.toLowerCase().includes(searchText.toLowerCase()) ||
        unit.description.toLowerCase().includes(searchText.toLowerCase());
      const matchesDomain = !domainFilter || unit.domain === domainFilter;
      const matchesStatus =
        !statusFilter ||
        (statusFilter === "active" ? unit.active : !unit.active);
      return matchesSearch && matchesDomain && matchesStatus;
    });
  }, [labUnits, searchText, domainFilter, statusFilter]);

  // Paginated list (following repository pattern)
  const paginatedUnits = useMemo(() => {
    const startIndex = (page - 1) * pageSize;
    const endIndex = startIndex + pageSize;
    return filteredUnits.slice(startIndex, endIndex);
  }, [filteredUnits, page, pageSize]);

  // Reset pagination when filters change
  useEffect(() => {
    setPage(1);
  }, [searchText, domainFilter, statusFilter]);

  const handlePageChange = useCallback(
    (pageInfo) => {
      if (page !== pageInfo.page) {
        setPage(pageInfo.page);
      }
      if (pageSize !== pageInfo.pageSize) {
        setPageSize(pageInfo.pageSize);
      }
    },
    [page, pageSize],
  );

  // Domain counts, keyed by the domains served from the single source.
  const domainCounts = useMemo(() => {
    const counts = {};
    domains.forEach((d) => {
      counts[d.id] = 0;
    });
    labUnits.forEach((unit) => {
      if (counts[unit.domain] !== undefined) {
        counts[unit.domain]++;
      }
    });
    return counts;
  }, [labUnits, domains]);

  // Hydrate editingUnit from the URL id whenever we're on the editor route.
  // The list may not be loaded yet on a deep link, so we tolerate an empty
  // labUnits list and re-run once it populates.
  useEffect(() => {
    if (view !== "editor" || !labUnitId) {
      return;
    }
    if (editingUnit && String(editingUnit.id) === String(labUnitId)) {
      return;
    }
    const unit = labUnits.find((item) => String(item.id) === String(labUnitId));
    if (!unit) {
      return;
    }
    setEditingUnit({ ...unit });
    setFormErrors({});
    setShowSuccess(false);
    setPendingDomain(null);
    setAssignedTests([]);
  }, [view, labUnitId, labUnits, editingUnit]);

  // Seed the "add" form once when we land on /LabUnitManagement/new.
  useEffect(() => {
    if (view !== "add") {
      return;
    }
    if (editingUnit && editingUnit.id === null) {
      return;
    }
    setEditingUnit({
      id: null,
      name: "",
      names: {},
      description: "",
      // New lab units land inactive until tests are assigned; the editor's
      // Active toggle covers activation afterwards.
      active: false,
      domain: "CLINICAL",
      testCount: 0,
      sortOrder: labUnits.length + 1,
    });
    setFormErrors({});
    setShowSuccess(false);
    setPendingDomain(null);
  }, [view, labUnits.length, editingUnit]);

  // Clear editor state when returning to the list URL.
  useEffect(() => {
    if (view === "list" && editingUnit) {
      setEditingUnit(null);
    }
  }, [view, editingUnit]);

  // Canonicalize the section into the URL so deep-links + the SideNav agree.
  useEffect(() => {
    if (labUnitId && (!section || !isValidLabUnitSection(section))) {
      history.replace(`${listUrl}/${labUnitId}/${DEFAULT_LAB_UNIT_SECTION}`);
    }
  }, [labUnitId, section, history, listUrl]);

  const openEditor = useCallback(
    (unit) => {
      history.push(`${listUrl}/${unit.id}/${DEFAULT_LAB_UNIT_SECTION}`);
    },
    [history, listUrl],
  );

  const openAddForm = useCallback(() => {
    history.push(`${listUrl}/new/${DEFAULT_LAB_UNIT_SECTION}`);
  }, [history, listUrl]);

  const goToList = useCallback(() => {
    history.push(listUrl);
  }, [history, listUrl]);

  // Form validation. Only the fallback locale's name is required — it is the
  // lab unit's identifying name; other languages are optional translations.
  const validateForm = useCallback(
    (formData) => {
      const errors = {};

      const name = formData.names?.[fallbackLocaleCode]?.trim();
      const nameErrorKey = `name-${fallbackLocaleCode}`;
      if (!name) {
        errors[nameErrorKey] = intl.formatMessage({
          id: "error.labUnit.name.required",
        });
      } else if (view === "add" && name.length > NAME_MAX_LENGTH) {
        errors[nameErrorKey] = intl.formatMessage(
          { id: "error.labUnit.name.maxLength" },
          { max: NAME_MAX_LENGTH },
        );
      } else if (
        labUnits.some(
          (unit) =>
            (unit.names?.[fallbackLocaleCode] || unit.name).toLowerCase() ===
              name.toLowerCase() && String(unit.id) !== String(formData.id),
        )
      ) {
        errors[nameErrorKey] = intl.formatMessage({
          id: "error.labUnit.name.duplicate",
        });
      }

      if (
        formData.description &&
        formData.description.trim().length > DESCRIPTION_MAX_LENGTH
      ) {
        errors.description = intl.formatMessage(
          { id: "error.labUnit.description.maxLength" },
          { max: DESCRIPTION_MAX_LENGTH },
        );
      }

      // Domain is required (OGC-361 CFG-1).
      if (!formData.domain) {
        errors.domain = intl.formatMessage({
          id: "error.labUnit.domain.required",
        });
      }

      return errors;
    },
    [labUnits, view, intl, fallbackLocaleCode],
  );

  const saveEditor = useCallback(async () => {
    if (!editingUnit) return;

    const errors = validateForm(editingUnit);
    setFormErrors(errors);
    if (Object.keys(errors).length > 0) {
      return;
    }

    setIsSubmitting(true);
    try {
      if (view === "add") {
        // Create through the unified endpoint with the full names map (any
        // active language); the backend wires the workplan/results/validation
        // role modules like the legacy flow did, and returns the new record.
        const createData = {
          names: trimmedNames(editingUnit.names),
          description: editingUnit.description?.trim() || undefined,
          domain: editingUnit.domain || "CLINICAL",
        };
        const created = await new Promise((resolve, reject) => {
          postToOpenElisServerJsonResponse(
            "/rest/lab-units-management",
            JSON.stringify(createData),
            (result) => {
              if (result && result.success && result.data) {
                resolve(result.data);
              } else if (result && (result.error || result.message)) {
                reject(new Error(result.message || result.error));
              } else {
                reject(new Error("Save failed"));
              }
            },
          );
        });
        await refreshLabUnits();
        setFormErrors({});
        setEditingUnit(null);
        if (created && created.id) {
          history.push(`${listUrl}/${created.id}/${DEFAULT_LAB_UNIT_SECTION}`);
        } else {
          history.push(listUrl);
        }
      } else if (view === "editor") {
        const updateData = {
          id: editingUnit.id,
          names: trimmedNames(editingUnit.names),
          description: editingUnit.description?.trim(),
          domain: editingUnit.domain || "CLINICAL",
          isActive:
            editingUnit.active !== undefined ? editingUnit.active : true,
        };
        await new Promise((resolve, reject) => {
          putToOpenElisServer(
            `/rest/lab-units-management/${editingUnit.id}`,
            JSON.stringify(updateData),
            (status) => {
              if (status === 200) {
                resolve(status);
              } else {
                reject(new Error(`Update failed (HTTP ${status})`));
              }
            },
          );
        });
        await refreshLabUnits();
        // Stay on the editor — re-sync from the authoritative record so the
        // toggle and every field reflect exactly what was persisted.
        await new Promise((resolve) => {
          getFromOpenElisServer(
            `/rest/lab-units-management/${editingUnit.id}`,
            (res) => {
              if (res && res.success && res.data) {
                setEditingUnit(mapLabUnit(res.data));
              }
              resolve();
            },
          );
        });
        setShowSuccess(true);
        setTimeout(() => setShowSuccess(false), 3000);
        setFormErrors({});
      }
    } catch (error) {
      const operation = view === "add" ? "create" : "update";
      setFormErrors({
        submit: `Failed to ${operation} lab unit: ${error.message}`,
      });
    } finally {
      setIsSubmitting(false);
    }
  }, [
    editingUnit,
    view,
    validateForm,
    history,
    listUrl,
    labUnits,
    refreshLabUnits,
  ]);

  // Domain radio handler: in the editor an actual change is staged behind the
  // confirmation modal; in the add form it applies directly.
  const handleDomainSelect = useCallback(
    (value) => {
      if (!editingUnit) return;
      if (
        view === "editor" &&
        editingUnit.domain &&
        value !== editingUnit.domain
      ) {
        setPendingDomain(value);
        return;
      }
      setEditingUnit((prev) => ({ ...prev, domain: value }));
      if (formErrors.domain) {
        setFormErrors((prev) => ({ ...prev, domain: "" }));
      }
    },
    [editingUnit, view, formErrors.domain],
  );

  const confirmDomainChange = useCallback(() => {
    setEditingUnit((prev) => ({ ...prev, domain: pendingDomain }));
    setPendingDomain(null);
  }, [pendingDomain]);

  const cancelDomainChange = useCallback(() => {
    setPendingDomain(null);
    // Remount the radio group so the checked value snaps back.
    setDomainRadioKey((prev) => prev + 1);
  }, []);

  // Assign/reassign mutate server-side state beyond the test list itself
  // (assigning into an inactive unit activates it). Re-sync the unit's
  // status + count so the header tags and Active toggle reflect reality —
  // otherwise a later Basic Info save would send the stale isActive and
  // silently deactivate the unit again. Unsaved name/description/domain
  // edits are deliberately left untouched.
  const handleAssignedTestsChange = useCallback(
    (tests) => {
      setAssignedTests(tests);
      if (!labUnitId || labUnitId === "new") {
        return;
      }
      getFromOpenElisServer(
        `/rest/lab-units-management/${labUnitId}`,
        (res) => {
          if (res && res.success && res.data) {
            setEditingUnit((prev) =>
              prev && String(prev.id) === String(labUnitId)
                ? {
                    ...prev,
                    active: res.data.isActive,
                    testCount: res.data.testCount,
                  }
                : prev,
            );
          }
        },
      );
    },
    [labUnitId],
  );

  const activeAssignedTestCount = assignedTests.filter(
    (test) => test.active,
  ).length;

  // ─── LIST VIEW ────────────────────────────────────────────────
  if (view === "list") {
    return (
      <div className="adminPageContent">
        <PageBreadCrumb breadcrumbs={breadcrumbs} />
        <Stack gap={5}>
          {isLoading && (
            <div
              style={{
                display: "flex",
                justifyContent: "center",
                padding: "var(--cds-spacing-07)",
                alignItems: "center",
                gap: "var(--cds-spacing-03)",
              }}
            >
              <Loading />
              <span>
                <FormattedMessage id="label.labUnit.list.loading" />
              </span>
            </div>
          )}

          {loadError && (
            <InlineNotification
              kind="error"
              title={intl.formatMessage({ id: "error.labUnit.list.load" })}
              subtitle={loadError}
              lowContrast
              hideCloseButton={false}
              onCloseButtonClick={() => setLoadError(null)}
            />
          )}

          {!isLoading && (
            <>
              {/* Page Header */}
              <Tile style={{ padding: "var(--cds-spacing-06)" }}>
                <Grid>
                  <Column lg={8} md={4} sm={4}>
                    <h2
                      style={{
                        margin: "0 0 var(--cds-spacing-03) 0",
                        color: "var(--cds-text-primary)",
                        fontWeight: 600,
                      }}
                    >
                      <FormattedMessage id="heading.labUnit.management" />
                    </h2>
                    <p
                      style={{
                        fontSize: "14px",
                        color: "var(--cds-text-secondary)",
                        margin: "0",
                        lineHeight: 1.4,
                      }}
                    >
                      <FormattedMessage id="heading.labUnit.subtitle" />
                    </p>
                    <p
                      style={{
                        fontSize: "12px",
                        color: "var(--cds-text-secondary)",
                        margin: "var(--cds-spacing-02) 0 0 0",
                        fontWeight: 500,
                      }}
                    >
                      {searchText || domainFilter || statusFilter ? (
                        <FormattedMessage
                          id="heading.labUnit.filtered"
                          values={{
                            filtered: filteredUnits.length,
                            total: labUnits.length,
                          }}
                        />
                      ) : (
                        <FormattedMessage
                          id="heading.labUnit.total"
                          values={{ total: labUnits.length }}
                        />
                      )}
                    </p>
                  </Column>
                  <Column lg={8} md={4} sm={4} style={{ textAlign: "right" }}>
                    <Stack
                      orientation="horizontal"
                      gap={4}
                      style={{
                        justifyContent: "flex-end",
                        alignItems: "center",
                      }}
                    >
                      {domains.map((d) => (
                        <div
                          key={d.id}
                          style={{
                            display: "flex",
                            alignItems: "center",
                            gap: "var(--cds-spacing-02)",
                          }}
                        >
                          <Tag type={domainColor(d.id)} size="md">
                            {domainCounts[d.id] || 0}
                          </Tag>
                          <span style={{ fontSize: "14px", fontWeight: 500 }}>
                            {domainLabel(d.id)}
                          </span>
                        </div>
                      ))}
                    </Stack>
                  </Column>
                </Grid>
              </Tile>

              {/* Lab Units Table */}
              <TableContainer style={{ marginBottom: 0 }}>
                <div
                  style={{
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "space-between",
                    padding: "0 var(--cds-spacing-05)",
                    height: "48px",
                    background: "var(--cds-layer)",
                    borderBottom: "1px solid var(--cds-border-subtle-01)",
                    gap: "var(--cds-spacing-05)",
                  }}
                >
                  <TextInput
                    id="lab-unit-search"
                    labelText={intl.formatMessage({
                      id: "placeholder.labUnit.search",
                    })}
                    hideLabel
                    placeholder={intl.formatMessage({
                      id: "placeholder.labUnit.search",
                    })}
                    value={searchText}
                    onChange={(e) => setSearchText(e.target.value)}
                    size="sm"
                    style={{ flex: "0 0 240px" }}
                  />
                  <div
                    style={{
                      display: "flex",
                      alignItems: "center",
                      gap: "var(--cds-spacing-05)",
                    }}
                  >
                    <Select
                      id="lab-unit-domain-filter"
                      labelText={intl.formatMessage({
                        id: "admin.labUnit.list.filter.domain",
                      })}
                      hideLabel
                      value={domainFilter}
                      onChange={(e) => setDomainFilter(e.target.value)}
                      style={{ flex: "0 0 180px" }}
                    >
                      <SelectItem
                        value=""
                        text={intl.formatMessage({
                          id: "placeholder.labUnit.filter.domain",
                        })}
                      />
                      {domains.map((d) => (
                        <SelectItem
                          key={d.id}
                          value={d.id}
                          text={domainLabel(d.id)}
                        />
                      ))}
                    </Select>
                    <Select
                      id="lab-unit-status-filter"
                      labelText={intl.formatMessage({
                        id: "label.labUnit.filterStatus",
                      })}
                      hideLabel
                      value={statusFilter}
                      onChange={(e) => setStatusFilter(e.target.value)}
                      style={{ flex: "0 0 140px" }}
                    >
                      <SelectItem
                        value=""
                        text={intl.formatMessage({
                          id: "placeholder.labUnit.filter.status",
                        })}
                      />
                      <SelectItem
                        value="active"
                        text={intl.formatMessage({ id: "label.active" })}
                      />
                      <SelectItem
                        value="inactive"
                        text={intl.formatMessage({ id: "label.inactive" })}
                      />
                    </Select>
                    <Button
                      kind="primary"
                      size="sm"
                      renderIcon={Add}
                      onClick={openAddForm}
                      style={{ whiteSpace: "nowrap", flex: "0 0 auto" }}
                    >
                      <FormattedMessage id="button.labUnit.add" />
                    </Button>
                  </div>
                </div>
                <Table>
                  <TableHead>
                    <TableRow>
                      <TableHeader>
                        <FormattedMessage id="label.labUnit.name" />
                      </TableHeader>
                      <TableHeader>
                        <FormattedMessage id="label.labUnit.domain" />
                      </TableHeader>
                      <TableHeader>
                        <FormattedMessage id="label.labUnit.status" />
                      </TableHeader>
                      <TableHeader>
                        <FormattedMessage id="label.labUnit.testCount" />
                      </TableHeader>
                      <TableHeader>
                        <FormattedMessage id="label.labUnit.actions" />
                      </TableHeader>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {paginatedUnits.length > 0 ? (
                      paginatedUnits.map((unit) => (
                        <TableRow key={unit.id}>
                          <TableCell>
                            <div>
                              <span
                                style={{
                                  fontWeight: 600,
                                  color: "var(--cds-text-primary)",
                                  fontSize: "14px",
                                }}
                              >
                                {unit.name}
                              </span>
                              <br />
                              <span
                                style={{
                                  fontSize: "12px",
                                  color: "var(--cds-text-secondary)",
                                  lineHeight: 1.3,
                                  marginTop: "var(--cds-spacing-01)",
                                }}
                              >
                                {unit.description}
                              </span>
                            </div>
                          </TableCell>
                          <TableCell>
                            <Tag type={domainColor(unit.domain)} size="sm">
                              {domainLabel(unit.domain)}
                            </Tag>
                          </TableCell>
                          <TableCell>
                            <Tag
                              type={unit.active ? "green" : "gray"}
                              size="sm"
                            >
                              {unit.active ? (
                                <FormattedMessage id="label.active" />
                              ) : (
                                <FormattedMessage id="label.inactive" />
                              )}
                            </Tag>
                          </TableCell>
                          <TableCell>
                            <span
                              style={{
                                fontWeight: 500,
                                color: "var(--cds-text-primary)",
                              }}
                            >
                              {unit.testCount}
                            </span>
                          </TableCell>
                          <TableCell>
                            <Button
                              kind="ghost"
                              size="sm"
                              renderIcon={Edit}
                              onClick={() => openEditor(unit)}
                            >
                              <FormattedMessage id="button.edit" />
                            </Button>
                          </TableCell>
                        </TableRow>
                      ))
                    ) : (
                      <TableRow>
                        <TableCell
                          colSpan={5}
                          style={{
                            textAlign: "center",
                            padding: "var(--cds-spacing-07)",
                          }}
                        >
                          <div style={{ color: "var(--cds-text-secondary)" }}>
                            <FormattedMessage id="message.labUnit.noResults" />
                          </div>
                        </TableCell>
                      </TableRow>
                    )}
                  </TableBody>
                </Table>
              </TableContainer>

              <div style={{ overflowX: "auto" }}>
                <Pagination
                  onChange={handlePageChange}
                  page={page}
                  pageSize={pageSize}
                  pageSizes={[10, 20, 30, 50, 100]}
                  totalItems={filteredUnits.length}
                  forwardText={intl.formatMessage({ id: "pagination.forward" })}
                  backwardText={intl.formatMessage({
                    id: "pagination.backward",
                  })}
                  itemRangeText={(min, max, total) =>
                    intl.formatMessage(
                      { id: "pagination.item-range" },
                      { min: min, max: max, total: total },
                    )
                  }
                  itemsPerPageText={intl.formatMessage({
                    id: "pagination.items-per-page",
                  })}
                  pageNumberText={intl.formatMessage({
                    id: "pagination.page-number",
                  })}
                  pageRangeText={(current, total) =>
                    intl.formatMessage(
                      { id: "pagination.page-range" },
                      { current: current, total: total },
                    )
                  }
                  pageText={intl.formatMessage({ id: "pagination.page" })}
                  size="md"
                />
              </div>
            </>
          )}
        </Stack>
      </div>
    );
  }

  // ─── EDITOR/ADD VIEW ──────────────────────────────────────────
  if (view === "editor" || view === "add") {
    return (
      <div className="adminPageContent">
        <PageBreadCrumb breadcrumbs={breadcrumbs} />
        <Stack gap={5}>
          {/* Editor Header — mirrors the list-view header so "Lab Unit
              Management" stays the page title and sections sit under it. */}
          <Tile style={{ padding: "var(--cds-spacing-06)" }}>
            <Grid>
              <Column lg={12} md={6} sm={4}>
                <h2
                  style={{
                    margin: "0 0 var(--cds-spacing-03) 0",
                    color: "var(--cds-text-primary)",
                    fontWeight: 600,
                  }}
                >
                  <FormattedMessage id="heading.labUnit.management" />
                </h2>
                <Stack
                  orientation="horizontal"
                  gap={3}
                  style={{ alignItems: "center", flexWrap: "wrap" }}
                >
                  <p
                    style={{
                      fontSize: "14px",
                      color: "var(--cds-text-secondary)",
                      margin: 0,
                      lineHeight: 1.4,
                    }}
                  >
                    {view === "add" ? (
                      <FormattedMessage id="heading.labUnit.add" />
                    ) : (
                      <FormattedMessage
                        id="heading.labUnit.editing"
                        values={{ name: editingUnit?.name }}
                      />
                    )}
                  </p>
                  {view === "editor" && editingUnit?.domain && (
                    <Tag type={domainColor(editingUnit?.domain)} size="md">
                      {domainLabel(editingUnit?.domain)}
                    </Tag>
                  )}
                  {view === "editor" &&
                    (editingUnit?.active ? (
                      <Tag type="green" size="md">
                        <FormattedMessage id="label.active" />
                      </Tag>
                    ) : (
                      <Tag type="gray" size="md">
                        <FormattedMessage id="label.inactive" />
                      </Tag>
                    ))}
                </Stack>
              </Column>
              <Column
                lg={4}
                md={2}
                sm={4}
                style={{
                  display: "flex",
                  justifyContent: "flex-end",
                  alignItems: "flex-start",
                }}
              >
                <Button kind="ghost" size="sm" onClick={goToList}>
                  <FormattedMessage id="button.back" />
                </Button>
              </Column>
            </Grid>
          </Tile>

          <div>
            {/* Render a loader until editingUnit is hydrated for the current
                id — otherwise controlled inputs (esp. the Carbon Active
                Toggle) would mount with undefined and not reflect the saved
                status. */}
            {view === "editor" &&
            !(editingUnit && String(editingUnit.id) === String(labUnitId)) ? (
              <div
                style={{
                  display: "flex",
                  alignItems: "center",
                  gap: "var(--cds-spacing-03)",
                  padding: "var(--cds-spacing-07)",
                }}
              >
                <Loading small withOverlay={false} />
                <FormattedMessage id="label.labUnit.loading" />
              </div>
            ) : (
              <>
                {activeSection === "basic-info" && (
                  <div>
                    {(showSuccess || formErrors.submit) && (
                      <div style={{ marginBottom: "var(--cds-spacing-06)" }}>
                        <Stack gap={5}>
                          {showSuccess && (
                            <InlineNotification
                              kind="success"
                              title=""
                              subtitle={intl.formatMessage({
                                id:
                                  view === "add"
                                    ? "message.labUnit.add.success"
                                    : "message.labUnit.edit.success",
                              })}
                              lowContrast
                              hideCloseButton
                            />
                          )}
                          {formErrors.submit && (
                            <InlineNotification
                              kind="error"
                              title=""
                              subtitle={formErrors.submit}
                              lowContrast
                              hideCloseButton={false}
                              onCloseButtonClick={() =>
                                setFormErrors((prev) => ({
                                  ...prev,
                                  submit: "",
                                }))
                              }
                            />
                          )}
                        </Stack>
                      </div>
                    )}
                    <Tile
                      style={{
                        padding: "var(--cds-spacing-07)",
                        border: "1px solid var(--cds-border-subtle)",
                        borderRadius: "var(--cds-border-radius)",
                      }}
                    >
                      <Grid>
                        <Column lg={12} md={8} sm={4}>
                          <Stack gap={6}>
                            <div
                              style={{ marginBottom: "var(--cds-spacing-03)" }}
                            />
                            {/* One name input per active language — driven by
                                the multi-language localization mechanism, not
                                hard-coded English/French. Only the fallback
                                locale's name is required. */}
                            {locales.map((locale) => {
                              const isFallback =
                                locale.localeCode === fallbackLocaleCode;
                              const errorKey = `name-${locale.localeCode}`;
                              return (
                                <TextInput
                                  key={locale.localeCode}
                                  ref={isFallback ? nameInputRef : undefined}
                                  id={`lu-name-${locale.localeCode}`}
                                  labelText={
                                    <>
                                      <FormattedMessage
                                        id="label.labUnit.nameInLocale"
                                        values={{
                                          language: locale.displayName,
                                        }}
                                      />
                                      {isFallback && (
                                        <span
                                          style={{
                                            color: "var(--cds-support-error)",
                                          }}
                                        >
                                          {" "}
                                          *
                                        </span>
                                      )}
                                    </>
                                  }
                                  value={
                                    editingUnit?.names?.[locale.localeCode] ||
                                    ""
                                  }
                                  onChange={(e) => {
                                    const value = e.target.value;
                                    setEditingUnit((prev) => ({
                                      ...prev,
                                      names: {
                                        ...prev.names,
                                        [locale.localeCode]: value,
                                      },
                                    }));
                                    if (formErrors[errorKey]) {
                                      setFormErrors((prev) => ({
                                        ...prev,
                                        [errorKey]: "",
                                      }));
                                    }
                                  }}
                                  invalid={!!formErrors[errorKey]}
                                  invalidText={formErrors[errorKey]}
                                  helperText={
                                    isFallback
                                      ? intl.formatMessage({
                                          id: "helper.labUnit.name",
                                        })
                                      : undefined
                                  }
                                  autoComplete="off"
                                />
                              );
                            })}

                            {/* Domain radio group (OGC-361 CFG-1): required,
                                Carbon RadioButtonGroup, values from the single
                                /rest/domains source. */}
                            <div>
                              <RadioButtonGroup
                                key={domainRadioKey}
                                legendText={
                                  <>
                                    <FormattedMessage id="admin.labUnit.basicInfo.domain.label" />
                                    <span
                                      style={{
                                        color: "var(--cds-support-error)",
                                      }}
                                    >
                                      {" "}
                                      *
                                    </span>
                                  </>
                                }
                                name="lu-domain"
                                orientation="horizontal"
                                valueSelected={editingUnit?.domain || ""}
                                onChange={handleDomainSelect}
                              >
                                {domains.map((d) => (
                                  <RadioButton
                                    key={d.id}
                                    id={`lu-domain-${d.id.toLowerCase()}`}
                                    labelText={domainLabel(d.id)}
                                    value={d.id}
                                  />
                                ))}
                              </RadioButtonGroup>
                              {formErrors.domain && (
                                <p
                                  style={{
                                    color: "var(--cds-support-error)",
                                    fontSize: "12px",
                                    margin: "var(--cds-spacing-02) 0 0 0",
                                  }}
                                >
                                  {formErrors.domain}
                                </p>
                              )}
                              <p
                                style={{
                                  fontSize: "12px",
                                  color: "var(--cds-text-secondary)",
                                  margin: "var(--cds-spacing-02) 0 0 0",
                                }}
                              >
                                <FormattedMessage id="helper.labUnit.domain" />
                              </p>
                            </div>

                            {view === "editor" && (
                              <>
                                <Toggle
                                  id="lu-active"
                                  labelText={intl.formatMessage({
                                    id: "label.labUnit.active",
                                  })}
                                  labelA={intl.formatMessage({
                                    id: "label.inactive",
                                  })}
                                  labelB={intl.formatMessage({
                                    id: "label.active",
                                  })}
                                  toggled={editingUnit?.active}
                                  onToggle={(checked) =>
                                    setEditingUnit((prev) => ({
                                      ...prev,
                                      active: checked,
                                    }))
                                  }
                                />

                                {/* Deactivating a unit in use warns but
                                    proceeds — no cascade, reversible. The
                                    cascade/impact flow lands in a later
                                    increment of OGC-189. */}
                                {!editingUnit?.active &&
                                  activeAssignedTestCount > 0 && (
                                    <InlineNotification
                                      kind="warning"
                                      lowContrast
                                      hideCloseButton
                                      title={intl.formatMessage(
                                        {
                                          id: "warning.labUnit.deactivateInUse",
                                        },
                                        { count: activeAssignedTestCount },
                                      )}
                                    />
                                  )}

                                <TextArea
                                  id="lu-description"
                                  labelText={
                                    <FormattedMessage id="label.labUnit.description" />
                                  }
                                  value={editingUnit?.description || ""}
                                  onChange={(e) => {
                                    setEditingUnit((prev) => ({
                                      ...prev,
                                      description: e.target.value,
                                    }));
                                    if (formErrors.description) {
                                      setFormErrors((prev) => ({
                                        ...prev,
                                        description: "",
                                      }));
                                    }
                                  }}
                                  rows={2}
                                  maxCount={DESCRIPTION_MAX_LENGTH}
                                  enableCounter
                                  invalid={!!formErrors.description}
                                  invalidText={formErrors.description}
                                  helperText={intl.formatMessage({
                                    id: "helper.labUnit.description",
                                  })}
                                />
                              </>
                            )}
                          </Stack>
                        </Column>
                      </Grid>

                      <div
                        style={{
                          borderTop: "1px solid var(--cds-border-subtle-01)",
                          marginTop:
                            view === "add" ? "3rem" : "var(--cds-spacing-08)",
                          paddingTop: "var(--cds-spacing-10)",
                        }}
                      >
                        <Stack orientation="horizontal" gap={4}>
                          <Button
                            kind="primary"
                            size="sm"
                            renderIcon={isSubmitting ? undefined : Save}
                            onClick={saveEditor}
                            disabled={
                              isSubmitting ||
                              !!Object.keys(formErrors).filter(
                                (key) => formErrors[key],
                              ).length ||
                              !editingUnit?.names?.[
                                fallbackLocaleCode
                              ]?.trim() ||
                              !editingUnit?.domain
                            }
                          >
                            {isSubmitting ? (
                              <>
                                <Loading style={{ marginRight: "8px" }} />
                                {view === "add" ? (
                                  <FormattedMessage id="button.labUnit.creating" />
                                ) : (
                                  <FormattedMessage id="button.saving" />
                                )}
                              </>
                            ) : view === "add" ? (
                              <FormattedMessage id="button.labUnit.create" />
                            ) : (
                              <FormattedMessage id="button.save" />
                            )}
                          </Button>
                          <Button kind="ghost" size="sm" onClick={goToList}>
                            <FormattedMessage id="button.cancel" />
                          </Button>
                        </Stack>
                      </div>
                    </Tile>

                    {/* Domain change confirmation (OGC-361 CFG-4, OGC-748
                        copy): forward-looking only, no data migration. */}
                    <Modal
                      open={!!pendingDomain}
                      modalHeading={intl.formatMessage({
                        id: "admin.labUnit.domain.confirm.heading",
                      })}
                      primaryButtonText={intl.formatMessage({
                        id: "label.button.confirm",
                      })}
                      secondaryButtonText={intl.formatMessage({
                        id: "button.cancel",
                      })}
                      onRequestClose={cancelDomainChange}
                      onRequestSubmit={confirmDomainChange}
                      size="sm"
                    >
                      <p>
                        <FormattedMessage id="admin.labUnit.domain.confirm.body" />
                      </p>
                    </Modal>
                  </div>
                )}

                {activeSection === "assigned-tests" && (
                  <div>
                    <Tile
                      style={{
                        padding: "var(--cds-spacing-06)",
                        border: "1px solid var(--cds-border-subtle)",
                        borderRadius: "var(--cds-border-radius)",
                      }}
                    >
                      {view === "add" ? (
                        <p
                          style={{
                            color: "var(--cds-text-secondary)",
                            fontSize: "14px",
                            margin: 0,
                          }}
                        >
                          <FormattedMessage id="label.labUnit.tests.addHint" />
                        </p>
                      ) : (
                        <AssignedTestsSection
                          labUnitId={labUnitId}
                          onChange={handleAssignedTestsChange}
                        />
                      )}
                    </Tile>
                  </div>
                )}

                {activeSection === "display-order" && (
                  <div>
                    <Tile
                      style={{
                        padding: "var(--cds-spacing-07)",
                        border: "1px solid var(--cds-border-subtle)",
                        borderRadius: "var(--cds-border-radius)",
                      }}
                    >
                      {view === "add" ? (
                        <p
                          style={{
                            color: "var(--cds-text-secondary)",
                            fontSize: "14px",
                            margin: 0,
                          }}
                        >
                          <FormattedMessage id="label.labUnit.displayOrder.addHint" />
                        </p>
                      ) : (
                        <DisplayOrderSection labUnitId={labUnitId} />
                      )}
                    </Tile>
                  </div>
                )}
              </>
            )}
          </div>
        </Stack>
      </div>
    );
  }

  return null;
}

export default injectIntl(LabUnitManagement);
