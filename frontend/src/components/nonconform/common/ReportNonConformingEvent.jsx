import React, { useState, useEffect, useContext } from "react";
import { format } from "date-fns";
import {
  Button,
  Column,
  DatePicker,
  DatePickerInput,
  Grid,
  Select,
  SelectItem,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
  TableBody,
  TextArea,
  TextInput,
  Table,
  Checkbox,
  Tile,
  Tag,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  OEToastNotificationKinds,
  OEToastNotification,
} from "../../common/OEToastNotification";
import { NotificationContext } from "../../layout/Layout";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
  postToOpenElisServerFormData,
} from "../../utils/Utils";
import NceFileAttachment from "./NceFileAttachment";
import UserSessionDetailsContext from "../../../UserSessionDetailsContext";
import "./ReportNonConformingEvent.css";

const initialReportFormValues = {
  type: undefined,
  value: "",
  error: undefined,
};

export const ReportNonConformingEvent = () => {
  const [reportFormValues, setReportFormValues] = useState(
    initialReportFormValues,
  );
  const [searchResults, setSearchResults] = useState(null);
  const [linkedSamples, setLinkedSamples] = useState([]);
  const [orderSampleMap, setOrderSampleMap] = useState({});
  const [categories, setCategories] = useState([]);
  const [types, setTypes] = useState([]);
  const [reportingUnits, setReportingUnits] = useState([]);
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);
  const { userSessionDetails } = useContext(UserSessionDetailsContext);

  const intl = useIntl();

  const [nceForm, setnceForm] = useState({
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

  const [errors, setErrors] = useState({});

  // Set reporter name from session when available
  useEffect(() => {
    if (userSessionDetails && userSessionDetails.authenticated) {
      const fullName = [
        userSessionDetails.firstName,
        userSessionDetails.lastName,
      ]
        .filter(Boolean)
        .join(" ");
      setnceForm((prev) => ({
        ...prev,
        reporterName: fullName || userSessionDetails.loginName || "",
      }));
    }
  }, [userSessionDetails]);

  // Load categories, reporting units, and generate NCE number on mount
  useEffect(() => {
    // Fetch categories - try new endpoint first, fall back to displayList
    getFromOpenElisServer("/rest/nce/categories", (response) => {
      if (response && Array.isArray(response)) {
        setCategories(response);
      } else {
        // Fallback: fetch from displayList (prebuilt image compatibility)
        getFromOpenElisServer(
          "/rest/displayList/NCE_CATEGORY",
          (fallbackResponse) => {
            if (fallbackResponse && Array.isArray(fallbackResponse)) {
              // Convert IdValuePair format to expected format
              setCategories(
                fallbackResponse.map((item) => ({
                  id: item.id,
                  name: item.value,
                  types: [], // Types will be fetched separately when category selected
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

    // Generate NCE number - try new endpoint first, fall back to timestamp
    getFromOpenElisServer("/rest/nce/generate-number", (response) => {
      if (response && response.nceNumber) {
        setnceForm((prev) => ({
          ...prev,
          nceNumber: response.nceNumber,
        }));
      } else {
        // Fallback: generate client-side NCE number
        const timestamp = Date.now();
        const nceNumber = `NCE-${timestamp}`;
        setnceForm((prev) => ({
          ...prev,
          nceNumber: nceNumber,
        }));
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
        // Fallback: fetch types for category from displayList
        getFromOpenElisServer(
          `/rest/displayList/NCE_TYPE_FOR_CATEGORY?categoryId=${nceForm.categoryId}`,
          (response) => {
            if (response && Array.isArray(response)) {
              setTypes(
                response.map((item) => ({
                  id: item.id,
                  name: item.value,
                })),
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

  const selectOptions = [
    { text: "Last Name", value: "lastName" },
    { text: "First Name", value: "firstName" },
    { value: "STNumber", text: "Patient Identification Code" },
    { text: "Lab Number", value: "labNumber" },
  ];

  const headers = [
    { key: "labOrderNumber", value: "Lab Number" },
    { key: "type", value: "Specimen type" },
  ];

  const handleSearch = () => {
    if (reportFormValues.type === undefined || reportFormValues.value === "") {
      setReportFormValues({
        ...reportFormValues,
        error: intl.formatMessage({
          id: "error.nonconform.report",
        }),
      });
      return;
    }

    setReportFormValues({ ...reportFormValues, error: undefined });

    getFromOpenElisServer(
      `/rest/nonconformevents?${reportFormValues.type}=${reportFormValues.value}`,
      (data) => {
        if (data && data.length > 0) {
          setSearchResults(data);
        } else {
          setSearchResults(null);
          setReportFormValues({
            ...reportFormValues,
            error: intl.formatMessage({
              id: "error.nonconform.report.data.found",
              defaultMessage: "No data found",
            }),
          });
        }
      },
    );
  };

  const handleLinkSamples = () => {
    const labNo = Object.keys(orderSampleMap)[0];
    if (labNo && orderSampleMap[labNo]?.length > 0) {
      // Check if this lab order is already linked
      const alreadyLinked = linkedSamples.find(
        (s) => s.labOrderNumber === labNo,
      );

      if (alreadyLinked) {
        // Filter out specimen IDs that are already linked
        const existingSpecimenIds = new Set(alreadyLinked.specimenIds);
        const newSpecimenIds = orderSampleMap[labNo].filter(
          (id) => !existingSpecimenIds.has(id),
        );

        if (newSpecimenIds.length === 0) {
          // All specimens already linked - show warning
          addNotification({
            kind: NotificationKinds.warning,
            title: intl.formatMessage({ id: "notification.title" }),
            message: intl.formatMessage({
              id: "nce.link.duplicate.warning",
              defaultMessage: "These specimens are already linked to this NCE",
            }),
          });
          setNotificationVisible(true);
        } else {
          // Add only new specimen IDs to existing linked sample
          setLinkedSamples((prev) =>
            prev.map((s) =>
              s.labOrderNumber === labNo
                ? { ...s, specimenIds: [...s.specimenIds, ...newSpecimenIds] }
                : s,
            ),
          );
        }
      } else {
        // New lab order - add it
        const newLinked = {
          labOrderNumber: labNo,
          specimenIds: orderSampleMap[labNo],
        };
        setLinkedSamples((prev) => [...prev, newLinked]);
      }

      setSearchResults(null);
      setOrderSampleMap({});
      setReportFormValues(initialReportFormValues);
    }
  };

  const handleRemoveLinkedSample = (labOrderNumber) => {
    setLinkedSamples((prev) =>
      prev.filter((s) => s.labOrderNumber !== labOrderNumber),
    );
  };

  const handleNCEFormSubmit = () => {
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

    // Build specimenId from linked samples (comma-separated)
    const specimenId = linkedSamples.flatMap((s) => s.specimenIds).join(",");
    const labOrderNumber =
      linkedSamples.length > 0 ? linkedSamples[0].labOrderNumber : "";

    const body = {
      nceNumber: nceForm.nceNumber,
      reporterName: nceForm.reporterName,
      dateOfEvent: nceForm.dateOfEvent,
      reportingUnit: nceForm.reportingUnit,
      labOrderNumber: labOrderNumber,
      specimenId: specimenId,
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
      setNotificationVisible(true);
      addNotification({
        kind: NotificationKinds.success,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({
          id: "nonconform.order.save.success",
        }),
      });
      // Reset form and reload defaults
      setnceForm({
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
      setLinkedSamples([]);
      setErrors({});

      // Set reporter name from session
      if (userSessionDetails && userSessionDetails.authenticated) {
        const fullName = [
          userSessionDetails.firstName,
          userSessionDetails.lastName,
        ]
          .filter(Boolean)
          .join(" ");
        setnceForm((prev) => ({
          ...prev,
          reporterName: fullName || userSessionDetails.loginName || "",
        }));
      }

      // Generate new NCE number
      getFromOpenElisServer("/rest/nce/generate-number", (response) => {
        if (response && response.nceNumber) {
          setnceForm((prev) => ({
            ...prev,
            nceNumber: response.nceNumber,
          }));
        } else {
          // Fallback: generate client-side NCE number
          const timestamp = Date.now();
          const nceNumber = `NCE-${timestamp}`;
          setnceForm((prev) => ({
            ...prev,
            nceNumber: nceNumber,
          }));
        }
      });
    };

    const handleError = () => {
      setNotificationVisible(true);
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "nonconform.order.save.fail" }),
      });
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
          if (data.success) {
            handleSuccess();
          } else {
            handleError();
          }
        },
      );
    }
  };

  const handleCancel = () => {
    setnceForm({
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
    setLinkedSamples([]);
    setSearchResults(null);
    setErrors({});
    setReportFormValues(initialReportFormValues);

    // Set reporter name from session
    if (userSessionDetails && userSessionDetails.authenticated) {
      const fullName = [
        userSessionDetails.firstName,
        userSessionDetails.lastName,
      ]
        .filter(Boolean)
        .join(" ");
      setnceForm((prev) => ({
        ...prev,
        reporterName: fullName || userSessionDetails.loginName || "",
      }));
    }

    // Generate new NCE number
    getFromOpenElisServer("/rest/nce/generate-number", (response) => {
      if (response && response.nceNumber) {
        setnceForm((prev) => ({
          ...prev,
          nceNumber: response.nceNumber,
        }));
      } else {
        // Fallback: generate client-side NCE number
        const timestamp = Date.now();
        const nceNumber = `NCE-${timestamp}`;
        setnceForm((prev) => ({
          ...prev,
          nceNumber: nceNumber,
        }));
      }
    });
  };

  return (
    <>
      {notificationVisible === true ? <OEToastNotification /> : ""}
      <div className="nce-form-container">
        {/* Header */}
        <div className="nce-form-header">
          <h2>
            <FormattedMessage
              id="nce.form.title"
              defaultMessage="Report Non-Conformity Event"
            />
          </h2>
          <p className="nce-form-subtitle">
            <FormattedMessage
              id="nce.form.subtitle"
              defaultMessage="Document a quality event and link to affected samples and results"
            />
          </p>
        </div>

        {/* Section 1: Reporter & Event Context */}
        <Tile className="nce-form-section">
          <div className="nce-section-header">
            <span className="nce-section-number">01</span>
            <h3>
              <FormattedMessage
                id="nce.section.reporterContext"
                defaultMessage="Reporter & Event Context"
              />
            </h3>
          </div>

          <Grid fullWidth>
            <Column lg={4} md={4} sm={4}>
              <TextInput
                id="nce-number"
                labelText={intl.formatMessage({
                  id: "nce.field.nceNumber",
                  defaultMessage: "NCE Number",
                })}
                value={nceForm.nceNumber}
                readOnly
                className="nce-readonly-field"
              />
            </Column>
            <Column lg={4} md={4} sm={4}>
              <TextInput
                id="reporter-name"
                labelText={intl.formatMessage({
                  id: "nce.field.reporterName",
                  defaultMessage: "Reporter Name",
                })}
                value={nceForm.reporterName}
                onChange={(e) =>
                  setnceForm((prev) => ({
                    ...prev,
                    reporterName: e.target.value,
                  }))
                }
              />
            </Column>
            <Column lg={4} md={4} sm={4}>
              <DatePicker
                id="date-of-event-picker"
                datePickerType="single"
                dateFormat="m/d/Y"
                value={nceForm.dateOfEvent}
                maxDate={format(new Date(), "MM/dd/yyyy")}
                onChange={(dates) => {
                  if (dates && dates[0]) {
                    const formatted = format(new Date(dates[0]), "MM/dd/yyyy");
                    setnceForm((prev) => ({
                      ...prev,
                      dateOfEvent: formatted,
                    }));
                    setErrors((prev) => ({ ...prev, dateOfEvent: "" }));
                  }
                }}
              >
                <DatePickerInput
                  id="date-of-event"
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
            </Column>
            <Column lg={4} md={4} sm={4}>
              <Select
                id="reporting-unit"
                labelText={
                  intl.formatMessage({
                    id: "nce.field.reportingUnit",
                    defaultMessage: "Reporting Unit",
                  }) + " *"
                }
                value={nceForm.reportingUnit}
                onChange={(e) => {
                  setnceForm((prev) => ({
                    ...prev,
                    reportingUnit: e.target.value,
                  }));
                  setErrors((prev) => ({ ...prev, reportingUnit: "" }));
                }}
                invalid={!!errors.reportingUnit}
                invalidText={errors.reportingUnit}
              >
                <SelectItem value="" text="" />
                {reportingUnits.map((option) => (
                  <SelectItem
                    key={option.id}
                    value={option.id}
                    text={option.value}
                  />
                ))}
              </Select>
            </Column>
          </Grid>
        </Tile>

        {/* Section 2: Classification */}
        <Tile className="nce-form-section">
          <div className="nce-section-header">
            <span className="nce-section-number">02</span>
            <h3>
              <FormattedMessage
                id="nce.section.classification"
                defaultMessage="Classification"
              />
            </h3>
          </div>

          <Grid fullWidth>
            <Column lg={8} md={4} sm={4}>
              <Select
                id="nce-category"
                labelText={
                  intl.formatMessage({
                    id: "nce.field.category",
                    defaultMessage: "Category",
                  }) + " *"
                }
                value={nceForm.categoryId}
                onChange={(e) => {
                  setnceForm((prev) => ({
                    ...prev,
                    categoryId: e.target.value,
                    typeId: "",
                  }));
                  setErrors((prev) => ({ ...prev, categoryId: "" }));
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
            </Column>
            <Column lg={8} md={4} sm={4}>
              <Select
                id="nce-type"
                labelText={intl.formatMessage({
                  id: "nce.field.subcategory",
                  defaultMessage: "Subcategory",
                })}
                value={nceForm.typeId}
                onChange={(e) =>
                  setnceForm((prev) => ({
                    ...prev,
                    typeId: e.target.value,
                  }))
                }
                disabled={!nceForm.categoryId}
              >
                <SelectItem
                  value=""
                  text={intl.formatMessage({
                    id: "nce.select.subcategory",
                    defaultMessage: "Select subcategory...",
                  })}
                />
                {types.map((type) => (
                  <SelectItem key={type.id} value={type.id} text={type.name} />
                ))}
              </Select>
            </Column>
          </Grid>

          <div className="nce-severity-section">
            <label className="nce-field-label">
              <FormattedMessage
                id="nce.field.severity"
                defaultMessage="Severity"
              />{" "}
              *
            </label>
            <div className="nce-severity-options">
              <div
                className={`nce-severity-card ${nceForm.severity === "CRITICAL" ? "selected critical" : ""}`}
                onClick={() => {
                  setnceForm((prev) => ({ ...prev, severity: "CRITICAL" }));
                  setErrors((prev) => ({ ...prev, severity: "" }));
                }}
              >
                <div className="nce-severity-indicator critical"></div>
                <div className="nce-severity-content">
                  <span className="nce-severity-label">
                    <FormattedMessage
                      id="nce.severity.critical"
                      defaultMessage="Critical"
                    />
                  </span>
                  <span className="nce-severity-description">
                    <FormattedMessage
                      id="nce.severity.critical.desc"
                      defaultMessage="Patient safety risk, regulatory violation"
                    />
                  </span>
                </div>
              </div>

              <div
                className={`nce-severity-card ${nceForm.severity === "MAJOR" ? "selected major" : ""}`}
                onClick={() => {
                  setnceForm((prev) => ({ ...prev, severity: "MAJOR" }));
                  setErrors((prev) => ({ ...prev, severity: "" }));
                }}
              >
                <div className="nce-severity-indicator major"></div>
                <div className="nce-severity-content">
                  <span className="nce-severity-label">
                    <FormattedMessage
                      id="nce.severity.major"
                      defaultMessage="Major"
                    />
                  </span>
                  <span className="nce-severity-description">
                    <FormattedMessage
                      id="nce.severity.major.desc"
                      defaultMessage="Significant quality or operational impact"
                    />
                  </span>
                </div>
              </div>

              <div
                className={`nce-severity-card ${nceForm.severity === "MINOR" ? "selected minor" : ""}`}
                onClick={() => {
                  setnceForm((prev) => ({ ...prev, severity: "MINOR" }));
                  setErrors((prev) => ({ ...prev, severity: "" }));
                }}
              >
                <div className="nce-severity-indicator minor"></div>
                <div className="nce-severity-content">
                  <span className="nce-severity-label">
                    <FormattedMessage
                      id="nce.severity.minor"
                      defaultMessage="Minor"
                    />
                  </span>
                  <span className="nce-severity-description">
                    <FormattedMessage
                      id="nce.severity.minor.desc"
                      defaultMessage="Limited impact, easily corrected"
                    />
                  </span>
                </div>
              </div>
            </div>
            {errors.severity && (
              <div className="nce-error-text">{errors.severity}</div>
            )}
          </div>
        </Tile>

        {/* Section 3: Details */}
        <Tile className="nce-form-section">
          <div className="nce-section-header">
            <span className="nce-section-number">03</span>
            <h3>
              <FormattedMessage
                id="nce.section.details"
                defaultMessage="Details"
              />
            </h3>
          </div>

          <Grid fullWidth>
            <Column lg={16} md={8} sm={4}>
              <TextInput
                id="nce-title"
                labelText={intl.formatMessage({
                  id: "nce.field.title",
                  defaultMessage: "Title",
                })}
                placeholder={intl.formatMessage({
                  id: "nce.field.title.placeholder",
                  defaultMessage: "Brief description of the event...",
                })}
                value={nceForm.title}
                onChange={(e) =>
                  setnceForm((prev) => ({
                    ...prev,
                    title: e.target.value,
                  }))
                }
              />
            </Column>

            <Column lg={16} md={8} sm={4}>
              <TextArea
                id="nce-description"
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
                onChange={(e) => {
                  setnceForm((prev) => ({
                    ...prev,
                    description: e.target.value,
                  }));
                  setErrors((prev) => ({ ...prev, description: "" }));
                }}
                rows={4}
                invalid={!!errors.description}
                invalidText={errors.description}
              />
            </Column>

            <Column lg={16} md={8} sm={4}>
              <TextArea
                id="nce-immediate-action"
                labelText={intl.formatMessage({
                  id: "nce.field.immediateAction",
                  defaultMessage: "Immediate Action Taken",
                })}
                placeholder={intl.formatMessage({
                  id: "nce.field.immediateAction.placeholder",
                  defaultMessage:
                    "What corrective steps were taken immediately...",
                })}
                value={nceForm.immediateAction}
                onChange={(e) =>
                  setnceForm((prev) => ({
                    ...prev,
                    immediateAction: e.target.value,
                  }))
                }
                rows={3}
              />
            </Column>

            <Column lg={8} md={4} sm={4}>
              <TextArea
                id="nce-suspected-causes"
                labelText={intl.formatMessage({
                  id: "nce.field.suspectedCauses",
                  defaultMessage: "Suspected Causes",
                })}
                placeholder={intl.formatMessage({
                  id: "nce.field.suspectedCauses.placeholder",
                  defaultMessage: "Reporter's initial hypotheses on cause...",
                })}
                value={nceForm.suspectedCauses}
                onChange={(e) =>
                  setnceForm((prev) => ({
                    ...prev,
                    suspectedCauses: e.target.value,
                  }))
                }
                rows={3}
                helperText={intl.formatMessage({
                  id: "nce.field.suspectedCauses.helper",
                  defaultMessage:
                    "Initial assessment only - formal root cause determined during investigation",
                })}
              />
            </Column>

            <Column lg={8} md={4} sm={4}>
              <TextArea
                id="nce-proposed-action"
                labelText={intl.formatMessage({
                  id: "nce.field.proposedAction",
                  defaultMessage: "Proposed Action",
                })}
                placeholder={intl.formatMessage({
                  id: "nce.field.proposedAction.placeholder",
                  defaultMessage: "Recommended next steps...",
                })}
                value={nceForm.proposedAction}
                onChange={(e) =>
                  setnceForm((prev) => ({
                    ...prev,
                    proposedAction: e.target.value,
                  }))
                }
                rows={3}
                helperText={intl.formatMessage({
                  id: "nce.field.proposedAction.helper",
                  defaultMessage:
                    "Separate from CAPA actions assigned during corrective action workflow",
                })}
              />
            </Column>
          </Grid>
        </Tile>

        {/* Section 4: Attachments */}
        <Tile className="nce-form-section">
          <div className="nce-section-header">
            <span className="nce-section-number">04</span>
            <h3>
              <FormattedMessage
                id="nce.section.attachments"
                defaultMessage="Attachments"
              />
            </h3>
          </div>

          <NceFileAttachment
            attachments={nceForm.attachments || []}
            onAttachmentsChange={(attachments) =>
              setnceForm((prev) => ({
                ...prev,
                attachments,
              }))
            }
          />
        </Tile>

        {/* Section 5: Link to Samples/Results */}
        <Tile className="nce-form-section">
          <div className="nce-section-header">
            <span className="nce-section-number">05</span>
            <h3>
              <FormattedMessage
                id="nce.section.linkSamples"
                defaultMessage="Link to Samples / Results (Optional)"
              />
            </h3>
          </div>

          {/* Display linked samples */}
          {linkedSamples.length > 0 && (
            <div className="nce-linked-samples">
              {linkedSamples.map((sample) => (
                <Tag
                  key={sample.labOrderNumber}
                  type="blue"
                  size="md"
                  filter
                  onClose={() =>
                    handleRemoveLinkedSample(sample.labOrderNumber)
                  }
                >
                  {sample.labOrderNumber}
                </Tag>
              ))}
            </div>
          )}

          {/* Search form */}
          <div className="nce-search-section">
            <Grid fullWidth>
              <Column lg={4} md={4} sm={2}>
                <Select
                  id="search-type"
                  labelText={intl.formatMessage({
                    id: "label.form.searchby",
                    defaultMessage: "Search by",
                  })}
                  value={reportFormValues.type || ""}
                  onChange={(e) =>
                    setReportFormValues({
                      ...reportFormValues,
                      type: e.target.value,
                    })
                  }
                >
                  <SelectItem value="" text="" />
                  {selectOptions.map((option) => (
                    <SelectItem
                      key={option.value}
                      value={option.value}
                      text={option.text}
                    />
                  ))}
                </Select>
              </Column>
              <Column lg={4} md={4} sm={2}>
                <TextInput
                  id="search-value"
                  labelText={intl.formatMessage({
                    id: "testcalculation.label.textValue",
                    defaultMessage: "Value",
                  })}
                  value={reportFormValues.value}
                  onChange={(e) =>
                    setReportFormValues({
                      ...reportFormValues,
                      value: e.target.value,
                    })
                  }
                />
              </Column>
              <Column lg={4} md={4} sm={2} className="nce-search-button-col">
                <Button kind="tertiary" size="md" onClick={handleSearch}>
                  <FormattedMessage
                    id="label.button.search"
                    defaultMessage="Search"
                  />
                </Button>
              </Column>
            </Grid>

            {reportFormValues.error && (
              <div className="nce-error-text" style={{ marginTop: "0.5rem" }}>
                {reportFormValues.error}
              </div>
            )}
          </div>

          {/* Search results table */}
          {searchResults && searchResults.length > 0 && (
            <div className="nce-search-results">
              <Table size="sm">
                <TableHead>
                  <TableRow>
                    <TableHeader />
                    {headers.map((header) => (
                      <TableHeader key={header.key}>{header.value}</TableHeader>
                    ))}
                  </TableRow>
                </TableHead>
                <TableBody>
                  {searchResults.map((row) => (
                    <TableRow key={row.id}>
                      <TableCell />
                      {headers.map((header) => (
                        <TableCell key={header.key}>
                          {header.key === "type"
                            ? row.sampleItems.map((item) => (
                                <Checkbox
                                  key={item.id}
                                  id={`${row.labOrderNumber}-${item.number}`}
                                  labelText={`${item.type} (${row.labOrderNumber}-${item.number})`}
                                  checked={
                                    orderSampleMap[
                                      row.labOrderNumber
                                    ]?.includes(item.id) || false
                                  }
                                  onChange={(e) => {
                                    const newMap = { ...orderSampleMap };
                                    if (e.target.checked) {
                                      if (!newMap[row.labOrderNumber]) {
                                        newMap[row.labOrderNumber] = [];
                                      }
                                      newMap[row.labOrderNumber].push(item.id);
                                    } else {
                                      const idx = newMap[
                                        row.labOrderNumber
                                      ]?.indexOf(item.id);
                                      if (idx > -1) {
                                        newMap[row.labOrderNumber].splice(
                                          idx,
                                          1,
                                        );
                                      }
                                    }
                                    setOrderSampleMap(newMap);
                                  }}
                                />
                              ))
                            : row[header.key]}
                        </TableCell>
                      ))}
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
              <Button
                kind="tertiary"
                size="sm"
                onClick={handleLinkSamples}
                style={{ marginTop: "1rem" }}
              >
                <FormattedMessage
                  id="nce.button.linkSelected"
                  defaultMessage="Link Selected Samples"
                />
              </Button>
            </div>
          )}

          {linkedSamples.length === 0 && !searchResults && (
            <p className="nce-helper-text">
              <FormattedMessage
                id="nce.linkSamples.helper"
                defaultMessage="Search for orders above to link samples to this NCE. This step is optional."
              />
            </p>
          )}
        </Tile>

        {/* Footer Actions */}
        <div className="nce-form-actions">
          <Button kind="secondary" onClick={handleCancel}>
            <FormattedMessage
              id="label.button.cancel"
              defaultMessage="Cancel"
            />
          </Button>
          <Button kind="primary" onClick={handleNCEFormSubmit}>
            <FormattedMessage
              id="nce.button.submit"
              defaultMessage="Submit NCE"
            />
          </Button>
        </div>
      </div>
    </>
  );
};

export default ReportNonConformingEvent;
