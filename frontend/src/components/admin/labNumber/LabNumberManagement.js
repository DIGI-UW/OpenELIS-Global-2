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
    link: "/MasterListsPage/labNumber",
  },
];

function LabNumberManagement() {
  const intl = useIntl();

  const componentMounted = useRef(false);

  const { configurationProperties, reloadConfiguration } =
    useContext(ConfigurationContext);
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  const [currentLabNumForDisplay, setCurrentLabNumForDisplay] = useState("");
  const [sampleLabNumForDisplay, setSampleLabNumForDisplay] = useState("");
  const [loading, setLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [labNumberValues, setLabNumberValues] = useState(LabNumberFormValues);
  const [testInput, setTestInput] = useState("");
  const [isTestValid, setIsTestValid] = useState(null);

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
    if (
      labNumberValues.labNumberType !== "ALPHANUM" &&
      labNumberValues.labNumberType !== "CUSTOM"
    ) {
      fetchLegacyLabNumNoIncrement();
    }
    generateSampleLabNum();
  }, [labNumberValues]);

  useEffect(() => {
    if (testInput && labNumberValues.customAccessionRegex) {
      try {
        const regex = new RegExp(labNumberValues.customAccessionRegex);
        setIsTestValid(regex.test(testInput));
      } catch (e) {
        setIsTestValid(false);
      }
    } else {
      setIsTestValid(null);
    }
  }, [testInput, labNumberValues.customAccessionRegex]);

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
          if (configurationProperties.AccessionFormat === "ALPHANUM") {
            setCurrentLabNumForDisplay(
              convertAlphaNumLabNumForDisplay(res.body),
            );
          } else {
            setCurrentLabNumForDisplay(res.body);
          }
        }
      },
    );
  };

  const toBase27String = (num) => {
    const chars = "0123456789CDFGHJKLMNPRTVWXY";
    let res = "";
    let n = num;
    while (n > 0) {
      res = chars[n % 27] + res;
      n = Math.floor(n / 27);
    }
    return res || "0";
  };

  const generateLivePreview = (template, seqStart) => {
    if (!template) return [];

    const now = new Date();
    const yyyy = now.getFullYear();
    const yy = ("" + yyyy).slice(-2);
    const mm = ("" + (now.getMonth() + 1)).padStart(2, "0");
    const dd = ("" + now.getDate()).padStart(2, "0");
    const prefix = labNumberValues.alphanumPrefix || "";

    const replaceTokens = (temp, seq) => {
      let res = temp;
      res = res.replace(/{YYYY}/g, yyyy);
      res = res.replace(/{YY}/g, yy);
      res = res.replace(/{MM}/g, mm);
      res = res.replace(/{DD}/g, dd);
      res = res.replace(/{PREFIX}/g, prefix);

      res = res.replace(/{SEQ:(\d+)}/g, (match, n) => {
        return ("" + seq).padStart(parseInt(n), "0");
      });

      res = res.replace(/{ALPHANUMSEQ:(\d+)}/g, (match, n) => {
        return toBase27String(seq).padStart(parseInt(n), "0");
      });
      return res;
    };

    return [
      replaceTokens(template, seqStart),
      replaceTokens(template, seqStart + 1),
      replaceTokens(template, seqStart + 2),
    ];
  };

  const generateSampleLabNum = () => {
    if (labNumberValues.labNumberType === "CUSTOM") {
      const preview = generateLivePreview(
        labNumberValues.customAccessionTemplate,
        1,
      );
      setSampleLabNumForDisplay(preview[0] || "");
      return;
    }

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

  const customPreviews =
    labNumberValues.labNumberType === "CUSTOM"
      ? generateLivePreview(labNumberValues.customAccessionTemplate, 1)
      : [];

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
                <FormattedMessage id="configure.labNumber.title" />
              </Heading>
            </Section>
          </Column>
        </Grid>
        <div className="orderLegendBody">
          <Form onSubmit={handleSubmit}>
            <Grid fullWidth={true}>
              <Column lg={8} md={4} sm={2}>
                <Select
                  id="lab_number_type"
                  labelText={intl.formatMessage({ id: "labNumber.type" })}
                  name="labNumberType"
                  value={labNumberValues.labNumberType}
                  onChange={handleFieldChange}
                >
                  <SelectItem value="ALPHANUM" text="Alpha Numeric" />
                  <SelectItem value="SITEYEARNUM" text="Legacy" />
                  <SelectItem
                    value="CUSTOM"
                    text={intl.formatMessage({ id: "labNumber.type.custom" })}
                  />
                </Select>
              </Column>
              <Column lg={8} md={4} sm={2}></Column>

              {labNumberValues.labNumberType === "ALPHANUM" && (
                <>
                  <Column lg={16} md={8} sm={4}>
                    <br />
                  </Column>
                  <Column lg={8} md={4} sm={2}>
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
                  <Column lg={8} md={4} sm={2}>
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

              {labNumberValues.labNumberType === "CUSTOM" && (
                <>
                  <Column lg={16} md={8} sm={4}>
                    <br />
                  </Column>
                  <Column lg={8} md={4} sm={2}>
                    <TextInput
                      id="customAccessionRegex"
                      name="customAccessionRegex"
                      labelText={intl.formatMessage({
                        id: "labNumber.customRegex",
                      })}
                      helperText={intl.formatMessage({
                        id: "labNumber.customRegex.helper",
                      })}
                      value={labNumberValues.customAccessionRegex}
                      onChange={handleFieldChange}
                      placeholder="^LAB-\d{4}-\d{2}-\d{6}$"
                      maxLength={200}
                    />
                  </Column>
                  <Column lg={8} md={4} sm={2}>
                    <TextInput
                      id="customAccessionTemplate"
                      name="customAccessionTemplate"
                      labelText={intl.formatMessage({
                        id: "labNumber.customTemplate",
                      })}
                      helperText={intl.formatMessage({
                        id: "labNumber.customTemplate.helper",
                      })}
                      value={labNumberValues.customAccessionTemplate}
                      onChange={handleFieldChange}
                      placeholder="LAB-{YYYY}-{MM}-{SEQ:6}"
                      maxLength={50}
                    />
                  </Column>

                  <Column lg={8} md={4} sm={2}>
                    <br />
                    <Heading size={100}>
                      <FormattedMessage id="labNumber.livePreview" />
                    </Heading>
                    <ul>
                      {customPreviews.map((p, i) => (
                        <li key={i}>
                          <code>{p}</code>
                        </li>
                      ))}
                    </ul>
                  </Column>

                  <Column lg={8} md={4} sm={2}>
                    <TextInput
                      id="testInput"
                      labelText={intl.formatMessage({
                        id: "labNumber.testInput",
                      })}
                      placeholder={intl.formatMessage({
                        id: "labNumber.testInput.placeholder",
                      })}
                      value={testInput}
                      onChange={(e) => setTestInput(e.target.value)}
                    />
                    {isTestValid !== null && (
                      <div
                        style={{
                          marginTop: "0.5rem",
                          color: isTestValid ? "#24a148" : "#da1e28",
                        }}
                      >
                        {isTestValid
                          ? intl.formatMessage({
                              id: "labNumber.testResult.valid",
                            })
                          : intl.formatMessage({
                              id: "labNumber.testResult.invalid",
                            })}
                      </div>
                    )}
                  </Column>
                </>
              )}

              <br></br>
              <Column lg={16} md={8} sm={4}>
                <br />
                <FormattedMessage id="labNumber.format.current" />:{" "}
                {currentLabNumForDisplay}
              </Column>
              <br></br>
              <Column lg={16} md={8} sm={4}>
                <FormattedMessage id="labNumber.format.new" />:{" "}
                {sampleLabNumForDisplay}
              </Column>
              <br></br>
              <Column lg={16} md={8} sm={4}>
                <Button
                  type="submit"
                  data-testid="submit-button"
                  disabled={isSubmitting}
                >
                  <FormattedMessage id="label.button.submit" />
                  {isSubmitting && <Loading small={true} />}
                </Button>
              </Column>
            </Grid>
          </Form>
        </div>
      </div>
    </>
  );
}

export default LabNumberManagement;
