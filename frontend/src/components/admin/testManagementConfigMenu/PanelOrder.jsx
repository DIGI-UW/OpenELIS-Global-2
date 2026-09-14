import React, { useContext, useState } from "react";
import {
  Heading,
  Button,
  Loading,
  Grid,
  Column,
  Section,
  ListItem,
} from "@carbon/react";
import { postToOpenElisServerJsonResponse } from "../../utils/Utils";
import {
  useServerData,
  useInvalidateServerData,
} from "../../utils/useServerData";
import { NotificationContext } from "../../layout/contexts";
import {
  AlertDialog,
  NotificationKinds,
} from "../../common/CustomNotification";
import { FormattedMessage, injectIntl, useIntl } from "react-intl";
import PageBreadCrumb from "../../common/PageBreadCrumb";
import { CustomCommonSortableOrderList } from "./sortableListComponent/SortableList";

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

function PanelOrder() {
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  const [confirmSelection, setConfirmSelection] = useState(false);
  const [pendingOrder, setPendingOrder] = useState(null);
  const [panelOrderListPost, setPanelOrderListPost] = useState([]);
  const intl = useIntl();

  const handlePanelOrderListCall = () => {
    if (!panelOrderListPost?.length) {
      // Accepting an unchanged preview is complete once it leaves confirmation.
      setPendingOrder(null);
      setPanelOrderListPost([]);
      setConfirmSelection(false);
      return;
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
        addNotification({
          title: intl.formatMessage({
            id: "notification.title",
          }),
          message: intl.formatMessage({
            id: "notification.user.post.delete.success",
          }),
          kind: NotificationKinds.success,
        });
        setPendingOrder(null);
        setPanelOrderListPost([]);
        setConfirmSelection(false);
        refreshPanelOrderList("/rest/PanelOrder");
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

  // The order shown is a read of /rest/PanelOrder; a save marks it out of date
  // and the screen reads it again, which is what reloading used to do.
  const { data: fetchedPanelOrderList, isFetching: panelOrderListFetching } =
    useServerData("/rest/PanelOrder");
  const refreshPanelOrderList = useInvalidateServerData();

  // A pending reorder sits on top of the stored order, so discarding it is
  // clearing it: the stored array comes back as the same reference the list
  // was seeded from, which is a change the list can see.
  const shownOrder = pendingOrder ?? fetchedPanelOrderList?.panelList;

  if (panelOrderListFetching && !fetchedPanelOrderList) {
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
              {shownOrder?.length > 0 && (
                <CustomCommonSortableOrderList
                  test={shownOrder}
                  onSort={(updatedList) => {
                    setPendingOrder(updatedList);
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
                    return;
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
                  // Discard the pending reordering and show what is stored.
                  setPendingOrder(null);
                  setPanelOrderListPost([]);
                  setConfirmSelection(false);
                  refreshPanelOrderList("/rest/PanelOrder");
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
            {fetchedPanelOrderList?.existingPanelList?.map((epl, index) => {
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
            {fetchedPanelOrderList?.inactivePanelList?.map((epl, index) => {
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

PanelOrder;
