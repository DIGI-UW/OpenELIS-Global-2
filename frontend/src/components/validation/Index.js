import React, { useState } from "react";
import { Grid, Column, Section, Heading } from "@carbon/react";
import { FormattedMessage } from "react-intl";
import SearchForm from "./SearchForm";
import ValidationPage from "./Validation";
import { AlertDialog } from "../common/CustomNotification";
import { getFromOpenElisServer } from "../utils/Utils";

function ValidationIndex() {
  const [results, setResults] = useState({ resultList: [] });
  const [isLoading, setIsLoading] = useState(false);
  const [searchParams, setSearchParams] = useState(null);

  const handleRefresh = () => {
    if (searchParams) {
      setIsLoading(true);
      getFromOpenElisServer(
        `/rest/AccessionValidation?${searchParams}`,
        (data) => {
          setIsLoading(false);
          if (data && data.resultList && data.resultList.length > 0) {
            setResults(data);
          } else {
            setResults({ resultList: [] });
          }
        },
      );
    } else {
      window.location.reload();
    }
  };

  return (
    <>
      <AlertDialog />
      <Grid fullWidth>
        <Column lg={16} md={8} sm={4}>
          <Section>
            <Heading>
              <FormattedMessage id="validation.page.title" />
            </Heading>
          </Section>
        </Column>
      </Grid>

      <SearchForm
        setResults={setResults}
        setIsLoading={setIsLoading}
        setSearchParams={setSearchParams}
      />

      {isLoading ? (
        <Section>
          <Grid>
            <Column lg={16}>
              <div style={{ textAlign: "center", padding: "2rem" }}>
                <FormattedMessage id="label.loading" />
              </div>
            </Column>
          </Grid>
        </Section>
      ) : (
        <ValidationPage
          results={results}
          setResults={setResults}
          onRefresh={handleRefresh}
        />
      )}
    </>
  );
}

export default ValidationIndex;
