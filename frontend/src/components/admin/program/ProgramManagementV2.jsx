import React, { useContext, useEffect, useMemo, useRef, useState } from "react";
import {
  Button,
  Column,
  ContentSwitcher,
  Dropdown,
  FilterableMultiSelect,
  Grid,
  IconButton,
  InlineNotification,
  Modal,
  OverflowMenu,
  OverflowMenuItem,
  RadioButton,
  RadioButtonGroup,
  Select,
  SelectItem,
  Stack,
  Switch,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableExpandedRow,
  TableExpandHeader,
  TableExpandRow,
  TableHead,
  TableHeader,
  TableRow,
  TableToolbar,
  TableToolbarContent,
  TableToolbarSearch,
  Tag,
  TextArea,
  TextInput,
  Tile,
  Toggle,
} from "@carbon/react";
import { Add, TrashCan } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import { NotificationContext } from "../../layout/Layout";
import {
  AlertDialog,
  NotificationKinds,
} from "../../common/CustomNotification";
import PageBreadCrumb from "../../common/PageBreadCrumb";
import {
  getFromOpenElisServer,
  postToOpenElisServerFullResponse,
} from "../../utils/Utils";
import "../../Style.css";

const breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "breadcrums.admin.managment", link: "/MasterListsPage" },
  {
    label: "master.lists.page.test.management",
    link: "/MasterListsPage/testManagementConfigMenu",
  },
  {
    label: "sidenav.label.admin.program",
    link: "/MasterListsPage/programV2",
  },
];

const DOMAINS = ["CLINICAL", "ENVIRONMENTAL", "VECTOR"];
const DOMAIN_TAG_COLOR = {
  CLINICAL: "blue",
  ENVIRONMENTAL: "green",
  VECTOR: "purple",
};
const QUESTION_TYPES = [
  "boolean",
  "choice",
  "checkbox",
  "integer",
  "decimal",
  "date",
  "time",
  "string",
  "text",
  "quantity",
];
// FR-16: a short example sentence rendered under the Question Type dropdown so
// admins can pick the right type without reading FHIR documentation. Quantity's
// unit comes from a FHIR extension in JSON — the note above the ContentSwitcher
// already tells the admin that.
const TYPE_EXAMPLE_IDS = {
  boolean: "admin.programs.questionnaire.question.typeExample.boolean",
  choice: "admin.programs.questionnaire.question.typeExample.choice",
  checkbox: "admin.programs.questionnaire.question.typeExample.checkbox",
  integer: "admin.programs.questionnaire.question.typeExample.integer",
  decimal: "admin.programs.questionnaire.question.typeExample.decimal",
  date: "admin.programs.questionnaire.question.typeExample.date",
  time: "admin.programs.questionnaire.question.typeExample.time",
  string: "admin.programs.questionnaire.question.typeExample.string",
  text: "admin.programs.questionnaire.question.typeExample.text",
  quantity: "admin.programs.questionnaire.question.typeExample.quantity",
};
const TYPE_EXAMPLE_DEFAULTS = {
  boolean: 'Yes/no answer. Example: "First antenatal visit?"',
  choice: 'One option from a fixed list. Example: "Specimen condition".',
  checkbox: 'Multiple options can be selected. Example: "Symptoms present".',
  integer: 'Whole numbers only. Example: "Gestational age (weeks)".',
  decimal: 'Numbers with decimals. Example: "Maternal weight (kg)".',
  date: 'Calendar date. Example: "Date of last menstrual period".',
  time: 'Time of day. Example: "Time of sample collection".',
  string: 'Short free-text. Example: "Provider name".',
  text: 'Long free-text. Example: "Clinical notes".',
  quantity:
    'A number plus a unit (e.g. "Volume collected — 5 mL"). The allowed unit(s) come from a FHIR extension set in JSON mode; a GUI-only Quantity accepts any unit.',
};

function domainLabel(intl, domain) {
  return intl.formatMessage({
    id: `admin.programs.domain.${domain.toLowerCase()}`,
    defaultMessage: domain.charAt(0) + domain.slice(1).toLowerCase(),
  });
}

function ProgramManagementV2() {
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);
  const intl = useIntl();
  const componentMounted = useRef(false);

  const [programSummaries, setProgramSummaries] = useState([]);
  const [programDetails, setProgramDetails] = useState({});
  const [testSections, setTestSections] = useState([]);
  const [query, setQuery] = useState("");
  const [domainFilter, setDomainFilter] = useState("all");
  const [showDeactivated, setShowDeactivated] = useState(false);
  const [expandedId, setExpandedId] = useState(null);
  const [adding, setAdding] = useState(false);
  const [confirmDeactivate, setConfirmDeactivate] = useState(null);
  const [deactivateOrderCount, setDeactivateOrderCount] = useState(null);

  useEffect(() => {
    componentMounted.current = true;
    getFromOpenElisServer("/rest/displayList/PROGRAM", (list) => {
      if (!componentMounted.current) return;
      // Backend fields for domain/active/labUnits are additive (see spec FR-1,
      // FR-10.2). Until the schema slice lands, default them client-side so
      // the UI is meaningful against the existing /rest/program payload.
      setProgramSummaries(
        (list || []).map((row) => ({
          id: row.id,
          name: row.value,
          domain: "CLINICAL",
          labUnitIds: [],
          active: true,
        })),
      );
    });
    getFromOpenElisServer("/rest/lab-units-management", (response) => {
      if (!componentMounted.current) return;
      const rows = Array.isArray(response?.data)
        ? response.data
        : Array.isArray(response)
          ? response
          : [];
      setTestSections(
        rows
          .filter((s) => s.isActive !== false)
          .map((s) => ({
            id: String(s.id),
            value: s.name,
            domain: s.domain || "",
          })),
      );
    });
    return () => {
      componentMounted.current = false;
    };
  }, []);

  const visiblePrograms = useMemo(
    () =>
      programSummaries.filter((p) => {
        if (!showDeactivated && !p.active) return false;
        if (query && !p.name.toLowerCase().includes(query.toLowerCase())) {
          return false;
        }
        if (domainFilter !== "all" && p.domain !== domainFilter) return false;
        return true;
      }),
    [programSummaries, showDeactivated, query, domainFilter],
  );

  const loadProgramDetail = (id) => {
    if (programDetails[id]) return;
    getFromOpenElisServer(`/rest/program/${id}`, (res) => {
      if (!componentMounted.current || !res) return;
      let questions = [];
      if (res.additionalOrderEntryQuestions) {
        try {
          const parsed =
            typeof res.additionalOrderEntryQuestions === "string"
              ? JSON.parse(res.additionalOrderEntryQuestions)
              : res.additionalOrderEntryQuestions;
          if (Array.isArray(parsed?.item)) {
            questions = parsed.item.map((it, i) => ({
              key: `${id}-${i}`,
              linkId: it.linkId || `q${i + 1}`,
              text: it.text || "",
              type: it.type || "string",
              options: Array.isArray(it.answerOption)
                ? it.answerOption.map((opt) => opt.valueString).filter(Boolean)
                : [],
            }));
          }
        } catch (_e) {
          // fall through — JSON tab shows raw payload for repair
        }
      }
      setProgramDetails((prev) => ({
        ...prev,
        [id]: {
          program: res.program || {},
          testSectionId: res.testSectionId || "",
          questions,
          rawQuestionnaire: res.additionalOrderEntryQuestions
            ? JSON.stringify(
                typeof res.additionalOrderEntryQuestions === "string"
                  ? JSON.parse(res.additionalOrderEntryQuestions)
                  : res.additionalOrderEntryQuestions,
                null,
                2,
              )
            : '{\n  "resourceType": "Questionnaire",\n  "item": []\n}',
        },
      }));
    });
  };

  const handleExpand = (row) => {
    const next = expandedId === row.id ? null : row.id;
    setExpandedId(next);
    if (next) loadProgramDetail(next);
  };

  const handleSaveResponse = (res, summary) => {
    setNotificationVisible(true);
    if (res.status === 200 || res.status === "200") {
      addNotification({
        kind: NotificationKinds.success,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "success.add.edited.msg" }),
      });
      // Refresh list so newly-added programs appear.
      getFromOpenElisServer("/rest/displayList/PROGRAM", (list) => {
        if (!componentMounted.current) return;
        setProgramSummaries((prev) => {
          const byId = new Map(prev.map((p) => [p.id, p]));
          return (list || []).map((row) => {
            const existing = byId.get(row.id);
            return {
              id: row.id,
              name: row.value,
              domain:
                summary?.id === row.id
                  ? summary.domain
                  : existing?.domain || "CLINICAL",
              labUnitIds:
                summary?.id === row.id
                  ? summary.labUnitIds
                  : existing?.labUnitIds || [],
              active: existing?.active ?? true,
            };
          });
        });
      });
      setAdding(false);
    } else {
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "error.add.edited.msg" }),
      });
    }
  };

  const handleDeactivate = () => {
    const target = confirmDeactivate;
    if (!target) return;
    setProgramSummaries((prev) =>
      prev.map((p) => (p.id === target.id ? { ...p, active: false } : p)),
    );
    setNotificationVisible(true);
    addNotification({
      kind: NotificationKinds.success,
      title: intl.formatMessage({ id: "notification.title" }),
      message: intl.formatMessage(
        { id: "admin.programs.reactivate.success" },
        { programName: target.name },
      ),
    });
    closeDeactivate();
    setExpandedId(null);
  };

  const openDeactivate = (row) => {
    setConfirmDeactivate(row);
    setDeactivateOrderCount(
      typeof row.historicalOrderCount === "number"
        ? row.historicalOrderCount
        : null,
    );
    // Read-time count (FR-18.2). getFromOpenElisServer silently no-ops on 404,
    // so the modal falls back to generic copy if the endpoint is not present.
    getFromOpenElisServer(`/rest/program/${row.id}/orderCount`, (res) => {
      if (!componentMounted.current) return;
      if (typeof res === "number") {
        setDeactivateOrderCount(res);
      } else if (res && typeof res.count === "number") {
        setDeactivateOrderCount(res.count);
      }
    });
  };

  const closeDeactivate = () => {
    setConfirmDeactivate(null);
    setDeactivateOrderCount(null);
  };

  const handleReactivate = (row) => {
    setProgramSummaries((prev) =>
      prev.map((p) => (p.id === row.id ? { ...p, active: true } : p)),
    );
    setNotificationVisible(true);
    addNotification({
      kind: NotificationKinds.success,
      title: intl.formatMessage({ id: "notification.title" }),
      message: intl.formatMessage(
        { id: "admin.programs.reactivate.success" },
        { programName: row.name },
      ),
    });
  };

  return (
    <>
      {notificationVisible === true ? <AlertDialog /> : ""}
      <div className="adminPageContent">
        <PageBreadCrumb breadcrumbs={breadcrumbs} />
        <Grid fullWidth>
          <Column lg={16} md={8} sm={4}>
            <h3>
              <FormattedMessage
                id="admin.programs.title"
                defaultMessage="Programs"
              />
            </h3>
            <p style={{ color: "var(--cds-text-secondary)" }}>
              <FormattedMessage
                id="admin.programs.subtitle"
                defaultMessage="The initiatives that orders are filed under — each with a domain, one or more lab units, and an optional order-entry questionnaire."
              />
            </p>

            <TableContainer title="" description="">
              <TableToolbar className="programs-toolbar">
                <TableToolbarContent>
                  <TableToolbarSearch
                    persistent
                    placeholder={intl.formatMessage({
                      id: "admin.programs.search",
                      defaultMessage: "Search by name",
                    })}
                    onChange={(e) => setQuery(e.target.value)}
                  />
                  <Dropdown
                    id="domain-filter"
                    titleText=""
                    size="lg"
                    label={intl.formatMessage({
                      id: "admin.programs.list.filter.domain.all",
                      defaultMessage: "All domains",
                    })}
                    items={[
                      { id: "all", label: "All domains" },
                      ...DOMAINS.map((d) => ({
                        id: d,
                        label: domainLabel(intl, d),
                      })),
                    ]}
                    itemToString={(item) => (item ? item.label : "")}
                    onChange={({ selectedItem }) =>
                      setDomainFilter(selectedItem?.id || "all")
                    }
                  />
                  <Toggle
                    id="show-deactivated"
                    size="sm"
                    hideLabel
                    labelText=""
                    labelA={intl.formatMessage({
                      id: "admin.programs.list.toggle.showDeactivated",
                      defaultMessage: "Show deactivated",
                    })}
                    labelB={intl.formatMessage({
                      id: "admin.programs.list.toggle.showDeactivated",
                      defaultMessage: "Show deactivated",
                    })}
                    toggled={showDeactivated}
                    onToggle={setShowDeactivated}
                  />
                  <Button
                    renderIcon={Add}
                    onClick={() => {
                      setAdding((a) => !a);
                      setExpandedId(null);
                    }}
                  >
                    {adding
                      ? intl.formatMessage({
                          id: "button.close",
                          defaultMessage: "Close",
                        })
                      : intl.formatMessage({
                          id: "admin.programs.add",
                          defaultMessage: "Add Program",
                        })}
                  </Button>
                </TableToolbarContent>
              </TableToolbar>

              {adding && (
                <Tile
                  style={{
                    borderLeft: "3px solid var(--cds-interactive)",
                    margin: "0 0 4px",
                  }}
                >
                  <h4>
                    <FormattedMessage
                      id="admin.programs.selector.new"
                      defaultMessage="New Program"
                    />
                  </h4>
                  <ProgramEditor
                    isNew
                    testSections={testSections}
                    onClose={() => setAdding(false)}
                    onSave={(payload, summary) =>
                      postToOpenElisServerFullResponse(
                        "/rest/program",
                        JSON.stringify(payload),
                        (res) => handleSaveResponse(res, summary),
                      )
                    }
                  />
                </Tile>
              )}

              <Table>
                <TableHead>
                  <TableRow>
                    <TableExpandHeader />
                    <TableHeader>
                      <FormattedMessage
                        id="admin.programs.list.column.name"
                        defaultMessage="Name"
                      />
                    </TableHeader>
                    <TableHeader>
                      <FormattedMessage
                        id="admin.programs.list.column.domain"
                        defaultMessage="Domain"
                      />
                    </TableHeader>
                    <TableHeader>
                      <FormattedMessage
                        id="admin.programs.list.column.active"
                        defaultMessage="Status"
                      />
                    </TableHeader>
                    <TableHeader>
                      <FormattedMessage
                        id="admin.programs.list.column.units"
                        defaultMessage="Lab unit(s)"
                      />
                    </TableHeader>
                    <TableHeader />
                  </TableRow>
                </TableHead>
                <TableBody>
                  {visiblePrograms.map((row) => {
                    const detail = programDetails[row.id];
                    const labUnitNames = row.labUnitIds
                      .map(
                        (id) =>
                          testSections.find((s) => String(s.id) === String(id))
                            ?.value,
                      )
                      .filter(Boolean);
                    const derivedName =
                      labUnitNames.length > 0
                        ? labUnitNames.join(", ")
                        : detail
                          ? testSections.find(
                              (s) =>
                                String(s.id) === String(detail.testSectionId),
                            )?.value || "—"
                          : "—";
                    return (
                      <React.Fragment key={row.id}>
                        <TableExpandRow
                          isExpanded={expandedId === row.id}
                          onExpand={() => handleExpand(row)}
                          ariaLabel={intl.formatMessage({
                            id: "admin.programs.row.expand.aria",
                            defaultMessage: "Edit program",
                          })}
                          style={
                            row.active
                              ? undefined
                              : { color: "var(--cds-text-disabled)" }
                          }
                        >
                          <TableCell>{row.name}</TableCell>
                          <TableCell>
                            <Tag type={DOMAIN_TAG_COLOR[row.domain]}>
                              {domainLabel(intl, row.domain)}
                            </Tag>
                          </TableCell>
                          <TableCell>
                            {row.active ? (
                              <Tag type="green">
                                <FormattedMessage
                                  id="admin.programs.list.status.active"
                                  defaultMessage="Active"
                                />
                              </Tag>
                            ) : (
                              <Tag type="gray">
                                <FormattedMessage
                                  id="admin.programs.list.status.inactive"
                                  defaultMessage="Inactive"
                                />
                              </Tag>
                            )}
                          </TableCell>
                          <TableCell>{derivedName}</TableCell>
                          <TableCell>
                            <OverflowMenu
                              aria-label={intl.formatMessage({
                                id: "admin.programs.row.actions.aria",
                                defaultMessage: "Program actions",
                              })}
                              flipped
                            >
                              {row.active ? (
                                <OverflowMenuItem
                                  isDelete
                                  itemText={intl.formatMessage({
                                    id: "admin.programs.action.deactivate",
                                    defaultMessage: "Deactivate",
                                  })}
                                  onClick={() => openDeactivate(row)}
                                />
                              ) : (
                                <OverflowMenuItem
                                  itemText={intl.formatMessage({
                                    id: "admin.programs.action.reactivate",
                                    defaultMessage: "Reactivate",
                                  })}
                                  onClick={() => handleReactivate(row)}
                                />
                              )}
                            </OverflowMenu>
                          </TableCell>
                        </TableExpandRow>
                        {expandedId === row.id && (
                          <TableExpandedRow colSpan={6}>
                            {detail ? (
                              <ProgramEditor
                                row={row}
                                detail={detail}
                                testSections={testSections}
                                onClose={() => setExpandedId(null)}
                                onSave={(payload, summary) =>
                                  postToOpenElisServerFullResponse(
                                    "/rest/program",
                                    JSON.stringify(payload),
                                    (res) => handleSaveResponse(res, summary),
                                  )
                                }
                              />
                            ) : (
                              <div style={{ padding: "1rem" }}>
                                <FormattedMessage
                                  id="loading.label"
                                  defaultMessage="Loading…"
                                />
                              </div>
                            )}
                          </TableExpandedRow>
                        )}
                      </React.Fragment>
                    );
                  })}
                </TableBody>
              </Table>
            </TableContainer>
          </Column>
        </Grid>

        {confirmDeactivate && (
          <Modal
            open
            danger
            modalHeading={intl.formatMessage({
              id: "admin.programs.deactivate.modal.title",
              defaultMessage: "Deactivate this Program?",
            })}
            primaryButtonText={intl.formatMessage({
              id: "admin.programs.deactivate.modal.confirm",
              defaultMessage: "Deactivate",
            })}
            secondaryButtonText={intl.formatMessage({
              id: "admin.programs.deactivate.modal.cancel",
              defaultMessage: "Cancel",
            })}
            onRequestClose={closeDeactivate}
            onRequestSubmit={handleDeactivate}
          >
            <p>
              {deactivateOrderCount === null ? (
                <FormattedMessage
                  id="admin.programs.deactivate.modal.body.unknownCount"
                  defaultMessage='"{programName}" will stop appearing in the order-entry Program picker for new orders. Historical orders keep their program coding and all indicator counts are preserved. You can reactivate it at any time from the Programs list.'
                  values={{ programName: confirmDeactivate.name }}
                />
              ) : deactivateOrderCount === 0 ? (
                <FormattedMessage
                  id="admin.programs.deactivate.modal.body.noOrders"
                  defaultMessage='"{programName}" has no orders and will simply stop appearing for new orders. You can reactivate it at any time from the Programs list.'
                  values={{ programName: confirmDeactivate.name }}
                />
              ) : (
                <FormattedMessage
                  id="admin.programs.deactivate.modal.body"
                  defaultMessage='"{programName}" will stop appearing in the order-entry Program picker for new orders. Its {orderCount, plural, one {# historical order} other {# historical orders}} keep their program coding and all indicator counts are preserved. You can reactivate it at any time from the Programs list.'
                  values={{
                    programName: confirmDeactivate.name,
                    orderCount: deactivateOrderCount,
                  }}
                />
              )}
            </p>
          </Modal>
        )}
      </div>
    </>
  );
}

function ProgramEditor({ row, detail, isNew, testSections, onClose, onSave }) {
  const intl = useIntl();
  const initialProgram = detail?.program || {};
  const [programName, setProgramName] = useState(
    initialProgram.programName || "",
  );
  const code = initialProgram.code || "";
  const questionnaireUUID = initialProgram.questionnaireUUID || "";
  const [labUnitIds, setLabUnitIds] = useState(
    detail?.testSectionId ? [String(detail.testSectionId)] : [],
  );
  const [domain, setDomain] = useState(row?.domain || "");

  const availableLabUnits = useMemo(
    () => (domain ? testSections.filter((s) => s.domain === domain) : []),
    [testSections, domain],
  );
  const availableLabUnitIds = useMemo(
    () => new Set(availableLabUnits.map((s) => s.id)),
    [availableLabUnits],
  );
  useEffect(() => {
    setLabUnitIds((ids) => {
      const kept = ids.filter((id) => availableLabUnitIds.has(id));
      return kept.length === ids.length ? ids : kept;
    });
  }, [availableLabUnitIds]);
  const [mode, setMode] = useState(0);
  const EMPTY_QUESTIONNAIRE_JSON =
    '{\n  "resourceType": "Questionnaire",\n  "item": []\n}';
  const initialRawJson = detail?.rawQuestionnaire || EMPTY_QUESTIONNAIRE_JSON;
  // Baseline preserves FHIR properties the Visual Builder does not expose
  // (extensions, definition, code, enableWhen, etc.) so a Visual-mode save
  // does not silently strip them from the persisted Questionnaire.
  const parseBaseline = (json) => {
    try {
      return JSON.parse(json);
    } catch (_e) {
      return { resourceType: "Questionnaire", item: [] };
    }
  };
  const [questions, setQuestions] = useState(detail?.questions || []);
  const [rawJson, setRawJson] = useState(initialRawJson);
  const [baseline, setBaseline] = useState(() => parseBaseline(initialRawJson));
  const [jsonError, setJsonError] = useState(null);
  const [jsonValid, setJsonValid] = useState(null);
  // Per-domain snapshots of the questionnaire builder so switching domains
  // preserves each domain's own edits (Visual + JSON) instead of stomping them.
  const [draftsByDomain, setDraftsByDomain] = useState(() =>
    row?.domain
      ? {
          [row.domain]: {
            questions: detail?.questions || [],
            rawJson: initialRawJson,
            baseline: parseBaseline(initialRawJson),
          },
        }
      : {},
  );

  const canSave = Boolean(domain) && Boolean(programName);

  const patchQuestion = (key, patch) =>
    setQuestions((qs) =>
      qs.map((q) => (q.key === key ? { ...q, ...patch } : q)),
    );

  const validateQuestionnaire = (parsed) => {
    if (parsed?.resourceType !== "Questionnaire") {
      throw new Error(
        intl.formatMessage({
          id: "admin.programs.questionnaire.json.error.resourceType",
          defaultMessage: 'resourceType must be "Questionnaire"',
        }),
      );
    }
    if (!Array.isArray(parsed.item)) {
      throw new Error(
        intl.formatMessage({
          id: "admin.programs.questionnaire.json.error.itemArray",
          defaultMessage: "item must be an array",
        }),
      );
    }
    const seen = new Set();
    for (const it of parsed.item) {
      if (!it.linkId || !it.text || !QUESTION_TYPES.includes(it.type)) {
        throw new Error(
          intl.formatMessage(
            {
              id: "admin.programs.questionnaire.json.error.itemShape",
              defaultMessage:
                "each item must have linkId, text, and type in [{allowed}]",
            },
            { allowed: QUESTION_TYPES.join(", ") },
          ),
        );
      }
      if (seen.has(it.linkId)) {
        throw new Error(
          intl.formatMessage(
            {
              id: "admin.programs.questionnaire.json.error.duplicateLinkId",
              defaultMessage:
                'duplicate linkId "{linkId}" — each item must have a unique linkId',
            },
            { linkId: it.linkId },
          ),
        );
      }
      seen.add(it.linkId);
    }
  };

  const buildFromQuestions = () => {
    const baseItemsByLink = new Map(
      (baseline?.item || []).map((it) => [it.linkId, it]),
    );
    return {
      ...baseline,
      resourceType: "Questionnaire",
      item: questions.map((q, i) => {
        const linkId = q.linkId || `q${i + 1}`;
        const baseItem = baseItemsByLink.get(linkId) || {};
        const merged = {
          ...baseItem,
          linkId,
          text: q.text,
          type: q.type,
        };
        if (
          (q.type === "choice" || q.type === "checkbox") &&
          q.options.length
        ) {
          merged.answerOption = q.options.map((o) => ({ valueString: o }));
        } else if (q.type !== "choice" && q.type !== "checkbox") {
          delete merged.answerOption;
        }
        return merged;
      }),
    };
  };

  const buildQuestionnairePayload = () => {
    const built = mode === 1 ? JSON.parse(rawJson) : buildFromQuestions();
    validateQuestionnaire(built);
    return built;
  };

  const handleSave = () => {
    let additionalOrderEntryQuestions;
    try {
      additionalOrderEntryQuestions = buildQuestionnairePayload();
    } catch (e) {
      setJsonError(e.message);
      setJsonValid(null);
      if (mode === 0) {
        try {
          setRawJson(JSON.stringify(buildFromQuestions(), null, 2));
        } catch (_e2) {
          /* ignore — keep existing rawJson */
        }
        setMode(1);
      }
      return;
    }
    // Saving from JSON mode: sync visual-mode state to the just-parsed
    // questionnaire so the Example preview reflects the saved payload
    // without needing a page reload.
    if (mode === 1) {
      setQuestions(questionsFromParsed(additionalOrderEntryQuestions));
      setBaseline(additionalOrderEntryQuestions);
    }
    const payload = {
      program: {
        id: initialProgram.id || "",
        programName,
        code,
        questionnaireUUID,
      },
      // Backend today accepts a single testSectionId; the many-to-many
      // program_lab_unit surface lands with the schema slice (FR-10.2).
      testSectionId: labUnitIds[0] || "",
      additionalOrderEntryQuestions,
    };
    const summary = {
      id: initialProgram.id || row?.id,
      domain,
      labUnitIds,
    };
    onSave(payload, summary);
  };

  const validateJson = () => {
    try {
      const parsed = JSON.parse(rawJson);
      validateQuestionnaire(parsed);
      setJsonError(null);
      setJsonValid(parsed.item.length);
      setBaseline(parsed);
    } catch (e) {
      setJsonValid(null);
      setJsonError(e.message);
    }
  };

  const reformatJson = () => {
    try {
      setRawJson(JSON.stringify(JSON.parse(rawJson), null, 2));
      setJsonError(null);
    } catch (e) {
      setJsonError(e.message);
      setJsonValid(null);
    }
  };

  const questionsFromParsed = (parsed) =>
    (parsed.item || []).map((it, i) => ({
      key: `sync-${Date.now()}-${i}`,
      linkId: it.linkId || `q${i + 1}`,
      text: it.text || "",
      type: it.type || "string",
      options: Array.isArray(it.answerOption)
        ? it.answerOption.map((o) => o.valueString).filter(Boolean)
        : [],
    }));

  // Sync state across tabs so edits in one mode carry over to the other.
  // Visual → JSON: always succeeds (serialize current questions + baseline).
  // JSON → Visual: only proceeds if JSON parses and passes shape validation;
  // otherwise surfaces the error and keeps the user on the JSON tab.
  const handleModeChange = (nextIndex) => {
    if (nextIndex === mode) return;
    if (nextIndex === 1) {
      try {
        setRawJson(JSON.stringify(buildFromQuestions(), null, 2));
        setJsonError(null);
        setJsonValid(null);
      } catch (_e) {
        /* leave rawJson untouched */
      }
      setMode(1);
      return;
    }
    try {
      const parsed = JSON.parse(rawJson);
      validateQuestionnaire(parsed);
      setQuestions(questionsFromParsed(parsed));
      setBaseline(parsed);
      setJsonError(null);
      setJsonValid(parsed.item.length);
      setMode(0);
    } catch (e) {
      setJsonError(e.message);
      setJsonValid(null);
    }
  };

  return (
    <Stack gap={5} style={{ padding: "0.5rem 0" }}>
      <Tile>
        <Grid>
          <Column lg={8} md={4} sm={2}>
            <TextInput
              id={`pname-${row ? row.id : "new"}`}
              labelText={intl.formatMessage({
                id: "admin.programs.basicInfo.programName.label",
                defaultMessage: "Program Name",
              })}
              value={programName}
              onChange={(e) => setProgramName(e.target.value)}
              placeholder={
                isNew
                  ? intl.formatMessage({
                      id: "admin.programs.basicInfo.programName.placeholder",
                      defaultMessage: "e.g. HBV Antenatal Screening",
                    })
                  : undefined
              }
              helperText={intl.formatMessage({
                id: "admin.programs.basicInfo.programName.helper",
                defaultMessage:
                  "The name reception staff see when picking a program at order entry.",
              })}
            />
          </Column>
          <Column lg={8} md={4} sm={2}>
            <FilterableMultiSelect
              key={`units-${domain || "none"}`}
              id={`units-${row ? row.id : "new"}`}
              titleText={intl.formatMessage({
                id: "admin.programs.basicInfo.labUnits.label",
                defaultMessage: "Lab unit(s)",
              })}
              helperText={
                domain
                  ? intl.formatMessage(
                      {
                        id: "admin.programs.basicInfo.labUnits.helper.byDomain",
                        defaultMessage:
                          "Lab units in the {domain} domain that run this program.",
                      },
                      { domain: domainLabel(intl, domain) },
                    )
                  : intl.formatMessage({
                      id: "admin.programs.basicInfo.labUnits.helper.pickDomain",
                      defaultMessage:
                        "Pick a domain first to choose lab units.",
                    })
              }
              disabled={!domain}
              items={availableLabUnits.map((s) => s.id)}
              itemToString={(id) =>
                availableLabUnits.find((s) => s.id === id)?.value || id
              }
              initialSelectedItems={labUnitIds}
              selectionFeedback="top-after-reopen"
              onChange={({ selectedItems }) =>
                setLabUnitIds(selectedItems || [])
              }
            />
          </Column>
        </Grid>

        <div style={{ marginTop: "1rem" }}>
          <RadioButtonGroup
            legendText={intl.formatMessage({
              id: "admin.programs.basicInfo.domain.label",
              defaultMessage: "Domain",
            })}
            name={`domain-${row ? row.id : "new"}`}
            valueSelected={domain}
            onChange={(nextDomain) => {
              if (nextDomain === domain) return;
              // Snapshot the current draft under the outgoing domain so it
              // can be restored if the user switches back.
              if (domain) {
                setDraftsByDomain((prev) => ({
                  ...prev,
                  [domain]: { questions, rawJson, baseline },
                }));
              }
              const restored = draftsByDomain[nextDomain];
              const nextQuestions = restored?.questions ?? [];
              const nextRawJson = restored?.rawJson ?? EMPTY_QUESTIONNAIRE_JSON;
              const nextBaseline =
                restored?.baseline ?? parseBaseline(EMPTY_QUESTIONNAIRE_JSON);
              setDomain(nextDomain);
              setLabUnitIds([]);
              setQuestions(nextQuestions);
              setRawJson(nextRawJson);
              setBaseline(nextBaseline);
              setJsonError(null);
              setJsonValid(null);
            }}
            orientation="vertical"
          >
            <RadioButton
              labelText={domainLabel(intl, "CLINICAL")}
              value="CLINICAL"
              id={`dc-${row ? row.id : "new"}`}
            />
            <RadioButton
              labelText={domainLabel(intl, "ENVIRONMENTAL")}
              value="ENVIRONMENTAL"
              id={`de-${row ? row.id : "new"}`}
            />
            <RadioButton
              labelText={domainLabel(intl, "VECTOR")}
              value="VECTOR"
              id={`dv-${row ? row.id : "new"}`}
            />
          </RadioButtonGroup>
          {!domain && (
            <p
              style={{
                color: "var(--cds-text-error)",
                fontSize: 12,
              }}
            >
              <FormattedMessage
                id="admin.programs.basicInfo.domain.required"
                defaultMessage="Domain is required"
              />
            </p>
          )}
        </div>
      </Tile>

      <ContentSwitcher
        selectedIndex={mode}
        onChange={({ index }) => handleModeChange(index)}
      >
        <Switch
          name="visual"
          text={intl.formatMessage({
            id: "admin.programs.questionnaire.mode.visualBuilder",
            defaultMessage: "Visual Builder",
          })}
        />
        <Switch
          name="json"
          text={intl.formatMessage({
            id: "admin.programs.questionnaire.mode.json",
            defaultMessage: "JSON",
          })}
        />
      </ContentSwitcher>

      <Grid>
        <Column lg={9} md={8} sm={4}>
          {mode === 0 ? (
            <Stack gap={3}>
              {questions.map((q) => (
                <Tile key={q.key}>
                  <div
                    style={{
                      display: "flex",
                      justifyContent: "space-between",
                      gap: "0.5rem",
                    }}
                  >
                    <TextInput
                      id={`qt-${q.key}`}
                      labelText={intl.formatMessage({
                        id: "admin.programs.questionnaire.question.text.label",
                        defaultMessage: "Question Text",
                      })}
                      value={q.text}
                      onChange={(e) =>
                        patchQuestion(q.key, { text: e.target.value })
                      }
                    />
                    <OverflowMenu
                      aria-label={intl.formatMessage({
                        id: "admin.programs.questionnaire.question.actions.aria",
                        defaultMessage: "Question actions",
                      })}
                    >
                      <OverflowMenuItem
                        isDelete
                        itemText={intl.formatMessage({
                          id: "admin.programs.questionnaire.question.actions.delete",
                          defaultMessage: "Delete question",
                        })}
                        onClick={() =>
                          setQuestions((qs) =>
                            qs.filter((x) => x.key !== q.key),
                          )
                        }
                      />
                    </OverflowMenu>
                  </div>
                  <Select
                    id={`qty-${q.key}`}
                    labelText={intl.formatMessage({
                      id: "admin.programs.questionnaire.question.type.label",
                      defaultMessage: "Question Type",
                    })}
                    value={q.type}
                    onChange={(e) =>
                      patchQuestion(q.key, { type: e.target.value })
                    }
                  >
                    {QUESTION_TYPES.map((ty) => (
                      <SelectItem
                        key={ty}
                        value={ty}
                        text={ty[0].toUpperCase() + ty.slice(1)}
                      />
                    ))}
                  </Select>
                  <p
                    style={{
                      fontSize: 12,
                      color: "var(--cds-text-secondary)",
                      marginTop: 4,
                    }}
                  >
                    {intl.formatMessage({
                      id: TYPE_EXAMPLE_IDS[q.type],
                      defaultMessage: TYPE_EXAMPLE_DEFAULTS[q.type],
                    })}
                  </p>
                  {(q.type === "choice" || q.type === "checkbox") && (
                    <Stack gap={2} style={{ marginTop: 8 }}>
                      <span
                        style={{ fontSize: 12, textTransform: "uppercase" }}
                      >
                        <FormattedMessage
                          id="admin.programs.questionnaire.answerOptions.section.title"
                          defaultMessage="Answer options"
                        />
                      </span>
                      {q.options.map((o, i) => (
                        <div key={i} style={{ display: "flex", gap: 8 }}>
                          <TextInput
                            id={`opt-${q.key}-${i}`}
                            size="sm"
                            labelText=""
                            value={o}
                            onChange={(e) => {
                              const next = [...q.options];
                              next[i] = e.target.value;
                              patchQuestion(q.key, { options: next });
                            }}
                          />
                          <IconButton
                            kind="ghost"
                            size="sm"
                            label={intl.formatMessage({
                              id: "admin.programs.questionnaire.answerOptions.deleteOption",
                              defaultMessage: "Delete option",
                            })}
                            onClick={() =>
                              patchQuestion(q.key, {
                                options: q.options.filter((_, j) => j !== i),
                              })
                            }
                          >
                            <TrashCan />
                          </IconButton>
                        </div>
                      ))}
                      {q.options.length === 0 && (
                        <p
                          style={{
                            fontSize: 12,
                            color: "var(--cds-text-secondary)",
                          }}
                        >
                          <FormattedMessage
                            id="admin.programs.questionnaire.answerOptions.empty"
                            defaultMessage="No options yet — add at least one so reception can pick a value."
                          />
                        </p>
                      )}
                      <Button
                        kind="ghost"
                        size="sm"
                        renderIcon={Add}
                        onClick={() =>
                          patchQuestion(q.key, {
                            options: [...q.options, ""],
                          })
                        }
                      >
                        <FormattedMessage
                          id="admin.programs.questionnaire.answerOptions.addOption"
                          defaultMessage="Add option"
                        />
                      </Button>
                    </Stack>
                  )}
                </Tile>
              ))}
              {questions.length === 0 && (
                <p
                  style={{
                    fontSize: 12,
                    color: "var(--cds-text-secondary)",
                  }}
                >
                  <FormattedMessage
                    id="admin.programs.guidance.gui.emptyState.body"
                    defaultMessage="No questions yet — add questions one at a time, or paste a Questionnaire in JSON mode."
                  />
                </p>
              )}
              <Button
                kind="ghost"
                renderIcon={Add}
                onClick={() =>
                  setQuestions((qs) => {
                    const taken = new Set(
                      qs.map((q) => q.linkId).filter(Boolean),
                    );
                    let n = qs.length + 1;
                    while (taken.has(`q${n}`)) n += 1;
                    return [
                      ...qs,
                      {
                        key: `new-${Date.now()}`,
                        linkId: `q${n}`,
                        text: "",
                        type: "string",
                        options: [],
                      },
                    ];
                  })
                }
              >
                {questions.length === 0 ? (
                  <FormattedMessage
                    id="admin.programs.guidance.gui.emptyState.action"
                    defaultMessage="Add First Question"
                  />
                ) : (
                  <FormattedMessage
                    id="admin.programs.questionnaire.question.addNew"
                    defaultMessage="Add New Question"
                  />
                )}
              </Button>
            </Stack>
          ) : (
            <Stack gap={3}>
              <TextArea
                id={`json-${row ? row.id : "new"}`}
                labelText={intl.formatMessage({
                  id: "admin.programs.questionnaire.mode.json",
                  defaultMessage: "JSON",
                })}
                rows={12}
                value={rawJson}
                onChange={(e) => {
                  setRawJson(e.target.value);
                  setJsonValid(null);
                  setJsonError(null);
                }}
                style={{
                  fontFamily:
                    "var(--cds-code-01-font-family, ui-monospace, Menlo, Consolas, monospace)",
                }}
              />
              <div style={{ display: "flex", gap: "0.5rem" }}>
                <Button kind="tertiary" size="sm" onClick={validateJson}>
                  <FormattedMessage
                    id="admin.programs.questionnaire.json.validate"
                    defaultMessage="Validate JSON"
                  />
                </Button>
                <Button kind="ghost" size="sm" onClick={reformatJson}>
                  <FormattedMessage
                    id="admin.programs.questionnaire.json.reformat"
                    defaultMessage="Reformat"
                  />
                </Button>
              </div>
              {jsonError && (
                <InlineNotification
                  kind="error"
                  lowContrast
                  title={intl.formatMessage({
                    id: "admin.programs.questionnaire.json.error.title",
                    defaultMessage: "Invalid Questionnaire",
                  })}
                  subtitle={jsonError}
                  hideCloseButton
                />
              )}
              {jsonValid !== null && (
                <InlineNotification
                  kind="success"
                  lowContrast
                  hideCloseButton
                  title={intl.formatMessage(
                    {
                      id: "admin.programs.questionnaire.json.validated",
                      defaultMessage: "Validated — {count} question(s)",
                    },
                    { count: jsonValid },
                  )}
                />
              )}
              <Tile>
                <p style={{ fontSize: 12 }}>
                  <FormattedMessage
                    id="admin.programs.guidance.json.referenceCard.format"
                    defaultMessage="Format: FHIR R4 Questionnaire"
                  />{" "}
                  — allowed item.type: {QUESTION_TYPES.join(", ")}.
                </p>
                <p
                  style={{
                    fontSize: 12,
                    color: "var(--cds-text-secondary)",
                    marginTop: 4,
                  }}
                >
                  <FormattedMessage
                    id="admin.programs.guidance.json.referenceCard.advanced"
                    defaultMessage="Advanced features (enableWhen, Quantity units via unit/unitOption, nested items) are edited here in JSON — the Visual Builder preserves them but does not surface them."
                  />
                </p>
              </Tile>
            </Stack>
          )}
        </Column>

        <Column lg={7} md={8} sm={4}>
          <p
            style={{
              fontSize: 12,
              textTransform: "uppercase",
              color: "var(--cds-text-secondary)",
            }}
          >
            <FormattedMessage
              id="admin.programs.questionnaire.preview.label"
              defaultMessage="Example"
            />
          </p>
          <Tile>
            {questions.length === 0 ? (
              <p style={{ color: "var(--cds-text-secondary)" }}>
                <FormattedMessage
                  id="admin.programs.questionnaire.preview.empty"
                  defaultMessage="No questions yet — start adding questions to see the preview."
                />
              </p>
            ) : (
              questions.map((q) => (
                <div key={q.key} style={{ marginBottom: 12 }}>
                  <label
                    style={{
                      fontSize: 12,
                      display: "block",
                      marginBottom: 4,
                    }}
                  >
                    {q.text ||
                      intl.formatMessage({
                        id: "admin.programs.questionnaire.preview.untitled",
                        defaultMessage: "(untitled)",
                      })}
                  </label>
                  {q.type === "choice" && (
                    <Select id={`pv-${q.key}`} labelText="" disabled>
                      <SelectItem text={q.options[0] || "—"} />
                    </Select>
                  )}
                  {q.type === "boolean" && (
                    <span style={{ fontSize: 13 }}>◯ Yes ◯ No</span>
                  )}
                  {q.type === "text" && (
                    <TextArea
                      id={`pv-${q.key}`}
                      labelText=""
                      disabled
                      rows={2}
                    />
                  )}
                  {["integer", "decimal", "string", "date", "time"].includes(
                    q.type,
                  ) && (
                    <TextInput
                      id={`pv-${q.key}`}
                      labelText=""
                      disabled
                      placeholder={q.type}
                    />
                  )}
                  {q.type === "quantity" && (
                    <div style={{ display: "flex", gap: 6 }}>
                      <TextInput
                        id={`pv-${q.key}`}
                        labelText=""
                        disabled
                        placeholder="number"
                      />
                      <Select id={`pvu-${q.key}`} labelText="" disabled>
                        <SelectItem text="unit" />
                      </Select>
                    </div>
                  )}
                  {q.type === "checkbox" &&
                    q.options.map((o, i) => (
                      <div key={i} style={{ fontSize: 13 }}>
                        ☐ {o}
                      </div>
                    ))}
                </div>
              ))
            )}
          </Tile>
        </Column>
      </Grid>

      <div style={{ display: "flex", gap: "0.5rem" }}>
        <Button kind="primary" disabled={!canSave} onClick={handleSave}>
          <FormattedMessage id="label.button.submit" defaultMessage="Save" />
        </Button>
        <Button kind="secondary" onClick={onClose}>
          <FormattedMessage id="label.button.cancel" defaultMessage="Cancel" />
        </Button>
      </div>
    </Stack>
  );
}

export default ProgramManagementV2;
