import { useContext, useState, useCallback, useEffect, useRef } from "react";
import {
  Heading,
  Loading,
  Grid,
  Column,
  Section,
  ClickableTile,
  Toggle,
  Tabs,
  TabList,
  Tab,
  TabPanels,
  TabPanel,
  Tag,
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
import PageBreadCrumb from "../../common/PageBreadCrumb";
import { CustomShowGuide } from "./customComponents/CustomShowGuide";
import { CustomTestDataDisplay } from "./customComponents/CustomTestDataDisplay";
import { TestStepForm } from "./customComponents/TestStepForm";
import { mapTestCatBeanToFormData } from "./customComponents/TestFormData";
import SearchTestNames from "./SearchTestNames";
import TestModifyFilters from "./TestModifyFilters";
import TestComplianceThresholds from "../complianceStandards/TestComplianceThresholds";

let breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "breadcrums.admin.managment", link: "/MasterListsPage" },
  {
    label: "master.lists.page.test.management",
    link: "/MasterListsPage/testManagementConfigMenu",
  },
  {
    label: "configuration.test.modify",
    link: "/MasterListsPage/TestModifyEntry",
  },
];

function TestModifyEntry() {
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  const intl = useIntl();
  const [isLoading, setIsLoading] = useState(false);
  const [testMonifyList, setTestModifyList] = useState({});
  const [filteredTests, setFilteredTests] = useState([]);
  const [searchFilteredTests, setSearchFilteredTests] = useState([]);
  const [showGuide, setShowGuide] = useState(false);
  const [selectedTestIdToEdit, setSelectedTestIdToEdit] = useState(null);
  const [selectedTestBean, setSelectedTestBean] = useState(null);
  const [selectedSampleType, setSelectedSampleType] = useState("");
  const [selectedTestSection, setSelectedTestSection] = useState("");
  const [complianceThresholdCount, setComplianceThresholdCount] = useState(0);

  const componentMounted = useRef(false);

  const handleToggleShowGuide = () => {
    setShowGuide(!showGuide);
  };

  // Internal helper that actually calls the backend
  const handleApiCall = useCallback((queryParams, editingTestId = null) => {
    setIsLoading(true);
    const apiUrl = queryParams
      ? `/rest/TestModifyEntry?${queryParams}`
      : "/rest/TestModifyEntry";

    getFromOpenElisServer(apiUrl, (res) => {
      if (res?.testCatBeanList) {
        // Convert to expected format for UI
        const testListFormat = res.testCatBeanList.map((test) => ({
          id: test.id,
          value:
            test.localization?.english ||
            test.localization?.french ||
            "Unknown Test",
        }));

        setFilteredTests(testListFormat);
        setSearchFilteredTests(testListFormat);
        setTestModifyList(res);

        // Keep the selected test bean up-to-date after a save
        if (editingTestId != null) {
          const refreshed = res.testCatBeanList.find(
            (t) => t.id === editingTestId,
          );
          if (refreshed) {
            setSelectedTestBean(refreshed);
          }
        }
      } else {
        // If no filters or no results, handle empty state
        const emptyList = [];
        setFilteredTests(emptyList);
        setSearchFilteredTests(emptyList);
        if (queryParams) {
          // Only update test list if we have query params (filtered request)
          setTestModifyList({ ...res, testCatBeanList: [] });
        }
      }
      setIsLoading(false);
    });
  }, []);

  // Handle clearing filters from TestModifyFilters component
  const handleClearFilters = useCallback(() => {
    // Clear the test results immediately
    setFilteredTests([]);
    setSearchFilteredTests([]);
    setSelectedSampleType("");
    setSelectedTestSection("");
  }, []);

  // Called when user changes either filter dropdown
  const handleFilterChange = useCallback(
    (sampleType, testSection) => {
      setSelectedSampleType(sampleType);
      setSelectedTestSection(testSection);

      const hasFilters =
        sampleType?.trim() !== "" || testSection?.trim() !== "";

      if (hasFilters) {
        const params = new URLSearchParams();
        if (sampleType && sampleType.trim() !== "") {
          params.append("sampleType", sampleType);
        }
        if (testSection && testSection.trim() !== "") {
          params.append("testSection", testSection);
        }
        handleApiCall(params.toString());
      } else {
        handleClearFilters();
      }
    },
    [handleApiCall, handleClearFilters],
  );

  // Handle search within filtered tests
  const handleTestsFilter = useCallback((searchFiltered) => {
    setSearchFilteredTests(searchFiltered);
  }, []);

  const handleCancelEdit = useCallback(() => {
    setSelectedTestIdToEdit(null);
    setSelectedTestBean(null);
    setComplianceThresholdCount(0);
  }, []);

  // Load filter metadata on component mount (sample types, test sections, etc.)
  useEffect(() => {
    componentMounted.current = true;
    setIsLoading(true);

    // Load only filter metadata, not test data
    getFromOpenElisServer(`/rest/TestModifyEntry`, (res) => {
      if (res) {
        setTestModifyList(res);
        setIsLoading(false);
      }
    });
    return () => {
      componentMounted.current = false;
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
      return;
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
    setIsLoading(false);
    if (res) {
      addNotification({
        title: intl.formatMessage({
          id: "notification.title",
        }),
        message: intl.formatMessage({
          id: "notification.user.post.save.success",
        }),
        kind: NotificationKinds.success,
      });

      setSelectedTestIdToEdit(null);
      setComplianceThresholdCount(0);

      const params = new URLSearchParams();
      if (selectedSampleType && selectedSampleType.trim() !== "") {
        params.append("sampleType", selectedSampleType);
      }
      if (selectedTestSection && selectedTestSection.trim() !== "") {
        params.append("testSection", selectedTestSection);
      }
      handleApiCall(params.toString(), selectedTestIdToEdit);
    } else {
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "server.error.msg" }),
      });
    }
    setNotificationVisible(true);
  };

  if (isLoading) {
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
            <CustomTestDataDisplay testToDisplay={selectedTestBean} />
          ) : (
            <>
              <TestModifyFilters
                sampleTypeList={testMonifyList?.sampleTypeList}
                labUnitList={testMonifyList?.labUnitList}
                selectedSampleType={selectedSampleType}
                selectedTestSection={selectedTestSection}
                onFilterChange={handleFilterChange}
                onClearFilters={handleClearFilters}
              />
              <br />
              <hr />
              <br />
              {filteredTests.length > 0 && (
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
                            testNames={filteredTests}
                            onFilter={handleTestsFilter}
                          />
                        </Heading>
                      </Section>
                    </Column>
                  </Grid>
                  <br />
                  <hr />
                </>
              )}
            </>
          )}
          <br />
          <hr />
          {selectedTestIdToEdit ? (
            <Tabs>
              <TabList aria-label="Test editor sections" contained>
                <Tab>
                  <FormattedMessage id="configuration.test.modify.tab.configuration" />
                </Tab>
                <Tab>
                  <FormattedMessage id="configuration.test.modify.tab.compliance" />
                  {complianceThresholdCount > 0 && (
                    <Tag type="teal" size="sm" style={{ marginLeft: "0.5rem" }}>
                      {complianceThresholdCount}
                    </Tag>
                  )}
                </Tab>
              </TabList>
              <TabPanels>
                <TabPanel>
                  {selectedTestBean && (
                    <TestStepForm
                      initialData={mapTestCatBeanToFormData(selectedTestBean)}
                      postCall={handleTestModifyEntryPostCall}
                      cancelCall={handleCancelEdit}
                      mode="edit"
                    />
                  )}
                </TabPanel>
                <TabPanel>
                  <TestComplianceThresholds
                    embeddedTestId={selectedTestIdToEdit}
                    onCountChange={setComplianceThresholdCount}
                  />
                </TabPanel>
              </TabPanels>
            </Tabs>
          ) : (
            <>
              {searchFilteredTests && searchFilteredTests.length > 0 ? (
                <>
                  <Grid fullWidth={true}>
                    {searchFilteredTests.map((test) => (
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
                            const bean = testMonifyList?.testCatBeanList?.find(
                              (t) => t.id === test.id,
                            );
                            setSelectedTestBean(bean || null);
                            setSelectedTestIdToEdit(test.id);
                          }}
                        >
                          {test.value}
                        </ClickableTile>
                      </Column>
                    ))}
                  </Grid>
                </>
              ) : filteredTests.length === 0 ? (
                <>
                  <Grid fullWidth={true}>
                    <Column lg={16} md={8} sm={4}>
                      <Section>
                        <p>
                          <FormattedMessage id="configuration.test.modify.filter.selectToBegin" />
                        </p>
                      </Section>
                    </Column>
                  </Grid>
                </>
              ) : (
                <>
                  <Grid fullWidth={true}>
                    <Column lg={16} md={8} sm={4}>
                      <Section>
                        <p>
                          <FormattedMessage id="configuration.test.modify.filter.noTestsFound" />
                        </p>
                      </Section>
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
