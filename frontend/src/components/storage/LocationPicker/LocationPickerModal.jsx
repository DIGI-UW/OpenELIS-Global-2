import React, { useEffect } from "react";
import {
  ComposedModal,
  ModalHeader,
  ModalBody,
  ModalFooter,
  Button,
  TextInput,
  TextArea,
} from "@carbon/react";
import { Search, Add } from "@carbon/icons-react";
import useLocationPicker, { LEVEL_ORDER } from "./useLocationPicker";
import SearchField from "./components/SearchField";
import CreateForm from "./components/CreateForm";

/**
 * LocationPickerModal — wraps the picker in a Carbon ComposedModal for
 * sites where page navigation would be jarring (e.g. a deeply-nested
 * expandable row).
 *
 * Layout: sample info → optional current-location → mode-toggle picker
 * → reason (movement only) → notes → Cancel/Confirm footer.
 *
 * onConfirm receives { selection, position, reason, notes }; the caller
 * translates that into the appropriate REST call. The modal is
 * workflow-agnostic.
 */
export default function LocationPickerModal({
  isOpen,
  sample,
  currentLocation,
  onConfirm,
  onCancel,
}) {
  const isMovement = !!currentLocation;
  const [state, dispatch] = useLocationPicker(
    currentLocation ? { initialAssignment: currentLocation } : {},
  );

  // Reset picker state when the modal opens (so a previous open's state
  // doesn't leak into a new sample's flow). Only re-init when isOpen
  // transitions to true.
  useEffect(() => {
    if (!isOpen) return;
    dispatch({
      type: "PRELOAD",
      selection: currentLocation?.selection || {},
      position: currentLocation?.position || null,
    });
    // dispatch is stable from useReducer; no need to include
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isOpen]);

  const setLevel = (level, value) =>
    dispatch({ type: "SET_LEVEL", level, value });

  const handleSearchSelect = (result) => {
    if (result && result.type && LEVEL_ORDER.includes(result.type)) {
      setLevel(result.type, { id: result.id, name: result.name });
    }
  };

  const handleConfirm = () => {
    onConfirm({
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
    <ComposedModal open={isOpen} onClose={onCancel}>
      <ModalHeader
        title={isMovement ? "Move Sample" : "Assign Storage Location"}
      />
      <ModalBody>
        <section className="storage-location-picker-modal-sample-info">
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
          <section className="storage-location-picker-modal-current">
            <h4>Current location</h4>
            <p>{currentSummary}</p>
          </section>
        )}

        <section className="storage-location-picker-modal-picker">
          <h4>{isMovement ? "New location" : "Storage location"}</h4>
          {summary && (
            <div className="storage-location-picker-modal-summary">
              {summary}
            </div>
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
                Create new location
              </Button>
            </>
          ) : (
            <>
              <CreateForm
                selection={state.selection}
                onLevelChange={setLevel}
              />
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
            id="storage-location-picker-modal-reason"
            labelText="Reason for move"
            value={state.reason || ""}
            onChange={(e) =>
              dispatch({ type: "SET_REASON", reason: e.target.value })
            }
          />
        )}

        <TextInput
          id="storage-location-picker-modal-position"
          labelText="Position (optional)"
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
          id="storage-location-picker-modal-notes"
          labelText="Notes"
          value={state.notes || ""}
          onChange={(e) =>
            dispatch({ type: "SET_NOTES", notes: e.target.value })
          }
        />
      </ModalBody>
      <ModalFooter>
        <Button kind="secondary" onClick={onCancel}>
          Cancel
        </Button>
        <Button kind="primary" onClick={handleConfirm}>
          Confirm
        </Button>
      </ModalFooter>
    </ComposedModal>
  );
}
