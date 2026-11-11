import React, { useState } from "react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  Tile,
  Select,
  SelectItem,
  Tag,
  TextArea,
  Checkbox,
  InlineNotification,
} from "@carbon/react";
import LocationPickerInline from "../../../storage/LocationPicker/LocationPickerInline";
import {
  getDeepestLocationSelection,
  positionToCoordinate,
  selectionToHierarchicalPath,
} from "../../../storage/LocationPicker/locationSelectionMapper";

/**
 * PrepareStorageSection - Storage on Prepare Samples (OGC-1266 M4, FR-E1,
 * FR-E5).
 *
 * The location, its notes and the "skip storage" decision are held on the
 * sample rows and the order, and travel with the step's Save as one
 * transaction with everything else on the page. Nothing here calls the
 * storage API on its own; the Label & Store step that did is gone.
 */
const PrepareStorageSection = ({
  samples,
  updateSampleCollectionDetails,
  storageSkipped,
  onStorageSkippedChange,
  labNumber,
  isReadOnly,
}) => {
  const intl = useIntl();
  const [selectedIndex, setSelectedIndex] = useState(0);
  const liveSamples = samples
    .map((sample, index) => ({ sample, index }))
    .filter(({ sample }) => sample.sampleTypeId && !sample.sampleRejected);
  const current =
    liveSamples.find(({ index }) => index === selectedIndex) || liveSamples[0];
  const assignedCount = liveSamples.filter(
    ({ sample }) => sample.storageLocationId,
  ).length;
  const unassigned = liveSamples.filter(
    ({ sample }) => !sample.storageLocationId,
  );

  const sampleLabel = ({ sample, index }) =>
    `${labNumber || ""}-${index + 1} ${sample.sampleTypeName || sample.name || ""}`.trim();

  const handleLocationChange = (state) => {
    if (!current) return;
    const deepest = getDeepestLocationSelection(state.selection, {
      requireAssignable: true,
    });
    if (!deepest) return;
    updateSampleCollectionDetails(current.index, {
      storageLocationId: String(deepest.value.id),
      storageLocationType: deepest.type,
      storagePositionCoordinate: positionToCoordinate(state.position) || "",
      storageHierarchicalPath: selectionToHierarchicalPath(state.selection),
    });
  };

  return (
    <Tile
      className="order-section prepare-storage-section"
      data-testid="prepare-storage-section"
    >
      <h4 className="section-title">
        <FormattedMessage id="storage.location" defaultMessage="Storage" />
      </h4>

      {liveSamples.length === 0 ? (
        <p className="helper-text">
          <FormattedMessage
            id="storage.noSamples"
            defaultMessage="Add a sample above to store it."
          />
        </p>
      ) : (
        <>
          {unassigned.length > 0 ? (
            <InlineNotification
              kind={storageSkipped ? "info" : "warning"}
              lowContrast
              hideCloseButton
              title={
                storageSkipped
                  ? intl.formatMessage(
                      {
                        id: "storage.skipped.title",
                        defaultMessage: "Storage skipped for {count} sample(s)",
                      },
                      { count: unassigned.length },
                    )
                  : intl.formatMessage({
                      id: "storage.unassigned.title",
                      defaultMessage: "Unassigned Samples",
                    })
              }
              subtitle={unassigned.map(sampleLabel).join(", ")}
            />
          ) : (
            <InlineNotification
              kind="success"
              lowContrast
              hideCloseButton
              title={intl.formatMessage(
                {
                  id: "storage.allAssigned.title",
                  defaultMessage: "All {count} sample(s) have storage assigned",
                },
                { count: assignedCount },
              )}
            />
          )}

          {!isReadOnly && (
            <Checkbox
              id="skip-storage-checkbox"
              labelText={intl.formatMessage(
                {
                  id: "storage.skipRemaining",
                  defaultMessage:
                    "Skip storage for unassigned samples ({count}) - will be processed immediately",
                },
                { count: unassigned.length },
              )}
              checked={Boolean(storageSkipped)}
              onChange={(_, { checked }) => onStorageSkippedChange(checked)}
            />
          )}

          {liveSamples.length > 1 && (
            <Select
              id="storage-sample-selector"
              labelText={intl.formatMessage({
                id: "storage.selectSample",
                defaultMessage: "Select Sample",
              })}
              value={current ? current.index : 0}
              onChange={(e) => setSelectedIndex(Number(e.target.value))}
            >
              {liveSamples.map((entry) => (
                <SelectItem
                  key={entry.index}
                  value={entry.index}
                  text={sampleLabel(entry)}
                />
              ))}
            </Select>
          )}

          {current && (
            <div className="sample-info-bar">
              <span>
                <strong>{sampleLabel(current)}</strong>
              </span>
              <span>
                <Tag
                  type={current.sample.storageLocationId ? "green" : "gray"}
                  size="sm"
                >
                  {current.sample.storageLocationId ? (
                    <FormattedMessage
                      id="storage.assigned"
                      defaultMessage="Assigned"
                    />
                  ) : (
                    <FormattedMessage
                      id="order.samples.notStored"
                      defaultMessage="Not stored"
                    />
                  )}
                </Tag>
              </span>
              {current.sample.storageHierarchicalPath && (
                <span>
                  <strong>
                    <FormattedMessage
                      id="storage.currentLocation"
                      defaultMessage="Location"
                    />
                    :
                  </strong>{" "}
                  {current.sample.storageHierarchicalPath}
                </span>
              )}
            </div>
          )}

          {!isReadOnly && current && (
            <>
              <LocationPickerInline
                key={current.index}
                allowCreate={false}
                onChange={handleLocationChange}
              />
              <TextArea
                id={`storage-notes-${current.index}`}
                labelText={intl.formatMessage({
                  id: "storage.conditionNotes",
                  defaultMessage: "Condition Notes (optional)",
                })}
                placeholder={intl.formatMessage({
                  id: "storage.conditionNotes.placeholder",
                  defaultMessage: "Enter any condition notes...",
                })}
                value={current.sample.storageNotes || ""}
                onChange={(e) =>
                  updateSampleCollectionDetails(current.index, {
                    storageNotes: e.target.value,
                  })
                }
                rows={2}
              />
            </>
          )}
        </>
      )}
    </Tile>
  );
};

export default PrepareStorageSection;
