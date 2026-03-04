import React, { useState } from "react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  Modal,
  StructuredListWrapper,
  StructuredListHead,
  StructuredListRow,
  StructuredListCell,
  StructuredListBody,
  Tag,
  Toggle,
} from "@carbon/react";
import NCEInlineWrapper from "../results/NCEInlineWrapper/NCEInlineWrapper";

/**
 * ValidationNCETriggerModal prompts the user to optionally (or mandatorily)
 * report an NCE when results are rejected during validation.
 *
 * Trigger #3: Rejection with retest → optional NCE (can skip)
 * Trigger #4: Rejection without retest → mandatory NCE (no skip)
 * FR-017: Support grouping multiple rejections into a single NCE
 */
const ValidationNCETriggerModal = ({
  open,
  rejectedResults,
  mandatory,
  onComplete,
  onCancel,
}) => {
  const intl = useIntl();
  const [showNCEForm, setShowNCEForm] = useState(false);
  const [currentResultIndex, setCurrentResultIndex] = useState(0);
  const [createdNCEs, setCreatedNCEs] = useState([]);
  const [groupMode, setGroupMode] = useState(false);

  const currentResult = rejectedResults[currentResultIndex];
  const isLastResult = currentResultIndex >= rejectedResults.length - 1;
  const hasMultipleResults = rejectedResults.length > 1;

  const handleReportNCE = () => {
    setShowNCEForm(true);
  };

  const handleNCECreated = (nceData) => {
    setCreatedNCEs((prev) => [...prev, nceData]);
    setShowNCEForm(false);
    if (groupMode) {
      // FR-017: In group mode, single NCE covers all results — done
      onComplete([...createdNCEs, nceData]);
    } else {
      advanceOrComplete();
    }
  };

  const handleSkip = () => {
    if (mandatory) return;
    setShowNCEForm(false);
    advanceOrComplete();
  };

  const advanceOrComplete = () => {
    if (isLastResult) {
      onComplete(createdNCEs);
    } else {
      setCurrentResultIndex((prev) => prev + 1);
    }
  };

  const handleRequestClose = () => {
    if (!mandatory) {
      onCancel();
    }
  };

  if (!open || rejectedResults.length === 0) {
    return null;
  }

  return (
    <Modal
      open={open}
      modalHeading={intl.formatMessage({
        id: mandatory
          ? "validation.trigger.heading.mandatory"
          : "validation.trigger.heading.optional",
      })}
      primaryButtonText={
        showNCEForm
          ? undefined
          : intl.formatMessage({ id: "validation.trigger.reportNCE" })
      }
      secondaryButtonText={
        showNCEForm
          ? undefined
          : mandatory
            ? undefined
            : intl.formatMessage({ id: "validation.trigger.skip" })
      }
      onRequestSubmit={showNCEForm ? undefined : handleReportNCE}
      onSecondarySubmit={showNCEForm ? undefined : handleSkip}
      onRequestClose={handleRequestClose}
      preventCloseOnClickOutside={mandatory}
      size="lg"
    >
      <p style={{ marginBottom: "1rem" }}>
        {rejectedResults.length === 1 ? (
          <FormattedMessage id="validation.trigger.description.single" />
        ) : groupMode ? (
          <FormattedMessage
            id="validation.trigger.description.grouped"
            values={{ total: rejectedResults.length }}
          />
        ) : (
          <FormattedMessage
            id="validation.trigger.description.multiple"
            values={{
              current: currentResultIndex + 1,
              total: rejectedResults.length,
            }}
          />
        )}
      </p>

      {mandatory && (
        <Tag type="red" style={{ marginBottom: "1rem" }}>
          <FormattedMessage id="validation.trigger.mandatory.tag" />
        </Tag>
      )}

      {hasMultipleResults && !showNCEForm && (
        <Toggle
          id="group-rejections-toggle"
          labelText={intl.formatMessage({
            id: "validation.trigger.groupToggle",
          })}
          labelA={intl.formatMessage({
            id: "validation.trigger.groupToggle.off",
          })}
          labelB={intl.formatMessage({
            id: "validation.trigger.groupToggle.on",
          })}
          toggled={groupMode}
          onToggle={(checked) => setGroupMode(checked)}
          size="sm"
          style={{ marginBottom: "1rem" }}
        />
      )}

      <StructuredListWrapper>
        <StructuredListHead>
          <StructuredListRow head>
            <StructuredListCell head>
              <FormattedMessage id="validation.trigger.column.labNumber" />
            </StructuredListCell>
            <StructuredListCell head>
              <FormattedMessage id="validation.trigger.column.testName" />
            </StructuredListCell>
            <StructuredListCell head>
              <FormattedMessage id="validation.trigger.column.result" />
            </StructuredListCell>
          </StructuredListRow>
        </StructuredListHead>
        <StructuredListBody>
          {groupMode ? (
            rejectedResults.map((r, idx) => (
              <StructuredListRow key={idx}>
                <StructuredListCell>{r.accessionNumber}</StructuredListCell>
                <StructuredListCell>{r.testName}</StructuredListCell>
                <StructuredListCell>{r.result}</StructuredListCell>
              </StructuredListRow>
            ))
          ) : (
            <StructuredListRow>
              <StructuredListCell>
                {currentResult?.accessionNumber}
              </StructuredListCell>
              <StructuredListCell>{currentResult?.testName}</StructuredListCell>
              <StructuredListCell>{currentResult?.result}</StructuredListCell>
            </StructuredListRow>
          )}
        </StructuredListBody>
      </StructuredListWrapper>

      {showNCEForm && currentResult && (
        <NCEInlineWrapper
          resultId={currentResult.resultId}
          sourceType="validation"
          triggerType={mandatory ? "mandatory" : "prompted"}
          triggerAction={
            mandatory ? "result_rejection_no_retest" : "result_rejection_retest"
          }
          context={
            groupMode
              ? {
                  labNumber: rejectedResults
                    .map((r) => r.accessionNumber)
                    .join(", "),
                  testName: rejectedResults.map((r) => r.testName).join(", "),
                  resultValue: rejectedResults.map((r) => r.result).join(", "),
                  suggestedSeverity: mandatory ? "Critical" : "Major",
                  qualityFlags: [
                    intl.formatMessage({
                      id: mandatory
                        ? "validation.trigger.flag.noRetest"
                        : "validation.trigger.flag.retest",
                    }),
                  ],
                }
              : {
                  labNumber: currentResult.accessionNumber,
                  testName: currentResult.testName,
                  resultValue: currentResult.result,
                  suggestedSeverity: mandatory ? "Critical" : "Major",
                  qualityFlags: [
                    intl.formatMessage({
                      id: mandatory
                        ? "validation.trigger.flag.noRetest"
                        : "validation.trigger.flag.retest",
                    }),
                  ],
                }
          }
          onNCECreated={handleNCECreated}
          onCancel={mandatory ? undefined : () => setShowNCEForm(false)}
        />
      )}
    </Modal>
  );
};

export default ValidationNCETriggerModal;
