import React, { useContext, useState, useEffect, useRef } from "react";
import { useParams } from "react-router-dom";
import {
  Heading,
  Grid,
  Column,
  Section,
  Loading,
  Button,
  Tile,
} from "@carbon/react";
import { NotificationContext } from "../layout/Layout";
import { AlertDialog } from "../common/CustomNotification";
import { FormattedMessage, useIntl } from "react-intl";
import PatientHeader from "../common/PatientHeader";
import PageBreadCrumb from "../common/PageBreadCrumb";

function GeneralProgrammeCaseView() {
  const { programmeId } = useParams();
  const { notificationVisible } = useContext(NotificationContext);
  const intl = useIntl();
  const [programme, setProgramme] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetch(`/rest/programs/${programmeId}`)
      .then((res) => res.json())
      .then((data) => {
        setProgramme(data);
        setLoading(false);
      });
  }, [programmeId]);

  const breadcrumbs = [
    { label: "home.label", link: "/" },
    {
      label: "banner.menu.generalProgramme",
      link: "/GeneralProgrammeDashboard",
    },
    { label: programme ? programme.name : "", link: "#" },
  ];

  return (
    <>
      {notificationVisible === true ? <AlertDialog /> : ""}
      {loading && <Loading description="Loading Case..." />}
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <Section>
            <Section>
              <Heading>
                {programme ? programme.name : <FormattedMessage id="loading" />}
              </Heading>
            </Section>
          </Section>
        </Column>
      </Grid>
      {programme && (
        <Grid fullWidth={true}>
          <Column lg={16} md={8} sm={4}>
            <Tile>
              <h4>
                <FormattedMessage id="admin.page.configuration.formEntryConfigMenu.description" />
                :
              </h4>
              <p>{programme.description}</p>
            </Tile>
          </Column>
        </Grid>
      )}
    </>
  );
}

export default GeneralProgrammeCaseView;
