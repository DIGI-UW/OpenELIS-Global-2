import React, { useContext, useState, useEffect, useRef } from "react";
import {
  Heading,
  Button,
  Loading,
  Grid,
  Column,
  Section,
  DataTable,
  Table,
  TableHead,
  TableRow,
  TableBody,
  TableHeader,
  TableCell,
  TableSelectRow,
  TableSelectAll,
  TableContainer,
  Pagination,
  Search,
  Select,
  SelectItem,
  Stack,
  Tag,
  UnorderedList,
  ListItem,
  TextInput,
} from "@carbon/react";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
  postToOpenElisServerFormData,
  postToOpenElisServerFullResponse,
  postToOpenElisServerJsonResponse,
} from "../../utils/Utils";
import { NotificationContext } from "../../layout/Layout";
import {
  AlertDialog,
  NotificationKinds,
} from "../../common/CustomNotification";
import { FormattedMessage, injectIntl, useIntl } from "react-intl";
import PageBreadCrumb from "../../common/PageBreadCrumb";
import CustomCheckBox from "../../common/CustomCheckBox";
import ActionPaginationButtonType from "../../common/ActionPaginationButtonType";
import { Formik, Form } from "formik";
import * as Yup from "yup";

let breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "breadcrums.admin.managment", link: "/MasterListsPage" },
  {
    label: "master.lists.page.test.management",
    link: "/MasterListsPage/testManagementConfigMenu",
  },
  {
    label: "configuration.panel.manage",
    link: "/MasterListsPage/PanelManagement",
  },
  {
    label: "configuration.panel.create",
    link: "/MasterListsPage/PanelCreate",
  },
];

function PanelCreate() {
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  const intl = useIntl();
  const [isLoading, setIsLoading] = useState(true);
  const [bothFilled, setBothFilled] = useState(false);
  const [panelCreateList, setPanelCreateList] = useState({});
  const [selectedDomain, setSelectedDomain] = useState("ALL");

  const componentMounted = useRef(false);

  const domainLabel = (code) => {
    if (!code) return null;
    switch (code.toUpperCase()) {
      case "H":
        return intl.formatMessage({ id: "panel.domain.clinical" });
      case "V":
        return intl.formatMessage({ id: "panel.domain.vector" });
      case "L":
        return intl.formatMessage({ id: "panel.domain.eqa" });
      case "E":
        return intl.formatMessage({ id: "panel.domain.environmental" });
      default:
        return code.toUpperCase();
    }
  };
  const domainTagColor = (code) => {
    switch ((code || "").toUpperCase()) {
      case "V":
        return "cyan";
      case "L":
        return "purple";
      case "E":
        return "teal";
      default:
        return "gray";
    }
  };

  const handlePanelCreateList = (res) => {
    if (!res) {
      setIsLoading(true);
    } else {
      setPanelCreateList(res);
    }
  };

  const handlePanelCreateListCall = ({
    englishLangPost,
    frenchLangPost,
    selectedSampleTypeId,
    loincPost,
    panelDomainPost,
    organismGroupPost,
  }) => {
    postToOpenElisServerJsonResponse(
      "/rest/PanelCreate",
      JSON.stringify({
        panelEnglishName: englishLangPost,
        panelFrenchName: frenchLangPost,
        sampleTypeId: selectedSampleTypeId,
        panelLoinc: loincPost,
        panelDomain: panelDomainPost || "CLINICAL",
        vectorOrganismGroup:
          panelDomainPost === "VECTOR" ? organismGroupPost || "" : "",
      }),
      (res) => {
        handlePostPanelCreateListCallBack(res);
      },
    );
  };

  const handlePostPanelCreateListCallBack = (res) => {
    if (res) {
      if (res) {
        setIsLoading(false);
        addNotification({
          title: intl.formatMessage({
            id: "notification.title",
          }),
          message: intl.formatMessage({
            id: "notification.user.post.delete.success",
          }),
          kind: NotificationKinds.success,
        });
        setTimeout(() => {
          window.location.reload();
        }, 200);
        setNotificationVisible(true);
      }
    } else {
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "server.error.msg" }),
      });
      setNotificationVisible(true);
    }
  };

  const validationSchema = Yup.object({
    englishLangPost: Yup.string()
      .required("fill this field")
      .test(
        "duplicate-check",
        intl.formatMessage({ id: "input.error.same.panel.type" }),
        (value) => !validatePanelType(value),
      )
      .trim(),
    frenchLangPost: Yup.string()
      .required("fill this field")
      .test(
        "duplicate-check",
        intl.formatMessage({ id: "input.error.same.panel.type" }),
        (value) => !validatePanelType(value),
      )
      .trim(),
    loincPost: Yup.string()
      .required("fill this field")
      .trim()
      .matches(
        /^(?!-)(?:\d+-)*\d+$/,
        "Invalid format. Use digits separated by single dashes (e.g. 1-2-3)",
      ),
  });

  const allPanels = [
    ...(panelCreateList?.existingPanelList
      ? panelCreateList.existingPanelList.flatMap((epl) => epl?.panels || [])
      : []),
    ...(panelCreateList?.inactivePanelList
      ? panelCreateList.inactivePanelList.flatMap((epl) => epl?.panels || [])
      : []),
  ];

  const validatePanelType = (name) => {
    return allPanels.some((panel) => panel?.panelName === name);
  };

  useEffect(() => {
    componentMounted.current = true;
    setIsLoading(true);
    getFromOpenElisServer(`/rest/PanelCreate`, handlePanelCreateList);
    return () => {
      componentMounted.current = false;
      setIsLoading(false);
    };
  }, []);

  if (!isLoading) {
    return (
      <>
        <Loading />
      </>
    );
  }

  return (
    <>
      {notificationVisible === true ? <AlertDialog /> : ""}
      <div className="adminPageContent">
        <PageBreadCrumb breadcrumbs={breadcrumbs} />
        <div className="orderLegendBody">
          <Grid fullWidth={true}>
            <Column lg={16} md={8} sm={4}>
              <Section>
                <Heading>
                  <FormattedMessage id="banner.menu.patientEdit" />
                </Heading>
              </Section>
            </Column>
          </Grid>
          <br />
          <hr />
          <br />
          <Grid fullWidth={true}>
            <Column lg={16} md={8} sm={4}>
              <Section>
                <Section>
                  <Heading>
                    <FormattedMessage id="configuration.panel.create" />
                  </Heading>
                </Section>
              </Section>
            </Column>
          </Grid>
          <br />
          <hr />
          <br />
          <Grid fullWidth={true}>
            <Column lg={16} md={8} sm={4}>
              <Section>
                <Section>
                  <Section>
                    <Heading>
                      <FormattedMessage id="panel.new" />
                    </Heading>
                  </Section>
                </Section>
              </Section>
            </Column>
          </Grid>
          <br />
          <hr />
          <br />
          <Formik
            initialValues={{
              englishLangPost: "",
              frenchLangPost: "",
              selectedSampleTypeId: "",
              loincPost: "",
              panelDomainPost: "CLINICAL",
              organismGroupPost: "",
            }}
            validationSchema={validationSchema}
            onSubmit={(values, actions) => {
              if (bothFilled) {
                handlePanelCreateListCall(values);
              } else {
                setBothFilled(true);
                actions.setSubmitting(false);
              }
            }}
          >
            {({
              values,
              errors,
              touched,
              handleChange,
              handleBlur,
              handleSubmit,
              isSubmitting,
            }) => (
              <Form onSubmit={handleSubmit}>
                <Grid fullWidth={true}>
                  <Column lg={8} md={4} sm={4}>
                    <>
                      <FormattedMessage id="english.label" />
                      <span className="requiredlabel">*</span> :
                    </>
                  </Column>
                  <Column lg={8} md={4} sm={4}>
                    <TextInput
                      id={`eng`}
                      name="englishLangPost"
                      labelText=""
                      hideLabel
                      disabled={bothFilled}
                      value={values.englishLangPost}
                      onChange={handleChange}
                      onBlur={handleBlur}
                      required
                      invalid={
                        touched.englishLangPost && !!errors.englishLangPost
                      }
                      invalidText={
                        touched.englishLangPost && errors.englishLangPost
                      }
                    />
                  </Column>
                  <Column lg={8} md={4} sm={4}>
                    <>
                      <FormattedMessage id="french.label" />
                      <span className="requiredlabel">*</span> :
                    </>
                  </Column>
                  <Column lg={8} md={4} sm={4}>
                    <TextInput
                      id={`fr`}
                      name="frenchLangPost"
                      labelText=""
                      hideLabel
                      disabled={bothFilled}
                      value={values.frenchLangPost}
                      onChange={handleChange}
                      onBlur={handleBlur}
                      required
                      invalid={
                        touched.frenchLangPost && !!errors.frenchLangPost
                      }
                      invalidText={
                        touched.frenchLangPost && errors.frenchLangPost
                      }
                    />
                  </Column>
                  <Column lg={8} md={4} sm={4}>
                    <>
                      <FormattedMessage id="sample.type" />
                      <span className="requiredlabel">*</span> :
                    </>
                  </Column>
                  <Column lg={8} md={4} sm={4}>
                    <Select
                      id="smapleTypeSelect"
                      name="selectedSampleTypeId"
                      value={values.selectedSampleTypeId}
                      onChange={handleChange}
                      onBlur={handleBlur}
                      labelText={intl.formatMessage({
                        id: "sample.select.type",
                      })}
                      invalid={
                        touched.selectedSampleTypeId &&
                        !!errors.selectedSampleTypeId
                      }
                      invalidText={
                        touched.selectedSampleTypeId &&
                        errors.selectedSampleTypeId
                      }
                      required
                    >
                      <SelectItem value={"0"} text={"Select Sample Type"} />
                      {panelCreateList?.existingSampleTypeList?.map(
                        (st, index) => (
                          <SelectItem
                            key={index}
                            text={st.value}
                            value={st.id}
                          />
                        ),
                      )}
                    </Select>
                  </Column>
                  <Column lg={8} md={4} sm={4}>
                    <>
                      <FormattedMessage id="field.loinc" />
                      <span className="requiredlabel">*</span> :
                    </>
                  </Column>
                  <Column lg={8} md={4} sm={4}>
                    <TextInput
                      id={`loincPost`}
                      name="loincPost"
                      labelText=""
                      hideLabel
                      disabled={bothFilled}
                      value={values.loincPost}
                      onChange={handleChange}
                      onBlur={handleBlur}
                      required
                      //invalid={touched.loincPost && !!errors.loincPost}
                      //invalidText={touched.loincPost && errors.loincPost}
                    />
                  </Column>
                  <Column lg={8} md={4} sm={4}>
                    <>
                      <FormattedMessage
                        id="panel.editor.domain"
                        defaultMessage="Domain"
                      />{" "}
                      :
                    </>
                  </Column>
                  <Column lg={8} md={4} sm={4}>
                    <Select
                      id="panelDomainPost"
                      name="panelDomainPost"
                      value={values.panelDomainPost}
                      onChange={handleChange}
                      onBlur={handleBlur}
                      labelText={intl.formatMessage({
                        id: "panel.editor.domain",
                        defaultMessage: "Domain",
                      })}
                      disabled={bothFilled}
                    >
                      <SelectItem
                        value="CLINICAL"
                        text={intl.formatMessage({
                          id: "panel.domain.clinical",
                        })}
                      />
                      <SelectItem
                        value="VECTOR"
                        text={intl.formatMessage({ id: "panel.domain.vector" })}
                      />
                      <SelectItem
                        value="EQA"
                        text={intl.formatMessage({ id: "panel.domain.eqa" })}
                      />
                      <SelectItem
                        value="ENVIRONMENTAL"
                        text={intl.formatMessage({
                          id: "panel.domain.environmental",
                        })}
                      />
                    </Select>
                  </Column>
                  {values.panelDomainPost === "VECTOR" && (
                    <>
                      <Column lg={8} md={4} sm={4}>
                        <>
                          <FormattedMessage
                            id="panel.editor.vectorOrganismGroup"
                            defaultMessage="Organism Group"
                          />{" "}
                          :
                        </>
                      </Column>
                      <Column lg={8} md={4} sm={4}>
                        <Select
                          id="organismGroupPost"
                          name="organismGroupPost"
                          value={values.organismGroupPost}
                          onChange={handleChange}
                          onBlur={handleBlur}
                          labelText={intl.formatMessage({
                            id: "panel.editor.vectorOrganismGroup",
                            defaultMessage: "Organism Group",
                          })}
                          disabled={bothFilled}
                        >
                          <SelectItem
                            value=""
                            text={intl.formatMessage({
                              id: "panel.organism.selectPlaceholder",
                            })}
                          />
                          <SelectItem
                            value="Mosquito"
                            text={intl.formatMessage({
                              id: "panel.organism.mosquito",
                            })}
                          />
                          <SelectItem
                            value="Fly"
                            text={intl.formatMessage({
                              id: "panel.organism.fly",
                            })}
                          />
                          <SelectItem
                            value="Flea"
                            text={intl.formatMessage({
                              id: "panel.organism.flea",
                            })}
                          />
                          <SelectItem
                            value="Rodent"
                            text={intl.formatMessage({
                              id: "panel.organism.rodent",
                            })}
                          />
                          <SelectItem
                            value="Tick"
                            text={intl.formatMessage({
                              id: "panel.organism.tick",
                            })}
                          />
                        </Select>
                      </Column>
                    </>
                  )}
                </Grid>
                {bothFilled && (
                  <>
                    <br />
                    <Grid fullWidth={true}>
                      <Column lg={16} md={8} sm={4}>
                        <Section>
                          <Section>
                            <Section>
                              <Section>
                                <Heading>
                                  <FormattedMessage id="configuration.panel.confirmation.explain" />
                                </Heading>
                              </Section>
                            </Section>
                          </Section>
                        </Section>
                      </Column>
                    </Grid>
                  </>
                )}
                <br />
                <Grid fullWidth={true}>
                  <Column lg={8} md={8} sm={4}>
                    <Button
                      disabled={isSubmitting}
                      type="submit"
                      kind="primary"
                    >
                      {bothFilled ? (
                        <FormattedMessage id="accept.action.button" />
                      ) : (
                        <FormattedMessage id="next.action.button" />
                      )}
                    </Button>{" "}
                    <Button
                      type="button"
                      kind="tertiary"
                      onClick={() => {
                        window.location.reload();
                      }}
                    >
                      {bothFilled ? (
                        <FormattedMessage id="reject.action.button" />
                      ) : (
                        <FormattedMessage id="label.button.previous" />
                      )}
                    </Button>
                  </Column>
                </Grid>
              </Form>
            )}
          </Formik>
          <br />
          <hr />
          <br />
          <Grid fullWidth={true}>
            <Column lg={16} md={8} sm={4}>
              <Section>
                <Section>
                  <Section>
                    <Heading>
                      <FormattedMessage id="panel.existing" />
                    </Heading>
                  </Section>
                </Section>
              </Section>
            </Column>
          </Grid>
          <br />
          <Grid fullWidth={true}>
            <Column lg={4} md={4} sm={4}>
              <Select
                id="panel-domain-filter"
                labelText={intl.formatMessage({
                  id: "panel.filter.domain",
                  defaultMessage: "Domain",
                })}
                value={selectedDomain}
                onChange={(e) => setSelectedDomain(e.target.value)}
              >
                <SelectItem
                  value="ALL"
                  text={intl.formatMessage({ id: "panel.filter.all" })}
                />
                <SelectItem
                  value="V"
                  text={intl.formatMessage({ id: "panel.domain.vector" })}
                />
                <SelectItem
                  value="H"
                  text={intl.formatMessage({ id: "panel.domain.clinical" })}
                />
                <SelectItem
                  value="L"
                  text={intl.formatMessage({ id: "panel.domain.eqa" })}
                />
                <SelectItem
                  value="E"
                  text={intl.formatMessage({
                    id: "panel.domain.environmental",
                  })}
                />
              </Select>
            </Column>
          </Grid>
          <br />
          <hr />
          <br />
          <Grid fullWidth={true}>
            {panelCreateList &&
              panelCreateList?.existingPanelList
                ?.filter(
                  (epl) =>
                    selectedDomain === "ALL" ||
                    (epl?.domain || "").toUpperCase() ===
                      selectedDomain.toUpperCase(),
                )
                .map((epl, index) => {
                  return (
                    <Column lg={4} md={4} sm={4} key={index}>
                      <span style={{ fontWeight: "bold" }}>
                        {epl?.typeOfSampleName}
                      </span>
                      {epl?.domain && (
                        <Tag
                          type={domainTagColor(epl.domain)}
                          size="sm"
                          style={{ marginLeft: 6, verticalAlign: "middle" }}
                        >
                          {domainLabel(epl.domain)}
                        </Tag>
                      )}
                      {epl?.panels?.map((panel, index) => {
                        return (
                          <Column lg={4} md={4} sm={4} key={index}>
                            <ListItem>{panel?.panelName}</ListItem>
                          </Column>
                        );
                      })}
                    </Column>
                  );
                })}
          </Grid>
          <br />
          <hr />
          <br />
          <Grid fullWidth={true}>
            <Column lg={16} md={8} sm={4}>
              <Section>
                <Section>
                  <Section>
                    <Heading>
                      <FormattedMessage id="panel.existing.inactive" />
                    </Heading>
                  </Section>
                </Section>
              </Section>
            </Column>
          </Grid>
          <br />
          <hr />
          <br />
          <Grid fullWidth={true}>
            {panelCreateList &&
              panelCreateList?.inactivePanelList
                ?.filter(
                  (epl) =>
                    selectedDomain === "ALL" ||
                    (epl?.domain || "").toUpperCase() ===
                      selectedDomain.toUpperCase(),
                )
                .map((epl, index) => {
                  return (
                    <Column lg={4} md={4} sm={4} key={index}>
                      <span style={{ fontWeight: "bold" }}>
                        {epl?.typeOfSampleName}
                      </span>
                      {epl?.domain && (
                        <Tag
                          type={domainTagColor(epl.domain)}
                          size="sm"
                          style={{ marginLeft: 6, verticalAlign: "middle" }}
                        >
                          {domainLabel(epl.domain)}
                        </Tag>
                      )}
                      {epl?.panels?.map((panel, index) => {
                        return (
                          <Column lg={4} md={4} sm={4} key={index}>
                            <ListItem>{panel?.panelName}</ListItem>
                          </Column>
                        );
                      })}
                    </Column>
                  );
                })}
          </Grid>
        </div>
      </div>
    </>
  );
}

export default injectIntl(PanelCreate);
