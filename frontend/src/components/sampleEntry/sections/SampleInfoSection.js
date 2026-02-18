import React from "react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  Section,
  Heading,
  TextInput,
  TimePicker,
  TimePickerSelect,
  SelectItem,
  Grid,
  Column,
} from "@carbon/react";
import CustomDatePicker from "../../common/CustomDatePicker";

const SampleInfoSection = ({ formData, onInputChange }) => {
  const intl = useIntl();

  return (
    <Section>
      <Heading>
        <FormattedMessage
          id="sample.entry.sample.info"
          defaultMessage="Sample Information"
        />
      </Heading>

      <Grid fullWidth={true}>
        <Column lg={8} md={4} sm={4}>
          <TextInput
            id="labNo"
            labelText={intl.formatMessage({
              id: "sample.entry.lab.number",
              defaultMessage: "Lab Number",
            })}
            value={formData.labNo || ""}
            onChange={(e) => onInputChange("labNo", e.target.value)}
            placeholder={intl.formatMessage({
              id: "sample.entry.lab.number.placeholder",
              defaultMessage: "Enter lab number",
            })}
          />
        </Column>

        <Column lg={8} md={4} sm={4}>
          <CustomDatePicker
            id="receivedDateForDisplay"
            labelText={intl.formatMessage({
              id: "sample.entry.received.date",
              defaultMessage: "Received Date",
            })}
            value={formData.receivedDateForDisplay || ""}
            onChange={(date) => onInputChange("receivedDateForDisplay", date)}
            disallowFutureDate={true}
          />
        </Column>

        <Column lg={8} md={4} sm={4}>
          <TimePicker
            id="receivedTimeForDisplay"
            labelText={intl.formatMessage({
              id: "sample.entry.received.time",
              defaultMessage: "Received Time",
            })}
            value={formData.receivedTimeForDisplay || ""}
            onChange={(e) =>
              onInputChange("receivedTimeForDisplay", e.target.value)
            }
            pattern="([01]?[0-9]|2[0-3]):[0-5][0-9]"
            placeholder="hh:mm"
          >
            <TimePickerSelect
              id="receivedTimeForDisplay-select"
              labelText={intl.formatMessage({
                id: "sample.entry.received.time.select",
                defaultMessage: "Select time",
              })}
            >
              <SelectItem value="AM" text="AM" />
              <SelectItem value="PM" text="PM" />
            </TimePickerSelect>
          </TimePicker>
        </Column>

        <Column lg={8} md={4} sm={4}>
          <CustomDatePicker
            id="interviewDate"
            labelText={intl.formatMessage({
              id: "sample.entry.interview.date",
              defaultMessage: "Interview Date",
            })}
            value={formData.interviewDate || ""}
            onChange={(date) => onInputChange("interviewDate", date)}
            disallowFutureDate={true}
          />
        </Column>

        <Column lg={8} md={4} sm={4}>
          <TimePicker
            id="interviewTime"
            labelText={intl.formatMessage({
              id: "sample.entry.interview.time",
              defaultMessage: "Interview Time",
            })}
            value={formData.interviewTime || ""}
            onChange={(e) => onInputChange("interviewTime", e.target.value)}
            pattern="([01]?[0-9]|2[0-3]):[0-5][0-9]"
            placeholder="hh:mm"
          >
            <TimePickerSelect
              id="interviewTime-select"
              labelText={intl.formatMessage({
                id: "sample.entry.interview.time.select",
                defaultMessage: "Select time",
              })}
            >
              <SelectItem value="AM" text="AM" />
              <SelectItem value="PM" text="PM" />
            </TimePickerSelect>
          </TimePicker>
        </Column>
      </Grid>
    </Section>
  );
};

export default SampleInfoSection;
