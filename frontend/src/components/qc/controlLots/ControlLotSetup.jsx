/**
 * ControlLotSetup Component
 *
 * Form for setting up control lots with statistics configuration
 * Task Reference: T054
 * Specification: FR-001 to FR-007, User Story 6
 *
 * Features:
 * - Control lot creation with lot number, material, expiration
 * - Statistics calculation method configuration
 * - Mean and SD entry (manufacturer or calculated)
 * - Association with analyzer/test combinations
 */

import React, { useState, useEffect } from "react";
import {
  Grid,
  Column,
  Form,
  FormGroup,
  TextInput,
  DatePicker,
  DatePickerInput,
  Dropdown,
  NumberInput,
  Button,
  Loading,
  InlineNotification,
  Toggle,
  Tile,
} from "@carbon/react";
import { Settings } from "@carbon/icons-react";
import { useIntl } from "react-intl";
import { useHistory, useParams } from "react-router-dom";
import { Formik } from "formik";
import * as Yup from "yup";
import {
  getFromOpenElisServer,
  postToOpenElisServerFullResponse,
} from "../../utils/Utils";
import StatisticsConfigModal from "./StatisticsConfigModal";
import PageTitle from "../../common/PageTitle/PageTitle";
import "./ControlLotSetup.css";

const ControlLotSetup = () => {
  const intl = useIntl();
  const history = useHistory();
  const { lotId } = useParams();

  const isEditMode = !!lotId;

  // State
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);
  const [existingLot, setExistingLot] = useState(null);
  const [analyzers, setAnalyzers] = useState([]);
  const [tests, setTests] = useState([]);
  const [statisticsModalOpen, setStatisticsModalOpen] = useState(false);
  const [statisticsConfig, setStatisticsConfig] = useState({
    calculationMethod: "MANUFACTURER_FIXED",
    rollingWindowSize: 20,
    initialRunsRequired: 20,
    mean: null,
    standardDeviation: null,
  });

  // Control level options (FR-002)
  const controlLevelOptions = [
    { id: "LOW", label: intl.formatMessage({ id: "qc.controlLot.level.low" }) },
    { id: "NORMAL", label: intl.formatMessage({ id: "qc.controlLot.level.normal" }) },
    { id: "HIGH", label: intl.formatMessage({ id: "qc.controlLot.level.high" }) },
  ];

  // Validation schema
  const validationSchema = Yup.object().shape({
    lotNumber: Yup.string().required(
      intl.formatMessage({ id: "qc.controlLot.validation.lotNumberRequired" })
    ),
    controlMaterial: Yup.string().required(
      intl.formatMessage({ id: "qc.controlLot.validation.materialRequired" })
    ),
    controlLevel: Yup.string().required(
      intl.formatMessage({ id: "qc.controlLot.validation.levelRequired" })
    ),
    expirationDate: Yup.date().required(
      intl.formatMessage({ id: "qc.controlLot.validation.expirationRequired" })
    ),
    analyzerId: Yup.string().required(
      intl.formatMessage({ id: "qc.controlLot.validation.analyzerRequired" })
    ),
    testId: Yup.string().required(
      intl.formatMessage({ id: "qc.controlLot.validation.testRequired" })
    ),
  });

  // Load existing lot for edit
  useEffect(() => {
    if (isEditMode) {
      setLoading(true);
      getFromOpenElisServer(`/rest/qc/control-lots/${lotId}`, (response) => {
        if (response && response.data) {
          setExistingLot(response.data);
          setStatisticsConfig({
            calculationMethod: response.data.statisticsCalculationMethod || "MANUFACTURER_FIXED",
            rollingWindowSize: response.data.rollingWindowSize || 20,
            initialRunsRequired: response.data.initialRunsRequired || 20,
            mean: response.data.targetMean,
            standardDeviation: response.data.targetSD,
          });
        } else if (response && response.id) {
          setExistingLot(response);
          setStatisticsConfig({
            calculationMethod: response.statisticsCalculationMethod || "MANUFACTURER_FIXED",
            rollingWindowSize: response.rollingWindowSize || 20,
            initialRunsRequired: response.initialRunsRequired || 20,
            mean: response.targetMean,
            standardDeviation: response.targetSD,
          });
        }
        setLoading(false);
      });
    }
  }, [isEditMode, lotId]);

  // Load analyzers
  useEffect(() => {
    getFromOpenElisServer("/rest/analyzers", (response) => {
      if (response && Array.isArray(response.data)) {
        setAnalyzers(response.data);
      } else if (Array.isArray(response)) {
        setAnalyzers(response);
      }
    });
  }, []);

  // Load tests when analyzer changes
  const loadTests = (analyzerId) => {
    if (!analyzerId) {
      setTests([]);
      return;
    }
    getFromOpenElisServer(`/rest/analyzers/${analyzerId}/tests`, (response) => {
      if (response && Array.isArray(response.data)) {
        setTests(response.data);
      } else if (Array.isArray(response)) {
        setTests(response);
      }
    });
  };

  // Initial form values
  const getInitialValues = () => {
    if (existingLot) {
      return {
        lotNumber: existingLot.lotNumber || "",
        controlMaterial: existingLot.controlMaterial || "",
        controlLevel: existingLot.controlLevel || "",
        expirationDate: existingLot.expirationDate ? new Date(existingLot.expirationDate) : null,
        analyzerId: existingLot.analyzerId || "",
        testId: existingLot.testId || "",
        isActive: existingLot.isActive !== false,
      };
    }
    return {
      lotNumber: "",
      controlMaterial: "",
      controlLevel: "",
      expirationDate: null,
      analyzerId: "",
      testId: "",
      isActive: true,
    };
  };

  // Handle form submit
  const handleSubmit = async (values, { setSubmitting: setFormSubmitting }) => {
    setSubmitting(true);
    setError(null);

    const payload = {
      ...values,
      expirationDate: values.expirationDate?.toISOString(),
      statisticsCalculationMethod: statisticsConfig.calculationMethod,
      rollingWindowSize: statisticsConfig.rollingWindowSize,
      initialRunsRequired: statisticsConfig.initialRunsRequired,
      targetMean: statisticsConfig.mean,
      targetSD: statisticsConfig.standardDeviation,
    };

    const endpoint = isEditMode
      ? `/rest/qc/control-lots/${lotId}`
      : "/rest/qc/control-lots";

    const method = isEditMode ? "PUT" : "POST";

    postToOpenElisServerFullResponse(
      endpoint,
      JSON.stringify(payload),
      (response) => {
        if (response.ok) {
          response.json().then((data) => {
            if (data.status === "success" || data.id) {
              history.push("/analyzers/qc/control-lots");
            } else {
              setError(data.error || intl.formatMessage({ id: "qc.controlLot.error.saveFailed" }));
            }
          });
        } else {
          setError(intl.formatMessage({ id: "qc.controlLot.error.saveFailed" }));
        }
        setSubmitting(false);
        setFormSubmitting(false);
      },
      method
    );
  };

  // Handle cancel
  const handleCancel = () => {
    history.goBack();
  };

  // Handle statistics config save
  const handleStatisticsConfigSave = (config) => {
    setStatisticsConfig(config);
    setStatisticsModalOpen(false);
  };

  if (loading) {
    return (
      <div className="control-lot-setup-loading" data-testid="control-lot-setup-loading">
        <Loading
          description={intl.formatMessage({ id: "qc.controlLot.loading" })}
          withOverlay={false}
        />
      </div>
    );
  }

  return (
    <div className="control-lot-setup" data-testid="control-lot-setup">
      {/* Header */}
      <div className="control-lot-setup-header" data-testid="control-lot-setup-header">
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
              label: intl.formatMessage({ id: "qc.controlLots.title" }),
              link: "/analyzers/qc/control-lots",
            },
            {
              label: isEditMode
                ? intl.formatMessage({ id: "qc.controlLot.edit.title" })
                : intl.formatMessage({ id: "qc.controlLot.new.title" }),
            },
          ]}
          subtitle={
            isEditMode
              ? intl.formatMessage({ id: "qc.controlLot.edit.subtitle" })
              : intl.formatMessage({ id: "qc.controlLot.new.subtitle" })
          }
        />
      </div>

      {/* Error notification */}
      {error && (
        <InlineNotification
          kind="error"
          title={intl.formatMessage({ id: "qc.controlLot.error.title" })}
          subtitle={error}
          onClose={() => setError(null)}
          data-testid="control-lot-setup-error"
        />
      )}

      {/* Form */}
      <Formik
        initialValues={getInitialValues()}
        validationSchema={validationSchema}
        onSubmit={handleSubmit}
        enableReinitialize
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
          <Form onSubmit={handleSubmit} data-testid="control-lot-setup-form">
            <Grid>
              {/* Lot Number */}
              <Column lg={8} md={4} sm={4}>
                <FormGroup legendText="">
                  <TextInput
                    id="lot-number"
                    name="lotNumber"
                    labelText={intl.formatMessage({ id: "qc.controlLot.field.lotNumber" })}
                    placeholder={intl.formatMessage({ id: "qc.controlLot.field.lotNumberPlaceholder" })}
                    value={values.lotNumber}
                    onChange={handleChange}
                    onBlur={handleBlur}
                    invalid={touched.lotNumber && !!errors.lotNumber}
                    invalidText={errors.lotNumber}
                    data-testid="control-lot-number-input"
                  />
                </FormGroup>
              </Column>

              {/* Control Material */}
              <Column lg={8} md={4} sm={4}>
                <FormGroup legendText="">
                  <TextInput
                    id="control-material"
                    name="controlMaterial"
                    labelText={intl.formatMessage({ id: "qc.controlLot.field.material" })}
                    placeholder={intl.formatMessage({ id: "qc.controlLot.field.materialPlaceholder" })}
                    value={values.controlMaterial}
                    onChange={handleChange}
                    onBlur={handleBlur}
                    invalid={touched.controlMaterial && !!errors.controlMaterial}
                    invalidText={errors.controlMaterial}
                    data-testid="control-lot-material-input"
                  />
                </FormGroup>
              </Column>

              {/* Control Level */}
              <Column lg={8} md={4} sm={4}>
                <FormGroup legendText="">
                  <Dropdown
                    id="control-level"
                    titleText={intl.formatMessage({ id: "qc.controlLot.field.level" })}
                    label={intl.formatMessage({ id: "qc.controlLot.field.selectLevel" })}
                    items={controlLevelOptions}
                    itemToString={(item) => item?.label || ""}
                    selectedItem={controlLevelOptions.find((o) => o.id === values.controlLevel)}
                    onChange={({ selectedItem }) => setFieldValue("controlLevel", selectedItem?.id || "")}
                    invalid={touched.controlLevel && !!errors.controlLevel}
                    invalidText={errors.controlLevel}
                    data-testid="control-lot-level-dropdown"
                  />
                </FormGroup>
              </Column>

              {/* Expiration Date */}
              <Column lg={8} md={4} sm={4}>
                <FormGroup legendText="">
                  <DatePicker
                    datePickerType="single"
                    dateFormat="Y-m-d"
                    value={values.expirationDate}
                    onChange={([date]) => setFieldValue("expirationDate", date)}
                  >
                    <DatePickerInput
                      id="expiration-date"
                      placeholder="yyyy-mm-dd"
                      labelText={intl.formatMessage({ id: "qc.controlLot.field.expiration" })}
                      invalid={touched.expirationDate && !!errors.expirationDate}
                      invalidText={errors.expirationDate}
                      data-testid="control-lot-expiration-input"
                    />
                  </DatePicker>
                </FormGroup>
              </Column>

              {/* Analyzer */}
              <Column lg={8} md={4} sm={4}>
                <FormGroup legendText="">
                  <Dropdown
                    id="analyzer"
                    titleText={intl.formatMessage({ id: "qc.controlLot.field.analyzer" })}
                    label={intl.formatMessage({ id: "qc.controlLot.field.selectAnalyzer" })}
                    items={analyzers}
                    itemToString={(item) => item?.name || ""}
                    selectedItem={analyzers.find((a) => a.id === values.analyzerId)}
                    onChange={({ selectedItem }) => {
                      setFieldValue("analyzerId", selectedItem?.id || "");
                      setFieldValue("testId", "");
                      loadTests(selectedItem?.id);
                    }}
                    invalid={touched.analyzerId && !!errors.analyzerId}
                    invalidText={errors.analyzerId}
                    data-testid="control-lot-analyzer-dropdown"
                  />
                </FormGroup>
              </Column>

              {/* Test */}
              <Column lg={8} md={4} sm={4}>
                <FormGroup legendText="">
                  <Dropdown
                    id="test"
                    titleText={intl.formatMessage({ id: "qc.controlLot.field.test" })}
                    label={intl.formatMessage({ id: "qc.controlLot.field.selectTest" })}
                    items={tests}
                    itemToString={(item) => item?.name || ""}
                    selectedItem={tests.find((t) => t.id === values.testId)}
                    onChange={({ selectedItem }) => setFieldValue("testId", selectedItem?.id || "")}
                    invalid={touched.testId && !!errors.testId}
                    invalidText={errors.testId}
                    disabled={!values.analyzerId}
                    data-testid="control-lot-test-dropdown"
                  />
                </FormGroup>
              </Column>

              {/* Active Toggle */}
              <Column lg={8} md={4} sm={4}>
                <FormGroup legendText="">
                  <Toggle
                    id="is-active"
                    labelText={intl.formatMessage({ id: "qc.controlLot.field.isActive" })}
                    labelA={intl.formatMessage({ id: "qc.controlLot.field.inactive" })}
                    labelB={intl.formatMessage({ id: "qc.controlLot.field.active" })}
                    toggled={values.isActive}
                    onToggle={(checked) => setFieldValue("isActive", checked)}
                    data-testid="control-lot-active-toggle"
                  />
                </FormGroup>
              </Column>

              {/* Statistics Configuration (FR-003, FR-004, FR-005) */}
              <Column lg={16} md={8} sm={4}>
                <Tile className="control-lot-setup-statistics" data-testid="control-lot-statistics-tile">
                  <div className="control-lot-setup-statistics-header">
                    <h4>{intl.formatMessage({ id: "qc.controlLot.statistics.title" })}</h4>
                    <Button
                      kind="ghost"
                      size="sm"
                      renderIcon={Settings}
                      onClick={() => setStatisticsModalOpen(true)}
                      data-testid="control-lot-statistics-config-button"
                    >
                      {intl.formatMessage({ id: "qc.controlLot.statistics.configure" })}
                    </Button>
                  </div>
                  <Grid>
                    <Column lg={4} md={4} sm={4}>
                      <div className="stat-item">
                        <span className="stat-label">
                          {intl.formatMessage({ id: "qc.controlLot.statistics.method" })}
                        </span>
                        <span className="stat-value">
                          {intl.formatMessage({
                            id: `qc.controlLot.statistics.method.${statisticsConfig.calculationMethod?.toLowerCase()}`,
                          })}
                        </span>
                      </div>
                    </Column>
                    <Column lg={4} md={4} sm={4}>
                      <div className="stat-item">
                        <span className="stat-label">
                          {intl.formatMessage({ id: "qc.controlLot.statistics.mean" })}
                        </span>
                        <span className="stat-value">
                          {statisticsConfig.mean?.toFixed(2) || "-"}
                        </span>
                      </div>
                    </Column>
                    <Column lg={4} md={4} sm={4}>
                      <div className="stat-item">
                        <span className="stat-label">
                          {intl.formatMessage({ id: "qc.controlLot.statistics.sd" })}
                        </span>
                        <span className="stat-value">
                          {statisticsConfig.standardDeviation?.toFixed(2) || "-"}
                        </span>
                      </div>
                    </Column>
                  </Grid>
                </Tile>
              </Column>

              {/* Actions */}
              <Column lg={16} md={8} sm={4}>
                <div className="control-lot-setup-actions">
                  <Button
                    kind="secondary"
                    onClick={handleCancel}
                    data-testid="control-lot-cancel-button"
                  >
                    {intl.formatMessage({ id: "button.cancel" })}
                  </Button>
                  <Button
                    kind="primary"
                    type="submit"
                    disabled={isSubmitting || submitting}
                    data-testid="control-lot-submit-button"
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

      {/* Statistics Configuration Modal */}
      {statisticsModalOpen && (
        <StatisticsConfigModal
          open={statisticsModalOpen}
          config={statisticsConfig}
          onClose={() => setStatisticsModalOpen(false)}
          onSave={handleStatisticsConfigSave}
        />
      )}
    </div>
  );
};

export default ControlLotSetup;
