import React, { useState, useEffect } from "react";
import { TextInput } from "@carbon/react";
import { injectIntl, useIntl } from "react-intl";
import type { WrappedComponentProps } from "react-intl";

interface SearchTestNameOption {
  id: string;
  value: string;
}

interface SearchTestNamesProps {
  testNames: SearchTestNameOption[];
  onFilter: (tests: SearchTestNameOption[]) => void;
}

function SearchTestNames({ testNames, onFilter }: SearchTestNamesProps) {
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

export default injectIntl(
  SearchTestNames as unknown as React.ComponentType<WrappedComponentProps>,
) as unknown as React.ComponentType<SearchTestNamesProps>;
