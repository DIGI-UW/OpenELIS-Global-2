import React, { useEffect, useMemo, useRef, useState } from "react";
import { Button, Search } from "@carbon/react";
import {
  ChevronDown,
  ChevronRight,
  ChevronUp,
  Close,
} from "@carbon/icons-react";
import { useIntl } from "react-intl";

// Port of the canonical mock's column builder at openelis-work 5b2df7e34f.
// Keep browse folds independent of search folds so clearing search restores context.
export default function ReportingColumns({ fields, selected, onChange }) {
  const intl = useIntl();
  const t = (id, values) => intl.formatMessage({ id }, values);
  const groups = useMemo(() => {
    const result = new Map();
    fields.forEach((field) => {
      if (!result.has(field.group)) result.set(field.group, []);
      result.get(field.group).push(field);
    });
    return [...result].map(([id, vars]) => ({
      id,
      label: intl.formatMessage({ id: `reporting.group.${id}` }),
      vars,
    }));
  }, [fields, intl]);
  const [collapsed, setCollapsed] = useState(
    () => new Set(groups.map((g) => g.id)),
  );
  const [searchCollapsed, setSearchCollapsed] = useState(new Set());
  const [search, setSearch] = useState("");
  const [view, setView] = useState("browse");
  const [message, setMessage] = useState("");
  const [dragging, setDragging] = useState(null);
  const [drop, setDrop] = useState(null);
  const handles = useRef(new Map());
  const focus = useRef(null);
  const searchInput = useRef(null);
  const dragged = useRef(null);
  const query = search.trim().toLocaleLowerCase();
  const folds = query ? searchCollapsed : collapsed;
  const setFolds = query ? setSearchCollapsed : setCollapsed;
  const byId = new Map(fields.map((f) => [f.id, f]));
  const chosen = new Set(selected);
  const visible = groups
    .map((group) => ({
      ...group,
      total: group.vars.length,
      added: group.vars.filter((f) => chosen.has(f.id)).length,
      vars: group.vars.filter(
        (f) =>
          !query ||
          `${group.label} ${f.label}`.toLocaleLowerCase().includes(query),
      ),
    }))
    .filter((group) => group.vars.length);
  const allExpanded = visible.every((g) => !folds.has(g.id));
  const allCollapsed = visible.every((g) => folds.has(g.id));
  const changeSearch = (value) => {
    setSearch(value);
    setSearchCollapsed(new Set());
  };
  const toggle = (id) =>
    setFolds((previous) => {
      const next = new Set(previous);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  useEffect(() => {
    if (focus.current) {
      handles.current.get(focus.current)?.focus();
      focus.current = null;
    }
  }, [selected, view]);
  const add = (field) => {
    if (chosen.has(field.id)) return;
    onChange([...selected, field.id]);
    setMessage(
      t("reporting.design.addedMessage", {
        field: field.label,
        count: selected.length + 1,
      }),
    );
  };
  const remove = (id) => {
    const next = selected.filter((key) => key !== id);
    focus.current = next[Math.min(selected.indexOf(id), next.length - 1)];
    onChange(next);
    setMessage(
      t("reporting.design.removedMessage", {
        field: byId.get(id)?.label || id,
        count: next.length,
      }),
    );
  };
  const move = (id, target) => {
    if (
      target < 0 ||
      target >= selected.length ||
      selected.indexOf(id) === target
    )
      return;
    const next = selected.filter((key) => key !== id);
    next.splice(target, 0, id);
    focus.current = id;
    onChange(next);
    setMessage(
      t("reporting.position", {
        field: byId.get(id)?.label || id,
        position: target + 1,
        count: next.length,
      }),
    );
  };
  const finishDrag = () => {
    dragged.current = null;
    setDragging(null);
    setDrop(null);
  };
  const dropPosition = (event) => {
    const rect = event.currentTarget.getBoundingClientRect();
    return event.clientY > rect.top + rect.height / 2;
  };
  const dropColumn = (event, id) => {
    event.preventDefault();
    const moving = dragged.current;
    if (moving && moving !== id) {
      const remaining = selected.filter((key) => key !== moving);
      const target = remaining.indexOf(id) + (dropPosition(event) ? 1 : 0);
      move(moving, target);
    }
    finishDrag();
  };
  return (
    <>
      <div
        className="column-mobile-nav"
        role="group"
        aria-label={t("reporting.design.columnViews")}
      >
        <Button
          kind="ghost"
          aria-pressed={view === "browse"}
          onClick={() => setView("browse")}
        >
          {t("reporting.design.available")}
        </Button>
        <Button
          kind="ghost"
          aria-pressed={view === "columns"}
          onClick={() => setView("columns")}
        >
          {t("reporting.design.yourColumns", { count: selected.length })}
        </Button>
      </div>
      <div className="column-builder" data-view={view}>
        <section
          className="field-browser"
          aria-label={t("reporting.design.available")}
        >
          <h2>{t("reporting.design.available")}</h2>
          <div className="field-search">
            <label className="cds--label" htmlFor="reporting-field-search">
              {t("reporting.design.findField")}
            </label>
            <Search
              id="reporting-field-search"
              ref={searchInput}
              labelText=""
              placeholder={t("reporting.design.searchPlaceholder")}
              value={search}
              onChange={(event) => changeSearch(event.target.value)}
            />
          </div>
          <div className="catalog-tools">
            <p className="column-hint" role="status">
              {t(
                query
                  ? "reporting.design.matchCount"
                  : "reporting.design.fieldCount",
                {
                  count: visible.reduce((sum, g) => sum + g.vars.length, 0),
                  groups: visible.length,
                },
              )}
            </p>
            {query && (
              <Button
                kind="ghost"
                size="sm"
                onClick={() => {
                  changeSearch("");
                  searchInput.current?.focus();
                }}
              >
                {t("reporting.design.clearSearch")}
              </Button>
            )}
          </div>
          {visible.length > 0 && (
            <div
              className="catalog-expansion"
              role="group"
              aria-label={t("reporting.design.expansion")}
            >
              <Button
                kind="ghost"
                size="sm"
                aria-disabled={allExpanded}
                onClick={() => !allExpanded && setFolds(new Set())}
              >
                {t("reporting.design.expandAll")}
              </Button>
              <Button
                kind="ghost"
                size="sm"
                aria-disabled={allCollapsed}
                onClick={() =>
                  !allCollapsed && setFolds(new Set(visible.map((g) => g.id)))
                }
              >
                {t("reporting.design.collapseAll")}
              </Button>
            </div>
          )}
          <div
            className="field-catalog"
            role="region"
            aria-label={t("reporting.design.scrollCatalog")}
            tabIndex="0"
          >
            {visible.map((group) => {
              const expanded = !folds.has(group.id);
              const allAdded = group.vars.every((f) => chosen.has(f.id));
              return (
                <section
                  className="field-domain"
                  key={group.id}
                  aria-labelledby={`reporting-group-${group.id}`}
                >
                  <div className="field-domain-heading">
                    <h3>
                      <Button
                        kind="ghost"
                        className="field-group-toggle"
                        id={`reporting-group-${group.id}`}
                        aria-expanded={expanded}
                        aria-controls={`reporting-group-fields-${group.id}`}
                        onClick={() => toggle(group.id)}
                      >
                        {expanded ? (
                          <ChevronDown size={16} />
                        ) : (
                          <ChevronRight size={16} />
                        )}
                        {group.label}
                      </Button>
                    </h3>
                    <span className="field-group-count">
                      {t("reporting.design.groupCount", {
                        added: group.added,
                        total: group.total,
                      })}
                    </span>
                  </div>
                  <div
                    id={`reporting-group-fields-${group.id}`}
                    hidden={!expanded}
                  >
                    {expanded && (
                      <>
                        <div className="field-group-tools">
                          <Button
                            kind="ghost"
                            size="sm"
                            aria-label={t(
                              allAdded
                                ? "reporting.design.removeShownGroup"
                                : "reporting.design.addShownGroup",
                              { group: group.label },
                            )}
                            onClick={() => {
                              const ids = new Set(group.vars.map((f) => f.id));
                              onChange(
                                allAdded
                                  ? selected.filter((id) => !ids.has(id))
                                  : [
                                      ...selected,
                                      ...group.vars
                                        .filter((f) => !chosen.has(f.id))
                                        .map((f) => f.id),
                                    ],
                              );
                            }}
                          >
                            {t(
                              allAdded
                                ? "reporting.design.removeShown"
                                : "reporting.design.addShown",
                            )}
                          </Button>
                        </div>
                        <ul className="available-field-list">
                          {group.vars.map((field) => (
                            <li key={field.id}>
                              <span>{field.label}</span>
                              <Button
                                kind="ghost"
                                size="sm"
                                className="field-add"
                                aria-label={t(
                                  chosen.has(field.id)
                                    ? "reporting.design.addedField"
                                    : "reporting.design.addField",
                                  { field: field.label },
                                )}
                                aria-disabled={chosen.has(field.id)}
                                onClick={() => add(field)}
                              >
                                {t(
                                  chosen.has(field.id)
                                    ? "reporting.design.added"
                                    : "reporting.design.add",
                                )}
                              </Button>
                            </li>
                          ))}
                        </ul>
                      </>
                    )}
                  </div>
                </section>
              );
            })}
            {!visible.length && (
              <p className="column-empty">{t("reporting.design.noFields")}</p>
            )}
          </div>
        </section>
        <section className="columns-pane" aria-label={t("reporting.selected")}>
          <h2>
            {t("reporting.design.selectedCount", { count: selected.length })}
          </h2>
          <p className="column-hint" id="reporting-column-order-help">
            {t("reporting.design.orderHelp")}
          </p>
          <ol
            className="selected-columns"
            aria-label={t("reporting.design.columnOrder")}
          >
            {selected.map((id, index) => {
              const label = byId.get(id)?.label || id;
              return (
                <li
                  key={id}
                  draggable
                  className={
                    dragging === id
                      ? "dragging"
                      : drop?.id === id
                        ? `drop-${drop.after ? "after" : "before"}`
                        : ""
                  }
                  onDragStart={(event) => {
                    dragged.current = id;
                    setDragging(id);
                    event.dataTransfer.effectAllowed = "move";
                    event.dataTransfer.setData("text/plain", id);
                  }}
                  onDragEnd={finishDrag}
                  onDragOver={(event) => {
                    event.preventDefault();
                    if (dragged.current)
                      setDrop({ id, after: dropPosition(event) });
                  }}
                  onDrop={(event) => dropColumn(event, id)}
                >
                  <Button
                    kind="ghost"
                    className="column-drag-handle"
                    ref={(element) => {
                      if (element) handles.current.set(id, element);
                      else handles.current.delete(id);
                    }}
                    aria-label={t("reporting.design.drag", { field: label })}
                    aria-describedby="reporting-column-order-help"
                    onKeyDown={(event) => {
                      const target = {
                        ArrowUp: index - 1,
                        ArrowDown: index + 1,
                        Home: 0,
                        End: selected.length - 1,
                      }[event.key];
                      if (target !== undefined) {
                        event.preventDefault();
                        move(id, target);
                      }
                    }}
                  >
                    <svg
                      width="16"
                      height="20"
                      viewBox="0 0 16 20"
                      aria-hidden="true"
                      fill="currentColor"
                    >
                      <circle cx="5" cy="4" r="1.5" />
                      <circle cx="11" cy="4" r="1.5" />
                      <circle cx="5" cy="10" r="1.5" />
                      <circle cx="11" cy="10" r="1.5" />
                      <circle cx="5" cy="16" r="1.5" />
                      <circle cx="11" cy="16" r="1.5" />
                    </svg>
                  </Button>
                  <span className="column-number" aria-hidden="true">
                    {index + 1}
                  </span>
                  <span className="column-name">{label}</span>
                  <div className="column-actions">
                    <div
                      className="column-order-buttons"
                      role="group"
                      aria-label={t("reporting.design.move", { field: label })}
                    >
                      <Button
                        kind="ghost"
                        hasIconOnly
                        renderIcon={ChevronUp}
                        iconDescription={t("reporting.moveUp", {
                          field: label,
                        })}
                        aria-disabled={index === 0}
                        onClick={() => move(id, index - 1)}
                      />
                      <Button
                        kind="ghost"
                        hasIconOnly
                        renderIcon={ChevronDown}
                        iconDescription={t("reporting.moveDown", {
                          field: label,
                        })}
                        aria-disabled={index === selected.length - 1}
                        onClick={() => move(id, index + 1)}
                      />
                    </div>
                    <Button
                      kind="ghost"
                      className="column-remove"
                      hasIconOnly
                      renderIcon={Close}
                      iconDescription={t("reporting.remove", { field: label })}
                      onClick={() => remove(id)}
                    />
                  </div>
                </li>
              );
            })}
          </ol>
          {!selected.length && (
            <p className="column-empty">{t("reporting.design.emptyColumns")}</p>
          )}
        </section>
      </div>
      <p className="column-feedback" role="status" aria-live="polite">
        {message}
      </p>
    </>
  );
}
