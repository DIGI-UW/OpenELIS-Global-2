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

import { Formik, Field, ErrorMessage } from "formik";
import CreatePatientFormValues from "../formModel/innitialValues/CreatePatientFormValues";
import PatientFormObserver from "./PatientFormObserver";
import { AlertDialog, NotificationKinds } from "../common/CustomNotification";
import { NotificationContext, ConfigurationContext } from "../layout/Layout";
import CreatePatientValidationSchema from "../formModel/validationSchema/CreatePatientValidationShema";
import CustomDatePicker from "../common/CustomDatePicker";
function CreatePatientForm(props) {
  const componentMounted = useRef(false);

  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);
  const { configurationProperties } = useContext(ConfigurationContext);

  const intl = useIntl();

  const [patientDetails, setPatientDetails] = useState(CreatePatientFormValues);
  const [healthRegions, setHealthRegions] = useState([]);
  const [healthDistricts, setHealthDistricts] = useState([]);
  const [educationList, setEducationList] = useState([]);
  const [maritalStatuses, setMaritalStatuses] = useState([]);
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
    setPatientDetails(values);
    let years = e.target.value;
    let dobFormatter = {
      ...dateOfBirthFormatter,
      years: years,
    };
    getDOBByYearMonthsDays(dobFormatter);
  }

  function handleMonthsChange(e, values) {
    setPatientDetails(values);
    let months = e.target.value;
    let dobFormatter = {
      ...dateOfBirthFormatter,
      months: months,
    };
    getDOBByYearMonthsDays(dobFormatter);
  }

  function handleDaysChange(e, values) {
    setPatientDetails(values);
    let days = e.target.value;
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

  function fetchHealthDistrictsCallback(res) {
    setHealthDistricts(res);
  }

  useEffect(() => {
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
      let patient = props.selectedPatient;
      patient.patientUpdateStatus = "UPDATE";
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
      patient = {
        ...patientDetails,
        ...patient,
        patientContact: patientContact,
      };
      setPatientDetails({
        ...patientDetails,
        ...patient,
        patientContact: patientContact,
      });
      getYearsMonthsDaysFromDOB(patient.birthDateForDisplay);
      setFormAction("UPDATE");
    }
  }, [props.selectedPatient]);

  const repopulatePatientInfo = () => {
    if (props.orderFormValues != null) {
      if (
        props.orderFormValues.patientProperties.firstName !== "" ||
        props.orderFormValues.patientProperties.guid !== ""
      ) {
        setPatientDetails(props.orderFormValues.patientProperties);
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
    console.debug(JSON.stringify(values));
    postToOpenElisServer(
      "/rest/patient-management",
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
                      labelText={intl.formatMessage(
                        {
                          id: "patient.label.primaryphone",
                          defaultMessage: "Phone: {PHONE_FORMAT}",
                        },
                        { PHONE_FORMAT: configurationProperties.PHONE_FORMAT },
                      )}
                      id={field.name}
                      invalid={errors.primaryPhone && touched.primaryPhone}
                      invalidText={errors.primaryPhone}
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
                              id={field.name}
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
                      <Column lg={16} md={8} sm={4}>
                        {" "}
                        <br></br>
                      </Column>
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
                              onChange={(e) => handleRegionSelection(e, values)}
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
                                id: "pateint.eduction",
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
                                id: "patient.emergency.additional.maritialstatus",
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
                    <Button type="submit" id="submit" disabled={isSubmitting}>
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
