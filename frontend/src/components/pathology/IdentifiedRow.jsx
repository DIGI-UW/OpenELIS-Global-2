import React from "react";
import { Button, Toggle } from "@carbon/react";
import { Add, Printer, Subtract } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import StatusBadge from "../caseView/StatusBadge";
import {
  blockIdentifier,
  isDeactivatedRow,
  isUnsavedRow,
  objectNaming,
  rowObjectName,
  slideIdentifier,
} from "./pathologyRows";

/**
 * The chrome every identified row shares, whichever object it stands for:
 * who the row is, what can still happen to it, and the line under the list.
 */

export const RowIdentity = ({
  identifier,
  barcode,
  stateBadge,
  deactivated,
}) => {
  const intl = useIntl();

  return (
    <div className="pathology-case-view__row-identity">
      <span className="pathology-case-view__row-designation">
        {identifier ||
          intl.formatMessage({ id: "pathology.label.designationPending" })}
      </span>
      {barcode && (
        <span className="pathology-case-view__row-barcode">
          <span className="cds--visually-hidden">
            {intl.formatMessage({ id: "label.barcode" }) + ": "}
          </span>
          {barcode}
        </span>
      )}
      {stateBadge && <StatusBadge {...stateBadge} />}
      {deactivated && (
        <StatusBadge kind="none" textKey="caseView.badge.deactivated" />
      )}
    </div>
  );
};

/**
 * An unsaved row can simply be dropped; a saved one is kept for the record
 * and can only be deactivated; a deactivated one has nothing left to do.
 */
export const RowLifecycleAction = ({
  unsaved,
  deactivated,
  readOnly,
  deactivateLocked,
  deactivateLockedReason,
  removeLabel,
  deactivateLabel,
  onRemove,
  onDeactivate,
}) => {
  if (unsaved) {
    return (
      <Button
        kind="ghost"
        size="md"
        renderIcon={Subtract}
        aria-label={removeLabel}
        disabled={readOnly}
        onClick={onRemove}
      >
        <FormattedMessage id="common.remove" />
      </Button>
    );
  }
  if (deactivated || readOnly) {
    return null;
  }
  return (
    <Button
      kind="ghost"
      size="md"
      aria-label={deactivateLabel}
      disabled={deactivateLocked}
      title={deactivateLocked ? deactivateLockedReason : undefined}
      onClick={onDeactivate}
    >
      <FormattedMessage id="common.deactivate" />
    </Button>
  );
};

/**
 * One cassette, block or slide on a list. Its fields and actions render from
 * `{ objectName, hiddenName, disabled }`, so every name and lock on the row
 * agrees. `hiddenName` goes inside a field's label, so the field is announced
 * with the object it belongs to while the row shows the object only once.
 * A list that repeats rows another list already removes and deactivates
 * passes `withLifecycleAction={false}`, so each such button exists once.
 */
const IdentifiedRow = ({
  kind,
  row,
  position,
  readOnly,
  showDeactivated,
  deactivateLocked,
  deactivateLockedReason,
  stateBadge,
  onRemove,
  onDeactivate,
  withLifecycleAction = true,
  actions,
  children,
}) => {
  const intl = useIntl();
  const deactivated = isDeactivatedRow(row);
  if (deactivated && !showDeactivated) {
    return null;
  }
  const objectName = rowObjectName(intl, kind, row, position);
  const naming = objectNaming(kind, row);
  const slots = {
    objectName,
    hiddenName: (
      <span className="cds--visually-hidden">{" " + objectName}</span>
    ),
    disabled: readOnly || deactivated,
  };

  return (
    <div
      className={
        "pathology-case-view__row" +
        (deactivated ? " pathology-case-view__row--deactivated" : "")
      }
    >
      <RowIdentity
        identifier={
          kind === "slide" ? slideIdentifier(row) : blockIdentifier(row)
        }
        barcode={row.barcode}
        stateBadge={stateBadge}
        deactivated={deactivated}
      />
      {children(slots)}
      <div className="pathology-case-view__row-actions">
        {actions?.(slots)}
        {withLifecycleAction && (
          <RowLifecycleAction
            unsaved={isUnsavedRow(row)}
            deactivated={deactivated}
            readOnly={readOnly}
            deactivateLocked={deactivateLocked}
            deactivateLockedReason={deactivateLockedReason}
            removeLabel={intl.formatMessage(
              { id: "common.removeSelection" },
              { name: objectName },
            )}
            deactivateLabel={intl.formatMessage(
              { id: naming.actionKey },
              naming.values,
            )}
            onRemove={onRemove}
            onDeactivate={(event) =>
              onDeactivate({ kind, row, launcher: event.currentTarget })
            }
          />
        )}
      </div>
    </div>
  );
};

export default IdentifiedRow;

/**
 * The line under a list. Add and Print render only for a list that is given
 * their labels; the Show deactivated switch only once the list holds a
 * deactivated row. With none of the three there is no line at all.
 */
export const ListToolbar = ({
  addLabelKey,
  onAdd,
  addDisabled,
  addTitle,
  printLabelKey,
  printUrl,
  canPrint,
  deactivatedCount,
  showDeactivated,
  onToggleDeactivated,
  toggleId,
}) => {
  const intl = useIntl();

  if (!addLabelKey && !printLabelKey && !(deactivatedCount > 0)) {
    return null;
  }

  return (
    <div className="pathology-case-view__list-toolbar">
      {addLabelKey && (
        <Button
          size="md"
          kind="tertiary"
          renderIcon={Add}
          disabled={addDisabled}
          title={addTitle}
          onClick={onAdd}
        >
          <FormattedMessage id={addLabelKey} />
        </Button>
      )}
      {printLabelKey && (
        <Button
          size="md"
          kind="ghost"
          renderIcon={Printer}
          disabled={!canPrint}
          title={
            canPrint
              ? undefined
              : intl.formatMessage({ id: "pathology.locked.nothingToPrint" })
          }
          onClick={() => window.open(printUrl, "_blank")}
        >
          <FormattedMessage id={printLabelKey} />
        </Button>
      )}
      {deactivatedCount > 0 && (
        <Toggle
          id={toggleId}
          className="pathology-case-view__list-toolbar-end"
          size="sm"
          hideLabel
          labelText={intl.formatMessage({
            id: "caseView.action.showDeactivated",
          })}
          toggled={showDeactivated}
          onToggle={onToggleDeactivated}
        />
      )}
    </div>
  );
};
