/**
 * CorrectiveActionForm Component
 *
 * Form for creating/editing corrective actions for QC violations
 * Task Reference: T146
 * Specification: FR-039 to FR-044, User Story 4
 *
 * Features:
 * - Action type selection (Recalibration, Maintenance, Repeat Control, Reagent Change, Other)
 * - User assignment
 * - Description input
 * - Resolution notes (for completion)
 */

import React, { useState, useEffect } from "react";
import {
  Grid,
  Column,
  Form,
  FormGroup,
  Dropdown,
  TextArea,
  Button,
  Loading,
  InlineNotification,
  ComboBox,
} from "@carbon/react";
import { useIntl } from "react-intl";
import { useHistory, useLocation } from "react-router-dom";
import { Formik } from "formik";
import * as Yup from "yup";
import {
  getFromOpenElisServer,
  postToOpenElisServerFullResponse,
} from "../../utils/Utils";
import PageTitle from "../../common/PageTitle/PageTitle";
import "./CorrectiveActionForm.css";

const CorrectiveActionForm = () => {
  const intl = useIntl();
  const history = useHistory();
  const location = useLocation();

  // Get violation ID from query params
  const params = new URLSearchParams(location.search);
  const violationId = params.get("violationId");

  // State
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);
  const [violation, setViolation] = useState(null);
  const [users, setUsers] = useState([]);

  // Action type options (FR-039)
  const actionTypeOptions = [
    { id: "RECALIBRATION", label: intl.formatMessage({ id: "qc.correctiveAction.type.recalibration" }) },
    { id: "MAINTENANCE", label: intl.formatMessage({ id: "qc.correctiveAction.type.maintenance" }) },
    { id: "REPEAT_CONTROL", label: intl.formatMessage({ id: "qc.correctiveAction.type.repeatControl" }) },
    { id: "REAGENT_CHANGE", label: intl.formatMessage({ id: "qc.correctiveAction.type.reagentChange" }) },
    { id: "OTHER", label: intl.formatMessage({ id: "qc.correctiveAction.type.other" }) },
  ];

  // Validation schema
  const validationSchema = Yup.object().shape({
    actionType: Yup.string().required(
      intl.formatMessage({ id: "qc.correctiveAction.validation.actionTypeRequired" })
    ),
    description: Yup.string().required(
      intl.formatMessage({ id: "qc.correctiveAction.validation.descriptionRequired" })
    ),
    assignedUserId: Yup.string().nullable(),
  });

  // Load violation details
  useEffect(() => {
    if (violationId) {
      setLoading(true);
      getFromOpenElisServer(`/rest/qc/violations/${violationId}`, (response) => {
        if (response && response.data) {
          setViolation(response.data);
        } else if (response && response.id) {
          setViolation(response);
        }
        setLoading(false);
      });
    }
  }, [violationId]);

  // Load users for assignment
  useEffect(() => {
    getFromOpenElisServer("/rest/users?role=BIOLOGIST", (response) => {
      if (response && Array.isArray(response.data)) {
        setUsers(response.data);
      } else if (Array.isArray(response)) {
        setUsers(response);
      }
    });
  }, []);

  // Initial form values
  const initialValues = {
    actionType: "",
    description: "",
    assignedUserId: null,
  };

  // Handle form submit
  const handleSubmit = async (values, { setSubmitting: setFormSubmitting }) => {
    setSubmitting(true);
    setError(null);

    const payload = {
      violationId: violationId,
      actionType: values.actionType,
      description: values.description,
      assignedUserId: values.assignedUserId,
    };

    postToOpenElisServerFullResponse(
      "/rest/qc/corrective-actions",
      JSON.stringify(payload),
      (response) => {
        if (response.ok) {
          response.json().then((data) => {
            if (data.status === "success" || data.id) {
              // Navigate back to violations list or corrective actions list
              history.push("/analyzers/qc/corrective-actions");
            } else {
              setError(data.error || intl.formatMessage({ id: "qc.correctiveAction.error.createFailed" }));
            }
          });
        } else {
          setError(intl.formatMessage({ id: "qc.correctiveAction.error.createFailed" }));
        }
        setSubmitting(false);
        setFormSubmitting(false);
      }
    );
  };

  // Handle cancel
  const handleCancel = () => {
    history.goBack();
  };

  if (loading) {
    return (
      <div className="corrective-action-form-loading" data-testid="corrective-action-form-loading">
        <Loading
          description={intl.formatMessage({ id: "qc.correctiveAction.loading" })}
          withOverlay={false}
        />
      </div>
    );
  }

  return (
    <div className="corrective-action-form" data-testid="corrective-action-form">
      {/* Header */}
      <div className="corrective-action-form-header" data-testid="corrective-action-form-header">
        <PageTitle
          breadcrumbs={[
            {
              label: intl.formatMessage({ id: "analyzer.page.hierarchy.root" }),
              link: "/analyzers",
            },
            {
              label: intl.formatMessage({ id: "qc.dashboard.title" }),
              link: "/analyzers/qc",
            },
            {
              label: intl.formatMessage({ id: "qc.correctiveActions.title" }),
              link: "/analyzers/qc/corrective-actions",
            },
            {
              label: intl.formatMessage({ id: "qc.correctiveAction.new.title" }),
            },
          ]}
          subtitle={intl.formatMessage({ id: "qc.correctiveAction.new.subtitle" })}
        />
      </div>

      {/* Error notification */}
      {error && (
        <InlineNotification
          kind="error"
          title={intl.formatMessage({ id: "qc.correctiveAction.error.title" })}
          subtitle={error}
          onClose={() => setError(null)}
          data-testid="corrective-action-form-error"
        />
      )}

      {/* Violation info */}
      {violation && (
        <div className="corrective-action-form-violation-info" data-testid="corrective-action-form-violation-info">
          <h4>{intl.formatMessage({ id: "qc.correctiveAction.violationInfo" })}</h4>
          <Grid>
            <Column lg={4} md={4} sm={4}>
              <div className="info-item">
                <span className="info-label">{intl.formatMessage({ id: "qc.violations.detail.rule" })}</span>
                <span className="info-value">{violation.ruleCode}</span>
              </div>
            </Column>
            <Column lg={4} md={4} sm={4}>
              <div className="info-item">
                <span className="info-label">{intl.formatMessage({ id: "qc.violations.detail.analyzer" })}</span>
                <span className="info-value">{violation.analyzerName || violation.analyzer?.name || "-"}</span>
              </div>
            </Column>
            <Column lg={4} md={4} sm={4}>
              <div className="info-item">
                <span className="info-label">{intl.formatMessage({ id: "qc.violations.detail.severity" })}</span>
                <span className="info-value">{violation.severity}</span>
              </div>
            </Column>
          </Grid>
        </div>
      )}

      {/* Form */}
      <Formik
        initialValues={initialValues}
        validationSchema={validationSchema}
        onSubmit={handleSubmit}
      >
        {({
          values,
          errors,
          touched,
          handleChange,
          handleBlur,
          handleSubmit,
          setFieldValue,
          isSubmitting,
        }) => (
          <Form onSubmit={handleSubmit} data-testid="corrective-action-form-form">
            <Grid>
              {/* Action Type (FR-039) */}
              <Column lg={8} md={4} sm={4}>
                <FormGroup legendText="">
                  <Dropdown
                    id="action-type"
                    titleText={intl.formatMessage({ id: "qc.correctiveAction.field.actionType" })}
                    label={intl.formatMessage({ id: "qc.correctiveAction.field.selectActionType" })}
                    items={actionTypeOptions}
                    itemToString={(item) => item?.label || ""}
                    selectedItem={actionTypeOptions.find((o) => o.id === values.actionType)}
                    onChange={({ selectedItem }) => setFieldValue("actionType", selectedItem?.id || "")}
                    invalid={touched.actionType && !!errors.actionType}
                    invalidText={errors.actionType}
                    data-testid="corrective-action-type-dropdown"
                  />
                </FormGroup>
              </Column>

              {/* Assigned User (FR-040) */}
              <Column lg={8} md={4} sm={4}>
                <FormGroup legendText="">
                  <ComboBox
                    id="assigned-user"
                    titleText={intl.formatMessage({ id: "qc.correctiveAction.field.assignedUser" })}
                    placeholder={intl.formatMessage({ id: "qc.correctiveAction.field.selectUser" })}
                    items={users}
                    itemToString={(item) => item?.displayName || item?.name || ""}
                    selectedItem={users.find((u) => u.id === values.assignedUserId)}
                    onChange={({ selectedItem }) => setFieldValue("assignedUserId", selectedItem?.id || null)}
                    data-testid="corrective-action-user-combobox"
                  />
                </FormGroup>
              </Column>

              {/* Description */}
              <Column lg={16} md={8} sm={4}>
                <FormGroup legendText="">
                  <TextArea
                    id="description"
                    name="description"
                    labelText={intl.formatMessage({ id: "qc.correctiveAction.field.description" })}
                    placeholder={intl.formatMessage({ id: "qc.correctiveAction.field.descriptionPlaceholder" })}
                    value={values.description}
                    onChange={handleChange}
                    onBlur={handleBlur}
                    invalid={touched.description && !!errors.description}
                    invalidText={errors.description}
                    rows={4}
                    data-testid="corrective-action-description-textarea"
                  />
                </FormGroup>
              </Column>

              {/* Actions */}
              <Column lg={16} md={8} sm={4}>
                <div className="corrective-action-form-actions">
                  <Button
                    kind="secondary"
                    onClick={handleCancel}
                    data-testid="corrective-action-cancel-button"
                  >
                    {intl.formatMessage({ id: "button.cancel" })}
                  </Button>
                  <Button
                    kind="primary"
                    type="submit"
                    disabled={isSubmitting || submitting}
                    data-testid="corrective-action-submit-button"
                  >
                    {submitting
                      ? intl.formatMessage({ id: "button.saving" })
                      : intl.formatMessage({ id: "button.save" })}
                  </Button>
                </div>
              </Column>
            </Grid>
          </Form>
        )}
      </Formik>
    </div>
  );
};

export default CorrectiveActionForm;
