import React, { useContext, useState, useEffect, useRef } from "react";
import { FormattedMessage, injectIntl, useIntl } from "react-intl";
import {
  Form,
  TextInput,
  Heading,
  Button,
  Loading,
  Grid,
  Column,
  Section,
  Checkbox,
  UnorderedList,
  ListItem,
} from "@carbon/react";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../utils/Utils.js";
import { NotificationContext } from "../../layout/Layout.js";
import {
  AlertDialog,
  NotificationKinds,
} from "../../common/CustomNotification.js";
import PageBreadCrumb from "../../common/PageBreadCrumb.js";
import { Field, Formik } from "formik";
import BarcodeConfigurationFormValues from "../../formModel/innitialValues/BarcodeConfigurationFormValues.js";

let breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "breadcrums.admin.managment", link: "/MasterListsPage" },
  {
    label: "sidenav.label.admin.barcodeconfiguration",
    link: "/MasterListsPage/barcodeConfiguration",
  },
];
function BarcodeConfiguration() {
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  const intl = useIntl();

  const [barcodeFromValues, setBarcodeFormValues] = useState(
    BarcodeConfigurationFormValues,
  );

  const componentMounted = useRef(false);
  const [saveButton, setSaveButton] = useState(true);

  const [loading, setLoading] = useState(true);
  const [prePrintDontUseAltAccession, setPrePrintDontUseAltAccession] =
    useState(barcodeFromValues.prePrintDontUseAltAccession);

  const handlePreFormValues = (res) => {
    if (!res) {
      setLoading(true);
    } else {
      setBarcodeFormValues(res);
      setPrePrintDontUseAltAccession(res.prePrintDontUseAltAccession);
      setLoading(false);
    }
  };

  function handleDefaultOrderLabelsValue(e) {
    setBarcodeFormValues({
      ...barcodeFromValues,
      numDefaultOrderLabels: parseFloat(e.target.value),
    });
    setSaveButton(false);
  }

  function handleDefaultSpecimenLabelsValue(e) {
    setBarcodeFormValues({
      ...barcodeFromValues,
      numDefaultSpecimenLabels: parseFloat(e.target.value),
    });
    setSaveButton(false);
  }

  function handleDefaultSlideLabelsValue(e) {
    setBarcodeFormValues({
      ...barcodeFromValues,
      numDefaultSlideLabels: parseFloat(e.target.value),
    });
    setSaveButton(false);
  }

  function handleDefaultBlockLabelsValue(e) {
    setBarcodeFormValues({
      ...barcodeFromValues,
      numDefaultBlockLabels: parseFloat(e.target.value),
    });
    setSaveButton(false);
  }

  function handleDefaultFreezerLabelsValue(e) {
    setBarcodeFormValues({
      ...barcodeFromValues,
      numDefaultFreezerLabels: parseFloat(e.target.value),
    });
    setSaveButton(false);
  }

  function handleMaxOrderLabelsValue(e) {
    setBarcodeFormValues({
      ...barcodeFromValues,
      numMaxOrderLabels: parseFloat(e.target.value),
    });
    setSaveButton(false);
  }

  function handleMaxSpecimenLabelsValue(e) {
    setBarcodeFormValues({
      ...barcodeFromValues,
      numMaxSpecimenLabels: parseFloat(e.target.value),
    });
    setSaveButton(false);
  }

  function handleMaxSlideLabelsValue(e) {
    setBarcodeFormValues({
      ...barcodeFromValues,
      numMaxSlideLabels: parseFloat(e.target.value),
    });
    setSaveButton(false);
  }

  function handleMaxBlockLabelsValue(e) {
    setBarcodeFormValues({
      ...barcodeFromValues,
      numMaxBlockLabels: parseFloat(e.target.value),
    });
    setSaveButton(false);
  }

  function handleMaxFreezerLabelsValue(e) {
    setBarcodeFormValues({
      ...barcodeFromValues,
      numMaxFreezerLabels: parseFloat(e.target.value),
    });
    setSaveButton(false);
  }

  function handleHeightOrderLabelsValue(e) {
    setBarcodeFormValues({
      ...barcodeFromValues,
      heightOrderLabels: parseFloat(e.target.value),
    });
    setSaveButton(false);
  }

  function handleWidthOrderLabelsValue(e) {
    setBarcodeFormValues({
      ...barcodeFromValues,
      widthOrderLabels: parseFloat(e.target.value),
    });
    setSaveButton(false);
  }

  function handleHeightSpecimenLabelsValue(e) {
    setBarcodeFormValues({
      ...barcodeFromValues,
      heightSpecimenLabels: parseFloat(e.target.value),
    });
    setSaveButton(false);
  }

  function handleWidthSpecimenLabelsValue(e) {
    setBarcodeFormValues({
      ...barcodeFromValues,
      widthSpecimenLabels: parseFloat(e.target.value),
    });
    setSaveButton(false);
  }

  function handleHeightBlockLabelsValue(e) {
    setBarcodeFormValues({
      ...barcodeFromValues,
      heightBlockLabels: parseFloat(e.target.value),
    });
    setSaveButton(false);
  }

  function handleWidthBlockLabelsValue(e) {
    setBarcodeFormValues({
      ...barcodeFromValues,
      widthBlockLabels: parseFloat(e.target.value),
    });
    setSaveButton(false);
  }

  function handleHeightSlideLabelsValue(e) {
    setBarcodeFormValues({
      ...barcodeFromValues,
      heightSlideLabels: parseFloat(e.target.value),
    });
    setSaveButton(false);
  }

  function handleWidthSlideLabelsValue(e) {
    setSaveButton(false);
    setBarcodeFormValues({
      ...barcodeFromValues,
      widthSlideLabels: parseFloat(e.target.value),
    });
  }

  function handleHeightFreezerLabelsValue(e) {
    setBarcodeFormValues({
      ...barcodeFromValues,
      heightFreezerLabels: parseFloat(e.target.value),
    });
    setSaveButton(false);
  }

  function handleWidthFreezerLabelsValue(e) {
    setSaveButton(false);
    setBarcodeFormValues({
      ...barcodeFromValues,
      widthFreezerLabels: parseFloat(e.target.value),
    });
  }

  function handleSitePrefixPrePrintedValue(e) {
    setBarcodeFormValues({
      ...barcodeFromValues,
      prePrintAltAccessionPrefix: e.target.value,
    });
    //setSaveButton(false);
  }

  useEffect(() => {
    componentMounted.current = true;
    getFromOpenElisServer(`/rest/BarcodeConfiguration`, handlePreFormValues);
    return () => {
      componentMounted.current = false;
    };
  }, []);

  function submitPost(e) {
    postToOpenElisServerJsonResponse(
      `/rest/BarcodeConfiguration`,
      JSON.stringify(e),
      (data) => {},
    );
    setLoading(false);
  }

  function handleModify(event) {
    event.preventDefault();
    setLoading(true);
    submitPost(barcodeFromValues);
  }

  if (loading)
    return (
      <>
        <Loading />
      </>
    );

  return (
    <>
      {notificationVisible === true ? <AlertDialog /> : ""}
      <div className="adminPageContent">
        <PageBreadCrumb breadcrumbs={breadcrumbs} />
        <Grid fullWidth={true}>
          <Column lg={16}>
            <Section>
              <Heading>
                <FormattedMessage id="barcodeconfiguration.browse.title" />
              </Heading>
            </Section>
          </Column>
        </Grid>
        <br />
        <div className="orderLegendBody">
          <Grid fullWidth={true} className="gridBoundary">
            <Column lg={16} md={8} sm={4}>
              <Formik
                initialValues={barcodeFromValues}
                enableReinitialize={true}
                // // validationSchema={}
                // validateOnChange={false}
                // validateOnBlur={true}
                // onSubmit
                // onChange
              >
                {({
                  values,
                  errors,
                  touched,
                  // setFieldValue,
                  // handleChange,
                  // handleBlur,
                  // handleSubmit,
                }) => (
                  <Form
                  // onSubmit={handleSubmit}
                  // onChange={setSaveButton(false)}
                  // onBlur={handleBlur}
                  >
                    <Section>
                      <h4>
                        <FormattedMessage id="siteInfo.section.number" />
                      </h4>
                    </Section>
                    <hr />
                    <h5>
                      <FormattedMessage id="siteInfo.title.default.barcode" />
                    </h5>
                    <br />
                    <FormattedMessage id="siteInfo.description.default.barcode" />
                    <br />
                    <br />
                    <Grid fullWidth={true}>
                      <Column lg={8} md={8} sm={4}>
                        <Field name="order">
                          {({ field }) => (
                            <TextInput
                              id={field.name}
                              className="default"
                              type="number"
                              labelText={intl.formatMessage({
                                id: "siteInfo.title.default.barcode.order",
                              })}
                              invalid={errors.order && touched.order}
                              invalidText={errors.order}
                              value={values.numDefaultOrderLabels}
                              onChange={(e) => handleDefaultOrderLabelsValue(e)}
                              min={0}
                            />
                          )}
                        </Field>
                      </Column>
                      <Column lg={8} md={8} sm={4}>
                        <Field name="specimen">
                          {({ field }) => (
                            <TextInput
                              id={field.name}
                              className="default"
                              type="number"
                              labelText={intl.formatMessage({
                                id: "siteInfo.title.default.barcode.specimen",
                              })}
                              value={values.numDefaultSpecimenLabels}
                              onChange={(e) =>
                                handleDefaultSpecimenLabelsValue(e)
                              }
                              min={0}
                            />
                          )}
                        </Field>
                      </Column>
                      <Column lg={8} md={8} sm={4}>
                        <Field name="slide">
                          {({ field }) => (
                            <TextInput
                              id={field.name}
                              className="default"
                              type="number"
                              labelText={intl.formatMessage({
                                id: "siteInfo.title.default.barcode.slide",
                              })}
                              invalid={errors.slide && touched.slide}
                              invalidText={errors.slide}
                              value={values.numDefaultSlideLabels}
                              onChange={(e) => handleDefaultSlideLabelsValue(e)}
                              min={0}
                            />
                          )}
                        </Field>
                      </Column>
                      <Column lg={8} md={8} sm={4}>
                        <Field name="specimen">
                          {({ field }) => (
                            <TextInput
                              id={field.name}
                              className="default"
                              type="number"
                              labelText={intl.formatMessage({
                                id: "siteInfo.title.default.barcode.block",
                              })}
                              value={values.numDefaultBlockLabels}
                              onChange={(e) => handleDefaultBlockLabelsValue(e)}
                              min={0}
                            />
                          )}
                        </Field>
                      </Column>
                      <Column lg={8} md={8} sm={4}>
                        <Field name="order">
                          {({ field }) => (
                            <TextInput
                              id={field.name}
                              className="default"
                              type="number"
                              labelText={intl.formatMessage({
                                id: "siteInfo.title.default.barcode.freezer",
                              })}
                              invalid={errors.freezer && touched.freezer}
                              invalidText={errors.freezer}
                              value={values.numDefaultFreezerLabels}
                              onChange={(e) =>
                                handleDefaultFreezerLabelsValue(e)
                              }
                              min={0}
                            />
                          )}
                        </Field>
                      </Column>
                    </Grid>
                    <br />
                    <h5>
                      <FormattedMessage id="siteInfo.title.max.barcode" />
                    </h5>
                    <br />
                    <FormattedMessage id="siteInfo.description.max.barcode" />
                    <br />
                    <br />
                    <Grid fullWidth={true}>
                      <Column lg={8} md={8} sm={4}>
                        <Field name="maxOrder">
                          {({ field }) => (
                            <TextInput
                              id={field.name}
                              className="default"
                              type="number"
                              labelText={intl.formatMessage({
                                id: "siteInfo.title.default.barcode.order",
                              })}
                              value={values.numMaxOrderLabels}
                              onChange={(e) => handleMaxOrderLabelsValue(e)}
                              min={0}
                            />
                          )}
                        </Field>
                      </Column>
                      <Column lg={8} md={8} sm={4}>
                        <Field name="maxSpecimen">
                          {({ field }) => (
                            <TextInput
                              id={field.name}
                              className="default"
                              type="number"
                              labelText={intl.formatMessage({
                                id: "siteInfo.title.default.barcode.specimen",
                              })}
                              value={values.numMaxSpecimenLabels}
                              onChange={(e) => handleMaxSpecimenLabelsValue(e)}
                              min={0}
                            />
                          )}
                        </Field>
                      </Column>
                      <Column lg={8} md={8} sm={4}>
                        <Field name="maxSlide">
                          {({ field }) => (
                            <TextInput
                              id={field.name}
                              className="default"
                              type="number"
                              labelText={intl.formatMessage({
                                id: "siteInfo.title.default.barcode.slide",
                              })}
                              value={values.numMaxSlideLabels}
                              onChange={(e) => handleMaxSlideLabelsValue(e)}
                              min={0}
                            />
                          )}
                        </Field>
                      </Column>
                      <Column lg={8} md={8} sm={4}>
                        <Field name="maxBlock">
                          {({ field }) => (
                            <TextInput
                              id={field.name}
                              className="default"
                              type="number"
                              labelText={intl.formatMessage({
                                id: "siteInfo.title.default.barcode.block",
                              })}
                              value={values.numMaxBlockLabels}
                              onChange={(e) => handleMaxBlockLabelsValue(e)}
                              min={0}
                            />
                          )}
                        </Field>
                      </Column>
                      <Column lg={8} md={8} sm={4}>
                        <Field name="maxFreezer">
                          {({ field }) => (
                            <TextInput
                              id={field.name}
                              className="default"
                              type="number"
                              labelText={intl.formatMessage({
                                id: "siteInfo.title.default.barcode.freezer",
                              })}
                              value={values.numMaxFreezerLabels}
                              onChange={(e) => handleMaxFreezerLabelsValue(e)}
                              min={0}
                            />
                          )}
                        </Field>
                      </Column>
                    </Grid>
                    <hr />
                    <Section>
                      <h4>
                        <FormattedMessage id="siteInfo.section.elements" />
                      </h4>
                      <br />
                      <FormattedMessage id="siteInfo.description.elements" />
                      <br />
                      <br />
                      <Grid fullWidth={true}>
                        <Column lg={16} md={8} sm={4}>
                          <h4>
                            <FormattedMessage id="siteInfo.elements.mandatory" />
                          </h4>
                          <br />
                          <Grid fullWidth={true}>
                            <Column lg={8} md={8} sm={4}>
                              <div>
                                <FormattedMessage id="siteInfo.title.default.barcode.order" />
                                <br />
                                <br />
                                <UnorderedList nested={true}>
                                  <ListItem>
                                    <FormattedMessage id="barcode.label.info.labnumber" />
                                  </ListItem>
                                  <ListItem>
                                    <FormattedMessage id="barcode.label.info.patientdobfull" />
                                  </ListItem>
                                  <ListItem>
                                    <FormattedMessage id="barcode.label.info.patientid" />
                                  </ListItem>
                                  <ListItem>
                                    <FormattedMessage id="barcode.label.info.patientname" />
                                  </ListItem>
                                  <ListItem>
                                    <FormattedMessage id="datasubmission.siteid" />
                                  </ListItem>
                                </UnorderedList>
                              </div>
                            </Column>
                            <Column lg={8} md={8} sm={4}>
                              <div>
                                <FormattedMessage id="siteInfo.title.default.barcode.specimen" />
                                <br />
                                <br />
                                <UnorderedList nested={true}>
                                  <ListItem>
                                    <FormattedMessage id="barcode.label.info.labnumber" />
                                  </ListItem>
                                  <ListItem>
                                    <FormattedMessage id="barcode.label.info.patientdobfull" />
                                  </ListItem>
                                  <ListItem>
                                    <FormattedMessage id="barcode.label.info.patientid" />
                                  </ListItem>
                                  <ListItem>
                                    <FormattedMessage id="barcode.label.info.patientname" />
                                  </ListItem>
                                </UnorderedList>
                              </div>
                            </Column>
                          </Grid>
                        </Column>
                      </Grid>
                      <br />
                      <Grid fullWidth={true}>
                        <Column lg={16} md={8} sm={4}>
                          <h4>
                            <FormattedMessage id="siteInfo.elements.optional" />
                          </h4>
                          <br />
                          <Grid fullWidth={true}>
                            <Column lg={8} md={8} sm={4}>
                              <div>
                                <FormattedMessage id="siteInfo.title.default.barcode.order" />
                                <br />
                                <br />
                                <FormattedMessage id="siteInfo.title.default.barcode.order.none" />
                              </div>
                            </Column>
                            <Column lg={8} md={8} sm={4}>
                              <div>
                                <FormattedMessage id="siteInfo.title.default.barcode.specimen" />
                                <br />
                                <Checkbox
                                  id="collectionDateCheck"
                                  checked={values.collectionDateCheck}
                                  onChange={(e) => {
                                    const isChecked = e.target.checked;
                                    setBarcodeFormValues({
                                      ...barcodeFromValues,
                                      collectionDateCheck: isChecked,
                                    });
                                    setSaveButton(false);
                                  }}
                                  labelText={intl.formatMessage({
                                    id: "barcode.label.info.collectiondatetime",
                                  })}
                                />
                                <Checkbox
                                  id="collectedBy"
                                  checked={values.collectedByCheck}
                                  onChange={(e) => {
                                    const isChecked = e.target.checked;
                                    setBarcodeFormValues({
                                      ...barcodeFromValues,
                                      collectedByCheck: isChecked,
                                    });
                                    setSaveButton(false);
                                  }}
                                  labelText={intl.formatMessage({
                                    id: "barcode.label.info.collectedBy",
                                  })}
                                />
                                <Checkbox
                                  id="tests"
                                  checked={values.testsCheck}
                                  onChange={(e) => {
                                    const isChecked = e.target.checked;
                                    setBarcodeFormValues({
                                      ...barcodeFromValues,
                                      testsCheck: isChecked,
                                    });
                                    setSaveButton(false);
                                  }}
                                  labelText={intl.formatMessage({
                                    id: "barcode.label.info.tests",
                                  })}
                                />
                                <Checkbox
                                  id="patientsexfull"
                                  checked={values.patientSexCheck}
                                  onChange={(e) => {
                                    const isChecked = e.target.checked;
                                    setBarcodeFormValues({
                                      ...barcodeFromValues,
                                      patientSexCheck: isChecked,
                                    });
                                    setSaveButton(false);
                                  }}
                                  labelText={intl.formatMessage({
                                    id: "barcode.label.info.patientsexfull",
                                  })}
                                />
                              </div>
                            </Column>
                          </Grid>
                        </Column>
                      </Grid>
                    </Section>
                    <hr />
                    <Section>
                      <h4>
                        <FormattedMessage id="siteInfo.section.altAccession" />
                      </h4>
                      <br />
                      <Checkbox
                        id="checkBox"
                        checked={prePrintDontUseAltAccession}
                        onChange={(e) => {
                          const isChecked = e.target.checked;
                          setPrePrintDontUseAltAccession(isChecked);
                          setBarcodeFormValues({
                            ...barcodeFromValues,
                            prePrintDontUseAltAccession: isChecked,
                          });
                          setSaveButton(false);
                        }}
                        labelText={intl.formatMessage({
                          id: "labno.alt.prefix.use",
                        })}
                      />
                      <br />
                      <Grid fullWidth={true}>
                        <Column lg={8} md={4} sm={4}>
                          <FormattedMessage id="labno.alt.prefix.instruction" />
                        </Column>
                        <Column lg={8} md={4} sm={4}>
                          <Field name="sitePrefix">
                            {({ field }) => (
                              <TextInput
                                // name="lable-prefix"
                                className="default"
                                type="text"
                                id={field.name}
                                labelText=""
                                size="md"
                                disabled={prePrintDontUseAltAccession}
                                value={values.prePrintAltAccessionPrefix}
                                onChange={(e) => {
                                  const value = e.target.value;
                                  if (
                                    /^[a-zA-Z0-9]*$/.test(value) &&
                                    value.length <= 4
                                  ) {
                                    handleSitePrefixPrePrintedValue(e);
                                  } else {
                                    setNotificationVisible(true);
                                    addNotification({
                                      kind: NotificationKinds.error,
                                      title: intl.formatMessage({
                                        id: "notification.title",
                                      }),
                                      message:
                                        "Input should be alphanumeric and have a maximum length of 4 characters.",
                                    });
                                  }
                                  if (value.length < 4) {
                                    setSaveButton(true);
                                  } else {
                                    setSaveButton(false);
                                  }
                                }}
                              />
                            )}
                          </Field>
                        </Column>
                      </Grid>
                      <br />
                      <FormattedMessage id="labno.alt.prefix.note" />
                      <br />
                    </Section>
                    <hr />
                    <Section>
                      <h4>
                        <FormattedMessage id="siteInfo.section.size" />
                      </h4>
                      <br />
                      <FormattedMessage id="siteInfo.description.dimensions" />
                      <br />
                      <br />
                      <Grid fullWidth={true}>
                        <Column lg={8} md={4} sm={2}>
                          <FormattedMessage id="siteInfo.title.default.barcode.order" />
                          <br />
                          <br />
                          <Field name="height-order">
                            {({ field }) => (
                              <TextInput
                                id={field.name}
                                className="default"
                                type="number"
                                labelText={intl.formatMessage({
                                  id: "siteInfo.title.default.barcode.height",
                                })}
                                helperText={intl.formatMessage({
                                  id: "barcode.label.helper.text",
                                })}
                                value={values.heightOrderLabels}
                                onChange={(e) =>
                                  handleHeightOrderLabelsValue(e)
                                }
                                min={0}
                              />
                            )}
                          </Field>

                          <br />
                          <Field name="width-order">
                            {({ field }) => (
                              <TextInput
                                id={field.name}
                                className="default"
                                type="number"
                                labelText={intl.formatMessage({
                                  id: "siteInfo.title.default.barcode.width",
                                })}
                                helperText={intl.formatMessage({
                                  id: "barcode.label.helper.text",
                                })}
                                value={values.widthOrderLabels}
                                onChange={(e) => handleWidthOrderLabelsValue(e)}
                                min={0}
                              />
                            )}
                          </Field>
                        </Column>
                        <Column lg={8} md={4} sm={2}>
                          <FormattedMessage id="siteInfo.title.default.barcode.specimen" />
                          <br />
                          <br />
                          <Field name="height-specimen">
                            {({ field }) => (
                              <TextInput
                                id={field.name}
                                className="default"
                                type="number"
                                labelText={intl.formatMessage({
                                  id: "siteInfo.title.default.barcode.height",
                                })}
                                helperText={intl.formatMessage({
                                  id: "barcode.label.helper.text",
                                })}
                                value={values.heightSpecimenLabels}
                                onChange={(e) =>
                                  handleHeightSpecimenLabelsValue(e)
                                }
                                min={0}
                              />
                            )}
                          </Field>

                          <br />
                          <Field name="width-specimen">
                            {({ field }) => (
                              <TextInput
                                id={field.name}
                                className="default"
                                type="number"
                                labelText={intl.formatMessage({
                                  id: "siteInfo.title.default.barcode.width",
                                })}
                                helperText={intl.formatMessage({
                                  id: "barcode.label.helper.text",
                                })}
                                value={values.widthSpecimenLabels}
                                onChange={(e) =>
                                  handleWidthSpecimenLabelsValue(e)
                                }
                                min={0}
                              />
                            )}
                          </Field>
                        </Column>
                      </Grid>
                      <br />
                      <Grid fullWidth={true}>
                        <Column lg={8} md={4} sm={2}>
                          <FormattedMessage id="siteInfo.title.default.barcode.block" />
                          <br />
                          <br />
                          <Field name="height-block">
                            {({ field }) => (
                              <TextInput
                                id={field.name}
                                className="default"
                                type="number"
                                labelText={intl.formatMessage({
                                  id: "siteInfo.title.default.barcode.height",
                                })}
                                helperText={intl.formatMessage({
                                  id: "barcode.label.helper.text",
                                })}
                                value={values.heightBlockLabels}
                                onChange={(e) =>
                                  handleHeightBlockLabelsValue(e)
                                }
                                min={0}
                              />
                            )}
                          </Field>
                          <br />
                          <Field name="width-block">
                            {({ field }) => (
                              <TextInput
                                id={field.name}
                                className="default"
                                type="number"
                                labelText={intl.formatMessage({
                                  id: "siteInfo.title.default.barcode.width",
                                })}
                                helperText={intl.formatMessage({
                                  id: "barcode.label.helper.text",
                                })}
                                value={values.widthBlockLabels}
                                onChange={(e) => handleWidthBlockLabelsValue(e)}
                                min={0}
                              />
                            )}
                          </Field>
                        </Column>
                        <Column lg={8} md={4} sm={2}>
                          <FormattedMessage id="siteInfo.title.default.barcode.slide" />
                          <br />
                          <br />
                          <Field name="height-slide">
                            {({ field }) => (
                              <TextInput
                                id={field.name}
                                className="default"
                                type="number"
                                labelText={intl.formatMessage({
                                  id: "siteInfo.title.default.barcode.height",
                                })}
                                helperText={intl.formatMessage({
                                  id: "barcode.label.helper.text",
                                })}
                                value={values.heightSlideLabels}
                                onChange={(e) =>
                                  handleHeightSlideLabelsValue(e)
                                }
                                min={0}
                              />
                            )}
                          </Field>

                          <br />
                          <Field name="width-slide">
                            {({ field }) => (
                              <TextInput
                                id={field.name}
                                className="default"
                                type="number"
                                labelText={intl.formatMessage({
                                  id: "siteInfo.title.default.barcode.width",
                                })}
                                helperText={intl.formatMessage({
                                  id: "barcode.label.helper.text",
                                })}
                                value={values.widthSlideLabels}
                                onChange={(e) => handleWidthSlideLabelsValue(e)}
                                min={0}
                              />
                            )}
                          </Field>
                        </Column>
                        <Column lg={8} md={4} sm={2}>
                          <FormattedMessage id="siteInfo.title.default.barcode.freezer" />
                          <br />
                          <br />
                          <Field name="height-freezer">
                            {({ field }) => (
                              <TextInput
                                id={field.name}
                                className="default"
                                type="number"
                                labelText={intl.formatMessage({
                                  id: "siteInfo.title.default.barcode.height",
                                })}
                                helperText={intl.formatMessage({
                                  id: "barcode.label.helper.text",
                                })}
                                value={values.heightFreezerLabels}
                                onChange={(e) =>
                                  handleHeightFreezerLabelsValue(e)
                                }
                                min={0}
                              />
                            )}
                          </Field>

                          <br />
                          <Field name="width-freezer">
                            {({ field }) => (
                              <TextInput
                                id={field.name}
                                className="default"
                                type="number"
                                labelText={intl.formatMessage({
                                  id: "siteInfo.title.default.barcode.width",
                                })}
                                helperText={intl.formatMessage({
                                  id: "barcode.label.helper.text",
                                })}
                                value={values.widthFreezerLabels}
                                onChange={(e) =>
                                  handleWidthFreezerLabelsValue(e)
                                }
                                min={0}
                              />
                            )}
                          </Field>
                        </Column>
                      </Grid>
                    </Section>
                    <hr />
                  </Form>
                )}
              </Formik>
            </Column>
            <Section>
              <Form
                onSubmit={handleModify}
                style={{
                  display: "flex",
                  justifyContent: "space-between",
                  alignItems: "center",
                }}
              >
                <Column
                  lg={16}
                  md={8}
                  sm={4}
                  style={{ display: "flex", gap: "10px" }}
                >
                  <Button
                    id="saveButton"
                    disabled={saveButton}
                    onClick={() => {
                      setNotificationVisible(true);
                      addNotification({
                        kind: NotificationKinds.success,
                        title: intl.formatMessage({
                          id: "notification.title",
                        }),
                        message: intl.formatMessage({
                          id: "barcode.notification.save",
                        }),
                      });
                    }}
                    type="submit"
                  >
                    <FormattedMessage id="label.button.save" />
                  </Button>{" "}
                  <Button
                    onClick={() => window.location.reload()}
                    kind="tertiary"
                    type="button"
                  >
                    <FormattedMessage id="label.button.cancel" />
                  </Button>
                </Column>
              </Form>
            </Section>
          </Grid>
        </div>
      </div>
    </>
  );
}

export default injectIntl(BarcodeConfiguration);
