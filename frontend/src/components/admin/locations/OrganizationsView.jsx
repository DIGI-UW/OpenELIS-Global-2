import React, {
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react";
import { FormattedMessage, useIntl } from "react-intl";
import { useHistory, useLocation } from "react-router-dom";
import {
  Button,
  Checkbox,
  FilterableMultiSelect,
  InlineNotification,
  Link,
  ListItem,
  Modal,
  Pagination,
  Select,
  SelectItem,
  Stack,
  Table,
  TableBatchAction,
  TableBatchActions,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableHeader,
  TableRow,
  TableSelectAll,
  TableSelectRow,
  TableToolbar,
  TableToolbarContent,
  TableToolbarSearch,
  Tag,
  Tile,
  Toggle,
  UnorderedList,
} from "@carbon/react";
import { Add, CheckmarkOutline, Download, Misuse } from "@carbon/icons-react";
import { LocationsContext } from "./LocationsPage";
import LocationComboBox from "./LocationComboBox";
import RecordForm from "./RecordForm";
import {
  getOrganization,
  getUsage,
  listOrganizations,
  pathText,
  setActive,
} from "./locationsApi";

const PAGE_SIZES = [25, 50, 100];

/** The list's filters live in the query string, so a view can be shared (FR-B deep links). */
const readFilters = (search) => {
  const params = new URLSearchParams(search);
  const list = (key) => (params.get(key) || "").split(",").filter(Boolean);
  return {
    q: params.get("q") || "",
    type: list("type"),
    location: params.get("location") || "",
    category: list("category"),
    ownership: list("ownership"),
    status: params.get("status") || "active",
    reviewOverdue: params.get("overdue") === "1",
    sort: params.get("sort") || "name",
    page: Number(params.get("page")) || 1,
    pageSize: Number(params.get("pageSize")) || 25,
    id: params.get("id") || "",
    add: params.get("add") === "1",
  };
};

const writeFilters = (filters) => {
  const out = new URLSearchParams();
  if (filters.q) out.set("q", filters.q);
  if (filters.type.length) out.set("type", filters.type.join(","));
  if (filters.location) out.set("location", filters.location);
  if (filters.category.length) out.set("category", filters.category.join(","));
  if (filters.ownership.length)
    out.set("ownership", filters.ownership.join(","));
  if (filters.status !== "active") out.set("status", filters.status);
  if (filters.reviewOverdue) out.set("overdue", "1");
  if (filters.sort !== "name") out.set("sort", filters.sort);
  if (filters.page > 1) out.set("page", String(filters.page));
  if (filters.pageSize !== 25) out.set("pageSize", String(filters.pageSize));
  if (filters.id) out.set("id", filters.id);
  if (filters.add) out.set("add", "1");
  return out.toString();
};

const ActiveToggle = ({ record, onChange, disabled }) => {
  const intl = useIntl();
  return (
    <Toggle
      id={`active-${record.id}`}
      size="sm"
      labelA={intl.formatMessage({ id: "label.locations.status.inactive" })}
      labelB={intl.formatMessage({ id: "label.locations.status.active" })}
      aria-label={`${intl.formatMessage({ id: "label.locations.column.active" })}, ${record.name}`}
      toggled={record.active}
      disabled={disabled}
      onToggle={(on) => onChange(on)}
    />
  );
};

const LocationCell = ({ location }) => {
  const intl = useIntl();
  if (!location) {
    return (
      <span className="cds--label">
        {intl.formatMessage({ id: "label.locations.location.notSet" })}
      </span>
    );
  }
  const above = (location.path || []).slice(0, -1);
  return (
    <div title={pathText(location)}>
      <div>
        <strong>{location.name}</strong>
        {location.levelName ? (
          <span className="cds--label"> · {location.levelName}</span>
        ) : null}
      </div>
      {above.length > 0 && (
        <div className="cds--label">{above.join(" / ")}</div>
      )}
    </div>
  );
};

/**
 * Sections B, C, D and E of the FRS: the Organizations and Sampling Sites
 * lists with their filters, search, inline edit, deactivation guard and undo.
 */
const OrganizationsView = ({ view, lists }) => {
  const intl = useIntl();
  const history = useHistory();
  const location = useLocation();
  const { notify, go, reloadLists } = useContext(LocationsContext);
  const filters = useMemo(
    () => readFilters(location.search),
    [location.search],
  );
  const [searchText, setSearchText] = useState(filters.q);
  const [page, setPage] = useState(null);
  const [loadError, setLoadError] = useState(false);
  const [loading, setLoading] = useState(true);
  const [selected, setSelected] = useState([]);
  const [guard, setGuard] = useState(null);
  const [elsewhere, setElsewhere] = useState(null);
  const searchTimer = useRef(null);
  const isSites = view === "sites";
  const expandedId = filters.id;
  const adding = filters.add;

  const update = useCallback(
    (patch) => {
      history.push({
        pathname: location.pathname,
        search: writeFilters({ ...filters, ...patch }),
      });
    },
    [history, location.pathname, filters],
  );

  const load = useCallback(() => {
    setLoading(true);
    listOrganizations({
      view,
      q: filters.q,
      type: filters.type,
      location: filters.location,
      category: filters.category,
      ownership: filters.ownership,
      status: filters.status,
      reviewOverdue: filters.reviewOverdue ? "true" : "",
      sort: filters.sort,
      page: filters.page,
      pageSize: filters.pageSize,
    })
      .then((loaded) => {
        setPage(loaded);
        setLoadError(false);
        setLoading(false);
      })
      .catch(() => {
        setPage(null);
        setLoadError(true);
        setLoading(false);
      });
    if (filters.q) {
      listOrganizations({
        view: isSites ? "organizations" : "sites",
        q: filters.q,
        status: filters.status,
        pageSize: 1,
      })
        .then((other) => setElsewhere(other ? other.total : 0))
        .catch(() => setElsewhere(0));
    } else {
      setElsewhere(null);
    }
  }, [view, filters, isSites]);

  useEffect(() => {
    load();
  }, [load]);

  useEffect(() => {
    setSearchText(filters.q);
  }, [filters.q]);

  useEffect(
    () => () => {
      if (searchTimer.current) {
        clearTimeout(searchTimer.current);
      }
    },
    [],
  );

  const onSearch = (text) => {
    setSearchText(text);
    if (searchTimer.current) {
      clearTimeout(searchTimer.current);
    }
    searchTimer.current = setTimeout(() => {
      update({ q: text.trim(), page: 1, id: "", add: false });
    }, 300);
  };

  const rows = page ? page.items : [];
  const typeItems = (lists ? lists.facilityTypes : []).map((type) => ({
    id: type.id,
    text: type.name,
  }));
  const categoryItems = (lists ? lists.categories : []).map((c) => ({
    id: c.id,
    text: c.label,
  }));
  const ownershipItems = (lists ? lists.ownerships : []).map((c) => ({
    id: c.id,
    text: c.label,
  }));
  const [locationItem, setLocationItem] = useState(null);
  useEffect(() => {
    if (!filters.location) {
      setLocationItem(null);
    } else if (!locationItem || locationItem.id !== filters.location) {
      const fromRow = rows
        .map((row) => row.location)
        .find((loc) => loc && loc.id === filters.location);
      if (fromRow) {
        setLocationItem({
          ...fromRow,
          text: fromRow.name,
          sub: pathText(fromRow),
        });
      } else {
        const wanted = filters.location;
        setLocationItem({ id: wanted, name: wanted, text: wanted });
        getOrganization(wanted)
          .then((detail) => {
            const area = detail.row;
            const path = [
              ...(area.location && Array.isArray(area.location.path)
                ? area.location.path
                : []),
              area.name,
            ];
            setLocationItem((current) =>
              current && current.id === wanted
                ? { id: wanted, name: area.name, text: area.name, path }
                : current,
            );
          })
          .catch(() => {});
      }
    }
  }, [filters.location, rows]);

  const anyFilter =
    filters.type.length ||
    filters.category.length ||
    filters.ownership.length ||
    filters.reviewOverdue ||
    filters.location ||
    filters.status !== "active";

  const clearFilters = () =>
    update({
      q: "",
      type: [],
      location: "",
      category: [],
      ownership: [],
      status: "active",
      reviewOverdue: false,
      page: 1,
      id: "",
      add: false,
    });

  const reactivate = (record) =>
    setActive([record.id], true)
      .then(() => {
        notify(
          intl.formatMessage(
            { id: "message.locations.reactivated" },
            { name: record.name },
          ),
        );
        load();
      })
      .catch((error) => notify(error.message, "error"));

  const deactivateWithUndo = (ids, label, includeChildren) =>
    setActive(ids, false, includeChildren)
      .then((result) => {
        const changed = result.changed || ids;
        notify(
          intl.formatMessage(
            { id: "message.locations.undo.deactivated" },
            { name: label },
          ),
          "success",
          {
            label: intl.formatMessage({ id: "button.locations.undo" }),
            run: () =>
              setActive(changed, true)
                .then(() => {
                  load();
                  notify(
                    intl.formatMessage(
                      { id: "message.locations.reactivated" },
                      { name: label },
                    ),
                  );
                })
                .catch((error) => notify(error.message, "error")),
          },
        );
        setSelected([]);
        load();
      })
      .catch((error) => notify(error.message, "error"));

  const askDeactivate = (record) =>
    getUsage(record.id)
      .then((usage) => {
        const kids = usage.activeChildren || [];
        if (usage.inUse.open > 0 || kids.length > 0) {
          setGuard({ record, usage, kids });
        } else {
          deactivateWithUndo([record.id], record.name, false);
        }
      })
      .catch((error) => notify(error.message, "error"));

  const toggleActive = (record, on) =>
    on ? reactivate(record) : askDeactivate(record);

  const kindLabel = (record) =>
    intl.formatMessage({
      id:
        record.kind === "facility"
          ? "label.locations.kind.wards"
          : "label.locations.kind.records",
    });

  const headers = [
    "label.locations.column.name",
    "label.locations.column.code",
    "label.locations.column.type",
    "label.locations.column.location",
    isSites
      ? "label.locations.column.siteType"
      : "label.locations.column.wards",
    "label.locations.column.inUse",
    "label.locations.column.active",
    "label.locations.column.actions",
  ];

  const matchNote = (row) => {
    if (!row.matchNote) return null;
    if (row.matchNote.startsWith("formerly:")) {
      return intl.formatMessage(
        { id: "label.locations.history.formerly" },
        { name: row.matchNote.slice("formerly:".length) },
      );
    }
    if (row.matchNote.startsWith("ward:")) {
      return intl.formatMessage(
        { id: "label.locations.search.ward" },
        { name: row.matchNote.slice("ward:".length) },
      );
    }
    return row.matchNote;
  };

  const onSaved = (detail, isNew) => {
    notify(
      intl.formatMessage(
        { id: isNew ? "message.locations.added" : "message.locations.saved" },
        { name: detail.row.name },
      ),
    );
    update({ id: isNew ? detail.row.id : "", add: false });
    load();
    reloadLists();
  };

  const closeForm = () => update({ id: "", add: false });

  const allSelected = rows.length > 0 && selected.length === rows.length;

  return (
    <div data-testid={`locations-${view}`}>
      <TableContainer>
        <TableToolbar
          aria-label={intl.formatMessage({ id: "label.locations.toolbar" })}
        >
          <TableToolbarContent className="locationsToolbar">
            <TableToolbarSearch
              persistent
              id={`locations-search-${view}`}
              labelText={intl.formatMessage({
                id: "placeholder.locations.search",
              })}
              placeholder={intl.formatMessage({
                id: "placeholder.locations.search",
              })}
              value={searchText}
              onChange={(e) => onSearch(e.target ? e.target.value : "")}
              onClear={() => onSearch("")}
            />
            {!isSites && (
              <FilterableMultiSelect
                id={`locations-type-${view}`}
                titleText={intl.formatMessage({
                  id: "label.locations.filter.type",
                })}
                placeholder={intl.formatMessage({
                  id: "label.locations.filter.type.all",
                })}
                items={typeItems}
                itemToString={(item) => (item ? item.text : "")}
                initialSelectedItems={typeItems.filter((item) =>
                  filters.type.includes(item.id),
                )}
                onChange={({ selectedItems }) =>
                  update({
                    type: selectedItems.map((item) => item.id),
                    page: 1,
                  })
                }
              />
            )}
            <LocationComboBox
              id={`locations-location-${view}`}
              titleText={intl.formatMessage({
                id: "label.locations.filter.location",
              })}
              placeholder={intl.formatMessage({
                id: "label.locations.filter.location.any",
              })}
              selected={locationItem}
              onChange={(item) => {
                setLocationItem(item);
                update({ location: item ? item.id : "", page: 1 });
              }}
            />
            {!isSites && (
              <>
                <FilterableMultiSelect
                  id="locations-category"
                  titleText={intl.formatMessage({
                    id: "label.locations.field.category",
                  })}
                  placeholder={intl.formatMessage({
                    id: "label.locations.filter.category.all",
                  })}
                  items={categoryItems}
                  itemToString={(item) => (item ? item.text : "")}
                  initialSelectedItems={categoryItems.filter((item) =>
                    filters.category.includes(item.id),
                  )}
                  onChange={({ selectedItems }) =>
                    update({
                      category: selectedItems.map((i) => i.id),
                      page: 1,
                    })
                  }
                />
                <FilterableMultiSelect
                  id="locations-ownership"
                  titleText={intl.formatMessage({
                    id: "label.locations.field.ownership",
                  })}
                  placeholder={intl.formatMessage({
                    id: "label.locations.filter.ownership.all",
                  })}
                  items={ownershipItems}
                  itemToString={(item) => (item ? item.text : "")}
                  initialSelectedItems={ownershipItems.filter((item) =>
                    filters.ownership.includes(item.id),
                  )}
                  onChange={({ selectedItems }) =>
                    update({
                      ownership: selectedItems.map((i) => i.id),
                      page: 1,
                    })
                  }
                />
              </>
            )}
            <Select
              id={`locations-status-${view}`}
              labelText={intl.formatMessage({
                id: "label.locations.filter.status",
              })}
              value={filters.status}
              onChange={(e) => update({ status: e.target.value, page: 1 })}
              helperText={
                filters.status !== "active"
                  ? intl.formatMessage({ id: "help.locations.status.inactive" })
                  : undefined
              }
            >
              <SelectItem
                value="active"
                text={intl.formatMessage({
                  id: "label.locations.status.active",
                })}
              />
              <SelectItem
                value="inactive"
                text={intl.formatMessage({
                  id: "label.locations.status.inactive",
                })}
              />
              <SelectItem
                value="all"
                text={intl.formatMessage({ id: "label.locations.status.all" })}
              />
            </Select>
            {!isSites && (
              <Checkbox
                id="locations-overdue"
                labelText={intl.formatMessage({
                  id: "label.locations.filter.reviewOverdue",
                })}
                checked={filters.reviewOverdue}
                onChange={(_, { checked }) =>
                  update({ reviewOverdue: checked, page: 1 })
                }
              />
            )}
            <div className="locationsToolbarActions">
              <Button
                kind="tertiary"
                size="md"
                onClick={() => go("import", "?area=organizations")}
              >
                <FormattedMessage id="button.locations.import" />
              </Button>
              <Button
                kind="tertiary"
                size="md"
                renderIcon={Download}
                onClick={() =>
                  window.open(
                    `/api/OpenELIS-Global/rest/locations/export?${new URLSearchParams(
                      {
                        view,
                        q: filters.q,
                        status: filters.status,
                        location: filters.location,
                        type: filters.type.join(","),
                        category: filters.category.join(","),
                        ownership: filters.ownership.join(","),
                      },
                    ).toString()}`,
                    "_blank",
                  )
                }
              >
                <FormattedMessage id="button.locations.export" />
              </Button>
              <Button
                size="md"
                renderIcon={Add}
                data-testid="locations-add"
                onClick={() => update({ add: true, id: "" })}
              >
                <FormattedMessage id="button.locations.add" />
              </Button>
            </div>
          </TableToolbarContent>
        </TableToolbar>
        {anyFilter ? (
          <div
            className="locationsFilterTags"
            data-testid="locations-filter-tags"
          >
            {filters.type.map((id) => (
              <Tag
                key={id}
                type="high-contrast"
                filter
                onClose={() =>
                  update({
                    type: filters.type.filter((x) => x !== id),
                    page: 1,
                  })
                }
              >
                {(typeItems.find((t) => t.id === id) || { text: id }).text}
              </Tag>
            ))}
            {filters.category.map((id) => (
              <Tag
                key={id}
                type="high-contrast"
                filter
                onClose={() =>
                  update({
                    category: filters.category.filter((x) => x !== id),
                    page: 1,
                  })
                }
              >
                {(categoryItems.find((c) => c.id === id) || { text: id }).text}
              </Tag>
            ))}
            {filters.ownership.map((id) => (
              <Tag
                key={id}
                type="high-contrast"
                filter
                onClose={() =>
                  update({
                    ownership: filters.ownership.filter((x) => x !== id),
                    page: 1,
                  })
                }
              >
                {(ownershipItems.find((c) => c.id === id) || { text: id }).text}
              </Tag>
            ))}
            {filters.reviewOverdue && (
              <Tag
                type="high-contrast"
                filter
                onClose={() => update({ reviewOverdue: false, page: 1 })}
              >
                <FormattedMessage id="label.locations.referral.overdue" />
              </Tag>
            )}
            {filters.location && (
              <Tag
                type="high-contrast"
                filter
                onClose={() => {
                  setLocationItem(null);
                  update({ location: "", page: 1 });
                }}
              >
                {locationItem
                  ? pathText(locationItem) || locationItem.name
                  : filters.location}
              </Tag>
            )}
            {filters.status !== "active" && (
              <Tag
                type="high-contrast"
                filter
                onClose={() => update({ status: "active", page: 1 })}
              >
                <FormattedMessage
                  id={
                    filters.status === "inactive"
                      ? "label.locations.status.inactive"
                      : "label.locations.status.all"
                  }
                />
              </Tag>
            )}
            <Button kind="ghost" size="sm" onClick={clearFilters}>
              <FormattedMessage id="label.locations.filter.clear" />
            </Button>
          </div>
        ) : null}
        {selected.length > 0 && (
          <TableBatchActions
            shouldShowBatchActions
            totalSelected={selected.length}
            onCancel={() => setSelected([])}
          >
            <TableBatchAction
              renderIcon={Misuse}
              onClick={() =>
                deactivateWithUndo(
                  [...selected],
                  `${selected.length} ${intl.formatMessage({ id: "label.locations.kind.records" })}`,
                  false,
                )
              }
            >
              <FormattedMessage id="button.locations.deactivate" />
            </TableBatchAction>
            <TableBatchAction
              renderIcon={CheckmarkOutline}
              onClick={() =>
                setActive([...selected], true)
                  .then(() => {
                    setSelected([]);
                    load();
                  })
                  .catch((error) => notify(error.message, "error"))
              }
            >
              <FormattedMessage id="button.locations.reactivate" />
            </TableBatchAction>
            <TableBatchAction
              renderIcon={Download}
              onClick={() => {
                window.open(
                  `/api/OpenELIS-Global/rest/locations/export?view=${view}&ids=${selected.join(",")}`,
                  "_blank",
                );
                setSelected([]);
              }}
            >
              <FormattedMessage id="button.locations.exportSelected" />
            </TableBatchAction>
          </TableBatchActions>
        )}
        {loadError && (
          <div className="locationsLoadError">
            <InlineNotification
              kind="error"
              lowContrast
              title=""
              subtitle={intl.formatMessage({
                id: "error.locations.load.failed",
              })}
              onClose={() => setLoadError(false)}
            />
            <Button kind="tertiary" size="sm" onClick={load}>
              <FormattedMessage id="common.retry" />
            </Button>
          </div>
        )}
        <Table
          size="md"
          aria-label={intl.formatMessage({ id: VIEW_LABEL[view] })}
          data-testid="locations-table"
        >
          <TableHead>
            <TableRow>
              <TableSelectAll
                id={`locations-select-all-${view}`}
                name={`locations-select-all-${view}`}
                ariaLabel={intl.formatMessage({
                  id: "label.locations.selectAll",
                })}
                checked={allSelected}
                onSelect={() =>
                  setSelected(allSelected ? [] : rows.map((row) => row.id))
                }
              />
              {headers.map((header, index) =>
                index === 3 ? (
                  <TableHeader
                    key={header}
                    isSortable
                    isSortHeader={filters.sort === "location"}
                    sortDirection={filters.sort === "location" ? "ASC" : "NONE"}
                    onClick={() =>
                      update({
                        sort: filters.sort === "location" ? "name" : "location",
                      })
                    }
                  >
                    <FormattedMessage id={header} />
                  </TableHeader>
                ) : (
                  <TableHeader key={header}>
                    <FormattedMessage id={header} />
                  </TableHeader>
                ),
              )}
            </TableRow>
          </TableHead>
          <TableBody>
            {adding && (
              <TableRow>
                <TableCell colSpan={9} className="locationsFormCell">
                  <RecordForm
                    isNew
                    kind={isSites ? "site" : "facility"}
                    lists={lists}
                    onCancel={closeForm}
                    onSaved={(detail) => onSaved(detail, true)}
                  />
                </TableCell>
              </TableRow>
            )}
            {rows.map((row) => {
              const isExpanded = expandedId === row.id;
              const note = matchNote(row);
              return (
                <React.Fragment key={row.id}>
                  <TableRow data-testid={`locations-row-${row.id}`}>
                    <TableSelectRow
                      id={`locations-select-${row.id}`}
                      name={`locations-select-${row.id}`}
                      ariaLabel={`${intl.formatMessage({ id: "label.locations.select" })} ${row.name}`}
                      checked={selected.includes(row.id)}
                      onSelect={() =>
                        setSelected(
                          selected.includes(row.id)
                            ? selected.filter((x) => x !== row.id)
                            : [...selected, row.id],
                        )
                      }
                    />
                    <TableCell>
                      {row.name}{" "}
                      {row.registry && (
                        <Tag
                          type="cyan"
                          size="sm"
                          title={intl.formatMessage({
                            id: "help.locations.registry",
                          })}
                        >
                          <FormattedMessage id="label.locations.source.registry" />
                        </Tag>
                      )}
                      {row.reviewOverdue && (
                        <Tag type="red" size="sm">
                          <FormattedMessage id="label.locations.referral.overdue" />
                        </Tag>
                      )}
                      {row.accreditationExpired && (
                        <Tag type="warm-gray" size="sm">
                          <FormattedMessage id="label.locations.referral.expired" />
                        </Tag>
                      )}
                      {note && <div className="cds--label">{note}</div>}
                    </TableCell>
                    <TableCell>
                      <code>
                        {row.code ||
                          intl.formatMessage({ id: "label.locations.none" })}
                      </code>
                    </TableCell>
                    <TableCell>
                      {(row.types || []).map((type) => (
                        <Tag
                          key={type.id}
                          type={row.kind === "site" ? "teal" : "blue"}
                          size="sm"
                        >
                          {type.name}
                        </Tag>
                      ))}
                      {(row.category || row.ownership) && (
                        <div className="cds--label">
                          {[row.category, row.ownership]
                            .filter(Boolean)
                            .map((x) => x.label)
                            .join(" · ")}
                        </div>
                      )}
                    </TableCell>
                    <TableCell>
                      <LocationCell location={row.location} />
                    </TableCell>
                    <TableCell>
                      {isSites ? row.siteType || "" : row.wardCount}
                    </TableCell>
                    <TableCell>
                      {(row.inUse ? row.inUse.total : 0).toLocaleString()}{" "}
                      <span className="cds--label">
                        ({row.inUse ? row.inUse.open : 0}{" "}
                        <FormattedMessage id="label.locations.open" />)
                      </span>
                    </TableCell>
                    <TableCell
                      title={
                        row.active
                          ? ""
                          : intl.formatMessage({
                              id: "help.locations.status.inactive",
                            })
                      }
                    >
                      <ActiveToggle
                        record={row}
                        onChange={(on) => toggleActive(row, on)}
                      />
                    </TableCell>
                    <TableCell>
                      <Button
                        kind="ghost"
                        size="sm"
                        aria-expanded={isExpanded}
                        aria-label={`${intl.formatMessage({ id: isExpanded ? "button.close" : "button.edit" })} ${row.name}`}
                        data-testid={`locations-edit-${row.id}`}
                        onClick={() =>
                          update({ id: isExpanded ? "" : row.id, add: false })
                        }
                      >
                        <FormattedMessage
                          id={isExpanded ? "button.close" : "button.edit"}
                        />
                      </Button>
                    </TableCell>
                  </TableRow>
                  {isExpanded && (
                    <TableRow>
                      <TableCell colSpan={9} className="locationsFormCell">
                        <RecordForm
                          id={row.id}
                          kind={row.kind}
                          lists={lists}
                          highlight={filters.q}
                          statusFilter={filters.status}
                          onCancel={closeForm}
                          onSaved={(detail) => onSaved(detail, false)}
                          onActiveChange={toggleActive}
                          reload={load}
                        />
                      </TableCell>
                    </TableRow>
                  )}
                </React.Fragment>
              );
            })}
          </TableBody>
        </Table>
        {!loading && !loadError && rows.length === 0 && !adding && (
          <Tile className="locationsEmpty" data-testid="locations-empty">
            <p>
              <FormattedMessage
                id={
                  filters.q || anyFilter
                    ? "message.locations.empty.filtered"
                    : "message.locations.empty.none"
                }
              />
            </p>
            {filters.q || anyFilter ? (
              <Button kind="ghost" size="sm" onClick={clearFilters}>
                <FormattedMessage id="label.locations.filter.clear" />
              </Button>
            ) : (
              <Stack orientation="horizontal" gap={3}>
                <Button
                  size="sm"
                  renderIcon={Add}
                  onClick={() => update({ add: true })}
                >
                  <FormattedMessage id="button.locations.add" />
                </Button>
                <Button kind="tertiary" size="sm" onClick={() => go("import")}>
                  <FormattedMessage id="button.locations.import" />
                </Button>
              </Stack>
            )}
            {filters.q && elsewhere > 0 && (
              <p>
                <Link
                  href="#"
                  onClick={(e) => {
                    e.preventDefault();
                    go(
                      isSites ? "organizations" : "sites",
                      `?q=${encodeURIComponent(filters.q)}`,
                    );
                  }}
                >
                  <FormattedMessage
                    id="message.locations.search.elsewhere"
                    values={{
                      count: elsewhere,
                      view: intl.formatMessage({
                        id: isSites
                          ? "sidenav.label.admin.locations.organizations"
                          : "sidenav.label.admin.locations.sites",
                      }),
                    }}
                  />
                </Link>
              </p>
            )}
          </Tile>
        )}
        <Pagination
          page={filters.page}
          pageSize={filters.pageSize}
          pageSizes={PAGE_SIZES}
          totalItems={page ? page.total : 0}
          onChange={({ page: nextPage, pageSize }) =>
            update({ page: nextPage, pageSize, id: "", add: false })
          }
          forwardText={intl.formatMessage({ id: "pagination.forward" })}
          backwardText={intl.formatMessage({ id: "pagination.backward" })}
          itemRangeText={(min, max, total) =>
            intl.formatMessage(
              { id: "pagination.item-range" },
              { min, max, total },
            )
          }
          itemsPerPageText={intl.formatMessage({
            id: "pagination.items-per-page",
          })}
          itemText={(min, max) =>
            intl.formatMessage({ id: "pagination.item" }, { min, max })
          }
          pageNumberText={intl.formatMessage({ id: "pagination.page-number" })}
          pageRangeText={(_current, total) =>
            intl.formatMessage({ id: "pagination.page-range" }, { total })
          }
          pageText={(p, pagesUnknown) =>
            intl.formatMessage(
              { id: "pagination.page" },
              { page: pagesUnknown ? "" : p },
            )
          }
        />
      </TableContainer>

      <Modal
        open={!!guard}
        danger
        modalHeading={
          guard
            ? intl.formatMessage(
                { id: "message.locations.deactivate.confirm" },
                { name: guard.record.name },
              )
            : ""
        }
        primaryButtonText={
          guard
            ? intl.formatMessage(
                {
                  id:
                    guard.kids.length > 0
                      ? "button.locations.deactivate.only"
                      : "button.locations.deactivate.record",
                },
                { name: guard.record.name },
              )
            : ""
        }
        secondaryButtonText={intl.formatMessage({ id: "button.cancel" })}
        onRequestClose={() => setGuard(null)}
        onRequestSubmit={() => {
          deactivateWithUndo([guard.record.id], guard.record.name, false);
          setGuard(null);
        }}
      >
        {guard && (
          <Stack gap={4}>
            <p>
              <FormattedMessage
                id="message.locations.deactivate.inUse"
                values={{
                  name: guard.record.name,
                  open: guard.usage.inUse.open,
                  children: guard.kids.length,
                  childKind: kindLabel(guard.record),
                }}
              />
            </p>
            {guard.kids.length > 0 && (
              <UnorderedList>
                {guard.kids.map((kid) => (
                  <ListItem key={kid.id}>
                    {kid.name} ({kid.inUse.open}{" "}
                    <FormattedMessage id="label.locations.openOrders" />)
                  </ListItem>
                ))}
              </UnorderedList>
            )}
            {guard.record.registry && (
              <InlineNotification
                kind="warning"
                lowContrast
                hideCloseButton
                title=""
                subtitle={intl.formatMessage({
                  id: "warning.locations.registry.deactivate",
                })}
              />
            )}
            <p className="cds--label">
              <FormattedMessage id="help.locations.status.inactive" />
            </p>
            {guard.kids.length > 0 && (
              <Button
                kind="danger--tertiary"
                onClick={() => {
                  deactivateWithUndo(
                    [guard.record.id],
                    `${guard.record.name} + ${guard.kids.length}`,
                    true,
                  );
                  setGuard(null);
                }}
              >
                <FormattedMessage
                  id="button.locations.deactivate.withChildren"
                  values={{
                    name: guard.record.name,
                    count: guard.kids.length,
                    childKind: kindLabel(guard.record),
                  }}
                />
              </Button>
            )}
          </Stack>
        )}
      </Modal>
    </div>
  );
};

const VIEW_LABEL = {
  organizations: "sidenav.label.admin.locations.organizations",
  sites: "sidenav.label.admin.locations.sites",
};

export default OrganizationsView;
