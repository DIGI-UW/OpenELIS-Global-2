/**
 * SearchField tests — debounced type-ahead against
 * /rest/storage/locations/search. The endpoint returns a pre-composed
 * `hierarchicalPath` per result; clicking a result fires onSelect.
 */

import React from "react";
import {
  render as rtlRender,
  screen,
  fireEvent,
  waitFor,
  act,
} from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import SearchField from "./SearchField";
import * as Utils from "../../../utils/Utils";

// SearchField uses useIntl for its labelText/placeholder, so every
// render needs an IntlProvider ancestor. Override both render and the
// result's rerender so call sites don't need to wrap anything.
const withIntl = (ui) => (
  <IntlProvider locale="en" messages={{}}>
    {ui}
  </IntlProvider>
);
const render = (ui, options) => {
  const result = rtlRender(withIntl(ui), options);
  const originalRerender = result.rerender;
  result.rerender = (newUi) => originalRerender(withIntl(newUi));
  return result;
};

jest.mock("../../../utils/Utils", () => ({
  getFromOpenElisServer: jest.fn(),
}));

beforeEach(() => {
  jest.useFakeTimers();
  Utils.getFromOpenElisServer.mockReset();
});

afterEach(() => {
  act(() => {
    jest.runOnlyPendingTimers();
  });
  jest.useRealTimers();
});

describe("SearchField", () => {
  it("renders a text input with the controlled query value", () => {
    render(
      <SearchField
        query="freezer"
        results={[]}
        onQueryChange={jest.fn()}
        onResultsChange={jest.fn()}
        onSelect={jest.fn()}
      />,
    );
    expect(screen.getByRole("combobox")).toHaveValue("freezer");
  });

  it("calls onQueryChange when the user types", () => {
    const onQueryChange = jest.fn();
    render(
      <SearchField
        query=""
        results={[]}
        onQueryChange={onQueryChange}
        onResultsChange={jest.fn()}
        onSelect={jest.fn()}
      />,
    );
    fireEvent.change(screen.getByRole("combobox"), {
      target: { value: "Lab" },
    });
    expect(onQueryChange).toHaveBeenCalledWith("Lab");
  });

  it("ignores stale search responses after a newer query is issued", () => {
    const onResultsChange = jest.fn();
    const callbacks = [];
    Utils.getFromOpenElisServer.mockImplementation((url, cb) => {
      callbacks.push(cb);
    });

    const { rerender } = render(
      <SearchField
        query="Free"
        results={[]}
        onQueryChange={jest.fn()}
        onResultsChange={onResultsChange}
        onSelect={jest.fn()}
      />,
    );

    act(() => {
      jest.advanceTimersByTime(300);
    });

    rerender(
      <SearchField
        query="Shelf"
        results={[]}
        onQueryChange={jest.fn()}
        onResultsChange={onResultsChange}
        onSelect={jest.fn()}
      />,
    );

    act(() => {
      jest.advanceTimersByTime(300);
    });

    expect(callbacks).toHaveLength(2);

    act(() => {
      callbacks[0]([{ id: 1, type: "device", name: "Old Result" }]);
    });
    expect(onResultsChange).not.toHaveBeenCalledWith([
      { id: 1, type: "device", name: "Old Result" },
    ]);

    act(() => {
      callbacks[1]([{ id: 2, type: "rack", name: "Latest Result" }]);
    });
    expect(onResultsChange).toHaveBeenCalledWith([
      { id: 2, type: "rack", name: "Latest Result" },
    ]);
  });

  it("does not fire the search API for queries shorter than 2 chars", () => {
    render(
      <SearchField
        query="L"
        results={[]}
        onQueryChange={jest.fn()}
        onResultsChange={jest.fn()}
        onSelect={jest.fn()}
      />,
    );
    act(() => {
      jest.advanceTimersByTime(1000);
    });
    expect(Utils.getFromOpenElisServer).not.toHaveBeenCalled();
  });

  it("debounces the search API call when query has ≥2 chars", () => {
    const onResultsChange = jest.fn();
    const { rerender } = render(
      <SearchField
        query="La"
        results={[]}
        onQueryChange={jest.fn()}
        onResultsChange={onResultsChange}
        onSelect={jest.fn()}
      />,
    );
    // Before debounce timer fires, no call yet
    act(() => {
      jest.advanceTimersByTime(100);
    });
    expect(Utils.getFromOpenElisServer).not.toHaveBeenCalled();

    // Type more before debounce — should reset, still no call
    rerender(
      <SearchField
        query="Lab"
        results={[]}
        onQueryChange={jest.fn()}
        onResultsChange={onResultsChange}
        onSelect={jest.fn()}
      />,
    );
    act(() => {
      jest.advanceTimersByTime(100);
    });
    expect(Utils.getFromOpenElisServer).not.toHaveBeenCalled();

    // After full debounce delay — exactly one call with latest query
    act(() => {
      jest.advanceTimersByTime(300);
    });
    expect(Utils.getFromOpenElisServer).toHaveBeenCalledTimes(1);
    expect(Utils.getFromOpenElisServer.mock.calls[0][0]).toBe(
      "/rest/storage/locations/search?q=Lab",
    );
  });

  it("invokes onResultsChange with the API response", () => {
    const onResultsChange = jest.fn();
    Utils.getFromOpenElisServer.mockImplementation((url, cb) => {
      cb([
        {
          id: 5,
          type: "device",
          name: "Freezer 1",
          hierarchicalPath: "Main Lab > Freezer 1",
        },
      ]);
    });
    render(
      <SearchField
        query="Free"
        results={[]}
        onQueryChange={jest.fn()}
        onResultsChange={onResultsChange}
        onSelect={jest.fn()}
      />,
    );
    act(() => {
      jest.advanceTimersByTime(300);
    });
    expect(onResultsChange).toHaveBeenCalledWith([
      {
        id: 5,
        type: "device",
        name: "Freezer 1",
        hierarchicalPath: "Main Lab > Freezer 1",
      },
    ]);
  });

  it("renders each result in a listbox showing its hierarchicalPath", () => {
    const results = [
      {
        id: 5,
        type: "device",
        name: "Freezer 1",
        hierarchicalPath: "Main Lab > Freezer 1",
      },
      {
        id: 10,
        type: "shelf",
        name: "Shelf A",
        hierarchicalPath: "Main Lab > Freezer 1 > Shelf A",
      },
    ];
    render(
      <SearchField
        query="Lab"
        results={results}
        onQueryChange={jest.fn()}
        onResultsChange={jest.fn()}
        onSelect={jest.fn()}
      />,
    );
    expect(screen.getByRole("listbox")).toBeInTheDocument();
    expect(
      screen.getByRole("option", { name: /Main Lab > Freezer 1$/ }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("option", { name: /Main Lab > Freezer 1 > Shelf A/ }),
    ).toBeInTheDocument();
  });

  it("falls back to the result name when hierarchicalPath is absent", () => {
    render(
      <SearchField
        query="Main"
        results={[{ id: 1, type: "room", name: "Main Lab" }]}
        onQueryChange={jest.fn()}
        onResultsChange={jest.fn()}
        onSelect={jest.fn()}
      />,
    );
    expect(
      screen.getByRole("option", { name: "Main Lab" }),
    ).toBeInTheDocument();
  });

  it("sets aria-selected from current picker selection", () => {
    render(
      <SearchField
        query="Free"
        results={[
          { id: 11, type: "device", name: "Freezer 1" },
          { id: 12, type: "device", name: "Freezer 2" },
        ]}
        selectedSelection={{
          room: { id: 1, name: "Main Lab" },
          device: { id: 12, name: "Freezer 2" },
        }}
        onQueryChange={jest.fn()}
        onResultsChange={jest.fn()}
        onSelect={jest.fn()}
      />,
    );

    expect(screen.getByRole("option", { name: "Freezer 2" })).toHaveAttribute(
      "aria-selected",
      "true",
    );
    expect(screen.getByRole("option", { name: "Freezer 1" })).toHaveAttribute(
      "aria-selected",
      "false",
    );
  });

  it("supports arrow keys plus Enter selection from active descendant", () => {
    const onSelect = jest.fn();
    render(
      <SearchField
        query="Main"
        results={[
          { id: 1, type: "room", name: "Main Lab" },
          { id: 2, type: "room", name: "Secondary Lab" },
        ]}
        onQueryChange={jest.fn()}
        onResultsChange={jest.fn()}
        onSelect={onSelect}
      />,
    );

    const input = screen.getByRole("combobox");
    fireEvent.keyDown(input, { key: "ArrowDown" });
    fireEvent.keyDown(input, { key: "Enter" });

    expect(onSelect).toHaveBeenCalledWith({
      id: 2,
      type: "room",
      name: "Secondary Lab",
    });
  });

  it("calls onSelect with the picked result on click", () => {
    const onSelect = jest.fn();
    const result = {
      id: 5,
      type: "device",
      name: "Freezer 1",
      hierarchicalPath: "Main Lab > Freezer 1",
    };
    render(
      <SearchField
        query="Free"
        results={[result]}
        onQueryChange={jest.fn()}
        onResultsChange={jest.fn()}
        onSelect={onSelect}
      />,
    );
    fireEvent.click(
      screen.getByRole("option", { name: /Main Lab > Freezer 1/ }),
    );
    expect(onSelect).toHaveBeenCalledWith(result);
  });

  it("renders no listbox when results is empty (no stale UI)", () => {
    render(
      <SearchField
        query="zzz"
        results={[]}
        onQueryChange={jest.fn()}
        onResultsChange={jest.fn()}
        onSelect={jest.fn()}
      />,
    );
    expect(screen.queryByRole("listbox")).not.toBeInTheDocument();
  });
});
