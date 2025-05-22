import React, { useState, useRef, useContext, useEffect } from "react";
import { FormattedMessage, injectIntl } from "react-intl";
import { Form, Grid, Column, Section, Button } from "@carbon/react";
import CustomLabNumberInput from "../../common/CustomLabNumberInput";
import { getFromOpenElisServer } from "../../utils/Utils";
import config from "../../../config.json";
import AutoComplete from "../../common/AutoComplete";
import { ConfigurationContext } from "../../layout/Layout";

const NonConformityNotification = (props) => {
  const { configurationProperties } = useContext(ConfigurationContext);
  const componentMounted = useRef(false);
  const [accessionNumber, setAccessionNumber] = useState("");
  const [siteNames, setSiteNames] = useState([]);
  const [selectedSiteId, setSelectedSiteId] = useState("");
  const [selectedSiteName, setSelectedSiteName] = useState("");

  function handleSiteName(e) {
    setSelectedSiteName(e.target.value);
  }

  function handleAutoCompleteSiteName(siteId) {
    setSelectedSiteId(siteId);
  }

  const getSiteList = (response) => {
    if (componentMounted.current) {
      setSiteNames(response);
    }
  };

  useEffect(() => {
    componentMounted.current = true;
    getFromOpenElisServer("/rest/site-names", getSiteList);
    return () => {
      componentMounted.current = false;
    };
  }, []);

  const handleSubmit = (e) => {
    e.preventDefault();

    const baseParams = `report=${props.report}&type=patient`;
    const baseUrl = `${config.serverBaseUrl}/ReportPrint`;
    const url = `${baseUrl}?${baseParams}&accessionDirect=${accessionNumber}&selectList.selection=${selectedSiteId}`;

    window.open(url);
  };

  return (
    <>
      <Form onSubmit={handleSubmit}>
        <Grid>
          <Column lg={16} md={8} sm={4}>
            <Section>
              <h3>
                <FormattedMessage id={props.id} />
              </h3>
            </Section>
          </Column>
        </Grid>
        <br />
        <Grid fullWidth={true}>
          <Column lg={16} md={8} sm={4}>
            <Section>
              <h5>
                <FormattedMessage id="report.enter.labNumber.headline" />
              </h5>
              <FormattedMessage id="sample.search.scanner.instructions2" />
            </Section>
          </Column>
        </Grid>
        <br />
        <Grid fullWidth={true}>
          <Column lg={7} md={4} sm={3}>
            <CustomLabNumberInput
              name="labNumber"
              value={accessionNumber}
              labelText={<FormattedMessage id="from.title" />}
              id="labNumber"
              className="inputText"
              onChange={(e, rawVal) =>
                setAccessionNumber(rawVal ? rawVal : e?.target?.value)
              }
            />
          </Column>
          <Column lg={7} md={4} sm={3}>
            <AutoComplete
              name="siteName"
              id="siteName"
              className="inputText"
              allowFreeText={
                !(
                  configurationProperties.restrictFreeTextRefSiteEntry ===
                  "true"
                )
              }
              onChange={handleSiteName}
              onSelect={handleAutoCompleteSiteName}
              label={
                <>
                  <FormattedMessage id="report.select.site" />
                </>
              }
              suggestions={siteNames.length > 0 ? siteNames : []}
            />
          </Column>
        </Grid>
        <br />
        <Grid fullWidth={true}>
          <Column lg={16} md={8} sm={4}>
            <Section>
              <FormattedMessage id="reports.nonConformity.notification.report.instructions" />
            </Section>
          </Column>
        </Grid>
        <br />
        <br />
        <Grid fullWidth={true}>
          <Column lg={16} md={8} sm={4}>
            <Section>
              <Button data-cy="printableVersion" type="submit">
                <FormattedMessage id="label.button.generatePrintableVersion" />
              </Button>
            </Section>
          </Column>
        </Grid>
      </Form>
    </>
  );
};

export default injectIntl(NonConformityNotification);
