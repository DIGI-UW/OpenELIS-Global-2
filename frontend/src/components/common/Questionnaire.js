import React from "react";
import { useIntl } from "react-intl";
import {
  FilterableMultiSelect,
  Select,
  SelectItem,
  TextInput,
} from "@carbon/react";

const Questionnaire = ({
  questionnaire,
  onAnswerChange = () => {
    console.debug("default onAnswerChange function does nothing");
  },
  getAnswer = () => {
    console.debug("default getAnswer function does nothing");
  },
}) => {
  const intl = useIntl();

  const getSelectOption = (answerOption, index) => {
    if ("valueString" in answerOption) {
      return (
        <SelectItem
          key={index}
          value={answerOption.valueString}
          text={answerOption.valueString}
        />
      );
    } else if ("valueCoding" in answerOption) {
      return (
        <SelectItem
          key={index}
          value={answerOption.valueCoding.code}
          text={answerOption.valueCoding.display}
        />
      );
    } else {
      return <></>;
    }
  };

  const renderQuestion = (item) => {
    var options = [];
    if (
      item.type == "choice" &&
      item.repeats === true &&
      "answerOption" in item
    ) {
      item.answerOption.map((answerOption, index) => {
        if ("valueString" in answerOption) {
          options.push({
            value: answerOption.valueString,
            text: answerOption.valueString,
            id: index,
          });
        }
        if ("valueCoding" in answerOption) {
          options.push({
            value: answerOption.valueCoding.code,
            text: answerOption.valueCoding.display,
            id: index,
          });
        }
      });
    }

    return (
      <>
        <div className="inputText">
          {item.type == "boolean" && (
            <Select
              id={item.linkId}
              className="inputText"
              labelText={item.text}
              onChange={onAnswerChange}
              value={getAnswer(item.linkId)}
            >
              <SelectItem
                disabled
                value=""
                text={intl.formatMessage({ id: "select.default.option.label" })}
              />
              <SelectItem value="" text="" />
              <SelectItem
                value="true"
                text={intl.formatMessage({ id: "yes.option" })}
              />
              <SelectItem
                value="false"
                text={intl.formatMessage({ id: "no.option" })}
              />
            </Select>
          )}
          {item.type == "choice" && item.repeats !== true && (
            <Select
              id={item.linkId}
              labelText={item.text}
              defaultValue={
                "inital" in item
                  ? "valueString" in item.initial[0]
                    ? item.initial[0].valueString
                    : "valueCoding" in item.initial[0]
                      ? item.initial[0].valueCoding
                      : ""
                  : ""
              }
              value={getAnswer(item.linkId)}
              onChange={onAnswerChange}
            >
              <SelectItem
                disabled
                value=""
                text={intl.formatMessage({ id: "select.default.option.label" })}
              />
              <SelectItem value="" text="" />
              {"answerOption" in item &&
                item.answerOption.map((answerOption, index) =>
                  getSelectOption(answerOption, index),
                )}
            </Select>
          )}
          {item.type == "choice" && item.repeats === true && (
            <FilterableMultiSelect
              id={item.linkId}
              titleText={item.text}
              label=""
              items={options}
              itemToString={(option) => (option ? option.text : "")}
              onChange={(changes) => {
                var e = { target: {} };
                e.target.id = item.linkId;
                e.target.value = changes.selectedItems;
                onAnswerChange(e);
              }}
              value={getAnswer(item.linkId)}
              selectionFeedback="top-after-reopen"
            />
          )}
          {item.type == "integer" && (
            <TextInput
              id={item.linkId}
              labelText={item.text}
              onChange={onAnswerChange}
              value={getAnswer(item.linkId)}
              type="number"
              step="1"
              pattern="\d+"
            />
          )}
          {item.type == "decimal" && (
            <TextInput
              id={item.linkId}
              labelText={item.text}
              onChange={onAnswerChange}
              value={getAnswer(item.linkId)}
              type="number"
              step="0.01"
            />
          )}
          {item.type == "date" && (
            <TextInput
              id={item.linkId}
              labelText={item.text}
              onChange={onAnswerChange}
              value={getAnswer(item.linkId)}
              type="date"
            />
          )}
          {item.type == "time" && (
            <TextInput
              id={item.linkId}
              labelText={item.text}
              onChange={onAnswerChange}
              value={getAnswer(item.linkId)}
              type="time"
            />
          )}
          {item.type == "string" && (
            <TextInput
              id={item.linkId}
              labelText={item.text}
              onChange={onAnswerChange}
              value={getAnswer(item.linkId)}
              type="text"
            />
          )}
          {item.type == "text" && (
            <TextInput
              id={item.linkId}
              labelText={item.text}
              onChange={onAnswerChange}
              value={getAnswer(item.linkId)}
              type="text"
            />
          )}
          {item.type == "quantity" && (
            <TextInput
              id={item.linkId}
              labelText={item.text}
              onChange={onAnswerChange}
              value={getAnswer(item.linkId)}
              type="number"
            />
          )}
        </div>
      </>
    );
  };
  if (questionnaire) {
    var inputs =
      "item" in questionnaire &&
      questionnaire.item.map((item, index) => {
        return <span key={index}>{renderQuestion(item)}</span>;
      });

    var groups = [];
    var children = [];
    var i = 0;
    for (; i < inputs.length; i++) {
      children.push(inputs[i]);
      if (children.length === 2) {
        groups.push(
          <div className="formInlineDiv" key={"group_" + i}>
            {children}
          </div>,
        );
        children = [];
      }
    }
    if (children.length > 0) {
      groups.push(
        <div className="formInlineDiv" key={"group_" + i}>
          {children}
        </div>,
      );
    }

    return <div className="extraQuestions">{groups}</div>;
  } else {
    return <></>;
  }
};

export default Questionnaire;
