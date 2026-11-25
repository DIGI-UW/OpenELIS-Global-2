import React, { useEffect, useRef, useState } from "react";
import { Column, Grid } from "@carbon/react";
import { useIntl } from "react-intl";
import "../Style.css";
import { getFromOpenElisServer } from "../utils/Utils";
import SearchableSelect from "../common/SearchableSelect";

/**
 * The workplan's choice of a test, panel or lab unit, searched by typing
 * (OGC-1515). The list comes from `url`; the choice named by the `param` URL
 * parameter is preselected, and `onSelect(id, label)` is told of every choice,
 * with the placeholder as the label while nothing is chosen.
 */
export default function WorkplanListSelect({
  id,
  url,
  param,
  placeholderId,
  title,
  onSelect,
}) {
  const intl = useIntl();
  const mounted = useRef(false);
  const [options, setOptions] = useState([]);
  const [selectedId, setSelectedId] = useState("");
  const placeholder = intl.formatMessage({ id: placeholderId });

  const choose = (chosenId, available) => {
    const chosen = available.find((option) => option.id === chosenId);
    setSelectedId(chosen ? chosen.id : "");
    onSelect(chosen ? chosen.id : "", chosen ? chosen.value : placeholder);
  };

  useEffect(() => {
    mounted.current = true;
    const preselected =
      new URLSearchParams(window.location.search).get(param) || "";
    getFromOpenElisServer(url, (response) => {
      const fetched = Array.isArray(response) ? response : [];
      if (mounted.current) {
        setOptions(fetched);
      }
      choose(preselected, fetched);
    });
    return () => {
      mounted.current = false;
    };
  }, []);

  return (
    <Grid fullWidth={true}>
      <Column sm={4} md={8} lg={16}>
        <SearchableSelect
          id={id}
          labelText={title}
          placeholder={placeholder}
          options={options}
          value={selectedId}
          onChange={(chosenId) => choose(chosenId, options)}
        />
      </Column>
    </Grid>
  );
}
