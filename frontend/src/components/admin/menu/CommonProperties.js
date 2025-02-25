import React, { useContext, useEffect, useState } from "react";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../utils/Utils";
import { FormattedMessage, useIntl } from "react-intl";
import { NotificationContext } from "../../layout/Layout";
import { AlertDialog } from "../../common/CustomNotification";
import {
  Grid,
  Heading,
  Column,
  Section,
  TextInput,
  Button,
  Loading,
} from "@carbon/react";
import PageBreadCrumb from "../../common/PageBreadCrumb";

export const CommonProperties = () => {
  const [commonProperties, setCommonProperties] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const intl = useIntl();

  const { notificationVisible, addNotification, setNotificationVisible } =
    useContext(NotificationContext);

  useEffect(() => {
    getFromOpenElisServer("/rest/properties", (fetchedProperties, err) => {
      if (err) {
        setError(err);
        setLoading(false); // Set loading to false in case of error
      } else {
        const sortedKeys = Object.keys(fetchedProperties).sort();
        let sortedProperties = {};
        sortedKeys.forEach((key) => {
          sortedProperties[key] = fetchedProperties[key];
        });
        setCommonProperties(sortedProperties);
        setLoading(false); // Set loading to false after data is fetched
      }
    });
  }, []);

  const handleSubmit = () => {
    setLoading(true);
    const url = `/rest/properties`;
    let body = JSON.stringify(commonProperties);
    postToOpenElisServerJsonResponse(url, body, (response, err) => {
      console.log("response from server", response);
    });
    addNotification({
      kind: "success",
      message: intl.formatMessage({ id: "message.propertiesupdate.success" }),
      title: intl.formatMessage({ id: "notification.title" }),
    });
    setNotificationVisible(true);

    setLoading(false);
  };

  return (
    <>
      <div className="adminPageContent">
        {notificationVisible === true ? <AlertDialog /> : ""}
        <PageBreadCrumb
          breadcrumbs={[
            { label: "home.label", link: "/" },
            { label: "breadcrums.admin.managment", link: "/MasterListsPage" },
            {
              label: "common properties",
              link: "/MasterListsPage#commonproperties",
            },
          ]}
        />
        <Grid fullWidth={true}>
          <Column lg={16} md={8} sm={4}>
            <Section>
              <Heading>
                <FormattedMessage
                  id="ewfggwgewgewgweg"
                  defaultMessage="Common Properties"
                />
              </Heading>
            </Section>
          </Column>
        </Grid>
        {loading && <Loading />} {error && <p>Error: {error}</p>}{" "}
        <div className="orderLegendBody">
          <Grid fullWidth={true}>
            <Column lg={8} md={8} sm={4}>
              {commonProperties && (
                <>
                  {Object.keys(commonProperties)
                    .slice(
                      0,
                      Math.ceil(Object.keys(commonProperties).length / 2),
                    )
                    .map((key) => {
                      // Remove the prefix "org.openelisglobal" using regex
                      let shortKey = key.replace(/^org\.openelisglobal\./, "");

                      return (
                        <div
                          key={key}
                          className="inlineDiv"
                          style={{ gap: "20px" }}
                        >
                          <TextInput
                            id={key + "-input"}
                            labelText={shortKey} // Use the modified key without the prefix
                            value={commonProperties[key]}
                            onChange={(e) => {
                              setCommonProperties({
                                ...commonProperties,
                                [key]: e.target.value,
                              });
                            }}
                          />
                        </div>
                      );
                    })}
                </>
              )}
            </Column>

            <Column lg={8} md={8} sm={4}>
              {commonProperties && (
                <>
                  {Object.keys(commonProperties)
                    .slice(Math.ceil(Object.keys(commonProperties).length / 2))
                    .map((key) => {
                      // Remove the prefix "org.openelisglobal" using regex
                      let shortKey = key.replace(/^org\.openelisglobal\./, "");

                      return (
                        <div
                          key={key}
                          className="inlineDiv"
                          style={{ gap: "20px" }}
                        >
                          <TextInput
                            id={key + "-input"}
                            labelText={shortKey} // Use the modified key without the prefix
                            value={commonProperties[key]}
                            onChange={(e) => {
                              setCommonProperties({
                                ...commonProperties,
                                [key]: e.target.value,
                              });
                            }}
                          />
                        </div>
                      );
                    })}
                </>
              )}
            </Column>
          </Grid>

          <div style={{ marginLeft: "2em" }} className="inlineDiv">
            <Button type="submit" onClick={handleSubmit}>
              <FormattedMessage
                id="label.button.update"
                defaultMessage="Update"
              />
            </Button>
          </div>
        </div>
      </div>
    </>
  );
};
