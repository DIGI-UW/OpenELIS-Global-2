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
import { Download } from "@carbon/react/icons";
import { useContext, useEffect, useRef, useState } from "react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  AlertDialog,
  NotificationKinds,
} from "../../common/CustomNotification";
import PageBreadCrumb from "../../common/PageBreadCrumb.js";
import { NotificationContext } from "../../layout/Layout";
import {
  getFromOpenElisServer,
  postToOpenElisServerFullResponse,
} from "../../utils/Utils";
import { MenuCheckBox } from "./MenuUtil";

let breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "breadcrums.admin.managment", link: "/MasterListsPage" },
  {
    label: "Global Menu Management",
    link: "/MasterListsPage/globalMenuManagement",
  },
];
function GlobalMenuManagement() {
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  const intl = useIntl();

  const componentMounted = useRef(false);

  const [loading, setLoading] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isExporting, setIsExporting] = useState(false);
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

  const convertToConfigFormat = (menuItems) => {
    return {
      includes: menuItems
        .filter((item) => item.menu.isActive)
        .map(convertMenuItem)
        .sort((a, b) => {
          const orderA =
            menuItems.find((item) => item.menu.elementId === a.elementId)?.menu
              .presentationOrder || 0;
          const orderB =
            menuItems.find((item) => item.menu.elementId === b.elementId)?.menu
              .presentationOrder || 0;
          return orderA - orderB;
        }),
    };
  };

  const convertMenuItem = (item) => {
    return {
      elementId: item.menu.elementId,
      childMenus: item.childMenus
        .filter((child) => child.menu.isActive)
        .map(convertMenuItem)
        .sort((a, b) => {
          const orderA =
            item.childMenus.find(
              (child) => child.menu.elementId === a.elementId,
            )?.menu.presentationOrder || 0;
          const orderB =
            item.childMenus.find(
              (child) => child.menu.elementId === b.elementId,
            )?.menu.presentationOrder || 0;
          return orderA - orderB;
        }),
    };
  };

  const downloadMenuConfig = (config, filename) => {
    const blob = new Blob([JSON.stringify(config, null, 2)], {
      type: "application/json",
    });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  };

  const handleExportConfig = async () => {
    try {
      setIsExporting(true);

      const config = convertToConfigFormat(menuItem.childMenus);
      const timestamp = new Date().toISOString().split("T")[0];
      const filename = `menu_config_export_${timestamp}.json`;

      downloadMenuConfig(config, filename);

      addNotification({
        kind: NotificationKinds.success,
        title: intl.formatMessage({ id: "notification.title" }),
        message: `Menu configuration exported as ${filename}`,
      });
    } catch (error) {
      console.error("Error exporting menu config:", error);
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message:
          intl.formatMessage({ id: "error.export.msg" }) ||
          "Error exporting menu configuration",
      });
    } finally {
      setIsExporting(false);
    }
  };

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
          </Column>
        </Grid>

        <div className="orderLegendBody">
          <Grid fullWidth={true}>
            <Column lg={16}>
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
                  <div style={{ display: "flex", gap: "1rem" }}>
                    <Button type="submit">
                      <FormattedMessage id="label.button.submit" />
                      {isSubmitting && <Loading small={true} />}
                    </Button>
                    <Button
                      kind="secondary"
                      renderIcon={Download}
                      onClick={handleExportConfig}
                      disabled={isExporting || isSubmitting}
                    >
                      Export Config
                      {isExporting && <Loading small={true} />}
                    </Button>
                  </div>
                </Form>
              </Section>
            </Column>
          </Grid>
        </div>
      </div>
    </>
  );
}

export default GlobalMenuManagement;
