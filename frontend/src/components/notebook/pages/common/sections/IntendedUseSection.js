import React from "react";
import {
  TextArea,
  RadioButtonGroup,
  RadioButton,
  DatePicker,
  DatePickerInput,
  Grid,
  Column,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";

/**
 * Section C: Intended Use (AHRI BR-F-02)
 * Description of intended use, whether samples will be destroyed,
 * and conditional estimated return date.
 */
function IntendedUseSection({ formData, onChange, readOnly }) {
  const intl = useIntl();

  return (
    <div className="biorepo-section" style={{ marginBottom: "2rem" }}>
      <h4 style={{ marginBottom: "1rem" }}>
        <FormattedMessage
          id="biorepo.import.section.intendedUse"
          defaultMessage="Section C: Intended Use"
        />
      </h4>
      <Grid condensed>
        <Column lg={16} md={8} sm={4}>
          <TextArea
            id="intendedUseDescription"
            labelText={intl.formatMessage({
              id: "biorepo.import.field.intendedUse",
              defaultMessage: "Description for Intended Use",
            })}
            value={formData.intendedUseDescription || ""}
            onChange={(e) => onChange("intendedUseDescription", e.target.value)}
            rows={4}
            readOnly={readOnly}
          />
        </Column>
        <Column lg={8} md={4} sm={4} style={{ marginTop: "1rem" }}>
          <RadioButtonGroup
            legendText={intl.formatMessage({
              id: "biorepo.import.field.willBeDestroyed",
              defaultMessage: "Will samples be destroyed after use?",
            })}
            name="samplesWillBeDestroyed"
            valueSelected={
              formData.samplesWillBeDestroyed === true
                ? "yes"
                : formData.samplesWillBeDestroyed === false
                  ? "no"
                  : ""
            }
            onChange={(value) =>
              onChange("samplesWillBeDestroyed", value === "yes")
            }
            disabled={readOnly}
          >
            <RadioButton
              id="destroy-yes"
              labelText={intl.formatMessage({
                id: "label.yes",
                defaultMessage: "Yes",
              })}
              value="yes"
            />
            <RadioButton
              id="destroy-no"
              labelText={intl.formatMessage({
                id: "label.no",
                defaultMessage: "No",
              })}
              value="no"
            />
          </RadioButtonGroup>
        </Column>
        {formData.samplesWillBeDestroyed === false && (
          <Column lg={8} md={4} sm={4} style={{ marginTop: "1rem" }}>
            <DatePicker
              datePickerType="single"
              value={formData.estimatedReturnDate || ""}
              onChange={(dates) => {
                if (dates && dates.length > 0) {
                  onChange("estimatedReturnDate", dates[0]);
                }
              }}
              readOnly={readOnly}
            >
              <DatePickerInput
                id="estimatedReturnDate"
                labelText={intl.formatMessage({
                  id: "biorepo.import.field.estimatedReturnDate",
                  defaultMessage: "Estimated Time of Return to Biorepository",
                })}
                placeholder="mm/dd/yyyy"
              />
            </DatePicker>
          </Column>
        )}
      </Grid>
    </div>
  );
}

export default IntendedUseSection;
