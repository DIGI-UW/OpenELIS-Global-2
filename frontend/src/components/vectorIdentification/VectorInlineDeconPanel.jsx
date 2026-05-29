import React, { useContext, useEffect, useState } from "react";
import {
  Accordion,
  AccordionItem,
  Button,
  InlineNotification,
  Modal,
  RadioButton,
  RadioButtonGroup,
  Select,
  SelectItem,
  Tag,
  TextArea,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { NotificationContext } from "../layout/Layout";
import { NotificationKinds } from "../common/CustomNotification";
import {
  VectorDeconvolutionAPI,
  VectorSamplingSiteAPI,
} from "./VectorIdentificationService";

const STRATEGIES = [
  {
    id: "random",
    titleKey: "vectorDec.strategy.random.title",
    hintKey: "vectorDec.strategy.random.hint",
  },
  {
    id: "species",
    titleKey: "vectorDec.strategy.species.title",
    hintKey: "vectorDec.strategy.species.hint",
  },
  {
    id: "manual",
    titleKey: "vectorDec.strategy.manual.title",
    hintKey: "vectorDec.strategy.manual.hint",
  },
];

const VectorInlineDeconPanel = ({
  specimens = [],
  accessionNumber,
  vectorPoolId,
  parentExternalId,
  positiveTest,
  speciesById = {},
  onClose,
  onInitiated,
}) => {
  const intl = useIntl();
  const { setNotificationVisible, addNotification } =
    useContext(NotificationContext);
  const parentQty = specimens.length || 0;

  const [phase, setPhase] = useState("setup"); // 'setup' | 'preview'
  const specimenCount = specimens.length;
  const [poolCount, setPoolCount] = useState(
    Math.max(2, Math.min(5, Math.floor(specimenCount / 2) || 2)),
  );
  const [strategy, setStrategy] = useState("random");
  const [assignment, setAssignment] = useState({}); // specimenId → poolId (0 = unassigned)
  const [dragOver, setDragOver] = useState(null); // poolId currently hovered as drop target
  const [draggedId, setDraggedId] = useState(null); // specimenId actively being dragged
  const [notes, setNotes] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const specimensWithSpecies = specimens.filter(
    (sp) => sp.vectorSpeciesId,
  ).length;
  const speciesStrategyAvailable = specimensWithSpecies > 0;
  const [samplingSites, setSamplingSites] = useState([]);
  const [subPoolLocationIds, setSubPoolLocationIds] = useState({});
  const [subPoolNotes, setSubPoolNotes] = useState({});
  const [reflexPreview, setReflexPreview] = useState(null);

  useEffect(() => {
    VectorSamplingSiteAPI.getAll()
      .then((data) => setSamplingSites(Array.isArray(data) ? data : []))
      .catch(() => setSamplingSites([]));
  }, []);

  useEffect(() => {
    if (!vectorPoolId) {
      setReflexPreview(null);
      return;
    }
    VectorDeconvolutionAPI.previewReflexes(vectorPoolId)
      .then((data) => setReflexPreview(data || null))
      .catch(() => setReflexPreview(null));
  }, [vectorPoolId]);
  const safePoolCount = Math.max(1, poolCount);
  const organismsPerPool = Math.max(
    1,
    Math.floor(specimenCount / safePoolCount),
  );
  const remainder = specimenCount > 0 ? specimenCount % safePoolCount : 0;
  const total = specimenCount;
  const exceedsParent = parentQty > 0 && total > parentQty;
  const isIndividualMode = organismsPerPool <= 1;

  const speciesName = (sid) => {
    if (!sid) return null;
    const sp = speciesById[sid];
    if (!sp) return null;
    return [sp.genus, sp.species].filter(Boolean).join(" ") || null;
  };

  // Distinct sample types in the source specimens. Used by the floor rule
  // (Random needs at least one pool per type) and by Manual UI hints.
  const distinctSampleTypes = (() => {
    const seen = new Map();
    specimens.forEach((sp) => {
      if (sp.typeOfSampleId && !seen.has(sp.typeOfSampleId)) {
        seen.set(sp.typeOfSampleId, sp.typeOfSampleName || "—");
      }
    });
    return Array.from(seen.entries()).map(([id, name]) => ({ id, name }));
  })();
  const distinctTypeCount = distinctSampleTypes.length;

  const applyStrategy = () => {
    // Floor rule: Random must produce sample-type-homogeneous pools, so a
    // mixed-type lot needs at least one pool per type. Reject early with a
    // clear message instead of silently producing illegal groupings.
    if (
      strategy === "random" &&
      distinctTypeCount > 1 &&
      poolCount < distinctTypeCount
    ) {
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage(
          {
            id: "vectorDec.error.poolFloorByType",
            defaultMessage:
              "Lot has {types} sample types ({names}) — minimum {types} pools required for Random distribution.",
          },
          {
            types: distinctTypeCount,
            names: distinctSampleTypes.map((t) => t.name).join(", "),
          },
        ),
      });
      setNotificationVisible(true);
      return;
    }

    const a = {};
    if (strategy === "random") {
      // Group by sample type, allocate pool indices proportionally so each
      // pool stays homogeneous. min 1 pool per type; remaining pools split by
      // specimen-count share (largest-remainder method).
      const groups = new Map();
      specimens.forEach((sp) => {
        const key = sp.typeOfSampleId || "untyped";
        if (!groups.has(key)) groups.set(key, []);
        groups.get(key).push(sp);
      });
      const groupEntries = Array.from(groups.entries());
      const totalSpecimens = specimens.length;
      const initialAlloc = groupEntries.map(([, list]) => {
        const proportional = (list.length / totalSpecimens) * poolCount;
        return { floor: Math.floor(proportional), remainder: proportional % 1 };
      });
      const allocations = initialAlloc.map((x) => Math.max(1, x.floor));
      let used = allocations.reduce((s, n) => s + n, 0);
      // Walk by largest remainder to distribute the leftover pool budget;
      // shrink over-budgeted groups too if the min-1 floor blew the count.
      while (used < poolCount) {
        let bestIdx = 0;
        let bestRem = -1;
        initialAlloc.forEach((x, i) => {
          if (x.remainder > bestRem) {
            bestRem = x.remainder;
            bestIdx = i;
          }
        });
        allocations[bestIdx] += 1;
        initialAlloc[bestIdx].remainder = -1; // don't pick the same twice
        used += 1;
      }
      while (used > poolCount) {
        let victim = -1;
        for (let i = 0; i < allocations.length; i++) {
          if (allocations[i] > 1) {
            if (victim === -1 || allocations[i] > allocations[victim])
              victim = i;
          }
        }
        if (victim === -1) break; // every group is at min 1; can't shrink
        allocations[victim] -= 1;
        used -= 1;
      }
      let nextPid = 1;
      groupEntries.forEach(([, list], gIdx) => {
        const slots = allocations[gIdx];
        list.forEach((sp, i) => {
          a[sp.sampleItemId] = nextPid + (i % slots);
        });
        nextPid += slots;
      });
    } else if (strategy === "species") {
      // Group specimens by species; spread each group round-robin across pools
      // so that large "Unknown" groups don't all land in one pool.
      const groups = {};
      specimens.forEach((sp) => {
        const k = sp.vectorSpeciesId || "Unknown";
        if (!groups[k]) groups[k] = [];
        groups[k].push(sp.sampleItemId);
      });
      let nextPid = 1;
      Object.values(groups).forEach((ids) => {
        ids.forEach((id) => {
          a[id] = nextPid;
          nextPid = (nextPid % poolCount) + 1;
        });
      });
    } else {
      specimens.forEach((sp) => {
        a[sp.sampleItemId] = 0;
      });
    }
    setAssignment(a);
    setPhase("preview");
  };

  // Pool homogeneity rules: a sub-pool can only contain organisms of a single
  // sample type. specimensByPool gives a quick lookup of what is currently in
  // each pool, and poolSampleType(pid) returns the (single) sample type if the
  // pool is occupied — null when empty (any type can drop in).
  const specimensById = (() => {
    const m = {};
    specimens.forEach((sp) => {
      m[sp.sampleItemId] = sp;
    });
    return m;
  })();
  const poolSampleType = (pid) => {
    if (!pid) return null;
    for (const [spId, assignedPid] of Object.entries(assignment)) {
      if (assignedPid !== pid) continue;
      const sp = specimensById[spId];
      if (sp && sp.typeOfSampleId) {
        return {
          id: sp.typeOfSampleId,
          name: sp.typeOfSampleName || "—",
        };
      }
    }
    return null;
  };
  const canDropInto = (spId, targetPid) => {
    if (!targetPid) return true; // unassigned zone — always allowed
    const sp = specimensById[spId];
    if (!sp || !sp.typeOfSampleId) return true; // no type info → permit
    const t = poolSampleType(targetPid);
    if (!t) return true; // pool empty
    return t.id === sp.typeOfSampleId;
  };

  const onDragStart = (e, spId) => {
    e.dataTransfer.setData("spId", String(spId));
    e.dataTransfer.effectAllowed = "move";
    setDraggedId(spId);
  };
  const onDragOver = (e, pid) => {
    // Refuse the drop visually when the candidate would violate type
    // homogeneity — browser shows the "not-allowed" cursor and onDrop won't
    // fire because preventDefault is skipped on the dragover.
    if (draggedId != null && !canDropInto(draggedId, pid)) {
      e.dataTransfer.dropEffect = "none";
      return;
    }
    e.preventDefault();
    e.dataTransfer.dropEffect = "move";
    setDragOver(pid);
  };
  const onDrop = (e, targetPid) => {
    e.preventDefault();
    const id = Number(e.dataTransfer.getData("spId"));
    if (!canDropInto(id, targetPid)) {
      setDragOver(null);
      setDraggedId(null);
      return;
    }
    setAssignment((prev) => ({ ...prev, [id]: targetPid }));
    setDragOver(null);
    setDraggedId(null);
  };
  const onDragLeave = () => setDragOver(null);
  const onDragEnd = () => {
    setDragOver(null);
    setDraggedId(null);
  };

  const cycleAssignment = (spId) => {
    if (strategy !== "manual") return;
    setAssignment((prev) => {
      const current = prev[spId] || 0; // 0 = unassigned
      const sp = specimensById[spId];
      // Cycle forward but skip pools whose type already conflicts. Unassigned
      // (0) is always available as a "release" stop.
      for (let step = 1; step <= poolCount + 1; step++) {
        const candidate = (current + step) % (poolCount + 1); // 0..poolCount
        if (candidate === 0) return { ...prev, [spId]: 0 };
        if (!sp || !sp.typeOfSampleId) return { ...prev, [spId]: candidate };
        const occupiedBy = (() => {
          for (const [otherId, otherPid] of Object.entries(prev)) {
            if (otherPid !== candidate) continue;
            if (Number(otherId) === spId) continue;
            const other = specimensById[otherId];
            if (other && other.typeOfSampleId) return other.typeOfSampleId;
          }
          return null;
        })();
        if (occupiedBy == null || occupiedBy === sp.typeOfSampleId) {
          return { ...prev, [spId]: candidate };
        }
      }
      return prev;
    });
  };

  const removePool = (pidToRemove) => {
    if (poolCount <= 2) return; // can't go below the minimum
    setAssignment((prev) => {
      const next = {};
      Object.entries(prev).forEach(([k, v]) => {
        if (v === pidToRemove) next[k] = 0;
        else if (v > pidToRemove) next[k] = v - 1;
        else next[k] = v;
      });
      return next;
    });
    const shiftMap = (m) => {
      const out = {};
      Object.entries(m || {}).forEach(([k, v]) => {
        const idx = Number(k);
        if (idx === pidToRemove) return; // drop the removed pool's entry
        out[idx > pidToRemove ? idx - 1 : idx] = v;
      });
      return out;
    };
    setSubPoolLocationIds((prev) => shiftMap(prev));
    setSubPoolNotes((prev) => shiftMap(prev));
    setPoolCount((c) => Math.max(2, c - 1));
  };

  const addPool = () => {
    setPoolCount((c) => c + 1);
  };

  const unassigned = specimens.filter(
    (sp) => !assignment[sp.sampleItemId] || assignment[sp.sampleItemId] === 0,
  );
  const assignedCount = specimens.length - unassigned.length;

  const handleInitiate = () => {
    if (!vectorPoolId) {
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "vectorDec.error.noParent" }),
      });
      setNotificationVisible(true);
      return;
    }
    if (poolCount < 2) {
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "vectorDec.error.minPoolCount" }),
      });
      setNotificationVisible(true);
      return;
    }

    const assignedCount = specimens.length - unassigned.length;
    if (strategy === "manual" && assignedCount === 0) {
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({
          id: "vectorDec.error.noAssignedSpecimens",
          defaultMessage:
            "Drag at least one specimen into a pool before saving.",
        }),
      });
      setNotificationVisible(true);
      return;
    }
    // Only send non-empty maps so the backend can short-circuit the no-override case.
    const hasLocOverride = Object.values(subPoolLocationIds).some((v) => v);
    const hasNoteOverride = Object.values(subPoolNotes).some(
      (v) => v && v.trim(),
    );
    // Send the actual specimen → sub-pool assignment so the backend honors
    // the Preview Grouping the tech saw. Filter out unassigned (0) entries.
    // When no assignment has been previewed (e.g. user clicked Save without
    // Preview), send nothing and let the backend fall back to sortOrder
    // slicing.
    const assignedOnly = {};
    Object.entries(assignment).forEach(([k, v]) => {
      if (v && v > 0) assignedOnly[k] = v;
    });
    const hasAssignments = Object.keys(assignedOnly).length > 0;
    const requestBody = {
      vectorPoolId,
      poolCount,
      organismsPerPool,
      notes: notes || null,
      subPoolLocationIds: hasLocOverride ? subPoolLocationIds : null,
      subPoolNotes: hasNoteOverride ? subPoolNotes : null,
      memberAssignments: hasAssignments ? assignedOnly : null,
      assignmentStrategy: strategy,
    };
    setSubmitting(true);
    VectorDeconvolutionAPI.initiate(requestBody)
      .then((result) => {
        const externalIds = Array.isArray(result?.childExternalIds)
          ? result.childExternalIds
          : [];
        const labRange =
          externalIds.length === 0
            ? ""
            : externalIds.length === 1
              ? externalIds[0]
              : `${externalIds[0]}–${externalIds[externalIds.length - 1]}`;
        const messageKey = isIndividualMode
          ? "vectorDec.message.successIndividual"
          : "vectorDec.message.success";
        addNotification({
          kind: NotificationKinds.success,
          title: intl.formatMessage({ id: "notification.title" }),
          message: intl.formatMessage(
            { id: messageKey },
            {
              count: result?.aliquotCount ?? poolCount,
              orders: result?.testOrdersCreated ?? 0,
              labRange,
            },
          ),
        });
        setNotificationVisible(true);
        if (onInitiated) onInitiated(result);
      })
      .catch((err) => {
        addNotification({
          kind: NotificationKinds.error,
          title: intl.formatMessage({ id: "notification.title" }),
          message:
            err.message ||
            intl.formatMessage({ id: "vectorDec.error.initiateFailed" }),
        });
        setNotificationVisible(true);
      })
      .finally(() => setSubmitting(false));
  };
  const poolLabel = (pid) => {
    if (strategy !== "species") {
      return intl.formatMessage({ id: "vectorDec.preview.poolN" }, { n: pid });
    }
    const inPool = specimens.filter(
      (sp) => assignment[sp.sampleItemId] === pid,
    );
    const unique = [
      ...new Set(inPool.map((sp) => sp.vectorSpeciesId).filter(Boolean)),
    ];
    if (unique.length === 1) {
      const name = speciesName(unique[0]);
      if (name) {
        return intl.formatMessage(
          { id: "vectorDec.preview.poolWithSpecies" },
          { n: pid, species: name },
        );
      }
    }
    return intl.formatMessage({ id: "vectorDec.preview.poolN" }, { n: pid });
  };

  const SetupPhase = (
    <>
      {/* Row 1: How many pools? + status text + INDIVIDUAL MODE badge | Split to individuals link */}
      <div
        style={{
          display: "flex",
          alignItems: "center",
          gap: "1rem",
          marginBottom: "1rem",
        }}
      >
        <div style={{ flex: "0 0 auto" }}>
          <label
            htmlFor="decon-poolCount"
            style={{
              fontSize: "0.75rem",
              fontWeight: 600,
              display: "block",
              marginBottom: 4,
            }}
          >
            <FormattedMessage id="vectorDec.field.howManyPools" />
          </label>
          <input
            id="decon-poolCount"
            type="number"
            min={2}
            max={Math.max(2, specimenCount)}
            value={poolCount}
            onChange={(e) =>
              setPoolCount(Math.max(2, parseInt(e.target.value, 10) || 2))
            }
            style={{
              width: 80,
              height: 32,
              padding: "0 0.5rem",
              border: "1px solid #8d8d8d",
              fontSize: "0.875rem",
              fontFamily: "inherit",
              color: "var(--cds-text-primary, #161616)",
            }}
          />
        </div>

        <div
          style={{
            flex: 1,
            fontSize: "0.75rem",
            color: "var(--cds-text-secondary, #525252)",
          }}
        >
          <FormattedMessage
            id="vectorDec.setup.distribution"
            values={{
              specimens: specimenCount,
              perPool: organismsPerPool,
              remainder,
            }}
          />
          {isIndividualMode && (
            <span
              style={{
                display: "inline-block",
                marginLeft: "0.5rem",
                padding: "2px 8px",
                background: "var(--cds-purple-60, #a56eff)",
                color: "var(--cds-background, #fff)",
                borderRadius: 10,
                fontSize: 10,
                fontWeight: 600,
                letterSpacing: 0.3,
                textTransform: "uppercase",
              }}
            >
              <FormattedMessage id="vectorDec.individualMode.badge" />
            </span>
          )}
        </div>

        {specimenCount > 1 && !isIndividualMode && (
          <button
            type="button"
            onClick={() => setPoolCount(specimenCount)}
            title={intl.formatMessage({
              id: "vectorDec.button.splitIndividuals.tooltip",
            })}
            style={{
              background: "transparent",
              border: "none",
              color: "var(--cds-link-primary, #0f62fe)",
              fontSize: "0.75rem",
              cursor: "pointer",
              fontFamily: "inherit",
              padding: 0,
            }}
          >
            <FormattedMessage id="vectorDec.button.splitIndividuals" />
          </button>
        )}
      </div>

      {/* Row 2-5: Assignment method radios with two-line labels */}
      <div style={{ marginBottom: "1rem" }}>
        <div
          style={{
            fontSize: "0.75rem",
            fontWeight: 600,
            marginBottom: "0.5rem",
          }}
        >
          <FormattedMessage id="vectorDec.field.strategy" />:
        </div>
        <RadioButtonGroup
          name="decon-strategy"
          orientation="vertical"
          valueSelected={strategy}
          onChange={(value) => {
            setStrategy(value);
            // Reset assignment when switching strategies — stale state from a
            // previously-applied strategy would otherwise leak into the new
            // preview. For Manual, jump straight into the assignment UI
            // (pools + unassigned zone) so the tech can start dragging or
            // clicking immediately — Preview Grouping doesn't make sense for
            // a strategy where there's nothing pre-computed to preview.
            if (value === "manual") {
              const blank = {};
              specimens.forEach((sp) => {
                blank[sp.sampleItemId] = 0;
              });
              setAssignment(blank);
              setPhase("preview");
            } else {
              setAssignment({});
              setPhase("setup");
            }
          }}
        >
          {STRATEGIES.map((s) => (
            <RadioButton
              key={s.id}
              id={`strategy-${s.id}`}
              value={s.id}
              labelText={
                // Carbon wraps RadioButton.labelText in a flex/inline label
                // that collapses children onto one line — using
                // display:block on a child gets ignored. Force a column flex
                // here so the hint lands on its own line.
                <span
                  style={{
                    display: "inline-flex",
                    flexDirection: "column",
                    gap: 2,
                    verticalAlign: "top",
                  }}
                >
                  <span style={{ fontWeight: 600 }}>
                    <FormattedMessage id={s.titleKey} />
                  </span>
                  <span
                    style={{
                      fontSize: "0.6875rem",
                      color: "var(--cds-text-secondary, #525252)",
                      fontWeight: 400,
                    }}
                  >
                    <FormattedMessage id={s.hintKey} />
                  </span>
                  {s.id === "species" && !speciesStrategyAvailable && (
                    <span
                      style={{
                        fontSize: "0.6875rem",
                        color: "var(--cds-text-helper, #6f6f6f)",
                        fontWeight: 400,
                        fontStyle: "italic",
                      }}
                    >
                      <FormattedMessage
                        id="vectorDec.strategy.species.hint"
                        defaultMessage="No specimens identified yet — unidentified specimens will be distributed evenly across pools."
                      />
                    </span>
                  )}
                </span>
              }
            />
          ))}
        </RadioButtonGroup>
      </div>

      {/* Live reflex preview — what will fire on save. */}
      <div
        style={{
          background: "var(--cds-blue-10, #edf5ff)",
          border: "1px solid #78a9ff",
          padding: "0.6rem 0.85rem",
          marginTop: "0.75rem",
        }}
      >
        <div
          style={{
            display: "flex",
            alignItems: "center",
            gap: 6,
            marginBottom: 6,
          }}
        >
          <span
            style={{
              fontSize: "0.8125rem",
              fontWeight: 600,
              color: "var(--cds-link-primary-hover, #0043ce)",
            }}
          >
            <FormattedMessage id="vectorDec.reflexPreview.heading" />
          </span>
          <span
            style={{
              padding: "1px 6px",
              background: "var(--cds-link-primary, #0f62fe)",
              color: "var(--cds-background, #fff)",
              borderRadius: 8,
              fontSize: 9,
              fontWeight: 600,
            }}
          >
            <FormattedMessage id="vectorDec.reflexPreview.eagerTag" />
          </span>
        </div>
        <ul
          style={{
            marginTop: 0,
            marginBottom: 0,
            paddingLeft: "1.125rem",
            fontSize: "0.75rem",
            color: "var(--cds-text-primary, #393939)",
            lineHeight: 1.5,
          }}
        >
          {/* Real backend preview when available — otherwise fall back to a
              generic copied-tests bullet so the panel doesn't look empty pre-
              fetch or when the pool has no analyses yet. */}
          {(() => {
            if (reflexPreview && reflexPreview.copiedTests?.length > 0) {
              return reflexPreview.copiedTests.map((entry, idx) => {
                const testName =
                  typeof entry === "string" ? entry : entry.testName;
                const ruleLabel =
                  typeof entry === "string" ? null : entry.ruleLabel;
                return (
                  <li key={`copied-${idx}-${testName}`}>
                    <span
                      style={{
                        padding: "1px 6px",
                        background: ruleLabel
                          ? "var(--cds-purple-60, #a56eff)"
                          : "var(--cds-border-subtle-01, #e0e0e0)",
                        color: ruleLabel
                          ? "var(--cds-background, #fff)"
                          : "var(--cds-text-primary, #161616)",
                        borderRadius: 8,
                        fontSize: 9,
                        fontWeight: 600,
                        marginRight: 4,
                      }}
                    >
                      {ruleLabel || (
                        <FormattedMessage id="vectorDec.reflexPreview.copiedTag" />
                      )}
                    </span>
                    {testName}
                  </li>
                );
              });
            }
            return (
              <li>
                <span
                  style={{
                    padding: "1px 6px",
                    background: "var(--cds-border-subtle-01, #e0e0e0)",
                    color: "var(--cds-text-primary, #161616)",
                    borderRadius: 8,
                    fontSize: 9,
                    fontWeight: 600,
                    marginRight: 4,
                  }}
                >
                  <FormattedMessage id="vectorDec.reflexPreview.copiedTag" />
                </span>
                <FormattedMessage
                  id="vectorDec.reflexPreview.parentTests"
                  values={{
                    test:
                      positiveTest ||
                      intl.formatMessage({
                        id: "vectorDec.reflexPreview.screeningPanel",
                      }),
                  }}
                />
              </li>
            );
          })()}
          {reflexPreview &&
            reflexPreview.reflexTests?.map((r, i) => (
              <li key={`reflex-${r.ruleLabel}-${r.testName}-${i}`}>
                <span
                  style={{
                    padding: "1px 6px",
                    background: "var(--cds-purple-60, #a56eff)",
                    color: "var(--cds-background, #fff)",
                    borderRadius: 8,
                    fontSize: 9,
                    fontWeight: 600,
                    marginRight: 4,
                  }}
                >
                  {r.ruleLabel}
                </span>
                {r.testName}
              </li>
            ))}
        </ul>
        {reflexPreview?.individualOnlyRuleLabels?.length > 0 &&
          !isIndividualMode && (
            <div
              style={{
                marginTop: 6,
                fontSize: "0.75rem",
                color: "var(--cds-text-secondary, #525252)",
              }}
            >
              <FormattedMessage
                id="vectorDec.reflexPreview.tipSplitIndividuals"
                values={{
                  rules: reflexPreview.individualOnlyRuleLabels.join(", "),
                }}
              />
            </div>
          )}
      </div>

      {exceedsParent && (
        <InlineNotification
          kind="warning"
          title={intl.formatMessage({
            id: "vectorDec.warning.exceedsParent.title",
          })}
          subtitle={intl.formatMessage(
            { id: "vectorDec.warning.exceedsParent.body" },
            { total, parent: parentQty },
          )}
          lowContrast
          hideCloseButton
          style={{ marginTop: "0.75rem", maxWidth: "100%" }}
        />
      )}

      {/* Optional per-sub-pool collection-site override; otherwise inherits the intake site. */}
      {samplingSites.length > 0 && poolCount >= 2 && (
        <Accordion style={{ marginTop: "1rem" }}>
          <AccordionItem
            title={intl.formatMessage({
              id: "vectorDec.locationOverride.title",
            })}
          >
            <p
              style={{
                fontSize: "0.75rem",
                color: "var(--cds-text-secondary, #525252)",
                marginBottom: "0.75rem",
              }}
            >
              <FormattedMessage id="vectorDec.locationOverride.help" />
            </p>
            {Array.from({ length: poolCount }, (_, i) => i + 1).map((pid) => (
              <div
                key={`override-${pid}`}
                style={{
                  marginBottom: "0.75rem",
                  paddingBottom: "0.75rem",
                  borderBottom:
                    pid < poolCount ? "1px solid #e0e0e0" : undefined,
                }}
              >
                <div
                  style={{
                    fontSize: "0.75rem",
                    fontWeight: 600,
                    marginBottom: "0.25rem",
                  }}
                >
                  {poolLabel(pid)}
                </div>
                <Select
                  id={`override-loc-${pid}`}
                  labelText={
                    <FormattedMessage id="vectorDec.locationOverride.site" />
                  }
                  value={subPoolLocationIds[pid] || ""}
                  onChange={(e) =>
                    setSubPoolLocationIds((prev) => ({
                      ...prev,
                      [pid]: e.target.value ? Number(e.target.value) : null,
                    }))
                  }
                >
                  <SelectItem
                    value=""
                    text={intl.formatMessage({
                      id: "vectorDec.locationOverride.inherit",
                    })}
                  />
                  {samplingSites.map((s) => (
                    <SelectItem key={s.id} value={s.id} text={s.name} />
                  ))}
                </Select>
                <TextArea
                  id={`override-notes-${pid}`}
                  labelText={
                    <FormattedMessage id="vectorDec.locationOverride.notes" />
                  }
                  value={subPoolNotes[pid] || ""}
                  onChange={(e) =>
                    setSubPoolNotes((prev) => ({
                      ...prev,
                      [pid]: e.target.value,
                    }))
                  }
                  maxCount={500}
                  enableCounter
                  rows={2}
                  style={{ marginTop: "0.5rem" }}
                />
              </div>
            ))}
          </AccordionItem>
        </Accordion>
      )}

      <div style={{ marginTop: "1rem" }}>
        <Button kind="primary" onClick={applyStrategy}>
          <FormattedMessage id="vectorDec.button.previewGrouping" /> →
        </Button>
      </div>
    </>
  );

  const PreviewPhase = (
    <>
      <InlineNotification
        kind="info"
        title={intl.formatMessage({ id: "vectorDec.preview.title" })}
        subtitle={intl.formatMessage(
          { id: "vectorDec.preview.body" },
          {
            count: poolCount,
            strategy: intl.formatMessage({
              id:
                STRATEGIES.find((s) => s.id === strategy)?.titleKey ||
                "vectorDec.strategy.random.title",
            }),
          },
        )}
        lowContrast
        hideCloseButton
        style={{ maxWidth: "100%", marginBottom: "0.75rem" }}
      />

      <div style={{ display: "flex", flexDirection: "column", gap: "0.5rem" }}>
        {Array.from({ length: poolCount }, (_, i) => i + 1).map((pid) => {
          const inPool = specimens.filter(
            (sp) => assignment[sp.sampleItemId] === pid,
          );
          const isHover = dragOver === pid;
          const isDropTarget = draggedId != null;
          return (
            <div
              key={pid}
              onDragOver={(e) => onDragOver(e, pid)}
              onDrop={(e) => onDrop(e, pid)}
              onDragLeave={onDragLeave}
              style={{
                // Three visual states:
                // 1. Directly hovered while dragging — strong blue dashed
                //    border + pale-blue background + ring shadow
                // 2. Drag in progress, not directly hovered — pale-blue
                //    background tint to advertise "you can drop here"
                // 3. Idle — neutral border, white background
                background: isHover
                  ? "var(--cds-blue-10, #edf5ff)"
                  : isDropTarget
                    ? "var(--cds-layer-01, #f4f7fb)"
                    : "var(--cds-background, #fff)",
                border: `2px ${isHover ? "dashed #0f62fe" : isDropTarget ? "dashed #a6c8ff" : "solid #e0e0e0"}`,
                boxShadow: isHover ? "0 0 0 3px rgba(15,98,254,0.15)" : "none",
                overflow: "hidden",
                transition:
                  "background 0.12s, border-color 0.12s, box-shadow 0.12s",
              }}
            >
              <div
                style={{
                  background: "var(--cds-layer-01, #f4f4f4)",
                  padding: "0.5rem 0.9rem",
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "space-between",
                  borderBottom: "1px solid #e0e0e0",
                }}
              >
                <div>
                  <strong style={{ fontSize: "0.8125rem" }}>
                    {poolLabel(pid)}
                  </strong>
                  {(() => {
                    const t = poolSampleType(pid);
                    if (!t) return null;
                    return (
                      <Tag
                        type="cyan"
                        size="sm"
                        style={{
                          marginLeft: 8,
                          verticalAlign: "middle",
                        }}
                      >
                        {t.name}
                      </Tag>
                    );
                  })()}
                </div>
                <div
                  style={{
                    display: "flex",
                    alignItems: "center",
                    gap: 8,
                  }}
                >
                  <span
                    style={{
                      fontSize: "0.75rem",
                      color: "var(--cds-text-secondary, #525252)",
                    }}
                  >
                    <FormattedMessage
                      id="vectorDec.preview.specimenCount"
                      values={{ count: inPool.length }}
                    />
                  </span>
                  {strategy === "manual" && poolCount > 2 && (
                    <button
                      type="button"
                      onClick={() => removePool(pid)}
                      title={intl.formatMessage({
                        id: "vectorDec.pool.remove",
                        defaultMessage:
                          "Remove this pool — its specimens go back to unassigned, later pools renumber",
                      })}
                      style={{
                        background: "transparent",
                        border: "1px solid #c6c6c6",
                        borderRadius: 2,
                        cursor: "pointer",
                        padding: "0 6px",
                        fontSize: "0.8125rem",
                        lineHeight: 1.4,
                        color: "var(--cds-text-secondary, #525252)",
                      }}
                      onMouseEnter={(e) => {
                        e.currentTarget.style.background =
                          "var(--cds-red-10, #ffe7e3)";
                        e.currentTarget.style.borderColor =
                          "var(--cds-support-error, #da1e28)";
                        e.currentTarget.style.color =
                          "var(--cds-text-error, #a2191f)";
                      }}
                      onMouseLeave={(e) => {
                        e.currentTarget.style.background = "transparent";
                        e.currentTarget.style.borderColor =
                          "var(--cds-border-strong-01, #c6c6c6)";
                        e.currentTarget.style.color =
                          "var(--cds-text-secondary, #525252)";
                      }}
                    >
                      ×
                    </button>
                  )}
                </div>
              </div>
              {inPool.length === 0 ? (
                <div
                  style={{
                    padding: "0.5rem 0.9rem",
                    fontSize: "0.75rem",
                    color: "var(--cds-text-placeholder, #8d8d8d)",
                    fontStyle: "italic",
                  }}
                >
                  <FormattedMessage id="vectorDec.preview.empty" />
                </div>
              ) : (
                inPool.map((sp) => {
                  const name = speciesName(sp.vectorSpeciesId);
                  const isDragging = draggedId === sp.sampleItemId;
                  return (
                    <div
                      key={sp.sampleItemId}
                      draggable={strategy === "manual"}
                      onDragStart={(e) => onDragStart(e, sp.sampleItemId)}
                      onDragEnd={onDragEnd}
                      onClick={() => cycleAssignment(sp.sampleItemId)}
                      title={
                        strategy === "manual"
                          ? "Drag to a pool — or click to cycle through pools"
                          : undefined
                      }
                      style={{
                        padding: "0.4rem 0.9rem",
                        fontSize: "0.75rem",
                        borderBottom: "1px solid #f4f4f4",
                        display: "flex",
                        alignItems: "center",
                        gap: 8,
                        cursor:
                          strategy === "manual"
                            ? isDragging
                              ? "grabbing"
                              : "grab"
                            : "default",
                        background: isDragging
                          ? "var(--cds-blue-10, #edf5ff)"
                          : "var(--cds-background, #fff)",
                        opacity: isDragging ? 0.55 : 1,
                        transition: "opacity 0.1s, background 0.1s",
                      }}
                    >
                      {strategy === "manual" && (
                        <span
                          style={{
                            color: "var(--cds-text-placeholder, #8d8d8d)",
                            fontSize: 14,
                          }}
                        >
                          ⠿
                        </span>
                      )}
                      <span style={{ fontWeight: 500 }}>
                        {sp.externalId || `#${sp.sortOrder}`}
                      </span>
                      {name && (
                        <span
                          style={{
                            color: "var(--cds-text-secondary, #525252)",
                            fontSize: "0.6875rem",
                          }}
                        >
                          — {name}
                        </span>
                      )}
                      {sp.confidence === "CONFIRMED" && (
                        <Tag type="green" size="sm">
                          ✓
                        </Tag>
                      )}
                      {sp.confidence === "PRESUMPTIVE" && (
                        <Tag type="warm-gray" size="sm">
                          ~
                        </Tag>
                      )}
                    </div>
                  );
                })
              )}
            </div>
          );
        })}
      </div>
      {strategy === "manual" && (
        <div style={{ marginTop: "0.5rem" }}>
          <button
            type="button"
            onClick={addPool}
            style={{
              background: "var(--cds-background, #fff)",
              border: "1px dashed #c6c6c6",
              borderRadius: 2,
              cursor: "pointer",
              padding: "0.4rem 0.75rem",
              fontSize: "0.75rem",
              color: "var(--cds-text-secondary, #525252)",
              width: "100%",
              textAlign: "center",
            }}
            onMouseEnter={(e) => {
              e.currentTarget.style.background = "var(--cds-blue-10, #edf5ff)";
              e.currentTarget.style.borderColor =
                "var(--cds-link-primary, #0f62fe)";
              e.currentTarget.style.color =
                "var(--cds-link-primary-hover, #0043ce)";
            }}
            onMouseLeave={(e) => {
              e.currentTarget.style.background = "var(--cds-background, #fff)";
              e.currentTarget.style.borderColor =
                "var(--cds-border-strong-01, #c6c6c6)";
              e.currentTarget.style.color =
                "var(--cds-text-secondary, #525252)";
            }}
          >
            <FormattedMessage
              id="vectorDec.pool.add"
              defaultMessage="+ Add another pool"
            />
          </button>
        </div>
      )}

      {strategy === "manual" && (
        <div
          style={{
            marginTop: "0.5rem",
            fontSize: "0.6875rem",
            color: "var(--cds-text-secondary, #525252)",
            fontStyle: "italic",
          }}
        >
          <FormattedMessage
            id="vectorDec.manual.hint"
            defaultMessage="Drag specimens between pools — or click a specimen to cycle. Use × on a pool to delete it (its specimens go back to unassigned), or + Add another pool to grow the list."
          />
        </div>
      )}

      {strategy === "manual" && (
        <div
          onDragOver={(e) => onDragOver(e, 0)}
          onDrop={(e) => onDrop(e, 0)}
          onDragLeave={onDragLeave}
          style={{
            marginTop: "0.5rem",
            padding: "0.5rem 0.75rem",
            background:
              dragOver === 0
                ? "var(--cds-blue-20, #e0e7ff)"
                : draggedId != null
                  ? "var(--cds-blue-10, #eef2ff)"
                  : "var(--cds-layer-01, #f4f4f4)",
            border: `2px ${
              dragOver === 0
                ? "dashed #0f62fe"
                : draggedId != null
                  ? "dashed #a6c8ff"
                  : "solid #e0e0e0"
            }`,
            boxShadow:
              dragOver === 0 ? "0 0 0 3px rgba(15,98,254,0.15)" : "none",
            transition:
              "background 0.12s, border-color 0.12s, box-shadow 0.12s",
          }}
        >
          <div
            style={{
              fontSize: "0.75rem",
              color: "var(--cds-text-secondary, #525252)",
              fontWeight: 600,
              marginBottom: "0.375rem",
            }}
          >
            {unassigned.length > 0 ? (
              <FormattedMessage
                id="vectorDec.preview.unassignedHint"
                values={{ count: unassigned.length }}
              />
            ) : (
              <FormattedMessage
                id="vectorDec.preview.unassigned"
                values={{ count: 0 }}
              />
            )}
          </div>
          <div style={{ display: "flex", flexWrap: "wrap", gap: "0.25rem" }}>
            {unassigned.map((sp) => {
              const isDragging = draggedId === sp.sampleItemId;
              return (
                <span
                  key={sp.sampleItemId}
                  draggable
                  onDragStart={(e) => onDragStart(e, sp.sampleItemId)}
                  onDragEnd={onDragEnd}
                  onClick={() => cycleAssignment(sp.sampleItemId)}
                  style={{
                    padding: "0.25rem 0.5rem",
                    background: isDragging
                      ? "var(--cds-blue-10, #edf5ff)"
                      : "var(--cds-background, #fff)",
                    fontSize: "0.75rem",
                    cursor: isDragging ? "grabbing" : "grab",
                    border: "1px solid #c6c6c6",
                    borderRadius: 2,
                    opacity: isDragging ? 0.55 : 1,
                    userSelect: "none",
                    transition: "opacity 0.1s, background 0.1s",
                  }}
                  title="Drag to a pool above — or click to cycle through pools"
                >
                  ⋮⋮ {sp.externalId || `#${sp.sortOrder}`}
                </span>
              );
            })}
          </div>
        </div>
      )}

      <div
        style={{
          marginTop: "1rem",
          padding: "0.5rem 0.75rem",
          background: "var(--cds-text-primary, #161616)",
          color: "var(--cds-background, #fff)",
          display: "flex",
          alignItems: "center",
          gap: "0.75rem",
          position: "sticky",
          bottom: 0,
        }}
      >
        <span style={{ fontSize: "0.8125rem" }}>
          <FormattedMessage
            id="vectorDec.actionBar.assigned"
            values={{
              assigned: assignedCount,
              total: specimens.length,
              pools: poolCount,
            }}
          />
        </span>
        <div style={{ marginLeft: "auto", display: "flex", gap: "0.5rem" }}>
          <Button
            kind="ghost"
            size="sm"
            onClick={() => setPhase("setup")}
            style={{ color: "var(--cds-background, #fff)" }}
          >
            <FormattedMessage id="vectorDec.button.back" />
          </Button>
          <Button
            kind="primary"
            size="sm"
            onClick={handleInitiate}
            disabled={
              submitting ||
              (strategy === "manual" &&
                specimens.length - unassigned.length === 0)
            }
          >
            <FormattedMessage id="vectorDec.button.savePools" />
          </Button>
        </div>
      </div>
    </>
  );

  // Modal hosts the full setup/preview flow. The footer's Save/Back buttons
  // live inside SetupPhase / PreviewPhase already, so passiveModal omits the
  // built-in footer and we let the body keep its own action bar.
  return (
    <Modal
      open
      passiveModal
      size="lg"
      modalHeading={intl.formatMessage({
        id:
          phase === "setup"
            ? "vectorDec.heading.split"
            : "vectorDec.heading.preview",
      })}
      onRequestClose={onClose}
    >
      <div style={{ paddingBottom: "0.5rem" }}>
        {phase === "setup" ? SetupPhase : PreviewPhase}
      </div>
    </Modal>
  );
};

export default VectorInlineDeconPanel;
