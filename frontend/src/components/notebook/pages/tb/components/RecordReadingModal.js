import React, { useState, useCallback, useMemo, useContext } from "react";
import {
  Modal,
  TextArea,
  Select,
  SelectItem,
  Grid,
  Column,
  InlineNotification,
  RadioButtonGroup,
  RadioButton,
  Tag,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import CustomDatePicker from "../../../../common/CustomDatePicker";
import { ConfigurationContext } from "../../../../layout/Layout";
import { convertToISODate } from "../../../../utils/Utils";

/**
 * Growth Observation Options.
 */
const GROWTH_OBSERVATIONS = [
  { id: "NO_GROWTH", text: "No Growth" },
  { id: "GROWTH_DETECTED", text: "Growth Detected" },
  { id: "CONTAMINATED", text: "Contaminated" },
];

/**
 * RecordReadingModal - Modal for recording weekly culture readings.
 *
 * @param {boolean} open - Whether modal is open
 * @param {function} onClose - Close handler
 * @param {function} onSave - Save handler (receives reading data)
 * @param {object} sample - Sample being read
 * @param {Array} existingReadings - Existing readings for this sample
 * @param {number} currentWeek - Current incubation week
 */
function RecordReadingModal({
  open,
  onClose,
  onSave,
  sample = null,
  existingReadings = [],
  currentWeek = 1,
}) {
  const intl = useIntl();
  const { configurationProperties } = useContext(ConfigurationContext);

  const [formData, setFormData] = useState({
    weekNumber: currentWeek,
    readingDate: configurationProperties?.currentDateAsText || "",
    observation: "",
    notes: "",
  });

  const [error, setError] = useState(null);

  // Determine which weeks have already been recorded
  const recordedWeeks = useMemo(
    () => existingReadings.map((r) => r.weekNumber),
    [existingReadings],
  );

  // Available weeks (1-8, excluding already recorded)
  const availableWeeks = useMemo(() => {
    const weeks = [];
    for (let w = 1; w <= 8; w++) {
      if (!recordedWeeks.includes(w)) {
        weeks.push(w);
      }
    }
    return weeks;
  }, [recordedWeeks]);

  const handleInputChange = useCallback((field, value) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
    setError(null);
  }, []);

  const validateForm = useCallback(() => {
    if (!formData.weekNumber) {
      setError(
        intl.formatMessage({
          id: "notebook.tb.reading.weekRequired",
          defaultMessage: "Please select a week number",
        }),
      );
      return false;
    }
    if (!formData.observation) {
      setError(
        intl.formatMessage({
          id: "notebook.tb.reading.observationRequired",
          defaultMessage: "Please select a growth observation",
        }),
      );
      return false;
    }
    if (!formData.readingDate) {
      setError(
        intl.formatMessage({
          id: "notebook.tb.reading.dateRequired",
          defaultMessage: "Reading date is required",
        }),
      );
      return false;
    }
    return true;
  }, [formData, intl]);

  const handleSave = useCallback(() => {
    if (!validateForm()) return;

    const dataToSave = {
      // Use the first reading's ID since sample.id is now sampleItemId after grouping
      cultureReadingId: sample?.readings?.[0]?.id || sample?.id,
      weekNumber: parseInt(formData.weekNumber, 10),
      observation: formData.observation,
      notes: formData.notes,
      readingDate: convertToISODate(formData.readingDate),
    };

    onSave(dataToSave);
  }, [formData, sample, validateForm, onSave]);

  const handleClose = useCallback(() => {
    setError(null);
    setFormData({
      weekNumber: currentWeek,
      readingDate: configurationProperties?.currentDateAsText || "",
      observation: "",
      notes: "",
    });
    onClose();
  }, [currentWeek, onClose, configurationProperties]);

  // Auto-determination hints based on observation
  const getAutoDeterminationHint = () => {
    if (formData.observation === "GROWTH_DETECTED") {
      return {
        type: "warning",
        message: intl.formatMessage({
          id: "notebook.tb.reading.growthDetectedHint",
          defaultMessage:
            "Growth detected! After saving, you can mark this culture as Positive.",
        }),
      };
    }
    if (
      formData.observation === "NO_GROWTH" &&
      parseInt(formData.weekNumber, 10) === 8
    ) {
      return {
        type: "info",
        message: intl.formatMessage({
          id: "notebook.tb.reading.week8NoGrowthHint",
          defaultMessage:
            "Week 8 with no growth. After saving, you can mark this culture as Negative.",
        }),
      };
    }
    if (formData.observation === "CONTAMINATED") {
      return {
        type: "error",
        message: intl.formatMessage({
          id: "notebook.tb.reading.contaminatedHint",
          defaultMessage:
            "Culture contaminated. Consider re-processing the sample or marking as Contaminated.",
        }),
      };
    }
    return null;
  };

  const hint = getAutoDeterminationHint();

  return (
    <Modal
      open={open}
      modalHeading={intl.formatMessage({
        id: "notebook.tb.reading.title",
        defaultMessage: "Record Weekly Reading",
      })}
      primaryButtonText={intl.formatMessage({
        id: "label.save",
        defaultMessage: "Save",
      })}
      secondaryButtonText={intl.formatMessage({
        id: "label.cancel",
        defaultMessage: "Cancel",
      })}
      onRequestClose={handleClose}
      onRequestSubmit={handleSave}
      size="md"
    >
      <div style={{ marginBottom: "1rem" }}>
        {/* Sample Info */}
        {sample && (
          <div
            style={{
              backgroundColor: "#e5f6ff",
              padding: "1rem",
              borderRadius: "4px",
              marginBottom: "1rem",
              borderLeft: "4px solid #0f62fe",
            }}
          >
            <Grid fullWidth>
              <Column lg={8} md={4} sm={2}>
                <strong>
                  <FormattedMessage
                    id="notebook.tb.reading.sample"
                    defaultMessage="Sample:"
                  />
                </strong>{" "}
                {sample.accessionNumber || sample.sampleItemId || sample.id}
              </Column>
              <Column lg={4} md={2} sm={2}>
                <strong>
                  <FormattedMessage
                    id="notebook.tb.reading.method"
                    defaultMessage="Method:"
                  />
                </strong>{" "}
                {sample.cultureMethod || "LJ"}
              </Column>
              <Column lg={4} md={2} sm={2}>
                <strong>
                  <FormattedMessage
                    id="notebook.tb.reading.currentWeek"
                    defaultMessage="Current Week:"
                  />
                </strong>{" "}
                <Tag type="blue" size="sm">
                  {currentWeek}
                </Tag>
              </Column>
            </Grid>
          </div>
        )}

        {error && (
          <InlineNotification
            kind="error"
            title={error}
            hideCloseButton
            lowContrast
            style={{ marginBottom: "1rem" }}
          />
        )}

        <Grid fullWidth>
          {/* Week Number */}
          <Column lg={8} md={4} sm={4}>
            <Select
              id="weekNumber"
              labelText={intl.formatMessage({
                id: "notebook.tb.reading.weekNumber",
                defaultMessage: "Week Number *",
              })}
              value={formData.weekNumber}
              onChange={(e) => handleInputChange("weekNumber", e.target.value)}
            >
              <SelectItem value="" text="Select week..." />
              {availableWeeks.map((week) => (
                <SelectItem key={week} value={week} text={`Week ${week}`} />
              ))}
            </Select>
            {recordedWeeks.length > 0 && (
              <div
                style={{
                  marginTop: "0.5rem",
                  fontSize: "0.75rem",
                  color: "#525252",
                }}
              >
                <FormattedMessage
                  id="notebook.tb.reading.recordedWeeks"
                  defaultMessage="Already recorded: Week {weeks}"
                  values={{ weeks: recordedWeeks.join(", ") }}
                />
              </div>
            )}
          </Column>

          {/* Reading Date */}
          <Column lg={8} md={4} sm={4}>
            <CustomDatePicker
              id="readingDate"
              labelText={intl.formatMessage({
                id: "notebook.tb.reading.date",
                defaultMessage: "Reading Date *",
              })}
              value={formData.readingDate}
              onChange={(date) => handleInputChange("readingDate", date)}
            />
          </Column>

          {/* Growth Observation */}
          <Column lg={16} md={8} sm={4}>
            <div
              style={{
                marginTop: "1rem",
                padding: "1rem",
                backgroundColor: formData.observation
                  ? formData.observation === "NO_GROWTH"
                    ? "#f4f4f4"
                    : formData.observation === "GROWTH_DETECTED"
                      ? "#fff1f1"
                      : "#fdf0d7"
                  : "#f4f4f4",
                borderRadius: "4px",
                borderLeft: `4px solid ${
                  formData.observation === "GROWTH_DETECTED"
                    ? "#da1e28"
                    : formData.observation === "CONTAMINATED"
                      ? "#f1c21b"
                      : "#8d8d8d"
                }`,
              }}
            >
              <RadioButtonGroup
                legendText={intl.formatMessage({
                  id: "notebook.tb.reading.observation",
                  defaultMessage: "Growth Observation *",
                })}
                name="observation"
                valueSelected={formData.observation}
                onChange={(value) => handleInputChange("observation", value)}
                orientation="horizontal"
              >
                {GROWTH_OBSERVATIONS.map((obs) => (
                  <RadioButton
                    key={obs.id}
                    id={`obs-${obs.id}`}
                    labelText={obs.text}
                    value={obs.id}
                  />
                ))}
              </RadioButtonGroup>
            </div>
          </Column>

          {/* Auto-determination hint */}
          {hint && (
            <Column lg={16} md={8} sm={4}>
              <InlineNotification
                kind={hint.type}
                title={hint.message}
                hideCloseButton
                lowContrast
                style={{ marginTop: "1rem" }}
              />
            </Column>
          )}

          {/* Notes */}
          <Column lg={16} md={8} sm={4}>
            <TextArea
              id="notes"
              labelText={intl.formatMessage({
                id: "notebook.tb.reading.notes",
                defaultMessage: "Notes",
              })}
              value={formData.notes}
              onChange={(e) => handleInputChange("notes", e.target.value)}
              rows={3}
              placeholder={intl.formatMessage({
                id: "notebook.tb.reading.notesPlaceholder",
                defaultMessage:
                  "Colony morphology, contamination details, or other observations...",
              })}
              style={{ marginTop: "1rem" }}
            />
          </Column>
        </Grid>
      </div>
    </Modal>
  );
}

export default RecordReadingModal;
