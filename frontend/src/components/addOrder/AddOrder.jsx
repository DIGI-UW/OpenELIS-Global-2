import React, { useContext, useEffect, useMemo, useRef, useState } from "react";
import {
  Button,
  Checkbox,
  DataTable,
  FileUploaderDropContainer,
  InlineNotification,
  Link,
  Modal,
  Select,
  SelectItem,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableHeader,
  TableRow,
  TextArea,
  TextInput,
  Column,
  Grid,
} from "@carbon/react";
import { Download, TrashCan, View } from "@carbon/react/icons";
import config from "../../config.json";
import CustomLabNumberInput from "../common/CustomLabNumberInput";
import CustomDatePicker from "../common/CustomDatePicker";
import CustomTimePicker from "../common/CustomTimePicker";
import {
  deleteFromOpenElisServer,
  getFromOpenElisServer,
  postToOpenElisServerFormData,
  postToOpenElisServerJsonResponse,
} from "../utils/Utils";
import { NotificationContext } from "../layout/Layout";
import { priorities } from "../data/orderOptions";
import { NotificationKinds } from "../common/CustomNotification";
import AutoComplete from "../common/AutoComplete";
import OrderResultReporting from "./OrderResultReporting";
import LabelsSection from "../barcodeWorkflow/LabelsSection";
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
    stagedAttachments,
    setStagedAttachments,
  } = props;
  const [otherSamplingVisible, setOtherSamplingVisible] = useState(false);
  const [providers, setProviders] = useState([]);
  const [paymentOptions, setPaymentOptions] = useState([]);
  const [samplingPerformed, setSamplingPerformed] = useState([]);
  const [siteNames, setSiteNames] = useState([]);
  // Ref (not state) because the value gates a one-time init inside an effect
  // and is never read during render — using state would trigger an extra
  // render and a react-hooks/set-state-in-effect lint violation.
  const initializedRef = useRef(false);
  const [departments, setDepartments] = useState([]);
  const [attachmentError, setAttachmentError] = useState(null);
  const [savedAttachments, setSavedAttachments] = useState([]);
  const [attachmentToDelete, setAttachmentToDelete] = useState(null);
  // OGC-285 M5b: the POST /api/orderEntry/labelRequest aggregation response that
  // drives the order-level LabelsSection (API mode). Null until the first fetch
  // (or when no sample carries tests), in which case the section is not shown.
  const [labelRequest, setLabelRequest] = useState(null);

  const ATTACHMENT_MAX_FILES = 5;
  const ATTACHMENT_MAX_SIZE = 10 * 1024 * 1024;
  const ATTACHMENT_ALLOWED_EXTS = [
    ".pdf",
    ".jpg",
    ".jpeg",
    ".png",
    ".tif",
    ".tiff",
  ];

  const formatFileSize = (bytes) => {
    if (!bytes) return "0 B";
    const units = ["B", "KB", "MB", "GB"];
    const i = Math.floor(Math.log(bytes) / Math.log(1024));
    return parseFloat((bytes / Math.pow(1024, i)).toFixed(2)) + " " + units[i];
  };

  const handleAttachmentAdd = (addedFiles) => {
    setAttachmentError(null);
    if (!addedFiles || addedFiles.length === 0) return;
    const current = stagedAttachments || [];
    if (current.length + addedFiles.length > ATTACHMENT_MAX_FILES) {
      setAttachmentError(
        intl.formatMessage(
          { id: "order.attachment.error.maxFiles" },
          { maxFiles: ATTACHMENT_MAX_FILES },
        ),
      );
      return;
    }
    const accepted = [];
    for (const file of addedFiles) {
      const lowerName = file.name.toLowerCase();
      const ext = lowerName.includes(".")
        ? lowerName.substring(lowerName.lastIndexOf("."))
        : "";
      if (!ATTACHMENT_ALLOWED_EXTS.includes(ext)) {
        setAttachmentError(
          intl.formatMessage(
            { id: "order.attachment.error.type" },
            { types: ATTACHMENT_ALLOWED_EXTS.join(", ") },
          ),
        );
        return;
      }
      if (file.size > ATTACHMENT_MAX_SIZE) {
        setAttachmentError(
          intl.formatMessage(
            { id: "order.attachment.error.size" },
            { maxSize: "10MB" },
          ),
        );
        return;
      }
      accepted.push({
        id: `staged-${Date.now()}-${Math.random().toString(36).slice(2, 9)}`,
        file,
        fileName: file.name,
        fileType: file.type,
        fileSize: file.size,
      });
    }
    if (setStagedAttachments) {
      setStagedAttachments([...current, ...accepted]);
    }
  };

  const handleAttachmentRemove = (id) => {
    if (!setStagedAttachments) return;
    setStagedAttachments((stagedAttachments || []).filter((a) => a.id !== id));
  };

  const loadSavedAttachments = (accessionNumber) => {
    if (!accessionNumber) return;
    getFromOpenElisServer(
      "/rest/order/" + encodeURIComponent(accessionNumber) + "/attachments",
      (data) => {
        if (componentMounted.current && Array.isArray(data)) {
          setSavedAttachments(data);
        }
      },
    );
  };

  const validateAttachmentBatch = (addedFiles, currentCount) => {
    if (currentCount + addedFiles.length > ATTACHMENT_MAX_FILES) {
      return intl.formatMessage(
        { id: "order.attachment.error.maxFiles" },
        { maxFiles: ATTACHMENT_MAX_FILES },
      );
    }
    for (const file of addedFiles) {
      const lower = file.name.toLowerCase();
      const ext = lower.includes(".")
        ? lower.substring(lower.lastIndexOf("."))
        : "";
      if (!ATTACHMENT_ALLOWED_EXTS.includes(ext)) {
        return intl.formatMessage(
          { id: "order.attachment.error.type" },
          { types: ATTACHMENT_ALLOWED_EXTS.join(", ") },
        );
      }
      if (file.size > ATTACHMENT_MAX_SIZE) {
        return intl.formatMessage(
          { id: "order.attachment.error.size" },
          { maxSize: "10MB" },
        );
      }
    }
    return null;
  };

  const handleSavedAttachmentUpload = (addedFiles) => {
    setAttachmentError(null);
    if (!addedFiles || addedFiles.length === 0) return;
    const accessionNumber = orderFormValues?.sampleOrderItems?.labNo;
    if (!accessionNumber) return;
    const validationError = validateAttachmentBatch(
      addedFiles,
      savedAttachments.length,
    );
    if (validationError) {
      setAttachmentError(validationError);
      return;
    }
    const formData = new FormData();
    addedFiles.forEach((file) => formData.append("files", file, file.name));
    postToOpenElisServerFormData(
      "/rest/order/" + encodeURIComponent(accessionNumber) + "/attachments",
      formData,
      (status) => {
        if (status >= 200 && status < 300) {
          loadSavedAttachments(accessionNumber);
        } else {
          setAttachmentError(
            intl.formatMessage({ id: "order.attachment.upload.failed" }),
          );
        }
      },
    );
  };

  const handleSavedAttachmentDownload = (attachmentId) => {
    window.open(
      config.serverBaseUrl +
        "/rest/order/attachments/" +
        encodeURIComponent(attachmentId) +
        "/download",
      "_blank",
    );
  };

  const handleSavedAttachmentView = (attachmentId) => {
    window.open(
      config.serverBaseUrl +
        "/rest/order/attachments/" +
        encodeURIComponent(attachmentId) +
        "/view",
      "_blank",
      "noopener,noreferrer",
    );
  };

  const confirmSavedAttachmentDelete = () => {
    if (!attachmentToDelete) return;
    const id = attachmentToDelete.id;
    const accessionNumber = orderFormValues?.sampleOrderItems?.labNo;
    setAttachmentToDelete(null);
    deleteFromOpenElisServer(
      "/rest/order/attachments/" + encodeURIComponent(id),
      (status) => {
        if (status >= 200 && status < 300) {
          loadSavedAttachments(accessionNumber);
        } else {
          setAttachmentError(
            intl.formatMessage({ id: "order.attachment.delete.failed" }),
          );
        }
      },
    );
  };

  useEffect(() => {
    if (isModifyOrder && orderFormValues?.sampleOrderItems?.labNo) {
      loadSavedAttachments(orderFormValues.sampleOrderItems.labNo);
    }
  }, [isModifyOrder, orderFormValues?.sampleOrderItems?.labNo]);

  useEffect(() => {
    componentMounted.current = true;
    getFromOpenElisServer("/rest/SamplePatientEntry", (response) => {
      if (componentMounted.current) {
        setSiteNames(response.sampleOrderItems.referringSiteList);
        setPaymentOptions(response.sampleOrderItems.paymentOptions);
        setSamplingPerformed(response.sampleOrderItems.testLocationCodeList);
        setProviders(response.sampleOrderItems.providersList);
      }
    });
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
      case "consentRecordedAt":
        obj = { ...orderFormValues.sampleOrderItems, consentRecordedAt: date };
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

  function handleReceivedTime(time) {
    setOrderFormValues({
      ...orderFormValues,
      sampleOrderItems: {
        ...orderFormValues.sampleOrderItems,
        receivedTime: time,
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

  const handleLabNoValidationOnChange = (value) => {
    if (value) {
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
    const checked = e.currentTarget.checked;
    setOrderFormValues({
      ...orderFormValues,
      rememberSiteAndRequester: checked,
    });
  }

  function handleConsentCheckBox(e) {
    const checked = e.currentTarget.checked;
    setOrderFormValues({
      ...orderFormValues,
      sampleOrderItems: {
        ...orderFormValues.sampleOrderItems,
        consentGiven: checked,
        // Clear all consent fields if unchecking consent
        consentFormReference: checked
          ? orderFormValues.sampleOrderItems.consentFormReference
          : "",
        consentRecordedAt: checked
          ? orderFormValues.sampleOrderItems.consentRecordedAt
          : "",
        consentRecordedBy: checked
          ? orderFormValues.sampleOrderItems.consentRecordedBy
          : "",
      },
    });
  }

  function handleConsentReferenceChange(e) {
    setOrderFormValues({
      ...orderFormValues,
      sampleOrderItems: {
        ...orderFormValues.sampleOrderItems,
        consentFormReference: e.target.value,
      },
    });
  }

  function handleConsentRecordedByChange(e) {
    setOrderFormValues({
      ...orderFormValues,
      sampleOrderItems: {
        ...orderFormValues.sampleOrderItems,
        consentRecordedBy: e.target.value,
      },
    });
  }

  useEffect(() => {
    if (!initializedRef.current) {
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
      initializedRef.current = true;
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

  // OGC-285 M5b — the samples that actually reach the backend, in the SAME order
  // the save's sampleXML is built (Index.jsx iterates `samples` and emits a
  // <sample> only when tests.length > 0). The backend parses those in document
  // order into getSampleItemsTests(); the i-th entry is keyed sample_id_local =
  // String(i). Keying off this filtered list (NOT the raw `samples` index) is
  // what makes the per-sample label rows correlate to the right SampleItem.
  const orderLabelSamples = useMemo(
    () => (samples || []).filter((s) => s.tests && s.tests.length > 0),
    [samples],
  );

  // Stable signature of the selected tests + sample types — re-fetch the
  // aggregation only when these change (not on every unrelated AddOrder render).
  const orderLabelSignature = useMemo(
    () =>
      JSON.stringify(
        orderLabelSamples.map((s) => [
          s.sampleTypeId,
          (s.tests || []).map((t) => t.id),
        ]),
      ),
    [orderLabelSamples],
  );

  // Fetch POST /api/orderEntry/labelRequest with all selected test ids + the
  // filtered samples (each carrying its positional sample_id_local). Renders via
  // LabelsSection's API mode. Clears when no sample carries tests.
  useEffect(() => {
    if (orderLabelSamples.length === 0) {
      setLabelRequest(null);
      return;
    }
    const testIds = [
      ...new Set(
        orderLabelSamples.flatMap((s) =>
          (s.tests || []).map((t) => Number(t.id)),
        ),
      ),
    ];
    const requestBody = {
      test_ids: testIds,
      samples: orderLabelSamples.map((s, index) => ({
        sample_id_local: String(index),
        sample_type: s.sampleTypeId,
      })),
    };
    postToOpenElisServerJsonResponse(
      "/api/orderEntry/labelRequest",
      JSON.stringify(requestBody),
      (response) => {
        if (response && !response.error) {
          setLabelRequest(response);
        }
      },
    );
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [orderLabelSignature]);

  // Human-facing row label for the sample table: the sample type name, falling
  // back to "Sample N" (the filtered position the backend correlates by).
  const orderLabelSampleFormatter = (row) => {
    const sample = orderLabelSamples[Number(row.sampleIdLocal)];
    if (sample && sample.name) {
      return sample.name;
    }
    return intl.formatMessage(
      { id: "orderEntry.labels.sampleRow.header" },
      { number: row.sampleNumber, id: row.sampleIdLocal },
    );
  };

  // Capture the LabelsSection's chosen quantities (persistPayload, already shaped
  // as OrderLabelPersistRequest) and lift it onto orderFormValues so Index.jsx's
  // save POST body carries it as the top-level labelPersistRequest.
  const handleOrderLabelsChange = ({ persistPayload }) => {
    setOrderFormValues((prev) => ({
      ...prev,
      labelPersistRequest: persistPayload,
    }));
  };

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
              <CustomTimePicker
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
              <TextArea
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
                rows={3}
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

            {/* Remember Site and Requester + Informed Consent Row */}
            <Column lg={8} md={4} sm={4}>
              <Checkbox
                labelText={
                  <FormattedMessage id="order.remember.site.and.requester.label" />
                }
                id="rememberSiteAndRequester"
                onChange={handleRememberCheckBox}
              />
            </Column>
            <Column lg={8} md={4} sm={4}>
              <Checkbox
                labelText={
                  <FormattedMessage id="label.informedConsent.consentGiven" />
                }
                id="consentGiven"
                checked={orderFormValues.sampleOrderItems.consentGiven}
                onChange={handleConsentCheckBox}
              />
            </Column>

            {/* Conditional Consent Reference Number Field */}
            {orderFormValues.sampleOrderItems.consentGiven && (
              <>
                <Column lg={16} md={8} sm={3}>
                  {" "}
                  &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp;{" "}
                </Column>
                <Column lg={8} md={4} sm={4}>
                  {/* Empty column for alignment */}
                </Column>
                <Column lg={8} md={4} sm={4}>
                  <TextInput
                    name="consentFormReference"
                    labelText={intl.formatMessage({
                      id: "label.informedConsent.formReference",
                    })}
                    placeholder={intl.formatMessage({
                      id: "placeholder.informedConsent.formReference",
                    })}
                    value={
                      orderFormValues.sampleOrderItems.consentFormReference
                    }
                    onChange={handleConsentReferenceChange}
                    id="consentFormReferenceId"
                    maxLength={100}
                  />
                </Column>

                {/* Consent Recorded By Field */}
                <Column lg={16} md={8} sm={3}>
                  {" "}
                  &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp;{" "}
                </Column>
                <Column lg={8} md={4} sm={4}>
                  {/* Empty column for alignment */}
                </Column>
                <Column lg={8} md={4} sm={4}>
                  <TextInput
                    name="consentRecordedBy"
                    labelText={intl.formatMessage({
                      id: "label.informedConsent.recordedBy",
                    })}
                    placeholder={intl.formatMessage({
                      id: "placeholder.informedConsent.recordedBy",
                    })}
                    maxLength={255}
                    value={orderFormValues.sampleOrderItems.consentRecordedBy}
                    onChange={handleConsentRecordedByChange}
                    id="consentRecordedById"
                  />
                </Column>

                {/* Consent Recorded At Field */}
                <Column lg={16} md={8} sm={3}>
                  {" "}
                  &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp;{" "}
                </Column>
                <Column lg={8} md={4} sm={4}>
                  {/* Empty column for alignment */}
                </Column>
                <Column lg={8} md={4} sm={4}>
                  <CustomDatePicker
                    id="consentRecordedAtId"
                    labelText={intl.formatMessage({
                      id: "label.informedConsent.recordedAt",
                    })}
                    autofillDate={false}
                    value={orderFormValues.sampleOrderItems.consentRecordedAt}
                    disallowFutureDate={true}
                    onChange={(date) =>
                      handleDatePickerChange("consentRecordedAt", date)
                    }
                  />
                </Column>
              </>
            )}
          </Grid>
        </div>
        {!isModifyOrder && setStagedAttachments && (
          <div className="orderLegendBody">
            <h3>
              <FormattedMessage id="order.attachment.heading" />
            </h3>
            <Stack gap={3}>
              <p>
                <FormattedMessage
                  id="order.attachment.hint"
                  values={{ maxFiles: ATTACHMENT_MAX_FILES }}
                />
              </p>
              {attachmentError && (
                <InlineNotification
                  kind="error"
                  hideCloseButton={false}
                  title={intl.formatMessage({ id: "notification.title" })}
                  subtitle={attachmentError}
                  onCloseButtonClick={() => setAttachmentError(null)}
                />
              )}
              <FileUploaderDropContainer
                accept={ATTACHMENT_ALLOWED_EXTS}
                multiple
                disabled={
                  (stagedAttachments || []).length >= ATTACHMENT_MAX_FILES
                }
                labelText={intl.formatMessage({
                  id: "order.attachment.dropzone",
                })}
                onAddFiles={(_event, { addedFiles }) =>
                  handleAttachmentAdd(addedFiles)
                }
              />
              {(stagedAttachments || []).length > 0 && (
                <Stack gap={2}>
                  {stagedAttachments.map((a) => (
                    <Grid key={a.id} condensed style={{ alignItems: "center" }}>
                      <Column lg={10} md={5} sm={3}>
                        <span>{a.fileName}</span>
                      </Column>
                      <Column lg={4} md={2} sm={1}>
                        <span>{formatFileSize(a.fileSize)}</span>
                      </Column>
                      <Column lg={2} md={1} sm={4}>
                        <Button
                          kind="ghost"
                          size="sm"
                          hasIconOnly
                          renderIcon={TrashCan}
                          iconDescription={intl.formatMessage({
                            id: "label.button.remove",
                          })}
                          onClick={() => handleAttachmentRemove(a.id)}
                        />
                      </Column>
                    </Grid>
                  ))}
                </Stack>
              )}
            </Stack>
          </div>
        )}
        {isModifyOrder && orderFormValues?.sampleOrderItems?.labNo && (
          <div className="orderLegendBody">
            <h3>
              <FormattedMessage id="order.attachment.heading" />
            </h3>
            <Stack gap={3}>
              <p>
                <FormattedMessage
                  id="order.attachment.hint"
                  values={{ maxFiles: ATTACHMENT_MAX_FILES }}
                />
              </p>
              {attachmentError && (
                <InlineNotification
                  kind="error"
                  hideCloseButton={false}
                  title={intl.formatMessage({ id: "notification.title" })}
                  subtitle={attachmentError}
                  onCloseButtonClick={() => setAttachmentError(null)}
                />
              )}
              <FileUploaderDropContainer
                accept={ATTACHMENT_ALLOWED_EXTS}
                multiple
                disabled={savedAttachments.length >= ATTACHMENT_MAX_FILES}
                labelText={intl.formatMessage({
                  id: "order.attachment.dropzone",
                })}
                onAddFiles={(_event, { addedFiles }) =>
                  handleSavedAttachmentUpload(addedFiles)
                }
              />
              <DataTable
                rows={savedAttachments.map((a) => ({
                  id: String(a.id),
                  fileName: a.fileName,
                  fileType: a.fileType,
                  fileSizeBytes: a.fileSizeBytes,
                  uploadedAt: a.uploadedAt,
                }))}
                headers={[
                  {
                    key: "fileName",
                    header: intl.formatMessage({
                      id: "order.attachment.column.fileName",
                    }),
                  },
                  {
                    key: "fileType",
                    header: intl.formatMessage({
                      id: "order.attachment.column.fileType",
                    }),
                  },
                  {
                    key: "fileSizeBytes",
                    header: intl.formatMessage({
                      id: "order.attachment.column.fileSize",
                    }),
                  },
                  {
                    key: "uploadedAt",
                    header: intl.formatMessage({
                      id: "order.attachment.column.uploadedAt",
                    }),
                  },
                  {
                    key: "actions",
                    header: intl.formatMessage({
                      id: "order.attachment.column.actions",
                    }),
                  },
                ]}
              >
                {({ rows, headers, getTableProps, getHeaderProps }) => (
                  <TableContainer>
                    <Table {...getTableProps()}>
                      <TableHead>
                        <TableRow>
                          {headers.map((header) => (
                            <TableHeader
                              key={header.key}
                              {...getHeaderProps({ header })}
                            >
                              {header.header}
                            </TableHeader>
                          ))}
                        </TableRow>
                      </TableHead>
                      <TableBody>
                        {rows.length === 0 && (
                          <TableRow>
                            <TableCell colSpan={headers.length}>
                              <FormattedMessage id="order.attachment.empty" />
                            </TableCell>
                          </TableRow>
                        )}
                        {rows.map((row) => {
                          const cells = {};
                          row.cells.forEach((c) => {
                            cells[c.info.header] = c.value;
                          });
                          return (
                            <TableRow key={row.id}>
                              <TableCell>{cells.fileName}</TableCell>
                              <TableCell>{cells.fileType}</TableCell>
                              <TableCell>
                                {formatFileSize(cells.fileSizeBytes)}
                              </TableCell>
                              <TableCell>{cells.uploadedAt}</TableCell>
                              <TableCell>
                                <Button
                                  kind="ghost"
                                  size="sm"
                                  hasIconOnly
                                  renderIcon={View}
                                  iconDescription={intl.formatMessage({
                                    id: "order.attachment.action.view",
                                  })}
                                  onClick={() =>
                                    handleSavedAttachmentView(row.id)
                                  }
                                />
                                <Button
                                  kind="ghost"
                                  size="sm"
                                  hasIconOnly
                                  renderIcon={Download}
                                  iconDescription={intl.formatMessage({
                                    id: "order.attachment.action.download",
                                  })}
                                  onClick={() =>
                                    handleSavedAttachmentDownload(row.id)
                                  }
                                />
                                <Button
                                  kind="ghost"
                                  size="sm"
                                  hasIconOnly
                                  renderIcon={TrashCan}
                                  iconDescription={intl.formatMessage({
                                    id: "order.attachment.action.delete",
                                  })}
                                  onClick={() =>
                                    setAttachmentToDelete({
                                      id: row.id,
                                      fileName: cells.fileName,
                                    })
                                  }
                                />
                              </TableCell>
                            </TableRow>
                          );
                        })}
                      </TableBody>
                    </Table>
                  </TableContainer>
                )}
              </DataTable>
            </Stack>
          </div>
        )}
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
        {labelRequest && (
          <div className="orderLegendBody">
            <h3>
              <FormattedMessage id="orderEntry.labels.heading" />
            </h3>
            <LabelsSection
              labelRequest={labelRequest}
              onChange={handleOrderLabelsChange}
              sampleLabelFormatter={orderLabelSampleFormatter}
            />
          </div>
        )}
      </Stack>
      <Modal
        open={attachmentToDelete !== null}
        modalHeading={intl.formatMessage({
          id: "order.attachment.delete.confirm.title",
        })}
        primaryButtonText={intl.formatMessage({ id: "label.button.delete" })}
        secondaryButtonText={intl.formatMessage({ id: "label.button.cancel" })}
        onRequestClose={() => setAttachmentToDelete(null)}
        onRequestSubmit={confirmSavedAttachmentDelete}
        danger
      >
        <p>
          <FormattedMessage
            id="order.attachment.delete.confirm.body"
            values={{ fileName: attachmentToDelete?.fileName ?? "" }}
          />
        </p>
      </Modal>
    </>
  );
};

export default AddOrder;
