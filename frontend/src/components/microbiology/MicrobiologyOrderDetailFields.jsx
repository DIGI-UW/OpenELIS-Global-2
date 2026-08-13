import React, { useContext } from "react";
import {
  Checkbox,
  InlineNotification,
  NumberInput,
  Select,
  SelectItem,
  Tag,
  TextArea,
} from "@carbon/react";
import { format, isValid, parse, parseISO } from "date-fns";
import { useIntl } from "react-intl";
import CustomDatePicker from "../common/CustomDatePicker";
import { ConfigurationContext } from "../layout/Layout";
import "./MicrobiologyOrderDetailFields.scss";

export const emptyMicrobiologyOrderDetail = {
  cultureMethodId: "",
  patientOrigin: "",
  admissionDate: "",
  numberOfSets: "",
  clinicalHistory: "",
  antibioticExposure: false,
};

const pickerPattern = (dateLocale) =>
  dateLocale === "fr-FR" ? "dd/MM/yyyy" : "MM/dd/yyyy";

export const formatAdmissionDateForPicker = (isoDate, dateLocale) => {
  if (!isoDate) {
    return "";
  }
  const parsed = parseISO(isoDate);
  return isValid(parsed) ? format(parsed, pickerPattern(dateLocale)) : "";
};

export const formatAdmissionDateForApi = (pickerDate, dateLocale) => {
  if (!pickerDate) {
    return "";
  }
  const pattern = pickerPattern(dateLocale);
  const parsed = parse(pickerDate, pattern, new Date());
  return isValid(parsed) && format(parsed, pattern) === pickerDate
    ? format(parsed, "yyyy-MM-dd")
    : "";
};

const MicrobiologyOrderDetailFields = ({
  fields,
  onChange,
  methods = [],
  patientOrigins = [],
  idPrefix = "microbiology-order-detail",
  isReadOnly = false,
  showCultureMethod = true,
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
