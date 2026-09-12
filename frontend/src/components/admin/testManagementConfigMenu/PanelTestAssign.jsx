import React, { useContext, useState } from "react";
import {
  Heading,
  Button,
  Grid,
  Column,
  Section,
  Select,
  SelectItem,
} from "@carbon/react";
import { postToOpenElisServerJsonResponse } from "../../utils/Utils";
import {
  useInvalidateServerData,
  useServerData,
} from "../../utils/useServerData";
import { NotificationContext } from "../../layout/contexts";
import {
  AlertDialog,
  NotificationKinds,
} from "../../common/CustomNotification";
import { FormattedMessage, injectIntl, useIntl } from "react-intl";
import PageBreadCrumb from "../../common/PageBreadCrumb";
import { CustomSharedList } from "./CustomSharedList";
import ServerDataState from "../../utils/ServerDataState";

const PANEL_TEST_ASSIGN_ENDPOINT = "/rest/PanelTestAssign";

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

function PanelTestAssign() {
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  const intl = useIntl();
  const [panelId, setPanelId] = useState("");
  // The tests moved between the two lists but not yet saved. Null means the
  // panel is shown as it is stored.
  const [movedTests, setMovedTests] = useState(null);

  const panelTestQuery = useServerData(PANEL_TEST_ASSIGN_ENDPOINT);
  const { data: panelTestList } = panelTestQuery;
  const invalidateServerData = useInvalidateServerData();

  // Held off until a panel is picked and keyed on it. Until the read for a
  // newly picked panel arrives the hook still serves the one picked before,
  // so the lists stay empty rather than showing the previous panel's tests.
  const { data: readPanel, isPreviousData } = useServerData(
    panelId ? `${PANEL_TEST_ASSIGN_ENDPOINT}?panelId=${panelId}` : null,
  );
  const storedPanel = isPreviousData ? undefined : readPanel;
  const selectedPanelIdData =
    storedPanel && movedTests
      ? {
          ...storedPanel,
          selectedPanel: { ...storedPanel.selectedPanel, ...movedTests },
        }
      : storedPanel;

  const handlePostPanelTestTestAssignListCall = () => {
    if (!panelId || !selectedPanelIdData?.selectedPanel) {
      setMovedTests(null);
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
      addNotification({
        title: intl.formatMessage({
          id: "notification.title",
        }),
        message: intl.formatMessage({
          id: "notification.user.post.delete.success",
        }),
        kind: NotificationKinds.success,
      });
      setNotificationVisible(true);
      setMovedTests(null);
      invalidateServerData();
    } else {
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "server.error.msg" }),
      });
      setNotificationVisible(true);
    }
  };

  if (!panelTestList) return <ServerDataState query={panelTestQuery} />;

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
                        setMovedTests(null);
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
                    setMovedTests({
                      tests: newLeft,
                      availableTests: newRight,
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
