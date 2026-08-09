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
import { useIntl } from "react-intl";
import useLocationPicker from "./useLocationPicker";
import {
  selectionToHierarchicalPath,
  positionToCoordinate,
} from "./locationSelectionMapper";
import { searchResultToReplaceAction } from "./searchResultToAction";
import SearchField from "./components/SearchField";
import CreateForm from "./components/CreateForm";
import "./LocationPickerModal.css";

/**
 * LocationPickerModal — wraps the picker in a Carbon ComposedModal for
 * sites where page navigation would be jarring (e.g. a deeply-nested
 * expandable row).
 *
 * Layout: occupant info → optional current-location → mode-toggle picker
 * → reason (movement only) → notes → Cancel/Confirm footer.
 *
 * onConfirm receives { selection, position, reason, notes }; the caller
 * translates that into the appropriate REST call. The modal is
 * workflow- and occupant-agnostic — `occupant` is display-only (identifier,
 * type, status) and `occupantType` (e.g. "SAMPLE_ITEM" | "INVENTORY_LOT")
 * is passed straight through so the caller can pick the right endpoint.
 */
export default function LocationPickerModal({
  isOpen,
  occupant,
  occupantType,
  currentLocation,
  onConfirm,
  onCancel,
}) {
  const intl = useIntl();
  const isMovement = !!currentLocation;
  const [state, dispatch] = useLocationPicker(
    currentLocation ? { initialAssignment: currentLocation } : {},
  );

  // Reset picker state when the modal opens (so a previous open's mode,
  // search query, reason, notes, etc. don't leak into the new flow). Then
  // preload selection/position from currentLocation for movement contexts.
  // Only re-init when isOpen transitions to true.
  useEffect(() => {
    if (!isOpen) return;
    dispatch({ type: "RESET", initialAssignment: currentLocation || null });
    if (currentLocation) {
      dispatch({
        type: "PRELOAD",
        selection: currentLocation.selection || {},
        position: currentLocation.position || null,
      });
    }
    // dispatch is stable from useReducer; currentLocation intentionally
    // omitted — we only want to reset on the isOpen rising edge.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isOpen]);

  const setLevel = (level, value) =>
    dispatch({ type: "SET_LEVEL", level, value });

  // Flat search returns a single leaf; replacing the whole selection
  // keeps the state consistent (no stale ancestors from a different
  // branch of the hierarchy).
  const handleSearchSelect = (result) => {
    const action = searchResultToReplaceAction(result);
    if (action) dispatch(action);
  };

  const handleConfirm = () => {
    onConfirm({
      selection: state.selection,
      position: state.position,
      reason: state.reason,
      notes: state.notes,
    });
  };

  const summary = selectionToHierarchicalPath(state.selection);
  const positionValue = positionToCoordinate(state.position);

  const currentSummary = currentLocation
    ? selectionToHierarchicalPath(currentLocation.selection) ||
      currentLocation.hierarchicalPath ||
      ""
    : "";

  return (
    <ComposedModal
      open={isOpen}
      onClose={onCancel}
      data-occupant-type={occupantType}
    >
      <ModalHeader
        title={intl.formatMessage({
          id: isMovement
            ? "storage.picker.heading.move"
            : "storage.picker.heading.assignLocation",
          defaultMessage: isMovement ? "Move Item" : "Assign Storage Location",
        })}
      />
      <ModalBody>
        <section className="storage-location-picker-modal-occupant-info">
          <h4>
            {intl.formatMessage({
              id: "storage.picker.occupant.heading",
              defaultMessage: "Item",
            })}
          </h4>
          <dl>
            <dt>
              {intl.formatMessage({
                id: "storage.picker.occupant.identifier",
                defaultMessage: "Identifier",
              })}
            </dt>
            <dd>{occupant.label}</dd>
            <dt>
              {intl.formatMessage({
                id: "storage.picker.occupant.type",
                defaultMessage: "Type",
              })}
            </dt>
            <dd>{occupant.type}</dd>
            <dt>
              {intl.formatMessage({
                id: "storage.picker.occupant.status",
                defaultMessage: "Status",
              })}
            </dt>
            <dd>{occupant.status}</dd>
          </dl>
        </section>

        {currentSummary && (
          <section className="storage-location-picker-modal-current">
            <h4>
              {intl.formatMessage({
                id: "storage.picker.currentLocation",
                defaultMessage: "Current location",
              })}
            </h4>
            <p>{currentSummary}</p>
          </section>
        )}

        <section className="storage-location-picker-modal-picker">
          <h4>
            {intl.formatMessage({
              id: isMovement
                ? "storage.picker.newLocation"
                : "storage.picker.storageLocation",
              defaultMessage: isMovement ? "New location" : "Storage location",
            })}
          </h4>
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
                {intl.formatMessage({
                  id: "storage.picker.createNewLocation",
                  defaultMessage: "Create new location",
                })}
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
            id="storage-location-picker-modal-reason"
            labelText={intl.formatMessage({
              id: "storage.move.reason.label",
              defaultMessage: "Reason for Move",
            })}
            value={state.reason}
            onChange={(e) =>
              dispatch({ type: "SET_REASON", reason: e.target.value })
            }
          />
        )}

        <TextInput
          id="storage-location-picker-modal-position"
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
          id="storage-location-picker-modal-notes"
          labelText={intl.formatMessage({
            id: "storage.picker.notes",
            defaultMessage: "Notes",
          })}
          value={state.notes}
          onChange={(e) =>
            dispatch({ type: "SET_NOTES", notes: e.target.value })
          }
        />
      </ModalBody>
      <ModalFooter>
        <Button kind="secondary" onClick={onCancel}>
          {intl.formatMessage({ id: "label.cancel", defaultMessage: "Cancel" })}
        </Button>
        <Button kind="primary" onClick={handleConfirm}>
          {intl.formatMessage({
            id: "label.confirm",
            defaultMessage: "Confirm",
          })}
        </Button>
      </ModalFooter>
    </ComposedModal>
  );
}
