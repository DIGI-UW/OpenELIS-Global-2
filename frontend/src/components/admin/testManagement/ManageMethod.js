import React, { useState, useEffect, useContext } from "react";
import {
  Button,
  Modal,
  TextInput,
  Grid,
  Column,
  Section,
  Heading,
} from "@carbon/react";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
  postToOpenElisServerFullResponse,
  postToOpenElisServerJsonResponse,
} from "../../utils/Utils.js";
import { NotificationContext } from "../../layout/Layout.js";
import { FormattedMessage, injectIntl, useIntl } from "react-intl";
import {
  AlertDialog,
  NotificationKinds,
} from "../../common/CustomNotification.js";
import PageBreadCrumb from "../../common/PageBreadCrumb.js";

let breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "breadcrums.admin.managment", link: "/MasterListsPage" },
  {
    label: "sidenav.label.admin.testmgt.ManageMethod",
    link: "/MasterListsPage#MethodManagment",
  },
];

function ManageMethod() {
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);
  const intl = useIntl();

  const [isAddModalOpen, setIsAddModalOpen] = useState(false);
  const [englishLabel, setEnglishLabel] = useState("");
  const [frenchLabel, setFrenchLabel] = useState("");
  const [confirmationStep, setConfirmationStep] = useState(false);
  const [inputError, setInputError] = useState(false);
  const [existingMethods, setExistingMethods] = useState([]);
  const [inactiveMethods, setInactiveMethods] = useState([]);

  useEffect(() => {
    getFromOpenElisServer("/rest/MethodCreate", handleMethods);
  }, []);

  const handleMethods = (res) => {
    setExistingMethods(res.existingMethodList);
    setInactiveMethods(res.inactiveMethodList);
  };

  const openAddModal = () => {
    setEnglishLabel("");
    setFrenchLabel("");
    setConfirmationStep(false);
    setInputError(false);
    setIsAddModalOpen(true);
  };

  const closeAddModal = () => {
    setIsAddModalOpen(false);
  };

  const displayStatus = (res) => {
    setNotificationVisible(true);
    if (res.status === 201 || res.status === 200) {
      addNotification({
        kind: NotificationKinds.success,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "save.config.success.msg" }),
      });
    } else {
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "server.error.msg" }),
      });
    }
  };

  const handleAddMethod = () => {
    if (!englishLabel || !frenchLabel) {
      setInputError(true);
      return;
    }
    if (confirmationStep) {
      const newMethod = {
        methodEnglishName: englishLabel,
        methodFrenchName: frenchLabel,
      };
      postToOpenElisServerFullResponse(
        "/rest/MethodCreate",
        JSON.stringify(newMethod),
        displayStatus,
      );
      closeAddModal();
    } else {
      setConfirmationStep(true);
    }
  };

  return (
    <>
      {notificationVisible === true ? <AlertDialog /> : ""}
      <div className="adminPageContent">
        <PageBreadCrumb breadcrumbs={breadcrumbs} />
        <Grid fullWidth={true}>
          <Column lg={16} md={8} sm={4}>
            <Section>
              <Heading>
                <FormattedMessage id="sidenav.label.admin.testmgt.ManageMethod" />
              </Heading>
              <br />
              <Button onClick={openAddModal}>
                {" "}
                <FormattedMessage id="modal.add.method" />
              </Button>
            </Section>
          </Column>
        </Grid>
        <Grid>
        <Modal
          open={isAddModalOpen}
          size="sm"
          modalHeading="Add New Method"
          primaryButtonText={confirmationStep ? "Accept" : "Save"}
          secondaryButtonText={confirmationStep ? "Reject" : "Cancel"}
          onRequestSubmit={handleAddMethod}
          onRequestClose={closeAddModal}
        >
        <Column lg={4} md={4} sm={4} >
          <TextInput
            id="englishLabel"
            labelText="English"
            value={englishLabel}
            onChange={(e) => {
              setEnglishLabel(e.target.value);
              setInputError(false);
            }}
            required
            invalid={inputError && !englishLabel}
            invalidText="This field is required"
          />
          </Column>
          <Column lg={4} md={4} sm={4}  >
          <TextInput
            id="frenchLabel"
            labelText="French"
            value={frenchLabel}
            onChange={(e) => {
              setFrenchLabel(e.target.value);
              setInputError(false);
            }}
            required
            invalid={inputError && !frenchLabel}
            invalidText="This field is required"
          />
          </Column>
          {confirmationStep && (
            <p style={{ color: "#3366B3", marginTop: "1rem" }}>
              <FormattedMessage id="message.method.activation" />
            </p>
          )}
        </Modal>
</Grid>
        <div className="orderLegendBody">
          <h4 style={{ color: "#3366B3" }}>
            <FormattedMessage id="label.existing.methods" />
          </h4>
          < Grid style={{marginz:"5px"}} >
            {existingMethods.map((method) => (
              <Column lg={5} md={4} sm={4}
                key={method.id}
               
              >
                {method.value}
              </Column>
            ))}
          </Grid>
          <hr />
          <h4 style={{ color: "#3366B3" }}>
            <FormattedMessage id="label.inactive.methods" />
          </h4>
          <Grid style={{  marginTop: "1rem" }}>
            {inactiveMethods.map((method) => (
              <Column lg={8} md={4} sm={4}
                key={method.id}
                style={{ width: "25%", padding: "0.5rem 0" }}
              >
                {method.value}
              </Column>
            ))}
          </Grid>
        </div>
      </div>
    </>
  );
}

export default injectIntl(ManageMethod);
