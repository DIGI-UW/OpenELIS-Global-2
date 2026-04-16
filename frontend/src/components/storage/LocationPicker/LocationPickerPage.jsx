import React from "react";
import { Button, TextArea } from "@carbon/react";
import { Search, Add } from "@carbon/icons-react";
import useLocationPicker, { LEVEL_ORDER } from "./useLocationPicker";
import SearchField from "./components/SearchField";
import CreateForm from "./components/CreateForm";

/**
 * LocationPickerPage — full-page picker for the new dedicated route
 * `/Storage/sample-items/:id/manage-location`. Same picker as Inline
 * and Modal, different chrome (page header + breadcrumb above, page-
 * level Cancel/Save buttons below).
 *
 * Props:
 *   - sample: { sampleAccessionNumber, sampleType, status, ... }
 *   - currentLocation?: { selection, position } — movement context
 *   - breadcrumb?: ReactNode — rendered above the heading; usually
 *     `<BreadcrumbNav />` from the route component
 *   - onSave({ selection, position, reason, notes }) — usually an API
 *     call that POSTs to /rest/storage/sample-items/{assign|move} and
 *     navigates back to the listing page
 *   - onCancel() — usually navigates back without saving
 */
export default function LocationPickerPage({
  sample,
  currentLocation,
  breadcrumb,
  onSave,
  onCancel,
}) {
  const isMovement = !!currentLocation;
  const [state, dispatch] = useLocationPicker(
    currentLocation ? { initialAssignment: currentLocation } : {},
  );

  const setLevel = (level, value) =>
    dispatch({ type: "SET_LEVEL", level, value });

  const handleSearchSelect = (result) => {
    if (result && result.type && LEVEL_ORDER.includes(result.type)) {
      setLevel(result.type, { id: result.id, name: result.name });
    }
  };

  const handleSave = () => {
    onSave({
      selection: state.selection,
      position: state.position,
      reason: state.reason,
      notes: state.notes,
    });
  };

  const summary = LEVEL_ORDER.map((lvl) => state.selection[lvl]?.name)
    .filter(Boolean)
    .join(" > ");

  const currentSummary = currentLocation
    ? LEVEL_ORDER.map((lvl) => currentLocation.selection?.[lvl]?.name)
        .filter(Boolean)
        .join(" > ")
    : "";

  return (
    <div className="storage-location-picker-page">
      {breadcrumb}
      <h1>{isMovement ? "Move Sample" : "Assign Storage Location"}</h1>

      <section className="storage-location-picker-page-sample-info">
        <h4>Sample</h4>
        <dl>
          <dt>Accession</dt>
          <dd>{sample.sampleAccessionNumber}</dd>
          <dt>Type</dt>
          <dd>{sample.sampleType}</dd>
          <dt>Status</dt>
          <dd>{sample.status}</dd>
        </dl>
      </section>

      {currentSummary && (
        <section className="storage-location-picker-page-current">
          <h4>Current location</h4>
          <p>{currentSummary}</p>
        </section>
      )}

      <section className="storage-location-picker-page-picker">
        <h4>{isMovement ? "New location" : "Storage location"}</h4>
        {summary && (
          <div className="storage-location-picker-page-summary">{summary}</div>
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
      </section>

      {isMovement && (
        <TextArea
          id="storage-location-picker-page-reason"
          labelText="Reason for move"
          value={state.reason || ""}
          onChange={(e) =>
            dispatch({ type: "SET_REASON", reason: e.target.value })
          }
        />
      )}

      <TextArea
        id="storage-location-picker-page-notes"
        labelText="Notes"
        value={state.notes || ""}
        onChange={(e) => dispatch({ type: "SET_NOTES", notes: e.target.value })}
      />

      <div className="storage-location-picker-page-actions">
        <Button kind="secondary" onClick={onCancel}>
          Cancel
        </Button>
        <Button kind="primary" onClick={handleSave}>
          Save
        </Button>
      </div>
    </div>
  );
}
