import React, { useEffect, useRef, useState } from "react";
import { useIntl } from "react-intl";
import { ComboBox } from "@carbon/react";
import { getOrganism, organismText, searchOrganisms } from "./OrganismSearch";

const TYPING_PAUSE_MS = 250;

/**
 * Picks one organism. The list is far too long to hold in the page, so the
 * choices are the server's matches to what has been typed.
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
      selectedItem={selected}
      shouldFilterItem={() => true}
      onInputChange={(text) => {
        clearTimeout(pause.current);
        if (text === organismText(selected)) return;
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
