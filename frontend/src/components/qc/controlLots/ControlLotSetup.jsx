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

import React, { useState, useEffect, useContext } from "react";
import {
  Grid,
  Column,
  Form,
  FormGroup,
  TextInput,
  Dropdown,
  Button,
  Loading,
  InlineNotification,
  Toggle,
} from "@carbon/react";
import { useIntl } from "react-intl";
import { useHistory, useParams } from "react-router-dom";
import { Formik } from "formik";
import * as Yup from "yup";
import {
  displayDateToIso,
  getFromOpenElisServer,
  postToOpenElisServerFullResponse,
} from "../../utils/Utils";
import CustomDatePicker from "../../common/CustomDatePicker";
import { ConfigurationContext } from "../../layout/Layout";
import StatisticsConfigSection from "./StatisticsConfigSection";
import PageTitle from "../../common/PageTitle/PageTitle";
import PageBreadCrumb from "../../common/PageBreadCrumb";
import "./ControlLotSetup.css";

const ControlLotSetup = () => {
  const intl = useIntl();
  const history = useHistory();
  const { id: lotId } = useParams();
  const { configurationProperties } = useContext(ConfigurationContext) || {};
  // CustomDatePicker renders and returns the locale display format; the lot
  // endpoint speaks ISO, so the expiration date is converted at both edges.
  const dateLocale = configurationProperties?.DEFAULT_DATE_LOCALE;
  const isoToDisplayDate = (value) => {
    if (!value) return "";
    const d = new Date(value);
    const mm = String(d.getUTCMonth() + 1).padStart(2, "0");
    const dd = String(d.getUTCDate()).padStart(2, "0");
    const yyyy = d.getUTCFullYear();
    return dateLocale === "fr-FR"
      ? `${dd}/${mm}/${yyyy}`
      : `${mm}/${dd}/${yyyy}`;
  };

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

  // A bench lot has no analyzer. Modelled as a first-class dropdown entry with an
  // empty id so the field always shows what will actually be submitted, instead of
  // reading as a required field the user forgot to fill in.
  const BENCH_ANALYZER_ID = "";
  const analyzerOptions = [
    {
      id: BENCH_ANALYZER_ID,
      name: intl.formatMessage({ id: "qc.controlLot.analyzer.bench" }),
    },
    ...analyzers,
  ];

  // Control level options (FR-002)
  const controlLevelOptions = [
    { id: "LOW", label: intl.formatMessage({ id: "qc.controlLot.level.low" }) },
    {
      id: "NORMAL",
      label: intl.formatMessage({ id: "qc.controlLot.level.normal" }),
    },
    {
      id: "HIGH",
      label: intl.formatMessage({ id: "qc.controlLot.level.high" }),
    },
  ];

  const frequencyOptions = [
    "DAILY",
    "PER_SHIFT",
    "CUSTOM_HOURS",
    "PER_RUN",
  ].map((id) => ({
    id,
    label: intl.formatMessage({ id: `qc.controlLot.frequency.${id}` }),
  }));

  // Validation schema
  const validationSchema = Yup.object().shape({
    lotNumber: Yup.string().required(
      intl.formatMessage({ id: "qc.controlLot.validation.lotNumberRequired" }),
    ),
    controlMaterial: Yup.string().required(
      intl.formatMessage({ id: "qc.controlLot.validation.materialRequired" }),
    ),
    controlLevel: Yup.string().required(
      intl.formatMessage({ id: "qc.controlLot.validation.levelRequired" }),
    ),
    expirationDate: Yup.string().required(
      intl.formatMessage({
        id: "qc.controlLot.validation.expirationRequired",
      }),
    ),
    // analyzerId is deliberately absent: a bench control lot for a manual method has
    // no analyzer (OGC-1147), and the dropdown makes that an explicit choice
    // rather than a blank field.
    testId: Yup.string().required(
      intl.formatMessage({ id: "qc.controlLot.validation.testRequired" }),
    ),
    // OGC-427: a custom window needs its length; a shift needs a positive one.
    qcFrequencyHours: Yup.number()
      .transform((value, original) => (original === "" ? undefined : value))
      .when("qcFrequencyType", {
        is: "CUSTOM_HOURS",
        then: (schema) =>
          schema
            .required(
              intl.formatMessage({
                id: "qc.controlLot.validation.frequencyHoursRequired",
              }),
            )
            .positive(
              intl.formatMessage({
                id: "qc.controlLot.validation.frequencyHoursRequired",
              }),
            )
            .integer(
              intl.formatMessage({
                id: "qc.controlLot.validation.frequencyHoursRequired",
              }),
            ),
        otherwise: (schema) => schema.notRequired(),
      }),
    shiftDefinitionHours: Yup.number()
      .transform((value, original) => (original === "" ? undefined : value))
      .when("qcFrequencyType", {
        is: "PER_SHIFT",
        then: (schema) =>
          schema
            .required(
              intl.formatMessage({
                id: "qc.controlLot.validation.shiftHoursRequired",
              }),
            )
            .positive(
              intl.formatMessage({
                id: "qc.controlLot.validation.shiftHoursRequired",
              }),
            )
            .integer(
              intl.formatMessage({
                id: "qc.controlLot.validation.shiftHoursRequired",
              }),
            ),
        otherwise: (schema) => schema.notRequired(),
      }),
  });

  // Load existing lot for edit
  useEffect(() => {
    if (isEditMode) {
      setLoading(true);
      getFromOpenElisServer(`/rest/qc/controlLot/${lotId}`, (response) => {
        const lot =
          response && response.data
            ? response.data
            : response && response.id
              ? response
              : null;
        if (lot) {
          setExistingLot(lot);
          setStatisticsConfig({
            calculationMethod: lot.calculationMethod || "MANUFACTURER_FIXED",
            rollingWindowSize: lot.rollingWindowSize || 20,
            initialRunsRequired: lot.initialRunsCount || 20,
            mean: lot.manufacturerMean,
            standardDeviation: lot.manufacturerStdDev,
          });
        }
        setLoading(false);
      });
    }
  }, [isEditMode, lotId]);

  // Load analyzers
  useEffect(() => {
    getFromOpenElisServer("/rest/analyzer/analyzers", (response) => {
      if (response && Array.isArray(response.analyzers)) {
        setAnalyzers(response.analyzers);
      } else if (response && Array.isArray(response.data)) {
        setAnalyzers(response.data);
      } else if (Array.isArray(response)) {
        setAnalyzers(response);
      }
    });
  }, []);

  // Load all tests on mount
  useEffect(() => {
    getFromOpenElisServer("/rest/displayList/ALL_TESTS", (response) => {
      if (Array.isArray(response)) {
        setTests(response);
      }
    });
  }, []);

  // Initial form values
  const getInitialValues = () => {
    if (existingLot) {
      return {
        lotNumber: existingLot.lotNumber || "",
        controlMaterial: existingLot.productName || "",
        controlLevel: existingLot.controlLevel || "",
        expirationDate: isoToDisplayDate(existingLot.expirationDate),
        analyzerId:
          existingLot.instrumentId != null
            ? String(existingLot.instrumentId)
            : "",
        testId: existingLot.testId != null ? String(existingLot.testId) : "",
        isActive: existingLot.status === "ACTIVE",
        qcFrequencyType: existingLot.qcFrequencyType || "DAILY",
        qcFrequencyHours:
          existingLot.qcFrequencyHours != null
            ? String(existingLot.qcFrequencyHours)
            : "",
        shiftDefinitionHours:
          existingLot.shiftDefinitionHours != null
            ? String(existingLot.shiftDefinitionHours)
            : "8",
        qcRequired: existingLot.qcRequired !== false,
      };
    }
    return {
      lotNumber: "",
      controlMaterial: "",
      controlLevel: "",
      expirationDate: "",
      analyzerId: "",
      testId: "",
      isActive: true,
      qcFrequencyType: "DAILY",
      qcFrequencyHours: "",
      shiftDefinitionHours: "8",
      qcRequired: true,
    };
  };

  // Handle form submit
  const handleSubmit = async (values, { setSubmitting: setFormSubmitting }) => {
    setSubmitting(true);
    setError(null);

    // Manufacturer-fixed lots need mean + SD, entered via "Configure". Catch it
    // here so the user gets a clear pointer instead of an opaque backend 400.
    if (
      statisticsConfig.calculationMethod === "MANUFACTURER_FIXED" &&
      (statisticsConfig.mean == null ||
        statisticsConfig.standardDeviation == null)
    ) {
      setError(intl.formatMessage({ id: "qc.controlLot.error.statsRequired" }));
      setSubmitting(false);
      setFormSubmitting(false);
      return;
    }

    // Same rule QCControlLotValidator enforces: with no analyzer there is nothing
    // accumulating runs, so a run-derived method would leave the lot in
    // ESTABLISHMENT forever. Caught here for a pointed message instead of a 400.
    if (
      !values.analyzerId &&
      statisticsConfig.calculationMethod !== "MANUFACTURER_FIXED"
    ) {
      setError(
        intl.formatMessage({ id: "qc.controlLot.error.benchNeedsFixedStats" }),
      );
      setSubmitting(false);
      setFormSubmitting(false);
      return;
    }

    const payload = {
      id: isEditMode ? lotId : undefined,
      productName: values.controlMaterial,
      lotNumber: values.lotNumber,
      controlLevel: values.controlLevel,
      expirationDate: values.expirationDate
        ? new Date(
            `${displayDateToIso(values.expirationDate, dateLocale)}T12:00:00`,
          ).toISOString()
        : undefined,
      instrumentId: values.analyzerId
        ? parseInt(values.analyzerId, 10)
        : undefined,
      testId: values.testId ? parseInt(values.testId, 10) : undefined,
      calculationMethod: statisticsConfig.calculationMethod,
      initialRunsCount: statisticsConfig.initialRunsRequired,
      manufacturerMean: statisticsConfig.mean,
      manufacturerStdDev: statisticsConfig.standardDeviation,
      activationDate: new Date().toISOString(),
      status: values.isActive ? "ACTIVE" : "EXPIRED",
      qcFrequencyType: values.qcFrequencyType,
      qcFrequencyHours:
        values.qcFrequencyType === "CUSTOM_HOURS" && values.qcFrequencyHours
          ? parseInt(values.qcFrequencyHours, 10)
          : null,
      shiftDefinitionHours: values.shiftDefinitionHours
        ? parseInt(values.shiftDefinitionHours, 10)
        : 8,
      qcRequired: values.qcRequired,
    };

    postToOpenElisServerFullResponse(
      "/rest/qc/controlLot",
      JSON.stringify(payload),
      async (response) => {
        if (response && response.ok) {
          history.push("/analyzers/qc/control-lots");
          return;
        } else {
          // Surface the backend's specific message (it returns the validation
          // reason as the 400 body) rather than a generic "save failed".
          const backendMsg = response
            ? await response.text().catch(() => "")
            : "";
          setError(
            backendMsg?.trim() ||
              intl.formatMessage({ id: "qc.controlLot.error.saveFailed" }),
          );
        }
        setSubmitting(false);
        setFormSubmitting(false);
      },
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
      <div
        className="control-lot-setup-loading"
        data-testid="control-lot-setup-loading"
      >
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
      <div
        className="control-lot-setup-header"
        data-testid="control-lot-setup-header"
      >
        <PageBreadCrumb
          breadcrumbs={[
            { label: "home.label", link: "/" },
            { label: "analyzer.page.hierarchy.root", link: "/analyzers" },
            { label: "qc.dashboard.title", link: "/analyzers/qc/db" },
            {
              label: "qc.controlLots.title",
              link: "/analyzers/qc/control-lots",
            },
            {
              label: isEditMode
                ? "qc.controlLot.edit.title"
                : "qc.controlLot.new.title",
              link: "",
            },
          ]}
        />
        <PageTitle
          breadcrumbs={[
            {
              label: intl.formatMessage({ id: "analyzer.page.hierarchy.root" }),
              link: "/analyzers",
            },
            {
              label: intl.formatMessage({ id: "qc.dashboard.title" }),
              link: "/qa/qc/dashboard",
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
                    labelText={intl.formatMessage({
                      id: "qc.controlLot.field.lotNumber",
                    })}
                    placeholder={intl.formatMessage({
                      id: "qc.controlLot.field.lotNumberPlaceholder",
                    })}
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
                    labelText={intl.formatMessage({
                      id: "qc.controlLot.field.material",
                    })}
                    placeholder={intl.formatMessage({
                      id: "qc.controlLot.field.materialPlaceholder",
                    })}
                    value={values.controlMaterial}
                    onChange={handleChange}
                    onBlur={handleBlur}
                    invalid={
                      touched.controlMaterial && !!errors.controlMaterial
                    }
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
                    titleText={intl.formatMessage({
                      id: "qc.controlLot.field.level",
                    })}
                    label={intl.formatMessage({
                      id: "qc.controlLot.field.selectLevel",
                    })}
                    items={controlLevelOptions}
                    itemToString={(item) => item?.label || ""}
                    selectedItem={
                      controlLevelOptions.find(
                        (o) => o.id === values.controlLevel,
                      ) ?? null
                    }
                    onChange={({ selectedItem }) =>
                      setFieldValue("controlLevel", selectedItem?.id || "")
                    }
                    invalid={touched.controlLevel && !!errors.controlLevel}
                    invalidText={errors.controlLevel}
                    data-testid="control-lot-level-dropdown"
                  />
                </FormGroup>
              </Column>

              {/* Expiration Date */}
              <Column lg={8} md={4} sm={4}>
                <FormGroup legendText="">
                  <CustomDatePicker
                    id="expiration-date"
                    labelText={intl.formatMessage({
                      id: "qc.controlLot.field.expiration",
                    })}
                    value={values.expirationDate}
                    updateStateValue
                    onChange={(displayed) =>
                      setFieldValue("expirationDate", displayed)
                    }
                    invalid={touched.expirationDate && !!errors.expirationDate}
                    invalidText={errors.expirationDate}
                  />
                </FormGroup>
              </Column>

              {/* Analyzer */}
              <Column lg={8} md={4} sm={4}>
                <FormGroup legendText="">
                  <Dropdown
                    id="analyzer"
                    titleText={intl.formatMessage({
                      id: "qc.controlLot.field.analyzer",
                    })}
                    label={intl.formatMessage({
                      id: "qc.controlLot.field.selectAnalyzer",
                    })}
                    helperText={intl.formatMessage({
                      id: "qc.controlLot.field.analyzerHelper",
                    })}
                    items={analyzerOptions}
                    itemToString={(item) => item?.name || ""}
                    selectedItem={
                      analyzerOptions.find((a) => a.id === values.analyzerId) ??
                      null
                    }
                    onChange={({ selectedItem }) => {
                      setFieldValue(
                        "analyzerId",
                        selectedItem?.id || BENCH_ANALYZER_ID,
                      );
                      setFieldValue("testId", "");
                    }}
                    data-testid="control-lot-analyzer-dropdown"
                  />
                </FormGroup>
              </Column>

              {/* Test */}
              <Column lg={8} md={4} sm={4}>
                <FormGroup legendText="">
                  <Dropdown
                    id="test"
                    titleText={intl.formatMessage({
                      id: "qc.controlLot.field.test",
                    })}
                    label={intl.formatMessage({
                      id: "qc.controlLot.field.selectTest",
                    })}
                    items={tests}
                    itemToString={(item) => item?.value || item?.name || ""}
                    selectedItem={
                      tests.find((t) => t.id === values.testId) ?? null
                    }
                    onChange={({ selectedItem }) =>
                      setFieldValue("testId", selectedItem?.id || "")
                    }
                    invalid={touched.testId && !!errors.testId}
                    invalidText={errors.testId}
                    data-testid="control-lot-test-dropdown"
                  />
                </FormGroup>
              </Column>

              {/* Active Toggle */}
              <Column lg={8} md={4} sm={4}>
                <FormGroup legendText="">
                  <Toggle
                    id="is-active"
                    labelText={intl.formatMessage({
                      id: "qc.controlLot.field.isActive",
                    })}
                    labelA={intl.formatMessage({
                      id: "qc.controlLot.field.inactive",
                    })}
                    labelB={intl.formatMessage({
                      id: "qc.controlLot.field.active",
                    })}
                    toggled={values.isActive}
                    onToggle={(checked) => setFieldValue("isActive", checked)}
                    data-testid="control-lot-active-toggle"
                  />
                </FormGroup>
              </Column>

              {/* QC frequency rule (OGC-427 FR-QC-001) */}
              <Column lg={8} md={4} sm={4}>
                <FormGroup legendText="">
                  <Dropdown
                    id="qc-frequency-type"
                    titleText={intl.formatMessage({
                      id: "qc.controlLot.field.frequency",
                    })}
                    label={intl.formatMessage({
                      id: "qc.controlLot.field.frequency",
                    })}
                    helperText={intl.formatMessage({
                      id: "qc.controlLot.field.frequencyHelper",
                    })}
                    items={frequencyOptions}
                    itemToString={(item) => item?.label || ""}
                    selectedItem={frequencyOptions.find(
                      (option) => option.id === values.qcFrequencyType,
                    )}
                    onChange={({ selectedItem }) =>
                      setFieldValue(
                        "qcFrequencyType",
                        selectedItem?.id || "DAILY",
                      )
                    }
                    data-testid="control-lot-frequency-dropdown"
                  />
                </FormGroup>
              </Column>
              {values.qcFrequencyType === "CUSTOM_HOURS" && (
                <Column lg={8} md={4} sm={4}>
                  <FormGroup legendText="">
                    <TextInput
                      id="qc-frequency-hours"
                      name="qcFrequencyHours"
                      type="number"
                      min={1}
                      labelText={intl.formatMessage({
                        id: "qc.controlLot.field.frequencyHours",
                      })}
                      value={values.qcFrequencyHours}
                      onChange={handleChange}
                      onBlur={handleBlur}
                      invalid={
                        touched.qcFrequencyHours && !!errors.qcFrequencyHours
                      }
                      invalidText={errors.qcFrequencyHours}
                      data-testid="control-lot-frequency-hours-input"
                    />
                  </FormGroup>
                </Column>
              )}
              {values.qcFrequencyType === "PER_SHIFT" && (
                <Column lg={8} md={4} sm={4}>
                  <FormGroup legendText="">
                    <TextInput
                      id="qc-shift-hours"
                      name="shiftDefinitionHours"
                      type="number"
                      min={1}
                      labelText={intl.formatMessage({
                        id: "qc.controlLot.field.shiftHours",
                      })}
                      value={values.shiftDefinitionHours}
                      onChange={handleChange}
                      onBlur={handleBlur}
                      invalid={
                        touched.shiftDefinitionHours &&
                        !!errors.shiftDefinitionHours
                      }
                      invalidText={errors.shiftDefinitionHours}
                      data-testid="control-lot-shift-hours-input"
                    />
                  </FormGroup>
                </Column>
              )}
              <Column lg={8} md={4} sm={4}>
                <FormGroup legendText="">
                  <Toggle
                    id="qc-required"
                    labelText={intl.formatMessage({
                      id: "qc.controlLot.field.qcRequired",
                    })}
                    labelA={intl.formatMessage({
                      id: "qc.controlLot.field.qcRequired.off",
                    })}
                    labelB={intl.formatMessage({
                      id: "qc.controlLot.field.qcRequired.on",
                    })}
                    toggled={values.qcRequired}
                    onToggle={(checked) => setFieldValue("qcRequired", checked)}
                    data-testid="control-lot-qc-required-toggle"
                  />
                </FormGroup>
              </Column>

              {/* Statistics Configuration (FR-003, FR-004, FR-005) */}
              <Column lg={16} md={8} sm={4}>
                <StatisticsConfigSection
                  statisticsConfig={statisticsConfig}
                  statisticsModalOpen={statisticsModalOpen}
                  onOpenModal={() => setStatisticsModalOpen(true)}
                  onCloseModal={() => setStatisticsModalOpen(false)}
                  onSave={handleStatisticsConfigSave}
                />
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
    </div>
  );
};

export default ControlLotSetup;
