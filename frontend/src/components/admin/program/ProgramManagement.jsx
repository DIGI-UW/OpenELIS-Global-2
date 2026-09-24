import React, {
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react";
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
import useDomains from "../../common/useDomains";
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
    label: "admin.programs.title",
    link: "/MasterListsPage/program",
  },
];

// FR-4 tag colours. The domain list itself is served by /rest/domains
// (useDomains) and labelled through the shared label.domain.* keys, so the
// Programs screen speaks the same Domain vocabulary as Tests and Lab Units.
const DOMAIN_TAG_COLOR = {
  CLINICAL: "blue",
  ENVIRONMENTAL: "green",
  VECTOR: "purple",
};
const FALLBACK_DOMAINS = ["CLINICAL", "ENVIRONMENTAL", "VECTOR"].map((id) => ({
  id,
  labelKey: `label.domain.${id}`,
}));
const DOMAIN_GUIDANCE_IDS = {
  CLINICAL: "admin.programs.guidance.domain.clinical",
  ENVIRONMENTAL: "admin.programs.guidance.domain.environmental",
  VECTOR: "admin.programs.guidance.domain.vector",
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
const EMPTY_QUESTIONNAIRE_JSON =
  '{\n  "resourceType": "Questionnaire",\n  "item": []\n}';
const SELECT_ALL_ID = "__select_all__";
// program.name and program.code are varchar(50) / varchar(20); without these
// the server answers 500 on an over-long value and the admin only sees a
// generic save error.
const NAME_MAX_LENGTH = 50;
const CODE_MAX_LENGTH = 20;

function toSummary(row) {
  return {
    id: String(row.id),
    name: row.name || "",
    code: row.code || "",
    domain: row.domain || "CLINICAL",
    labUnitIds: Array.isArray(row.labUnitIds) ? row.labUnitIds.map(String) : [],
    active: row.active !== false,
  };
}

function parseQuestionnaire(raw) {
  if (!raw) {
    return null;
  }
  try {
    return typeof raw === "string" ? JSON.parse(raw) : raw;
  } catch (_e) {
    return null;
  }
}

/**
 * FR-14: an answer option the Visual Builder can edit is a plain
 * `{valueString}`. Anything else — a coded option (`valueCoding`, which the
 * shipped Histopathology and Vector Field Survey questionnaires use) or a typed
 * value — is shown read-only and written back exactly as it came in, so editing
 * a neighbouring option can never rewrite it.
 */
function toOption(raw) {
  if (raw && typeof raw.valueString === "string") {
    return { value: raw.valueString, coded: false, raw };
  }
  const coding = raw?.valueCoding;
  const label = coding
    ? coding.display || coding.code || ""
    : String(
        Object.entries(raw || {})
          .filter(([key]) => key.startsWith("value"))
          .map(([, value]) => value)[0] ?? "",
      );
  return { value: label, coded: true, raw };
}

function fromOption(option) {
  return option.coded ? option.raw : { valueString: option.value };
}

function questionsFromParsed(parsed, keyPrefix) {
  if (!Array.isArray(parsed?.item)) {
    return [];
  }
  return parsed.item.map((it, i) => ({
    key: `${keyPrefix}-${i}`,
    linkId: it.linkId || `q${i + 1}`,
    text: it.text || "",
    type: it.type || "string",
    options: Array.isArray(it.answerOption)
      ? it.answerOption.map(toOption)
      : [],
  }));
}

function ProgramManagement() {
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);
  const intl = useIntl();
  const componentMounted = useRef(false);
  const fetchedDomains = useDomains();
  const domains = fetchedDomains.length > 0 ? fetchedDomains : FALLBACK_DOMAINS;
  const domainLabel = useCallback(
    (id) => {
      const match = domains.find((d) => d.id === id);
      return intl.formatMessage({
        id: match ? match.labelKey : `label.domain.${id}`,
      });
    },
    [domains, intl],
  );

  const [programSummaries, setProgramSummaries] = useState([]);
  const [programDetails, setProgramDetails] = useState({});
  const [labUnits, setLabUnits] = useState([]);
  const [query, setQuery] = useState("");
  const [domainFilter, setDomainFilter] = useState("all");
  const [showDeactivated, setShowDeactivated] = useState(false);
  const [expandedId, setExpandedId] = useState(null);
  const [adding, setAdding] = useState(false);
  const [confirmDeactivate, setConfirmDeactivate] = useState(null);
  const [deactivateOrderCount, setDeactivateOrderCount] = useState(null);

  const loadPrograms = useCallback(() => {
    getFromOpenElisServer("/rest/program-list", (list) => {
      if (!componentMounted.current) return;
      setProgramSummaries((Array.isArray(list) ? list : []).map(toSummary));
    });
  }, []);

  useEffect(() => {
    componentMounted.current = true;
    loadPrograms();
    getFromOpenElisServer("/rest/lab-units-management", (response) => {
      if (!componentMounted.current) return;
      const rows = Array.isArray(response?.data)
        ? response.data
        : Array.isArray(response)
          ? response
          : [];
      setLabUnits(
        rows.map((s) => ({
          id: String(s.id),
          name: s.name,
          domain: s.domain || "",
          active: s.isActive !== false,
        })),
      );
    });
    return () => {
      componentMounted.current = false;
    };
  }, [loadPrograms]);

  const labUnitName = useCallback(
    (id) => labUnits.find((s) => s.id === String(id))?.name || String(id),
    [labUnits],
  );

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
      const parsed = parseQuestionnaire(res.additionalOrderEntryQuestions);
      setProgramDetails((prev) => ({
        ...prev,
        [id]: {
          program: res.program || {},
          labUnitIds: Array.isArray(res.labUnitIds)
            ? res.labUnitIds.map(String)
            : res.testSectionId
              ? [String(res.testSectionId)]
              : [],
          domain: res.domain || "",
          active: res.active !== false,
          questions: questionsFromParsed(parsed, id),
          rawQuestionnaire: parsed
            ? JSON.stringify(parsed, null, 2)
            : EMPTY_QUESTIONNAIRE_JSON,
        },
      }));
    });
  };

  const forgetDetail = (id) =>
    setProgramDetails((prev) => {
      if (!prev[id]) return prev;
      const next = { ...prev };
      delete next[id];
      return next;
    });

  const handleExpand = (row) => {
    const next = expandedId === row.id ? null : row.id;
    setExpandedId(next);
    if (next) {
      setAdding(false);
      loadProgramDetail(next);
    }
  };

  const notify = (kind, messageId, values) => {
    setNotificationVisible(true);
    addNotification({
      kind,
      title: intl.formatMessage({ id: "notification.title" }),
      message: intl.formatMessage({ id: messageId }, values),
    });
  };

  const handleSaveResponse = (res, summary) => {
    if (!componentMounted.current) return;
    if (res && (res.status === 200 || res.status === "200")) {
      notify(NotificationKinds.success, "success.add.edited.msg");
      loadPrograms();
      if (summary?.id) forgetDetail(summary.id);
      setExpandedId(null);
      setAdding(false);
    } else {
      notify(NotificationKinds.error, "error.add.edited.msg");
    }
  };

  const saveProgram = (payload, summary) =>
    postToOpenElisServerFullResponse(
      "/rest/program",
      JSON.stringify(payload),
      (res) => handleSaveResponse(res, summary),
    );

  // Lifecycle is a field update on the existing save endpoint (FR-18). The
  // server keeps every field the payload does not name, so the flip only has
  // to carry the identity fields Program requires plus the new flag.
  const persistActiveFlag = (row, nextActive) => {
    const payload = {
      program: { id: row.id, programName: row.name, code: row.code },
      active: nextActive,
    };
    postToOpenElisServerFullResponse(
      "/rest/program",
      JSON.stringify(payload),
      (res) => {
        if (!componentMounted.current) return;
        if (res && (res.status === 200 || res.status === "200")) {
          setProgramSummaries((prev) =>
            prev.map((p) =>
              p.id === row.id ? { ...p, active: nextActive } : p,
            ),
          );
          notify(
            NotificationKinds.success,
            nextActive
              ? "admin.programs.reactivate.success"
              : "admin.programs.deactivate.success",
            { programName: row.name },
          );
        } else {
          notify(NotificationKinds.error, "error.add.edited.msg");
        }
      },
    );
  };

  const openDeactivate = (row) => {
    setConfirmDeactivate(row);
    setDeactivateOrderCount(null);
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

  const handleDeactivate = () => {
    const target = confirmDeactivate;
    if (!target) return;
    persistActiveFlag(target, false);
    closeDeactivate();
    if (expandedId === target.id) setExpandedId(null);
  };

  const handleReactivate = (row) => persistActiveFlag(row, true);

  const domainFilterItems = [
    {
      id: "all",
      label: intl.formatMessage({
        id: "admin.programs.list.filter.domain.all",
      }),
    },
    ...domains.map((d) => ({ id: d.id, label: domainLabel(d.id) })),
  ];

  const editorProps = {
    labUnits,
    domains,
    domainLabel,
    onDeactivate: openDeactivate,
    onReactivate: handleReactivate,
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
                    titleText={intl.formatMessage({
                      id: "admin.programs.list.filter.domain.label",
                    })}
                    hideLabel
                    size="lg"
                    label={intl.formatMessage({
                      id: "admin.programs.list.filter.domain.all",
                    })}
                    items={domainFilterItems}
                    itemToString={(item) => (item ? item.label : "")}
                    onChange={({ selectedItem }) =>
                      setDomainFilter(selectedItem?.id || "all")
                    }
                  />
                  <Toggle
                    id="show-deactivated"
                    size="sm"
                    hideLabel
                    labelText={intl.formatMessage({
                      id: "admin.programs.list.toggle.showDeactivated",
                    })}
                    labelA=""
                    labelB=""
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
                      ? intl.formatMessage({ id: "button.close" })
                      : intl.formatMessage({ id: "admin.programs.add" })}
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
                    {...editorProps}
                    onClose={() => setAdding(false)}
                    onSave={saveProgram}
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
                  {visiblePrograms.length === 0 && (
                    <TableRow>
                      <TableCell colSpan={6}>
                        <FormattedMessage id="admin.programs.list.empty" />
                      </TableCell>
                    </TableRow>
                  )}
                  {visiblePrograms.map((row) => {
                    const detail = programDetails[row.id];
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
                            <Tag type={DOMAIN_TAG_COLOR[row.domain] || "gray"}>
                              {domainLabel(row.domain)}
                            </Tag>
                          </TableCell>
                          <TableCell>
                            <StatusTag active={row.active} />
                          </TableCell>
                          <TableCell>
                            {row.labUnitIds.length > 0
                              ? row.labUnitIds.map(labUnitName).join(", ")
                              : "—"}
                          </TableCell>
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
                                {...editorProps}
                                onClose={() => setExpandedId(null)}
                                onSave={saveProgram}
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

function StatusTag({ active }) {
  return active ? (
    <Tag type="green">
      <FormattedMessage id="admin.programs.list.status.active" />
    </Tag>
  ) : (
    <Tag type="gray">
      <FormattedMessage id="admin.programs.list.status.inactive" />
    </Tag>
  );
}

function ProgramEditor({
  row,
  detail,
  isNew,
  labUnits,
  domains,
  domainLabel,
  onClose,
  onSave,
  onDeactivate,
  onReactivate,
}) {
  const intl = useIntl();
  const initialProgram = detail?.program || {};
  const persistedDomain = row?.domain || "";
  const idSuffix = row ? row.id : "new";

  const [programName, setProgramName] = useState(
    initialProgram.programName || "",
  );
  const [code, setCode] = useState(initialProgram.code || "");
  const [labUnitIds, setLabUnitIds] = useState(detail?.labUnitIds || []);
  const [domain, setDomain] = useState(persistedDomain);
  // FR-3: changing the Domain of a persisted program is staged behind a
  // confirmation. Bumping the radio key on cancel snaps the checked radio back.
  const [pendingDomain, setPendingDomain] = useState(null);
  const [domainRadioKey, setDomainRadioKey] = useState(0);
  const [showDomainGuidance, setShowDomainGuidance] = useState(false);

  const [mode, setMode] = useState(0);
  // Carbon's ContentSwitcher moves its highlight as soon as a segment is
  // clicked; when a JSON -> Visual switch is refused the highlight has to be
  // snapped back, which a remount keyed on this counter does.
  const [switcherKey, setSwitcherKey] = useState(0);
  const initialRawJson = detail?.rawQuestionnaire || EMPTY_QUESTIONNAIRE_JSON;
  const parseBaseline = (json) =>
    parseQuestionnaire(json) || { resourceType: "Questionnaire", item: [] };
  const [questions, setQuestions] = useState(detail?.questions || []);
  const [rawJson, setRawJson] = useState(initialRawJson);
  // Baseline preserves FHIR properties the Visual Builder does not expose
  // (extensions, enableWhen, definition, code) so a Visual-mode save does not
  // strip them from the persisted Questionnaire.
  const [baseline, setBaseline] = useState(() => parseBaseline(initialRawJson));
  const [jsonError, setJsonError] = useState(null);
  const [jsonValid, setJsonValid] = useState(null);

  const codePattern = /^[a-z0-9_ ]*$/i;
  const codeInvalid =
    !codePattern.test(code) || code.trim().length > CODE_MAX_LENGTH;
  const nameInvalid = programName.trim().length > NAME_MAX_LENGTH;
  const canSave =
    Boolean(domain) &&
    Boolean(programName.trim()) &&
    !nameInvalid &&
    !codeInvalid;

  const labUnitItems = useMemo(
    () => [
      {
        id: SELECT_ALL_ID,
        isSelectAll: true,
        label: intl.formatMessage({
          id: "admin.programs.basicInfo.labUnits.selectAll",
        }),
      },
      ...labUnits.map((unit) => ({
        id: unit.id,
        label: [
          unit.name,
          unit.domain ? domainLabel(unit.domain) : null,
          unit.active
            ? null
            : intl.formatMessage({
                id: "admin.programs.list.status.inactive",
              }),
        ]
          .filter(Boolean)
          .join(" · "),
      })),
    ],
    [labUnits, domainLabel, intl],
  );
  const selectedLabUnitItems = labUnitItems.filter((item) =>
    labUnitIds.includes(item.id),
  );

  const handleDomainChange = (nextDomain) => {
    if (!nextDomain || nextDomain === domain) return;
    if (!isNew && persistedDomain && nextDomain !== persistedDomain) {
      setPendingDomain(nextDomain);
      return;
    }
    setDomain(nextDomain);
  };
  const confirmDomainChange = () => {
    setDomain(pendingDomain);
    setPendingDomain(null);
  };
  const cancelDomainChange = () => {
    setPendingDomain(null);
    setDomainRadioKey((k) => k + 1);
  };

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
            { id: "admin.programs.questionnaire.json.error.itemShape" },
            { allowed: QUESTION_TYPES.join(", ") },
          ),
        );
      }
      if (seen.has(it.linkId)) {
        throw new Error(
          intl.formatMessage(
            { id: "admin.programs.questionnaire.json.error.duplicateLinkId" },
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
        const merged = {
          ...(baseItemsByLink.get(linkId) || {}),
          linkId,
          text: q.text,
          type: q.type,
        };
        if (
          (q.type === "choice" || q.type === "checkbox") &&
          q.options.length
        ) {
          merged.answerOption = q.options.map(fromOption);
        } else if (q.type !== "choice" && q.type !== "checkbox") {
          delete merged.answerOption;
        }
        return merged;
      }),
    };
  };

  const syncFromParsed = (parsed) => {
    setQuestions(questionsFromParsed(parsed, `sync-${Date.now()}`));
    setBaseline(parsed);
  };

  const handleSave = () => {
    let additionalOrderEntryQuestions;
    try {
      additionalOrderEntryQuestions =
        mode === 1 ? JSON.parse(rawJson) : buildFromQuestions();
      validateQuestionnaire(additionalOrderEntryQuestions);
    } catch (e) {
      setJsonError(e.message);
      setJsonValid(null);
      if (mode === 0) {
        try {
          setRawJson(JSON.stringify(buildFromQuestions(), null, 2));
        } catch (_e2) {
          /* keep the current rawJson */
        }
        setMode(1);
      }
      return;
    }
    if (mode === 1) {
      syncFromParsed(additionalOrderEntryQuestions);
    }
    const payload = {
      program: {
        id: initialProgram.id || "",
        programName: programName.trim(),
        code: code.trim(),
        ...(initialProgram.questionnaireUUID
          ? { questionnaireUUID: initialProgram.questionnaireUUID }
          : {}),
      },
      domain,
      active: isNew ? true : row.active !== false,
      labUnitIds,
      testSectionId: labUnitIds[0] || "",
      additionalOrderEntryQuestions,
    };
    onSave(payload, { id: initialProgram.id || row?.id, domain, labUnitIds });
  };

  const validateJson = () => {
    try {
      const parsed = JSON.parse(rawJson);
      validateQuestionnaire(parsed);
      setJsonError(null);
      setJsonValid(parsed.item.length);
      syncFromParsed(parsed);
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

  // Visual -> JSON always succeeds (serialize questions over the baseline).
  // JSON -> Visual only proceeds when the JSON parses and passes the shape
  // check; otherwise the error stays on screen and the JSON pane stays put.
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
      syncFromParsed(parsed);
      setJsonError(null);
      setJsonValid(parsed.item.length);
      setMode(0);
    } catch (e) {
      setJsonError(e.message);
      setJsonValid(null);
      setSwitcherKey((k) => k + 1);
    }
  };

  const addQuestion = () =>
    setQuestions((qs) => {
      const taken = new Set(qs.map((q) => q.linkId).filter(Boolean));
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
    });

  return (
    <Stack gap={5} style={{ padding: "0.5rem 0" }}>
      <Tile>
        <Grid>
          <Column lg={8} md={4} sm={2}>
            <TextInput
              id={`pname-${idSuffix}`}
              labelText={intl.formatMessage({
                id: "admin.programs.basicInfo.programName.label",
              })}
              value={programName}
              onChange={(e) => setProgramName(e.target.value)}
              maxLength={NAME_MAX_LENGTH}
              placeholder={
                isNew
                  ? intl.formatMessage({
                      id: "admin.programs.basicInfo.programName.placeholder",
                    })
                  : undefined
              }
              invalid={nameInvalid}
              invalidText={intl.formatMessage(
                { id: "admin.programs.basicInfo.programName.tooLong" },
                { max: NAME_MAX_LENGTH },
              )}
            />
          </Column>
          <Column lg={8} md={4} sm={2}>
            <TextInput
              id={`pcode-${idSuffix}`}
              labelText={intl.formatMessage({
                id: "admin.programs.basicInfo.code.label",
              })}
              value={code}
              onChange={(e) => setCode(e.target.value)}
              maxLength={CODE_MAX_LENGTH}
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
            <FilterableMultiSelect
              id={`units-${idSuffix}`}
              titleText={intl.formatMessage({
                id: "admin.programs.basicInfo.labUnits.label",
              })}
              placeholder={intl.formatMessage({
                id: "admin.programs.basicInfo.labUnits.placeholder",
              })}
              items={labUnitItems}
              itemToString={(item) => item?.label || ""}
              initialSelectedItems={selectedLabUnitItems}
              onChange={({ selectedItems }) =>
                setLabUnitIds(
                  (selectedItems || [])
                    .filter((item) => !item.isSelectAll)
                    .map((item) => item.id),
                )
              }
              selectionFeedback="top-after-reopen"
            />
          </Column>
        </Grid>

        <div style={{ marginTop: "1rem" }}>
          <RadioButtonGroup
            key={domainRadioKey}
            legendText={intl.formatMessage({
              id: "admin.programs.basicInfo.domain.label",
            })}
            helperText={intl.formatMessage({
              id: "admin.programs.basicInfo.domain.helper",
            })}
            name={`domain-${idSuffix}`}
            valueSelected={domain}
            onChange={handleDomainChange}
            orientation="vertical"
          >
            {domains.map((d) => (
              <RadioButton
                key={d.id}
                labelText={domainLabel(d.id)}
                value={d.id}
                id={`d${d.id.charAt(0).toLowerCase()}-${idSuffix}`}
              />
            ))}
          </RadioButtonGroup>
          {!domain && (
            <p style={{ color: "var(--cds-text-error)", fontSize: 12 }}>
              <FormattedMessage id="admin.programs.basicInfo.domain.required" />
            </p>
          )}
          <Button
            kind="ghost"
            size="sm"
            onClick={() => setShowDomainGuidance((s) => !s)}
          >
            <FormattedMessage
              id={
                showDomainGuidance
                  ? "admin.programs.guidance.domain.toggle.hide"
                  : "admin.programs.guidance.domain.toggle.show"
              }
            />
          </Button>
          {showDomainGuidance && (
            <ul style={{ fontSize: 12, color: "var(--cds-text-secondary)" }}>
              {domains
                .filter((d) => DOMAIN_GUIDANCE_IDS[d.id])
                .map((d) => (
                  <li key={d.id} style={{ marginBottom: 4 }}>
                    <strong>{domainLabel(d.id)}</strong>:{" "}
                    <FormattedMessage id={DOMAIN_GUIDANCE_IDS[d.id]} />
                  </li>
                ))}
            </ul>
          )}
        </div>

        {!isNew && (
          <div
            style={{
              display: "flex",
              alignItems: "center",
              flexWrap: "wrap",
              gap: "0.75rem",
              marginTop: "1rem",
            }}
          >
            <span style={{ fontSize: 12 }}>
              <FormattedMessage id="admin.programs.editor.status.label" />
            </span>
            <StatusTag active={row.active} />
            {row.active ? (
              <Button
                kind="danger--tertiary"
                size="sm"
                onClick={() => onDeactivate(row)}
              >
                <FormattedMessage id="admin.programs.action.deactivate" />
              </Button>
            ) : (
              <Button
                kind="tertiary"
                size="sm"
                onClick={() => onReactivate(row)}
              >
                <FormattedMessage id="admin.programs.action.reactivate" />
              </Button>
            )}
            <span style={{ fontSize: 12, color: "var(--cds-text-secondary)" }}>
              <FormattedMessage id="admin.programs.editor.status.noHardDelete" />
            </span>
          </div>
        )}

        {pendingDomain && (
          <Modal
            open
            size="sm"
            modalHeading={intl.formatMessage({
              id: "admin.programs.domain.confirm.heading",
            })}
            primaryButtonText={intl.formatMessage({
              id: "label.button.confirm",
            })}
            secondaryButtonText={intl.formatMessage({ id: "button.cancel" })}
            onRequestClose={cancelDomainChange}
            onRequestSubmit={confirmDomainChange}
          >
            <p>
              <FormattedMessage
                id="admin.programs.domain.confirm.body"
                values={{
                  from: persistedDomain ? domainLabel(persistedDomain) : "",
                  to: domainLabel(pendingDomain),
                }}
              />
            </p>
          </Modal>
        )}
      </Tile>

      <ContentSwitcher
        key={`mode-${mode}-${switcherKey}`}
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
                  {TYPE_EXAMPLE_IDS[q.type] && (
                    <p
                      style={{
                        fontSize: 12,
                        color: "var(--cds-text-secondary)",
                        marginTop: 4,
                      }}
                    >
                      {intl.formatMessage({ id: TYPE_EXAMPLE_IDS[q.type] })}
                    </p>
                  )}
                  {(q.type === "choice" || q.type === "checkbox") && (
                    <Stack gap={2} style={{ marginTop: 8 }}>
                      <span
                        style={{ fontSize: 12, textTransform: "uppercase" }}
                      >
                        <FormattedMessage id="admin.programs.questionnaire.answerOptions.section.title" />
                      </span>
                      {q.options.map((o, i) => (
                        <div
                          key={i}
                          style={{
                            display: "flex",
                            gap: 8,
                            alignItems: "center",
                          }}
                        >
                          <TextInput
                            id={`opt-${q.key}-${i}`}
                            size="sm"
                            labelText=""
                            value={o.value}
                            readOnly={o.coded}
                            onChange={(e) => {
                              if (o.coded) return;
                              const next = [...q.options];
                              next[i] = { ...o, value: e.target.value };
                              patchQuestion(q.key, { options: next });
                            }}
                          />
                          {o.coded ? (
                            <Tag type="cool-gray" size="sm">
                              <FormattedMessage id="admin.programs.questionnaire.answerOptions.coded" />
                            </Tag>
                          ) : (
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
                          )}
                        </div>
                      ))}
                      {q.options.some((o) => o.coded) && (
                        <p
                          style={{
                            fontSize: 12,
                            color: "var(--cds-text-secondary)",
                          }}
                        >
                          <FormattedMessage id="admin.programs.questionnaire.answerOptions.codedHelp" />
                        </p>
                      )}
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
                            options: [
                              ...q.options,
                              { value: "", coded: false, raw: null },
                            ],
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
              <Button kind="ghost" renderIcon={Add} onClick={addQuestion}>
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
                id={`json-${idSuffix}`}
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
                    { id: "admin.programs.questionnaire.json.validated" },
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
                    style={{ fontSize: 12, display: "block", marginBottom: 4 }}
                  >
                    {q.text ||
                      intl.formatMessage({
                        id: "admin.programs.questionnaire.preview.untitled",
                      })}
                  </label>
                  {q.type === "choice" && (
                    <Select id={`pv-${q.key}`} labelText="" disabled>
                      <SelectItem
                        value={q.options[0]?.value || ""}
                        text={q.options[0]?.value || "—"}
                      />
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
                          value="unit"
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
                        ☐ {o.value}
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

export default ProgramManagement;
