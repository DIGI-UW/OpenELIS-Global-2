/**
 * OGC-949 — Test Catalog Management: v1/v2 Scope & Artifact Inventory (review canvas)
 *
 * A self-contained, dependency-free dashboard for reviewing the OGC-949 test-catalog
 * "mess" with the team: what's v1 vs v2, what's built vs specced vs outdated, which
 * openelis-work design artifacts are authoritative, and the open PRs.
 *
 * Self-contained: only React, inline styles — renders anywhere a .tsx can.
 *
 * Sources (ground truth, as of 2026-06-16):
 *   - openelis-work test-catalog/test-catalog-v2.5-v1-jira.md  (authoritative v1 scope)
 *   - openelis-work test-catalog/test-catalog-v2.5-v2-jira.md  (v2 roadmap, draft)
 *   - SpecKit specs/OGC-949-unified-test-catalog/{spec,plan,tasks,data-model}.md
 *   - shipped Liquibase 042/043 + the M9–M12 code on PR #3716
 */
import React from "react";

// ─────────────────────────────────────────────────────────────────────────────
// Palette + primitives
// ─────────────────────────────────────────────────────────────────────────────
const C = {
  bg: "#0f1115",
  panel: "#171a21",
  panel2: "#1e222b",
  border: "#2a2f3a",
  text: "#e6e9ef",
  dim: "#9aa3b2",
  v1: "#3b82f6",
  v2: "#a78bfa",
  built: "#22c55e",
  gap: "#f59e0b",
  deferred: "#64748b",
  outdated: "#ef4444",
  ref: "#38bdf8",
  process: "#94a3b8",
  separate: "#eab308",
};

type Status =
  | "built"
  | "gap"
  | "deferred"
  | "authoritative"
  | "reference"
  | "outdated"
  | "process"
  | "separate"
  | "merged"
  | "open";

const STATUS_COLOR: Record<Status, string> = {
  built: C.built,
  gap: C.gap,
  deferred: C.deferred,
  authoritative: C.built,
  reference: C.ref,
  outdated: C.outdated,
  process: C.process,
  separate: C.separate,
  merged: C.built,
  open: C.v1,
};

const Badge: React.FC<{ status: Status; label?: string }> = ({ status, label }) => (
  <span
    style={{
      background: STATUS_COLOR[status] + "22",
      color: STATUS_COLOR[status],
      border: `1px solid ${STATUS_COLOR[status]}55`,
      borderRadius: 999,
      padding: "2px 9px",
      fontSize: 11,
      fontWeight: 700,
      letterSpacing: 0.3,
      textTransform: "uppercase",
      whiteSpace: "nowrap",
    }}
  >
    {label ?? status}
  </span>
);

const Card: React.FC<{ children: React.ReactNode; accent?: string; style?: React.CSSProperties }> = ({
  children,
  accent,
  style,
}) => (
  <div
    style={{
      background: C.panel2,
      border: `1px solid ${C.border}`,
      borderLeft: accent ? `3px solid ${accent}` : `1px solid ${C.border}`,
      borderRadius: 10,
      padding: "12px 14px",
      ...style,
    }}
  >
    {children}
  </div>
);

const Panel: React.FC<{ title: string; subtitle?: string; children: React.ReactNode }> = ({
  title,
  subtitle,
  children,
}) => (
  <section
    style={{
      background: C.panel,
      border: `1px solid ${C.border}`,
      borderRadius: 14,
      padding: 18,
      marginBottom: 18,
    }}
  >
    <div style={{ display: "flex", alignItems: "baseline", gap: 10, marginBottom: 14 }}>
      <h2 style={{ margin: 0, fontSize: 16, color: C.text }}>{title}</h2>
      {subtitle && <span style={{ color: C.dim, fontSize: 13 }}>{subtitle}</span>}
    </div>
    {children}
  </section>
);

// ─────────────────────────────────────────────────────────────────────────────
// Data — the 14 editor sections (9 v1 + 5 v2)
// ─────────────────────────────────────────────────────────────────────────────
type SectionRow = {
  name: string;
  scope: "v1" | "v2";
  status: Status;
  milestone: string;
  jira: string;
  note: string;
};

const SECTIONS: SectionRow[] = [
  { name: "Basic Info", scope: "v1", status: "built", milestone: "M4", jira: "OGC-748", note: "Domain + AMR + status flags" },
  { name: "Sample & Results", scope: "v1", status: "built", milestone: "M5", jira: "OGC-749", note: "Multi-component + interpretations + inline UoM" },
  { name: "Methods", scope: "v1", status: "built", milestone: "M6", jira: "OGC-750", note: "Link + inline-create + shortcodes" },
  { name: "Ranges", scope: "v1", status: "built", milestone: "M7", jira: "OGC-751", note: "Structured view + coverage gate (Table/Visual = v2)" },
  { name: "Sample Storage", scope: "v1", status: "built", milestone: "M8", jira: "OGC-752", note: "Conditions/handling/disposal (audit-write = v2)" },
  { name: "Display Order", scope: "v1", status: "built", milestone: "M12", jira: "OGC-756", note: "Per-sample-type drag + keyboard arrows + auto-save" },
  { name: "Terminology", scope: "v1", status: "built", milestone: "M10", jira: "OGC-754", note: "Source/Code/Relationship (Display Name = v2.4 vision)" },
  { name: "Analyzers (read-only)", scope: "v1", status: "built", milestone: "M11", jira: "OGC-755", note: "Read-only derived list + info card" },
  { name: "Panels", scope: "v1", status: "gap", milestone: "M9", jira: "OGC-753", note: "GAP: built add/position/remove; inline-create + drag-preview deferred (v1 Jira Story 7 wants them)" },
  { name: "Labels", scope: "v2", status: "deferred", milestone: "—", jira: "v2 epic", note: "4 fixed presets per test" },
  { name: "Reagents", scope: "v2", status: "deferred", milestone: "—", jira: "v2 epic", note: "Needs test_reagent_link (built in v2)" },
  { name: "Alerts", scope: "v2", status: "deferred", milestone: "—", jira: "v2 epic", note: "Per-test rules via Notification system" },
  { name: "Reflex & Calc", scope: "v2", status: "deferred", milestone: "—", jira: "OGC-764", note: "Read-only cross-links to Master Lists" },
  { name: "Compliance", scope: "v2", status: "deferred", milestone: "—", jira: "v2 / S-01", note: "ENV/VECTOR threshold authoring" },
];

const FOUNDATION: { name: string; status: Status; note: string }[] = [
  { name: "Editor scaffold + URL routing", status: "built", note: "Single SideNav-routed surface (#3504)" },
  { name: "Test List View + filters", status: "built", note: "Domain/AMR/status + URL state" },
  { name: "Permissions + standard states", status: "built", note: "ADMIN-gated; empty/loading/error" },
  { name: "Activation Acknowledgment", status: "built", note: "Coverage-gate modal (M7)" },
  { name: "Heavy schema migrations", status: "built", note: "Liquibase 040–044 (merged)" },
  { name: "Legacy admin decommission (M-DC)", status: "deferred", note: "OGC-940 — NOT started; gated on review" },
];

// ─────────────────────────────────────────────────────────────────────────────
// Data — openelis-work design artifacts
// ─────────────────────────────────────────────────────────────────────────────
type DocRow = { file: string; what: string; status: Status };

const DOC_BUCKETS: { title: string; tint: string; docs: DocRow[] }[] = [
  {
    title: "Canonical v2.5 set — use these",
    tint: C.built,
    docs: [
      { file: "test-catalog/test-catalog-requirements-v2.5.md", what: "Current FRS (v1/v2 sliced)", status: "authoritative" },
      { file: "test-catalog/test-catalog-v2.5-v1-jira.md", what: "v1 epic OGC-746 + OGC-747–757", status: "authoritative" },
      { file: "test-catalog/test-catalog-v2.5-v2-jira.md", what: "v2 epic + stories (DRAFT, not filed)", status: "reference" },
      { file: "test-catalog/test-catalog-preview-v2.5-v1.html", what: "Interactive v1 preview mock", status: "reference" },
      { file: "test-catalog/test-catalog-v2.5-epic-restructure.md", what: "Epic reorganization plan", status: "process" },
      { file: "test-catalog/test-catalog-v2.5-design-critique.md", what: "Design critique", status: "process" },
      { file: "test-catalog/test-catalog-v2.5-staging-review.md", what: "Staging review", status: "process" },
    ],
  },
  {
    title: "Full-vision reference (not outdated)",
    tint: C.ref,
    docs: [
      { file: "test-catalog/test-catalog-requirements-v2.4.md", what: "~3,275-line full vision; v2 quotes it", status: "reference" },
      { file: "test-catalog.{md,jsx,html}", what: "v2.4-era consolidated mock (top-level)", status: "reference" },
      { file: "test-catalog-microbiology-workflow-attribute.{md,html}", what: "Niche microbiology addendum", status: "reference" },
    ],
  },
  {
    title: "Outdated — ignore",
    tint: C.outdated,
    docs: [
      { file: "upload/processed/test-catalog-requirements-v2.1.md", what: "Original v2.1 FRS", status: "outdated" },
      { file: "upload/processed/test-catalog-mockup-v2.1.jsx", what: "Original v2.1 mockup", status: "outdated" },
    ],
  },
  {
    title: "Separate standalone admin pages (NOT the unified editor)",
    tint: C.separate,
    docs: [
      { file: "panel.{jsx,md}", what: "Standalone Panel Management redesign", status: "separate" },
      { file: "methods.{jsx,md}", what: "Methods admin", status: "separate" },
      { file: "range-editor.{jsx,md}", what: "Range editor", status: "separate" },
      { file: "result-options.{jsx,md}", what: "Result options", status: "separate" },
      { file: "sample-type-management / -domain-classification / -multi-domain-addendum", what: "Sample type admin", status: "separate" },
      { file: "concept-mapping-multi-coding.{jsx,md}", what: "Terminology / coding", status: "separate" },
      { file: "compliance-standards-admin.{jsx,md}", what: "Compliance surface (v2 section)", status: "separate" },
      { file: "reporting-ranges-by-method.{jsx,md}", what: "Reporting ranges", status: "separate" },
      { file: "lab-units / data-dictionary / test-accreditation / catalog-subscription", what: "Other related admin surfaces", status: "separate" },
    ],
  },
];

// ─────────────────────────────────────────────────────────────────────────────
// Data — PRs
// ─────────────────────────────────────────────────────────────────────────────
const PRS: { id: string; title: string; status: Status; detail: string }[] = [
  { id: "#3709", title: "M0–M4 (schema, shell, list, Basic Info)", status: "merged", detail: "merged 2026-06-13 → develop" },
  { id: "#3714", title: "M0–M4 review follow-ups", status: "merged", detail: "merged 2026-06-15 → develop" },
  { id: "#3716", title: "M5–M12 (all remaining v1 sections + demo)", status: "open", detail: "OPEN → develop · the v1 PR · title stale (says M5–M9)" },
  { id: "M-DC", title: "Legacy admin decommission (OGC-940)", status: "deferred", detail: "not started; gated on checkpoint review" },
];

// ─────────────────────────────────────────────────────────────────────────────
// Component
// ─────────────────────────────────────────────────────────────────────────────
const LEGEND: { label: string; status: Status }[] = [
  { label: "Built", status: "built" },
  { label: "Gap (v1, under-built)", status: "gap" },
  { label: "Deferred to v2", status: "deferred" },
  { label: "Authoritative", status: "authoritative" },
  { label: "Reference", status: "reference" },
  { label: "Outdated", status: "outdated" },
  { label: "Separate page", status: "separate" },
];

const SectionGrid: React.FC<{ scope: "v1" | "v2" }> = ({ scope }) => (
  <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 10 }}>
    {SECTIONS.filter((s) => s.scope === scope).map((s) => (
      <Card key={s.name} accent={STATUS_COLOR[s.status]}>
        <div style={{ display: "flex", justifyContent: "space-between", gap: 8, alignItems: "center" }}>
          <strong style={{ fontSize: 13.5 }}>{s.name}</strong>
          <Badge status={s.status} label={s.status === "built" ? `✓ ${s.milestone}` : s.status === "gap" ? `⚠ ${s.milestone}` : "v2"} />
        </div>
        <div style={{ color: C.dim, fontSize: 11.5, marginTop: 5 }}>{s.note}</div>
        <div style={{ color: C.dim, fontSize: 10.5, marginTop: 4, opacity: 0.7 }}>{s.jira}</div>
      </Card>
    ))}
  </div>
);

export default function TestCatalogScopeInventory(): JSX.Element {
  return (
    <div
      style={{
        background: C.bg,
        color: C.text,
        fontFamily: "Inter, system-ui, sans-serif",
        padding: 24,
        minHeight: "100vh",
        boxSizing: "border-box",
      }}
    >
      {/* Header */}
      <header style={{ marginBottom: 18 }}>
        <h1 style={{ margin: 0, fontSize: 22 }}>OGC-949 · Test Catalog Management — Scope & Artifact Inventory</h1>
        <p style={{ color: C.dim, margin: "6px 0 0", fontSize: 13 }}>
          What's v1 vs v2 · built vs specced vs outdated · which openelis-work docs are authoritative. Snapshot 2026-06-16.
        </p>
        <div style={{ display: "flex", flexWrap: "wrap", gap: 8, marginTop: 12 }}>
          {LEGEND.map((l) => (
            <Badge key={l.label} status={l.status} label={l.label} />
          ))}
        </div>
      </header>

      {/* Version lineage */}
      <Panel title="Version lineage" subtitle="why there are so many files">
        <div style={{ display: "flex", gap: 10, flexWrap: "wrap", alignItems: "stretch" }}>
          <Card accent={C.outdated} style={{ flex: 1, minWidth: 200 }}>
            <Badge status="outdated" label="v2.1" />
            <div style={{ marginTop: 6, fontSize: 12.5 }}>Original. Archived in <code>upload/processed/</code>. Ignore.</div>
          </Card>
          <div style={{ alignSelf: "center", color: C.dim }}>→</div>
          <Card accent={C.ref} style={{ flex: 1, minWidth: 200 }}>
            <Badge status="reference" label="v2.4" />
            <div style={{ marginTop: 6, fontSize: 12.5 }}>Full vision (~3,275 lines, all 14 sections). The reference v2 quotes from.</div>
          </Card>
          <div style={{ alignSelf: "center", color: C.dim }}>→</div>
          <Card accent={C.built} style={{ flex: 1, minWidth: 200 }}>
            <Badge status="authoritative" label="v2.5" />
            <div style={{ marginTop: 6, fontSize: 12.5 }}>
              Current planning. Slices v2.4 into shipped <b>v1</b> + deferred <b>v2</b>. <b>Authoritative scope.</b>
            </div>
          </Card>
        </div>
      </Panel>

      {/* Sections board */}
      <Panel title="Editor sections" subtitle="SideNav grows 9 (v1) → 14 (v2); v2 entries are hidden in v1, not stubs">
        <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 18 }}>
          <div>
            <h3 style={{ color: C.v1, fontSize: 13, margin: "0 0 10px" }}>● v1 — shipped on #3716 (9 sections)</h3>
            <SectionGrid scope="v1" />
          </div>
          <div>
            <h3 style={{ color: C.v2, fontSize: 13, margin: "0 0 10px" }}>● v2 — not built (correct) (5 sections)</h3>
            <SectionGrid scope="v2" />
          </div>
        </div>
      </Panel>

      {/* Foundation */}
      <Panel title="v1 foundation (beyond the sections)">
        <div style={{ display: "grid", gridTemplateColumns: "repeat(3, 1fr)", gap: 10 }}>
          {FOUNDATION.map((f) => (
            <Card key={f.name} accent={STATUS_COLOR[f.status]}>
              <div style={{ display: "flex", justifyContent: "space-between", gap: 8 }}>
                <strong style={{ fontSize: 12.5 }}>{f.name}</strong>
                <Badge status={f.status} label={f.status === "built" ? "✓" : "todo"} />
              </div>
              <div style={{ color: C.dim, fontSize: 11.5, marginTop: 5 }}>{f.note}</div>
            </Card>
          ))}
        </div>
      </Panel>

      {/* The gap */}
      <Panel title="⚠ The one real v1 gap">
        <Card accent={C.gap}>
          <strong>Panels (M9 / OGC-753, Story 7)</strong>
          <div style={{ color: C.dim, fontSize: 12.5, marginTop: 6, lineHeight: 1.5 }}>
            Built: add-to-panel typeahead, per-panel numeric position, remove. <br />
            <b style={{ color: C.gap }}>Missing vs v1 Jira:</b> inline "Create New Panel" (name-only) + the drag-drop/keyboard
            position preview ("← THIS TEST" among siblings). Deferred to a Master-Lists pointer because an OE panel needs
            orderable scaffolding (localization + workplan/result/validation modules + role modules + sample-type link) a
            name-only create can't produce. <b>Decision needed:</b> close the gap (build the create flow) or amend the v1 Jira.
          </div>
        </Card>
      </Panel>

      {/* PRs */}
      <Panel title="Pull requests">
        <div style={{ display: "grid", gridTemplateColumns: "repeat(2, 1fr)", gap: 10 }}>
          {PRS.map((p) => (
            <Card key={p.id} accent={STATUS_COLOR[p.status]}>
              <div style={{ display: "flex", justifyContent: "space-between", gap: 8, alignItems: "center" }}>
                <strong style={{ fontSize: 13 }}>
                  {p.id} · {p.title}
                </strong>
                <Badge status={p.status} />
              </div>
              <div style={{ color: C.dim, fontSize: 11.5, marginTop: 5 }}>{p.detail}</div>
            </Card>
          ))}
        </div>
      </Panel>

      {/* Doc buckets */}
      <Panel title="openelis-work design artifacts" subtitle="designs/admin-config/ — sorted by authority">
        <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 14 }}>
          {DOC_BUCKETS.map((b) => (
            <div key={b.title}>
              <h3 style={{ color: b.tint, fontSize: 12.5, margin: "0 0 8px" }}>{b.title}</h3>
              <div style={{ display: "flex", flexDirection: "column", gap: 6 }}>
                {b.docs.map((d) => (
                  <Card key={d.file} accent={STATUS_COLOR[d.status]} style={{ padding: "8px 11px" }}>
                    <div style={{ display: "flex", justifyContent: "space-between", gap: 8, alignItems: "center" }}>
                      <code style={{ fontSize: 11, color: C.text }}>{d.file}</code>
                      <Badge status={d.status} />
                    </div>
                    <div style={{ color: C.dim, fontSize: 11, marginTop: 3 }}>{d.what}</div>
                  </Card>
                ))}
              </div>
            </div>
          ))}
        </div>
      </Panel>

      {/* Evidence footer */}
      <Panel title="v1 build evidence (M9–M12, this session)">
        <div style={{ display: "flex", gap: 24, flexWrap: "wrap", fontSize: 12.5, color: C.dim }}>
          <span><b style={{ color: C.text }}>173</b> admin vitest pass</span>
          <span><b style={{ color: C.text }}>19</b> backend integration tests (real Postgres)</span>
          <span><b style={{ color: C.text }}>18</b> security/auth cases</span>
          <span><b style={{ color: C.text }}>Live-verified</b> — WAR rebuilt, all 4 sections clicked + Terminology save round-trips</span>
          <span><b style={{ color: C.text }}>Video + screenshots</b> in <code>frontend/e2e-evidence/</code></span>
        </div>
      </Panel>

      <footer style={{ color: C.dim, fontSize: 11, marginTop: 8, opacity: 0.7 }}>
        Sources: openelis-work test-catalog v2.5 v1/v2 Jira · SpecKit spec/plan/tasks/data-model · Liquibase 042/043 · PR #3716 code.
      </footer>
    </div>
  );
}
