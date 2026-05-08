import React, { useContext, useState, useEffect, useRef } from "react";
import {
  Form,
  Heading,
  Toggle,
  Button,
  Loading,
  Grid,
  Column,
  Section,
} from "@carbon/react";
import {
  getFromOpenElisServer,
  postToOpenElisServerFullResponse,
} from "../../utils/Utils";
import { MenuCheckBox } from "./MenuUtil";
import { NotificationContext } from "../../layout/Layout";
import {
  AlertDialog,
  NotificationKinds,
} from "../../common/CustomNotification";
import { FormattedMessage, useIntl } from "react-intl";
import PageBreadCrumb from "../../common/PageBreadCrumb";

let breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "breadcrums.admin.managment", link: "/MasterListsPage" },
  {
    label: "sidenav.label.admin.menu.nonconform",
    link: "/MasterListsPage/nonConformityMenuManagement",
  },
];
function NonConformityMenuManagement() {
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  const intl = useIntl();

  const componentMounted = useRef(false);

  const [loading, setLoading] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [showChildren, setShowChildren] = useState(false);
  const [menuItem, setMenuItem] = useState({ menu: {}, childMenus: [] });

  async function displayStatus(res) {
    setNotificationVisible(true);
    setIsSubmitting(false);
    // #region agent log
    fetch("http://localhost:7409/ingest/55da6f2c-f986-41bf-b998-e611407c1faa", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "X-Debug-Session-Id": "c0dd4a",
      },
      body: JSON.stringify({
        sessionId: "c0dd4a",
        runId: "pre-fix",
        hypothesisId: "H6,H7,H8",
        location: "NonConformityMenuManagement.jsx:displayStatus",
        message: "Nonconform POST status",
        data: { status: res?.status },
        timestamp: Date.now(),
      }),
    }).catch(() => {});
    // #endregion
    if (res.status == "200") {
      addNotification({
        kind: NotificationKinds.success,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "success.add.edited.msg" }),
      });
      var body = await res.json();
      // #region agent log
      fetch(
        "http://localhost:7409/ingest/55da6f2c-f986-41bf-b998-e611407c1faa",
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            "X-Debug-Session-Id": "c0dd4a",
          },
          body: JSON.stringify({
            sessionId: "c0dd4a",
            runId: "pre-fix",
            hypothesisId: "H6,H7,H8",
            location: "NonConformityMenuManagement.jsx:displayStatus",
            message: "Nonconform POST body",
            data: {
              elementId: body?.menu?.elementId,
              isActive: body?.menu?.isActive,
              childCount: body?.childMenus?.length,
              childStates: (body?.childMenus || []).map((item) => ({
                elementId: item?.menu?.elementId,
                isActive: item?.menu?.isActive,
              })),
            },
            timestamp: Date.now(),
          }),
        },
      ).catch(() => {});
      // #endregion
      setMenuItem(body);
    } else {
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "error.add.edited.msg" }),
      });
    }
  }

  function handleSubmit(event) {
    event.preventDefault();
    setIsSubmitting(true);
    // #region agent log
    fetch("http://localhost:7409/ingest/55da6f2c-f986-41bf-b998-e611407c1faa", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "X-Debug-Session-Id": "c0dd4a",
      },
      body: JSON.stringify({
        sessionId: "c0dd4a",
        runId: "pre-fix",
        hypothesisId: "H6,H7,H8",
        location: "NonConformityMenuManagement.jsx:handleSubmit",
        message: "Nonconform submit payload",
        data: {
          elementId: menuItem?.menu?.elementId,
          isActive: menuItem?.menu?.isActive,
          childCount: menuItem?.childMenus?.length,
          childStates: (menuItem?.childMenus || []).map((item) => ({
            elementId: item?.menu?.elementId,
            isActive: item?.menu?.isActive,
          })),
        },
        timestamp: Date.now(),
      }),
    }).catch(() => {});
    // #endregion
    postToOpenElisServerFullResponse(
      "/rest/menu/menu_nonconformity",
      JSON.stringify(menuItem),
      displayStatus,
    );
  }

  const handleMenuItems = (res) => {
    // #region agent log
    fetch("http://localhost:7409/ingest/55da6f2c-f986-41bf-b998-e611407c1faa", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "X-Debug-Session-Id": "c0dd4a",
      },
      body: JSON.stringify({
        sessionId: "c0dd4a",
        runId: "pre-fix",
        hypothesisId: "H6,H7",
        location: "NonConformityMenuManagement.jsx:handleMenuItems",
        message: "Nonconform GET body",
        data: {
          hasResponse: !!res,
          elementId: res?.menu?.elementId,
          isActive: res?.menu?.isActive,
          childCount: res?.childMenus?.length,
        },
        timestamp: Date.now(),
      }),
    }).catch(() => {});
    // #endregion
    if (res) {
      setMenuItem(res);
    }
  };

  useEffect(() => {
    componentMounted.current = true;
    getFromOpenElisServer(
      "/rest/admin/menu/menu_nonconformity",
      handleMenuItems,
    );
    return () => {
      componentMounted.current = false;
    };
  }, []);

  return (
    <>
      {notificationVisible === true ? <AlertDialog /> : ""}
      {loading && <Loading />}
      <div className="adminPageContent">
        <PageBreadCrumb breadcrumbs={breadcrumbs} />
        <Grid fullWidth={true}>
          <Column lg={16}>
            <Section>
              <Heading>
                <FormattedMessage id="menu.nonconform.title" />
              </Heading>
            </Section>
            <Section>
              <Form onSubmit={handleSubmit}>
                <br></br>
                <Toggle
                  id="toggleShowChildren"
                  labelText={intl.formatMessage({
                    id: "label.showChildren",
                  })}
                  size="md"
                  toggled={showChildren}
                  onToggle={() => {
                    setShowChildren(!showChildren);
                  }}
                />
                <br></br>
                <br></br>
                <MenuCheckBox
                  menuItem={menuItem}
                  curMenuItem={menuItem}
                  path="$"
                  setMenuItem={setMenuItem}
                  labelKey="menu.nonconform.active"
                  recurse={showChildren}
                />
                <br></br>
                <div>
                  <Button type="submit">
                    <FormattedMessage id="label.button.submit" />
                    {isSubmitting && <Loading small={true} />}
                  </Button>
                </div>
              </Form>
            </Section>
          </Column>
        </Grid>
      </div>
    </>
  );
}

export default NonConformityMenuManagement;
