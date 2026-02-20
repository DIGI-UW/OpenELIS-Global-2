import React, { useState, useRef, useEffect, useContext } from "react";
import { FormattedMessage, injectIntl, useIntl } from "react-intl";
import "../Style.css";
import { getFromOpenElisServer, postToOpenElisServer } from "../utils/Utils";
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
} from "@carbon/react";
import AddressSearch from "./AddressSearch";

import { Formik, Field, ErrorMessage } from "formik";
import CreatePatientFormValues from "../formModel/innitialValues/CreatePatientFormValues";
import PatientFormObserver from "./PatientFormObserver";
import { AlertDialog, NotificationKinds } from "../common/CustomNotification";
import { NotificationContext, ConfigurationContext } from "../layout/Layout";
import CreatePatientValidationSchema from "../formModel/validationSchema/CreatePatientValidationShema";
import CustomDatePicker from "../common/CustomDatePicker";
import PatientImageSelector from "./photoManagement/uploadPhoto/PatientImageSelector";

function CreatePatientForm(props) {
  const componentMounted = useRef(false);

  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);
  const { configurationProperties } = useContext(ConfigurationContext);

  const intl = useIntl();

  const [patientDetails, setPatientDetails] = useState(CreatePatientFormValues);
  const [healthRegions, setHealthRegions] = useState([]);
  const [healthDistricts, setHealthDistricts] = useState([]);
  const [addressHierarchyLevels, setAddressHierarchyLevels] = useState([]);
  const [addressHierarchyValues, setAddressHierarchyValues] = useState({});
  const [addressHierarchyInitialized, setAddressHierarchyInitialized] =
    useState(null); // Track which patient's hierarchy is initialized
  const [educationList, setEducationList] = useState([]);
  const [maritalStatuses, setMaritalStatuses] = useState([]);
  const [prevfirstName, setPrevfirstName] = useState("");
  const [prevlastName, setPrevlastName] = useState("");
  const [prevfirstContactName, setPrevfirstContactName] = useState("");
  const [prevlastContactName, setPrevlastContactName] = useState("");
  const [formAction, setFormAction] = useState("ADD");
  const [dateOfBirthFormatter, setDateOfBirthFormatter] = useState({
    years: "",
    months: "",
    days: "",
  });
  const [nationalId, setNationalId] = useState(
    props.selectedPatient.nationalId,
  );
  const [subjectNo, setSubjectNo] = useState(
    props.selectedPatient.subjectNumber,
  );

  const [isSubmitting, setIsSubmitting] = useState(false);
  const [phoneValidation, setPhoneValidation] = useState({
    primaryPhone: { body: "", status: true },
    contactPhone: { body: "", status: true },
  });

  const handlePhotoChange = (photo, setFieldValue) => {
    if (setFieldValue) {
      setFieldValue("photo", photo);
    }
  };

  const handleNationalIdChange = (event) => {
    const newValue = event.target.value;
    setNationalId(newValue);
  };

  const handleSubjectNoChange = (event) => {
    const newValue = event.target.value;
    setSubjectNo(newValue);
  };
  const handleDatePickerChange = (values, date) => {
    var patient = { ...values };
    if ("date-picker-default-id" in patient) {
      delete patient["date-picker-default-id"];
    }
    patient.birthDateForDisplay = date;
    setPatientDetails(patient);
    if (patient.birthDateForDisplay) {
      getYearsMonthsDaysFromDOB(patient.birthDateForDisplay);
    }
  };

  function getYearsMonthsDaysFromDOB(date) {
    if (!date || date === "") {
      console.warn("trying to parse empty date");
      return;
    }
    const selectedDate = date.split("/");
    let yy;
    let mm;
    let dd;
    if (configurationProperties.DEFAULT_DATE_LOCALE == "fr-FR") {
      yy = parseInt(selectedDate[2]);
      mm = parseInt(selectedDate[1]);
      dd = parseInt(selectedDate[0]);
    } else {
      yy = parseInt(selectedDate[2]);
      mm = parseInt(selectedDate[0]);
      dd = parseInt(selectedDate[1]);
    }
    let formatDate = mm + "/" + dd + "/" + yy;

    const birthDate = new Date(formatDate);
    const now = new Date();
    const years = differenceInYears(now, birthDate);
    const months = differenceInMonths(now, addYears(birthDate, years));
    const days = differenceInDays(
      now,
      addMonths(addYears(birthDate, years), months),
    );

    setDateOfBirthFormatter({
      ...dateOfBirthFormatter,
      years: years,
      months: months,
      days: days,
    });
  }

  const getDOBByYearMonthsDays = (dobFormatter) => {
    const currentDate = new Date();
    const pastDate = new Date();

    pastDate.setFullYear(currentDate.getFullYear() - dobFormatter.years);
    pastDate.setMonth(currentDate.getMonth() - dobFormatter.months);
    pastDate.setDate(currentDate.getDate() - dobFormatter.days);
    const dob = format(
      new Date(pastDate),
      configurationProperties.DEFAULT_DATE_LOCALE == "fr-FR"
        ? "dd/MM/yyyy"
        : "MM/dd/yyyy",
    );
    setPatientDetails((prevState) => ({
      ...prevState,
      birthDateForDisplay: dob,
    }));
  };

  function handleYearsChange(e, values) {
    // Ensure years is not negative
    const years = Math.max(0, Number(e.target.value));

    // Update form values with the validated years
    setPatientDetails({
      ...values,
      // Update the specific field that contains years to ensure the form shows the corrected value
      [e.target.name]: years,
    });

    let dobFormatter = {
      ...dateOfBirthFormatter,
      years: years,
    };
    getDOBByYearMonthsDays(dobFormatter);
  }

  function handleMonthsChange(e, values) {
    // Ensure months is not negative
    const months = Math.max(0, Number(e.target.value));

    // Update form values with the validated months
    setPatientDetails({
      ...values,
      // Update the specific field that contains months to ensure the form shows the corrected value
      [e.target.name]: months,
    });

    let dobFormatter = {
      ...dateOfBirthFormatter,
      months: months,
    };
    getDOBByYearMonthsDays(dobFormatter);
  }

  function handleDaysChange(e, values) {
    // Ensure days is not negative
    const days = Math.max(0, Number(e.target.value));

    // Update form values with the validated days
    setPatientDetails({
      ...values,
      // Update the specific field that contains days to ensure the form shows the corrected value
      [e.target.name]: days,
    });

    let dobFormatter = {
      ...dateOfBirthFormatter,
      days: days,
    };
    getDOBByYearMonthsDays(dobFormatter);
  }
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

    console.log(
      "DEBUG handleAddressSearchSelect: selected levels:",
      hierarchyLevels,
    );

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
    console.log("DEBUG fetchAddressHierarchyLevels: levels received:", levels);
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
          console.log(
            "DEBUG fetchAddressHierarchyLevels: hasDefaults=",
            hasDefaults,
            "patientPK=",
            props.selectedPatient?.patientPK,
          );

          // Apply defaults for new patients only
          // Only apply if not editing an existing patient and defaults exist
          if (!props.selectedPatient?.patientPK && hasDefaults) {
            console.log(
              "DEBUG fetchAddressHierarchyLevels: Applying defaults for new patient",
            );
            applyAddressHierarchyDefaults(levels);
          }
        }
      });
    }
  };

  const applyAddressHierarchyDefaults = (levels) => {
    console.log("DEBUG: Applying address hierarchy defaults for new patient");

    // Build the defaults object and fetch child values for each level
    const defaults = {};
    const fetchChildrenForDefaultLevel = (levelIndex) => {
      if (levelIndex >= levels.length) {
        // All defaults applied, update patientDetails
        console.log("DEBUG: Final defaults to apply:", defaults);
        if (Object.keys(defaults).length > 0) {
          setPatientDetails((prev) => ({
            ...prev,
            ...defaults,
          }));
        }
        return;
      }

      const level = levels[levelIndex];
      if (level.defaultId) {
        defaults[`addressHierarchy_${levelIndex}`] = level.defaultId;
        console.log(
          `DEBUG: Set default for level ${levelIndex}: ${level.defaultId} (${level.defaultValue})`,
        );

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
                // Continue to next level
                fetchChildrenForDefaultLevel(levelIndex + 1);
              }
            },
          );
        } else {
          // Last level, finish
          fetchChildrenForDefaultLevel(levelIndex + 1);
        }
      } else {
        // No default for this level, stop cascading
        console.log(`DEBUG: No default for level ${levelIndex}, stopping`);
        if (Object.keys(defaults).length > 0) {
          setPatientDetails((prev) => ({
            ...prev,
            ...defaults,
          }));
        }
      }
    };

    // Start from level 0
    fetchChildrenForDefaultLevel(0);
  };

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
  function handleFirstNameChange(event) {
    const regexFlags = "iu";
    const regex = new RegExp(
      configurationProperties.FIRST_NAME_REGEX,
      regexFlags,
    );
    const value = event.target.value;
    if (!regex.test(value)) {
      event.target.value = prevfirstName;
    }
    setPrevfirstName(event.target.value);
  }

  function handleLastNameChange(event) {
    const regexFlags = "iu";
    const regex = new RegExp(
      configurationProperties.LAST_NAME_REGEX,
      regexFlags,
    );
    const value = event.target.value;
    if (!regex.test(value)) {
      event.target.value = prevlastName;
    }
    setPrevlastName(event.target.value);
  }

  function handleFirstContactNameChange(event) {
    const regexFlags = "iu";
    const regex = new RegExp(
      configurationProperties.FIRST_NAME_REGEX,
      regexFlags,
    );
    const value = event.target.value;
    if (!regex.test(value)) {
      event.target.value = prevfirstContactName;
    }
    setPrevfirstContactName(event.target.value);
  }

  function handleLastContactNameChange(event) {
    const regexFlags = "iu";
    const regex = new RegExp(
      configurationProperties.LAST_NAME_REGEX,
      regexFlags,
    );
    const value = event.target.value;
    if (!regex.test(value)) {
      event.target.value = prevlastContactName;
    }
    setPrevlastContactName(event.target.value);
  }

  function fetchHealthDistrictsCallback(res) {
    setHealthDistricts(res);
  }

  useEffect(() => {
    console.log(
      "DEBUG CreatePatientForm: selectedPatient changed:",
      props.selectedPatient,
    );
    console.log(
      "DEBUG CreatePatientForm: addressHierarchy:",
      props.selectedPatient?.addressHierarchy,
    );

    // Reset address hierarchy initialization when patient changes
    if (
      props.selectedPatient?.patientPK &&
      addressHierarchyInitialized !== props.selectedPatient.patientPK
    ) {
      console.log(
        "DEBUG: Resetting addressHierarchyInitialized for new patient",
        props.selectedPatient.patientPK,
      );
      setAddressHierarchyInitialized(null);
    }

    if (props.selectedPatient.patientPK) {
      if (props.selectedPatient.healthRegion != null) {
        getFromOpenElisServer(
          "/rest/health-districts-for-region?regionId=" +
            props.selectedPatient.healthRegion,
          fetchHealthDistrictsCallback,
        );
      } else {
        //nextState.healthDistricts = [];
        setHealthDistricts([]);
      }
      //merge objects together to avoid "A component is changing a controlled input to be uncontrolled"
      let patient = props.selectedPatient;
      patient.patientUpdateStatus = "UPDATE";
      patient.photo = "";
      //merge objects together to avoid "A component is changing a controlled input to be uncontrolled"
      const patientContactPerson = {
        ...patientDetails?.patientContact?.person,
        ...patient?.patientContact?.person,
      };
      const patientContact = {
        ...patientDetails?.patientContact,
        ...patient?.patientContact,
        person: patientContactPerson,
      };
      // Flatten addressHierarchy map into top-level form fields
      const flattenedAddressHierarchy = {};
      if (patient.addressHierarchy) {
        console.log(
          "DEBUG CreatePatientForm: Flattening addressHierarchy:",
          patient.addressHierarchy,
        );
        Object.entries(patient.addressHierarchy).forEach(([key, value]) => {
          flattenedAddressHierarchy[key] = value;
        });
        console.log(
          "DEBUG CreatePatientForm: Flattened:",
          flattenedAddressHierarchy,
        );
      }
      patient = {
        ...patientDetails,
        ...patient,
        ...flattenedAddressHierarchy,
        patientContact: patientContact,
      };
      setPatientDetails({
        ...patientDetails,
        ...patient,
        ...flattenedAddressHierarchy,
        patientContact: patientContact,
      });
      getYearsMonthsDaysFromDOB(patient.birthDateForDisplay);
      setFormAction("UPDATE");
      // Fetch patient photo if patient exists
      getFromOpenElisServer(
        `/rest/patient-photos/${patient.patientPK}/${false}`,
        (response) => {
          if (response && response.data) {
            // Update patient details with photo
            setPatientDetails((prevDetails) => ({
              ...prevDetails,
              photo: response.data,
            }));
          }
        },
      );
    }
  }, [props.selectedPatient]);

  const repopulatePatientInfo = () => {
    if (props.orderFormValues != null) {
      if (
        props.orderFormValues.patientProperties.firstName !== "" ||
        props.orderFormValues.patientProperties.guid !== ""
      ) {
        // Flatten addressHierarchy map into top-level form fields (same as selectedPatient useEffect)
        const patient = props.orderFormValues.patientProperties;
        const flattenedAddressHierarchy = {};
        if (patient.addressHierarchy) {
          console.log(
            "DEBUG repopulatePatientInfo: Flattening addressHierarchy:",
            patient.addressHierarchy,
          );
          Object.entries(patient.addressHierarchy).forEach(([key, value]) => {
            flattenedAddressHierarchy[key] = value;
          });
        }
        setPatientDetails({
          ...patient,
          ...flattenedAddressHierarchy,
        });
        getYearsMonthsDaysFromDOB(
          props.orderFormValues.patientProperties.birthDateForDisplay,
        );
      }
    }
  };

  useEffect(() => {
    componentMounted.current = true;
    getFromOpenElisServer("/rest/health-regions", fetchHeathRegions);
    getFromOpenElisServer("/rest/education-list", fetchEducationList);
    getFromOpenElisServer("/rest/marital-statuses", fetchMaritalStatuses);
    // getFromOpenElisServer("/rest/nationalities", fetchNationalities);
    repopulatePatientInfo();
    return () => {
      componentMounted.current = false;
    };
  }, []);

  // Fetch address hierarchy levels when configurationProperties changes
  useEffect(() => {
    console.log(
      "DEBUG: USE_NEW_ADDRESS_HIERARCHY =",
      configurationProperties.USE_NEW_ADDRESS_HIERARCHY,
      "componentMounted =",
      componentMounted.current,
    );
    if (
      configurationProperties.USE_NEW_ADDRESS_HIERARCHY === "true" &&
      componentMounted.current
    ) {
      console.log("DEBUG: Fetching address hierarchy levels...");
      getFromOpenElisServer(
        "/rest/address-hierarchy/levels",
        fetchAddressHierarchyLevels,
      );
    }
  }, [configurationProperties.USE_NEW_ADDRESS_HIERARCHY]);

  // Initialize address hierarchy values when editing a patient
  useEffect(() => {
    const patientPK = props.selectedPatient?.patientPK;
    const addressHierarchy = props.selectedPatient?.addressHierarchy;

    console.log(
      "DEBUG: Address hierarchy init useEffect triggered. Conditions:",
      {
        USE_NEW_ADDRESS_HIERARCHY:
          configurationProperties.USE_NEW_ADDRESS_HIERARCHY,
        patientPK: patientPK,
        addressHierarchy: addressHierarchy,
        addressHierarchyKeysLength: addressHierarchy
          ? Object.keys(addressHierarchy).length
          : 0,
        addressHierarchyLevelsLength: addressHierarchyLevels.length,
        addressHierarchyValues0: addressHierarchyValues[0],
        addressHierarchyValues0Length: addressHierarchyValues[0]?.length,
        alreadyInitialized: addressHierarchyInitialized === patientPK,
      },
    );

    // Skip if already initialized for this patient
    if (addressHierarchyInitialized === patientPK) {
      console.log(
        "DEBUG: Already initialized for patient",
        patientPK,
        ", skipping",
      );
      return;
    }

    if (
      configurationProperties.USE_NEW_ADDRESS_HIERARCHY === "true" &&
      patientPK &&
      addressHierarchy &&
      Object.keys(addressHierarchy).length > 0 &&
      addressHierarchyLevels.length > 0 &&
      addressHierarchyValues[0] && // Ensure level 0 options are loaded
      addressHierarchyValues[0].length > 0
    ) {
      console.log(
        "DEBUG: All conditions met! Initializing address hierarchy with saved values:",
        addressHierarchy,
      );

      // Mark as initialized for this patient to prevent re-running
      setAddressHierarchyInitialized(patientPK);

      // For each saved level, fetch the children to populate the next dropdown
      // Use a recursive approach with callbacks to ensure sequential execution
      const fetchChildrenForLevel = (levelIndex) => {
        if (levelIndex >= addressHierarchyLevels.length - 1) {
          console.log("DEBUG: Reached last level, done fetching children");
          return;
        }
        const value = addressHierarchy[`addressHierarchy_${levelIndex}`];
        console.log(`DEBUG: Level ${levelIndex} saved value: ${value}`);
        if (value) {
          getFromOpenElisServer(
            `/rest/address-hierarchy/children?parentId=${value}`,
            (children) => {
              console.log(
                `DEBUG: Fetched ${children ? children.length : 0} children for level ${levelIndex + 1}`,
              );
              if (componentMounted.current && children) {
                setAddressHierarchyValues((prev) => ({
                  ...prev,
                  [levelIndex + 1]: children,
                }));
                // Continue to next level after state update
                // Use setTimeout to allow React to process state update
                setTimeout(() => {
                  fetchChildrenForLevel(levelIndex + 1);
                }, 100);
              }
            },
          );
        } else {
          console.log(`DEBUG: No value for level ${levelIndex}, stopping`);
        }
      };

      // Start fetching from level 0
      fetchChildrenForLevel(0);
    } else {
      console.log("DEBUG: Conditions NOT met, skipping address hierarchy init");
    }
  }, [
    props.selectedPatient?.patientPK,
    props.selectedPatient?.addressHierarchy,
    addressHierarchyLevels,
    addressHierarchyValues[0], // Re-run when level 0 options are loaded
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
    if (
      res.status === false &&
      (props.selectedPatient.nationalId !== nationalId ||
        props.selectedPatient.subjectNumber !== subjectNo)
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

  const handleSubmit = async (values, { resetForm }) => {
    // Prevent multiple submissions.
    if (isSubmitting) {
      return;
    }

    setIsSubmitting(true);

    if ("years" in values) {
      delete values.years;
    }
    if ("months" in values) {
      delete values.months;
    }
    if ("days" in values) {
      delete values.days;
    }
    console.log(JSON.stringify(values));
    postToOpenElisServer(
      "/rest/PatientManagement",
      JSON.stringify(values),
      (status) => {
        handlePost(status);
        resetForm({ values: CreatePatientFormValues });
        setDateOfBirthFormatter({
          years: "",
          months: "",
          days: "",
        });
      },
    );
  };

  const handlePost = (status) => {
    setNotificationVisible(true);
    setIsSubmitting(false);
    if (status === 200) {
      addNotification({
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "success.save.patient" }),
        kind: NotificationKinds.success,
      });
    } else {
      addNotification({
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "error.save.patient" }),
        kind: NotificationKinds.error,
      });
    }
  };

  return (
    <>
      {notificationVisible === true ? <AlertDialog /> : ""}
      <Formik
        initialValues={patientDetails}
        enableReinitialize
        validationSchema={CreatePatientValidationSchema}
        validateOnChange={false}
        validateOnBlur={true}
        onSubmit={handleSubmit}
        onChange
      >
        {({
          values,
          errors,
          touched,
          resetForm,
          handleChange,
          handleBlur,
          handleSubmit,
          setFieldValue,
        }) => (
          <Form
            onSubmit={handleSubmit}
            onChange={handleChange}
            onBlur={handleBlur}
          >
            {props.orderFormValues && (
              <PatientFormObserver
                orderFormValues={props.orderFormValues}
                setOrderFormValues={props.setOrderFormValues}
                formAction={formAction}
              />
            )}
            <Grid>
              <Column lg={16} md={8} sm={4}>
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
              </Column>
              <Column lg={16} md={8} sm={4}>
                {" "}
                <br></br>
              </Column>
              <Column lg={16} md={8} sm={4}>
                <PatientImageSelector
                  value={values.photo}
                  onChange={(photo) => handlePhotoChange(photo, setFieldValue)}
                  required={false}
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
                        invalid={errors.subjectNumber && touched.subjectNumber}
                        invalidText={errors.subjectNumber}
                        onMouseOut={() => {
                          handleSubjectNoValidation(
                            "subjectNumber",
                            "subjectNumberID",
                            values.subjectNumber,
                          );
                        }}
                        onChange={handleSubjectNoChange}
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
                          <span className="requiredlabel">*</span>
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
                      onChange={handleNationalIdChange}
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
                      onChange={(e) => handleLastNameChange(e)}
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
                      onChange={(e) => handleFirstNameChange(e)}
                    />
                  )}
                </Field>
              </Column>
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
                        { PHONE_FORMAT: configurationProperties.PHONE_FORMAT },
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
                      invalid={errors.gender && touched.gender}
                      invalidText={errors.gender}
                      id="create_patient_gender"
                    >
                      <RadioButton
                        id="radio-1"
                        labelText={intl.formatMessage({ id: "patient.male" })}
                        value="M"
                      />
                      <RadioButton
                        id="radio-2"
                        labelText={intl.formatMessage({ id: "patient.female" })}
                        value="F"
                      />
                    </RadioButtonGroup>
                  )}
                </Field>
                <div className="error">
                  <ErrorMessage name="gender"></ErrorMessage>
                </div>
              </Column>
              <Column lg={16} md={8} sm={4}>
                {" "}
                <br></br>
              </Column>
              <Column lg={8} md={4} sm={4}>
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
                      onChange={(date) => handleDatePickerChange(values, date)}
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
              <Column lg={2} md={2} sm={2}>
                <TextInput
                  value={dateOfBirthFormatter.years}
                  name="years"
                  labelText={intl.formatMessage({
                    id: "patient.age.years",
                  })}
                  id="years"
                  type="number"
                  min="0"
                  onChange={(e) => handleYearsChange(e, values)}
                  placeholder={intl.formatMessage({
                    id: "patient.information.age",
                  })}
                />
              </Column>
              <Column lg={2} md={2} sm={2}>
                <TextInput
                  value={dateOfBirthFormatter.months}
                  name="months"
                  labelText={intl.formatMessage({ id: "patient.age.months" })}
                  type="number"
                  min="0"
                  onChange={(e) => handleMonthsChange(e, values)}
                  id="months"
                  placeholder={intl.formatMessage({
                    id: "patient.information.months",
                  })}
                />
              </Column>
              <Column lg={2} md={2} sm={2}>
                <TextInput
                  value={dateOfBirthFormatter.days}
                  name="days"
                  type="number"
                  min="0"
                  onChange={(e) => handleDaysChange(e, values)}
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
              <Column lg={16} md={8} sm={4}>
                <Accordion>
                  <AccordionItem
                    title={intl.formatMessage({
                      id: "emergencyContactInfo.title",
                    })}
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
                                values.patientContact?.person?.lastName || ""
                              }
                              name={field.name}
                              labelText={intl.formatMessage({
                                id: "patientcontact.person.lastname",
                              })}
                              id={field.name}
                              onChange={(e) => handleLastContactNameChange(e)}
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
                                values.patientContact?.person?.firstName || ""
                              }
                              name={field.name}
                              labelText={intl.formatMessage({
                                id: "patientcontact.person.firstname",
                              })}
                              id={field.name}
                              onChange={(e) => handleFirstContactNameChange(e)}
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
                              value={values.patientContact?.person?.email || ""}
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
                                values.patientContact?.person?.primaryPhone ||
                                ""
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
                                {
                                  PHONE_FORMAT:
                                    configurationProperties.PHONE_FORMAT,
                                },
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
                  </AccordionItem>
                  <AccordionItem
                    title={intl.formatMessage({
                      id: "patient.label.additionalInfo",
                    })}
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
                                handleAddressSearchSelect(levels, setFieldValue)
                              }
                              addressHierarchyLevels={addressHierarchyLevels}
                            />
                          </Column>
                        )}
                      {/* Dynamic Address Hierarchy Dropdowns - Always show when new hierarchy is enabled */}
                      {configurationProperties.USE_NEW_ADDRESS_HIERARCHY ===
                        "true" &&
                        addressHierarchyLevels.length > 0 &&
                        addressHierarchyLevels.map((level, levelIndex) => (
                          <Column lg={8} md={4} sm={4} key={level.level}>
                            <Field name={`addressHierarchy_${levelIndex}`}>
                              {({ field }) => (
                                <Select
                                  id={`address_hierarchy_${levelIndex}`}
                                  value={
                                    values[`addressHierarchy_${levelIndex}`] ||
                                    ""
                                  }
                                  name={field.name}
                                  labelText={level.typeName}
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
                                    // For backward compatibility, also set healthRegion/healthDistrict
                                    if (levelIndex === 0) {
                                      setFieldValue(
                                        "healthRegion",
                                        e.target.value,
                                      );
                                      handleRegionSelection(e, values);
                                    } else if (levelIndex === 1) {
                                      setFieldValue(
                                        "healthDistrict",
                                        e.target.value,
                                      );
                                    }
                                  }}
                                >
                                  <SelectItem text="" value="" />
                                  {(
                                    addressHierarchyValues[levelIndex] || []
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
                        ))}
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
                                  onChange={() => {}}
                                  helperText={intl.formatMessage({
                                    id: "patient.emergency.additional.district",
                                  })}
                                >
                                  <SelectItem text="" value="" />
                                  {healthDistricts.map((district, index) => (
                                    <SelectItem
                                      text={district.value}
                                      value={district.value}
                                      key={index}
                                    />
                                  ))}
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
                              onChange={() => {}}
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
                              onChange={() => {}}
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
                              value={values.nationality || ""}
                              name={field.name}
                              labelText={intl.formatMessage({
                                id: "patient.nationality",
                              })}
                              onChange={() => {}}
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
                    </Grid>
                  </AccordionItem>
                </Accordion>
              </Column>
              <Column lg={16} md={8} sm={4}>
                {" "}
                <br></br>
              </Column>
              {props.showActionsButton && (
                <>
                  <Column lg={4} md={4} sm={4}>
                    <Button
                      type="submit"
                      id="submit"
                      disabled={
                        isSubmitting ||
                        Object.values(phoneValidation).some(
                          (item) => item.status === false,
                        )
                      }
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
                        resetForm({ values: CreatePatientFormValues });
                        setHealthDistricts([]);
                        setDateOfBirthFormatter({
                          years: "",
                          months: "",
                          days: "",
                        });
                      }}
                    >
                      <FormattedMessage id="label.button.clear" />
                    </Button>
                  </Column>
                </>
              )}
            </Grid>
          </Form>
        )}
      </Formik>
    </>
  );
}

export default injectIntl(CreatePatientForm);
