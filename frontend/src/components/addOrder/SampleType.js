import React, { useContext, useEffect, useRef, useState } from "react";
import {
  Checkbox,
  FormGroup,
  Layer,
  Search,
  Select,
  SelectItem,
  Tag,
  Tile,
  TextInput,
  Loading,
} from "@carbon/react";
import CustomCheckBox from "../common/CustomCheckBox";
import CustomSelect from "../common/CustomSelect";
import CustomDatePicker from "../common/CustomDatePicker";
import CustomTimePicker from "../common/CustomTimePicker";
import { NotificationKinds } from "../common/CustomNotification";
import { FormattedMessage, useIntl } from "react-intl";
import { getFromOpenElisServer } from "../utils/Utils";
import { NotificationContext, ConfigurationContext } from "../layout/Layout";
import { sampleTypeTestsStructure } from "../data/SampleEntryTestsForTypeProvider";
import CustomTextInput from "../common/CustomTextInput";
import OrderReferralRequest from "../addOrder/OrderReferralRequest";
import UserSessionDetailsContext from "../../UserSessionDetailsContext";
import StorageLocationSelector from "../storage/StorageLocationSelector";

const SampleType = (props) => {
  const { userSessionDetails } = useContext(UserSessionDetailsContext);
  const { configurationProperties } = useContext(ConfigurationContext);

  const intl = useIntl();

  const componentMounted = useRef(false);
  const sampleTypesRef = useRef(null);

  const { index, rejectSampleReasons, removeSample, sample } = props;

  const [sampleTypes, setSampleTypes] = useState([]);
  const [selectedSampleType, setSelectedSampleType] = useState({
    id: null,
    name: "",
    element_index: 0,
  });
  const [sampleTypeTests, setSampleTypeTests] = useState(
    sampleTypeTestsStructure,
  );
  const [selectedTests, setSelectedTests] = useState([]);
  const [searchBoxTests, setSearchBoxTests] = useState([]);
  const [requestTestReferral, setRequestTestReferral] = useState(false);
  const [referralReasons, setReferralReasons] = useState([]);
  const [referralOrganizations, setReferralOrganizations] = useState([]);
  const [testSearchTerm, setTestSearchTerm] = useState("");
  const [referralRequests, setReferralRequests] = useState([]);
  const { setNotificationVisible, addNotification } =
    useContext(NotificationContext);
  const [rejectionReasonsDisabled, setRejectionReasonsDisabled] =
    useState(true);
  const [selectedPanels, setSelectedPanels] = useState([]);
  const [panelSearchTerm, setPanelSearchTerm] = useState("");
  const [searchBoxPanels, setSearchBoxPanels] = useState([]);
  const [uomList, setUomList] = useState([]);
  const [sampleXml, setSampleXml] = useState(
    sample?.sampleXML != null
      ? sample.sampleXML
      : {
          collectionDate:
            configurationProperties?.AUTOFILL_COLLECTION_DATE === "true"
              ? configurationProperties.currentDateAsText
              : "",
          collector: "",
          quantity: "",
          uom: "",
          rejected: false,
          rejectionReason: "",
          collectionTime:
            configurationProperties?.AUTOFILL_COLLECTION_DATE === "true"
              ? configurationProperties.currentTimeAsText
              : "",
        },
  );
  const [loading, setLoading] = useState(true);

  const defaultSelect = { id: "", value: "Choose Rejection Reason" };

  function handleCollectionDate(date) {
    setSampleXml({
      ...sampleXml,
      collectionDate: date,
    });
  }

  function handleReasons(value) {
    setSampleXml({
      ...sampleXml,
      rejectionReason: value,
    });
    props.sampleTypeObject({
      rejectionReason: value,
      sampleObjectIndex: index,
    });
  }

  function handleCollectionTime(time) {
    setSampleXml({
      ...sampleXml,
      collectionTime: time,
    });
  }

  function handleCollector(value) {
    setSampleXml({
      ...sampleXml,
      collector: value,
    });
  }

  function handleStorageLocationChange(location, sampleIndex) {
    setSampleXml({
      ...sampleXml,
      storageLocation: location,
      storagePositionId: location?.position?.id || null,
    });
  }

  function handleQuantity(value) {
    setSampleXml({
      ...sampleXml,
      quantity: value.target.value,
    });
  }

  function handleUom(value) {
    setSampleXml({
      ...sampleXml,
      uom: value,
    });
  }

  useEffect(() => {
    updateSampleXml(sampleXml, index);
  }, [sampleXml]);

  const handleRemoveSampleTest = (index) => {
    removeSample(index);
  };

  const handleReferralRequest = () => {
    setRequestTestReferral(!requestTestReferral);
    if (selectedTests.length > 0) {
      const defaultReferralRequest = [];
      selectedTests.map((test) => {
        defaultReferralRequest.push({
          reasonForReferral: referralReasons[0].id,
          referrer:
            userSessionDetails.firstName + " " + userSessionDetails.lastName,
          institute: referralOrganizations[0].id,
          sentDate: "",
          testId: test.id,
        });
      });
      setReferralRequests(defaultReferralRequest);
    }
  };

  const handleTestSearchChange = (event) => {
    const query = event.target.value;
    setTestSearchTerm(query);
    const results = sampleTypeTests.tests.filter((test) => {
      return test.name.toLowerCase().includes(query.toLowerCase());
    });
    setSearchBoxTests(results);
  };

  const handleRemoveSelectedTest = (test) => {
    removedTestFromSelectedTests(test);
  };

  const handleFilterSelectTest = (test) => {
    setTestSearchTerm("");
    addTestToSelectedTests(test);
  };

  const handleTestCheckbox = (e, test) => {
    if (e.currentTarget.checked) {
      addTestToSelectedTests(test);
    } else {
      removedTestFromSelectedTests(test);
    }
  };

  function findTestById(testId) {
    return sampleTypeTests.tests.find((test) => test.id === testId);
  }

  function findTestIndex(testId) {
    return sampleTypeTests.tests.findIndex((test) => test.id === testId);
  }

  const panelIsSelected = (panelId) => {
    for (let i in selectedPanels) {
      if (selectedPanels[i].id === panelId) {
        return true;
      }
    }
    return false;
  };

  const testIsSelected = (testId) => {
    for (let i in selectedTests) {
      if (selectedTests[i].id === testId) {
        return true;
      }
    }
    return false;
  };

  const triggerPanelCheckBoxChange = (isChecked, testIds) => {
    const testIdsList = testIds.split(",").map((id) => id.trim());
    testIdsList.map((testId) => {
      let testIndex = findTestIndex(testId);
      let test = findTestById(testId);
      if (testIndex !== -1) {
        if (isChecked) {
          if (!testIsSelected(test.id)) {
            setSelectedTests((prevState) => {
              return [...prevState, { id: test.id, name: test.name }];
            });
          }
        } else {
          removedTestFromSelectedTests(test);
        }
      }
    });
  };

  const removedTestFromSelectedTests = (test) => {
    let index = 0;
    for (let i in selectedTests) {
      if (selectedTests[i].id === test.id) {
        const newTests = selectedTests;
        newTests.splice(index, 1);
        setSelectedTests([...newTests]);
        break;
      }
      index++;
    }
  };

  function addReferralRequest(test) {
    setReferralRequests([
      ...referralRequests,
      {
        reasonForReferral: referralReasons[0].id,
        referrer:
          userSessionDetails.firstName + " " + userSessionDetails.lastName,
        institute: referralOrganizations[0].id,
        sentDate: "",
        testId: test.id,
      },
    ]);
  }

  function removeReferralRequest(test) {
    let index = 0;
    for (let x in referralRequests) {
      if (referralRequests[x].testId === test.id) {
        const newReferralRequests = referralRequests;
        newReferralRequests.splice(index, 1);
        setReferralRequests([...newReferralRequests]);
        break;
      }
      index++;
    }
  }

  const handleFetchSampleTypeTests = (e, index) => {
    setSelectedTests([]);
    setReferralRequests([]);
    const { value } = e.target;
    const selectedSampleTypeOption =
      sampleTypesRef.current.options[sampleTypesRef.current.selectedIndex].text;
    setSelectedSampleType({
      ...selectedSampleType,
      id: value,
      name: selectedSampleTypeOption,
      element_index: index,
    });
    props.sampleTypeObject({ sampleTypeId: value, sampleObjectIndex: index });
  };

  const updateSampleXml = (sampleXML, index) => {
    props.sampleTypeObject({ sampleXML: sampleXML, sampleObjectIndex: index });
  };

  const fetchSamplesTypes = (res) => {
    if (componentMounted.current) {
      setSampleTypes(res);
      setLoading(false);
    }
  };

  const fetchSampleTypeTests = (res) => {
    if (componentMounted.current) {
      setSampleTypeTests(res);
    }
  };

  useEffect(() => {
    if (props.sample.referralItems.length > 0 && referralReasons.length > 0) {
      setRequestTestReferral(props.sample.requestReferralEnabled);
      setReferralRequests(props.sample.referralItems);
    }
  }, [referralReasons]);

  useEffect(() => {
    props.sampleTypeObject({
      requestReferralEnabled: requestTestReferral,
      sampleObjectIndex: index,
    });
    if (!requestTestReferral) {
      setReferralRequests([]);
    }
  }, [requestTestReferral]);

  useEffect(() => {
    props.sampleTypeObject({
      referralItems: referralRequests,
      sampleObjectIndex: index,
    });
  }, [referralRequests]);

  const displayReferralReasonsOptions = (res) => {
    if (componentMounted.current) {
      setReferralReasons(res);
    }
  };
  const displayReferralOrgOptions = (res) => {
    if (componentMounted.current) {
      setReferralOrganizations(res);
    }
  };

  function handleRejection(checked) {
    if (checked) {
      addNotification({
        kind: NotificationKinds.warning,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "reject.order.sample.notification" }),
      });
      setNotificationVisible(true);
    }
    setSampleXml({
      ...sampleXml,
      rejected: checked,
    });
    setRejectionReasonsDisabled(!rejectionReasonsDisabled);
  }

  const removedPanelFromSelectedPanels = (panel) => {
    let index = 0;
    for (let i in selectedPanels) {
      if (selectedPanels[i].id === panel.id) {
        triggerPanelCheckBoxChange(false, selectedPanels[i].testIds);
        const newPanels = selectedPanels;
        newPanels.splice(index, 1);
        setSelectedPanels([...newPanels]);
        break;
      }
      index++;
    }
  };

  const handlePanelSearchChange = (event) => {
    const query = event.target.value;
    setPanelSearchTerm(query);
    const results = sampleTypeTests.panels.filter((panel) => {
      return panel.name.toLowerCase().includes(query.toLowerCase());
    });
    setSearchBoxPanels(results);
  };

  const handleFilterSelectPanel = (panel) => {
    setPanelSearchTerm("");
    addPanelToSelectedPanels(panel);
  };

  const handlePanelCheckbox = (panel) => {
    if (!panelIsSelected(panel.id)) {
      addPanelToSelectedPanels(panel);
    } else {
      removedPanelFromSelectedPanels(panel);
    }
  };

  const handleRemoveSelectedPanel = (panel) => {
    removedPanelFromSelectedPanels(panel);
  };

  function addTestToSelectedTests(test) {
    if (!testIsSelected(test.id)) {
      setSelectedTests([...selectedTests, { id: test.id, name: test.name }]);
    }
  }

  const addPanelToSelectedPanels = (panel) => {
    setSelectedPanels([
      ...selectedPanels,
      { id: panel.id, name: panel.name, testIds: panel.testIds },
    ]);
  };

  useEffect(() => {
    componentMounted.current = true;
    if (selectedSampleType.id !== "" && selectedSampleType.id != null) {
      getFromOpenElisServer(
        `/rest/sample-type-tests?sampleType=${selectedSampleType.id}`,
        fetchSampleTypeTests,
      );
    }
    return () => {
      componentMounted.current = false;
    };
  }, [selectedSampleType.id]);

  useEffect(() => {
    getFromOpenElisServer(`/rest/UomCreate`, fetchUomCreate);
  }, []);

  const fetchUomCreate = (res) => {
    if (componentMounted.current) {
      setUomList(res.existingUomList || []);
    }
  };

  useEffect(() => {
    props.sampleTypeObject({
      sampleRejected: rejectionReasonsDisabled,
      sampleObjectIndex: index,
    });
  }, [rejectionReasonsDisabled]);

  useEffect(() => {
    props.sampleTypeObject({
      selectedTests: selectedTests,
      sampleObjectIndex: index,
    });
  }, [selectedTests]);

  useEffect(() => {
    props.sampleTypeObject({
      selectedPanels: selectedPanels,
      sampleObjectIndex: index,
    });
    for (let i in selectedPanels) {
      triggerPanelCheckBoxChange(true, selectedPanels[i].testIds);
    }
  }, [selectedPanels, sampleTypeTests]);

  const repopulateUI = () => {
    if (props.sample !== null) {
      setSelectedTests(props.sample.tests);
      setSelectedPanels(props.sample.panels);
      setSelectedSampleType({
        id: props.sample.sampleTypeId,
      });
    }
  };

  useEffect(() => {
    componentMounted.current = true;
    getFromOpenElisServer(
      "/rest/referral-reasons",
      displayReferralReasonsOptions,
    );
    getFromOpenElisServer(
      "/rest/referral-organizations",
      displayReferralOrgOptions,
    );
    repopulateUI();
    getFromOpenElisServer("/rest/user-sample-types", fetchSamplesTypes);
    return () => {
      componentMounted.current = false;
    };
  }, []);

  return (
    <>
      {loading && <Loading />}
      <div className="sampleBody">
        <Select
          className="selectSampleType"
          id={"sampleId_" + index}
          ref={sampleTypesRef}
          value={
            props.sample.sampleTypeId === "" ? "" : props.sample.sampleTypeId
          }
          name="sampleId"
          labelText=""
          onChange={(e) => {
            handleFetchSampleTypeTests(e, index);
          }}
          required
        >
          <SelectItem text="Select sample type" value="" />
          {sampleTypes?.map((sampleType, i) => (
            <SelectItem text={sampleType.value} value={sampleType.id} key={i} />
          ))}
        </Select>

        <CustomCheckBox
          id={"reject_" + index}
          onChange={(value) => handleRejection(value)}
          label={intl.formatMessage({ id: "sample.reject.label" })}
        />
        {sampleXml.rejected && (
          <CustomSelect
            id={"rejectedReasonId_" + index}
            options={rejectSampleReasons}
            disabled={rejectionReasonsDisabled}
            defaultSelect={defaultSelect}
            onChange={(e) => handleReasons(e)}
          />
        )}
        <div className="inlineDiv" style={{ display: "flex", gap: "1rem" }}>
          <TextInput
            value={sampleXml.quantity}
            name="quantity"
            labelText={intl.formatMessage({
              id: "sample.quantity.label",
            })}
            id="quantity"
            type="number"
            min="0"
            onChange={(value) => handleQuantity(value)}
            placeholder={intl.formatMessage({
              id: "sample.quantity.label",
            })}
          />

          <CustomSelect
            id={"uomId_" + index}
            labelText={intl.formatMessage({ id: "sample.uom.label" })}
            options={uomList}
            disabled={false}
            value={sampleXml.uom}
            onChange={(value) => handleUom(value)}
          />
        </div>
        <div className="inlineDiv">
          <CustomDatePicker
            id={"collectionDate_" + index}
            autofillDate={
              configurationProperties?.AUTOFILL_COLLECTION_DATE === "true"
            }
            onChange={(date) => handleCollectionDate(date)}
            value={sampleXml.collectionDate}
            labelText={intl.formatMessage({ id: "sample.collection.date" })}
            className="inputText"
            disallowFutureDate={true}
          />

          <CustomTimePicker
            id={"collectionTime_" + index}
            autofillTime={
              configurationProperties?.AUTOFILL_COLLECTION_DATE === "true"
            }
            onChange={(time) => handleCollectionTime(time)}
            value={sampleXml.collectionTime}
            className="inputText"
            labelText={intl.formatMessage({ id: "sample.collection.time" })}
          />
        </div>
        <div className="inlineDiv">
          <CustomTextInput
            id={"collector_" + index}
            onChange={(value) => handleCollector(value)}
            defaultValue={""}
            value={sampleXml.collector}
            labelText={intl.formatMessage({ id: "collector.label" })}
            className="inputText"
          />
        </div>
        {/* Storage Location Selector - INT-001: Integration point */}
        {/* NOTE: In order entry workflow, SampleItems are created after Sample is saved.
            Storage assignment operates at SampleItem level, so actual assignment happens
            after SampleItems are created. The location preference is stored here for
            later assignment to the first/default SampleItem. */}
        <div className="inlineDiv">
          <StorageLocationSelector
            workflow="orders"
            optional={true}
            sampleInfo={{
              // Note: sampleId here is temporary/placeholder - actual SampleItem ID will be available after SampleItems are created
              sampleId: sample?.id || sample?.sampleId || `TEMP-${index}`,
              type: selectedSampleType?.name || sampleXml?.sampleTypeName || "",
              status: sampleXml?.rejected ? "Rejected" : "Active",
            }}
            onLocationChange={(locationData) => {
              // locationData format: { sample, newLocation, reason?, conditionNotes?, positionCoordinate? }
              // Extract newLocation from locationData for backward compatibility
              // Store location preference - will be assigned to SampleItem after SampleItems are created
              const location = locationData?.newLocation || locationData;
              handleStorageLocationChange(location, index);
            }}
          />
        </div>
        <div className="testPanels">
          <div className="cds--col">
            <h4>
              <FormattedMessage id="sample.label.orderpanel" />
            </h4>
            <div
              className={"searchTestText"}
              style={{ marginBottom: "1.188rem" }}
            >
              {selectedPanels && selectedPanels.length ? (
                <>
                  {selectedPanels.map((panel, panel_index) => (
                    <Tag
                      filter
                      key={`panelTags_` + panel_index}
                      onClose={() => handleRemoveSelectedPanel(panel)}
                      style={{ marginRight: "0.5rem" }}
                      type={"green"}
                    >
                      {panel.name}
                    </Tag>
                  ))}
                </>
              ) : (
                <></>
              )}
            </div>
            <FormGroup
              legendText={
                <FormattedMessage id="sample.search.panel.legend.text" />
              }
            >
              <Search
                size="lg"
                id={`panels_search_` + index}
                labelText={
                  <FormattedMessage id="label.search.availablepanel" />
                }
                placeholder={intl.formatMessage({
                  id: "choose.availablepanel",
                })}
                onChange={handlePanelSearchChange}
                value={(() => {
                  if (panelSearchTerm) {
                    return panelSearchTerm;
                  }
                  return "";
                })()}
              />
              <div>
                {(() => {
                  if (!panelSearchTerm) return null;
                  if (searchBoxPanels && searchBoxPanels.length) {
                    return (
                      <ul className={"searchTestsList"}>
                        {searchBoxPanels.map((panel, panel_index) => (
                          <li
                            role="menuitem"
                            className={"singleTest"}
                            key={`panelFilter_` + panel_index}
                            onClick={() => handleFilterSelectPanel(panel)}
                          >
                            {panel.name}
                          </li>
                        ))}
                      </ul>
                    );
                  }
                  return (
                    <>
                      <Layer>
                        <Tile className={"emptyFilterTests"}>
                          <span>
                            <FormattedMessage id="sample.panel.search.error.msg" />{" "}
                            <strong>"{panelSearchTerm}"</strong>{" "}
                          </span>
                        </Tile>
                      </Layer>
                    </>
                  );
                })()}
              </div>
            </FormGroup>
            {sampleTypeTests.panels != null &&
              sampleTypeTests.panels.map((panel) => {
                return panel.name === "" ? (
                  ""
                ) : (
                  <Checkbox
                    onChange={() => handlePanelCheckbox(panel)}
                    labelText={panel.name}
                    id={`panel_` + index + "_" + panel.id}
                    key={index + panel.id}
                    checked={
                      selectedPanels.filter((item) => item.id === panel.id)
                        .length > 0
                    }
                  />
                );
              })}
          </div>
        </div>

        <div className="cds--col">
          {selectedTests && !selectedTests.length ? "" : <h4>Order Tests</h4>}
          <div
            className={"searchTestText"}
            style={{ marginBottom: "1.188rem" }}
          >
            {selectedTests && selectedTests.length ? (
              <>
                {selectedTests.map((test, index) => (
                  <Tag
                    filter
                    key={`testTags_` + index}
                    onClose={() => handleRemoveSelectedTest(test)}
                    style={{ marginRight: "0.5rem" }}
                    type={"red"}
                  >
                    {test.name}
                  </Tag>
                ))}
              </>
            ) : (
              <></>
            )}
          </div>
          <FormGroup
            legendText={intl.formatMessage({
              id: "legend.search.availabletests",
            })}
          >
            <Search
              size="lg"
              id={`tests_search_` + index}
              labelText={
                <FormattedMessage id="label.search.available.targetest" />
              }
              placeholder={intl.formatMessage({
                id: "holder.choose.availabletest",
              })}
              onChange={handleTestSearchChange}
              value={(() => {
                if (testSearchTerm) {
                  return testSearchTerm;
                }
                return "";
              })()}
            />
            <div>
              {(() => {
                if (!testSearchTerm) return null;
                if (searchBoxTests && searchBoxTests.length) {
                  return (
                    <ul className={"searchTestsList"}>
                      {searchBoxTests.map((test, test_index) => (
                        <li
                          role="menuitem"
                          className={"singleTest"}
                          key={`filterTest_` + test_index}
                          onClick={() => handleFilterSelectTest(test)}
                        >
                          {test.name}
                        </li>
                      ))}
                    </ul>
                  );
                }
                return (
                  <>
                    <Layer>
                      <Tile className={"emptyFilterTests"}>
                        <span>
                          <FormattedMessage id="title.notestfoundmatching" />
                          <strong> "{testSearchTerm}"</strong>{" "}
                        </span>
                      </Tile>
                    </Layer>
                  </>
                );
              })()}
            </div>
          </FormGroup>
          {sampleTypeTests.tests != null &&
            sampleTypeTests.tests.map((test) => {
              return test.name === "" ? (
                ""
              ) : (
                <Checkbox
                  onChange={(e) => handleTestCheckbox(e, test)}
                  labelText={test.name}
                  id={`test_` + index + "_" + test.id}
                  key={`test_checkBox_` + index + test.id}
                  checked={
                    selectedTests.filter((item) => item.id === test.id).length >
                    0
                  }
                />
              );
            })}
        </div>

        <div className="requestTestReferral">
          <Checkbox
            id={`useReferral_` + index}
            labelText={intl.formatMessage({
              id: "label.refertest.referencelab",
            })}
            onChange={handleReferralRequest}
          />
          {requestTestReferral === true && (
            <OrderReferralRequest
              index={index}
              selectedTests={selectedTests}
              referralReasons={referralReasons}
              referralOrganizations={referralOrganizations}
              referralRequests={referralRequests}
              setReferralRequests={setReferralRequests}
            />
          )}
        </div>
      </div>
    </>
  );
};

export default SampleType;
