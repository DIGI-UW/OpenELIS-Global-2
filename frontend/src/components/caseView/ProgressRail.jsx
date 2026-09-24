import React from "react";
import { useIntl } from "react-intl";
import { ProgressIndicator, ProgressStep } from "@carbon/react";

/**
 * The optional left-hand rail a case view shows once it has enough gated
 * sections that a technician needs an overview of the whole case, not just
 * the section in front of them.
 *
 * Built on Carbon's ProgressIndicator rather than a hand-rolled list, so the
 * three anatomic-pathology case views read the same as the sibling case
 * screens that already use it, and so the step states, keyboard behaviour and
 * status icons stay whatever Carbon says they are. What each step claims about
 * the bench, though, is stated per step here rather than derived from the
 * rail's position: see the comment on the index handed to Carbon.
 *
 * A stage this deployment does not track is still listed, disabled, rather
 * than removed: hiding it would make the case's overall shape depend on
 * configuration nobody at the bench can see, and the workflow reads the same
 * from item to item only if every stage is always in the list.
 *
 * A count is never enough on its own. "2" tells nobody which two cassettes are
 * outstanding, so anything pending arrives as a pendingLabel the screen has
 * already composed from the actual rows ("cassette A4 outstanding"). Carbon
 * renders a secondary label inside the step's own button, so that text is both
 * visible and part of the button's accessible name, and it is the step's hover
 * title as well. A step with nothing pending is left without a title, because
 * Carbon's own default there only repeats the label already on screen. The
 * shell specification calls this element a count badge; a Carbon
 * secondary label is used instead because new UI is built from Carbon
 * components by name, and it carries the naming text in more places than a tag
 * beside a visually hidden span did.
 *
 * One piece of Carbon behaviour the adopting screen should expect, because it
 * cannot be corrected from out here: Carbon withholds the button's own click
 * handler from the step it treats as current, while the key handler on that
 * same button still runs. Pressing Enter or Space on the current step
 * therefore navigates, and clicking it does nothing. Every other step
 * responds the same way to both.
 */
const ProgressRail = ({ items = [], currentIndex = 0, onNavigate }) => {
  const intl = useIntl();

  return (
    <nav aria-label={intl.formatMessage({ id: "caseView.label.caseProgress" })}>
      <ProgressIndicator
        vertical
        // Held below every step on purpose. Given a real index, Carbon rewrites
        // each step before it with complete: true and discards what the step
        // itself said, so a stage this deployment does not track would carry a
        // completion checkmark and the assistive word "Complete" beside its own
        // "N/A" as soon as the case moved past it. That is the rail asserting
        // bench work on a stage nobody records. Below every step, Carbon stays
        // in the one branch that preserves both flags, and completion and
        // position are stated per step below instead.
        currentIndex={-1}
        // Wired only when the screen can act on it, so a rail with nowhere to
        // navigate does not render steps that look clickable.
        onChange={
          onNavigate ? (index) => onNavigate(items[index].id) : undefined
        }
      >
        {items.map((item, index) => {
          const {
            id,
            labelKey,
            complete = false,
            notApplicable = false,
            pendingLabel,
          } = item;

          const secondaryLabel =
            pendingLabel ||
            (notApplicable
              ? intl.formatMessage({ id: "caseView.badge.notApplicable" })
              : undefined);

          return (
            <ProgressStep
              key={id}
              label={intl.formatMessage({ id: labelKey })}
              secondaryLabel={secondaryLabel}
              complete={complete}
              current={index === currentIndex}
              disabled={notApplicable}
              // Suppresses Carbon's own step title, which only repeats the
              // label already on screen. It takes effect only because Carbon
              // spreads the caller's remaining props after setting that title,
              // so a reordering there would quietly restore the tooltip.
              title={pendingLabel}
            />
          );
        })}
      </ProgressIndicator>
    </nav>
  );
};

export default ProgressRail;
