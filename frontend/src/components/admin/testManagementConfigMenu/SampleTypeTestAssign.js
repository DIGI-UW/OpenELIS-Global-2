import React, { useContext, useState, useEffect, useRef } from "react";
import {
  Form,
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
  ClickableTile,
  Modal,
} from "@carbon/react";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
  postToOpenElisServerFormData,
  postToOpenElisServerFullResponse,
  postToOpenElisServerJsonResponse,
} from "../../utils/Utils.js";
import { NotificationContext } from "../../layout/Layout.js";
import {
  AlertDialog,
  NotificationKinds,
} from "../../common/CustomNotification.js";
import { FormattedMessage, injectIntl, useIntl } from "react-intl";
import PageBreadCrumb from "../../common/PageBreadCrumb.js";
import CustomCheckBox from "../../common/CustomCheckBox.js";
import ActionPaginationButtonType from "../../common/ActionPaginationButtonType.js";

let breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "breadcrums.admin.managment", link: "/MasterListsPage" },
  {
    label: "master.lists.page.test.management",
    link: "/MasterListsPage#testManagementConfigMenu",
  },
  {
    label: "configuration.sampleType.manage",
    link: "/MasterListsPage#SampleTypeManagement",
  },
  {
    label: "configuration.panel.assign",
    link: "/MasterListsPage#SampleTypeTestAssign",
  },
];

function SampleTypeTestAssign() {
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  const intl = useIntl();
  const [isLoading, setIsLoading] = useState(false);
  const [confirmation, setConfirmation] = useState(false);
  const [sampleTypeTestAssignModal, setSampleTypeTestAssignModal] =
    useState(false);
  const [sampleTypeTestAssign, setSampleTypeTestAssign] = useState({});
  const [sampleTypeTestAssignPost, setSampleTypeTestAssignPost] = useState({
    testId: "",
    testValue: "",
    sampleTypeIdNew: "",
    sampleTypeNameNew: "",
    sampleTypeIdOld: "",
    sampleTypeNameOld: "",
  });
  const componentMounted = useRef(false);

  const handleSampleTypeTestAssignList = (res) => {
    if (!res) {
      setIsLoading(true);
    } else {
      setSampleTypeTestAssign(res);
    }
  };

  const handlePostSampleTypeTestAssignListCall = () => {
    if (
      !sampleTypeTestAssignPost.testId ||
      !sampleTypeTestAssignPost.sampleTypeIdNew
    ) {
      window.location.reload();
      return;
    }
    postToOpenElisServerJsonResponse(
      "/rest/SampleTypeTestAssign",
      JSON.stringify({
        testId: sampleTypeTestAssignPost.testId,
        sampleTypeId: sampleTypeTestAssignPost.sampleTypeIdNew,
        deactivateSampleTypeId: "", // TODO: need to chemk
      }),
      (res) => {
        handlePostSampleTypeTestAssignListCallBack(res);
      },
    );
  };

  const handlePostSampleTypeTestAssignListCallBack = (res) => {
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
    } else {
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "server.error.msg" }),
      });
      setNotificationVisible(true);
      setTimeout(() => {
        window.location.reload();
      }, 200);
    }
  };

  useEffect(() => {
    componentMounted.current = true;
    setIsLoading(true);
    getFromOpenElisServer(
      `/rest/SampleTypeTestAssign`,
      handleSampleTypeTestAssignList,
    );
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
                  <FormattedMessage id="label.button.select" />
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
                  <Section>
                    <Heading>
                      <FormattedMessage id="configuration.panel.assign" />
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
            <Column lg={16} md={8} sm={4}>
              <Section>
                <Section>
                  <Section>
                    <Section>
                      <Heading>
                        <FormattedMessage id="configuration.sampleType.assign.explain" />
                      </Heading>
                    </Section>
                  </Section>
                </Section>
              </Section>
            </Column>
          </Grid>
          <br />
          <Grid fullWidth={true}>
            {sampleTypeTestAssign &&
            sampleTypeTestAssign?.sampleTypeTestList &&
            Object.keys(sampleTypeTestAssign?.sampleTypeTestList).length > 0 ? (
              <>
                {Object.entries(sampleTypeTestAssign?.sampleTypeTestList).map(
                  ([sectionKey, tests]) => {
                    const sectionId = sectionKey
                      .split(", value=")[0]
                      .split("id=")[1];
                    const sectionName = sectionKey.split(", value=")[1];
                    return (
                      <React.Fragment key={`${sectionKey}-${sectionId}`}>
                        <Column lg={16} md={8} sm={4}>
                          <h4>{sectionName}</h4>
                        </Column>
                        {tests.map((test) => (
                          <Column
                            style={{ margin: "2px" }}
                            key={`${sectionId}-${test.id}`}
                            lg={4}
                            md={4}
                            sm={4}
                          >
                            <ClickableTile
                              onClick={() => {
                                (setSampleTypeTestAssignModal(true),
                                  setSampleTypeTestAssignPost({
                                    testId: test.id,
                                    testValue: test.value,
                                    sampleTypeNameOld: sectionName,
                                    sampleTypeIdOld: sectionId,
                                  }));
                              }}
                            >
                              {test.value}
                            </ClickableTile>
                          </Column>
                        ))}
                      </React.Fragment>
                    );
                  },
                )}
              </>
            ) : (
              <></>
            )}
          </Grid>
        </div>
      </div>

      <Modal
        open={sampleTypeTestAssignModal}
        size="md"
        modalHeading={
          confirmation
            ? `${intl.formatMessage({
                id: "uom.create.heading.confirmation",
              })}`
            : `${intl.formatMessage({
                id: "banner.menu.patientEdit",
              })}`
        }
        primaryButtonText={
          confirmation
            ? intl.formatMessage({ id: "accept.action.button" })
            : intl.formatMessage({ id: "label.button.save" })
        }
        secondaryButtonText={
          confirmation
            ? intl.formatMessage({ id: "reject.action.button" })
            : intl.formatMessage({ id: "label.button.cancel" })
        }
        onRequestSubmit={() => {
          if (confirmation) {
            setSampleTypeTestAssignModal(false);
            handlePostSampleTypeTestAssignListCall();
          } else {
            setConfirmation(true);
          }
        }}
        onRequestClose={() => {
          setSampleTypeTestAssignModal(false);
          window.location.reload();
        }}
        preventCloseOnClickOutside={true}
        shouldSubmitOnEnter={true}
      >
        <Grid fullWidth={true}>
          <Column lg={16} md={8} sm={4}>
            <Section>
              <Section>
                <Heading>
                  <FormattedMessage id="configuration.panel.assign" />
                </Heading>
              </Section>
            </Section>
            <br />
            <Section>
              <Section>
                <Section>
                  <Heading>
                    <FormattedMessage id="Test" /> :{" "}
                    {sampleTypeTestAssignPost?.testValue}
                  </Heading>
                </Section>
              </Section>
            </Section>
            <br />
            <Section>
              {sampleTypeTestAssign?.sampleTypeList &&
              sampleTypeTestAssign?.sampleTypeList?.length > 0 ? (
                <Select
                  size="sm"
                  id="sampleTypeListSelect"
                  labelText={
                    <span style={{ fontSize: "1.2rem", fontWeight: 500 }}>
                      {`${intl.formatMessage({ id: "configuration.sampleType.assign.new.type" })} : `}
                    </span>
                  }
                  onChange={(e) => {
                    const selectedOption = e.target.selectedOptions[0];
                    const selectedId = selectedOption.value;
                    const selectedValue = selectedOption.dataset.value;

                    setSampleTypeTestAssignPost((prev) => ({
                      ...prev,
                      sampleTypeIdNew: selectedId,
                      sampleTypeNameNew: selectedValue,
                    }));
                  }}
                >
                  {sampleTypeTestAssign?.sampleTypeList.map((item) => (
                    <SelectItem
                      key={item.id}
                      value={item.id}
                      text={item.value}
                      data-value={item.value}
                    />
                  ))}
                </Select>
              ) : (
                ""
              )}
            </Section>
            <br />
            {confirmation &&
              sampleTypeTestAssignPost &&
              sampleTypeTestAssignPost?.testValue &&
              sampleTypeTestAssignPost?.sampleTypeNameOld &&
              sampleTypeTestAssignPost?.sampleTypeNameNew && (
                <Section>
                  <Section>
                    <Section>
                      <Heading>
                        <span
                          style={{
                            fontWeight: "bold",
                            textDecoration: "underline",
                          }}
                        >
                          {sampleTypeTestAssignPost?.testValue}
                        </span>{" "}
                        {"will be moved from"}{" "}
                        <span
                          style={{
                            fontWeight: "bold",
                            textDecoration: "underline",
                          }}
                        >
                          {sampleTypeTestAssignPost?.sampleTypeNameOld}
                        </span>{" "}
                        {"to"}{" "}
                        <span
                          style={{
                            fontWeight: "bold",
                            textDecoration: "underline",
                          }}
                        >
                          {sampleTypeTestAssignPost?.sampleTypeNameNew}
                        </span>
                      </Heading>
                    </Section>
                  </Section>
                </Section>
              )}
          </Column>
        </Grid>
      </Modal>
    </>
  );
}

export default injectIntl(SampleTypeTestAssign);
