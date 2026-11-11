import React, { useEffect, useRef, useState } from "react";
import { useIntl } from "react-intl";
import { ComboBox } from "@carbon/react";
import { getOrganism, organismText, searchOrganisms } from "./OrganismSearch";
import "./OrganismPicker.scss";

const TYPING_PAUSE_MS = 250;

/**
 * Picks one organism. The list is far too long to hold in the page, so the
 * choices are the server's matches to what has been typed. A row shows WHONET's
 * name and code, and the older name when that is what was typed.
 */
const OrganismPicker = ({ id, titleText, value, onChange, ...rest }) => {
  const intl = useIntl();
  const [matches, setMatches] = useState([]);
  // The organism last chosen or fetched; it is the selection only while it is the value.
  const [known, setKnown] = useState(null);
  const selected = value && known?.id === value ? known : null;
  const latest = useRef(0);
  const pause = useRef();
  const mounted = useRef(true);

  const search = (text) => {
    const asked = ++latest.current;
    searchOrganisms(text).then((found) => {
      if (mounted.current && asked === latest.current) setMatches(found);
    });
  };

  useEffect(() => {
    search("");
    return () => {
      mounted.current = false;
      clearTimeout(pause.current);
    };
  }, []);

  useEffect(() => {
    if (!value || known?.id === value) return undefined;
    let current = true;
    getOrganism(value).then((organism) => {
      if (current && mounted.current) setKnown(organism);
    });
    return () => {
      current = false;
    };
  }, [value]);

  return (
    <ComboBox
      id={id}
      titleText={titleText}
      placeholder={intl.formatMessage({
        id: "microbiology.organism.search.placeholder",
      })}
      items={matches}
      itemToString={organismText}
      itemToElement={(organism) => (
        <span className="organismPicker__row">
          <span>{organism.label}</span>
          {organism.code && (
            <span className="organismPicker__code">{organism.code}</span>
          )}
          {organism.formerName && (
            <span className="organismPicker__former">
              {intl.formatMessage(
                { id: "microbiology.organism.formerly" },
                { name: organism.formerName },
              )}
            </span>
          )}
        </span>
      )}
      selectedItem={selected}
      shouldFilterItem={() => true}
      onInputChange={(text) => {
        clearTimeout(pause.current);
        // Choosing a row writes its text here; that is not a new search.
        if (
          text === organismText(selected) ||
          matches.some((organism) => organismText(organism) === text)
        )
          return;
        // Rows for the earlier text must not be choosable while the new ones load.
        latest.current++;
        setMatches([]);
        pause.current = setTimeout(() => search(text), TYPING_PAUSE_MS);
      }}
      onChange={({ selectedItem }) => {
        setKnown(selectedItem || null);
        onChange(selectedItem?.id || "");
      }}
      {...rest}
    />
  );
};

/** The name of an organism known only by its id. */
export const OrganismName = ({ id, fallback }) => {
  const [known, setKnown] = useState(null);
  useEffect(() => {
    if (!id) return undefined;
    let current = true;
    getOrganism(id).then((found) => current && setKnown(found));
    return () => {
      current = false;
    };
  }, [id]);
  return <>{(id && known?.id === id && known.label) || fallback}</>;
};

export default OrganismPicker;
