import React, {
  useCallback,
  useContext,
  useEffect,
  useRef,
  useState,
} from "react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  Button,
  InlineNotification,
  Select,
  SelectItem,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableHeader,
  TableRow,
  TableToolbar,
  TableToolbarContent,
  TableToolbarSearch,
  Tag,
  TextInput,
  Toggle,
} from "@carbon/react";
import { Add, ChevronDown, ChevronRight } from "@carbon/icons-react";
import { LocationsContext } from "./LocationsPage";
import HistoryPanel from "./HistoryPanel";
import {
  createArea,
  listAreas,
  searchAreas,
  setAreaActive,
  updateArea,
} from "./locationsApi";

const ROOT = "__root__";

/**
 * FR-B7 and FR-L1: the geographic areas as a nested tree table. Children load
 * when a row is expanded, a search shows every match with its ancestors, each
 * row carries an Active toggle, Edit and an add button named for the level
 * below, and the tree is navigable with the keyboard.
 */
const AreasView = ({ lists }) => {
  const intl = useIntl();
  const { notify } = useContext(LocationsContext);
  const [children, setChildren] = useState({});
  const [open, setOpen] = useState(new Set());
  const [status, setStatus] = useState("active");
  const [query, setQuery] = useState("");
  const [search, setSearch] = useState(null);
  const [editing, setEditing] = useState(null);
  const [adding, setAdding] = useState(null);
  const [draft, setDraft] = useState({ name: "", code: "" });
  const [errors, setErrors] = useState({});
  const [history, setHistory] = useState(null);
  const timer = useRef(null);
  const levels = lists ? lists.areaLevels : [];

  const loadChildren = useCallback(
    (parentId) =>
      listAreas(parentId === ROOT ? "" : parentId, status)
        .then((loaded) => {
          setChildren((current) => ({ ...current, [parentId]: loaded || [] }));
          return loaded || [];
        })
        .catch((error) => {
          notify(error.message, "error");
          return [];
        }),
    [status, notify],
  );

  useEffect(() => {
    setChildren({});
    loadChildren(ROOT).then((roots) =>
      setOpen(new Set(roots.map((area) => area.id))),
    );
  }, [loadChildren]);

  useEffect(() => {
    open.forEach((id) => {
      if (!children[id]) {
        loadChildren(id);
      }
    });
  }, [open]);

  const runSearch = (text) => {
    setQuery(text);
    if (timer.current) clearTimeout(timer.current);
    timer.current = setTimeout(() => {
      if (!text.trim()) {
        setSearch(null);
        return;
      }
      searchAreas(text, status)
        .then((nodes) => setSearch(nodes || []))
        .catch((error) => notify(error.message, "error"));
    }, 300);
  };

  const refresh = (parentId) => loadChildren(parentId || ROOT);

  const toggle = (id) => {
    setOpen((current) => {
      const next = new Set(current);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  };

  const levelBelow = (level) => levels.find((l) => l.level === level + 1);

  const saveArea = (area, parentId) => {
    setErrors({});
    const body = {
      name: draft.name,
      code: draft.code,
      parentId,
      lastupdated: area ? area.lastupdated : null,
    };
    const call = area ? updateArea(area.id, body) : createArea(body);
    call
      .then(() => {
        setEditing(null);
        setAdding(null);
        setDraft({ name: "", code: "" });
        notify(
          intl.formatMessage(
            {
              id: area ? "message.locations.saved" : "message.locations.added",
            },
            { name: draft.name },
          ),
        );
        if (search !== null) {
          runSearch(query);
        }
        refresh(parentId);
        if (parentId) {
          setOpen((current) => new Set([...current, parentId]));
          const parentArea = Object.values(children)
            .flat()
            .find((candidate) => candidate.id === parentId);
          if (!area && parentArea) {
            refresh(parentArea.parentId);
          }
        }
      })
      .catch((error) => {
        if (error.status === 422) setErrors(error.fieldErrors || {});
        notify(error.message, "error");
      });
  };

  const changeActive = (area, on) =>
    setAreaActive(area.id, on)
      .then(() => {
        if (on) {
          notify(
            intl.formatMessage(
              { id: "message.locations.reactivated" },
              { name: area.name },
            ),
          );
        } else {
          notify(
            intl.formatMessage(
              { id: "message.locations.undo.deactivated" },
              { name: area.name },
            ),
            "success",
            {
              label: intl.formatMessage({ id: "button.locations.undo" }),
              run: () =>
                setAreaActive(area.id, true)
                  .then(() => {
                    refresh(area.parentId);
                    notify(
                      intl.formatMessage(
                        { id: "message.locations.reactivated" },
                        { name: area.name },
                      ),
                    );
                  })
                  .catch((error) => notify(error.message, "error")),
            },
          );
        }
        if (search !== null) runSearch(query);
        refresh(area.parentId);
      })
      .catch((error) => notify(error.message, "error"));

  const rows = [];
  if (search !== null) {
    const byParent = {};
    search.forEach((node) => {
      const key =
        node.parentId && search.some((n) => n.id === node.parentId)
          ? node.parentId
          : ROOT;
      (byParent[key] = byParent[key] || []).push(node);
    });
    const walk = (parentKey, depth) =>
      (byParent[parentKey] || []).forEach((node) => {
        rows.push({ area: node, depth, hasChildren: !!byParent[node.id] });
        walk(node.id, depth + 1);
      });
    walk(ROOT, 0);
  } else {
    const walk = (parentKey, depth) =>
      (children[parentKey] || []).forEach((area) => {
        rows.push({ area, depth, hasChildren: area.childCount > 0 });
        if (open.has(area.id)) walk(area.id, depth + 1);
      });
    walk(ROOT, 0);
  }

  const onKeyDown = (event) => {
    const items = [...event.currentTarget.querySelectorAll("tr[data-id]")];
    const index = items.indexOf(document.activeElement);
    if (index < 0) return;
    const id = items[index].dataset.id;
    const row = rows.find((r) => r.area.id === id);
    if (event.key === "ArrowDown" && items[index + 1]) {
      event.preventDefault();
      items[index + 1].focus();
    } else if (event.key === "ArrowUp" && items[index - 1]) {
      event.preventDefault();
      items[index - 1].focus();
    } else if (
      event.key === "ArrowRight" &&
      row &&
      row.hasChildren &&
      !open.has(id)
    ) {
      event.preventDefault();
      toggle(id);
    } else if (event.key === "ArrowLeft") {
      event.preventDefault();
      if (open.has(id) && row && row.hasChildren) {
        toggle(id);
      } else if (row && row.area.parentId) {
        const parent = items.find(
          (item) => item.dataset.id === row.area.parentId,
        );
        if (parent) parent.focus();
      }
    } else if (event.key === "Enter") {
      event.preventDefault();
      setEditing(editing === id ? null : id);
      if (row) setDraft({ name: row.area.name, code: row.area.code || "" });
    }
  };

  const renderAddRow = (depth, parent) => {
    const level = parent ? levelBelow(parent.level) : levels[0];
    return (
      <TableRow data-testid="locations-area-add-row">
        <TableCell colSpan={6} style={{ paddingLeft: `${1 + depth * 1.5}rem` }}>
          <Stack orientation="horizontal" gap={3}>
            <TextInput
              id="locations-new-area-name"
              labelText={`${intl.formatMessage({ id: "label.locations.area.new" })} ${level ? level.name : ""}${parent ? ` · ${parent.name}` : ""} *`}
              value={draft.name}
              invalid={!!errors.name}
              invalidText={errors.name}
              onChange={(e) => setDraft({ ...draft, name: e.target.value })}
            />
            <TextInput
              id="locations-new-area-code"
              labelText={intl.formatMessage({
                id: "label.locations.column.code",
              })}
              value={draft.code}
              onChange={(e) => setDraft({ ...draft, code: e.target.value })}
            />
            <Button
              size="sm"
              disabled={!draft.name.trim()}
              data-testid="locations-area-save"
              onClick={() => saveArea(null, parent ? parent.id : null)}
            >
              <FormattedMessage id="button.save" />
            </Button>
            <Button kind="ghost" size="sm" onClick={() => setAdding(null)}>
              <FormattedMessage id="button.cancel" />
            </Button>
          </Stack>
          {errors.parent && (
            <InlineNotification
              kind="error"
              lowContrast
              hideCloseButton
              title=""
              subtitle={errors.parent}
            />
          )}
        </TableCell>
      </TableRow>
    );
  };

  return (
    <TableContainer data-testid="locations-areas">
      <TableToolbar>
        <TableToolbarContent className="locationsToolbar">
          <TableToolbarSearch
            persistent
            id="locations-area-search"
            labelText={intl.formatMessage({
              id: "placeholder.locations.area.search",
            })}
            placeholder={intl.formatMessage({
              id: "placeholder.locations.area.search",
            })}
            value={query}
            onChange={(e) => runSearch(e.target ? e.target.value : "")}
            onClear={() => runSearch("")}
          />
          <Select
            id="locations-area-status"
            labelText={intl.formatMessage({
              id: "label.locations.filter.status",
            })}
            value={status}
            onChange={(e) => setStatus(e.target.value)}
          >
            <SelectItem
              value="active"
              text={intl.formatMessage({ id: "label.locations.status.active" })}
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
          <Button
            kind="ghost"
            size="md"
            onClick={() => {
              const all = new Set(open);
              Object.values(children)
                .flat()
                .forEach((area) => all.add(area.id));
              setOpen(all);
            }}
          >
            <FormattedMessage id="button.locations.area.expandAll" />
          </Button>
          <Button kind="ghost" size="md" onClick={() => setOpen(new Set())}>
            <FormattedMessage id="button.locations.area.collapseAll" />
          </Button>
          <Button
            size="md"
            renderIcon={Add}
            disabled={levels.length === 0}
            data-testid="locations-area-add-top"
            onClick={() => {
              setAdding(ROOT);
              setDraft({ name: "", code: "" });
            }}
          >
            <FormattedMessage
              id="button.locations.area.addTop"
              values={{ level: levels[0] ? levels[0].name : "" }}
            />
          </Button>
        </TableToolbarContent>
      </TableToolbar>
      <Table
        size="md"
        role="treegrid"
        className="locationsTree"
        aria-label={intl.formatMessage({
          id: "sidenav.label.admin.locations.areas",
        })}
        onKeyDown={onKeyDown}
      >
        <TableHead>
          <TableRow>
            {[
              "label.locations.column.name",
              "label.locations.column.code",
              "label.locations.column.level",
              "label.locations.column.children",
              "label.locations.column.active",
              "label.locations.column.actions",
            ].map((column) => (
              <TableHeader key={column}>
                <FormattedMessage id={column} />
              </TableHeader>
            ))}
          </TableRow>
        </TableHead>
        <TableBody>
          {adding === ROOT && renderAddRow(0, null)}
          {rows.map(({ area, depth, hasChildren }) => {
            const isOpen = search !== null || open.has(area.id);
            const below = levelBelow(area.level);
            return (
              <React.Fragment key={area.id}>
                <TableRow
                  data-id={area.id}
                  data-testid={`locations-area-${area.id}`}
                  tabIndex={0}
                  aria-level={area.level}
                  aria-expanded={hasChildren ? isOpen : undefined}
                  className={area.matched ? "lo-highlight" : ""}
                >
                  <TableCell style={{ paddingLeft: `${1 + depth * 1.5}rem` }}>
                    {hasChildren ? (
                      <Button
                        kind="ghost"
                        size="sm"
                        hasIconOnly
                        renderIcon={isOpen ? ChevronDown : ChevronRight}
                        iconDescription={`${intl.formatMessage({
                          id: isOpen ? "button.collapse" : "button.expand",
                        })} ${area.name}`}
                        disabled={search !== null}
                        onClick={() => toggle(area.id)}
                      />
                    ) : (
                      <span
                        style={{ display: "inline-block", width: "2rem" }}
                      />
                    )}
                    {area.level === 1 ? (
                      <strong>{area.name}</strong>
                    ) : (
                      area.name
                    )}
                  </TableCell>
                  <TableCell>
                    <code>
                      {area.code ||
                        intl.formatMessage({ id: "label.locations.none" })}
                    </code>
                  </TableCell>
                  <TableCell>
                    <Tag type="warm-gray" size="sm">
                      {area.level}. {area.levelName}
                    </Tag>
                  </TableCell>
                  <TableCell>{area.childCount}</TableCell>
                  <TableCell>
                    <Toggle
                      id={`area-active-${area.id}`}
                      size="sm"
                      labelA={intl.formatMessage({
                        id: "label.locations.status.inactive",
                      })}
                      labelB={intl.formatMessage({
                        id: "label.locations.status.active",
                      })}
                      aria-label={`${intl.formatMessage({ id: "label.locations.column.active" })}, ${area.name}`}
                      toggled={area.active}
                      onToggle={(on) => changeActive(area, on)}
                    />
                  </TableCell>
                  <TableCell>
                    {below && area.active && (
                      <Button
                        kind="ghost"
                        size="sm"
                        renderIcon={Add}
                        data-testid={`locations-area-add-${area.id}`}
                        onClick={() => {
                          setAdding(area.id);
                          setDraft({ name: "", code: "" });
                          setOpen((current) => new Set([...current, area.id]));
                        }}
                      >
                        {below.name}
                      </Button>
                    )}
                    <Button
                      kind="ghost"
                      size="sm"
                      aria-label={`${intl.formatMessage({ id: "button.edit" })} ${area.name}`}
                      onClick={() => {
                        setEditing(editing === area.id ? null : area.id);
                        setDraft({ name: area.name, code: area.code || "" });
                        setErrors({});
                      }}
                    >
                      <FormattedMessage id="button.edit" />
                    </Button>
                  </TableCell>
                </TableRow>
                {editing === area.id && (
                  <TableRow>
                    <TableCell
                      colSpan={6}
                      style={{ paddingLeft: `${1 + depth * 1.5}rem` }}
                    >
                      <Stack orientation="horizontal" gap={3}>
                        <TextInput
                          id={`area-name-${area.id}`}
                          labelText={`${intl.formatMessage({ id: "label.locations.column.name" })} *`}
                          value={draft.name}
                          invalid={!!errors.name}
                          invalidText={errors.name}
                          onChange={(e) =>
                            setDraft({ ...draft, name: e.target.value })
                          }
                        />
                        <TextInput
                          id={`area-code-${area.id}`}
                          labelText={intl.formatMessage({
                            id: "label.locations.column.code",
                          })}
                          value={draft.code}
                          onChange={(e) =>
                            setDraft({ ...draft, code: e.target.value })
                          }
                        />
                        <Button
                          size="sm"
                          disabled={!draft.name.trim()}
                          onClick={() => saveArea(area, area.parentId)}
                        >
                          <FormattedMessage id="button.save" />
                        </Button>
                        <Button
                          kind="ghost"
                          size="sm"
                          onClick={() => setEditing(null)}
                        >
                          <FormattedMessage id="button.cancel" />
                        </Button>
                        <Button
                          kind="ghost"
                          size="sm"
                          onClick={() =>
                            setHistory(history === area.id ? null : area.id)
                          }
                        >
                          <FormattedMessage id="button.locations.history" />
                        </Button>
                      </Stack>
                      {history === area.id && (
                        <HistoryPanel id={area.id} name={area.name} />
                      )}
                    </TableCell>
                  </TableRow>
                )}
                {adding === area.id && renderAddRow(depth + 1, area)}
              </React.Fragment>
            );
          })}
        </TableBody>
      </Table>
      <p className="cds--label locationsFooterNote">
        {search !== null ? (
          <FormattedMessage
            id="message.locations.area.searchContext"
            values={{ count: search.filter((n) => n.matched).length }}
          />
        ) : (
          <FormattedMessage id="message.locations.area.loadOnExpand" />
        )}
      </p>
    </TableContainer>
  );
};

export default AreasView;
