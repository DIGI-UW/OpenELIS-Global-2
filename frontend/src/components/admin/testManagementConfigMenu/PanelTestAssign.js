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
import { CustomSharedList } from "./CustomSharedList.js";

let breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "breadcrums.admin.managment", link: "/MasterListsPage" },
  {
    label: "master.lists.page.test.management",
    link: "/MasterListsPage#testManagementConfigMenu",
  },
  {
    label: "configuration.panel.manage",
    link: "/MasterListsPage#PanelManagement",
  },
  {
    label: "configuration.panel.assign",
    link: "/MasterListsPage#PanelTestAssign",
  },
];

function PanelTestAssign() {
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  const intl = useIntl();
  const [isLoading, setIsLoading] = useState(true);
  const [panelTestList, setPanelTestList] = useState([]);
  const [panelId, setPanelId] = useState("");
  const [selectedPanelIdData, setSelectedPanelIdData] = useState({});

  const componentMounted = useRef(false);

  const handlePostPanelTestTestAssignListCall = () => {
    if (!panelId || !selectedPanelIdData) {
      window.location.reload();
      return;
    }
    postToOpenElisServerJsonResponse(
      "/rest/PanelTestAssign",
      JSON.stringify({
        panelId: panelId,
        currentTests: selectedPanelIdData?.selectedPanel?.tests?.map((item) =>
          String(item.id),
        ),
        availableTests: ["1"], //TODO: need to check backend why ["1"] is working as hardcoded
        deactivatePanelId: "", //TODO: need to check backend
      }),
      (res) => {
        handlePostPanelTestTestAssignListCallBack(res);
      },
    );
  };

  const handlePostPanelTestTestAssignListCallBack = (res) => {
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

  const handleSelectedPanelTestList = (res) => {
    if (!res) {
      window.location.reload();
    } else {
      setSelectedPanelIdData(res);
    }
  };

  const handlePanelTestAssignList = (res) => {
    if (!res) {
      setIsLoading(true);
    } else {
      setPanelTestList(res);
    }
  };

  useEffect(() => {
    if (componentMounted.current) {
      if (panelId) {
        getFromOpenElisServer(
          `/rest/PanelTestAssign?panelId=${panelId}`,
          handleSelectedPanelTestList,
        );
      }
    }
  }, [panelId]);

  useEffect(() => {
    componentMounted.current = true;
    setIsLoading(true);
    getFromOpenElisServer(`/rest/PanelTestAssign`, handlePanelTestAssignList);
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
                {panelTestList && panelTestList?.panelList?.length > 0 ? (
                  <>
                    <Select
                      size="sm"
                      id="panelTestList"
                      labelText={
                        <span style={{ fontSize: "1.2rem", fontWeight: 500 }}>
                          {`${intl.formatMessage({ id: "Panel" })} : `}
                        </span>
                      }
                      value={panelId}
                      onChange={(e) => {
                        setPanelId(e.target.value);
                      }}
                    >
                      <SelectItem
                        disabled
                        hidden
                        value=""
                        text="-- Select Panel Test --"
                      />
                      {panelTestList?.panelList?.map((panelTest) => (
                        <SelectItem
                          key={panelTest.id}
                          value={panelTest.id}
                          text={panelTest.value}
                        />
                      ))}
                    </Select>
                  </>
                ) : (
                  <></>
                )}
              </Section>
            </Column>
          </Grid>
          <br />
          <Grid fullWidth={true}>
            <Column lg={16} md={8} sm={4}>
              {selectedPanelIdData && selectedPanelIdData?.selectedPanel && (
                <CustomSharedList
                  leftTitle={`${selectedPanelIdData?.selectedPanel?.panelIdValuePair?.value} - Tests`}
                  rightTitle={`Available Tests (${selectedPanelIdData?.selectedPanel?.sampleTypeIdValuePair?.value})`}
                  leftList={selectedPanelIdData?.selectedPanel?.tests}
                  rightList={selectedPanelIdData?.selectedPanel?.availableTests}
                  renderItem={(item) => item}
                  onChange={(newLeft, newRight) => {
                    setSelectedPanelIdData((prev) => {
                      return {
                        ...prev,
                        selectedPanel: {
                          ...prev.selectedPanel,
                          tests: newLeft,
                          availableTests: newRight,
                        },
                      };
                    });
                  }}
                />
              )}
            </Column>
          </Grid>
          <br />
          <Grid fullWidth={true}>
            <Column lg={16} md={8} sm={4}>
              <Section>
                <Button
                  kind="primary"
                  onClick={() => {
                    handlePostPanelTestTestAssignListCall();
                  }}
                >
                  <FormattedMessage id="label.button.save" />
                </Button>
              </Section>
            </Column>
          </Grid>
        </div>
      </div>
    </>
  );
}

export default injectIntl(PanelTestAssign);
