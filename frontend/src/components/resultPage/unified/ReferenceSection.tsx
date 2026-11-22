import React from "react";

/**
 * OGC-1021 (R2) — one reference-zone section (FR-C3/C4/C5).
 *
 * Collapsed-but-summarized: the header always shows a one-line gist so a
 * closed section still communicates. Open/closed comes from the sticky layout
 * (remembered > auto-open > collapsed); clicking the header records an
 * explicit user choice. A section with no content is not rendered — the
 * caller enforces FR-C5 by not mounting it.
 */
interface ReferenceSectionProps {
  sectionId: string;
  title: React.ReactNode;
  summary: React.ReactNode;
  open: boolean;
  autoOpened?: boolean;
  autoOpenHint?: React.ReactNode;
  onToggle: (open: boolean) => void;
  children: React.ReactNode;
}

const ReferenceSection: React.FC<ReferenceSectionProps> = ({
  sectionId,
  title,
  summary,
  open,
  autoOpened,
  autoOpenHint,
  onToggle,
  children,
}) => (
  <div className="unifiedRefSection" data-testid={`ref-section-${sectionId}`}>
    <button
      type="button"
      className="unifiedRefSectionHeader"
      aria-expanded={open}
      onClick={() => onToggle(!open)}
    >
      <span className="unifiedRefSectionChevron">{open ? "▾" : "▸"}</span>
      <span className="unifiedRefSectionTitle">{title}</span>
      {!open && <span className="unifiedRefSectionSummary">{summary}</span>}
      {open && autoOpened && autoOpenHint && (
        <span className="unifiedRefSectionSummary">{autoOpenHint}</span>
      )}
    </button>
    {open && <div className="unifiedRefSectionBody">{children}</div>}
  </div>
);

export default ReferenceSection;
