import React, { useState } from "react";
import { AccordionItem } from "@carbon/react";
import { useIntl } from "react-intl";
import { SECTION_STATE } from "./sectionState";
import StatusBadge from "./StatusBadge";
import "./caseView.scss";

/**
 * One numbered work section of an anatomic-pathology case view.
 *
 * Must be rendered as a child of a Carbon <Accordion>: it returns an
 * AccordionItem, which renders as an <li> and depends on the parent's
 * accordion context.
 *
 * A section is never removed from the DOM because of the case's status or
 * the viewer's role: a step nobody can act on yet is still part of the
 * record, so it is disabled in place and made to say why, rather than
 * disappearing and leaving the workflow's shape unclear to whoever opens the
 * case next. "Locked" on its own is never an acceptable reason; the hint
 * text always names the actual unlock condition. That reason is carried in
 * the section header only, never repeated in the body: Carbon only marks an
 * AccordionItem's wrapper active, which is what makes its content visible,
 * when the item is both open and not disabled, so a disabled section's body
 * can never be shown to explain itself. The header is where the reason is
 * always visible, disabled or not.
 *
 * State is a prop, not something this component figures out. It is produced
 * by deriveSectionState (see sectionState.js) from the case's current status,
 * never stored on the section itself, so the section and the rest of the
 * screen can never disagree about why a step is or is not available.
 *
 * The COMPLETE state renders the same markup as a collapsed, open section:
 * there is no separate "complete" presentation beyond whatever badge the
 * caller supplies.
 *
 * The optional `id` is what a progress rail scrolls to: a screen rendering a
 * rail alongside its sections should give each section the same id it gives
 * the matching rail item.
 *
 * Carbon's AccordionItem already renders its header as a native
 * <button type="button"> with aria-expanded, and a disabled AccordionItem is
 * already dimmed, already shows cursor: not-allowed, and is already out of
 * tab order. None of that is reimplemented here: there is no hand-rolled
 * role="button" element and no explicit tabIndex.
 */
const CaseSection = ({
  number,
  titleKey,
  state = SECTION_STATE.OPEN,
  badge = null,
  lockedHintKey,
  lockedHintValues,
  defaultOpen,
  id,
  children,
}) => {
  const intl = useIntl();

  const isDisabled = state === SECTION_STATE.DISABLED;

  // Seeded once at mount and never recomputed: Carbon's AccordionItem
  // re-syncs its own open/closed state whenever this open prop's value
  // changes, so recomputing it from `state` on every render would snap a
  // section shut (or open) the moment the case advances past its stage,
  // discarding whatever the user had deliberately expanded or collapsed.
  const [isOpen] = useState(() => defaultOpen ?? state === SECTION_STATE.OPEN);

  const hintText = isDisabled
    ? intl.formatMessage(
        { id: lockedHintKey ?? "caseView.locked.prerequisite" },
        lockedHintValues,
      )
    : null;

  let headerRight = null;
  if (isDisabled) {
    headerRight = <span className="case-view__locked-hint">{hintText}</span>;
  } else if (badge) {
    headerRight = (
      <StatusBadge
        kind={badge.kind}
        textKey={badge.textKey}
        values={badge.values}
      />
    );
  } else if (state === SECTION_STATE.READ_ONLY) {
    headerRight = <StatusBadge kind="none" textKey="caseView.badge.readOnly" />;
  }

  const sectionTitle = intl.formatMessage(
    { id: "caseView.label.numberedSection" },
    { number, title: intl.formatMessage({ id: titleKey }) },
  );

  const title = (
    <span className="case-view__section-title">
      <span>{sectionTitle}</span>
      {headerRight}
    </span>
  );

  return (
    <AccordionItem id={id} title={title} open={isOpen} disabled={isDisabled}>
      {children}
    </AccordionItem>
  );
};

export default CaseSection;
