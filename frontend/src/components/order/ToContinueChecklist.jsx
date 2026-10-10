import React from "react";
import { Link } from "@carbon/react";
import { useIntl } from "react-intl";

/**
 * ToContinueChecklist - What the step still needs before Save and next
 * (FR-A9). Each missing item is a link that scrolls to and focuses its field.
 * The list updates as the user works and disappears when the step is complete.
 *
 * Items: { id, label, targetId }. `label` is already translated. `targetId`
 * may list fallbacks: the first that is on the page and enabled is focused.
 */
const ToContinueChecklist = ({ nextStep, items = [] }) => {
  const intl = useIntl();
  if (!items.length) {
    return null;
  }
  const heading = intl.formatMessage(
    { id: "order.continue.heading" },
    { step: nextStep },
  );

  const focusable = (element) =>
    element.matches("input, select, textarea, button")
      ? element
      : element.querySelector("input, select, textarea, button");

  const focusTarget = (targetId) => {
    const candidates = [].concat(targetId || []);
    const element = candidates
      .map((id) => document.getElementById(id))
      .find((candidate) => {
        const control = candidate && focusable(candidate);
        return control && !control.matches(":disabled");
      });
    if (!element) return;
    element.scrollIntoView({ behavior: "smooth", block: "center" });
    focusable(element).focus({ preventScroll: true });
  };

  return (
    <div
      className="order-to-continue"
      role="region"
      aria-live="polite"
      aria-label={heading}
      data-testid="to-continue-checklist"
    >
      <strong>{heading}</strong>
      <ul>
        {items.map((item) => (
          <li key={item.id}>
            <Link
              href={
                item.targetId ? `#${[].concat(item.targetId)[0]}` : undefined
              }
              onClick={(event) => {
                event.preventDefault();
                focusTarget(item.targetId);
              }}
            >
              {item.label}
            </Link>
          </li>
        ))}
      </ul>
    </div>
  );
};

export default ToContinueChecklist;
