import React, { useContext, useState, useEffect, useRef } from "react";
import {
  Form,
  Heading,
  TextInput,
  Button,
  Loading,
  Grid,
  Column,
  Section,
  Checkbox,
} from "@carbon/react";
import {
  getFromOpenElisServer,
  postToOpenElisServerFullResponse,
} from "../../utils/Utils";
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
    label: "sidenav.label.admin.menu.billing",
    link: "/MasterListsPage/billingMenuManagement",
  },
];
function BillingMenuManagement() {
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  const intl = useIntl();

  const componentMounted = useRef(false);

  const [loading, setLoading] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
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
        hypothesisId: "H1,H2",
        location: "BillingMenuManagement.jsx:displayStatus",
        message: "Billing menu POST response status",
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
            hypothesisId: "H1,H2",
            location: "BillingMenuManagement.jsx:displayStatus",
            message: "Billing menu POST response body",
            data: {
              elementId: body?.menu?.elementId,
              isActive: body?.menu?.isActive,
              actionURL: body?.menu?.actionURL,
              childCount: body?.childMenus?.length,
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
        hypothesisId: "H1",
        location: "BillingMenuManagement.jsx:handleSubmit",
        message: "Billing menu submit payload",
        data: {
          elementId: menuItem?.menu?.elementId,
          isActive: menuItem?.menu?.isActive,
          actionURL: menuItem?.menu?.actionURL,
          childCount: menuItem?.childMenus?.length,
        },
        timestamp: Date.now(),
      }),
    }).catch(() => {});
    // #endregion
    postToOpenElisServerFullResponse(
      "/rest/menu/menu_billing",
      JSON.stringify(menuItem),
      displayStatus,
    );
  }

  const handleMenuItems = (res) => {
    setLoading(false);
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
        hypothesisId: "H2",
        location: "BillingMenuManagement.jsx:handleMenuItems",
        message: "Billing menu GET response",
        data: {
          hasResponse: !!res,
          elementId: res?.menu?.elementId,
          isActive: res?.menu?.isActive,
          actionURL: res?.menu?.actionURL,
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
    setLoading(true);
    getFromOpenElisServer("/rest/menu/menu_billing", handleMenuItems);
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
          <Column lg={16} md={8} sm={4}>
            <Section>
              <Heading>
                <FormattedMessage id="menu.billing.title" />
              </Heading>
            </Section>
            <br />
            <Section>
              <Form onSubmit={handleSubmit}>
                <div className="formInlineDiv">
                  <TextInput
                    id="billing_address"
                    labelText={intl.formatMessage({
                      id: "menu.billing.address",
                    })}
                    value={menuItem.menu.actionURL || ""}
                    onChange={(e) => {
                      setMenuItem({
                        ...menuItem,
                        menu: { ...menuItem.menu, actionURL: e.target.value },
                      });
                    }}
                    type="url"
                    required
                    pattern="https?://.*"
                  />
                </div>
                <div className="formInlineDiv">
                  <Checkbox
                    id="billing_active"
                    labelText={intl.formatMessage({
                      id: "menu.billing.active",
                    })}
                    checked={menuItem.menu.isActive || false}
                    onChange={(_, { checked }) => {
                      setMenuItem({
                        ...menuItem,
                        menu: { ...menuItem.menu, isActive: checked },
                      });
                    }}
                  />
                </div>
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

export default BillingMenuManagement;
