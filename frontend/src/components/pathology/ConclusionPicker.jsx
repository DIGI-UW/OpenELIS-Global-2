import React, { useEffect, useRef, useState } from "react";
import { Button, ComboBox, Link } from "@carbon/react";
import { useIntl } from "react-intl";
import { fetchFromOpenElisServer } from "../utils/Utils";
import SelectionChips from "./SelectionChips";
import "./pathologyCaseView.scss";

const SEARCH_URL = "/rest/pathology/conclusionEntries?q=";
const DICTIONARY_PAGE = "/MasterListsPage/DictionaryMenu";
const SEARCH_PAUSE_MS = 250;
const INPUT_ID = "conclusion";
const ADD_REASON_ID = "conclusion-add-diagnosis-reason";

/**
 * A diagnosis as the pathologist reads it: its code first, when it has one,
 * and the word for a critical entry after the name (given by the caller, who
 * holds the translation), so the mark never depends on a colour.
 */
export const conclusionLabel = (item, criticalWord) => {
  if (!item) return "";
  const name = item.code ? `${item.code} ${item.value}` : item.value;
  return item.critical && criticalWord ? `${name} (${criticalWord})` : name;
};

/**
 * FR-13.2: the coded conclusion, searched over the whole diagnosis dictionary
 * by name or code on the server, so an entry beyond the first page, or one an
 * administrator added a moment ago, is found too. Each pick becomes a chip and
 * the box clears for the next one.
 *
 * The server folds case and accents, so the box offers its answer as it
 * stands and does not filter it again. Only the answer to the latest query is
 * shown; until it arrives the previous one stays listed.
 */
const ConclusionPicker = ({
  value = [],
  onChange,
  readOnly,
  canAddDiagnosis,
}) => {
  const intl = useIntl();
  const criticalWord = intl.formatMessage({ id: "common.critical" });
  const labelOf = (item) => conclusionLabel(item, criticalWord);
  const actions = useRef(null);
  const request = useRef(null);
  const timer = useRef(null);
  const lastQuery = useRef(null);
  const [entries, setEntries] = useState([]);
  const [answeredQuery, setAnsweredQuery] = useState(null);
  const [dictionaryEmpty, setDictionaryEmpty] = useState(false);
  const [failed, setFailed] = useState(false);

  const search = (query) => {
    if (query === lastQuery.current) return;
    lastQuery.current = query;
    request.current?.abort();
    const controller = new AbortController();
    request.current = controller;
    fetchFromOpenElisServer(
      SEARCH_URL + encodeURIComponent(query),
      controller.signal,
    )
      .then((answer) => {
        if (controller.signal.aborted) return;
        const items = Array.isArray(answer?.items) ? answer.items : [];
        setEntries(items);
        setAnsweredQuery(query);
        setFailed(false);
        if (query === "") setDictionaryEmpty(items.length === 0);
      })
      .catch((error) => {
        if (controller.signal.aborted || error?.name === "AbortError") return;
        lastQuery.current = null;
        setFailed(true);
      });
  };

  useEffect(() => {
    if (!readOnly) search("");
    return () => {
      clearTimeout(timer.current);
      request.current?.abort();
      lastQuery.current = null;
    };
    // The search runs once each time the picker becomes editable.
  }, [readOnly]);

  // An administrator may add the first diagnosis in another tab, so an empty
  // dictionary is asked about again when this window has focus back.
  useEffect(() => {
    if (readOnly || !dictionaryEmpty) return undefined;
    const askAgain = () => {
      lastQuery.current = null;
      search("");
    };
    window.addEventListener("focus", askAgain);
    return () => window.removeEventListener("focus", askAgain);
  }, [readOnly, dictionaryEmpty]);

  const onInputChange = (text) => {
    clearTimeout(timer.current);
    const query = (text ?? "").trim();
    timer.current = setTimeout(() => search(query), SEARCH_PAUSE_MS);
  };

  const chosenIds = new Set(value.map((item) => String(item.id)));
  const offered = entries.filter((item) => !chosenIds.has(String(item.id)));

  const statusText = () => {
    if (failed) {
      return intl.formatMessage({ id: "pathology.error.diagnosisListFailed" });
    }
    if (dictionaryEmpty) {
      return intl.formatMessage({ id: "pathology.empty.noDiagnoses" });
    }
    if (!answeredQuery) {
      return "";
    }
    if (entries.length === 0) {
      return intl.formatMessage(
        { id: "pathology.empty.noDiagnosisMatch" },
        { query: answeredQuery },
      );
    }
    if (offered.length === 0) {
      return intl.formatMessage(
        { id: "pathology.empty.allMatchesChosen" },
        { query: answeredQuery },
      );
    }
    return "";
  };
  const status = statusText();

  return (
    <div className="pathology-case-view__conclusion-picker">
      <div className="pathology-case-view__field-group">
        <ComboBox
          id={INPUT_ID}
          titleText={intl.formatMessage({
            id: "pathology.label.codedConclusion",
          })}
          placeholder={intl.formatMessage({
            id: "pathology.placeholder.diagnosisSearch",
          })}
          disabled={readOnly || dictionaryEmpty}
          items={readOnly ? [] : offered}
          itemToString={labelOf}
          selectedItem={null}
          downshiftActions={actions}
          shouldFilterItem={() => true}
          onInputChange={onInputChange}
          onChange={({ selectedItem }) => {
            if (!selectedItem) return;
            onChange([...value, selectedItem]);
            actions.current?.setInputValue("");
            document.getElementById(INPUT_ID)?.focus();
          }}
        />
        {!readOnly && (
          <p
            className="cds--form__helper-text pathology-case-view__picker-status"
            role="status"
          >
            {status}
          </p>
        )}
      </div>
      <SelectionChips
        items={value}
        label={labelOf}
        onRemove={(_, position) =>
          onChange(value.filter((__, index) => index !== position))
        }
        readOnly={readOnly}
        testId="findings-conclusion-chips"
        fallbackFocusId={INPUT_ID}
      />
      {!readOnly && (
        <div data-testid="findings-add-diagnosis">
          {canAddDiagnosis ? (
            <Link
              href={DICTIONARY_PAGE}
              target="_blank"
              rel="noopener noreferrer"
            >
              {intl.formatMessage({ id: "pathology.action.addDiagnosis" })}
            </Link>
          ) : (
            <>
              <Button
                kind="ghost"
                size="sm"
                disabled
                aria-describedby={ADD_REASON_ID}
              >
                {intl.formatMessage({ id: "pathology.action.addDiagnosis" })}
              </Button>
              <p id={ADD_REASON_ID} className="cds--form__helper-text">
                {intl.formatMessage({
                  id: "pathology.locked.addDiagnosisAdminOnly",
                })}
              </p>
            </>
          )}
        </div>
      )}
    </div>
  );
};

export default ConclusionPicker;
