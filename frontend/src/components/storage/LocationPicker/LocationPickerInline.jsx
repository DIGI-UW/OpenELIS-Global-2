import React, { useEffect } from "react";
import { Button, TextInput } from "@carbon/react";
import { Search, Add } from "@carbon/icons-react";
import { useIntl } from "react-intl";
import useLocationPicker from "./useLocationPicker";
import { selectionToHierarchicalPath } from "./locationSelectionMapper";
import { searchResultToReplaceAction } from "./searchResultToAction";
import useLatestCallback from "./useLatestCallback";
import SearchField from "./components/SearchField";
import CreateForm from "./components/CreateForm";

/**
 * LocationPickerInline — the picker rendered flat for embedding inside a
 * host form (OrderLabel, AddOrder Sample Type). No modal chrome, no
 * breadcrumb. The host wraps it with whatever surrounding chrome it wants
 * (typically a section heading + the rest of the order form).
 *
 * Props:
 *   - initialSelection?: Selection — pre-fill (e.g., barcode auto-open or
 *     existing-assignment editing context)
 *   - initialPosition?: string — pre-fill the position text input. Hosts
 *     persist position as a flat string (e.g. sampleXml.storageLocation
 *     .positionCoordinate); pass it back on remount so users don't lose
 *     it when switching tabs.
 *   - onChange(state) — fired whenever picker state changes; the host
 *     persists `state.selection` + `state.position` with the order form
 *
 * State lives in the useLocationPicker reducer; this shell just toggles
 * the mode and forwards select-events to the reducer.
 */
export default function LocationPickerInline({
  initialSelection,
  initialPosition,
  onChange,
  allowCreate = true,
}) {
  const intl = useIntl();
  const [state, dispatch] = useLocationPicker(
    initialSelection || initialPosition
      ? {
          initialAssignment: {
            selection: initialSelection || {},
            position: initialPosition
              ? { mode: "text", value: initialPosition }
              : null,
          },
        }
      : {},
  );

  // Forward meaningful state changes to the host form. Only selection
  // and position are relevant to the caller. onChange is routed through
  // a ref (useLatestCallback) so inline arrow callbacks in the parent
  // don't retrigger this effect on every render — the effect fires
  // strictly when selection or position changes.
  const onChangeRef = useLatestCallback(onChange);
  useEffect(() => {
    const cb = onChangeRef.current;
    if (cb) cb(state);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [state.selection, state.position]);

  const setLevel = (level, value) =>
    dispatch({ type: "SET_LEVEL", level, value });

  // Flat search returns a single leaf; replacing the whole selection
  // keeps the state consistent (no stale ancestors from a different
  // branch of the hierarchy).
  const handleSearchSelect = (result) => {
    const action = searchResultToReplaceAction(result);
    if (action) dispatch(action);
  };

  const summary = selectionToHierarchicalPath(state.selection);

  return (
    <div className="storage-location-picker-inline">
      {summary && (
        <div className="storage-location-picker-inline-summary">{summary}</div>
      )}
      {state.mode === "search" || !allowCreate ? (
        <>
          <SearchField
            query={state.searchQuery}
            results={state.searchResults}
            onQueryChange={(q) =>
              dispatch({ type: "SET_SEARCH_QUERY", query: q })
            }
            onResultsChange={(r) =>
              dispatch({ type: "SET_SEARCH_RESULTS", results: r })
            }
            onSelect={handleSearchSelect}
            selectedSelection={state.selection}
          />
          {allowCreate && (
            <Button
              kind="ghost"
              size="sm"
              renderIcon={Add}
              onClick={() => dispatch({ type: "SET_MODE", mode: "create" })}
            >
              {intl.formatMessage({
                id: "storage.picker.createNewLocation",
                defaultMessage: "Create new location",
              })}
            </Button>
          )}
        </>
      ) : (
        <>
          <CreateForm selection={state.selection} onLevelChange={setLevel} />
          <Button
            kind="ghost"
            size="sm"
            renderIcon={Search}
            onClick={() => dispatch({ type: "SET_MODE", mode: "search" })}
          >
            {intl.formatMessage({
              id: "storage.picker.backToSearch",
              defaultMessage: "Back to search",
            })}
          </Button>
        </>
      )}
    </div>
  );
}
