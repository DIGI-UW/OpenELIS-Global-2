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
import { MenuCheckBox } from "./MenuUtil";
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
    label: "sidenav.label.admin.menu.study",
    link: "/MasterListsPage#studyMenuManagement",
  },
];

function StudyMenuManagement() {
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  const intl = useIntl();

  const componentMounted = useRef(false);

  const [loading, setLoading] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [showChildren, setShowChildren] = useState(false);
  const [menuItem, setMenuItem] = useState({
    menu: { isActive: false, elementId: "menu_study" },
    childMenus: [
      {
        menu: { isActive: false, elementId: "menu_sample_create" },
        childMenus: [],
      },
      {
        menu: { isActive: false, elementId: "menu_reports_study" },
        childMenus: [],
      },
      {
        menu: { isActive: false, elementId: "menu_reports_patients" },
        childMenus: [],
      },
      {
        menu: { isActive: false, elementId: "menu_reports_arv" },
        childMenus: [],
      },
      {
        menu: { isActive: false, elementId: "menu_reports_eid" },
        childMenus: [],
      },
      {
        menu: { isActive: false, elementId: "menu_reports_indeterminate" },
        childMenus: [],
      },
      {
        menu: { isActive: false, elementId: "menu_reports_indicator" },
        childMenus: [],
      },
      {
        menu: {
          isActive: false,
          elementId: "menu_reports_nonconformity.study",
        },
        childMenus: [],
      },
      {
        menu: { isActive: false, elementId: "menu_resultvalidation_study" },
        childMenus: [],
      },
      {
        menu: { isActive: false, elementId: "menu_reports_vl" },
        childMenus: [],
      },
      {
        menu: { isActive: false, elementId: "menu_resultvalidation_virology" },
        childMenus: [],
      },
    ],
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
      // setMenuItems(body); unpack response
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
      "/rest/menu/",
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
  };

  const handleMenuItems = (res, tag) => {
    if (res) {
      let newMenuItems = { ...menuItem };
      //this handles the fact that the root object is not a real menu
      newMenuItems.childMenus[findIndex(newMenuItems.childMenus, tag)] = res;
      setMenuItem(newMenuItems);
    }
  };

  useEffect(() => {
    componentMounted.current = true;
    getFromOpenElisServer("/rest/menu/menu_sample_create", (res) => {
      if (res) {
        let newMenuItems = { ...menuItem };
        //this handles the fact that the root object is not a real menu
        newMenuItems.menu.isActive = res.menu.isActive;
        setMenuItem(newMenuItems);
      }
      handleMenuItems(res, "menu_sample_create");
    });
    getFromOpenElisServer("/rest/menu/menu_reports_study", (res) => {
      handleMenuItems(res, "menu_reports_study");
    });
    getFromOpenElisServer("/rest/menu/menu_reports_patients", (res) => {
      handleMenuItems(res, "menu_reports_patients");
    });
    getFromOpenElisServer("/rest/menu/menu_reports_arv", (res) => {
      handleMenuItems(res, "menu_reports_arv");
    });
    getFromOpenElisServer("/rest/menu/menu_reports_eid", (res) => {
      handleMenuItems(res, "menu_reports_eid");
    });
    getFromOpenElisServer("/rest/menu/menu_reports_indeterminate", (res) => {
      handleMenuItems(res, "menu_reports_indeterminate");
    });
    getFromOpenElisServer("/rest/menu/menu_reports_indicator", (res) => {
      handleMenuItems(res, "menu_reports_indicator");
    });
    getFromOpenElisServer(
      "/rest/menu/menu_reports_nonconformity.study",
      (res) => {
        handleMenuItems(res, "menu_reports_nonconformity.study");
      },
    );
    getFromOpenElisServer("/rest/menu/menu_patient_create", (res) => {
      handleMenuItems(res, "menu_patient_create");
    });
    getFromOpenElisServer("/rest/menu/menu_resultvalidation_study", (res) => {
      handleMenuItems(res, "menu_resultvalidation_study");
    });
    getFromOpenElisServer("/rest/menu/menu_reports_vl", (res) => {
      handleMenuItems(res, "menu_reports_vl");
    });
    getFromOpenElisServer(
      "/rest/menu/menu_resultvalidation_virology",
      (res) => {
        handleMenuItems(res, "menu_resultvalidation_virology");
      },
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
                <FormattedMessage id="menu.study.title" />
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
                  labelKey="menu.study.active"
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

export default StudyMenuManagement;
