import CultureBottleFields from "./CultureBottleFields";
import { useContext, useEffect, useMemo, useRef, useState } from "react";
import { useIntl, FormattedMessage } from "react-intl";
import { ConfigurationContext } from "../../../layout/Layout";
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
  Tooltip,
} from "@carbon/react";
import { Printer } from "@carbon/icons-react";
import CustomDatePicker from "../../../common/CustomDatePicker";
import { getFromOpenElisServer } from "../../../utils/Utils";
import GpsCoordinatesCapture from "../../../addOrder/GpsCoordinatesCapture";
import {
  formatHoldingMinutes,
  formatIsoDateForBackend,
  formatPickerDateForIso,
} from "../../dateUtils";
import {
  getHandlingRequirements,
  getSampleStorageLocation,
} from "../../api/orderEntryCleanupApi";
import {
  ARRIVAL_CONDITIONS,
  arrivalLabelId,
  customConditions,
  describeMismatch,
  handlingMismatches,
  isPlausibleTemperature,
  requiredConditions,
  shortestHoldingMinutes,
  storageConditionLabelId,
} from "./handlingRules";

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
  printDisabled = false,
  isReadOnly,
  canRemove,
  workflowType = "clinical",
  labNumber = "",
  onSameForAll,
}) => {
  const isClinical = workflowType === "clinical";
  const intl = useIntl();
  const userEditedFields = useRef(new Set());
  const initializedSampleIdentity = useRef(null);
  const sampleIdentity =
    sample.sampleItemId ||
    sample.sampleTypeRequestId ||
    `sample-index-${sampleIndex}`;
  const [collectionMethods, setCollectionMethods] = useState([]);
  const [specimenOrigins, setSpecimenOrigins] = useState([]);
  const [requirementsResponse, setRequirementsResponse] = useState({
    key: "",
    requirements: [],
  });
  const [storageLocation, setStorageLocation] = useState(null);
  const handlingTestIds = useMemo(() => {
    const ids = (sample.tests || []).map((test) => String(test.id));
    (sample.panels || []).forEach((panel) => {
      String(panel.testIds || "")
        .split(",")
        .filter(Boolean)
        .forEach((id) => ids.push(id.trim()));
    });
    return [...new Set(ids)].sort();
  }, [sample.tests, sample.panels]);
  const handlingKey = handlingTestIds.join(",");
  const { configurationProperties = {} } =
    useContext(ConfigurationContext) || {};
  const dateLocale = configurationProperties.DEFAULT_DATE_LOCALE || "en-US";

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

  // FR-C9a: the Required line reads the test catalog's storage condition and
  // holding time for the tests on this sample.
  useEffect(() => {
    if (!isClinical || !handlingKey) {
      return undefined;
    }
    let active = true;
    getHandlingRequirements(handlingKey.split(","))
      .then((requirements) => {
        if (active) setRequirementsResponse({ key: handlingKey, requirements });
      })
      .catch(() => {
        if (active)
          setRequirementsResponse({ key: handlingKey, requirements: [] });
      });
    return () => {
      active = false;
    };
  }, [isClinical, handlingKey]);

  useEffect(() => {
    if (!isClinical || !sample.sampleItemId) {
      return undefined;
    }
    let active = true;
    getSampleStorageLocation(sample.sampleItemId)
      .then((location) => {
        if (active) setStorageLocation(location);
      })
      .catch(() => {
        if (active) setStorageLocation(null);
      });
    return () => {
      active = false;
    };
  }, [isClinical, sample.sampleItemId]);

  // Collection and receipt default to the laboratory's current time, taken
  // from the server, for a sample not yet saved. A default is filled whenever
  // its field is empty, so it survives the order reloading the sample list,
  // but never over a value the user has edited or cleared on this sample.
  useEffect(() => {
    if (initializedSampleIdentity.current !== sampleIdentity) {
      initializedSampleIdentity.current = sampleIdentity;
      userEditedFields.current = new Set();
    }
    if (sample.sampleItemId || isReadOnly || !serverReceivedDate) {
      return;
    }
    const defaults = {
      collectionDate: serverReceivedDate,
      collectionTime: serverReceivedTime,
      receivedDate: serverReceivedDate,
      receivedTime: serverReceivedTime,
    };
    const updates = {};
    Object.entries(defaults).forEach(([field, value]) => {
      if (value && !sample[field] && !userEditedFields.current.has(field)) {
        updates[field] = value;
      }
    });
    if (Object.keys(updates).length > 0) {
      onUpdate(sampleIndex, updates);
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

  const requirements =
    requirementsResponse.key === handlingKey && handlingKey
      ? requirementsResponse.requirements
      : [];
  const required = requiredConditions(requirements);
  const custom = customConditions(requirements);
  const holdingMinutes = shortestHoldingMinutes(requirements);
  const storedAt =
    sample.sampleItemId && storageLocation?.hierarchicalPath
      ? storageLocation
      : null;
  const mismatchText = isClinical
    ? handlingMismatches({
        requirements,
        arrivalCondition: sample.arrivalCondition || "",
        arrivalTemperature: sample.arrivalTemperature ?? "",
        storageTemperature: storedAt?.temperatureSetting ?? "",
      })
        .map((mismatch) => describeMismatch(intl, mismatch))
        .join(" ")
    : "";
  const requiredText =
    required.length + custom.length === 0
      ? intl.formatMessage({ id: "sample.handling.noRequirement" })
      : [
          ...required.map((condition) =>
            intl.formatMessage({ id: storageConditionLabelId(condition) }),
          ),
          ...custom,
        ].join(" / ") +
        (holdingMinutes
          ? ` · ${intl.formatMessage(
              { id: "sample.handling.processWithin" },
              { time: formatHoldingMinutes(holdingMinutes) },
            )}`
          : "");
  const legacyValues = isClinical
    ? [
        {
          id: "collect.sample.specimenOrigin",
          value:
            specimenOrigins.find(
              (origin) => origin.dictEntry === sample.specimenOrigin,
            )?.localizedName || sample.specimenOrigin,
        },
        {
          id: "collect.sample.collectionConditions",
          value: sample.collectionConditions,
        },
        { id: "collect.sample.temperature", value: sample.sampleTemperature },
      ].filter((entry) => entry.value)
    : [];

  const handleFieldChange = (field, value) => {
    if ((sample[field] ?? "") === (value ?? "")) {
      return;
    }
    userEditedFields.current.add(field);
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
        {mismatchText && (
          <div
            className="handling-mismatch"
            data-testid={`handling-mismatch-${sampleIndex}`}
          >
            <Tooltip label={mismatchText} align="bottom">
              <span
                tabIndex={0}
                className="handling-mismatch-trigger"
                aria-label={mismatchText}
              >
                <Tag type="warm-gray" size="sm">
                  <FormattedMessage id="sample.handling.mismatch" />
                </Tag>
              </span>
            </Tooltip>
            <Link
              href={`/ReportNonConformingEvent?labNumber=${encodeURIComponent(
                labNumber,
              )}&description=${encodeURIComponent(mismatchText)}`}
              target="_blank"
              rel="noopener noreferrer"
              data-testid={`handling-report-nce-${sampleIndex}`}
            >
              <FormattedMessage id="sample.handling.reportNonConformity" />
            </Link>
          </div>
        )}
        <div className="sample-card-actions">
          <Button
            kind="ghost"
            size="sm"
            renderIcon={Printer}
            onClick={() => onPrintLabels(sampleIndex)}
            disabled={isReadOnly || printDisabled}
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
        {((sample.tests || []).some((test) => test.collectedInSets) ||
          sample.cultureSetNumber) && (
          <CultureBottleFields
            sample={sample}
            sampleIndex={sampleIndex}
            isReadOnly={isReadOnly}
            onChange={handleFieldChange}
          />
        )}
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

        {!isClinical && (
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
        )}

        {!isClinical && (
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
        )}

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

        {!isClinical && (
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
        )}

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

        {/* Lab Performed Sampling: environmental and vector only (FR-C9);
            on clinical the collector records who sampled. */}
        {!isClinical && (
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
        )}
      </Grid>

      {isClinical && (
        <div
          className="handling-section"
          data-testid={`handling-group-${sampleIndex}`}
        >
          <h6>
            <FormattedMessage id="sample.handling.title" />
          </h6>
          <p data-testid={`handling-required-${sampleIndex}`}>
            <strong>
              <FormattedMessage id="sample.handling.required" />
              {": "}
            </strong>
            {requiredText}
          </p>
          <Grid>
            <Column lg={5} md={4} sm={4}>
              <Select
                id={`arrivalCondition-${sampleIndex}`}
                labelText={intl.formatMessage({
                  id: "sample.handling.arrivedAs",
                })}
                value={sample.arrivalCondition || ""}
                onChange={(e) =>
                  handleFieldChange("arrivalCondition", e.target.value)
                }
                disabled={isReadOnly}
              >
                <SelectItem
                  value=""
                  text={intl.formatMessage({ id: "common.notRecorded" })}
                />
                {ARRIVAL_CONDITIONS.map((condition) => (
                  <SelectItem
                    key={condition}
                    value={condition}
                    text={intl.formatMessage({
                      id: arrivalLabelId(condition),
                    })}
                  />
                ))}
              </Select>
            </Column>
            <Column lg={4} md={4} sm={4}>
              <TextInput
                id={`arrivalTemperature-${sampleIndex}`}
                labelText={intl.formatMessage({
                  id: "sample.handling.measuredTemp",
                })}
                type="number"
                step="0.1"
                min="-100"
                max="60"
                value={sample.arrivalTemperature ?? ""}
                invalid={!isPlausibleTemperature(sample.arrivalTemperature)}
                invalidText={intl.formatMessage({
                  id: "sample.handling.measuredTemp.invalid",
                })}
                onChange={(e) =>
                  handleFieldChange("arrivalTemperature", e.target.value)
                }
                onWheel={(e) => e.target.blur()}
                disabled={isReadOnly}
              />
            </Column>
            <Column lg={7} md={8} sm={4} className="handling-same-for-all">
              {onSameForAll && (
                <Button
                  kind="ghost"
                  size="sm"
                  onClick={() =>
                    onSameForAll({
                      arrivalCondition: sample.arrivalCondition || "",
                      arrivalTemperature: sample.arrivalTemperature ?? "",
                    })
                  }
                  disabled={isReadOnly}
                  data-testid={`handling-same-for-all-${sampleIndex}`}
                >
                  <FormattedMessage id="sample.handling.sameForAll" />
                </Button>
              )}
            </Column>
          </Grid>
          <p data-testid={`handling-stored-at-${sampleIndex}`}>
            <strong>
              <FormattedMessage id="sample.handling.storedAt" />
              {": "}
            </strong>
            {storedAt ? (
              storedAt.temperatureSetting ? (
                <FormattedMessage
                  id="sample.handling.storedAt.temperature"
                  values={{
                    location: storedAt.hierarchicalPath,
                    degrees: storedAt.temperatureSetting,
                  }}
                />
              ) : (
                storedAt.hierarchicalPath
              )
            ) : (
              <FormattedMessage id="sample.handling.notStored" />
            )}
          </p>
          {legacyValues.length > 0 && (
            <div
              className="legacy-values"
              data-testid={`legacy-values-${sampleIndex}`}
            >
              <h6>
                <FormattedMessage id="sample.legacy.recordedBefore" />
              </h6>
              {legacyValues.map((entry) => (
                <p key={entry.id}>
                  <FormattedMessage id={entry.id} />
                  {": "}
                  {entry.value}
                </p>
              ))}
            </div>
          )}
        </div>
      )}

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
