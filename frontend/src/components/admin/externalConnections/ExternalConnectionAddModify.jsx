import React, { useContext, useState, useEffect, useRef } from "react";
import {
  Form,
  Heading,
  Button,
  Loading,
  Grid,
  Column,
  Section,
  TextInput,
  TextArea,
  Select,
  SelectItem,
  Toggle,
  PasswordInput,
} from "@carbon/react";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../utils/Utils";
import { NotificationContext } from "../../layout/Layout";
import {
  AlertDialog,
  NotificationKinds,
} from "../../common/CustomNotification";
import { FormattedMessage, injectIntl, useIntl } from "react-intl";
import { useLocation } from "react-router-dom";
import PageBreadCrumb from "../../common/PageBreadCrumb";

let breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "breadcrums.admin.managment", link: "/MasterListsPage" },
  {
    label: "externalconnections.browse.title",
    link: "/MasterListsPage/externalConnections",
  },
];

function ExternalConnectionAddModify() {
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  const intl = useIntl();
  const componentMounted = useRef(false);
  const [loading, setLoading] = useState(true);
  const [saveButton, setSaveButton] = useState(true);
  const [formData, setFormData] = useState(null);
  const [connectionName, setConnectionName] = useState("");
  const [connectionDescription, setConnectionDescription] = useState("");
  const [programmedConnection, setProgrammedConnection] = useState("");
  const [authType, setAuthType] = useState("NONE");
  const [uri, setUri] = useState("");
  const [active, setActive] = useState(true);
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [programmedConnectionOptions, setProgrammedConnectionOptions] =
    useState([]);
  const [authTypeOptions, setAuthTypeOptions] = useState([]);

  const location = useLocation();
  const ID = (() => {
    const search = location.search;
    if (search) {
      const urlParams = new URLSearchParams(search);
      return urlParams.get("ID");
    }
    return "0";
  })();

  useEffect(() => {
    componentMounted.current = true;
    setLoading(true);
    if (ID) {
      getFromOpenElisServer(
        `/rest/ExternalConnection?ID=${ID}`,
        handleFormData,
      );
    } else {
      setTimeout(() => {
        window.location.assign("/MasterListsPage/externalConnections");
      }, 1000);
    }
    return () => {
      componentMounted.current = false;
    };
  }, [ID]);

  const handleFormData = (res) => {
    if (!res) {
      setLoading(true);
    } else {
      setFormData(res);
    }
  };

  useEffect(() => {
    if (formData) {
      if (formData.programmedConnections) {
        setProgrammedConnectionOptions(formData.programmedConnections);
      }
      if (formData.authenticationTypes) {
        setAuthTypeOptions(formData.authenticationTypes);
      }

      const conn = formData.externalConnection;
      if (conn) {
        if (conn.nameLocalization && conn.nameLocalization.localizedValue) {
          setConnectionName(conn.nameLocalization.localizedValue);
        }
        if (
          conn.descriptionLocalization &&
          conn.descriptionLocalization.localizedValue
        ) {
          setConnectionDescription(conn.descriptionLocalization.localizedValue);
        }
        if (conn.programmedConnection) {
          setProgrammedConnection(conn.programmedConnection);
        }
        if (conn.activeAuthenticationType) {
          setAuthType(conn.activeAuthenticationType);
        }
        if (conn.uri) {
          setUri(conn.uri);
        }
        if (conn.active != null) {
          setActive(conn.active);
        }
      }

      const basicAuth = formData.basicAuthenticationData;
      if (basicAuth) {
        if (basicAuth.username) {
          setUsername(basicAuth.username);
        }
        if (basicAuth.password) {
          setPassword(basicAuth.password);
        }
      }
    }
  }, [formData]);

  function handleFieldChange(setter) {
    return (e) => {
      setSaveButton(false);
      setter(e.target.value);
    };
  }

  function submitForm() {
    setLoading(true);

    const existingConn = formData ? formData.externalConnection : {};
    const existingBasicAuth = formData ? formData.basicAuthenticationData : {};

    const payload = {
      externalConnection: {
        ...existingConn,
        id: ID === "0" ? null : parseInt(ID),
        active: active,
        programmedConnection: programmedConnection || null,
        activeAuthenticationType: authType || "NONE",
        uri: uri || null,
        nameLocalization: {
          ...(existingConn ? existingConn.nameLocalization : {}),
          localizedValue: connectionName,
        },
        descriptionLocalization: {
          ...(existingConn ? existingConn.descriptionLocalization : {}),
          localizedValue: connectionDescription,
        },
      },
      basicAuthenticationData:
        authType === "BASIC"
          ? {
              ...(existingBasicAuth || {}),
              username: username,
              password: password,
            }
          : null,
      externalConnectionContacts: [],
    };

    postToOpenElisServerJsonResponse(
      `/rest/ExternalConnection`,
      JSON.stringify(payload),
      submitCallback,
    );
  }

  const submitCallback = (res) => {
    setLoading(false);
    addNotification({
      title: intl.formatMessage({ id: "notification.title" }),
      message: intl.formatMessage({
        id: "externalconnections.save.success",
      }),
      kind: NotificationKinds.success,
    });
    setNotificationVisible(true);
    setTimeout(() => {
      window.location.assign("/MasterListsPage/externalConnections");
    }, 200);
  };

  if (!loading) {
    return (
      <>
        <Loading />
      </>
    );
  }

  return (
    <>
      {notificationVisible === true ? <AlertDialog /> : ""}
      <div className="adminPageContent">
        <PageBreadCrumb breadcrumbs={breadcrumbs} />
        <Grid fullWidth={true}>
          <Column lg={16} md={8} sm={4}>
            <Section>
              <Heading>
                {ID === "0" ? (
                  <FormattedMessage id="externalconnections.add.title" />
                ) : (
                  <FormattedMessage id="externalconnections.edit.title" />
                )}
              </Heading>
            </Section>
          </Column>
        </Grid>
        <br />
        <div className="orderLegendBody">
          <Grid fullWidth={true} className="gridBoundary">
            <Column lg={16} md={8} sm={4}>
              <Form>
                <Grid fullWidth={true}>
                  <Column lg={8} md={4} sm={4}>
                    <>
                      <FormattedMessage id="externalconnections.name" />
                      <span className="requiredlabel">*</span> :
                    </>
                  </Column>
                  <Column lg={8} md={4} sm={4}>
                    <TextInput
                      id="connection-name"
                      type="text"
                      labelText=""
                      placeholder={intl.formatMessage({
                        id: "externalconnections.name.placeholder",
                      })}
                      required={true}
                      value={connectionName}
                      onChange={handleFieldChange(setConnectionName)}
                    />
                  </Column>
                </Grid>

                <Grid fullWidth={true}>
                  <Column lg={8} md={4} sm={4}>
                    <>
                      <FormattedMessage id="externalconnections.programmedconnection" />
                      <span className="requiredlabel">*</span> :
                    </>
                  </Column>
                  <Column lg={8} md={4} sm={4}>
                    <Select
                      id="programmed-connection"
                      labelText=""
                      value={programmedConnection}
                      onChange={(e) => {
                        setSaveButton(false);
                        setProgrammedConnection(e.target.value);
                      }}
                    >
                      <SelectItem value="" text="" />
                      {programmedConnectionOptions.map((pc) => (
                        <SelectItem key={pc} value={pc} text={pc} />
                      ))}
                    </Select>
                  </Column>
                </Grid>

                <Grid fullWidth={true}>
                  <Column lg={8} md={4} sm={4}>
                    <>
                      <FormattedMessage id="externalconnections.description" />{" "}
                      :
                    </>
                  </Column>
                  <Column lg={8} md={4} sm={4}>
                    <TextArea
                      id="connection-description"
                      labelText=""
                      rows={4}
                      value={connectionDescription}
                      onChange={handleFieldChange(setConnectionDescription)}
                    />
                  </Column>
                </Grid>

                <Grid fullWidth={true}>
                  <Column lg={8} md={4} sm={4}>
                    <>
                      <FormattedMessage id="externalconnections.authtype" />
                      <span className="requiredlabel">*</span> :
                    </>
                  </Column>
                  <Column lg={8} md={4} sm={4}>
                    <Select
                      id="auth-type"
                      labelText=""
                      value={authType}
                      onChange={(e) => {
                        setSaveButton(false);
                        setAuthType(e.target.value);
                      }}
                    >
                      {authTypeOptions.map((at) => (
                        <SelectItem key={at} value={at} text={at} />
                      ))}
                    </Select>
                  </Column>
                </Grid>

                {authType === "BASIC" && (
                  <>
                    <Grid fullWidth={true}>
                      <Column lg={8} md={4} sm={4}>
                        <>
                          <FormattedMessage id="externalconnections.authtype.basic.username" />{" "}
                          :
                        </>
                      </Column>
                      <Column lg={8} md={4} sm={4}>
                        <TextInput
                          id="basic-auth-username"
                          type="text"
                          labelText=""
                          value={username}
                          onChange={handleFieldChange(setUsername)}
                        />
                      </Column>
                    </Grid>
                    <Grid fullWidth={true}>
                      <Column lg={8} md={4} sm={4}>
                        <>
                          <FormattedMessage id="externalconnections.authtype.basic.password" />{" "}
                          :
                        </>
                      </Column>
                      <Column lg={8} md={4} sm={4}>
                        <PasswordInput
                          id="basic-auth-password"
                          labelText=""
                          value={password}
                          onChange={handleFieldChange(setPassword)}
                        />
                      </Column>
                    </Grid>
                  </>
                )}

                <Grid fullWidth={true}>
                  <Column lg={8} md={4} sm={4}>
                    <>
                      <FormattedMessage id="externalconnections.uri" />
                      <span className="requiredlabel">*</span> :
                    </>
                  </Column>
                  <Column lg={8} md={4} sm={4}>
                    <TextInput
                      id="connection-uri"
                      type="text"
                      labelText=""
                      placeholder={intl.formatMessage({
                        id: "externalconnections.uri.placeholder",
                      })}
                      required={true}
                      value={uri}
                      onChange={handleFieldChange(setUri)}
                    />
                  </Column>
                </Grid>

                <Grid fullWidth={true}>
                  <Column lg={8} md={4} sm={4}>
                    <>
                      <FormattedMessage id="externalconnections.active" /> :
                    </>
                  </Column>
                  <Column lg={8} md={4} sm={4}>
                    <Toggle
                      id="connection-active"
                      labelText=""
                      labelA={intl.formatMessage({ id: "label.no" })}
                      labelB={intl.formatMessage({ id: "label.yes" })}
                      toggled={active}
                      onToggle={(checked) => {
                        setSaveButton(false);
                        setActive(checked);
                      }}
                    />
                  </Column>
                </Grid>
              </Form>
            </Column>
          </Grid>
          <br />
          <Grid fullWidth={true}>
            <Column lg={16} md={8} sm={4}>
              <Button
                id="saveButton"
                disabled={saveButton}
                onClick={submitForm}
                type="button"
              >
                <FormattedMessage id="label.button.save" />
              </Button>{" "}
              <Button
                onClick={() =>
                  window.location.assign("/MasterListsPage/externalConnections")
                }
                kind="tertiary"
                type="button"
              >
                <FormattedMessage id="label.button.exit" />
              </Button>
            </Column>
          </Grid>
        </div>
      </div>
    </>
  );
}

export default injectIntl(ExternalConnectionAddModify);
