import React, { useContext } from "react";
import {
  Checkbox,
  InlineNotification,
  NumberInput,
  RadioButton,
  RadioButtonGroup,
  Select,
  SelectItem,
  Tag,
  TextArea,
} from "@carbon/react";
import { format } from "date-fns";
import { useIntl } from "react-intl";
import CustomDatePicker from "../common/CustomDatePicker";
import { ConfigurationContext } from "../layout/Layout";
import {
  daysBetweenIsoDates,
  formatIsoDateForBackend,
  formatPickerDateForIso,
} from "../order/dateUtils";
import "./MicrobiologyOrderDetailFields.scss";

export const emptyMicrobiologyOrderDetail = {
  culturePurpose: "",
  cultureMethodId: "",
  patientOrigin: "",
  admissionDate: "",
  numberOfSets: "",
  clinicalHistory: "",
  antibioticExposure: false,
};

export const formatAdmissionDateForPicker = formatIsoDateForBackend;
export const formatAdmissionDateForApi = formatPickerDateForIso;

const MicrobiologyOrderDetailFields = ({
  fields,
  onChange,
  methods = [],
  patientOrigins = [],
  idPrefix = "microbiology-order-detail",
  isReadOnly = false,
  showCultureMethod = true,
  collectionDate = "",
}) => {
  const intl = useIntl();
  const { configurationProperties = {} } =
    useContext(ConfigurationContext) || {};
  const dateLocale = configurationProperties.DEFAULT_DATE_LOCALE || "en-US";
  const selectedMethod =
    methods.find(
      (method) => String(method.methodId) === String(fields.cultureMethodId),
    ) || null;
  const methodSummary = selectedMethod
    ? [
        selectedMethod.mediaDefaults,
        selectedMethod.incubationDefaults,
        selectedMethod.atmosphereDefaults,
      ]
        .filter(Boolean)
        .join(" - ")
    : "";
  const isOutpatient = fields.patientOrigin === "OUTPATIENT";
  const admissionDateIsFuture =
    Boolean(fields.admissionDate) &&
    fields.admissionDate > format(new Date(), "yyyy-MM-dd");
  const daysAfterAdmission = daysBetweenIsoDates(
    fields.admissionDate,
    collectionDate,
  );

  return (
    <div className="microbiology-order-detail-fields">
      {showCultureMethod && (
        <div className="microbiology-order-detail-fields__protocol">
          <div className="microbiology-order-detail-fields__label-row">
            <span className="cds--label">
              {intl.formatMessage({
                id: "microbiology.orderDetail.cultureMethod",
              })}
            </span>
            <Tag type="gray" size="sm">
              {intl.formatMessage({
                id: "microbiology.orderDetail.protocolDerived",
              })}
            </Tag>
          </div>
          {selectedMethod ? (
            <>
              <p className="microbiology-order-detail-fields__protocol-name">
                {selectedMethod.methodName}
              </p>
              {methodSummary && (
                <p className="microbiology-order-detail-fields__protocol-summary">
                  {methodSummary}
                </p>
              )}
              <p className="microbiology-order-detail-fields__helper">
                {intl.formatMessage({
                  id: "microbiology.orderDetail.protocolHelper",
                })}
              </p>
            </>
          ) : (
            <InlineNotification
              kind="warning"
              lowContrast
              hideCloseButton
              title={intl.formatMessage({
                id: "microbiology.orderDetail.protocolUnsetTitle",
              })}
              subtitle={intl.formatMessage({
                id: "microbiology.orderDetail.protocolUnset",
              })}
            />
          )}
        </div>
      )}
      <div className="microbiology-order-detail-fields__wide">
        <RadioButtonGroup
          legendText={intl.formatMessage({
            id: "microbiology.culturePurpose.label",
          })}
          name={`${idPrefix}-culture-purpose`}
          valueSelected={fields.culturePurpose || ""}
          onChange={(value) => onChange("culturePurpose", value)}
          orientation="vertical"
        >
          <RadioButton
            id={`${idPrefix}-culture-purpose-clinical`}
            labelText={intl.formatMessage({
              id: "microbiology.culturePurpose.clinical",
            })}
            value="CLINICAL_DIAGNOSTIC"
            disabled={isReadOnly}
          />
          <RadioButton
            id={`${idPrefix}-culture-purpose-screening`}
            labelText={intl.formatMessage({
              id: "microbiology.culturePurpose.screening",
            })}
            value="ACTIVE_SCREENING"
            disabled={isReadOnly}
          />
        </RadioButtonGroup>
        {!fields.culturePurpose && (
          <Tag type="gray" size="sm">
            {intl.formatMessage({
              id: "microbiology.culturePurpose.unspecified",
            })}
          </Tag>
        )}
      </div>
      <Select
        id={`${idPrefix}-patient-origin`}
        labelText={intl.formatMessage({
          id: "microbiology.orderDetail.patientOrigin",
        })}
        value={fields.patientOrigin}
        onChange={(event) => onChange("patientOrigin", event.target.value)}
        disabled={isReadOnly}
      >
        <SelectItem value="" text="" />
        {patientOrigins.map((option) => (
          <SelectItem
            key={option.code}
            value={option.code}
            text={option.label}
          />
        ))}
      </Select>
      <div className="microbiology-order-detail-fields__admission-date">
        <CustomDatePicker
          id={`${idPrefix}-admission-date`}
          labelText={intl.formatMessage({
            id: "microbiology.orderDetail.admissionDate",
          })}
          helperText={intl.formatMessage({
            id: isOutpatient
              ? "microbiology.orderDetail.admissionDateOutpatient"
              : "microbiology.orderDetail.admissionDateHelper",
          })}
          value={formatAdmissionDateForPicker(fields.admissionDate, dateLocale)}
          updateStateValue
          disallowFutureDate
          invalid={admissionDateIsFuture}
          invalidText={intl.formatMessage({
            id: "microbiology.orderDetail.admissionDateFuture",
          })}
          onChange={(value) =>
            onChange(
              "admissionDate",
              formatAdmissionDateForApi(value, dateLocale),
            )
          }
          disabled={isReadOnly || isOutpatient}
        />
        {collectionDate && (
          <div className="microbiology-order-detail-fields__collection-context">
            <p>
              {intl.formatMessage(
                { id: "microbiology.orderDetail.collectionDateContext" },
                {
                  date: formatIsoDateForBackend(collectionDate, dateLocale),
                },
              )}
            </p>
            {daysAfterAdmission !== null && daysAfterAdmission >= 0 && (
              <p>
                {intl.formatMessage(
                  {
                    id:
                      daysAfterAdmission > 2
                        ? "microbiology.orderDetail.collectionTimingHospital"
                        : "microbiology.orderDetail.collectionTimingCommunity",
                  },
                  { days: daysAfterAdmission },
                )}
              </p>
            )}
          </div>
        )}
      </div>
      <NumberInput
        id={`${idPrefix}-number-of-sets`}
        label={intl.formatMessage({
          id: "microbiology.orderDetail.numberOfSets",
        })}
        value={fields.numberOfSets}
        min={1}
        max={10}
        allowEmpty
        onChange={(event, state = {}) =>
          onChange("numberOfSets", state.value ?? event.target.value)
        }
        disabled={isReadOnly}
      />
      <div className="microbiology-order-detail-fields__wide">
        <TextArea
          id={`${idPrefix}-clinical-history`}
          labelText={intl.formatMessage({
            id: "microbiology.orderDetail.clinicalHistory",
          })}
          value={fields.clinicalHistory}
          onChange={(event) => onChange("clinicalHistory", event.target.value)}
          maxLength={1000}
          disabled={isReadOnly}
        />
      </div>
      <Checkbox
        id={`${idPrefix}-antibiotic-exposure`}
        labelText={intl.formatMessage({
          id: "microbiology.orderDetail.antibioticExposure",
        })}
        checked={Boolean(fields.antibioticExposure)}
        onChange={(_, { checked }) => onChange("antibioticExposure", checked)}
        disabled={isReadOnly}
      />
    </div>
  );
};

export default MicrobiologyOrderDetailFields;
