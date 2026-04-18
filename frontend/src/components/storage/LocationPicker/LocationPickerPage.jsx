import React from "react";
import { Button, TextArea, TextInput } from "@carbon/react";
import { Search, Add } from "@carbon/icons-react";
import { useIntl } from "react-intl";
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
  const intl = useIntl();
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
  const positionValue =
    state.position?.mode === "grid"
      ? `${state.position?.row || ""}${state.position?.column || ""}`
      : state.position?.value || "";

  const currentSummary = currentLocation
    ? LEVEL_ORDER.map((lvl) => currentLocation.selection?.[lvl]?.name)
        .filter(Boolean)
        .join(" > ") ||
      currentLocation.hierarchicalPath ||
      ""
    : "";

  return (
    <div className="storage-location-picker-page">
      {breadcrumb}
      <h1>
        {intl.formatMessage({
          id: isMovement
            ? "storage.picker.heading.moveSample"
            : "storage.picker.heading.assignLocation",
          defaultMessage: isMovement
            ? "Move Sample"
            : "Assign Storage Location",
        })}
      </h1>

      <section className="storage-location-picker-page-sample-info">
        <h4>
          {intl.formatMessage({
            id: "storage.picker.sample.heading",
            defaultMessage: "Sample",
          })}
        </h4>
        <dl>
          <dt>
            {intl.formatMessage({
              id: "storage.picker.sample.accession",
              defaultMessage: "Accession",
            })}
          </dt>
          <dd>{sample.sampleAccessionNumber}</dd>
          <dt>
            {intl.formatMessage({
              id: "storage.picker.sample.type",
              defaultMessage: "Type",
            })}
          </dt>
          <dd>{sample.sampleType}</dd>
          <dt>
            {intl.formatMessage({
              id: "storage.picker.sample.status",
              defaultMessage: "Status",
            })}
          </dt>
          <dd>{sample.status}</dd>
        </dl>
      </section>

      {currentSummary && (
        <section className="storage-location-picker-page-current">
          <h4>
            {intl.formatMessage({
              id: "storage.picker.currentLocation",
              defaultMessage: "Current location",
            })}
          </h4>
          <p>{currentSummary}</p>
        </section>
      )}

      <section className="storage-location-picker-page-picker">
        <h4>
          {intl.formatMessage({
            id: isMovement
              ? "storage.picker.newLocation"
              : "storage.picker.storageLocation",
            defaultMessage: isMovement ? "New location" : "Storage location",
          })}
        </h4>
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
              selectedSelection={state.selection}
            />
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
      </section>

      {isMovement && (
        <TextArea
          id="storage-location-picker-page-reason"
          labelText={intl.formatMessage({
            id: "storage.move.reason.label",
            defaultMessage: "Reason for Move",
          })}
          value={state.reason || ""}
          onChange={(e) =>
            dispatch({ type: "SET_REASON", reason: e.target.value })
          }
        />
      )}

      <TextInput
        id="storage-location-picker-page-position"
        labelText={intl.formatMessage({
          id: "storage.picker.position.optional",
          defaultMessage: "Position (optional)",
        })}
        value={positionValue}
        onChange={(e) =>
          dispatch({
            type: "SET_POSITION",
            position: e.target.value
              ? { mode: "text", value: e.target.value }
              : null,
          })
        }
      />

      <TextArea
        id="storage-location-picker-page-notes"
        labelText={intl.formatMessage({
          id: "storage.picker.notes",
          defaultMessage: "Notes",
        })}
        value={state.notes || ""}
        onChange={(e) => dispatch({ type: "SET_NOTES", notes: e.target.value })}
      />

      <div className="storage-location-picker-page-actions">
        <Button kind="secondary" onClick={onCancel}>
          {intl.formatMessage({ id: "label.cancel", defaultMessage: "Cancel" })}
        </Button>
        <Button kind="primary" onClick={handleSave}>
          {intl.formatMessage({ id: "label.save", defaultMessage: "Save" })}
        </Button>
      </div>
    </div>
  );
}
