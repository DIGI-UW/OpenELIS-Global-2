import { render, screen, act } from "@testing-library/react";
import {
  afterEach,
  beforeAll,
  beforeEach,
  describe,
  expect,
  test,
  vi,
} from "vitest";
import "@testing-library/jest-dom";
import { Roles } from "./components/utils/Utils";

/**
 * Two properties of App's route table that only show once a page is already on
 * screen: what happens when the page rewrites its own query string, which is
 * what every page keeping its filters in the URL does on each change, and what
 * the user still sees while a lazy page is fetching its chunk.
 *
 * <p>Its own file because a mocked route module does not come back out of the
 * module registry, the same reason App.chunkFailure.test.jsx is separate.
 */
const probe = vi.hoisted(() => ({
  mounts: 0,
  pick: null,
  suspending: false,
  pending: null,
  rerender: null,
}));

// /Microbiology/whonet sits inside the route table App rebuilds on every
// location change, and the page writes its own filters into the query string.
vi.mock("./pages/MicrobiologyWhonetPage", async () => {
  const React = await vi.importActual("react");
  const { useHistory } = await vi.importActual("react-router-dom");
  return {
    default: function WhonetProbe() {
      const [picked, setPicked] = React.useState([]);
      const history = useHistory();
      React.useEffect(() => {
        probe.mounts += 1;
      }, []);
      probe.pick = (value) =>
        setPicked((prev) => {
          const next = [...prev, value];
          history.replace(`/Microbiology/whonet?specimen=${next.join(",")}`);
          return next;
        });
      return <div data-testid="whonet-probe">{picked.join(",")}</div>;
    },
  };
});

// Header's own markup is not what is under test here, and rendering the whole
// component library behind it costs more than the rest of the file together.
vi.mock("./components/layout/Header", () => ({
  default: () => <div data-testid="chrome-header" />,
}));

// /Dashboard is one of the lazy routes with no Suspense of its own, so it
// suspends to whichever boundary App puts above it.
vi.mock("./components/Home", async () => {
  const React = await vi.importActual("react");
  return {
    default: function HomeProbe() {
      const [, setTick] = React.useState(0);
      probe.rerender = () => setTick((tick) => tick + 1);
      if (probe.suspending) {
        throw probe.pending;
      }
      return <div data-testid="home-probe">home</div>;
    },
  };
});

// Rendering the whole App is worth more than a default 5s budget once the rest
// of the suite is running alongside it.
const WHOLE_APP_RENDER_TIMEOUT_MS = 20000;

const renderAt = async (pathname, testId) => {
  const store = {};
  vi.stubGlobal("localStorage", {
    getItem: (key) => store[key] ?? null,
    setItem: (key, value) => {
      store[key] = value;
    },
    removeItem: (key) => delete store[key],
  });
  vi.spyOn(globalThis, "fetch").mockImplementation((resource) => {
    if (String(resource).endsWith("/session")) {
      return Promise.resolve({
        status: 200,
        json: async () => ({
          authenticated: true,
          roles: [Roles.GLOBAL_ADMIN],
          userLabRolesMap: {},
        }),
      });
    }
    return Promise.resolve(
      new Response("[]", {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }),
    );
  });
  window.history.pushState({}, "", pathname);
  const { default: App } = await import("./App");
  render(<App />);
  await screen.findByTestId(
    testId,
    {},
    { timeout: WHOLE_APP_RENDER_TIMEOUT_MS },
  );
};

// Layout is lazy, so without this the first render waits on its chunk inside a
// findBy poll and spends most of a default budget there.
beforeAll(async () => {
  await import("./components/layout/Layout");
});

beforeEach(() => {
  probe.mounts = 0;
  probe.suspending = false;
  probe.pending = new Promise(() => {});
});

afterEach(() => {
  window.history.pushState({}, "", "/");
  vi.restoreAllMocks();
  vi.unstubAllGlobals();
});

describe("App route table", () => {
  test(
    "keeps a page mounted when it rewrites its own query string",
    async () => {
      await renderAt("/Microbiology/whonet", "whonet-probe");
      expect(probe.mounts).toBe(1);

      await act(async () => probe.pick("46"));
      await act(async () => probe.pick("47"));

      expect(screen.getByTestId("whonet-probe")).toHaveTextContent("46,47");
      expect(probe.mounts).toBe(1);
      expect(window.location.search).toBe("?specimen=46,47");
    },
    WHOLE_APP_RENDER_TIMEOUT_MS,
  );

  test(
    "leaves the chrome painted while a page suspends",
    async () => {
      await renderAt("/Dashboard", "home-probe");
      expect(screen.getByTestId("content-wrapper")).toBeVisible();

      probe.suspending = true;
      await act(async () => probe.rerender());

      // React 17 keeps a suspended boundary's DOM and hides it, so the page
      // going invisible is what proves the boundary took the suspension.
      expect(screen.getByTestId("home-probe")).not.toBeVisible();
      expect(screen.getByTestId("chrome-header")).toBeVisible();
      expect(screen.getByTestId("content-wrapper")).toBeVisible();
    },
    WHOLE_APP_RENDER_TIMEOUT_MS,
  );
});
