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
import PageBreadCrumb from "../../common/PageBreadCrumb.js";

let breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "breadcrums.admin.managment", link: "/MasterListsPage" },
  {
    label: "Billing Menu Management",
    link: "/MasterListsPage#billingMenuManagement",
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
    if (res.status == "200") {
      addNotification({
        kind: NotificationKinds.success,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "success.add.edited.msg" }),
      });
      var body = await res.json();
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
    postToOpenElisServerFullResponse(
      "/rest/menu/menu_billing",
      JSON.stringify(menuItem),
      displayStatus,
    );
  }

  const handleMenuItems = (res) => {
    setLoading(false);
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
                    id="billing address"
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
