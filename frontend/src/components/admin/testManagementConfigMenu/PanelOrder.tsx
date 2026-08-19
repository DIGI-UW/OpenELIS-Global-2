/* eslint-disable @typescript-eslint/no-unused-vars -- preserve unused declarations from the original JavaScript */
import React, { useContext, useState, useEffect, useRef } from "react";
import {
  Heading,
  Button,
  Loading,
  Grid,
  Column,
  Section,
  ListItem,
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
import { CustomCommonSortableOrderList } from "./sortableListComponent/SortableList";

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
    label: "configuration.panel.order",
    link: "/MasterListsPage/PanelOrder",
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

function PanelOrder() {
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext) as NotificationContextValue;

  const [isLoading, setIsLoading] = useState(false);
  const [confirmSelection, setConfirmSelection] = useState(false);
  const [panelOrderList, setPanelOrderList] = useState<{
    panelList?: Array<{ id: string; sortOrder: number; value?: string }>;
    existingPanelList?: Array<{
      typeOfSampleName?: string;
      panels?: Array<{ id?: string; value?: string; panelName?: string }>;
    }>;
    inactivePanelList?: Array<{
      typeOfSampleName?: string;
      panels?: Array<{ id?: string; value?: string; panelName?: string }>;
    }>;
  }>({});
  const [panelOrderListPost, setPanelOrderListPost] = useState([]);
  const intl = useIntl();

  const componentMounted = useRef(false);

  const handlePanelOrderList = (res) => {
    if (!res) {
      setIsLoading(true);
    } else {
      setPanelOrderList(res);
    }
  };

  const handlePanelOrderListCall = () => {
    if (!panelOrderListPost) {
      setIsLoading(true);
      setTimeout(() => {
        window.location.reload();
      }, 200);
    }
    postToOpenElisServerJsonResponse(
      "/rest/PanelOrder",
      JSON.stringify({
        jsonChangeList: JSON.stringify({
          panels: JSON.stringify(panelOrderListPost),
        }),
      }),
      (res) => {
        handlePostPanelOrderListCallBack(res);
      },
    );
  };

  const handlePostPanelOrderListCallBack = (res) => {
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

  useEffect(() => {
    componentMounted.current = true;
    setIsLoading(true);
    getFromOpenElisServer(`/rest/PanelOrder`, handlePanelOrderList);
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
                  <Section>
                    <Heading>
                      <FormattedMessage id="configuration.panel.order.explain" />
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
                        <FormattedMessage id="configuration.panel.order.explain.limits" />
                      </Heading>
                    </Section>
                  </Section>
                </Section>
              </Section>
            </Column>
          </Grid>
          <br />
          <Grid fullWidth={true}>
            <Column lg={16} md={8} sm={4}>
              {panelOrderList &&
                panelOrderList?.panelList &&
                panelOrderList?.panelList?.length > 0 && (
                  <CustomCommonSortableOrderList
                    test={panelOrderList?.panelList}
                    onSort={(updatedList) => {
                      setPanelOrderList((prev) => ({
                        ...prev,
                        panelList: updatedList,
                      }));
                      setPanelOrderListPost(
                        updatedList.map(({ id, sortOrder }) => ({
                          id: Number(id),
                          sortOrder,
                        })),
                      );
                    }}
                    disableSorting={confirmSelection}
                  />
                )}
            </Column>
          </Grid>
          {confirmSelection && (
            <>
              <br />
              <Grid fullWidth={true}>
                <Column lg={16} md={8} sm={4}>
                  <Section>
                    <Section>
                      <Heading>
                        <FormattedMessage id="uom.create.heading.confirmation" />
                      </Heading>
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
                  if (confirmSelection) {
                    handlePanelOrderListCall();
                  }
                  setConfirmSelection(true);
                }}
                type="button"
                kind="primary"
              >
                {confirmSelection ? (
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
                {confirmSelection ? (
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
            {panelOrderList &&
              panelOrderList?.existingPanelList?.map((epl, index) => {
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
            {panelOrderList &&
              panelOrderList?.inactivePanelList?.map((epl, index) => {
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

export default injectIntl(PanelOrder);

// eslint-disable-next-line @typescript-eslint/no-unused-expressions -- preserve the original JavaScript expression
PanelOrder;
