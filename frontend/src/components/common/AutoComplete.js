import React, { useState, useEffect } from "react";
import "../admin/reflexTests/ReflexStyles.css";
import { TextInput } from "@carbon/react";

function AutoComplete(props) {
  const allowFreeText = props.allowFreeText;

  const [textValue, setTextValue] = useState("");
  const [activeSuggestion, setActiveSuggestion] = useState(0);
  const [filteredSuggestions, setFilteredSuggestions] = useState([]);
  const [showSuggestions, setShowSuggestions] = useState(false);
  const [userInput, setUserInput] = useState("");
  const [invalid, setInvalid] = useState(false);
  const [innitialised, setInnitialised] = useState(false);

  useEffect(() => {
    if (props.value && !innitialised) {
      if (props.suggestions) {
        var filteredSuggestion = props.suggestions.filter(
          (suggestion) =>
            suggestion.id == props.value || suggestion.id === props.value,
        );
        if (filteredSuggestion[0]) {
          setTextValue(filteredSuggestion[0].value);
        } else {
          setTextValue(props.value);
        }
      }
    }
  }, [props]);

  const onChange = (e) => {
    const { suggestions } = props;
    const userInput = e.currentTarget.value;
    setTextValue(userInput);
    const filteredSuggestions = suggestions.filter(
      (suggestion) =>
        suggestion.value.toLowerCase().indexOf(userInput.toLowerCase()) > -1,
    );

    setActiveSuggestion(0);
    setFilteredSuggestions(filteredSuggestions);
    setUserInput(e.currentTarget.value);
    setShowSuggestions(true);
    setInnitialised(true);

    if (filteredSuggestions.length == 0 && !allowFreeText) {
      setInvalid(true);
    }
    if (typeof props.onChange === "function") {
      props.onChange(e);
    }
  };

  const onClick = (e, id, suggestion) => {
    const { onSelect } = props;
    setTextValue(suggestion.value);
    setActiveSuggestion(0);
    setFilteredSuggestions([]);
    setUserInput(e.currentTarget.innerText);
    setShowSuggestions(false);
    setInvalid(false);

    if (typeof onSelect === "function") {
      onSelect(id);
    }
  };

  const onKeyDown = (e) => {
    // Handeling enter key
    const { onSelect } = props;
    if (e.keyCode === 13) {
      if (filteredSuggestions[activeSuggestion]) {
        const selectedValue = filteredSuggestions[activeSuggestion].value;
        setUserInput(selectedValue);
        setTextValue(selectedValue);
        setShowSuggestions(false);
        setInvalid(false);

        if (typeof onSelect === "function") {
          onSelect(filteredSuggestions[activeSuggestion].id);
        }
      }
    }
    // Handeling up arrow
    else if (e.keyCode === 38) {
      if (activeSuggestion === 0) {
        return;
      }
      setActiveSuggestion(activeSuggestion - 1);
    }
    // Handeling down arrow
    else if (e.keyCode === 40) {
      if (activeSuggestion === filteredSuggestions.length - 1) {
        return;
      }
      setActiveSuggestion(activeSuggestion + 1);
    }
  };

  let suggestionsListComponent;
  if (showSuggestions && userInput) {
    if (filteredSuggestions.length) {
      suggestionsListComponent = (
        <div className="suggestions-container">
          <ul className="suggestions">
            {filteredSuggestions.map((suggestion, index) => {
              let className;
              // Flag the active suggestion with a class
              if (index === activeSuggestion) {
                className = "suggestion-active";
              }
              return (
                <li
                  className={className}
                  key={index}
                  onClick={(e) => onClick(e, suggestion.id, suggestion)}
                >
                  {suggestion.value}
                </li>
              );
            })}
          </ul>
        </div>
      );
    } else {
      suggestionsListComponent = (
        <div className="no-suggestions">
          <em>No suggestions available.</em>
        </div>
      );
    }
  }

  return (
    <>
      <TextInput
        type="text"
        id={props.id}
        name={props.name}
        labelText={props.label ? props.label : ""}
        className={props.class}
        onChange={onChange}
        onKeyDown={onKeyDown}
        value={textValue}
        invalid={invalid}
        required={props.required ? props.required : false}
        invalidText={props.invalidText}
      />
      {suggestionsListComponent}
    </>
  );
}

export default AutoComplete;
