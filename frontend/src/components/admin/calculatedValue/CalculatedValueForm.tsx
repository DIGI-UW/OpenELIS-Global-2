import React, { useCallback } from "react";
import { useContext, useState, useEffect, useRef } from "react";
import {
  Form,
  Stack,
  TextInput,
  Select,
  SelectItem,
  Button,
  IconButton,
  Toggle,
  Loading,
  Checkbox,
  ModalWrapper,
  Grid,
  Column,
  Section,
  Heading,
  TextArea,
} from "@carbon/react";
import AutoComplete from "../../common/AutoComplete.js";
import { Add, Subtract, Save } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  CalculatedValueFormValues,
  CalculatedValueFormModel,
  OperationType,
  OperationModel,
} from "../../formModel/innitialValues/CalculatedValueFormSchema";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
} from "../../utils/Utils.js";
import { NotificationContext } from "../../layout/Layout";
import {
  AlertDialog,
  NotificationKinds,
} from "../../common/CustomNotification";
import PageBreadCrumb from "../../common/PageBreadCrumb";

const breadcrumbs = [{ label: "home.label", link: "/" }];
interface CalculatedValueProps {}

type TestListField = "TEST_RESULT" | "FINAL_RESULT";
interface SampleTestListInterface {
  TEST_RESULT: { [key: number]: { [key: number]: Array<TestResponse> } };
  FINAL_RESULT: { [key: number]: Array<TestResponse> };
}

interface TestResponse {
  id: number;
  value: string;
  resultType: string;
  resultList: Array<IdValue>;
}

interface IdValue {
  id: number;
  value: string;
}
interface NotificationContextType {
  notificationVisible: boolean;
  setNotificationVisible: (visible: boolean) => void;
  addNotification: (body: NotificationBody) => void;
}

interface NotificationBody {
  kind: any;
  title: string | JSX.Element;
  message: string | JSX.Element;
}

const TestListObj: SampleTestListInterface = {
  TEST_RESULT: {},
  FINAL_RESULT: {},
};

const mathFunction: IdValue = {
  id: null,
  value: null,
};

const CalculatedValue: React.FC<CalculatedValueProps> = () => {
  const componentMounted = useRef(true);
  const [calculationList, setCalculationList] = useState([
    CalculatedValueFormValues,
  ]);
  const [sampleList, setSampleList] = useState([]);
  const [sampleTestList, setSampleTestList] = useState(TestListObj);
  const [loading, setLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext<NotificationContextType>(NotificationContext);
  const [mathFunctions, setMathFunctions] = useState([mathFunction]);
  const intl = useIntl();

  useEffect(() => {
    getFromOpenElisServer("/rest/samples", fetchSamples);
    getFromOpenElisServer("/rest/test-calculations", loadCalculationList);
    getFromOpenElisServer("/rest/math-functions", loadMathFunctions);

    return () => {
      // This code runs when component is unmounted
      componentMounted.current = false;
    };
  }, []);

  const loadCalculationList = (calculations) => {
    if (componentMounted.current) {
      // console.log(JSON.stringify(reflexRuleList))
      const sampleList = [];
      if (calculations.length > 0) {
        setCalculationList(calculations);

        calculations.forEach((calculation, index) => {
          if (calculation.sampleId) {
            sampleList.push(calculation.sampleId);
          }

          calculation.operations.forEach((operation, opeartionIdex) => {
            if (operation.sampleId) {
              sampleList.push(operation.sampleId);
            }
          });
        });
        getFromOpenElisServer(
          "/rest/test-display-beans-map?samplesTypes=" + sampleList.join(","),
          (resp) => buildSampleTests(resp, calculations),
        );
      }
      setLoading(false);
    }
  };

  const buildSampleTests = (sampleTestsMap, calculations) => {
    if (calculations.length > 0) {
      setCalculationList(calculations);

      calculations.forEach((calculation, index) => {
        if (calculation.sampleId) {
          sampleList.push(calculation.sampleId);
          fetchTests(
            sampleTestsMap[calculation.sampleId],
            "FINAL_RESULT",
            index,
            0,
          );
        }

        calculation.operations.forEach((operation, opeartionIdex) => {
          if (operation.sampleId) {
            fetchTests(
              sampleTestsMap[operation.sampleId],
              "TEST_RESULT",
              index,
              opeartionIdex,
            );
          }
        });
      });
    }
  };

  const loadMathFunctions = (functions) => {
    setMathFunctions(functions);
  };

  const fetchSamples = (sampleList) => {
    if (componentMounted.current) {
      setSampleList(sampleList);
    }
  };

  const CalculatedValueObj: CalculatedValueFormModel = {
    id: null,
    name: null,
    sampleId: null,
    testId: null,
    result: null,
    note: null,
    toggled: true,
    active: true,
    operations: [
      {
        id: null,
        order: null,
        type: "TEST_RESULT",
        value: null,
        sampleId: null,
      },
    ],
  };

  const handleRuleAdd = () => {
    setCalculationList([...calculationList, CalculatedValueObj]);
  };

  const handleRuleRemove = (index, id) => {
    if (id) {
      postToOpenElisServer(
        "/rest/deactivate-test-calculation/" + id,
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

  const addOperation = (index: number, type: OperationType) => {
    const list = [...calculationList];
    const operation: OperationModel = {
      id: null,
      order: null,
      type: type,
      value: null,
      sampleId: null,
    };

    list[index]["operations"].push(operation);
    console.log(JSON.stringify(list[index]["operations"]));
    setCalculationList(list);
  };

  const insertOperation = (
    index: number,
    operationIndex: number,
    type: OperationType,
  ) => {
    const operation: OperationModel = {
      id: null,
      order: null,
      type: type,
      value: null,
      sampleId: null,
    };
    const list = [...calculationList];
    //list[index]['operations'].push(operation);
    list[index]["operations"].splice(operationIndex + 1, 0, operation);
    console.log(JSON.stringify(list[index]["operations"]));
    setCalculationList(list);
  };

  const removeOperation = (index: number, operationIndex: number) => {
    const list = [...calculationList];
    list[index]["operations"].splice(operationIndex, 1);
    setCalculationList(list);
  };

  const handleSampleSelected = (
    e: any,
    field: TestListField,
    index: number,
    item_index: number,
  ) => {
    const { value } = e.target;
    getFromOpenElisServer(
      "/rest/test-display-beans?sampleType=" + value,
      (resp) => fetchTests(resp, field, index, item_index),
    );
  };

  const loadSampleTestList = (
    field: TestListField,
    index: number,
    item_index: number,
    resultList: any,
  ) => {
    const results = { ...sampleTestList };
    if (!results[field][index]) {
      results[field][index] = {};
    }
    switch (field) {
      case "TEST_RESULT":
        results[field][index][item_index] = resultList.filter(
          (result) => result.resultType === "N",
        );
        break;
      case "FINAL_RESULT":
        results[field][index] = resultList;
        break;
    }
    setSampleTestList(results);
  };

  const fetchTests = (
    testList: any,
    field: TestListField,
    index: number,
    item_index: number,
  ) => {
    loadSampleTestList(field, index, item_index, testList);
  };

  function handleTestSelection(id: number, index: number) {
    const list = [...calculationList];
    list[index].testId = id;
    setCalculationList(list);
  }

  function handleOperationTestSelection(
    id: number,
    index: number,
    operationIndex: number,
  ) {
    const list = [...calculationList];
    list[index].operations[operationIndex].value = id;
    setCalculationList(list);
  }

  const handleCalculationFieldChange = (e: any, index: number) => {
    const { name, value } = e.target;
    const list = [...calculationList];
    list[index][name] = value;
    setCalculationList(list);
  };

  const handleOperationFieldChange = (
    e: any,
    index: number,
    operationIndex: number,
  ) => {
    const { name, value } = e.target;
    const list = [...calculationList];
    list[index]["operations"][operationIndex][name] = value;
    setCalculationList(list);
  };

  const handleCalculationSubmited = (status, index) => {
    setIsSubmitting(false);
    setNotificationVisible(true);
    if (status == "200") {
      const element = document.getElementById(
        "submit_" + index,
      ) as HTMLInputElement;
      element.disabled = true;
      addNotification({
        kind: NotificationKinds.success,
        title: intl.formatMessage({ id: "notification.title" }),
        message: "Succesfuly saved",
      });
    } else {
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: "Duplicate Calculation Name or Error while saving",
      });
    }
  };

  function replaceString(
    string: string,
    sequenceToReplace: string,
    replacement: string,
  ) {
    const regex = new RegExp(sequenceToReplace, "g");
    return string.replace(regex, replacement);
  }

  const handleSubmit = (event: any, index: number) => {
    event.preventDefault();
    if (isSubmitting) {
      return;
    }
    setIsSubmitting(true);
    let mathematicalOperation = "";
    calculationList[index]["operations"].forEach(
      (operation, operationIndex) => {
        operation.order = operationIndex;
        mathematicalOperation = mathematicalOperation + operation.value + " ";
      },
    );
    // for the function validation , remove text values
    mathematicalOperation = replaceString(mathematicalOperation, "AGE", "0");
    mathematicalOperation = replaceString(mathematicalOperation, "WEIGHT", "0");
    mathematicalOperation = replaceString(
      mathematicalOperation,
      "IS_IN_NORMAL_RANGE",
      ">=0 && 1<=10",
    );
    mathematicalOperation = replaceString(
      mathematicalOperation,
      "IS_OUTSIDE_NORMAL_RANGE",
      "<0 || 1>10",
    );

    try {
      // Code that might throw an error
      eval(mathematicalOperation);
      console.log(JSON.stringify(calculationList[index]));
      postToOpenElisServer(
        "/rest/test-calculation",
        JSON.stringify(calculationList[index]),
        (status) => handleCalculationSubmited(status, index),
      );
    } catch (error) {
      setNotificationVisible(true);
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: "Invalid Calculation Logic : " + error.message,
      });
    }
  };
  function getOperationInputByType(
    index: number,
    operationIndex: number,
    type: OperationType,
    operation: OperationModel,
  ) {
    switch (type) {
      case "TEST_RESULT":
        return (
          <>
            <Column lg={5}>
              <Select
                id={index + "_" + operationIndex + "_sample"}
                name="sampleId"
                labelText={
                  <FormattedMessage id="rulebuilder.label.selectSample" />
                }
                value={operation.sampleId}
                onChange={(e) => {
                  handleSampleSelected(e, "TEST_RESULT", index, operationIndex);
                  handleOperationFieldChange(
                    e,
                    index,
                    operationIndex,
                  ); /*resetOperationValue(index, operationIndex, operation)*/
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
            <Column lg={5}>
              <AutoComplete
                id={index + "_" + operationIndex + "_testresult"}
                label={
                  <FormattedMessage id="testcalculation.label.searchNumericTest" />
                }
                name="operationtestName"
                value={operation.value}
                onSelect={(id) =>
                  handleOperationTestSelection(id, index, operationIndex)
                }
                suggestions={
                  sampleTestList["TEST_RESULT"][index]
                    ? sampleTestList["TEST_RESULT"][index][operationIndex]
                    : []
                }
              ></AutoComplete>
            </Column>
          </>
        );
      case "MATH_FUNCTION":
        return (
          <>
            <Column lg={5}>
              <Select
                id={index + "_" + operationIndex + "_mathfunction"}
                name="value"
                labelText={
                  <FormattedMessage id="testcalculation.label.mathFucntion" />
                }
                value={operation.value}
                onChange={(e) => {
                  handleOperationFieldChange(e, index, operationIndex);
                }}
                required
              >
                <SelectItem text="" value="" />
                {mathFunctions.map((fn, fn_index) => (
                  <SelectItem text={fn.value} value={fn.id} key={fn_index} />
                ))}
              </Select>
            </Column>
            <Column lg={5}> </Column>
          </>
        );
      case "INTEGER":
        return (
          <>
            <Column lg={5}>
              <TextInput
                name="value"
                type="number"
                id={index + "_" + operationIndex + "_integer"}
                step="any"
                labelText={
                  <FormattedMessage id="testcalculation.label.integer" />
                }
                value={operation.value}
                onChange={(e) => {
                  handleOperationFieldChange(e, index, operationIndex);
                }}
              />
            </Column>
            <Column lg={5}> </Column>
          </>
        );
      case "PATIENT_ATTRIBUTE":
        return (
          <>
            <Column lg={5}>
              <Select
                id={index + "_" + operationIndex + "_patientattribute"}
                name="value"
                labelText={
                  <FormattedMessage id="testcalculation.label.patientAttribute" />
                }
                value={operation.value}
                onChange={(e) => {
                  handleOperationFieldChange(e, index, operationIndex);
                }}
                required
              >
                <SelectItem text="" value="" />
                <SelectItem text="Patient Age(Years)" value="AGE" />
                <SelectItem text="Patient Weight(Kg)" value="WEIGHT" />
              </Select>
            </Column>
            <Column lg={5}> </Column>
          </>
        );
    }
  }

  function getResultInputByResultType(
    resultType: string,
    index: number,
    calculation: CalculatedValueFormModel,
  ) {
    switch (resultType) {
      case "D":
        return (
          <div>
            <Select
              id={index + "_resultdictionary"}
              name="result"
              labelText={
                <FormattedMessage id="testcalculation.label.selectDictionaryValue" />
              }
              value={calculation.result}
              className="inputSelect"
              onChange={(e) => {
                handleCalculationFieldChange(e, index);
              }}
              required
            >
              <SelectItem text="" value="" />
              {sampleTestList["FINAL_RESULT"][index]
                .filter((test) => test.id == calculation.testId)[0]
                .resultList.map((result, result_index) => (
                  <SelectItem
                    text={result.value}
                    value={result.id}
                    key={result_index}
                  />
                ))}
            </Select>
          </div>
        );

      case "A":
      case "R":
        return (
          <div>
            <TextInput
              name="result"
              className="inputText"
              id={index + "_resultfreetext"}
              labelText={
                <FormattedMessage id="testcalculation.label.textValue" />
              }
              value={calculation.result}
              onChange={(e) => {
                handleCalculationFieldChange(e, index);
              }}
            />
          </div>
        );
    }
  }
  const addOperationBySelect = (
    e: any,
    index: number,
    operationIndex: number,
  ) => {
    const { value } = e.target;
    insertOperation(index, operationIndex, value);
  };

  const toggleCalculation = (e, index) => {
    const list = [...calculationList];
    list[index]["toggled"] = e;
    setCalculationList(list);
  };

  return (
    <div className="adminPageContent">
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <Grid>
        <Column lg={16}>
          <Section>
            <Heading>
              <FormattedMessage id="sidenav.label.admin.testmgt.calculated" />
            </Heading>
          </Section>
        </Column>
      </Grid>
      {notificationVisible === true ? <AlertDialog /> : ""}
      {loading && <Loading></Loading>}
      {calculationList.map((calculation, index) => (
        <div key={index} className="rules">
          <div className="first-division">
            <Form onSubmit={(e) => handleSubmit(e, index)}>
              <Stack gap={7}>
                <div className="ruleBody">
                  <div className="inlineDiv">
                    <div>
                      <TextInput
                        required
                        name="name"
                        className="reflexInputText"
                        type="text"
                        id={index + "_name"}
                        labelText={
                          <FormattedMessage id="testcalculation.label.name" />
                        }
                        value={calculation.name}
                        onChange={(e) => handleCalculationFieldChange(e, index)}
                      />
                    </div>
                    <div>&nbsp; &nbsp;</div>
                    <div>
                      <Toggle
                        toggled={calculation.toggled}
                        aria-label="toggle button"
                        id={index + "_toggle"}
                        labelText={
                          <FormattedMessage id="rulebuilder.label.toggleRule" />
                        }
                        onToggle={(e) => toggleCalculation(e, index)}
                      />
                    </div>
                    <div>&nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp;</div>
                    <div>
                      <Checkbox
                        labelText={"Active: " + calculation.active}
                        name="active"
                        id={index + "_active"}
                        checked={calculation.active}
                        disabled={calculation.active}
                        onChange={(e) => {
                          const list = [...calculationList];
                          list[index]["active"] = e.target.checked;
                          setCalculationList(list);
                        }}
                      />
                    </div>
                  </div>
                  {calculation.toggled && (
                    <>
                      <div className="inlineDiv">
                        <FormattedMessage id="label.button.add" /> &nbsp; &nbsp;
                        <div>
                          <Button
                            renderIcon={Add}
                            id={index + "_testresult"}
                            kind="tertiary"
                            size="sm"
                            onClick={() => addOperation(index, "TEST_RESULT")}
                          >
                            <FormattedMessage id="testcalculation.label.testResult" />
                          </Button>
                        </div>
                        <div>&nbsp; &nbsp;</div>
                        <div>
                          <Button
                            renderIcon={Add}
                            id={index + "_mathfunction"}
                            kind="tertiary"
                            size="sm"
                            onClick={() => addOperation(index, "MATH_FUNCTION")}
                          >
                            <FormattedMessage id="testcalculation.label.mathFucntion" />
                          </Button>
                        </div>
                        <div>&nbsp; &nbsp;</div>
                        <div>
                          <Button
                            renderIcon={Add}
                            id={index + "_integer"}
                            kind="tertiary"
                            size="sm"
                            onClick={() => addOperation(index, "INTEGER")}
                          >
                            <FormattedMessage id="testcalculation.label.integer" />
                          </Button>
                        </div>
                        <div>&nbsp; &nbsp;</div>
                        <div>
                          <Button
                            renderIcon={Add}
                            id={index + "_patientattribute"}
                            kind="tertiary"
                            size="sm"
                            onClick={() =>
                              addOperation(index, "PATIENT_ATTRIBUTE")
                            }
                          >
                            <FormattedMessage id="testcalculation.label.patientAttribute" />
                          </Button>
                        </div>
                      </div>
                      <div className="section">
                        <div className="inlineDiv">
                          <h5>
                            <FormattedMessage id="testcalculation.label.calculation" />
                          </h5>
                        </div>
                        <div className="section">
                          <div className="inlineDiv">
                            &nbsp;{" "}
                            {calculation.operations.map(
                              (operation, operationIndex) => (
                                <div key={index + "_" + operationIndex}>
                                  {operation.type === "TEST_RESULT" &&
                                  operation.value
                                    ? "'"
                                    : ""}
                                  {operation.type === "TEST_RESULT"
                                    ? sampleTestList["TEST_RESULT"][index]
                                      ? sampleTestList["TEST_RESULT"][index][
                                          operationIndex
                                        ]?.filter(
                                          (test) => test.id == operation.value,
                                        )[0]?.value + "'"
                                      : ""
                                    : operation.value}{" "}
                                  &nbsp;
                                </div>
                              ),
                            )}{" "}
                            {<b style={{ color: "red" }}>{" => "}</b>} &nbsp;{" "}
                            {calculation.testId ? "'" : ""}
                            {sampleTestList["FINAL_RESULT"][index]
                              ? sampleTestList["FINAL_RESULT"][index]?.filter(
                                  (test) => test.id == calculation.testId,
                                )[0]?.value + "'"
                              : ""}
                          </div>
                        </div>
                        <Grid>
                          <Column lg={16}>
                            {" "}
                            &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp;{" "}
                          </Column>
                          <Column lg={16}>
                            {" "}
                            &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp;{" "}
                          </Column>
                        </Grid>
                        {calculation.operations.map(
                          (operation, operation_index) => (
                            <Grid key={index + "_" + operation_index}>
                              {getOperationInputByType(
                                index,
                                operation_index,
                                operation.type,
                                operation,
                              )}
                              <Column lg={2}>
                                {operation.type !== "" && (
                                  <IconButton
                                    renderIcon={Subtract}
                                    id={index + "_removeoperation"}
                                    kind="danger"
                                    label=""
                                    size="sm"
                                    onClick={() =>
                                      removeOperation(index, operation_index)
                                    }
                                  />
                                )}
                              </Column>
                              <Column lg={4}>
                                <Select
                                  id={
                                    index +
                                    "_" +
                                    operation_index +
                                    "_addopeartion"
                                  }
                                  name="addoperation"
                                  labelText={
                                    <FormattedMessage id="testcalculation.label.insertOperation" />
                                  }
                                  value={calculation.sampleId}
                                  className="inputSelect"
                                  onChange={(e) => {
                                    addOperationBySelect(
                                      e,
                                      index,
                                      operation_index,
                                    );
                                  }}
                                >
                                  <SelectItem text="" value="" />
                                  <SelectItem
                                    text="Test Result"
                                    value="TEST_RESULT"
                                  />
                                  <SelectItem
                                    text="Mathematical Function"
                                    value="MATH_FUNCTION"
                                  />
                                  <SelectItem text="Integer" value="INTEGER" />
                                  <SelectItem
                                    text="Patient Attribute"
                                    value="PATIENT_ATTRIBUTE"
                                  />
                                </Select>
                                {/* )} */}
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
                          ),
                        )}
                      </div>
                      <div className="section">
                        <div className="inlineDiv">
                          <h6>
                            <FormattedMessage id="testcalculation.label.finalresult" />
                          </h6>
                        </div>
                        <div className="inlineDiv">
                          <div>
                            <Select
                              id={index + "_sample"}
                              name="sampleId"
                              labelText={
                                <FormattedMessage id="rulebuilder.label.selectSample" />
                              }
                              value={calculation.sampleId}
                              className="inputSelect"
                              onChange={(e) => {
                                handleSampleSelected(
                                  e,
                                  "FINAL_RESULT",
                                  index,
                                  0,
                                );
                                handleCalculationFieldChange(
                                  e,
                                  index,
                                ); /*resetCalculationValue(index, calculation)*/
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
                          </div>
                          <div>
                            <AutoComplete
                              id={index + "_finalresult"}
                              class="inputText"
                              label="Final Result"
                              name="testName"
                              onSelect={(id) => handleTestSelection(id, index)}
                              value={calculation.testId}
                              suggestions={
                                sampleTestList["FINAL_RESULT"][index]
                                  ? sampleTestList["FINAL_RESULT"][index]
                                  : []
                              }
                            ></AutoComplete>
                          </div>
                          <div>&nbsp; &nbsp;</div>
                          {sampleTestList["FINAL_RESULT"][index] && (
                            <>
                              {getResultInputByResultType(
                                sampleTestList["FINAL_RESULT"][index].filter(
                                  (test) => test.id == calculation.testId,
                                )[0]?.resultType,
                                index,
                                calculation,
                              )}
                            </>
                          )}
                          <div>&nbsp; &nbsp;</div>
                          <div>
                            <TextArea
                              name="note"
                              id={index + "_note"}
                              rows={1}
                              labelText={
                                <FormattedMessage id="rulebuilder.label.addExternalNote" />
                              }
                              value={calculation.note}
                              onChange={(e) => {
                                handleCalculationFieldChange(e, index);
                              }}
                            />
                          </div>
                        </div>
                      </div>
                      <Button
                        renderIcon={Save}
                        id={"submit_" + index}
                        type="submit"
                        kind="primary"
                        size="sm"
                        disabled={isSubmitting}
                      >
                        <FormattedMessage id="label.button.submit" />
                      </Button>
                    </>
                  )}
                </div>
              </Stack>
            </Form>
            {calculationList.length - 1 === index && (
              <IconButton
                onClick={handleRuleAdd}
                label={<FormattedMessage id="rulebuilder.label.addRule" />}
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
            {calculationList.length !== 1 && (
              <ModalWrapper
                modalLabel={
                  <FormattedMessage id="label.button.confirmDelete" />
                }
                handleSubmit={() => handleRuleRemove(index, calculation.id)}
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
    </div>
  );
};

export default CalculatedValue;
