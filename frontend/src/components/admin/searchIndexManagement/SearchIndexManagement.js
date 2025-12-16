import React, { useState, useContext } from "react";
import { Button, Loading, Grid, Column, Section, Heading } from "@carbon/react";
import { FormattedMessage, useIntl, injectIntl } from "react-intl";
import { getFromOpenElisServer } from "../../utils/Utils";
import PageBreadCrumb from "../../common/PageBreadCrumb";
import { NotificationContext } from "../../layout/Layout";
import {
  AlertDialog,
  NotificationKinds,
} from "../../common/CustomNotification";

function SearchIndexManagement() {
  const [loading, setLoading] = useState(false);
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);
  const intl = useIntl();
  const rebuildIndex = async (res) => {
    setNotificationVisible(true);
    if (res) {
      addNotification({
        kind: NotificationKinds.success,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({
          id: "searchindexmanagement.reindex.success",
        }),
      });
    } else {
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({
          id: "searchindexmanagement.reindex.error",
        }),
      });
    }
    setLoading(false);
  };
  const handleReindexClick = async () => {
    setLoading(true);
    getFromOpenElisServer("/rest/reindex", rebuildIndex);
  };

  const breadcrumbs = [
    { label: "home.label", link: "/" },
    {
      label: "breadcrums.admin.managment",
      link: "/MasterListsPage",
    },
    {
      label: "searchindexmanagement.label",
      link: "/MasterListsPage#SearchIndexManagement",
    },
  ];

  return (
    <>
      {notificationVisible === true ? <AlertDialog /> : ""}
      {loading && <Loading />}
      <div className="adminPageContent">
        <PageBreadCrumb breadcrumbs={breadcrumbs} />
        <Grid fullWidth={true}>
          <Column lg={16} md={8} sm={4}>
            <Section>
              <Heading>
                <FormattedMessage id="searchindexmanagement.label" />
              </Heading>
            </Section>
          </Column>
        </Grid>
        <div className="orderLegendBody">
          <Grid>
            <Column lg={16} md={8} sm={4}>
              <Section>
                <Section>
                  <Heading>
                    <FormattedMessage id="searchindexmanagement.reindex" />
                  </Heading>
                </Section>
              </Section>
              <br />
              <Section>
                <FormattedMessage id="searchindexmanagement.description" />
              </Section>
              <br />
              <Button onClick={handleReindexClick} disabled={loading}>
                <FormattedMessage id="searchindexmanagement.reindex" />
              </Button>
            </Column>
          </Grid>
        </div>
      </div>
    </>
  );
}

export default injectIntl(SearchIndexManagement);
