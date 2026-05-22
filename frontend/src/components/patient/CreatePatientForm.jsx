import React, { useState, useRef, useEffect, useContext } from "react";
import { FormattedMessage, injectIntl, useIntl } from "react-intl";
import { useHistory } from "react-router-dom";
import "../Style.css";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
  resolveApiErrorMessage,
} from "../utils/Utils";
import { nationalityList } from "../data/countries";
import format from "date-fns/format";
import {
  differenceInYears,
  differenceInMonths,
  differenceInDays,
  addYears,
  addMonths,
} from "date-fns";

import {
  Heading,
  Form,
  FormLabel,
  TextInput,
  TextArea,
  Button,
  RadioButton,
  RadioButtonGroup,
  Section,
  Select,
  SelectItem,
  Accordion,
  AccordionItem,
  Grid,
  Column,
  Toggle,
  InlineNotification,
} from "@carbon/react";
import AddressSearch from "./AddressSearch";

import { Formik, Field, ErrorMessage } from "formik";
import CreatePatientFormValues from "../formModel/innitialValues/CreatePatientFormValues";
import PatientFormObserver from "./PatientFormObserver";
import { AlertDialog, NotificationKinds } from "../common/CustomNotification";
import { NotificationContext, ConfigurationContext } from "../layout/Layout";
import { createPatientValidationSchema } from "../formModel/validationSchema/CreatePatientValidationShema";
import CustomDatePicker from "../common/CustomDatePicker";
import PatientImageSelector from "./photoManagement/uploadPhoto/PatientImageSelector";
import IdentificationDocuments from "./IdentificationDocuments";
import { getPhoneFormatHint } from "./phoneFormatHint";

const configIsTrue = (value) => value === "true";

const configuredText = (value, fallback) =>
  typeof value === "string" && value.trim() ? value : fallback;

// Captured once at mount and frozen for Formik's initialValues. The parent
// remounts CreatePatientForm via a `key` prop when selectedPatient changes,
// so mount-time capture is the right re-init boundary — no enableReinitialize
// needed, and writes to Formik state after mount no longer clobber user input.
const buildInitialFormValues = ({
  base,
  defaultNationality,
  selectedPatient,
  orderFormValues,
  dateLocale,
}) => {
  const seed = defaultNationality
    ? { ...base, nationality: defaultNationality }
    : { ...base };

  const withAgeParts = (values) => {
    const ageParts = computeAgePartsFromDob(
      values.birthDateForDisplay,
      dateLocale,
    );
    return { ...values, ...ageParts };
  };

  if (selectedPatient?.patientPK) {
    const flattenedAddressHierarchy = {};
    if (selectedPatient.addressHierarchy) {
      Object.entries(selectedPatient.addressHierarchy).forEach(([k, v]) => {
        flattenedAddressHierarchy[k] = v || "";
      });
    }
    return withAgeParts({
      ...seed,
      ...selectedPatient,
      ...flattenedAddressHierarchy,
      patientContact: {
        ...seed.patientContact,
        ...(selectedPatient.patientContact || {}),
        person: {
          ...seed.patientContact.person,
          ...(selectedPatient.patientContact?.person || {}),
        },
      },
      // `selectedPatient.photo` is pre-fetched by usePatientDetails. Pass it
      // through rather than resetting to "" and then refetching on mount.
      photo: selectedPatient.photo || "",
      patientUpdateStatus: "NO_ACTION",
    });
  }

  const fromOrder = orderFormValues?.patientProperties;
  if (fromOrder && (fromOrder.firstName !== "" || fromOrder.guid !== "")) {
    const flattenedAddressHierarchy = {};
    if (fromOrder.addressHierarchy) {
      Object.entries(fromOrder.addressHierarchy).forEach(([k, v]) => {
        flattenedAddressHierarchy[k] = v || "";
      });
    }
    return withAgeParts({
      ...seed,
      ...fromOrder,
      ...flattenedAddressHierarchy,
    });
  }

  return seed;
};

const computeDobFromFormatter = ({ years, months, days }, dateLocale) => {
  const currentDate = new Date();
  const pastDate = new Date();
  pastDate.setFullYear(currentDate.getFullYear() - (Number(years) || 0));
  pastDate.setMonth(currentDate.getMonth() - (Number(months) || 0));
  pastDate.setDate(currentDate.getDate() - (Number(days) || 0));
  return format(
    new Date(pastDate),
    dateLocale === "fr-FR" ? "dd/MM/yyyy" : "MM/dd/yyyy",
  );
};

// Derive {years, months, days} from a displayed DOB string. Returns empty
// strings for an absent or unparseable DOB so the field defaults remain
// blank rather than "NaN".
const computeAgePartsFromDob = (dob, dateLocale) => {
  if (!dob || dob === "") return { years: "", months: "", days: "" };
  const parts = dob.split("/");
  if (parts.length !== 3) return { years: "", months: "", days: "" };
  let yy;
  let mm;
  let dd;
  if (dateLocale === "fr-FR") {
    yy = parseInt(parts[2]);
    mm = parseInt(parts[1]);
    dd = parseInt(parts[0]);
  } else {
    yy = parseInt(parts[2]);
    mm = parseInt(parts[0]);
    dd = parseInt(parts[1]);
  }
  const birthDate = new Date(mm + "/" + dd + "/" + yy);
  if (Number.isNaN(birthDate.getTime())) {
    return { years: "", months: "", days: "" };
  }
  const now = new Date();
  const years = differenceInYears(now, birthDate);
  const months = differenceInMonths(now, addYears(birthDate, years));
  const days = differenceInDays(
    now,
    addMonths(addYears(birthDate, years), months),
  );
  return { years, months, days };
};

function CreatePatientForm(props) {
  const componentMounted = useRef(false);
  const history = useHistory();

  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);
  const { configurationProperties } = useContext(ConfigurationContext);

  const intl = useIntl();
  const nationalIdRequired =
    configurationProperties.PATIENT_NATIONAL_ID_REQUIRED !== "false";
  const aliasEnabled = configIsTrue(
    configurationProperties.PATIENT_ALIAS_ENABLED,
  );
  const aliasLabel = configuredText(
    configurationProperties.PATIENT_ALIAS_LABEL,
    intl.formatMessage({ id: "patient.alias", defaultMessage: "Alias" }),
  );
  const idDocumentsLabel = configuredText(
    configurationProperties.PATIENT_ID_DOCUMENTS_LABEL,
    intl.formatMessage({ id: "patient.idDoc.title" }),
  );
  const validationSchema = createPatientValidationSchema(
    configurationProperties,
  );

  const defaultNationality =
    configurationProperties.DEFAULT_NATIONALITY &&
    nationalityList.some(
      (n) => n.value === configurationProperties.DEFAULT_NATIONALITY,
    )
      ? configurationProperties.DEFAULT_NATIONALITY
      : "";

  // Frozen at mount. CreatePatientForm is keyed on selectedPatient.patientPK
  // in the parent so a different patient remounts the form rather than
  // reinitializing Formik — that means writes to Formik state after mount
  // (photo arrival, hierarchy defaults, DOB derivation) no longer race
  // against user typing.
  const [initialValues] = useState(() =>
    buildInitialFormValues({
      base: CreatePatientFormValues,
      defaultNationality,
      selectedPatient: props.selectedPatient,
      orderFormValues: props.orderFormValues,
      dateLocale: configurationProperties.DEFAULT_DATE_LOCALE,
    }),
  );
  // Bridge so async callbacks (photo fetch, hierarchy defaults) can write
  // into Formik state without going through `initialValues`. Set via
  // <Formik innerRef={formikRef}>.
  const formikRef = useRef(null);
  const [healthRegions, setHealthRegions] = useState([]);
  const [healthDistricts, setHealthDistricts] = useState([]);
  const [addressHierarchyLevels, setAddressHierarchyLevels] = useState([]);
  const [addressHierarchyValues, setAddressHierarchyValues] = useState({});
  const [addressHierarchyInitialized, setAddressHierarchyInitialized] =
    useState(null); // Track which patient's hierarchy is initialized
  const [educationList, setEducationList] = useState([]);
  const [maritalStatuses, setMaritalStatuses] = useState([]);
  const [diseaseProgrammes, setDiseaseProgrammes] = useState([]);
  const [formAction, setFormAction] = useState("ADD");
  // Read-only-by-default for existing patients. The Edit toggle flips this
  // on; saving flips it back. The parent keys this component on patientPK,
  // so loading a different patient remounts (no need for a separate reset).
  const [isEditing, setIsEditing] = useState(false);
  const isExistingPatient = !!props.selectedPatient?.patientPK;
  const isReadOnly = isExistingPatient && !isEditing;
  const [phoneValidation, setPhoneValidation] = useState({
    primaryPhone: { body: "", status: true },
    contactPhone: { body: "", status: true },
  });

  const handlePhotoChange = (photo, setFieldValue) => {
    if (setFieldValue) {
      setFieldValue("photo", photo);
    }
  };

  const handleDatePickerChange = (setFieldValue, date) => {
    setFieldValue("birthDateForDisplay", date);
    const ageParts = computeAgePartsFromDob(
      date,
      configurationProperties.DEFAULT_DATE_LOCALE,
    );
    setFieldValue("years", ageParts.years);
    setFieldValue("months", ageParts.months);
    setFieldValue("days", ageParts.days);
  };

  // Single age-part change handler. Years/months/days live in Formik state;
  // typing into any of them writes that field and re-derives birthDateForDisplay
  // from the trio. Other parts remain whatever the user already typed.
  const handleAgePartChange = (field, e, values, setFieldValue) => {
    const next = Math.max(0, Number(e.target.value));
    setFieldValue(field, next);
    const dob = computeDobFromFormatter(
      {
        years: field === "years" ? next : values.years || 0,
        months: field === "months" ? next : values.months || 0,
        days: field === "days" ? next : values.days || 0,
      },
      configurationProperties.DEFAULT_DATE_LOCALE,
    );
    setFieldValue("birthDateForDisplay", dob);
  };
  const handleRegionSelection = (e, values) => {
    var patient = values;
    patient.healthDistrict = "";
    const { value } = e.target;
    getFromOpenElisServer(
      "/rest/health-districts-for-region?regionId=" + value,
      fetchHeathDistricts,
    );
  };

  const handleAddressHierarchySelection = (
    levelIndex,
    selectedId,
    setFieldValue,
  ) => {
    // Clear all child levels
    const clearedValues = { ...addressHierarchyValues };
    for (let i = levelIndex + 1; i < addressHierarchyLevels.length; i++) {
      clearedValues[i] = [];
      setFieldValue(`addressHierarchy_${i}`, "");
    }
    setAddressHierarchyValues(clearedValues);

    // Fetch children for the selected value
    if (selectedId && levelIndex < addressHierarchyLevels.length - 1) {
      getFromOpenElisServer(
        `/rest/address-hierarchy/children?parentId=${selectedId}`,
        (children) => {
          if (componentMounted.current) {
            setAddressHierarchyValues((prev) => ({
              ...prev,
              [levelIndex + 1]: children,
            }));
          }
        },
      );
    }
  };

  /**
   * Handle address selection from the search component.
   * Auto-populates all hierarchy level fields and fetches child options for each level.
   */
  const handleAddressSearchSelect = (hierarchyLevels, setFieldValue) => {
    if (!hierarchyLevels || hierarchyLevels.length === 0) return;

    // Set field values for each level
    hierarchyLevels.forEach((level) => {
      const levelIndex = level.level - 1; // Convert 1-based to 0-based index
      setFieldValue(`addressHierarchy_${levelIndex}`, level.id);

      // For backward compatibility
      if (levelIndex === 0) {
        setFieldValue("healthRegion", level.id);
      } else if (levelIndex === 1) {
        setFieldValue("healthDistrict", level.id);
      }
    });

    // Fetch child options for each level to populate the dropdowns
    const fetchChildrenForLevels = (levelIndex) => {
      if (levelIndex >= hierarchyLevels.length) return;

      const level = hierarchyLevels[levelIndex];
      const nextLevelIndex = levelIndex + 1;

      // Fetch children for next dropdown
      if (nextLevelIndex < addressHierarchyLevels.length) {
        getFromOpenElisServer(
          `/rest/address-hierarchy/children?parentId=${level.id}`,
          (children) => {
            if (componentMounted.current && children) {
              setAddressHierarchyValues((prev) => ({
                ...prev,
                [nextLevelIndex]: children,
              }));
              // Continue to next level
              fetchChildrenForLevels(nextLevelIndex);
            }
          },
        );
      }
    };

    // Start from the first level to populate all dropdowns
    fetchChildrenForLevels(0);
  };

  const fetchAddressHierarchyLevels = (levels) => {
    if (componentMounted.current && levels && levels.length > 0) {
      setAddressHierarchyLevels(levels);
      // Fetch top level values
      getFromOpenElisServer(`/rest/address-hierarchy/level/1`, (values) => {
        if (componentMounted.current) {
          setAddressHierarchyValues({ 0: values });
          // Also populate healthRegions for backward compatibility
          setHealthRegions(values);

          // Check if any level has defaults configured
          const hasDefaults = levels.some((lvl) => lvl.defaultId);

          // Apply defaults for new patients only
          if (!props.selectedPatient?.patientPK && hasDefaults) {
            applyAddressHierarchyDefaults(levels);
          }
        }
      });
    }
  };

  const applyAddressHierarchyDefaults = (levels) => {
    // Build the defaults object and fetch child values for each level
    const defaults = {};
    const fetchChildrenForDefaultLevel = (levelIndex) => {
      if (levelIndex >= levels.length) {
        // All defaults applied, flush them into Formik via the bridged ref.
        if (Object.keys(defaults).length > 0 && formikRef.current) {
          Object.entries(defaults).forEach(([key, value]) => {
            formikRef.current.setFieldValue(key, value);
          });
        }
        return;
      }

      const level = levels[levelIndex];
      if (level.defaultId) {
        defaults[`addressHierarchy_${levelIndex}`] = level.defaultId;

        // Fetch children for next level if this level has a default
        if (levelIndex < levels.length - 1) {
          getFromOpenElisServer(
            `/rest/address-hierarchy/children?parentId=${level.defaultId}`,
            (children) => {
              if (componentMounted.current && children) {
                setAddressHierarchyValues((prev) => ({
                  ...prev,
                  [levelIndex + 1]: children,
                }));
                fetchChildrenForDefaultLevel(levelIndex + 1);
              }
            },
          );
        } else {
          fetchChildrenForDefaultLevel(levelIndex + 1);
        }
      } else {
        // No default for this level, stop cascading
        if (Object.keys(defaults).length > 0 && formikRef.current) {
          Object.entries(defaults).forEach(([key, value]) => {
            formikRef.current.setFieldValue(key, value);
          });
        }
      }
    };

    fetchChildrenForDefaultLevel(0);
  };

  const getAddressLevelLabel = (level) =>
    level.displayKey
      ? intl.formatMessage({
          id: level.displayKey,
          defaultMessage: level.typeName,
        })
      : level.typeName;

  const getAddressLevelIndex = (level) => {
    const index = addressHierarchyLevels.findIndex(
      (configuredLevel) => configuredLevel.level === level.level,
    );
    return index >= 0 ? index : level.level - 1;
  };

  const getAddressRenderLevels = () =>
    [...addressHierarchyLevels].sort((left, right) => {
      const leftOrder = left.sortOrder ?? left.level;
      const rightOrder = right.sortOrder ?? right.level;
      if (leftOrder !== rightOrder) {
        return leftOrder - rightOrder;
      }
      return left.level - right.level;
    });

  const handlePhoneValidation = (e) => {
    const { id, value } = e.target;
    getFromOpenElisServer(
      "/rest/PhoneNumberValidationProvider?fieldId=patientPhone&value=" +
        encodeURIComponent(value),
      (resp) => {
        const validation = { ...phoneValidation };
        validation[id] = resp;
        setPhoneValidation(validation);
      },
    );
  };

  useEffect(() => {
    if (typeof props.setPhoneValidation === "function") {
      props.setPhoneValidation(phoneValidation);
    }
  }, [phoneValidation]);
  // Reject characters that don't match the configured name regex BEFORE the
  // change bubbles to Formik's form-level onChange. Mutating
  // event.target.value to the last accepted value lets the form-level
  // handler write the cleaned value back to Formik in the same event tick.
  // The "last accepted value" is whatever Formik currently holds for the
  // field — no parallel `prev*Name` mirror needed.
  //
  // Empty is always allowed (the user clearing the field). The configured
  // name regex is `^[A-Za-z...]+$` which requires at least one character,
  // but rejecting a clear would be hostile UX.
  const blockInvalidName = (regexSource, currentValue) => (event) => {
    if (event.target.value === "") return;
    const regex = new RegExp(regexSource, "iu");
    if (!regex.test(event.target.value)) {
      event.target.value = currentValue || "";
    }
  };

  function fetchHealthDistrictsCallback(res) {
    setHealthDistricts(res);
  }

  // Edit-flow side effects: fetch health-districts cascade for the patient's
  // region, derive the years/months/days display state, and stream the
  // photo into Formik when it arrives. The form's initial Formik values
  // were already seeded from `selectedPatient` at mount via
  // buildInitialFormValues; this effect only owns post-mount fetches.
  useEffect(() => {
    if (!props.selectedPatient?.patientPK) return;

    if (props.selectedPatient.healthRegion != null) {
      getFromOpenElisServer(
        "/rest/health-districts-for-region?regionId=" +
          props.selectedPatient.healthRegion,
        fetchHealthDistrictsCallback,
      );
    } else {
      setHealthDistricts([]);
    }

    setFormAction("NO_ACTION");
    // Photo arrives via usePatientDetails → selectedPatient.photo →
    // buildInitialFormValues. No separate fetch from the form is needed.
  }, [props.selectedPatient?.patientPK]);

  useEffect(() => {
    componentMounted.current = true;
    getFromOpenElisServer(
      "/rest/displayList/PATIENT_HEALTH_REGIONS",
      fetchHeathRegions,
    );
    getFromOpenElisServer(
      "/rest/displayList/PATIENT_EDUCATION",
      fetchEducationList,
    );
    getFromOpenElisServer(
      "/rest/displayList/PATIENT_MARITAL_STATUS",
      fetchMaritalStatuses,
    );
    getFromOpenElisServer(
      "/rest/displayList/PATIENT_DISEASE_PROGRAMME",
      (list) => {
        if (componentMounted.current) {
          setDiseaseProgrammes(list || []);
        }
      },
    );
    return () => {
      componentMounted.current = false;
    };
  }, []);

  // Fetch address hierarchy levels when configurationProperties changes
  useEffect(() => {
    if (
      configurationProperties.USE_NEW_ADDRESS_HIERARCHY === "true" &&
      componentMounted.current
    ) {
      getFromOpenElisServer(
        "/rest/address-hierarchy/levels",
        fetchAddressHierarchyLevels,
      );
    }
  }, [configurationProperties.USE_NEW_ADDRESS_HIERARCHY]);

  // Initialize address hierarchy values when editing a patient
  // Initialize address hierarchy values when editing a patient
  useEffect(() => {
    const patientPK = props.selectedPatient?.patientPK;
    const addressHierarchy = props.selectedPatient?.addressHierarchy;

    // Skip if already initialized for this patient
    if (addressHierarchyInitialized === patientPK) {
      return;
    }

    if (
      configurationProperties.USE_NEW_ADDRESS_HIERARCHY === "true" &&
      patientPK &&
      addressHierarchy &&
      Object.keys(addressHierarchy).length > 0 &&
      addressHierarchyLevels.length > 0 &&
      addressHierarchyValues[0] &&
      addressHierarchyValues[0].length > 0
    ) {
      // Mark as initialized for this patient to prevent re-running
      setAddressHierarchyInitialized(patientPK);

      // For each saved level, fetch the children to populate the next dropdown
      const fetchChildrenForLevel = (levelIndex) => {
        if (levelIndex >= addressHierarchyLevels.length - 1) {
          return;
        }
        const value = addressHierarchy[`addressHierarchy_${levelIndex}`];
        if (value) {
          getFromOpenElisServer(
            `/rest/address-hierarchy/children?parentId=${value}`,
            (children) => {
              if (componentMounted.current && children) {
                setAddressHierarchyValues((prev) => ({
                  ...prev,
                  [levelIndex + 1]: children,
                }));
                // Continue to next level after state update
                setTimeout(() => {
                  fetchChildrenForLevel(levelIndex + 1);
                }, 100);
              }
            },
          );
        }
      };

      fetchChildrenForLevel(0);
    }
  }, [
    props.selectedPatient?.patientPK,
    props.selectedPatient?.addressHierarchy,
    addressHierarchyLevels,
    addressHierarchyValues[0],
    configurationProperties.USE_NEW_ADDRESS_HIERARCHY,
    addressHierarchyInitialized,
  ]);

  const fetchHeathRegions = (regions) => {
    if (componentMounted.current) {
      setHealthRegions(regions);
    }
  };

  const accessionNumberValidationResponse = (res, numberType, numberValue) => {
    let error;
    // Suppress the "already in use" notification when the patient hasn't
    // actually changed the id since loading — a freshly-loaded patient
    // legitimately matches its own existing record. Read current Formik
    // values via the bridge ref to avoid mirroring nationalId/subjectNumber
    // into local state.
    const currentValues = formikRef.current?.values || {};
    if (
      res.status === false &&
      (props.selectedPatient.nationalId !== currentValues.nationalId ||
        props.selectedPatient.subjectNumber !== currentValues.subjectNumber)
    ) {
      setNotificationVisible(true);
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: numberType + ":" + numberValue + " Already in use",
      });
      error = "duplicate";
    }
    return error;
  };

  const handleSubjectNoValidation = async (
    numberType,
    fieldId,
    numberValue,
  ) => {
    let error;
    if (numberValue !== "") {
      error = getFromOpenElisServer(
        `/rest/subjectNumberValidationProvider?fieldId=${fieldId}&numberType=${numberType}&subjectNumber=${numberValue}`,
        (response) =>
          accessionNumberValidationResponse(response, numberType, numberValue),
      );
    }
    return error;
  };

  const fetchMaritalStatuses = (statuses) => {
    if (componentMounted.current) {
      setMaritalStatuses(statuses);
    }
  };

  const fetchEducationList = (eductationList) => {
    if (componentMounted.current) {
      setEducationList(eductationList);
    }
  };

  const fetchHeathDistricts = (districts) => {
    setHealthDistricts(districts);
  };

  const handleSubmit = (values, formikBag) => {
    // Strip display-only age parts before they reach the wire — the backend
    // only knows birthDateForDisplay.
    const payload = { ...values };
    delete payload.years;
    delete payload.months;
    delete payload.days;

    postToOpenElisServerJsonResponse(
      "/rest/PatientManagement",
      JSON.stringify(payload),
      (response) => {
        setNotificationVisible(true);
        formikBag.setSubmitting(false);

        const isSuccess = !response?.error && !(response?.statusCode >= 400);
        if (isSuccess) {
          addNotification({
            title: intl.formatMessage({ id: "notification.title" }),
            message: intl.formatMessage({ id: "success.save.patient" }),
            kind: NotificationKinds.success,
          });
          // Reseed Formik so `dirty` clears, then drop edit mode so the
          // form re-locks. Same-URL history.push wouldn't remount the
          // component, so this is the only way the saved state shows.
          formikBag.resetForm({ values });
          setIsEditing(false);
          const savedId =
            props.selectedPatient?.patientPK ||
            response?.patientId ||
            response?.patientPK;
          history.push(
            savedId ? `/PatientManagement/${savedId}` : "/PatientManagement",
          );
          return;
        }

        // Surface the backend's actual error message rather than a generic
        // "save failed" — recognises messageKey/errorKey for i18n, plain
        // message/error strings, and Spring fieldErrors arrays.
        const message = resolveApiErrorMessage(
          intl,
          response,
          "error.save.patient",
        );
        addNotification({
          title: intl.formatMessage({ id: "notification.title" }),
          message,
          kind: NotificationKinds.error,
        });
      },
    );
  };

  const mergedIntoLabel =
    props.selectedPatient?.mergedIntoNationalId ||
    props.selectedPatient?.mergedIntoPatientId;

  return (
    <>
      {notificationVisible === true ? <AlertDialog /> : ""}
      {props.selectedPatient?.isMerged === true && (
        <InlineNotification
          kind="warning"
          lowContrast
          hideCloseButton
          title={intl.formatMessage({
            id: "patient.merged.banner.title",
            defaultMessage: "This patient record was merged",
          })}
          subtitle={
            mergedIntoLabel
              ? intl.formatMessage(
                  {
                    id: "patient.merged.banner.subtitle",
                    defaultMessage:
                      "Active records are kept on Patient {target}.",
                  },
                  { target: mergedIntoLabel },
                )
              : intl.formatMessage({
                  id: "patient.merged.banner.subtitle.noTarget",
                  defaultMessage:
                    "This record has been merged into another patient.",
                })
          }
        />
      )}
      <Formik
        initialValues={initialValues}
        innerRef={formikRef}
        validationSchema={validationSchema}
        validateOnChange={false}
        validateOnBlur={true}
        onSubmit={handleSubmit}
      >
        {({
          values,
          errors,
          touched,
          resetForm,
          handleChange,
          handleBlur,
          setFieldValue,
          submitForm,
          isSubmitting,
        }) => (
          <Form
            onSubmit={(e) => e.preventDefault()}
            onChange={handleChange}
            onBlur={handleBlur}
          >
            {props.orderFormValues && (
              <PatientFormObserver
                orderFormValues={props.orderFormValues}
                setOrderFormValues={props.setOrderFormValues}
                formAction={formAction}
                selectedPatient={props.selectedPatient}
              />
            )}
            {/* Heading + Edit toggle live OUTSIDE the disabled fieldset so the
                toggle stays clickable when the form is read-only.
                The Toggle must also be OUTSIDE FormLabel — FormLabel renders
                as <label>, which captures clicks and redirects them to its
                associated form control, preventing the Toggle from flipping. */}
            <Grid>
              <Column lg={16} md={8} sm={4}>
                <div
                  style={{
                    display: "flex",
                    alignItems: "center",
                    gap: "1rem",
                  }}
                >
                  <FormLabel>
                    <Section>
                      <Section>
                        <Section>
                          <Heading>
                            <FormattedMessage id="patient.label.info" />
                          </Heading>
                        </Section>
                      </Section>
                    </Section>
                  </FormLabel>
                  {isExistingPatient && (
                    <Toggle
                      id="patient-edit-toggle"
                      size="sm"
                      labelText=""
                      labelA={intl.formatMessage({
                        id: "label.button.edit",
                        defaultMessage: "Edit",
                      })}
                      labelB={intl.formatMessage({
                        id: "label.editing",
                        defaultMessage: "Editing",
                      })}
                      toggled={isEditing}
                      onToggle={(checked) => setIsEditing(checked)}
                    />
                  )}
                </div>
              </Column>
            </Grid>
            {/* Outer fieldset handles only the explicit `props.disabled` case
                (parent locks the whole form, accordions included). Read-only
                mode (toggle off for an existing patient) is applied via inner
                fieldsets — placed inside each AccordionItem and around the
                pre-accordion section — so the accordion headers themselves
                remain clickable and users can still expand sections to view
                the data. */}
            <fieldset disabled={!!props.disabled} className="fieldset-reset">
              <Grid>
                {/* Read-only fieldset for pre-accordion fields. `display:
                    contents` keeps it out of the Grid layout while still
                    propagating the disabled attribute to its descendants. */}
                <fieldset
                  disabled={isReadOnly}
                  className="fieldset-reset-contents"
                >
                  <Column lg={16} md={8} sm={4}>
                    {" "}
                    <br></br>
                  </Column>
                  <Column lg={16} md={8} sm={4}>
                    <PatientImageSelector
                      value={values.photo}
                      onChange={(photo) =>
                        handlePhotoChange(photo, setFieldValue)
                      }
                      required={false}
                      disabled={!!props.disabled}
                    />
                  </Column>
                  <Column lg={8} md={4} sm={4}>
                    <Field name="subjectNumber">
                      {({ field }) => (
                        <>
                          <TextInput
                            value={values.subjectNumber || ""}
                            name={field.name}
                            labelText={intl.formatMessage({
                              id: "patient.subject.number",
                            })}
                            id={field.name}
                            invalid={
                              errors.subjectNumber && touched.subjectNumber
                            }
                            invalidText={errors.subjectNumber}
                            onMouseOut={() => {
                              handleSubjectNoValidation(
                                "subjectNumber",
                                "subjectNumberID",
                                values.subjectNumber,
                              );
                            }}
                            placeholder={intl.formatMessage({
                              id: "patient.information.healthid",
                            })}
                          />
                        </>
                      )}
                    </Field>
                  </Column>
                  <Column lg={8} md={4} sm={4}>
                    <Field name="nationalId">
                      {({ field }) => (
                        <TextInput
                          value={values.nationalId || ""}
                          name={field.name}
                          labelText={
                            <>
                              {intl.formatMessage({
                                id: "patient.natioanalid",
                              })}
                              {nationalIdRequired && (
                                <span className="requiredlabel">*</span>
                              )}
                            </>
                          }
                          id={field.name}
                          invalid={
                            props.error
                              ? props.error("patientProperties.nationalId")
                                ? true
                                : false
                              : false
                          }
                          invalidText={
                            props.error
                              ? props.error("patientProperties.nationalId")
                              : ""
                          }
                          onMouseOut={() => {
                            handleSubjectNoValidation(
                              "nationalId",
                              "nationalID",
                              values.nationalId,
                            );
                          }}
                          placeholder={intl.formatMessage({
                            id: "patient.information.nationalid",
                          })}
                        />
                      )}
                    </Field>
                    <div className="error">
                      <ErrorMessage name="nationalId"></ErrorMessage>
                    </div>
                  </Column>
                  <Column lg={16} md={8} sm={4}>
                    {" "}
                    <br></br>
                  </Column>
                  <Column lg={8} md={4} sm={4}>
                    <Field name="lastName">
                      {({ field }) => (
                        <TextInput
                          value={values.lastName || ""}
                          name={field.name}
                          labelText={intl.formatMessage({
                            id: "patient.last.name",
                          })}
                          id={field.name}
                          invalid={errors.lastName && touched.lastName}
                          invalidText={errors.lastName}
                          placeholder={intl.formatMessage({
                            id: "patient.information.lastname",
                          })}
                          onChange={blockInvalidName(
                            configurationProperties.LAST_NAME_REGEX,
                            values.lastName,
                          )}
                        />
                      )}
                    </Field>
                  </Column>
                  <Column lg={8} md={4} sm={4}>
                    <Field name="firstName">
                      {({ field }) => (
                        <TextInput
                          value={values.firstName || ""}
                          name={field.name}
                          labelText={intl.formatMessage({
                            id: "patient.first.name",
                          })}
                          id={field.name}
                          invalid={errors.firstName && touched.firstName}
                          invalidText={errors.firstName}
                          placeholder={intl.formatMessage({
                            id: "patient.information.firstname",
                          })}
                          onChange={blockInvalidName(
                            configurationProperties.FIRST_NAME_REGEX,
                            values.firstName,
                          )}
                        />
                      )}
                    </Field>
                  </Column>
                  {aliasEnabled && (
                    <Column lg={8} md={4} sm={4}>
                      <Field name="aka">
                        {({ field }) => (
                          <TextInput
                            value={values.aka || ""}
                            name={field.name}
                            labelText={aliasLabel}
                            id={field.name}
                          />
                        )}
                      </Field>
                    </Column>
                  )}
                  <Column lg={16} md={8} sm={4}>
                    {" "}
                    <br></br>
                  </Column>
                  <Column lg={8} md={4} sm={4}>
                    <Field name="primaryPhone">
                      {({ field }) => (
                        <TextInput
                          value={values.primaryPhone || ""}
                          name={field.name}
                          onBlur={(e) => {
                            handlePhoneValidation(e);
                          }}
                          id="primaryPhone"
                          labelText={intl.formatMessage(
                            {
                              id: "patient.label.primaryphone",
                              defaultMessage: "Phone: {PHONE_FORMAT}",
                            },
                            { PHONE_FORMAT: "" },
                          )}
                          helperText={getPhoneFormatHint(
                            intl,
                            configurationProperties,
                          )}
                          invalid={!phoneValidation.primaryPhone.status}
                          invalidText={
                            phoneValidation.primaryPhone.status
                              ? ""
                              : phoneValidation.primaryPhone.body
                          }
                          placeholder={intl.formatMessage({
                            id: "patient.information.primaryphone",
                          })}
                        />
                      )}
                    </Field>
                  </Column>
                  <Column lg={8} md={4} sm={4}>
                    <Field name="email">
                      {({ field }) => (
                        <TextInput
                          value={values.email || ""}
                          name={field.name}
                          id="email"
                          labelText={intl.formatMessage({
                            id: "patient.label.email",
                          })}
                          placeholder={intl.formatMessage({
                            id: "patient.information.email",
                          })}
                          invalid={errors.email && touched.email}
                          invalidText={errors.email}
                        />
                      )}
                    </Field>
                    <div className="error">
                      <ErrorMessage name="email"></ErrorMessage>
                    </div>
                  </Column>
                  <Column lg={16} md={8} sm={4}>
                    {" "}
                    <br></br>
                  </Column>
                  <Column lg={4} md={4} sm={4}>
                    <Field name="gender">
                      {({ field }) => (
                        <RadioButtonGroup
                          valueSelected={values.gender}
                          legendText={
                            <>
                              {intl.formatMessage({ id: "patient.gender" })}{" "}
                              <span className="requiredlabel">*</span>
                            </>
                          }
                          name={field.name}
                          onChange={(value) => setFieldValue("gender", value)}
                          invalid={errors.gender && touched.gender}
                          invalidText={errors.gender}
                          id="create_patient_gender"
                        >
                          <RadioButton
                            id="radio-1"
                            labelText={intl.formatMessage({
                              id: "patient.male",
                            })}
                            value="M"
                          />
                          <RadioButton
                            id="radio-2"
                            labelText={intl.formatMessage({
                              id: "patient.female",
                            })}
                            value="F"
                          />
                        </RadioButtonGroup>
                      )}
                    </Field>
                    <div className="error">
                      <ErrorMessage name="gender"></ErrorMessage>
                    </div>
                  </Column>
                  <Column lg={4} md={4} sm={4}>
                    <Field name="birthDateForDisplay">
                      {({ field }) => (
                        <CustomDatePicker
                          id={"date-picker-default-id"}
                          labelText={
                            <>
                              {intl.formatMessage({
                                id: "patient.dob",
                              })}
                              <span className="requiredlabel">*</span>
                            </>
                          }
                          autofillDate={true}
                          value={values.birthDateForDisplay || ""}
                          onChange={(date) =>
                            handleDatePickerChange(setFieldValue, date)
                          }
                          invalid={
                            errors.birthDateForDisplay &&
                            touched.birthDateForDisplay
                          }
                          invalidText={errors.birthDateForDisplay}
                          name={field.name}
                          disallowFutureDate={true}
                          updateStateValue={true}
                        />
                      )}
                    </Field>
                  </Column>
                  <Column lg={3} md={2} sm={2}>
                    <TextInput
                      value={values.years ?? ""}
                      name="years"
                      labelText={intl.formatMessage({
                        id: "patient.age.years",
                      })}
                      id="years"
                      type="number"
                      min="0"
                      onChange={(e) =>
                        handleAgePartChange("years", e, values, setFieldValue)
                      }
                      placeholder={intl.formatMessage({
                        id: "patient.information.age",
                      })}
                    />
                  </Column>
                  <Column lg={3} md={2} sm={2}>
                    <TextInput
                      value={values.months ?? ""}
                      name="months"
                      labelText={intl.formatMessage({
                        id: "patient.age.months",
                      })}
                      type="number"
                      min="0"
                      onChange={(e) =>
                        handleAgePartChange("months", e, values, setFieldValue)
                      }
                      id="months"
                      placeholder={intl.formatMessage({
                        id: "patient.information.months",
                      })}
                    />
                  </Column>
                  <Column lg={2} md={2} sm={2}>
                    <TextInput
                      value={values.days ?? ""}
                      name="days"
                      type="number"
                      min="0"
                      onChange={(e) =>
                        handleAgePartChange("days", e, values, setFieldValue)
                      }
                      labelText={intl.formatMessage({ id: "patient.age.days" })}
                      id="days"
                      placeholder={intl.formatMessage({
                        id: "patient.information.days",
                      })}
                    />
                    <div className="error">
                      <ErrorMessage name="birthDateForDisplay"></ErrorMessage>
                    </div>
                  </Column>
                  <Column lg={16} md={8} sm={4}>
                    {" "}
                    <br></br>
                  </Column>
                </fieldset>
                <Column lg={16} md={8} sm={4}>
                  <Accordion>
                    <AccordionItem
                      title={intl.formatMessage({
                        id: "emergencyContactInfo.title",
                      })}
                    >
                      <fieldset
                        disabled={isReadOnly}
                        className="fieldset-reset"
                      >
                        <Grid>
                          <Column lg={16} md={8} sm={4}>
                            {" "}
                            <br></br>
                          </Column>
                          <Column lg={8} md={4} sm={4}>
                            <Field name="patientContact.person.lastName">
                              {({ field }) => (
                                <TextInput
                                  value={
                                    values.patientContact?.person?.lastName ||
                                    ""
                                  }
                                  name={field.name}
                                  labelText={intl.formatMessage({
                                    id: "patientcontact.person.lastname",
                                  })}
                                  id={field.name}
                                  onChange={blockInvalidName(
                                    configurationProperties.LAST_NAME_REGEX,
                                    values.patientContact?.person?.lastName,
                                  )}
                                  placeholder={intl.formatMessage({
                                    id: "patient.emergency.lastname",
                                  })}
                                />
                              )}
                            </Field>
                          </Column>
                          <Column lg={8} md={4} sm={4}>
                            <Field name="patientContact.person.firstName">
                              {({ field }) => (
                                <TextInput
                                  value={
                                    values.patientContact?.person?.firstName ||
                                    ""
                                  }
                                  name={field.name}
                                  labelText={intl.formatMessage({
                                    id: "patientcontact.person.firstname",
                                  })}
                                  id={field.name}
                                  onChange={(e) =>
                                    blockInvalidName(
                                      configurationProperties.FIRST_NAME_REGEX,
                                      values.patientContact?.person?.firstName,
                                    )(e)
                                  }
                                  placeholder={intl.formatMessage({
                                    id: "patient.emergency.firstname",
                                  })}
                                />
                              )}
                            </Field>
                          </Column>
                          <Column lg={16} md={8} sm={4}>
                            {" "}
                            <br></br>
                          </Column>
                          <Column lg={8} md={4} sm={4}>
                            <Field name="patientContact.person.email">
                              {({ field }) => (
                                <TextInput
                                  value={
                                    values.patientContact?.person?.email || ""
                                  }
                                  name={field.name}
                                  labelText={intl.formatMessage({
                                    id: "patientcontact.person.email",
                                  })}
                                  id={field.name}
                                  placeholder={intl.formatMessage({
                                    id: "patient.emergency.email",
                                  })}
                                />
                              )}
                            </Field>
                            <div className="error">
                              <ErrorMessage name="patientContact.person.email"></ErrorMessage>
                            </div>
                            <div className="error"></div>
                          </Column>
                          <Column lg={8} md={4} sm={4}>
                            <Field name="patientContact.person.primaryPhone">
                              {({ field }) => (
                                <TextInput
                                  value={
                                    values.patientContact?.person
                                      ?.primaryPhone || ""
                                  }
                                  name={field.name}
                                  id="contactPhone"
                                  onBlur={(e) => {
                                    handlePhoneValidation(e);
                                  }}
                                  labelText={intl.formatMessage(
                                    {
                                      id: "patient.label.contactphone",
                                      defaultMessage:
                                        "Contact Phone: {PHONE_FORMAT}",
                                    },
                                    { PHONE_FORMAT: "" },
                                  )}
                                  helperText={getPhoneFormatHint(
                                    intl,
                                    configurationProperties,
                                  )}
                                  invalid={!phoneValidation.contactPhone.status}
                                  invalidText={
                                    phoneValidation.contactPhone.status
                                      ? ""
                                      : phoneValidation.contactPhone.body
                                  }
                                  placeholder={intl.formatMessage({
                                    id: "patient.emergency.phone",
                                  })}
                                />
                              )}
                            </Field>
                          </Column>
                          <Column lg={16} md={8} sm={4}>
                            {" "}
                            <br></br>
                          </Column>
                        </Grid>
                      </fieldset>
                    </AccordionItem>
                    <AccordionItem
                      title={intl.formatMessage({
                        id: "patient.label.additionalInfo",
                      })}
                    >
                      <fieldset
                        disabled={isReadOnly}
                        className="fieldset-reset"
                      >
                        <Grid>
                          {/* Legacy address fields - Show ONLY if new hierarchy is disabled */}
                          {configurationProperties.USE_NEW_ADDRESS_HIERARCHY ===
                            "false" && (
                            <>
                              <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br></br>
                              </Column>
                              <Column lg={8} md={4} sm={4}>
                                <Field name="city">
                                  {({ field }) => (
                                    <TextInput
                                      value={values.city || ""}
                                      name={field.name}
                                      labelText={intl.formatMessage({
                                        id: "patient.address.town",
                                      })}
                                      id={field.name}
                                      placeholder={intl.formatMessage({
                                        id: "patient.emergency.additional.town",
                                      })}
                                    />
                                  )}
                                </Field>
                              </Column>
                              <Column lg={8} md={4} sm={4}>
                                <Field name="streetAddress">
                                  {({ field }) => (
                                    <TextInput
                                      value={values.streetAddress || ""}
                                      name={field.name}
                                      labelText={intl.formatMessage({
                                        id: "patient.address.street",
                                      })}
                                      id={field.name}
                                      placeholder={intl.formatMessage({
                                        id: "patient.emergency.additional.street",
                                      })}
                                    />
                                  )}
                                </Field>
                              </Column>
                              <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br></br>
                              </Column>
                              <Column lg={8} md={4} sm={4}>
                                <Field name="commune">
                                  {({ field }) => (
                                    <TextInput
                                      value={values.commune || ""}
                                      name={field.name}
                                      labelText={intl.formatMessage({
                                        id: "patient.address.camp",
                                      })}
                                      id={field.name}
                                      placeholder={intl.formatMessage({
                                        id: "patient.emergency.additional.camp",
                                      })}
                                    />
                                  )}
                                </Field>
                              </Column>
                            </>
                          )}
                          <Column lg={16} md={8} sm={4}>
                            {" "}
                            <br></br>
                          </Column>
                          {/* Address Hierarchy Section - Quick Search */}
                          {configurationProperties.USE_NEW_ADDRESS_HIERARCHY ===
                            "true" &&
                            addressHierarchyLevels.length > 0 && (
                              <Column lg={16} md={8} sm={4}>
                                <AddressSearch
                                  onAddressSelect={(levels) =>
                                    handleAddressSearchSelect(
                                      levels,
                                      setFieldValue,
                                    )
                                  }
                                  addressHierarchyLevels={
                                    addressHierarchyLevels
                                  }
                                />
                              </Column>
                            )}
                          {/* Dynamic Address Hierarchy fields — distro CSV
                              metadata controls input type, label, binding, and
                              render order. Dropdown cascade keys continue to
                              use logical hierarchy order so address search and
                              child loading stay stable. */}
                          {configurationProperties.USE_NEW_ADDRESS_HIERARCHY ===
                            "true" &&
                            addressHierarchyLevels.length > 0 &&
                            getAddressRenderLevels().map((level) => {
                              const levelIndex = getAddressLevelIndex(level);
                              const labelText = getAddressLevelLabel(level);
                              if (level.inputType === "freetext") {
                                const bindKey = level.bindKey;
                                if (!bindKey) {
                                  return null;
                                }
                                return (
                                  <Column
                                    lg={8}
                                    md={4}
                                    sm={4}
                                    key={level.level}
                                  >
                                    <Field name={bindKey}>
                                      {({ field }) => (
                                        <TextInput
                                          id={bindKey}
                                          name={field.name}
                                          value={values[bindKey] || ""}
                                          onChange={(e) =>
                                            setFieldValue(
                                              bindKey,
                                              e.target.value,
                                            )
                                          }
                                          labelText={labelText}
                                        />
                                      )}
                                    </Field>
                                  </Column>
                                );
                              }
                              // OGC-669: lg=5 so up to 3 dropdown levels fit one
                              // row (5*3=15, 1 col gutter).
                              return (
                                <Column lg={5} md={4} sm={4} key={level.level}>
                                  <Field
                                    name={`addressHierarchy_${levelIndex}`}
                                  >
                                    {({ field }) => (
                                      <Select
                                        id={`address_hierarchy_${levelIndex}`}
                                        value={
                                          values[
                                            `addressHierarchy_${levelIndex}`
                                          ] || ""
                                        }
                                        name={field.name}
                                        labelText={labelText}
                                        // Cascade UX — dependent dropdowns
                                        // disabled until parent selected.
                                        disabled={
                                          levelIndex > 0 &&
                                          !values[
                                            `addressHierarchy_${levelIndex - 1}`
                                          ]
                                        }
                                        onChange={(e) => {
                                          setFieldValue(
                                            `addressHierarchy_${levelIndex}`,
                                            e.target.value,
                                          );
                                          handleAddressHierarchySelection(
                                            levelIndex,
                                            e.target.value,
                                            setFieldValue,
                                          );
                                          // Keep legacy Region/District fields
                                          // in sync for existing consumers.
                                          if (
                                            level.typeName === "Health Region"
                                          ) {
                                            setFieldValue(
                                              "healthRegion",
                                              e.target.value,
                                            );
                                            handleRegionSelection(e, values);
                                          } else if (
                                            level.typeName === "Health District"
                                          ) {
                                            setFieldValue(
                                              "healthDistrict",
                                              e.target.value,
                                            );
                                          }
                                        }}
                                      >
                                        <SelectItem text="" value="" />
                                        {(
                                          addressHierarchyValues[levelIndex] ||
                                          []
                                        ).map((item, index) => (
                                          <SelectItem
                                            text={item.value}
                                            value={item.id}
                                            key={index}
                                          />
                                        ))}
                                      </Select>
                                    )}
                                  </Field>
                                </Column>
                              );
                            })}
                          {/* Legacy Health Region/District - Show ONLY if new hierarchy is explicitly disabled */}
                          {configurationProperties.USE_NEW_ADDRESS_HIERARCHY ===
                            "false" && (
                            <>
                              <Column lg={8} md={4} sm={4}>
                                <Field name="healthRegion">
                                  {({ field }) => (
                                    <Select
                                      id="health_region"
                                      value={values.healthRegion || ""}
                                      name={field.name}
                                      labelText={intl.formatMessage({
                                        id: "patient.address.healthregion",
                                      })}
                                      onChange={(e) =>
                                        handleRegionSelection(e, values)
                                      }
                                      helperText={intl.formatMessage({
                                        id: "patient.emergency.additional.region",
                                      })}
                                    >
                                      <SelectItem text="" value="" />
                                      {healthRegions?.map((region, index) => (
                                        <SelectItem
                                          text={region.value}
                                          value={region.id}
                                          key={index}
                                        />
                                      ))}
                                    </Select>
                                  )}
                                </Field>
                              </Column>

                              <Column lg={8} md={4} sm={4}>
                                <Field name="healthDistrict">
                                  {({ field }) => (
                                    <Select
                                      id="health_district"
                                      value={values.healthDistrict || ""}
                                      name={field.name}
                                      labelText={intl.formatMessage({
                                        id: "patient.address.healthdistrict",
                                      })}
                                      onChange={(e) =>
                                        setFieldValue(
                                          "healthDistrict",
                                          e.target.value,
                                        )
                                      }
                                      helperText={intl.formatMessage({
                                        id: "patient.emergency.additional.district",
                                      })}
                                    >
                                      <SelectItem text="" value="" />
                                      {healthDistricts?.map(
                                        (district, index) => (
                                          <SelectItem
                                            text={district.value}
                                            value={district.value}
                                            key={index}
                                          />
                                        ),
                                      )}
                                    </Select>
                                  )}
                                </Field>
                              </Column>
                            </>
                          )}

                          <Column lg={16} md={8} sm={4}>
                            {" "}
                            <br></br>
                          </Column>
                          {/* OGC-650 (LO-01-01): patient registration GPS lat/long.
                              Toggle-gated by PATIENT_GPS_CAPTURE_ENABLED config.
                              Default off in core OE2; on in Madagascar distro.
                              Row break ensures Lat + Long pair on a fresh row
                              instead of getting interleaved with the freetext
                              row above. */}
                          {configurationProperties.PATIENT_GPS_CAPTURE_ENABLED ===
                            "true" && (
                            <>
                              <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br></br>
                              </Column>
                              <Column lg={8} md={4} sm={4}>
                                <Field name="gpsLatitude">
                                  {({ field }) => (
                                    <TextInput
                                      id="gpsLatitude"
                                      name={field.name}
                                      value={values.gpsLatitude || ""}
                                      onChange={(e) =>
                                        setFieldValue(
                                          "gpsLatitude",
                                          e.target.value,
                                        )
                                      }
                                      labelText={intl.formatMessage({
                                        id: "patient.gps.latitude",
                                        defaultMessage: "GPS Latitude",
                                      })}
                                      placeholder={intl.formatMessage({
                                        id: "patient.gps.latitude.placeholder",
                                        defaultMessage:
                                          "e.g., -18.879190 (range -90 to 90)",
                                      })}
                                    />
                                  )}
                                </Field>
                              </Column>
                              <Column lg={8} md={4} sm={4}>
                                <Field name="gpsLongitude">
                                  {({ field }) => (
                                    <TextInput
                                      id="gpsLongitude"
                                      name={field.name}
                                      value={values.gpsLongitude || ""}
                                      onChange={(e) =>
                                        setFieldValue(
                                          "gpsLongitude",
                                          e.target.value,
                                        )
                                      }
                                      labelText={intl.formatMessage({
                                        id: "patient.gps.longitude",
                                        defaultMessage: "GPS Longitude",
                                      })}
                                      placeholder={intl.formatMessage({
                                        id: "patient.gps.longitude.placeholder",
                                        defaultMessage:
                                          "e.g., 47.507905 (range -180 to 180)",
                                      })}
                                    />
                                  )}
                                </Field>
                              </Column>
                            </>
                          )}
                          <Column lg={16} md={8} sm={4}>
                            {" "}
                            <br></br>
                          </Column>
                          <Column lg={8} md={4} sm={4}>
                            <Field name="education">
                              {({ field }) => (
                                <Select
                                  id="education"
                                  value={values.education || ""}
                                  name={field.name}
                                  labelText={intl.formatMessage({
                                    id: "patient.eduction",
                                  })}
                                  onChange={(e) =>
                                    setFieldValue("education", e.target.value)
                                  }
                                  helperText={intl.formatMessage({
                                    id: "patient.emergency.additional.education",
                                  })}
                                >
                                  <SelectItem text="" value="" />
                                  {educationList.map((education, index) => (
                                    <SelectItem
                                      text={education.value}
                                      value={education.value}
                                      key={index}
                                    />
                                  ))}
                                </Select>
                              )}
                            </Field>
                          </Column>
                          <Column lg={8} md={4} sm={4}>
                            <Field name="maritialStatus">
                              {({ field }) => (
                                <Select
                                  id="maritialStatus"
                                  value={values.maritialStatus || ""}
                                  name={field.name}
                                  labelText={intl.formatMessage({
                                    id: "patient.maritalstatus",
                                  })}
                                  onChange={(e) =>
                                    setFieldValue(
                                      "maritialStatus",
                                      e.target.value,
                                    )
                                  }
                                  helperText={intl.formatMessage({
                                    id: "patient.emergency.additional.maritalstatus",
                                  })}
                                >
                                  <SelectItem text="" value="" />
                                  {maritalStatuses.map((status, index) => (
                                    <SelectItem
                                      text={status.value}
                                      value={status.value}
                                      key={index}
                                    />
                                  ))}
                                </Select>
                              )}
                            </Field>
                          </Column>
                          <Column lg={16} md={8} sm={4}>
                            {" "}
                            <br></br>
                          </Column>
                          <Column lg={8} md={4} sm={4}>
                            <Field name="nationality">
                              {({ field }) => (
                                <Select
                                  id="nationality"
                                  value={
                                    values.nationality ||
                                    defaultNationality ||
                                    ""
                                  }
                                  name={field.name}
                                  labelText={intl.formatMessage({
                                    id: "patient.nationality",
                                  })}
                                  onChange={(e) =>
                                    setFieldValue("nationality", e.target.value)
                                  }
                                  helperText={intl.formatMessage({
                                    id: "patient.emergency.additional.nationnality",
                                  })}
                                >
                                  <SelectItem text="" value="" />
                                  {nationalityList.map((nationality, index) => (
                                    <SelectItem
                                      text={nationality.label}
                                      value={nationality.value}
                                      key={index}
                                    />
                                  ))}
                                </Select>
                              )}
                            </Field>
                          </Column>
                          <Column lg={8} md={4} sm={4}>
                            <Field name="otherNationality">
                              {({ field }) => (
                                <TextInput
                                  value={values.otherNationality || ""}
                                  name={field.name}
                                  labelText={intl.formatMessage({
                                    id: "patient.nationality.other",
                                  })}
                                  id={field.name}
                                  placeholder={intl.formatMessage({
                                    id: "patient.emergency.additional.othernationality",
                                  })}
                                />
                              )}
                            </Field>
                          </Column>
                          <Column lg={16} md={8} sm={4}>
                            {" "}
                            <br></br>
                          </Column>
                          <Column lg={8} md={4} sm={4}>
                            <Field name="occupation">
                              {({ field }) => (
                                <TextInput
                                  value={values.occupation || ""}
                                  name={field.name}
                                  labelText={intl.formatMessage({
                                    id: "patient.label.occupation",
                                  })}
                                  id={field.name}
                                />
                              )}
                            </Field>
                          </Column>
                          {/* OGC-669 (LO-01-01): Target type per Beth's spec
                              read = freetext-with-counter, not the dropdown
                              that previously rendered. Column kept as
                              targetDiseaseProgramme (varchar) so existing data
                              is preserved; only the input control changed. */}
                          <Column lg={8} md={4} sm={4}>
                            <Field name="targetDiseaseProgramme">
                              {({ field }) => (
                                <TextArea
                                  id="targetDiseaseProgramme"
                                  value={values.targetDiseaseProgramme || ""}
                                  name={field.name}
                                  onChange={(e) =>
                                    setFieldValue(
                                      "targetDiseaseProgramme",
                                      e.target.value,
                                    )
                                  }
                                  labelText={intl.formatMessage({
                                    id: "patient.label.diseaseProgramme",
                                  })}
                                  rows={3}
                                  maxCount={255}
                                  enableCounter
                                />
                              )}
                            </Field>
                          </Column>
                          <Column lg={16} md={8} sm={4}>
                            <Field name="customNotes">
                              {({ field }) => (
                                <TextArea
                                  value={values.customNotes || ""}
                                  name={field.name}
                                  labelText={intl.formatMessage({
                                    id: "patient.label.customNotes",
                                  })}
                                  id={field.name}
                                  rows={3}
                                  maxCount={255}
                                  enableCounter
                                />
                              )}
                            </Field>
                          </Column>
                        </Grid>
                      </fieldset>
                    </AccordionItem>
                    <AccordionItem title={idDocumentsLabel}>
                      <fieldset
                        disabled={isReadOnly}
                        className="fieldset-reset"
                      >
                        <IdentificationDocuments
                          patientId={props.selectedPatient?.patientPK || null}
                          pendingDocuments={values.idDocuments || []}
                          onDocumentsChange={(docs) =>
                            setFieldValue("idDocuments", docs)
                          }
                          disabled={!!props.disabled}
                        />
                      </fieldset>
                    </AccordionItem>
                  </Accordion>
                </Column>
                <Column lg={16} md={8} sm={4}>
                  {" "}
                  <br></br>
                </Column>
                {props.showActionsButton && !isReadOnly && (
                  <>
                    <Column lg={4} md={4} sm={4}>
                      <Button
                        type="button"
                        id="submit"
                        disabled={
                          isSubmitting ||
                          Object.values(phoneValidation).some(
                            (item) => item.status === false,
                          )
                        }
                        onClick={() => submitForm()}
                      >
                        <FormattedMessage id="label.button.save" />
                      </Button>
                    </Column>
                    <Column lg={4} md={4} sm={4}>
                      <Button
                        id="clear"
                        kind="danger"
                        disabled={isSubmitting}
                        onClick={() => {
                          // resetForm resets years/months/days alongside
                          // birthDateForDisplay — they're real Formik fields
                          // now, no parallel state to reset.
                          resetForm({
                            values: defaultNationality
                              ? {
                                  ...CreatePatientFormValues,
                                  nationality: defaultNationality,
                                }
                              : CreatePatientFormValues,
                          });
                          setHealthDistricts([]);
                          props.onClear?.();
                        }}
                      >
                        <FormattedMessage id="label.button.clear" />
                      </Button>
                    </Column>
                  </>
                )}
              </Grid>
            </fieldset>
          </Form>
        )}
      </Formik>
    </>
  );
}

export default injectIntl(CreatePatientForm);
