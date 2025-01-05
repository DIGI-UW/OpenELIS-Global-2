import React, { useState, useEffect, useContext } from "react";
import {
  Grid,
  Column,
  Section,
  Heading,
  RadioButtonGroup,
  RadioButton,
  TextInput,
  Button,
  Loading,
  Dropdown,
  FileUploader,
  Checkbox,
} from "@carbon/react";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
  postToOpenElisServerFormData,
  postToOpenElisServerFullResponse,
} from "../../../utils/Utils.js";
import {
  AlertDialog,
  NotificationKinds,
} from "../../../common/CustomNotification.js";
import config from "../../../../config.json";
import { NotificationContext } from "../../../layout/Layout.js";

import { FormattedMessage, useIntl } from "react-intl";

const GenericConfigEdit = ({ menuType, ID }) => {
  const intl = useIntl();

  const [FormEntryConfig, setFormEntryConfig] = useState(null);
  const [radioValue, setRadioValue] = useState("");
  const [textInputEnglishValue, setTextInputEnglishValue] = useState("");
  const [textInputFrenchValue, setTextInputFrenchValue] = useState("");
  const [textInputValue, setTextInputValue] = useState("");
  const [selectedDictionaryValue, setSelectedDictionaryValue] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [removeImage, setRemoveImage] = useState(false);

  const [img, setImg] = useState(null);
  const [file, setFile] = useState(null);

  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  useEffect(() => {
    setIsLoading(true);
    getFromOpenElisServer(`/rest/${menuType}?ID=${ID}`, handleMenuItems);
  }, [menuType, ID]);

  const handleMenuItems = (res) => {
    setFormEntryConfig(res);
    if (res.localization) {
      setTextInputEnglishValue(res.localization.localeValues.en);
      setTextInputFrenchValue(res.localization.localeValues.fr);
    }
    if (res.valueType === "boolean") {
      setRadioValue(res.value);
    }
    if (res.valueType === "dictionary") {
      setSelectedDictionaryValue(res.value);
    }
    if (res.valueType === "logoUpload") {
      getFromOpenElisServer(
        `/dbImage/siteInformation/${res.paramName}`,
        (res) => {
          setImg(res.value);
        },
      );
    }
    setTextInputValue(res.value);
    setIsLoading(false);
  };

  const updateFormEntryConfig = (newState) => {
    setFormEntryConfig((prevState) => ({
      ...prevState,
      ...newState,
    }));
  };

  const handleRadioChange = (value) => {
    setRadioValue(value);
    updateFormEntryConfig({ value });
  };

  const handleInputChange = (event) => {
    const newValue = event.target.value;
    setTextInputValue(newValue);
    updateFormEntryConfig({ value: newValue });
  };

  const handleInputEnglishChange = (event) => {
    const newValue = event.target.value;
    setTextInputEnglishValue(newValue);
    updateFormEntryConfig({
      localization: {
        id: FormEntryConfig.localization.id,
        description: FormEntryConfig.localization.description,
        localeValues: {
          ...FormEntryConfig.localization.localeValues,
          en: newValue,
        },
      },
    });
  };

  const handleInputFrenchChange = (event) => {
    const newValue = event.target.value;
    setTextInputFrenchValue(newValue);
    updateFormEntryConfig({
      localization: {
        id: FormEntryConfig.localization.id,
        description: FormEntryConfig.localization.description,
        localeValues: {
          ...FormEntryConfig.localization.localeValues,
          fr: newValue,
        },
      },
    });
  };

  const handleDictionaryChange = (event) => {
    const newValue = event.selectedItem;
    setSelectedDictionaryValue(newValue);
    updateFormEntryConfig({ value: newValue });
  };

  const showAlertMessage = (msg, kind) => {
    setNotificationVisible(true);
    addNotification({
      kind: kind,
      title: intl.formatMessage({ id: "notification.title" }),
      message: msg,
    });
  };

  const handleSubmitButton = () => {
    if (FormEntryConfig.valueType === "logoUpload") {
      const formData = new FormData();
      if (!removeImage) {
        formData.append("logoFile", file);
      }
      formData.append("logoName", FormEntryConfig.paramName);
      formData.append("removeImage", removeImage ? "true" : "false");

      postToOpenElisServerFormData(`/rest/logoUpload`, formData, handleSubmit);
    } else {
      const body = JSON.stringify(FormEntryConfig);
      postToOpenElisServer(`/rest/${menuType}?ID=${ID}`, body, handleSubmit);
    }
  };

  const handleFileUpload = (event) => {
    const files = event.target.files;

    const file = files[0];
    setFile(file);

    const reader = new FileReader();

    reader.onloadend = () => {
      const base64String = reader.result;
      setImg(base64String);
    };

    reader.readAsDataURL(file);
  };

  const handleSubmit = (status) => {
    if (status === 200) {
      showAlertMessage(
        intl.formatMessage({ id: "save.config.success.msg" }),
        NotificationKinds.success,
      );
      window.location.reload();
    } else {
      showAlertMessage(
        intl.formatMessage({ id: "server.error.msg" }),
        NotificationKinds.error,
      );
    }
  };

  return (
    <div className="adminPageContent">
      {isLoading && (
        <Loading
          description={intl.formatMessage({ id: "loading.description" })}
        />
      )}
      {notificationVisible === true ? <AlertDialog /> : ""}
      {FormEntryConfig && (
        <>
          <Grid>
            <Column lg={16} md={8} sm={4}>
              <Section>
                <Heading>
                  <FormattedMessage id="admin.page.configuration.formEntryConfigMenu.editRecord" />
                </Heading>
              </Section>
              <br />
            </Column>
          </Grid>
          <div className="orderLegendBody">
            <div className="gridBoundary">
              <Grid fullWidth={true}>
                <Column lg={3}>
                  <h4>
                    <FormattedMessage id="admin.page.configuration.formEntryConfigMenu.name" />
                  </h4>
                </Column>
                <Column lg={3}>{FormEntryConfig.paramName}</Column>
              </Grid>
              <br />
              <Grid fullWidth={true}>
                <Column lg={3}>
                  <h4>
                    <FormattedMessage id="admin.page.configuration.formEntryConfigMenu.description" />
                  </h4>
                </Column>
                <Column lg={7}>{FormEntryConfig.description}</Column>
              </Grid>
              <br />
              {FormEntryConfig.valueType === "boolean" && (
                <Grid fullWidth={true}>
                  <Column lg={3}>
                    <h4>
                      <FormattedMessage id="admin.page.configuration.formEntryConfigMenu.value" />
                    </h4>
                  </Column>
                  <Column lg={5}>
                    <RadioButtonGroup
                      name="radioValue"
                      valueSelected={radioValue}
                      onChange={handleRadioChange}
                    >
                      <RadioButton
                        labelText={intl.formatMessage({ id: "true.label" })}
                        value="true"
                        id="radio-1"
                      />
                      <RadioButton
                        labelText={intl.formatMessage({ id: "false.label" })}
                        value="false"
                        id="radio-2"
                      />
                    </RadioButtonGroup>
                  </Column>
                </Grid>
              )}
              {FormEntryConfig.valueType === "dictionary" && (
                <>
                  <Grid fullWidth={true}>
                    <Column lg={3}>
                      <h4>
                        <FormattedMessage id="admin.page.configuration.formEntryConfigMenu.value" />
                      </h4>
                    </Column>
                    <Column lg={3}>
                      <Dropdown
                        id="dictionaryDropdown"
                        items={FormEntryConfig.dictionaryValues}
                        selectedItem={selectedDictionaryValue}
                        onChange={handleDictionaryChange}
                      />
                    </Column>
                  </Grid>
                </>
              )}
              {FormEntryConfig.valueType === "logoUpload" && (
                <>
                  <Grid fullWidth={true}>
                    <Column lg={3}>
                      <h4>
                        <FormattedMessage id="admin.page.configuration.formEntryConfigMenu.value" />
                      </h4>
                    </Column>
                    <Column lg={3}>
                      {!removeImage && (
                        <FileUploader
                          buttonLabel="Choose file"
                          buttonKind="primary"
                          size="sm"
                          filenameStatus="edit"
                          accept={[".jpg", ".png", ".gif"]}
                          multiple={false}
                          disabled={false}
                          iconDescription="Delete file"
                          onChange={handleFileUpload}
                        />
                      )}
                    </Column>
                  </Grid>
                  <br />
                  {img ? (
                    <Grid>
                      <Column lg={3}>
                        <img
                          src={img}
                          alt="Logo"
                          style={{ maxWidth: "100px" }}
                        />
                      </Column>
                    </Grid>
                  ) : null}

                  <br />
                  <Grid>
                    <Column lg={3}>
                      <Checkbox
                        labelText={`Remove Image`}
                        id="checkbox-label-1"
                        checked={removeImage}
                        onChange={() => {
                          setRemoveImage(!removeImage);
                          setImg(null);
                        }}
                      />
                    </Column>
                  </Grid>
                </>
              )}
              {(FormEntryConfig.valueType === "text" ||
                FormEntryConfig.valueType === "freeText") && (
                <>
                  <Grid fullWidth={true}>
                    <Column lg={3}>
                      <h4>
                        <FormattedMessage id="admin.page.configuration.formEntryConfigMenu.value" />
                      </h4>
                    </Column>
                    {!FormEntryConfig.tag && (
                      <Column lg={8}>
                        <TextInput
                          id="textInput"
                          value={textInputValue}
                          onChange={handleInputChange}
                        />
                      </Column>
                    )}
                  </Grid>
                  {FormEntryConfig.tag === "localization" && (
                    <>
                      <br />
                      <Grid>
                        <Column lg={8}>
                          <TextInput
                            id="myInputEnglish"
                            labelText={<FormattedMessage id="english.label" />}
                            value={textInputEnglishValue}
                            onChange={handleInputEnglishChange}
                          />
                        </Column>
                        <Column lg={8}>
                          <TextInput
                            id="myInputFrench"
                            labelText={<FormattedMessage id="french.label" />}
                            value={textInputFrenchValue}
                            onChange={handleInputFrenchChange}
                          />
                        </Column>
                      </Grid>
                    </>
                  )}
                </>
              )}
              <br />
              <br />
              <Grid fullWidth={true}>
                <Column lg={2}>
                  <Button onClick={handleSubmitButton} disabled={isLoading}>
                    <FormattedMessage id="admin.page.configuration.formEntryConfigMenu.button.save" />
                  </Button>
                </Column>
                <Column lg={2}>
                  <Button onClick={() => window.location.reload()}>
                    <FormattedMessage id="admin.page.configuration.formEntryConfigMenu.button.exit" />
                  </Button>
                </Column>
              </Grid>
            </div>
          </div>
        </>
      )}
    </div>
  );
};

export default GenericConfigEdit;
