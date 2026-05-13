import React, { useState, useEffect, useRef } from "react";
import { useIntl, FormattedMessage } from "react-intl";
import { Tile, Button, Stack, Tag } from "@carbon/react";
import { Add, Printer } from "@carbon/icons-react";
import SampleCollectionCard from "./SampleCollectionCard";
import { getFromOpenElisServer } from "../../../utils/Utils";
import { sampleObject } from "../../OrderContext";

/**
 * SamplesCollectionSection - Container for all sample collection cards
 *
 * Features:
 * - Displays all samples with collection details
 * - Add new sample button
 * - Print more labels button
 * - Auto-populates received date/time from server
 */

const SamplesCollectionSection = ({
  samples,
  setSamples,
  sampleTypes,
  unitOfMeasures,
  updateSampleCollectionDetails,
  isReadOnly,
}) => {
  const intl = useIntl();
  const componentMounted = useRef(true);

  // Get current date/time as fallback
  const getClientDate = () => {
    const now = new Date();
    const year = now.getFullYear();
    const month = String(now.getMonth() + 1).padStart(2, "0");
    const day = String(now.getDate()).padStart(2, "0");
    return `${year}-${month}-${day}`;
  };

  const getClientTime = () => {
    const now = new Date();
    const hours = String(now.getHours()).padStart(2, "0");
    const minutes = String(now.getMinutes()).padStart(2, "0");
    return `${hours}:${minutes}`;
  };

  // Current server time for "Received at Lab" - always shows current time when page opens
  // Initialize with client time as fallback, then update with server time
  const [serverReceivedDate, setServerReceivedDate] = useState(getClientDate());
  const [serverReceivedTime, setServerReceivedTime] = useState(getClientTime());

  // Fetch current server time on mount - this is "now" for receiving samples
  useEffect(() => {
    componentMounted.current = true;

    getFromOpenElisServer("/rest/server-time", (response) => {
      if (componentMounted.current && response) {
        setServerReceivedDate(response.date || getClientDate());
        setServerReceivedTime(response.time || getClientTime());
      }
    });

    return () => {
      componentMounted.current = false;
    };
  }, []);

  // Handle sample update
  const handleSampleUpdate = (sampleIndex, updates) => {
    updateSampleCollectionDetails(sampleIndex, updates);
  };

  // Handle sample removal
  const handleSampleRemove = (sampleIndex) => {
    if (samples.length <= 1) return; // Keep at least one sample
    const updated = samples.filter((_, i) => i !== sampleIndex);
    // Re-index remaining samples
    const reindexed = updated.map((s, i) => ({ ...s, index: i }));
    setSamples(reindexed);
  };

  // Handle print labels for a specific sample
  const handlePrintLabels = (sampleIndex) => {
    // TODO: Implement label printing
  };

  // Handle add new sample
  const handleAddSample = () => {
    // Get current server time for new sample
    getFromOpenElisServer("/rest/server-time", (response) => {
      const newSample = {
        ...sampleObject,
        index: samples.length,
        receivedDate: response?.date || "",
        receivedTime: response?.time || "",
      };
      setSamples([...samples, newSample]);
    });
  };

  // Handle print more sample labels
  const handlePrintMoreLabels = () => {
    // TODO: Implement printing additional labels
  };

  return (
    <Tile className="order-section samples-collection-section">
      <h4 className="section-title">
        <FormattedMessage id="collect.samples.title" defaultMessage="Samples" />
      </h4>

      <Stack gap={5}>
        {/* Sample Cards — only regular (non-QC) samples get full collection forms */}
        {samples.map((sample, index) =>
          sample.qcMetadata?.qcType ? null : (
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
                isReadOnly={isReadOnly}
                canRemove={samples.length > 1}
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

          <Button
            kind="tertiary"
            size="md"
            renderIcon={Printer}
            onClick={handlePrintMoreLabels}
            disabled={isReadOnly}
          >
            <FormattedMessage
              id="collect.printMoreLabels.button"
              defaultMessage="Print More Sample Labels"
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
