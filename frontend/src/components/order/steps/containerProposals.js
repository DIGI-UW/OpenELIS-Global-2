/**
 * Proposed samples on Prepare Samples (OGC-1611 FR-B24 to B26, G8).
 *
 * The server nets each sample's tests into the containers to draw. A sample
 * still open to the proposal (not saved, not edited by the user) takes the
 * first container proposed for it; each further container becomes a new
 * proposed sample holding the tests that need it. A container drawn for tests
 * that need their own is never shared with other tests, so tests nothing is
 * proposed for then stay on the sample without a container. Saved and edited samples
 * only receive the container choices and whether a container is required.
 */

const ids = (items) => (items || []).map((item) => String(item.id));

/** The samples the proposal is asked about, keyed by their position. */
export const proposalRequest = (samples) =>
  (samples || [])
    .map((sample, index) => ({
      key: String(index),
      sampleTypeId: sample?.sampleTypeId ? String(sample.sampleTypeId) : "",
      testIds: ids(sample?.tests),
      panelIds: ids(sample?.panels),
    }))
    .filter((request) => request.sampleTypeId);

/** Changes when any sample's type, tests or panels change. */
export const proposalSignature = (samples) =>
  JSON.stringify(proposalRequest(samples));

/** A sample the proposal may still change (FR-B26). */
export const isProposable = (sample) =>
  !!sample && !sample.sampleItemId && !sample.containerEdited;

const SPLIT_RESET = {
  sampleItemId: "",
  sampleTypeRequestId: "",
  clientKey: undefined,
  index: undefined,
  quantity: "",
  cultureSetNumber: "",
  referralItems: [],
  storageLocationId: "",
  storageLocationType: "",
  storagePositionCoordinate: "",
  storageNotes: "",
};

const proposedFields = (group) => ({
  containerTypeId: String(group.containerTypeId),
  proposedName: group.name,
  proposed: true,
  proposedFor: group.testNames || [],
  proposedCount: group.count || 1,
  proposedOwn: !!group.ownContainer,
});

const union = (left, right, key) => {
  const seen = new Set((left || []).map(key));
  return [
    ...(left || []),
    ...(right || []).filter((item) => !seen.has(key(item))),
  ];
};

/**
 * Whether a container proposed for one sample's tests can be drawn as an
 * untouched proposed sample already on the order: same sample type and
 * container, and, for tests needing their own container, the same panel
 * (FR-B25), so a test added later joins the tube its panel already has.
 */
const joins = (candidate, row, group) =>
  isProposable(candidate) &&
  candidate.proposed &&
  String(candidate.sampleTypeId) === String(row.sampleTypeId) &&
  String(candidate.containerTypeId) === String(group.containerTypeId) &&
  !!candidate.proposedOwn === !!group.ownContainer &&
  (!group.ownContainer ||
    (group.panelIds || []).some((panelId) =>
      ids(candidate.panels).includes(String(panelId)),
    ));

const merged = (target, row, group) => ({
  ...target,
  tests: union(target.tests, row.tests, (test) => String(test.id)),
  panels: union(target.panels, row.panels, (panel) => String(panel.id)),
  proposedFor: union(target.proposedFor, group.testNames, (name) => name),
  proposedCount: Math.max(target.proposedCount || 1, group.count || 1),
});

const sameSample = (a, b) =>
  ["containerTypeId", "proposed", "containerRequired"].every(
    (field) => (a[field] ?? "") === (b[field] ?? ""),
  ) &&
  JSON.stringify(ids(a.tests)) === JSON.stringify(ids(b.tests)) &&
  JSON.stringify(ids(a.panels)) === JSON.stringify(ids(b.panels)) &&
  JSON.stringify(a.proposedFor || []) === JSON.stringify(b.proposedFor || []) &&
  JSON.stringify(ids(a.containerChoices)) ===
    JSON.stringify(ids(b.containerChoices));

/**
 * Applies the proposal to the samples. Returns the new samples, whether
 * anything changed, and the containers added, joined and removed for
 * untouched proposed samples, for the message FR-B26 asks for.
 */
export function applyProposals(samples, views) {
  const byIndex = new Map(
    (views || []).map((view) => [Number(view.key), view]),
  );
  const kept = [];
  const appended = [];
  const added = [];
  const removed = [];
  const pending = [];
  const joined = [];
  (samples || []).forEach((sample, index) => {
    const view = byIndex.get(index);
    if (!view) {
      kept.push(sample);
      return;
    }
    const base = {
      ...sample,
      containerChoices: view.choices || [],
      containerRequired: !!view.containerRequired,
    };
    if (!isProposable(sample)) {
      const fits =
        sample.sampleItemId ||
        !sample.containerTypeId ||
        ids(view.choices).includes(String(sample.containerTypeId));
      kept.push(fits ? base : { ...base, containerTypeId: "" });
      return;
    }
    const groups = view.proposed || [];
    if (
      sample.proposedSplit &&
      !sample.tests?.length &&
      !sample.panels?.length
    ) {
      removed.push({
        name:
          sample.proposedName ||
          containerName({ ...sample, containerChoices: view.choices }),
        count: 1,
      });
      return;
    }
    if (groups.length === 0) {
      kept.push(
        sample.proposed
          ? {
              ...base,
              containerTypeId: "",
              proposed: false,
              proposedFor: [],
            }
          : base,
      );
      return;
    }
    const unproposed = view.unproposedTestIds || [];
    const first =
      groups[0].ownContainer && unproposed.length > 0 ? null : groups[0];
    const others = first ? groups.slice(1) : groups;
    const elsewhere = new Set(others.flatMap((group) => group.testIds));
    const firstPanels = new Set(first?.panelIds || []);
    const panelsElsewhere = new Set(
      first
        ? others
            .flatMap((group) => group.panelIds || [])
            .filter((panelId) => !firstPanels.has(panelId))
        : [],
    );
    const remaining = {
      ...base,
      tests: (sample.tests || []).filter(
        (test) => !elsewhere.has(String(test.id)),
      ),
      panels: (sample.panels || []).filter(
        (panel) => !panelsElsewhere.has(String(panel.id)),
      ),
    };
    const source = first
      ? { ...remaining, ...proposedFields(first) }
      : {
          ...remaining,
          containerTypeId: sample.proposed ? "" : sample.containerTypeId,
          containerRequired: false,
          proposed: false,
          proposedFor: [],
        };
    kept.push(source);
    others.forEach((group) => {
      const testIds = new Set(group.testIds);
      const panelIds = new Set(group.panelIds || []);
      pending.push({
        source,
        group,
        row: {
          ...base,
          ...SPLIT_RESET,
          ...proposedFields(group),
          proposedSplit: true,
          containerEdited: false,
          tests: (sample.tests || []).filter((test) =>
            testIds.has(String(test.id)),
          ),
          panels: (sample.panels || []).filter((panel) =>
            panelIds.has(String(panel.id)),
          ),
        },
      });
    });
  });
  pending.forEach(({ source, group, row }) => {
    const all = [...kept, ...appended];
    const target = all.find(
      (candidate) => candidate !== source && joins(candidate, row, group),
    );
    if (target) {
      const into = merged(target, row, group);
      const keptAt = kept.indexOf(target);
      if (keptAt >= 0) kept[keptAt] = into;
      else appended[appended.indexOf(target)] = into;
      joined.push({ name: group.name, testNames: group.testNames || [] });
      return;
    }
    appended.push(row);
    added.push({
      name: group.name,
      count: group.count || 1,
      testNames: group.testNames || [],
    });
  });
  const next = [...kept, ...appended];
  const changed =
    next.length !== (samples || []).length ||
    next.some((sample, index) => !sameSample(sample, samples[index]));
  return { samples: next, changed, added, joined, removed };
}

/** The name of a sample's container, from its choices. */
export const containerName = (sample) =>
  (sample?.containerChoices || []).find(
    (choice) => String(choice.id) === String(sample?.containerTypeId),
  )?.name || "";
