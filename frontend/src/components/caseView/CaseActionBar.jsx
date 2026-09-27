import React, { useState } from "react";
import { Button } from "@carbon/react";
import { Prompt } from "react-router-dom";
import { FormattedMessage, useIntl } from "react-intl";
import useUnsavedChangesGuard from "./useUnsavedChangesGuard";
import "./caseView.scss";

// This codebase is still on React 17, which has no useId hook, so a disabled
// primary's reason id is minted once per mounted bar from a lazy state
// initialiser. The counter only needs to make ids unique among bars mounted
// at the same time; it is never persisted or compared across renders.
let nextReasonId = 0;

/**
 * The full-width bar every anatomic-pathology case view keeps below its
 * detail grid.
 *
 * The action group is exactly three buttons and never four. A screen with
 * several possible next steps says so by changing what the primary button is
 * labelled, not by adding a competing button beside it, so this component
 * accepts only one primary action and renders nothing when none is given.
 *
 * The primary has no disabled prop. Whether it is disabled is derived from
 * whether a reason was given: pass disabledReasonKey and the button is
 * disabled and explains itself; leave it out and the button is enabled. An
 * earlier design let a caller disable the button and separately decide
 * whether to show a reason, and that gap produced a real defect: a tooltip
 * that named one condition while the code tested another. A disabled button
 * with no reason at all is the same failure in a shorter form, so this shape
 * makes it structurally impossible, not merely documented, by removing the
 * only way to disable the button without also saying why.
 *
 * The reason is put on the button's title and is also placed in text an
 * assistive technology can reach through aria-describedby: a disabled button
 * is not focusable, so a title alone would never reach a screen-reader user.
 * The reason disappears the moment the primary is given no
 * disabledReasonKey, so it can never be mistaken for boilerplate that is
 * simply always there.
 *
 * The unsaved-changes warning is an action-bar concern, not something each
 * screen has to remember to wire up: rendering this bar is what turns both
 * halves of it on. useUnsavedChangesGuard catches the browser discarding the
 * document, which is a reload or a closed tab. The router Prompt below
 * catches a move to another screen of the application, which is how a
 * pathologist actually loses a half-written case and is the half a
 * beforeunload listener can never see, because a single-page navigation
 * never unloads anything.
 *
 * That Prompt means this bar must be rendered inside a react-router Router.
 * Every screen that shows it is a routed screen, so the requirement costs
 * those screens nothing, but a bar dropped into an unrouted tree throws
 * rather than quietly losing its warning, and the message it throws names
 * the Prompt and not this component.
 *
 * primary shape: { labelKey, disabledReasonKey, disabledReasonValues,
 * onClick }. It is always rendered as the Carbon primary button kind, so the
 * bar's one-primary hierarchy cannot be undermined by a caller choosing a
 * different kind for it.
 */
const CaseActionBar = ({
  status = null,
  dirty = false,
  onDiscard,
  onSaveDraft,
  primary,
}) => {
  const intl = useIntl();
  const [reasonId] = useState(() => `case-action-bar-reason-${nextReasonId++}`);

  useUnsavedChangesGuard(dirty);

  const primaryDisabled = Boolean(primary?.disabledReasonKey);
  const reasonText = primaryDisabled
    ? intl.formatMessage(
        { id: primary.disabledReasonKey },
        primary.disabledReasonValues,
      )
    : null;

  return (
    <div className="case-view__action-bar">
      {/*
        The Prompt is rendered from the bar's own markup rather than returned
        by useUnsavedChangesGuard. A hook that handed back an element would
        hide two things from the screens that call it: that they are now
        rendering something, and that the something demands a Router around
        them. Keeping it here puts both facts in the component a reader is
        already looking at, and leaves the hook a plain effect with no
        opinion about the tree it sits in.
      */}
      <Prompt
        when={dirty}
        message={intl.formatMessage({ id: "caseView.banner.unsavedChanges" })}
      />
      <div className="case-view__action-bar-status">
        <span>
          <FormattedMessage id="common.status" />
        </span>
        {status}
        {dirty && (
          <span className="case-view__locked-hint">
            <FormattedMessage id="caseView.label.unsavedChanges" />
          </span>
        )}
      </div>
      <div className="case-view__action-bar-actions">
        <Button kind="ghost" disabled={!dirty} onClick={onDiscard}>
          <FormattedMessage id="caseView.action.discard" />
        </Button>
        <Button kind="secondary" onClick={onSaveDraft}>
          <FormattedMessage id="caseView.action.saveDraft" />
        </Button>
        {primary && (
          <Button
            kind="primary"
            disabled={primaryDisabled}
            onClick={primary.onClick}
            title={primaryDisabled ? reasonText : undefined}
            aria-describedby={primaryDisabled ? reasonId : undefined}
          >
            <FormattedMessage id={primary.labelKey} />
          </Button>
        )}
        {primaryDisabled && (
          <span id={reasonId} className="case-view__locked-hint">
            {reasonText}
          </span>
        )}
      </div>
    </div>
  );
};

export default CaseActionBar;
