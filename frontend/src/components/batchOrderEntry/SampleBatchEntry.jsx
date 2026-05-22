import React, { useContext, useState, useEffect, useRef } from "react";
import { useHistory } from "react-router-dom";
import { ConfigurationContext, NotificationContext } from "../layout/Layout";
import {
  Button,
  Grid,
  Column,
  Section,
  Loading,
  Heading,
  TextInput,
  Tag,
  Link,
  Accordion,
  AccordionItem,
  Row,
  FlexGrid,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { AlertDialog, NotificationKinds } from "../common/CustomNotification";
import "../Style.css";
import PageBreadCrumb from "../common/PageBreadCrumb";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../utils/Utils";
import CustomLabNumberInput from "../common/CustomLabNumberInput";
import PatientInfo from "../addOrder/PatientInfo";
import { createOrderEntryValidationSchema } from "../formModel/validationSchema/OrderEntryValidationSchema";
import LabelsSection from "../barcodeWorkflow/LabelsSection";
import PostSavePrintDialog from "../barcodeWorkflow/PostSavePrintDialog";

const normalizeQuantity = (value) => {
  const parsed = Number.parseInt(value, 10);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : 1;
};

const upsertSampleAttribute = (sampleTag, attributeName, value) => {
  const attributePattern = new RegExp(`${attributeName}='[^']*'`);
  if (attributePattern.test(sampleTag)) {
    return sampleTag.replace(attributePattern, `${attributeName}='${value}'`);
  }
  return sampleTag.replace("<sample ", `<sample ${attributeName}='${value}' `);
};

const updateSampleXmlLabelQuantities = (
  sampleXml,
  numOrderLabels,
  numSpecimenLabels,
) => {
  if (!sampleXml || !sampleXml.includes("<sample")) {
    return sampleXml;
  }

  return sampleXml.replace(/<sample\s+[^>]*\/>/, (sampleTag) => {
    let nextTag = upsertSampleAttribute(
      sampleTag,
      "numOrderLabels",
      numOrderLabels,
    );
    nextTag = upsertSampleAttribute(
      nextTag,
      "numSpecimenLabels",
      numSpecimenLabels,
    );
    return nextTag;
  });
};

const extractSampleXmlLabelQuantity = (sampleXml, attributeName) => {
  if (!sampleXml) {
    return 1;
  }
  const match = sampleXml.match(new RegExp(`${attributeName}='([^']+)'`));
  return normalizeQuantity(match?.[1]);
};

const SampleBatchEntry = (props) => {
  const { orderFormValues, setOrderFormValues } = props;
  // const [notificationVisible, setNotificationVisible] = useState(false);
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);
  const { configurationProperties } = useContext(ConfigurationContext);
  const intl = useIntl();
  const componentMounted = useRef(false);
  const history = useHistory();
  const [siteNames, setSiteNames] = useState([]);
  const [departments, setDepartments] = useState([]);
  const [loading, setLoading] = useState(false);
  const [showSampleComponent, setShowSampleComponent] = useState(false);
  const [generatedLabNos, setGeneratedLabNos] = useState([]);
  const [saveResponse, setSaveResponse] = useState(null);
  const [errors, setErrors] = useState([]);
  const [generateSaveButtonDisabled, setGenerateSaveButtonDisabled] =
    useState(false);
  const [buttonDisabled, setButtonDisabled] = useState(true);
  const [labNoGenerated, setLabNoGenerated] = useState(false);

  let breadcrumbs = [{ label: "home.label", link: "/" }];

  useEffect(() => {
    componentMounted.current = true;
    getFromOpenElisServer("/rest/SamplePatientEntry", getSampleEntryPreform);
    setLabNoGenerated(true);
    window.scrollTo(0, 0);
    return () => {
      componentMounted.current = false;
    };
  }, []);

  useEffect(() => {
    createOrderEntryValidationSchema(configurationProperties)
      .validate(orderFormValues, { abortEarly: false })
      .then((validData) => {
        setErrors([]);
        console.debug("Valid Data:", validData);
      })
      .catch((errors) => {
        setErrors(errors);
        console.error("Validation Errors:", errors.errors);
      });
  }, [configurationProperties, orderFormValues]);

  useEffect(() => {
    getFromOpenElisServer(
      "/rest/departments-for-site?refferingSiteId=" +
        (orderFormValues.sampleOrderItems.referringSiteId || ""),
      loadDepartments,
    );
  }, [orderFormValues.referringSiteId]);

  const getSampleEntryPreform = (response) => {
    if (componentMounted.current) {
      setSiteNames(response.sampleOrderItems.referringSiteList);
    }
  };

  const loadDepartments = (data) => {
    setDepartments(data);
  };

  function handleLabNo(e, rawVal) {
    setOrderFormValues({
      ...orderFormValues,
      sampleOrderItems: {
        ...orderFormValues.sampleOrderItems,
        labNo: rawVal ? rawVal : e?.target?.value,
      },
    });
    // setNotificationVisible(false);
  }

  const showAlertMessage = (msg, kind) => {
    setNotificationVisible(true);
    addNotification({
      kind: kind,
      title: intl.formatMessage({ id: "notification.title" }),
      message: msg,
    });
  };

  const handleKeyPress = (event) => {
    if (event.key === "Enter") {
      handleLabNoGeneration(event);
    }
  };
  function fetchGeneratedAccessionNo(res) {
    if (res.status) {
      setOrderFormValues({
        ...orderFormValues,
        sampleOrderItems: {
          ...orderFormValues.sampleOrderItems,
          labNo: res.body,
        },
      });
      if (orderFormValues.method === "On Demand") {
        setGeneratedLabNos((prevLabNos) => [...prevLabNos, res.body]);
      }
      setNotificationVisible(false);
    }
  }

  function post() {
    if ("years" in orderFormValues.patientProperties) {
      delete orderFormValues.patientProperties.years;
    }
    if ("months" in orderFormValues.patientProperties) {
      delete orderFormValues.patientProperties.months;
    }
    if ("days" in orderFormValues.patientProperties) {
      delete orderFormValues.patientProperties.days;
    }
    if (!orderFormValues.currentDate) {
      orderFormValues.currentDate = "";
    }
    const normalizedOrderLabels = extractSampleXmlLabelQuantity(
      orderFormValues.sampleXML,
      "numOrderLabels",
    );
    const normalizedSpecimenLabels = extractSampleXmlLabelQuantity(
      orderFormValues.sampleXML,
      "numSpecimenLabels",
    );
    const payload = {
      ...orderFormValues,
      sampleXML: updateSampleXmlLabelQuantities(
        orderFormValues.sampleXML,
        normalizedOrderLabels,
        normalizedSpecimenLabels,
      ),
    };
    const body = JSON.stringify(payload);
    postToOpenElisServerJsonResponse(
      "/rest/SamplePatientEntryBatch",
      body,
      printLabelSets,
    );
  }
  useEffect(() => {
    if (
      componentMounted.current &&
      orderFormValues.sampleOrderItems.labNo != "" &&
      labNoGenerated &&
      (orderFormValues.method === "On Demand" ? !buttonDisabled : true)
    ) {
      post();
    } else {
      componentMounted.current = true;
    }
  }, [
    orderFormValues.sampleOrderItems.labNo,
    orderFormValues.method,
    buttonDisabled,
  ]);

  const handleLabNoGeneration = (e) => {
    if (e) {
      e.preventDefault();
    }
    getFromOpenElisServer(
      "/rest/SampleEntryGenerateScanProvider",
      fetchGeneratedAccessionNo,
    );
  };

  const printLabelSets = (res) => {
    const responseStatus = res?.statusCode ?? res?.status ?? 200;
    if (!res?.error && responseStatus < 400) {
      showAlertMessage(
        <FormattedMessage id="save.order.success.msg" />,
        NotificationKinds.success,
      );
      setSaveResponse(res);
    }
  };

  const handleLabelsSectionChange = (labelsModel) => {
    const numOrderLabels =
      labelsModel?.orderRow?.quantities?.order != null
        ? normalizeQuantity(labelsModel.orderRow.quantities.order)
        : 1;
    const numSpecimenLabels =
      labelsModel?.sampleRows?.[0]?.quantities?.specimen != null
        ? normalizeQuantity(labelsModel.sampleRows[0].quantities.specimen)
        : 1;

    setOrderFormValues((current) => ({
      ...current,
      sampleXML: updateSampleXmlLabelQuantities(
        current.sampleXML,
        numOrderLabels,
        numSpecimenLabels,
      ),
    }));
  };

  const buildFallbackPrintableLabels = (accessionNumber) => {
    const orderLabels = extractSampleXmlLabelQuantity(
      orderFormValues.sampleXML,
      "numOrderLabels",
    );
    const specimenLabels = extractSampleXmlLabelQuantity(
      orderFormValues.sampleXML,
      "numSpecimenLabels",
    );
    const encodedAccession = encodeURIComponent(accessionNumber || "");
    return [
      {
        labelType: "order",
        quantity: orderLabels,
        printUrl: `/LabelMakerServlet?labNo=${encodedAccession}&type=order&quantity=${orderLabels}`,
      },
      {
        labelType: "specimen",
        quantity: specimenLabels,
        printUrl: `/LabelMakerServlet?labNo=${encodedAccession}&type=specimen&quantity=${specimenLabels}`,
      },
    ];
  };

  const elementError = (path) => {
    if (errors?.errors?.length > 0) {
      let error = errors.inner?.find((e) => e.path === path);
      if (error) {
        return error.message;
      } else {
        return null;
      }
    }
  };

  return (
    <>
      {notificationVisible && <AlertDialog />}
      {loading && <Loading description="Loading Dasboard..." />}
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      {!showSampleComponent && (
        <>
          <Grid fullWidth={true}>
            <Column lg={16} md={8} sm={4}>
              <Section>
                <Heading>
                  <FormattedMessage
                    id="order.entry.setup.batch"
                    defaultMessage="Batch Order Entry"
                  />
                </Heading>
              </Section>
            </Column>
          </Grid>
          <Grid fullWidth={true}>
            <Column lg={16} md={8} sm={4}>
              <div className="orderLegendBody">
                <Grid fullWidth={true}>
                  <Column lg={16}>
                    <h3>
                      <FormattedMessage id="order.legend.commonFields" />
                    </h3>
                  </Column>

                  <Column lg={4}>
                    <TextInput
                      labelText={<FormattedMessage id="sample.currentDate" />}
                      value={orderFormValues.currentDate}
                      readOnly
                      id="current-date"
                    />
                  </Column>
                  <Column lg={4}>
                    <TextInput
                      labelText={<FormattedMessage id="order.current.time" />}
                      value={orderFormValues.currentTime}
                      readOnly
                      id="current-time"
                    />
                  </Column>

                  <Column lg={4}>
                    <TextInput
                      labelText={<FormattedMessage id="sample.receivedDate" />}
                      value={
                        orderFormValues.sampleOrderItems.receivedDateForDisplay
                      }
                      readOnly
                      id="recieved-date"
                    />
                  </Column>
                  <Column lg={4}>
                    <TextInput
                      labelText={<FormattedMessage id="order.received.time" />}
                      value={orderFormValues.sampleOrderItems.receivedTime}
                      readOnly
                      id="recieved-time"
                    />
                  </Column>
                  <Column lg={16}>
                    <br></br>
                  </Column>
                  {orderFormValues.sampleTypeSelect && (
                    <>
                      <Column lg={4}>
                        {" "}
                        <FormattedMessage id="sample.type" />
                      </Column>
                      <Column lg={8}>
                        <Tag key="1" type="cyan" size="lg">
                          {orderFormValues.sampleTypeSelect}
                        </Tag>
                      </Column>
                      <Column lg={4}></Column>
                    </>
                  )}
                  <Column lg={4}>
                    <FormattedMessage id="eorder.test.name" />
                  </Column>
                  <Column lg={12}>
                    {orderFormValues.tests.map((test) => (
                      <Tag key={test.id} type="cyan">
                        {test.value}
                      </Tag>
                    ))}
                  </Column>
                  <Column lg={4}>
                    <FormattedMessage id="order.legend.facility" />
                  </Column>
                  <Column lg={8}>
                    <Tag type="cyan" size="lg">
                      {orderFormValues.sampleOrderItems.referringSiteName}
                    </Tag>
                  </Column>
                  <Column lg={4}></Column>
                  <Column lg={4}>
                    <FormattedMessage id="order.department.label" />
                  </Column>
                  <Column lg={8}>
                    <Tag type="cyan" size="lg">
                      {
                        orderFormValues.sampleOrderItems
                          .referringSiteDepartmentName
                      }
                    </Tag>
                  </Column>
                </Grid>
              </div>
              {orderFormValues.PatientInfoCheck && (
                <PatientInfo
                  orderFormValues={orderFormValues}
                  setOrderFormValues={setOrderFormValues}
                  error={elementError}
                />
              )}
              {orderFormValues.method === "On Demand" && (
                <div className="orderLegendBody">
                  <Column lg={16}>
                    <h3>
                      <FormattedMessage id="order.generate.barcode" />
                    </h3>
                  </Column>
                  <Column lg={16}>
                    {" "}
                    <br />
                  </Column>

                  <Column lg={8}>
                    <CustomLabNumberInput
                      name="labNo"
                      placeholder={intl.formatMessage({
                        id: "input.placeholder.labNo",
                      })}
                      value={orderFormValues.sampleOrderItems.labNo}
                      onChange={handleLabNo}
                      onKeyPress={handleKeyPress}
                      labelText={
                        <>
                          <FormattedMessage id="sample.label.labnumber" />{" "}
                          <span className="requiredlabel">*</span>
                        </>
                      }
                      id="labNo"
                    />
                    <Link
                      href="#"
                      onClick={(e) => {
                        handleLabNoGeneration(e);
                        setButtonDisabled(false);
                        setGenerateSaveButtonDisabled(true);
                      }}
                      disabled={generateSaveButtonDisabled}
                      data-testid="generate-barcode-link-BatchOrderEntry"
                    >
                      <p>
                        <FormattedMessage id="order.generate.barcode" />
                      </p>
                    </Link>
                  </Column>
                  <Column lg={8}></Column>
                  <Column lg={16}>
                    {" "}
                    <br />
                  </Column>

                  <Column lg={16}>
                    <Accordion>
                      <AccordionItem
                        title={intl.formatMessage({
                          id: "order.generate.barcode.history",
                        })}
                      >
                        {generatedLabNos.map((labNo, index) => (
                          <h6 key={index}> {labNo}</h6>
                        ))}
                      </AccordionItem>
                    </Accordion>
                  </Column>
                  <Column lg={16}>
                    <br />
                  </Column>
                  <Column lg={16}>
                    <Button
                      onClick={() => {
                        setButtonDisabled(true);
                        setGenerateSaveButtonDisabled(false);
                        setOrderFormValues({
                          ...orderFormValues,
                          sampleOrderItems: {
                            ...orderFormValues.sampleOrderItems,
                            labNo: "",
                          },
                        });
                      }}
                      data-testid="next-button-BatchOrderEntry"
                      disabled={buttonDisabled}
                    >
                      <FormattedMessage id="nextLabel.action.button" />
                    </Button>
                  </Column>
                </div>
              )}
              {orderFormValues.method === "Pre-Printed" && (
                <div className="orderLegendBody">
                  <Column lg={16}>
                    <h3>
                      <FormattedMessage id="accession.entry" />
                    </h3>
                  </Column>
                  <Column lg={16}>
                    <CustomLabNumberInput
                      name="labNo"
                      placeholder={intl.formatMessage({
                        id: "input.placeholder.labNo",
                      })}
                      value={orderFormValues.sampleOrderItems.labNo}
                      onChange={handleLabNo}
                      onKeyPress={handleKeyPress}
                      labelText={
                        <>
                          <FormattedMessage id="sample.label.labnumber" />{" "}
                          <span className="requiredlabel">*</span>
                        </>
                      }
                      id="labNo"
                    />
                    <FormattedMessage id="label.order.scan.text" />{" "}
                    <Link href="#" onClick={(e) => handleLabNoGeneration(e)}>
                      <FormattedMessage id="sample.label.labnumber.generate" />
                    </Link>
                  </Column>

                  <br />
                  <Column lg={16}>
                    <Accordion>
                      <AccordionItem
                        title={intl.formatMessage({
                          id: "order.generate.barcode.history",
                        })}
                      >
                        {generatedLabNos.map((labNo, index) => (
                          <h6 key={index}> {labNo}</h6>
                        ))}
                      </AccordionItem>
                    </Accordion>
                  </Column>
                  <Column lg={16}>
                    <br />
                  </Column>
                  <Column lg={16}>
                    <Button
                      onClick={() => {
                        setLabNoGenerated(true);
                        setGenerateSaveButtonDisabled(false);
                        setGeneratedLabNos((prevLabNos) => [
                          ...prevLabNos,
                          orderFormValues.sampleOrderItems.labNo,
                        ]);
                      }}
                      disabled={generateSaveButtonDisabled}
                      data-testid="generate-barcode-btn-BatchOrderEntry"
                    >
                      <FormattedMessage id="column.name.save" />
                    </Button>
                  </Column>
                </div>
              )}

              <div className="orderLegendBody">
                <Grid>
                  <Column lg={16} md={8} sm={4}>
                    <h4>
                      <FormattedMessage id="barcode.labels.section.title" />
                    </h4>
                  </Column>
                  <Column lg={16} md={8} sm={4}>
                    <LabelsSection
                      orderQuantity={extractSampleXmlLabelQuantity(
                        orderFormValues.sampleXML,
                        "numOrderLabels",
                      )}
                      specimenQuantities={[
                        extractSampleXmlLabelQuantity(
                          orderFormValues.sampleXML,
                          "numSpecimenLabels",
                        ),
                      ]}
                      onChange={handleLabelsSectionChange}
                      orderLabelText={intl.formatMessage({
                        id: "barcode.labels.order.row",
                      })}
                      specimenLabelFormatter={(sampleNumber) =>
                        intl.formatMessage(
                          { id: "barcode.labels.sample.row" },
                          { sampleNumber },
                        )
                      }
                      runningTotalLabel={intl.formatMessage({
                        id: "barcode.labels.running.total",
                      })}
                    />
                  </Column>
                </Grid>
              </div>

              {saveResponse && (
                <div className="orderLegendBody">
                  <Grid>
                    <Column lg={16} md={8} sm={4}>
                      <h4>
                        <FormattedMessage id="barcode.print.dialog.title" />
                      </h4>
                    </Column>
                    <Column lg={16} md={8} sm={4}>
                      <PostSavePrintDialog
                        accessionNumber={
                          saveResponse?.postSavePrintDialog?.accessionNumber ||
                          orderFormValues.sampleOrderItems.labNo
                        }
                        printableLabelTypes={
                          saveResponse?.postSavePrintDialog
                            ?.printableLabelTypes &&
                          saveResponse.postSavePrintDialog.printableLabelTypes
                            .length > 0
                            ? saveResponse.postSavePrintDialog
                                .printableLabelTypes
                            : buildFallbackPrintableLabels(
                                orderFormValues.sampleOrderItems.labNo,
                              )
                        }
                      />
                    </Column>
                  </Grid>
                </div>
              )}
              <Grid>
                <Button
                  data-cy="finishButton"
                  onClick={() =>
                    (window.location.href = "/SampleBatchEntrySetup")
                  }
                >
                  <FormattedMessage id="label.button.finish" />
                </Button>
              </Grid>
            </Column>
          </Grid>
        </>
      )}
    </>
  );
};

export default SampleBatchEntry;
