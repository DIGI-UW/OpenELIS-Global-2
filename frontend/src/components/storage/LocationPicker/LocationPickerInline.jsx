import React, { useEffect } from "react";
import { Button } from "@carbon/react";
import { Search, Add } from "@carbon/icons-react";
import useLocationPicker, { LEVEL_ORDER } from "./useLocationPicker";
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
 *   - onChange(state) — fired whenever picker state changes; the host
 *     persists `state.selection` + `state.position` with the order form
 *
 * State lives in the useLocationPicker reducer; this shell just toggles
 * the mode and forwards select-events to the reducer.
 */
export default function LocationPickerInline({ initialSelection, onChange }) {
  const [state, dispatch] = useLocationPicker(
    initialSelection
      ? {
          initialAssignment: {
            selection: initialSelection,
            position: null,
          },
        }
      : {},
  );

  // Forward meaningful state changes to the host form. Only selection
  // and position are relevant to the caller — omitting the full `state`
  // object from deps avoids spamming onChange on every keystroke or
  // mode toggle.
  useEffect(() => {
    if (onChange) onChange(state);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [state.selection, state.position, onChange]);

  const setLevel = (level, value) =>
    dispatch({ type: "SET_LEVEL", level, value });

  const handleSearchSelect = (result) => {
    // The flat search returns one location node; the picker doesn't know
    // its parents from this payload alone. The simplest mapping: set the
    // single level the result belongs to (using the result's `type`),
    // letting the user fill in deeper levels via the create-mode cascade
    // if needed. This trades a click for not pummelling the API to walk
    // the parent chain on every selection.
    if (result && result.type && LEVEL_ORDER.includes(result.type)) {
      setLevel(result.type, { id: result.id, name: result.name });
    }
  };

  const summary = LEVEL_ORDER.map((lvl) => state.selection[lvl]?.name)
    .filter(Boolean)
    .join(" > ");

  return (
    <div className="storage-location-picker-inline">
      {summary && (
        <div className="storage-location-picker-inline-summary">{summary}</div>
      )}
      {state.mode === "search" ? (
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
          />
          <Button
            kind="ghost"
            size="sm"
            renderIcon={Add}
            onClick={() => dispatch({ type: "SET_MODE", mode: "create" })}
          >
            Create new location
          </Button>
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
            Back to search
          </Button>
        </>
      )}
    </div>
  );
}
