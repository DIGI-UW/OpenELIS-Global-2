import React, { useState, useEffect, useRef } from "react";
import { useIntl, FormattedMessage } from "react-intl";
import { Tile, Button, Stack } from "@carbon/react";
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
        {/* Sample Cards */}
        {samples.map((sample, index) => (
          <SampleCollectionCard
            key={index}
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
        ))}

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
