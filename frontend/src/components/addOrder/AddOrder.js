import React, { useContext, useEffect, useRef, useState } from "react";
import {
  Checkbox,
  Link,
  Select,
  SelectItem,
  Stack,
  TextInput,
  TimePicker,
  Column,
  Grid,
} from "@carbon/react";
import CustomLabNumberInput from "../common/CustomLabNumberInput";
import CustomDatePicker from "../common/CustomDatePicker";
import { getFromOpenElisServer } from "../utils/Utils";
import { NotificationContext } from "../layout/Layout";
import { priorities } from "../data/orderOptions";
import { NotificationKinds } from "../common/CustomNotification";
import AutoComplete from "../common/AutoComplete";
import OrderResultReporting from "./OrderResultReporting";
import { FormattedMessage, useIntl } from "react-intl";
import { ConfigurationContext } from "../layout/Layout";
const AddOrder = (props) => {
  const { setNotificationVisible, addNotification } =
    useContext(NotificationContext);
  const { configurationProperties } = useContext(ConfigurationContext);

  const intl = useIntl();

  const componentMounted = useRef(false);

  const {
    orderFormValues,
    setOrderFormValues,
    samples,
    error,
    isModifyOrder,
    changed,
    setChanged,
  } = props;
  const [otherSamplingVisible, setOtherSamplingVisible] = useState(false);
  const [providers, setProviders] = useState([]);
  const [paymentOptions, setPaymentOptions] = useState([]);
  const [samplingPerformed, setSamplingPerformed] = useState([]);
  const [siteNames, setSiteNames] = useState([]);
  const [innitialized, setInnitialized] = useState(false);
  const [departments, setDepartments] = useState([]);

  useEffect(() => {
    componentMounted.current = true;
    getFromOpenElisServer("/rest/SamplePatientEntry", getSampleEntryPreform);
    window.scrollTo(0, 0);
    return () => {
      componentMounted.current = false;
    };
  }, []);

  const handleDatePickerChange = (datePicker, date) => {
    let obj = null;
    switch (datePicker) {
      case "requestDate":
        obj = { ...orderFormValues.sampleOrderItems, requestDate: date };
        break;
      case "receivedDate":
        obj = {
          ...orderFormValues.sampleOrderItems,
          receivedDateForDisplay: date,
        };
        break;
      case "nextVisitDate":
        obj = { ...orderFormValues.sampleOrderItems, nextVisitDate: date };
        break;
      default:
    }
    setOrderFormValues({
      ...orderFormValues,
      sampleOrderItems: obj,
    });
  };

  function handlePaymentStatus(e) {
    setOrderFormValues({
      ...orderFormValues,
      sampleOrderItems: {
        ...orderFormValues.sampleOrderItems,
        paymentOptionSelection: e.target.value,
      },
    });
  }

  function handleBillReferenceNo(e) {
    setOrderFormValues({
      ...orderFormValues,
      sampleOrderItems: {
        ...orderFormValues.sampleOrderItems,
        billingReferenceNumber: e.target.value,
      },
    });
  }

  function handleRequesterFax(e) {
    setOrderFormValues({
      ...orderFormValues,
      sampleOrderItems: {
        ...orderFormValues.sampleOrderItems,
        providerFax: e.target.value,
      },
    });
  }

  function handleRequesterEmail(e) {
    setOrderFormValues({
      ...orderFormValues,
      sampleOrderItems: {
        ...orderFormValues.sampleOrderItems,
        providerEmail: e.target.value,
      },
    });
  }

  function handleProvisionalClinicalDiagnosisChange(e) {
    setOrderFormValues({
      ...orderFormValues,
      sampleOrderItems: {
        ...orderFormValues.sampleOrderItems,
        provisionalClinicalDiagnosis: e.target.value,
      },
    });
    setNotificationVisible(false);
  }

  function handleRequesterWorkPhone(e) {
    setOrderFormValues({
      ...orderFormValues,
      sampleOrderItems: {
        ...orderFormValues.sampleOrderItems,
        providerWorkPhone: e.target.value,
      },
    });
    setNotificationVisible(false);
  }

  function handleRequesterFirstName(e) {
    setOrderFormValues({
      ...orderFormValues,
      sampleOrderItems: {
        ...orderFormValues.sampleOrderItems,
        providerFirstName: e.target.value,
      },
    });
  }
  function handleChange(path) {
    console.log([path]);
    setChanged({
      ...changed,
      [path]: true,
    });
  }

  function handleRequesterLastName(e) {
    setOrderFormValues({
      ...orderFormValues,
      sampleOrderItems: {
        ...orderFormValues.sampleOrderItems,
        providerLastName: e.target.value,
      },
    });
  }

  const handleSamplingPerformed = (e) => {
    const { value } = e.target;
    if (value === "1310") {
      setOtherSamplingVisible(!otherSamplingVisible);
    } else {
      setOtherSamplingVisible(false);
    }
    setOrderFormValues({
      ...orderFormValues,
      sampleOrderItems: {
        ...orderFormValues.sampleOrderItems,
        testLocationCode: value,
      },
    });
  };

  function handleOtherLocationCode(e) {
    setOrderFormValues({
      ...orderFormValues,
      sampleOrderItems: {
        ...orderFormValues.sampleOrderItems,
        otherLocationCode: e.target.value,
      },
    });
  }

  function handleReceivedTime(e) {
    setOrderFormValues({
      ...orderFormValues,
      sampleOrderItems: {
        ...orderFormValues.sampleOrderItems,
        receivedTime: e.target.value,
      },
    });
  }

  const handleLabNoGeneration = (e) => {
    if (e) {
      e.preventDefault();
    }
    getFromOpenElisServer(
      "/rest/SampleEntryGenerateScanProvider",
      fetchGeneratedAccessionNo,
    );
  };

  function accessionNumberValidationResults(res) {
    if (res.status === false) {
      setNotificationVisible(true);
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: res.body,
      });
    }
  }

  function handleProviderSelectOptions(providerId) {
    setOrderFormValues({
      ...orderFormValues,
      sampleOrderItems: {
        ...orderFormValues.sampleOrderItems,
        providerPersonId: providerId,
      },
    });

    getFromOpenElisServer(
      "/rest/practitioner?providerId=" + providerId,
      fetchPractitioner,
    );
  }

  function fetchPractitioner(data) {
    setOrderFormValues({
      ...orderFormValues,
      sampleOrderItems: {
        ...orderFormValues.sampleOrderItems,
        providerFirstName: data.person.firstName,
        providerLastName: data.person.lastName,
        providerWorkPhone: data.person.workPhone,
        providerEmail: data.person.email,
        providerFax: data.person.fax,
        providerId: data.id,
        providerPersonId: data.person.id,
        referringSiteName: "",
      },
    });
  }

  function handleRequesterDept(e) {
    setOrderFormValues({
      ...orderFormValues,
      sampleOrderItems: {
        ...orderFormValues.sampleOrderItems,
        referringSiteDepartmentId: e.target.value,
        referringSiteName: "",
      },
    });
  }

  function handleSiteName(e) {
    setOrderFormValues({
      ...orderFormValues,
      sampleOrderItems: {
        ...orderFormValues.sampleOrderItems,
        referringSiteName: e.target.value,
        referringSiteId: "",
        referringSiteDepartmentId: "",
      },
    });
  }

  function clearProviderId(e) {
    if (e.target.value == "") {
      setOrderFormValues({
        ...orderFormValues,
        sampleOrderItems: {
          ...orderFormValues.sampleOrderItems,
          providerId: "",
          providerPersonId: "",
        },
      });
    }
  }

  function handleAutoCompleteSiteName(siteId) {
    setOrderFormValues({
      ...orderFormValues,
      sampleOrderItems: {
        ...orderFormValues.sampleOrderItems,
        referringSiteId: siteId,
        referringSiteName: "",
        referringSiteDepartmentId: "",
      },
    });
  }
  const loadDepartments = (data) => {
    setDepartments(data);
  };

  function handleLabNo(e, rawVal) {
    if (isModifyOrder) {
      setOrderFormValues({
        ...orderFormValues,
        newAccessionNumber: e?.target?.value,
      });
    } else {
      setOrderFormValues({
        ...orderFormValues,
        sampleOrderItems: {
          ...orderFormValues.sampleOrderItems,
          labNo: rawVal ? rawVal : e?.target?.value,
        },
      });
    }
    handleLabNoValidationOnChange(e?.target?.value);
    setNotificationVisible(false);
  }

  const handleLabNoValidation = () => {
    if (orderFormValues.sampleOrderItems.labNo !== "") {
      getFromOpenElisServer(
        "/rest/SampleEntryAccessionNumberValidation?ignoreYear=false&ignoreUsage=false&field=labNo&accessionNumber=" +
          orderFormValues.sampleOrderItems.labNo,
        accessionNumberValidationResults,
      );
    }
  };

  const handleLabNoValidationOnChange = (value) => {
    if (value !== "") {
      getFromOpenElisServer(
        "/rest/SampleEntryAccessionNumberValidation?ignoreYear=false&ignoreUsage=false&field=labNo&accessionNumber=" +
          value,
        accessionNumberValidationResults,
      );
    }
  };

  function fetchPhoneNoValidation(res) {
    if (res.status === false) {
      addNotification({
        title: intl.formatMessage({ id: "notification.title" }),
        message: res.body,
        kind: NotificationKinds.error,
      });
      setNotificationVisible(true);
    }
  }

  const handlePhoneNoValidation = () => {
    if (orderFormValues.sampleOrderItems.providerWorkPhone) {
      const providerPhoneNo =
        orderFormValues.sampleOrderItems.providerWorkPhone.replace(
          /\+/g,
          "%2B",
        );
      getFromOpenElisServer(
        "/rest/PhoneNumberValidationProvider?fieldId=providerWorkPhoneID&value=" +
          providerPhoneNo,
        fetchPhoneNoValidation,
      );
    }
  };

  function handleRememberCheckBox(e) {
    let checked = false;
    if (e.currentTarget.checked) {
      checked = true;
    }
    setOrderFormValues({
      ...orderFormValues,
      rememberSiteAndRequester: checked,
    });
  }

  useEffect(() => {
    if (!innitialized) {
      setOrderFormValues({
        ...orderFormValues,
        sampleOrderItems: {
          ...orderFormValues.sampleOrderItems,
          requestDate: configurationProperties.currentDateAsText,
          receivedDateForDisplay: configurationProperties.currentDateAsText,
          nextVisitDate: configurationProperties.currentDateAsText,
          receivedTime: configurationProperties.currentTimeAsText,
        },
      });
    }
    if (orderFormValues.sampleOrderItems.requestDate != "") {
      setInnitialized(true);
    }
  }, [orderFormValues]);

  useEffect(() => {
    getFromOpenElisServer(
      "/rest/departments-for-site?refferingSiteId=" +
        (orderFormValues.sampleOrderItems.referringSiteId || ""),
      loadDepartments,
    );
  }, [orderFormValues.sampleOrderItems.referringSiteId]);

  function handlePriority(e) {
    setOrderFormValues({
      ...orderFormValues,
      sampleOrderItems: {
        ...orderFormValues.sampleOrderItems,
        priority: e.target.value,
      },
    });
  }

  function fetchGeneratedAccessionNo(res) {
    if (res.status) {
      if (isModifyOrder) {
        setOrderFormValues({
          ...orderFormValues,
          newAccessionNumber: res.body,
        });
      } else {
        setOrderFormValues({
          ...orderFormValues,
          sampleOrderItems: {
            ...orderFormValues.sampleOrderItems,
            labNo: res.body,
          },
        });
      }

      setNotificationVisible(false);
    }
  }

  const reportingNotifications = (object) => {
    setOrderFormValues({
      ...orderFormValues,
      customNotificationLogic: true,
      patientSMSNotificationTestIds: object.patientSMSNotificationTestIds,
      patientEmailNotificationTestIds: object.patientEmailNotificationTestIds,
      providerSMSNotificationTestIds: object.providerSMSNotificationTestIds,
      providerEmailNotificationTestIds: object.providerEmailNotificationTestIds,
    });
  };

  const getSampleEntryPreform = (response) => {
    if (componentMounted.current) {
      setSiteNames(response.sampleOrderItems.referringSiteList);
      setPaymentOptions(response.sampleOrderItems.paymentOptions);
      setSamplingPerformed(response.sampleOrderItems.testLocationCodeList);
      setProviders(response.sampleOrderItems.providersList);
    }
  };

  const handleKeyPress = (event) => {
    if (event.key === "Enter") {
      handleLabNoGeneration(event);
    }
  };

  return (
    <>
      <Stack gap={10}>
        <div className="orderLegendBody">
          <Grid>
            <Column lg={16} md={8} sm={4}>
              <h3>
                <FormattedMessage id="order.title" />
              </h3>
            </Column>
            {configurationProperties.ACCEPT_EXTERNAL_ORDERS === "true" && (
              <Column lg={16} md={8} sm={4}>
                <input
                  type="hidden"
                  name="externalOrderNumber"
                  id="externalOrderNumber"
                  value={orderFormValues.sampleOrderItems.externalOrderNumber}
                />
              </Column>
            )}
            {isModifyOrder && (
              <Column lg={16} md={8} sm={4}>
                <h5>
                  {" "}
                  <FormattedMessage id="sample.label.labnumber" />:{" "}
                  {orderFormValues.accessionNumber}
                </h5>
              </Column>
            )}

            <Column lg={8} md={4} sm={4}>
              <div>
                <CustomLabNumberInput
                  name="labNo"
                  placeholder={intl.formatMessage({
                    id: "input.placeholder.labNo",
                  })}
                  value={
                    isModifyOrder
                      ? orderFormValues.newAccessionNumber
                      : orderFormValues.sampleOrderItems.labNo
                  }
                  //onMouseLeave={handleLabNoValidation}
                  onClick={() => handleChange("sampleOrderItems.labNo")}
                  onChange={handleLabNo}
                  onKeyPress={handleKeyPress}
                  labelText={
                    <>
                      <FormattedMessage id="sample.label.labnumber" />{" "}
                      <span className="requiredlabel">*</span>
                    </>
                  }
                  id="labNo"
                  invalid={
                    changed["sampleOrderItems.labNo"] &&
                    error("sampleOrderItems.labNo")
                      ? true
                      : false
                  }
                  invalidText={error("sampleOrderItems.labNo")}
                />
                <div>
                  <FormattedMessage id="label.order.scan.text" />{" "}
                  <Link
                    data-cy="generate-labNumber"
                    href="#"
                    onClick={(e) => handleLabNoGeneration(e)}
                  >
                    <FormattedMessage id="sample.label.labnumber.generate" />
                  </Link>
                </div>
              </div>
            </Column>
            <Column lg={8} md={4} sm={4}>
              <Select
                id="priorityId"
                name="priority"
                labelText={intl.formatMessage({ id: "workplan.priority.list" })}
                value={orderFormValues.sampleOrderItems.priority}
                onChange={handlePriority}
                required
              >
                {priorities.map((priority, index) => {
                  return (
                    <SelectItem
                      key={index}
                      text={priority.label}
                      value={priority.value}
                    />
                  );
                })}
              </Select>
            </Column>
            <Column lg={16} md={8} sm={3}>
              {" "}
              &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp;{" "}
            </Column>
            <Column lg={8} md={4} sm={4}>
              <CustomDatePicker
                id={"order_requestDate"}
                labelText={intl.formatMessage({ id: "sample.requestDate" })}
                autofillDate={true}
                value={
                  orderFormValues.sampleOrderItems.requestDate
                    ? orderFormValues.sampleOrderItems.requestDate
                    : configurationProperties.currentDateAsText
                }
                disallowFutureDate={true}
                onChange={(date) => handleDatePickerChange("requestDate", date)}
              />
            </Column>
            <Column lg={8} md={4} sm={4}>
              <CustomDatePicker
                id={"order_receivedDate"}
                labelText={intl.formatMessage({ id: "sample.receivedDate" })}
                autofillDate={true}
                value={
                  orderFormValues.sampleOrderItems.receivedDateForDisplay
                    ? orderFormValues.sampleOrderItems.receivedDateForDisplay
                    : configurationProperties.currentDateAsText
                }
                disallowFutureDate={true}
                onChange={(date) =>
                  handleDatePickerChange("receivedDate", date)
                }
              />
            </Column>
            <Column lg={16} md={8} sm={3}>
              {" "}
              &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp;{" "}
            </Column>
            <Column lg={8} md={4} sm={4}>
              <TimePicker
                id="order_receivedTime"
                labelText={intl.formatMessage({ id: "order.reception.time" })}
                onChange={handleReceivedTime}
                value={
                  orderFormValues.sampleOrderItems.receivedTime
                    ? orderFormValues.sampleOrderItems.receivedTime
                    : configurationProperties.currentTimeAsText
                }
              />
            </Column>
            <Column lg={8} md={4} sm={4}>
              <CustomDatePicker
                id={"order_nextVisitDate"}
                labelText={intl.formatMessage({
                  id: "sample.entry.nextVisit.date",
                })}
                value={orderFormValues.sampleOrderItems.nextVisitDate}
                autofillDate={false}
                disallowPastDate={true}
                onChange={(date) =>
                  handleDatePickerChange("nextVisitDate", date)
                }
              />
            </Column>
            <Column lg={16} md={8} sm={3}>
              {" "}
              &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp;{" "}
            </Column>
            <Column lg={8} md={4} sm={4}>
              <AutoComplete
                name="siteName"
                id="siteName"
                allowFreeText={
                  !(
                    configurationProperties.restrictFreeTextRefSiteEntry ===
                    "true"
                  )
                }
                value={
                  orderFormValues.sampleOrderItems.referringSiteId != ""
                    ? orderFormValues.sampleOrderItems.referringSiteId
                    : orderFormValues.sampleOrderItems.referringSiteName
                }
                onChange={handleSiteName}
                onSelect={handleAutoCompleteSiteName}
                label={
                  <>
                    <FormattedMessage id="order.search.site.name" />{" "}
                    <span className="requiredlabel">*</span>
                  </>
                }
                style={{ width: "!important 100%" }}
                suggestions={siteNames.length > 0 ? siteNames : []}
                required
              />
              {/* )} */}
            </Column>
            <Column lg={8} md={4} sm={4}>
              <Select
                id="requesterDepartmentId"
                name="requesterDepartmentId"
                labelText={intl.formatMessage({ id: "order.department.label" })}
                onChange={handleRequesterDept}
                required
                value={
                  orderFormValues.sampleOrderItems.referringSiteDepartmentId
                }
              >
                <SelectItem value="" text="" />
                {departments.map((department, index) => (
                  <SelectItem
                    key={index}
                    text={department.value}
                    value={department.id}
                  />
                ))}
              </Select>
            </Column>
            <Column lg={16} md={8} sm={3}>
              {" "}
              &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp;{" "}
            </Column>
            <Column lg={8} md={4} sm={4}>
              <AutoComplete
                name="requesterId"
                id="requesterId"
                allowFreeText={
                  !(
                    configurationProperties.restrictFreeTextProviderEntry ===
                    "true"
                  )
                }
                onSelect={handleProviderSelectOptions}
                onChange={clearProviderId}
                label={
                  <>
                    <FormattedMessage id="order.search.requester.label" />{" "}
                    <span className="requiredlabel">*</span>
                  </>
                }
                style={{ width: "!important 100%" }}
                invalidText={
                  <FormattedMessage id="order.invalid.requester.name.label" />
                }
                suggestions={providers.length > 0 ? providers : []}
                required
              />
            </Column>
            <Column lg={8} md={4} sm={4}>
              <TextInput
                name="provisionalDiagnosis"
                placeholder={intl.formatMessage({
                  id: "input.placeholder.provisionalClinicalDiagnosis",
                })}
                onChange={handleProvisionalClinicalDiagnosisChange}
                value={
                  orderFormValues.sampleOrderItems.provisionalClinicalDiagnosis
                }
                labelText={intl.formatMessage({
                  id: "order.requester.provisionalDiagnosis.label",
                })}
                id="provisionalDiagnosisId"
              />
            </Column>
            {/* <Column lg={8} md={4} sm={4}>
              {" "}
            </Column> */}
            <Column lg={16} md={4} sm={3}>
              {" "}
              &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp;{" "}
            </Column>
            <Column lg={8} md={4} sm={4}>
              <TextInput
                name="requesterFirstName"
                placeholder={intl.formatMessage({
                  id: "input.placeholder.requesterFirstName",
                })}
                labelText={
                  <>
                    <FormattedMessage id="order.requester.firstName.label" />
                    <span className="requiredlabel">*</span>
                  </>
                }
                disabled={
                  configurationProperties.restrictFreeTextProviderEntry ===
                  "true"
                }
                onChange={handleRequesterFirstName}
                onClick={() =>
                  handleChange("sampleOrderItems.providerFirstName")
                }
                value={orderFormValues.sampleOrderItems.providerFirstName}
                invalid={
                  changed["sampleOrderItems.providerFirstName"] &&
                  error("sampleOrderItems.providerFirstName")
                    ? true
                    : false
                }
                invalidText={error("sampleOrderItems.providerFirstName")}
                id="requesterFirstName"
              />
            </Column>

            <Column lg={8} md={4} sm={4}>
              <TextInput
                name="requesterLastName"
                placeholder={intl.formatMessage({
                  id: "input.placeholder.requesterLastName",
                })}
                labelText={
                  <>
                    <FormattedMessage id="order.requester.lastName.label" />
                    <span className="requiredlabel">*</span>
                  </>
                }
                disabled={
                  configurationProperties.restrictFreeTextProviderEntry ===
                  "true"
                }
                value={orderFormValues.sampleOrderItems.providerLastName}
                onClick={() =>
                  handleChange("sampleOrderItems.providerLastName")
                }
                onChange={handleRequesterLastName}
                id="requesterLastName"
                invalid={
                  changed["sampleOrderItems.providerLastName"] &&
                  error("sampleOrderItems.providerLastName")
                    ? true
                    : false
                }
                invalidText={error("sampleOrderItems.providerLastName")}
              />
            </Column>
            <Column lg={16} md={8} sm={3}>
              {" "}
              &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp;{" "}
            </Column>
            <Column lg={8} sm={4}>
              <TextInput
                name="providerWorkPhone"
                placeholder={intl.formatMessage({
                  id: "input.placeholder.providerWorkPhone",
                })}
                disabled={
                  configurationProperties.restrictFreeTextProviderEntry ===
                  "true"
                }
                onChange={handleRequesterWorkPhone}
                value={orderFormValues.sampleOrderItems.providerWorkPhone}
                onMouseLeave={handlePhoneNoValidation}
                labelText={intl.formatMessage({
                  id: "order.requester.phone.label",
                })}
                id="providerWorkPhoneId"
              />
            </Column>

            <Column lg={8} md={4} sm={4}>
              <TextInput
                name="providerFax"
                placeholder={intl.formatMessage({
                  id: "input.placeholder.providerFax",
                })}
                labelText={intl.formatMessage({
                  id: "order.requester.fax.label",
                })}
                disabled={
                  configurationProperties.restrictFreeTextProviderEntry ===
                  "true"
                }
                onChange={handleRequesterFax}
                value={orderFormValues.sampleOrderItems.providerFax}
                id="providerFaxId"
              />
            </Column>
            <Column lg={16} md={8} sm={3}>
              {" "}
              &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp;{" "}
            </Column>
            <Column lg={8} md={4} sm={4}>
              <TextInput
                name="providerEmail"
                placeholder={intl.formatMessage({
                  id: "input.placeholder.providerEmail",
                })}
                labelText={intl.formatMessage({
                  id: "order.requester.email.label",
                })}
                disabled={
                  configurationProperties.restrictFreeTextProviderEntry ===
                  "true"
                }
                onChange={handleRequesterEmail}
                value={orderFormValues.sampleOrderItems.providerEmail}
                id="providerEmailId"
                invalid={error("sampleOrderItems.providerEmail") ? true : false}
                invalidText={intl.formatMessage({
                  id: "error.invalid.email",
                })}
              />
            </Column>

            <Column lg={8} md={4} sm={4}>
              <Select
                id="paymentOptionSelectionId"
                name="paymentOptionSelections"
                value={orderFormValues.sampleOrderItems.paymentOptionSelection}
                labelText={intl.formatMessage({
                  id: "order.payment.status.label",
                })}
                onChange={handlePaymentStatus}
                required
              >
                <SelectItem value="" text="" />
                {paymentOptions &&
                  paymentOptions.map((option) => {
                    return (
                      <SelectItem
                        key={option.id}
                        value={option.id}
                        text={option.value}
                      />
                    );
                  })}
              </Select>
            </Column>
            <Column lg={16} md={8} sm={3}>
              {" "}
              &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp;{" "}
            </Column>
            <Column lg={8} md={4} sm={4}>
              <Select
                id="testLocationCodeId"
                name="testLocationCode"
                value={orderFormValues.sampleOrderItems.testLocationCode}
                labelText={
                  <FormattedMessage id="order.sampling.performed.label" />
                }
                onChange={(e) => handleSamplingPerformed(e)}
                required
              >
                <SelectItem value="" text="" />
                {samplingPerformed.map((option) => {
                  return (
                    <SelectItem
                      key={option.id}
                      value={option.id}
                      text={option.value}
                    />
                  );
                })}
              </Select>
            </Column>
            <Column lg={8} md={4} sm={4}>
              <TextInput
                name="testLocationCodeOther"
                labelText={intl.formatMessage({ id: "order.if.other.label" })}
                onChange={handleOtherLocationCode}
                value={orderFormValues.sampleOrderItems.otherLocationCode}
                disabled={!otherSamplingVisible}
                id="testLocationCodeOtherId"
              />
            </Column>
            <Column lg={16} md={8} sm={3}>
              {" "}
              &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp;{" "}
            </Column>
            <Column lg={8} md={4} sm={4}>
              <Checkbox
                labelText={
                  <FormattedMessage id="order.remember.site.and.requester.label" />
                }
                id="rememberSiteAndRequester"
                onChange={handleRememberCheckBox}
              />
            </Column>
          </Grid>
        </div>
        <div className="orderLegendBody">
          <h3>
            <FormattedMessage id="order.result.reporting.heading" />
          </h3>
          {samples.map((sample, index) => {
            if (sample.tests.length > 0) {
              return (
                <div key={index}>
                  <h4>
                    {" "}
                    <FormattedMessage id="label.button.sample" /> {index + 1}
                  </h4>
                  <OrderResultReporting
                    selectedTests={sample.tests}
                    reportingNotifications={reportingNotifications}
                  />
                </div>
              );
            }
          })}
        </div>
      </Stack>
    </>
  );
};

export default AddOrder;
