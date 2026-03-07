import React from "react";

const QuestionnaireResponse = ({ questionnaireResponse }) => {

  const rowMapping = [
    ["Lab Number", "Request Date", "Unit Number", "Private Reference Number"],
    ["Specimen", "Specimen Type", "Nature/Site of Specimen"],
    ["Referring Provider", "Procedure Performed", "Assigned Technician"],
    ["Provisional Clinical Diagnosis"],
    ["Previous Surgery / Treatment"],
    ["Clinical History"],
    ["Assigned Pathologist", "Current Status"]
  ];

  if (!questionnaireResponse || !questionnaireResponse.item) return null;

  
  const itemsByText = {};
  
  for (let index = 0; index < questionnaireResponse.item.length; index++) {
    const item = questionnaireResponse.item[index];
    itemsByText[item.text] = item;
  }

  const renderQuestionResponse = (item, fallbackText) => {
    const questionText = item?.text || fallbackText;
    const answers = item?.answer || [];
    const answerElements = [];

    if (answers.length > 0) {
      for (let index = 0; index < answers.length; index++) {
        const answer = answers[index];
        answerElements.push(
          <React.Fragment key={index}>
            {renderAnswer(answer)}
            {answers.length > 1 && index < answers.length - 1 ? ", " : ""}
          </React.Fragment>
        );
      }
    }

    return (
      <div style={{ display: "flex", flexDirection: "column", flex: 1 }}>
        <span style={{ fontSize: "12px", color: "#525252", fontWeight: 600, marginBottom: "0.25rem" }}>
          {questionText}
        </span>
        <span style={{ fontSize: "14px", color: "#161616" }}>
          {answers.length > 0 ? answerElements : "-"}
        </span>
      </div>
    );
  };

  const renderAnswer = (answer) => {
    var display = "";
    if ("valueString" in answer) {
      display = answer.valueString;
    } else if ("valueBoolean" in answer) {
      display = answer.valueBoolean ? "True" : "False";
    } else if ("valueCoding" in answer) {
      display = answer.valueCoding.display;
    } else if ("valueDate" in answer) {
      display = answer.valueDate;
    } else if ("valueDecimal" in answer) {
      display = answer.valueDecimal;
    } else if ("valueInteger" in answer) {
      display = answer.valueInteger;
    } else if ("valueQuantity" in answer) {
      display = answer.valueQuantity.value + answer.valueQuantity.unit;
    } else if ("valueTime" in answer) {
      display = answer.valueTime;
    }
    return (
      <span className="questionnaireResponseAnswer">{display}</span>
    );
  };

  const renderedRows = [];

  for (let rowIndex = 0; rowIndex < rowMapping.length; rowIndex++) {
    const rowArr = rowMapping[rowIndex];
    const renderedItems = [];

    for (let itemIndex = 0; itemIndex < rowArr.length; itemIndex++) {
      const text = rowArr[itemIndex];
      const item = itemsByText[text];

      renderedItems.push(
        <React.Fragment key={itemIndex}>
          {renderQuestionResponse(item, text)}
        </React.Fragment>
      );
    }

    renderedRows.push(
      <div key={rowIndex} className="questionnaireResponseRow">
        {renderedItems}
      </div>
    );
  }
  
  return (
    <div className="questionnaireResponseContainer">
      {renderedRows}
    </div>
  );
};

export default QuestionnaireResponse;
