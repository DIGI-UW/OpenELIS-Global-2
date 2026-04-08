import React, { useState, useEffect, useContext } from "react";
import { format } from "date-fns";
import {
  Button,
  DatePicker,
  DatePickerInput,
  Select,
  SelectItem,
  TextArea,
  TextInput,
  Tag,
  Tile,
} from "@carbon/react";
import { Warning, CheckmarkFilled } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
  postToOpenElisServerFormData,
} from "../../utils/Utils";
import { NotificationContext } from "../../layout/Layout";
import { NotificationKinds } from "../../common/CustomNotification";
import UserSessionDetailsContext from "../../../UserSessionDetailsContext";
import NceFileAttachment from "./NceFileAttachment";
import "./InlineNceForm.css";

/**
 * Inline NCE form for embedding in the result entry page.
 * Contains all fields from the main Report NCE form.
 * Auto-populates context from the result row data.
 *
 * Props:
 *  - resultRow: the result row data (accessionNumber, patientName, testName, resultValue, etc.)
 *  - onClose: callback when form is cancelled or submitted
 *  - onSubmitSuccess: optional callback after successful NCE creation
 */
const InlineNceForm = ({ resultRow, onClose, onSubmitSuccess }) => {
  const intl = useIntl();
  const { addNotification, setNotificationVisible } =
    useContext(NotificationContext);
  const { userSessionDetails } = useContext(UserSessionDetailsContext);

  const [categories, setCategories] = useState([]);
  const [types, setTypes] = useState([]);
  const [reportingUnits, setReportingUnits] = useState([]);
  const [submitting, setSubmitting] = useState(false);
  const [errors, setErrors] = useState({});

  const [nceForm, setNceForm] = useState({
    nceNumber: "",
    reporterName: "",
    dateOfEvent: format(new Date(), "MM/dd/yyyy"),
    reportingUnit: "",
    title: "",
    description: "",
    immediateAction: "",
    suspectedCauses: "",
    proposedAction: "",
    severity: "",
    categoryId: "",
    typeId: "",
    attachments: [],
  });

  // Build context string from result row
  const contextString = resultRow
    ? `Lab #: ${resultRow.accessionNumber || ""} - Test: ${resultRow.testName || ""}, Result: ${resultRow.resultValue || ""}, Patient: ${resultRow.patientName || ""}`
    : "";

  // Set reporter name from session
  useEffect(() => {
    if (userSessionDetails && userSessionDetails.authenticated) {
      const fullName = [
        userSessionDetails.firstName,
        userSessionDetails.lastName,
      ]
        .filter(Boolean)
        .join(" ");
      setNceForm((prev) => ({
        ...prev,
        reporterName: fullName || userSessionDetails.loginName || "",
      }));
    }
  }, [userSessionDetails]);

  // Load categories, reporting units, and generate NCE number
  useEffect(() => {
    getFromOpenElisServer("/rest/nce/categories", (response) => {
      if (response && Array.isArray(response)) {
        setCategories(response);
      } else {
        getFromOpenElisServer(
          "/rest/displayList/NCE_CATEGORY",
          (fallbackResponse) => {
            if (fallbackResponse && Array.isArray(fallbackResponse)) {
              setCategories(
                fallbackResponse.map((item) => ({
                  id: item.id,
                  name: item.value,
                  types: [],
                })),
              );
            }
          },
        );
      }
    });

    getFromOpenElisServer(
      "/rest/displayList/TEST_SECTION_ACTIVE",
      (response) => {
        if (response && Array.isArray(response)) {
          setReportingUnits(response);
        }
      },
    );

    getFromOpenElisServer("/rest/nce/generate-number", (response) => {
      if (response && response.nceNumber) {
        setNceForm((prev) => ({ ...prev, nceNumber: response.nceNumber }));
      } else {
        const nceNumber = `NCE-${Date.now()}`;
        setNceForm((prev) => ({ ...prev, nceNumber }));
      }
    });
  }, []);

  // Update types when category changes
  useEffect(() => {
    if (nceForm.categoryId) {
      const category = categories.find((cat) => cat.id === nceForm.categoryId);
      if (category && category.types && category.types.length > 0) {
        setTypes(category.types);
      } else {
        getFromOpenElisServer(
          `/rest/displayList/NCE_TYPE_FOR_CATEGORY?categoryId=${nceForm.categoryId}`,
          (response) => {
            if (response && Array.isArray(response)) {
              setTypes(
                response.map((item) => ({ id: item.id, name: item.value })),
              );
            } else {
              setTypes([]);
            }
          },
        );
      }
    } else {
      setTypes([]);
    }
  }, [nceForm.categoryId, categories]);

  const handleFormChange = (field, value) => {
    setNceForm((prev) => ({ ...prev, [field]: value }));
    if (errors[field]) {
      setErrors((prev) => ({ ...prev, [field]: undefined }));
    }
  };

  const handleSubmit = () => {
    const newErrors = {};
    if (!nceForm.dateOfEvent) {
      newErrors.dateOfEvent = intl.formatMessage({
        id: "nce.error.dateOfEvent.required",
        defaultMessage: "Date of event is required",
      });
    }
    if (!nceForm.reportingUnit) {
      newErrors.reportingUnit = intl.formatMessage({
        id: "nce.error.reportingUnit.required",
        defaultMessage: "Reporting unit is required",
      });
    }
    if (!nceForm.description) {
      newErrors.description = intl.formatMessage({
        id: "nce.error.description.required",
        defaultMessage: "Description is required",
      });
    }
    if (!nceForm.severity) {
      newErrors.severity = intl.formatMessage({
        id: "nce.error.severity.required",
        defaultMessage: "Severity is required",
      });
    }
    if (!nceForm.categoryId) {
      newErrors.categoryId = intl.formatMessage({
        id: "nce.error.category.required",
        defaultMessage: "Category is required",
      });
    }

    if (Object.keys(newErrors).length > 0) {
      setErrors(newErrors);
      return;
    }

    setSubmitting(true);

    const body = {
      nceNumber: nceForm.nceNumber,
      reporterName: nceForm.reporterName,
      dateOfEvent: nceForm.dateOfEvent,
      reportingUnit: nceForm.reportingUnit,
      labOrderNumber: resultRow?.accessionNumber || "",
      specimenId: resultRow?.sampleItemId ? String(resultRow.sampleItemId) : "",
      analysisId: resultRow?.analysisId ? String(resultRow.analysisId) : "",
      title: nceForm.title,
      description: nceForm.description,
      immediateAction: nceForm.immediateAction,
      suspectedCauses: nceForm.suspectedCauses,
      proposedAction: nceForm.proposedAction,
      severity: nceForm.severity,
      nceCategoryId: nceForm.categoryId,
      nceTypeId: nceForm.typeId,
    };

    const localAttachments = (nceForm.attachments || []).filter(
      (att) => att.isNew && att.file,
    );

    const handleSuccess = () => {
      setSubmitting(false);
      addNotification({
        kind: NotificationKinds.success,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({
          id: "nonconform.order.save.success",
          defaultMessage: "NCE reported successfully",
        }),
      });
      setNotificationVisible(true);
      onSubmitSuccess?.();
      onClose?.();
    };

    const handleError = () => {
      setSubmitting(false);
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({
          id: "nonconform.order.save.fail",
          defaultMessage: "Failed to report NCE",
        }),
      });
      setNotificationVisible(true);
    };

    if (localAttachments.length > 0) {
      const formData = new FormData();
      formData.append("nceData", JSON.stringify(body));
      localAttachments.forEach((att) => {
        formData.append("files", att.file);
      });

      postToOpenElisServerFormData(
        "/rest/reportnonconformingevent/with-attachments",
        formData,
        (status) => {
          if (status === 200 || status === 201) {
            handleSuccess();
          } else {
            handleError();
          }
        },
      );
    } else {
      postToOpenElisServerJsonResponse(
        "/rest/reportnonconformingevent",
        JSON.stringify(body),
        (data) => {
          if (data && data.success) {
            handleSuccess();
          } else {
            handleError();
          }
        },
      );
    }
  };

  return (
    <div className="inline-nce-form">
      <div className="inline-nce-header">
        <Warning size={20} />
        <h4>
          <FormattedMessage
            id="nce.inline.title"
            defaultMessage="Report Non-Conformity Event"
          />
        </h4>
      </div>

      {/* Auto-populated context */}
      <Tile className="inline-nce-context">
        <Tag type="green">
          <FormattedMessage
            id="nce.inline.context"
            defaultMessage="CONTEXT (Auto-populated)"
          />
        </Tag>
        <p className="inline-nce-context-text">{contextString}</p>
      </Tile>

      {/* Section 1: Reporter & Event Context */}
      <div className="inline-nce-row">
        <TextInput
          id="inline-nce-number"
          labelText={intl.formatMessage({
            id: "nce.field.nceNumber",
            defaultMessage: "NCE Number",
          })}
          value={nceForm.nceNumber}
          readOnly
        />
        <TextInput
          id="inline-nce-reporter"
          labelText={intl.formatMessage({
            id: "nce.field.reporterName",
            defaultMessage: "Reporter Name",
          })}
          value={nceForm.reporterName}
          onChange={(e) => handleFormChange("reporterName", e.target.value)}
        />
        <DatePicker
          id="inline-nce-date-picker"
          datePickerType="single"
          dateFormat="m/d/Y"
          value={nceForm.dateOfEvent}
          maxDate={format(new Date(), "MM/dd/yyyy")}
          onChange={(dates) => {
            if (dates && dates[0]) {
              const formatted = format(new Date(dates[0]), "MM/dd/yyyy");
              handleFormChange("dateOfEvent", formatted);
            }
          }}
        >
          <DatePickerInput
            id="inline-nce-date"
            placeholder="mm/dd/yyyy"
            labelText={
              intl.formatMessage({
                id: "nce.field.dateOfEvent",
                defaultMessage: "Date of Event",
              }) + " *"
            }
            invalid={!!errors.dateOfEvent}
            invalidText={errors.dateOfEvent}
          />
        </DatePicker>
        <Select
          id="inline-nce-reporting-unit"
          labelText={
            intl.formatMessage({
              id: "nce.field.reportingUnit",
              defaultMessage: "Reporting Unit",
            }) + " *"
          }
          value={nceForm.reportingUnit}
          onChange={(e) => handleFormChange("reportingUnit", e.target.value)}
          invalid={!!errors.reportingUnit}
          invalidText={errors.reportingUnit}
        >
          <SelectItem value="" text="" />
          {reportingUnits.map((option) => (
            <SelectItem key={option.id} value={option.id} text={option.value} />
          ))}
        </Select>
      </div>

      {/* Section 2: Classification */}
      <div className="inline-nce-row">
        <Select
          id="inline-nce-category"
          labelText={
            intl.formatMessage({
              id: "nce.field.category",
              defaultMessage: "Category",
            }) + " *"
          }
          value={nceForm.categoryId}
          onChange={(e) => {
            handleFormChange("categoryId", e.target.value);
            setNceForm((prev) => ({ ...prev, typeId: "" }));
          }}
          invalid={!!errors.categoryId}
          invalidText={errors.categoryId}
        >
          <SelectItem
            value=""
            text={intl.formatMessage({
              id: "nce.select.category",
              defaultMessage: "Select category...",
            })}
          />
          {categories.map((cat) => (
            <SelectItem key={cat.id} value={cat.id} text={cat.name} />
          ))}
        </Select>
        <Select
          id="inline-nce-type"
          labelText={intl.formatMessage({
            id: "nce.field.type",
            defaultMessage: "Subcategory",
          })}
          value={nceForm.typeId}
          onChange={(e) => handleFormChange("typeId", e.target.value)}
          disabled={!nceForm.categoryId}
        >
          <SelectItem
            value=""
            text={intl.formatMessage({
              id: "nce.select.type",
              defaultMessage: "Select...",
            })}
          />
          {types.map((type) => (
            <SelectItem key={type.id} value={type.id} text={type.name} />
          ))}
        </Select>
      </div>

      {/* Severity */}
      <div className="inline-nce-section">
        <label className="inline-nce-label">
          <FormattedMessage id="nce.field.severity" defaultMessage="Severity" />{" "}
          *
        </label>
        {errors.severity && (
          <span className="inline-nce-error">{errors.severity}</span>
        )}
        <div className="inline-nce-severity">
          <div
            className={`inline-nce-severity-card ${nceForm.severity === "CRITICAL" ? "selected critical" : ""}`}
            onClick={() => handleFormChange("severity", "CRITICAL")}
          >
            <div className="inline-nce-severity-indicator critical"></div>
            <div className="inline-nce-severity-content">
              <span className="inline-nce-severity-label">
                <FormattedMessage
                  id="nce.severity.critical"
                  defaultMessage="Critical"
                />
              </span>
              <span className="inline-nce-severity-desc">
                <FormattedMessage
                  id="nce.severity.critical.description"
                  defaultMessage="Patient safety risk"
                />
              </span>
            </div>
          </div>

          <div
            className={`inline-nce-severity-card ${nceForm.severity === "MAJOR" ? "selected major" : ""}`}
            onClick={() => handleFormChange("severity", "MAJOR")}
          >
            <div className="inline-nce-severity-indicator major"></div>
            <div className="inline-nce-severity-content">
              <span className="inline-nce-severity-label">
                <FormattedMessage
                  id="nce.severity.major"
                  defaultMessage="Major"
                />
              </span>
              <span className="inline-nce-severity-desc">
                <FormattedMessage
                  id="nce.severity.major.description"
                  defaultMessage="Significant impact"
                />
              </span>
            </div>
          </div>

          <div
            className={`inline-nce-severity-card ${nceForm.severity === "MINOR" ? "selected minor" : ""}`}
            onClick={() => handleFormChange("severity", "MINOR")}
          >
            <div className="inline-nce-severity-indicator minor"></div>
            <div className="inline-nce-severity-content">
              <span className="inline-nce-severity-label">
                <FormattedMessage
                  id="nce.severity.minor"
                  defaultMessage="Minor"
                />
              </span>
              <span className="inline-nce-severity-desc">
                <FormattedMessage
                  id="nce.severity.minor.description"
                  defaultMessage="Limited impact"
                />
              </span>
            </div>
          </div>
        </div>
      </div>

      {/* Section 3: Details */}
      <TextInput
        id="inline-nce-title"
        labelText={intl.formatMessage({
          id: "nce.field.title",
          defaultMessage: "Title",
        })}
        placeholder={intl.formatMessage({
          id: "nce.field.title.placeholder",
          defaultMessage: "Brief description of the event...",
        })}
        value={nceForm.title}
        onChange={(e) => handleFormChange("title", e.target.value)}
        style={{ marginBottom: "0.5rem" }}
      />

      <TextArea
        id="inline-nce-description"
        labelText={
          intl.formatMessage({
            id: "nce.field.description",
            defaultMessage: "Description",
          }) + " *"
        }
        placeholder={intl.formatMessage({
          id: "nce.field.description.placeholder",
          defaultMessage:
            "Describe what happened, when it was detected, and any relevant context...",
        })}
        value={nceForm.description}
        onChange={(e) => handleFormChange("description", e.target.value)}
        rows={3}
        invalid={!!errors.description}
        invalidText={errors.description}
        style={{ marginBottom: "0.5rem" }}
      />

      <TextArea
        id="inline-nce-immediate-action"
        labelText={intl.formatMessage({
          id: "nce.field.immediateAction",
          defaultMessage: "Immediate Action Taken",
        })}
        placeholder={intl.formatMessage({
          id: "nce.field.immediateAction.placeholder",
          defaultMessage: "What corrective steps were taken immediately...",
        })}
        value={nceForm.immediateAction}
        onChange={(e) => handleFormChange("immediateAction", e.target.value)}
        rows={2}
        style={{ marginBottom: "0.5rem" }}
      />

      <div className="inline-nce-row">
        <TextArea
          id="inline-nce-suspected-causes"
          labelText={intl.formatMessage({
            id: "nce.field.suspectedCauses",
            defaultMessage: "Suspected Causes",
          })}
          placeholder={intl.formatMessage({
            id: "nce.field.suspectedCauses.placeholder",
            defaultMessage: "Reporter's initial hypotheses on cause...",
          })}
          value={nceForm.suspectedCauses}
          onChange={(e) => handleFormChange("suspectedCauses", e.target.value)}
          rows={2}
        />
        <TextArea
          id="inline-nce-proposed-action"
          labelText={intl.formatMessage({
            id: "nce.field.proposedAction",
            defaultMessage: "Proposed Action",
          })}
          placeholder={intl.formatMessage({
            id: "nce.field.proposedAction.placeholder",
            defaultMessage: "Recommended next steps...",
          })}
          value={nceForm.proposedAction}
          onChange={(e) => handleFormChange("proposedAction", e.target.value)}
          rows={2}
        />
      </div>

      {/* Section 4: Attachments */}
      <div className="inline-nce-section">
        <label className="inline-nce-label">
          <FormattedMessage
            id="nce.section.attachments"
            defaultMessage="Attachments"
          />
        </label>
        <NceFileAttachment
          attachments={nceForm.attachments || []}
          onAttachmentsChange={(attachments) =>
            setNceForm((prev) => ({ ...prev, attachments }))
          }
        />
      </div>

      {/* Linked specimens */}
      {resultRow && (
        <div className="inline-nce-linked">
          <label className="inline-nce-label">
            <FormattedMessage
              id="nce.field.linkedItems"
              defaultMessage="Linked Items"
            />
          </label>
          <div className="inline-nce-linked-item">
            <CheckmarkFilled size={16} />
            <span>
              Sample: {resultRow.accessionNumber}
              {resultRow.sequenceNumber ? `-${resultRow.sequenceNumber}` : ""}
            </span>
          </div>
          <div className="inline-nce-linked-item">
            <CheckmarkFilled size={16} />
            <span>
              Result: {resultRow.testName} — {resultRow.resultValue || ""}
            </span>
          </div>
        </div>
      )}

      {/* Buttons */}
      <div className="inline-nce-buttons">
        <Button kind="ghost" size="sm" onClick={onClose} disabled={submitting}>
          <FormattedMessage id="label.button.cancel" defaultMessage="Cancel" />
        </Button>
        <Button
          kind="danger"
          size="sm"
          onClick={handleSubmit}
          disabled={submitting}
        >
          {submitting ? (
            <FormattedMessage
              id="nce.creating"
              defaultMessage="Creating NCE..."
            />
          ) : (
            <FormattedMessage
              id="nce.button.createNce"
              defaultMessage="Submit NCE"
            />
          )}
        </Button>
      </div>
    </div>
  );
};

export default InlineNceForm;
