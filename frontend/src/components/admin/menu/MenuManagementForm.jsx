import {
  Button,
  Column,
  Form,
  Grid,
  Heading,
  Loading,
  Section,
  Toggle,
} from "@carbon/react";
import { useContext, useEffect, useRef, useState } from "react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  AlertDialog,
  NotificationKinds,
} from "../../common/CustomNotification";
import PageBreadCrumb from "../../common/PageBreadCrumb";
import { NotificationContext } from "../../layout/Layout";
import {
  getFromOpenElisServer,
  postToOpenElisServerFullResponse,
} from "../../utils/Utils";
import { MenuCheckBox } from "./MenuUtil";

/**
 * Tree-of-checkboxes form for toggling is_active on a set of menu rows.
 * Backs the lab nav (GlobalMenuManagement) and the admin nav
 * (AdminMenuManagement) — same UI, same wire format, different endpoint.
 *
 * The synthetic root uses elementId="menu_sidenav" so MenuCheckBox keeps
 * the root checkbox locked (see MenuUtil.jsx).
 */
function MenuManagementForm({ endpoint, breadcrumbs, titleKey, toggleId }) {
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);
  const intl = useIntl();

  const componentMounted = useRef(false);
  const [loading] = useState(false);
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
      const body = await res.json();
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
      endpoint,
      JSON.stringify(menuItem.childMenus),
      displayStatus,
    );
  }

  const handleMenuItems = (res) => {
    if (res) {
      const newMenuItems = { ...menuItem };
      newMenuItems.childMenus = res;
      setMenuItem(newMenuItems);
    }
  };

  useEffect(() => {
    componentMounted.current = true;
    getFromOpenElisServer(endpoint, handleMenuItems);
    return () => {
      componentMounted.current = false;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [endpoint]);

  return (
    <>
      {notificationVisible === true ? <AlertDialog /> : ""}
      {loading && <Loading />}
      <div className="orderLegendBody">
        <PageBreadCrumb breadcrumbs={breadcrumbs} />
        <Grid fullWidth={true}>
          <Column lg={16}>
            <Section>
              <Heading>
                <FormattedMessage id={titleKey} />
              </Heading>
            </Section>
            <Section>
              <Form onSubmit={handleSubmit}>
                <br />
                <Toggle
                  id={toggleId}
                  labelText={intl.formatMessage({ id: "label.showChildren" })}
                  size="md"
                  toggled={showChildren}
                  onToggle={() => setShowChildren(!showChildren)}
                />
                <br />
                <br />
                <MenuCheckBox
                  menuItem={menuItem}
                  curMenuItem={menuItem}
                  path="$"
                  setMenuItem={setMenuItem}
                  labelKey="menu.sidenav.active"
                  recurse={showChildren}
                />
                <br />
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

export default MenuManagementForm;
