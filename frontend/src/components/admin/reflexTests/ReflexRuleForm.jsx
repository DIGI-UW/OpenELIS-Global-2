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
  Grid,
  Column,
  Accordion,
  AccordionItem,
} from "@carbon/react";
import { Add, Subtract } from "@carbon/react/icons";
import AutoComplete from "../../common/AutoComplete";
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
    componentId: "",
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
    getFromOpenElisServer("/rest/displayList/SAMPLE_TYPE_ACTIVE", fetchSamples);
    getFromOpenElisServer("/rest/reflexrule-options", fetchRuleOptions);
    // A link from the Test Editor names one rule, so fetch just that rule instead
    // of the whole collection.
    const selectedRuleId = new URLSearchParams(window.location.search).get(
      "id",
    );
    getFromOpenElisServer(
      selectedRuleId
        ? `/rest/reflexrules?id=${encodeURIComponent(selectedRuleId)}`
        : "/rest/reflexrules",
      fetchReflexRules,
    );

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
    let processedAny = false;
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
            processedAny = true;
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
            processedAny = true;
          }
        });
      }
    });
    if (processedAny) {
      setSampleTestList(defaultSampleTests);
      setTestResultList(defaultTestResultList);
      setLoading(false);
    }
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
    if (name === "componentId") {
      // The new component reports its own type and offers its own options, so a
      // value picked from the previous one no longer means anything here.
      list[index][field][itemIndex].value = "0";
      list[index][field][itemIndex].componentPending = false;
    }
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

    // A new test invalidates the component chosen under the old one, and with
    // it the type and the options that were read off that component.
    const rules = [...ruleList];
    const condition = rules[index]?.[field]?.[item_index];
    if (condition) {
      condition.componentId = "";
      condition.value = "0";
      condition.componentPending = true;
      setRuleList(rules);
    }
  };

  /**
   * The components of a condition's test, read from the test the search
   * already returned. Deriving it means an existing rule shows the component
   * it is bound to on load, not only after the test is re-picked.
   */
  const componentsFor = (index, item_index) => {
    const tests =
      (sampleTestList[FIELD.conditions][index] &&
        sampleTestList[FIELD.conditions][index][item_index]) ||
      [];
    const condition = ruleList[index]?.conditions?.[item_index];
    const test = tests.find((t) => String(t.id) === String(condition?.testId));
    return (test?.components || []).map((c) => ({
      id: c.id,
      value: c.value,
      resultType: c.resultType || test?.resultType,
      resultList: c.resultList || [],
      primary: c.primary,
    }));
  };

  /**
   * The options the chosen component offers, falling back to the ones fetched
   * for its test.
   *
   * <p>The test-level list is every component's merged together, so it offers
   * the whole test's vocabulary whichever component a condition names - two
   * coded components with different option sets read identically there. The
   * fallback still matters: a test whose components carry no options of their
   * own reports through the test, which is what a single-component test has
   * always done, and an existing rule renders once before its lists arrive.
   */
  const dictionaryResultsFor = (index, item_index) => {
    const component = selectedComponentFor(index, item_index);
    if (component?.resultList?.length) {
      return component.resultList;
    }
    return testResultList[index]?.[item_index]?.["list"];
  };

  /**
   * The type the condition editor works against: the chosen component's, not
   * the test's. A numeric condition under a coded primary was being offered
   * the dictionary editor and could never match.
   */
  /**
   * The component a condition should show when it names none yet: its test's
   * primary. Legacy rules arrive without one and must not appear unset.
   */
  const defaultComponentFor = (index, item_index) => {
    const list = componentsFor(index, item_index);
    const primary = list.find((c) => c.primary);
    return (primary || list[0])?.id;
  };

  /**
   * The component a condition reads, or undefined while the user has yet to
   * choose one. A test carries components that report different things, so
   * until one is named there is no result type to render against.
   */
  const selectedComponentFor = (index, item_index) => {
    const condition = ruleList[index]?.conditions?.[item_index];
    if (condition?.componentPending) {
      return undefined;
    }
    const effectiveId =
      condition?.componentId || defaultComponentFor(index, item_index);
    return componentsFor(index, item_index).find((c) => c.id === effectiveId);
  };

  const conditionResultType = (index, item_index) => {
    const condition = ruleList[index]?.conditions?.[item_index];
    if (condition?.componentPending) {
      return undefined;
    }
    const component = selectedComponentFor(index, item_index);
    if (component?.resultType) {
      return component.resultType;
    }
    return testResultList[index]?.[item_index]?.type;
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
    const rule = list[index];
    list[index]["active"] = e;
    setRuleList(list);

    if (rule.id != null) {
      const endpoint = e
        ? "/rest/activate-reflexrule/" + rule.id
        : "/rest/deactivate-reflexrule/" + rule.id;
      postToOpenElisServer(endpoint, {}, (status) => {
        if (status != 200) {
          const revert = [...list];
          revert[index]["active"] = !e;
          setRuleList(revert);
          setNotificationVisible(true);
          addNotification({
            kind: NotificationKinds.error,
            title: intl.formatMessage({ id: "notification.title" }),
            message: intl.formatMessage({ id: "save.error.msg" }),
          });
        }
      });
    }
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
    // The picker shows the primary component for a condition that names none,
    // but showing it is not choosing it - the rule has to carry the component
    // it is displaying, or it saves against no component at all.
    const rule = {
      ...ruleList[index],
      conditions: (ruleList[index].conditions || []).map(
        (condition, condition_index) => {
          // componentPending only says whether this condition is waiting on a
          // choice; it describes the editor, not the rule.
          const saved = { ...condition };
          delete saved.componentPending;
          return {
            ...saved,
            componentId:
              condition.componentId ||
              selectedComponentFor(index, condition_index)?.id ||
              null,
          };
        },
      ),
    };
    console.debug(JSON.stringify(rule));
    postToOpenElisServer("/rest/reflexrule", JSON.stringify(rule), (status) =>
      handleSubmited(status, index),
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
                  <Grid>
                    <Column lg={16} md={8} sm={4}>
                      <Grid>
                        <Column lg={3} md={2} sm={4}>
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
                        </Column>

                        <Column lg={4} md={3} sm={4}>
                          <div style={{ whiteSpace: "nowrap" }}>
                            <Toggle
                              toggled={!!rule.active}
                              aria-label="toggle button"
                              id={index + "_toggle"}
                              labelText={
                                <FormattedMessage id="rulebuilder.label.toggleRule" />
                              }
                              onToggle={(e) => toggleRule(e, index)}
                              onClick={handleClick}
                            />
                          </div>
                        </Column>
                        <div>&nbsp; &nbsp; &nbsp; &nbsp;</div>
                        <Column lg={5} md={2} sm={4}>
                          <div>
                            <Checkbox
                              labelText={"Active: " + rule.active}
                              name="active"
                              id={index + "_active"}
                              checked={!!rule.active}
                              disabled
                              readOnly
                            />
                          </div>
                        </Column>
                      </Grid>
                    </Column>
                  </Grid>
                  <div style={{ marginTop: "1rem" }}>
                    <Accordion>
                      <AccordionItem
                        title={
                          <FormattedMessage
                            id="rulebuilder.label.ruleDetails"
                            defaultMessage="Rule details"
                          />
                        }
                      >
                        <div className="section">
                          <Grid>
                            <Column lg={16} sm={3}>
                              <h5>
                                <FormattedMessage id="rulebuilder.label.addRuleConditions" />
                              </h5>
                            </Column>

                            <Column lg={16} sm={3}>
                              {" "}
                              &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp;
                              &nbsp;{" "}
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
                                onChange={(e) =>
                                  handleRuleFieldChange(e, index)
                                }
                                required
                              >
                                <SelectItem text="" value="" />
                                {overallOptions.map(
                                  (overall, overall_index) => (
                                    <SelectItem
                                      text={overall.label}
                                      value={overall.value}
                                      key={overall_index}
                                    />
                                  ),
                                )}
                              </Select>
                            </Column>
                            <Column lg={16}>
                              {" "}
                              &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp;
                              &nbsp;{" "}
                            </Column>
                            <Column lg={16}>
                              {" "}
                              &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp;
                              &nbsp;{" "}
                            </Column>
                          </Grid>
                          {rule.conditions.map((condition, condition_index) => {
                            return (
                              <Grid key={index + "_" + condition_index}>
                                <Column lg={2} md={2} sm={4}>
                                  <Select
                                    data-cy="addSample"
                                    id={
                                      index + "_" + condition_index + "_sample"
                                    }
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

                                <Column lg={4} md={1} sm={4}>
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
                                        ? sampleTestList[FIELD.conditions][
                                            index
                                          ][condition_index]
                                        : []
                                    }
                                    required
                                  />
                                </Column>

                                <Column lg={3} md={1} sm={4}>
                                  <Select
                                    id={
                                      index +
                                      "_" +
                                      condition_index +
                                      "_component"
                                    }
                                    name="componentId"
                                    labelText={
                                      <FormattedMessage id="rulebuilder.label.selectComponent" />
                                    }
                                    value={
                                      selectedComponentFor(
                                        index,
                                        condition_index,
                                      )?.id || ""
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
                                    {!selectedComponentFor(
                                      index,
                                      condition_index,
                                    ) && <SelectItem text="" value="" />}
                                    {componentsFor(index, condition_index).map(
                                      (component, c_index) => (
                                        <SelectItem
                                          text={component.value}
                                          value={component.id}
                                          key={c_index}
                                        />
                                      ),
                                    )}
                                  </Select>
                                </Column>
                                <Column lg={2} md={1} sm={4}>
                                  <Select
                                    value={condition.relation}
                                    id={
                                      index +
                                      "_" +
                                      condition_index +
                                      "_relation"
                                    }
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
                                    {conditionResultType(
                                      index,
                                      condition_index,
                                    ) && (
                                      <>
                                        {conditionResultType(
                                          index,
                                          condition_index,
                                        ) === "N" ? (
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
                                <Column lg={3} md={1} sm={4}>
                                  {conditionResultType(
                                    index,
                                    condition_index,
                                  ) ? (
                                    <>
                                      {conditionResultType(
                                        index,
                                        condition_index,
                                      ) === "D" ? (
                                        <Select
                                          value={condition.value}
                                          id={
                                            index +
                                            "_" +
                                            condition_index +
                                            "_value"
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
                                            {dictionaryResultsFor(
                                              index,
                                              condition_index,
                                            ) && (
                                              <>
                                                {dictionaryResultsFor(
                                                  index,
                                                  condition_index,
                                                ).map(
                                                  (
                                                    result,
                                                    condition_value_index,
                                                  ) => (
                                                    <SelectItem
                                                      text={result.value}
                                                      value={result.id}
                                                      key={
                                                        condition_value_index
                                                      }
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
                                              conditionResultType(
                                                index,
                                                condition_index,
                                              ) === "N"
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
                                              conditionResultType(
                                                index,
                                                condition_index,
                                              ) === "N" ? (
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
                                                conditionResultType(
                                                  index,
                                                  condition_index,
                                                ),
                                                "condition-value_" +
                                                  index +
                                                  "_" +
                                                  condition_index,
                                              );
                                            }}
                                            invalid={validateTextInPut(
                                              condition.value,
                                              conditionResultType(
                                                index,
                                                condition_index,
                                              ),
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
                                          index +
                                          "_" +
                                          condition_index +
                                          "_value"
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
                                {condition.relation === "BETWEEN" && (
                                  <Column lg={2} md={1} sm={4}>
                                    {conditionResultType(
                                      index,
                                      condition_index,
                                    ) && (
                                      <>
                                        {conditionResultType(
                                          index,
                                          condition_index,
                                        ) === "N" &&
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
                                                  conditionResultType(
                                                    index,
                                                    condition_index,
                                                  ),
                                                  "condition-value2_" +
                                                    index +
                                                    "_" +
                                                    condition_index,
                                                );
                                              }}
                                              invalid={validateTextInPut(
                                                condition.value2,
                                                conditionResultType(
                                                  index,
                                                  condition_index,
                                                ),
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
                                )}
                                <Column
                                  lg={2}
                                  md={1}
                                  sm={4}
                                  className="ruleRowActions"
                                >
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
                                  &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp;
                                  &nbsp;{" "}
                                </Column>
                                <Column lg={16}>
                                  {" "}
                                  &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp;
                                  &nbsp;{" "}
                                </Column>
                              </Grid>
                            );
                          })}
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
                                  data-cy="selectSample"
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
                                &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp;
                                &nbsp;{" "}
                              </Column>
                              <Column lg={16}>
                                {" "}
                                &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp;
                                &nbsp;{" "}
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
                      </AccordionItem>
                    </Accordion>
                  </div>
                </div>
              </Stack>
            </Form>
            {ruleList.length - 1 === index && (
              <Button
                data-cy="rule"
                onClick={handleRuleAdd}
                size="lg"
                kind="tertiary"
                renderIcon={Add}
                style={{ marginLeft: "30px", marginTop: "1rem" }}
              >
                <FormattedMessage id="rulebuilder.label.addRule" />
              </Button>
            )}
          </div>
        </div>
      ))}
    </>
  );
}

export default ReflexRule;
