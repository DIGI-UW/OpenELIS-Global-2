import React, { useEffect, useRef } from "react";
import { DismissibleTag, Tag } from "@carbon/react";
import { useIntl } from "react-intl";
import "./pathologyCaseView.scss";

/**
 * The chosen items of a picker as a row of chips. Read-only, each is a plain
 * tag whose label carries its full text as a title, for a chip cut short by
 * its width. Otherwise each has a remove button named after its item.
 *
 * A removed chip hands focus to the chip that took its place, or to the last
 * chip when it was the last, or to the element named by `fallbackFocusId`
 * when none is left, so a keyboard user is never dropped to the page.
 *
 * Carbon labels a tag's close button from title, and falls back to
 * dismissTooltipLabel only once the tag's own text has been ellipsised, so
 * both are given or the button reads Carbon's English "Dismiss".
 */
const SelectionChips = ({
  items = [],
  label,
  onRemove,
  readOnly,
  testId,
  fallbackFocusId,
}) => {
  const intl = useIntl();
  const rowRef = useRef(null);
  const focusAfterRemove = useRef(null);

  useEffect(() => {
    const position = focusAfterRemove.current;
    if (position === null) return;
    focusAfterRemove.current = null;
    const buttons = rowRef.current?.querySelectorAll("button") ?? [];
    const next = buttons[Math.min(position, buttons.length - 1)];
    (next ?? document.getElementById(fallbackFocusId))?.focus();
  }, [items]);

  const removeLabel = (item) =>
    intl.formatMessage({ id: "common.removeSelection" }, { name: label(item) });

  const remove = (item, position) => {
    focusAfterRemove.current = position;
    onRemove(item, position);
  };

  return (
    <div
      className="pathology-case-view__chips"
      data-testid={testId}
      ref={rowRef}
    >
      {items.map((item, index) =>
        readOnly ? (
          <Tag key={item.id} type="gray">
            {label(item)}
          </Tag>
        ) : (
          <DismissibleTag
            key={item.id}
            text={label(item)}
            onClose={() => remove(item, index)}
            title={removeLabel(item)}
            dismissTooltipLabel={removeLabel(item)}
          />
        ),
      )}
    </div>
  );
};

export default SelectionChips;
