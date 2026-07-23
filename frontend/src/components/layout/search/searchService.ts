import {
  useState,
  useEffect,
  type ChangeEvent,
  type KeyboardEvent,
  type MouseEvent,
} from "react";
import { getFromOpenElisServer } from "../../utils/Utils";

export interface PatientSearchResult {
  id?: string | number;
  patientID?: string | number;
  firstName?: string;
  lastName?: string;
  gender?: string;
  age?: string | number;
  dob?: string;
  nationalId?: string;
  referringFacility?: string;
  subjectNumber?: string;
}

interface SearchQueryParams {
  firstName: string;
  lastName: string;
  dateOfBirth: string;
  nationalID: string;
  subjectNumber: string;
}

export interface AutocompleteSuggestion {
  id: string | number;
  value: string;
}

interface AutocompleteProps {
  value?: string;
  suggestions?: AutocompleteSuggestion[];
  allowFreeText?: boolean;
  onChange?: (event: ChangeEvent<HTMLInputElement>) => void;
  onDelete?: (id: string | number) => void;
  onSelect?: (id: string | number) => void;
}

export const fetchPatientData = async (
  query: string,
  callback: (results: PatientSearchResult[]) => void,
) => {
  const [firstName, lastName] = query.split(" ");
  const queryParams: SearchQueryParams = {
    firstName: firstName || query,
    lastName: lastName || query,
    dateOfBirth: query,
    nationalID: query,
    subjectNumber: query,
  };

  const createEndpoint = (param: string, value: string) =>
    `/rest/patient-search?${param}=${value}`;

  const endpoints = Object.entries(queryParams)
    .map(([param, value]) => value && createEndpoint(param, value))
    .filter(Boolean);

  if (firstName && lastName) {
    endpoints.push(
      createEndpoint("firstName", firstName) + `&lastName=${lastName}`,
    );
  }

  const fetchEndpointData = async (endpoint: string) => {
    return new Promise<PatientSearchResult[] | null>((resolve) => {
      getFromOpenElisServer(endpoint, (response) => {
        if (response && response.length > 0) {
          resolve(response);
        } else {
          resolve(null);
        }
      });
    });
  };

  try {
    const results = await Promise.all(endpoints.map(fetchEndpointData));
    const filteredResults = results.filter((result) => result !== null);
    const combinedResults = ([] as PatientSearchResult[]).concat(
      ...filteredResults,
    );
    const uniqueResults = combinedResults.filter(
      (value, index, self) =>
        index === self.findIndex((t) => t.patientID === value.patientID),
    );

    callback(uniqueResults);
  } catch {
    callback([]);
  }
};

export const openPatientResults = (patientId?: string | number) => {
  if (patientId) {
    window.location.href = "/PatientResults/" + patientId;
  }
};

type UserInput = string | AutocompleteSuggestion | undefined;

export const useAutocomplete = (props: AutocompleteProps) => {
  const allowFreeText = props.allowFreeText;

  const [textValue, setTextValue] = useState("");
  const [activeSuggestion, setActiveSuggestion] = useState(0);
  const [filteredSuggestions, setFilteredSuggestions] = useState<
    AutocompleteSuggestion[]
  >([]);
  const [showSuggestions, setShowSuggestions] = useState(false);
  const [userInput, setUserInput] = useState<UserInput>("");
  const [invalid, setInvalid] = useState(false);
  const [initialised, setInitialised] = useState(false);

  useEffect(() => {
    if (props.value && !initialised) {
      if (props.suggestions) {
        const filteredSuggestion = props.suggestions.filter(
          (suggestion) => suggestion.id === props.value,
        );
        if (filteredSuggestion[0]) {
          setTextValue(filteredSuggestion[0].value);
        } else {
          setTextValue(props.value);
        }
      }
    }
  }, [props, initialised]);

  const onChange = (e: ChangeEvent<HTMLInputElement>) => {
    const { suggestions } = props;
    const userInput = e?.currentTarget?.value || "";
    setTextValue(userInput);

    if (suggestions) {
      const filteredSuggestions = suggestions.filter(
        (suggestion) =>
          suggestion.value.toLowerCase().indexOf(userInput.toLowerCase()) > -1,
      );

      setActiveSuggestion(0);
      setFilteredSuggestions(filteredSuggestions);
      setUserInput(userInput);
      setShowSuggestions(true);
      setInitialised(true);

      if (filteredSuggestions.length === 0 && !allowFreeText) {
        setInvalid(true);
      }
      if (typeof props.onChange === "function") {
        props.onChange(e);
      }
    }
  };

  const onClick = (
    e: MouseEvent<HTMLElement>,
    id: string | number,
    suggestion: AutocompleteSuggestion,
  ) => {
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

  const onDelete = (id: string | number) => {
    const updatedSuggestions = filteredSuggestions.filter(
      (suggestion) => suggestion.id !== id,
    );
    setFilteredSuggestions(updatedSuggestions);

    if (props.onDelete) {
      props.onDelete(id);
    }
  };

  const onKeyDown = (e: KeyboardEvent<HTMLInputElement>) => {
    if (e.keyCode === 13) {
      setActiveSuggestion(0);
      setUserInput(filteredSuggestions[activeSuggestion]);
      setShowSuggestions(false);
    } else if (e.keyCode === 38) {
      if (activeSuggestion === 0) {
        return;
      }
      setActiveSuggestion(activeSuggestion - 1);
    } else if (e.keyCode === 40) {
      if (activeSuggestion - 1 === filteredSuggestions.length) {
        return;
      }
      setActiveSuggestion(activeSuggestion + 1);
    }
  };

  return {
    textValue,
    setTextValue,
    activeSuggestion,
    filteredSuggestions,
    showSuggestions,
    userInput,
    invalid,
    onChange,
    onClick,
    onKeyDown,
    onDelete,
  };
};
