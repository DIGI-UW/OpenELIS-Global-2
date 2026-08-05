import React, {
  forwardRef,
  useEffect,
  useImperativeHandle,
  useMemo,
  useRef,
  useState,
} from "react";
import { Layer, TextArea } from "@carbon/react";
import { useIntl } from "react-intl";
import { getTextMacros } from "./TextMacroService";
import {
  expandMacroToken,
  filterMacroSuggestions,
  getMacroToken,
} from "./macroTextEngine";
import "./textMacro.scss";

const MacroTextArea = forwardRef(
  ({ context, value = "", onChange, onKeyDown, ...textAreaProps }, ref) => {
    const intl = useIntl();
    const inputRef = useRef(null);
    const [macros, setMacros] = useState([]);
    const [loaded, setLoaded] = useState(false);
    const [loading, setLoading] = useState(false);
    const [open, setOpen] = useState(false);
    const [token, setToken] = useState(null);
    const [activeIndex, setActiveIndex] = useState(0);
    const [announcement, setAnnouncement] = useState("");
    const requestRef = useRef(null);

    useImperativeHandle(ref, () => inputRef.current);

    useEffect(() => {
      requestRef.current?.abort();
      setMacros([]);
      setLoaded(false);
      setOpen(false);
      return () => requestRef.current?.abort();
    }, [context]);

    const load = () => {
      if (loaded || loading) return;
      requestRef.current?.abort();
      const controller = new AbortController();
      requestRef.current = controller;
      setLoading(true);
      getTextMacros(context, controller.signal)
        .then((items) => {
          setMacros(Array.isArray(items) ? items : items?.items || []);
          setLoaded(true);
        })
        .catch((error) => {
          if (error.name === "AbortError") return;
          setAnnouncement(
            intl.formatMessage({ id: "textMacro.runtime.loadError" }),
          );
        })
        .finally(() => {
          if (!controller.signal.aborted) setLoading(false);
        });
    };

    const suggestions = useMemo(() => {
      return filterMacroSuggestions(macros, token?.token);
    }, [macros, token]);

    useEffect(() => {
      if (!open) return;
      setAnnouncement(
        intl.formatMessage(
          { id: "textMacro.runtime.resultCount" },
          { count: suggestions.length },
        ),
      );
    }, [intl, open, suggestions.length]);

    const emitValue = (nextValue) => {
      onChange({ target: { value: nextValue } });
    };

    const replaceToken = (macro, suffix = "", replacementToken = token) => {
      if (!replacementToken) return;
      const expansion = expandMacroToken(
        value,
        replacementToken,
        macro,
        suffix,
      );
      if (!expansion) return;
      emitValue(expansion.value);
      setOpen(false);
      setAnnouncement(
        intl.formatMessage(
          { id: "textMacro.runtime.inserted" },
          { code: macro.code },
        ),
      );
      requestAnimationFrame(() => {
        inputRef.current?.focus();
        inputRef.current?.setSelectionRange(expansion.caret, expansion.caret);
      });
    };

    const updateToken = (event) => {
      const nextToken = getMacroToken(
        event.target.value,
        event.target.selectionStart ?? event.target.value.length,
      );
      setToken(nextToken);
      setActiveIndex(0);
      setOpen(Boolean(nextToken?.token?.startsWith(".")));
      onChange(event);
    };

    const handleKeyDown = (event) => {
      const currentToken = getMacroToken(
        value,
        event.currentTarget.selectionStart ?? value.length,
      );
      setToken(currentToken);
      const exact = currentToken
        ? macros.find(
            (macro) =>
              macro.code.toLowerCase() === currentToken.token.toLowerCase(),
          )
        : null;

      if ((event.key === " " || event.key === "Tab") && exact) {
        event.preventDefault();
        setToken(currentToken);
        replaceToken(exact, event.key === " " ? " " : "", currentToken);
        return;
      }
      if (open && suggestions.length > 0 && event.key === "ArrowDown") {
        event.preventDefault();
        setActiveIndex((current) => (current + 1) % suggestions.length);
        return;
      }
      if (open && suggestions.length > 0 && event.key === "ArrowUp") {
        event.preventDefault();
        setActiveIndex(
          (current) => (current - 1 + suggestions.length) % suggestions.length,
        );
        return;
      }
      if (
        open &&
        suggestions.length > 0 &&
        (event.key === "Enter" || event.key === "Tab")
      ) {
        event.preventDefault();
        replaceToken(suggestions[activeIndex]);
        return;
      }
      if (event.key === "Escape") {
        setOpen(false);
      }
      onKeyDown?.(event);
    };

    const listboxId = `${textAreaProps.id}-macro-options`;

    return (
      <div className="text-macro-field">
        <TextArea
          {...textAreaProps}
          ref={inputRef}
          value={value}
          onFocus={(event) => {
            load();
            textAreaProps.onFocus?.(event);
          }}
          onChange={updateToken}
          onKeyDown={handleKeyDown}
          aria-autocomplete="list"
          aria-label={textAreaProps["aria-label"] || textAreaProps.labelText}
          aria-controls={open ? listboxId : undefined}
          aria-expanded={open}
          aria-activedescendant={
            open && suggestions[activeIndex]
              ? `${listboxId}-${suggestions[activeIndex].id}`
              : undefined
          }
        />
        {open && (loading || suggestions.length > 0) && (
          <Layer className="text-macro-field__popover">
            <div
              id={listboxId}
              role="listbox"
              aria-label={intl.formatMessage({
                id: "textMacro.runtime.suggestions",
              })}
              className="text-macro-field__options"
            >
              {loading ? (
                <div className="text-macro-field__status">
                  {intl.formatMessage({ id: "textMacro.runtime.loading" })}
                </div>
              ) : (
                suggestions.map((macro, index) => (
                  <button
                    id={`${listboxId}-${macro.id}`}
                    type="button"
                    role="option"
                    aria-selected={index === activeIndex}
                    className="text-macro-field__option"
                    key={macro.id}
                    onMouseDown={(event) => event.preventDefault()}
                    onClick={() => replaceToken(macro)}
                  >
                    <span className="text-macro-field__code">{macro.code}</span>
                    <span>{macro.expansionText}</span>
                  </button>
                ))
              )}
            </div>
          </Layer>
        )}
        <span className="text-macro-field__announcement" aria-live="polite">
          {announcement}
        </span>
      </div>
    );
  },
);

MacroTextArea.displayName = "MacroTextArea";

export default MacroTextArea;
