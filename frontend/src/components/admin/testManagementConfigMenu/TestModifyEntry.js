import { useContext, useState, useEffect, useRef, useCallback } from "react";
import {
  Heading,
  Loading,
  Grid,
  Column,
  Section,
  ClickableTile,
  Toggle,
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
import { CustomShowGuide } from "./customComponents/CustomShowGuide.js";
import { CustomTestDataDisplay } from "./customComponents/CustomTestDataDisplay.js";
import { TestStepForm } from "./customComponents/TestStepForm.js";
import { mapTestCatBeanToFormData } from "./customComponents/TestFormData.js";
import SearchTestNames from "./SearchTestNames";

let breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "breadcrums.admin.managment", link: "/MasterListsPage" },
  {
    label: "master.lists.page.test.management",
    link: "/MasterListsPage#testManagementConfigMenu",
  },
  {
    label: "configuration.test.modify",
    link: "/MasterListsPage#TestModifyEntry",
  },
];

function TestModifyEntry() {
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  const intl = useIntl();
  const [isLoading, setIsLoading] = useState(false);
  const [testMonifyList, setTestModifyList] = useState({});
  const [filteredTests, setFilteredTests] = useState(testMonifyList?.testList);
  const [showGuide, setShowGuide] = useState(false);
  const [selectedTestIdToEdit, setSelectedTestIdToEdit] = useState(null);

  const componentMounted = useRef(false);

  const handleToggleShowGuide = () => {
    setShowGuide(!showGuide);
  };

  const handleTestsFilter = useCallback((filtered) => {
    setFilteredTests(filtered);
  }, []);

  const handleTestModifyEntryList = (res) => {
    if (!res) {
      setIsLoading(true);
    } else {
      setTestModifyList(res);
    }
  };

  useEffect(() => {
    componentMounted.current = true;
    setIsLoading(true);
    getFromOpenElisServer(`/rest/TestModifyEntry`, handleTestModifyEntryList);
    return () => {
      componentMounted.current = false;
      setIsLoading(false);
    };
  }, []);

  const handleTestModifyEntryPostCall = (values) => {
    if (!values) {
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: "Form submission failed due to missing data.",
      });
      setNotificationVisible(true);
      setTimeout(() => {
        window.location.reload();
      }, 500);
    }
    setIsLoading(true);
    postToOpenElisServerJsonResponse(
      `/rest/TestModifyEntry`,
      JSON.stringify({ jsonWad: JSON.stringify(values) }),
      (res) => {
        handleTestModifyEntryPostCallBack(res);
      },
    );
  };

  const handleTestModifyEntryPostCallBack = (res) => {
    if (res) {
      setIsLoading(false);
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
      setTimeout(() => {
        window.location.reload();
      }, 200);
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
  };

  if (!isLoading) {
    return (
      <>
        <Loading />
      </>
    );
  }

  const rows = [
    {
      id: "name",
      field: intl.formatMessage({ id: "field.name" }),
      description: <FormattedMessage id="test.description.name" />,
    },
    {
      id: "reportName",
      field: intl.formatMessage({ id: "field.reportName" }),
      description: <FormattedMessage id="test.description.reportName" />,
    },
    {
      id: "testSection",
      field: intl.formatMessage({ id: "field.testSection" }),
      description: <FormattedMessage id="test.description.testSection" />,
    },
    {
      id: "panel",
      field: intl.formatMessage({ id: "field.panel" }),
      description: <FormattedMessage id="test.description.panel" />,
    },
    {
      id: "uom",
      field: intl.formatMessage({ id: "field.uom" }),
      description: <FormattedMessage id="test.description.uom" />,
    },
    {
      id: "resultType",
      field: intl.formatMessage({ id: "field.resultType" }),
      description: (
        <>
          <p>
            <FormattedMessage id="description.resultType.kind" />
          </p>
          <ul>
            <li>
              <strong>
                <FormattedMessage id="description.resultType.numeric" />
              </strong>
              <FormattedMessage id="description.resultType.numericDesc" />
            </li>
            <li>
              <strong>
                <FormattedMessage id="description.resultType.alphanumeric" />
              </strong>
              <FormattedMessage id="description.resultType.alphanumericDesc" />
            </li>
            <li>
              <strong>
                <FormattedMessage id="description.resultType.textArea" />
              </strong>
              <FormattedMessage id="description.resultType.textAreaDesc" />
            </li>
            <li>
              <strong>
                <FormattedMessage id="description.resultType.selectList" />
              </strong>
              <FormattedMessage id="description.resultType.selectListDesc" />
            </li>
            <li>
              <strong>
                <FormattedMessage id="description.resultType.multiSelectList" />
              </strong>
              <FormattedMessage id="description.resultType.multiSelectListDesc" />
            </li>
            <li>
              <strong>
                <FormattedMessage id="description.resultType.cascadingMultiSelectList" />
              </strong>
              <FormattedMessage id="description.resultType.cascadingMultiSelectListDesc" />
            </li>
          </ul>
        </>
      ),
    },
    {
      id: "active",
      field: intl.formatMessage({ id: "test.field.active" }),
      description: <FormattedMessage id="test.description.active" />,
    },
    {
      id: "orderable",
      field: intl.formatMessage({ id: "test.field.orderable" }),
      description: <FormattedMessage id="test.description.orderable" />,
    },
  ];

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
                  <FormattedMessage id="configuration.test.modify" />
                </Heading>
              </Section>
            </Column>
          </Grid>
          <br />
          <hr />
          <br />
          <Grid fullWidth={true}>
            <Column lg={16} md={8} sm={4}>
              <Section>
                <Section>
                  <Section>
                    <Heading>
                      <FormattedMessage id="label.viewtestCatalog" />
                    </Heading>
                  </Section>
                </Section>
              </Section>
            </Column>
            <Column lg={16} md={8} sm={4}>
              <Toggle
                id="toggle"
                labelText={<FormattedMessage id="test.show.guide" />}
                onClick={handleToggleShowGuide}
              />
            </Column>
          </Grid>
          {showGuide && <CustomShowGuide rows={rows} />}
          <br />
          <hr />
          <br />
          {selectedTestIdToEdit ? (
            <CustomTestDataDisplay
              testToDisplay={testMonifyList?.testCatBeanList?.find(
                (test) => test.id === selectedTestIdToEdit,
              )}
            />
          ) : (
            <>
              <Grid fullWidth={true}>
                <Column lg={8} md={4} sm={2}>
                  <Section>
                    <Section>
                      <Section>
                        <Heading>
                          <FormattedMessage id="test.modify.header.modify" />
                        </Heading>
                      </Section>
                    </Section>
                  </Section>
                </Column>
                <Column lg={8} md={4} sm={2}>
                  <Section>
                    <Heading>
                      <SearchTestNames
                        testNames={testMonifyList?.testList}
                        onFilter={handleTestsFilter}
                      />
                    </Heading>
                  </Section>
                </Column>
              </Grid>
            </>
          )}
          <br />
          <hr />
          {selectedTestIdToEdit ? (
            <>
              <TestStepForm
                initialData={mapTestCatBeanToFormData(
                  testMonifyList?.testCatBeanList?.find(
                    (test) => test.id === selectedTestIdToEdit,
                  ),
                )}
                postCall={handleTestModifyEntryPostCall}
                mode="edit"
              />
            </>
          ) : (
            <>
              {testMonifyList && testMonifyList?.testList?.length > 0 ? (
                <>
                  <Grid fullWidth={true}>
                    {filteredTests?.map((test) => (
                      <Column
                        style={{ margin: "2px" }}
                        lg={4}
                        md={4}
                        sm={2}
                        key={test.id}
                      >
                        <ClickableTile
                          id={test.id}
                          onClick={() => {
                            setSelectedTestIdToEdit(test.id);
                          }}
                        >
                          {test.value}
                        </ClickableTile>
                      </Column>
                    ))}
                  </Grid>
                </>
              ) : (
                <>
                  <Grid fullWidth={true}>
                    <Column lg={16} md={8} sm={4}>
                      <Loading
                        description="loading"
                        small={true}
                        withOverlay={true}
                      />
                    </Column>
                  </Grid>
                </>
              )}
            </>
          )}
          <hr />
          <br />
        </div>
      </div>
    </>
  );
}

export default injectIntl(TestModifyEntry);
