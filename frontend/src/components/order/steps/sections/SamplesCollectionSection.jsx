import React, { useState } from "react";
import { useIntl, FormattedMessage } from "react-intl";
import { Tile, Button, Stack, Tag } from "@carbon/react";
import { Add, Printer } from "@carbon/icons-react";
import SampleCollectionCard from "./SampleCollectionCard";
import ReceivedByLine from "./ReceivedByLine";
import { sampleObject } from "../../OrderContext";
import { currentLocalTime, todayLocalIso } from "../../dateUtils";

/**
 * SamplesCollectionSection - Container for all sample collection cards
 *
 * Features:
 * - Displays all samples with collection details
 * - Add new sample button
 * - Print more labels button
 * - Auto-populates received date/time from the lab's clock
 */

const SamplesCollectionSection = ({
  samples,
  setSamples,
  sampleTypes,
  unitOfMeasures,
  updateSampleCollectionDetails,
  isReadOnly,
  onPrintLabels,
  printDisabled = false,
  workflowType = "clinical",
  labNumber = "",
}) => {
  const intl = useIntl();
  // The laboratory's "now" when the page opens: the default collection and
  // receipt date and time of a new sample.
  const [serverReceivedDate] = useState(() => todayLocalIso());
  const [serverReceivedTime] = useState(() => currentLocalTime());

  // Handle sample update
  const handleSampleUpdate = (sampleIndex, updates) => {
    updateSampleCollectionDetails(sampleIndex, updates);
  };

  // Handle sample removal
  const handleSampleRemove = (sampleIndex) => {
    const updated = samples.filter((_, i) => i !== sampleIndex);
    // Re-index remaining samples
    const reindexed = updated.map((s, i) => ({ ...s, index: i }));
    setSamples(reindexed);
  };

  // Handle print labels for a specific sample
  const handlePrintLabels = (sampleIndex) => {
    if (onPrintLabels) {
      onPrintLabels(sampleIndex);
    }
  };

  // Handle add new sample
  const primarySampleIndexes = samples
    .map((sample, index) => ({ sample, index }))
    .filter(
      ({ sample }) => !sample.qcMetadata?.qcType && !sample.sampleRejected,
    )
    .map(({ index }) => index);

  // FR-C9a: one cooler usually carries every tube, so Same for all samples
  // copies the arrival condition to every primary sample on the order.
  const handleSameForAll = (arrival) => {
    primarySampleIndexes.forEach((index) =>
      updateSampleCollectionDetails(index, arrival),
    );
  };

  const handleReceiverChange = ({ id, name }) => {
    primarySampleIndexes.forEach((index) =>
      updateSampleCollectionDetails(index, {
        receivedById: id,
        receivedByName: name,
      }),
    );
  };

  const handleAddSample = () => {
    const newSample = {
      ...sampleObject,
      index: samples.length,
      receivedDate: todayLocalIso(),
      receivedTime: currentLocalTime(),
    };
    setSamples([...samples, newSample]);
  };

  return (
    <Tile className="order-section samples-collection-section">
      <h4 className="section-title">
        <FormattedMessage id="collect.samples.title" defaultMessage="Samples" />
      </h4>
      {workflowType === "clinical" && (
        <ReceivedByLine
          samples={samples.filter((_, index) =>
            primarySampleIndexes.includes(index),
          )}
          onChange={handleReceiverChange}
          isReadOnly={isReadOnly}
        />
      )}

      <Stack gap={5}>
        {/* Sample Cards — only regular (non-QC) samples get full collection forms */}
        {samples.map((sample, index) =>
          // Skip QC summaries and rejected/resampled specimens — a rejected
          // specimen is read-only in the QA acceptance table, not collected here.
          sample.qcMetadata?.qcType || sample.sampleRejected ? null : (
            <div key={index}>
              <SampleCollectionCard
                sample={sample}
                sampleIndex={index}
                sampleTypes={sampleTypes}
                unitOfMeasures={unitOfMeasures}
                serverReceivedDate={serverReceivedDate}
                serverReceivedTime={serverReceivedTime}
                onUpdate={handleSampleUpdate}
                onRemove={handleSampleRemove}
                onPrintLabels={handlePrintLabels}
                printDisabled={printDisabled}
                isReadOnly={isReadOnly}
                canRemove={!isReadOnly}
                workflowType={workflowType}
                labNumber={labNumber}
                onSameForAll={
                  primarySampleIndexes.length > 1 ? handleSameForAll : undefined
                }
              />

              {/* Nested QC sample summaries — inherit collection details from parent */}
              {samples
                .map((s, i) => ({ s, i }))
                .filter(
                  ({ s }) =>
                    s.qcMetadata?.qcType &&
                    s.qcMetadata?.parentSampleIndex === index,
                )
                .map(({ s: qcSample }) => {
                  const qcTypeColors = {
                    BLANK: "#0043ce",
                    DUPLICATE: "#009d9a",
                    CONTROL: "#8a3ffc",
                  };
                  const qcTagTypes = {
                    BLANK: "blue",
                    DUPLICATE: "teal",
                    CONTROL: "purple",
                  };
                  return (
                    <div
                      key={`qc-collect-${qcSample.qcMetadata.qcType}`}
                      style={{
                        marginLeft: "2rem",
                        marginTop: "0.5rem",
                        borderLeft: `3px solid ${qcTypeColors[qcSample.qcMetadata.qcType] || "#525252"}`,
                        paddingLeft: "1rem",
                        padding: "0.75rem 1rem",
                        background: "#f4f4f4",
                        borderRadius: "0 4px 4px 0",
                      }}
                    >
                      <div
                        style={{
                          display: "flex",
                          alignItems: "center",
                          gap: "0.5rem",
                        }}
                      >
                        <Tag
                          type={
                            qcTagTypes[qcSample.qcMetadata.qcType] || "gray"
                          }
                          size="sm"
                        >
                          <FormattedMessage
                            id={`qc.type.${qcSample.qcMetadata.qcType.toLowerCase()}`}
                            defaultMessage={`QC: ${qcSample.qcMetadata.qcType}`}
                          />
                        </Tag>
                        <span
                          style={{ fontSize: "0.875rem", color: "#525252" }}
                        >
                          {qcSample.tests?.map((t) => t.name).join(", ") ||
                            intl.formatMessage({
                              id: "collect.sample.noTests",
                              defaultMessage: "No tests assigned",
                            })}
                        </span>
                        <span
                          style={{
                            fontSize: "0.75rem",
                            color: "#8d8d8d",
                            marginLeft: "auto",
                          }}
                        >
                          <FormattedMessage
                            id="qc.collect.inheritsFromParent"
                            defaultMessage="Collection details inherited from parent sample"
                          />
                        </span>
                      </div>
                    </div>
                  );
                })}
            </div>
          ),
        )}

        {/* Action Buttons */}
        <div className="sample-action-buttons">
          <Button
            kind="tertiary"
            size="md"
            renderIcon={Add}
            onClick={handleAddSample}
            disabled={isReadOnly}
          >
            <FormattedMessage
              id="collect.addSample.button"
              defaultMessage="+ Add Another Sample"
            />
          </Button>
        </div>

        <p className="helper-text">
          <FormattedMessage
            id="collect.printMoreLabels.helper"
            defaultMessage="Use 'Print More Sample Labels' if you draw more than expected or need labels for a different sample type."
          />
        </p>
      </Stack>
    </Tile>
  );
};

export default SamplesCollectionSection;
