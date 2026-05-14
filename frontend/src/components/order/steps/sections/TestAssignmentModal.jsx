import React, { useState } from "react";
import { useIntl, FormattedMessage } from "react-intl";
import { Modal, RadioButtonGroup, RadioButton, Stack } from "@carbon/react";

/**
 * TestAssignmentModal - Modal for assigning a test to a sample
 *
 * Shows when user clicks a sample type tag in the RequestedTestsSection.
 * Allows user to:
 * - Add test to an existing sample of the same type
 * - Create a new sample (separate draw) for this test
 */

const TestAssignmentModal = ({
  isOpen,
  onClose,
  test,
  sampleType,
  existingSamples,
  onAssign,
}) => {
  const intl = useIntl();
  const [selectedOption, setSelectedOption] = useState(
    existingSamples.length > 0 ? `sample-${existingSamples[0]?.index}` : "new",
  );

  // Reset selection when modal opens with new data
  React.useEffect(() => {
    if (isOpen && existingSamples.length > 0) {
      setSelectedOption(`sample-${existingSamples[0]?.index}`);
    } else if (isOpen) {
      setSelectedOption("new");
    }
  }, [isOpen, existingSamples]);

  const handleSubmit = () => {
    if (selectedOption === "new") {
      onAssign(null, true); // Create new sample
    } else {
      const sampleIndex = parseInt(selectedOption.replace("sample-", ""), 10);
      onAssign(sampleIndex, false); // Add to existing sample
    }
  };

  if (!test || !sampleType) {
    return null;
  }

  // Get tests already in each existing sample for display
  const getSampleTestsList = (sample) => {
    const testNames = sample.tests?.map((t) => t.name) || [];
    const panelNames = sample.panels?.map((p) => p.name) || [];
    return [...panelNames, ...testNames].join(", ");
  };

  return (
    <Modal
      open={isOpen}
      onRequestClose={onClose}
      onRequestSubmit={handleSubmit}
      modalHeading={intl.formatMessage(
        {
          id: "collect.assign.title",
          defaultMessage: "Assign '{testName}' — {sampleType}",
        },
        { testName: test.name, sampleType: sampleType.name },
      )}
      primaryButtonText={intl.formatMessage({
        id: "collect.assign.button",
        defaultMessage: "Assign",
      })}
      secondaryButtonText={intl.formatMessage({
        id: "button.cancel",
        defaultMessage: "Cancel",
      })}
      size="md"
      className="test-assignment-modal"
    >
      <Stack gap={5}>
        {existingSamples.length > 0 ? (
          <>
            <p className="modal-description">
              <FormattedMessage
                id="collect.assign.description"
                defaultMessage="A {sampleType} sample already exists. Choose where to assign this test, or draw a new sample."
                values={{ sampleType: sampleType.name }}
              />
            </p>

            <RadioButtonGroup
              name="sample-assignment"
              valueSelected={selectedOption}
              onChange={(value) => setSelectedOption(value)}
              orientation="vertical"
            >
              {existingSamples.map((sample) => (
                <RadioButton
                  key={sample.index}
                  value={`sample-${sample.index}`}
                  labelText={
                    <div className="sample-option">
                      <strong>
                        <FormattedMessage
                          id="collect.assign.addToSample"
                          defaultMessage="Add to Sample {number} — {sampleType}"
                          values={{
                            number: sample.index + 1,
                            sampleType: sampleType.name,
                          }}
                        />
                      </strong>
                      {getSampleTestsList(sample) && (
                        <span className="sample-tests-list">
                          Currently has: {getSampleTestsList(sample)}
                        </span>
                      )}
                    </div>
                  }
                />
              ))}
              <RadioButton
                value="new"
                labelText={
                  <div className="sample-option">
                    <strong>
                      <FormattedMessage
                        id="collect.assign.newSample"
                        defaultMessage="New {sampleType} sample (separate draw)"
                        values={{ sampleType: sampleType.name }}
                      />
                    </strong>
                    <span className="sample-option-help">
                      <FormattedMessage
                        id="collect.assign.newSampleHelp"
                        defaultMessage="Creates a new sample requiring its own collection"
                      />
                    </span>
                  </div>
                }
              />
            </RadioButtonGroup>
          </>
        ) : (
          <p className="modal-description">
            <FormattedMessage
              id="collect.assign.noExisting"
              defaultMessage="No existing {sampleType} sample. A new sample will be created for this test."
              values={{ sampleType: sampleType.name }}
            />
          </p>
        )}
      </Stack>
    </Modal>
  );
};

export default TestAssignmentModal;
