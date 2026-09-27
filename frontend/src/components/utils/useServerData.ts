import { useContext, useEffect, useRef } from "react";
import { useIntl } from "react-intl";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { serverQuery } from "./queryClient";
import { NotificationContext } from "../layout/contexts";
import { NotificationKinds } from "../common/CustomNotification";

/** Every read through this hook shares this prefix, so one call can retire all of it. */
export const SERVER_DATA_KEY = "serverData";

/**
 * Reads a GET endpoint and keeps it in the cache, replacing the
 * `useState` + `useEffect` + `getFromOpenElisServer` triple each screen used to
 * write by hand. The endpoint is the key, so the same URL read from two places
 * is fetched once and refreshed together.
 *
 * Pass a falsy endpoint to hold off — for a read that depends on a selection the
 * user has not made yet.
 *
 * Failed reads notify screens that already have data. Screens with a required
 * initial read render ServerDataState while data is absent, so their early
 * return exposes an error and retry action as well as the loading state.
 */
export const useServerData = <T>(endPoint: string | null | undefined) => {
  const query = useQuery({
    ...serverQuery<T>([SERVER_DATA_KEY, endPoint ?? ""], endPoint ?? ""),
    enabled: Boolean(endPoint),
    keepPreviousData: true,
  });

  const notificationContext = useContext(NotificationContext);
  const intl = useIntl();
  const notifiedFor = useRef<string | null>(null);

  useEffect(() => {
    if (!endPoint) {
      return;
    }
    if (query.isError) {
      if (notifiedFor.current === endPoint) {
        return;
      }
      notifiedFor.current = endPoint;
      notificationContext?.addNotification?.({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "server.error.msg" }),
      });
      notificationContext?.setNotificationVisible?.(true);
    } else {
      notifiedFor.current = null;
    }
  }, [query.isError, endPoint, notificationContext, intl]);

  return query;
};

/**
 * Marks server reads as out of date after a write, so the screens showing them
 * read again.
 *
 * Called with no endpoint it retires every read, which is what reloading the
 * document used to do — and the reason to prefer it on a screen built from
 * several reads, where refreshing only one would leave the rest stale.
 */
export const useInvalidateServerData = () => {
  const queryClient = useQueryClient();
  return (endPoint?: string) =>
    queryClient.invalidateQueries({
      queryKey: endPoint ? [SERVER_DATA_KEY, endPoint] : [SERVER_DATA_KEY],
    });
};
