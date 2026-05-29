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
  Checkbox,
  InlineLoading,
  InlineNotification,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableHeader,
  TableRow,
  Tag,
  Tile,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { NotificationContext } from "../layout/Layout";
import { NotificationKinds } from "../common/CustomNotification";
import {
  VectorDeconvolutionAPI,
  VectorIdentificationAPI,
  VectorSpeciesAPI,
} from "./VectorIdentificationService";
import VectorSpecimenForm from "./VectorSpecimenForm";
import VectorBulkApplyTile from "./VectorBulkApplyTile";
import VectorInlineDeconPanel from "./VectorInlineDeconPanel";

const SpecimenStatusTag = ({ value }) => {
  if (value === "CONFIRMED")
    return (
      <Tag type="green">
        <FormattedMessage id="vectorId.specimen.confirmed" />
      </Tag>
    );
  if (value === "PRESUMPTIVE")
    return (
      <Tag type="blue">
        <FormattedMessage id="vectorId.specimen.presumptive" />
      </Tag>
    );
  return (
    <Tag type="gray">
      <FormattedMessage id="vectorId.specimen.notIdentified" />
    </Tag>
  );
};

const SummaryTile = ({ accessionNumber, samplingSiteName, collectionDate }) => (
  <Tile style={{ marginBottom: "0.75rem" }}>
    <div
      style={{
        display: "grid",
        gridTemplateColumns: "repeat(3, 1fr)",
        gap: "1rem",
      }}
    >
      {[
        ["vectorId.col.lotId", accessionNumber],
        ["vectorId.col.site", samplingSiteName || "—"],
        [
          "vectorId.col.collectionDate",
          collectionDate ? new Date(collectionDate).toLocaleDateString() : "—",
        ],
      ].map(([labelKey, value]) => (
        <div key={labelKey}>
          <div
            style={{
              fontSize: "0.6875rem",
              color: "var(--cds-text-secondary, #6f6f6f)",
              textTransform: "uppercase",
              letterSpacing: "0.05em",
              marginBottom: 3,
            }}
          >
            <FormattedMessage id={labelKey} />
          </div>
          <div style={{ fontWeight: 600, fontSize: "0.8125rem" }}>{value}</div>
        </div>
      ))}
    </div>
  </Tile>
);

const SpeciesDistribution = ({ specimens, speciesById }) => {
  const total = specimens.length;
  const counts = useMemo(() => {
    const map = {};
    let unidentified = 0;
    specimens.forEach((s) => {
      if (s.vectorSpeciesId) {
        const sp = speciesById[s.vectorSpeciesId];
        const label = sp
          ? [sp.genus, sp.species].filter(Boolean).join(" ") ||
            `Species ${s.vectorSpeciesId}`
          : `Species ${s.vectorSpeciesId}`;
        map[label] = (map[label] || 0) + 1;
      } else {
        unidentified += 1;
      }
    });
    return { map, unidentified };
  }, [specimens, speciesById]);

  const confirmed = specimens.filter(
    (s) => s.confidence === "CONFIRMED",
  ).length;
  const presumptive = specimens.filter(
    (s) => s.confidence === "PRESUMPTIVE",
  ).length;
  const unid = total - confirmed - presumptive;

  return (
    <Tile style={{ marginBottom: "0.75rem" }}>
      <div
        style={{
          fontWeight: 600,
          fontSize: "0.8125rem",
          marginBottom: "0.625rem",
        }}
      >
        <FormattedMessage id="vectorId.distribution.heading" />
      </div>
      <div
        style={{
          display: "grid",
          gridTemplateColumns: "1fr auto",
          gap: "1rem",
        }}
      >
        <div>
          {Object.entries(counts.map).map(([sp, cnt]) => (
            <div key={sp} style={{ marginBottom: "0.5rem" }}>
              <div
                style={{
                  display: "flex",
                  justifyContent: "space-between",
                  fontSize: "0.75rem",
                }}
              >
                <span>{sp}</span>
                <span style={{ color: "var(--cds-text-secondary, #6f6f6f)" }}>
                  {cnt}/{total}
                </span>
              </div>
              <div
                style={{
                  height: 6,
                  background: "var(--cds-border-subtle-01, #e0e0e0)",
                  borderRadius: 3,
                  marginTop: 2,
                }}
              >
                <div
                  style={{
                    width: `${(cnt / total) * 100}%`,
                    height: "100%",
                    background: sp.toLowerCase().includes("culex")
                      ? "var(--cds-purple-70, #8a3ffc)"
                      : "var(--cds-link-primary, #0f62fe)",
                    borderRadius: 3,
                  }}
                />
              </div>
            </div>
          ))}
          {counts.unidentified > 0 && (
            <div style={{ marginBottom: "0.5rem" }}>
              <div
                style={{
                  display: "flex",
                  justifyContent: "space-between",
                  fontSize: "0.75rem",
                }}
              >
                <span style={{ color: "var(--cds-text-placeholder, #8d8d8d)" }}>
                  <FormattedMessage id="vectorId.distribution.notYet" />
                </span>
                <span style={{ color: "var(--cds-text-secondary, #6f6f6f)" }}>
                  {counts.unidentified}/{total}
                </span>
              </div>
              <div
                style={{
                  height: 6,
                  background: "var(--cds-border-subtle-01, #e0e0e0)",
                  borderRadius: 3,
                  marginTop: 2,
                }}
              >
                <div
                  style={{
                    width: `${(counts.unidentified / total) * 100}%`,
                    height: "100%",
                    background: "var(--cds-border-strong-01, #c6c6c6)",
                    borderRadius: 3,
                  }}
                />
              </div>
            </div>
          )}
        </div>
        <div
          style={{ display: "flex", flexDirection: "column", gap: "0.375rem" }}
        >
          <Tag type="green">
            <FormattedMessage
              id="vectorId.distribution.confirmed"
              values={{ count: confirmed }}
            />
          </Tag>
          <Tag type="warm-gray">
            <FormattedMessage
              id="vectorId.distribution.presumptive"
              values={{ count: presumptive }}
            />
          </Tag>
          <Tag type="gray">
            <FormattedMessage
              id="vectorId.distribution.notIdentified"
              values={{ count: unid }}
            />
          </Tag>
        </div>
      </div>
    </Tile>
  );
};

const VectorLotDetail = ({
  vectorPoolId,
  sampleId,
  accessionNumber,
  samplingSiteName,
  collectionDate,
  positiveTest,
  deconvolutionStatus,
  onChange,
}) => {
  const intl = useIntl();
  const { setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  const [specimens, setSpecimens] = useState([]);
  const [speciesById, setSpeciesById] = useState({});
  const [loading, setLoading] = useState(false);
  const [expandedSpecimenId, setExpandedSpecimenId] = useState(null);
  const [selected, setSelected] = useState(new Set());
  const [bulkOpen, setBulkOpen] = useState(false);
  const [resplitItem, setResplitItem] = useState(null);
  const [confirmingAnalysisId, setConfirmingAnalysisId] = useState(null);
  const [reviewingPoolId, setReviewingPoolId] = useState(null);
  const [collapsedPools, setCollapsedPools] = useState(new Set());
  const togglePoolCollapsed = (poolId) =>
    setCollapsedPools((prev) => {
      const next = new Set(prev);
      next.has(poolId) ? next.delete(poolId) : next.add(poolId);
      return next;
    });
  const [deconSummary, setDeconSummary] = useState(null);

  // Track mounted state so the worklist row collapsing (which unmounts this
  // detail panel) doesn't trigger setState-on-unmounted warnings from
  // in-flight specimens / species / decon fetches.
  const mountedRef = useRef(true);
  useEffect(() => {
    mountedRef.current = true;
    return () => {
      mountedRef.current = false;
    };
  }, []);

  // `addNotification` / `intl` / `setNotificationVisible` are not in the deps
  // intentionally: they're either context callbacks the outer Layout recreates
  // on every render, or stable identity refs. Including them spins this
  // callback's identity on every notification dispatch, which re-fires the
  // effect below and triggers setLoading(true) during the unmount window —
  // producing the "state update on unmounted component" warning. We capture
  // them via closure and only depend on the values that actually change
  // (vectorPoolId — lot == pool per FRS).
  // eslint-disable-next-line react-hooks/exhaustive-deps
  const load = useCallback(() => {
    if (!vectorPoolId) return;
    if (!mountedRef.current) return;
    setLoading(true);
    VectorIdentificationAPI.getSpecimensForLot(vectorPoolId)
      .then((data) => {
        if (!mountedRef.current) return;
        setSpecimens(Array.isArray(data) ? data : []);
      })
      .catch((err) => {
        if (!mountedRef.current) return;
        addNotification({
          kind: NotificationKinds.error,
          title: intl.formatMessage({ id: "notification.title" }),
          message:
            err.message ||
            intl.formatMessage({ id: "vectorId.error.loadSpecimens" }),
        });
        setNotificationVisible(true);
      })
      .finally(() => {
        if (mountedRef.current) setLoading(false);
      });
  }, [vectorPoolId]);

  useEffect(() => {
    load();
  }, [load]);

  useEffect(() => {
    VectorSpeciesAPI.getAll()
      .then((data) => {
        if (!mountedRef.current) return;
        const map = {};
        (Array.isArray(data) ? data : []).forEach((s) => {
          map[s.id] = s;
        });
        setSpeciesById(map);
      })
      .catch(() => {
        if (mountedRef.current) setSpeciesById({});
      });
  }, []);

  // Fetch decon tree: used for (a) completion tile when COMPLETE/IN_PROGRESS
  // and (b) result tags on pool nodes whenever the pool has entered results.
  useEffect(() => {
    if (!vectorPoolId) return;
    if (
      deconvolutionStatus !== "COMPLETE" &&
      deconvolutionStatus !== "IN_PROGRESS" &&
      deconvolutionStatus !== "PENDING"
    ) {
      setDeconSummary(null);
      return;
    }
    VectorDeconvolutionAPI.getDeconvolution(vectorPoolId)
      .then((data) => {
        if (mountedRef.current) setDeconSummary(data || null);
      })
      .catch(() => {
        if (mountedRef.current) setDeconSummary(null);
      });
  }, [vectorPoolId, deconvolutionStatus]);

  const toggleSelect = (id) =>
    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });

  // Unified table: intake pool at depth 0, decon sub-pools nested, specimens
  // under their DEEPEST pool (backend depth-sort guarantees vectorPoolId points
  // there).
  const tree = useMemo(() => {
    // Group members by their pool id.
    const membersByPool = new Map();
    const parentPoolByPool = new Map();
    specimens.forEach((s) => {
      if (s.vectorPoolId == null) return;
      if (!membersByPool.has(s.vectorPoolId)) {
        membersByPool.set(s.vectorPoolId, []);
        parentPoolByPool.set(s.vectorPoolId, s.parentPoolId ?? null);
      }
      membersByPool.get(s.vectorPoolId).push(s);
    });
    if (membersByPool.size === 0) return null;

    // Also collect the authoritative pool external_id from each specimen's
    // poolExternalId field (set by the backend from vector_pool.external_id).
    const poolExternalIdMap = new Map();
    specimens.forEach((s) => {
      if (s.vectorPoolId != null && s.poolExternalId != null) {
        poolExternalIdMap.set(s.vectorPoolId, s.poolExternalId);
      }
      if (s.parentPoolId != null && s.parentPoolExternalId != null) {
        poolExternalIdMap.set(s.parentPoolId, s.parentPoolExternalId);
      }
    });

    const nodes = new Map();
    membersByPool.forEach((members, poolId) => {
      const parentPoolId = parentPoolByPool.get(poolId);
      const label = poolExternalIdMap.get(poolId) || accessionNumber || "—";
      const deconStatus =
        members.length > 0 ? members[0].poolDeconvolutionStatus : null;
      nodes.set(poolId, {
        poolId,
        parentPoolId,
        members,
        children: [],
        externalId: label,
        deconvolutionStatus: deconStatus,
      });
    });

    // Reconstruct stub nodes for parent pools that don't appear as members'
    // direct vectorPoolId. After decon, the intake pool's members have all
    // been moved to sub-pools (their deepest pool wins per backend depth-sort),
    // so no specimen carries the intake pool's id any more. Without this
    // stub, the intake header row disappears from the tree and sub-pools
    // appear as orphan roots.
    nodes.forEach((node) => {
      if (node.parentPoolId != null && !nodes.has(node.parentPoolId)) {
        const stubLabel =
          poolExternalIdMap.get(node.parentPoolId) || accessionNumber || "—";
        nodes.set(node.parentPoolId, {
          poolId: node.parentPoolId,
          parentPoolId: null,
          members: [],
          children: [],
          externalId: stubLabel,
        });
      }
    });

    // Wire children — sub-sub-pools attach under their parent. Anything whose
    // parentPoolId doesn't resolve (e.g. orphaned sub-pool) falls back to root.
    const roots = [];
    nodes.forEach((node) => {
      const parent = nodes.get(node.parentPoolId);
      if (parent) parent.children.push(node);
      else roots.push(node);
    });
    return roots.length > 0 ? roots : null;
  }, [specimens, accessionNumber]);

  // poolId → [{testName, resultDisplay, analysisId, confirmedForAllMembers}]
  // Both node.vectorPoolId (decon tree) and pool.poolId (specimen tree) are Long
  // serialized as JSON numbers. Normalise both to String at build time and lookup
  // time so Map identity is consistent regardless of JS numeric coercion.
  const poolResultsMap = useMemo(() => {
    const map = new Map();
    if (!deconSummary || !Array.isArray(deconSummary.tree)) return map;
    deconSummary.tree.forEach((node) => {
      if (
        node.vectorPoolId != null &&
        Array.isArray(node.results) &&
        node.results.length > 0
      ) {
        map.set(String(node.vectorPoolId), node.results);
      }
    });
    return map;
  }, [deconSummary]);

  // Selectable individuals = all sample_items with qty<=1 (every pool member).
  const allSelectableIds = useMemo(
    () =>
      specimens
        .filter((s) => (s.quantity || 0) <= 1)
        .map((s) => s.sampleItemId),
    [specimens],
  );
  const allSelected =
    allSelectableIds.length > 0 && selected.size === allSelectableIds.length;
  const toggleAll = () => {
    if (allSelected) setSelected(new Set());
    else setSelected(new Set(allSelectableIds));
  };

  const onSpecimenSaved = () => {
    setExpandedSpecimenId(null);
    load();
    if (onChange) onChange();
  };

  const onBulkApplied = () => {
    setBulkOpen(false);
    setSelected(new Set());
    load();
    if (onChange) onChange();
  };

  const onDeconInitiated = () => {
    setResplitItem(null);
    load();
    if (onChange) onChange();
  };

  const handleConfirmResult = (poolId, analysisId) => {
    setConfirmingAnalysisId(analysisId);
    VectorDeconvolutionAPI.confirmResult(poolId, analysisId)
      .then(() => {
        if (!mountedRef.current) return;
        setConfirmingAnalysisId(null);
        setReviewingPoolId(null);
        addNotification({
          kind: NotificationKinds.success,
          title: intl.formatMessage({ id: "notification.title" }),
          message: intl.formatMessage({
            id: "vectorId.notif.confirmAll.success",
          }),
        });
        setNotificationVisible(true);
        VectorDeconvolutionAPI.getDeconvolution(vectorPoolId).then((data) => {
          if (mountedRef.current) setDeconSummary(data || null);
        });
        load();
        if (onChange) onChange();
      })
      .catch((err) => {
        if (!mountedRef.current) return;
        setConfirmingAnalysisId(null);
        addNotification({
          kind: NotificationKinds.error,
          title: intl.formatMessage({ id: "notification.title" }),
          message:
            err.message ||
            intl.formatMessage({ id: "vectorId.notif.confirmAll.error" }),
        });
        setNotificationVisible(true);
      });
  };

  const speciesLabelFor = (id) => {
    const sp = speciesById[id];
    if (!sp) return id ? `Species ${id}` : "—";
    return [sp.genus, sp.species].filter(Boolean).join(" ");
  };

  // resplitItem is a pool tree node (see renderPoolNode); its members come
  // directly from the node, which we attached when grouping by vectorPoolId.
  // This is the only Split entry point — every intake pool and every sub-pool
  // gets its own Split arrow in the tree.
  const resplitSpecimens = useMemo(() => {
    if (!resplitItem) return [];
    return resplitItem.members || [];
  }, [resplitItem]);

  if (loading) {
    return (
      <div style={{ padding: "1rem" }}>
        <InlineLoading
          description={intl.formatMessage({ id: "vectorId.loading" })}
        />
      </div>
    );
  }

  // Total organism count under a pool — direct members plus everything
  // recursively under any sub-pools. Matches the "X organisms" label the
  // mockup shows on every pool header, including ancestors of decon descendants.
  const countOrganisms = (pool) => {
    const direct = (pool.members || []).length;
    const fromChildren = (pool.children || []).reduce(
      (sum, child) => sum + countOrganisms(child),
      0,
    );
    return direct + fromChildren;
  };

  const renderPoolNode = (pool, depth) => {
    const indent = depth * 20;
    const hasChildren = pool.children && pool.children.length > 0;
    const members = pool.members || [];
    const directCount = members.length;
    const totalCount = countOrganisms(pool);
    // Split only on a pool that has direct members, no sub-pools, and is not yet closed.
    const canSplit =
      !hasChildren &&
      directCount > 1 &&
      pool.deconvolutionStatus !== "COMPLETE";
    const headerBg =
      depth === 0
        ? "var(--cds-layer-01, #f4f4f4)"
        : "var(--cds-purple-10, #ede8ff)";
    const arrowColor =
      depth === 0
        ? "var(--cds-text-secondary, #525252)"
        : "var(--cds-purple-70, #8a3ffc)";
    const hasContentBelow = hasChildren || directCount > 0;
    const isCollapsed = collapsedPools.has(pool.poolId);
    const homogeneousSpeciesLabel = (() => {
      if (members.length === 0) return null;
      const ids = members.map((l) => l.vectorSpeciesId).filter(Boolean);
      if (ids.length !== members.length) return null;
      const unique = [...new Set(ids)];
      if (unique.length !== 1) return null;
      return speciesLabelFor(unique[0]);
    })();
    // Sample type tag — disambiguates intake pools when a lot has multiple
    // (one per sample type after V-02 fan-out). Sub-pools inherit homogeneity
    // from their parent so this also surfaces "this is the mosquito branch".
    const homogeneousSampleType = (() => {
      if (members.length === 0) return null;
      const names = members.map((l) => l.typeOfSampleName).filter(Boolean);
      if (names.length !== members.length) return null;
      const unique = [...new Set(names)];
      if (unique.length !== 1) return null;
      return unique[0];
    })();

    return (
      <React.Fragment key={pool.poolId}>
        <TableRow style={{ background: headerBg }}>
          <TableCell style={{ paddingLeft: 6 + indent }}>
            {hasContentBelow ? (
              <button
                onClick={() => togglePoolCollapsed(pool.poolId)}
                style={{
                  background: "none",
                  border: "none",
                  cursor: "pointer",
                  padding: 0,
                  fontSize: 20,
                  color: arrowColor,
                  lineHeight: 1,
                }}
                aria-label={isCollapsed ? "Expand pool" : "Collapse pool"}
              >
                {isCollapsed ? "▸" : "▾"}
              </button>
            ) : (
              <span style={{ fontSize: 20, color: arrowColor }}>▸</span>
            )}
          </TableCell>
          <TableCell style={{ fontWeight: 700, paddingLeft: 10 + indent }}>
            {pool.externalId || "—"}
            {pool.deconvolutionStatus === "PENDING" && (
              <Tag
                type="red"
                size="sm"
                style={{ marginLeft: 8, verticalAlign: "middle" }}
              >
                <FormattedMessage
                  id="vectorId.tag.deconNeeded"
                  defaultMessage="Decon Needed"
                />
              </Tag>
            )}
            {homogeneousSampleType && (
              <Tag
                type="cyan"
                size="sm"
                style={{ marginLeft: 8, verticalAlign: "middle" }}
              >
                {homogeneousSampleType}
              </Tag>
            )}
            {(poolResultsMap.get(String(pool.poolId)) || []).map((r, i) => (
              <Tag
                key={i}
                type={r.confirmedForAllMembers ? "green" : "teal"}
                size="sm"
                style={{ marginLeft: 4, verticalAlign: "middle" }}
              >
                {r.confirmedForAllMembers ? "✓ " : ""}
                {r.testName}: {r.resultDisplay}
                {r.confirmedForAllMembers && totalCount > 0
                  ? ` — all ${totalCount}`
                  : ""}
              </Tag>
            ))}
            <span
              style={{
                marginLeft: 8,
                fontSize: "0.6875rem",
                color: "var(--cds-text-secondary, #525252)",
                fontWeight: 400,
              }}
            >
              ·{" "}
              <FormattedMessage
                id="vectorId.tree.organisms"
                values={{ count: totalCount }}
              />
            </span>
          </TableCell>
          <TableCell
            style={{
              fontFamily: "monospace",
              fontSize: "0.6875rem",
              color: "var(--cds-text-secondary, #525252)",
            }}
          >
            {pool.externalId}
          </TableCell>
          <TableCell />
          <TableCell
            style={{
              fontSize: "0.75rem",
              color: "var(--cds-text-secondary, #525252)",
            }}
          >
            {homogeneousSpeciesLabel || (
              <span style={{ color: "var(--cds-border-strong-01, #c6c6c6)" }}>
                —
              </span>
            )}
          </TableCell>
          <TableCell />
          <TableCell>
            {(() => {
              const isSplitOpen =
                resplitItem && resplitItem.poolId === pool.poolId;
              const poolResults = poolResultsMap.get(String(pool.poolId)) || [];
              const unconfirmedResults = poolResults.filter(
                (r) => !r.confirmedForAllMembers,
              );
              const isReviewing = reviewingPoolId === pool.poolId;
              return (
                <div style={{ display: "flex", flexWrap: "wrap", gap: 4 }}>
                  {canSplit && (
                    <Button
                      kind="ghost"
                      size="sm"
                      disabled={!!confirmingAnalysisId}
                      onClick={() => setResplitItem(pool)}
                    >
                      <FormattedMessage id="vectorId.button.splitPool" />
                    </Button>
                  )}
                  {!hasChildren && unconfirmedResults.length > 0 && (
                    <Button
                      kind="ghost"
                      size="sm"
                      disabled={isSplitOpen}
                      onClick={() =>
                        setReviewingPoolId((prev) =>
                          prev === pool.poolId ? null : pool.poolId,
                        )
                      }
                    >
                      {isReviewing ? (
                        <FormattedMessage
                          id="vectorId.button.hideResults"
                          defaultMessage="Hide results"
                        />
                      ) : (
                        <FormattedMessage
                          id="vectorId.button.reviewResults"
                          defaultMessage="Review results ({n})"
                          values={{ n: unconfirmedResults.length }}
                        />
                      )}
                    </Button>
                  )}
                </div>
              );
            })()}
          </TableCell>
        </TableRow>
        {/* Expandable results sub-row — one confirm button per unconfirmed test */}
        {reviewingPoolId === pool.poolId &&
          !hasChildren &&
          (() => {
            const poolResults = poolResultsMap.get(String(pool.poolId)) || [];
            const unconfirmedResults = poolResults.filter(
              (r) => !r.confirmedForAllMembers,
            );
            if (unconfirmedResults.length === 0) return null;
            return (
              <TableRow>
                <TableCell
                  colSpan={7}
                  style={{
                    padding: 0,
                    background: "var(--cds-layer-02, #e8e8e8)",
                  }}
                >
                  <div
                    style={{
                      paddingLeft: 28 + indent,
                      paddingRight: 12,
                      paddingTop: 4,
                      paddingBottom: 4,
                    }}
                  >
                    {unconfirmedResults.map((r, i) => (
                      <div
                        key={r.analysisId}
                        style={{
                          display: "flex",
                          alignItems: "center",
                          gap: "0.75rem",
                          padding: "0.3rem 0",
                          borderTop:
                            i > 0
                              ? "1px solid var(--cds-border-subtle-01, #e0e0e0)"
                              : undefined,
                        }}
                      >
                        <span
                          style={{
                            flex: 1,
                            fontSize: "0.8125rem",
                            fontWeight: 500,
                          }}
                        >
                          {r.testName}
                        </span>
                        <Tag type="teal" size="sm">
                          {r.resultDisplay}
                        </Tag>
                        <Button
                          kind="tertiary"
                          size="sm"
                          disabled={confirmingAnalysisId === r.analysisId}
                          onClick={() =>
                            handleConfirmResult(pool.poolId, r.analysisId)
                          }
                        >
                          <FormattedMessage
                            id="vectorId.button.confirm"
                            defaultMessage="Confirm"
                          />
                        </Button>
                      </div>
                    ))}
                  </div>
                </TableCell>
              </TableRow>
            );
          })()}
        {!isCollapsed &&
          hasChildren &&
          pool.children.map((child) => renderPoolNode(child, depth + 1))}
        {!isCollapsed &&
          members.map((leaf, li) => {
            const last = li === members.length - 1;
            return (
              <React.Fragment key={leaf.sampleItemId}>
                <TableRow style={{ background: "var(--cds-background, #fff)" }}>
                  <TableCell style={{ paddingLeft: 6 + indent + 16 }}>
                    <div
                      style={{ position: "relative", display: "inline-block" }}
                    >
                      <Checkbox
                        id={`sp-${leaf.sampleItemId}`}
                        labelText=""
                        hideLabel
                        checked={selected.has(leaf.sampleItemId)}
                        onChange={() => toggleSelect(leaf.sampleItemId)}
                      />
                      {leaf.vectorSpeciesId && (
                        <span
                          title={leaf.identificationStatus}
                          style={{
                            position: "absolute",
                            top: -5,
                            right: -6,
                            width: 12,
                            height: 12,
                            borderRadius: "50%",
                            background: "var(--cds-support-success, #24a148)",
                            display: "flex",
                            alignItems: "center",
                            justifyContent: "center",
                            pointerEvents: "none",
                          }}
                        >
                          <svg
                            viewBox="0 0 12 12"
                            width="8"
                            height="8"
                            fill="none"
                            stroke="#fff"
                            strokeWidth="2"
                            strokeLinecap="round"
                            strokeLinejoin="round"
                          >
                            <polyline points="2,6 5,9 10,3" />
                          </svg>
                        </span>
                      )}
                    </div>
                  </TableCell>
                  <TableCell style={{ paddingLeft: 10 + indent + 16 }}>
                    <span
                      style={{
                        color: "var(--cds-text-placeholder, #8d8d8d)",
                        marginRight: 5,
                      }}
                    >
                      {last ? "└╴" : "├╴"}
                    </span>
                    <span style={{ fontWeight: 500 }}>
                      <FormattedMessage
                        id="vectorId.specimen.label"
                        defaultMessage="Specimen #{n}"
                        values={{ n: leaf.sortOrder }}
                      />
                    </span>
                  </TableCell>
                  <TableCell
                    style={{
                      fontFamily: "monospace",
                      fontSize: "0.6875rem",
                      color: "var(--cds-text-secondary, #525252)",
                    }}
                  >
                    {leaf.externalId || `#${leaf.sortOrder}`}
                  </TableCell>
                  <TableCell>
                    <SpecimenStatusTag value={leaf.identificationStatus} />
                  </TableCell>
                  <TableCell>
                    {leaf.vectorSpeciesId
                      ? speciesLabelFor(leaf.vectorSpeciesId)
                      : "—"}
                  </TableCell>
                  <TableCell>{leaf.confidence || "—"}</TableCell>
                  <TableCell>
                    <Button
                      kind="ghost"
                      size="sm"
                      onClick={() =>
                        setExpandedSpecimenId((prev) =>
                          prev === leaf.sampleItemId ? null : leaf.sampleItemId,
                        )
                      }
                    >
                      {expandedSpecimenId === leaf.sampleItemId ? (
                        <FormattedMessage id="vectorId.button.close" />
                      ) : (
                        <FormattedMessage id="vectorId.button.identify" />
                      )}
                    </Button>
                  </TableCell>
                </TableRow>
                {expandedSpecimenId === leaf.sampleItemId && (
                  <TableRow>
                    <TableCell colSpan={7} style={{ padding: 0 }}>
                      <VectorSpecimenForm
                        specimen={leaf}
                        lotId={vectorPoolId}
                        speciesById={speciesById}
                        onSaved={onSpecimenSaved}
                        onCancel={() => setExpandedSpecimenId(null)}
                      />
                    </TableCell>
                  </TableRow>
                )}
              </React.Fragment>
            );
          })}
      </React.Fragment>
    );
  };

  // Flat (no decon) renderer — keeps the simple pre-decon view.
  const renderFlatRow = (sp) => (
    <React.Fragment key={sp.sampleItemId}>
      <TableRow>
        <TableCell>
          <div style={{ position: "relative", display: "inline-block" }}>
            <Checkbox
              id={`sp-${sp.sampleItemId}`}
              labelText=""
              hideLabel
              checked={selected.has(sp.sampleItemId)}
              onChange={() => toggleSelect(sp.sampleItemId)}
            />
            {sp.vectorSpeciesId && (
              <span
                title={sp.identificationStatus}
                style={{
                  position: "absolute",
                  top: -5,
                  right: -6,
                  width: 12,
                  height: 12,
                  borderRadius: "50%",
                  background: "var(--cds-support-success, #24a148)",
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "center",
                  pointerEvents: "none",
                }}
              >
                <svg
                  viewBox="0 0 12 12"
                  width="8"
                  height="8"
                  fill="none"
                  stroke="#fff"
                  strokeWidth="2"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                >
                  <polyline points="2,6 5,9 10,3" />
                </svg>
              </span>
            )}
          </div>
        </TableCell>
        <TableCell style={{ fontWeight: 500 }}>
          {sp.externalId || `#${sp.sortOrder}`}
        </TableCell>
        <TableCell
          style={{
            fontFamily: "monospace",
            fontSize: "0.75rem",
            color: "var(--cds-text-placeholder, #8d8d8d)",
          }}
        >
          {sp.externalId || "—"}
        </TableCell>
        <TableCell>
          <SpecimenStatusTag value={sp.identificationStatus} />
        </TableCell>
        <TableCell>
          {sp.vectorSpeciesId ? speciesLabelFor(sp.vectorSpeciesId) : "—"}
        </TableCell>
        <TableCell>{sp.confidence || "—"}</TableCell>
        <TableCell>
          <Button
            kind="ghost"
            size="sm"
            onClick={() =>
              setExpandedSpecimenId((prev) =>
                prev === sp.sampleItemId ? null : sp.sampleItemId,
              )
            }
          >
            {expandedSpecimenId === sp.sampleItemId ? (
              <FormattedMessage id="vectorId.button.close" />
            ) : (
              <FormattedMessage id="vectorId.button.identify" />
            )}
          </Button>
        </TableCell>
      </TableRow>
      {expandedSpecimenId === sp.sampleItemId && (
        <TableRow>
          <TableCell colSpan={7} style={{ padding: 0 }}>
            <VectorSpecimenForm
              specimen={sp}
              lotId={vectorPoolId}
              speciesById={speciesById}
              onSaved={onSpecimenSaved}
              onCancel={() => setExpandedSpecimenId(null)}
            />
          </TableCell>
        </TableRow>
      )}
    </React.Fragment>
  );

  const flatLeaves = specimens.filter((s) => (s.quantity || 0) <= 1);

  return (
    <div style={{ padding: "0.75rem 1rem" }}>
      {deconvolutionStatus === "PENDING" && (
        <InlineNotification
          kind="warning"
          title={intl.formatMessage({ id: "vectorId.notif.deconNeeded.title" })}
          subtitle={
            positiveTest
              ? intl.formatMessage(
                  { id: "vectorId.notif.deconNeeded.bodyWithTest" },
                  { test: positiveTest },
                )
              : intl.formatMessage({ id: "vectorId.notif.deconNeeded.body" })
          }
          lowContrast
          hideCloseButton
          style={{ marginBottom: "0.5rem", maxWidth: "100%" }}
        />
      )}

      {/* Sub-pooling is intrinsically a per-pool operation — the pool tree
          (renderPoolNode) exposes a Split action on every pool that's
          eligible. There is no lot-level Split because a lot with mixed
          sample types has multiple intake pools (one per type after V-02
          fan-out) and "splitting the lot" makes no biological sense. */}

      {resplitItem && (
        <VectorInlineDeconPanel
          specimens={resplitSpecimens}
          accessionNumber={accessionNumber}
          vectorPoolId={resplitItem.poolId}
          parentExternalId={resplitItem.externalId}
          positiveTest={positiveTest}
          speciesById={speciesById}
          onClose={() => setResplitItem(null)}
          onInitiated={onDeconInitiated}
        />
      )}

      {deconSummary &&
        (() => {
          const isComplete = deconvolutionStatus === "COMPLETE";
          const wasDeconvolved =
            deconSummary.tree &&
            deconSummary.tree.some((n) => n.parentPoolId != null);
          // Collect every confirmed result across all pool nodes in the tree.
          const allConfirmed = (deconSummary.tree || [])
            .flatMap((n) => n.results || [])
            .filter((r) => r.confirmedForAllMembers);

          // Show the banner as soon as any result is confirmed — don't wait for
          // the entire pool to reach COMPLETE status.
          if (allConfirmed.length === 0 && !isComplete) return null;

          if (!isComplete) {
            // Partial confirmation — at least one test confirmed, others pending.
            return (
              <Tile
                style={{
                  marginBottom: "0.75rem",
                  background: "var(--cds-support-success-inverse, #defbe9)",
                  border: "1px solid #24a148",
                  padding: "0.75rem 1rem",
                }}
              >
                <div
                  style={{
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "space-between",
                    gap: "0.75rem",
                  }}
                >
                  <div>
                    <div
                      style={{
                        fontWeight: 600,
                        fontSize: "0.875rem",
                        color: "var(--cds-green-70, #0e6027)",
                      }}
                    >
                      <FormattedMessage
                        id="vectorId.completion.heading.partial"
                        defaultMessage="Results Partially Confirmed"
                      />
                    </div>
                    <div
                      style={{
                        fontSize: "0.75rem",
                        color: "var(--cds-text-primary, #393939)",
                        marginTop: 2,
                      }}
                    >
                      <FormattedMessage
                        id="vectorId.completion.body.partial"
                        defaultMessage="Confirmed for all vectors in this pool:"
                      />
                      <div
                        style={{
                          marginTop: "0.5rem",
                          display: "flex",
                          flexWrap: "wrap",
                          gap: 4,
                        }}
                      >
                        {allConfirmed.map((r, i) => (
                          <Tag key={i} type="green" size="sm">
                            ✓ {r.testName}: {r.resultDisplay}
                          </Tag>
                        ))}
                      </div>
                    </div>
                  </div>
                  <Tag type="green">
                    <FormattedMessage
                      id="vectorId.completion.tag.partial"
                      defaultMessage="Partially Confirmed"
                    />
                  </Tag>
                </div>
              </Tile>
            );
          }

          return (
            <Tile
              style={{
                marginBottom: "0.75rem",
                background: "var(--cds-support-success-inverse, #defbe9)",
                border: "1px solid #24a148",
                padding: "0.75rem 1rem",
              }}
            >
              <div
                style={{
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "space-between",
                  gap: "0.75rem",
                }}
              >
                <div>
                  <div
                    style={{
                      fontWeight: 600,
                      fontSize: "0.875rem",
                      color: "var(--cds-green-70, #0e6027)",
                    }}
                  >
                    <FormattedMessage
                      id={
                        wasDeconvolved
                          ? "vectorId.completion.heading"
                          : "vectorId.completion.heading.confirmed"
                      }
                    />
                  </div>
                  <div
                    style={{
                      fontSize: "0.75rem",
                      color: "var(--cds-text-primary, #393939)",
                      marginTop: 2,
                    }}
                  >
                    {wasDeconvolved ? (
                      <FormattedMessage
                        id="vectorId.completion.body"
                        values={{
                          positive: deconSummary.leafPositiveCount ?? 0,
                          total: deconSummary.leafTotalCount ?? 0,
                          pct:
                            deconSummary.deconvolutionOutcomePct != null
                              ? deconSummary.deconvolutionOutcomePct.toFixed(1)
                              : "0.0",
                        }}
                      />
                    ) : (
                      <>
                        <FormattedMessage
                          id="vectorId.completion.body.confirmed"
                          values={{ total: deconSummary.leafTotalCount ?? 0 }}
                        />
                        {allConfirmed.length > 0 && (
                          <div
                            style={{
                              marginTop: "0.5rem",
                              display: "flex",
                              flexWrap: "wrap",
                              gap: 4,
                            }}
                          >
                            {allConfirmed.map((r, i) => (
                              <Tag key={i} type="green" size="sm">
                                ✓ {r.testName}: {r.resultDisplay}
                              </Tag>
                            ))}
                          </div>
                        )}
                      </>
                    )}
                  </div>
                </div>
                <Tag type="green">
                  <FormattedMessage
                    id={
                      wasDeconvolved
                        ? "vectorId.completion.tag"
                        : "vectorId.completion.tag.confirmed"
                    }
                  />
                </Tag>
              </div>
            </Tile>
          );
        })()}

      <SummaryTile
        accessionNumber={accessionNumber}
        samplingSiteName={samplingSiteName}
        collectionDate={collectionDate}
      />

      {flatLeaves.length > 0 && (
        <SpeciesDistribution specimens={flatLeaves} speciesById={speciesById} />
      )}

      {selected.size > 1 && (
        <div
          style={{
            background: "var(--cds-text-primary, #161616)",
            color: "var(--cds-background, #fff)",
            padding: "0.5rem 0.75rem",
            display: "flex",
            alignItems: "center",
            gap: "0.75rem",
            marginBottom: "0.5rem",
          }}
        >
          <span style={{ fontWeight: 500 }}>
            <FormattedMessage
              id="vectorId.batch.selectedCount"
              values={{ count: selected.size }}
            />
          </span>
          <Button kind="secondary" size="sm" onClick={() => setBulkOpen(true)}>
            <FormattedMessage
              id="vectorId.button.bulkApply"
              values={{ count: selected.size }}
            />
          </Button>
          <Button
            kind="ghost"
            size="sm"
            onClick={() => setSelected(new Set())}
            style={{ color: "var(--cds-background, #fff)" }}
          >
            <FormattedMessage id="vectorId.button.cancel" />
          </Button>
        </div>
      )}

      {bulkOpen && (
        <VectorBulkApplyTile
          sampleItemIds={Array.from(selected)}
          prefillSpecimen={specimens.find((s) => selected.has(s.sampleItemId))}
          onClose={() => setBulkOpen(false)}
          onApplied={onBulkApplied}
        />
      )}

      <TableContainer>
        <Table size="sm">
          <TableHead>
            <TableRow>
              <TableHeader style={{ width: 32 }}>
                {!tree && (
                  <Checkbox
                    id={`selectAll-${sampleId}`}
                    labelText=""
                    hideLabel
                    checked={allSelected}
                    onChange={toggleAll}
                  />
                )}
              </TableHeader>
              <TableHeader>
                <FormattedMessage
                  id={
                    tree
                      ? "vectorId.specimen.col.poolOrSpecimen"
                      : "vectorId.specimen.col.label"
                  }
                />
              </TableHeader>
              <TableHeader>
                <FormattedMessage id="vectorId.specimen.col.labNo" />
              </TableHeader>
              <TableHeader>
                <FormattedMessage id="vectorId.specimen.col.idStatus" />
              </TableHeader>
              <TableHeader>
                <FormattedMessage id="vectorId.specimen.col.species" />
              </TableHeader>
              <TableHeader>
                <FormattedMessage id="vectorId.specimen.col.confidence" />
              </TableHeader>
              <TableHeader />
            </TableRow>
          </TableHead>
          <TableBody>
            {specimens.length === 0 && (
              <TableRow>
                <TableCell
                  colSpan={7}
                  style={{
                    textAlign: "center",
                    color: "var(--cds-text-placeholder, #8d8d8d)",
                  }}
                >
                  <FormattedMessage id="vectorId.specimen.empty" />
                </TableCell>
              </TableRow>
            )}
            {tree
              ? tree.map((root) => renderPoolNode(root, 0))
              : flatLeaves.map((sp) => renderFlatRow(sp))}
          </TableBody>
        </Table>
      </TableContainer>
    </div>
  );
};

export default VectorLotDetail;
