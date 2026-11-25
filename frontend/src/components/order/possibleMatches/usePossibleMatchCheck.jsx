import React, { useCallback, useRef, useState } from "react";
import { useIntl } from "react-intl";
import PossibleMatchesDialog from "./PossibleMatchesDialog";
import {
  findPossibleMatches,
  recordCreateAnyway,
} from "../api/orderEntryCleanupApi";

/**
 * Runs the possible-match check when the user creates a new patient, facility
 * or provider (FR-B6a). With no possible matches the record is created at once;
 * otherwise the dialog offers Use this one, or Create new anyway, which is
 * confirmed and recorded before the record is created.
 *
 * Returns `{ check, checking, dialog }`: call
 * `check(kind, params, entered, { onUse, onCreate })` and render `dialog`.
 */
export default function usePossibleMatchCheck() {
  const intl = useIntl();
  const [state, setState] = useState({
    open: false,
    kind: "",
    matches: [],
    failed: false,
  });
  const [checking, setChecking] = useState(false);
  const [checkCount, setCheckCount] = useState(0);
  const [recording, setRecording] = useState(false);
  const [recordError, setRecordError] = useState("");
  const pending = useRef(null);

  const close = useCallback(() => {
    pending.current = null;
    setRecordError("");
    setState((previous) => ({ ...previous, open: false }));
  }, []);

  const check = useCallback((kind, params, entered, handlers) => {
    pending.current = { kind, entered, ...handlers };
    setCheckCount((count) => count + 1);
    setChecking(true);
    setRecordError("");
    findPossibleMatches(kind, params)
      .then((matches) => {
        setChecking(false);
        if (matches.length === 0) {
          pending.current = null;
          handlers.onCreate();
          return;
        }
        setState({ open: true, kind, matches, failed: false });
      })
      .catch(() => {
        setChecking(false);
        setState({ open: true, kind, matches: [], failed: true });
      });
  }, []);

  const use = (match) => {
    const handlers = pending.current;
    close();
    handlers?.onUse(match);
  };

  const createAnyway = () => {
    const handlers = pending.current;
    if (!handlers) {
      return;
    }
    if (state.failed) {
      close();
      handlers.onCreate();
      return;
    }
    setRecording(true);
    setRecordError("");
    recordCreateAnyway(
      handlers.kind,
      handlers.entered,
      state.matches.map((match) => ({
        id: match.id,
        matchedOn: match.matchedOn,
      })),
    )
      .then(() => {
        setRecording(false);
        close();
        handlers.onCreate();
      })
      .catch(() => {
        setRecording(false);
        setRecordError(
          intl.formatMessage({ id: "search.possibleMatches.recordFailed" }),
        );
      });
  };

  const dialog = (
    <PossibleMatchesDialog
      key={checkCount}
      open={state.open}
      kind={state.kind}
      matches={state.matches}
      failed={state.failed}
      recording={recording}
      recordError={recordError}
      onUse={use}
      onCreateAnyway={createAnyway}
      onCancel={close}
    />
  );

  return { check, checking, dialog };
}
