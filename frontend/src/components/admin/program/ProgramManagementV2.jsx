import React, { useContext, useEffect, useMemo, useRef, useState } from "react";
import {
  Button,
  Column,
  ComboBox,
  ContentSwitcher,
  Dropdown,
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
function domainLabel(intl, domain) {
  return intl.formatMessage({
    id: `admin.programs.domain.${domain.toLowerCase()}`,
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
    getFromOpenElisServer("/rest/program-list", (list) => {
      if (!componentMounted.current) return;
      setProgramSummaries(
        (list || []).map((row) => ({
          id: row.id,
          name: row.name,
          domain: row.domain || "CLINICAL",
          labUnitId:
            Array.isArray(row.labUnitIds) && row.labUnitIds.length > 0
              ? String(row.labUnitIds[0])
              : "",
          active: row.active !== false,
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
          testSectionId:
            Array.isArray(res.labUnitIds) && res.labUnitIds.length > 0
              ? String(res.labUnitIds[0])
              : res.testSectionId || "",
          domain: res.domain || "",
          active: res.active !== false,
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
      // Refresh list so newly-added programs and just-persisted
      // domain/active/labUnit changes are reflected canonically.
      getFromOpenElisServer("/rest/program-list", (list) => {
        if (!componentMounted.current) return;
        setProgramSummaries(
          (list || []).map((row) => ({
            id: row.id,
            name: row.name,
            domain: row.domain || "CLINICAL",
            labUnitId:
              Array.isArray(row.labUnitIds) && row.labUnitIds.length > 0
                ? String(row.labUnitIds[0])
                : "",
            active: row.active !== false,
          })),
        );
      });
      // Drop any cached editor state for this program so the next expand
      // pulls the freshly-persisted domain/active/labUnitIds from the server.
      if (summary?.id) {
        setProgramDetails((prev) => {
          if (!prev[summary.id]) return prev;
          const next = { ...prev };
          delete next[summary.id];
          return next;
        });
      }
      // Collapse the editor and return to the list. Otherwise the row stays
      // expanded, but detail was just cleared, so the ProgramEditor slot
      // would flash the "Loading..." fallback while the next fetch runs.
      setExpandedId(null);
      setAdding(false);
    } else {
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "error.add.edited.msg" }),
      });
    }
  };

  // The controller's POST handler overwrites program.programName/code from the
  // request body, so a lifecycle-only flip still has to send the full existing
  // state. Load detail if it is not cached, then POST with `active` flipped.
  const persistActiveFlag = (row, nextActive) => {
    const submit = (detail) => {
      const payload = {
        program: {
          id: row.id,
          programName: detail?.program?.programName || row.name,
          code: detail?.program?.code || "",
          ...(detail?.program?.questionnaireUUID
            ? { questionnaireUUID: detail.program.questionnaireUUID }
            : {}),
        },
        domain: row.domain || "CLINICAL",
        active: nextActive,
        labUnitIds: row.labUnitId ? [row.labUnitId] : [],
        testSectionId: row.labUnitId || "",
        additionalOrderEntryQuestions: (() => {
          if (!detail?.rawQuestionnaire) {
            return { resourceType: "Questionnaire", item: [] };
          }
          try {
            return JSON.parse(detail.rawQuestionnaire);
          } catch (_e) {
            return { resourceType: "Questionnaire", item: [] };
          }
        })(),
      };
      postToOpenElisServerFullResponse(
        "/rest/program",
        JSON.stringify(payload),
        (res) => {
          if (!componentMounted.current) return;
          setNotificationVisible(true);
          if (res && (res.status === 200 || res.status === "200")) {
            setProgramSummaries((prev) =>
              prev.map((p) =>
                p.id === row.id ? { ...p, active: nextActive } : p,
              ),
            );
            addNotification({
              kind: NotificationKinds.success,
              title: intl.formatMessage({ id: "notification.title" }),
              message: intl.formatMessage(
                { id: "admin.programs.reactivate.success" },
                { programName: row.name },
              ),
            });
          } else {
            addNotification({
              kind: NotificationKinds.error,
              title: intl.formatMessage({ id: "notification.title" }),
              message: intl.formatMessage({ id: "error.add.edited.msg" }),
            });
          }
        },
      );
    };
    const cached = programDetails[row.id];
    if (cached) {
      submit(cached);
    } else {
      getFromOpenElisServer(`/rest/program/${row.id}`, (res) => {
        if (!componentMounted.current || !res) return;
        submit({
          program: res.program || {},
          rawQuestionnaire: res.additionalOrderEntryQuestions
            ? typeof res.additionalOrderEntryQuestions === "string"
              ? res.additionalOrderEntryQuestions
              : JSON.stringify(res.additionalOrderEntryQuestions)
            : null,
        });
      });
    }
  };

  const handleDeactivate = () => {
    const target = confirmDeactivate;
    if (!target) return;
    persistActiveFlag(target, false);
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
    persistActiveFlag(row, true);
  };

  return (
    <>
      {notificationVisible === true ? <AlertDialog /> : ""}
      <div className="adminPageContent">
        <PageBreadCrumb breadcrumbs={breadcrumbs} />
        <Grid fullWidth>
          <Column lg={16} md={8} sm={4}>
            <h3>
              <FormattedMessage id="admin.programs.title" />
            </h3>
            <p style={{ color: "var(--cds-text-secondary)" }}>
              <FormattedMessage id="admin.programs.subtitle" />
            </p>

            <TableContainer title="" description="">
              <TableToolbar className="programs-toolbar">
                <TableToolbarContent>
                  <TableToolbarSearch
                    persistent
                    placeholder={intl.formatMessage({
                      id: "admin.programs.search",
                    })}
                    onChange={(e) => setQuery(e.target.value)}
                  />
                  <Dropdown
                    id="domain-filter"
                    titleText=""
                    size="lg"
                    label={intl.formatMessage({
                      id: "admin.programs.list.filter.domain.all",
                    })}
                    items={[
                      {
                        id: "all",
                        label: intl.formatMessage({
                          id: "admin.programs.list.filter.domain.all",
                        }),
                      },
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
                    })}
                    labelB={intl.formatMessage({
                      id: "admin.programs.list.toggle.showDeactivated",
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
                        })
                      : intl.formatMessage({
                          id: "admin.programs.add",
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
                    <FormattedMessage id="admin.programs.selector.new" />
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
                      <FormattedMessage id="admin.programs.list.column.name" />
                    </TableHeader>
                    <TableHeader>
                      <FormattedMessage id="admin.programs.list.column.domain" />
                    </TableHeader>
                    <TableHeader>
                      <FormattedMessage id="admin.programs.list.column.active" />
                    </TableHeader>
                    <TableHeader>
                      <FormattedMessage id="admin.programs.list.column.units" />
                    </TableHeader>
                    <TableHeader />
                  </TableRow>
                </TableHead>
                <TableBody>
                  {visiblePrograms.map((row) => {
                    const detail = programDetails[row.id];
                    const rowUnitName = row.labUnitId
                      ? testSections.find(
                          (s) => String(s.id) === String(row.labUnitId),
                        )?.value
                      : null;
                    const derivedName =
                      rowUnitName ||
                      (detail
                        ? testSections.find(
                            (s) =>
                              String(s.id) === String(detail.testSectionId),
                          )?.value || "—"
                        : "—");
                    return (
                      <React.Fragment key={row.id}>
                        <TableExpandRow
                          isExpanded={expandedId === row.id}
                          onExpand={() => handleExpand(row)}
                          ariaLabel={intl.formatMessage({
                            id: "admin.programs.row.expand.aria",
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
                                <FormattedMessage id="admin.programs.list.status.active" />
                              </Tag>
                            ) : (
                              <Tag type="gray">
                                <FormattedMessage id="admin.programs.list.status.inactive" />
                              </Tag>
                            )}
                          </TableCell>
                          <TableCell>{derivedName}</TableCell>
                          <TableCell>
                            <OverflowMenu
                              aria-label={intl.formatMessage({
                                id: "admin.programs.row.actions.aria",
                              })}
                              flipped
                            >
                              {row.active ? (
                                <OverflowMenuItem
                                  isDelete
                                  itemText={intl.formatMessage({
                                    id: "admin.programs.action.deactivate",
                                  })}
                                  onClick={() => openDeactivate(row)}
                                />
                              ) : (
                                <OverflowMenuItem
                                  itemText={intl.formatMessage({
                                    id: "admin.programs.action.reactivate",
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
                                <FormattedMessage id="loading.label" />
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
            })}
            primaryButtonText={intl.formatMessage({
              id: "admin.programs.deactivate.modal.confirm",
            })}
            secondaryButtonText={intl.formatMessage({
              id: "admin.programs.deactivate.modal.cancel",
            })}
            onRequestClose={closeDeactivate}
            onRequestSubmit={handleDeactivate}
          >
            <p>
              {deactivateOrderCount === null ? (
                <FormattedMessage
                  id="admin.programs.deactivate.modal.body.unknownCount"
                  values={{ programName: confirmDeactivate.name }}
                />
              ) : deactivateOrderCount === 0 ? (
                <FormattedMessage
                  id="admin.programs.deactivate.modal.body.noOrders"
                  values={{ programName: confirmDeactivate.name }}
                />
              ) : (
                <FormattedMessage
                  id="admin.programs.deactivate.modal.body"
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
  const [code, setCode] = useState(initialProgram.code || "");
  const [questionnaireUUID, setQuestionnaireUUID] = useState(
    initialProgram.questionnaireUUID || "",
  );
  const [labUnitId, setLabUnitId] = useState(
    detail?.testSectionId ? String(detail.testSectionId) : "",
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
    setLabUnitId((id) => (id && availableLabUnitIds.has(id) ? id : ""));
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

  const uuidPattern =
    /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
  const trimmedUUID = questionnaireUUID.trim();
  const uuidInvalid = trimmedUUID.length > 0 && !uuidPattern.test(trimmedUUID);
  // Backend @Pattern on Program.code — letters, digits, underscore and spaces only.
  const codePattern = /^[a-z0-9_ ]*$/i;
  const codeInvalid = !codePattern.test(code);
  const canSave =
    Boolean(domain) && Boolean(programName) && !uuidInvalid && !codeInvalid;

  const patchQuestion = (key, patch) =>
    setQuestions((qs) =>
      qs.map((q) => (q.key === key ? { ...q, ...patch } : q)),
    );

  const validateQuestionnaire = (parsed) => {
    if (parsed?.resourceType !== "Questionnaire") {
      throw new Error(
        intl.formatMessage({
          id: "admin.programs.questionnaire.json.error.resourceType",
        }),
      );
    }
    if (!Array.isArray(parsed.item)) {
      throw new Error(
        intl.formatMessage({
          id: "admin.programs.questionnaire.json.error.itemArray",
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
        ...(trimmedUUID ? { questionnaireUUID: trimmedUUID } : {}),
      },
      domain,
      // The editor is single-select today (matches the current UX); the
      // backend's program_lab_unit junction is many-to-many so the payload
      // sends an array. Legacy testSectionId is kept populated for readers
      // that have not migrated to labUnitIds.
      labUnitIds: labUnitId ? [labUnitId] : [],
      testSectionId: labUnitId || "",
      additionalOrderEntryQuestions,
    };
    const summary = {
      id: initialProgram.id || row?.id,
      domain,
      labUnitId,
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
              })}
              value={programName}
              onChange={(e) => setProgramName(e.target.value)}
              placeholder={
                isNew
                  ? intl.formatMessage({
                      id: "admin.programs.basicInfo.programName.placeholder",
                    })
                  : undefined
              }
            />
          </Column>
          <Column lg={8} md={4} sm={2}>
            <TextInput
              id={`pcode-${row ? row.id : "new"}`}
              labelText={intl.formatMessage({
                id: "admin.programs.basicInfo.code.label",
              })}
              value={code}
              onChange={(e) => setCode(e.target.value)}
              placeholder={
                isNew
                  ? intl.formatMessage({
                      id: "admin.programs.basicInfo.code.placeholder",
                    })
                  : undefined
              }
              invalid={codeInvalid}
              invalidText={intl.formatMessage({
                id: "admin.programs.basicInfo.code.invalid",
              })}
            />
          </Column>
          <Column lg={8} md={4} sm={2}>
            <TextInput
              id={`puuid-${row ? row.id : "new"}`}
              labelText={intl.formatMessage({
                id: "admin.programs.basicInfo.uuid.label",
              })}
              value={questionnaireUUID}
              onChange={(e) => setQuestionnaireUUID(e.target.value)}
              placeholder={
                isNew
                  ? intl.formatMessage({
                      id: "admin.programs.basicInfo.uuid.placeholder",
                    })
                  : undefined
              }
              invalid={uuidInvalid}
              invalidText={intl.formatMessage({
                id: "admin.programs.basicInfo.uuid.invalid",
              })}
            />
          </Column>
          <Column lg={8} md={4} sm={2}>
            <ComboBox
              key={`units-${domain || "none"}`}
              id={`units-${row ? row.id : "new"}`}
              titleText={intl.formatMessage({
                id: "admin.programs.basicInfo.labUnits.label",
              })}
              helperText={
                domain
                  ? undefined
                  : intl.formatMessage({
                      id: "admin.programs.basicInfo.labUnits.helper.pickDomain",
                    })
              }
              disabled={!domain}
              items={availableLabUnits.map((s) => s.id)}
              itemToString={(id) =>
                availableLabUnits.find((s) => s.id === id)?.value || id || ""
              }
              selectedItem={labUnitId || null}
              onChange={({ selectedItem }) => setLabUnitId(selectedItem || "")}
            />
          </Column>
        </Grid>

        <div style={{ marginTop: "1rem" }}>
          <RadioButtonGroup
            legendText={intl.formatMessage({
              id: "admin.programs.basicInfo.domain.label",
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
              setLabUnitId("");
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
              <FormattedMessage id="admin.programs.basicInfo.domain.required" />
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
          })}
        />
        <Switch
          name="json"
          text={intl.formatMessage({
            id: "admin.programs.questionnaire.mode.json",
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
                      })}
                      value={q.text}
                      onChange={(e) =>
                        patchQuestion(q.key, { text: e.target.value })
                      }
                    />
                    <OverflowMenu
                      aria-label={intl.formatMessage({
                        id: "admin.programs.questionnaire.question.actions.aria",
                      })}
                    >
                      <OverflowMenuItem
                        isDelete
                        itemText={intl.formatMessage({
                          id: "admin.programs.questionnaire.question.actions.delete",
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
                    })}
                  </p>
                  {(q.type === "choice" || q.type === "checkbox") && (
                    <Stack gap={2} style={{ marginTop: 8 }}>
                      <span
                        style={{ fontSize: 12, textTransform: "uppercase" }}
                      >
                        <FormattedMessage id="admin.programs.questionnaire.answerOptions.section.title" />
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
                          <FormattedMessage id="admin.programs.questionnaire.answerOptions.empty" />
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
                        <FormattedMessage id="admin.programs.questionnaire.answerOptions.addOption" />
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
                  <FormattedMessage id="admin.programs.guidance.gui.emptyState.body" />
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
                  <FormattedMessage id="admin.programs.guidance.gui.emptyState.action" />
                ) : (
                  <FormattedMessage id="admin.programs.questionnaire.question.addNew" />
                )}
              </Button>
            </Stack>
          ) : (
            <Stack gap={3}>
              <TextArea
                id={`json-${row ? row.id : "new"}`}
                labelText={intl.formatMessage({
                  id: "admin.programs.questionnaire.mode.json",
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
                  <FormattedMessage id="admin.programs.questionnaire.json.validate" />
                </Button>
                <Button kind="ghost" size="sm" onClick={reformatJson}>
                  <FormattedMessage id="admin.programs.questionnaire.json.reformat" />
                </Button>
              </div>
              {jsonError && (
                <InlineNotification
                  kind="error"
                  lowContrast
                  title={intl.formatMessage({
                    id: "admin.programs.questionnaire.json.error.title",
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
                    },
                    { count: jsonValid },
                  )}
                />
              )}
              <Tile>
                <p style={{ fontSize: 12 }}>
                  <FormattedMessage id="admin.programs.guidance.json.referenceCard.format" />{" "}
                  <FormattedMessage
                    id="admin.programs.guidance.json.referenceCard.allowedTypes"
                    values={{ allowed: QUESTION_TYPES.join(", ") }}
                  />
                </p>
                <p
                  style={{
                    fontSize: 12,
                    color: "var(--cds-text-secondary)",
                    marginTop: 4,
                  }}
                >
                  <FormattedMessage id="admin.programs.guidance.json.referenceCard.advanced" />
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
            <FormattedMessage id="admin.programs.questionnaire.preview.label" />
          </p>
          <Tile>
            {questions.length === 0 ? (
              <p style={{ color: "var(--cds-text-secondary)" }}>
                <FormattedMessage id="admin.programs.questionnaire.preview.empty" />
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
                      })}
                  </label>
                  {q.type === "choice" && (
                    <Select id={`pv-${q.key}`} labelText="" disabled>
                      <SelectItem text={q.options[0] || "—"} />
                    </Select>
                  )}
                  {q.type === "boolean" && (
                    <span style={{ fontSize: 13 }}>
                      <FormattedMessage id="admin.programs.questionnaire.preview.boolean.example" />
                    </span>
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
                        placeholder={intl.formatMessage({
                          id: "admin.programs.questionnaire.preview.quantity.numberPlaceholder",
                        })}
                      />
                      <Select id={`pvu-${q.key}`} labelText="" disabled>
                        <SelectItem
                          text={intl.formatMessage({
                            id: "admin.programs.questionnaire.preview.quantity.unit",
                          })}
                        />
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
          <FormattedMessage id="label.button.submit" />
        </Button>
        <Button kind="secondary" onClick={onClose}>
          <FormattedMessage id="label.button.cancel" />
        </Button>
      </div>
    </Stack>
  );
}

export default ProgramManagementV2;
