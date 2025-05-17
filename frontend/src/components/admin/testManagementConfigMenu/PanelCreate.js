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
    label: "configuration.panel.manage",
    link: "/MasterListsPage#SampleTypeManagement",
  },
  {
    label: "configuration.panel.create",
    link: "/MasterListsPage#PanelCreate",
  },
];

function PanelCreate() {
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  const intl = useIntl();
  const [isLoading, setIsLoading] = useState(true);
  const [bothFilled, setBothFilled] = useState(false);
  const [englishLangPost, setEnglishLangPost] = useState("");
  const [frenchLangPost, setFrenchLangPost] = useState("");
  const [selectedSampleTypeId, setSelectedSampleTypeId] = useState("");
  const [inputError, setInputError] = useState(false);
  const [panelCreateList, setPanelCreateList] = useState({});

  const componentMounted = useRef(false);

  const handlePanelCreateList = (res) => {
    if (!res) {
      setIsLoading(true);
    } else {
      setPanelCreateList(res);
    }
  };

  const handlePanelCreateListCall = () => {
    if (
      !englishLangPost ||
      (!frenchLangPost && inputError && selectedSampleTypeId)
    ) {
      setInputError(true);
      return;
    }
    postToOpenElisServerJsonResponse(
      "/rest/PanelCreate",
      JSON.stringify({
        panelEnglishName: englishLangPost,
        panelFrenchName: frenchLangPost,
        sampleTypeId: selectedSampleTypeId,
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

  const handleSampleTypeChange = (e) => {
    setSelectedSampleTypeId(e.target.value);
  };

  const validatePanelType = (name) => {
    const allPanels = [
      ...(panelCreateList?.existingPanelList
        ? panelCreateList.existingPanelList.flatMap((epl) => epl?.panels || [])
        : []),
      ...(panelCreateList?.inactivePanelList
        ? panelCreateList.inactivePanelList.flatMap((epl) => epl?.panels || [])
        : []),
    ];

    return allPanels.some((panel) => panel?.panelName === name);
  };

  useEffect(() => {
    if (englishLangPost || frenchLangPost) {
      const isPanelExists =
        validatePanelType(englishLangPost) || validatePanelType(frenchLangPost);
      setInputError(isPanelExists);
    }
  }, [englishLangPost, frenchLangPost, panelCreateList]);

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
                labelText=""
                hideLabel
                disabled={bothFilled}
                value={`${englishLangPost}` || ""}
                onChange={(e) => {
                  setEnglishLangPost(e.target.value);
                }}
                required
                invalid={inputError}
                invalidText={
                  <FormattedMessage id="input.error.same.panel.type" />
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
                labelText=""
                hideLabel
                disabled={bothFilled}
                value={`${frenchLangPost}` || ""}
                onChange={(e) => {
                  setFrenchLangPost(e.target.value);
                }}
                required
                invalid={inputError}
                invalidText={
                  <FormattedMessage id="input.error.same.panel.type" />
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
                name="SampleType"
                labelText={intl.formatMessage({ id: "sample.select.type" })}
                onChange={handleSampleTypeChange}
                required
              >
                {panelCreateList &&
                  panelCreateList?.existingSampleTypeList?.map((st, index) => {
                    return (
                      <SelectItem key={index} text={st.value} value={st.id} />
                    );
                  })}
              </Select>
            </Column>
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
                onClick={() => {
                  if (bothFilled) {
                    handlePanelCreateListCall();
                  }
                  setBothFilled(true);
                  setInputError(false);
                }}
                type="button"
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
          <hr />
          <br />
          <Grid fullWidth={true}>
            {panelCreateList &&
              panelCreateList?.existingPanelList?.map((epl, index) => {
                return (
                  <Column lg={4} md={4} sm={4} key={index}>
                    <span style={{ fontWeight: "bold" }}>
                      {epl?.typeOfSampleName}
                    </span>
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
              panelCreateList?.inactivePanelList?.map((epl, index) => {
                return (
                  <Column lg={4} md={4} sm={4} key={index}>
                    <span style={{ fontWeight: "bold" }}>
                      {epl?.typeOfSampleName}
                    </span>
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
