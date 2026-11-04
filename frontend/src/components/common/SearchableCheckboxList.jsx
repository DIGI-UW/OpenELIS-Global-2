import React, { useMemo, useState } from "react";
import { Checkbox, DismissibleTag, Search } from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { matchesSearch } from "./searchText";

/**
 * Several choices from a list that comes from site data (panels, tests), picked
 * by ticking them after typing part of a name (OGC-1515). The list narrows to
 * the items containing the typed text, ignoring case and accents. Chosen items
 * are shown as tags above the search, so a choice stays visible and can be
 * removed while the search hides it.
 *
 * Items are `{ id, name }`; each checkbox gets the id `idPrefix + item.id`.
 * `onToggle(item, checked)` reports every tick, untick and dismissed tag.
 */
const SearchableCheckboxList = ({
  id,
  idPrefix,
  searchLabel,
  items = [],
  selectedIds = [],
  onToggle,
}) => {
  const intl = useIntl();
  const [query, setQuery] = useState("");
  const named = useMemo(() => items.filter((item) => item.name), [items]);
  const isChosen = (item) => selectedIds.includes(item.id);
  const chosen = named.filter(isChosen);
  const shown = named.filter((item) => matchesSearch(item.name, query));

  return (
    <div id={id} className="searchableCheckboxList">
      {chosen.length > 0 && (
        <div data-testid={`${id}-selected`}>
          {chosen.map((item) => (
            <DismissibleTag
              key={item.id}
              type="blue"
              text={item.name}
              title={intl.formatMessage(
                { id: "searchableList.remove" },
                { name: item.name },
              )}
              onClose={() => onToggle(item, false)}
            />
          ))}
        </div>
      )}
      <Search
        id={`${id}_search`}
        size="md"
        labelText={searchLabel}
        placeholder={searchLabel}
        value={query}
        onChange={(e) => setQuery(e.target.value)}
      />
      {shown.map((item) => (
        <Checkbox
          key={item.id}
          id={idPrefix + item.id}
          labelText={item.name}
          checked={isChosen(item)}
          onChange={(_, { checked }) => onToggle(item, checked)}
        />
      ))}
      {named.length > 0 && shown.length === 0 && (
        <p className="cds--label">
          <FormattedMessage id="searchableList.noMatch" values={{ query }} />
        </p>
      )}
    </div>
  );
};

export default SearchableCheckboxList;
