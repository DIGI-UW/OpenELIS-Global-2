import React, { useContext, useState, useRef, useEffect } from "react";
import {
  Heading,
  Loading,
  Grid,
  Column,
  Section,
  Toggle,
  Button,
  Select,
  SelectItem,
  Tile,
  Tag,
  InlineNotification,
  Checkbox,
} from "@carbon/react";
import { getFromOpenElisServer, postToOpenElisServerJsonResponse } from "../../utils/Utils";
import { NotificationContext } from "../../layout/Layout";
import {
  AlertDialog,
  NotificationKinds,
} from "../../common/CustomNotification";
import { FormattedMessage, injectIntl, useIntl } from "react-intl";
import PageBreadCrumb from "../../common/PageBreadCrumb";
import { CustomShowGuide } from "../testManagementConfigMenu/customComponents/CustomShowGuide";

let breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "breadcrums.admin.managment", link: "/MasterListsPage" },
  {
    label: "configuration.sampleType.manage",
    link: "/MasterListsPage/SampleTypeManagement",
  },
  {
    label: "configuration.panel.assign",
    link: "/MasterListsPage/SampleTypeTestAssignment",
  },
];

function SampleTypeTestAssignment() {
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  const intl = useIntl();

  const componentMounted = useRef(false);
  const [isLoading, setIsLoading] = useState(false);
  const [showGuide, setShowGuide] = useState(false);
  const [sampleTypesWithTests, setSampleTypesWithTests] = useState([]);
  const [availableSampleTypes, setAvailableSampleTypes] = useState([]);
  const [selectedTests, setSelectedTests] = useState([]);
  const [targetSampleType, setTargetSampleType] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleToggleShowGuide = () => {
    setShowGuide(!showGuide);
  };

  const loadTestAssignments = () => {
    setIsLoading(true);
    getFromOpenElisServer(
      `/rest/SampleTypeManagement/testAssignments`,
      (res) => {
        if (componentMounted.current) {
          setSampleTypesWithTests(res.sampleTypesWithTests || []);
          setAvailableSampleTypes(res.availableSampleTypes || []);
          setIsLoading(false);
        }
      }
    );
  };

  const handleTestSelect = (testId, sampleTypeId, isSelected) => {
    const testInfo = { testId, currentSampleTypeId: sampleTypeId };

    if (isSelected) {
      setSelectedTests(prev => [...prev, testInfo]);
    } else {
      setSelectedTests(prev => prev.filter(t => t.testId !== testId));
    }
  };

  const handleSelectAllForSampleType = (sampleTypeId, isSelected) => {
    const sampleType = sampleTypesWithTests.find(st => st.id === sampleTypeId);
    if (!sampleType) return;

    if (isSelected) {
      const testsToAdd = sampleType.tests.map(test => ({
        testId: test.id,
        currentSampleTypeId: sampleTypeId
      }));
      setSelectedTests(prev => [
        ...prev.filter(t => t.currentSampleTypeId !== sampleTypeId),
        ...testsToAdd
      ]);
    } else {
      setSelectedTests(prev => prev.filter(t => t.currentSampleTypeId !== sampleTypeId));
    }
  };

  const handleReassign = () => {
    if (selectedTests.length === 0) {
      addNotification({
        kind: NotificationKinds.warning,
        title: intl.formatMessage({ id: "notification.title" }),
        message: "Please select tests to reassign.",
      });
      setNotificationVisible(true);
      return;
    }

    if (!targetSampleType) {
      addNotification({
        kind: NotificationKinds.warning,
        title: intl.formatMessage({ id: "notification.title" }),
        message: "Please select a target sample type.",
      });
      setNotificationVisible(true);
      return;
    }

    setIsSubmitting(true);

    const reassignmentData = {
      testIds: selectedTests.map(t => t.testId),
      targetSampleTypeId: parseInt(targetSampleType)
    };

    postToOpenElisServerJsonResponse(
      `/rest/SampleTypeManagement/reassignTests`,
      reassignmentData,
      (res) => {
        if (res && res.success) {
          addNotification({
            title: intl.formatMessage({ id: "notification.title" }),
            message: `Successfully reassigned ${selectedTests.length} test(s) to the selected sample type.`,
            kind: NotificationKinds.success,
          });
          setNotificationVisible(true);

          // Reset selections and reload data
          setSelectedTests([]);
          setTargetSampleType("");
          loadTestAssignments();
        } else {
          addNotification({
            kind: NotificationKinds.error,
            title: intl.formatMessage({ id: "notification.title" }),
            message: res?.message || "Failed to reassign tests.",
          });
          setNotificationVisible(true);
        }
        setIsSubmitting(false);
      }
    );
  };

  const handleCancel = () => {
    setSelectedTests([]);
    setTargetSampleType("");
  };

  const isTestSelected = (testId) => {
    return selectedTests.some(t => t.testId === testId);
  };

  const getSelectedTestsForSampleType = (sampleTypeId) => {
    return selectedTests.filter(t => t.currentSampleTypeId === sampleTypeId);
  };

  const rows = [
    {
      id: "testSelection",
      field: "Test Selection",
      description: <FormattedMessage id="configuration.sampleType.assign.explain" />,
    },
    {
      id: "reassignment",
      field: "Reassignment",
      description: "Select tests from any sample type and choose a new sample type to move them to.",
    },
  ];

  useEffect(() => {
    componentMounted.current = true;
    loadTestAssignments();
    return () => {
      componentMounted.current = false;
    };
  }, []);

  if (isLoading) {
    return <Loading />;
  }

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
                  <FormattedMessage id="configuration.panel.assign" />
                </Heading>
              </Section>
            </Column>
          </Grid>
          <br />
          <hr />
          <br />

          <Grid fullWidth={true}>
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

          {selectedTests.length > 0 && (
            <>
              <Grid fullWidth={true}>
                <Column lg={16} md={8} sm={4}>
                  <InlineNotification
                    kind="info"
                    title="Tests Selected"
                    subtitle={`${selectedTests.length} test(s) selected for reassignment`}
                    hideCloseButton
                  />
                </Column>
              </Grid>
              <br />

              <Grid fullWidth={true}>
                <Column lg={8} md={4} sm={2}>
                  <Select
                    id="target-sample-type"
                    labelText={<FormattedMessage id="configuration.sampleType.assign.new.type" />}
                    value={targetSampleType}
                    onChange={(e) => setTargetSampleType(e.target.value)}
                  >
                    <SelectItem value="" text="Select target sample type..." />
                    {availableSampleTypes.map((sampleType) => (
                      <SelectItem
                        key={sampleType.id}
                        value={sampleType.id}
                        text={sampleType.description || sampleType.name}
                      />
                    ))}
                  </Select>
                </Column>

                <Column lg={8} md={4} sm={2}>
                  <div style={{ paddingTop: "1.5rem" }}>
                    <Button
                      onClick={handleReassign}
                      disabled={isSubmitting || !targetSampleType}
                    >
                      {isSubmitting ? "Reassigning..." : "Reassign Tests"}
                    </Button>

                    <Button
                      kind="secondary"
                      onClick={handleCancel}
                      style={{ marginLeft: "1rem" }}
                      disabled={isSubmitting}
                    >
                      <FormattedMessage id="label.button.cancel" />
                    </Button>
                  </div>
                </Column>
              </Grid>

              <br />
              <hr />
              <br />
            </>
          )}

          <Grid fullWidth={true}>
            <Column lg={16} md={8} sm={4}>
              <Heading size="sm">Tests by Sample Type</Heading>
              <p>Click on test names to select them for reassignment</p>
            </Column>
          </Grid>

          <br />

          {sampleTypesWithTests.map((sampleType) => {
            const selectedForThisType = getSelectedTestsForSampleType(sampleType.id);
            const allSelected = sampleType.tests.length > 0 &&
              selectedForThisType.length === sampleType.tests.length;
            const someSelected = selectedForThisType.length > 0;

            return (
              <Grid fullWidth={true} key={sampleType.id} style={{ marginBottom: "1rem" }}>
                <Column lg={16} md={8} sm={4}>
                  <Tile style={{ padding: "1rem" }}>
                    <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "1rem" }}>
                      <div>
                        <Heading size="sm">
                          {sampleType.description || sampleType.name}
                        </Heading>
                        <p style={{ margin: "0.5rem 0", color: "#6f6f6f" }}>
                          {sampleType.tests.length} test(s) assigned
                        </p>
                      </div>
                      {sampleType.tests.length > 0 && (
                        <Checkbox
                          id={`select-all-${sampleType.id}`}
                          labelText="Select All"
                          checked={allSelected}
                          indeterminate={someSelected && !allSelected}
                          onChange={(checked) => handleSelectAllForSampleType(sampleType.id, checked)}
                        />
                      )}
                    </div>

                    {sampleType.tests.length === 0 ? (
                      <p style={{ color: "#6f6f6f", fontStyle: "italic" }}>
                        No tests assigned to this sample type
                      </p>
                    ) : (
                      <div style={{ display: "flex", flexWrap: "wrap", gap: "0.5rem" }}>
                        {sampleType.tests.map((test) => (
                          <Tag
                            key={test.id}
                            type={isTestSelected(test.id) ? "blue" : "gray"}
                            style={{
                              cursor: "pointer",
                              margin: "0.25rem",
                            }}
                            onClick={() => handleTestSelect(
                              test.id,
                              sampleType.id,
                              !isTestSelected(test.id)
                            )}
                          >
                            {test.name || test.description}
                          </Tag>
                        ))}
                      </div>
                    )}
                  </Tile>
                </Column>
              </Grid>
            );
          })}

          {sampleTypesWithTests.length === 0 && (
            <Grid fullWidth={true}>
              <Column lg={16} md={8} sm={4}>
                <InlineNotification
                  kind="info"
                  title="No Data"
                  subtitle="No sample types with test assignments found."
                  hideCloseButton
                />
              </Column>
            </Grid>
          )}
        </div>
      </div>
    </>
  );
}

export default injectIntl(SampleTypeTestAssignment);