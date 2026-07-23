import { useEffect } from "react";
import { useIntl, FormattedMessage } from "react-intl";
import {
  Tile,
  Grid,
  Column,
  Tag,
  Button,
  Select,
  SelectItem,
  NumberInput,
  TextInput,
  DatePicker,
  DatePickerInput,
  TimePicker,
  Link,
} from "@carbon/react";
import { Printer, TrashCan } from "@carbon/icons-react";

/**
 * SampleCollectionCard - Card for a single sample with collection details
 *
 * Features:
 * - Shows assigned tests as tags
 * - Sample type, quantity, collection conditions
 * - Collection date/time and collector
 * - Received at lab date/time (auto-populated)
 * - NCE reporting link
 * - Print labels button
 */

const SampleCollectionCard = ({
  sample,
  sampleIndex,
  sampleTypes,
  unitOfMeasures,
  serverReceivedDate,
  serverReceivedTime,
  onUpdate,
  onRemove,
  onPrintLabels,
  isReadOnly,
  canRemove,
}) => {
  const intl = useIntl();

  // Auto-populate dates/times for new samples that don't have values yet
  // collectionDate/Time default to now; receivedDate/Time default to server values
  useEffect(() => {
    if (!sample.sampleItemId && !isReadOnly) {
      const updates = {};

      if (!sample.collectionDate) {
        const now = new Date();
        const yyyy = now.getFullYear();
        const mm = String(now.getMonth() + 1).padStart(2, "0");
        const dd = String(now.getDate()).padStart(2, "0");
        updates.collectionDate = `${yyyy}-${mm}-${dd}`;
      }
      if (!sample.collectionTime) {
        const now = new Date();
        const hh = String(now.getHours()).padStart(2, "0");
        const min = String(now.getMinutes()).padStart(2, "0");
        updates.collectionTime = `${hh}:${min}`;
      }
      if (!sample.receivedDate && serverReceivedDate) {
        updates.receivedDate = serverReceivedDate;
      }
      if (!sample.receivedTime && serverReceivedTime) {
        updates.receivedTime = serverReceivedTime;
      }

      if (Object.keys(updates).length > 0) {
        onUpdate(sampleIndex, updates);
      }
    }
  }, [
    sample.sampleItemId,
    sample.collectionDate,
    sample.collectionTime,
    sample.receivedDate,
    sample.receivedTime,
    serverReceivedDate,
    serverReceivedTime,
    sampleIndex,
    onUpdate,
    isReadOnly,
  ]);

  const sampleTypeName =
    sample.sampleTypeName ||
    sampleTypes.find((st) => st.id === sample.sampleTypeId)?.value ||
    "";

  const handleFieldChange = (field, value) => {
    onUpdate(sampleIndex, { [field]: value });
  };

  const formatDateForPicker = (dateStr) => {
    if (!dateStr) return "";
    if (dateStr.includes("/")) {
      return dateStr;
    }
    const parts = dateStr.split("-");
    if (parts.length === 3) {
      return `${parts[1]}/${parts[2]}/${parts[0]}`;
    }
    return dateStr;
  };

  const parseDateFromPicker = (dateStr) => {
    if (!dateStr) return "";
    const parts = dateStr.split("/");
    if (parts.length === 3) {
      return `${parts[2]}-${parts[0].padStart(2, "0")}-${parts[1].padStart(2, "0")}`;
    }
    return dateStr;
  };

  return (
    <Tile className="sample-collection-card">
      {/* Header */}
      <div className="sample-card-header">
        <h5>
          <FormattedMessage
            id="collect.sample.header"
            defaultMessage="Sample {number} — {sampleType}"
            values={{ number: sampleIndex + 1, sampleType: sampleTypeName }}
          />
        </h5>
        <div className="sample-card-actions">
          <Button
            kind="ghost"
            size="sm"
            renderIcon={Printer}
            onClick={() => onPrintLabels(sampleIndex)}
            disabled={isReadOnly}
          >
            <FormattedMessage
              id="collect.sample.printLabels"
              defaultMessage="Print Labels"
            />
          </Button>
          {canRemove && (
            <Link
              className="remove-link"
              onClick={() => onRemove(sampleIndex)}
              disabled={isReadOnly}
            >
              <FormattedMessage
                id="collect.sample.remove"
                defaultMessage="Remove"
              />
            </Link>
          )}
        </div>
      </div>

      {/* Assigned Tests */}
      <div className="assigned-tests">
        <span className="assigned-label">
          <FormattedMessage
            id="collect.sample.assignedTests"
            defaultMessage="Assigned Tests:"
          />
        </span>
        <div className="assigned-tags">
          {sample.panels?.map((panel) => (
            <Tag key={`panel-${panel.id}`} type="blue" size="sm">
              {panel.name}
            </Tag>
          ))}
          {sample.tests?.map((test) => (
            <Tag key={`test-${test.id}`} type="teal" size="sm">
              {test.name}
            </Tag>
          ))}
          {(!sample.tests || sample.tests.length === 0) &&
            (!sample.panels || sample.panels.length === 0) && (
              <span className="no-tests">
                <FormattedMessage
                  id="collect.sample.noTests"
                  defaultMessage="No tests assigned"
                />
              </span>
            )}
        </div>
      </div>

      {/* Collection Details Grid */}
      <Grid className="collection-details-grid">
        {/* Sample Type */}
        <Column lg={4} md={4} sm={4}>
          <Select
            id={`sampleType-${sampleIndex}`}
            labelText={intl.formatMessage({
              id: "sample.type",
              defaultMessage: "Sample Type",
            })}
            value={sample.sampleTypeId || ""}
            onChange={(e) => {
              const newTypeId = e.target.value;
              const newTypeName =
                sampleTypes.find((st) => st.id === newTypeId)?.value || "";
              onUpdate(sampleIndex, {
                sampleTypeId: newTypeId,
                sampleTypeName: newTypeName,
              });
            }}
            disabled={isReadOnly}
          >
            <SelectItem value="" text="" />
            {sampleTypes.map((type) => (
              <SelectItem key={type.id} value={type.id} text={type.value} />
            ))}
          </Select>
        </Column>

        {/* Quantity */}
        <Column lg={4} md={4} sm={2}>
          <NumberInput
            id={`quantity-${sampleIndex}`}
            label={intl.formatMessage({
              id: "collect.sample.quantity",
              defaultMessage: "Quantity",
            })}
            value={sample.quantity || ""}
            onChange={(e, { value }) => handleFieldChange("quantity", value)}
            onWheel={(e) => e.target.blur()}
            min={0}
            step={0.5}
            disabled={isReadOnly}
            hideSteppers
          />
        </Column>

        {/* Quantity Unit */}
        <Column lg={2} md={2} sm={2}>
          <Select
            id={`quantityUnit-${sampleIndex}`}
            labelText={intl.formatMessage({
              id: "label.unit",
              defaultMessage: "Unit",
            })}
            value={sample.quantityUnit || ""}
            onChange={(e) => handleFieldChange("quantityUnit", e.target.value)}
            disabled={isReadOnly}
          >
            <SelectItem value="" text="" />
            {unitOfMeasures &&
              unitOfMeasures.map((uom) => (
                <SelectItem key={uom.id} value={uom.id} text={uom.value} />
              ))}
          </Select>
        </Column>

        {/* Collection Conditions */}
        <Column lg={6} md={4} sm={4}>
          <TextInput
            id={`collectionConditions-${sampleIndex}`}
            labelText={intl.formatMessage({
              id: "collect.sample.collectionConditions",
              defaultMessage: "Collection Conditions",
            })}
            placeholder={intl.formatMessage({
              id: "collect.sample.collectionConditions.placeholder",
              defaultMessage: "e.g., Fasting, Room temp",
            })}
            value={sample.collectionConditions || ""}
            onChange={(e) =>
              handleFieldChange("collectionConditions", e.target.value)
            }
            disabled={isReadOnly}
          />
        </Column>

        {/* Collection Date */}
        <Column lg={4} md={4} sm={4}>
          <DatePicker
            datePickerType="single"
            maxDate={new Date().toISOString()}
            value={formatDateForPicker(sample.collectionDate)}
            onChange={(dates) => {
              if (dates && dates[0]) {
                const month = String(dates[0].getMonth() + 1).padStart(2, "0");
                const day = String(dates[0].getDate()).padStart(2, "0");
                const year = dates[0].getFullYear();
                handleFieldChange("collectionDate", `${year}-${month}-${day}`);
              }
            }}
          >
            <DatePickerInput
              id={`collectionDate-${sampleIndex}`}
              labelText={
                <>
                  <FormattedMessage
                    id="collect.sample.collectionDate"
                    defaultMessage="Collection Date"
                  />
                  <span className="helper-inline">
                    {" "}
                    <FormattedMessage
                      id="collect.sample.collectionDate.helper"
                      defaultMessage="(optional — filled when specimen is physically collected)"
                    />
                  </span>
                </>
              }
              placeholder="mm/dd/yyyy"
              disabled={isReadOnly}
            />
          </DatePicker>
        </Column>

        {/* Collection Time */}
        <Column lg={3} md={2} sm={2}>
          <TimePicker
            id={`collectionTime-${sampleIndex}`}
            labelText={intl.formatMessage({
              id: "collect.sample.collectionTime",
              defaultMessage: "Collection Time",
            })}
            value={sample.collectionTime || ""}
            onChange={(e) =>
              handleFieldChange("collectionTime", e.target.value)
            }
            disabled={isReadOnly}
          />
        </Column>

        {/* Collector */}
        <Column lg={5} md={4} sm={4}>
          <TextInput
            id={`collector-${sampleIndex}`}
            labelText={intl.formatMessage({
              id: "collect.sample.collector",
              defaultMessage: "Collector",
            })}
            placeholder="COL-0000"
            value={sample.collectorId || ""}
            onChange={(e) => handleFieldChange("collectorId", e.target.value)}
            disabled={isReadOnly}
          />
        </Column>
      </Grid>

      {/* Received at Lab Section */}
      <div className="received-at-lab-section">
        <h6>
          <FormattedMessage
            id="collect.sample.receivedAtLab"
            defaultMessage="Received at Lab"
          />
          {/* Only show auto-populated hint for new samples */}
          {!sample.sampleItemId && (
            <span className="helper-inline">
              {" "}
              <FormattedMessage
                id="collect.sample.receivedAtLab.helper"
                defaultMessage="(auto-populated from server — editable)"
              />
            </span>
          )}
        </h6>
        <Grid>
          <Column lg={4} md={4} sm={4}>
            <DatePicker
              datePickerType="single"
              maxDate={new Date().toISOString()}
              onChange={(dates) => {
                if (dates && dates[0]) {
                  const month = String(dates[0].getMonth() + 1).padStart(
                    2,
                    "0",
                  );
                  const day = String(dates[0].getDate()).padStart(2, "0");
                  const year = dates[0].getFullYear();
                  handleFieldChange("receivedDate", `${year}-${month}-${day}`);
                }
              }}
            >
              <DatePickerInput
                id={`receivedDate-${sampleIndex}`}
                labelText={intl.formatMessage({
                  id: "collect.sample.receivedDate",
                  defaultMessage: "Received Date",
                })}
                placeholder="mm/dd/yyyy"
                value={formatDateForPicker(
                  // Use stored value if editing existing sample, otherwise use server time for new samples
                  sample.receivedDate ||
                    (sample.sampleItemId ? "" : serverReceivedDate),
                )}
                disabled={isReadOnly}
              />
            </DatePicker>
            {/* Only show auto-filled hint for new samples without sampleItemId */}
            {!sample.sampleItemId && (
              <span className="auto-filled-hint">
                <FormattedMessage
                  id="label.autoFilledFromServer"
                  defaultMessage="Auto-filled from server"
                />
              </span>
            )}
          </Column>

          <Column lg={3} md={2} sm={2}>
            <TimePicker
              id={`receivedTime-${sampleIndex}`}
              labelText={intl.formatMessage({
                id: "collect.sample.receivedTime",
                defaultMessage: "Received Time",
              })}
              value={
                // Use stored value if editing existing sample, otherwise use server time for new samples
                sample.receivedTime ||
                (sample.sampleItemId ? "" : serverReceivedTime) ||
                ""
              }
              onChange={(e) =>
                handleFieldChange("receivedTime", e.target.value)
              }
              disabled={isReadOnly}
            />
            {/* Only show auto-filled hint for new samples without sampleItemId */}
            {!sample.sampleItemId && (
              <span className="auto-filled-hint">
                <FormattedMessage
                  id="label.autoFilledFromServer"
                  defaultMessage="Auto-filled from server"
                />
              </span>
            )}
          </Column>
        </Grid>
      </div>

      {/* NCE Link */}
      <div className="nce-section">
        <Link
          className="nce-link"
          onClick={() => {
            // TODO: Navigate to NCE form
          }}
        >
          <FormattedMessage
            id="collect.sample.nce.link"
            defaultMessage="Report Non-Conforming Event (NCE)"
          />
        </Link>
      </div>
    </Tile>
  );
};

export default SampleCollectionCard;
