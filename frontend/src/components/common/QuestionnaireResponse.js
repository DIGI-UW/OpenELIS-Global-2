import React from "react";

const QuestionnaireResponse = ({ questionnaireResponse }) => {
  const renderQuestionResponse = (item) => {
    console.debug(JSON.stringify(item));
    return (
      <>
        <td>
          <h6>{item.text}</h6>
        </td>

        <td>
          <div className="font1">
            :&nbsp;
            {item.answer &&
              item.answer.map((answer, index) => {
                return (
                  <>
                    {renderAnswer(answer)}{" "}
                    {item.answer.length > 1 && index < item.answer.length - 1
                      ? " ,"
                      : ""}
                  </>
                );
              })}
          </div>
        </td>
      </>
    );
  };

  const renderAnswer = (answer) => {
    console.debug(JSON.stringify(answer));

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
      <>
        <span className="questionnaireResponseAnswer">{display}</span>
      </>
    );
  };

  return (
    <>
      <table className="my-custom-table">
        <tbody>
          {questionnaireResponse &&
            questionnaireResponse.item.map((item, index) => {
              return <tr key={index}>{renderQuestionResponse(item)}</tr>;
            })}
        </tbody>
      </table>
    </>
  );
};

export default QuestionnaireResponse;
