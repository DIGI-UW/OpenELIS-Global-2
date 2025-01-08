import React, { useContext, useState, useEffect, useRef } from "react";
import {
  Form,
  Stack,
  TextInput,
  TextArea,
  Select,
  SelectItem,
  Button,
  Checkbox,
  IconButton,
  Toggle,
  Loading,
  RadioButtonGroup,
  RadioButton,
  ModalWrapper,
  Grid,
  Column,
} from "@carbon/react";
import { Add, Subtract } from "@carbon/react/icons";
import AutoComplete from "../../common/AutoComplete.js";
import RuleBuilderFormValues from "../../formModel/innitialValues/RuleBuilderFormValues";
import { getFromOpenElisServer, postToOpenElisServer } from "../../utils/Utils";
import { NotificationContext } from "../../layout/Layout";
import {
  AlertDialog,
  NotificationKinds,
} from "../../common/CustomNotification";
import { FormattedMessage, useIntl } from "react-intl";
import "./ReflexStyles.css";

var defaultTestResultList = {};
var defaultSampleTests = { conditions: {}, actions: {} };

function ReflexRule() {
  const componentMounted = useRef(false);

  const intl = useIntl();

  const FIELD = {
    conditions: "conditions",
    actions: "actions",
  };
  const conditionsObj = {
    id: null,
    sampleId: "",
    testName: "",
    testId: "",
    relation: "",
    value: "0",
    value2: "0",
    testAnalyteId: null,
  };
  const actionObj = {
    id: null,
    sampleId: "",
    reflexTestName: "",
    reflexTestId: "",
    internalNote: "",
    externalNote: "",
    addNotification: "Y",
    testReflexId: null,
  };

  const ruleObj = {
    id: null,
    ruleName: "",
    overall: "",
    toggled: true,
    active: true,
    analyteId: null,
    conditions: [conditionsObj],
    actions: [actionObj],
  };

  const [ruleList, setRuleList] = useState([RuleBuilderFormValues]);
  const [sampleList, setSampleList] = useState([]);
  const [generalRelationOptions, setGeneralRelationOptions] = useState([]);
  const [numericRelationOptions, setNumericRelationOptions] = useState([]);
  const [overallOptions, setOverallOptions] = useState([]);
  const [testResultList, setTestResultList] = useState({
    0: { 0: { type: "N", list: [] } },
  }); //{index :{field_index:{type : "T" ,list : []}}}
  const [sampleTestList, setSampleTestList] = useState({
    conditions: {},
    actions: {},
  }); //{field :{index :{field_index:[]}}}
  const [counter, setCounter] = useState(0);
  const [loading, setLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errors, setErrors] = useState({});
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  useEffect(() => {
    componentMounted.current = true;
    getFromOpenElisServer("/rest/samples", fetchSamples);
    getFromOpenElisServer("/rest/reflexrule-options", fetchRuleOptions);
    getFromOpenElisServer("/rest/reflexrules", fetchReflexRules);

    return () => {
      // This code runs when component is unmounted
      componentMounted.current = false;
    };
  }, []);

  useEffect(() => {
    if (loading) {
      loadDefaultTestResultList();
    }
  }, [ruleList]);

  const loadDefaultTestResultList = () => {
    var sampleList = [];
    ruleList.forEach(function (rule, index) {
      if (rule.conditions) {
        rule.conditions.forEach(function (condition, conditionIndex) {
          if (condition.sampleId) {
            sampleList.push(condition.sampleId);
          }
        });
      }
      if (rule.actions) {
        rule.actions.forEach(function (action, actionIndex) {
          if (action.sampleId) {
            sampleList.push(action.sampleId);
          }
        });
      }
    });

    getFromOpenElisServer(
      "/rest/test-display-beans-map?samplesTypes=" + sampleList.join(","),
      (resp) => buildSampleTests(resp),
    );
  };

  const buildSampleTests = (sampleTestsMap) => {
    ruleList.forEach(function (rule, index) {
      if (rule.conditions) {
        rule.conditions.forEach(function (condition, conditionIndex) {
          if (condition.sampleId) {
            fetchDeafultTests(
              sampleTestsMap[condition.sampleId],
              index,
              conditionIndex,
              FIELD.conditions,
              condition,
            );
          }
        });
      }
      if (rule.actions) {
        rule.actions.forEach(function (action, actionIndex) {
          if (action.sampleId) {
            fetchDeafultTests(
              sampleTestsMap[action.sampleId],
              index,
              actionIndex,
              FIELD.actions,
              null,
            );
          }
        });
      }
    });
  };

  const addError = (errorObj) => {
    const error = { ...errors };
    error[errorObj.name] = errorObj.error;
    setErrors(error);
  };

  const clearError = (field) => {
    const error = { ...errors };
    delete error[field];
    setErrors(error);
  };

  const fetchDeafultTests = (testList, index, item_index, field, condition) => {
    loadDeafultSampleTestList(field, index, item_index, testList);

    if (field == FIELD.conditions) {
      if (condition.value) {
        const test = defaultSampleTests.conditions[index][item_index].find(
          (test) => {
            if (test.id.trim() === condition.testId) {
              return true;
            }
          },
        );

        if (test) {
          loadDefaultResultList(index, item_index, test);
        }
      }
    }

    if (
      Object.keys(defaultSampleTests["conditions"]).length == ruleList.length
    ) {
      setLoading(false);
    }
  };

  const loadDeafultSampleTestList = (field, index, item_index, resulList) => {
    if (!defaultSampleTests[field][index]) {
      defaultSampleTests[field][index] = {};
    }
    defaultSampleTests[field][index][item_index] = resulList;
  };

  const loadDefaultResultList = (index, item_index, test) => {
    if (!defaultTestResultList[index]) {
      defaultTestResultList[index] = {};
    }
    if (!defaultTestResultList[index][item_index]) {
      defaultTestResultList[index][item_index] = {};
    }

    defaultTestResultList[index][item_index]["list"] = test.resultList;
    defaultTestResultList[index][item_index]["type"] = test.resultType;
  };

  const handleRuleFieldChange = (e, index) => {
    const { name, value } = e.target;
    const list = [...ruleList];
    list[index][name] = value;
    setRuleList(list);
  };

  const handleRuleFieldItemChange = (e, index, itemIndex, field) => {
    const { name, value } = e.target;
    const list = [...ruleList];
    list[index][field][itemIndex][name] = value;
    setRuleList(list);
  };

  const handleAutoCompleteRuleFieldItemChange = (
    value,
    name,
    index,
    itemIndex,
    field,
  ) => {
    const list = [...ruleList];
    list[index][field][itemIndex][name] = value;
    setRuleList(list);
  };

  const handleAddNotificationChange = (value, index, itemIndex, field) => {
    const e = { target: { name: "addNotification", value: value } };
    handleRuleFieldItemChange(e, index, itemIndex, field);
  };

  const handleTestSelected = (id, index, item_index, field) => {
    var testDetails = { resultList: [], resultType: "N" };
    if (sampleTestList[field]) {
      testDetails = sampleTestList[field][index][item_index].find(
        (test) => test.id == id,
      );
    }
    const results = { ...testResultList };
    if (!results[index]) {
      results[index] = {};
    }
    if (!results[index][item_index]) {
      results[index][item_index] = {};
    }
    results[index][item_index]["list"] = testDetails.resultList;
    results[index][item_index]["type"] = testDetails.resultType;
    setTestResultList(results);
  };

  const loadSampleTestList = (field, index, item_index, resulList) => {
    const results = { ...sampleTestList };
    if (!results[field][index]) {
      results[field][index] = {};
    }
    results[field][index][item_index] = resulList;
    setSampleTestList(results);
  };

  const handleSampleSelected = (e, index, item_index, field) => {
    const { value } = e.target;
    getFromOpenElisServer(
      "/rest/test-display-beans?sampleType=" + value,
      (resp) => fetchTests(resp, index, item_index, field),
    );
  };

  const handleRuleRemove = (index, id) => {
    if (id) {
      postToOpenElisServer(
        "/rest/deactivate-reflexrule/" + id,
        {},
        handleDelete,
      );
    }
  };

  const handleDelete = (status) => {
    setNotificationVisible(true);
    if (status == "200") {
      addNotification({
        kind: NotificationKinds.success,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "delete.success.msg" }),
      });
      window.location.reload();
    } else {
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "delete.error.msg" }),
      });
    }
  };

  const handleRuleAdd = () => {
    setRuleList([...ruleList, ruleObj]);
  };

  const toggleRule = (e, index) => {
    const list = [...ruleList];
    list[index]["toggled"] = e;
    setRuleList(list);
  };

  const handleRuleFieldItemAdd = (index, field, fieldObj) => {
    const list = [...ruleList];
    list[index][field].push(fieldObj);
    setRuleList(list);
  };

  const handleRuleFieldItemRemove = (index, itemIndex, field) => {
    const list = [...ruleList];
    list[index][field].splice(itemIndex, 1);
    setRuleList(list);
  };

  const handleSubmited = (status, index) => {
    setIsSubmitting(false);
    setNotificationVisible(true);
    if (status == "200") {
      const element = document.getElementById("submit_" + index);
      element.disabled = true;
      addNotification({
        kind: NotificationKinds.success,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "save.success" }),
      });
    } else {
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "error.duplicate.calculationname" }),
      });
    }
  };

  const handleSubmit = (event, index) => {
    event.preventDefault();
    if (isSubmitting) {
      return;
    }
    setIsSubmitting(true);
    console.debug(JSON.stringify(ruleList[index]));
    postToOpenElisServer(
      "/rest/reflexrule",
      JSON.stringify(ruleList[index]),
      (status) => handleSubmited(status, index),
    );
  };

  const fetchTests = (testList, index, item_index, field) => {
    loadSampleTestList(field, index, item_index, testList);
  };

  const fetchSamples = (sampleList) => {
    if (componentMounted.current) {
      setSampleList(sampleList);
    }
  };

  const fetchReflexRules = (reflexRuleList) => {
    if (componentMounted.current) {
      // console.debug(JSON.stringify(reflexRuleList))
      if (reflexRuleList.length > 0) {
        setRuleList(reflexRuleList);
      } else {
        setLoading(false);
      }
    }
  };

  const fetchRuleOptions = (options) => {
    if (componentMounted.current) {
      console.debug(JSON.stringify(options));
      if (options) {
        setGeneralRelationOptions(options.generalRelationOptions);
        setNumericRelationOptions(options.numericRelationOptions);
        setOverallOptions(options.overallOptions);
      }
    }
  };

  const handleClick = () => {
    var count = counter + 1;
    if (count == 1) {
      setTestResultList(defaultTestResultList);
      setSampleTestList(defaultSampleTests);
    }
    setCounter(count);
  };

  const validateTextInPut = (value, type) => {
    if (type === "N") {
      if (value.match(/^-?\d+$/)) {
        //valid integer (positive or negative)
        return false;
      } else if (value.match(/^\d+\.\d+$/)) {
        //valid float
        return false;
      } else {
        console.error("invalid value");
        return true;
      }
    }
  };

  const addTextInPutError = (value, type, fieldName) => {
    if (type === "N") {
      if (value.match(/^-?\d+$/)) {
        //valid integer (positive or negative)
        clearError(fieldName);
      } else if (value.match(/^\d+\.\d+$/)) {
        //valid float
        clearError(fieldName);
      } else {
        console.error("invalid value");
        addError({ name: fieldName, error: "Invaid Numeric Value" });
      }
    }
  };

  const normalRangeSelected = (relation) => {
    if (
      relation === "OUTSIDE_NORMAL_RANGE" ||
      relation === "INSIDE_NORMAL_RANGE"
    ) {
      return true;
    }
    return false;
  };

  return (
    <>
      {notificationVisible === true ? <AlertDialog /> : ""}
      {loading && <Loading></Loading>}
      {ruleList.map((rule, index) => (
        <div key={index} className="rules">
          <div className="first-division">
            <Form onSubmit={(e) => handleSubmit(e, index)}>
              <Stack gap={7}>
                <div className="ruleBody">
                  <div className="inlineDiv">
                    <div>
                      <TextInput
                        name="ruleName"
                        className="reflexInputText"
                        type="text"
                        id={index + "_rulename"}
                        labelText={
                          <FormattedMessage id="rulebuilder.label.ruleName" />
                        }
                        value={rule.ruleName}
                        onChange={(e) => handleRuleFieldChange(e, index)}
                        required
                      />
                    </div>
                    <div>&nbsp; &nbsp;</div>
                    <div>
                      <Toggle
                        toggled={rule.toggled}
                        aria-label="toggle button"
                        id={index + "_toggle"}
                        labelText={
                          <FormattedMessage id="rulebuilder.label.toggleRule" />
                        }
                        onToggle={(e) => toggleRule(e, index)}
                        onClick={handleClick}
                      />
                    </div>
                    <div>&nbsp; &nbsp; &nbsp; &nbsp;</div>
                    <div>
                      <Checkbox
                        labelText={"Active: " + rule.active}
                        name="active"
                        id={index + "_active"}
                        checked={rule.active}
                        disabled={rule.active}
                        onChange={(e) => {
                          const list = [...ruleList];
                          list[index]["active"] = e.target.checked;
                          setRuleList(list);
                        }}
                      />
                    </div>
                  </div>
                  {rule.toggled && (
                    <>
                      <div className="section">
                        <Grid>
                          <Column lg={16} sm={3}>
                            <h5>
                              <FormattedMessage id="rulebuilder.label.addRuleConditions" />
                            </h5>
                          </Column>

                          <Column lg={16} sm={3}>
                            {" "}
                            &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp;{" "}
                          </Column>
                          <Column lg={16} sm={3}>
                            <Select
                              value={rule.overall}
                              id={index + "_overall"}
                              name="overall"
                              labelText={
                                <FormattedMessage id="rulebuilder.label.overallOptions" />
                              }
                              className="reflexInputSelect"
                              onChange={(e) => handleRuleFieldChange(e, index)}
                              required
                            >
                              <SelectItem text="" value="" />
                              {overallOptions.map((overall, overall_index) => (
                                <SelectItem
                                  text={overall.label}
                                  value={overall.value}
                                  key={overall_index}
                                />
                              ))}
                            </Select>
                          </Column>
                          <Column lg={16}>
                            {" "}
                            &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp;{" "}
                          </Column>
                          <Column lg={16}>
                            {" "}
                            &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp;{" "}
                          </Column>
                        </Grid>
                        {rule.conditions.map((condition, condition_index) => (
                          <Grid key={index + "_" + condition_index}>
                            <Column lg={3} sm={4}>
                              <Select
                                id={index + "_" + condition_index + "_sample"}
                                name="sampleId"
                                labelText={
                                  <FormattedMessage id="rulebuilder.label.selectSample" />
                                }
                                value={condition.sampleId}
                                onChange={(e) => {
                                  handleRuleFieldItemChange(
                                    e,
                                    index,
                                    condition_index,
                                    FIELD.conditions,
                                  );
                                  handleSampleSelected(
                                    e,
                                    index,
                                    condition_index,
                                    FIELD.conditions,
                                  );
                                }}
                                required
                              >
                                <SelectItem text="" value="" />
                                {sampleList.map((sample, sample_index) => (
                                  <SelectItem
                                    text={sample.value}
                                    value={sample.id}
                                    key={sample_index}
                                  />
                                ))}
                              </Select>
                            </Column>

                            <Column lg={3} sm={4}>
                              <AutoComplete
                                id={
                                  index +
                                  "_" +
                                  condition_index +
                                  "_conditionTestId"
                                }
                                value={condition.testId}
                                onSelect={(id) => {
                                  handleAutoCompleteRuleFieldItemChange(
                                    id,
                                    "testId",
                                    index,
                                    condition_index,
                                    FIELD.conditions,
                                  );
                                  handleTestSelected(
                                    id,
                                    index,
                                    condition_index,
                                    FIELD.conditions,
                                  );
                                }}
                                name="testName"
                                label={
                                  <FormattedMessage id="rulebuilder.label.searchTest" />
                                }
                                suggestions={
                                  sampleTestList[FIELD.conditions][index]
                                    ? sampleTestList[FIELD.conditions][index][
                                        condition_index
                                      ]
                                    : []
                                }
                                required
                              />
                            </Column>
                            <Column lg={3} sm={4}>
                              <Select
                                value={condition.relation}
                                id={index + "_" + condition_index + "_relation"}
                                name="relation"
                                labelText={
                                  <FormattedMessage id="rulebuilder.label.relation" />
                                }
                                onChange={(e) =>
                                  handleRuleFieldItemChange(
                                    e,
                                    index,
                                    condition_index,
                                    FIELD.conditions,
                                  )
                                }
                                required
                              >
                                <SelectItem text="" value="" />
                                {testResultList[index] &&
                                  testResultList[index][condition_index] &&
                                  testResultList[index][condition_index][
                                    "type"
                                  ] && (
                                    <>
                                      {testResultList[index][condition_index][
                                        "type"
                                      ] === "N" ? (
                                        <>
                                          {numericRelationOptions.map(
                                            (relation, relation_index) => (
                                              <SelectItem
                                                text={relation.label}
                                                value={relation.value}
                                                key={relation_index}
                                              />
                                            ),
                                          )}
                                        </>
                                      ) : (
                                        <>
                                          {generalRelationOptions.map(
                                            (relation, relation_index) => (
                                              <SelectItem
                                                text={relation.label}
                                                value={relation.value}
                                                key={relation_index}
                                              />
                                            ),
                                          )}
                                        </>
                                      )}
                                    </>
                                  )}
                              </Select>
                            </Column>
                            <Column lg={3} sm={4}>
                              {testResultList[index] &&
                              testResultList[index][condition_index] &&
                              testResultList[index][condition_index]["type"] ? (
                                <>
                                  {testResultList[index][condition_index][
                                    "type"
                                  ] === "D" ? (
                                    <Select
                                      value={condition.value}
                                      id={
                                        index + "_" + condition_index + "_value"
                                      }
                                      name="value"
                                      labelText={
                                        <FormattedMessage id="rulebuilder.label.dictValue" />
                                      }
                                      onChange={(e) =>
                                        handleRuleFieldItemChange(
                                          e,
                                          index,
                                          condition_index,
                                          FIELD.conditions,
                                        )
                                      }
                                      disabled={normalRangeSelected(
                                        condition.relation,
                                      )}
                                      required
                                    >
                                      <SelectItem text="" value="" />
                                      <>
                                        {testResultList[index][condition_index][
                                          "list"
                                        ] && (
                                          <>
                                            {testResultList[index][
                                              condition_index
                                            ]["list"].map(
                                              (
                                                result,
                                                condition_value_index,
                                              ) => (
                                                <SelectItem
                                                  text={result.value}
                                                  value={result.id}
                                                  key={condition_value_index}
                                                />
                                              ),
                                            )}
                                          </>
                                        )}
                                      </>
                                    </Select>
                                  ) : (
                                    <>
                                      <TextInput
                                        name="value"
                                        type={
                                          testResultList[index][
                                            condition_index
                                          ]["type"] === "N"
                                            ? "number"
                                            : "text"
                                        }
                                        id={
                                          index +
                                          "_" +
                                          condition_index +
                                          "_value"
                                        }
                                        labelText={
                                          testResultList[index][
                                            condition_index
                                          ]["type"] === "N" ? (
                                            <FormattedMessage id="rulebuilder.label.numericValue" />
                                          ) : (
                                            <FormattedMessage id="rulebuilder.label.textValue" />
                                          )
                                        }
                                        value={condition.value}
                                        onChange={(e) => {
                                          handleRuleFieldItemChange(
                                            e,
                                            index,
                                            condition_index,
                                            FIELD.conditions,
                                          );
                                          addTextInPutError(
                                            condition.value,
                                            testResultList[index][
                                              condition_index
                                            ]["type"],
                                            "condition-value_" +
                                              index +
                                              "_" +
                                              condition_index,
                                          );
                                        }}
                                        invalid={validateTextInPut(
                                          condition.value,
                                          testResultList[index][
                                            condition_index
                                          ]["type"],
                                        )}
                                        invalidText={
                                          <FormattedMessage id="rulebuilder.error.invalidNumeric" />
                                        }
                                        disabled={normalRangeSelected(
                                          condition.relation,
                                        )}
                                        required
                                      />
                                    </>
                                  )}
                                </>
                              ) : (
                                <>
                                  <TextInput
                                    name="value"
                                    type="text"
                                    id={
                                      index + "_" + condition_index + "_value"
                                    }
                                    labelText={
                                      <FormattedMessage id="rulebuilder.label.numericValue" />
                                    }
                                    value={condition.value}
                                    onChange={(e) =>
                                      handleRuleFieldItemChange(
                                        e,
                                        index,
                                        condition_index,
                                        FIELD.conditions,
                                      )
                                    }
                                    required
                                  />
                                </>
                              )}
                            </Column>
                            <Column lg={2} sm={4}>
                              {testResultList[index] &&
                                testResultList[index][condition_index] &&
                                testResultList[index][condition_index][
                                  "type"
                                ] && (
                                  <>
                                    {testResultList[index][condition_index][
                                      "type"
                                    ] === "N" &&
                                      condition.relation === "BETWEEN" && (
                                        <TextInput
                                          name="value2"
                                          type="text"
                                          id={
                                            index +
                                            "_" +
                                            condition_index +
                                            "_value"
                                          }
                                          labelText={
                                            <FormattedMessage id="rulebuilder.label.numericValue2" />
                                          }
                                          value={condition.value2}
                                          onChange={(e) => {
                                            handleRuleFieldItemChange(
                                              e,
                                              index,
                                              condition_index,
                                              FIELD.conditions,
                                            );
                                            addTextInPutError(
                                              condition.value2,
                                              testResultList[index][
                                                condition_index
                                              ]["type"],
                                              "condition-value2_" +
                                                index +
                                                "_" +
                                                condition_index,
                                            );
                                          }}
                                          invalid={validateTextInPut(
                                            condition.value2,
                                            testResultList[index][
                                              condition_index
                                            ]["type"],
                                          )}
                                          invalidText={
                                            <FormattedMessage id="rulebuilder.error.invalidNumeric" />
                                          }
                                          required
                                        />
                                      )}
                                  </>
                                )}
                            </Column>
                            <Column lg={1} sm={4}>
                              {rule.conditions.length !== 1 && (
                                <IconButton
                                  label={
                                    <FormattedMessage id="rulebuilder.label.removeCondition" />
                                  }
                                  className="ruleFieldButton"
                                  onClick={() =>
                                    handleRuleFieldItemRemove(
                                      index,
                                      condition_index,
                                      FIELD.conditions,
                                    )
                                  }
                                  kind="danger"
                                  size="sm"
                                >
                                  {" "}
                                  <Subtract size={18} />
                                </IconButton>
                              )}
                            </Column>
                            <Column lg={1} sm={4}>
                              {rule.conditions.length - 1 ===
                                condition_index && (
                                <IconButton
                                  label={
                                    <FormattedMessage id="rulebuilder.label.addCondition" />
                                  }
                                  className="ruleFieldButton"
                                  onClick={() =>
                                    handleRuleFieldItemAdd(
                                      index,
                                      FIELD.conditions,
                                      conditionsObj,
                                    )
                                  }
                                  kind="tertiary"
                                  size="sm"
                                >
                                  {" "}
                                  <Add size={18} />
                                </IconButton>
                              )}
                            </Column>
                            <Column lg={16}>
                              {" "}
                              &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp;{" "}
                            </Column>
                            <Column lg={16}>
                              {" "}
                              &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp;{" "}
                            </Column>
                          </Grid>
                        ))}
                      </div>
                      <div className="section">
                        <div className="inlineDiv">
                          <div>
                            <h5>
                              <FormattedMessage id="rulebuilder.label.perfomActions" />
                            </h5>
                          </div>
                        </div>
                        {rule.actions.map((action, action_index) => (
                          <Grid key={index + "_" + action_index}>
                            <Column lg={3} sm={4}>
                              <Select
                                id={index + "_" + action_index + "_sample"}
                                name="sampleId"
                                labelText={
                                  <FormattedMessage id="rulebuilder.label.selectSample" />
                                }
                                value={action.sampleId}
                                onChange={(e) => {
                                  handleRuleFieldItemChange(
                                    e,
                                    index,
                                    action_index,
                                    FIELD.actions,
                                  );
                                  handleSampleSelected(
                                    e,
                                    index,
                                    action_index,
                                    FIELD.actions,
                                  );
                                }}
                                required
                              >
                                <SelectItem text="" value="" />
                                {sampleList.map((sample, sample_index) => (
                                  <SelectItem
                                    text={sample.value}
                                    value={sample.id}
                                    key={sample_index}
                                  />
                                ))}
                              </Select>
                            </Column>
                            <Column lg={3} sm={4}>
                              <AutoComplete
                                id={
                                  index + "_" + action_index + "_reflexTestId"
                                }
                                onSelect={(id) => {
                                  handleAutoCompleteRuleFieldItemChange(
                                    id,
                                    "reflexTestId",
                                    index,
                                    action_index,
                                    FIELD.actions,
                                  );
                                }}
                                label={
                                  <FormattedMessage id="rulebuilder.label.searchTest" />
                                }
                                name="reflexTestName"
                                value={action.reflexTestId}
                                suggestions={
                                  sampleTestList[FIELD.actions][index]
                                    ? sampleTestList[FIELD.actions][index][
                                        action_index
                                      ]
                                    : []
                                }
                              />
                            </Column>
                            <Column lg={4} sm={4}>
                              <TextArea
                                style={{
                                  width: "100%",
                                  height: "1px",
                                }}
                                name="internalNote"
                                type="text"
                                id={index + "_" + action_index + "_inote"}
                                labelText={
                                  <FormattedMessage id="rulebuilder.label.addInternalNote" />
                                }
                                value={action.internalNote}
                                onChange={(e) =>
                                  handleRuleFieldItemChange(
                                    e,
                                    index,
                                    action_index,
                                    FIELD.actions,
                                  )
                                }
                              />
                            </Column>
                            <Column lg={4} sm={4}>
                              <TextArea
                                name="externalNote"
                                style={{
                                  width: "100%",
                                  height: "1px",
                                }}
                                type="text"
                                id={index + "_" + action_index + "_xnote"}
                                labelText={
                                  <FormattedMessage id="rulebuilder.label.addExternalNote" />
                                }
                                value={action.externalNote}
                                onChange={(e) =>
                                  handleRuleFieldItemChange(
                                    e,
                                    index,
                                    action_index,
                                    FIELD.actions,
                                  )
                                }
                              />
                            </Column>
                            {/* <div>&nbsp; &nbsp;</div>
                            <div>
                              <RadioButtonGroup
                                valueSelected={action.addNotification}
                                legendText={
                                  <FormattedMessage id="rulebuilder.label.addPopup" />
                                }
                                name={
                                  index +
                                  "_" +
                                  action_index +
                                  "_add_notofocation"
                                }
                                id={index + "_" + action_index + "_popup"}
                                onChange={(value) =>
                                  handleAddNotificationChange(
                                    value,
                                    index,
                                    action_index,
                                    FIELD.actions,
                                  )
                                }
                              >
                                <RadioButton
                                  id={index + "_" + action_index + "_no"}
                                  labelText="Yes"
                                  value="Y"
                                />
                                <RadioButton
                                  id={index + "_" + action_index + "_yes"}
                                  labelText="No"
                                  value="N"
                                />
                              </RadioButtonGroup>
                            </div> */}
                            <Column lg={1} sm={4}>
                              {rule.actions.length !== 1 && (
                                <IconButton
                                  label={
                                    <FormattedMessage id="rulebuilder.label.removeAction" />
                                  }
                                  className="ruleFieldButton"
                                  kind="danger"
                                  onClick={() =>
                                    handleRuleFieldItemRemove(
                                      index,
                                      action_index,
                                      FIELD.actions,
                                    )
                                  }
                                  size="sm"
                                >
                                  {" "}
                                  <Subtract size={18} />
                                </IconButton>
                              )}
                            </Column>
                            <Column lg={1}>
                              {rule.actions.length - 1 === action_index && (
                                <IconButton
                                  label={
                                    <FormattedMessage id="rulebuilder.label.addAction" />
                                  }
                                  className="ruleFieldButton"
                                  onClick={() =>
                                    handleRuleFieldItemAdd(
                                      index,
                                      FIELD.actions,
                                      actionObj,
                                    )
                                  }
                                  kind="tertiary"
                                  size="sm"
                                >
                                  {" "}
                                  <Add size={18} />
                                </IconButton>
                              )}
                            </Column>
                            <Column lg={16}>
                              {" "}
                              &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp;{" "}
                            </Column>
                            <Column lg={16}>
                              {" "}
                              &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp;{" "}
                            </Column>
                          </Grid>
                        ))}
                      </div>
                      <Button
                        id={"submit_" + index}
                        disabled={isSubmitting}
                        type="submit"
                        kind="tertiary"
                        size="sm"
                      >
                        <FormattedMessage id="label.button.submit" />
                      </Button>
                    </>
                  )}
                </div>
              </Stack>
            </Form>
            {ruleList.length - 1 === index && (
              <IconButton
                onClick={handleRuleAdd}
                label={intl.formatMessage({ id: "rulebuilder.label.addRule" })}
                size="md"
                kind="tertiary"
              >
                <Add size={16} />
                <span>
                  <FormattedMessage id="rulebuilder.label.rule" />
                </span>
              </IconButton>
            )}
          </div>
          <div className="second-division">
            {ruleList.length !== 1 && (
              <ModalWrapper
                modalLabel={
                  <FormattedMessage id="label.button.confirmDelete" />
                }
                handleSubmit={() => handleRuleRemove(index, rule.id)}
                primaryButtonText={
                  <FormattedMessage id="label.button.confirm" />
                }
                secondaryButtonText={
                  <FormattedMessage id="label.button.cancel" />
                }
                modalHeading={
                  <FormattedMessage id="rulebuilder.label.confirmDelete" />
                }
                buttonTriggerText={
                  <FormattedMessage id="rulebuilder.label.removeRule" />
                }
                size="md"
              ></ModalWrapper>
            )}
          </div>
        </div>
      ))}
    </>
  );
}

export default ReflexRule;
