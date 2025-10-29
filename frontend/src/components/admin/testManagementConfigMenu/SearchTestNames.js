import React, { useState, useEffect } from "react";
import { TextInput } from "@carbon/react";
import { injectIntl, useIntl } from "react-intl";

function SearchTestNames({ testNames, onFilter }) {
  const intl = useIntl();
  const [searchTest, setSearchTest] = useState("");

  useEffect(() => {
    const filtered = testNames?.filter((test) =>
      test.value.toLowerCase().includes(searchTest.toLowerCase()),
    );
    onFilter(filtered);
  }, [searchTest, testNames, onFilter]);

  return (
    <>
      <TextInput
        type="text"
        placeholder={intl.formatMessage({
          id: "input.placeholder.searchTestName",
        })}
        value={searchTest}
        onChange={(e) => setSearchTest(e.target.value)}
        labelText={""}
        id="searchTestNameField"
      />
    </>
  );
}

export default injectIntl(SearchTestNames);
