import { useContext, useEffect, useRef, useState } from "react";
import { useIntl, FormattedMessage } from "react-intl";
import { ConfigurationContext } from "../../../layout/contexts";
import {
  Tile,
  Grid,
  Column,
  Tag,
  Button,
  Select,
  SelectItem,
  TextInput,
  TimePicker,
  Link,
  Checkbox,
} from "@carbon/react";
import { Printer } from "@carbon/icons-react";
import CustomDatePicker from "../../../common/CustomDatePicker";
import { getFromOpenElisServer } from "../../../utils/Utils";
import GpsCoordinatesCapture from "../../../addOrder/GpsCoordinatesCapture";
import {
  currentLocalTime,
  formatIsoDateForBackend,
  formatPickerDateForIso,
  isCollectionDateBeforeAdmissionDate,
  todayLocalIso,
} from "../../dateUtils";

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
  admissionDate = "",
}) => {
  const intl = useIntl();
  const hasInitializedDefaults = useRef(false);
  const initializedSampleIdentity = useRef(null);
  const sampleIdentity =
    sample.sampleItemId ||
    sample.sampleTypeRequestId ||
    `sample-index-${sampleIndex}`;
  const [collectionMethods, setCollectionMethods] = useState([]);
  const [specimenOrigins, setSpecimenOrigins] = useState([]);
  const { configurationProperties = {} } =
    useContext(ConfigurationContext) || {};
  const dateLocale = configurationProperties.DEFAULT_DATE_LOCALE || "en-US";
  const collectionDateBeforeAdmission = isCollectionDateBeforeAdmissionDate(
    sample.collectionDate,
    admissionDate,
  );

  useEffect(() => {
    let active = true;
    getFromOpenElisServer(
      "/rest/clinical/dictionary/collection-methods",
      (data) => {
        if (active) setCollectionMethods(Array.isArray(data) ? data : []);
      },
    );
    getFromOpenElisServer(
      "/rest/clinical/dictionary/specimen-origins",
      (data) => {
        if (active) setSpecimenOrigins(Array.isArray(data) ? data : []);
      },
    );
    return () => {
      active = false;
    };
  }, []);

  useEffect(() => {
    if (initializedSampleIdentity.current !== sampleIdentity) {
      initializedSampleIdentity.current = sampleIdentity;
      hasInitializedDefaults.current = false;
    }
    if (
      !hasInitializedDefaults.current &&
      !sample.sampleItemId &&
      !isReadOnly
    ) {
      const updates = {};

      if (!sample.collectionDate) {
        updates.collectionDate = todayLocalIso();
      }
      if (!sample.collectionTime) {
        updates.collectionTime = currentLocalTime();
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
      hasInitializedDefaults.current = true;
    }
  }, [
    sample.sampleItemId,
    sampleIdentity,
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

  return (
    <Tile
      className="sample-collection-card"
      data-testid={`sample-collection-card-${sampleIndex}`}
    >
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
          <TextInput
            id={`quantity-${sampleIndex}`}
            labelText={intl.formatMessage({
              id: "collect.sample.quantity",
              defaultMessage: "Quantity",
            })}
            type="number"
            min="0"
            step="0.5"
            value={sample.quantity ?? ""}
            onChange={(e) => handleFieldChange("quantity", e.target.value)}
            onWheel={(e) => e.target.blur()}
            disabled={isReadOnly}
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
        {/* Clinical collection previously captured only free-text conditions,
            while the environmental lane had a coded method. These are the
            coded equivalents; sample_item already stores all three. */}
        <Column lg={5} md={4} sm={4}>
          <Select
            id={`collectionMethod-${sampleIndex}`}
            labelText={intl.formatMessage({
              id: "collect.sample.collectionMethod",
              defaultMessage: "Collection Method",
            })}
            value={sample.collectionMethod || ""}
            onChange={(e) =>
              handleFieldChange("collectionMethod", e.target.value)
            }
            disabled={isReadOnly}
          >
            <SelectItem
              value=""
              text={intl.formatMessage({
                id: "label.select",
                defaultMessage: "Select...",
              })}
            />
            {collectionMethods.map((method) => (
              <SelectItem
                key={method.id}
                value={method.dictEntry}
                text={method.localizedName || method.dictEntry}
              />
            ))}
          </Select>
        </Column>

        <Column lg={5} md={4} sm={4}>
          <Select
            id={`specimenOrigin-${sampleIndex}`}
            labelText={intl.formatMessage({
              id: "collect.sample.specimenOrigin",
              defaultMessage: "Specimen Origin",
            })}
            value={sample.specimenOrigin || ""}
            onChange={(e) =>
              handleFieldChange("specimenOrigin", e.target.value)
            }
            disabled={isReadOnly}
          >
            <SelectItem
              value=""
              text={intl.formatMessage({
                id: "label.select",
                defaultMessage: "Select...",
              })}
            />
            {specimenOrigins.map((origin) => (
              <SelectItem
                key={origin.id}
                value={origin.dictEntry}
                text={origin.localizedName || origin.dictEntry}
              />
            ))}
          </Select>
        </Column>

        <Column lg={4} md={4} sm={4}>
          <TextInput
            id={`sampleTemperature-${sampleIndex}`}
            labelText={intl.formatMessage({
              id: "collect.sample.temperature",
              defaultMessage: "Sample Temperature",
            })}
            placeholder={intl.formatMessage({
              id: "collect.sample.temperature.placeholder",
              defaultMessage: "e.g. 4 C",
            })}
            value={sample.sampleTemperature || ""}
            onChange={(e) =>
              handleFieldChange("sampleTemperature", e.target.value)
            }
            disabled={isReadOnly}
          />
        </Column>

        {/* V-8: clinical collection had no way to record where the specimen was
            taken; env and vector only showed GPS read-only from the site
            record. The same capture control the legacy screen used is reused
            here so the coordinates land on the sample itself. */}
        <Column lg={16} md={8} sm={4}>
          <GpsCoordinatesCapture
            index={sampleIndex}
            sampleXml={{
              gpsLatitude: sample.gpsLatitude || "",
              gpsLongitude: sample.gpsLongitude || "",
              gpsAccuracy: sample.gpsAccuracy || null,
              gpsCaptureMethod: sample.gpsCaptureMethod || "",
            }}
            onChange={(gps) => onUpdate(sampleIndex, gps)}
            disabled={isReadOnly}
          />
        </Column>

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
          <CustomDatePicker
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
                    defaultMessage="(optional - filled when specimen is physically collected)"
                  />
                </span>
              </>
            }
            value={formatIsoDateForBackend(sample.collectionDate, dateLocale)}
            updateStateValue
            disallowFutureDate
            invalid={collectionDateBeforeAdmission}
            invalidText={intl.formatMessage({
              id: "collect.sample.collectionDateBeforeAdmission",
            })}
            onChange={(value) =>
              handleFieldChange(
                "collectionDate",
                formatPickerDateForIso(value, dateLocale),
              )
            }
            disabled={isReadOnly}
          />
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

        {/* Lab Performed Sampling */}
        <Column lg={7} md={4} sm={4} className="checkbox-column">
          <Checkbox
            id={`labPerformedSampling-${sampleIndex}`}
            labelText={intl.formatMessage({
              id: "collect.sample.labPerformedSampling",
              defaultMessage: "Lab performed sampling",
            })}
            checked={!!sample.labPerformedSampling}
            onChange={(_, { checked }) =>
              handleFieldChange("labPerformedSampling", checked)
            }
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
            <CustomDatePicker
              id={`receivedDate-${sampleIndex}`}
              labelText={intl.formatMessage({
                id: "collect.sample.receivedDate",
                defaultMessage: "Received Date",
              })}
              value={formatIsoDateForBackend(
                sample.receivedDate ||
                  (sample.sampleItemId ? "" : serverReceivedDate),
                dateLocale,
              )}
              updateStateValue
              disallowFutureDate
              onChange={(value) =>
                handleFieldChange(
                  "receivedDate",
                  formatPickerDateForIso(value, dateLocale),
                )
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
    </Tile>
  );
};

export default SampleCollectionCard;
