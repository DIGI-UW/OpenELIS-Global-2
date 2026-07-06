/* eslint-disable @typescript-eslint/no-unused-vars -- preserve unused declarations from the original JavaScript */
import React, { useContext, useState, useEffect, useRef } from "react";
import { Heading, Button, Loading, Grid, Column, Section } from "@carbon/react";
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
    label: "configuration.sampleType.manage",
    link: "/MasterListsPage/SampleTypeManagement",
  },
  {
    label: "configuration.sampleType.order",
    link: "/MasterListsPage/SampleTypeOrder",
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

function SampleTypeOrder() {
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext) as NotificationContextValue;

  const intl = useIntl();
  const [isLoading, setIsLoading] = useState(false);
  const [confirmSelection, setConfirmSelection] = useState(false);
  const [sampleTypeOrderList, setSampleTypeOrderList] = useState<{
    sampleTypeList?: Array<{ id: string; sortOrder: number; value?: string }>;
  }>({});
  const [sampleTypeOrderListPost, setSampleTypeOrderListPost] = useState([]);

  const componentMounted = useRef(false);

  const handleSampleTypeOrderList = (res) => {
    if (!res) {
      setIsLoading(true);
    } else {
      setSampleTypeOrderList(res);
    }
  };

  const handleSampleTypeOrderListCall = () => {
    if (!sampleTypeOrderListPost) {
      setIsLoading(true);
      setTimeout(() => {
        window.location.reload();
      }, 200);
    }
    postToOpenElisServerJsonResponse(
      "/rest/SampleTypeOrder",
      JSON.stringify({
        jsonChangeList: JSON.stringify({
          sampleTypes: JSON.stringify(sampleTypeOrderListPost),
        }),
      }),
      (res) => {
        handlePostSampleTypeOrderListCallBack(res);
      },
    );
  };

  const handlePostSampleTypeOrderListCallBack = (res) => {
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
    getFromOpenElisServer(`/rest/SampleTypeOrder`, handleSampleTypeOrderList);
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
                      <FormattedMessage id="configuration.sampleType.order.explain" />
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
                        <FormattedMessage id="configuration.sampleType.order.explain.limits" />
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
              {sampleTypeOrderList &&
                sampleTypeOrderList?.sampleTypeList &&
                sampleTypeOrderList?.sampleTypeList?.length > 0 && (
                  <CustomCommonSortableOrderList
                    test={sampleTypeOrderList?.sampleTypeList}
                    onSort={(updatedList) => {
                      setSampleTypeOrderList((prev) => ({
                        ...prev,
                        sampleTypeList: updatedList,
                      }));
                      setSampleTypeOrderListPost(
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
                    handleSampleTypeOrderListCall();
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
        </div>
      </div>
    </>
  );
}

export default injectIntl(SampleTypeOrder);
