import React, { useContext, useState, useEffect, useRef } from "react";
import { Heading, Button, Grid, Column, Section } from "@carbon/react";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../utils/Utils.js";
import { NotificationContext } from "../../layout/Layout.js";
import {
  AlertDialog,
  NotificationKinds,
} from "../../common/CustomNotification.js";
import { FormattedMessage, injectIntl, useIntl } from "react-intl";
import PageBreadCrumb from "../../common/PageBreadCrumb.js";
import RenameModelBox from "./renameModel/RenameModelBox.js";

let breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "breadcrums.admin.managment", link: "/MasterListsPage" },
  {
    label: "master.lists.page.test.management",
    link: "/MasterListsPage#testManagementConfigMenu",
  },
  {
    label: "panel.panelName",
    link: "/MasterListsPage#PanelRenameEntry",
  },
];

function PanelRenameEntry() {
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  const intl = useIntl();
  const modalHeading = intl.formatMessage({ id: "field.panel" });

  const componentMounted = useRef(false);
  const [isLoading, setIsLoading] = useState(false);
  const [finished, setFinished] = useState(true);
  const [isAddModalOpen, setIsAddModalOpen] = useState(false);
  const [confirmationStep, setConfirmationStep] = useState(false);
  const [inputError, setInputError] = useState(false);
  const [panel, setPanel] = useState({});
  const [panelListShow, setPanelListShow] = useState([]);
  const [panelPost, setPanelPost] = useState({});
  const [entityNamesProvider, setEntityNamesProvider] = useState({
    name: { english: "", french: "" },
  });
  const [entityNamesProviderPost, setEntityNamesProviderPost] = useState({
    name: {
      english: "",
      french: "",
    },
  });
  const [entityId, setEntityId] = useState();
  const [entityName, setEntityName] = useState("panel");
  const [selectedItem, setSelectedItem] = useState({});

  useEffect(() => {
    componentMounted.current = true;
    getFromOpenElisServer("/rest/PanelRenameEntry", handelPanelRename);
    return () => {
      componentMounted.current = false;
    };
  }, []);

  const handelPanelRename = (res) => {
    if (!res) {
      setIsLoading(true);
    } else {
      setPanel(res);
      setPanelPost(res);
      setPanelListShow(res.panelList);
    }
  };

  useEffect(() => {
    if (entityId && entityName) {
      getFromOpenElisServer(
        `/rest/EntityNamesProvider?entityId=${entityId}&entityName=${entityName}`,
        handelEntityNamesProvider,
      );
    }
  }, [entityId, entityName]);

  const handelEntityNamesProvider = (res) => {
    if (!res) {
      setIsLoading(true);
    } else {
      setEntityNamesProvider(res);
      setEntityNamesProviderPost(res);
    }
  };

  function panelUpdatePost() {
    setIsLoading(true);
    if (confirmationStep) {
      postToOpenElisServerJsonResponse(
        `/rest/PanelRenameEntry`,
        JSON.stringify(panelPost),
        (res) => {
          panelUpdatePostCallback(res);
        },
      );
    } else {
      setConfirmationStep(true);
    }
  }

  function panelUpdatePostCallback(res) {
    if (res) {
      setIsLoading(false);
      setFinished(false);
      addNotification({
        title: intl.formatMessage({
          id: "notification.title",
        }),
        message: intl.formatMessage({
          id: "notification.user.post.save.success",
        }),
        kind: NotificationKinds.success,
      });
      setNotificationVisible(true);
      setIsAddModalOpen(false);
      setTimeout(() => {
        window.location.reload();
      }, 10);
    } else {
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "server.error.msg" }),
      });
      setNotificationVisible(true);
      setTimeout(() => {
        window.location.reload();
      }, 200);
    }
  }

  const openAppModle = (item) => {
    setConfirmationStep(false);
    setIsAddModalOpen(true);
    setEntityId(item.id);
    // setEntityName(test.value);
    setSelectedItem(item);
  };

  const onInputChangeEn = (e) => {
    e.preventDefault();
    const englishName = e.target.value;
    setEntityNamesProviderPost((prev) => ({
      name: {
        ...prev.name,
        english: englishName,
      },
    }));
    setInputError(false);
  };

  const onInputChangeFr = (e) => {
    e.preventDefault();
    const frenchName = e.target.value;
    setEntityNamesProviderPost((prev) => ({
      name: {
        ...prev.name,
        french: frenchName,
      },
    }));
    setInputError(false);
  };

  useEffect(() => {
    if (entityId && entityNamesProviderPost && entityNamesProviderPost.name) {
      setPanelPost((prev) => ({
        ...prev,
        panelId: entityId,
        nameEnglish: entityNamesProviderPost.name.english,
        nameFrench: entityNamesProviderPost.name.french,
      }));
    }
  }, [entityNamesProviderPost, entityId]);

  const closeAddModal = () => {
    setIsAddModalOpen(false);
  };

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
                  <FormattedMessage id="panel.panelName" />
                </Heading>
              </Section>
            </Column>
          </Grid>
          <br />
          <hr />
          <br />
          <RenameModelBox
            data={panelListShow}
            isModalOpen={isAddModalOpen}
            openModel={openAppModle}
            closeModel={closeAddModal}
            onSubmit={panelUpdatePost}
            onInputChangeEn={onInputChangeEn}
            onInputChangeFr={onInputChangeFr}
            isLoading={isLoading}
            modalHeading={modalHeading}
            heading="banner.menu.patientEdit"
            mainLabel="panel.panelName"
            confirmationStep={confirmationStep}
            inputError={inputError}
            lang={entityNamesProvider}
            langPost={entityNamesProviderPost}
            selectedItem={selectedItem}
            hasFrench={true}
          />
        </div>
      </div>
    </>
  );
}

export default injectIntl(PanelRenameEntry);
