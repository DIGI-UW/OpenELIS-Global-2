import { navigate } from "./commons";
import { dashboardMeta } from "./dashboard.meta";

export const makeThrottled = <T extends (...args: any[]) => any>(
  func: T,
  time = 1000,
): ((...funcArgs: Parameters<T>) => void) => {
  let waiting = false;
  let toBeExecuted = false;
  let lastArgs: Parameters<T> = null;

  const throttledFunc = (...args: Parameters<T>) => {
    if (!waiting) {
      waiting = true;
      setTimeout(() => {
        waiting = false;
        if (toBeExecuted) {
          toBeExecuted = false;
          throttledFunc(...lastArgs);
        }
      }, time);
      func(...args);
    } else {
      toBeExecuted = true;
      lastArgs = args;
    }
  };

  return throttledFunc;
};

export const navigateToTimeline = (basePath: string, panelUuid: string) =>
  navigate({ to: `${testResultsBasePath(basePath)}/timeline/${panelUuid}` });

export const navigateToTrendline = (
  basePath: string,
  panelUuid: string,
  testUuid: string,
) =>
  navigate({
    to: `${testResultsBasePath(basePath)}/trendline/${panelUuid}/${testUuid}`,
  });

export const navigateToResults = (basePath: string) =>
  navigate({ to: `${testResultsBasePath(basePath)}` });

export const testResultsBasePath = (basePath: string) =>
  `${"https://localhost/PatientHistory"}${basePath}/${dashboardMeta.title}`;
