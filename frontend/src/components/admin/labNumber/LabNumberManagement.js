import React, { useContext, useState, useEffect, useRef } from "react";
import {
  Form,
  FormLabel,
  Heading,
  Checkbox,
  TextInput,
  Select,
  SelectItem,
  Button,
  Loading,
  Grid,
  Column,
  Section,
} from "@carbon/react";
import LabNumberFormValues from "./LabNumberFormValues";
import {
  getFromOpenElisServer,
  postToOpenElisServerFullResponse,
  convertAlphaNumLabNumForDisplay,
} from "../../utils/Utils";
import { NotificationContext } from "../../layout/Layout";
import {
  AlertDialog,
  NotificationKinds,
} from "../../common/CustomNotification";
import { FormattedMessage, useIntl } from "react-intl";
import { ConfigurationContext } from "../../layout/Layout";
import PageBreadCrumb from "../../common/PageBreadCrumb.js";

let breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "breadcrums.admin.managment", link: "/MasterListsPage" },
  {
    label: "sidenav.label.admin.labNumber",
    link: "/MasterListsPage#labNumber",
  },
];
function LabNumberManagement() {
  const intl = useIntl();

  const componentMounted = useRef(false);

  const { configurationProperties, reloadConfiguration } =
    useContext(ConfigurationContext);
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  const [currentLabNumForDisplay, setCurrentLabNumForDisplay] = useState(
    convertAlphaNumLabNumForDisplay("23000000"),
  );
  const [sampleLabNumForDisplay, setSampleLabNumForDisplay] = useState(
    convertAlphaNumLabNumForDisplay("23000000"),
  );
  const [loading, setLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [labNumberValues, setLabNumberValues] = useState(LabNumberFormValues);

  useEffect(() => {
    componentMounted.current = true;
    loadValues();
    return () => {
      componentMounted.current = false;
    };
  }, []);

  useEffect(() => {
    fetchCurrentLabNumberNoIncrement();
  }, [configurationProperties]);

  useEffect(() => {
    if (!(labNumberValues.labNumberType === "ALPHANUM")) {
      fetchLegacyLabNumNoIncrement();
    }
    generateSampleLabNum();
  }, [labNumberValues]);

  const handleFieldChange = (e) => {
    const { name, value } = e.target;
    const updatedValues = { ...labNumberValues };
    var jp = require("jsonpath");
    jp.value(updatedValues, name, value);
    setLabNumberValues(updatedValues);
  };

  async function displayStatus(res) {
    setNotificationVisible(true);
    setIsSubmitting(false);
    if (res.status == "200") {
      addNotification({
        kind: NotificationKinds.success,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "success.add.edited.msg" }),
      });
      var body = await res.json();
      setLabNumberValues({ ...LabNumberFormValues, ...body });
    } else {
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "error.add.edited.msg" }),
      });
    }
    reloadConfiguration();
  }

  const loadValues = () => {
    getFromOpenElisServer("/rest/labnumbermanagement", (body) => {
      setLabNumberValues({ ...LabNumberFormValues, ...body });
      setLoading(false);
    });
  };

  const handleSubmit = (event) => {
    event.preventDefault();
    setIsSubmitting(true);
    var submitValues = { ...labNumberValues };
    postToOpenElisServerFullResponse(
      "/rest/labnumbermanagement",
      JSON.stringify(submitValues),
      displayStatus,
    );
  };

  const fetchCurrentLabNumberNoIncrement = () => {
    getFromOpenElisServer(
      "/rest/SampleEntryGenerateScanProvider?noIncrement=true",
      (res) => {
        if (res.status) {
          if (configurationProperties.AccessionFormat != "ALPHANUM") {
            setCurrentLabNumForDisplay(res.body);
          } else {
            setCurrentLabNumForDisplay(
              convertAlphaNumLabNumForDisplay(res.body),
            );
          }
        }
      },
    );
  };

  const generateSampleLabNum = () => {
    let dateDigits = new Date().getFullYear() % 100;
    let labNumber = "" + dateDigits;
    if (labNumberValues.usePrefix && labNumberValues.alphanumPrefix) {
      labNumber = labNumber + labNumberValues.alphanumPrefix;
    }
    labNumber = labNumber + "000000";
    setSampleLabNumForDisplay(convertAlphaNumLabNumForDisplay(labNumber));
  };

  const fetchLegacyLabNumNoIncrement = () => {
    getFromOpenElisServer(
      "/rest/SampleEntryGenerateScanProvider?noIncrement=true&format=SITEYEARNUM",
      (res) => {
        if (res.status) {
          setSampleLabNumForDisplay(res.body);
        }
      },
    );
  };

  return (
    <>
      {notificationVisible === true ? <AlertDialog /> : ""}
      {loading && <Loading />}
      <div className="adminPageContent">
        <PageBreadCrumb breadcrumbs={breadcrumbs} />
        <Grid fullWidth={true}>
          <Column lg={16}>
            <Section>
              <Heading>
                <FormattedMessage id="configure.labNumber.title" />
              </Heading>
            </Section>
          </Column>
        </Grid>
        <Form onSubmit={handleSubmit}>
          <Grid fullWidth={true}>
            <Column lg={8}>
              <Select
                id="lab_number_type"
                labelText={intl.formatMessage({ id: "labNumber.type" })}
                name="labNumberType"
                value={labNumberValues.labNumberType}
                onChange={handleFieldChange}
              >
                <SelectItem value="ALPHANUM" text="Alpha Numeric" />
                <SelectItem value="SITEYEARNUM" text="Legacy" />
              </Select>
            </Column>
            <Column lg={8}></Column>
            <Column lg={16}>
              {" "}
              <br></br>
            </Column>
            {labNumberValues.labNumberType === "ALPHANUM" && (
              <>
                <Column lg={8}>
                  <TextInput
                    type="text"
                    name="alphanumPrefix"
                    id="alphanumPrefix"
                    labelText={intl.formatMessage({ id: "labNumber.prefix" })}
                    disabled={!labNumberValues.usePrefix}
                    value={labNumberValues.alphanumPrefix}
                    onChange={handleFieldChange}
                    enableCounter={true}
                    maxCount={5}
                  />
                </Column>
                <Column lg={8}>
                  <span className="middleAlignVertical">
                    <Checkbox
                      type="checkbox"
                      name="usePrefix"
                      id="usePrefix"
                      labelText={intl.formatMessage({
                        id: "labNumber.usePrefix",
                      })}
                      checked={labNumberValues.usePrefix}
                      onClick={() => {
                        const updatedValues = { ...labNumberValues };
                        updatedValues.usePrefix = !labNumberValues.usePrefix;
                        setLabNumberValues(updatedValues);
                      }}
                    />
                  </span>
                </Column>
              </>
            )}
            <br></br>
            <Column lg={16}>
              <FormattedMessage id="labNumber.format.current" />:{" "}
              {currentLabNumForDisplay}
            </Column>
            <br></br>
            <Column lg={16}>
              <FormattedMessage id="labNumber.format.new" />:{" "}
              {sampleLabNumForDisplay}
            </Column>
            <br></br>
            <Column lg={16}>
              <Button type="submit">
                <FormattedMessage id="label.button.submit" />
                {isSubmitting && <Loading small={true} />}
              </Button>
            </Column>
          </Grid>
        </Form>
      </div>
    </>
  );
}

export default LabNumberManagement;
