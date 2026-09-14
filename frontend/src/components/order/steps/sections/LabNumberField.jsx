import React, { useCallback, useEffect, useRef, useState } from "react";
import { useIntl, FormattedMessage } from "react-intl";
import { Button, TextInput } from "@carbon/react";
import { getFromOpenElisServer } from "../../../utils/Utils";

/**
 * Lab Number control shared by every order-entry lane.
 *
 * The lab number is the only required field on the entry step, so a new order
 * asks the server for one as soon as the form is ready instead of loading in a
 * state that cannot be saved. Generation is also offered as a Button rather
 * than a link: an anchor without an href takes no keyboard focus and conveys
 * no disabled state, which left the one required control unreachable without a
 * mouse and let repeat clicks consume a lab number per click.
 */
const LabNumberField = ({
  value,
  onLabNumberChange,
  disabled = false,
  autoGenerate = false,
  invalid = false,
  invalidText,
}) => {
  const intl = useIntl();
  const componentMounted = useRef(true);
  const autoGenerateRequested = useRef(false);
  const [isGenerating, setIsGenerating] = useState(false);

  useEffect(() => {
    componentMounted.current = true;
    return () => {
      componentMounted.current = false;
    };
  }, []);

  const generateLabNumber = useCallback(() => {
    setIsGenerating(true);
    getFromOpenElisServer(
      "/rest/SampleEntryGenerateScanProvider",
      (response) => {
        if (!componentMounted.current) {
          return;
        }
        setIsGenerating(false);
        if (response?.body) {
          onLabNumberChange(response.body);
        }
      },
    );
  }, [onLabNumberChange]);

  // Runs at the first render where the field is genuinely empty and editable.
  // A new order that resets its context on mount only reaches that state on a
  // later render, so this cannot be a mount-only effect; the ref keeps it to
  // one request per mount and leaves a manually cleared field alone.
  useEffect(() => {
    if (autoGenerateRequested.current || !autoGenerate || disabled || value) {
      return;
    }
    autoGenerateRequested.current = true;
    generateLabNumber();
  }, [autoGenerate, disabled, value, generateLabNumber]);

  return (
    <>
      <div className="lab-number-field">
        <TextInput
          id="labNumber"
          labelText={
            <span>
              <FormattedMessage
                id="order.labNumber"
                defaultMessage="Lab Number"
              />
              <span className="required-indicator"> *</span>
            </span>
          }
          value={value}
          onChange={(event) => onLabNumberChange(event.target.value)}
          invalid={invalid}
          invalidText={invalidText}
          placeholder={intl.formatMessage({
            id: "order.labNumber.placeholder",
            defaultMessage: "Enter or generate lab number",
          })}
          disabled={disabled}
        />
        <Button
          className="generate-link"
          kind="ghost"
          size="sm"
          onClick={generateLabNumber}
          disabled={isGenerating || disabled}
        >
          {isGenerating ? (
            <FormattedMessage id="generating" defaultMessage="Generating..." />
          ) : (
            <FormattedMessage
              id="order.labNumber.generate"
              defaultMessage="Generate"
            />
          )}
        </Button>
      </div>
      <p className="helper-text">
        <FormattedMessage
          id="order.labNumber.helper"
          defaultMessage="Auto-generated per existing lab number rules. Assigned here to enable tracking across all steps."
        />
      </p>
    </>
  );
};

export default LabNumberField;
