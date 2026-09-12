import React, { useContext, useState } from "react";
import { Heading, Grid, Column, Section } from "@carbon/react";
import { postToOpenElisServerJsonResponse } from "../../utils/Utils";
import {
  useInvalidateServerData,
  useServerData,
} from "../../utils/useServerData";
import { NotificationContext } from "../../layout/contexts";
import {
  AlertDialog,
  NotificationKinds,
} from "../../common/CustomNotification";
import { FormattedMessage, injectIntl, useIntl } from "react-intl";
import PageBreadCrumb from "../../common/PageBreadCrumb";
import RenameModelBox from "./renameModel/RenameModelBox";

const UOM_ENDPOINT = "/rest/UomRenameEntry";
const EMPTY_NAMES = { name: { english: "", french: "" } };

let breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "breadcrums.admin.managment", link: "/MasterListsPage" },
  {
    label: "master.lists.page.test.management",
    link: "/MasterListsPage/testManagementConfigMenu",
  },
  {
    label: "configuration.uom.rename",
    link: "/MasterListsPage/UomRenameEntry",
  },
];

function UomRenameEntry() {
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  const intl = useIntl();

  const modalHeading = intl.formatMessage({
    id: "uom.browse.title",
  });

  const [isLoading, setIsLoading] = useState(false);
  const [isAddModalOpen, setIsAddModalOpen] = useState(false);
  const [confirmationStep, setConfirmationStep] = useState(false);
  const [inputError, setInputError] = useState(false);
  const [nameEdits, setNameEdits] = useState(null);
  const [entityId, setEntityId] = useState();
  const entityName = "unitOfMeasure";
  const [selectedItem, setSelectedItem] = useState({});

  const invalidateServerData = useInvalidateServerData();
  const { data: uomRename } = useServerData(UOM_ENDPOINT);
  const uomListShow = uomRename?.uomList ?? [];

  // Held off until an entry is picked and keyed on it, so picking another
  // entry is a second read rather than a hand-written refetch. Until that
  // read arrives the hook still serves the entry picked before, so the names
  // offered for editing are the empty ones rather than the previous entry's.
  const { data: readNames, isPreviousData } = useServerData(
    entityId && entityName
      ? `/rest/EntityNamesProvider?entityId=${entityId}&entityName=${entityName}`
      : null,
  );
  const entityNamesProvider =
    isPreviousData || !readNames ? EMPTY_NAMES : readNames;

  // Only the edits live in state, on top of what the server holds, so
  // clearing them is all it takes to show the stored names again.
  const entityNamesProviderPost = nameEdits ?? entityNamesProvider;

  const uomRenamePost = {
    ...uomRename,
    ...(entityId
      ? {
          uomId: entityId,
          nameEnglish: entityNamesProviderPost?.name?.english,
          nameFrench: entityNamesProviderPost?.name?.french,
        }
      : {}),
  };

  function uomRenameUpdatePost() {
    setIsLoading(true);
    if (confirmationStep) {
      postToOpenElisServerJsonResponse(
        `/rest/UomRenameEntry`,
        JSON.stringify(uomRenamePost),
        (res) => {
          uomRenameUpdatePostCallback(res);
        },
      );
    } else {
      setConfirmationStep(true);
    }
  }

  function uomRenameUpdatePostCallback(res) {
    setIsLoading(false);
    if (res) {
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
      setConfirmationStep(false);
      setEntityId(undefined);
      setNameEdits(null);
      invalidateServerData();
    } else {
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "server.error.msg" }),
      });
      setNotificationVisible(true);
    }
  }

  const openAppModle = (item) => {
    setConfirmationStep(false);
    setIsAddModalOpen(true);
    setEntityId(item.id);
    setNameEdits(null);
    setSelectedItem(item);
  };

  const onInputChangeEn = (e) => {
    e.preventDefault();
    const englishName = e.target.value;
    setNameEdits({
      name: { ...entityNamesProviderPost.name, english: englishName },
    });
    setInputError(false);
  };

  const onInputChangeFr = (e) => {
    e.preventDefault();
    const frenchName = e.target.value;
    setNameEdits({
      name: { ...entityNamesProviderPost.name, french: frenchName },
    });
    setInputError(false);
  };

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
                  <FormattedMessage id="configuration.uom.rename" />
                </Heading>
              </Section>
            </Column>
          </Grid>
          <br />
          <hr />
          <br />
          <RenameModelBox
            data={uomListShow}
            isModalOpen={isAddModalOpen}
            openModel={openAppModle}
            closeModel={closeAddModal}
            onSubmit={uomRenameUpdatePost}
            onInputChangeEn={onInputChangeEn}
            onInputChangeFr={onInputChangeFr}
            isLoading={isLoading}
            modalHeading={modalHeading}
            heading="banner.menu.patientEdit"
            mainLabel="uom.uomName"
            confirmationStep={confirmationStep}
            inputError={inputError}
            lang={entityNamesProvider}
            langPost={entityNamesProviderPost}
            selectedItem={selectedItem}
            hasFrench={false}
          />
        </div>
      </div>
    </>
  );
}

export default injectIntl(UomRenameEntry);
