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
import PageBreadCrumb from "../../common/PageBreadCrumb.js";

let breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "breadcrums.admin.managment", link: "/MasterListsPage" },
  {
    label: "Global Menu Management",
    link: "/MasterListsPage#globalMenuManagement",
  },
];
function GlobalMenuManagement() {
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  const intl = useIntl();

  const componentMounted = useRef(false);

  const [loading, setLoading] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [showChildren, setShowChildren] = useState(true);
  const [menuItem, setMenuItem] = useState({
    menu: { isActive: true, elementId: "menu_sidenav" },
    childMenus: [],
  });

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
      setMenuItem({
        menu: { isActive: true, elementId: "menu_sidenav" },
        childMenus: body,
      });
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
      "/rest/menu",
      JSON.stringify(menuItem.childMenus),
      displayStatus,
    );
  }

  const findIndex = (arr, elementId) => {
    for (var i = 0; i < arr.length; i++) {
      if (arr[i].menu.elementId === elementId) {
        return i;
      }
    }
    return -1;
  };

  const handleMenuItems = (res) => {
    if (res) {
      let newMenuItems = { ...menuItem };
      newMenuItems.childMenus = res;
      setMenuItem(newMenuItems);
    }
  };

  useEffect(() => {
    componentMounted.current = true;
    getFromOpenElisServer("/rest/menu", handleMenuItems);
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
                <FormattedMessage id="menu.global.title" />
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
                  labelKey="menu.sidenav.active"
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

export default GlobalMenuManagement;
