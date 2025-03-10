import React, { useContext, useState, useEffect, useRef } from "react";
import {
  Heading,
  Button,
  Grid,
  Column,
  Section,
  Loading,
  Modal,
  TextInput,
} from "@carbon/react";
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

let breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "breadcrums.admin.managment", link: "/MasterListsPage" },
  {
    label: "master.lists.page.test.management",
    link: "/MasterListsPage#testManagementConfigMenu",
  },
  {
    label: "configuration.selectList.rename",
    link: "/MasterListsPage#SelectListRenameEntry",
  },
];

function SelectListRenameEntry() {
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  const intl = useIntl();

  const componentMounted = useRef(false);
  const modalHeading = intl.formatMessage({
    id: "selectListRenameEntry.selectListEdit",
  });

  const [isLoading, setIsLoading] = useState(false);
  const [finished, setFinished] = useState(true);
  const [isAddModalOpen, setIsAddModalOpen] = useState(false);
  const [confirmationStep, setConfirmationStep] = useState(false);
  const [inputError, setInputError] = useState(false);
  const [selectListRename, setSelectListRename] = useState({});
  const [selectListRenameListShow, setSelectListRenameListShow] = useState([]);
  const [displayValueList, setDisplayValueList] = useState([]);
  const [selectListRenamePost, setSelectListRenamePost] = useState({});
  const [selectedItem, setSelectedItem] = useState({});
  const [selectedItemChange, setSelectedItemChange] = useState({});
  const [selectedIndex, setSelectedIndex] = useState(null);

  useEffect(() => {
    componentMounted.current = true;
    getFromOpenElisServer(
      "/rest/SelectListRenameEntry",
      handleSelectListRename,
    );
    return () => {
      componentMounted.current = false;
    };
  }, []);

  const handleSelectListRename = (res) => {
    if (!res) {
      setIsLoading(true);
    } else {
      setSelectListRename(res);
      setSelectListRenamePost(res);
      setSelectListRenameListShow(res.resultSelectOptionList);
    }
  };

  function selectListRenameUpdatePost() {
    setIsLoading(true);
    if (confirmationStep) {
      postToOpenElisServerJsonResponse(
        `/rest/SelectListRenameEntry`,
        JSON.stringify(selectListRenamePost),
        (res) => {
          selectListRenameUpdatePostCallback(res);
        },
      );
    } else {
      setConfirmationStep(true);
    }
  }

  function selectListRenameUpdatePostCallback(res) {
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

  const openAppModle = (item, index) => {
    setConfirmationStep(false);
    setIsAddModalOpen(true);
    setSelectedItem(item);
    setSelectedItemChange(item);
    setSelectedIndex(index);
  };

  const onInputChangeEn = (e) => {
    e.preventDefault();
    const updatedValue = e.target.value;
    setDisplayValueList((prevList) =>
      prevList.map((item, i) =>
        i === selectedIndex
          ? { ...item, displayValueEnglish: updatedValue }
          : item,
      ),
    );
    // setSelectedItem((prev) => ({ ...prev, displayValueEnglish: updatedValue }));
    setSelectedItemChange((prev) => ({
      ...prev,
      displayValueEnglish: updatedValue,
    }));
    setInputError(false);
  };

  const onInputChangeFr = (e) => {
    e.preventDefault();
    const updatedValue = e.target.value;
    setDisplayValueList((prevList) =>
      prevList.map((item, i) =>
        i === selectedIndex
          ? { ...item, displayValueFrench: updatedValue }
          : item,
      ),
    );
    // setSelectedItem((prev) => ({ ...prev, displayValueFrench: updatedValue }));
    setSelectedItemChange((prev) => ({
      ...prev,
      displayValueFrench: updatedValue,
    }));
    setInputError(false);
  };

  useEffect(() => {
    if (selectedItemChange) {
      setSelectListRenamePost((prev) => ({
        ...prev,
        resultSelectOptionId: selectedItemChange.id,
        nameEnglish: selectedItemChange.displayValueEnglish,
        nameFrench: selectedItemChange.displayValueFrench,
      }));
    }
  }, [selectedItemChange]);

  const closeAddModal = () => {
    setIsAddModalOpen(false);
  };

  useEffect(() => {
    if (selectListRenameListShow && selectListRenameListShow.length > 0) {
      const extractedValues = selectListRenameListShow.map((item) => ({
        id: item.id,
        displayValueEnglish: item.displayValue,
        displayValueFrench: item.displayValue,
      }));
      setDisplayValueList(extractedValues);
    }
  }, [selectListRenameListShow]);

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
                  <FormattedMessage id="configuration.selectList.rename" />
                </Heading>
              </Section>
            </Column>
          </Grid>
          <br />
          <hr />
          <br />
          {displayValueList ? (
            <Grid fullWidth={true}>
              {displayValueList.map((valueItem, index) => (
                <Column key={index} lg={5} md={5} sm={5}>
                  <Button
                    id={`button-${index}`}
                    kind="ghost"
                    type="button"
                    onClick={() => {
                      openAppModle(valueItem, index);
                    }}
                    style={{ color: "#000000" }}
                  >
                    {valueItem.displayValueEnglish}
                  </Button>
                </Column>
              ))}
              <Modal
                open={isAddModalOpen}
                size="md"
                modalHeading={`${modalHeading} : ${selectedItem?.displayValueEnglish}`} // secondary lable
                primaryButtonText={
                  confirmationStep ? (
                    <>
                      <FormattedMessage id="column.name.accept" />
                    </>
                  ) : (
                    <>
                      <FormattedMessage id="column.name.save" />
                    </>
                  )
                }
                secondaryButtonText={
                  confirmationStep ? (
                    <>
                      <FormattedMessage id="header.reject" />
                    </>
                  ) : (
                    <>
                      <FormattedMessage id="label.button.cancel" />
                    </>
                  )
                }
                onRequestSubmit={selectListRenameUpdatePost}
                onRequestClose={closeAddModal}
              >
                {displayValueList &&
                selectedItem &&
                selectedItem.id &&
                selectedItem.displayValueEnglish &&
                selectedItem.displayValueFrench ? (
                  <Grid fullWidth={true}>
                    <Column lg={16} md={8} sm={4}>
                      <Section>
                        <Section>
                          <Heading>
                            <FormattedMessage id="banner.menu.patientEdit" />
                          </Heading>
                        </Section>
                      </Section>
                      <br />
                      <Section>
                        <Section>
                          <Section>
                            <Heading>
                              <FormattedMessage id="selectListRenameEntry.selectList" />
                            </Heading>
                          </Section>
                        </Section>
                      </Section>
                      <br />
                      <>
                        <FormattedMessage id="english.current" /> :{" "}
                        {selectedItem.displayValueEnglish}
                      </>
                      <br />
                      <br />
                      <TextInput
                        id={`eng`}
                        labelText=""
                        hideLabel
                        value={selectedItemChange.displayValueEnglish || ""}
                        onChange={(e) => {
                          onInputChangeEn(e);
                        }}
                        required
                        invalid={inputError}
                        invalidText={
                          <FormattedMessage id="required.invalidtext" />
                        }
                      />
                      <br />
                      {/* <>
                        <FormattedMessage id="french.current" /> :{" "}
                        {selectedItem.displayValueFrench}
                      </>
                      <br />
                      <br />
                      <TextInput
                        id={`fr`}
                        labelText=""
                        hideLabel
                        value={selectedItemChange.displayValueFrench || ""}
                        onChange={(e) => {
                          onInputChangeFr(e);
                        }}
                        required
                        invalid={inputError}
                        invalidText={
                          <FormattedMessage id="required.invalidtext" />
                        }
                      /> */}
                    </Column>
                  </Grid>
                ) : (
                  <>
                    <div>
                      <Loading />
                    </div>
                  </>
                )}
                <br />
                {confirmationStep && (
                  <>
                    <Section>
                      <Section>
                        <Section>
                          <Heading>
                            <FormattedMessage id="confirmation.rename" />
                          </Heading>
                        </Section>
                      </Section>
                    </Section>
                  </>
                )}
              </Modal>
            </Grid>
          ) : (
            <>
              <Loading active={isLoading} />
            </>
          )}
        </div>
      </div>
    </>
  );
}

export default injectIntl(SelectListRenameEntry);
