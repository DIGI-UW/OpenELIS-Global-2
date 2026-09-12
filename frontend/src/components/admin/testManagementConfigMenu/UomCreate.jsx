import React, { useContext, useState } from "react";
import {
  Heading,
  Button,
  Grid,
  Column,
  Section,
  TextInput,
  Modal,
} from "@carbon/react";
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
import { useHistory } from "react-router-dom";
import ServerDataState from "../../utils/ServerDataState";

const UOM_CREATE_ENDPOINT = "/rest/UomCreate";

let breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "breadcrums.admin.managment", link: "/MasterListsPage" },
  {
    label: "master.lists.page.test.management",
    link: "/MasterListsPage/testManagementConfigMenu",
  },
  {
    label: "configuration.uom.manage",
    link: "/MasterListsPage/UomManagement",
  },
  {
    label: "configuration.uom.create",
    link: "/MasterListsPage/UomCreate",
  },
];

function UomCreate() {
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  const intl = useIntl();

  const history = useHistory();
  const [saveButton, setSaveButton] = useState(true);
  const [isConfirmModalOpen, setIsConfirmModalOpen] = useState(false);
  const [uomNew, setUomNew] = useState("");
  const [inputError, setInputError] = useState(false);

  const uomQuery = useServerData(UOM_CREATE_ENDPOINT);
  const { data: uomRes } = uomQuery;
  const invalidateServerData = useInvalidateServerData();

  // The names a new one may not collide with, from the same read the screen
  // shows, so a name created here is checked against it once it is reread.
  const allUomValues = [
    ...(uomRes?.existingUomList?.map((uom) => uom.value.toLowerCase()) || []),
    ...(uomRes?.inactiveUomList?.map((uom) => uom.value.toLowerCase()) || []),
    ...(uomRes?.existingEnglishNames || "")
      .split("$")
      .map((name) => name.toLowerCase()),
  ];

  function handleUomCreatePostResponse() {
    postToOpenElisServerJsonResponse(
      `/rest/UomCreate`,
      JSON.stringify({
        uomEnglishName: uomNew,
      }),
      (data) => {
        handleUomCreatePostResponseCallBack(data);
      },
    );
  }

  const handleUomCreatePostResponseCallBack = (res) => {
    if (!res) {
      setNotificationVisible(true);
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notification.title",
        }),
        message: intl.formatMessage({
          id: "server.error.msg",
        }),
      });
      return;
    }
    setNotificationVisible(true);
    addNotification({
      kind: NotificationKinds.success,
      title: intl.formatMessage({
        id: "notification.title",
      }),
      message: intl.formatMessage({
        id: "uom.notification.save",
      }),
    });
    setUomNew("");
    setSaveButton(true);
    setInputError(false);
    invalidateServerData();
  };

  if (!uomRes) return <ServerDataState query={uomQuery} />;

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
                      <FormattedMessage id="configuration.uom.create" />
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
              <TextInput
                id="uomNew"
                name="uomNew"
                labelText={intl.formatMessage({
                  id: "uom.new",
                })}
                value={uomNew}
                onChange={(e) => {
                  const inputValue = e.target.value.toLowerCase();

                  if (allUomValues.includes(inputValue)) {
                    setInputError(true);
                    setSaveButton(true);
                    setNotificationVisible(true);
                    addNotification({
                      kind: NotificationKinds.error,
                      title: intl.formatMessage({
                        id: "notification.title",
                      }),
                      message: intl.formatMessage({
                        id: "uom.notification.duplicate",
                      }),
                    });
                  } else {
                    setSaveButton(false);
                    setInputError(false);
                  }
                  setUomNew(e.target.value);
                }}
                required
                inputError={inputError}
                invalidText="This field should be unique."
              />
            </Column>
          </Grid>
          <br />
          <Grid fullWidth={true}>
            <Column lg={16} md={8} sm={4}>
              <Button
                disabled={saveButton}
                onClick={() => {
                  setIsConfirmModalOpen(true);
                }}
                type="button"
              >
                <FormattedMessage id="next.action.button" />
              </Button>{" "}
              <Button
                onClick={() => history.push("/MasterListsPage/UomManagement")}
                kind="tertiary"
                type="button"
              >
                <FormattedMessage id="label.button.previous" />
              </Button>
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
                      <FormattedMessage id="uom.existing" />
                    </Heading>
                  </Section>
                </Section>
              </Section>
            </Column>
          </Grid>
          <br />
          <Grid fullWidth={true}>
            {uomRes && uomRes?.existingUomList ? (
              uomRes?.existingUomList.map((uom, index) => {
                return (
                  <Column lg={4} md={4} sm={4} key={index}>
                    <h6>{uom.value}</h6>
                  </Column>
                );
              })
            ) : (
              <></>
            )}
          </Grid>
        </div>
      </div>

      <Modal
        open={isConfirmModalOpen}
        size="md"
        modalHeading={<FormattedMessage id="uom.create.heading.confirmation" />}
        primaryButtonText={<FormattedMessage id="column.name.accept" />}
        secondaryButtonText={<FormattedMessage id="reject.action.button" />}
        onRequestSubmit={() => {
          setIsConfirmModalOpen(false);
          handleUomCreatePostResponse();
        }}
        onRequestClose={() => {
          setIsConfirmModalOpen(false);
        }}
      >
        <Grid fullWidth={true}>
          <Column lg={16} md={8} sm={4}>
            <Section>
              <Section>
                <Section>
                  <Section>
                    <Heading>
                      <FormattedMessage id="uom.new.added" />
                    </Heading>
                  </Section>
                </Section>
              </Section>
            </Section>
            <br />
            {uomNew ? (
              <>
                <div>{uomNew}</div>
              </>
            ) : (
              <></>
            )}
          </Column>
        </Grid>
      </Modal>
    </>
  );
}

export default injectIntl(UomCreate);
