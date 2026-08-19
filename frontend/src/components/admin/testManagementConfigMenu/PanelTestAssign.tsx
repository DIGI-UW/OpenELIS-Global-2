/* eslint-disable @typescript-eslint/no-unused-vars -- preserve unused declarations from the original JavaScript */
import React, { useContext, useState, useEffect, useRef } from "react";
import {
  Heading,
  Button,
  Loading,
  Grid,
  Column,
  Section,
  Select,
  SelectItem,
} from "@carbon/react";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../utils/Utils";
import { NotificationContext } from "../../layout/Layout";
import {
  AlertDialog,
  NotificationKinds,
} from "../../common/CustomNotification";
import { FormattedMessage, injectIntl, useIntl } from "react-intl";
import PageBreadCrumb from "../../common/PageBreadCrumb";
import { CustomSharedList } from "./CustomSharedList";

// eslint-disable-next-line prefer-const -- preserve the original JavaScript let declaration
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
    label: "configuration.panel.assign",
    link: "/MasterListsPage/PanelTestAssign",
  },
];

interface NotificationContextValue {
  notificationVisible: boolean;
  setNotificationVisible: (visible: boolean) => void;
  addNotification: (notification: {
    kind: string;
    title: string;
    message: string;
  }) => void;
}

interface IdValuePair {
  id?: string;
  value: string;
}

interface SelectedPanelData {
  selectedPanel?: {
    panelIdValuePair?: IdValuePair;
    sampleTypeIdValuePair?: IdValuePair;
    tests: IdValuePair[];
    availableTests: IdValuePair[];
  };
}

function PanelTestAssign() {
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext) as NotificationContextValue;

  const intl = useIntl();
  const [isLoading, setIsLoading] = useState(true);
  const [panelTestList, setPanelTestList] = useState<{
    panelList?: IdValuePair[];
  }>([] as unknown as { panelList?: IdValuePair[] });
  const [panelId, setPanelId] = useState("");
  const [selectedPanelIdData, setSelectedPanelIdData] =
    useState<SelectedPanelData>({});

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
